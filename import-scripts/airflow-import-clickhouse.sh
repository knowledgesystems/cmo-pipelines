#!/bin/bash

# Script for updating ClickHouse DB
# Consists of the following:
# - Drop ClickHouse tables
# - Copy MySQL tables to ClickHouse
# - Create derived ClickHouse tables

PORTAL_DATABASE=$1
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

tmp=$PORTAL_HOME/tmp/import-cron-$PORTAL_DATABASE
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh"
DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_clickhouse_database.sh"
COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/copy_mysql_database_tables_to_clickhouse.sh"
DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/download_clickhouse_sql_scripts_py3.py"
CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/create_derived_tables_in_clickhouse_database.sh"

# Get the current production database color
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

echo "Destination DB color: $destination_database_color"

# Drop tables from non-production ClickHouse DB to make room for incoming copy
echo "dropping tables from clickhouse database $destination_database_color to make room for incoming copy"
if ! $DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during dropping of tables from clickhouse database $destination_database_color" >&2
    exit 1
fi

# Use Sling to copy data from non-production MySQL DB to non-production ClickHouse DB
echo "copying tables from mysql database $destination_database_color to clickhouse database $destination_database_color"
if ! $COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during copying of tables from mysql database $destination_database_color to clickhouse database $destination_database_color" >&2
    exit 1
fi

# Check if derived table sql script dirpath exists
# If not, try to create it
derived_table_sql_script_dirpath="$tmp/create_derived_clickhouse_tables"
if ! [ -e "$derived_table_sql_script_dirpath" ] ; then
    if ! mkdir -p "$derived_table_sql_script_dirpath" ; then
        echo "Error: could not create target directory '$derived_table_sql_script_dirpath'" >&2
        exit 1
    fi
fi

# Remove any scripts currently in the derived table sql script dirpath
if [[ -d "$derived_table_sql_script_dirpath" && "$derived_table_sql_script_dirpath" != "/" ]]; then
    rm -rf "$derived_table_sql_script_dirpath"/*
fi

# Attempt to download the derived table SQL files from github
clickhouse_schema_branch_name="master" # default
if [ "$PORTAL_DATABASE" == "public" ] ; then
    clickhouse_schema_branch_name="public-portal-db-clickhouse-sql-for-import"
fi
if ! $DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH --github_branch_name "$clickhouse_schema_branch_name" "$derived_table_sql_script_dirpath" ; then
    echo "Error during download of derived table construction .sql files from github" >&2
    exit 1
fi

# Create the additional derived tables inside of non-production Clickhouse DB
echo "creating derived tables in clickhouse database $destination_database_color"
if ! $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color "$derived_table_sql_script_dirpath"/* ; then
    echo "Error during derivation of clickhouse tables in clickhouse database $destination_database_color" >&2
    exit 1
fi
