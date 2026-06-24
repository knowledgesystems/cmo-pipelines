"""
refresh_hackathon_study_list.py

Runs hourly to refresh the Airflow Variable 'hackathon_available_study_ids'
with the list of study IDs currently present in the S3 bucket.  The main
import_public_hackathon DAG reads this Variable at parse time to populate
the cancer_study_ids Param enum (multiselect dropdown in the trigger UI).
"""
import json
import logging
from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.models import Variable

S3_BUCKET             = "hackathon-databricks"
STUDY_LIST_VARIABLE_KEY = "hackathon_available_study_ids"

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}


@dag(
    dag_id="refresh_hackathon_study_list",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule="@hourly",
    catchup=False,
    tags=["hackathon", "maintenance"],
)
def refresh_hackathon_study_list():

    @task
    def fetch_and_store_study_ids() -> list[str]:
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        s3 = boto3.client("s3", config=Config(signature_version=UNSIGNED))
        study_ids: set[str] = set()

        paginator = s3.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=S3_BUCKET, Delimiter="/"):
            # top-level directories → study folders
            for prefix in page.get("CommonPrefixes", []):
                study_ids.add(prefix["Prefix"].rstrip("/"))
            # top-level .tar files → study archives
            for obj in page.get("Contents", []):
                key = obj["Key"]
                if key.endswith(".tar"):
                    study_ids.add(key[: -len(".tar")])

        sorted_ids = sorted(study_ids)
        Variable.set(STUDY_LIST_VARIABLE_KEY, json.dumps(sorted_ids))
        logging.info(
            "Stored %d study IDs to Variable '%s': %s",
            len(sorted_ids),
            STUDY_LIST_VARIABLE_KEY,
            sorted_ids,
        )
        return sorted_ids

    fetch_and_store_study_ids()


refresh_hackathon_study_list()
