"""
import_public_dag.py
Imports to Public cBioPortal MySQL and ClickHouse databases using blue/green deployment strategy.
"""
from datetime import timedelta, datetime
from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException
from airflow.models.param import Param
from airflow.providers.ssh.operators.ssh import SSHOperator
from airflow.utils.trigger_rule import TriggerRule

args = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}

"""
If any upstream tasks failed, this task will propagate the "Failed" status to the Dag Run.
"""
@task(trigger_rule=TriggerRule.ONE_FAILED, retries=0)
def watcher():
    raise AirflowException("Failing task because one or more upstream tasks failed.")

with DAG(
    dag_id="import_public_dag",
    default_args=args,
    description="Imports to Public cBioPortal MySQL and ClickHouse databases using blue/green deployment strategy",
    max_active_runs=1,
    start_date=datetime(2024, 12, 3),
    schedule_interval=None,
    tags=["public"],
    render_template_as_native_obj=True,
    params={
        "importer": Param("public", type="string", enum=["public"], title="Import Pipeline", description="Determines which importer to use."),
        "data_repos": Param(["datahub"], type="array", description="Comma-separated list of data repositories to pull updates from/cleanup.", title="Data Repositories", examples=["datahub", "impact", "private"],)
    }
) as dag:

    importer = "{{ params.importer }}"
    pipelines3_conn_id = "pipelines3_ssh"
    import_node_conn_id = "importer_ssh"
    import_scripts_path = "/data/portal-cron/scripts"
    creds_dir = "/data/portal-cron/pipelines-credentials"
    db_properties_filepath = f"{creds_dir}/manage_{importer}_database_update_tools.properties"
    data_source_properties_filepath = f"{creds_dir}/importer-data-source-manager-config.yaml"

    """
    Parses and validates DAG arguments
    """
    @task
    def get_data_repos(data_repos: list):
        return ' '.join(data_repos)

    data_repos = get_data_repos("{{ params.data_repos }}")

    """
    Verifies the update process management database state and fails import early if incorrect
    """
    verify_management_state = SSHOperator(
        task_id="verify_management_state",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-verify-management.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Determines which database is "production" vs "not production"
    Drops tables in the non-production MySQL database
    Clones the production MySQL database into the non-production database
    """
    clone_database = SSHOperator(
        task_id="clone_database",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-clone-db.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Fetch data updates on import node
    """
    fetch_data_local = SSHOperator(
        task_id="fetch_data_local",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} pull {importer} {data_repos}",
        dag=dag,
    )

    """
    Fetch data updates within MSK network
    """
    fetch_data_remote = SSHOperator(
        task_id="fetch_data_remote",
        ssh_conn_id=pipelines3_conn_id,
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} pull {importer} {data_repos}",
        dag=dag,
    )

    """
    Does a db check for specified importer/pipeline
    Refreshes CDD/Oncotree caches
    """
    setup_import = SSHOperator(
        task_id="setup_import",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-setup.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Imports cancer types
    Imports studies from public-portal column portal-configuration spreadsheet
    """
    import_sql = SSHOperator(
        task_id="import_sql",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-import-sql.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Drops ClickHouse tables
    Copies MySQL tables to ClickHouse
    Creates derived ClickHouse tables
    """
    import_clickhouse = SSHOperator(
        task_id="import_clickhouse",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-import-clickhouse.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Transfers Public deployment to newly updated database
    """
    transfer_deployment = SSHOperator(
        task_id="transfer_deployment",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-transfer-deployment.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    If any upstream tasks failed, mark the import attempt as abandoned.
    """
    set_import_status = SSHOperator(
        task_id="set_import_status",
        ssh_conn_id=import_node_conn_id,
        trigger_rule=TriggerRule.ONE_FAILED,
        command=f"{import_scripts_path}/set_update_process_state.sh {db_properties_filepath} abandoned",
        dag=dag,
    )

    """
    Clean up data repos on import node
    """
    cleanup_data_local = SSHOperator(
        task_id="cleanup_data_local",
        ssh_conn_id=import_node_conn_id,
        trigger_rule=TriggerRule.ALL_DONE,
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} cleanup {importer} {data_repos}",
        dag=dag,
    )

    """
    Clean up data repos within MSK network
    """
    cleanup_data_remote = SSHOperator(
        task_id="cleanup_data_remote",
        ssh_conn_id=pipelines3_conn_id,
        trigger_rule=TriggerRule.ALL_DONE,
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} cleanup {importer} {data_repos}",
        dag=dag,
    )

    data_repos >> verify_management_state >> [clone_database, fetch_data_local, fetch_data_remote] >> setup_import >> import_sql >> import_clickhouse >> transfer_deployment >> set_import_status >> [cleanup_data_local, cleanup_data_remote] 
    list(dag.tasks) >> watcher()
