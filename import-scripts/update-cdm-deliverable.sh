#!/bin/bash

DELIVERED_SAMPLE_ATTRIBUTES="SAMPLE_ID PATIENT_ID CANCER_TYPE CANCER_TYPE_DETAILED"

if ! [ -n "$PORTAL_HOME" ] ; then
  echo "Error : update-cdm-deliverable.sh cannot be run without setting the PORTAL_HOME environment variable."
  exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] || [ ! -f $PORTAL_HOME/scripts/filter-clinical-arg-functions.sh ] ; then
  echo "`date`: Unable to locate automation_env and additional modules, exiting..."
  exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh
source $PORTAL_HOME/scripts/filter-clinical-arg-functions.sh

TMP_SAMPLE_FILE=$(mktemp -q)
TMP_MERGED_SEQ_DATE=$(mktemp -q)
TMP_PROCESSING_FILE=$(mktemp -q)
CDM_DELIVERABLE=$(mktemp -q)

SEQ_DATE_FILENAME="cvr/seq_date.txt"
MSK_SOLID_HEME_CLINICAL_FILE="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"
MSK_ACCESS_SEQ_DATE="$MSK_ACCESS_DATA_HOME/$SEQ_DATE_FILENAME"
MSK_HEMEPACT_SEQ_DATE="$MSK_HEMEPACT_DATA_HOME/$SEQ_DATE_FILENAME"
MSK_IMPACT_SEQ_DATE="$MSK_IMPACT_DATA_HOME/$SEQ_DATE_FILENAME"

TMP_LOG_FILE="${PORTAL_HOME}/tmp/trigger_s3_dag.log"
AIRFLOW_ADMIN_CREDENTIALS_FILE="${PORTAL_HOME}/pipelines-credentials/airflow-admin.credentials"
AIRFLOW_CREDS=$(cat $AIRFLOW_ADMIN_CREDENTIALS_FILE)
AIRFLOW_URL="https://airflow.cbioportal.aws.mskcc.org"
DAG_ID="cdm_etl_cbioportal_s3_pull"
AIRFLOW_API_ENDPOINT="${AIRFLOW_URL}/api/v1/dags/${DAG_ID}/dagRuns"

if [ ! -f $MSK_SOLID_HEME_CLINICAL_FILE ] || [ ! -f $MSK_ACCESS_SEQ_DATE ] || [ ! -f $MSK_HEMEPACT_SEQ_DATE ] || [ ! -f $MSK_IMPACT_SEQ_DATE ] ; then
  echo "`date`: Unable to locate required files, exiting..."
  exit 1
fi

# Copy sample file to tmp file since script overwrites existing file (don't want to overwrite DMP pipeline files)
# Removes all clinical attributes except those specified in $DELIVERED_SAMPLE_ATTRIBUTES set at top
# TMP_PROCESSING_FILE automatically removed // TODO: move TMP_FILE creation to filter_clinical function
cp -a $MSK_SOLID_HEME_CLINICAL_FILE $TMP_SAMPLE_FILE
filter_clinical_attribute_columns "$TMP_SAMPLE_FILE" "$DELIVERED_SAMPLE_ATTRIBUTES" "$TMP_PROCESSING_FILE"
if [ $? -ne 0 ] ; then
  echo "`date`: Failed to subset clinical sample file, exiting..."
  exit 1
fi

# Combines impact, access, hemepact seq date files
$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$MSK_ACCESS_SEQ_DATE" "$MSK_HEMEPACT_SEQ_DATE" "$MSK_IMPACT_SEQ_DATE" -o "$TMP_MERGED_SEQ_DATE" -m outer

# Combines filtered clinical sample file with seq data file and outputs to tmp file for upload
# Uses left join -- SAMPLE_ID and PATIENT_ID in the clinical_sample_file (first arg) will be valid keys
$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$TMP_SAMPLE_FILE" "$TMP_MERGED_SEQ_DATE" -o "$CDM_DELIVERABLE" -c SAMPLE_ID PATIENT_ID -m left
if [ $? -ne 0 ] ; then
  echo "`date`: Failed to combine files, exiting..."
  exit 1
fi

# Authenticate and upload into S3 bucket
$PORTAL_HOME/scripts/authenticate_service_account.sh eks
aws s3 cp $CDM_DELIVERABLE s3://cdm-deliverable/data_clinical_sample.txt --profile saml
if [ $? -ne 0 ] ; then
  echo "`date`: Failed to upload CDM deliverable to S3, exiting..."
fi

rm $TMP_SAMPLE_FILE
rm $TMP_MERGED_SEQ_DATE
rm $CDM_DELIVERABLE

# Trigger CDM DAG to pull updated data_clinical_sample.txt from S3
# This DAG will kick off the rest of the CDM pipeline when it completes
HTTP_STATUS_CODE=$(curl -X POST --write-out "%{http_code}" --silent --output $TMP_LOG_FILE --header "Authorization: Basic ${AIRFLOW_CREDS}" --header "Content-Type: application/json" --data "{}" $AIRFLOW_API_ENDPOINT)
if [ $HTTP_STATUS_CODE -ne 200 ] ; then
  # Send alert for HTTP status code if not 200
  echo "`date`: Failed attempt to trigger DAG ${DAG_ID} on Airflow server ${AIRFLOW_URL}. HTTP status code = ${HTTP_STATUS_CODE}, exiting..."
  # Write out failed HTTP response contents and exit with error
  cat $TMP_LOG_FILE
  rm $TMP_LOG_FILE
  exit 1
fi

echo "`date`: CDM deliverable generation and upload complete"