"""Shared builder for ClickHouse import DAGs."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Callable, Mapping, Optional, Sequence
import logging
import shlex

from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException, AirflowSkipException
from airflow.models.param import Param
from airflow.providers.ssh.operators.ssh import SSHOperator
from airflow.providers.ssh.hooks.ssh import SSHHook
from airflow.utils.trigger_rule import TriggerRule
from airflow.utils.state import State
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
import_sql_failure_slack_msg = """
        :red_circle: Import SQL Failed. Please check the notification file in the Airflow logs.
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
        *Log Url*: {{ import_sql_log_url }}
"""
import_sql_success_slack_msg = """
        :large_green_circle: Import SQL Success!
        *DAG ID*: {{ dag.dag_id }}
        *Execution Time*: {{ execution_date }}
        *Log Url*: {{ import_sql_log_url }}
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
        notification_filepath = f"/tmp/airflow-notifications/{config.dag_id}/{{{{ ts_nodash }}}}.txt"

        @task
        def get_data_repos(repos: list[str]) -> str:
            return " ".join(repos)

        # run this task even if import_sql failed
        @task(trigger_rule=TriggerRule.ALL_DONE)
        def send_update_notification(notification_filepath: str, ssh_conn_id: str) -> None:
            """
            Sends a Slack message to the #airflow-logs channel with a link to the import_sql logs URL.
            This tells the curators whether there were any studies that suceeded or failed to import during a given run.
            To avoid confusion -- we run this task towards the end of the DAG
            (eg. after the transfer_deployment step) because we don't want to
            send a success message before the entire import run completes.
            """

            # Get the log URL for the import_sql task
            context = get_current_context()
            dag_run = context.get("dag_run")
            import_sql_ti = None
            if dag_run is not None:
                import_sql_ti = dag_run.get_task_instance("import_sql", map_index=0)
            import_sql_log_url = import_sql_ti.log_url if import_sql_ti is not None else ""
            if not import_sql_log_url:
                logger.warning("Could not determine import_sql log url; skipping Slack notification.")
                raise AirflowSkipException()

            import_sql_failed = (
                import_sql_ti is not None and import_sql_ti.state == State.FAILED
            )
            if not import_sql_failed:
                # Read the notification file from the remote node to check if any studies failed
                try:
                    ssh_hook = SSHHook(ssh_conn_id=ssh_conn_id)
                    ssh_client = ssh_hook.get_conn()
                    exit_status, notif_contents, _ = ssh_hook.exec_ssh_client_command(
                        ssh_client, f"cat {shlex.quote(notification_filepath)}", get_pty=False, environment=None
                    )
                    if exit_status != 0:
                        logger.warning("Notification file not found at %s; treating as failure", notification_filepath)
                        import_sql_failed = True
                    else:
                        notification_content = notif_contents.decode("utf-8")
                        ERROR_STRING = "The following studies had errors during import"
                        import_sql_failed = (ERROR_STRING in notification_content)
                except Exception as exc:
                    logger.warning("Could not read notification file from remote node; skipping Slack notification")
                    logger.warning("Stack trace:")
                    logger.warning(exc)
                    raise AirflowSkipException() from exc

            # Build the msg and send to Slack
            msg_template = import_sql_failure_slack_msg if import_sql_failed else import_sql_success_slack_msg
            rendered_message = Template(msg_template).render(
                import_sql_log_url=import_sql_log_url,
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
                notification_filepath,
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

        tasks: dict[str, object] = {"data_repos": data_repos}
        for name in config.task_names:
            if name == "send_update_notification":
                tasks[name] = send_update_notification(
                    notification_filepath=notification_filepath,
                    ssh_conn_id=config.target_nodes[0],
                )
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
