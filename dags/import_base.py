"""Shared builder for ClickHouse import DAGs."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Callable, Mapping, Optional, Sequence

from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException
from airflow.models.param import Param
from airflow.providers.ssh.operators.ssh import SSHOperator
from airflow.utils.trigger_rule import TriggerRule


_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}

WireDependencies = Callable[[dict[str, object]], None]


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
        schedule_interval=None,
        tags=list(config.tags),
        render_template_as_native_obj=True,
        params=params,
    )

    with dag:
        importer = config.importer
        scripts_dir = config.scripts_dir
        creds_dir = config.creds_dir
        db_properties_filepath = f"{creds_dir}/{config.db_properties_filename}"
        color_swap_config_filepath = f"{creds_dir}/{config.color_swap_config_filename}"
        data_source_properties_filepath = f"{creds_dir}/{config.data_source_properties_filename}"

        @task
        def get_data_repos(repos: list[str]) -> str:
            return " ".join(repos)

        data_repos = get_data_repos("{{ params.get('data_repos', []) }}")

        command_map = {
            "verify_management_state": _script(
                scripts_dir,
                "airflow-verify-management.sh",
                scripts_dir,
                db_properties_filepath,
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
            "transfer_deployment": _script(
                scripts_dir,
                "airflow-transfer-deployment.sh",
                scripts_dir,
                db_properties_filepath,
                color_swap_config_filepath,
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

            ssh_targets: Sequence[str]
            if name in ("fetch_data", "cleanup_data"):
                ssh_targets = config.data_nodes
            else:
                ssh_targets = config.target_nodes

            return SSHOperator.partial(**params).expand(ssh_conn_id=list(ssh_targets))

        tasks: dict[str, object] = {"data_repos": data_repos}
        for name in config.task_names:
            tasks[name] = _build_task(name)

        config.wire_dependencies(tasks)

        @task(trigger_rule=TriggerRule.ONE_FAILED, retries=0)
        def watcher():
            raise AirflowException("Failing task because one or more upstream tasks failed.")

        list(dag.tasks) >> watcher()

    return dag


__all__ = ["ImporterConfig", "build_import_dag", "_script"]
