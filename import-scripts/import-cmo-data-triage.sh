#!/bin/bash

# set necessary env variables with automation-environment.sh

echo $(date)
tmp=$PORTAL_HOME/tmp/import-cron-cmo-triage
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
cmo_email_list="cbioportal-cmo-importer@cbio.mskcc.org"
pipeline_email_list="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/triage-cmo-importer.jar"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
triage_notification_file=$(mktemp $tmp/triage-portal-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release

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
    EMAIL_BODY="The CMO (bic-mskcc) data fetch failed.  Imports into Triage and production WILL NOT HAVE UP-TO-DATE DATA until this is resolved.\n\n*** DO NOT MARK STUDIES FOR IMPORT INTO msk-automation-portal. ***\n\n*** DO NOT MERGE ANY STUDIES until this has been resolved. Please uncheck any merged studies in the cBio Portal Google document. ***\n\nYou may keep projects marked for import into Triage in the cBio Portal Google document.  Triage studies will be reimported once there has been a successful data fetch.\n\nPlease don't hesitate to ask if you have any questions."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: CMO (bic-mskcc)" $cmo_email_list
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
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: private" $pipeline_email_list
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
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: genie" $pipeline_email_list
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
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: impact" $pipeline_email_list
fi

echo "fetching updates from impact-MERGED..."
IMPACT_MERGED_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source impact-MERGED --run-date latest
if [ $? -gt 0 ]; then
    echo "impact-MERGED fetch failed!"
    IMPACT_MERGED_FETCH_FAIL=1
    EMAIL_BODY="The impact-MERGED data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: impact-MERGED" $pipeline_email_list
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
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: cbio-portal-data" $pipeline_email_list
fi

# fetch updates in immunotherapy repository
echo "fetching updates from immunotherapy..."
IMMUNOTHERAPY_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source immunotherapy --run-date latest
if [ $? -gt 0 ]; then
    echo "immunotherapy fetch failed!"
    IMMUNOTHERAPY_FETCH_FAIL=1
    EMAIL_BODY="The immunotherapy data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: immunotherapy" $pipeline_email_list
fi

# fetch updates in datahub repository
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

# fetch updates in shah-lab repository
echo "fetching updates from shah-lab..."
SHAH_LAB_FETCH_FAIL=0
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source shah-lab --run-date latest
if [ $? -gt 0 ]; then
    echo "shah-lab fetch failed!"
    SHAH_LAB_FETCH_FAIL=1
    EMAIL_BODY="The shah-lab data fetch failed."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: shah-lab" $pipeline_email_list
fi


# import data that requires QC into triage portal
echo "importing cancer type updates into triage portal database..."
$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}

$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --apply-overrides --portal triage-portal

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
if [[ $DB_VERSION_FAIL -eq 0 && $CMO_FETCH_FAIL -eq 0 && $PRIVATE_FETCH_FAIL -eq 0 && $GENIE_FETCH_FAIL -eq 0 && $CMO_IMPACT_FETCH_FAIL -eq 0 && $IMPACT_MERGED_FETCH_FAIL -eq 0 && $CBIO_PORTAL_DATA_FETCH_FAIL -eq 0 && $IMMUNOTHERAPY_FETCH_FAIL -eq 0 && $DATAHUB_FETCH_FAIL -eq 0 && $SHAH_LAB_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
    echo "importing study data into triage portal database..."
    IMPORT_FAIL=0
    $JAVA_BINARY -Xmx32G $JAVA_IMPORTER_ARGS --update-study-data --portal triage-portal --use-never-import --update-worksheet --notification-file "$triage_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
    if [ $? -gt 0 ]; then
        echo "Triage import failed!"
        IMPORT_FAIL=1
        EMAIL_BODY="Triage import failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: triage" $pipeline_email_list
    fi
    num_studies_updated=`cat $tmp/num_studies_updated.txt`

    # redeploy war
    if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
        TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Triage Tomcat"
        TOMCAT_SERVER_DISPLAY_NAME="triage-tomcat"
        #echo "'$num_studies_updated' studies have been updated, redeploying triage-portal war..."
        echo "'$num_studies_updated' studies have been updated.  Restarting triage-tomcat server..."
        if ! /usr/bin/sudo /etc/init.d/triage-tomcat restart ; then
            EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $pipeline_email_list
        fi
        #echo "'$num_studies_updated' studies have been updated (no longer need to restart triage-tomcat server...)"
    else
        echo "No studies have been updated, skipping redeploy of triage-portal war..."
    fi

    # import ran and either failed or succeeded
    echo "sending notification email.."
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal triage-portal --notification-file "$triage_notification_file"
fi

EMAIL_BODY="The Triage database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]
then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Triage Update Failure: DB version is incompatible" $cmo_email_list
fi

echo "Cleaning up any untracked files from MSK-TRIAGE import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/bic-mskcc $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/genie $PORTAL_DATA_HOME/impact $PORTAL_DATA_HOME/immunotherapy $PORTAL_DATA_HOME/datahub $PORTAL_DATA_HOME/shah-lab

