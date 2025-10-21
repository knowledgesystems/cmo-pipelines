"""
import_public_dag.py
Imports to Public cBioPortal MySQL and ClickHouse databases using blue/green deployment strategy.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["verify_management_state"]
    tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["clone_database"]]
    [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["transfer_deployment"] >> tasks["set_import_abandoned"] >> tasks["cleanup_data"]

_PUBLIC_CONFIG = ImporterConfig(
    dag_id="import_public_dag",
    description="Imports to Public cBioPortal MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="public",
    tags=["public"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh", "pipelines3_ssh"),
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
    db_properties_filename="manage_public_database_update_tools.properties",
    color_swap_config_filename="public-db-color-swap-config.yaml",
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

globals()[_PUBLIC_CONFIG.dag_id] = build_import_dag(_PUBLIC_CONFIG)
