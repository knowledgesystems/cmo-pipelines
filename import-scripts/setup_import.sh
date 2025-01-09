#!/bin/bash

# Script for running pre-import steps
# Consists of the following:
# - Database check (given a specific importer) // maybe convert to pipeline name
# - Data fetch from provided sources // add arg for accepting other sources
# - Refreshing CDD/Oncotree caches

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
echo "Checking using $IMPORTER_JAR_FILENAME"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

# Database check
echo "Checking if database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Database version expected by portal does not match version in database!" >&2
    exit 1
fi

# Fetch updates in genie repository
echo "Fetching updates from genie..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source genie --run-date latest
if [ $? -gt 0 ]; then
    echo "GENIE fetch failed!" >&2
    exit 1
fi

# Refresh CDD/Oncotree cache to pull latest metadata
echo "Refreshing CDD/ONCOTREE caches..."
bash $PORTAL_SCRIPTS_DIRECTORY/refresh-cdd-oncotree-cache.sh
if [ $? -gt 0 ]; then
    echo "Failed to refresh CDD and/or ONCOTREE cache during GENIE import!" >&2
    exit 1
fi
