#!/bin/bash

# Script for running pre-import steps
# Consists of the following:
# - Determine which database is "production" vs "not production"
# - Drop tables in the non-production database
# - Clone the production database into the non-production database

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

# Create tmp directory for processing
tmp=$PORTAL_HOME/tmp/import-cron-public-data
if ! [ -d "$tmp" ] ; then
    if ! mkdir -p "$tmp" ; then
        echo "Error: could not create tmp directory '$tmp'" >&2
        exit 1
    fi
fi
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi

SET_UPDATE_PROCESS_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/set_update_process_state.sh"
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_mysql_database.sh"
CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/clone_mysql_database.sh"

# Update the process status database
if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH running ; then
    echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not set running state" >&2
    exit 1
fi

# Get the current production database color
current_production_database_color=$(sh $GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
source_database_color="unset"
destination_database_color="unset"
if [ ${current_production_database_color:0:4} == "blue" ] ; then
    source_database_color="blue"
    destination_database_color="green"
fi
if [ ${current_production_database_color:0:5} == "green" ] ; then
    source_database_color="green"
    destination_database_color="blue"
fi
if [ "$destination_database_color" == "unset" ] ; then
    echo "Error during determination of the destination database color" >&2
    exit 1
fi

echo "Source DB color: $source_database_color"
echo "Destination DB color: $destination_database_color"

# Drop tables in the non-production database to make space for cloning
echo "dropping tables from mysql database $destination_database_color"
if ! $DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    message="Error during dropping of tables from mysql database $destination_database_color"
    echo $message >&2
    exit 1
else
    # Clone the content of the production MySQL database into the non-production database
    echo "copying tables from mysql database $source_database_color to $destination_database_color"
    if ! $CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $source_database_color $destination_database_color ; then
        message="Error during cloning the mysql database (from $source_database_color to $destination_database_color)"
        echo $message >&2
        exit 1
    fi
fi
