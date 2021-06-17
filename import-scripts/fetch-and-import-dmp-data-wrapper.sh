#!/bin/bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"
DMP_FETCH_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-dmp-data-for-import.lock"

SKIP_DMP_IMPORT_AFTER_HHMM=0700

(
    date

    # check lock so that executions of this script not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    # The dmp imports will wait until cmo imports are complete
    date
    echo "executing import-cmo-data-msk.sh"
    /data/portal-cron/scripts/import-cmo-data-msk.sh
    date
    fetch_dmp_data_complete=0 # not yet complete
    echo "acquiring fetch-dmp-data.sh lock -- if so fetch is complete"
    while [ "$fetch_dmp_data_complete" -eq 0 ] ; do
        # give up at 07:00 - we will skip over our dmp import attempt today because we don't want to start too late
        if [ $(date +"%H%M") -gt "$SKIP_DMP_IMPORT_AFTER_HHMM" ] ; then
            echo "Giving up while waiting for completion of fetch-dmp-data.sh : time has passed $SKIP_DMP_IMPORT_AFTER_HHMM."
            break
        fi
        sleep 60 # time between tests
        if flock --nonblock --exclusive $dmp_fetch_flock_fd ; then
            date
            echo "lock acquired"
            fetch_dmp_data_complete=1
        fi
    done
    date
    if [ "$fetch_dmp_data_complete" -ne 0 ] ; then
        echo "executing import-dmp-impact-data.sh"
        /data/portal-cron/scripts/import-dmp-impact-data.sh
    else
        echo "skipping import-dmp-impact-data.sh"
    fi
    date
    echo "releasing fetch-dmp-data.sh lock -- import is complete now so a new fetch could be allowed to start"
    flock --unlock $dmp_fetch_flock_fd
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
) {my_flock_fd}>$MY_FLOCK_FILEPATH {dmp_fetch_flock_fd}>$DMP_FETCH_FLOCK_FILEPATH
