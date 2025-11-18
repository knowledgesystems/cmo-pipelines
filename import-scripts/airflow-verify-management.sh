#!/usr/bin/env bash

# Task for verifying that the update process management database is in sync with the actual cluster

PORTAL_SCRIPTS_DIRECTORY=$1
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$2
COLOR_SWAP_CONFIG_FILEPATH=$3

if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f $AUTOMATION_ENV_SCRIPT_FILEPATH ] ; then
    echo "`date`: Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source $AUTOMATION_ENV_SCRIPT_FILEPATH

# Verify the state in the update process management databases and fail if it is incorrect
VERIFY_MANAGEMENT_STATE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/verify-management-state.sh"
if ! $VERIFY_MANAGEMENT_STATE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH "$COLOR_SWAP_CONFIG_FILEPATH" ; then
    echo "Error: update process management database state is incorrect. This must be remedied before imports can proceed." >&2
    exit 1
fi
exit 0
