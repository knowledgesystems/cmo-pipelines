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
    "email": ["chennac@mskcc.org"],
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
    dagrun_timeout=timedelta(minutes=360),
    max_active_runs=1,
    start_date=datetime(2024, 12, 3),
    schedule_interval=None,
    tags=["public"],
    params={
        "importer": Param("public", type="string", enum=["public"], title="Import Pipeline", description="Determines which importer to use."),
        "data_repos": Param(["datahub"], type="array", description="Comma-separated list of data repositories to pull updates from/cleanup.", title="Data Repositories", examples=["datahub", "msk-impact", "private"],)
    }
) as dag:

    importer = "{{ params.importer }}"
    # TODO add pipelines3 connection string
    pipelines3_conn_id = ""
    # TODO rename import node connection string
    import_node_conn_id = "genie_importer_ssh"
    import_scripts_path = "/data/portal-cron/scripts"
    db_properties_filepath = f"/data/portal-cron/pipelines-credentials/manage_{importer}_database_update_tools.properties"

    """
    Parses and validates DAG arguments
    """
    @task
    def parse_args(data_repos: list):
        return ' '.join(data_repos)

    datarepos = parse_args("{{ params.data_repos }}")

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
    # TODO pass repos
    fetch_data_local = SSHOperator(
        task_id="fetch_data_local",
        ssh_conn_id=import_node_conn_id,
        command=f"echo {import_scripts_path} {db_properties_filepath} {datarepos}",
        dag=dag,
    )

    """
    Fetch data updates within MSK network
    """
    # TODO pass repos
    fetch_data_remote = SSHOperator(
        task_id="fetch_data_remote",
        ssh_conn_id=pipelines3_conn_id,
        command=f"echo {import_scripts_path} {db_properties_filepath} {datarepos}",
        dag=dag,
    )

    """
    Does a db check for specified importer/pipeline
    Fetches latest commit from repository
    Refreshes CDD/Oncotree caches
    """
    # will check if repos are updated
    setup_import = SSHOperator(
        task_id="setup_import",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-setup.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    Imports cancer types
    Imports studies from ? column in portal-configuration spreadsheet
    """
    import_sql = SSHOperator(
        task_id="import_sql",
        ssh_conn_id=import_node_conn_id,
        command=f"{import_scripts_path}/public-airflow-import-sql.sh {import_scripts_path} {db_properties_filepath}",
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
    Removes untracked files/LFS objects from repos.
    """
    cleanup_repo = SSHOperator(
        task_id="cleanup_repo",
        ssh_conn_id=import_node_conn_id,
        trigger_rule=TriggerRule.ALL_DONE,
        command=f"{import_scripts_path}/datasource-repo-cleanup.sh {datarepos}",
        dag=dag,
    )

    datarepos >> [clone_database, fetch_data_local, fetch_data_remote] >> setup_import >> import_sql >> import_clickhouse >> transfer_deployment >> set_import_status >> cleanup_repo
    list(dag.tasks) >> watcher()
