"""Manual containerized imports for the public v7 preview."""

from pathlib import Path
import sys

from airflow.decorators import task
from airflow.exceptions import AirflowException, AirflowSkipException
from airflow.utils.trigger_rule import TriggerRule
from dags.containerized_workflow import SKIPPABLE_TASK_IDS as STAGES, wire_import
from kubernetes.client import models as k8s

# Airflow may discover this DAG below the git-sync root. Ensure its companion
# modules resolve in scheduler and KubernetesExecutor task processes alike.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import public_v7_runtime as runtime


def pod(image, memory, java_opts=None):
    env = [
        k8s.V1EnvVar(name="SAML2AWS_CONFIGFILE", value=str(runtime.CREDS / ".saml2aws")),
        k8s.V1EnvVar(name="KUBECONFIG", value=""),
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))
    mounts = [
        ("v7-credentials", str(runtime.CREDS), True),
        ("v7-config", "/v7-config", True),
        ("studies", str(runtime.INPUT), True),
    ]
    return {"pod_override": k8s.V1Pod(spec=k8s.V1PodSpec(
        service_account_name="airflow-worker",
        security_context=k8s.V1PodSecurityContext(fs_group=1000),
        image_pull_secrets=[k8s.V1LocalObjectReference(name="ghcr-pull")],
        node_selector={"workload": "airflow-importer-dag", "kubernetes.io/arch": "amd64"},
        tolerations=[k8s.V1Toleration(key="workload", operator="Equal", value="airflow-importer-dag", effect="NoSchedule")],
        containers=[k8s.V1Container(
            name="base", image=image, env=env,
            resources=k8s.V1ResourceRequirements(requests={"cpu": "1", "memory": memory}, limits={"memory": memory}),
            volume_mounts=[k8s.V1VolumeMount(name=n, mount_path=p, read_only=ro) for n, p, ro in mounts],
        )],
        volumes=[
            k8s.V1Volume(name="v7-credentials", secret=k8s.V1SecretVolumeSource(secret_name="public-v7-import-credentials", default_mode=0o440, optional=True)),
            k8s.V1Volume(name="v7-config", config_map=k8s.V1ConfigMapVolumeSource(name="public-v7-config", optional=True)),
            k8s.V1Volume(name="studies", persistent_volume_claim=k8s.V1PersistentVolumeClaimVolumeSource(claim_name="cdsi-curation-hgnc-s3-pvc", read_only=True)),
            # The deployed worker template supplies airflow-logs at
            # /opt/airflow/logs. Airflow extends these lists during merging;
            # redefining it here would produce duplicate volumes/mounts.
        ],
    ))}


# The scheduler needs the images before task pods can read the ConfigMap.
CMO = "ghcr.io/cbioportal/containerized-importer-cmo@sha256:1286f2d45ca7dfdf4643eb3c3bad560af94e941647a06e4fedbafcdae5e6dc9b"
CORE = "ghcr.io/cbioportal/containerized-importer-core@sha256:b0b9f7b30db842a20dfbf145aeadaeb04419e6854b4a6b8c4633948219e83d98"
INFRA = pod(CMO, "3Gi")
VALIDATE = pod(CORE, "3Gi")
IMPORT = pod(CORE, "26Gi", "-Xmx22g")


def skip_stage(stage, params):
    if stage in params["skip_tasks"]:
        raise AirflowSkipException(f"{stage} listed in skip_tasks - skipping")


def build_v7_tasks():
    @task(executor_config=INFRA)
    def prepare(params=None, run_id=None):
        return runtime.prepare(params["cancer_study_ids"], run_id, CORE, CMO)

    @task(executor_config=INFRA)
    def selected(plan):
        return list(plan["studies"])

    @task(executor_config=VALIDATE, max_active_tis_per_dag=4)
    def validate(study, plan):
        return runtime.validate_study(study, plan)

    @task(executor_config=INFRA)
    def verify_portal(plan):
        runtime.verify_portal(plan)

    @task(executor_config=INFRA)
    def begin(plan, params=None):
        if set(params["skip_tasks"]) != set(STAGES):
            runtime.begin(plan)
            return True
        return False

    @task(executor_config=INFRA)
    def clone(plan, params=None):
        skip_stage("clone_live_database_into_standby", params)
        runtime.clone(plan)

    @task(executor_config=IMPORT, trigger_rule=TriggerRule.NONE_FAILED)
    def import_studies(plan, params=None):
        skip_stage("import_into_standby_database", params)
        runtime.import_studies(plan)

    @task(executor_config=IMPORT, trigger_rule=TriggerRule.NONE_FAILED)
    def derive(plan, params=None):
        skip_stage("create_derived_tables_in_standby_database", params)
        runtime.derive(plan, publish="import_into_standby_database" not in params["skip_tasks"])

    @task(executor_config=INFRA, trigger_rule=TriggerRule.NONE_FAILED)
    def transfer(plan, params=None):
        skip_stage("transfer_deployment_color", params)
        runtime.transfer(plan)

    @task(executor_config=INFRA, trigger_rule=TriggerRule.NONE_FAILED)
    def complete(plan, params=None):
        skipped = set(params["skip_tasks"])
        if skipped == set(STAGES):
            return
        if "transfer_deployment_color" in skipped:
            runtime.abandon(plan)
        else:
            runtime.complete(plan)

    @task(executor_config=INFRA, trigger_rule=TriggerRule.ONE_FAILED)
    def failure_cleanup(dag_run=None, ti=None):
        plan = ti.xcom_pull(task_ids="prepare")
        if not plan:
            raise AirflowException("V7 import failed before a run plan was created")
        states = {x.task_id: x.state for x in dag_run.get_task_instances()}
        runtime.failure_cleanup(
            plan, began=states.get("begin") == "success" and ti.xcom_pull(task_ids="begin"),
            transfer_started=states.get("transfer") not in {None, "upstream_failed", "skipped", "scheduled", "queued"},
        )
        # A successful cleanup leaf must not mask an upstream task failure.
        raise AirflowException("V7 import failed; pre-promotion cleanup completed")

    plan = prepare()
    studies = selected(plan)
    checked = validate.partial(plan=plan).expand(study=studies)
    verified = verify_portal(plan)
    started = begin(plan)
    cloned = clone(plan)
    imported = import_studies(plan)
    derived = derive(plan)
    transferred = transfer(plan)
    completed = complete(plan)
    checked >> verified
    verified >> started
    wire_import(started, cloned, imported, derived, transferred, completed)
    [plan, studies, checked, verified, started, cloned, imported, derived, transferred, completed] >> failure_cleanup()
    return plan
