# Test Plan — `docker/Dockerfile` for `import_public_hackathon`

**Goal:** the image built from [docker/Dockerfile](Dockerfile) must successfully run
every Airflow task in [dags/import_public_hackathon.py](../dags/import_public_hackathon.py).

Items are ordered by **effort / dependency**, ascending: each tier needs only the
image (Tier 1), then the public S3 bucket (Tiers 2–3), then a live cluster +
mounted Secret + the still-missing scripts (Tier 4), then everything at once
(Tier 5). Work top-to-bottom and stop blocking downstream items on a failure.

Each item is either **succeeded** or **failed**. On failure, fill in the
**reason for failure:** line.

Run task-level tests in the container, e.g.:

```bash
docker run --rm cmo-import:dev \
  airflow tasks test import_public_hackathon <task_id> 2026-06-25 \
  --task-params '{"cancer_study_ids": ["..."]}'   # only where the param is read
```

When testing the locally built image, ensure the run actually uses `cmo-import:dev`
(all tasks set `executor_config=_POD_OVERRIDE` pinning `apache/airflow:2.10.5`).

---

## Tier 1 — Image build & smoke (no network, no creds, no cluster)

The fastest feedback loop — validates the image itself. Nothing here touches S3,
the Secret, or ClickHouse.

```bash
docker build -f docker/Dockerfile -t cmo-import:dev .
docker run --rm cmo-import:dev python -c "import boto3, clickhouse_connect, yaml, kubernetes, pandas, requests, git; print('deps ok')"
docker run --rm cmo-import:dev bash -c "command -v clickhouse-client aws kubectl yq saml2aws git curl"
docker run --rm cmo-import:dev python -c "import dags.import_public_hackathon; print('DAG imports')"
docker run --rm cmo-import:dev airflow dags list 2>&1 | grep import_public_hackathon
```

- [x] **Image builds** — **succeeded** (native arm64), *after* excluding `clickhouse-connect` from requirements.txt. ⚠️ With `clickhouse-connect` in, the native arm64 build fails: it pulls `lz4`, the Airflow constraints pin `lz4==4.4.3`, and that version has **no linux/aarch64 cp312 wheel**, so `--only-binary=:all:` can't resolve it.
  - **reason for failure:** n/a (resolved by exclusion; see requirements.txt note)
- [x] **Build for cluster arch** — **succeeded** — `docker build --platform linux/amd64 ...` resolves `lz4==4.4.3` (x86_64 wheel exists), so `clickhouse-connect` can be restored for amd64-only builds.
  - **reason for failure:**
- [x] **CLI binaries present** — **succeeded** — `clickhouse-client`, `aws`, `kubectl`, `yq`, `saml2aws`, `git`, `curl` all on PATH
  - **reason for failure:**
- [x] **Python deps importable** — **succeeded** — `import boto3, yaml, kubernetes, pandas, requests, git` ok. ⚠️ `clickhouse_connect` currently excluded (see build note above).
  - **reason for failure:**
- [x] **Airflow providers present** — **succeeded** — `import airflow.providers.amazon, airflow.providers.slack` ok
  - **reason for failure:**
- [x] **PORTAL_HOME tree exists & owned by airflow** — **succeeded** — all 7 dirs present, owned `airflow:root`
  - **reason for failure:**
- [x] **Scripts overlaid** — **succeeded** — `import-scripts/` at `/data/portal-cron/scripts/`
  - **reason for failure:**
- [x] **DAG code present** — **succeeded** — `/opt/airflow/dags/import_public_hackathon.py` and `dags/utils/` exist
  - **reason for failure:**
- [x] **DAG imports cleanly** — **succeeded** — `import dags.import_public_hackathon` ok
  - **reason for failure:**
- [x] **DAG registered with Airflow** — **succeeded** — after `airflow db migrate`, `airflow dags list` shows `import_public_hackathon` with no import errors (exercises the `_POD_OVERRIDE` Secret-volume edit)
  - **reason for failure:**

## Tier 2 — Trivial stub tasks (run, log, exit 0; no creds/cluster)

Pure-Python stubs that only log. "Passing" means "runs without error," not "does
the real work." Cheapest way to prove `airflow tasks test` works in-container.

- [x] **`verify_import_not_in_progress`** (Python, STUB) — **succeeded** — `airflow tasks test` marks SUCCESS, logs the stub line
  - **reason for failure:**
- [x] **`send_slack_notifications`** (Python, STUB) — **succeeded** — `airflow tasks test` marks SUCCESS, logs the stub line
  - **reason for failure:**

## Tier 3 — Real S3 tasks (need network → public unsigned bucket; no Secret)

Both use the **unsigned** S3 client ([secret_manager.py](../dags/utils/secret_manager.py)),
so they need **no Secret and no AWS creds** — only egress to `s3://hackathon-databricks`.
Test fixtures in the bucket: `testa/` and `testb/` are the directory-style prefixes
the `{study_id}/` check matches (the real `*_*.tar` studies sit at the top level).

- [x] **`verify_studies_exist`** (Python) — **succeeded** — `airflow tasks test ... --task-params '{"cancer_study_ids":["testa","testb"]}'`; both found via unsigned client, returned `['testa','testb']`
  - **reason for failure:**
- [x] **`validate_studies`** (Python) — **succeeded** — exercised via the task's `python_callable` (isolated `airflow tasks test` can't supply the upstream XCom); downloaded `testa/clinicala` + `testb/clinicalb` to `/tmp`, returned the passing list. Full XCom-wired run is covered by Tier 5.
  - **reason for failure:**

## Tier 4 — Cluster / Secret / missing-script tasks

Everything here needs a live ClickHouse cluster, the mounted `pipelines-credentials`
Secret, and (for two tasks) scripts that are **not yet in `import-scripts/`**.

### Prerequisites (mounts & config)

The image leaves the creds dir empty on purpose; at run time a Kubernetes Secret
is mounted over it (see `_POD_OVERRIDE` in the DAG). Create/update the Secret with
[create-pipelines-credentials-secret.sh](create-pipelines-credentials-secret.sh).
For local `docker run`, bind-mount a host dir over `/data/portal-cron/pipelines-credentials`.

- [ ] **`pipelines-credentials` Secret mounted** — `manage_public_clickhouse_database_update_tools.properties` present at the mount path — succeeded / failed
  - **reason for failure:**
- [ ] **Color-swap config mounted** — `public-db-color-swap-config.yaml` present (same Secret) — succeeded / failed
  - **reason for failure:**
- [ ] **ClickHouse reachable** — host/port/creds in the properties file connect — succeeded / failed
  - **reason for failure:**
- [ ] **`set_update_process_state.sh` exists in scripts dir** — ⚠️ referenced by tasks `set_import_running` / `set_import_complete` but currently **missing from `import-scripts/`** — succeeded / failed
  - **reason for failure:**
- [ ] **`get_database_currently_in_production.sh` exists** — color-resolution helper the bash scripts call internally; also **missing from `import-scripts/`** — succeeded / failed
  - **reason for failure:**

### Bash tasks (call the production import-scripts, in DAG order)

- [ ] **`verify_cluster_state`** (Bash → `airflow-verify-management.sh`) — cluster health + management/ingress color consistency check passes — succeeded / failed
  - **reason for failure:**
- [ ] **`set_import_running`** (Bash → `set_update_process_state.sh running`) — sources `automation-environment.sh`, marks state `running` — succeeded / failed
  - **reason for failure:**
- [ ] **`clone_live_database_into_standby`** (Bash → `airflow-clone-db.sh`) — wipes + clones live DB into standby color — succeeded / failed
  - **reason for failure:**
- [ ] **`import_into_standby_database`** (Bash → `airflow-import-direct-to-clickhouse.sh`) — imports studies into standby color; writes notification file. ⚠️ Also needs Java + the MSK importer jar (or the metaImport rework — see [METAIMPORT_PLAN.md](METAIMPORT_PLAN.md)) — succeeded / failed
  - **reason for failure:**
- [ ] **`transfer_deployment_color`** (Bash → `airflow-transfer-deployment.sh`) — swaps production traffic to freshly imported standby — succeeded / failed
  - **reason for failure:**
- [ ] **`set_import_complete`** (Bash → `set_update_process_state.sh complete`) — marks state `complete` — succeeded / failed
  - **reason for failure:**

## Tier 5 — Full DAG run

- [ ] **End-to-end run** — full DAG run (`airflow dags test import_public_hackathon <date>` or triggered run) completes with all tasks green, respecting the fork/diamond graph (`set_import_running >> [clone, validate] >> import >> transfer >> complete >> slack`) — succeeded / failed
  - **reason for failure:**

---

### Notes
- Tasks `verify_import_not_in_progress`, `validate_studies` (validator), and
  `send_slack_notifications` are **stubs** — passing means "runs without error,"
  not "does the real work."
- `set_import_running` / `set_import_complete` will fail until
  `set_update_process_state.sh` is added to `import-scripts/` (flagged in Tier 4).
- Only Tier 3 needs network; nothing in Tiers 1–3 needs the Secret or a cluster, so
  a lot can go green before any infra is wired up.
