"""
import_genie_dag.py
Imports Genie study to MySQL and ClickHouse databases using blue/green deployment strategy.
"""
from airflow.models.param import Param

from dags.import_base import ClickhouseImporterConfig, build_clickhouse_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["verify_management_state"]
    tasks["verify_management_state"] >> [tasks["fetch_data"], tasks["clone_database"]]
    [tasks["fetch_data"], tasks["clone_database"]] >> tasks["setup_import"]
    tasks["setup_import"] >> tasks["import_sql"] >> tasks["import_clickhouse"] >> tasks["transfer_deployment"] >> tasks["set_import_status"] >> tasks["cleanup_data"]

_GENIE_CONFIG = ClickhouseImporterConfig(
    dag_id="import_genie_dag",
    description="Imports Genie study to MySQL and ClickHouse databases using blue/green deployment strategy",
    importer="genie",
    tags=["genie"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        # "verify_management_state",
        "clone_database",
        "fetch_data",
        "setup_import",
        "import_sql",
        "import_clickhouse",
        "transfer_deployment",
        "set_import_status",
        "cleanup_data",
    ),
    wire_dependencies=_wire,
    data_repos_param=Param(
        ["genie"],
        type="array",
        title="Data Repositories",
        description="List of GENIE data repositories to clean up after import.",
        examples=["genie"],
    ),
)

globals()[_GENIE_CONFIG.dag_id] = build_clickhouse_import_dag(_GENIE_CONFIG)
