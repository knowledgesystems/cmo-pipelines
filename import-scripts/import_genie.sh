#!/bin/bash

# Script for running GENIE import
# Consists of the following:
# - Import of cancer types
# - Import from genie-portal column in spreadsheet

IMPORTER=$1
PORTAL_SCRIPTS_DIRECTORY=$2
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
if [ ! -f $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi
source $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh

# Get the current production database color
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$PORTAL_SCRIPTS_DIRECTORY/airflowdb.properties.test
current_production_database_color=$(sh $PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
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

tmp=$PORTAL_HOME/tmp/import-cron-genie
IMPORTER_JAR_FILENAME="/data/portal-cron/lib/$IMPORTER-aws-importer-$destination_database_color-test.jar"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
ONCOTREE_VERSION="oncotree_2019_12_01"

echo "would have used $IMPORTER_JAR_FILENAME"
echo "Importing cancer type updates into genie portal database..."
$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version $ONCOTREE_VERSION
# TODO error check

echo "Importing study data into genie portal database..."
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-archive-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version $ONCOTREE_VERSION --transcript-overrides-source mskcc --disable-redcap-export
IMPORT_EXIT_STATUS=$?
if [ $IMPORT_EXIT_STATUS -ne 0 ]; then
    echo "Genie import failed!" >&2
    exit 1
fi

num_studies_updated=''
num_studies_updated_filename="$tmp/num_studies_updated.txt"
if [ -r "$num_studies_updated_filename" ] ; then
    num_studies_updated=$(cat "$num_studies_updated_filename")
fi
if [[ -z $num_studies_updated ]] || [[ $num_studies_updated == "0" ]] ; then
    echo "No studies updated, either due to error or failure to mark a study in the spreadsheet" >&2
    exit 1
fi
echo "$num_studies_updated number of studies were updated"
