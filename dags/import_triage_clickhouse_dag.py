"""
import_triage_dag.py
Imports Triage study to MySQL database.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_clickhouse_base import ClickhouseImporterConfig, build_import_dag

def _wire(tasks: dict[str, object]) -> None:
    
    tasks["data_repos"] >> tasks["verify_management_state"]
    
    tasks["verify_management_state"] >> tasks["set_import_running"]
    
    tasks["set_import_running"] >> [
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
        tasks["clear_persistence_caches"],
        tasks["cleanup_data"],
        tasks["send_update_notification"]
    ]
    
    [
        tasks["clear_persistence_caches"],
        tasks["cleanup_data"],
        tasks["send_update_notification"]
    ] >> tasks["set_import_complete"]
    
    # for now, all the 'finally' handlers are taken care of by the parent class

_TRIAGE_CONFIG = ClickhouseImporterConfig(
    dag_id="import_triage_clickhouse_dag",
    description="Imports Triage study to Clickhouse database",
    importer="triage-clickhouse",
    tags=["triage-clickhouse"],
    target_nodes=("pipelines3_ssh",),
    data_nodes=("pipelines3_ssh",),
    task_names=(
        "data_repos",
        "verify_management_state",
        "set_import_running",
        "fetch_data",
        "clone_database",
        "setup_import",
        "import_direct_to_clickhouse",
        "create_derived_tables",
        "transfer_deployment",
        "clear_persistence_caches",
        "send_update_notification",
        "cleanup_data",
        "set_import_complete",
        "set_import_abandoned",
    ),
    db_properties_filename="manage_triage_clickhouse_database_update_tools.properties",
    color_swap_config_filename="triage-db-color-swap-config.yaml",
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
    # Leave out the scheduled runs for now
    # pool="triage_import_pool",
    # schedule_interval="0 0 * * *",
)

globals()[_TRIAGE_CONFIG.dag_id] = build_import_dag(_TRIAGE_CONFIG)
