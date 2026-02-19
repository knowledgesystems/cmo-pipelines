#!/usr/bin/env bash

DEFAULT_WAIT_TIMEOUT_FOR_RDS_INSTANCE_SCALING=3600 # seconds

# Get current instance class
rds_current_class() {
    local id="$1"
    local profile="$2"

    aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].DBInstanceClass' \
        --output text \
        --profile "$profile"
}

# Get current instance status
rds_current_status() {
    local id="$1"
    local profile="$2"

    aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].DBInstanceStatus' \
        --output text \
        --profile "$profile"
}

# Start instance
rds_start() {
    local id="$1"
    local profile="$2"

    aws rds start-db-instance \
        --db-instance-identifier "$id" \
        --no-cli-pager \
        --profile "$profile"
    aws rds wait db-instance-available \
        --db-instance-identifier "$id" \
        --profile "$profile"
}

# Stop instance
rds_stop() {
    local id="$1"
    local profile="$2"

    aws rds stop-db-instance \
        --db-instance-identifier "$id" \
        --no-cli-pager \
        --profile "$profile"

    # The functionality to check whether an RDS instance is stopped doesn't yet exist from the AWS CLI;
    # we have to emulate it ourselves manually
    # aws rds wait db-instance-stopped --db-instance-identifier "$id"

    while true; do
        STATUS=$(rds_current_status "$id" "$profile")
        if [ "$STATUS" = "stopped" ]; then
            # instance is stopped
            break
        fi
        sleep 5
    done
}

# Validate that a class is orderable for the instance's engine/version
rds_validate_class() {
    local id="$1"
    local profile="$2"
    local new_class="$3"
    local engine engine_version

    read -r engine engine_version <<<"$(aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].[Engine,EngineVersion]' \
        --output text \
        --profile "$profile")"

    if [[ -z "$engine" || -z "$engine_version" ]]; then
        echo "Unable to determine engine/version for '$id'" >&2
        return 1
    fi

    aws rds describe-orderable-db-instance-options \
        --engine "$engine" \
        --engine-version "$engine_version" \
        --query "OrderableDBInstanceOptions[?DBInstanceClass=='$new_class'].DBInstanceClass" \
        --output text \
        --profile "$profile" | grep -qw "$new_class"
}

# Explicitly set class
rds_set_class() {
    local id="$1"
    local profile="$2"
    local new_class="$3"
    local timeout_seconds="${4:-$DEFAULT_WAIT_TIMEOUT_FOR_RDS_INSTANCE_SCALING}"

    if ! rds_validate_class "$id" "$profile" "$new_class"; then
        echo "Invalid DB instance class '$new_class' for '$id'" >&2
        return 1
    fi

    aws rds modify-db-instance \
        --db-instance-identifier "$id" \
        --db-instance-class "$new_class" \
        --apply-immediately \
        --no-cli-pager \
        --profile "$profile"

    wait_for_class "$id" "$profile" "$new_class" "$timeout_seconds"
}

# Wait for the instance class change to complete and become available
wait_for_class() {
    local id="$1"
    local profile="$2"
    local new_class="$3"
    local timeout="${4:-$DEFAULT_WAIT_TIMEOUT_FOR_RDS_INSTANCE_SCALING}"
    local start now class status pending

    start=$(date +%s)
    while true; do
        read -r class status pending <<<"$(aws rds describe-db-instances \
            --db-instance-identifier "$id" \
            --query 'DBInstances[0].[DBInstanceClass,DBInstanceStatus,PendingModifiedValues.DBInstanceClass]' \
            --output text \
            --profile "$profile")"

        # Succeed when the new class is applied, instance is available, and no pending modification remains
        if [[ "$class" == "$new_class" && "$status" == "available" && ( "$pending" == "None" || -z "$pending" ) ]]; then
            return 0
        fi
        now=$(date +%s)
        if (( now - start >= timeout )); then
            echo "Timed out waiting for $id to become $new_class (status=$status, pending=$pending, current=$class)" >&2
            return 1
        fi
        sleep 10
    done
}
