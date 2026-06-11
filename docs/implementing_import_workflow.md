# Implementing the Import Workflow Redesign

> This document covers the *how*. For the rationale behind each decision, see [redesigning_import_workflow.md](redesigning_import_workflow.md).

## Contents

- [External Service Clients](#external-service-clients)
- [Docker Image](#docker-image)
- [Configuration File Spec](#configuration-file-spec)
- [Re-entrancy: `on_execute_callback`](#re-entrancy-on_execute_callback)
- [Failure Handling: `on_failure_callback`](#failure-handling-on_failure_callback)
- [Execution Target: Kubernetes Pods](#execution-target-kubernetes-pods)
  - [KubernetesPodOperator vs. KubernetesExecutor](#kubernetespodoperator-vs-kubernetesexecutor)
  - [Implementation](#implementation)
- [Data Sharing Between Steps](#data-sharing-between-steps)
- [Testing](#testing)
- [Migration Plan](#migration-plan)

## External Service Clients

Replace shell script invocations with direct Python client calls:

| Service | Library |
|---|---|
| ClickHouse | `clickhouse-connect` |
| Kubernetes | `kubernetes` (official Python client) |
| S3 | `boto3` |

---

## Docker Image

Most tasks run in a shared base image. It should be as slim as possible — a minimal Python 3.11 image with only the dependencies actually needed at runtime:

```
python:3.11-slim
  ├── clickhouse-connect
  ├── kubernetes
  ├── boto3
  └── apache-airflow (client only, not the full server install)
```

The image is built from this repository and pushed to Docker Hub. Each importer task references it by digest (or a pinned tag) via `ImporterConfig.default_image`.

**The Java importer complication.** The current `import_direct_to_clickhouse` step runs a cBioPortal importer JAR. Two options:

1. **Separate image for the import step** — a heavier image that includes a JRE, used only for `import_into_standby_database`. The base image stays slim. This is the preferred approach: most tasks use the lightweight image, only the import step pays the JRE overhead.
2. **Include JRE in the base image** — simpler, but bloats the image every task pulls.

The `task_overrides` field in `ImporterConfig` (see below) is the mechanism for pointing a specific task at a different image.

---

## Configuration File Spec

The current `ImporterConfig` carries SSH-era fields (`target_nodes`, `data_nodes`, `scripts_dir`, `creds_dir`, `db_properties_filename`, etc.) that have no meaning in a Kubernetes pod world. These are replaced by pod-oriented fields.

A worked example for the GENIE importer is in [example_importer_config.yaml](example_importer_config.yaml).

Proposed shape:

```python
@dataclass(frozen=True, kw_only=True)
class TaskOverride:
    image: Optional[str] = None           # override the default image for this task
    cpu: Optional[str] = None             # e.g. "4"
    memory: Optional[str] = None          # e.g. "16Gi"
    run_on_airflow_server: bool = False   # skip pod, run directly on the Airflow server

@dataclass(frozen=True, kw_only=True)
class ImporterConfig:
    dag_id: str
    description: str
    importer: str
    tags: Sequence[str]
    default_image: str                              # Docker Hub image used by most tasks
    task_names: Sequence[str]
    source_repositories: Sequence[str]              # repos this importer pulls from
    task_overrides: Mapping[str, TaskOverride] = field(default_factory=dict)
    params: Mapping[str, Param] = field(default_factory=dict)
    wire_dependencies: WireDependencies
    pool: Optional[str] = None
    schedule_interval: Optional[str] = None
```

**Absorbing `importer-data-source-manager-config.yaml`.** The host concept (persistent machines, per-host clone paths, SSH keys) goes away entirely — pods handle their own working directories and authenticate via IAM roles. The per-importer source repository list moves into `ImporterConfig.source_repositories`. ClickHouse credentials and color-swap config move into Airflow Connections / Variables or AWS Secrets Manager. See [example_importer_config.yaml](example_importer_config.yaml) for a worked example.

---

## Re-entrancy: `on_execute_callback`

Airflow's `on_execute_callback` fires before a task's `execute()` on every run — including when a task is manually cleared and re-queued from the UI (unlike `on_retry_callback`, which only fires on automatic retries).

The callback is defined as a closure inside `build_import_dag`, capturing `config` directly rather than trying to deserialize it from `dag_run.conf`:

```python
def build_import_dag(config: ImporterConfig) -> DAG:

    def _set_running(context):
        set_import_state(config, "running")

    default_args = {
        **_DEFAULT_ARGS,
        "on_execute_callback": [_set_running],
    }
    ...
```

**Exclusions:** tasks running under `TriggerRule.ALL_DONE` (e.g., `send_slack_notifications`, `cleanup_data`) must be excluded — resetting state to `running` before a cleanup step is incorrect. Pass `on_execute_callback=[]` explicitly on those tasks to override the default.

---

## Failure Handling: `on_failure_callback`

Replace the `set_import_abandoned` sentinel task and `watcher` task with an `on_failure_callback`. Like the re-entrancy callback, it is defined as a closure inside `build_import_dag`:

```python
def build_import_dag(config: ImporterConfig) -> DAG:

    def _set_abandoned(context):
        set_import_state(config, "abandoned")

    default_args = {
        **_DEFAULT_ARGS,
        "on_failure_callback": [_set_abandoned, dag_failure_slack_webhook_notification],
    }
    ...
```

Setting state to `abandoned` is idempotent, so it is safe for the callback to fire multiple times if several tasks fail in the same run.

This eliminates:
- The `set_import_abandoned` task and its manual `ONE_FAILED` dependency wiring
- The `watcher` task

---

## Execution Target: Kubernetes Pods

### Implementation

Replace `SSHOperator` with either `KubernetesPodOperator` or a `PythonOperator` with `executor_config` overrides, depending on the approach chosen above. `build_import_dag` reads `ImporterConfig.task_overrides` to configure each task.

**Option A — `KubernetesPodOperator`:**

```python
def _build_task(name: str, config: ImporterConfig) -> BaseOperator:
    override = config.task_overrides.get(name, TaskOverride())

    if override.run_on_airflow_server:
        return PythonOperator(task_id=name, python_callable=TASK_FN_MAP[name])

    return KubernetesPodOperator(
        task_id=name,
        image=override.image or config.default_image,
        container_resources=k8s.V1ResourceRequirements(
            requests={
                k: v for k, v in
                {"cpu": override.cpu, "memory": override.memory}.items()
                if v is not None
            }
        ),
        ...
    )
```

**Option B — `KubernetesExecutor` + `executor_config`:**

```python
def _build_task(name: str, config: ImporterConfig) -> BaseOperator:
    override = config.task_overrides.get(name, TaskOverride())

    pod_override = k8s.V1Pod(
        spec=k8s.V1PodSpec(
            containers=[k8s.V1Container(
                name="base",
                image=override.image or config.default_image,
                resources=k8s.V1ResourceRequirements(
                    requests={
                        k: v for k, v in
                        {"cpu": override.cpu, "memory": override.memory}.items()
                        if v is not None
                    }
                ),
            )]
        )
    )

    return PythonOperator(
        task_id=name,
        python_callable=TASK_FN_MAP[name],
        executor_config={"pod_override": pod_override},
    )
```

Option B is more verbose per task but avoids double-pod overhead and requires no new operator. The `run_on_airflow_server` field in `TaskOverride` is only meaningful under Option A and can be dropped if Option B is chosen.

---

## Data Sharing Between Steps

**Lightweight signals** (e.g., database color, status flags): use Airflow XCom.

```python
context["ti"].xcom_push(key="db_color", value="blue")
color = context["ti"].xcom_pull(task_ids="verify_cluster_state", key="db_color")
```

**Study data** is too large for XCom — see the co-location vs. shared EFS tradeoff in the design doc.

---

## Testing

Because each step is a plain Python function, unit tests can mock external clients directly. Integration tests that require a real ClickHouse instance or a live cluster should be tagged `pytest.mark.integration` and run in CI against a test environment, not mocked.

---

## Migration Plan

The new pipeline is written as a fresh DAG — not an incremental port of the existing one. The existing SSHOperator-based DAGs continue running in parallel until confidence in the new DAG is established, at which point the old ones are deleted.

The main risk of co-existence is two DAGs running against the same importer simultaneously. Mutual exclusion is handled via Airflow pools — see the design decisions.

Suggested order:
1. Stand up the pod infrastructure: Docker Hub images, K8s namespace, IAM roles, secrets in Secrets Manager. Validate end-to-end with a single no-op pod task.
2. Write the new DAG for Triage (lowest blast radius). Implement each step as a Python function. Keep the old Triage DAG paused.
3. Run the new Triage DAG in staging. Once stable, enable it in production with the old DAG paused.
4. Port GENIE and Public DAGs in the same way.
5. Delete the old DAGs and remove the SSHOperator dependency once all importers are running on the new pipeline.
