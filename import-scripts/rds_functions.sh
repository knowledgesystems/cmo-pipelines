#!/usr/bin/env bash

# Ordered list of classes for up/down scaling
# We are sticking with the R5 instance family, and scaling up/down within that
RDS_CLASSES=(
  db.r5.large
  db.r5.2xlarge
  db.r5.4xlarge
)

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

# Scale up/down helper
_rds_scale() {
    local direction="$1"  # up | down
    local id="$2"

    local curr; curr="$(rds_current_class "$id")"

    local idx=-1
    for i in "${!RDS_CLASSES[@]}"; do
        [[ "${RDS_CLASSES[$i]}" == "$curr" ]] && idx="$i"
    done

    if [[ "$idx" == "-1" ]]; then
        echo "Current class $curr not in RDS_CLASSES list." >&2
        return 1
    fi

    local new_idx
    if [[ "$direction" == "up" ]]; then
        new_idx=$((idx + 1))
    else
        new_idx=$((idx - 1))
    fi

    if [[ "$new_idx" -lt 0 || "$new_idx" -ge "${#RDS_CLASSES[@]}" ]]; then
        echo "Cannot scale $direction from $curr." >&2
        return 1
    fi

    local target="${RDS_CLASSES[$new_idx]}"
    echo "Scaling $id: $curr → $target"
    rds_set_class "$id" "$target"
}

rds_scale_up()   { _rds_scale "up" "$1"; }
rds_scale_down() { _rds_scale "down" "$1"; }
