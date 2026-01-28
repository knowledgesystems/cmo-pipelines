"""Shared builder for ClickHouse import DAGs."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Callable, Mapping, Optional, Sequence
import logging
import shlex

from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException
from airflow.models.param import Param
from airflow.providers.ssh.operators.ssh import SSHOperator
from airflow.providers.ssh.hooks.ssh import SSHHook
from airflow.utils.trigger_rule import TriggerRule
from airflow.operators.python import get_current_context
from airflow.providers.slack.notifications.slack_webhook import send_slack_webhook_notification
from airflow.providers.slack.hooks.slack_webhook import SlackWebhookHook
from jinja2 import Template

fail_slack_msg = """
        :red_circle: DAG Failed.
        *DAG ID*: {{ dag.dag_id }}
        *Task ID*: {{ task_instance.task_id }}
        *Execution Time*: {{ execution_date }}
        *Log Url*: {{ task_instance.log_url }}
"""
success_slack_msg = """
        :large_green_circle: DAG Success!
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
"""
import_status_slack_msg = """
        :bell: Importer Status Report
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
        *Message*:
        {{ message_text }}
"""
dag_failure_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=fail_slack_msg
)
dag_success_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=success_slack_msg
)

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "on_failure_callback": [dag_failure_slack_webhook_notification],
}

WireDependencies = Callable[[dict[str, object]], None]
logger = logging.getLogger(__name__)


@dataclass(frozen=True, kw_only=True)
class ImporterConfig:
    dag_id: str
    description: str
    importer: str
    tags: Sequence[str]
    target_nodes: Sequence[str]
    data_nodes: Sequence[str]
    task_names: Sequence[str]
    scripts_dir: str = "/data/portal-cron/scripts"
    creds_dir: str = "/data/portal-cron/pipelines-credentials"
    db_properties_filename: str
    color_swap_config_filename: str
    data_source_properties_filename: str = "importer-data-source-manager-config.yaml"
    params: Mapping[str, Param]
    wire_dependencies: WireDependencies
    pool: Optional[str] = None
    schedule_interval: Optional[str] = None


def _script(scripts_dir: str, script_name: str, *args: object) -> str:
    parts = [f"{scripts_dir}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    return " ".join(parts)


def build_import_dag(config: ImporterConfig) -> DAG:
    params = dict(config.params) if config.params else {}

    dag = DAG(
        dag_id=config.dag_id,
        default_args=_DEFAULT_ARGS,
        description=config.description,
        max_active_runs=1,
        start_date=datetime(2024, 12, 3),
        schedule_interval=config.schedule_interval,
        tags=list(config.tags),
        render_template_as_native_obj=True,
        on_success_callback=[dag_success_slack_webhook_notification],
        params=params,
    )

    with dag:
        importer = config.importer
        scripts_dir = config.scripts_dir
        creds_dir = config.creds_dir
        db_properties_filepath = f"{creds_dir}/{config.db_properties_filename}"
        color_swap_config_filepath = f"{creds_dir}/{config.color_swap_config_filename}"
        data_source_properties_filepath = f"{creds_dir}/{config.data_source_properties_filename}"
        if len(config.target_nodes) != 1:
            raise ValueError(
                f"Expected exactly one target node for importer '{importer}', got {len(config.target_nodes)}."
            )

        @task
        def get_data_repos(repos: list[str]) -> str:
            return " ".join(repos)

        @task
        def fetch_notification_text(ssh_conn_id: str, notification_file: str) -> str:
            if not notification_file:
                logger.warning("Notification filename is empty; nothing to fetch.")
                return ""
            command = f"cat {shlex.quote(notification_file)}"
            hook = SSHHook(ssh_conn_id=ssh_conn_id)
            client = hook.get_conn()
            stdin, stdout, stderr = client.exec_command(command)
            exit_status = stdout.channel.recv_exit_status()
            output = stdout.read().decode("utf-8", errors="replace")
            err_output = stderr.read().decode("utf-8", errors="replace")
            if exit_status != 0:
                logger.warning(
                    "Notification file fetch failed on %s (exit %s): %s",
                    ssh_conn_id,
                    exit_status,
                    err_output.strip(),
                )
                return ""
            return output.strip()

        @task
        def extract_notification_filename(import_sql_output: object) -> str:
            if isinstance(import_sql_output, list):
                raw_text = next((text for text in import_sql_output if text), "")
            else:
                raw_text = import_sql_output or ""
            text = str(raw_text)
            for line in reversed(text.splitlines()):
                if line.startswith("NOTIFICATION_FILE="):
                    return line.split("=", 1)[1].strip()
            logger.warning("Notification filename not found in import_sql output.")
            return ""

        @task
        def send_update_notification(notification_texts: list[str]) -> None:
            message_text = next((text for text in notification_texts if text and text.strip()), "")
            if not message_text:
                logger.warning("Notification file is missing or empty; Slack message not sent.")
                return
            context = get_current_context()
            rendered_message = Template(import_status_slack_msg).render(
                message_text=message_text,
                **context,
            )
            SlackWebhookHook(slack_webhook_conn_id="slack_default").send(text=rendered_message)

        data_repos = get_data_repos("{{ params.get('data_repos', []) }}")

        command_map = {
            "verify_management_state": _script(
                scripts_dir,
                "airflow-verify-management.sh",
                scripts_dir,
                db_properties_filepath,
                color_swap_config_filepath,
            ),
            "scale_up_rds_node": _script(
                scripts_dir,
                "scale-rds.sh",
                "up",
                importer,
                color_swap_config_filepath,
            ),
            "clone_database": _script(
                scripts_dir,
                "airflow-clone-db.sh",
                importer,
                scripts_dir,
                db_properties_filepath,
            ),
            "fetch_data": _script(
                scripts_dir,
                "data_source_repo_clone_manager.sh",
                data_source_properties_filepath,
                "pull",
                importer,
                data_repos,
            ),
            "setup_import": _script(
                scripts_dir,
                "airflow-setup-import.sh",
                importer,
                scripts_dir,
                db_properties_filepath,
            ),
            "import_sql": _script(
                scripts_dir,
                "airflow-import-sql.sh",
                importer,
                scripts_dir,
                db_properties_filepath,
            ),
            "import_clickhouse": _script(
                scripts_dir,
                "airflow-import-clickhouse.sh",
                importer,
                scripts_dir,
                db_properties_filepath,
            ),
            "scale_down_rds_node": _script(
                scripts_dir,
                "scale-rds.sh",
                "down",
                importer,
                color_swap_config_filepath,
                # Normally, we would verify that we are in a "scaled up" state before trying to scale down.
                # However, if the DAG run failed before "scale_up_rds_node" completed successfully,
                # we may still be in a "scaled down" state when we run the scale down task
                # (which runs regardless of upstream failures).
                # In those cases -- skip verifying that we're in a scaled down state
                "{{ '' if (dag_run.get_task_instance('scale_up_rds_node', map_index=ti.map_index) and dag_run.get_task_instance('scale_up_rds_node', map_index=ti.map_index).state == 'success') else '--skip-pre-validation' }}",
            ),
            "transfer_deployment": _script(
                scripts_dir,
                "airflow-transfer-deployment.sh",
                scripts_dir,
                db_properties_filepath,
                color_swap_config_filepath,
            ),
            "clear_persistence_caches": _script(
                scripts_dir,
                "airflow-clear-persistence-caches.sh",
                importer,
                scripts_dir,
            ),
            "set_import_running": _script(
                scripts_dir,
                "set_update_process_state.sh",
                db_properties_filepath,
                "running",
            ),
            "set_import_abandoned": _script(
                scripts_dir,
                "set_update_process_state.sh",
                db_properties_filepath,
                "abandoned",
            ),
            "cleanup_data": _script(
                scripts_dir,
                "data_source_repo_clone_manager.sh",
                data_source_properties_filepath,
                "cleanup",
                importer,
                data_repos,
            ),
        }

        def _build_task(name: str) -> object:
            if name not in command_map:
                raise ValueError(f"Unsupported task '{name}' for importer '{importer}'.")

            params: dict[str, object] = {
                "task_id": name,
                "command": command_map[name],
            }

            if name == "set_import_abandoned":
                params["trigger_rule"] = TriggerRule.ONE_FAILED
            elif name == "cleanup_data":
                params["trigger_rule"] = TriggerRule.ALL_DONE
            elif name == "scale_up_rds_node":
                # Use XCom to signal downstream that the scale up task completed successfully
                params["do_xcom_push"] = True
            elif name == "import_sql":
                # Capture notification filename from stdout
                params["do_xcom_push"] = True
            elif name == "scale_down_rds_node":
                # Run scale down task regardless of upstream failures during import
                params["trigger_rule"] = TriggerRule.ALL_DONE

            if config.pool is not None:
                params["pool"] = config.pool

            ssh_targets: Sequence[str]
            if name in ("fetch_data", "cleanup_data"):
                ssh_targets = config.data_nodes
            else:
                ssh_targets = config.target_nodes

            return SSHOperator.partial(**params).expand(ssh_conn_id=list(ssh_targets))

        tasks: dict[str, object] = {}
        for name in config.task_names:
            if name == "data_repos":
                tasks[name] = data_repos
            elif name == "send_update_notification":
                notification_filename = extract_notification_filename(tasks["import_sql"].output)
                notification_texts = fetch_notification_text.expand(
                    ssh_conn_id=list(config.target_nodes),
                    notification_file=[notification_filename] * len(config.target_nodes),
                )
                tasks[name] = send_update_notification(notification_texts)
            else:
                tasks[name] = _build_task(name)

        config.wire_dependencies(tasks)

        @task(trigger_rule=TriggerRule.ONE_FAILED, retries=0, on_failure_callback=None)
        def watcher():
            raise AirflowException("Failing task because one or more upstream tasks failed.")

        list(dag.tasks) >> watcher()
        
        # set_import_abandoned needs to be directly downstream of all other DAG tasks in
        # order for it to trigger if any one of them fails
        if "set_import_abandoned" in config.task_names:
            # make sure we don't create a cyclical dependency
            other_tasks = [t for t in dag.tasks if t.task_id not in ("set_import_abandoned", "watcher")]
            other_tasks >> tasks["set_import_abandoned"]

    return dag


__all__ = ["ImporterConfig", "build_import_dag", "_script"]
