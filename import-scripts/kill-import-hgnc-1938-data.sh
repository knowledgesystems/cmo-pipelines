#!/usr/bin/env bash

script_path="$0"

FLOCK_FILEPATH="/data/portal-cron/cron-lock/kill-import-hgnc-1938-data.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        exit 0
    fi

    # set necessary env variables with automation-environment.sh

    if [ -z "$PORTAL_HOME" ] || [ -z "$START_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] || [ -z "$KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] || [ -z "$HGNC_1938_IMPORT_IN_PROGRESS_FILENAME" ] || [ -z "$HGNC_1938_IMPORT_KILLING_FILENAME" ] ; then
        echo "Error : kill-import-hgnc-1938-data.sh cannot be run without setting the PORTAL_HOME, START_HGNC_1938_IMPORT_TRIGGER_FILENAME, KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME, HGNC_1938_IMPORT_IN_PROGRESS_FILENAME, HGNC_1938_IMPORT_KILLING_FILENAME environment variables. (Use automation-environment.sh)"
        exit 1
    fi

    if ! [ -f "$KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME" ] ; then
        # exit if kill has not been triggered
        exit 0
    fi

    echo $(date)

    if [ -f "$HGNC_1938_IMPORT_KILLING_FILENAME" ] ; then
        echo "Warning : killing of import-hgnc-1938-data was in progress when another attempt to kill it began."
    fi
    # remove kill trigger and set status
    rm -f $KILL_HGNC_1938_IMPORT_TRIGGER_FILENAME
    touch $HGNC_1938_IMPORT_KILLING_FILENAME
    # cancel any start trigger
    rm -f $START_HGNC_1938_IMPORT_TRIGGER_FILENAME

    tmp="$PORTAL_HOME/tmp/kill-hgnc-1938-import"
    if ! [ -d "$tmp" ] ; then
        if ! mkdir -p "$tmp" ; then
            echo "Error : could not create tmp directory '$tmp'" >&2
            exit 1
        fi
    fi
    if [[ -d "$tmp" && "$tmp" != "/" ]]; then
        rm -rf "$tmp"/*
    fi

    # find importer parent process id
    script_owner_uid=$(stat --format="%u" $script_path)
    process_list_filename=$(mktemp --tmpdir=$tmp process_list.tmpXXXXXXXX )
    ps -o pid,ppid,command -u $script_owner_uid > "$process_list_filename"
    parent_process_line=$(grep import-hgnc-1938-data.sh "$process_list_filename" | grep -v kill-import-hgnc-1938-data.sh | grep import-hgnc-1938-data.log | head -n 1)
    if [ -z "$parent_process_line" ] ; then
        # nothing to kill ... importer is not running
        rm -f "$HGNC_1938_IMPORT_KILLING_FILENAME"
    exit 0
    fi
    # parent process line has fields : PID, PARENT_PID, COMMAND
    read pid ppid command <<< "$parent_process_line"
    if [ -z "$pid" ] ; then
        rm -f "$HGNC_1938_IMPORT_KILLING_FILENAME"
        echo "Error : could not obtain parent process id from process list" >&2
        exit 1
    fi
    if [ -z "$ppid" ] ; then
        rm -f "$HGNC_1938_IMPORT_KILLING_FILENAME"
        echo "Error : could not obtain grandparent process id from process list" >&2
        exit 1
    fi
    origin_process_id="$pid"
    # kill origin process and all children
    unset kill_list
    declare -A kill_list
    kill_list[$origin_process_id]=1
    a_new_process_was_added=1
    while [ "$a_new_process_was_added" == "1" ] ; do
        a_new_process_was_added=0
        first_line_read=0
        while read line ; do
            if [ "$first_line_read" == "0" ] ; then
                first_line_read=1
                continue
            fi
            read pid ppid command <<< "$line"
            # check if pid is a child of any process on the kill list
            if [ "${kill_list[$ppid]}" == "1" ] ; then
                # this process's parent is on the kill list
                if ! [ "${kill_list[$pid]}" == "1" ] ; then
                    # this process is not on the list
                    kill_list[$pid]=1
                    a_new_process_was_added=1
                fi
            fi
        done < $process_list_filename
    done
    kill ${!kill_list[@]}
    # after kill, import is no longer in progress
    rm -f "$HGNC_1938_IMPORT_IN_PROGRESS_FILENAME"
    # kill is done - remove status file showing "killing"
    rm -f "$HGNC_1938_IMPORT_KILLING_FILENAME"
) {flock_fd}>$FLOCK_FILEPATH
