#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : s3_functions.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

# Syncs a directory to an s3 bucket
# Files that have been removed in the local directory will be removed in the bucket
function upload_to_s3() {
    DIR_TO_UPLOAD="$1"
    DIR_NAME_IN_S3="$2"
    BUCKET_NAME="$3"

    # Check if the directory exists
    if [ ! -d "$DIR_TO_UPLOAD" ]; then
        echo "`date`: Directory '$DIR_TO_UPLOAD' does not exist, exiting..."
        exit 1
    fi

    # Authenticate and upload into S3 bucket
    $PORTAL_HOME/scripts/authenticate_service_account.sh eks
    aws s3 sync $DIR_TO_UPLOAD s3://$BUCKET_NAME/$DIR_NAME_IN_S3 --delete --profile saml
    if [ $? -ne 0 ] ; then
        echo "`date`: Failed to upload '$DIR_TO_UPLOAD' to S3, exiting..."
        exit 1
    fi
}
