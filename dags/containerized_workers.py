"""Select the import worker after KubernetesExecutor knows the DAG run."""
from kubernetes.client import models as k8s
from dags import public_v7_workers
from dags.study_sources import S3_SOURCES

K8S_IMAGE            = "ghcr.io/cbioportal/containerized-importer-cmo@sha256:fcc3506549bf5607283dc991ef064ad1091157be94ad3b9aac78931f3de76158"
# Core image combining PRs #183 and #203 with Airflow-compatible dependencies and the derived OPTIMIZE retry from core PR #229.
K8S_IMAGE_VALIDATE   = "ghcr.io/cbioportal/containerized-importer-core@sha256:c7677619303911d726fa900c5210ba01819d5f9d0c73c6691f22ad4b61fa145d"
CREDS_DIR = "/data/portal-cron/pipelines-credentials"
CREDS_SECRET_NAME = "pipelines-credentials"
CREDS_VOLUME_NAME = "pipelines-credentials"
_SAML2AWS_ENV = k8s.V1EnvVar(name="SAML2AWS_CONFIGFILE", value=f"{CREDS_DIR}/.saml2aws")


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
                service_account_name="airflow-worker",
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
                    ] + [
                        k8s.V1VolumeMount(name=f"study-source-{i}", mount_path=source['mount'], read_only=True)
                        for i, source in enumerate(S3_SOURCES.values())
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
                ] + [
                    k8s.V1Volume(name=f"study-source-{i}", persistent_volume_claim=
                        k8s.V1PersistentVolumeClaimVolumeSource(claim_name=source['claim'], read_only=True))
                    for i, source in enumerate(S3_SOURCES.values())
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


def _make_cbioportal_pod_override(java_opts: str | None = None, memory_request: str = "2Gi", memory_limit: str = "3Gi", jdbc_diagnostics: bool = False) -> dict:
    env = [
        k8s.V1EnvVar(name="PORTAL_HOME", value="/"),
        _SAML2AWS_ENV,
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))
    if jdbc_diagnostics:
        # Reaches Java subprocesses launched by the Python importer, too.
        env.append(k8s.V1EnvVar(name="JAVA_TOOL_OPTIONS", value="-Dcbio.jdbc.diagnostics=true"))

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
_POD_OVERRIDE_IMPORT   = _make_cbioportal_pod_override(java_opts="-Xmx22g", memory_request="24Gi", memory_limit="26Gi", jdbc_diagnostics=True)


def executor_config(role):
    """Carry the two pod specs to the scheduler, which selects one by run target."""
    import json
    from airflow.providers.cncf.kubernetes.pod_generator import PodGenerator

    profiles = {
        "infra": (_POD_OVERRIDE, public_v7_workers.INFRA),
        "validate": (_POD_OVERRIDE_VALIDATE, public_v7_workers.VALIDATE),
        "import": (_POD_OVERRIDE_IMPORT, public_v7_workers.IMPORT),
    }
    public, preview = profiles[role]
    specs = {name: PodGenerator.serialize_pod(config["pod_override"])
             for name, config in (("public", public), ("public_v7", preview))}
    return {"pod_override": k8s.V1Pod(
        metadata=k8s.V1ObjectMeta(annotations={"cbioportal.org/import-worker-profiles": json.dumps(specs)}),
        spec=k8s.V1PodSpec(containers=[k8s.V1Container(name="base")]),
    )}
