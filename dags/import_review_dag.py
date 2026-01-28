"""
import_review_dag.py
Imports Review study to MySQL database.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag

def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["fetch_data"]
    tasks["fetch_data"] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["cleanup_data"]

_REVIEW_CONFIG = ImporterConfig(
    dag_id="import_review_dag",
    description="Imports Review study to MySQL database",
    importer="review",
    tags=["review"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        "data_repos",
        "fetch_data",
        "setup_import",
        "import_sql",
        "cleanup_data",
    ),
    db_properties_filename="manage_review_database_update_tools.properties",
    color_swap_config_filename=None, # Not used for MySQL
    params={
        "data_repos": Param(
            ["datahub-publicdbv7"],
            type="array",
            description="Comma-separated list of data repositories to pull updates from/cleanup.",
            title="Data Repositories",
            examples=["datahub-publicdbv7"],
        ),
    },
    wire_dependencies=_wire,
)

globals()[_REVIEW_CONFIG.dag_id] = build_import_dag(_REVIEW_CONFIG)
