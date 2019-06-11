#!/bin/bash

# set necessary env variables with automation-environment.sh
if [[ -z $PORTAL_HOME || -z $JAVA_BINARY ]] ; then
    echo "Error : import-aws-gdac-data.sh cannot be run without setting PORTAL_HOME and JAVA_BINARY environment variables. (Use automation-environment.sh)"
    exit 1
fi

if [[ ! -f $AWS_GDAC_SSL_TRUSTSTORE || ! -f $AWS_GDAC_SSL_TRUSTSTORE_PASSWORD_FILE ]] ; then
    echo "Error: cannot find SSL truststore and/or truststore password file."
    exit 1
fi

tmp=$PORTAL_HOME/tmp/import-cron-aws-gdac
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
email_list="cbioportal-pipelines@cbio.mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
TRUSTSTORE_PASSWORD=`cat $AWS_GDAC_SSL_TRUSTSTORE_PASSWORD_FILE`
ENABLE_DEBUGGING=0
JAVA_DEBUG_ARGS=""
if [ $ENABLE_DEBUGGING != "0" ] ; then
    JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27185"
fi
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/aws-gdac-importer.jar"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -Djavax.net.ssl.trustStore=$AWS_GDAC_SSL_TRUSTSTORE -Djavax.net.ssl.turstStorePassword=$TRUSTSTORE_PASSWORD -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
static_gdac_aws_notification_file=$(mktemp $tmp/static-aws-gdac-update-notification.$now.XXXXXX)
gdac_aws_notification_file=$(mktemp $tmp/aws-gdac-update-notification.$now.XXXXXX)
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release

DB_VERSION_FAIL=0
# check database version before importing anything
echo "Checking if database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

# all data fetches are ignored because we are using same data from the previous night's import

if [[ $DB_VERSION_FAIL -eq 0 ]]; then
    echo "importing cancer type updates into aws gdac database..."
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}

    # import checked off studies (mirror msk-automation-portal)
    # current there is a google script running at 1AM (right before cmo import) which copies the column
    # if that is changed, google script trigger should also be moved
    echo "importing gdac studies into aws gdac database..."
    GDAC_IMPORT_FAIL=0
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal aws-gdac --update-worksheet --notification-file "$gdac_aws_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
    if [ $? -gt 0 ] ; then
        echo "aws gdac import (cmo) failed!"
        GDAC_IMPORT_FAIL=1
        EMAIL_BODY="Import of gdac studes (cmo) into aws gdac failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: aws gdac" $email_list
    fi
    num_studies_updated=`cat $tmp/num_studes_updated.txt`
    echo "`$num_studies_updated` studies have been updated"
    
    # import static daily studies into aws gdac
    echo "importing daily studies (dmp/pdx) into aws gdac database..."
    STATIC_GDAC_IMPORT_FAIL=0
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal static-aws-gdac --notification-file "$static_gdac_aws_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
    if [ $? -gt 0 ]; then
        echo "aws gdac import (dmp/pdx) failed!"
        STATIC_GDAC_IMPORT_FAIL=1
        EMAIL_BODY="Import of daily studies (dmp/pdx) into aws gdac failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: aws gdac" $email_list
    fi
    num_studies_updated=`cat $tmp/num_studies_updated.txt`
    echo "'$num_studies_updated' studies have been updated"
fi

# no restart of aws tomcat yet

EMAIL_BODY="The aws gdac database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "AWS GDAC Update Failure: DB version is incompatible" $email_list
fi

if [ $GDAC_IMPORT_FAIL -eq 0 ] ; then
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --send-update-notification --portal aws-portal --notification-file "$gdac_aws_notification_file"
else
    echo "Update failed for AWS GDAC studies"
fi

if [ $STATIC_GDAC_IMPORT_FAIL -eq 0 ] ; then
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --send-update-notification --portal static-aws-portal --notification-file "$static_gdac_aws_notification_file"
else
    echo "Update failed for static AWS GDAC studies"
fi

echo "Cleaning up any untracked files from CBIO-AWS_GDAC import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/impact $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/datahub $PORTAL_DATA_HOME/dmp $PORTAL_DATA_HOME/bic-mskcc $PORTAL_DATA_HOME/crdb_pdx
