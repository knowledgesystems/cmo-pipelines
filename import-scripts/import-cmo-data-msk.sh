#!/bin/bash

# set necessary env variables with automation-environment.sh

FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-cmo-data-msk.lock"
(
    echo $(date)

    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        echo "Failure : could not acquire lock for $FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    if [ -z "$PORTAL_HOME" ] ; then
        echo "Error : import-cmo-data-msk.sh cannot be run without setting the PORTAL_HOME environment variable. (Use automation-environment.sh)"
        exit 1
    fi
    # set data source env variables
    source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh
    source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

    tmp=$PORTAL_HOME/tmp/import-cron-cmo-msk
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
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-cmo-importer.jar"
    java_debug_args=""
    ENABLE_DEBUGGING=0
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184"
    fi
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    msk_automation_notification_file=$(mktemp $tmp/msk-automation-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
    CANCERSTUDIESLOGFILENAME="$PORTAL_HOME/logs/update-studies-dashi-gdac.log"
    DATA_SOURCES_TO_BE_FETCHED="bic-mskcc-legacy cmo-argos private impact datahub_shahlab msk-mind-datahub"
    GMAIL_USERNAME=`grep gmail_username $GMAIL_CREDS_FILE | sed 's/^.*=//g'`
    GMAIL_PASSWORD=`grep gmail_password $GMAIL_CREDS_FILE | sed 's/^.*=//g'`
    unset failed_data_source_fetches
    declare -a failed_data_source_fetches

    CDD_ONCOTREE_RECACHE_FAIL=0
    if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
        # refresh cdd and oncotree cache
        bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
        if [ $? -gt 0 ]; then
            CDD_ONCOTREE_RECACHE_FAIL=1
            message="Failed to refresh CDD and/or ONCOTREE cache during TRIAGE import!"
            echo $message
            echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        fi
    fi

    # fetch updates to data source repos
    fetch_updates_in_data_sources $DATA_SOURCES_TO_BE_FETCHED

    DB_VERSION_FAIL=0
    # check database version before importing anything
    echo "Checking if database version is compatible"
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ]; then
        echo "Database version expected by portal does not match version in database!"
        DB_VERSION_FAIL=1
    fi

    if [[ $DB_VERSION_FAIL -eq 0 && ${#failed_data_source_fetches[*]} -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
        # import vetted studies into MSK portal
        echo "importing cancer type updates into msk portal database..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
        echo "importing study data into msk portal database..."
        IMPORT_FAIL=0
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal msk-automation-portal --update-worksheet --notification-file "$msk_automation_notification_file" --use-never-import --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -gt 0 ]; then
            echo "MSK CMO import failed!"
            IMPORT_FAIL=1
            EMAIL_BODY="MSK CMO import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: MSK CMO" $CMO_EMAIL_LIST
        fi

        num_studies_updated=`cat $tmp/num_studies_updated.txt`

        # clear persistence cache
        if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
            echo "'$num_studies_updated' studies have been updated, clearing persistence cache of msk portal..."
            if ! clearPersistenceCachesForMskPortals ; then
                sendClearCacheFailureMessage msk import-cmo-data-msk.sh
            fi
        else
            echo "No studies have been updated, not clearing persistence cache of msk portal..."
        fi
    fi

    EMAIL_BODY="The GDAC database version is incompatible. Imports will be skipped until database is updated."
    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GDAC Update Failure: DB version is incompatible" $CMO_EMAIL_LIST
    fi

    echo "Cleaning up any untracked files from MSK-CMO import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/bic-mskcc-legacy $PORTAL_DATA_HOME/cmo-argos $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/datahub_shahlab $PORTAL_DATA_HOME/msk-mind

    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal msk-automation-portal --notification-file "$msk_automation_notification_file"

    echo "### Starting import" >> "$CANCERSTUDIESLOGFILENAME"

    date >> "$CANCERSTUDIESLOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true --gmail-username $GMAIL_USERNAME --gmail-password $GMAIL_PASSWORD >> "$CANCERSTUDIESLOGFILENAME" 2>&1
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk import-cmo-data-msk.sh
    fi
    if ! clearPersistenceCachesForExternalPortals ; then
        sendClearCacheFailureMessage external import-cmo-data-msk.sh
    fi

) {flock_fd}>$FLOCK_FILEPATH
