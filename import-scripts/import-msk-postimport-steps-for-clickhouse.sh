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
DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/download_clickhouse_sql_scripts_py3.py"
TRANSFER_COLOR_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/transfer-deployment-color.sh"
COLOR_SWAP_CONFIG_FILEPATH="/data/portal-cron/pipelines-credentials/msk-db-color-swap-config.yaml"
CREATE_DERIVED_TABLES_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/create_derived_tables_in_clickhouse_database.sh"
CREATE_DERIVED_TABLES_SQL_FILE_DIRPATH="$PORTAL_HOME/tmp/import-cron-dmp-wrapper/create_clickhouse_derived_tables_download"
CLICKHOUSE_SCHEMA_BRANCH_NAME="msk-portal-db-clickhouse-sql-for-import"

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

function download_derived_table_construction_script() {
    # Check if derived table sql script dirpath exists
    # If not, try to create it
    derived_table_sql_script_dirpath="$CREATE_DERIVED_TABLES_SQL_FILE_DIRPATH"
    if ! [ -e "$derived_table_sql_script_dirpath" ] ; then
        if ! mkdir -p "$derived_table_sql_script_dirpath" ; then
            echo "Error: could not create target directory '$derived_table_sql_script_dirpath'" >&2
            return 1
        fi
    fi
    # Remove any scripts currently in the derived table sql script dirpath
    if [[ -d "$derived_table_sql_script_dirpath" && "$derived_table_sql_script_dirpath" != "/" ]]; then
        rm -rf "$derived_table_sql_script_dirpath"/*
    fi
    # Attempt to download the derived table SQL files from github
    if ! $DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH --github_branch_name "$CLICKHOUSE_SCHEMA_BRANCH_NAME" "$derived_table_sql_script_dirpath" ; then
        echo "Error during download of derived table construction .sql files from github" >&2
        return 1
    fi
    echo "downloaded clickhouse derived table construction file(s) from branch "$CLICKHOUSE_SCHEMA_BRANCH_NAME" of cbioportal github repository"
    return 0
}

function create_derived_tables_in_target_clickhouse_database() {
    destination_database_color="$1"
    if ! download_derived_table_construction_script ; then
        return 1
    fi
    echo "creating derived tables in clickhouse database with color $destination_database_color"
    $CREATE_DERIVED_TABLES_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color "$CREATE_DERIVED_TABLES_SQL_FILE_DIRPATH"/*
}

function transfer_color_to_new_database() {
    destination_database_color="$1"
    echo "transferring to databases with color $destination_database_color"
    $TRANSFER_COLOR_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $COLOR_SWAP_CONFIG_FILEPATH $destination_database_color
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
