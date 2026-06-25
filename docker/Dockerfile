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
    && rm -rf /var/lib/apt/lists/*

# ── Standalone CLI binaries the import-scripts/*.sh scripts shell out to ──
# clickhouse, aws v2, kubectl, yq, saml2aws.
# Arch is derived from the running image (uname -m) so the binaries always match
# the build platform; build for the cluster with `docker build --platform linux/amd64`.
RUN set -eux; \
    case "$(uname -m)" in \
        x86_64|amd64) AWS_ARCH=x86_64; GO_ARCH=amd64 ;; \
        aarch64|arm64) AWS_ARCH=aarch64; GO_ARCH=arm64 ;; \
        *) echo "unsupported arch $(uname -m)" >&2; exit 1 ;; \
    esac; \
    # clickhouse: fetch the binary then run its self-installer (drops symlinks
    # clickhouse-client / clickhouse-local into /usr/bin).
    cd /tmp && curl -fsSL https://clickhouse.com/ | sh && ./clickhouse install --noninteractive && rm -f /tmp/clickhouse; \
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
    # saml2aws (release tarball contains the single saml2aws binary)
    curl -fsSL "https://github.com/Versent/saml2aws/releases/download/v${SAML2AWS_VERSION}/saml2aws_${SAML2AWS_VERSION}_linux_${GO_ARCH}.tar.gz" -o /tmp/saml2aws.tar.gz; \
    tar -xzf /tmp/saml2aws.tar.gz -C /usr/local/bin saml2aws; \
    chmod +x /usr/local/bin/saml2aws; \
    rm -f /tmp/saml2aws.tar.gz

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

# ── Content (changes most frequently; copied last for cache efficiency) ──
# Whole import-scripts tree as-is, then overlay the safety-patched env file at
# the path the scripts source ($PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh).
COPY --chown=airflow:0 import-scripts/ /data/portal-cron/scripts/
COPY --chown=airflow:0 docker/automation-environment.sh /data/portal-cron/scripts/automation-environment.sh
# DAG code so the task pod can `from dags.utils... import` (DAG appends its parent
# dir to sys.path; dags/utils/__init__.py already exists).
COPY --chown=airflow:0 dags/ /opt/airflow/dags/

ENV PORTAL_HOME=/data/portal-cron \
    PORTAL_SCRIPTS_DIRECTORY=/data/portal-cron/scripts
