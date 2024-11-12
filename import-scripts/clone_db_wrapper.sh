#!/bin/bash

# Uses clone_mysql_database.sh script to clone from one db to another
# Figures out which database is "production" versus "not production" and passes it to the clone_mysql_database script
# Also determines which one is "blue"/"green" for the deployment

for i in "$@"; do
case $i in
    -p=*|--portal-scripts-directory=*)
    PORTAL_SCRIPTS_DIRECTORY="${i#*=}"
    shift # past argument=value
    ;;
    *)
      # default option
      echo "This option does not exist!  " "${i#*=}"
    ;;
esac
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="/data/portal-cron/scripts"
fi

if [ ! -f $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh ] ; then
  echo "`date`: Unable to locate automation_env, exiting..."
  exit 1
fi

source $PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh

# Create tmp directory for processing
tmp=$PORTAL_HOME/tmp/import-cron-genie
if ! [ -d "$tmp" ] ; then
    if ! mkdir -p "$tmp" ; then
        echo "Error : could not create tmp directory '$tmp'" >&2
        exit 1
    fi
fi
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi

# This file will be used throughout entire import job to determine directionality
SET_UPDATE_PROCESS_OUTPUT_FILEPATH="$tmp/update_process_output.txt"
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$PORTAL_SCRIPTS_DIRECTORY/airflowdb.properties
DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH=$PORTAL_SCRIPTS_DIRECTORY/drop_tables_in_mysql_database.sh
CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH=$PORTAL_SCRIPTS_DIRECTORY/clone_mysql_database.sh

if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH running > "$SET_UPDATE_PROCESS_OUTPUT_FILEPATH" ; then
    echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not set running state" >&2
    exit 1
    #TODO : recover from this error
fi

current_production_database_color=$(sh $PORTAL_SCRIPTS_DIRECTORY/get_database_currently_in_production.sh $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
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

MYSQL_DATABASE_CLONE_FAIL=0
if ! $DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
    message="Error during dropping of tables from mysql database $destination_database_color"
    echo $message >&2 # TODO in other parts of this script we are writing these error messages to stdout
    exit 1
else
    if ! $CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $source_database_color $destination_database_color ; then
        message="Error during cloning the mysql database (from $source_database_color to $destination_database_color)"
        echo $message >&2 # TODO in other parts of this script we are writing these error messages to stdout
        exit 1
    fi
fi
