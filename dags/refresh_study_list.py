"""Hourly refresh of the 'available_study_ids' Variable from the S3 bucket;
import_public_hackathon reads it at parse time for its study-picker dropdown."""
import json
import logging
from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.models import Variable

S3_BUCKET             = "sc-203403084713-pp-4rxlzd426npxu-bucket-kswubqqre3jr"
STUDY_LIST_VARIABLE_KEY = "available_study_ids"

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}


@dag(
    dag_id="refresh_study_list",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule="@hourly",
    catchup=False,
    tags=["hackathon", "maintenance"],
)
def refresh_study_list():

    @task
    def fetch_and_store_study_ids() -> list[str]:
        import boto3

        s3 = boto3.client("s3")
        study_ids: set[str] = set()

        paginator = s3.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=S3_BUCKET, Delimiter="/"):
            for prefix in page.get("CommonPrefixes", []):
                study_ids.add(prefix["Prefix"].rstrip("/"))
            for obj in page.get("Contents", []):
                key = obj["Key"]
                if key.endswith(".tar") or key.endswith(".tar.gz"):
                    study_ids.add(key[:-4] if key.endswith(".tar") else key[:-7])

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


refresh_study_list()
