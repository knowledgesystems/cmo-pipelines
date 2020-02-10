#!/usr/bin/env bash

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh

# Make sure necessary environment variables are set before running
if [ -z $PYTHON_BINARY ] || [ -z $GIT_BINARY ] || [ -z $ONCOKB_ANNOTATOR_HOME ] || [ -z $DMP_DATA_HOME ] || [ -z $MSK_SOLID_HEME_DATA_HOME ] || [ -z $ONCOKB_URL ] || [ -z $CANCER_HOTSPOTS_URL ] || [ -z $ONCOKB_TOKEN_FILE ] || [ -z $PORTAL_HOME ] ; then
    message="Could not run oncokb annotation script: automation-environment.sh script must be run in order to set needed environment variables."
    send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
    exit 2
fi

function send_failure_messages () {
    email_message=$1
    email_subject=$2
    slack_message=$3
    echo $email_message
    echo -e "$email_message" |  mail -s "$email_subject" $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$slack_message"
}

function rsync_oncokb_annotated_files () {
    SOURCE_DIR="/data/portal-cron/cbio-portal-data/dmp/msk_solid_heme/oncokb"
    TMP_DIR="/data/portal-cron/tmp/rsync_oncokb"
    INDEX_FILE="$TMP_DIR/index.html"
    TITLE_STRING="oncokb-annotation-msk-impact"
    BASE_URL="http://download.cbioportal.org/oncokb-annotated-msk-impact"
    REMOTE_HOST="ramen.cbio.mskcc.org"
    REMOTE_DIR="/data/public/oncokb-annotated-msk-impact"
    RSYNC_TARGET="$REMOTE_HOST:$REMOTE_DIR"
    RSYNC_IDENTITY_FILE="/home/cbioportal_importer/.ssh/id_rsa_restful_login_counter_key"
    RSYNC_RSH="--rsh=ssh -i $RSYNC_IDENTITY_FILE"
    $PORTAL_HOME/scripts/create_web_directory_index_file.sh "$SOURCE_DIR" "$TMP_DIR" "$BASE_URL" "$TITLE_STRING"
    rsync -a --rsh="ssh -i $RSYNC_IDENTITY_FILE" "$SOURCE_DIR/" "$RSYNC_TARGET"
    rsync -a --rsh="ssh -i $RSYNC_IDENTITY_FILE" "$INDEX_FILE" "$RSYNC_TARGET"
    rm -f "$INDEX_FILE"
}

if [ ! -r $ONCOKB_TOKEN_FILE ]; then
    message="Could not run oncokb annotation script: token file '$ONCOKB_TOKEN_FILE' does not exist or is not readable."
    send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
    exit 2
fi

ONCOKB_TOKEN=`cat $ONCOKB_TOKEN_FILE`

if [ -z $ONCOKB_TOKEN ]; then
    message="Could not run oncokb annotation script: token file '$ONCOKB_TOKEN_FILE' is empty."
    send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
    exit 2
fi

ONCOKB_ANNOTATION_SUCCESS=1

# Update DMP repo before running annotations
fetch_updates_in_data_sources dmp

# Annotating MAF
echo $(date)
echo "Beginning MAF annotation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    # if an oncokb annotated maf already exists then create a copy that can be used to speed up the annotation process
    PREVIOUS_ANNOTATED_MAF_ARGS=""
    PREVIOUS_ANNOTATED_MAF_NAME=$MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.previous.txt
    if [ -f $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt ] ; then
        cp $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt $PREVIOUS_ANNOTATED_MAF_NAME
        PREVIOUS_ANNOTATED_MAF_ARGS="-p $PREVIOUS_ANNOTATED_MAF_NAME"
    fi

    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/MafAnnotator.py -b $ONCOKB_TOKEN -i $MSK_SOLID_HEME_DATA_HOME/data_mutations_extended.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL -v $CANCER_HOTSPOTS_URL $PREVIOUS_ANNOTATED_MAF_ARGS
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate MAF, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi

    # if a previous annotated maf was created during this step then remove it from directory
    if [ -f $PREVIOUS_ANNOTATED_MAF_NAME ] ; then
        rm $PREVIOUS_ANNOTATED_MAF_NAME
    fi
fi

# Annotating Fusions
echo $(date)
echo "Beginning fusions annotation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/FusionAnnotator.py -b $ONCOKB_TOKEN -i $MSK_SOLID_HEME_DATA_HOME/data_fusions.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate fusion file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Annotating CNA
echo $(date)
echo "Beginning CNA annotation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/CnaAnnotator.py -b $ONCOKB_TOKEN -i $MSK_SOLID_HEME_DATA_HOME/data_CNA.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt -c $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -u $ONCOKB_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate CNA file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Annotating Clinical
echo $(date)
echo "Beginning clinical annotation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/ClinicalDataAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.txt -a $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate clinical file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Generating Clinical PDF
echo $(date)
echo "Beginning clinical PDF generation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/OncoKBPlots.py -i $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample.oncokb.pdf -n 100
    if [ $? -ne 0 ] ; then
        echo "Failed to generate clinical pdf, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Annotating Somatic Clinical
echo $(date)
echo "Beginning somatic clinical annotation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    # Generate somatic-only MAF by excluding lines including 'GERMLINE'
    awk -F'\t' '$26 != "GERMLINE"' $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended.oncokb.txt > $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended_somatic.oncokb.txt

    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/ClinicalDataAnnotator.py -i $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.txt -a $MSK_SOLID_HEME_DATA_HOME/oncokb/data_mutations_extended_somatic.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_fusions.oncokb.txt,$MSK_SOLID_HEME_DATA_HOME/oncokb/data_CNA.oncokb.txt
    if [ $? -ne 0 ] ; then
        echo "Failed to annotate somatic clinical file, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Generating Somatic Clinical PDF
echo $(date)
echo "Beginning somatic clinical PDF generation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/OncoKBPlots.py -i $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.txt -o $MSK_SOLID_HEME_DATA_HOME/oncokb/data_clinical_sample_somatic.oncokb.pdf -n 100
    if [ $? -ne 0 ] ; then
        echo "Failed to generate somatic clinical pdf, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Generating README
echo $(date)
echo "Beginning README generation..."
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
    $PYTHON_BINARY $ONCOKB_ANNOTATOR_HOME/GenerateReadMe.py -o $MSK_SOLID_HEME_DATA_HOME/oncokb/README -u $ONCOKB_URL
    if [ $? -ne 0 ] ; then
        echo "Failed to generate README, exiting..."
        ONCOKB_ANNOTATION_SUCCESS=0
    fi
fi

# Only commit if all steps succeeded
if [ $ONCOKB_ANNOTATION_SUCCESS -eq 0 ] ; then
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/dmp
    message="OncoKB Annotation failed for MSKSOLIDHEME. Check logs for more details."
    send_failure_messages "$message" "MSKSOLIDHEME OncoKB Annotation Failure" "MSKSOLIDHEME OncoKB Annotation"
else
    echo "Running rsync to download.cbioportal.org for OncoKB Annotation for MSKSOLIDHEME"
    rsync_oncokb_annotated_files 
fi

echo $(date)
echo "OncoKB annotations complete"
