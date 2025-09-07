"""
import_triage_dag.py
Imports triage dataset using the import-cmo-data-triage.sh script
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
    dag_id="import_triage_dag",
    default_args=args,
    description="Imports studies into triage",
    max_active_runs=1,
    start_date=datetime(2025, 9, 5),
    schedule_interval=None,
    tags=["triage"],
    render_template_as_native_obj=True,
    params={
        "importer": Param("triage", type="string", enum=["triage"], title="Import Pipeline", description="Determines which importer to use."),
    }
) as dag:

    pipelines3_conn_id = "pipelines3_ssh"
    import_scripts_path = "/data/portal-cron/scripts"
    creds_dir = "/data/portal-cron/pipelines-credentials"

    import_triage_data = SSHOperator(
        task_id="import_triage_data",
        ssh_conn_id=pipelines3_conn_id,
        # IMPORTANT -- do not end with .sh, otherwise the command will be treated as a path to a Jinja template which we do not want
        # added a space at the end to prevent Airflow from this behavior
        command=f"{import_scripts_path}/import-cmo-data-triage.sh ",
        dag=dag,
    )

    import_triage_data
    list(dag.tasks) >> watcher()
