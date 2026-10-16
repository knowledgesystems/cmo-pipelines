#!/bin/bash

# Delivers the AstraZeneca MSK-IMPACT study exported by the Databricks bundle
# data-partnerships-databricks-pipelines/astrazeneca-mskimpact-v1 (replaces the delivery half
# of update-az-mskimpact.sh). Run one step at a time by the deliver_az_mskimpact DAG:
#
#   find-run        select the latest exported run (s3://<bucket>/<prefix>/_LATEST) that is
#                   marked _READY and not yet _DELIVERED. Exits 1 if it is not ready yet
#                   (the DAG retries) and 99 if it was already delivered (the DAG skips).
#   fetch           download the run to $AZ_DELIVERY_HOME/<run date>/
#   add-headers     add the clinical attribute metadata header rows (CDD)
#   sftp            upload az_mskimpact/, gene_panels/ and README.md to AstraZeneca's SFTP server
#   mark-delivered  write <run>/_DELIVERED to S3 (the next export's changelog diffs against it)
#   cleanup         remove the local copy of the run

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : airflow-deliver-az-mskimpact.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
    echo "`date`: Unable to locate automation_env, exiting..."
    exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

STEP="$1"

AZ_S3_BUCKET="${AZ_S3_BUCKET:-az-mskimpact}"
AZ_S3_PREFIX="${AZ_S3_PREFIX:-delivery}"
AZ_DELIVERY_HOME="${AZ_DELIVERY_HOME:-$PORTAL_DATA_HOME/az-mskimpact-delivery}"
AZ_SFTP_KEY="/home/cbioportal_importer/.ssh/id_rsa_astrazeneca_sftp"
AZ_SFTP_REMOTE_DIR="az-data"
AZ_STUDY_DIR="az_mskimpact"
CDD_URL="https://cdd.cbioportal.mskcc.org/api/"

# The run selected by find-run, read by the later steps.
CURRENT_RUN_FILE="$AZ_DELIVERY_HOME/current_run"

S3_BASE="s3://$AZ_S3_BUCKET/$AZ_S3_PREFIX"
EXIT_NOT_READY=1
EXIT_ALREADY_DELIVERED=99

function usage() {
    echo "airflow-deliver-az-mskimpact.sh <find-run|fetch|add-headers|sftp|mark-delivered|cleanup>"
}

function authenticate() {
    if ! $PORTAL_HOME/scripts/authenticate_service_account.sh eks ; then
        echo "`date`: Failed to authenticate to AWS, exiting..."
        exit 1
    fi
}

function s3_exists() {
    aws s3 ls "$1" --profile saml > /dev/null 2>&1
}

function current_run_date() {
    if [ ! -s "$CURRENT_RUN_FILE" ] ; then
        echo "`date`: No run selected ($CURRENT_RUN_FILE missing); run the find-run step first, exiting..." >&2
        exit 1
    fi
    cat "$CURRENT_RUN_FILE"
}

function find_run() {
    authenticate
    mkdir -p "$AZ_DELIVERY_HOME"

    LATEST_RUN_PATH=$(aws s3 cp "$S3_BASE/_LATEST" - --profile saml 2> /dev/null | tr -d '[:space:]')
    if [ -z "$LATEST_RUN_PATH" ] ; then
        echo "`date`: No exported run found at $S3_BASE/_LATEST yet"
        exit $EXIT_NOT_READY
    fi
    RUN_DATE=$(basename "$LATEST_RUN_PATH")

    if s3_exists "$S3_BASE/$RUN_DATE/_DELIVERED" ; then
        echo "`date`: Latest run $RUN_DATE was already delivered, nothing to do"
        exit $EXIT_ALREADY_DELIVERED
    fi
    if ! s3_exists "$S3_BASE/$RUN_DATE/_READY" ; then
        echo "`date`: Latest run $RUN_DATE is not marked _READY yet"
        exit $EXIT_NOT_READY
    fi

    echo "$RUN_DATE" > "$CURRENT_RUN_FILE"
    echo "`date`: Selected run $S3_BASE/$RUN_DATE"
}

function fetch() {
    RUN_DATE=$(current_run_date) || exit 1
    LOCAL_RUN_DIR="$AZ_DELIVERY_HOME/$RUN_DATE"
    authenticate
    rm -rf "$LOCAL_RUN_DIR" && mkdir -p "$LOCAL_RUN_DIR"
    if ! aws s3 sync "$S3_BASE/$RUN_DATE" "$LOCAL_RUN_DIR" --profile saml ; then
        echo "`date`: Failed to download $S3_BASE/$RUN_DATE, exiting..."
        exit 1
    fi
    for required in README.md "$AZ_STUDY_DIR/data_clinical_patient.txt" "$AZ_STUDY_DIR/data_clinical_sample.txt" gene_panels ; do
        if [ ! -e "$LOCAL_RUN_DIR/$required" ] ; then
            echo "`date`: Downloaded run is missing $required, exiting..."
            exit 1
        fi
    done
    echo "`date`: Downloaded $S3_BASE/$RUN_DATE to $LOCAL_RUN_DIR"
}

function add_headers() {
    RUN_DATE=$(current_run_date) || exit 1
    STUDY_PATH="$AZ_DELIVERY_HOME/$RUN_DATE/$AZ_STUDY_DIR"
    # Same call as add_metadata_headers() in update-az-mskimpact.sh
    INPUT_FILENAMES="$STUDY_PATH/data_clinical_sample.txt $STUDY_PATH/data_clinical_patient.txt"
    if ! $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact ; then
        echo "`date`: Failed to add clinical attribute metadata headers, exiting..."
        exit 1
    fi
}

function sftp_upload() {
    RUN_DATE=$(current_run_date) || exit 1
    LOCAL_RUN_DIR="$AZ_DELIVERY_HOME/$RUN_DATE"
    SFTP_USER=$(cat $AZ_SFTP_USER_FILE)
    SERVICE_ENDPOINT=$(cat $AZ_SERVICE_ENDPOINT_FILE)

    # Same transfer as transfer_to_az_sftp_server() in update-az-mskimpact.sh
    # With use of here-doc, there must be no leading whitespace until EOF
    sftp -i "$AZ_SFTP_KEY" -b - "$SFTP_USER"@"$SERVICE_ENDPOINT" <<EOF
put -R "$LOCAL_RUN_DIR/$AZ_STUDY_DIR" "$AZ_SFTP_REMOTE_DIR"
put -R "$LOCAL_RUN_DIR/gene_panels" "$AZ_SFTP_REMOTE_DIR"
put "$LOCAL_RUN_DIR/README.md" "$AZ_SFTP_REMOTE_DIR"
exit
EOF
    if [ $? -gt 0 ] ; then
        echo "`date`: Failed to transfer $RUN_DATE to the AstraZeneca SFTP server, exiting..."
        exit 1
    fi
    echo "`date`: Transferred $RUN_DATE to the AstraZeneca SFTP server"
}

function mark_delivered() {
    RUN_DATE=$(current_run_date) || exit 1
    authenticate
    if ! date -u '+%Y-%m-%dT%H:%M:%SZ' | aws s3 cp - "$S3_BASE/$RUN_DATE/_DELIVERED" --profile saml ; then
        echo "`date`: Failed to write $S3_BASE/$RUN_DATE/_DELIVERED, exiting..."
        exit 1
    fi
    echo "`date`: Marked $S3_BASE/$RUN_DATE delivered"
}

function cleanup() {
    RUN_DATE=$(current_run_date) || exit 1
    if [ -n "$RUN_DATE" ] && [ -d "$AZ_DELIVERY_HOME/$RUN_DATE" ] ; then
        rm -rf "$AZ_DELIVERY_HOME/$RUN_DATE"
    fi
    rm -f "$CURRENT_RUN_FILE"
}

case "$STEP" in
    find-run)       find_run ;;
    fetch)          fetch ;;
    add-headers)    add_headers ;;
    sftp)           sftp_upload ;;
    mark-delivered) mark_delivered ;;
    cleanup)        cleanup ;;
    *)              usage ; exit 1 ;;
esac
