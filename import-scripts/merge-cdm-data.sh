#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : merge-cdm-data.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

COHORT=$1
CDM_DATA_DIR=""
PROD_DATA_DIR=""
COHORT_NAME_FOR_COMMIT_MSG=""

function check_args() {
    if [[ -z $COHORT ]] || [[ "$COHORT" != "mskimpact" && "$COHORT" != "mskimpact_heme" && "$COHORT" != "mskaccess" && "$COHORT" != "mskarcher" ]]; then
        usage
        exit 1
    fi
}

function usage {
    echo "merge-cdm-data.sh \$COHORT_ID"
    echo -e "\t\$COHORT_ID                      one of: ['mskimpact', 'mskimpact_heme', 'mskaccess', 'mskarcher']"
}

function set_cohort_filepaths() {
    # Set data directory paths
    CDM_DATA_DIR="$MSK_CHORD_DATA_HOME/$COHORT"
    if [ "$COHORT" == "mskimpact" ] ; then
        PROD_DATA_DIR="$MSK_IMPACT_DATA_HOME"
        COHORT_NAME_FOR_COMMIT_MSG="MSKIMPACT"
    elif [ "$COHORT" == "mskimpact_heme" ] ; then
        PROD_DATA_DIR="$MSK_HEMEPACT_DATA_HOME"
        COHORT_NAME_FOR_COMMIT_MSG="HEMEPACT"
    elif [ "$COHORT" == "mskarcher" ] ; then
        PROD_DATA_DIR="$MSK_ARCHER_UNFILTERED_DATA_HOME"
        COHORT_NAME_FOR_COMMIT_MSG="ARCHER"
    elif [ "$COHORT" == "mskaccess" ] ; then
        PROD_DATA_DIR="$MSK_ACCESS_DATA_HOME"
        COHORT_NAME_FOR_COMMIT_MSG="ACCESS"
    fi

    # Check that required directories exist
    if [ ! -d $CDM_DATA_DIR ] || [ ! -d $PROD_DATA_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function validate_sample_file() {
    # Validate the clinical sample file
    $PYTHON3_BINARY $PORTAL_HOME/scripts/validation_utils_py3.py --validation-type cdm --study-dir $CDM_DATA_DIR
    if [ $? -gt 0 ] ; then
        echo "Error: CDM study validation failure for $COHORT_NAME_FOR_COMMIT_MSG"
        exit 1
    fi
}

function merge_cdm_data_and_commit() {
    # Create tmp_processing_directory for merging mskimpact and cdm clinical files
    # All processing is done here and only copied over if everything succeeds
    # No git cleanup needed - tmp_processing_directory removed at the end
    TMP_PROCESSING_DIRECTORY=$(mktemp --tmpdir=$MSK_DMP_TMPDIR -d merge.XXXXXXXX)
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $TMP_PROCESSING_DIRECTORY -i "merged_cdm_${COHORT}" -m true -f clinical_patient,clinical_sample $CDM_DATA_DIR $PROD_DATA_DIR
    if [ $? -gt 0 ] ; then
        echo "Error: Unable to merge CDM and $COHORT_NAME_FOR_COMMIT_MSG clinical files"
        exit 1
    else
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $TMP_PROCESSING_DIRECTORY/data_clinical*.txt -i /data/portal-cron/scripts/cdm_metadata.json
        if [ $? -gt 0 ] ; then
            echo "Unable to add metadata headers to merged CDM and $COHORT_NAME_FOR_COMMIT_MSG clinical files"
            exit 1
        else
            cp -a $TMP_PROCESSING_DIRECTORY/data_clinical*.txt $PROD_DATA_DIR
            cp -a $CDM_DATA_DIR/data_timeline*.txt $PROD_DATA_DIR
            cd $PROD_DATA_DIR ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest $COHORT_NAME_FOR_COMMIT_MSG dataset: CDM Annotation"
        fi
    fi
    rm -rf $TMP_PROCESSING_DIRECTORY
}

date
check_args
set_cohort_filepaths
validate_sample_file
merge_cdm_data_and_commit

echo "`date`: CDM merge for $COHORT_NAME_FOR_COMMIT_MSG complete"
