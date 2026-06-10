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
    tasks["data_repos"] >> tasks["turn_ec2_on"] >> tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["scale_up_rds_node"]]
    tasks["scale_up_rds_node"] >> tasks["clone_database"]
    [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["transfer_deployment"] >> tasks["scale_down_rds_node"] >> tasks["send_update_notification"] >> tasks["cleanup_data"] >> tasks["turn_ec2_off"]

_PUBLIC_CONFIG = ImporterConfig(
    dag_id="import_public_dag",
    description="Imports to Public cBioPortal MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="public",
    ec2_instance_id="i-0a8c3a8a243d16d10",
    sibling_dag_ids=("import_genie_dag",),
    tags=["public"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh", "pipelines3_ssh"),
    task_names=(
        "turn_ec2_on",
        "verify_management_state",
        "scale_up_rds_node",
        "clone_database",
        "fetch_data",
        "setup_import",
        "import_sql",
        "import_clickhouse",
        "transfer_deployment",
        "scale_down_rds_node",
        "send_update_notification",
        "cleanup_data",
        "set_import_abandoned",
        "turn_ec2_off",
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
