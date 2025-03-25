#!/bin/bash

# Script for transferring the Genie deployment to the newly updated database

PORTAL_SCRIPTS_DIRECTORY=$1
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$2
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f $AUTOMATION_ENV_SCRIPT_FILEPATH ] ; then
    echo "`date`: Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source $AUTOMATION_ENV_SCRIPT_FILEPATH

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

# Switch over to the newly updated database, mark the process as complete if the switchover succeeded
TRANSFER_DEPLOYMENT_COLOR_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/import-genie-data-transfer-deployment-color.sh"
if ! $TRANSFER_DEPLOYMENT_COLOR_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    COLOR_SWITCH_FAIL=1
    echo "Error during deployment transfer to $destination_database_color!" >&2
    exit 1
fi
