#!/bin/bash
#
#Script for copying tables from mysql into clickhouse

. /data/portal-cron/scripts/automation-environment.sh

# TODO: Update to include arguments for specifying script directory

# Set paths for necessary scripts
#########################################################
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="$PORTAL_HOME/scripts/airflowdb.properties"
DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/drop_tables_in_clickhouse_database.sh"
COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/copy_mysql_database_tables_to_clickhouse.sh"
CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/create_derived_tables_in_clickhouse_database.sh"
#########################################################

# Decide whether to interact with blue or green database
#########################################################
current_production_database_color=$(sh $PORTAL_HOME/scripts/get_database_currently_in_production.sh $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
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
#########################################################


# Clickhouse Operations
#########################################################
echo "copying tables from mysql database $destination_database_color to clickhouse database $destination_database_color..."

if ! $DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during dropping of tables from clickhouse database $destination_database_color"  >&2
    exit 1
fi
         
if ! $COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error during copying of tables from mysql database $destination_database_color to clickhouse database $destination_database_color"  >&2
    exit 1
fi
            
if ! $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    echo "Error creating derived tables in $destination_database_color clickhouse database"  >&2
    exit 1
fi
#########################################################
