#!/bin/bash

# Script for running arbitrary import
# Consists of the following:
# - Import of cancer types
# - Import from relevant column in spreadsheet

PORTAL_DATABASE="$1"
PORTAL_SCRIPTS_DIRECTORY="$2"
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="$3"
if [ -z "$PORTAL_SCRIPTS_DIRECTORY" ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
AUTOMATION_ENV_SCRIPT_FILEPATH="${PORTAL_SCRIPTS_DIRECTORY}/automation-environment.sh"
if [ ! -f "$AUTOMATION_ENV_SCRIPT_FILEPATH" ] ; then
    echo "$(date): Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source "$AUTOMATION_ENV_SCRIPT_FILEPATH"

function is_mysql_import() {
    [[ "$PORTAL_DATABASE" = "triage" ]]
}

# Set needed paths/filenames for import
case "$PORTAL_DATABASE" in
  genie)
    TMP_DIR_NAME="import-cron-genie"
    IMPORTER_NAME="genie-aws"
    LOG_FILE_NAME="genie-aws-importer.log"
    PORTAL_NAME="genie-portal"
    ONCOTREE_VERSION="oncotree_2019_12_01"
    ;;
  public)
    TMP_DIR_NAME="import-cron-public-data"
    IMPORTER_NAME="public"
    LOG_FILE_NAME="public-data-importer.log"
    PORTAL_NAME="public-portal"
    ONCOTREE_VERSION="oncotree_latest_stable"
    ;;
  triage)
    TMP_DIR_NAME="import-cron-triage"
    IMPORTER_NAME="triage-cmo"
    LOG_FILE_NAME="triage-cmo-importer.log"
    PORTAL_NAME="triage-portal"
    ONCOTREE_VERSION="oncotree_candidate_release"
    ;;
#   msk)
#     TMP_DIR_NAME="import-cron-msk"
#     IMPORTER_NAME="msk-cmo"
#     LOG_FILE_NAME="msk-cmo-importer.log"
#     PORTAL_NAME="msk-automation-portal"
#     ;;
  *)
    echo "Unsupported portal database: $PORTAL_DATABASE" >&2
    exit 1
    ;;
esac

if ! is_mysql_import; then
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
else
    IMPORTER_JAR_FILENAME="/data/portal-cron/lib/${IMPORTER_NAME}-importer.jar"
fi

tmp="${PORTAL_HOME}/tmp/${TMP_DIR_NAME}"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

# Direct importer logs to stdout
# Make sure to kill the tail process on exit so we don't hang the script
tail -f "$PORTAL_HOME/logs/$LOG_FILE_NAME" &
TAIL_PID=$!
trap 'kill "$TAIL_PID" 2>/dev/null; wait "$TAIL_PID" 2>/dev/null' EXIT INT TERM

if ! is_mysql_import; then
    echo "Destination DB color: $destination_database_color"
fi
echo "Importing with $IMPORTER_JAR_FILENAME"
echo "Importing cancer type updates into $destination_database_color mysql database"
$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version $ONCOTREE_VERSION
if [ $? -gt 0 ]; then
    echo "Error: Cancer type import failed!" >&2
    exit 1
fi

echo "Importing $PORTAL_DATABASE study data into $destination_database_color mysql database"
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal $PORTAL_NAME --update-worksheet --oncotree-version $ONCOTREE_VERSION --transcript-overrides-source uniprot --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "Error: $PORTAL_DATABASE import failed!" >&2
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
