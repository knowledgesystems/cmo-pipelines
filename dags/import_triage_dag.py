"""
import_triage_dag.py
Imports Triage study to MySQL database.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag

def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["fetch_data"]
    tasks["fetch_data"] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["clear_persistence_caches"] >> tasks["cleanup_data"]

_TRIAGE_CONFIG = ImporterConfig(
    dag_id="import_triage_dag",
    description="Imports Triage study to MySQL database",
    importer="triage",
    tags=["triage"],
    target_nodes=("pipelines3_ssh",),
    data_nodes=("pipelines3_ssh",),
    task_names=(
        "fetch_data",
        "setup_import",
        "import_sql",
        "clear_persistence_caches",
        "cleanup_data",
    ),
    db_properties_filename="manage_triage_database_update_tools.properties",
    color_swap_config_filename=None, # Not used for MySQL
    params={
        "data_repos": Param(
            [
                "datahub",
                "cmo-argos",
                "private",
                "impact"
            ],
            type="array",
            description="Comma-separated list of data repositories to pull updates from/cleanup.",
            title="Data Repositories",
            examples=[
                "datahub",
                "bic-mskcc-legacy",
                "cmo-argos",
                "private",
                "impact",
                "knowledge-systems-curated-studies",
                "datahub_shahlab",
                "msk-mind-datahub",
                "pipelines-testing",
                "genie",
                "extract-projects",
                "cmo-access"
            ],
        ),
    },
    wire_dependencies=_wire,
    pool="triage_import_pool",
    schedule_interval="0 0 * * *",
)

globals()[_TRIAGE_CONFIG.dag_id] = build_import_dag(_TRIAGE_CONFIG)
