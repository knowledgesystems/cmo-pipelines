#!/bin/bash

#------------ FLOCK USED FOR MUTUAL EXCLUSION ... SO SCRIPT WILL NOT START IF IT IS ALREADY RUNNING
FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-genie-data.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        exit 0
    fi

#------------ VALIDATE ENVIRONMENT -- ALL NEEDED ENVIRONMENT VARIABLES ARE SET
    # set necessary env variables with automation-environment.sh
    if [[ -z "$PORTAL_HOME" || -z "$JAVA_BINARY" || -z "$START_GENIE_IMPORT_TRIGGER_FILENAME" || -z "$KILL_GENIE_IMPORT_TRIGGER_FILENAME" || -z "$GENIE_IMPORT_IN_PROGRESS_FILENAME" || -z "$GENIE_IMPORT_KILLING_FILENAME" ]] ; then
        echo "Error : import-genie-data.sh cannot be run without setting the PORTAL_HOME, JAVA_BINARY, START_GENIE_IMPORT_TRIGGER_FILENAME, KILL_GENIE_IMPORT_TRIGGER_FILENAME, GENIE_IMPORT_IN_PROGRESS_FILENAME, GENIE_IMPORT_KILLING_FILENAME environment variables. (Use automation-environment.sh)"
        exit 1
    fi

#------------ KILL IMPORT IN PROGRESS LOGIC AND TRIGGER IMPORT START FUNCTIONALITY LOGIC
    if [ -f "${KILL_GENIE_IMPORT_TRIGGER_FILENAME}" ] || [ -f "${GENIE_IMPORT_KILLING_FILENAME}" ] ; then
        # if import kill is happening or is triggered, cancel any import trigger and exit
        rm -f "${START_GENIE_IMPORT_TRIGGER_FILENAME}"
        exit 1
    fi

#------------ SAFETY MECHANISM TO AVOID TOO MUCH LOG EMAIL SPAMMING : INHIBIT FOR A WHILE AFTER DB SCHEMA MISMATCH
    # inhibit import attempt if db mismatch occurred recently
    inhibit_counter=0
    if [ -f "${GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME}" ] ; then
        unset inhibit_counter_line
        read inhibit_counter_line < ${GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME}
        COUNTER_RE='([0-9]+)'
        if [[ $inhibit_counter_line =~ $COUNTER_RE ]] ; then
            inhibit_counter=${BASH_REMATCH[0]}
        else
            # inhibit file is corrupted (no counter on first line)
            rm -f "${GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME}"
        fi
    fi
    if [ $inhibit_counter -ne 0 ] ; then
        # decrease inhibit counter and exit
        echo $(($inhibit_counter-1)) > "${GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME}"
        exit 1
    else
        # clean up inhibit file with zero counter
        rm -f "${GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME}"
    fi

#------------ EXIT IF NO IMPORT HAS BEEN TRIGGERED
    if ! [ -f "${START_GENIE_IMPORT_TRIGGER_FILENAME}" ] ; then
        # no start trigger for import, so exit
        exit 1
    fi

#------------ SET UP / PREPARE THE TMP DIRECTORY FOR THE IMPORT RUN
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

#------------ ENVIRONMENT VARIABLE SETUP FOR THE REST OF THE SCRIPT
    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START=10 # inhibition cycle count after sending db version mismatch error email
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    IMPORTER_JAR_BLUE_FILENAME="$PORTAL_HOME/lib/genie-aws-importer-blue.jar"
    IMPORTER_JAR_GREEN_FILENAME="$PORTAL_HOME/lib/genie-aws-importer-green.jar"
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="$PORTAL_HOME/pipelines-credentials/manage_genie_database_update_tools.properties"
    SET_UPDATE_PROCESS_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/set_update_process_state.sh"
    GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="$PORTAL_HOME/scripts/get_database_currently_in_production.sh"
    DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/drop_tables_in_mysql_database.sh"
    DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/drop_tables_in_clickhouse_database.sh"
    COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/copy_mysql_database_tables_to_clickhouse.sh"
    DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/download_clickhouse_sql_scripts_py3.py"
    CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/create_derived_tables_in_clickhouse_database.sh"
    CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/clone_mysql_database.sh"
    TRANSFER_DEPLOYMENT_COLOR_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/import-genie-data-transfer-deployment-color.sh"
    ENABLE_DEBUGGING=0
    java_debug_args=""
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27186"
    fi
    JAVA_IMPORTER_BLUE_ARGS="$java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_BLUE_FILENAME org.mskcc.cbio.importer.Admin"
    JAVA_IMPORTER_GREEN_ARGS="$java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_GREEN_FILENAME org.mskcc.cbio.importer.Admin"
    JAVA_IMPORTER_ARGS=""
    genie_portal_notification_file=$(mktemp $tmp/genie-portal-update-notification.$now.XXXXXX)

#------------ THE GENIE BASE RELEASE DETERMINES WHAT ONCOTREE VERSION TO USE - THIS IS PROBABLY OBSOLETE
    # ROB : I believe that this code should never be used. The oncotree (type_of_cancer table) is used for *all* studies in the database - it is not study specific.
    IMPORTING_GENIE_RELEASE_BEFORE_9_0=0
    if [ $IMPORTING_GENIE_RELEASE_BEFORE_9_0 != "0" ] ; then
        oncotree_version_to_use=oncotree_2018_06_01
    else
        oncotree_version_to_use=oncotree_2019_12_01
    fi
    COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE=0 # 0 = do not copy tables, non-0 = copy tables

#------------ ADDITIONAL START/KILL TRIGGER LOGIC
    # import is beginning - create status file showing "in_progress", and remove trigger
    rm -f "$START_GENIE_IMPORT_TRIGGER_FILENAME"
    touch "$GENIE_IMPORT_IN_PROGRESS_FILENAME"

    echo $now : starting import

#------------ THE PROCESSES STATUS DATABASE IS UPDATED FOR GENIE GREEN/BLUE
    SET_DATABASE_UPDATE_RUNNING_STATE_FAIL=0
    if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH running ; then
        echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not set running state" >&2
        SET_DATABASE_UPDATE_RUNNING_STATE_FAIL=1
    else
        CURRENT_DATABASE_OUTPUT_FILEPATH="$tmp/get_current_database_output.txt"
        rm -f $CURRENT_DATABASE_OUTPUT_FILEPATH
        if ! $GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH > $CURRENT_DATABASE_OUTPUT_FILEPATH; then
            echo "Error during determination of the destination database color" >&2
            SET_DATABASE_UPDATE_RUNNING_STATE_FAIL=1
        else
            current_production_database_color=$(tail -n 1 "$CURRENT_DATABASE_OUTPUT_FILEPATH")
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
                SET_DATABASE_UPDATE_RUNNING_STATE_FAIL=1
            fi
        fi
        rm -f $CURRENT_DATABASE_OUTPUT_FILEPATH
    fi
#------------ DROP TABLES IN THE NON_PRODUCTION DATABASE TO MAKE SPACE FOR CLONING
    if [ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 ] ; then
        echo "we are going to clone $source_database_color into $destination_database_color and then import into $destination_database_color"
        MYSQL_DATABASE_CLONE_FAIL=0
        if ! $DROP_TABLES_FROM_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
            message="Error during dropping of tables from mysql database $destination_database_color"
            echo $message
            MYSQL_DATABASE_CLONE_FAIL=1
            echo -e "$message" | mail -s "Failed to drop tables in $destination_database_color genie MySQL database" $PIPELINES_EMAIL_LIST
        else
#------------ CLONE THE CONTENT OF THE PRODUCTION MYSQL DATABASE INTO THE NON-PRODUCTION DATABASE
            if ! $CLONE_MYSQL_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $source_database_color $destination_database_color ; then
                message="Error during cloning the mysql database (from $source_database_color to $destination_database_color)"
                echo $message
                MYSQL_DATABASE_CLONE_FAIL=1
                echo -e "$message" | mail -s "Failed to clone genie MySQL tables (from $source_database_color to $destination_database_color)" $PIPELINES_EMAIL_LIST
            fi
        fi
    fi
#------------ REFRESH THE CDD AND ONCOTREE CACHES (ON THOSE SERVERS, FROM TOPBRAID/GRAPHITE)
    CDD_ONCOTREE_RECACHE_FAIL=0
    if [ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 ] && [ $MYSQL_DATABASE_CLONE_FAIL -eq 0 ] ; then
        if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
            # refresh cdd and oncotree cache
            bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
            if [ $? -gt 0 ]; then
                CDD_ONCOTREE_RECACHE_FAIL=1
                message="Failed to refresh CDD and/or ONCOTREE cache during GENIE import!"
                echo $message
                echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
            fi
        fi
    fi
#------------ CHECK THAT THE IMPORTER .JAR FILE IS COMPATIBLE WITH THE DATABASE (DB_SCHEMA_VERSION)
    DB_VERSION_FAIL=0
    if [ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 ] && [ $MYSQL_DATABASE_CLONE_FAIL -eq 0 ] && [ $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ] ; then
#------------ BEGIN SETTING UP FOR THE ACTUAL IMPORT RUN (SELECT THE IMPORTER JAR TO USE)
        if [ $destination_database_color == "blue" ] ; then
            JAVA_IMPORTER_ARGS=$JAVA_IMPORTER_BLUE_ARGS
        else
            JAVA_IMPORTER_ARGS=$JAVA_IMPORTER_GREEN_ARGS
        fi
        # check database version before importing anything
        echo "Checking if database version is compatible"
        $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
        if [ $? -gt 0 ]; then
            echo "Database version expected by portal does not match version in database!"
            DB_VERSION_FAIL=1
        fi
    fi
#------------ FETCH UPDATES TO THE GENIE GITHUB REPO CLONE
    # fetch updates in genie repository
    echo "fetching updates from genie..."
    GENIE_FETCH_FAIL=0
    if [ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 ] && [ $MYSQL_DATABASE_CLONE_FAIL -eq 0 ] && [ $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ] &&
            [ $DB_VERSION_FAIL -eq 0 ] ; then
        $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source genie --run-date latest
        if [ $? -gt 0 ]; then
            echo "genie fetch failed!"
            GENIE_FETCH_FAIL=1
            EMAIL_BODY="The genie data fetch failed."
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: genie" $PIPELINES_EMAIL_LIST
        fi
    fi
    SETUP_CLICKHOUSE_FAIL=0
    if [[ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 && $MYSQL_DATABASE_CLONE_FAIL -eq 0 && $DB_VERSION_FAIL -eq 0 && $GENIE_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
        # import genie studies into genie portal
#------------ IMPORT ONCOTREE TO TYPES_OF_CANCER TABLE
        echo "importing cancer type updates into genie portal database..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${oncotree_version_to_use}
#------------ IMPORT/UPDATE ANY STUDIES MARKED ON THE IMPORTER CONFIGURATION SPREADSHEET
        echo "importing study data into genie portal database..."
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal genie-portal --update-worksheet --notification-file "$genie_portal_notification_file" --oncotree-version ${oncotree_version_to_use} --transcript-overrides-source mskcc --disable-redcap-export
        IMPORT_EXIT_STATUS=$?
        if [ $IMPORT_EXIT_STATUS -ne 0 ]; then
            echo "Genie import failed!"
            EMAIL_BODY="Genie import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: Genie" $PIPELINES_EMAIL_LIST
        fi
#------------ DETECT WHETHER OR NOT *ANY* STUDIES WERE SUCCESSFULLY IMPORTED IN THE DATABASE. NOT SURE IF "R"s COUNTS
#       TODO: test what happens when only study removals have occurred. That is still an update, so copy and switchover should proceed
        num_studies_updated=''
        num_studies_updated_filename="$tmp/num_studies_updated.txt"
        if [ -r "$num_studies_updated_filename" ] ; then
            num_studies_updated=$(cat "$num_studies_updated_filename")
        fi
#------------ WE WILL SKIP FURTHER PROCESSING IF NO DATABASE ALTERATIONS OCCURRED. DETECT THIS.
        if [ -z $num_studies_updated ] ; then
            echo "could not determine the number of studies that have been updated"
            COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE=1
        else
            echo "'$num_studies_updated' studies have been updated"
            if [[ $num_studies_updated != "0" ]]; then
                COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE=1
            fi
        fi
        # update clickhouse
        if [ $COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE -ne 0 ] ; then
            echo "copying tables from mysql database $destination_database_color to clickhouse database $destination_database_color..."
#------------ DROP TABLES FROM NON-PRODUCTION CLIKCHOUSE DATABASE TO MAKE ROOM FOR INCOMING COPY
            if ! $DROP_TABLES_FROM_CLICKHOUSE_DATABASE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
                echo "Error during dropping of tables from clickhouse database $destination_database_color" >&2
                SETUP_CLICKHOUSE_FAIL=1
            fi
#------------ USE SLING TO COPY DATA FROM NON-PRODUCTION MYSQL DATABASE TO NON-PRODUCTION CLICKHOUSE DB
            if [ $SETUP_CLICKHOUSE_FAIL -eq 0 ] ; then
                if ! $COPY_TABLES_FROM_MYSQL_TO_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
                    echo "Error during copying of tables from mysql database $destination_database_color to clickhouse database $destination_database_color" >&2
                    SETUP_CLICKHOUSE_FAIL=1
                fi
            fi
#------------ CREATE THE ADDITIONAL DERIVED TABLES INSIDE OF NON-PRODUCTION CLICKHOUSE DB
            if [ $SETUP_CLICKHOUSE_FAIL -eq 0 ] ; then
                derived_table_sql_script_dirpath="$tmp/create_derived_clickhouse_tables"
                if ! [ -e "$derived_table_sql_script_dirpath" ] ; then
                    if ! mkdir -p "$derived_table_sql_script_dirpath" ; then
                        echo "Error : could not create target directory '$derived_table_sql_script_dirpath'" >&2
                        SETUP_CLICKHOUSE_FAIL=1
                    fi
                fi
            fi
            if [[ -d "$derived_table_sql_script_dirpath" && "$derived_table_sql_script_dirpath" != "/" ]]; then
                rm -rf "$derived_table_sql_script_dirpath"/*
            fi
            if [ $SETUP_CLICKHOUSE_FAIL -eq 0 ] ; then
                if ! $DOWNLOAD_DERVIED_TABLE_SQL_FILES_SCRIPT_FILEPATH "$derived_table_sql_script_dirpath" ; then
                    echo "Error : could not download needed derived table construction .sql files from github" >&2
                    SETUP_CLICKHOUSE_FAIL=1
                fi
            fi
            if [ $SETUP_CLICKHOUSE_FAIL -eq 0 ] ; then
                if ! $CREATE_DERIVED_TABLES_IN_CLICKHOUSE_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color "$derived_table_sql_script_dirpath"/* ; then
                    echo "Error during derivation of clickhouse tables in clickhouse database $destination_database_color" >&2
                    SETUP_CLICKHOUSE_FAIL=1
                fi
            fi
        fi
    fi

#------------ IF ALL HAS GONE WELL SO FAR, AUTOMATICALLY SWITCH OVER TO THE NEWLY UPDATED DATABASE, AND MARK THE UPDATE PROCESS AS COMPLETE IF SWITCHOVER SUCCEEDED
    COLOR_SWITCH_FAIL=0
    if [[ $SET_DATABASE_UPDATE_RUNNING_STATE_FAIL -eq 0 && $MYSQL_DATABASE_CLONE_FAIL -eq 0 && $DB_VERSION_FAIL -eq 0 && $GENIE_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 && $SETUP_CLICKHOUSE_FAIL -eq 0 ]]; then
        if ! $TRANSFER_DEPLOYMENT_COLOR_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $destination_database_color ; then
            COLOR_SWITCH_FAIL=1
            if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH abandoned ; then
                echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not abandon update" >&2
            fi
        fi
    else
        if ! $SET_UPDATE_PROCESS_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH abandoned ; then
            echo "Error during execution of $SET_UPDATE_PROCESS_SCRIPT_FILEPATH : could not abandon update" >&2
        fi
    fi

#------------ SEND APPROPRIATE NOTIFICATIONS NOW THAT PROCESS IS COMPLETING

    # TODO add message for failure during management database update

    # TDOD: add message for other failures (such as failure to automatically switchover to new color)

    # send email if we failed to setup clickhouse
    if [ $SETUP_CLICKHOUSE_FAIL -gt 0 ]; then
        # TODO more in message?
        EMAIL_BODY="The genie clickhouse $destination_database_color database failed to be setup."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GENIE Update Failure: Clickhouse update failed" $PIPELINES_EMAIL_LIST
    fi

    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        EMAIL_BODY="The genie database version is incompatible. Imports will be skipped until database is updated, and file $GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME has been created to inhibit imports for the next $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START minutes."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "GENIE Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
        echo -e "Inhibiting import attempts for $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START cycles/minutes"
        echo $VERSION_MISMATCH_EMAIL_INHIBIT_COUNTER_START > "$GENIE_IMPORT_VERSION_EMAIL_INHIBIT_FILENAME"
    fi

    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal genie-portal --notification-file "$genie_portal_notification_file"

#------------ PURGE UNNECESSARY LFS STORAGE FROM GENIE DATA REPO CLONE (COULD BE MOVED EARLIER)

    echo "Cleaning up any untracked files from GENIE import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/genie

#------------ ADDITIONAL LOGIC FOR IMPORT-TOOL --- USERS CAN GET STATUS OF IMPORT PROCESS AND NOW IT IS "DONE"
    # import is done - remove status file showing "in_progress"
    rm -f "$GENIE_IMPORT_IN_PROGRESS_FILENAME"

) {flock_fd}>$FLOCK_FILEPATH
