"""
import_mysql_dag.py
Imports to cBioPortal MySQL databases.
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
    dag_id="import_mysql_dag",
    default_args=args,
    description="Imports to cBioPortal MySQL database",
    max_active_runs=1,
    start_date=datetime(2024, 12, 3),
    schedule_interval=None,
    tags=["public"],
    render_template_as_native_obj=True,
    params={
        "importer": Param("triage", type="string", enum=["triage"], title="Import Pipeline", description="Determines which importer to use."),
        "data_repos": Param(["datahub"], type="array", description="Comma-separated list of data repositories to pull updates from/cleanup.", title="Data Repositories", examples=["datahub", "impact", "private"],)
    }
) as dag:

    importer = "{{ params.importer }}"
    import_scripts_path = "/data/portal-cron/scripts"
    creds_dir = "/data/portal-cron/pipelines-credentials"
    db_properties_filepath = f"{creds_dir}/manage_{importer}_database_update_tools.properties"
    data_source_properties_filepath = f"{creds_dir}/importer-data-source-manager-config.yaml"
    
    """
    Gets data repos as a single string
    """
    @task
    def get_data_repos(data_repos: list[str]) -> str:
        return ' '.join(data_repos)

    data_repos = get_data_repos("{{ params.data_repos }}")

    """
    Returns the target SSH connection ID for the import job itself
    A list[str] is returned so that we can set the ssh_conn_id dynamically using
    partial() + expand()
    """
    @task
    def get_target_node(importer: str) -> list[str]:
        if importer in ('triage',):
            return ['pipelines3_ssh']
        raise ValueError(importer)

    target_node = get_target_node("{{ params.importer }}")

    """
    Returns the list of SSH connection IDs to use for fetch/cleanup
    """
    @task
    def get_data_nodes(importer: str) -> list[str]:
        if importer in ('triage',):
            return ['pipelines3_ssh']
        raise ValueError(importer)

    data_nodes = get_data_nodes("{{ params.importer }}")

    """
    Fetch data updates for each data node
    """
    fetch_data = SSHOperator.partial(
        task_id="fetch_data",
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} pull {importer} {data_repos}",
        dag=dag,
    ).expand(ssh_conn_id=data_nodes)

    """
    Does a db check for specified importer/pipeline
    Refreshes CDD/Oncotree caches
    """
    setup_import = SSHOperator.partial(
        task_id="setup_import",
        command=f"{import_scripts_path}/airflow-setup-import.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)

    """
    Imports cancer types
    Imports studies from public-portal column portal-configuration spreadsheet
    """
    import_sql = SSHOperator.partial(
        task_id="import_sql",
        command=f"{import_scripts_path}/airflow-import-sql.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)


    """
    Clean up data repos for each data node
    """
    cleanup_data = SSHOperator.partial(
        task_id="cleanup_data",
        trigger_rule=TriggerRule.ALL_DONE,
        command=f"{import_scripts_path}/data_source_repo_clone_manager.sh {data_source_properties_filepath} cleanup {importer} {data_repos}",
        dag=dag,
    ).expand(ssh_conn_id=data_nodes)

    data_repos >> fetch_data >> setup_import >> import_sql >> cleanup_data 
    list(dag.tasks) >> watcher()
