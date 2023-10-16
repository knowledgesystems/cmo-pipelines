#!/usr/bin/env bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-hgnc-1938-data.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        exit 0
    fi

    # set necessary env variables with automation-environment.sh
    if [ -z "$PORTAL_HOME" ] || [ -z "$START_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] || [ -z "$KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] || [ -z "$HGNC_1938_IMPORT_IN_PROGRESS_FILENAME" ] || [ -z "$HGNC_1938_IMPORT_KILLING_FILENAME" ] ; then
        echo "Error : import-hgnc-1938-data.sh cannot be run without setting the PORTAL_HOME, START_HGNC_1938_IMPORT_TRIGGER_FILENAME, KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME, HGNC_1938_IMPORT_IN_PROGRESS_FILENAME, HGNC_1938_IMPORT_KILLING_FILENAME environment variables. (Use automation-environment.sh)"
        exit 1
    fi

    if [ -f "$KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] || [ -f "$HGNC_1938_IMPORT_KILLING_FILENAME" ] ; then
        # if import kill is happening or is triggered, cancel any import trigger and exit
        rm -f "$START_HGNC_1938_IMPORT_TRIGGER_FILENAME"
        exit 1
    fi

    if ! [ -f "$START_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] ; then
        # no start trigger for import, so exit
        exit 1
    fi

    echo $(date)

    # set data source env variables
    source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh
    source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

    tmp=$PORTAL_HOME/tmp/import-cron-hgnc-1938
    if ! [ -d "$tmp" ] ; then
        if ! mkdir -p "$tmp" ; then
            echo "Error : could not create tmp directory '$tmp'" >&2
            exit 1
        fi
    fi
    if [[ -d "$tmp" && "$tmp" != "/" ]]; then
        rm -rf "$tmp"/*
    fi
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/hgnc-1938-importer.jar"
    java_debug_args=""
    ENABLE_DEBUGGING=0
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183"
    fi
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    hgnc_notification_file=$(mktemp $tmp/hgnc-1938-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
    DATA_SOURCES_TO_BE_FETCHED="datahub impact private"
    unset failed_data_source_fetches
    declare -a failed_data_source_fetches

    # redirect PORTAL_DATA_HOME to use the fork of datahub (rather than cBioPortal/datahub)
    #export PORTAL_DATA_HOME="$PORTAL_HOME/hgnc-portal-data"

    # import is beginning - create status file showing "in_progress", and remove trigger
    rm -f "$START_HGNC_1938_IMPORT_TRIGGER_FILENAME"
    touch "$HGNC_1938_IMPORT_IN_PROGRESS_FILENAME"

    CDD_ONCOTREE_RECACHE_FAIL=0
    if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
        # refresh cdd and oncotree cache
        bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
        if [ $? -gt 0 ]; then
            CDD_ONCOTREE_RECACHE_FAIL=1
            message="Failed to refresh CDD and/or ONCOTREE cache during HGNC 19-38 import!"
            echo $message
            echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        fi
    fi

    # fetch updates to data source repos
    fetch_updates_in_data_sources $DATA_SOURCES_TO_BE_FETCHED

    # import data that requires QC into hgnc 19-38 portal
    echo "importing cancer type updates into hgnc 19-38 portal database..."
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}

    DB_VERSION_FAIL=0
    # check database version before importing anything
    echo "Checking if database version is compatible"
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ]
    then
        echo "Database version expected by portal does not match version in database!"
        DB_VERSION_FAIL=1
    fi

    # if the database version is correct and ALL fetches succeed, then import
    if [[ $DB_VERSION_FAIL -eq 0 && ${#failed_data_source_fetches[*]} -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
        echo "importing study data into hgnc 19-38 portal database..."
        IMPORT_FAIL=0
        $JAVA_BINARY -Xmx32G $JAVA_IMPORTER_ARGS --update-study-data --portal hgnc-portal-19-38 --use-never-import --update-worksheet --notification-file "$hgnc_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -gt 0 ]; then
            echo "HGNC 19-38 import failed!"
            IMPORT_FAIL=1
            EMAIL_BODY="HGNC 19-38 import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: hgnc 19-38" $PIPELINES_EMAIL_LIST
        fi
        num_studies_updated=`cat $tmp/num_studies_updated.txt`

        # clear persistence cache
        if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
            echo "'$num_studies_updated' studies have been updated, clearing persistence cache for hgnc 19-38 portal..."
            if ! clearPersistenceCachesForHgnc1938Portals ; then
                sendClearCacheFailureMessage "hgnc-1938(msk-beta)" import-data-hgnc-1938.sh
            fi
        else
            echo "No studies have been updated, not clearing persistence cache for hgnc 19-38 portal..."
        fi

        # import ran and either failed or succeeded
        echo "sending notification email.."
        $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal hgnc-portal-19-38 --notification-file "$hgnc_notification_file"
    fi

    EMAIL_BODY="The HGNC 19-38 database version is incompatible. Imports will be skipped until database is updated."
    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]
    then
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "HGNC 19-38 Update Failure: DB version is incompatible" $CMO_EMAIL_LIST
    fi

    echo "Cleaning up any untracked files from HGNC 19-38 import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/datahub $PORTAL_DATA_HOME/impact

    # import is done - remove status file showing "in_progress"
    rm -f "$HGNC_1938_IMPORT_IN_PROGRESS_FILENAME"
) {flock_fd}>$FLOCK_FILEPATH
