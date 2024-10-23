#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : merge-cdm-timeline-files.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

COHORT=$1
OUTPUT_DIR=""
MERGE_DIRS=()
FILE_LIST=()
COHORT_NAME_FOR_COMMIT_MSG=""

function check_args() {
    if [[ -z $COHORT ]] || [[ "$COHORT" != "mixedpact" && "$COHORT" != "mskimpact" && "$COHORT" != "lymphoma_super_cohort_fmi_msk" ]]; then
        usage
        exit 1
    fi
}

function usage {
    echo "merge-cdm-timeline-files.sh \$COHORT_ID"
    echo -e "\t\$COHORT_ID                      one of: ['mixedpact', 'mskimpact', 'lymphoma_super_cohort_fmi_msk']"
}

function set_cohort_filepaths() {
    # Set data directory paths
    if [ "$COHORT" == "mixedpact" ] ; then
        COHORT="MIXEDPACT"
        OUTPUT_DIR=$MSK_MIXEDPACT_DATA_HOME
        MERGE_DIRS=("$MSK_IMPACT_DATA_HOME" "$MSK_HEMEPACT_DATA_HOME" "$MSK_RAINDANCE_DATA_HOME" "$MSK_ARCHER_UNFILTERED_DATA_HOME" "$MSK_ACCESS_DATA_HOME")
    elif [ "$COHORT" == "mskimpact" ] ; then
        COHORT="MSKSOLIDHEME"
        OUTPUT_DIR=$MSK_SOLID_HEME_DATA_HOME
        MERGE_DIRS=("$MSK_IMPACT_DATA_HOME" "$MSK_HEMEPACT_DATA_HOME" "$MSK_ACCESS_DATA_HOME")
    elif [ "$COHORT" == "lymphoma_super_cohort_fmi_msk" ] ; then
        COHORT="LYMPHOMA_SUPER_COHORT"
        OUTPUT_DIR=$LYMPHOMA_SUPER_COHORT_DATA_HOME
        MERGE_DIRS=("$MSK_IMPACT_DATA_HOME" "$MSK_HEMEPACT_DATA_HOME" "$FMI_BATLEVI_DATA_HOME")
    fi

    # This gets the base filename for each of the timeline files
    FILE_LIST=($(cd ${MERGE_DIRS[0]} && ls data_timeline_*.txt))
    
    # Check that required directories exist
    if [ ! -d $OUTPUT_DIR ] || [ ! -d $MSK_IMPACT_DATA_HOME ] || [ ! -d $MSK_HEMEPACT_DATA_HOME ] || [ ! -d $MSK_RAINDANCE_DATA_HOME ] || [ ! -d $MSK_ARCHER_UNFILTERED_DATA_HOME ] || [ ! -d $MSK_ACCESS_DATA_HOME ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function merge_timeline_files() {
    # Merge each type of timeline file in each data directory
    for TIMELINE_FILE in ${FILE_LIST[@]}; do
        FILES_TO_MERGE=""
        for MERGE_DIR in ${MERGE_DIRS[@]}; do
            # Check if the file exists before adding to command
            FILE_TO_MERGE="$MERGE_DIR/$TIMELINE_FILE"
            if [ -f $FILE_TO_MERGE ]; then
                FILES_TO_MERGE="$FILES_TO_MERGE $FILE_TO_MERGE"
            fi
        done
        $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i $FILES_TO_MERGE -o $OUTPUT_DIR/$TIMELINE_FILE -m outer
    done

}

date
check_args
set_cohort_filepaths
merge_timeline_files

echo "`date`: CDM timeline file merge for $COHORT complete"