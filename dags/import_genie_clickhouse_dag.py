"""
import_genie_clickhouse_dag.py
Imports Genie study to ClickHouse database.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_clickhouse_base import ClickhouseImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:

    tasks["data_repos"] >> tasks["verify_management_state"]

    tasks["verify_management_state"] >> [
        tasks["fetch_data"],
        tasks["clone_database"]
    ]

    [
        tasks["fetch_data"],
        tasks["clone_database"]
    ] >> tasks["setup_import"]

    tasks["setup_import"] >> tasks["import_direct_to_clickhouse"]

    tasks["import_direct_to_clickhouse"] >> tasks["create_derived_tables"]

    tasks["create_derived_tables"] >> tasks["transfer_deployment"]

    tasks["transfer_deployment"] >> [
        tasks["cleanup_data"],
        tasks["send_update_notification"]
    ]


_GENIE_CLICKHOUSE_CONFIG = ClickhouseImporterConfig(
    dag_id="import_genie_clickhouse_dag",
    description="Imports Genie study to ClickHouse database",
    importer="genie",
    tags=["genie-clickhouse"],
    target_nodes=("pipelines5_ssh",),
    data_nodes=("pipelines5_ssh",),
    task_names=(
        "data_repos",
        "verify_management_state",
        "fetch_data",
        "clone_database",
        "setup_import",
        "import_direct_to_clickhouse",
        "create_derived_tables",
        "transfer_deployment",
        "send_update_notification",
        "cleanup_data",
        "set_import_abandoned",
    ),
    db_properties_filename="manage_genie_database_update_tools.properties",
    color_swap_config_filename="genie-db-color-swap-config.yaml",
    params={
        "data_repos": Param(
            ["genie"],
            type="array",
            description="Comma-separated list of data repositories to pull updates from/cleanup.",
            title="Data Repositories",
            examples=["genie"],
        ),
    },
    wire_dependencies=_wire,
)

globals()[_GENIE_CLICKHOUSE_CONFIG.dag_id] = build_import_dag(_GENIE_CLICKHOUSE_CONFIG)
