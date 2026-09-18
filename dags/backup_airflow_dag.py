"""
backup_airflow_dag.py
Back up the Airflow metadata database and log files to S3 on a daily schedule.

Tasks
-----
backup_metadata_db
    Streams a gzipped pg_dump directly to
    ``s3://<s3_bucket>/<s3_prefix>/db/airflow_metadata_<timestamp>.sql.gz``.
backup_logs
    Syncs the Airflow log directory to
    ``s3://<s3_bucket>/<s3_prefix>/logs/`` using ``aws s3 sync``.
cleanup_old_backups
    Deletes DB dump objects in S3 whose keys contain a timestamp older than
    ``retention_days`` days.  Log retention is handled via an S3 Lifecycle
    rule on the bucket.

Configuration
-------------
All values below can be overridden at runtime via Airflow Params.

AWS credentials
---------------
The AWS CLI authenticates using whatever credential chain is active on the
Airflow server (instance profile, env vars, ~/.aws/credentials, etc.).

Database credentials
--------------------
The pg_dump command reads the database connection string from
``AIRFLOW__DATABASE__SQL_ALCHEMY_CONN``, which Airflow already exports in
the environment of every task.  No extra credential setup is needed.
"""
from __future__ import annotations

from datetime import timedelta

from airflow import DAG
from airflow.exceptions import AirflowException
from airflow.operators.bash import BashOperator
from airflow.providers.slack.notifications.slack_webhook import send_slack_webhook_notification
from airflow.models.param import Param
from airflow.decorators import task
from airflow.utils.dates import days_ago
from airflow.utils.trigger_rule import TriggerRule

# ---------------------------------------------------------------------------
# Slack notification templates (reuse project-wide style)
# ---------------------------------------------------------------------------

_FAIL_MSG = """
        :red_circle: DAG Failed.
        *DAG ID*: {{ dag.dag_id }}
        *Task ID*: {{ task_instance.task_id }}
        *Execution Time*: {{ execution_date }}
        *Log URL*: {{ task_instance.log_url }}
"""

_SUCCESS_MSG = """
        :large_green_circle: DAG Success!
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
"""

_on_failure = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=_FAIL_MSG
)
_on_success = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=_SUCCESS_MSG
)

# ---------------------------------------------------------------------------
# Default arguments
# ---------------------------------------------------------------------------

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=10),
    "on_failure_callback": [_on_failure],
}

# ---------------------------------------------------------------------------
# Shell commands
# S3 bucket, prefix, and retention period are injected via Jinja so they
# can be overridden at runtime through Airflow Params.
# ---------------------------------------------------------------------------

# Stream pg_dump directly to S3 — no local temp file needed.
# Parses AIRFLOW__DATABASE__SQL_ALCHEMY_CONN (exported by Airflow automatically).
# Supports both postgresql:// and postgresql+psycopg2:// schemes.
_BACKUP_DB_CMD = """\
set -euo pipefail

S3_BUCKET="{{ params.s3_bucket }}"
S3_PREFIX="{{ params.s3_prefix }}"
TIMESTAMP=$(date +%Y%m%dT%H%M%S)
S3_KEY="${S3_PREFIX}/db/airflow_metadata_${TIMESTAMP}.sql.gz"

# Parse the SQLAlchemy connection string to extract pg_dump arguments.
CONN="${AIRFLOW__DATABASE__SQL_ALCHEMY_CONN}"
CONN="${CONN#postgresql+psycopg2://}"
CONN="${CONN#postgresql://}"

PG_USER="${CONN%%:*}"
CONN="${CONN#*:}"
PG_PASSWORD="${CONN%%@*}"
CONN="${CONN#*@}"
PG_HOST_PORT="${CONN%%/*}"
PG_DB="${CONN##*/}"
PG_HOST="${PG_HOST_PORT%%:*}"
PG_PORT="${PG_HOST_PORT##*:}"
# Default port when host == port (i.e., no port in URL)
[ "${PG_PORT}" = "${PG_HOST}" ] && PG_PORT=5432

echo "Streaming pg_dump to s3://${S3_BUCKET}/${S3_KEY} ..."

PGPASSWORD="${PG_PASSWORD}" pg_dump \
    --host="${PG_HOST}" \
    --port="${PG_PORT}" \
    --username="${PG_USER}" \
    --format=plain \
    "${PG_DB}" \
    | gzip \
    | aws s3 cp - "s3://${S3_BUCKET}/${S3_KEY}"

echo "Done."
"""

# Sync the log directory to S3.  Using sync means only new/changed files are
# uploaded on each run, keeping subsequent runs fast.
_BACKUP_LOGS_CMD = """\
set -euo pipefail

S3_BUCKET="{{ params.s3_bucket }}"
S3_PREFIX="{{ params.s3_prefix }}"
LOG_DIR="{{ params.log_dir }}"
S3_DEST="s3://${S3_BUCKET}/${S3_PREFIX}/logs/"

echo "Syncing ${LOG_DIR} to ${S3_DEST} ..."

aws s3 sync "${LOG_DIR}" "${S3_DEST}" \
    --no-progress \
    --exclude "*.pyc"

echo "Done."
"""

# Delete DB dump objects in S3 whose embedded timestamp is older than
# retention_days.  Log retention is best handled via an S3 Lifecycle rule
# since iterating every log object would be very slow.
_CLEANUP_CMD = """\
set -euo pipefail

S3_BUCKET="{{ params.s3_bucket }}"
S3_PREFIX="{{ params.s3_prefix }}"
RETENTION_DAYS="{{ params.retention_days }}"

CUTOFF=$(date -d "-${RETENTION_DAYS} days" +%Y%m%d 2>/dev/null \
         || date -v "-${RETENTION_DAYS}d" +%Y%m%d)  # GNU/BSD date compat

echo "Removing DB dump objects older than ${RETENTION_DAYS} days (before ${CUTOFF}) ..."

aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}/db/" \
    | awk '{print $4}' \
    | grep -E '^airflow_metadata_[0-9]{8}T' \
    | while read -r KEY; do
        FILE_DATE="${KEY#airflow_metadata_}"  # strip prefix  → 20260401T120000.sql.gz
        FILE_DATE="${FILE_DATE%%T*}"           # keep date part → 20260401
        if [ "${FILE_DATE}" -lt "${CUTOFF}" ]; then
            FULL_KEY="${S3_PREFIX}/db/${KEY}"
            echo "  Deleting s3://${S3_BUCKET}/${FULL_KEY}"
            aws s3 rm "s3://${S3_BUCKET}/${FULL_KEY}"
        fi
    done

echo "Cleanup complete."
"""

# ---------------------------------------------------------------------------
# DAG definition
# ---------------------------------------------------------------------------

with DAG(
    dag_id="backup_airflow_dag",
    description=(
        "Back up the Airflow metadata database (pg_dump) and log directory "
        "to an S3 bucket on a daily schedule, then prune DB dump objects "
        "older than the configured retention period. "
        "Log retention is best handled via an S3 Lifecycle rule on the bucket."
    ),
    default_args=_DEFAULT_ARGS,
    start_date=days_ago(1),
    schedule_interval="0 2 * * *",  # 02:00 UTC every day
    max_active_runs=1,
    catchup=False,
    tags=["backup", "airflow"],
    on_success_callback=[_on_success],
    params={
        "s3_bucket": Param(
            "",
            type="string",
            title="S3 bucket name",
            description="Name of the S3 bucket where backups are written (no s3:// prefix).",
        ),
        "s3_prefix": Param(
            "airflow-backups",
            type="string",
            title="S3 key prefix",
            description=(
                "Key prefix inside the bucket.  DB dumps go under "
                "<prefix>/db/ and logs under <prefix>/logs/."
            ),
        ),
        "log_dir": Param(
            "/opt/airflow/logs",
            type="string",
            title="Airflow log directory",
            description="Local path to the Airflow log directory that will be synced to S3.",
        ),
        "retention_days": Param(
            30,
            type="integer",
            title="DB dump retention period (days)",
            description=(
                "DB dump objects older than this many days are deleted. "
                "For log retention, configure an S3 Lifecycle rule on the bucket."
            ),
        ),
    },
) as dag:

    backup_metadata_db = BashOperator(
        task_id="backup_metadata_db",
        bash_command=_BACKUP_DB_CMD,
        execution_timeout=timedelta(minutes=30),  # ceiling for large DBs
    )

    backup_logs = BashOperator(
        task_id="backup_logs",
        bash_command=_BACKUP_LOGS_CMD,
        execution_timeout=timedelta(hours=1),  # ceiling for large log trees
    )

    cleanup_old_backups = BashOperator(
        task_id="cleanup_old_backups",
        bash_command=_CLEANUP_CMD,
        execution_timeout=timedelta(minutes=5),
        trigger_rule=TriggerRule.ALL_DONE,  # run even if a backup task failed
    )

    @task(trigger_rule=TriggerRule.ONE_FAILED, retries=0, on_failure_callback=None)
    def watcher() -> None:
        raise AirflowException("Failing task because one or more upstream tasks failed.")

    # Dependency graph:
    # backup_metadata_db ──┐
    #                       ├──> cleanup_old_backups ──> watcher
    # backup_logs       ──┘
    [backup_metadata_db, backup_logs] >> cleanup_old_backups
    [backup_metadata_db, backup_logs, cleanup_old_backups] >> watcher()
