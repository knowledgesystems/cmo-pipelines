"""
rollback_genie_portal.py
Transfer the production Genie deployment to the backup database.
"""
from airflow.models.param import Param
from dags.import_base import ImporterConfig, build_import_dag


def _wire(tasks: dict[str, object]) -> None:
    tasks["verify_management_state"] >> tasks["set_import_running"] >> tasks["transfer_deployment"] >> tasks["set_import_abandoned"]

_ROLLBACK_GENIE_CONFIG = ImporterConfig(
    dag_id="rollback_genie_portal_dev",
    description="",
    importer="genie",
    tags=["genie"],
    target_nodes=("importer_ssh",),
    data_nodes=("importer_ssh",),
    task_names=(
        "verify_management_state",
        "set_import_running",
        "transfer_deployment",
        "set_import_abandoned",
    ),
    db_properties_filename="manage_genie_database_update_tools.properties",
    params={
        "confirm": Param(
            type="string",
            enum=["yes"],
            title="You are running a DAG that will roll back the current GENIE production deployment.",
            description="Please confirm that you understand by typing 'yes' in the text box.",
        ),
    },
    wire_dependencies=_wire,
)

globals()[_ROLLBACK_GENIE_CONFIG.dag_id] = build_import_dag(_ROLLBACK_GENIE_CONFIG)
