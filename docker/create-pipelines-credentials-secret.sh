#!/bin/bash
# Create/update the `pipelines-credentials` Secret the import_public_hackathon DAG
# mounts into task pods. Idempotent; mounted pods pick up changes within ~1 min.
#
# Usage: ./create-pipelines-credentials-secret.sh <namespace> <creds_dir>

set -euo pipefail

NAMESPACE="${1:?usage: $0 <namespace> <creds_dir>}"
CREDS_DIR="${2:?usage: $0 <namespace> <creds_dir>}"
SECRET_NAME="pipelines-credentials"

CLICKHOUSE_PROPS="$CREDS_DIR/manage_public_clickhouse_database_update_tools.properties"
COLOR_SWAP_YAML="$CREDS_DIR/public-db-color-swap-config.yaml"

for f in "$CLICKHOUSE_PROPS" "$COLOR_SWAP_YAML"; do
    if [ ! -f "$f" ]; then
        echo "ERROR: required credentials file not found: $f" >&2
        exit 1
    fi
done

kubectl create secret generic "$SECRET_NAME" \
    --namespace "$NAMESPACE" \
    --from-file="manage_public_clickhouse_database_update_tools.properties=$CLICKHOUSE_PROPS" \
    --from-file="public-db-color-swap-config.yaml=$COLOR_SWAP_YAML" \
    --dry-run=client -o yaml \
    | kubectl apply --namespace "$NAMESPACE" -f -

echo "Secret '$SECRET_NAME' applied to namespace '$NAMESPACE'."
