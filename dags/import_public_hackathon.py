"""Blue/green ClickHouse import of public cancer studies from S3. Infrastructure
tasks run the production import-scripts/ via BashOperator (in-cluster, not SSH);
validation/import tasks run the cbioportal-core scripts directly."""
import json
import logging
import os
import subprocess
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.operators.bash import BashOperator
from airflow.exceptions import AirflowException, AirflowSkipException
from airflow.models import Variable
from airflow.models.param import Param
from airflow.utils.trigger_rule import TriggerRule
from kubernetes.client import models as k8s

logger = logging.getLogger(__name__)


def _run_and_stream(
    cmd: list[str],
    check: bool = False,
    timeout: int | None = None,
    **kwargs,
) -> "subprocess.CompletedProcess":
    """Run a command, streaming its stdout/stderr via ``logging`` in real time, then
    return a ``CompletedProcess`` with full captured output for post-hoc inspection.

    Uses two daemon threads to drain stdout and stderr concurrently, which avoids
    deadlocks on pipe buffers while preserving the distinction between the two streams.
    For Python subprocesses the ``-u`` flag is automatically appended so output is
    unbuffered (no long silences during import).
    """
    import subprocess
    import sys as _sys
    import threading

    # If running a Python script, add -u for unbuffered output.
    # Check only the first flag position to avoid matching a path argument.
    if cmd and cmd[0] == _sys.executable and (len(cmd) < 2 or cmd[1] != "-u"):
        cmd = [cmd[0], "-u"] + cmd[1:]

    logger.info("Running: %s", " ".join(str(c) for c in cmd))

    process = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1,
        **kwargs,
    )

    stdout_lines: list[str] = []
    stderr_lines: list[str] = []

    def _drain(stream, log_fn, lines):
        for raw in iter(stream.readline, ""):
            line = raw.rstrip("\n\r")
            lines.append(line)
            log_fn("%s", line)
        stream.close()

    t_out = threading.Thread(
        target=_drain, args=(process.stdout, logger.info, stdout_lines), daemon=True
    )
    t_err = threading.Thread(
        target=_drain, args=(process.stderr, logger.warning, stderr_lines), daemon=True
    )
    t_out.start()
    t_err.start()

    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait()
        raise

    # Close our read ends so the drain threads see EOF and exit.
    process.stdout.close()
    process.stderr.close()
    t_out.join()
    t_err.join()

    rc = process.returncode
    captured_stdout = "\n".join(stdout_lines)
    captured_stderr = "\n".join(stderr_lines)
    logger.info("Exit code: %d", rc)
    if rc != 0:
        logger.warning("stderr was:\n%s", captured_stderr)

    if check and rc != 0:
        raise subprocess.CalledProcessError(rc, cmd)

    return subprocess.CompletedProcess(
        args=cmd,
        returncode=rc,
        stdout=captured_stdout,
        stderr=captured_stderr,
    )

K8S_IMAGE            = "ghcr.io/cbioportal/containerized-importer-cmo:dev"
K8S_IMAGE_VALIDATE   = "ghcr.io/cbioportal/containerized-importer-core:dev"
VALIDATE_SCRIPT_PATH = "/scripts/importer/validateStudies.py"
IMPORT_SCRIPT_PATH   = "/scripts/importer/metaImport.py"
STUDY_LIST_VARIABLE_KEY = "available_study_ids"
SCRIPTS_DIR = "/data/portal-cron/scripts"
IMPORTER = "public"
CREDS_DIR = "/data/portal-cron/pipelines-credentials"
CREDS_SECRET_NAME = "pipelines-credentials"
CREDS_VOLUME_NAME = "pipelines-credentials"
APP_PROPERTIES_SECRET_BLUE  = "containerized-properties-blue"
APP_PROPERTIES_SECRET_GREEN = "containerized-properties-green"
APP_PROPERTIES_PATH         = "/application.properties"
GET_DB_IN_PROD_SCRIPT       = f"{SCRIPTS_DIR}/get_database_currently_in_production.sh"
COLOR_SWAP_CONFIG_FILE = f"{CREDS_DIR}/public-db-color-swap-config.yaml"
CLICKHOUSE_CONFIG_FILE = f"{CREDS_DIR}/manage_public_clickhouse_database_update_tools.properties"

S3_MOUNT_PATH = "/mnt/s3-data"
S3_PVC_CLAIM_NAME = "databricks-s3-pvc"


def _study_data_path(study_id: str) -> str | None:
    """Resolve a study on the S3 mount; extract tarballs to a temp dir. None if absent."""
    import pathlib
    import tarfile
    import tempfile
    import shutil

    mount_tar = pathlib.Path(S3_MOUNT_PATH) / f"{study_id}.tar.gz"
    mount_dir = pathlib.Path(S3_MOUNT_PATH) / study_id

    if mount_dir.is_dir():
        return str(mount_dir)

    if mount_tar.is_file():
        tmp = tempfile.mkdtemp(prefix=f"{study_id}_")
        try:
            with tarfile.open(str(mount_tar), mode="r:gz") as tf:
                tf.extractall(tmp)

            # flatten single top-level dir
            entries = list(pathlib.Path(tmp).iterdir())
            if len(entries) == 1 and entries[0].is_dir():
                inner = entries[0]
                for child in inner.iterdir():
                    child.rename(pathlib.Path(tmp) / child.name)
                inner.rmdir()

            return tmp
        except Exception as e:
            logger.error("Failed to extract %s: %s", mount_tar, e)
            shutil.rmtree(tmp, ignore_errors=True)
            return None

    logger.error("Study '%s' not found at %s (neither .tar.gz nor directory)", study_id, S3_MOUNT_PATH)
    return None


def _available_study_ids() -> list[str]:
    """Read the study-list Variable at parse time to populate the Param enum."""
    try:
        return json.loads(Variable.get(STUDY_LIST_VARIABLE_KEY, default_var="[]"))
    except Exception:
        return []


def _script(script_name: str, *args: object, source_automation_env: bool = False) -> str:
    """Builds a ``{SCRIPTS_DIR}/{script} {args...}`` command, mirroring import_base._script."""
    parts = [f"{SCRIPTS_DIR}/{script_name}"]
    parts.extend(str(arg) for arg in args)
    cmd = " ".join(parts)
    if source_automation_env:
        return f"source {SCRIPTS_DIR}/automation-environment.sh && {cmd}"
    return cmd


_SAML2AWS_ENV = k8s.V1EnvVar(name="SAML2AWS_CONFIGFILE", value=f"{CREDS_DIR}/.saml2aws")


def _clickhouse_secret_env(name: str, key: str) -> k8s.V1EnvVar:
    return k8s.V1EnvVar(
        name=name,
        value_from=k8s.V1EnvVarSource(
            secret_key_ref=k8s.V1SecretKeySelector(name="hackathon-clickhouse-secret", key=key)
        ),
    )


def _pod_override(
    image: str,
    env: list,
    resources: k8s.V1ResourceRequirements | None = None,
    extra_volumes: list | None = None,
    extra_mounts: list | None = None,
) -> dict:
    return {
        "pod_override": k8s.V1Pod(
            spec=k8s.V1PodSpec(
                image_pull_secrets=[k8s.V1LocalObjectReference(name="ghcr-pull")],
                # fsGroup so the mounted Secret volume is readable with the S3 CSI driver v2
                security_context=k8s.V1PodSecurityContext(fs_group=1000),
                node_selector={"workload": "airflow-importer-dag"},
                tolerations=[
                    k8s.V1Toleration(
                        key="workload",
                        operator="Equal",
                        value="airflow-importer-dag",
                        effect="NoSchedule",
                    ),
                ],
                containers=[k8s.V1Container(
                    name="base",
                    image=image,
                    image_pull_policy="Always",
                    resources=resources,
                    env=env,
                    volume_mounts=(extra_mounts or []) + [
                        k8s.V1VolumeMount(
                            name=CREDS_VOLUME_NAME,
                            mount_path=CREDS_DIR,
                            read_only=True,
                        ),
                        k8s.V1VolumeMount(
                            name="s3-data",
                            mount_path=S3_MOUNT_PATH,
                        ),
                    ],
                )],
                volumes=(extra_volumes or []) + [
                    k8s.V1Volume(
                        name=CREDS_VOLUME_NAME,
                        secret=k8s.V1SecretVolumeSource(
                            secret_name=CREDS_SECRET_NAME,
                            default_mode=0o400,
                        ),
                    ),
                    k8s.V1Volume(
                        name="s3-data",
                        persistent_volume_claim=k8s.V1PersistentVolumeClaimVolumeSource(
                            claim_name=S3_PVC_CLAIM_NAME,
                        ),
                    ),
                ],
            )
        )
    }


_POD_OVERRIDE = _pod_override(
    image=K8S_IMAGE,
    env=[
        _SAML2AWS_ENV,
        # blank so the Airflow cluster kubeconfig does not leak into kubectl calls
        k8s.V1EnvVar(name="KUBECONFIG", value=""),
    ],
)


def _make_cbioportal_pod_override(java_opts: str | None = None, memory_request: str = "2Gi", memory_limit: str = "3Gi") -> dict:
    env = [
        k8s.V1EnvVar(name="PORTAL_HOME", value="/"),
        _clickhouse_secret_env("CLICKHOUSE_HOST", "host"),
        _clickhouse_secret_env("CLICKHOUSE_NATIVE_PORT", "native_port"),
        _clickhouse_secret_env("CLICKHOUSE_USER", "user"),
        _clickhouse_secret_env("CLICKHOUSE_PASSWORD", "password"),
        _clickhouse_secret_env("CLICKHOUSE_DB", "database"),
        _SAML2AWS_ENV,
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))

    return _pod_override(
        image=K8S_IMAGE_VALIDATE,
        env=env,
        resources=k8s.V1ResourceRequirements(
            requests={"memory": memory_request, "cpu": "1"},
            limits={"memory": memory_limit},
        ),
        extra_volumes=[
            k8s.V1Volume(
                name="app-properties-blue",
                secret=k8s.V1SecretVolumeSource(secret_name=APP_PROPERTIES_SECRET_BLUE),
            ),
            k8s.V1Volume(
                name="app-properties-green",
                secret=k8s.V1SecretVolumeSource(secret_name=APP_PROPERTIES_SECRET_GREEN),
            ),
            k8s.V1Volume(
                name="clickhouse-sql",
                secret=k8s.V1SecretVolumeSource(secret_name="hackathon-clickhouse-sql"),
            ),
        ],
        extra_mounts=[
            k8s.V1VolumeMount(
                name="app-properties-blue",
                mount_path=f"{APP_PROPERTIES_PATH}.blue",
                sub_path="application.properties",
                read_only=True,
            ),
            k8s.V1VolumeMount(
                name="app-properties-green",
                mount_path=f"{APP_PROPERTIES_PATH}.green",
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
    )


_POD_OVERRIDE_VALIDATE = _make_cbioportal_pod_override(memory_request="2Gi", memory_limit="3Gi")
_POD_OVERRIDE_IMPORT   = _make_cbioportal_pod_override(java_opts="-Xmx22g", memory_request="24Gi", memory_limit="26Gi")

_DEFAULT_ARGS = {
    "owner": "airflow",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
}


def _activate_standby_properties() -> str:
    """Determine the standby color and copy the matching application.properties into place.

    Calls get_database_currently_in_production.sh from the cbioportal-core scripts
    (available at /scripts/clickhouse_import_support/ in the core image).

    Returns the standby color string ('blue' or 'green').
    """
    import shutil
    # The core image has cbioportal-core scripts at /scripts/clickhouse_import_support/
    result = _run_and_stream([
        "/scripts/clickhouse_import_support/get_database_currently_in_production.sh",
        CLICKHOUSE_CONFIG_FILE,
    ])
    if result.returncode != 0:
        raise Exception(f"get_database_currently_in_production failed (exit {result.returncode})")
    live_db       = result.stdout.strip()  # e.g. "cbioportal_public_blue : current production database"
    live_color    = "blue" if "blue" in live_db else "green"
    standby_color = "green" if live_color == "blue" else "blue"
    src_path = f"{APP_PROPERTIES_PATH}.{standby_color}"
    dest_path = "/tmp/application.properties"
    shutil.copy(src_path, dest_path)
    shutil.copy("/clickhouse.sql", "/tmp/clickhouse.sql")
    os.environ["PORTAL_HOME"] = "/tmp"
    logging.info("Activated %s application.properties (live=%s, standby=%s) -> %s", standby_color, live_color, standby_color, dest_path)
    return standby_color


@dag(
    dag_id="import_public_hackathon",
    default_args=_DEFAULT_ARGS,
    start_date=datetime(2026, 1, 1),
    schedule=None,  # trigger-only: a run swaps production traffic
    catchup=False,
    max_active_runs=1,
    render_template_as_native_obj=True,
    params={
        "database": Param(
            "public",
            type="string",
            enum=["containerized", "public"],
            description="Which database environment to import into.",
            title="Database",
        ),
        "cancer_study_ids": Param(
            [],
            type="array",
            examples=_available_study_ids(),
            description="Select one or more cancer study IDs to import. Run refresh_study_list to update the list.",
            title="Cancer Study IDs",
        ),
    },
)
def import_public_hackathon():
    @task(executor_config=_POD_OVERRIDE)
    def verify_studies_exist(study_ids: list[str]) -> list[str]:
        """All-or-nothing: every requested study must exist on the S3 mount, else fail the DAG."""
        import pathlib

        # render_template_as_native_obj may render the array Param as its string repr
        if isinstance(study_ids, str):
            import ast
            try:
                study_ids = json.loads(study_ids)
            except (json.JSONDecodeError, ValueError):
                study_ids = ast.literal_eval(study_ids)
        study_ids = [s.strip() for s in (study_ids or []) if s and s.strip()]
        if not study_ids:
            raise AirflowException("No study IDs provided")

        mount = pathlib.Path(S3_MOUNT_PATH)
        missing = [
            s for s in study_ids
            if not (mount / f"{s}.tar.gz").is_file() and not (mount / s).is_dir()
        ]
        if missing:
            raise AirflowException(f"Studies not found at {S3_MOUNT_PATH}: {missing}")
        return study_ids

    t_verify_cluster_state = BashOperator(
        task_id="verify_cluster_state",
        # unset IRSA vars so aws uses the saml2aws profile
        bash_command="unset AWS_ROLE_ARN AWS_WEB_IDENTITY_TOKEN_FILE; " + _script(
            "airflow-verify-management.sh",
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
            COLOR_SWAP_CONFIG_FILE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    t_clone_live_database = BashOperator(
        task_id="clone_live_database_into_standby",
        bash_command=_script(
            "airflow-clone-db.sh",
            IMPORTER,
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    @task(executor_config=_POD_OVERRIDE_VALIDATE)
    def pull_and_validate_study(study_id: str) -> str | None:
        import pathlib

        try:
            local_dir = _study_data_path(study_id)
            if local_dir is None:
                return None

            log_dir = f"/tmp/validate_logs/{study_id}"
            os.makedirs(log_dir, exist_ok=True)
            result = _run_and_stream(
                [sys.executable, VALIDATE_SCRIPT_PATH, "-l", local_dir, "-n", "-html", log_dir],
            )
            for log_file in pathlib.Path(log_dir).glob("log-validate-studies-*.txt"):
                logging.info("=== Validation log: %s ===\n%s", log_file.name, log_file.read_text())
            if result.returncode not in (0, 3):
                logging.error("Validation failed for %s (exit %d)", study_id, result.returncode)
                return None
            return study_id
        except Exception as e:
            logging.error("Validation failed for %s: %s", study_id, e)
            return None

    @task(executor_config=_POD_OVERRIDE)
    def collect_valid_studies(results: list) -> list[str]:
        valid = [sid for sid in (results or []) if sid is not None]
        if not valid:
            raise AirflowSkipException("No studies passed validation — skipping import")
        logging.info("Studies passing validation: %s", valid)
        return valid

    @task(executor_config=_POD_OVERRIDE_IMPORT)
    def import_into_standby_database(valid_studies: list[str]):
        _activate_standby_properties()
        if not valid_studies:
            logging.info("No valid studies to import — exiting.")
            return

        failed = []
        for study_id in valid_studies:
            local_dir = _study_data_path(study_id)
            if local_dir is None:
                failed.append(study_id)
                continue

            result = _run_and_stream(
                [sys.executable, IMPORT_SCRIPT_PATH,
                 "-s", local_dir,
                 "-n",
                 "-o",
                 "--no-derive-tables"],
            )
            if result.returncode != 0:
                logging.error("Import failed for %s (exit %d)", study_id, result.returncode)
                failed.append(study_id)

        if failed:
            raise Exception(f"Import failed for {len(failed)} study/studies: {failed}")

    @task(executor_config=_POD_OVERRIDE_IMPORT)
    def create_derived_tables_in_standby_database():
        _activate_standby_properties()
        rebuild = _run_and_stream(
            [sys.executable, IMPORT_SCRIPT_PATH, "derive-tables"],
        )
        if rebuild.returncode != 0:
            raise Exception(f"Derived table rebuild failed (exit {rebuild.returncode})")

    t_transfer_deployment_color = BashOperator(
        task_id="transfer_deployment_color",
        bash_command="unset AWS_ROLE_ARN AWS_WEB_IDENTITY_TOKEN_FILE; " + _script(
            "airflow-transfer-deployment.sh",
            SCRIPTS_DIR,
            CLICKHOUSE_CONFIG_FILE,
            COLOR_SWAP_CONFIG_FILE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    t_set_import_complete = BashOperator(
        task_id="set_import_complete",
        bash_command=_script(
            "set_update_process_state.sh",
            CLICKHOUSE_CONFIG_FILE,
            "complete",
            source_automation_env=True,
        ),
        executor_config=_POD_OVERRIDE,
    )

    t_set_import_abandoned = BashOperator(
        task_id="set_import_abandoned",
        bash_command=_script(
            "set_update_process_state.sh",
            CLICKHOUSE_CONFIG_FILE,
            "abandoned",
            source_automation_env=True,
        ),
        executor_config=_POD_OVERRIDE,
        trigger_rule=TriggerRule.ONE_FAILED,
    )

    t_found_studies  = verify_studies_exist("{{ params.cancer_study_ids }}")
    t_pull_and_validate = pull_and_validate_study.partial().expand(study_id=t_found_studies)
    t_collect_valid  = collect_valid_studies(t_pull_and_validate)
    t_import         = import_into_standby_database(t_collect_valid)
    t_create_derived_tables = create_derived_tables_in_standby_database()

    t_found_studies >> t_verify_cluster_state >> [t_clone_live_database, t_pull_and_validate]
    t_clone_live_database >> t_import
    (
        t_import
        >> t_create_derived_tables
        >> t_transfer_deployment_color
        >> t_set_import_complete
    )

    [
        t_found_studies,
        t_verify_cluster_state,
        t_clone_live_database,
        t_pull_and_validate,
        t_collect_valid,
        t_import,
        t_create_derived_tables,
        t_transfer_deployment_color,
        t_set_import_complete,
    ] >> t_set_import_abandoned


import_public_hackathon()
