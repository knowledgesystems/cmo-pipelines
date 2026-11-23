"""
deliver_az_mskimpact_dag.py
Delivers the AstraZeneca MSK-IMPACT study exported to S3 by the Databricks bundle
data-partnerships-databricks-pipelines/astrazeneca-mskimpact-v1.

When it starts on a Sunday (ET, the legacy rule), deng's msk_impact_public_job runs the Databricks
job after it refreshes the MSK-IMPACT data product (no fixed time; a late start finishes on
Monday). That job builds the delivery tables and writes the cBioPortal study
to s3://az-mskimpact/delivery/<run date>/, marked _READY. This DAG adds the clinical attribute
metadata header rows (CDD), uploads the study to AstraZeneca's SFTP server, and marks the run
_DELIVERED. Each step runs airflow-deliver-az-mskimpact.sh on the pipelines node.

find_ready_run retries until Monday noon for an export from this DAG run's Sunday or later, and
fails (alerting) if none is _READY by then. It skips the run (and all later tasks) when that
export was already delivered.
"""
import os
import sys
from datetime import timedelta

import pendulum
from airflow import DAG
from airflow.providers.ssh.operators.ssh import SSHOperator

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import dag_failure_slack_webhook_notification, dag_success_slack_webhook_notification

# Node holding the AstraZeneca SFTP key and credentials (previously the update-az-mskimpact.sh host).
SSH_CONN_ID = "pipelines3_ssh"
SCRIPT = "/data/portal-cron/scripts/airflow-deliver-az-mskimpact.sh"
ALREADY_DELIVERED_EXIT_CODE = 99

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "on_failure_callback": [dag_failure_slack_webhook_notification],
}


def _step(task_id: str, step: str, env: str = "", **kwargs) -> SSHOperator:
    return SSHOperator(
        task_id=task_id,
        ssh_conn_id=SSH_CONN_ID,
        command=f"{env}{SCRIPT} {step}",
        cmd_timeout=None,
        **kwargs,
    )


with DAG(
    dag_id="deliver_az_mskimpact",
    default_args=_DEFAULT_ARGS,
    description="Adds clinical metadata headers to the exported AstraZeneca MSK-IMPACT study and delivers it over SFTP",
    # Sundays 09:00 ET: the public job usually reaches the AZ export around 08:00 ET.
    schedule="0 9 * * 0",
    start_date=pendulum.datetime(2026, 10, 1, tz="America/New_York"),
    catchup=False,
    max_active_runs=1,
    # Starts paused for the parallel run with the legacy delivery; unpause at cutover.
    is_paused_upon_creation=True,
    tags=["astrazeneca", "delivery"],
    on_success_callback=[dag_success_slack_webhook_notification],
) as dag:

    # Waits for the export: retries every 30 minutes for 27 hours (until Monday 12:00 ET), since a
    # public job that starts late on Sunday finishes on Monday. AZ_RUN_DATE pins the earliest
    # accepted run to this DAG run's Sunday, so retries after midnight still accept it.
    find_ready_run = _step(
        "find_ready_run",
        "find-run",
        env="AZ_RUN_DATE={{ data_interval_end.in_timezone('America/New_York').strftime('%Y-%m-%d') }} ",
        retries=54,
        retry_delay=timedelta(minutes=30),
        skip_on_exit_code=ALREADY_DELIVERED_EXIT_CODE,
    )
    fetch = _step("fetch", "fetch")
    add_headers = _step("add_clinical_metadata_headers", "add-headers")
    sftp = _step("sftp_upload", "sftp")
    mark_delivered = _step("mark_delivered", "mark-delivered")
    cleanup = _step("cleanup", "cleanup")

    find_ready_run >> fetch >> add_headers >> sftp >> mark_delivered >> cleanup
