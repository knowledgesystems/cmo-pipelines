import pendulum
from datetime import datetime

from airflow.decorators import dag, task
from airflow.providers.slack.notifications.slack_webhook import send_slack_webhook_notification

fail_slack_msg = f"""
        :red_circle: DAG Failed.
        *Dag ID*: {{{{ dag.dag_id }}}}
        *Task ID*: {{{{ task_instance.task_id }}}}
        *Execution Time*: {{{{ execution_date }}}}
        *Log Url*: {{{{ task_instance.log_url }}}}
"""
success_slack_msg = f"""
        :green_circle: DAG Success!
        *Dag*: {{{{ dag.dag_id }}}}
        *Execution Time*: {{{{ execution_date }}}}
"""
dag_failure_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=fail_slack_msg
)
dag_success_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=success_slack_msg
)

@dag(
    schedule=None,
    start_date=pendulum.datetime(2021, 1, 1, tz="UTC"),
    catchup=False,
    tags=['example'],
    default_args={
        'email_on_failure': False,
        'email_on_retry': False,
        'on_failure_callback': [dag_failure_slack_webhook_notification],
    },
    on_success_callback=[dag_success_slack_webhook_notification]
)
def test_slack_notif_dag():    
    @task
    def hello():
        print('Hello world')

    hello()

# Execute the dag
test_slack_notif_dag()