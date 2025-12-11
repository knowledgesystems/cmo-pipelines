#!/usr/bin/env bash

# Get current instance class
rds_current_class() {
    local id="$1"

    aws rds describe-db-instances \
        --db-instance-identifier "$id" \
        --query 'DBInstances[0].DBInstanceClass' \
        --output text
}

# Start instance
rds_start() {
    local id="$1"

    aws rds start-db-instance --db-instance-identifier "$id"
    aws rds wait db-instance-available --db-instance-identifier "$id"
}

# Stop instance
rds_stop() {
    local id="$1"

    aws rds stop-db-instance --db-instance-identifier "$id"
    aws rds wait db-instance-stopped --db-instance-identifier "$id"
}

# Explicitly set class
rds_set_class() {
    local id="$1"
    local new_class="$2"

    aws rds modify-db-instance \
        --db-instance-identifier "$id" \
        --db-instance-class "$new_class" \
        --apply-immediately

    aws rds wait db-instance-available --db-instance-identifier "$id"
}
