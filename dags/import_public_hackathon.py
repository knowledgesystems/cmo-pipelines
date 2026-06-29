"""
import_public_hackathon.py

Blue/green ClickHouse import pipeline for public cancer studies pulled from S3.

The infrastructure tasks (verify cluster, set state, clone DB, import, transfer, mark
complete) shell out to the same import-scripts/ bash scripts the production DAGs run --
but via BashOperator (executed locally in the task pod) rather than SSHOperator, since
this pipeline runs in-cluster. Those scripts read the config files and determine the
blue/green color themselves (via get_database_currently_in_production.sh).

The S3/validation/notification tasks stay as TaskFlow @task functions and resolve their
clients through SecretManager (see dags/utils/). The config-file paths and S3 bucket are
hardcoded constants for now; only ``cancer_study_ids`` is a DAG param.
"""
import json
import logging
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.exceptions import AirflowException, AirflowSkipException
from airflow.models import Variable
from airflow.models.param import Param
from airflow.utils.trigger_rule import TriggerRule
from kubernetes.client import models as k8s

from dags.utils.secret_manager import SecretManager


def skippable(func):
    """Decorator: skip the task if it's not in the run_tasks param (empty = run all)."""
    task_id = func.__name__

    def wrapper(*args, **kwargs):
        run_tasks = kwargs.pop("run_tasks", None)
        if not _should_run(task_id, run_tasks):
            raise AirflowSkipException(f"Skipped per run_tasks param: {task_id}")
        return func(*args, **kwargs)

    wrapper.__name__ = func.__name__
    wrapper.__qualname__ = func.__qualname__
    wrapper.__doc__ = func.__doc__
    return wrapper

logger = logging.getLogger(__name__)

S3_BUCKET_NAME = "hackathon-databricks"
K8S_IMAGE            = "callachennault/cmo-import:dev"
K8S_IMAGE_VALIDATE   = "averyniceday/hackathon-import:latest"
VALIDATE_SCRIPT_PATH = "/scripts/importer/validateStudies.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer/metaImport.py"
STUDY_LIST_VARIABLE_KEY = "hackathon_available_study_ids"
SCRIPTS_DIR = "/data/portal-cron/scripts"
IMPORTER = "public"
CREDS_DIR = "/data/portal-cron/pipelines-credentials"
CREDS_SECRET_NAME = "import-credentials-test"
CREDS_VOLUME_NAME = "pipelines-credentials"
COLOR_SWAP_CONFIG_FILE = f"{CREDS_DIR}/public-db-color-swap-config.yaml"
CLICKHOUSE_CONFIG_FILE = f"{CREDS_DIR}/manage_public_clickhouse_database_update_tools.properties"
NOTIFICATION_FILE = "/tmp/airflow-notifications/import_public_hackathon/{{ ts_nodash }}.txt"

def _available_study_ids() -> list[str]:
    """Read the study-list Variable at parse time to populate the Param enum."""
    try:
        return json.loads(Variable.get(STUDY_LIST_VARIABLE_KEY, default_var="[]"))
    except Exception:
        return []


def _download_study_from_s3(s3_client, s3_bucket: str, study_id: str, local_dir: str) -> None:
    import pathlib
    import tarfile

    tar_key = f"{study_id}.tar"
    tar_resp = s3_client.list_objects_v2(Bucket=s3_bucket, Prefix=tar_key, MaxKeys=1)
    if tar_resp.get("KeyCount", 0) > 0:
        tar_path = f"/tmp/{study_id}.tar"
        s3_client.download_file(s3_bucket, tar_key, tar_path)
        with tarfile.open(tar_path) as tf:
            tf.extractall(local_dir)
        entries = list(pathlib.Path(local_dir).iterdir())
        if len(entries) == 1 and entries[0].is_dir():
            for child in entries[0].iterdir():
                child.rename(pathlib.Path(local_dir) / child.name)
            entries[0].rmdir()
    else:
        prefix = f"{study_id}/"
        paginator = s3_client.get_paginator("list_objects_v2")
        for page in paginator.paginate(Bucket=s3_bucket, Prefix=prefix):
            for obj in page.get("Contents", []):
                key = obj["Key"]
                rel_path = key[len(prefix):]
                if not rel_path:
                    continue
                dest = os.path.join(local_dir, rel_path)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                s3_client.download_file(s3_bucket, key, dest)


def _script(script_name: str, *args: object, source_automation_env: bool = False) -> str:
    """Builds a ``{SCRIPTS_DIR}/{script} {args...}`` command, mirroring import_base._script."""
    parts = [f"{SCRIPTS_DIR}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    cmd = " ".join(parts)
    if source_automation_env:
        return f"source {SCRIPTS_DIR}/automation-environment.sh && {cmd}"
    return cmd


_POD_OVERRIDE = {
    "pod_override": k8s.V1Pod(
        spec=k8s.V1PodSpec(
            containers=[k8s.V1Container(
                name="base",
                image=K8S_IMAGE,
                image_pull_policy="Always",
                env=[
                    k8s.V1EnvVar(
                        name="SAML2AWS_CONFIGFILE",
                        value=f"{CREDS_DIR}/.saml2aws",
                    ),
                    k8s.V1EnvVar(name="KUBECONFIG", value=""),
                ],
                volume_mounts=[k8s.V1VolumeMount(
                    name=CREDS_VOLUME_NAME,
                    mount_path=CREDS_DIR,
                    read_only=True,
                )],
            )],
            volumes=[k8s.V1Volume(
                name=CREDS_VOLUME_NAME,
                secret=k8s.V1SecretVolumeSource(
                    secret_name=CREDS_SECRET_NAME,
                    default_mode=0o400,
                ),
            )],
        )
    )
}

def _make_cbioportal_pod_override(java_opts: str | None = None, memory_request: str = "2Gi", memory_limit: str = "3Gi") -> dict:
    env = [
        k8s.V1EnvVar(name="PORTAL_HOME", value="/"),
        k8s.V1EnvVar(
            name="CLICKHOUSE_HOST",
            value_from=k8s.V1EnvVarSource(
                secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="host")
            ),
        ),
        k8s.V1EnvVar(
            name="CLICKHOUSE_NATIVE_PORT",
            value_from=k8s.V1EnvVarSource(
                secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="native_port")
            ),
        ),
        k8s.V1EnvVar(
            name="CLICKHOUSE_USER",
            value_from=k8s.V1EnvVarSource(
                secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="user")
            ),
        ),
        k8s.V1EnvVar(
            name="CLICKHOUSE_PASSWORD",
            value_from=k8s.V1EnvVarSource(
                secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="password")
            ),
        ),
        k8s.V1EnvVar(
            name="CLICKHOUSE_DB",
            value_from=k8s.V1EnvVarSource(
                secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key="database")
            ),
        ),
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))

    return {
        "pod_override": k8s.V1Pod(
            spec=k8s.V1PodSpec(
                tolerations=[
                    k8s.V1Toleration(
                        key="workload",
                        operator="Equal",
                        value="airflow",
                        effect="NoSchedule",
                    ),
                ],
                containers=[k8s.V1Container(
                    name="base",
                    image=K8S_IMAGE_VALIDATE,
                    image_pull_policy="Always",
                    resources=k8s.V1ResourceRequirements(
                        requests={"memory": memory_request, "cpu": "1"},
                        limits={"memory": memory_limit},
                    ),
                    env=env,
                    volume_mounts=[
                        k8s.V1VolumeMount(
                            name="app-properties",
                            mount_path="/application.properties",
                            sub_path="application.properties",
                            read_only=True,
                        ),
                        k8s.V1VolumeMount(
                            name="clickhouse-sql",
                            mount_path="/clickhouse.sql",
                            sub_path="clickhouse.sql",
                            read_only=True,
                        ),
                    ],
                )],
                volumes=[
                    k8s.V1Volume(
                        name="app-properties",
                        secret=k8s.V1SecretVolumeSource(secret_name="hackathon-app-properties"),
                    ),
                    k8s.V1Volume(
                        name="clickhouse-sql",
                        secret=k8s.V1SecretVolumeSource(secret_name="hackathon-clickhouse-sql"),
                    ),
                ],
            )
        )
    }


_POD_OVERRIDE_VALIDATE = _make_cbioportal_pod_override(memory_request="2Gi", memory_limit="3Gi")
_POD_OVERRIDE_IMPORT   = _make_cbioportal_pod_override(java_opts="-Xmx4g", memory_request="5Gi", memory_limit="6Gi")

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}


_ALL_TASK_IDS = [
    "verify_cluster_state",
    "verify_studies_exist",
    "verify_import_not_in_progress",
    "set_import_running",
    "clone_live_database_into_standby",
    "pull_and_validate_study",
    "collect_valid_studies",
    "import_into_standby_database",
    "create_derived_tables_in_standby_database",
    "transfer_deployment_color",
    "set_import_complete",
    "send_slack_notifications",
]


def _should_run(task_id: str, run_tasks: list[str] | None) -> bool:
    """Returns True if the task should run (empty/None list = run all)."""
    return not run_tasks or task_id in run_tasks


@dag(
    dag_id="import_public_hackathon",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    max_active_runs=1,
    render_template_as_native_obj=True,
    params={
        "cancer_study_ids": Param(
            [],
            type="array",
            examples=_available_study_ids(),
            description="Select one or more cancer study IDs to import. Run refresh_hackathon_study_list to update the list.",
            title="Cancer Study IDs",
        ),
        "run_tasks": Param(
            [],
            type="array",
            items={"type": "string", "enum": _ALL_TASK_IDS},
            description="Leave empty to run all tasks, or select specific tasks to run.",
            title="Run Tasks (empty = all)",
        ),
    },
)
def import_public_hackathon():
    # ── 1 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def verify_studies_exist(study_ids: list[str], s3_bucket: str) -> list[str]:
        """All-or-nothing: every requested study must exist in S3, else fail the DAG."""
        # Params with type="array" should arrive as a list, but render_template_as_native_obj
        # may render them as a string repr (e.g. "['study1', 'study2']"). Parse defensively.
        if isinstance(study_ids, str):
            import ast
            import json
            try:
                study_ids = json.loads(study_ids)
            except (json.JSONDecodeError, ValueError):
                study_ids = ast.literal_eval(study_ids)
        study_ids = [s.strip() for s in (study_ids or []) if s and s.strip()]
        if not study_ids:
            raise AirflowException("No study IDs provided")

        s3 = SecretManager.s3_client()
        missing = []
        for study_id in study_ids:
            dir_resp = s3.list_objects_v2(Bucket=s3_bucket, Prefix=f"{study_id}/", MaxKeys=1)
            tar_resp = s3.list_objects_v2(Bucket=s3_bucket, Prefix=f"{study_id}.tar", MaxKeys=1)
            if dir_resp.get("KeyCount", 0) > 0 or tar_resp.get("KeyCount", 0) > 0:
                logger.info("Study '%s' found in s3://%s", study_id, s3_bucket)
            else:
                logger.error("Study '%s' NOT found in s3://%s", study_id, s3_bucket)
                missing.append(study_id)

        if missing:
            raise AirflowException(f"Studies not found in s3://{s3_bucket}: {missing}")
        return study_ids

    # ── 2 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def verify_cluster_state() -> None:
        """Verify cluster health + management/ingress color consistency."""
        import subprocess
        cmd = _script(
            "airflow-verify-management.sh",
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
            COLOR_SWAP_CONFIG_FILE,
        )
        subprocess.run(cmd, shell=True, check=True, executable="/bin/bash")

    # ── 3 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def verify_import_not_in_progress(clickhouse_config_file: str) -> None:
        """Gate: fail if the management DB reports an import already running.

        No production bash script covers this check, so it stays a Python task that
        queries the management DB directly.
        """
        # TODO: client = SecretManager.clickhouse_client(clickhouse_config_file)
        #       query the management DB for the current update-process state; raise if 'running'.
        logger.info("[STUB] verify_import_not_in_progress via %s", clickhouse_config_file)

    # ── 4 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def set_import_running() -> None:
        """Mark the import as running in the management DB."""
        import subprocess
        cmd = _script(
            "set_update_process_state.sh",
            CLICKHOUSE_CONFIG_FILE,
            "running",
            source_automation_env=True,
        )
        subprocess.run(cmd, shell=True, check=True, executable="/bin/bash")

    # ── 5 ──────────────────────────────────────────────────────────────
    # wipe + clone live DB into standby (airflow-clone-db.sh does both)
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def clone_live_database_into_standby() -> None:
        """Wipe + clone live DB into standby."""
        import subprocess
        cmd = _script(
            "airflow-clone-db.sh",
            IMPORTER,
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
        )
        subprocess.run(cmd, shell=True, check=True, executable="/bin/bash")

    # ── 6 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE_VALIDATE)
    @skippable
    def pull_and_validate_study(study_id: str, s3_bucket: str) -> str | None:
        import pathlib
        import subprocess

        local_dir = f"/tmp/{study_id}"
        pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

        try:
            s3 = SecretManager.s3_client()
            _download_study_from_s3(s3, s3_bucket, study_id, local_dir)
            log_dir = f"/tmp/validate_logs/{study_id}"
            os.makedirs(log_dir, exist_ok=True)
            result = subprocess.run(
                [sys.executable, VALIDATE_SCRIPT_PATH, "-l", local_dir, "-n", "-html", log_dir],
                capture_output=True,
                text=True,
            )
            logging.info(result.stdout)
            for log_file in pathlib.Path(log_dir).glob("log-validate-studies-*.txt"):
                logging.info("=== Validation log: %s ===\n%s", log_file.name, log_file.read_text())
            if result.returncode not in (0, 3):
                logging.error("Validation failed for %s (exit %d):\n%s", study_id, result.returncode, result.stderr)
                return None
            return study_id
        except Exception as e:
            logging.error("Validation failed for %s: %s", study_id, e)
            return None

    # ── 8 ──────────────────────────────────────────────────────────────
    @task(trigger_rule=TriggerRule.ALL_DONE, executor_config=_POD_OVERRIDE)
    @skippable
    def collect_valid_studies(results: list) -> list[str]:
        valid = [sid for sid in (results or []) if sid is not None]
        if not valid:
            raise AirflowSkipException("No studies passed validation — skipping import")
        logging.info("Studies passing validation: %s", valid)
        return valid

    # ── 9 ──────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE_IMPORT)
    @skippable
    def import_into_standby_database(valid_studies: list[str]):
        import pathlib
        import subprocess

        if not valid_studies:
            logging.info("No valid studies to import — exiting.")
            return

        s3 = SecretManager.s3_client()
        failed = []

        for study_id in valid_studies:
            local_dir = f"/tmp/{study_id}"
            pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)

            try:
                _download_study_from_s3(s3, S3_BUCKET_NAME, study_id, local_dir)
            except Exception as e:
                logging.error("Download failed for %s: %s", study_id, e)
                failed.append(study_id)
                continue

            result = subprocess.run(
                [sys.executable, IMPORT_SCRIPT_PATH,
                 "-s", local_dir,
                 "-n",
                 "-o",
                 "--no-derive-tables"],
                capture_output=True, text=True,
            )
            logging.info(result.stdout)
            if result.stderr:
                logging.info(result.stderr)
            if result.returncode != 0:
                logging.error("Import failed for %s (exit %d)", study_id, result.returncode)
                failed.append(study_id)

        if failed:
            raise Exception(f"Import failed for {len(failed)} study/studies: {failed}")

    # ── 10 ─────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE_IMPORT)
    @skippable
    def create_derived_tables_in_standby_database():
        import subprocess

        # Rebuild derived tables once after all studies are loaded
        rebuild = subprocess.run(
            [sys.executable, IMPORT_SCRIPT_PATH, "derive-tables"],
            capture_output=True, text=True,
        )
        logging.info(rebuild.stdout)
        if rebuild.stderr:
            logging.info(rebuild.stderr)
        if rebuild.returncode != 0:
            raise Exception(f"Derived table rebuild failed (exit {rebuild.returncode}):\n{rebuild.stderr}")

    # ── 11 ─────────────────────────────────────────────────────────────
    # swap production traffic to the freshly imported standby color
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def transfer_deployment_color() -> None:
        """Swap production traffic to the freshly imported standby color."""
        import subprocess
        cmd = _script(
            "airflow-transfer-deployment.sh",
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
            COLOR_SWAP_CONFIG_FILE,
        )
        subprocess.run(cmd, shell=True, check=True, executable="/bin/bash")

    # ── 12 ─────────────────────────────────────────────────────────────
    # mark the import as complete in the management DB
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def set_import_complete() -> None:
        """Mark the import as complete in the management DB."""
        import subprocess
        cmd = _script(
            "set_update_process_state.sh",
            CLICKHOUSE_CONFIG_FILE,
            "complete",
            source_automation_env=True,
        )
        subprocess.run(cmd, shell=True, check=True, executable="/bin/bash")

    # ── 13 ─────────────────────────────────────────────────────────────
    @task(executor_config=_POD_OVERRIDE)
    @skippable
    def send_slack_notifications() -> None:
        """Posts the import result to Slack."""
        # TODO: hook = SecretManager.slack_hook()
        #       read NOTIFICATION_FILE / task states + log URLs and send the message.
        logger.info("[STUB] send_slack_notifications")

    # ── Instantiate tasks in execution order ─────────────────────────
    t_found_studies                  = verify_studies_exist("{{ params.cancer_study_ids }}", S3_BUCKET_NAME, run_tasks="{{ params.run_tasks }}")
    t_verify_cluster_state           = verify_cluster_state(run_tasks="{{ params.run_tasks }}")
    t_verify_import_not_in_progress  = verify_import_not_in_progress(CLICKHOUSE_CONFIG_FILE, run_tasks="{{ params.run_tasks }}")
    t_set_import_running             = set_import_running(run_tasks="{{ params.run_tasks }}")
    t_clone_live_database            = clone_live_database_into_standby(run_tasks="{{ params.run_tasks }}")
    t_pull_and_validate              = pull_and_validate_study.partial(s3_bucket=S3_BUCKET_NAME, run_tasks="{{ params.run_tasks }}").expand(study_id=t_found_studies)
    t_collect_valid                  = collect_valid_studies(t_pull_and_validate, run_tasks="{{ params.run_tasks }}")
    t_import                         = import_into_standby_database(t_collect_valid, run_tasks="{{ params.run_tasks }}")
    t_create_derived_tables          = create_derived_tables_in_standby_database(run_tasks="{{ params.run_tasks }}")
    t_transfer_deployment_color      = transfer_deployment_color(run_tasks="{{ params.run_tasks }}")
    t_set_import_complete            = set_import_complete(run_tasks="{{ params.run_tasks }}")
    t_send_slack_notifications       = send_slack_notifications(run_tasks="{{ params.run_tasks }}")

    # Sequential gate chain
    (
        t_found_studies
        >> t_verify_cluster_state
        >> t_verify_import_not_in_progress
        >> t_set_import_running
    )

    # Fork after gate: DB prep and validation run in parallel
    t_set_import_running >> [t_clone_live_database, t_pull_and_validate]

    # Diamond join: import waits for both branches
    t_clone_live_database >> t_import

    # Tail chain
    (
        t_import
        >> t_create_derived_tables
        >> t_transfer_deployment_color
        >> t_set_import_complete
        >> t_send_slack_notifications
    )


import_public_hackathon()
