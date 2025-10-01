"""Shared builder for ClickHouse import DAGs."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Callable, Sequence

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

_DEFAULT_DATA_REPOS_PARAM = Param(
    ["datahub"],
    type="array",
    description="Comma-separated list of data repositories to pull updates from/cleanup.",
    title="Data Repositories",
    examples=["datahub", "impact", "private"],
)


WireDependencies = Callable[[dict[str, object]], None]


@dataclass(frozen=True)
class ImporterConfig:
    dag_id: str
    description: str
    importer: str
    tags: Sequence[str]
    target_nodes: Sequence[str]
    data_nodes: Sequence[str]
    task_names: Sequence[str]
    wire_dependencies: WireDependencies
    data_repos_param: Param | None = None


def _script(scripts_path: str, script_name: str, *args: object) -> str:
    parts = [f"{scripts_path}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    return " ".join(parts)


def _importer_script(
    scripts_path: str,
    script_name: str,
    importer: str,
    db_properties_filepath: str,
) -> str:
    return _script(scripts_path, script_name, importer, scripts_path, db_properties_filepath)


def _transform_data_repos(importer: str, values: Sequence[str]) -> str:
    if importer == "genie":
        root = "/data/portal-cron/cbio-portal-data"
        allowed = {"genie"}
        paths: list[str] = []
        for value in values:
            name = value.strip()
            if name not in allowed:
                raise ValueError(f"Unsupported GENIE data repo '{name}'.")
            paths.append(f"{root}/{name}")
        return " ".join(paths)

    return " ".join(values)


def build_import_dag(config: ImporterConfig) -> DAG:
    params = {"data_repos": config.data_repos_param or _DEFAULT_DATA_REPOS_PARAM}

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
        scripts_path = "/data/portal-cron/scripts"
        creds_dir = "/data/portal-cron/pipelines-credentials"
        db_properties_filepath = f"{creds_dir}/manage_{importer}_database_update_tools.properties"
        data_source_properties_filepath = f"{creds_dir}/importer-data-source-manager-config.yaml"

        @task
        def get_data_repos(data_repos: list[str]) -> str:
            return _transform_data_repos(importer, data_repos)

        data_repos = get_data_repos("{{ params.data_repos }}")

        command_map = {
            "verify_management_state": _importer_script(
                scripts_path,
                "airflow-verify-management.sh",
                importer,
                db_properties_filepath,
            ),
            "clone_database": _importer_script(
                scripts_path,
                "airflow-clone-db.sh",
                importer,
                db_properties_filepath,
            ),
            "fetch_data": _script(
                scripts_path,
                "data_source_repo_clone_manager.sh",
                data_source_properties_filepath,
                "pull",
                importer,
                data_repos,
            ),
            "setup_import": _importer_script(
                scripts_path,
                "airflow-setup-import.sh",
                importer,
                db_properties_filepath,
            ),
            "import_sql": _importer_script(
                scripts_path,
                "airflow-import-sql.sh",
                importer,
                db_properties_filepath,
            ),
            "import_clickhouse": _importer_script(
                scripts_path,
                "airflow-import-clickhouse.sh",
                importer,
                db_properties_filepath,
            ),
            "transfer_deployment": _script(
                scripts_path,
                "airflow-transfer-deployment.sh",
                scripts_path,
                db_properties_filepath,
            ),
            "set_import_status": _script(
                scripts_path,
                "set_update_process_state.sh",
                db_properties_filepath,
                "abandoned",
            ),
            "cleanup_data": _script(
                scripts_path,
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

            if name == "set_import_status":
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


__all__ = ["ImporterConfig", "build_import_dag", "_script", "_importer_script"]
