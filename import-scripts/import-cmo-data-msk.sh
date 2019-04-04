#!/bin/bash

# set necessary env variables with automation-environment.sh

# we need this file for the tomcat restart funcions
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

tmp=$PORTAL_HOME/tmp/import-cron-cmo-msk
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
email_list="cbioportal-cmo-importer@cbio.mskcc.org"
pipeline_email_list="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-cmo-importer.jar"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
msk_automation_notification_file=$(mktemp $tmp/msk-automation-portal-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
CANCERSTUDIESLOGFILENAME="$PORTAL_HOME/logs/update-studies-dashi-gdac.log"

CDD_ONCOTREE_RECACHE_FAIL=0
if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
    # refresh cdd and oncotree cache
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
    if [ $? -gt 0 ]; then
        CDD_ONCOTREE_RECACHE_FAIL=1
        message="Failed to refresh CDD and/or ONCOTREE cache during TRIAGE import!"
        echo $message
        echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $pipeline_email_list
    fi
fi

# fetch updates in CMO repository
echo "fetching updates from bic-mskcc..."
CMO_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source bic-mskcc --run-date latest --update-worksheet
if [ $? -gt 0 ]; then
    echo "CMO (bic-mskcc) fetch failed!"
    CMO_FETCH_FAIL=1
    EMAIL_BODY="The CMO (bic-mskcc) data fetch failed. Imports into Triage and production WILL NOT HAVE UP-TO-DATE DATA until this is resolved.\n\n*** DO NOT MARK STUDIES FOR IMPORT INTO msk-automation-portal. ***\n\n*** DO NOT MERGE ANY STUDIES until this has been resolved. Please uncheck any merged studies in the cBio Portal Google document. ***\n\nYou may keep projects marked for import into Triage in the cBio Portal Google document. Triage studies will be reimported once there has been a successful data fetch.\n\nPlease don't hesitate to ask if you have any questions."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: CMO (bic-mskcc)" $email_list
fi

# fetch updates in private repository
echo "fetching updates from private..."
PRIVATE_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source private --run-date latest
if [ $? -gt 0 ]; then
    echo "CMO (private) fetch failed!"
    PRIVATE_FETCH_FAIL=1
    EMAIL_BODY="The CMO (private) data fetch failed. Imports into Triage and production WILL NOT HAVE UP-TO-DATE DATA until this is resolved.\n\n*** DO NOT MARK STUDIES FOR IMPORT INTO msk-automation-portal. ***\n\n*** DO NOT MERGE ANY STUDIES until this has been resolved. Please uncheck any merged studies in the cBio Portal Google document. ***\n\nYou may keep projects marked for import into Triage in the cBio Portal Google document. Triage studies will be reimported once there has been a successful data fetch.\n\nPlease don't hesitate to ask if you have any questions."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: CMO (private)" $email_list
fi

# fetch updates in shah-lab repository
echo "fetching updates from shah-lab..."
SHAH_LAB_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source shah-lab --run-date latest
if [ $? -gt 0 ]; then
    echo "CMO (shah-lab) fetch failed!"
    SHAH_LAB_FETCH_FAIL=1
    EMAIL_BODY="The CMO (shah-lab) data fetch failed. Imports into Triage and production WILL NOT HAVE UP-TO-DATE DATA until this is resolved.\n\n*** DO NOT MARK STUDIES FOR IMPORT INTO msk-automation-portal. ***\n\n*** DO NOT MERGE ANY STUDIES until this has been resolved. Please uncheck any merged studies in the cBio Portal Google document. ***\n\nYou may keep projects marked for import into Triage in the cBio Portal Google document. Triage studies will be reimported once there has been a successful data fetch.\n\nPlease don't hesitate to ask if you have any questions."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: CMO (shah-lab)" $email_list
fi

DB_VERSION_FAIL=0
# check database version before importing anything
echo "Checking if database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

if [[ $DB_VERSION_FAIL -eq 0 && $CMO_FETCH_FAIL -eq 0 && $PRIVATE_FETCH_FAIL -eq 0 && $SHAH_LAB_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
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
        echo -e "$EMAIL_BODY" | mail -s "Import failure: MSK CMO" $email_list
    fi

    num_studies_updated=`cat $tmp/num_studies_updated.txt`

    # redeploy war
    if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
        echo "'$num_studies_updated' studies have been updated, requesting redeployment of msk portal war..."
        restartMSKTomcats
        echo "'$num_studies_updated' studies have been updated (no longer need to restart $TOMCAT_SERVER_DISPLAY_NAME server...)"
    else
        echo "No studies have been updated, skipping redeploy of msk portal war..."
    fi
fi

EMAIL_BODY="The GDAC database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "GDAC Update Failure: DB version is incompatible" $email_list
fi

echo "Cleaning up any untracked files from MSK-CMO import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/bic-mskcc $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/shah-lab

$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal msk-automation-portal --notification-file "$msk_automation_notification_file"

echo "### Starting import" >> "$CANCERSTUDIESLOGFILENAME"
date >> "$CANCERSTUDIESLOGFILENAME"
$PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true >> "$CANCERSTUDIESLOGFILENAME" 2>&1
restartMSKTomcats > /dev/null 2>&1
restartSchultzTomcats > /dev/null 2>&1
