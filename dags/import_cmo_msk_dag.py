"""
import_cmo_msk_dag.py
Imports CMO MSK study to ClickHouse database using blue/green deployment strategy.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:

    tasks["data_repos"] >> tasks["verify_management_state"]

    tasks["verify_management_state"] >> tasks["verify_import_not_in_progress"]

    tasks["verify_import_not_in_progress"] >> [
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


_CMO_MSK_CONFIG = ImporterConfig(
    dag_id="import_cmo_msk_dag",
    description="Imports CMO MSK study to ClickHouse database using blue/green deployment strategy",
    importer="msk",
    tags=["cmo"],
    target_nodes=("pipelines3_ssh",),
    data_nodes=("pipelines3_ssh",),
    task_names=(
        "data_repos",
        "verify_management_state",
        "verify_import_not_in_progress",
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
    db_properties_filename="manage_msk_clickhouse_database_update_tools.properties",
    color_swap_config_filename="msk-db-color-swap-config.yaml",
    params={
        "data_repos": Param(
            ["datahub", "private", "impact"],
            type="array",
            description="Comma-separated list of data repositories to pull updates from/cleanup.",
            title="Data Repositories",
            examples=["datahub", "private", "impact"],
        ),
    },
    wire_dependencies=_wire,
)

globals()[_CMO_MSK_CONFIG.dag_id] = build_import_dag(_CMO_MSK_CONFIG)
