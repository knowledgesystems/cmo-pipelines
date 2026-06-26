# Self-contained image for running the CMO import scripts and the
# import_public_hackathon Airflow DAG on a Kubernetes task pod.
#
# Build from the repo root so the context sees import-scripts/, dags/ and docker/:
#   docker build -f docker/Dockerfile -t cmo-import:dev .
#
# Extend the base image the DAG's K8S_IMAGE points at (apache/airflow:2.10.5),
# pinning the Python version explicitly so it can't drift. The plain 2.10.5 tag
# currently resolves to python3.12, so this matches the rest of the deployment.
FROM apache/airflow:2.10.5-python3.12

ARG KUBECTL_VERSION=v1.30.3
ARG YQ_VERSION=v4.44.3
ARG SAML2AWS_VERSION=2.36.13

# ── OS packages: runtime tools only (pip installs wheels only — no compiler needed) ──
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
        git \
        curl \
        unzip \
        ca-certificates \
        gnupg \
    && curl -fsSL 'https://packages.clickhouse.com/rpm/lts/repodata/repomd.xml.key' \
        | gpg --dearmor -o /usr/share/keyrings/clickhouse-keyring.gpg \
    && echo 'deb [arch=amd64 signed-by=/usr/share/keyrings/clickhouse-keyring.gpg] https://packages.clickhouse.com/deb stable main' \
        > /etc/apt/sources.list.d/clickhouse.list \
    && apt-get update && apt-get install -y --no-install-recommends clickhouse-client \
    && rm -rf /var/lib/apt/lists/*

# ── Standalone CLI binaries the import-scripts/*.sh scripts shell out to ──
# aws v2, kubectl, yq, saml2aws (clickhouse-client installed via apt above).
# Arch is derived from the running image (uname -m) so the binaries always match
# the build platform; build for the cluster with `docker build --platform linux/amd64`.
RUN set -eux; \
    case "$(uname -m)" in \
        x86_64|amd64) AWS_ARCH=x86_64; GO_ARCH=amd64 ;; \
        aarch64|arm64) AWS_ARCH=aarch64; GO_ARCH=arm64 ;; \
        *) echo "unsupported arch $(uname -m)" >&2; exit 1 ;; \
    esac; \
    # aws cli v2
    curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" -o /tmp/awscliv2.zip; \
    unzip -q /tmp/awscliv2.zip -d /tmp; \
    /tmp/aws/install; \
    rm -rf /tmp/awscliv2.zip /tmp/aws; \
    # kubectl
    curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${GO_ARCH}/kubectl" -o /usr/local/bin/kubectl; \
    chmod +x /usr/local/bin/kubectl; \
    # yq
    curl -fsSL "https://github.com/mikefarah/yq/releases/download/${YQ_VERSION}/yq_linux_${GO_ARCH}" -o /usr/local/bin/yq; \
    chmod +x /usr/local/bin/yq; \
    # saml2aws (extract all then find the binary — tarball layout varies by version)
    curl -fsSL "https://github.com/Versent/saml2aws/releases/download/v${SAML2AWS_VERSION}/saml2aws_${SAML2AWS_VERSION}_linux_${GO_ARCH}.tar.gz" -o /tmp/saml2aws.tar.gz; \
    mkdir -p /tmp/saml2aws-extract; \
    tar -xzf /tmp/saml2aws.tar.gz -C /tmp/saml2aws-extract; \
    find /tmp/saml2aws-extract -type f -name 'saml2aws' -exec install -m 0755 {} /usr/local/bin/saml2aws \;; \
    rm -rf /tmp/saml2aws.tar.gz /tmp/saml2aws-extract

# ── On-disk layout the import-scripts assume (PORTAL_HOME tree) ──
# pipelines-credentials/ is a runtime mount point and is left empty here.
RUN mkdir -p \
        /data/portal-cron/scripts \
        /data/portal-cron/lib \
        /data/portal-cron/logs \
        /data/portal-cron/tmp \
        /data/portal-cron/cbio-portal-data \
        /data/portal-cron/git-repos \
        /data/portal-cron/pipelines-credentials \
    && chown -R airflow:0 /data/portal-cron

# ── Python dependencies (under the Airflow constraints file) ──
USER airflow
COPY docker/requirements.txt /tmp/requirements.txt
# --only-binary=:all: guarantees every package installs from a prebuilt wheel;
# the build fails loudly rather than silently compiling anything from source.
RUN pip install --no-cache-dir --only-binary=:all: -r /tmp/requirements.txt \
        --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.10.5/constraints-3.12.txt"

# ── clickhouse import-support scripts the DAG's bash tasks call but that don't
#    live in this repo (fetched from cbioportal-core). They source each other via
#    $this_script_dir, so all 7 must land together in the scripts dir. Fetched
#    before the content COPY so this layer caches independently of local edits
#    (re-runs only when CBIOPORTAL_CORE_REF changes); pin to a commit SHA later. ──
ARG CBIOPORTAL_CORE_REF=main
RUN set -eux; \
    base="https://raw.githubusercontent.com/cBioPortal/cbioportal-core/${CBIOPORTAL_CORE_REF}/scripts/clickhouse_import_support"; \
    for f in \
        set_update_process_state.sh \
        get_database_currently_in_production.sh \
        drop_tables_in_clickhouse_database.sh \
        clone_clickhouse_database.sh \
        synchronize_user_tables_between_databases.sh \
        parse_property_file_functions.sh \
        clickhouse_client_command_line_functions.sh \
    ; do \
        curl -fsSL "$base/$f" -o "/data/portal-cron/scripts/$f"; \
    done; \
    chmod +x /data/portal-cron/scripts/*.sh

# ── Content (changes most frequently; copied last for cache efficiency) ──
# Only the import-scripts the import_public_hackathon DAG executes, plus their
# transitively sourced/called dependencies. The rest of import-scripts/ (~660
# files) is not needed at runtime. Then overlay the safety-patched env file at
# the path the scripts source ($PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh).
COPY --chown=airflow:0 \
    import-scripts/airflow-verify-management.sh \
    import-scripts/airflow-clone-db.sh \
    import-scripts/airflow-import-direct-to-clickhouse.sh \
    import-scripts/airflow-transfer-deployment.sh \
    import-scripts/verify-management-state.sh \
    import-scripts/transfer-deployment-color.sh \
    import-scripts/color-config-parsing-functions.sh \
    import-scripts/clear-persistence-cache-shell-functions.sh \
    import-scripts/clear_cbioportal_persistence_cache.sh \
    import-scripts/slack-message-functions.sh \
    import-scripts/authenticate_service_account.sh \
    /data/portal-cron/scripts/
COPY --chown=airflow:0 docker/automation-environment.sh /data/portal-cron/scripts/automation-environment.sh
# DAG code so the task pod can `from dags.utils... import` (DAG appends its parent
# dir to sys.path; dags/utils/__init__.py already exists).
COPY --chown=airflow:0 dags/ /opt/airflow/dags/

# ── Supplementary scripts from cbioportal-core (not in cmo-pipelines) ──
# get_database_currently_in_production.sh and related ClickHouse helpers are
# maintained in cbioportal-core. Sparse-clone just that subdirectory.
USER root
RUN git clone --depth 1 --filter=blob:none --sparse \
        https://github.com/cBioPortal/cbioportal-core.git /tmp/cbioportal-core \
    && git -C /tmp/cbioportal-core sparse-checkout set scripts/clickhouse_import_support \
    && cp /tmp/cbioportal-core/scripts/clickhouse_import_support/*.sh /data/portal-cron/scripts/ \
    && chown airflow:0 /data/portal-cron/scripts/*.sh \
    && rm -rf /tmp/cbioportal-core
USER airflow

ENV PORTAL_HOME=/data/portal-cron \
    PORTAL_SCRIPTS_DIRECTORY=/data/portal-cron/scripts
