"""
import_msk_dag.py
Imports MSK study to MySQL and ClickHouse databases using blue/green deployment strategy.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["clone_database"]]
    [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["transfer_deployment"] >> tasks["set_import_abandoned"] >> tasks["cleanup_data"]

_MSK_CONFIG = ImporterConfig(
    dag_id="import_msk_dag",
    description="Imports MSK study to MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="msk",
    tags=["msk"],
    target_nodes=("pipelines3_ssh",),
    data_nodes=("pipelines3_ssh",),
    task_names=(
        "verify_management_state",
        "clone_database",
        "fetch_data",
        "setup_import",
        "import_sql",
        "import_clickhouse",
        "transfer_deployment",
        "set_import_abandoned",
        "cleanup_data",
    ),
    db_properties_filename="manage_msk_database_update_tools.properties",
    color_swap_config_filename="msk-db-color-swap-config.yaml",
    params={
        "data_repos": Param(
            ["datahub"],
            type="array",
            description="Comma-separated list of data repositories to pull updates from/cleanup.",
            title="Data Repositories",
            examples=["datahub", "impact", "private"],
        ),
    },
    wire_dependencies=_wire,
)

globals()[_MSK_CONFIG.dag_id] = build_import_dag(_MSK_CONFIG)
