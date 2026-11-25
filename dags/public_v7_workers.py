"""Kubernetes worker configuration for the HGNC preview."""
from kubernetes.client import models as k8s


def pod(image, memory, java_opts=None):
    env = [
        k8s.V1EnvVar(name="SAML2AWS_CONFIGFILE", value="/data/portal-cron/pipelines-credentials/.saml2aws"),
        k8s.V1EnvVar(name="KUBECONFIG", value=""),
    ]
    if java_opts:
        env.append(k8s.V1EnvVar(name="JAVA_OPTS", value=java_opts))
    mounts = [
        ("v7-credentials", "/data/portal-cron/pipelines-credentials", True),
        ("v7-config", "/v7-config", True),
        ("studies", "/mnt/hgnc-studies", True),
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
