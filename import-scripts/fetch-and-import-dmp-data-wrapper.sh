#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"

SKIP_OVER_ALL_DMP_COHORT_PROCESSING=0

if [ -z "$PORTAL_HOME" ] ; then
    export PORTAL_HOME=/data/portal-cron
fi
source "$PORTAL_HOME/scripts/slack-message-functions.sh"
SET_UPDATE_PROCESS_STATE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/set_update_process_state.sh"
VERIFY_MANAGEMENT_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/verify-management-state.sh"
COLOR_SWAP_CONFIG_FILEPATH="/data/portal-cron/pipelines-credentials/msk-db-color-swap-config.yaml"
MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH="$PORTAL_HOME/pipelines-credentials/manage_msk_database_update_tools.properties"
MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH="$PORTAL_HOME/pipelines-credentials/manage_msk_clickhouse_database_update_tools.properties"
CLONE_DB_OUTPUT_FILEPATH="$PORTAL_HOME/tmp/import-cron-dmp-wrapper/clone-clickhouse-db.out"

(
    date
    # check lock so that executions of this script not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    day_of_week_at_process_start=$(date +%u)
    update_status_is_valid="no"
    databases_are_prepared_for_import="no"
    if $VERIFY_MANAGEMENT_SCRIPT_FILEPATH "$MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" "$COLOR_SWAP_CONFIG_FILEPATH" ; then
        update_status_is_valid="yes"
    fi
    if [ $update_status_is_valid == "yes" ] ; then
        # Attempt to abandon any prior incomplete import attempt. Detect if already running.
        if ! "$SET_UPDATE_PROCESS_STATE_SCRIPT_FILEPATH" "$MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" running ; then 
            echo "Warning : the update management database showed that a prior update attempt seemed to be running." 2>&1
            echo "    Since this script is the only script which should be used to update the msk portal database," 2>&1
            echo "    and because this script will not run if a run is in progress, we are inferring that the prior run" 2>&1
            echo "    is no longer running and the update management database is incorrect. However, this inference will" 2>&1
            echo "    no longer be valid if we introduce any other processes which might independently run an update" 2>&1
            echo "    into the msk portal database. The update management database has been reset by abandoning the attempt." 2>&1
        fi
        "$SET_UPDATE_PROCESS_STATE_SCRIPT_FILEPATH" "$MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" abandoned > /dev/null 2>&1
        # Clone the ClickHouse DB in the background — runs in parallel with data fetch
        mkdir -p "$(dirname "$CLONE_DB_OUTPUT_FILEPATH")"
        nohup "$PORTAL_HOME/scripts/airflow-clone-db.sh" msk-clickhouse "$PORTAL_HOME/scripts" "$MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" > "$CLONE_DB_OUTPUT_FILEPATH" 2>&1 &
        CLONE_DB_PID=$!
        if [[ -z "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" || "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" == 0 ]] ; then
            date
            echo executing fetch-dmp-data-for-import.sh
            oldwd=$(pwd)
            cd /data/portal-cron/tmp/separate_working_directory_for_dmp
            /data/portal-cron/scripts/fetch-dmp-data-for-import.sh
            cd ${oldwd}
        fi
        # Wait for ClickHouse DB clone to finish
        if wait $CLONE_DB_PID ; then
            databases_are_prepared_for_import="yes"
        else
            echo "airflow-clone-db.sh msk-clickhouse failed — see $CLONE_DB_OUTPUT_FILEPATH" >&2
            databases_are_prepared_for_import="no"
        fi
        date
        if [ "$databases_are_prepared_for_import" == "yes" ] ; then
            echo "executing airflow-setup-import.sh msk-clickhouse"
            "$PORTAL_HOME/scripts/airflow-setup-import.sh" msk-clickhouse "$PORTAL_HOME/scripts" "$MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH"
            echo "executing airflow-import-sql.sh msk-clickhouse"
            "$PORTAL_HOME/scripts/airflow-import-sql.sh" msk-clickhouse "$PORTAL_HOME/scripts" "$MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH"
            echo "executing airflow-create-derived-tables.sh msk-clickhouse"
            "$PORTAL_HOME/scripts/airflow-create-derived-tables.sh" msk-clickhouse "$PORTAL_HOME/scripts" "$MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH"
            echo "executing airflow-transfer-deployment.sh msk-clickhouse"
            "$PORTAL_HOME/scripts/airflow-transfer-deployment.sh" "$PORTAL_HOME/scripts" "$MSK_CLICKHOUSE_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" "$COLOR_SWAP_CONFIG_FILEPATH"
            # Only run pdx updates on Friday->Saturday
            if [ "$day_of_week_at_process_start" -eq 5 ] ; then
                date
                echo "executing import-pdx-data.sh"
                /data/portal-cron/scripts/import-pdx-data.sh
            fi
            #date
            #echo "executing update-msk-mind-cohort.sh"
            #/data/portal-cron/scripts/update-msk-mind-cohort.sh
            date
            echo "executing update-msk-spectrum-cohort.sh"
            /data/portal-cron/scripts/update-msk-spectrum-cohort.sh
            echo "executing import-msk-extract-projects.sh"
            /data/portal-cron/scripts/import-msk-extract-projects.sh
        else
            echo "skipping all imports because airflow-clone-db.sh msk-clickhouse failed to prepare the database"
        fi
    else
        echo "skipping all imports into cgds_gdac database because update state is not valid"
    fi
    # Only run AstraZeneca updates on Sunday->Monday
    if [ "$day_of_week_at_process_start" -eq 7 ] ; then
        date
        echo "executing update-az-mskimpact.sh"
        /data/portal-cron/scripts/update-az-mskimpact.sh
    fi
    date
    echo "wrapper complete"
) {my_flock_fd}>$MY_FLOCK_FILEPATH
