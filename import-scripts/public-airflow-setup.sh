#!/bin/bash

# Script for running pre-import steps
# Consists of the following:
# - Database check (given a specific importer)
# - Data fetch from provided sources
# - Refreshing CDD/Oncotree caches

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
echo $AUTOMATION_ENV_SCRIPT_FILEPATH
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

echo "Destination DB color: $destination_database_color"
echo "Importing with $IMPORTER_JAR_FILENAME"

# Database check
echo "Checking if mysql database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Error: Database version expected by portal does not match version in database!" >&2
    exit 1
fi

# Refresh CDD/Oncotree cache to pull latest metadata
echo "Refreshing CDD/ONCOTREE caches"
bash $PORTAL_SCRIPTS_DIRECTORY/refresh-cdd-oncotree-cache.sh
if [ $? -gt 0 ]; then
    echo "Error: Failed to refresh CDD and/or ONCOTREE cache during public-data import!" >&2
    exit 1
fi