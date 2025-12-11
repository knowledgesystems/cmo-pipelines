#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : merge-cdm-timeline-files.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation-env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

COHORT="$1"
OUTPUT_DIR="$2"
MERGE_DIRS="$3"

function check_args() {
    if [[ -z $COHORT ]] ; then
        usage
        exit 1
    fi

    if [ ! -d $OUTPUT_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function usage {
    echo "merge-cdm-timeline-files.sh \$COHORT_ID \$OUTPUT_DIR \$MERGE_DIRS"
    echo -e "\t\$COHORT_ID                      name of merged cohort"
    echo -e "\t\$OUTPUT_DIR                     merged timeline file output will be written to this directory"
    echo -e "\t\$MERGE_DIRS                     directories containing timeline files to be merged"
}

function merge_timeline_files() {
    # Split the contents of the MERGE_DIRS string into a list
    IFS=' ' read -r -a MERGE_DIRS_LIST <<< "$MERGE_DIRS"

    # This gets the base filename for each of the timeline files
    FILE_LIST=($(cd ${MERGE_DIRS_LIST[0]} && ls data_timeline_*.txt))
    
    # Merge each type of timeline file in each data directory
    for TIMELINE_FILE in ${FILE_LIST[@]}; do
        FILES_TO_MERGE=""
        for MERGE_DIR in ${MERGE_DIRS_LIST[@]}; do
            # Check that MERGE_DIR exists
            if [ ! -d $MERGE_DIR ] ; then
                echo "`date`: Unable to locate required data directories, exiting..."
                exit 1
            fi

            # Check if the file exists before adding to command
            FILE_TO_MERGE="$MERGE_DIR/$TIMELINE_FILE"
            if [ -f $FILE_TO_MERGE ]; then
                FILES_TO_MERGE="$FILES_TO_MERGE $FILE_TO_MERGE"
            fi
        done
        $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i $FILES_TO_MERGE -o $OUTPUT_DIR/$TIMELINE_FILE -m outer
        if [ $? -gt 0 ] ; then
            echo "Error: CDM timeline file merge for $COHORT"
            exit 1
        fi
    done
}

date
check_args
merge_timeline_files

echo "`date`: CDM timeline file merge for $COHORT complete"