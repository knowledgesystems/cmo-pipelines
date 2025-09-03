"""
import_tempo_dag.py
Imports TEMPO dataset into triage using default import method
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
    dag_id="import_tempo_dag",
    default_args=args,
    description="Imports TEMPO dataset into triage using default import method",
    max_active_runs=1,
    start_date=datetime(2025, 8, 8),
    schedule_interval=None,
    tags=["tempo"],
    render_template_as_native_obj=True,
    params={
        "importer": Param("triage", type="string", enum=["triage"], title="Import Pipeline", description="Determines which importer to use."),
    }
) as dag:

    pipelines5_conn_id = "pipelines5_ssh"
    import_scripts_path = "/data/portal-cron/scripts"
    creds_dir = "/data/portal-cron/pipelines-credentials"

    """
    Clean up data repos within MSK network
    """
    import_tempo_data = SSHOperator(
        task_id="import_tempo_data",
        ssh_conn_id=pipelines5_conn_id,
        command=f"{import_scripts_path}/import-tempo-data.sh {{ params.importer }}",
        dag=dag,
    )

    import_tempo_data
    list(dag.tasks) >> watcher()
