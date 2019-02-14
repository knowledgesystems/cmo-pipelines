#!/bin/bash

# set necessary env variables with automation-environment.sh

tmp=$PORTAL_HOME/tmp/import-cron-genie
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
email_list="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/genie-importer.jar"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27186"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
genie_portal_notification_file=$(mktemp $tmp/genie-portal-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_2018_06_01

# refresh cdd and oncotree cache
CDD_ONCOTREE_RECACHE_FAIL=0
bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
if [ $? -gt 0 ]; then
    CDD_ONCOTREE_RECACHE_FAIL=1
    message="Failed to refresh CDD and/or ONCOTREE cache during TRIAGE import!"
    echo $message
    echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $email_list
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
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: genie" $email_list
fi

if [[ $DB_VERSION_FAIL -eq 0 && $GENIE_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
    # import genie studies into genie portal
    echo "importing cancer type updates into genie portal database..."
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    echo "importing study data into genie portal database..."
    IMPORT_FAIL=0
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
    if [ $? -gt 0 ]; then
        echo "Genie import failed!"
        IMPORT_FAIL=1
        EMAIL_BODY="Genie import failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: Genie" $email_list
    fi
    num_studies_updated=`cat $tmp/num_studies_updated.txt`

    # redeploy war
    if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
        echo "'$num_studies_updated' studies have been updated, requesting redeployment of genie portal war..."
        TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
        TOMCAT_HOST_USERNAME=cbioportal_importer
        TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_schultz_tomcat_restarts_key
        TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/schultz-tomcat-restart
        TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Schultz Tomcat" # e.g. Public Tomcat
        TOMCAT_SERVER_DISPLAY_NAME="schultz-tomcat" # e.g. schultz-tomcat
        SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
        declare -a failed_restart_server_list
        for server in ${TOMCAT_HOST_LIST[@]}; do
            if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
                failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
            fi
        done
        if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
            EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
        fi
        echo "'$num_studies_updated' studies have been updated (no longer need to restart $TOMCAT_SERVER_DISPLAY_NAME server...)"
    else
        echo "No studies have been updated, skipping redeploy of schultz portal war..."
    fi
fi

EMAIL_BODY="The genie database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "GENIE Update Failure: DB version is incompatible" $email_list
fi

$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal genie-portal --notification-file "$genie_portal_notification_file"

echo "Cleaning up any untracked files from MSK-TRIAGE import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/genie

if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
