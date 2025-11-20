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

MSK_SOLID_HEME_DATA_HOME="$1"
MSK_IMPACT_DATA_HOME="$2"
MSK_HEMEPACT_DATA_HOME="$3"
MSK_ACCESS_DATA_HOME="$4"
MAPPED_ARCHER_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt

function check_args() {
    if [[ -z $COHORT ]] ; then
        usage
        exit 1
    fi

    if [ ! -d $MSK_SOLID_HEME_DATA_HOME ] || [ ! -d $MSK_IMPACT_DATA_HOME ] || [ ! -d $MSK_HEMEPACT_DATA_HOME ] || [ ! -d $MSK_ACCESS_DATA_HOME ] ; then
        echo "`date`: Unable to locate required data directories, exiting..."
        exit 1
    fi
}

function usage {
    echo "airflow-solid-heme-merge.sh \$MSK_SOLID_HEME_DATA_HOME \$MSK_IMPACT_DATA_HOME \$MSK_HEMEPACT_DATA_HOME \$MSK_ACCESS_DATA_HOME"
    echo -e "\t\$MSK_SOLID_HEME_DATA_HOME                 path to msk_solid_heme merged output"
    echo -e "\t\$MSK_IMPACT_DATA_HOME                     path to mskimpact cohort"
    echo -e "\t\$MSK_HEMEPACT_DATA_HOME                   path to mskimpact_heme cohort"
    echo -e "\t\$MSK_ACCESS_DATA_HOME                     path to mskaccess cohort"
}

function solid_heme_merge() {
    echo "merge of MSK-IMPACT, HEMEPACT, ACCESS data for MSKSOLIDHEME"
    # MSKSOLIDHEME merge and check exit code
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_SOLID_HEME_DATA_HOME -i mskimpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_ACCESS_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSKSOLIDHEME merge failed!"
        exit 1
    else
        echo "MSKSOLIDHEME merge successful! Creating cancer type case lists..."
        echo $(date)
        # add metadata headers and overrides before importing
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical_patient.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
        if [ $? -gt 0 ] ; then
            echo "Error: Adding metadata headers for MSKSOLIDHEME failed!"
            exit 1
        fi
        addCancerTypeCaseLists $MSK_SOLID_HEME_DATA_HOME "mskimpact" "data_clinical_sample.txt" "data_clinical_patient.txt"
        # Merge CDM timeline files
        sh $PORTAL_HOME/scripts/merge-cdm-timeline-files.sh mskimpact $MSK_SOLID_HEME_DATA_HOME "$MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_ACCESS_DATA_HOME"
        if [ $? -gt 0 ] ; then
            echo "Error: CDM timeline file merge for MSKSOLIDHEME"
            exit 1
        fi
    fi
}

date
check_args
solid_heme_merge

echo "`date`: msk_solid_heme merge complete"