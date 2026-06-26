"""
import_public_hackathon_calla.py

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
import logging
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.exceptions import AirflowException
from airflow.models import Variable
from airflow.models.param import Param
from airflow.operators.bash import BashOperator
from kubernetes.client import models as k8s

from dags.utils.secret_manager import SecretManager

logger = logging.getLogger(__name__)

STUDY_LIST_VARIABLE_KEY = "hackathon_available_study_ids"


def _available_study_ids() -> list[str]:
    """Read the study-list Variable at parse time to populate the Param enum."""
    try:
        import json
        return json.loads(Variable.get(STUDY_LIST_VARIABLE_KEY, default_var="[]"))
    except Exception:
        return []


K8S_IMAGE = "callachennault/cmo-import:dev"

# Hardcoded for now; promote to DAG params later if they need to vary per run.
S3_BUCKET_NAME = "hackathon-databricks"
SCRIPTS_DIR = "/data/portal-cron/scripts"
IMPORTER = "public"
# Runtime credentials live in a directory the image leaves empty; at run time a
# Kubernetes Secret is mounted over it (see _POD_OVERRIDE / CREDS_* constants).
CREDS_DIR = "/data/portal-cron/pipelines-credentials"
CREDS_SECRET_NAME = "import-credentials-test"
CREDS_VOLUME_NAME = "pipelines-credentials"
COLOR_SWAP_CONFIG_FILE = f"{CREDS_DIR}/public-db-color-swap-config.yaml"
CLICKHOUSE_CONFIG_FILE = f"{CREDS_DIR}/manage_public_clickhouse_database_update_tools.properties"
# Importer writes its per-study success/failure summary here; read it for Slack later.
NOTIFICATION_FILE = "/tmp/airflow-notifications/import_public_hackathon_calla/{{ ts_nodash }}.txt"


_POD_OVERRIDE = {
    "pod_override": k8s.V1Pod(
        spec=k8s.V1PodSpec(
            containers=[k8s.V1Container(
                name="base",
                image=K8S_IMAGE,
                image_pull_policy="Always",
                # Point saml2aws at the mounted config file without touching the shared script.
                env=[
                    k8s.V1EnvVar(
                        name="SAML2AWS_CONFIGFILE",
                        value=f"{CREDS_DIR}/.saml2aws",
                    ),
                    # Airflow injects KUBECONFIG pointing at the Airflow cluster kubeconfig
                    # (namespace: airflow). Unset it so kubectl --kubeconfig uses only the
                    # file we specify and defaults namespace to 'default'.
                    k8s.V1EnvVar(name="KUBECONFIG", value=""),
                ],
                # Mount the credentials Secret over the (empty) creds dir baked
                # into the image. Each Secret key surfaces as a file here, so the
                # hardcoded *_CONFIG_FILE paths above resolve unchanged.
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
                    # 0o400: readable only by the owning (airflow) user.
                    default_mode=0o400,
                ),
            )],
        )
    )
}

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}


def _script(script_name: str, *args: object, source_automation_env: bool = False) -> str:
    """Builds a ``{SCRIPTS_DIR}/{script} {args...}`` command, mirroring import_base._script."""
    parts = [f"{SCRIPTS_DIR}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    cmd = " ".join(parts)
    if source_automation_env:
        return f"source {SCRIPTS_DIR}/automation-environment.sh && {cmd}"
    return cmd


@dag(
    dag_id="import_public_hackathon_calla",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    max_active_runs=1,
    # Needed so the list-typed cancer_study_ids param renders as a native list.
    render_template_as_native_obj=True,
    params={
        "cancer_study_ids": Param(
            [],
            type="array",
            examples=_available_study_ids(),
            description="Select one or more cancer study IDs to import. Run refresh_hackathon_study_list to update the list.",
            title="Cancer Study IDs",
        ),
    },
)
def import_public_hackathon_calla():
    # ── Python tasks (S3 / ClickHouse / Slack via SecretManager) ───────

    @task(executor_config=_POD_OVERRIDE)
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
            # Studies may be stored as a directory prefix (study_id/) or a tarball
            # (study_id.tar). Check both; either form is acceptable.
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

    @task(executor_config=_POD_OVERRIDE)
    def verify_import_not_in_progress(clickhouse_config_file: str) -> None:
        """Gate: fail if the management DB reports an import already running.

        No production bash script covers this check, so it stays a Python task that
        queries the management DB directly.
        """
        # TODO: client = SecretManager.clickhouse_client(clickhouse_config_file)
        #       query the management DB for the current update-process state; raise if 'running'.
        logger.info("[STUB] verify_import_not_in_progress via %s", clickhouse_config_file)

    @task(executor_config=_POD_OVERRIDE)
    def validate_studies(study_ids: list[str], s3_bucket: str) -> list[str]:
        """Downloads and validates each study; returns the ones that pass."""
        import pathlib

        s3 = SecretManager.s3_client()
        valid: list[str] = []
        for study_id in study_ids:
            local_dir = f"/tmp/{study_id}"
            pathlib.Path(local_dir).mkdir(parents=True, exist_ok=True)
            try:
                prefix = f"{study_id}/"
                paginator = s3.get_paginator("list_objects_v2")
                for page in paginator.paginate(Bucket=s3_bucket, Prefix=prefix):
                    for obj in page.get("Contents", []):
                        key = obj["Key"]
                        rel_path = key[len(prefix):]
                        if not rel_path:
                            continue
                        dest = os.path.join(local_dir, rel_path)
                        os.makedirs(os.path.dirname(dest), exist_ok=True)
                        s3.download_file(s3_bucket, key, dest)
                # TODO: run the real validator against local_dir.
                logger.info("[STUB] validated %s at %s", study_id, local_dir)
                valid.append(study_id)
            except Exception as e:
                logger.error("Validation failed for %s: %s", study_id, e)

        if not valid:
            raise AirflowException("No studies passed validation")
        logger.info("Studies passing validation: %s", valid)
        return valid

    @task(executor_config=_POD_OVERRIDE)
    def send_slack_notifications() -> None:
        """Posts the import result to Slack."""
        # TODO: hook = SecretManager.slack_hook()
        #       read NOTIFICATION_FILE / task states + log URLs and send the message.
        logger.info("[STUB] send_slack_notifications")

    # ── BashOperator tasks (call the production import-scripts) ────────

    # verify cluster health + management/ingress color consistency
    t_verify_cluster = BashOperator(
        task_id="verify_cluster_state",
        bash_command=_script(
            "airflow-verify-management.sh",
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
            COLOR_SWAP_CONFIG_FILE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    # mark the import as running in the management DB
    # t_set_running = BashOperator(
    #     task_id="set_import_running",
    #     bash_command=_script(
    #         "set_update_process_state.sh",
    #         CLICKHOUSE_CONFIG_FILE,
    #         "running",
    #         source_automation_env=True,
    #     ),
    #     executor_config=_POD_OVERRIDE,
    # )

    # wipe + clone live DB into standby (airflow-clone-db.sh does both)
    # t_clone = BashOperator(
    #     task_id="clone_live_database_into_standby",
    #     bash_command=_script(
    #         "airflow-clone-db.sh",
    #         IMPORTER,
    #         SCRIPTS_DIR,
    #         CLICKHOUSE_CONFIG_FILE,
    #     ),
    #     executor_config=_POD_OVERRIDE,
    # )

    # import studies into the standby color DB
    # t_import = BashOperator(
    #     task_id="import_into_standby_database",
    #     bash_command=_script(
    #         "airflow-import-direct-to-clickhouse.sh",
    #         IMPORTER,
    #         SCRIPTS_DIR,
    #         CLICKHOUSE_CONFIG_FILE,
    #         NOTIFICATION_FILE,
    #     ),
    #     executor_config=_POD_OVERRIDE,
    # )

    # swap production traffic to the freshly imported standby color
    # t_transfer = BashOperator(
    #     task_id="transfer_deployment_color",
    #     bash_command=_script(
    #         "airflow-transfer-deployment.sh",
    #         SCRIPTS_DIR,
    #         CLICKHOUSE_CONFIG_FILE,
    #         COLOR_SWAP_CONFIG_FILE,
    #     ),
    #     executor_config=_POD_OVERRIDE,
    # )

    # mark the import as complete in the management DB
    # t_complete = BashOperator(
    #     task_id="set_import_complete",
    #     bash_command=_script(
    #         "set_update_process_state.sh",
    #         CLICKHOUSE_CONFIG_FILE,
    #         "complete",
    #         source_automation_env=True,
    #     ),
    #     executor_config=_POD_OVERRIDE,
    # )

    # ── Instantiate Python tasks ──────────────────────────────────────
    cancer_study_ids = "{{ params.cancer_study_ids }}"

    t_found_studies          = verify_studies_exist(cancer_study_ids, S3_BUCKET_NAME)
    # t_verify_not_in_progress = verify_import_not_in_progress(CLICKHOUSE_CONFIG_FILE)
    # t_valid                  = validate_studies(t_found_studies, S3_BUCKET_NAME)
    # t_slack                  = send_slack_notifications()

    # ── Wire the graph ────────────────────────────────────────────────
    t_found_studies >> t_verify_cluster
    # t_verify_cluster >> t_verify_not_in_progress >> t_set_running
    # t_set_running >> [t_clone, t_valid]
    # [t_clone, t_valid] >> t_import
    # t_import >> t_transfer >> t_complete >> t_slack


import_public_hackathon_calla()
