"""
import_genie_dag.py
Imports Genie study to MySQL and ClickHouse databases using blue/green deployment strategy.
"""
import os
import sys
from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["scale_up_rds_node"]]
    tasks["scale_up_rds_node"] >> tasks["clone_database"]
    [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["transfer_deployment"] >> tasks["scale_down_rds_node"] >> tasks["cleanup_data"]

_GENIE_CONFIG = ImporterConfig(
    dag_id="import_genie_dag",
    description="Imports Genie study to MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="genie",
    tags=["genie"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        "verify_management_state",
        "scale_up_rds_node",
        "clone_database",
        "fetch_data",
        "setup_import",
        "import_sql",
        "import_clickhouse",
        "transfer_deployment",
        "scale_down_rds_node",
        "cleanup_data",
        "set_import_abandoned",
    ),
    db_properties_filename="manage_genie_database_update_tools.properties",
    color_swap_config_filename="genie-db-color-swap-config.yaml",
    params={
        "data_repos": Param(
            ["genie"],
            type="array",
            title="Data Repositories",
            description="List of GENIE data repositories to clean up after import.",
            examples=["genie"],
        ),
    },
    wire_dependencies=_wire,
)

globals()[_GENIE_CONFIG.dag_id] = build_import_dag(_GENIE_CONFIG)
