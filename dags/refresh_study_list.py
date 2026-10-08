"""Hourly refresh of study sources from configured S3 buckets;
import_public_hackathon reads it at parse time for its study-picker dropdown."""
import json
import logging
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.models import Variable

from dags.study_sources import STUDY_LIST_VARIABLE_KEY, discover_studies

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
        sorted_ids = discover_studies(s3)
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
