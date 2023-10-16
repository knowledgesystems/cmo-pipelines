#!/bin/bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-genie-data.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        exit 0
    fi

    # set necessary env variables with automation-environment.sh
    if [[ -z "$PORTAL_HOME" || -z "$JAVA_BINARY" || -z "$START_GENIE_IMPORT_TRIGGER_FILENAME" || -z "$KILL_GENIE_IMPORT_TRIGGER_FILENAME" || -z "$GENIE_IMPORT_IN_PROGRESS_FILENAME" || -z "$GENIE_IMPORT_KILLING_FILENAME" ]] ; then
        echo "Error : import-genie-data.sh cannot be run without setting the PORTAL_HOME, JAVA_BINARY, START_GENIE_IMPORT_TRIGGER_FILENAME, KILL_GENIE_IMPORT_TRIGGER_FILENAME, GENIE_IMPORT_IN_PROGRESS_FILENAME, GENIE_IMPORT_KILLING_FILENAME environment variables. (Use automation-environment.sh)"
        exit 1
    fi

    if [ -f "$KILL_GENIE_IMPORT_TRIGGER_FILENAME" ] || [ -f "$GENIE_IMPORT_KILLING_FILENAME" ] ; then
        # if import kill is happening or is triggered, cancel any import trigger and exit
        rm -f "$START_GENIE_IMPORT_TRIGGER_FILENAME"
        exit 1
    fi

    # inhibit import attempt if db mismatch occurred recently
    inhibit_counter=0
    if [ -f "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME" ] ; then
        unset inhibit_counter_line
        read inhibit_counter_line < $GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME
        COUNTER_RE='([0-9]+)'
        if [[ $inhibit_counter_line =~ $COUNTER_RE ]] ; then
            inhibit_counter=${BASH_REMATCH[0]}
        else
            # inhibit file is corrupted (no counter on first line)
            rm -f "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME"
        fi
    fi
    if [ $inhibit_counter -ne 0 ] ; then
        # decrease inhibit counter and exit
        echo $(($inhibit_counter-1)) > "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME"
        exit 1
    else
        # clean up inhibit file with zero counter
        rm -f "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME"
    fi

    if ! [ -f "$START_GENIE_IMPORT_TRIGGER_FILENAME" ] ; then
        # no start trigger for import, so exit
        exit 1
    fi

    source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

    tmp=$PORTAL_HOME/tmp/import-cron-genie
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
    VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START=10 # inhibition cycle count after sending db version mismatch error email
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/genie-aws-importer.jar"
    ENABLE_DEBUGGING=0
    java_debug_args=""
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27186"
    fi
    JAVA_IMPORTER_ARGS="$java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    genie_portal_notification_file=$(mktemp $tmp/genie-portal-update-notification.$now.XXXXXX)
    IMPORTING_GENIE_RELEASE_BEFORE_9_0=0
    if [ $IMPORTING_GENIE_RELEASE_BEFORE_9_0 != "0" ] ; then
        oncotree_version_to_use=oncotree_2018_06_01
    else
        oncotree_version_to_use=oncotree_2019_12_01
    fi
    CLEAR_PERSISTENCE_CACHE=0 # 0 = do not clear cache, non-0 = clear cache

    # import is beginning - create status file showing "in_progress", and remove trigger
    rm -f "$START_GENIE_IMPORT_TRIGGER_FILENAME"
    touch "$GENIE_IMPORT_IN_PROGRESS_FILENAME"

    echo $now : starting import
    CDD_ONCOTREE_RECACHE_FAIL=0
    if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
        # refresh cdd and oncotree cache
        bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
        if [ $? -gt 0 ]; then
            CDD_ONCOTREE_RECACHE_FAIL=1
            message="Failed to refresh CDD and/or ONCOTREE cache during GENIE import!"
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
        echo "importing cancer type updates into genie portal database..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${oncotree_version_to_use}
        echo "importing study data into genie portal database..."
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version ${oncotree_version_to_use} --transcript-overrides-source mskcc --disable-redcap-export
        IMPORT_EXIT_STATUS=$?
        if [ $IMPORT_EXIT_STATUS -ne 0 ]; then
            echo "Genie import failed!"
            EMAIL_BODY="Genie import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: Genie" $PIPELINES_EMAIL_LIST
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
            echo "clearing persistence cache for genie portal ..."
            if ! clearPersistenceCachesForGeniePortals ; then
                sendClearCacheFailureMessage genie import-genie-data.sh
            fi
        else
            echo "No studies have been updated, skipping redeployment of genie portal pods"
        fi
    fi

    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        EMAIL_BODY="The genie database version is incompatible. Imports will be skipped until database is updated, and file $GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME has been created to inhibit imports for the next $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START minutes."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GENIE Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
        echo -e "Inhibiting import attempts for $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START cycles/minutes"
        echo $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START > "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME"
    fi

    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal genie-portal --notification-file "$genie_portal_notification_file"

    echo "Cleaning up any untracked files from GENIE import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/genie

    # import is done - remove status file showing "in_progress"
    rm -f "$GENIE_IMPORT_IN_PROGRESS_FILENAME"

) {flock_fd}>$FLOCK_FILEPATH
