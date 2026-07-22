#!/usr/bin/env bash

# Task for verifying that no other import process is currently running.
# Checks the update_process_status in the management database and fails
# if it is "running", meaning another import is in progress.

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

# Load the ClickHouse property parsing and client functions
source "$PORTAL_SCRIPTS_DIRECTORY/parse_property_file_functions.sh"
source "$PORTAL_SCRIPTS_DIRECTORY/clickhouse_client_command_line_functions.sh"

if ! [ -r "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" ] ; then
    echo "`date`: Unable to read properties file $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH, exiting..."
    exit 1
fi

# Parse properties
unset my_properties
declare -A my_properties
if ! parse_property_file "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" my_properties ; then
    echo "Error: could not parse properties file $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" >&2
    exit 1
fi

update_management_database="${my_properties['clickhouse_update_management_database']}"
portal_database_name="${my_properties['portal_database_name']}"

if [ -z "$update_management_database" ] ; then
    echo "Error: clickhouse_update_management_database not found in properties file" >&2
    exit 1
fi
if [ -z "$portal_database_name" ] ; then
    echo "Error: portal_database_name not found in properties file" >&2
    exit 1
fi

# Write ClickHouse config file for the management database
configured_clickhouse_config_file_path="$(pwd)/clickhouse_client_config_$(date "+%Y-%m-%d-%H-%M-%S").yaml"
if ! rm -f "$configured_clickhouse_config_file_path" || ! touch "$configured_clickhouse_config_file_path" ; then
    echo "Error: unable to create clickhouse_client_config file $configured_clickhouse_config_file_path" >&2
    exit 1
fi
chmod 600 "$configured_clickhouse_config_file_path"
echo "user: ${my_properties['clickhouse_server_username']}" >> "$configured_clickhouse_config_file_path"
echo "password: ${my_properties['clickhouse_server_password']}" >> "$configured_clickhouse_config_file_path"
echo "host: ${my_properties['clickhouse_server_host_name']}" >> "$configured_clickhouse_config_file_path"
echo "port: ${my_properties['clickhouse_server_port']}" >> "$configured_clickhouse_config_file_path"
echo "database: $update_management_database" >> "$configured_clickhouse_config_file_path"

if [ "$(cat $configured_clickhouse_config_file_path | wc -l)" -ne 5 ] ; then
    echo "Error: could not successfully write clickhouse_client config properties to file $configured_clickhouse_config_file_path" >&2
    rm -f "$configured_clickhouse_config_file_path"
    exit 1
fi

# Query the update_process_status for this portal
status_filepath="$(pwd)/sups_verify_not_running_status.txt"
rm -f "$status_filepath"

get_status_statement="SELECT update_process_status FROM \`$update_management_database\`.update_status WHERE portal_database = '$portal_database_name' LIMIT 1"
if ! execute_sql_statement_via_clickhouse_client "$get_status_statement" "$status_filepath" ; then
    echo "Error: could not query update_process_status from management database. ClickHouse statement failed: $get_status_statement" >&2
    rm -f "$configured_clickhouse_config_file_path"
    rm -f "$status_filepath"
    exit 1
fi

set_clickhouse_sql_data_array_from_file "$status_filepath" 0
current_status="${sql_data_array[0]}"

rm -f "$configured_clickhouse_config_file_path"
rm -f "$status_filepath"

if [ "$current_status" == "running" ] ; then
    echo "Error: Another import process is currently running for portal database '$portal_database_name' (update_process_status = 'running'). Please wait for it to complete before triggering a new import." >&2
    exit 1
fi

echo "Import not in progress for portal database '$portal_database_name' (update_process_status = '$current_status'). Proceeding with import."
exit 0
