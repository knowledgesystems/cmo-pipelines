#!/bin/bash

# Generic pre-import setup
# - Determines correct importer JAR (color-specific for blue/green importers)
# - Runs DB version check
# - Refreshes CDD/Oncotree caches

IMPORTER=$1
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

# Helper: returns success for MySQL-style imports (no blue/green), otherwise failure
is_mysql_import() {
    [[ "$IMPORTER" == "triage" ]]
}

# Configure names/paths based on importer
case "$IMPORTER" in
  genie)
    TMP_DIR_NAME="import-cron-genie"
    IMPORTER_NAME="genie-aws-importer"
    LOG_FILE_NAME="genie-aws-importer.log"
    ;;
  public)
    TMP_DIR_NAME="import-cron-public-data"
    IMPORTER_NAME="public-importer"
    LOG_FILE_NAME="public-data-importer.log"
    ;;
  triage)
    TMP_DIR_NAME="import-cron-triage"
    IMPORTER_NAME="triage-cmo-importer"
    LOG_FILE_NAME="triage-cmo-importer.log"
    ;;
# TODO add msk
  *)
    echo "Unsupported importer: $IMPORTER" >&2
    exit 1
    ;;
esac

# Determine JAR filename; use destination color when not a MySQL-only import
if ! is_mysql_import; then
    GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
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
    IMPORTER_JAR_FILENAME="/data/portal-cron/lib/${IMPORTER_NAME}-${destination_database_color}.jar"
else
    IMPORTER_JAR_FILENAME="/data/portal-cron/lib/${IMPORTER_NAME}.jar"
fi

tmp="$PORTAL_HOME/tmp/$TMP_DIR_NAME"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

# Direct importer logs to stdout
tail -f "$PORTAL_HOME/logs/$LOG_FILE_NAME" &

if ! is_mysql_import; then
    echo "Destination DB color: $destination_database_color"
fi
echo "Using importer JAR: $IMPORTER_JAR_FILENAME"

# Database check
echo "Checking if mysql database version is compatible"
"$JAVA_BINARY" $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]; then
    echo "Error: Database version expected by portal does not match version in database!" >&2
    exit 1
fi

# Refresh CDD/Oncotree cache to pull latest metadata
echo "Refreshing CDD/ONCOTREE caches"
bash "$PORTAL_SCRIPTS_DIRECTORY/refresh-cdd-oncotree-cache.sh"
if [ $? -gt 0 ]; then
    echo "Error: Failed to refresh CDD and/or ONCOTREE cache during $IMPORTER setup!" >&2
    exit 1
fi
