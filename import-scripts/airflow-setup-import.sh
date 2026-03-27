#!/bin/bash

# Generic pre-import setup
# - Determines correct importer JAR (color-specific for blue/green importers)
# - Runs DB version check
# - Refreshes CDD cache

PORTAL_DATABASE=$1
PORTAL_SCRIPTS_DIRECTORY=$2
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$3

if [ -z "$PORTAL_SCRIPTS_DIRECTORY" ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi

AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f "$AUTOMATION_ENV_SCRIPT_FILEPATH" ] ; then
    echo "$(date): Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..." >&2
    exit 1
fi
source "$AUTOMATION_ENV_SCRIPT_FILEPATH"

# Configure names/paths based on portal database
case "$PORTAL_DATABASE" in
  genie)
    TMP_DIR_NAME="import-cron-genie"
    IMPORTER_NAME="genie-aws"
    LOG_FILE_NAME="genie-aws-importer.log"
    PORTAL_NAME="genie-portal"
    ;;
  public)
    TMP_DIR_NAME="import-cron-public-data"
    IMPORTER_NAME="public"
    LOG_FILE_NAME="public-data-importer.log"
    PORTAL_NAME="public-portal"
    ;;
  triage-clickhouse)
    TMP_DIR_NAME="import-cron-triage-clickhouse"
    IMPORTER_NAME="triage-clickhouse"
    LOG_FILE_NAME="triage-clickhouse-importer.log"
    PORTAL_NAME="triage-portal"
    ONCOTREE_VERSION="oncotree_candidate_release"
    ;;
  *)
    echo "Unsupported portal database: $PORTAL_DATABASE" >&2
    exit 1
    ;;
esac

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

tmp="$PORTAL_HOME/tmp/$TMP_DIR_NAME"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

# Direct importer logs to stdout
# Make sure to kill the tail process on exit so we don't hang the script
tail -f "$PORTAL_HOME/logs/$LOG_FILE_NAME" &
TAIL_PID=$!
trap 'kill "$TAIL_PID" 2>/dev/null; wait "$TAIL_PID" 2>/dev/null' EXIT INT TERM

echo "Destination DB color: $destination_database_color"
echo "Using importer JAR: $IMPORTER_JAR_FILENAME"

# Database check
echo "Checking if mysql database version is compatible"
"$JAVA_BINARY" $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Error: Database version expected by portal does not match version in database!" >&2
    exit 1
fi
