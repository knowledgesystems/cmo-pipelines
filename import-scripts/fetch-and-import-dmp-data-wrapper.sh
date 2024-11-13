#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"

SKIP_OVER_ALL_DMP_COHORT_PROCESSING=0
SKIP_DMP_IMPORT_AFTER_HHMM=1200
SKIP_DMP_IMPORT_BEFORE_HHMM=2000

if [ -z "$PORTAL_HOME" ] ; then
    export PORTAL_HOME=/data/portal-cron
fi
source "$PORTAL_HOME/scripts/slack-message-functions.sh"

(
    date
    # check lock so that executions of this script not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    day_of_week_at_process_start=$(date +%u)

    if [[ -z "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" || "$SKIP_OVER_ALL_DMP_COHORT_PROCESSING" == 0 ]] ; then
        date
        echo executing fetch-dmp-data-for-import.sh
        oldwd=$(pwd)
        cd /data/portal-cron/tmp/separate_working_directory_for_dmp
        /data/portal-cron/scripts/fetch-dmp-data-for-import.sh
        # we don't want to start dmp imports too late (after 07:00), and we also do not want to exit prematurely once the script has started
        current_time=$(date +"%H%M")
        if [ "$current_time" -gt "$SKIP_DMP_IMPORT_AFTER_HHMM" ] && [ "$current_time" -lt "$SKIP_DMP_IMPORT_BEFORE_HHMM" ] ; then
            echo "skipping import-dmp-impact-data.sh"
            message=":warning: the import of dmp studies has been skipped because the clock time was after the cutoff ($SKIP_DMP_IMPORT_AFTER_HHMM)"
            send_slack_message_to_channel "#msk-pipeline-logs" "string" "$message"
        else
            echo "executing import-dmp-impact-data.sh"
            /data/portal-cron/scripts/import-dmp-impact-data.sh
        fi
        cd ${oldwd}
    fi
    date
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
    # Only run AstraZeneca updates on Sunday->Monday
    if [ "$day_of_week_at_process_start" -eq 7 ] ; then
        date
        echo "executing update-az-mskimpact.sh"
        /data/portal-cron/scripts/update-az-mskimpact.sh
    fi
    date
    echo "wrapper complete"
) {my_flock_fd}>$MY_FLOCK_FILEPATH
