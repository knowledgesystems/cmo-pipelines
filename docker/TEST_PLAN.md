# Test Plan — `docker/Dockerfile` for `import_public_hackathon`

**Goal:** the image built from [docker/Dockerfile](Dockerfile) must successfully run
every Airflow task in [dags/import_public_hackathon.py](../dags/import_public_hackathon.py).

Work top-to-bottom. Each item is either **succeeded** or **failed**. On failure,
fill in the **reason for failure:** line and stop blocking downstream items that
depend on it.

---

## Phase 0 — Image build & smoke

- [ ] **Image builds** — `docker build -f docker/Dockerfile -t cmo-import:dev .` (from repo root) — succeeded / failed
  - **reason for failure:**
- [ ] **Build for cluster arch** — `docker build --platform linux/amd64 -f docker/Dockerfile -t cmo-import:dev .` — succeeded / failed
  - **reason for failure:**
- [ ] **CLI binaries present** — `clickhouse-client`, `aws`, `kubectl`, `yq`, `saml2aws`, `git`, `curl` all on PATH inside the container — succeeded / failed
  - **reason for failure:**
- [ ] **Python deps importable** — `python -c "import boto3, clickhouse_connect, yaml, kubernetes, pandas, requests, git"` — succeeded / failed
  - **reason for failure:**
- [ ] **Airflow providers present** — `python -c "import airflow.providers.amazon, airflow.providers.slack"` — succeeded / failed
  - **reason for failure:**
- [ ] **PORTAL_HOME tree exists & owned by airflow** — `/data/portal-cron/{scripts,lib,logs,tmp,cbio-portal-data,git-repos,pipelines-credentials}` — succeeded / failed
  - **reason for failure:**
- [ ] **Scripts overlaid** — `import-scripts/` copied to `/data/portal-cron/scripts/`, and `automation-environment.sh` is the patched docker/ copy — succeeded / failed
  - **reason for failure:**
- [ ] **DAG code present** — `/opt/airflow/dags/import_public_hackathon.py` and `/opt/airflow/dags/utils/` exist — succeeded / failed
  - **reason for failure:**
- [ ] **DAG imports cleanly** — `python -c "import dags.import_public_hackathon"` (no parse/import errors, no missing modules) — succeeded / failed
  - **reason for failure:**
- [ ] **DAG registered with Airflow** — `airflow dags list | grep import_public_hackathon` shows no import errors — succeeded / failed
  - **reason for failure:**

## Phase 1 — Runtime prerequisites (mounts & config)

These are runtime mounts the image intentionally leaves empty. Each task that
touches ClickHouse / color-swap config needs them present.

- [ ] **`pipelines-credentials/` mounted** — `manage_public_clickhouse_database_update_tools.properties` present — succeeded / failed
  - **reason for failure:**
- [ ] **Color-swap config mounted** — `public-db-color-swap-config.yaml` present — succeeded / failed
  - **reason for failure:**
- [ ] **ClickHouse reachable** — host/port/creds in the properties file connect — succeeded / failed
  - **reason for failure:**
- [ ] **`set_update_process_state.sh` exists in scripts dir** — ⚠️ referenced by tasks `set_import_running` / `set_import_complete` but currently **missing from `import-scripts/`** — succeeded / failed
  - **reason for failure:**
- [ ] **`get_database_currently_in_production.sh` exists** — color-resolution helper the bash scripts call internally; also **missing from `import-scripts/`** — succeeded / failed
  - **reason for failure:**

## Phase 2 — Per-task execution (DAG order)

Run each via `airflow tasks test import_public_hackathon <task_id> <logical_date>`
(pass `--task-params '{"cancer_study_ids": ["..."]}'` where the param is read).

- [ ] **1. `verify_studies_exist`** (Python) — lists S3 objects via unsigned client; all requested studies found in `s3://hackathon-databricks` — succeeded / failed
  - **reason for failure:**
- [ ] **2. `verify_cluster_state`** (Bash → `airflow-verify-management.sh`) — cluster health + management/ingress color consistency check passes — succeeded / failed
  - **reason for failure:**
- [ ] **3. `verify_import_not_in_progress`** (Python, STUB) — runs and logs; no import-in-progress gate yet — succeeded / failed
  - **reason for failure:**
- [ ] **4. `set_import_running`** (Bash → `set_update_process_state.sh running`) — sources `automation-environment.sh`, marks state `running` — succeeded / failed
  - **reason for failure:**
- [ ] **5. `clone_live_database_into_standby`** (Bash → `airflow-clone-db.sh`) — wipes + clones live DB into standby color — succeeded / failed
  - **reason for failure:**
- [ ] **6. `validate_studies`** (Python) — downloads each study from S3 to `/tmp/<study>` and validates (STUB validator); returns passing list — succeeded / failed
  - **reason for failure:**
- [ ] **7. `import_into_standby_database`** (Bash → `airflow-import-direct-to-clickhouse.sh`) — imports studies into standby color; writes notification file — succeeded / failed
  - **reason for failure:**
- [ ] **8. `transfer_deployment_color`** (Bash → `airflow-transfer-deployment.sh`) — swaps production traffic to freshly imported standby — succeeded / failed
  - **reason for failure:**
- [ ] **9. `set_import_complete`** (Bash → `set_update_process_state.sh complete`) — marks state `complete` — succeeded / failed
  - **reason for failure:**
- [ ] **10. `send_slack_notifications`** (Python, STUB) — runs and logs; no real Slack post yet — succeeded / failed
  - **reason for failure:**

## Phase 3 — Full DAG run

- [ ] **End-to-end run** — full DAG run (`airflow dags test import_public_hackathon <date>` or triggered run) completes with all tasks green, respecting the fork/diamond graph (`set_import_running >> [clone, validate] >> import >> transfer >> complete >> slack`) — succeeded / failed
  - **reason for failure:**

---

### Notes
- Tasks 3, 6 (validator), and 10 are **stubs** in the current DAG — passing means
  "runs without error," not "does the real work."
- Tasks 4 and 9 will fail at Phase 2 until `set_update_process_state.sh` is added to
  `import-scripts/` (flagged in Phase 1).
- All tasks set `executor_config=_POD_OVERRIDE` pinning `apache/airflow:2.10.5`; when
  testing the locally built image, ensure the run actually uses `cmo-import:dev`.
