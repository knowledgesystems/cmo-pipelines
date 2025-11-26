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

STUDY_ID="$1"
OUTPUT_DIR="$2"
MSK_IMPACT_DIR="$3"
MSK_HEMEPACT_DIR="$4"
MSK_ACCESS_DIR="$5"
MAPPED_ARCHER_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt

function check_args() {
    if [ ! -d $OUTPUT_DIR ] || [ ! -d $MSK_IMPACT_DIR ] || [ ! -d $MSK_HEMEPACT_DIR ] || [ ! -d $MSK_ACCESS_DIR ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function usage {
    echo "airflow-solid-heme-merge.sh \$OUTPUT_DIR \$MSK_IMPACT_DIR \$MSK_HEMEPACT_DIR \$MSK_ACCESS_DIR"
    echo -e "\t\$STUDY_ID                           cancer_study_identifier for study"
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
    # Copy over metadata files from production msk_solid_heme study
    rsync -a --include="*meta*.txt" $MSK_SOLID_HEME_DATA_HOME/ $OUTPUT_DIR/

    # Replace cancer_study_identifier with msk_chord_review
    sed -i "/^cancer_study_identifier: .*$/s/mskimpact/$STUDY_ID/g" $OUTPUT_DIR/*meta*.txt
}

date
check_args
solid_heme_merge
add_meta_files

echo "`date`: msk_solid_heme merge complete"