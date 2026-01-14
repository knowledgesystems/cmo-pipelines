import pendulum
from datetime import datetime

from airflow.decorators import dag, task
from airflow.providers.slack.notifications.slack_webhook import send_slack_webhook_notification

dag_failure_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=f"DAG **{{{{ dag.dag_id }}}}** failed at {datetime.now().isoformat(timespec='minutes')}"
)
dag_success_slack_webhook_notification = send_slack_webhook_notification(
    slack_webhook_conn_id="slack_default", text=f"DAG **{{{{ dag.dag_id }}}}** succeeded at {datetime.now().isoformat(timespec='minutes')}"
)

@dag(
    schedule=None,
    start_date=pendulum.datetime(2021, 1, 1, tz="UTC"),
    catchup=False,
    tags=['example'],
    default_args={
        'email_on_failure': False,
        'email_on_retry': False,
    },
    on_failure_callback=[dag_failure_slack_webhook_notification],
    on_success_callback=[dag_success_slack_webhook_notification]
)
def test_slack_notifs_dag():    
    @task
    def hello():
        print('Hello world')

    hello()

# Execute the dag
test_slack_notifs_dag()