"""
import_public_hackathon.py
"""
import json
import logging
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.exceptions import AirflowSkipException
from airflow.models import Variable
from airflow.models.param import Param
from airflow.operators.bash import BashOperator
from airflow.utils.trigger_rule import TriggerRule
from kubernetes.client import models as k8s

S3_BUCKET            = "hackathon-databricks"
K8S_IMAGE            = "apache/airflow:2.10.5"
K8S_IMAGE_VALIDATE   = "averyniceday/hackathon-import:latest"
VALIDATE_SCRIPT_PATH = "/scripts/importer/validateStudies.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer/metaImport.py"
STUDY_LIST_VARIABLE_KEY = "hackathon_available_study_ids"

def _available_study_ids() -> list[str]:
    """Read the study-list Variable at parse time to populate the Param enum."""
    try:
        return json.loads(Variable.get(STUDY_LIST_VARIABLE_KEY, default_var="[]"))
    except Exception:
        return []


def _download_study_from_s3(s3_client, s3_bucket: str, study_id: str, local_dir: str) -> None:
    import pathlib
    import tarfile

    tar_key = f"{study_id}.tar"
    tar_resp = s3_client.list_objects_v2(Bucket=s3_bucket, Prefix=tar_key, MaxKeys=1)
    if tar_resp.get("KeyCount", 0) > 0:
        tar_path = f"/tmp/{study_id}.tar"
        s3_client.download_file(s3_bucket, tar_key, tar_path)
        with tarfile.open(tar_path) as tf:
            tf.extractall(local_dir)
        entries = list(pathlib.Path(local_dir).iterdir())
        if len(entries) == 1 and entries[0].is_dir():
            for child in entries[0].iterdir():
                child.rename(pathlib.Path(local_dir) / child.name)
            entries[0].rmdir()
    else:
        prefix = f"{study_id}/"
        paginator = s3_client.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=s3_bucket, Prefix=prefix):
            for obj in page.get("Contents", []):
                key = obj["Key"]
                rel_path = key[len(prefix):]
                if not rel_path:
                    continue
                dest = os.path.join(local_dir, rel_path)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                s3_client.download_file(s3_bucket, key, dest)


_POD_OVERRIDE = {
    "pod_override": k8s.V1Pod(
        spec=k8s.V1PodSpec(
            containers=[k8s.V1Container(
                name="base",
                image=K8S_IMAGE,
            )]
        )
    )
}

_POD_OVERRIDE_VALIDATE = {
    "pod_override": k8s.V1Pod(
        spec=k8s.V1PodSpec(
            containers=[k8s.V1Container(
                name="base",
                image=K8S_IMAGE_VALIDATE,
                image_pull_policy="Always",
                resources=k8s.V1ResourceRequirements(
                    requests={"memory": "6Gi", "cpu": "1"},
                    limits={"memory": "7Gi"},
                ),
                env=[
                    k8s.V1EnvVar(name="PORTAL_HOME", value="/"),
                    k8s.V1EnvVar(name="JAVA_OPTS", value="-Xmx4g"),
                    k8s.V1EnvVar(
                        name="CLICKHOUSE_HOST",
                        value_from=k8s.V1EnvVarSource(
                            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="host")
                        ),
                    ),
                    k8s.V1EnvVar(
                        name="CLICKHOUSE_NATIVE_PORT",
                        value_from=k8s.V1EnvVarSource(
                            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="native_port")
                        ),
                    ),
                    k8s.V1EnvVar(
                        name="CLICKHOUSE_USER",
                        value_from=k8s.V1EnvVarSource(
                            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="user")
                        ),
                    ),
                    k8s.V1EnvVar(
                        name="CLICKHOUSE_PASSWORD",
                        value_from=k8s.V1EnvVarSource(
                            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="password")
                        ),
                    ),
                    k8s.V1EnvVar(
                        name="CLICKHOUSE_DB",
                        value_from=k8s.V1EnvVarSource(
                            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="database")
                        ),
                    ),
                ],
                volume_mounts=[
                    k8s.V1VolumeMount(
                        name="app-properties",
                        mount_path="/application.properties",
                        sub_path="application.properties",
                        read_only=True,
                    ),
                    k8s.V1VolumeMount(
                        name="clickhouse-sql",
                        mount_path="/clickhouse.sql",
                        sub_path="clickhouse.sql",
                        read_only=True,
                    ),
                ],
            )],
            volumes=[
                k8s.V1Volume(
                    name="app-properties",
                    secret=k8s.V1SecretVolumeSource(secret_name="hackathon-app-properties"),
                ),
                k8s.V1Volume(
                    name="clickhouse-sql",
                    secret=k8s.V1SecretVolumeSource(secret_name="hackathon-clickhouse-sql"),
                ),
            ],
        )
    )
}

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
            [],
            type="array",
            examples=_available_study_ids(),
            description="Select one or more cancer study IDs to import. Run refresh_hackathon_study_list to update the list.",
            title="Cancer Study IDs",
        ),
    },
)
def import_public_hackathon():
    # ── 1 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    def verify_studies_exist(study_ids: list[str]) -> list[str]:
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        # Params with type="array" should arrive as a list, but render_template_as_native_obj
        # may render them as a string repr (e.g. "['study1', 'study2']"). Parse defensively.
        if isinstance(study_ids, str):
            import ast
            import json
            try:
                study_ids = json.loads(study_ids)
            except (json.JSONDecodeError, ValueError):
                study_ids = ast.literal_eval(study_ids)

        if not study_ids:
            raise AirflowSkipException("No study IDs provided")

        s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
        found = []
        for study_id in study_ids:
            tar_resp = s3.list_objects_v2(Bucket=S3_BUCKET, Prefix=f"{study_id}.tar", MaxKeys=1)
            dir_resp = s3.list_objects_v2(Bucket=S3_BUCKET, Prefix=f"{study_id}/", MaxKeys=1)
            if tar_resp.get("KeyCount", 0) > 0 or dir_resp.get("KeyCount", 0) > 0:
                found.append(study_id)
                logging.info("Study '%s' found in s3://%s", study_id, S3_BUCKET)
            else:
                logging.warning("Study '%s' NOT found in s3://%s — will be skipped", study_id, S3_BUCKET)

        if not found:
            raise AirflowSkipException("None of the requested studies exist in S3")

        return found

    # ── 2 ──────────────────────────────────────────────────────────────
    def verify_cluster_state():
        return BashOperator(
            task_id="verify_cluster_state",
            bash_command='echo "verify_cluster_state"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 3 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    def verify_import_not_in_progress():
        pass

    # ── 4 ──────────────────────────────────────────────────────────────
    def set_import_running():
        return BashOperator(
            task_id="set_import_running",
            bash_command='echo "set_import_running"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 5 ──────────────────────────────────────────────────────────────
    def wipe_standby_database():
        return BashOperator(
            task_id="wipe_standby_database",
            bash_command='echo "wipe_standby_database"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 6 ──────────────────────────────────────────────────────────────
    def clone_live_database_into_standby():
        return BashOperator(
            task_id="clone_live_database_into_standby",
            bash_command='echo "clone_live_database_into_standby"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 7 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE_VALIDATE)
    def pull_and_validate_study(study_id: str, s3_bucket: str) -> str | None:
        import pathlib
        import subprocess
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        local_dir = f"/tmp/{study_id}"
        pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

        try:
            s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
            _download_study_from_s3(s3, s3_bucket, study_id, local_dir)
            log_dir = f"/tmp/validate_logs/{study_id}"
            os.makedirs(log_dir, exist_ok=True)
            result = subprocess.run(
                [sys.executable, VALIDATE_SCRIPT_PATH, "-l", local_dir, "-n", "-html", log_dir],
                capture_output=True,
                text=True,
            )
            logging.info(result.stdout)
            for log_file in pathlib.Path(log_dir).glob("log-validate-studies-*.txt"):
                logging.info("=== Validation log: %s ===\n%s", log_file.name, log_file.read_text())
            if result.returncode not in (0, 3):
                logging.error("Validation failed for %s (exit %d):\n%s", study_id, result.returncode, result.stderr)
                return None
            return study_id
        except Exception as e:
            logging.error("Validation failed for %s: %s", study_id, e)
            return None

    # ── 8 ──────────────────────────────────────────────────────────────
    @task(trigger_rule=TriggerRule.ALL_DONE, executor_config=_POD_OVERRIDE)
    def collect_valid_studies(results: list) -> list[str]:
        valid = [sid for sid in (results or []) if sid is not None]
        if not valid:
            raise AirflowSkipException("No studies passed validation — skipping import")
        logging.info("Studies passing validation: %s", valid)
        return valid

    # ── 9 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE_VALIDATE)
    def import_into_standby_database(valid_studies: list[str]):
        import pathlib
        import subprocess
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        if not valid_studies:
            logging.info("No valid studies to import — exiting.")
            return

        s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
        failed = []

        for study_id in valid_studies:
            local_dir = f"/tmp/{study_id}"
            pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

            try:
                _download_study_from_s3(s3, S3_BUCKET, study_id, local_dir)
            except Exception as e:
                logging.error("Download failed for %s: %s", study_id, e)
                failed.append(study_id)
                continue

            result = subprocess.run(
                [sys.executable, IMPORT_SCRIPT_PATH,
                 "-s", local_dir,
                 "-n",
                 "-o",
                 "--no-derive-tables"],
                capture_output=True, text=True,
            )
            logging.info(result.stdout)
            if result.stderr:
                logging.info(result.stderr)
            if result.returncode != 0:
                logging.error("Import failed for %s (exit %d)", study_id, result.returncode)
                failed.append(study_id)

        # Rebuild derived tables once after all studies are loaded
        rebuild = subprocess.run(
            [sys.executable, IMPORT_SCRIPT_PATH, "derive-tables"],
            capture_output=True, text=True,
        )
        logging.info(rebuild.stdout)
        if rebuild.stderr:
            logging.info(rebuild.stderr)
        if rebuild.returncode != 0:
            raise Exception(f"Derived table rebuild failed (exit {rebuild.returncode}):\n{rebuild.stderr}")

        if failed:
            raise Exception(f"Import failed for {len(failed)} study/studies: {failed}")

    # ── 10 ─────────────────────────────────────────────────────────────
    def transfer_deployment_color():
        return BashOperator(
            task_id="transfer_deployment_color",
            bash_command='echo "transfer_deployment_color"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 11 ─────────────────────────────────────────────────────────────
    def set_import_complete():
        return BashOperator(
            task_id="set_import_complete",
            bash_command='echo "set_import_complete"',
            executor_config=_POD_OVERRIDE,
        )

    # ── 12 ─────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    def send_slack_notifications():
        pass

    # ── Instantiate tasks in execution order ─────────────────────────
    t_found_studies                  = verify_studies_exist("{{ params.cancer_study_ids }}")
    t_verify_cluster_state           = verify_cluster_state()
    t_verify_import_not_in_progress  = verify_import_not_in_progress()
    t_set_import_running             = set_import_running()
    t_wipe_standby_database          = wipe_standby_database()
    t_clone_live_database            = clone_live_database_into_standby()
    t_pull_and_validate              = pull_and_validate_study.partial(s3_bucket=S3_BUCKET).expand(study_id=t_found_studies)
    t_collect_valid                  = collect_valid_studies(t_pull_and_validate)
    t_import                         = import_into_standby_database(t_collect_valid)
    t_transfer_deployment_color      = transfer_deployment_color()
    t_set_import_complete            = set_import_complete()
    t_send_slack_notifications       = send_slack_notifications()

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
        >> t_send_slack_notifications
    )


import_public_hackathon()
