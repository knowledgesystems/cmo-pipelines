#!/bin/bash

PORTAL_DATABASE=$1
PORTAL_SCRIPTS_DIRECTORY=$2
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$3

if [ -z "$PORTAL_SCRIPTS_DIRECTORY" ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
AUTOMATION_ENV_SCRIPT_FILEPATH="${PORTAL_SCRIPTS_DIRECTORY}/automation-environment.sh"
if [ ! -f "$AUTOMATION_ENV_SCRIPT_FILEPATH" ] ; then
    echo "$(date): Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source "$AUTOMATION_ENV_SCRIPT_FILEPATH"

# Get the current production database color
GET_DB_IN_PROD_SCRIPT_FILEPATH="${PORTAL_SCRIPTS_DIRECTORY}/get_database_currently_in_production.sh"
current_production_database_color=$(sh "$GET_DB_IN_PROD_SCRIPT_FILEPATH" "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH")
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

# eg. genie-aws-importer-blue.jar
IMPORTER_JAR_FILENAME="/data/portal-cron/lib/${IMPORTER_NAME}-importer-${destination_database_color}.jar"

tmp="${PORTAL_HOME}/tmp/${TMP_DIR_NAME}"
#INTEGRITY_CHECK_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
INTEGRITY_CHECK_ARGS="-cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.portal.scripts.CheckClickHouseConstraints"

"$JAVA_BINARY" $INTEGRITY_CHECK_ARGS
if [ $? -gt 0 ]; then
    echo "Error: Integrity check failed! Will not transfer deployment" >&2
    exit 1
fi
