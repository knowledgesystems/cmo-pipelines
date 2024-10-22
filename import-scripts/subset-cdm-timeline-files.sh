#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : subset-cdm-timeline-files.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

COHORT=$1
OUTPUT_DIR=$2
SUBSET_DIR=$3

function check_args() {
    if [[ -z $COHORT ]] || [[ -z $OUTPUT_DIR ]] || [[ -z $SUBSET_DIR ]] ; then
        usage
        exit 1
    fi

    # Check that required directories exist
    if [ ! -d $OUTPUT_DIR ] || [ ! -d $OUTPUT_DIR ] || [ ! -d $SUBSET_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function usage {
    echo "subset-cdm-timeline-files.sh \$COHORT_ID \$OUTPUT_DIR \$SUBSET_DIR"
    echo -e "\t\$COHORT_ID                      name of affiliate cohort"
    echo -e "\t\$OUTPUT_DIR                     where to write subsetted timeline files"
    echo -e "\t\$SUBSET_DIR                     directory to subset off of"
}

function subset_timeline_files() {
    # get patient list file
    TMP_PATIENT_FILE=$(mktemp -q)
    cut -f 1 $OUTPUT_DIR/data_clinical_patient.txt > $TMP_PATIENT_FILE

    # Set data directory paths
    # This gets the base filename for each of the timeline files
    FILE_LIST=($(cd ${SUBSET_DIR[0]} && ls data_timeline_*.txt))

    for TIMELINE_FILE in ${FILE_LIST[@]}; do
        INPUT_TIMELINE_FILEPATH="$SUBSET_DIR/$TIMELINE_FILE"
        OUTPUT_TIMELINE_FILEPATH="$OUTPUT_DIR/$TIMELINE_FILE"
        $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i $TMP_PATIENT_FILE $INPUT_TIMELINE_FILEPATH -o $OUTPUT_TIMELINE_FILEPATH -m left --drop-na
    done
}

date
check_args
subset_timeline_files

echo "`date`: CDM timeline file subset for $COHORT complete"