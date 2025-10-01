"""
import_triage_dag.py
Imports Triage study to MySQL database.
"""
from airflow.models.param import Param

from dags.import_base import ImporterConfig, build_import_dag

def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["fetch_data"]
    tasks["fetch_data"] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["cleanup_data"]

_TRIAGE_CONFIG = ImporterConfig(
    dag_id="import_triage_dag",
    description="Imports Triage study to MySQL database",
    importer="triage",
    tags=["triage"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        "fetch_data",
        "setup_import",
        "import_sql",
        "cleanup_data",
    ),
    wire_dependencies=_wire,
)

globals()[_TRIAGE_CONFIG.dag_id] = build_import_dag(_TRIAGE_CONFIG)
