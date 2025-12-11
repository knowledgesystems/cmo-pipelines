#!/usr/bin/env bash

# Get current instance class
rds_current_class() {
    local id="$1"

    aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].DBInstanceClass' \
        --output text
}

# Get current instance status
rds_current_status() {
    local id="$1"

    aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].DBInstanceStatus' \
        --output text
}

# Start instance
rds_start() {
    local id="$1"

    aws rds start-db-instance --db-instance-identifier "$id" --no-cli-pager
    aws rds wait db-instance-available --db-instance-identifier "$id"
}

# Stop instance
rds_stop() {
    local id="$1"

    aws rds stop-db-instance --db-instance-identifier "$id" --no-cli-pager

    # The functionality to check whether an RDS instance is stopped doesn't yet exist from the AWS CLI;
    # we have to emulate it ourselves manually
    # aws rds wait db-instance-stopped --db-instance-identifier "$id"

    while true; do
        STATUS=$(rds_current_status "$id")
        
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
    local new_class="$2"
    local engine engine_version

    read -r engine engine_version <<<"$(aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].[Engine,EngineVersion]' \
        --output text)"

    if [[ -z "$engine" || -z "$engine_version" ]]; then
        echo "Unable to determine engine/version for '$id'" >&2
        return 1
    fi

    aws rds describe-orderable-db-instance-options \
        --engine "$engine" \
        --engine-version "$engine_version" \
        --query "OrderableDBInstanceOptions[?DBInstanceClass=='$new_class'].DBInstanceClass" \
        --output text | grep -qw "$new_class"
}

# Explicitly set class
rds_set_class() {
    local id="$1"
    local new_class="$2"

    if ! rds_validate_class "$id" "$new_class"; then
        echo "Invalid DB instance class '$new_class' for '$id'" >&2
        return 1
    fi

    aws rds modify-db-instance \
        --db-instance-identifier "$id" \
        --db-instance-class "$new_class" \
        --apply-immediately \
        --no-cli-pager

    aws rds wait db-instance-available --db-instance-identifier "$id"
}
