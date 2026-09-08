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
GET_DB_IN_PROD_SCRIPT       = f"{SCRIPTS_DIR}/get_database_currently_in_production.sh"

# Both environments' config files live in the pipelines-credentials secret under
# env-prefixed keys; params.database ('containerized' or 'public') selects the set.
MANAGE_PROPS_TEMPLATE = f"{CREDS_DIR}/{{{{ params.database }}}}.manage.properties"
COLOR_SWAP_TEMPLATE   = f"{CREDS_DIR}/{{{{ params.database }}}}.color-swap.yaml"


def _manage_props_path(env: str) -> str:
    return f"{CREDS_DIR}/{env}.manage.properties"

S3_MOUNT_PATH = "/mnt/s3-data"
S3_PVC_CLAIM_NAME = "databricks-s3-pvc"


def _study_prefix(params: dict | None) -> str:
    """Normalize params.study_prefix to a single relative path segment ('' = bucket root)."""
    prefix = str((params or {}).get("study_prefix") or "").strip().strip("/")
    if prefix and (".." in prefix or "/" in prefix):
        raise AirflowException(f"study_prefix must be a single top-level folder name, got {prefix!r}")
    return prefix


def _study_mount(prefix: str = "") -> "pathlib.Path":
    import pathlib
    return pathlib.Path(S3_MOUNT_PATH) / prefix if prefix else pathlib.Path(S3_MOUNT_PATH)

# Task IDs that may be listed in the skip_tasks param (dry-run support).
SKIPPABLE_TASK_IDS = (
    "clone_live_database_into_standby",
    "import_into_standby_database",
    "create_derived_tables_in_standby_database",
    "transfer_deployment_color",
)


def _bash_skip_guard(task_id: str) -> str:
    """Jinja prefix for a BashOperator command: exit 99 (BashOperator's skip exit
    code) when the task is listed in params.skip_tasks."""
    return (
        f"{{% if '{task_id}' in params.skip_tasks %}}"
        f"echo '{task_id} listed in skip_tasks - skipping'; exit 99"
        f"{{% endif %}}\n"
    )


def _skip_if_requested(task_id: str, params: dict | None) -> None:
    if task_id in ((params or {}).get("skip_tasks") or []):
        raise AirflowSkipException(f"{task_id} listed in skip_tasks - skipping")


def _study_data_path(study_id: str, prefix: str = "") -> str | None:
    """Resolve a study under the S3 mount (optionally inside a top-level folder);
    extract tarballs to a temp dir. None if absent."""
    import pathlib
    import tarfile
    import tempfile
    import shutil

    mount = _study_mount(prefix)
    mount_tar = mount / f"{study_id}.tar.gz"
    mount_dir = mount / study_id

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

    logger.error("Study '%s' not found at %s (neither .tar.gz nor directory)", study_id, mount)
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


def _load_properties(path: str) -> dict[str, str]:
    """Parse a simple key=value properties file (comments and blank lines ignored)."""
    props: dict[str, str] = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            props[key.strip()] = value.strip()
    return props


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
        _SAML2AWS_ENV,
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))

    # application.properties, clickhouse.sql and manage properties for both
    # environments come from the pipelines-credentials secret (mounted at
    # CREDS_DIR) under env-prefixed keys — no extra per-environment mounts.
    return _pod_override(
        image=K8S_IMAGE_VALIDATE,
        env=env,
        resources=k8s.V1ResourceRequirements(
            requests={"memory": memory_request, "cpu": "1"},
            limits={"memory": memory_limit},
        ),
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


def _activate_standby_properties(env: str) -> str:
    """Determine the standby color for the given environment ('containerized' or
    'public') and copy the matching application.properties + derived-table SQL into
    /tmp, pointing PORTAL_HOME and the CLICKHOUSE_* env vars at the standby database.

    Calls get_database_currently_in_production.sh from the cbioportal-core scripts
    (available at /scripts/clickhouse_import_support/ in the core image).

    Returns the standby color string ('blue' or 'green').
    """
    import shutil
    manage_props_path = _manage_props_path(env)
    # The core image has cbioportal-core scripts at /scripts/clickhouse_import_support/
    result = _run_and_stream([
        "/scripts/clickhouse_import_support/get_database_currently_in_production.sh",
        manage_props_path,
    ])
    if result.returncode != 0:
        raise Exception(f"get_database_currently_in_production failed (exit {result.returncode})")
    live_db       = result.stdout.strip()  # e.g. "cbioportal_public_blue : current production database"
    live_color    = "blue" if "blue" in live_db else "green"
    standby_color = "green" if live_color == "blue" else "blue"
    src_path = f"{CREDS_DIR}/{env}.application.properties.{standby_color}"
    dest_path = "/tmp/application.properties"
    shutil.copy(src_path, dest_path)
    shutil.copy(f"{CREDS_DIR}/{env}.clickhouse.sql", "/tmp/clickhouse.sql")
    os.environ["PORTAL_HOME"] = "/tmp"

    # Point the derive-tables clickhouse client at the standby database.
    # rebuild_derived_tables.py reads CLICKHOUSE_* env vars; a fixed value would
    # ignore the blue/green color, so derive them from the manage properties here.
    ch_props = _load_properties(manage_props_path)
    standby_db = ch_props[f"clickhouse_{standby_color}_database_name"]
    os.environ["CLICKHOUSE_HOST"] = ch_props["clickhouse_server_host_name"]
    os.environ["CLICKHOUSE_NATIVE_PORT"] = ch_props["clickhouse_server_port"]
    os.environ["CLICKHOUSE_USER"] = ch_props["clickhouse_server_username"]
    os.environ["CLICKHOUSE_PASSWORD"] = ch_props["clickhouse_server_password"]
    os.environ["CLICKHOUSE_DB"] = standby_db
    logging.info(
        "=== TARGET ENVIRONMENT: %s | live=%s standby=%s | standby db=%s ===",
        env, live_color, standby_color, standby_db,
    )
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
            "containerized",
            type="string",
            enum=["containerized", "public"],
            description=(
                "Which database environment to import into. 'containerized' targets the "
                "test databases and containerized.cbioportal.org; 'public' targets the "
                "REAL public databases — its traffic swap moves www.cbioportal.org. "
                "Defaults to the safe environment; select 'public' deliberately."
            ),
            title="Database",
        ),
        "cancer_study_ids": Param(
            [],
            type="array",
            examples=_available_study_ids(),
            description="Select one or more cancer study IDs to import. Run refresh_study_list to update the list.",
            title="Cancer Study IDs",
        ),
        "study_prefix": Param(
            "",
            type="string",
            examples=["", "staging"],
            description=(
                "Top-level folder in the S3 bucket to read studies from. Empty = bucket "
                "root (the studies the scheduled public import uses). 'staging' reads "
                "s3://<bucket>/staging/, a scratch area for dry runs of pre-processed or "
                "otherwise modified studies that must not affect the real import."
            ),
            title="Study Prefix",
        ),
        "skip_tasks": Param(
            [],
            type="array",
            examples=list(SKIPPABLE_TASK_IDS),
            description=(
                "Task IDs to skip this run (dry-run support). Skipping "
                "transfer_deployment_color imports into the standby database without "
                "swapping production traffic; the run then finishes in the 'abandoned' "
                "management state so the next run re-clones the standby database."
            ),
            title="Skip Tasks",
        ),
    },
)
def import_public_hackathon():
    @task(executor_config=_POD_OVERRIDE)
    def verify_studies_exist(study_ids: list[str], params: dict | None = None) -> list[str]:
        """All-or-nothing: every requested study must exist on the S3 mount
        (under params.study_prefix, if set), else fail the DAG."""

        # Fail fast on typos: a misspelled skip_tasks entry would silently NOT skip
        # its task, which for transfer_deployment_color means an unintended traffic swap.
        unknown = set((params or {}).get("skip_tasks") or []) - set(SKIPPABLE_TASK_IDS)
        if unknown:
            raise AirflowException(f"skip_tasks contains unknown task ids: {sorted(unknown)}")

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

        mount = _study_mount(_study_prefix(params))
        missing = [
            s for s in study_ids
            if not (mount / f"{s}.tar.gz").is_file() and not (mount / s).is_dir()
        ]
        if missing:
            raise AirflowException(f"Studies not found at {mount}: {missing}")
        return study_ids

    t_verify_cluster_state = BashOperator(
        task_id="verify_cluster_state",
        # unset IRSA vars so aws uses the saml2aws profile
        bash_command="unset AWS_ROLE_ARN AWS_WEB_IDENTITY_TOKEN_FILE; " + _script(
            "airflow-verify-management.sh",
            SCRIPTS_DIR,
            MANAGE_PROPS_TEMPLATE,
            COLOR_SWAP_TEMPLATE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    t_clone_live_database = BashOperator(
        task_id="clone_live_database_into_standby",
        bash_command=_bash_skip_guard("clone_live_database_into_standby") + _script(
            "airflow-clone-db.sh",
            IMPORTER,
            SCRIPTS_DIR,
            MANAGE_PROPS_TEMPLATE,
        ),
        executor_config=_POD_OVERRIDE,
    )

    @task(executor_config=_POD_OVERRIDE_VALIDATE)
    def pull_and_validate_study(study_id: str, params: dict | None = None) -> str | None:
        import pathlib

        try:
            local_dir = _study_data_path(study_id, _study_prefix(params))
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
            # Fail (not skip): downstream tasks use NONE_FAILED so that explicit
            # skip_tasks skips flow through them — a skip here would let
            # transfer_deployment_color swap traffic onto an unmodified clone.
            raise AirflowException("No studies passed validation")
        logging.info("Studies passing validation: %s", valid)
        return valid

    @task(executor_config=_POD_OVERRIDE_IMPORT, trigger_rule=TriggerRule.NONE_FAILED)
    def import_into_standby_database(valid_studies: list[str], params: dict | None = None):
        _skip_if_requested("import_into_standby_database", params)
        _activate_standby_properties(params["database"])
        if not valid_studies:
            logging.info("No valid studies to import — exiting.")
            return

        failed = []
        for study_id in valid_studies:
            local_dir = _study_data_path(study_id, _study_prefix(params))
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

    @task(executor_config=_POD_OVERRIDE_IMPORT, trigger_rule=TriggerRule.NONE_FAILED)
    def create_derived_tables_in_standby_database(params: dict | None = None):
        _skip_if_requested("create_derived_tables_in_standby_database", params)
        _activate_standby_properties(params["database"])
        rebuild = _run_and_stream(
            [sys.executable, IMPORT_SCRIPT_PATH, "derive-tables",
             "--derived-table-sql", "/tmp/clickhouse.sql"],
        )
        if rebuild.returncode != 0:
            raise Exception(f"Derived table rebuild failed (exit {rebuild.returncode})")

    t_transfer_deployment_color = BashOperator(
        task_id="transfer_deployment_color",
        bash_command=_bash_skip_guard("transfer_deployment_color")
        + "unset AWS_ROLE_ARN AWS_WEB_IDENTITY_TOKEN_FILE; " + _script(
            "airflow-transfer-deployment.sh",
            SCRIPTS_DIR,
            MANAGE_PROPS_TEMPLATE,
            COLOR_SWAP_TEMPLATE,
        ),
        executor_config=_POD_OVERRIDE,
        trigger_rule=TriggerRule.NONE_FAILED,
    )

    @task(executor_config=_POD_OVERRIDE, trigger_rule=TriggerRule.NONE_FAILED)
    def finalize_import_state(ti=None, params: dict | None = None):
        """Set the management DB end state: 'complete' only if production traffic was
        actually swapped, otherwise 'abandoned' so the next run re-clones standby."""
        transfer_state = ti.get_dagrun().get_task_instance("transfer_deployment_color").state
        state = "complete" if transfer_state == "success" else "abandoned"
        logging.info(
            "transfer_deployment_color finished in state %r -> setting update process state %r",
            transfer_state, state,
        )
        _run_and_stream(
            ["bash", "-c", _script(
                "set_update_process_state.sh",
                _manage_props_path(params["database"]),
                state,
                source_automation_env=True,
            )],
            check=True,
        )

    t_set_import_abandoned = BashOperator(
        task_id="set_import_abandoned",
        bash_command=_script(
            "set_update_process_state.sh",
            MANAGE_PROPS_TEMPLATE,
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
    t_finalize       = finalize_import_state()

    t_found_studies >> t_verify_cluster_state >> [t_clone_live_database, t_pull_and_validate]
    t_clone_live_database >> t_import
    (
        t_import
        >> t_create_derived_tables
        >> t_transfer_deployment_color
        >> t_finalize
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
        t_finalize,
    ] >> t_set_import_abandoned


import_public_hackathon()
