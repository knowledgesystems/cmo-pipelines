#!/bin/bash

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

ONCOKB_ANNOTATION_SUCCESS=1

# Make sure necessary environment variables are set before running
if [ -z $PYTHON3_BINARY ] | [ -z $HG_BINARY ] | [ -z $ONCOKB_ANNOTATOR_HOME ] | [ -z $MSK_SOLID_HEME_DATA_HOME ] | [ -z $ONCOKB_URL ] | [ -z $CANCER_HOTSPOTS_URL ] ; then
    message="could not run oncokb annotation script: automation-environment.sh script must be run in order to set needed environment variables."
    echo $message
    echo -e "$message" |  mail -s "oncokb-annotator failed to run." $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$message"
    exit 2
fi

# Annotating MAF
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/MafAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_mutations_extended.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL -v $CANCER_HOTSPOTS_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate MAF, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Annotating Fusions
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/FusionAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_fusions.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate fusion file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Annotating CNA
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/CnaAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_CNA.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate CNA file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Annotating Clinical
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/ClinicalDataAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.txt -a $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate clinical file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Generating Clinical PDF
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/OncoKBPlots.py -i $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.pdf -n 100
    if [ $? -ne 0 ] ; then
        echo "Failed to generate clinical pdf, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Annotating Somatic Clinical
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    # Generate somatic-only MAF by excluding lines including 'GERMLINE'
    awk -F'\t' '$26 != "GERMLINE"' $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt >$MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended_somatic.oncokb.txt

    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/ClinicalDataAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.txt -a $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended_somatic.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate somatic clinical file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Generating Somatic Clinical PDF
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON3_BINARY $ONCOKB_ANNOTATOR_HOME/OncoKBPlots.py -i $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.pdf -n 100
    if [ $? -ne 0 ] ; then
        echo "Failed to generate somatic clinical pdf, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0 
    fi
fi

# Only commit if all steps succeeded
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 0 ] ; then
    cd $MSK_SOLID_HEME_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    message="OncoKB Annotation failed for MSKSOLIDHEME. Check logs for more details."
    echo $message
    echo -e "$message" |  mail -s "MSKSOLIDHEME OncoKB Annotation Failure" $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKSOLIDHEME OncoKB Annotation"
else
    echo "committing OncoKB Annotation for MSKSOLIDHEME"
    cd $MSK_SOLID_HEME_DATA_HOME ; $HG_BINARY commit -m "Latest MSKSOLIDHEME OncoKB Annotations"
fi
