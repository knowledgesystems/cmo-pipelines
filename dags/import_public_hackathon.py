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
from airflow.utils.trigger_rule import TriggerRule

S3_BUCKET            = "hackathon-databricks"
K8S_IMAGE            = "your-custom-image:latest"
VALIDATE_SCRIPT_PATH = "/scripts/validate_study.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer.py"

K8S_EXECUTOR_CONFIG = {"KubernetesExecutor": {"image": K8S_IMAGE}}

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
    @task(executor_config=K8S_EXECUTOR_CONFIG)
    def verify_studies_exist(study_ids_str: str) -> list[str]:
        import boto3
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
                logging.warning("Study '%s' NOT found in s3://%s — will be skipped", study_id, S3_BUCKET)

        if not found:
            raise AirflowSkipException("None of the requested studies exist in S3")

        return found

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
        import pathlib
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
    t_found_studies                  = verify_studies_exist("{{ params.cancer_study_ids }}")
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
