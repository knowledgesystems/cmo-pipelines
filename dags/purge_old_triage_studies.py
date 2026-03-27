"""
purge_old_triage_studies.py
Finds triage studies older than 30 days in the ClickHouse database and marks
them for removal in the portal_importer_configuration Google Spreadsheet.

Required Airflow Variables:
  portal_importer_configuration_spreadsheet_id - Google Spreadsheet ID
  google_service_account_key_path             - Path to service account JSON key file on Airflow worker
  triage_cancer_studies_worksheet             - Worksheet name (default: "cancer_studies")
  triage_stable_id_column                     - Stable ID column header (default: "stableid")
  triage_remove_column                        - Column to mark with 'r' (default: "remove")

ClickHouse connection parameters are derived at runtime by:
  1. SSHing to pipelines3 and running get_database_currently_in_production.sh with the
     manage_triage_clickhouse_database_update_tools.properties file to learn the active color.
  2. Reading the same properties file over SSH to extract the color-specific ClickHouse
     connection parameters (host, port, credentials, database name).
"""
from __future__ import annotations

import shlex
from datetime import timedelta
import logging

from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException
from airflow.models import Variable
from airflow.providers.slack.notifications.slack_webhook import send_slack_webhook_notification
from airflow.providers.ssh.hooks.ssh import SSHHook
from airflow.utils.dates import days_ago
from airflow.utils.trigger_rule import TriggerRule

logger = logging.getLogger(__name__)

STUDY_EXPIRY_DAYS = 30
SCRIPTS_DIR = "/data/portal-cron/scripts"
CREDS_DIR = "/data/portal-cron/pipelines-credentials"
PROPERTIES_FILENAME = "manage_triage_clickhouse_database_update_tools.properties"
PROPERTIES_FILEPATH = f"{CREDS_DIR}/{PROPERTIES_FILENAME}"
SSH_CONN_ID = "pipelines3_ssh"

_fail_slack_msg = """
        :red_circle: DAG Failed.
        *DAG ID*: {{ dag.dag_id }}
        *Task ID*: {{ task_instance.task_id }}
        *Execution Time*: {{ execution_date }}
        *Log Url*: {{ task_instance.log_url }}
"""
_success_slack_msg = """
        :large_green_circle: DAG Success!
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
"""

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "on_failure_callback": [send_slack_webhook_notification(
        slack_webhook_conn_id="slack_default", text=_fail_slack_msg
    )],
}


def _col_letter(n: int) -> str:
    """Convert 1-indexed column number to spreadsheet column letter (1→A, 27→AA, etc.)."""
    result = ""
    while n > 0:
        n, remainder = divmod(n - 1, 26)
        result = chr(65 + remainder) + result
    return result


def _ssh_exec(hook: SSHHook, command: str) -> str:
    """Run a command on the remote host and return stdout as a string. Raises on non-zero exit."""
    client = hook.get_conn()
    exit_status, stdout_bytes, stderr_bytes = hook.exec_ssh_client_command(
        client, command, get_pty=False, environment=None
    )
    if exit_status != 0:
        stderr = stderr_bytes.decode("utf-8", errors="replace").strip()
        raise AirflowException(
            f"Remote command exited with status {exit_status}: {command!r}\nstderr: {stderr}"
        )
    return stdout_bytes.decode("utf-8", errors="replace")


def _parse_properties(content: str) -> dict[str, str]:
    """Parse a Java-style key=value properties file, ignoring blank lines and comments."""
    props: dict[str, str] = {}
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" in stripped:
            key, _, value = stripped.partition("=")
            props[key.strip()] = value.strip()
    return props


with DAG(
    dag_id="purge_old_triage_studies",
    default_args=_DEFAULT_ARGS,
    description=(
        f"Finds triage studies older than {STUDY_EXPIRY_DAYS} days in ClickHouse "
        "and marks them for removal in the portal_importer_configuration spreadsheet"
    ),
    max_active_runs=1,
    start_date=days_ago(2),
    schedule_interval="0 0 * * *",
    tags=["triage", "triage-clickhouse", "purge"],
    on_success_callback=[send_slack_webhook_notification(
        slack_webhook_conn_id="slack_default", text=_success_slack_msg
    )],
) as dag:

    @task
    def get_production_db_color() -> str:
        """
        SSHs to pipelines3 and runs get_database_currently_in_production.sh against the
        triage ClickHouse properties file to determine whether 'blue' or 'green' is
        currently serving production traffic.
        """
        hook = SSHHook(ssh_conn_id=SSH_CONN_ID)
        script = f"{SCRIPTS_DIR}/get_database_currently_in_production.sh"
        output = _ssh_exec(hook, f"{shlex.quote(script)} {shlex.quote(PROPERTIES_FILEPATH)}")

        # Script emits "<color> : current production database"
        first_word = output.strip().split()[0].lower()
        if first_word not in ("blue", "green"):
            raise AirflowException(
                f"Unexpected output from get_database_currently_in_production.sh: {output!r}"
            )
        logger.info("Production ClickHouse database color: %s", first_word)
        return first_word

    @task
    def get_clickhouse_connection_params(color: str) -> dict:
        """
        Reads manage_triage_clickhouse_database_update_tools.properties from pipelines3
        and returns the ClickHouse connection parameters for the given color.

        Property keys used (from cbioportal-core clickhouse_client_command_line_functions.sh):
          clickhouse_server_host_name
          clickhouse_server_port
          clickhouse_server_username
          clickhouse_server_password
          clickhouse_blue_database_name  (when color == 'blue')
          clickhouse_green_database_name (when color == 'green')
        """
        hook = SSHHook(ssh_conn_id=SSH_CONN_ID)
        content = _ssh_exec(hook, f"cat {shlex.quote(PROPERTIES_FILEPATH)}")
        props = _parse_properties(content)

        required_keys = [
            "clickhouse_server_host_name",
            "clickhouse_server_port",
            "clickhouse_server_username",
            "clickhouse_server_password",
            f"clickhouse_{color}_database_name",
        ]
        missing = [k for k in required_keys if k not in props]
        if missing:
            raise AirflowException(
                f"Missing required keys in {PROPERTIES_FILENAME}: {missing}"
            )

        return {
            "host": props["clickhouse_server_host_name"],
            "port": int(props["clickhouse_server_port"]),
            "username": props["clickhouse_server_username"],
            "password": props["clickhouse_server_password"],
            "database": props[f"clickhouse_{color}_database_name"],
        }

    @task
    def get_old_triage_studies(conn_params: dict) -> list[str]:
        """
        Connects to the production ClickHouse database and returns the stable IDs of
        all cancer studies whose import_date is older than STUDY_EXPIRY_DAYS days.
        """
        import clickhouse_connect

        client = clickhouse_connect.get_client(
            host=conn_params["host"],
            port=conn_params["port"],
            username=conn_params["username"],
            password=conn_params["password"],
            database=conn_params["database"],
            secure=True,
        )

        result = client.query(
            f"""
            SELECT cancer_study_identifier
            FROM cancer_study
            WHERE import_date < now() - INTERVAL {STUDY_EXPIRY_DAYS} DAY
            """
        )

        study_ids = [row[0] for row in result.result_rows]
        logger.info(
            "Found %d studies older than %d days: %s",
            len(study_ids), STUDY_EXPIRY_DAYS, study_ids,
        )
        return study_ids

    @task
    def mark_studies_for_removal(study_ids: list[str]) -> None:
        """
        For each study in study_ids, finds the corresponding row in the
        portal_importer_configuration Google Spreadsheet and writes 'r' into
        the removal-action column.
        """
        if not study_ids:
            logger.info("No expired studies found; nothing to mark.")
            return

        import re
        from google.oauth2 import service_account
        from googleapiclient.discovery import build

        service_account_key_path = Variable.get("google_service_account_key_path")
        spreadsheet_id = Variable.get("portal_importer_configuration_spreadsheet_id")
        worksheet_name = "cancer_studies"
        stable_id_col = "STABLE_ID"
        remove_col = "triage-portal"

        credentials = service_account.Credentials.from_service_account_file(
            service_account_key_path,
            scopes=["https://www.googleapis.com/auth/spreadsheets"],
        )
        sheets = build("sheets", "v4", credentials=credentials).spreadsheets()

        response = sheets.values().get(
            spreadsheetId=spreadsheet_id,
            range=worksheet_name,
        ).execute()
        rows = response.get("values", [])

        if not rows:
            raise AirflowException(
                f"Worksheet '{worksheet_name}' is empty or could not be read."
            )

        # Normalise header names the same way py3importUsers.py does
        header = [
            re.sub(r"[^0-9a-zA-Z]+", "", col.strip().lower())
            for col in rows[0]
        ]

        if stable_id_col not in header:
            raise AirflowException(
                f"Stable ID column '{stable_id_col}' not found in worksheet header: {header}"
            )
        if remove_col not in header:
            raise AirflowException(
                f"Remove column '{remove_col}' not found in worksheet header: {header}"
            )

        stable_id_idx = header.index(stable_id_col)
        remove_col_idx = header.index(remove_col)
        remove_col_letter = _col_letter(remove_col_idx + 1)

        study_ids_set = set(study_ids)
        updates = []

        for row_num, row in enumerate(rows[1:], start=2):  # 1-indexed; skip header
            if stable_id_idx >= len(row):
                continue
            stable_id = row[stable_id_idx].strip()
            if stable_id in study_ids_set:
                cell = f"{worksheet_name}!{remove_col_letter}{row_num}"
                updates.append({"range": cell, "values": [["r"]]})
                logger.info("Queuing 'r' for study '%s' at cell %s", stable_id, cell)

        if not updates:
            logger.info(
                "None of the %d expired studies were found in the spreadsheet.", len(study_ids)
            )
            return

        sheets.values().batchUpdate(
            spreadsheetId=spreadsheet_id,
            body={"valueInputOption": "RAW", "data": updates},
        ).execute()

        logger.info("Marked %d studies for removal in the spreadsheet.", len(updates))

    @task(trigger_rule=TriggerRule.ONE_FAILED, retries=0, on_failure_callback=None)
    def watcher():
        raise AirflowException("Failing task because one or more upstream tasks failed.")

    color = get_production_db_color()
    conn_params = get_clickhouse_connection_params(color)
    old_studies = get_old_triage_studies(conn_params)
    mark_task = mark_studies_for_removal(old_studies)
    [color, conn_params, old_studies, mark_task] >> watcher()
