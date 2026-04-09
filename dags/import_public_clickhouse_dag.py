"""
import_public_clickhouse_dag.py
Imports to Public cBioPortal ClickHouse database.
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


_PUBLIC_CLICKHOUSE_CONFIG = ClickhouseImporterConfig(
    dag_id="import_public_clickhouse_dag",
    description="Imports to Public cBioPortal ClickHouse database",
    importer="public",
    tags=["public-clickhouse"],
    target_nodes=("pipelines5_ssh",),
    data_nodes=("pipelines5_ssh"),
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
    db_properties_filename="manage_public_clickhouse_database_update_tools.properties",
    color_swap_config_filename=None,
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

globals()[_PUBLIC_CLICKHOUSE_CONFIG.dag_id] = build_import_dag(_PUBLIC_CLICKHOUSE_CONFIG)
