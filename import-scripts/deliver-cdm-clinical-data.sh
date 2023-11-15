#!/usr/bin/env bash
# deliver-cdm-clinical-data.sh

source $PORTAL_HOME/scripts/filter-clinical-arg-functions.sh

INPUT_CLINICAL_FILE="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"
# Clinical attributes that we want to deliver in our data
DELIVERED_ATTRIBUTES="SAMPLE_ID PATIENT_ID"
# TODO where do we want the output clinical file to go?
OUTPUT_CLINICAL_FILE="/path/to/output"
# TODO put the cert file somewhere
MINIO_CERTS="/path/to/cert"
MINIO_ENDPOINT="tllihpcmind6:9000"
# TODO put access_key and secret_key files somewhere
MINIO_ACCESS_KEY="cat /path/to/access/key"
MINIO_SECRET_KEY="cat /path/to/secret/key"
MINIO_CDM_BUCKET_NAME="cdm-data"
# TODO this will be the name of the object in minio - what name do we want?
MINIO_OBJECT_NAME="cbioportal/test_data_clinical_sample.txt"
MINIO_FILE_TO_INSERT="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"

filter_clinical_attribute_columns $INPUT_FILE $DELIVERED_ATTRIBUTES $OUTPUT_FILE
if [ $? -gt 0 ] ; then
    echo "Failed to filter clinical attribute columns for CDM."
else
    # Upload the file to MinIO
    $PYTHON3_BINARY $PORTAL_HOME/scripts/minio_insert_py3.py \
        -c $MINIO_CERTS \
        -e $MINIO_ENDPOINT \
        -a $MINIO_ACCESS_KEY \
        -s $MINIO_SECRET_KEY \
        -b $MINIO_CDM_BUCKET_NAME \
        -o $MINIO_OBJECT_NAME \
        -f $MINIO_FILE_TO_INSERT
    
    if [ $? -gt 0 ] ; then
        echo "Failed to filter clinical attribute columns for CDM."
    fi
fi