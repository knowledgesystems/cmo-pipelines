"""
import_public_hackathon.py
"""
import logging
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.exceptions import AirflowSkipException
from airflow.models.param import Param
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from airflow.utils.trigger_rule import TriggerRule

S3_BUCKET            = "hackathon-databricks"
K8S_IMAGE            = "python:3.11-slim"
VALIDATE_SCRIPT_PATH = "/scripts/validate_study.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer.py"

def _current_namespace() -> str:
    try:
        with open("/var/run/secrets/kubernetes.io/serviceaccount/namespace") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "default"

K8S_NAMESPACE       = _current_namespace()
K8S_SERVICE_ACCOUNT = None
K8S_EXECUTOR_CONFIG = {"KubernetesExecutor": {"image": K8S_IMAGE}}

_VERIFY_STUDIES_SCRIPT = """\
import subprocess, sys
subprocess.run([sys.executable, "-m", "pip", "install", "boto3", "-q"], check=True)
import boto3, json, os
from botocore import UNSIGNED
from botocore.config import Config

study_ids_str = os.environ["CANCER_STUDY_IDS"]
s3_bucket     = os.environ["S3_BUCKET"]
study_ids     = [s.strip() for s in study_ids_str.split(",") if s.strip()]

if not study_ids:
    print("No study IDs provided", file=sys.stderr)
    sys.exit(1)

s3    = boto3.client("s3", config=Config(signature_version=UNSIGNED))
found = []
for study_id in study_ids:
    resp = s3.list_objects_v2(Bucket=s3_bucket, Prefix=f"{study_id}/", MaxKeys=1)
    if resp.get("KeyCount", 0) > 0:
        found.append(study_id)
        print(f"Study '{study_id}' found in s3://{s3_bucket}")
    else:
        print(f"Study '{study_id}' NOT found in s3://{s3_bucket} — skipping", file=sys.stderr)

os.makedirs("/airflow/xcom", exist_ok=True)
with open("/airflow/xcom/return.json", "w") as f:
    json.dump(found, f)

if not found:
    print("None of the requested studies exist in S3", file=sys.stderr)
    sys.exit(1)
"""

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}


@dag(
    dag_id="import_public_hackathon",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    params={
        "cancer_study_ids": Param(
            "",
            type="string",
            description="Comma-separated cancer study IDs to import (e.g. msk_impact_2017,brca_tcga)",
            title="Cancer Study IDs",
        ),
    },
)
def import_public_hackathon():
    @task
    def verify_cluster_state():
        pass

    @task
    def verify_import_not_in_progress():
        pass

    @task
    def set_import_running():
        pass

    @task
    def wipe_standby_database():
        pass

    @task
    def clone_live_database_into_standby():
        pass

    @task(executor_config=K8S_EXECUTOR_CONFIG)
    def pull_and_validate_study(study_id: str, s3_bucket: str) -> str | None:
        import subprocess, sys, pathlib
        subprocess.run([sys.executable, "-m", "pip", "install", "boto3", "-q"], check=True)
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        local_dir = f"/tmp/{study_id}"
        pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

        try:
            s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
            prefix = f"{study_id}/"
            paginator = s3.get_paginator("list_objects_v2")
            for page in paginator.paginate(Bucket=s3_bucket, Prefix=prefix):
                for obj in page.get("Contents", []):
                    key = obj["Key"]
                    rel_path = key[len(prefix):]
                    if not rel_path:
                        continue
                    dest = os.path.join(local_dir, rel_path)
                    os.makedirs(os.path.dirname(dest), exist_ok=True)
                    s3.download_file(s3_bucket, key, dest)
            logging.info("[STUB] Would run: %s %s %s", sys.executable, VALIDATE_SCRIPT_PATH, local_dir)
            return study_id
        except Exception as e:
            logging.error("Validation failed for %s: %s", study_id, e)
            return None

    @task(trigger_rule=TriggerRule.ALL_DONE)
    def collect_valid_studies(results: list) -> list[str]:
        valid = [sid for sid in (results or []) if sid is not None]
        if not valid:
            raise AirflowSkipException("No studies passed validation — skipping import")
        logging.info("Studies passing validation: %s", valid)
        return valid

    @task(executor_config=K8S_EXECUTOR_CONFIG)
    def import_into_standby_database(valid_studies: list[str]):
        if not valid_studies:
            logging.info("No valid studies to import — exiting.")
            return
        logging.info("Importing %d studies: %s", len(valid_studies), valid_studies)
        logging.info("[STUB] Would run: %s %s %s", sys.executable, IMPORT_SCRIPT_PATH, " ".join(valid_studies))

    @task
    def transfer_deployment_color():
        pass

    @task
    def set_import_complete():
        pass

    @task
    def send_slack_notifications():
        pass

    @task
    def cleanup_data():
        pass

    # Gate tasks run sequentially
    t_found_studies = KubernetesPodOperator(
        task_id="verify_studies_exist",
        namespace=K8S_NAMESPACE,
        image=K8S_IMAGE,
        name="verify-studies-exist",
        service_account_name=K8S_SERVICE_ACCOUNT,
        cmds=["python", "-c"],
        arguments=[_VERIFY_STUDIES_SCRIPT],
        env_vars={
            "CANCER_STUDY_IDS": "{{ params.cancer_study_ids }}",
            "S3_BUCKET": S3_BUCKET,
        },
        do_xcom_push=True,
        get_logs=True,
        is_delete_operator_pod=True,
    )
    t_verify_cluster_state           = verify_cluster_state()
    t_verify_import_not_in_progress  = verify_import_not_in_progress()
    t_set_import_running             = set_import_running()

    # DB branch
    t_wipe_standby_database  = wipe_standby_database()
    t_clone_live_database    = clone_live_database_into_standby()

    # Validation branch: data dep on found_studies, sequencing dep on set_import_running
    t_pull_and_validate = pull_and_validate_study.partial(s3_bucket=S3_BUCKET).expand(study_id=t_found_studies)
    t_collect_valid     = collect_valid_studies(t_pull_and_validate)

    # Diamond join: import waits for validated studies (data) and cloned DB (sequencing)
    t_import = import_into_standby_database(t_collect_valid)

    t_transfer_deployment_color = transfer_deployment_color()
    t_set_import_complete       = set_import_complete()
    t_send_slack_notifications  = send_slack_notifications()
    t_cleanup_data              = cleanup_data()

    # Sequential gate chain
    (
        t_found_studies
        >> t_verify_cluster_state
        >> t_verify_import_not_in_progress
        >> t_set_import_running
    )

    # Fork after gate: DB prep and validation run in parallel
    t_set_import_running >> [t_wipe_standby_database, t_pull_and_validate]
    t_wipe_standby_database >> t_clone_live_database

    # Diamond join: import waits for both branches
    t_clone_live_database >> t_import

    # Tail chain
    (
        t_import
        >> t_transfer_deployment_color
        >> t_set_import_complete
        >> [t_send_slack_notifications, t_cleanup_data]
    )


import_public_hackathon()
