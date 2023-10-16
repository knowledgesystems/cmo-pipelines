#!/bin/bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-genie-data.lock"
(
    echo $(date)

    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        exit 0
    fi

    # set necessary env variables with automation-environment.sh
    if [[ -z $PORTAL_HOME || -z $JAVA_BINARY ]] ; then
        echo "Error : import-aws-gdac-data.sh cannot be run without setting PORTAL_HOME and JAVA_BINARY environment variables. (Use automation-environment.sh)"
        exit 1
    fi

    source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

    tmp=$PORTAL_HOME/tmp/import-cron-genie-archive
    if ! [ -d "$tmp" ] ; then
        if ! mkdir -p "$tmp" ; then
            echo "Error : could not create tmp directory '$tmp'" >&2
            exit 1
        fi
    fi
    if [[ -d "$tmp" && "$tmp" != "/" ]]; then
        rm -rf "$tmp"/*
    fi
    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/genie-archive-importer.jar"
    ENABLE_DEBUGGING=0
    java_debug_args=""
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27186"
    fi
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    genie_archive_portal_notification_file=$(mktemp $tmp/genie-archive-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE=oncotree_2018_06_01
    CLEAR_PERSISTENCE_CACHE=0 # 0 = do not clear cache, non-0 = clear cache

    echo $now : starting import
    CDD_ONCOTREE_RECACHE_FAIL=0
    if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
        # refresh cdd and oncotree cache
        bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
        if [ $? -gt 0 ]; then
            CDD_ONCOTREE_RECACHE_FAIL=1
            message="Failed to refresh CDD and/or ONCOTREE cache during GENIE archive import!"
            echo $message
            echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        fi
    fi

    DB_VERSION_FAIL=0
    # check database version before importing anything
    echo "Checking if database version is compatible"
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ]; then
        echo "Database version expected by portal does not match version in database!"
        DB_VERSION_FAIL=1
    fi

    # fetch updates in genie repository
    echo "fetching updates from genie..."
    GENIE_FETCH_FAIL=0
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source genie --run-date latest
    if [ $? -gt 0 ]; then
        echo "genie fetch failed!"
        GENIE_FETCH_FAIL=1
        EMAIL_BODY="The genie data fetch failed."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: genie" $PIPELINES_EMAIL_LIST
    fi

    if [[ $DB_VERSION_FAIL -eq 0 && $GENIE_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
        # import genie studies into genie portal
        echo "importing cancer type updates into genie archive portal database..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
        echo "importing study data into genie archive portal database..."
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-archive-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        IMPORT_EXIT_STATUS=$?
        if [ $IMPORT_EXIT_STATUS -ne 0 ]; then
            echo "Genie archive import failed!"
            EMAIL_BODY="Genie archive import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: Genie Archive" $PIPELINES_EMAIL_LIST
        fi
        num_studies_updated=''
        num_studies_updated_filename="$tmp/num_studies_updated.txt"
        if [ -r "$num_studies_updated_filename" ] ; then
            num_studies_updated=$(cat "$num_studies_updated_filename")
        fi
        if [ -z $num_studies_updated ] ; then
            echo "could not determine the number of studies that have been updated"
            # if import fails [presumed to have failed if num_studies_updated.txt is missing or empty], some checked-off studies still may have been successfully imported, so clear the persistence caches
            CLEAR_PERSISTENCE_CACHE=1
        else
            echo "'$num_studies_updated' studies have been updated"
            if [[ $num_studies_updated != "0" ]]; then
                # if at least 1 study was imported, clear the persistence cache
                CLEAR_PERSISTENCE_CACHE=1
            fi
        fi

        # clear persistence cache
        if [ $CLEAR_PERSISTENCE_CACHE -ne 0 ] ; then
            echo "clearing persistence cache for genie archive portal ..."
            if ! clearPersistenceCachesForGenieArchivePortals ; then
                sendClearCacheFailureMessage genie-archive import-genie-archive.sh
            fi
        else
            echo "No studies have been updated, not clearing persistence cache for genie archive portal"
        fi
    fi

    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        EMAIL_BODY="The genie archive database version is incompatible. Imports will be skipped until database is updated."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GENIE Archive Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
    fi

    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal genie-archive-portal --notification-file "$genie_archive_portal_notification_file"

    echo "Cleaning up any untracked files from MSK-TRIAGE import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/genie
) {flock_fd}>$FLOCK_FILEPATH
