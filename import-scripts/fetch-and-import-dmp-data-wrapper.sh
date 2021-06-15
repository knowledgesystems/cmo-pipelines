#!/bin/bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-and-import-dmp-data-wrapper.lock"
(
    echo $(date)

    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        echo "Failure : could not acquire lock for $FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    date
    echo executing fetch-dmp-data-for-import.sh
    /data/portal-cron/scripts/fetch-dmp-data-for-import.sh
    date
    echo executing import-dmp-impact-data.sh
    /data/portal-cron/scripts/import-dmp-impact-data.sh
    date
    echo executing import-pdx-data.sh
    /data/portal-cron/scripts/import-pdx-data.sh
    #TODO: fix import into AWS GDAC - speed up import time
    #date
    #echo executing import-gdac-aws-data.sh
    #/data/portal-cron/scripts/import-gdac-aws-data.sh
    date
    echo executing update-msk-mind-cohort.sh
    /data/portal-cron/scripts/update-msk-mind-cohort.sh
    date
    echo executing update-msk-spectrum-cohort.sh
    /data/portal-cron/scripts/update-msk-spectrum-cohort.sh
    date
    echo wrapper complete
) {flock_fd}>$FLOCK_FILEPATH
