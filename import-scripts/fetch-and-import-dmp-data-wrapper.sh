#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"

SKIP_OVER_ALL_DMP_COHORT_PROCESSING=0

if [ -z "$PORTAL_HOME" ] ; then
    export PORTAL_HOME=/data/portal-cron
fi
source "$PORTAL_HOME/scripts/slack-message-functions.sh"
VERIFY_MANAGEMENT_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/import-msk-verify-management-state.sh"
MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH="$PORTAL_HOME/pipelines-credentials/manage_msk_database_update_tools.properties"
MSK_PREIMPORT_STEPS_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/import-msk-preimport-steps-for-clickhouse.sh"
MSK_PREIMPORT_STEPS_OUTPUT_FILEPATH="$PORTAL_HOME/tmp/import-cron-dmp-wrapper/preimport-steps-for-clickhouse.out"
MSK_PREIMPORT_STEPS_STATUS_FILEPATH="$PORTAL_HOME/tmp/import-cron-dmp-wrapper/preimport-steps-for-clickhouse-result"

function output_whether_preimport_steps_successfully_completed() {
    local MAX_WAIT_FOR_COMPLETION_OF_PREIMPORT_STEPS=$((3*60*60))
    local NUMBER_OF_CHECKS=$((3*12))
    local seconds_between_checks=$((MAX_WAIT_FOR_COMPLETION_OF_PREIMPORT_STEPS/$NUMBER_OF_CHECKS))
    local remaining_checks=$NUMBER_OF_CHECKS
    while [ $remaining_checks -gt 0 ] ; do
        if [ -r "$MSK_PREIMPORT_STEPS_STATUS_FILEPATH" ] ; then
            local status="$(head -n 1 $MSK_PREIMPORT_STEPS_STATUS_FILEPATH)"
            if [ "$status" == "yes" ] ; then
                echo "yes"
            else
                echo "no"
            fi
            return 0
        fi
        $remaining_checks=$(($remaining_checks-1))
        if [ $remaining_checks -gt 0 ] ; then
            sleep $seconds_between_checks
        fi
    done
    echo "no"
    return 0
}

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
    if $VERIFY_MANAGEMENT_SCRIPT_FILEPATH "$MSK_PORTAL_MANAGE_DATABASE_UPDATE_STATUS_PROPERTIES_FILEPATH" ; then
        update_status_is_valid="yes"
    fi
    if [ $update_status_is_valid == "yes" ] ; then
        # Launch the preimport setup script as a background process. This runs for about 2 hours and can run in parallel with fetches.
        rm "$MSK_PREIMPORT_STEPS_STATUS_FILEPATH"
        nohup "$MSK_PREIMPORT_STEPS_SCRIPT_FILEPATH" "$MSK_PREIMPORT_STEPS_STATUS_FILEPATH" > $MSK_PREIMPORT_STEPS_OUTPUT_FILEPATH 2>&1 &
        if [[ -z "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" || "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" == 0 ]] ; then
            date
            echo executing fetch-dmp-data-for-import.sh
            oldwd=$(pwd)
            cd /data/portal-cron/tmp/separate_working_directory_for_dmp
            /data/portal-cron/scripts/fetch-dmp-data-for-import.sh
            databases_are_prepared_for_import=$(output_whether_preimport_steps_successfully_completed)
            if [ "$databases_are_prepared_for_import" == "yes" ] ; then
                echo "executing import-dmp-impact-data.sh"
                /data/portal-cron/scripts/import-dmp-impact-data.sh
            fi
            cd ${oldwd}
        fi
        date
        if [ "$databases_are_prepared_for_import" == "yes" ] ; then
            # cmo data msk imports now start after dmp imports are done
            echo "executing import-cmo-data-msk.sh"
            /data/portal-cron/scripts/import-cmo-data-msk.sh
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
            #complete clickhouse update steps
            $PORTAL_HOME/scripts/import-msk-postimport-steps-for-clickhouse.sh
        else
            echo "skipping all imports into cgds_gdac database because $MSK_PREIMPORT_STEPS_SCRIPT_FILEPATH failed to prepare the database"
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
