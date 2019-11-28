#!/bin/bash

# set necessary env variables with automation-environment.sh
if [[ -z $PORTAL_HOME || -z $JAVA_BINARY ]] ; then
    echo "Error : import-aws-gdac-data.sh cannot be run without setting PORTAL_HOME and JAVA_BINARY environment variables. (Use automation-environment.sh)"
    exit 1
fi

if [[ ! -f $AWS_SSL_TRUSTSTORE || ! -f $AWS_SSL_TRUSTSTORE_PASSWORD_FILE ]] ; then
    echo "Error: cannot find SSL truststore and/or truststore password file."
    exit 1
fi

tmp=$PORTAL_HOME/tmp/import-cron-genie
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
TRUSTSTORE_PASSWORD=`cat $AWS_SSL_TRUSTSTORE_PASSWORD_FILE`
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/genie-aws-importer.jar"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27186"
JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$AWS_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
genie_portal_notification_file=$(mktemp $tmp/genie-portal-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_2018_06_01
WEB_APPLICATION_SHOULD_BE_RESTARTED=0 # 0 = skip the restart, non-0 = do the restart

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
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    echo "importing study data into genie portal database..."
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
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
        # if import fails [presumed to have failed if num_studies_updated.txt is missing or empty], some checked-off studies still may have been successfully imported, so restart tomcat
        WEB_APPLICATION_SHOULD_BE_RESTARTED=1
    else
        echo "'$num_studies_updated' studies have been updated"
        if [[ $num_studies_updated != "0" ]]; then
            # if at least 1 study was imported, restart tomcat
            WEB_APPLICATION_SHOULD_BE_RESTARTED=1
        fi
    fi

    # restart pods
    if [ $WEB_APPLICATION_SHOULD_BE_RESTARTED -ne 0 ] ; then
        echo "requesting redeployment of genie portal pods..."
        bash $PORTAL_HOME/scripts/restart-portal-pods.sh genie
        GENIE_RESTART_EXIT_STATUS=$?
        bash $PORTAL_HOME/scripts/restart-portal-pods.sh genie-private
        GENIE_PRIVATE_RESTART_EXIT_STATUS=$?
        if [[ $GENIE_RESTART_EXIT_STATUS -ne 0 || $GENIE_PRIVATE_RESTART_EXIT_STATUS -ne 0 ]] ; then
            EMAIL_BODY="Attempt to trigger a redeployment of all genie and genie-private portal pods failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Genie Portal Pod Redeployment Error : unable to trigger redeployment" $PIPELINES_EMAIL_LIST
        fi
    else
        echo "No studies have been updated, skipping redeployment of genie portal pods"
    fi
fi

# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    EMAIL_BODY="The genie database version is incompatible. Imports will be skipped until database is updated."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "GENIE Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
fi

$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal genie-portal --notification-file "$genie_portal_notification_file"

echo "Cleaning up any untracked files from MSK-TRIAGE import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/genie
