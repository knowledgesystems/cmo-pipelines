#!/bin/bash

# Script for running PUBLIC import
# Consists of the following:
# - Import of cancer types
# - Import from public-data-portal column in spreadsheet

IMPORTER=$1 # takes "public"
PORTAL_SCRIPTS_DIRECTORY=$2
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$3
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f $AUTOMATION_ENV_SCRIPT_FILEPATH ] ; then
    echo "`date`: Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source $AUTOMATION_ENV_SCRIPT_FILEPATH

# Get the current production database color
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
current_production_database_color=$(sh $GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
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

tmp=$PORTAL_HOME/tmp/import-cron-public-data
IMPORTER_JAR_FILENAME="/data/portal-cron/lib/$IMPORTER-importer-$destination_database_color.jar"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
ONCOTREE_VERSION="oncotree_latest_stable"

# Direct importer logs to stdout
tail -f $PORTAL_HOME/logs/public-data-importer.log &

echo "Destination DB color: $destination_database_color"
echo "Importing with $IMPORTER_JAR_FILENAME"
echo "Importing cancer type updates into mysql database $destination_database_color"
$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version $ONCOTREE_VERSION
if [ $? -gt 0 ]; then
    echo "Error: Cancer type import failed!" >&2
    exit 1
fi

echo "Importing public study data into mysql database $destination_database_color"
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal public-data-portal --update-worksheet --oncotree-version $ONCOTREE_VERSION --transcript-overrides-source uniprot --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "Error: public-data import failed!" >&2
    exit 1
fi

num_studies_updated=''
num_studies_updated_filename="$tmp/num_studies_updated.txt"
if [ -r "$num_studies_updated_filename" ] ; then
    num_studies_updated=$(cat "$num_studies_updated_filename")
fi
if [[ -z $num_studies_updated ]] || [[ $num_studies_updated == "0" ]] ; then
    echo "Error: No studies updated, either due to error or failure to mark a study in the spreadsheet" >&2
    exit 1
fi
echo "$num_studies_updated number of studies were updated"
