#!/bin/bash

# Slimmed copy of import-scripts/pipelines_eks/automation-environment.sh: only
# the variables the DAG's scripts reference, paths patched for this image.

# JAVA_* feed the JAR importer, not installed here; resolve from PATH, empty if absent
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

export PORTAL_HOME=/data/portal-cron

# kubeconfigs used by clear_cbioportal_persistence_cache.sh
export PUBLICARGOCD_CLUSTER_KUBECONFIG=$PORTAL_HOME/pipelines-credentials/publicargocd-cluster-kubeconfig
export EKSARGOCD_CLUSTER_KUBECONFIG=$PORTAL_HOME/pipelines-credentials/eksargocd-cluster-kubeconfig

# read the truststore password only when present, so sourcing works without credentials
export AWS_SSL_TRUSTSTORE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore
export AWS_SSL_TRUSTSTORE_PASSWORD_FILE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore.password
_aws_ssl_truststore_password=""
if [ -f "$AWS_SSL_TRUSTSTORE_PASSWORD_FILE" ]; then
    _aws_ssl_truststore_password=$(cat "$AWS_SSL_TRUSTSTORE_PASSWORD_FILE")
fi
export JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$AWS_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$_aws_ssl_truststore_password"

# Debian CA bundle (clickhouse-client, git over HTTPS)
export SSL_CERT_FILE="/etc/ssl/certs/ca-certificates.crt"
export GIT_SSL_CAINFO="/etc/ssl/certs/ca-certificates.crt"
