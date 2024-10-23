#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : update-cdm-deliverable.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh
source $PORTAL_HOME/scripts/filter-clinical-arg-functions.sh

COHORT=$1
SEQ_DATE_FILEPATH=""
SEQ_DATE_FILENAME="cvr/seq_date.txt"
CLINICAL_SAMPLE_FILEPATH=""
CLINICAL_SAMPLE_FILENAME="data_clinical_sample.txt"
CLINICAL_SAMPLE_S3_FILEPATH="sample-files/$COHORT/$CLINICAL_SAMPLE_FILENAME"
TMP_SAMPLE_FILE=$(mktemp -q)
CDM_DELIVERABLE=$(mktemp -q)

function check_args() {
    if [[ -z $COHORT ]] || [[ "$COHORT" != "mskimpact" && "$COHORT" != "mskimpact_heme" && "$COHORT" != "mskaccess" && "$COHORT" != "mskarcher" ]]; then
        usage
        exit 1
    fi
}

function usage {
    echo "update-cdm-deliverable.sh \$COHORT_ID"
    echo -e "\t\$COHORT_ID                      one of: ['mskimpact', 'mskimpact_heme', 'mskaccess', 'mskarcher']"
}

function set_cohort_filepaths() {
    # SET DATA DIRECTORY for seq date file and clinical file
    if [ "$COHORT" == "mskimpact" ] ; then
        SEQ_DATE_FILEPATH="$MSK_IMPACT_DATA_HOME/$SEQ_DATE_FILENAME"
        CLINICAL_SAMPLE_FILEPATH="$MSK_IMPACT_DATA_HOME/$CLINICAL_SAMPLE_FILENAME"
    elif [ "$COHORT" == "mskimpact_heme" ] ; then
        SEQ_DATE_FILEPATH="$MSK_HEMEPACT_DATA_HOME/$SEQ_DATE_FILENAME"
        CLINICAL_SAMPLE_FILEPATH="$MSK_HEMEPACT_DATA_HOME/$CLINICAL_SAMPLE_FILENAME"
    elif [ "$COHORT" == "mskarcher" ] ; then
        SEQ_DATE_FILEPATH="$MSK_ARCHER_UNFILTERED_DATA_HOME/$SEQ_DATE_FILENAME"
        CLINICAL_SAMPLE_FILEPATH="$MSK_ARCHER_UNFILTERED_DATA_HOME/$CLINICAL_SAMPLE_FILENAME"
    elif [ "$COHORT" == "mskaccess" ] ; then
        SEQ_DATE_FILEPATH="$MSK_ACCESS_DATA_HOME/$SEQ_DATE_FILENAME"
        CLINICAL_SAMPLE_FILEPATH="$MSK_ACCESS_DATA_HOME/$CLINICAL_SAMPLE_FILENAME"
    fi

    # Check that required files exist
    if [ ! -f $SEQ_DATE_FILEPATH ] || [ ! -f $CLINICAL_SAMPLE_FILEPATH ] ; then
        echo "`date`: Unable to locate required files, exiting..."
        exit 1
    fi
}

function filter_sample_file() {
    DELIVERED_SAMPLE_ATTRIBUTES="SAMPLE_ID PATIENT_ID CANCER_TYPE CANCER_TYPE_DETAILED"
    TMP_PROCESSING_FILE=$(mktemp -q)

    # Copy sample file to tmp file since script overwrites existing file (don't want to overwrite DMP pipeline files)
    # Removes all clinical attributes except those specified in $DELIVERED_SAMPLE_ATTRIBUTES set
    # TMP_PROCESSING_FILE automatically removed // TODO: move TMP_FILE creation to filter_clinical function
    cp -a $CLINICAL_SAMPLE_FILEPATH $TMP_SAMPLE_FILE
    filter_clinical_attribute_columns "$TMP_SAMPLE_FILE" "$DELIVERED_SAMPLE_ATTRIBUTES" "$TMP_PROCESSING_FILE"
    if [ $? -ne 0 ] ; then
        echo "`date`: Failed to subset clinical sample file, exiting..."
        exit 1
    fi
}

function add_seq_date_to_sample_file() {
    # Combines filtered clinical sample file with seq data file and outputs to tmp file for upload
    # Uses left join -- SAMPLE_ID and PATIENT_ID in the clinical_sample_file (first arg) will be valid keys
    $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$TMP_SAMPLE_FILE" "$SEQ_DATE_FILEPATH" -o "$CDM_DELIVERABLE" -c SAMPLE_ID PATIENT_ID -m left
    if [ $? -ne 0 ] ; then
        echo "`date`: Failed to combine files, exiting..."
        exit 1
    fi
    rm $TMP_SAMPLE_FILE
}

function upload_to_s3() {
    BUCKET_NAME="cdm-deliverable"

    # Authenticate and upload into S3 bucket
    $PORTAL_HOME/scripts/authenticate_service_account.sh eks
    aws s3 cp $CDM_DELIVERABLE s3://$BUCKET_NAME/$CLINICAL_SAMPLE_S3_FILEPATH --profile saml
    if [ $? -ne 0 ] ; then
        echo "`date`: Failed to upload CDM deliverable to S3, exiting..."
        exit 1
    fi
    rm $CDM_DELIVERABLE
}

function trigger_cdm_dags() {
    TMP_LOG_FILE=$(mktemp -q)
    AIRFLOW_ADMIN_CREDENTIALS_FILE="${PORTAL_HOME}/pipelines-credentials/airflow-admin.credentials"
    AIRFLOW_CREDS=$(cat $AIRFLOW_ADMIN_CREDENTIALS_FILE)
    DATA='{"conf": {"sample_filepath": "'"$CLINICAL_SAMPLE_S3_FILEPATH"'", "cohort_name": "'"$COHORT"'"}}'
    AIRFLOW_URL="https://airflow.cbioportal.aws.mskcc.org"
    DAG_ID="cdm_etl_cbioportal_s3_pull"
    AIRFLOW_API_ENDPOINT="${AIRFLOW_URL}/api/v1/dags/${DAG_ID}/dagRuns"

    # Trigger CDM DAG to pull updated data_clinical_sample.txt from S3
    # This DAG will kick off the rest of the CDM pipeline when it completes
    HTTP_STATUS_CODE=$(curl -X POST --write-out "%{http_code}" --silent --output $TMP_LOG_FILE --header "Authorization: Basic ${AIRFLOW_CREDS}" --header "Content-Type: application/json" --data "$DATA" $AIRFLOW_API_ENDPOINT)
    if [ $HTTP_STATUS_CODE -ne 200 ] ; then
        # Send alert for HTTP status code if not 200
        echo "`date`: Failed attempt to trigger DAG ${DAG_ID} on Airflow server ${AIRFLOW_URL}. HTTP status code = ${HTTP_STATUS_CODE}, exiting..."
        # Write out failed HTTP response contents and exit with error
        cat $TMP_LOG_FILE
        rm $TMP_LOG_FILE
        exit 1
    fi
}

date
check_args
set_cohort_filepaths
filter_sample_file
add_seq_date_to_sample_file
upload_to_s3
trigger_cdm_dags

echo "`date`: CDM deliverable generation and upload complete"
