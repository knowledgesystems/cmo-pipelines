# inputs:
# scale 'up' or 'down' ?
# which portal are we scaling for?
# are we scaling for the green or blue version of that portal?

# logic:
# 1. for the given portal -- determine its 'scale up' and 'scale down' instance types
# - eg. for the public portal: up = r5.4xlarge, down = t3.micro (cheapest possible class)
# - (may want to consider switching to ARM CPUs.
#   in that case -- up = r6g/r7g.4xlarge, down = t4g.micro)
# - for now, abstract it out
# 2. if we're scaling up, validate that we're scaled down. if we're scaling down, validate that we're scaled up
# - this can be checked via rds_get_current_class
# 3. call rds_set_class with the new instance type
# 4. validate that the new instance class matches the one we changed it to

# considerations:
# - where to store the scale up/down instance classes? (eg in a config file?)
# - should the database color be provided as an input to the script, or inferred?
# - do we only want to run this script via airflow, or should it be usable as a general-purpose CLI script?

#!/usr/bin/env bash
set -eEuo pipefail

PORTAL_SCRIPTS_DIRECTORY="$1"
source "$PORTAL_SCRIPTS_DIRECTORY/rds_functions.sh"

DIRECTION="$2"
PORTAL="$3"
COLOR="$4"

[[ "$DIRECTION" == "up" || "$DIRECTION" == "down" ]]
[[ "$PORTAL" == "genie" || "$PORTAL" == "public" ]]
[[ "$COLOR" == "blue" || "$COLOR" == "green" ]]

read_cfg_knob() {
    ; # TODO discuss how to implement
}

get_node_id() {
    ;
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
scale_up_class=$(read_cfg_knob "$PORTAL" 'rds_scale_up_class')
scale_down_class=$(read_cfg_knob "$PORTAL" 'rds_scale_down_class')


# Validate the current class for the given direction
# We should be scaling up from a downsized node, and scaling down from an upsized node
rds_node_id=$(get_node_id "$PORTAL" "$COLOR")
current_class=$(rds_current_class "$rds_node_id")

if [[ "$DIRECTION" == "up" ]]; then
    [[ "$current_class" == "$scale_down_class" ]] || err_mismatched_instance_class
else
    [[ "$current_class" == "$scale_up_class" ]] || err_mismatched_instance_class
fi

# Do the scaling
if [[ "$DIRECTION" == "up" ]]; then
    rds_set_class "$rds_node_id" "$scale_up_class"
else
    rds_set_class "$rds_node_id" "$scale_down_class"
fi

# After scaling: validate that the instance class was changed successfully
new_class=$(rds_current_class "$rds_node_id")
if [[ "$DIRECTION" == "up" ]]; then
    [[ "$new_class" == "$scale_up_class" ]] || err_failed_to_change_instance_class
else
    [[ "$new_class" == "$scale_down_class" ]] || err_failed_to_change_instance_class
fi
