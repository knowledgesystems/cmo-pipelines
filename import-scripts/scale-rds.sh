#!/usr/bin/env bash
set -eEuo pipefail

DIRECTION="$1"
PORTAL_DATABASE="$2"
COLOR_SWAP_CONFIG_FILEPATH="$3"
SKIP_PRE_VALIDATION="${4:-}"

[[ "$DIRECTION" == "up" || "$DIRECTION" == "down" ]]
[[ "$PORTAL_DATABASE" == "genie" || "$PORTAL_DATABASE" == "public" ]]
[[ -f "$COLOR_SWAP_CONFIG_FILEPATH" ]]

source /data/portal-cron/scripts/automation-environment.sh
source /data/portal-cron/scripts/rds_functions.sh

# Authenticate with AWS
# (for now everything's on the public service acct --
#  but switch on PORTAL_DATABASE and auth for private for msk portal)
/data/portal-cron/scripts/authenticate_service_account.sh public

function read_scalar() {
    local key="$1"
    local value
    value=$("$YQ_BINARY" -r "$key" "$COLOR_SWAP_CONFIG_FILEPATH")
    if [ "$value" == "null" ] || [ -z "$value" ] ; then
        echo "Error : missing required scalar '$key' in $COLOR_SWAP_CONFIG_FILEPATH" >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

get_node_id() {
    case "$PORTAL_DATABASE" in
        public)
            echo "cbioportal-public-db-green"
            ;;
        genie)
            echo "cbioportal-genie-db-blue"
            ;;
        *)
            echo "Unsupported portal: $PORTAL_DATABASE" >&2
            exit 1
            ;;
    esac
}

err_mismatched_instance_class() {
    echo "ERROR: trying to scale $DIRECTION when $rds_node_id node is already scaled $DIRECTION" >&2
    exit 1
}

err_failed_to_change_instance_class() {
    echo "ERROR: failed to scale $rds_node_id node $DIRECTION" >&2
    exit 1
}

# Get the scale up / scale down classes for this portal
echo "Reading configuration knobs from $COLOR_SWAP_CONFIG_FILEPATH"
scale_up_class=$(read_scalar '.rds_scale_up_class')
scale_down_class=$(read_scalar '.rds_scale_down_class')


rds_node_id=$(get_node_id)
current_class=$(rds_current_class "$rds_node_id")

if [[ "$SKIP_PRE_VALIDATION" != "--skip-pre-validation" ]]; then
    # Validate the current class for the given direction
    # We should be scaling up from a downsized node, and scaling down from an upsized node
    echo "Validating RDS instance class pre-scaling"

    if [[ "$DIRECTION" == "up" ]]; then
        [[ "$current_class" == "$scale_down_class" ]] || err_mismatched_instance_class
    else
        [[ "$current_class" == "$scale_up_class" ]] || err_mismatched_instance_class
    fi
fi

# Do the scaling
echo "Scaling node $DIRECTION"
if [[ "$DIRECTION" == "up" ]]; then
    rds_set_class "$rds_node_id" "$scale_up_class"
else
    rds_set_class "$rds_node_id" "$scale_down_class"
fi

# After scaling: validate that the instance class was changed successfully
echo "Validating RDS instance class post-scaling"
new_class=$(rds_current_class "$rds_node_id")
if [[ "$DIRECTION" == "up" ]]; then
    [[ "$new_class" == "$scale_up_class" ]] || err_failed_to_change_instance_class
else
    [[ "$new_class" == "$scale_down_class" ]] || err_failed_to_change_instance_class
fi
