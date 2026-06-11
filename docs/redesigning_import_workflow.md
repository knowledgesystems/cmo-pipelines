# Redesigning the Import Workflow

> See [implementing_import_workflow.md](implementing_import_workflow.md) for how each decision below translates to code.

## Contents

- [Current Architecture](#current-architecture)
- [Goals](#goals)
- [Proposed Import Flow](#proposed-import-flow)
- [Key Design Decisions](#key-design-decisions)
  - [Everything is Python, not shell scripts](#everything-is-python-not-shell-scripts)
  - [Single YAML config file as source of truth](#single-yaml-config-file-as-source-of-truth)
  - [Early error detection](#early-error-detection)
  - [Re-entrancy via import state reset at task start](#re-entrancy-via-import-state-reset-at-task-start)
  - [Mutual exclusion](#mutual-exclusion)
  - [Failure handling: callbacks over sentinel tasks](#failure-handling-callbacks-over-sentinel-tasks)
- [Open Questions](#open-questions)
  - [KubernetesPodOperator vs. KubernetesExecutor](#kubernetespodoperator-vs-kubernetesexecutor)
  - [Data sharing between steps](#data-sharing-between-steps)
  - [Config file structure](#config-file-structure)
  - [Secrets storage](#secrets-storage)
  - [Handling bad studies](#handling-bad-studies)
  - [Airflow version](#airflow-version)

## Current Architecture

The existing pipelines run via Airflow DAGs that issue SSH commands to persistent machines (`pipelines3`, `pipelines5`). Each task shells out to a bash script (e.g., `airflow-clone-db.sh`, `airflow-transfer-deployment.sh`) which in turn calls further scripts. Behavior is controlled by a mix of properties files and config files spread across the persistent machine's filesystem.

Pain points with the current approach:

- **Reliant on persistent machine** — adding a new importer or changing step behavior requires editing bash scripts on the machine, not just checking in code.
  - **Opacity** — it is not obvious what a given task does without tracing through multiple layers of shell scripts. Inputs and side effects are implicit.
  - **Not isolated** -- uncertain about if issues are stemming from environment vs code
- **Scattered configuration** — properties files, color-swap configs, and data-source configs live in separate locations on the persistent machine, outside version control.
- **Fragile re-entry** — resuming after a mid-run failure requires manual bookkeeping of what state was left behind.
- **Errors surface late** — the import process can run for hours before a data error is discovered, and when it is, the error output is often opaque. Time spent importing bad data is time wasted.
- **One bad study blocks the rest** — if a single study fails during import, it can stall or invalidate the entire run rather than being quarantined and skipped.

---

## Goals

The redesigned import pipeline should satisfy these core properties:

- **Re-entrant** — if a step fails, the pipeline can resume from that step without re-running earlier work. No manual state cleanup required.
- **Observable** — each step is a single, named function with explicit inputs. No monolithic tasks performing many unrelated operations under the hood. Minimize persistent state that changes across steps.
- **Configurable** — a single YAML file, checked into source control, is the complete source of truth for all importers. Every knob lives there, documented. No configuration hidden across multiple files on a persistent machine.
- **Modular** — each task is given as little responsibility as possible and does not have side effects.
- **Testable** — individual tasks can be tested in isolation by mocking external dependencies. No need to stand up a full pipeline environment to verify a single step's logic.
- **Fail fast** — data errors should be caught as early as possible in the pipeline, before expensive import work begins, so curators can find and fix problems without waiting for a full run to complete.

---

## Proposed Import Flow

```
verify_cluster_state
verify_import_not_in_progress
set_import_running
  ├── wipe_standby_database ─┐
  │   clone_live_database    ┤ (parallel)
  └── fetch_studies_from_s3 ─┘
validate_studies --- Decide which studies to import here
import_into_standby_database
run_data_integrity_checks
  ├── check_referential_integrity  (parallel)
  └── check_foreign_key_integrity
transfer_deployment_color
  ├── check_cluster_matches_github_deployment
  ├── scale_replicas_pre
  ├── switch_ingress_rules
  ├── scale_replicas_post
  └── clear_persistence_caches
set_import_complete
send_slack_notifications
```

Each step is a Python function. No step touches the persistent machine directly — all behavior is expressed in code checked into this repository.

---

## Key Design Decisions

### Everything is Python, not shell scripts

Steps are plain Python functions rather than wrappers around bash scripts. This makes it possible to understand what a step does by reading the DAG file alone, enables unit testing of individual steps in isolation, and removes the dependency on bash scripts living on a persistent machine.

### Single YAML config file as source of truth

All importers (Public, GENIE, Triage, etc.) are configured in one YAML file checked into this repository. The DAG loads it at startup — task definitions, resource overrides, source repositories, and scheduling all live there. There are no external properties files to hunt for. See [example_importer_config.yaml](example_importer_config.yaml) for the schema.

### Early error detection

The redesigned pipeline adds two checkpoints that don't exist in the current flow: `validate_studies` (before import) and `run_data_integrity_checks` (after import, before the color swap). `validate_studies` catches malformed or incomplete study data before the expensive import begins, so curators don't have to wait hours to find out a study was bad. `run_data_integrity_checks` catches consistency problems in the imported data before it goes live, so a bad import never reaches production. Both steps are open questions in terms of scope; see the Open Questions section.

### Re-entrancy via import state reset at task start

Each task resets the import state to `running` before executing its main logic. This means a user can clear and re-run any failed task from the Airflow UI and the pipeline state is automatically corrected — no manual intervention needed. Terminal and cleanup tasks are excluded from this pattern. See the implementation doc for the specific Airflow mechanism used.

### Mutual exclusion

If the new DAG co-exists with the current `import_public_dag`, then concurrent imports have to be prevented; the extra check `verify_import_not_in_progress` that was added will add an extra layer of robustness and ensure 2 imports cannot be running simultaneously.

### Failure handling: callbacks over sentinel tasks

The current pipeline uses a dedicated `set_import_abandoned` task — manually wired downstream of every other task — to mark a run as failed. This is fragile: any new task added to the DAG must be explicitly connected to it, and if the sentinel itself fails the import state is left indeterminate. The new design uses Airflow's `on_failure_callback` instead, applied globally so it fires automatically on any task failure without any wiring. See the implementation doc for details.

---

## Open Questions

### Data sharing between steps

Study data is too large for XCom. Two options: **co-location** (run `fetch_studies_from_s3`, `validate_studies`, and `import_into_standby_database` in the same pod so data stays on local disk) or a **shared EFS volume** (each step runs in its own pod and mounts a shared `PersistentVolumeClaim`). XCom remains appropriate for lightweight signals like database color.

| | Co-location | Shared EFS volume |
|---|---|---|
| Infrastructure | No new infra — just one pod | EFS filesystem, PersistentVolume, PVC to provision and maintain |
| Airflow task granularity | Three steps collapse into one task — no per-step status, logs, or retry | Each step is a separate task with its own status, logs, and retry |
| Re-entrancy | Entire fetch → validate → import sequence reruns on failure | Each step can be individually cleared and re-run |
| Resource profiles | All three steps share one pod spec, sized for the most demanding (import) | Each step declares its own CPU/memory |
| Cleanup | Pod's local disk is ephemeral — no cleanup needed | Study data on EFS must be explicitly cleaned up after the run |

Co-location is simpler but trades away task-level observability and re-entrancy for the three most important steps in the pipeline. Shared EFS preserves those properties at the cost of infrastructure overhead. Avoid EBS — it is AZ-scoped and can only attach to one node at a time.

**Goal:** move to EFS in phase 2

### Secrets storage

Where do credentials live — ClickHouse passwords, Slack webhook URLs, S3 access keys? Two options: **AWS Secrets Manager** (secrets live outside Airflow, pods fetch them via IAM role at runtime, rotation is independent of Airflow) or **Airflow Connections / Variables** (simpler to set up, but secrets are coupled to the Airflow deployment and rotation requires updating them through the Airflow UI or API). The choice affects the IAM role design for pods and how developers access credentials locally.

**Goal:** phase 1, have a unified configuration file -- refactor Bash scripts to use the config

Phase 2 -- loop Zain in and see what they're using for credential management

### Handling bad studies

If a single study fails during `import_into_standby_database`, the current pipeline stalls the entire run. Is there a way we can tackle this problem as we're re-designing the system?

(addressed)

### KubernetesPodOperator vs. KubernetesExecutor

These are mutually exclusive choices, not nested ones. `KubernetesPodOperator` would replace `KubernetesExecutor` — the executor would switch back to something like Celery or Local, and only tasks using `KubernetesPodOperator` would run in pods. Either way, each task runs in at most one pod.

Our pipeline has significant task heterogeneity, which is the main axis this decision turns on:

| | `KubernetesExecutor` | `KubernetesPodOperator` |
|---|---|---|
| Executor change required | No — already in use | Yes — switch away from `KubernetesExecutor` |
| Per-task image override | Verbose — nested `k8s.V1Pod` object | First-class `image=` param |
| Per-task resource override | Verbose — nested `k8s.V1Pod` object | First-class `cpu=`, `memory=` params |
| Tasks that skip pods | Not possible — every task runs in a pod | `PythonOperator` runs on the worker instead |
| XCom | Standard Airflow backend | File-based — tasks pushing/pulling XCom need adjustment |
| Log streaming | Native | Streamed from pod — gaps possible on eviction |

The cleaner ergonomics of `KubernetesPodOperator` are most valuable precisely because we have heterogeneous tasks. The main costs are the XCom/logging differences and the executor migration. See the implementation doc for a code-level comparison.

(phase 2 task)
(publish leaner cBioPortal importer image)

#### How important is image heterogeneity to us?

The per-task `image=` param is one of `KubernetesPodOperator`'s strongest selling points, but its value depends on how many distinct images we actually need. If the pipeline can run entirely on one image — for example, a single derived cBioPortal image that also includes our fetch/validation tooling — then the ergonomics gap between the two operators narrows considerably. If different tasks genuinely need different runtime environments (e.g., the cBioPortal importer image for import, a separate image for S3 fetch or data integrity checks), then first-class `image=` support becomes more load-bearing. Worth mapping out the actual image requirements per task before treating this as a settled argument for `KubernetesPodOperator`.

### Config file structure

The current design puts everything in one YAML file: task definitions, resource overrides, source repositories, scheduling, and DAG topology. The alternative is to split by concern — for example, one file per importer, or a shared base config for defaults with per-importer overrides on top. A single file is simple to start but may become unwieldy as the number of importers grows and configs diverge. What should drive the split, and where should the boundaries be?

(phase 1 -- be clear on the split; write abstracted code)

### Running the importer: cBioPortal Docker image vs. purpose-built pod image

The cBioPortal importer ships as a Docker image. Two options: **run the importer directly using that image** in a `KubernetesPodOperator` task (no extra image to build or maintain, always in sync with the cBioPortal release) or **build a custom image derived from it** (`FROM cbioportal/cbioportal:latest`, then layer in our config, wrapper scripts, and dependencies). The derived image approach is not Docker-in-Docker — the pod still runs a single image directly; we just own the build. True DinD (running `docker run` inside a pod) would require a privileged pod, fights the cluster's container runtime, and re-pulls the image on every run with no cache — not worth it here. The tradeoff between the two real options is convenience vs. control: the upstream image is zero-maintenance but opaque; a derived image gives us flexibility at the cost of a build/publish step and pinning to a specific cBioPortal version.

(package metaImport + JAR into the Dockerfile)

### Airflow version

Should this redesign target Airflow 3? Notable features: DAG versioning (run history tied to the exact DAG code), asset-based scheduling (trigger a DAG when upstream data is ready rather than on a fixed cron), and improved **dynamic task mapping** (per-study task instances instead of one bulk import step). Migration cost is real — breaking changes, retesting all DAGs — so the question is whether any of these are worth it given where we are.
