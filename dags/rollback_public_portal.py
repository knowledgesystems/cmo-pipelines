"""
rollback_public_portal.py
Transfer the production Public deployment to the backup database.
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
    dag_id="rollback_public_portal",
    default_args=args,
    description="",
    max_active_runs=1,
    start_date=datetime(2024, 12, 3),
    schedule_interval=None,
    tags=["public"],
    params={
        "confirm": Param(type="string", enum=["yes"], title="You are running a DAG that will roll back the current Public production deployment.", description="Please confirm that you understand by typing 'yes' in the text box."),
    }
) as dag:

    conn_id = "importer_ssh"
    import_scripts_path = "/data/portal-cron/scripts"
    db_properties_filepath="/data/portal-cron/pipelines-credentials/manage_public_database_update_tools.properties"

    """
    Set the import attempt status to "running".
    """
    set_import_running = SSHOperator(
        task_id="set_import_running",
        ssh_conn_id=conn_id,
        trigger_rule=TriggerRule.ONE_FAILED,
        command=f"{import_scripts_path}/set_update_process_state.sh {db_properties_filepath} running",
        dag=dag,
    )

    """
    Transfer Public deployment to the backup database.
    """
    transfer_deployment = SSHOperator(
        task_id="transfer_deployment",
        ssh_conn_id=conn_id,
        command=f"{import_scripts_path}/public-airflow-transfer-deployment.sh {import_scripts_path} {db_properties_filepath}",
        dag=dag,
    )

    """
    If any upstream tasks failed, mark the import attempt as abandoned.
    """
    set_import_status = SSHOperator(
        task_id="set_import_status",
        ssh_conn_id=conn_id,
        trigger_rule=TriggerRule.ONE_FAILED,
        command=f"{import_scripts_path}/set_update_process_state.sh {db_properties_filepath} abandoned",
        dag=dag,
    )

    set_import_running >> transfer_deployment >> set_import_status
    list(dag.tasks) >> watcher()