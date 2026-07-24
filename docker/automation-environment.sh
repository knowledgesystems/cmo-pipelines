#!/bin/bash

# ──────────────────────────────────────────────────────────────────────────────
# Container-baked, slimmed copy of import-scripts/pipelines_eks/automation-environment.sh.
#
# Trimmed from the full EKS original to only the variables the
# import_public_hackathon script tree actually references — the 11 in-repo +
# 7 cbioportal-core scripts the DAG executes — plus the PATH / TLS vars those
# scripts' tools (clickhouse-client, kubectl, git) read implicitly. The full
# original's ~110 PORTAL_*/DMP_*/data-home/credential vars are unused by this
# tree and were dropped.
#
# Safety patches from the full version are preserved:
#   1. JAVA_SSL_ARGS reads the truststore password only when the file is present,
#      so sourcing does not abort when no credentials are mounted.
#   2. SSL_CERT_FILE / GIT_SSL_CAINFO use the Debian (apache/airflow base) CA path.
#   3. Tool-path vars point at the image's actual binaries.
# JAVA_* vars are referenced by airflow-import-direct-to-clickhouse.sh; kept
# verbatim even though the JAR importer they feed is not installed in this image.
# ──────────────────────────────────────────────────────────────────────────────

#######################
# tool / binary paths (PATH lets the scripts find clickhouse-client, kubectl, aws)
#######################
# JAVA_HOME/JAVA_BINARY: this image installs NO JDK — the JAR importer these
# feed (via airflow-import-direct-to-clickhouse.sh) is not present here, so they
# are inert. The old hardcoded /usr/lib/jdk-21.0.2 pointed at a path that does
# not exist in this image, which would mask a real "java missing" error behind a
# confusing one. Resolve from PATH instead: empty when java is absent (so any
# accidental $JAVA_BINARY call fails loudly, matching this file's safety intent),
# and self-correcting if a JDK is ever added (e.g. /opt/java/openjdk).
if _java_bin="$(command -v java 2>/dev/null)"; then
    export JAVA_HOME="$(dirname "$(dirname "$_java_bin")")"
    export JAVA_BINARY="$_java_bin"
else
    export JAVA_HOME=""
    export JAVA_BINARY=""
fi
export GIT_BINARY=/usr/bin/git
export YQ_BINARY=/usr/local/bin/yq
export PATH="/usr/local/sbin:/usr/sbin:/usr/local/bin:/usr/bin:/bin"

#######################
# data-home root (the kubeconfig + truststore paths below hang off this)
#######################
export PORTAL_HOME=/data/portal-cron

#######################
# kubeconfig paths used by clear_cbioportal_persistence_cache.sh
#######################
export PUBLICARGOCD_CLUSTER_KUBECONFIG=$PORTAL_HOME/pipelines-credentials/publicargocd-cluster-kubeconfig
export EKSARGOCD_CLUSTER_KUBECONFIG=$PORTAL_HOME/pipelines-credentials/eksargocd-cluster-kubeconfig

#######################
# SSL args for the JAR importer (read by airflow-import-direct-to-clickhouse.sh)
#######################
# Patched: only read the truststore password when the file is actually present,
# so sourcing this file does not fail when credentials are not mounted.
export AWS_SSL_TRUSTSTORE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore
export AWS_SSL_TRUSTSTORE_PASSWORD_FILE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore.password
_aws_ssl_truststore_password=""
if [ -f "$AWS_SSL_TRUSTSTORE_PASSWORD_FILE" ]; then
    _aws_ssl_truststore_password=$(cat "$AWS_SSL_TRUSTSTORE_PASSWORD_FILE")
fi
export JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$AWS_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$_aws_ssl_truststore_password"

#######################
# TLS CA bundle (read implicitly by clickhouse-client and git over HTTPS)
#######################
# Patched: Debian (apache/airflow base) CA bundle path, not the RHEL path.
export SSL_CERT_FILE="/etc/ssl/certs/ca-certificates.crt" # needed to use the clickhouse CLI
export GIT_SSL_CAINFO="/etc/ssl/certs/ca-certificates.crt" # needed to use git
