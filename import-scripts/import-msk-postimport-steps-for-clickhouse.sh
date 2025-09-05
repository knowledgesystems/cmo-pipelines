#!/usr/bin/env bash

PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts" # TODO : change this to get through main()
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_database_update_tools.properties" # TODO : change this to get through main()

AUTOMATION_ENV_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
if [ ! -f $AUTOMATION_ENV_SCRIPT_FILEPATH ] ; then
    echo "`date`: Unable to locate $AUTOMATION_ENV_SCRIPT_FILEPATH, exiting..."
    exit 1
fi
source $AUTOMATION_ENV_SCRIPT_FILEPATH

SET_UPDATE_PROCESS_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/set_update_process_state.sh"
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_clickhouse_database.sh"
COPY_MYSQL_DATABASE_TO_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/copy_mysql_database_tables_to_clickhouse.sh"
TRANSFER_COLOR_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/import-msk-transfer-deployment-color.sh"
CREATE_DERIVED_TABLES_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/create_derived_tables_in_clickhouse_database.sh"
#TODO : need to create logic to download a dev branch of the following file from the upstream repo
CREATE_DERIVED_TABLES_SQL_FILE_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/cdt/clickhouse.sql"

function output_source_database_color() {
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
        exit 1
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

function drop_tables_in_destination_clickhouse_database() {
    destination_database_color="$1"
    echo "dropping tables from clickhouse database $destination_database_color"
    if ! $DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
        message="Error during dropping of tables from clickhouse database $destination_database_color"
        echo $message >&2
    fi
}

function copy_target_mysql_database_to_target_clickhouse_database() {
    destination_database_color="$1"
    echo "copying tables from mysql database $destination_database_color to clickhouse database $destination_database_color"
    if ! $COPY_MYSQL_DATABASE_TO_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
        message="Error during copying the mysql database (from $source_database_color to $destination_database_color)"
        echo $message >&2
        return 1
    fi
    return 0
}

function create_derived_tables_in_target_clickhouse_database() {
    destination_database_color="$1"
    echo "creating derived tables in clickhouse database with color $destination_database_color"
    $CREATE_DERIVED_TABLES_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color $CREATE_DERIVED_TABLES_SQL_FILE_FILEPATH
}

function transfer_color_to_new_database() {
    destination_database_color="$1"
    echo "transferring to databases with color $destination_database_color"
    $TRANSFER_COLOR_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color
}

function main() {
    echo "Performing Post-Import clickhouse processing steps"
    source_database_color=$(output_source_database_color)
    destination_database_color=$(output_destination_database_color $source_database_color)
    echo "Source DB color: $source_database_color"
    echo "Destination DB color: $destination_database_color"
    drop_tables_in_destination_clickhouse_database $destination_database_color
    if copy_target_mysql_database_to_target_clickhouse_database $destination_database_color ; then
        if create_derived_tables_in_target_clickhouse_database $destination_database_color ; then
            if transfer_color_to_new_database $destination_database_color ; then
                return 0
            else
                echo "Error : failed to transfer color to $destination_database_color --- manual adjustment needed"
            fi
        fi
    fi
}

main $@
