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
    source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
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
    java_debug_args=""
    ENABLE_DEBUGGING=0
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184"
    fi
    msk_automation_notification_file=$(mktemp $tmp/msk-automation-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
    CANCERSTUDIESLOGFILENAME="$PORTAL_HOME/logs/update-studies-dashi-gdac.log"
    DATA_SOURCES_TO_BE_FETCHED="bic-mskcc-legacy cmo-argos private impact datahub_shahlab msk-mind-datahub"
    GMAIL_USERNAME=`grep gmail_username $GMAIL_CREDS_FILE | sed 's/^.*=//g'`
    GMAIL_PASSWORD=`grep gmail_password $GMAIL_CREDS_FILE | sed 's/^.*=//g'`

    # Get the current production database color
    GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/get_database_currently_in_production.sh"
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_database_update_tools.properties"
    current_production_database_color=$($GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
    destination_database_color="unset"
    if [ ${current_production_database_color:0:4} == "blue" ] ; then
        destination_database_color="green"
    fi
    if [ ${current_production_database_color:0:5} == "green" ] ; then
        destination_database_color="blue"
    fi
    if [ "$destination_database_color" == "unset" ] ; then
        echo "Error during determination of the destination database color" >&2
        exit 1
    fi
    DATA_SOURCE_MANAGER_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/data_source_repo_clone_manager.sh"
    DATA_SOURCE_MANAGER_CONFIG_FILEPATH="$PORTAL_HOME/pipelines-credentials/importer-data-source-manager-config.yaml"
    CMO_IMPORTER_JAR_FILENAME="/data/portal-cron/lib/msk-cmo-$destination_database_color-importer.jar"
    CMO_JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $CMO_IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

    # Get the current production database color
    GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/get_database_currently_in_production.sh"
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_database_update_tools.properties"
    current_production_database_color=$($GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
    destination_database_color="unset"
    if [ ${current_production_database_color:0:4} == "blue" ] ; then
        destination_database_color="green"
    fi
    if [ ${current_production_database_color:0:5} == "green" ] ; then
        destination_database_color="blue"
    fi
    if [ "$destination_database_color" == "unset" ] ; then
        echo "Error during determination of the destination database color" >&2
        exit 1
    fi
    CMO_IMPORTER_JAR_FILENAME="/data/portal-cron/lib/msk-cmo-$destination_database_color-importer.jar"
    CMO_JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $CMO_IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

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

    DATA_SOURCE_REPO_FETCH_FAIL=0
    if ! $DATA_SOURCE_MANAGER_SCRIPT_FILEPATH $DATA_SOURCE_MANAGER_CONFIG_FILEPATH pull $DATA_SOURCES_TO_BE_FETCHED ; then
        DATA_SOURCE_REPO_FETCH_FAIL=1
    fi

    DB_VERSION_FAIL=0
    # check database version before importing anything
    echo "Checking if database version is compatible"
    $JAVA_BINARY $CMO_JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ]; then
        echo "Database version expected by portal does not match version in database!"
        DB_VERSION_FAIL=1
    fi

    if [[ $DB_VERSION_FAIL -eq 0 && $DATA_SOURCE_REPO_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
        # import vetted studies into MSK portal
        echo "importing cancer type updates into msk portal database..."
        $JAVA_BINARY -Xmx16g $CMO_JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
        echo "importing study data into msk portal database..."
        IMPORT_FAIL=0
        $JAVA_BINARY -Xmx64g $CMO_JAVA_IMPORTER_ARGS --update-study-data --portal msk-automation-portal --update-worksheet --notification-file "$msk_automation_notification_file" --use-never-import --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -gt 0 ]; then
            echo "MSK CMO import failed!"
            IMPORT_FAIL=1
            EMAIL_BODY="MSK CMO import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: MSK CMO" $CMO_EMAIL_LIST
        fi

        num_studies_updated=`cat $tmp/num_studies_updated.txt`
    fi

    EMAIL_BODY="The GDAC database version is incompatible. Imports will be skipped until database is updated."
    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GDAC Update Failure: DB version is incompatible" $CMO_EMAIL_LIST
    fi

    echo "Cleaning up any untracked files from MSK-CMO import..."
    $DATA_SOURCE_MANAGER_SCRIPT_FILEPATH $DATA_SOURCE_MANAGER_CONFIG_FILEPATH cleanup $DATA_SOURCES_TO_BE_FETCHED

    $JAVA_BINARY $CMO_JAVA_IMPORTER_ARGS --send-update-notification --portal msk-automation-portal --notification-file "$msk_automation_notification_file"

    echo "### Starting import" >> "$CANCERSTUDIESLOGFILENAME"

    date >> "$CANCERSTUDIESLOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true --gmail-username $GMAIL_USERNAME --gmail-password $GMAIL_PASSWORD >> "$CANCERSTUDIESLOGFILENAME" 2>&1

) {flock_fd}>$FLOCK_FILEPATH
