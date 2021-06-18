#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"

SKIP_DMP_IMPORT_AFTER_HHMM=0700

(
    date

    # check lock so that executions of this script not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    date
    echo executing fetch-dmp-data-for-import.sh
    /data/portal-cron/scripts/fetch-dmp-data-for-import.sh
    # we don't want to start dmp imports too late (after 07:00)
    if [ $(date +"%H%M") -gt "$SKIP_DMP_IMPORT_AFTER_HHMM" ] ; then
        echo "skipping import-dmp-impact-data.sh"
    else
        echo "executing import-dmp-impact-data.sh"
        /data/portal-cron/scripts/import-dmp-impact-data.sh
    fi
    date
    # cmo data msk imports now start after dmp imports are done
    echo "executing import-cmo-data-msk.sh"
    /data/portal-cron/scripts/import-cmo-data-msk.sh
    date
    echo "executing import-pdx-data.sh"
    /data/portal-cron/scripts/import-pdx-data.sh
    #TODO: fix import into AWS GDAC - speed up import time
    #date
    #echo "executing import-gdac-aws-data.sh"
    #/data/portal-cron/scripts/import-gdac-aws-data.sh
    date
    echo "executing update-msk-mind-cohort.sh"
    /data/portal-cron/scripts/update-msk-mind-cohort.sh
    date
    echo "executing update-msk-spectrum-cohort.sh"
    /data/portal-cron/scripts/update-msk-spectrum-cohort.sh
    date
    echo "wrapper complete"
) {my_flock_fd}>$MY_FLOCK_FILEPATH
