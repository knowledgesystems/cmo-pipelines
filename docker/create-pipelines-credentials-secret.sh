#!/bin/bash
# Create / update the `pipelines-credentials` Kubernetes Secret that the
# import_public_hackathon DAG mounts into every task pod (see _POD_OVERRIDE in
# dags/import_public_hackathon.py).
#
# Each --from-file key becomes a file under /data/portal-cron/pipelines-credentials/
# inside the pod, matching the hardcoded *_CONFIG_FILE paths in the DAG.
#
# Usage:
#   ./create-pipelines-credentials-secret.sh <namespace> <creds_dir>
#
#   <creds_dir> must contain:
#     - manage_public_clickhouse_database_update_tools.properties
#     - public-db-color-swap-config.yaml
#
# Re-running updates the Secret in place (apply semantics); mounted pods pick up
# the new contents within ~1 min, no pod restart needed.

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

# `create --dry-run | apply` makes this idempotent (create-or-update).
kubectl create secret generic "$SECRET_NAME" \
    --namespace "$NAMESPACE" \
    --from-file="manage_public_clickhouse_database_update_tools.properties=$CLICKHOUSE_PROPS" \
    --from-file="public-db-color-swap-config.yaml=$COLOR_SWAP_YAML" \
    --dry-run=client -o yaml \
    | kubectl apply --namespace "$NAMESPACE" -f -

echo "Secret '$SECRET_NAME' applied to namespace '$NAMESPACE'."
