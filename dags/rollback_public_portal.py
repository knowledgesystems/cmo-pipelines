"""
rollback_public_portal.py
Transfer the production public deployment to the backup database.
"""
import os
import sys

from airflow.models.param import Param

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["data_repos"] >> tasks["verify_management_state"] >> tasks["set_import_running"] >> tasks["transfer_deployment"] >> tasks["set_import_abandoned"]

_ROLLBACK_PUBLIC_CONFIG = ImporterConfig(
    dag_id="rollback_public_portal",
    description="",
    importer="public",
    tags=["public"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        "verify_management_state",
        "set_import_running",
        "transfer_deployment",
        "set_import_abandoned",
    ),
    db_properties_filename="manage_public_database_update_tools.properties",
    color_swap_config_filename="public-db-color-swap-config.yaml",
    params={
        "confirm": Param(
            type="string",
            enum=["yes"],
            title="You are running a DAG that will roll back the current public production deployment.",
            description="Please confirm that you understand by typing 'yes' in the text box.",
        ),
    },
    wire_dependencies=_wire,
)

globals()[_ROLLBACK_PUBLIC_CONFIG.dag_id] = build_import_dag(_ROLLBACK_PUBLIC_CONFIG)
