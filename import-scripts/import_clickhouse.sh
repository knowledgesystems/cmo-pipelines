#!/bin/bash

# Script for updating ClickHouse DB
# Consists of the following:
# - Drop ClickHouse tables
# - Copy MySQL tables to ClickHouse
# - Create derived ClickHouse tables

PORTAL_SCRIPTS_DIRECTORY=$1
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi
if [ ! -f $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi
source $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh

MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$PORTAL_SCRIPTS_DIRECTORY/airflowdb.properties
DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_clickhouse_database.sh"
COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/copy_mysql_database_tables_to_clickhouse.sh"
CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_SCRIPTS_DIRECTORY/create_derived_tables_in_clickhouse_database.sh"
CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SQL_1_FILEPATH="$PORTAL_HOME/create_derived_clickhouse_tables/genie/clickhouse.sql"
CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SQL_2_FILEPATH="$PORTAL_HOME/create_derived_clickhouse_tables/genie/materialized_views.sql"

# Get the current production database color
current_production_database_color=$(sh $PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
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

# Drop tables from non-production ClickHouse DB to make room for incoming copy
echo "dropping tables from clickhouse database $destination_database_color..."
if ! $DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during dropping of tables from clickhouse database $destination_database_color" >&2
    exit 1
fi

# Use Sling to copy data from non-production MySQL DB to non-production ClickHouse DB
echo "copying tables from mysql database $destination_database_color to clickhouse database $destination_database_color..."
if ! $COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during copying of tables from mysql database $destination_database_color to clickhouse database $destination_database_color" >&2
    exit 1
fi

# Create the additional derived tables inside of non-production Clickhouse DB
echo "creating derived tables in clickhouse database $destination_database_color..."
if ! $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SQL_1_FILEPATH $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SQL_2_FILEPATH ; then
    echo "Error during creation of derived tables in clickhouse database $destination_database_color" >&2
    exit 1
fi
