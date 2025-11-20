#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : merge-cdm-data.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation-env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

COHORT="$1"
CDM_DATA_DIR="$2"
MERGE_DIR="$3"
OUTPUT_DIR="$4"

function check_args() {
    if [[ -z $COHORT ]] || [[ "$COHORT" != "mskimpact" && "$COHORT" != "mskimpact_heme" && "$COHORT" != "mskaccess" && "$COHORT" != "mskarcher" ]]; then
        usage
        exit 1
    fi

    # Check that required directories exist
    if [ ! -d $CDM_DATA_DIR ] || [ ! -d $MERGE_DIR ] || [ ! -d $OUTPUT_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function usage {
    echo "merge-cdm-data.sh \$COHORT_ID \$CDM_DATA_DIR \$MERGE_DIR \$OUTPUT_DIR"
    echo -e "\t\$COHORT_ID                      one of: ['mskimpact', 'mskimpact_heme', 'mskaccess', 'mskarcher']"
    echo -e "\t\$CDM_DATA_DIR                   path to directory containing CDM data for given cohort"
    echo -e "\t\$MERGE_DIR                      path to directory containing study to merge with"
    echo -e "\t\$OUTPUT_DIR                     merged output will be written to this directory"
}

function validate_sample_file() {
    # Validate the clinical sample file
    $PYTHON3_BINARY $PORTAL_HOME/scripts/validation_utils_py3.py --validation-type cdm --study-dir $CDM_DATA_DIR
    if [ $? -gt 0 ] ; then
        echo "Error: CDM study validation failure for $COHORT"
        exit 1
    fi
}

function merge_cdm_data_and_commit() {
    # Create tmp_processing_directory for merging mskimpact and cdm clinical files
    # All processing is done here and only copied over if everything succeeds
    # No git cleanup needed - tmp_processing_directory removed at the end
    TMP_PROCESSING_DIRECTORY=$(mktemp --tmpdir=$MSK_DMP_TMPDIR -d merge.XXXXXXXX)
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $TMP_PROCESSING_DIRECTORY -i "merged_cdm_${COHORT}" -m true -f clinical_patient,clinical_sample $CDM_DATA_DIR $MERGE_DIR
    if [ $? -gt 0 ] ; then
        echo "Error: Unable to merge CDM and $COHORT clinical files"
        exit 1
    else
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $TMP_PROCESSING_DIRECTORY/data_clinical*.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
        if [ $? -gt 0 ] ; then
            echo "Unable to add metadata headers to merged CDM and $COHORT clinical files"
            exit 1
        else
            cp -a $TMP_PROCESSING_DIRECTORY/data_clinical*.txt $OUTPUT_DIR
            if [[ "$CDM_DATA_DIR" != "$OUTPUT_DIR" ]]; then
                cp -a $CDM_DATA_DIR/data_timeline*.txt $OUTPUT_DIR
            fi
        fi
    fi
    rm -rf $TMP_PROCESSING_DIRECTORY
}

date
check_args
validate_sample_file
merge_cdm_data_and_commit

echo "`date`: CDM merge for $COHORT complete"
