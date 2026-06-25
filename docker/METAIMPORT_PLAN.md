# Plan — pulling cbioportal `metaImport` into the Airflow image

**Goal:** replace the MSK importer jar invocation in
[airflow-import-direct-to-clickhouse.sh](../import-scripts/airflow-import-direct-to-clickhouse.sh)
with cBioPortal's `metaImport.py`, sourced from the published
`cbioportal/cbioportal` image via a Docker multi-stage build.

Status: **planned, not implemented.** Build the credentials-mount work first;
this depends on an open architectural question (see "Blocker" below).

---

## How the file copy works (mechanism)

A multi-stage build references the published image as a named stage and copies
files straight out of its filesystem with `COPY --from=`. The cbioportal image is
pulled only so Docker can read those paths; none of its runtime layers end up in
the final image except the files we explicitly copy.

Latest stable tag at time of writing: **`cbioportal/cbioportal:7.0.3`** (488 MB).
Pin by `@sha256` digest in the real change for reproducibility.

---

## What `metaImport` actually requires

`metaImport.py` does two things: **validate** (pure Python) and **import** (shells
out to Java). The import step runs, via `cbioportal_common.run_java`:

```
java -cp /core/core-IMPORTER.jar -Dspring.profiles.active=dbcp \
     org.mskcc.cbio.portal.scripts.ImportProfileData ...
```

`cbioportalImporter.locate_jar()` globs `core-*.jar` at `<scripts>/../..` (i.e.
`/core/`), so **the `/core/` layout must be preserved** in our image, or we pass
`--jar_path` explicitly.

| Need | Path in `cbioportal/cbioportal:7.0.3` | Why |
|---|---|---|
| Importer Python package | `/core/scripts/importer/` (whole dir) | `metaImport.py` + its relative-imported modules |
| Java loader jar | `/core/core-IMPORTER.jar` | the actual DB-load engine `run_java` invokes |
| Python deps list | `/core/requirements.txt` | `Jinja2==2.11.3`, `markupsafe==2.0.1`, `PyYAML`, `requests`, `dsnparse` |
| Config template | `/cbioportal-webapp/application.properties` | supplies `db.spring.datasource.url` the jar reads |
| A JRE | `/opt/java/openjdk` | the Airflow base image has **no Java** |

---

## Two issues to handle

### 1. Python dependency conflict (solvable)

`metaImport`'s `requirements.txt` pins `Jinja2==2.11.3` / `markupsafe==2.0.1`.
Airflow 2.10.5 needs **Jinja2 3.x**. Installing these into Airflow's env would
break Airflow → install them into an **isolated venv** and run `metaImport` with
that interpreter.

### 2. Architectural mismatch (BLOCKER — confirm before building)

The current script imports **direct to ClickHouse**. But `core-IMPORTER.jar`
loads into cbioportal's **relational DB** via the Spring datasource
(`db.spring.datasource.url`); `cbioportal_common.py` explicitly notes the old
direct-DB helper "would need to be adapted to interact with a clickhouse
database." In cbioportal v7, ClickHouse is a *derived* store rebuilt afterward
(`rebuild_derived_tables.py` sits next to `metaImport.py`).

**So "use metaImport instead" is not a drop-in swap of the import target** — it
changes which database is loaded and likely adds a derived-table rebuild step.
Confirm metaImport can target the intended ClickHouse setup before investing.

---

## Dockerfile changes (once unblocked)

```dockerfile
# ── top of file ──
FROM cbioportal/cbioportal:7.0.3 AS cbioportal   # pin by @sha256 digest

FROM apache/airflow:2.10.5-python3.12
# ... existing OS packages, CLIs, PORTAL_HOME tree ...

# ── Java runtime, lifted from the cbioportal image (version-matched to the jar) ──
COPY --from=cbioportal /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:$PATH

# ── importer scripts + loader jar, /core/ layout preserved so locate_jar() works ──
COPY --from=cbioportal --chown=airflow:0 /core/scripts/importer/  /core/scripts/importer/
COPY --from=cbioportal --chown=airflow:0 /core/core-IMPORTER.jar  /core/core-IMPORTER.jar
COPY --from=cbioportal --chown=airflow:0 /core/requirements.txt   /core/requirements.txt

# ── isolated venv for metaImport (keeps Jinja2 2.11.3 away from Airflow's 3.x) ──
USER airflow
RUN python -m venv /opt/cbio-venv \
    && /opt/cbio-venv/bin/pip install --no-cache-dir -r /core/requirements.txt
# invoke as:
#   /opt/cbio-venv/bin/python /core/scripts/importer/metaImport.py \
#       -s <study_dir> -p application.properties ...
```

## DAG changes (once unblocked)

- Replace `t_import`'s bash command with a `metaImport.py` invocation via
  `/opt/cbio-venv/bin/python`.
- Supply an `application.properties` with the DB connection string — this becomes
  another file in the `pipelines-credentials/` secret mount (see the
  credentials-mount work), not the `.properties` the MSK jar used.

---

## Open question for the team

Does the import flow want cbioportal's relational-load-then-derive model, or true
direct-to-ClickHouse? This decides whether this whole approach is the right path.
