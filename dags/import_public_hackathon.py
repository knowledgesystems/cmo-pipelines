"""
import_public_hackathon.py
"""
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.models.param import Param

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
        "study_name": Param(
            "my_study",
            type="string",
            description="Name of the study to import.",
            title="Study Name",
        ),
    },
)
def import_public_hackathon():
    @task
    def verify_studies_exist():
        # INPUT: list of strings for studies to import (from DAG params)
        # OUTPUT: list of strings for studies that exist in S3
        pass

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

    @task
    def validate_studies():
        # INPUT: list of strings for studies that exist in S3
        # OUTPUT: list of strings for studies that passed validation
        pass

    @task
    def import_into_standby_database():
        pass

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

    # Wire top-level dependencies
    t_verify_studies_exist = verify_studies_exist()
    t_verify_cluster_state = verify_cluster_state()
    t_verify_import_not_in_progress = verify_import_not_in_progress()
    t_set_import_running = set_import_running()
    t_wipe_standby_database = wipe_standby_database()
    t_clone_live_database_into_standby = clone_live_database_into_standby()
    t_validate_studies = validate_studies()
    t_import_into_standby_database = import_into_standby_database()
    t_transfer_deployment_color = transfer_deployment_color()
    t_set_import_complete = set_import_complete()
    t_send_slack_notifications = send_slack_notifications()
    t_cleanup_data = cleanup_data()

    (
        t_verify_studies_exist
        >> t_verify_cluster_state
        >> t_verify_import_not_in_progress
        >> t_set_import_running
        >> t_wipe_standby_database
        >> t_clone_live_database_into_standby
        >> t_validate_studies
        >> t_import_into_standby_database
        >> t_transfer_deployment_color
        >> t_set_import_complete
        >> [t_send_slack_notifications, t_cleanup_data]
    )


import_public_hackathon()
