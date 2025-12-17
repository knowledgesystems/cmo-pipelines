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
    # tasks["data_repos"] >> tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["scale_up_rds_node"]]
    # tasks["scale_up_rds_node"] >> tasks["clone_database"]
    # [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    # tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["scale_down_rds_node"] >> tasks["transfer_deployment"] >> tasks["set_import_abandoned"] >> tasks["cleanup_data"]
    tasks["scale_up_rds_node"] >> tasks["scale_down_rds_node"]

_TEST_CONFIG = ImporterConfig(
    dag_id="import_test_rds_scaling_dag",
    description="Imports Genie study to MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="test",
    tags=["genie"],
    target_nodes=("pipelines3_ssh",),
    data_nodes=("pipelines3_ssh",),
    task_names=(
        "scale_up_rds_node",
        "scale_down_rds_node",
    ),
    db_properties_filename=None,
    color_swap_config_filename="test-rds-scaling-color-swap-config.yaml",
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

globals()[_TEST_CONFIG.dag_id] = build_import_dag(_TEST_CONFIG)
