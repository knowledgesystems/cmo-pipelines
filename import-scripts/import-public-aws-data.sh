#!/bin/bash

# set necessary env variables with automation-environment.sh
if [[ -z $PORTAL_HOME || -z $JAVA_BINARY ]] ; then
    echo "Error : import-public-aws-data.sh cannot be run without setting PORTAL_HOME and JAVA_BINARY environment variables. (Use automation-environment.sh)"
    exit 1
fi

tmp=$PORTAL_HOME/tmp/import-cron-public-aws
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
email_list="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/public-aws-importer.jar"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27185"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
public_aws_portal_notification_file=$(mktemp $tmp/aws-public-portal-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_latest_stable

CDD_ONCOTREE_RECACHE_FAIL=0
if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
    # refresh cdd and oncotree cache
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
    if [ $? -gt 0 ]; then
        CDD_ONCOTREE_RECACHE_FAIL=1
        message="Failed to refresh CDD and/or ONCOTREE cache during TRIAGE import!"
        echo $message
        echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $email_list
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

# fetch updates in studies repository
echo "fetching updates from cbio-portal-data..."
CBIO_PORTAL_DATA_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source knowledge-systems-curated-studies --run-date latest
if [ $? -gt 0 ]; then
    echo "cbio-portal-data fetch failed!"
    CBIO_PORTAL_DATA_FETCH_FAIL=1
    EMAIL_BODY="The cbio-portal-data data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: cbio-portal-data" $email_list
fi

# fetch updates in CMO impact
echo "fetching updates from impact..."
CMO_IMPACT_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source impact --run-date latest
if [ $? -gt 0 ]; then
    echo "impact fetch failed!"
    CMO_IMPACT_FETCH_FAIL=1
    EMAIL_BODY="The impact data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: impact" $email_list
fi

# fetch updates in private repository
echo "fetching updates from private..."
PRIVATE_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source private --run-date latest
if [ $? -gt 0 ]; then
    echo "private fetch failed!"
    PRIVATE_FETCH_FAIL=1
    EMAIL_BODY="The private data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: private" $email_list
fi

echo "fetching updates from datahub..."
DATAHUB_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source datahub --run-date latest
if [ $? -gt 0 ]; then
    echo "datahub fetch failed!"
    DATAHUB_FETCH_FAIL=1
    EMAIL_BODY="The datahub data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: datahub" $pipeline_email_list
fi

if [[ $DB_VERSION_FAIL -eq 0 && $PRIVATE_FETCH_FAIL -eq 0 && $CMO_IMPACT_FETCH_FAIL -eq 0 && $CBIO_PORTAL_DATA_FETCH_FAIL -eq 0 && $DATAHUB_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
    # import public studies into public aws portal
    echo "importing cancer type updates into public aws portal database..."
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    echo "importing study data into public aws portal database..."
    IMPORT_FAIL=0
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal public-aws-portal --update-worksheet --notification-file "$public_aws_portal_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source uniprot
    if [ $? -gt 0 ]; then
        echo "Public aws import failed!"
        IMPORT_FAIL=1
        EMAIL_BODY="Public aws import failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: public aws" $email_list
    fi
    num_studies_updated=`cat $tmp/num_studies_updated.txt`
    echo "'$num_studies_updated' studies have been updated"
fi

# TODO: replace this with restart of Kubernetes instance
PUBLIC_TOMCAT_RESTART=1
if [ $PUBLIC_TOMCAT_RESTART -eq 0 ] ; then
echo "requesting redeployment of public aws portal war..."
   TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
   TOMCAT_HOST_USERNAME=cbioportal_importer
   TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_public_tomcat_restarts_key
   TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/public-tomcat-restart
   SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
   declare -a failed_restart_server_list
   for server in ${TOMCAT_HOST_LIST[@]}; do
       if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
           failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
       fi
   done
   if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
       EMAIL_BODY="Attempt to trigger a restart of the public-tomcat server on the following hosts failed: ${failed_restart_server_list[*]}"
       echo -e "Sending email $EMAIL_BODY"
       echo -e "$EMAIL_BODY" | mail -s "Public Tomcat Restart Error : unable to trigger restart" $email_list
   fi
fi

EMAIL_BODY="The Public aws database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Public AWS Update Failure: DB version is incompatible" $email_list
fi

$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal public-aws-portal --notification-file "$public_aws_portal_notification_file"

echo "Cleaning up any untracked files from CBIO-PUBLIC AWS import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/impact $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/datahub
