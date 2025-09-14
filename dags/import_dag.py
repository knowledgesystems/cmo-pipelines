"""
import_dag.py
Unified import DAG supporting MySQL-only and ClickHouse (blue/green) flows.
"""
from datetime import timedelta, datetime
from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException
from airflow.models.param import Param
from airflow.providers.ssh.operators.ssh import SSHOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.python import ShortCircuitOperator
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
    dag_id="import_dag",
    default_args=args,
    description="Imports to cBioPortal MySQL or Clickhouse database using blue/green deployment strategy",
    max_active_runs=1,
    start_date=datetime(2024, 12, 3),
    schedule_interval=None,
    tags=["public"],
    render_template_as_native_obj=True,
    params={
        "importer": Param("triage", type="string", enum=["public", "msk", "genie", "triage"], title="Import Pipeline", description="Determines which importer to use."),
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
        if importer in ('triage', 'msk'):
            return ['pipelines3_ssh']
        elif importer in ('public', 'genie'):
            return ['importer_ssh']
        raise ValueError(importer)

    target_node = get_target_node("{{ params.importer }}")

    """
    Returns the list of SSH connection IDs to use for fetch/cleanup
    """
    @task
    def get_data_nodes(importer: str) -> list[str]:
        if importer in ('triage', 'msk'):
            return ['pipelines3_ssh']
        elif importer == 'genie':
            # genie only needs repos that are publicly accessible from the import node
            return ['importer_ssh']
        elif importer == 'public':
            # The public importer runs on the import node, which is outside the MSK network
            # and thus can't access github.mskcc.org.
            # To resolve this problem, we fetch updates on both the import node / pipelines3,
            # and pipelines3 will rsync those private repos to the import node.
            return ['importer_ssh', 'pipelines3_ssh']
        raise ValueError(importer)

    data_nodes = get_data_nodes("{{ params.importer }}")
    
    """
    Verifies the update process management database state and fails import early if incorrect
    Only runs for the Public (ClickHouse) importer path.
    """
    verify_management_state = SSHOperator.partial(
        task_id="verify_management_state",
        command=f"{import_scripts_path}/public-airflow-verify-management.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)

    """
    Determines which database is "production" vs "not production"
    Drops tables in the non-production MySQL database
    Clones the production MySQL database into the non-production database
    """
    clone_database = SSHOperator.partial(
        task_id="clone_database",
        command=f"{import_scripts_path}/airflow-clone-db.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)

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
    Drops ClickHouse tables
    Copies MySQL tables to ClickHouse
    Creates derived ClickHouse tables
    """
    import_clickhouse = SSHOperator.partial(
        task_id="import_clickhouse",
        command=f"{import_scripts_path}/airflow-import-clickhouse.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)

    """
    Transfers Public deployment to newly updated database
    """
    transfer_deployment = SSHOperator.partial(
        task_id="transfer_deployment",
        command=f"{import_scripts_path}/airflow-transfer-deployment.sh {importer} {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    ).expand(ssh_conn_id=target_node)

    """
    If any upstream tasks failed, mark the import attempt as abandoned.
    """
    set_import_status = SSHOperator.partial(
        task_id="set_import_status",
        trigger_rule=TriggerRule.ONE_FAILED,
        command=f"{import_scripts_path}/set_update_process_state.sh {db_properties_filepath} abandoned",
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

    # Aggregate argument tasks behind a single node
    parse_args = EmptyOperator(task_id="parse_args")
    [data_repos, target_node, data_nodes] >> parse_args

    # Branch: choose MySQL-only or ClickHouse flow based on importer
    @task.branch
    def branch_on_importer(importer: str) -> str:
        return "mysql_gate" if importer == "triage" else "clickhouse_gate"

    branch = branch_on_importer(importer)
    mysql_gate = EmptyOperator(task_id="mysql_gate")
    clickhouse_gate = EmptyOperator(task_id="clickhouse_gate")
    branch >> [mysql_gate, clickhouse_gate]
    
    @task.branch
    def branch_on_public(importer: str) -> str:
        return "public_gate" if importer == "public" else "non_public_gate"

    branch = branch_on_public(importer)
    public_gate = EmptyOperator(task_id="public_gate")
    non_public_gate = EmptyOperator(task_id="non_public_gate")
    branch >> [public_gate, non_public_gate]

    # Join node to converge pre-import steps; tolerate skipped branch
    join = EmptyOperator(task_id="join_pre_import",
                         trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS)

    # Wire DAG dependencies
    mysql_gate >> parse_args >> fetch_data >> join
    public_gate >> parse_args >> verify_management_state >> [fetch_data, clone_database] >> join
    [clickhouse_gate, non_public_gate] >> parse_args >> [fetch_data, clone_database] >> join
    
    join >> setup_import

    # Allow import_sql to proceed when the non-selected branch is skipped
    import_sql.trigger_rule = TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS
    setup_import >> import_sql

    mysql_gate >> import_sql >> cleanup_data
    clickhouse_gate >> import_sql >> import_clickhouse >> transfer_deployment >> set_import_status >> cleanup_data

    list(dag.tasks) >> watcher()
