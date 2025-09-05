#!/usr/bin/env bash

PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts" # TODO : change this to get through main()
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_database_update_tools.properties" # TODO : change this to get through main()

AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f $AUTOMATION_ENV_SCRIPT_FILEPATH ] ; then
    echo "`date`: Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source $AUTOMATION_ENV_SCRIPT_FILEPATH

# Create tmp directory for processing
#### ROB : for now this is part of the import-dmp-impact-data script, so commented out here
####tmp=$PORTAL_HOME/tmp/import-cron-msk-data
####if ! [ -d "$tmp" ] ; then
####    if ! mkdir -p "$tmp" ; then
####        echo "Error: could not create tmp directory '$tmp'" >&2
####        exit 1
####    fi
####fi
####if [[ -d "$tmp" && "$tmp" != "/" ]]; then
####    rm -rf "$tmp"/*
####fi

SET_UPDATE_PROCESS_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/set_update_process_state.sh"
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_mysql_database.sh"
CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/clone_mysql_database.sh"

function output_fail_status_and_exit() {
    local output_status_filepath="$1"
    echo "no" > "$output_status_filepath"
    exit 1
}

function output_success_status_and_exit() {
    local output_status_filepath="$1"
    echo "yes" > "$output_status_filepath"
    exit 0
}

function update_state_to_show_update_is_running() {
    # Update the process status database
    if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH running ; then
        echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not set running state" >&2
        return 1
    fi
    return 0
}

function output_source_database_color() {
    local output_status_filepath="$1"
    # Get the current production database color
    current_production_database_color=$(sh $GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
    source_database_color="unset"
    if [ ${current_production_database_color:0:4} == "blue" ] ; then
        source_database_color="blue"
    fi
    if [ ${current_production_database_color:0:5} == "green" ] ; then
        source_database_color="green"
    fi
    if [ "$destination_database_color" == "unset" ] ; then
        echo "Error during determination of the destination database color" >&2
        output_fail_status_and_exit "$output_status_filepath"
    fi
    echo "$source_database_color"
}

function output_destination_database_color () {
    source_database_color="$1"
    if [ "$source_database_color" == "blue" ] ; then
        echo "green"
    fi
    if [ "$source_database_color" == "green" ] ; then
        echo "blue"
    fi
}

function drop_tables_in_destination_mysql_database() {
    destination_database_color="$1"
    echo "dropping tables from mysql database $destination_database_color"
    if ! $DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
        message="Error during dropping of tables from mysql database $destination_database_color"
        echo $message >&2
        return 1
    fi
    return 0
}

function clone_production_mysql_database_to_non_production_mysql_database() {
    source_database_color="$1"
    destination_database_color="$2"
    echo "copying tables from mysql database $source_database_color to $destination_database_color"
    if ! $CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $source_database_color $destination_database_color ; then
        message="Error during cloning the mysql database (from $source_database_color to $destination_database_color)"
        echo $message >&2
        return 1
    fi
    return 0
}

function main() {
    output_status_filepath="$1"
    echo "Performing Pre-Import clickhouse processing steps"
    if ! update_state_to_show_update_is_running ; then
        output_fail_status_and_exit "$output_status_filepath"
    fi
    source_database_color=$(output_source_database_color "$output_status_filepath")
    destination_database_color=$(output_destination_database_color $source_database_color)
    echo "Source DB color: $source_database_color"
    echo "Destination DB color: $destination_database_color"
    if ! drop_tables_in_destination_mysql_database $destination_database_color ; then
        output_fail_status_and_exit "$output_status_filepath"
    fi
    if ! clone_production_mysql_database_to_non_production_mysql_database $source_database_color $destination_database_color ; then
        output_fail_status_and_exit "$output_status_filepath"
    fi
    output_success_status_and_exit "$output_status_filepath"
}

main $@
