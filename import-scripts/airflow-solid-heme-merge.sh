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
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

MERGED_STUDY_ID="$1"
OUTPUT_DIR="$2"
MSK_IMPACT_DIR="$3"
MSK_HEMEPACT_DIR="$4"
MSK_ACCESS_DIR="$5"
MAPPED_ARCHER_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt

function check_args() {
    # Ensure that data directories exist
    if [ ! -d $MSK_IMPACT_DIR ] || [ ! -d $MSK_HEMEPACT_DIR ] || [ ! -d $MSK_ACCESS_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi

    # Create output dir if it doesn't exist
    if [ ! -d $OUTPUT_DIR ] ; then
        mkdir $OUTPUT_DIR
    fi
}

function usage {
    echo "airflow-solid-heme-merge.sh \$OUTPUT_DIR \$MSK_IMPACT_DIR \$MSK_HEMEPACT_DIR \$MSK_ACCESS_DIR"
    echo -e "\t\$MERGED_STUDY_ID                    cancer_study_identifier for study"
    echo -e "\t\$OUTPUT_DIR                         path for msk_solid_heme merged output"
    echo -e "\t\$MSK_IMPACT_DIR                     path to mskimpact cohort"
    echo -e "\t\$MSK_HEMEPACT_DIR                   path to mskimpact_heme cohort"
    echo -e "\t\$MSK_ACCESS_DIR                     path to mskaccess cohort"
}

function solid_heme_merge() {
    echo "merge of MSK-IMPACT, HEMEPACT, ACCESS data for MSKSOLIDHEME"
    # MSKSOLIDHEME merge and check exit code
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $OUTPUT_DIR -i mskimpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DIR $MSK_HEMEPACT_DIR $MSK_ACCESS_DIR
    if [ $? -gt 0 ] ; then
        echo "MSKSOLIDHEME merge failed!"
        exit 1
    else
        echo "MSKSOLIDHEME merge successful! Creating cancer type case lists..."
        echo $(date)
        # add metadata headers and overrides before importing
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $OUTPUT_DIR/data_clinical_sample.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $OUTPUT_DIR/data_clinical_patient.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
        if [ $? -gt 0 ] ; then
            echo "Error: Adding metadata headers for MSKSOLIDHEME failed!"
            exit 1
        fi
        addCancerTypeCaseLists $OUTPUT_DIR "mskimpact" "data_clinical_sample.txt" "data_clinical_patient.txt"
        # Merge CDM timeline files
        sh $PORTAL_HOME/scripts/merge-cdm-timeline-files.sh mskimpact $OUTPUT_DIR "$MSK_IMPACT_DIR $MSK_HEMEPACT_DIR $MSK_ACCESS_DIR"
        if [ $? -gt 0 ] ; then
            echo "Error: CDM timeline file merge for MSKSOLIDHEME"
            exit 1
        fi
    fi
}

function add_meta_files() {
    # If meta files already exist for this study, don't recreate them
    if ls $OUTPUT_DIR/*meta*.txt 1> /dev/null 2>&1; then
        exit 0
    fi

    echo "Adding metadata files to MSKSOLIDHEME"
    # Copy over metadata files from production msk_solid_heme study
    rsync -a --include="*meta*.txt" $MSK_SOLID_HEME_DATA_HOME/ $OUTPUT_DIR/

    # Replace cancer_study_identifier and stable_id with $STUDY_ID in meta files
    sed -i "/^cancer_study_identifier: .*$/s/mskimpact/${MERGED_STUDY_ID}/g" $OUTPUT_DIR/*meta*.txt
    sed -i "/^stable_id: .*$/s/mskimpact/${MERGED_STUDY_ID}/g" ${OUTPUT_DIR}/*meta*.txt

    # Replace cancer_study_identifier and stable_id with $STUDY_ID in case list files
    sed -i "/^cancer_study_identifier: .*$/s/mskimpact/${MERGED_STUDY_ID}/g" $OUTPUT_DIR/case_lists/case_list*.txt
    sed -i "/^stable_id: .*$/s/mskimpact/${MERGED_STUDY_ID}/g" $OUTPUT_DIR/case_lists/case_list*.txt
}

date
check_args
solid_heme_merge
add_meta_files

echo "`date`: msk_solid_heme merge complete"