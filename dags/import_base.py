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

CommandBuilder = Callable[["CommandArgs"], str]
WireDependencies = Callable[[dict[str, object]], None]


@dataclass(frozen=True)
class ClickhouseImporterConfig:
    dag_id: str
    description: str
    importer: str
    tags: Sequence[str]
    target_nodes: Sequence[str]
    data_nodes: Sequence[str]
    task_names: Sequence[str]
    wire_dependencies: WireDependencies
    data_repos_param: Param | None = None


@dataclass(frozen=True)
class CommandArgs:
    importer: str
    scripts_path: str
    db_properties_filepath: str
    data_source_properties_filepath: str
    data_repos: object  # XComArg string at runtime


@dataclass(frozen=True)
class CommandSet:
    verify_management: CommandBuilder
    clone_database: CommandBuilder
    fetch_data: CommandBuilder
    setup_import: CommandBuilder
    import_sql: CommandBuilder
    import_clickhouse: CommandBuilder
    transfer_deployment: CommandBuilder
    set_import_status: CommandBuilder
    cleanup_data: CommandBuilder


def _script(scripts_path: str, script_name: str, *args: object) -> str:
    parts = [f"{scripts_path}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    return " ".join(parts)


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


def _command_set() -> CommandSet:
    def _importer_script(script_name: str) -> CommandBuilder:
        def _inner(args: CommandArgs) -> str:
            return _script(
                args.scripts_path,
                script_name,
                args.importer,
                args.scripts_path,
                args.db_properties_filepath,
            )

        return _inner

    def _script_with_paths(script_name: str) -> CommandBuilder:
        def _inner(args: CommandArgs) -> str:
            return _script(
                args.scripts_path,
                script_name,
                args.scripts_path,
                args.db_properties_filepath,
            )

        return _inner

    return CommandSet(
        verify_management=_importer_script("airflow-verify-management.sh"),
        clone_database=_importer_script("airflow-clone-db.sh"),
        fetch_data=lambda args: _script(
            args.scripts_path,
            "data_source_repo_clone_manager.sh",
            args.data_source_properties_filepath,
            "pull",
            args.importer,
            args.data_repos,
        ),
        setup_import=_importer_script("airflow-setup-import.sh"),
        import_sql=_importer_script("airflow-import-sql.sh"),
        import_clickhouse=_importer_script("airflow-import-clickhouse.sh"),
        transfer_deployment=_script_with_paths("airflow-transfer-deployment.sh"),
        set_import_status=lambda args: _script(
            args.scripts_path,
            "set_update_process_state.sh",
            args.db_properties_filepath,
            "abandoned",
        ),
        cleanup_data=lambda args: _script(
            args.scripts_path,
            "data_source_repo_clone_manager.sh",
            args.data_source_properties_filepath,
            "cleanup",
            args.importer,
            args.data_repos,
        ),
    )


def build_clickhouse_import_dag(config: ClickhouseImporterConfig) -> DAG:
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

        args = CommandArgs(
            importer=importer,
            scripts_path=scripts_path,
            db_properties_filepath=db_properties_filepath,
            data_source_properties_filepath=data_source_properties_filepath,
            data_repos=data_repos,
        )
        commands = _command_set()

        target_nodes = list(config.target_nodes)
        data_nodes = list(config.data_nodes)

        available_factories: dict[str, Callable[[], object]] = {
            "verify_management_state": lambda: SSHOperator.partial(
                task_id="verify_management_state",
                command=commands.verify_management(args),
            ).expand(ssh_conn_id=target_nodes),
            "clone_database": lambda: SSHOperator.partial(
                task_id="clone_database",
                command=commands.clone_database(args),
            ).expand(ssh_conn_id=target_nodes),
            "fetch_data": lambda: SSHOperator.partial(
                task_id="fetch_data",
                command=commands.fetch_data(args),
            ).expand(ssh_conn_id=data_nodes),
            "setup_import": lambda: SSHOperator.partial(
                task_id="setup_import",
                command=commands.setup_import(args),
            ).expand(ssh_conn_id=target_nodes),
            "import_sql": lambda: SSHOperator.partial(
                task_id="import_sql",
                command=commands.import_sql(args),
            ).expand(ssh_conn_id=target_nodes),
            "import_clickhouse": lambda: SSHOperator.partial(
                task_id="import_clickhouse",
                command=commands.import_clickhouse(args),
            ).expand(ssh_conn_id=target_nodes),
            "transfer_deployment": lambda: SSHOperator.partial(
                task_id="transfer_deployment",
                command=commands.transfer_deployment(args),
            ).expand(ssh_conn_id=target_nodes),
            "set_import_status": lambda: SSHOperator.partial(
                task_id="set_import_status",
                trigger_rule=TriggerRule.ONE_FAILED,
                command=commands.set_import_status(args),
            ).expand(ssh_conn_id=target_nodes),
            "cleanup_data": lambda: SSHOperator.partial(
                task_id="cleanup_data",
                trigger_rule=TriggerRule.ALL_DONE,
                command=commands.cleanup_data(args),
            ).expand(ssh_conn_id=data_nodes),
        }

        tasks: dict[str, object] = {"data_repos": data_repos}
        for name in config.task_names:
            factory = available_factories.get(name)
            if factory is None:
                raise ValueError(f"Unsupported task '{name}' for importer '{config.importer}'.")
            tasks[name] = factory()

        config.wire_dependencies(tasks)

        @task(trigger_rule=TriggerRule.ONE_FAILED, retries=0)
        def watcher():
            raise AirflowException("Failing task because one or more upstream tasks failed.")

        list(dag.tasks) >> watcher()

    return dag


__all__ = ["ClickhouseImporterConfig", "build_clickhouse_import_dag", "_script"]
