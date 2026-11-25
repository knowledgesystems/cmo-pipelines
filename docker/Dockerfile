# CMO import scripts + import_public_hackathon DAG on a Kubernetes task pod.
# Build from the repo root: docker build -f docker/Dockerfile -t cmo-import:dev .
FROM apache/airflow:2.10.5-python3.12

ARG KUBECTL_VERSION=v1.30.3
ARG YQ_VERSION=v4.44.3
ARG SAML2AWS_VERSION=2.36.13

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

# CLI binaries the import-scripts shell out to; arch follows the build platform
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

# PORTAL_HOME tree the import-scripts assume; pipelines-credentials/ is a runtime mount
RUN mkdir -p \
        /data/portal-cron/scripts \
        /data/portal-cron/logs \
        /data/portal-cron/tmp \
        /data/portal-cron/pipelines-credentials \
    && chown -R airflow:0 /data/portal-cron

USER airflow
COPY docker/requirements.txt /tmp/requirements.txt
# --only-binary: fail loudly rather than silently compile from source
RUN pip install --no-cache-dir --only-binary=:all: -r /tmp/requirements.txt \
        --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.10.5/constraints-3.12.txt"

# clickhouse import-support scripts from cbioportal-core, SHA-pinned; bump to advance:
#   git ls-remote https://github.com/cBioPortal/cbioportal-core.git main
ARG CBIOPORTAL_CORE_REF=6f69e5459a75b6b1dffdcb8c1d820d0426454577
RUN set -eux; \
    git clone --filter=blob:none --sparse \
        https://github.com/cBioPortal/cbioportal-core.git /tmp/cbioportal-core; \
    git -C /tmp/cbioportal-core sparse-checkout set scripts/clickhouse_import_support; \
    git -C /tmp/cbioportal-core checkout "${CBIOPORTAL_CORE_REF}"; \
    cp /tmp/cbioportal-core/scripts/clickhouse_import_support/*.sh /data/portal-cron/scripts/; \
    chmod +x /data/portal-cron/scripts/*.sh; \
    rm -rf /tmp/cbioportal-core

# only the scripts the DAG executes + their sourced dependencies; copied last for caching
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
COPY --chown=airflow:0 dags/ /opt/airflow/dags/

ENV PORTAL_HOME=/data/portal-cron \
    PORTAL_SCRIPTS_DIRECTORY=/data/portal-cron/scripts
