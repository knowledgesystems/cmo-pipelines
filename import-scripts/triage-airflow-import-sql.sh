#!/bin/bash

# Script for running PUBLIC import
# Consists of the following:
# - Import of cancer types
# - Import from triage-portal column in spreadsheet

IMPORTER=$1 # takes "triage"
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

tmp=$PORTAL_HOME/tmp/import-cron-triage-data
IMPORTER_JAR_FILENAME="/data/portal-cron/lib/triage-cmo-importer.jar"
JAVA_IMPORTER_ARGS="$JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
ONCOTREE_VERSION="oncotree_latest_stable"

# Direct importer logs to stdout
tail -f $PORTAL_HOME/logs/triage-cmo-data-importer.log &

echo "Importing with $IMPORTER_JAR_FILENAME"
echo "Importing cancer type updates into mysql database"
$JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version $ONCOTREE_VERSION
if [ $? -gt 0 ]; then
    echo "Error: Cancer type import failed!" >&2
    exit 1
fi

echo "Importing triage study data into mysql database"
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal triage-portal --update-worksheet --oncotree-version $ONCOTREE_VERSION --transcript-overrides-source uniprot --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "Error: triage-data import failed!" >&2
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
