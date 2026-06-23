"""
validate_study_dag.py
Checks if cancer studies exist in S3, then for each found study runs a single pod that
downloads the study to /tmp and validates it. After all validations complete, a single
import pod imports only the studies that passed.
Missing studies warn in check_studies; failed validations are excluded from import.
"""
import logging
from datetime import timedelta

from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowSkipException
from airflow.models.param import Param
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from airflow.utils.dates import days_ago
from airflow.utils.trigger_rule import TriggerRule

# TODO: fill in deployment-specific values
S3_BUCKET            = "hackathon-databricks"
def _current_namespace() -> str:
    try:
        with open("/var/run/secrets/kubernetes.io/serviceaccount/namespace") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "default"

K8S_NAMESPACE        = _current_namespace()
K8S_IMAGE            = "python:3.11-slim"
K8S_SERVICE_ACCOUNT  = None  # set if your cluster requires a specific service account
VALIDATE_SCRIPT_PATH = "/scripts/validate_study.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer.py"

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}

# Writes the study_id to XCom on success so collect_valid_studies can gather results.
# If validate_study.py exits non-zero, subprocess.run raises and the pod fails —
# no XCom is written, so the study is excluded from import.
_PULL_AND_VALIDATE_SCRIPT = f"""\
import subprocess, sys
subprocess.run([sys.executable, "-m", "pip", "install", "boto3", "-q"], check=True)
import boto3, json, os, pathlib
from botocore import UNSIGNED
from botocore.config import Config
study_id  = os.environ["CANCER_STUDY_ID"]
bucket    = os.environ["S3_BUCKET"]
local_dir = f"/tmp/{{study_id}}"
pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

print(f"Downloading s3://{{bucket}}/{{study_id}}/ -> {{local_dir}}")
s3        = boto3.client("s3", config=Config(signature_version=UNSIGNED))
prefix    = f"{{study_id}}/"
paginator = s3.get_paginator("list_objects_v2")
for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
    for obj in page.get("Contents", []):
        key      = obj["Key"]
        rel_path = key[len(prefix):]
        if not rel_path:
            continue
        dest = os.path.join(local_dir, rel_path)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        s3.download_file(bucket, key, dest)
        print(f"  {{key}} -> {{dest}}")
print("Download complete. Running validate_study.py...")

print(f"[STUB] Would run: {sys.executable} {VALIDATE_SCRIPT_PATH} {{local_dir}}")

os.makedirs("/airflow/xcom", exist_ok=True)
with open("/airflow/xcom/return.json", "w") as f:
    json.dump(study_id, f)
"""

# Receives STUDY_IDS_JSON (a JSON array of study IDs) from the collect_valid_studies XCom.
# TODO: replace the subprocess stub with the real importer invocation.
_IMPORT_SCRIPT = f"""\
import json, os, sys
study_ids = json.loads(os.environ["STUDY_IDS_JSON"])
if not study_ids:
    print("No valid studies to import — exiting.")
    sys.exit(0)
print(f"Importing {{len(study_ids)}} studies: {{study_ids}}")
print(f"[STUB] Would run: {sys.executable} {IMPORT_SCRIPT_PATH} {{' '.join(study_ids)}}")
"""

with DAG(
    dag_id="validate_study_dag",
    default_args=_DEFAULT_ARGS,
    description="Check, pull, validate, and import cancer studies from S3",
    max_active_runs=1,
    start_date=days_ago(2),
    schedule_interval=None,
    tags=["validation"],
    params={
        "cancer_study_ids": Param(
            "",
            type="string",
            description="Comma-separated cancer study IDs to validate (e.g. msk_impact_2017,brca_tcga)",
            title="Cancer Study IDs",
        ),
    },
) as dag:

    @task
    def check_studies(study_ids_str: str) -> list[str]:
        """
        Checks each study ID against S3.
        Logs a WARNING for each missing study; returns only the found ones.
        Raises AirflowSkipException if none are found.
        """
        import boto3  # imported here to avoid loading on scheduler parse
        from botocore import UNSIGNED
        from botocore.config import Config

        study_ids = [s.strip() for s in study_ids_str.split(",") if s.strip()]
        if not study_ids:
            raise AirflowSkipException("No study IDs provided")

        s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
        found = []
        for study_id in study_ids:
            resp = s3.list_objects_v2(Bucket=S3_BUCKET, Prefix=f"{study_id}/", MaxKeys=1)
            if resp.get("KeyCount", 0) > 0:
                found.append(study_id)
                logging.info("Study '%s' found in s3://%s", study_id, S3_BUCKET)
            else:
                logging.warning(
                    "Study '%s' NOT found in s3://%s — will be skipped",
                    study_id,
                    S3_BUCKET,
                )

        if not found:
            raise AirflowSkipException("None of the requested studies exist in S3")

        return found

    @task
    def build_kpo_kwargs(study_ids: list[str]) -> list[dict]:
        """Converts found study IDs into per-study env_var dicts for dynamic task mapping."""
        return [
            {"env_vars": {"CANCER_STUDY_ID": sid, "S3_BUCKET": S3_BUCKET}}
            for sid in study_ids
        ]

    @task(trigger_rule=TriggerRule.ALL_DONE)
    def collect_valid_studies(results: list) -> list[str]:
        """
        Runs after all pull_and_validate pods finish (success or failure).
        Each successful pod wrote its study_id to XCom; failed pods wrote nothing (None).
        Returns only the study IDs that passed, or skips import if none did.
        """
        valid = [sid for sid in (results or []) if sid is not None]
        if not valid:
            raise AirflowSkipException("No studies passed validation — skipping import")
        logging.info("Studies passing validation: %s", valid)
        return valid

    found_studies  = check_studies("{{ params.cancer_study_ids }}")
    kpo_kwargs     = build_kpo_kwargs(found_studies)

    # Parallel: one pod per found study
    pull_and_validate = KubernetesPodOperator.partial(
        task_id="pull_and_validate_study",
        namespace=K8S_NAMESPACE,
        image=K8S_IMAGE,
        name="pull-and-validate-study",
        service_account_name=K8S_SERVICE_ACCOUNT,
        cmds=["python", "-c"],
        arguments=[_PULL_AND_VALIDATE_SCRIPT],
        do_xcom_push=True,
        get_logs=True,
        is_delete_operator_pod=True,
    ).expand_kwargs(kpo_kwargs)

    valid_studies = collect_valid_studies(pull_and_validate.output)

    # Single pod: imports all validated studies in one call
    import_studies = KubernetesPodOperator(
        task_id="import_studies",
        namespace=K8S_NAMESPACE,
        image=K8S_IMAGE,
        name="import-studies",
        service_account_name=K8S_SERVICE_ACCOUNT,
        cmds=["python", "-c"],
        arguments=[_IMPORT_SCRIPT],
        env_vars={"STUDY_IDS_JSON": "{{ ti.xcom_pull(task_ids='collect_valid_studies') | tojson }}"},
        get_logs=True,
        is_delete_operator_pod=True,
    )

    valid_studies >> import_studies
