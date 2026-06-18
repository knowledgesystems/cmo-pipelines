#!/bin/bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : s3_functions.sh cannot be run without setting the PORTAL_HOME environment variable."
    exit 1
fi

# Uploads a file or directory to an S3 bucket (from local to S3).
# If a directory is provided, files removed locally will be deleted from S3.
# Returns 0 on success, 1 on failure (does not exit the shell).
#
# Args:
#   $1 PATH_TO_UPLOAD  Local file or directory to upload (must exist).
#   $2 PATH_IN_S3      S3 key prefix within the bucket (no s3:// prefix). Must match
#                      the trailing path of PATH_TO_UPLOAD, unless empty to sync the
#                      bucket root (e.g. upload_to_s3 "$DMP_DATA_HOME" "" "mskimpact-databricks").
#   $3 BUCKET_NAME     S3 bucket name (e.g. mskimpact-databricks).
function try_upload_to_s3() {
    PATH_TO_UPLOAD="$1"
    PATH_IN_S3="$2"
    BUCKET_NAME="$3"

    # Check if path exists
    if [ ! -e "$PATH_TO_UPLOAD" ]; then
        echo "`date`: Path '$PATH_TO_UPLOAD' does not exist, exiting..."
        return 1
    fi

    # Normalize paths
    PATH_TO_UPLOAD_ABS=$(realpath "$PATH_TO_UPLOAD")
    PATH_IN_S3_CLEAN=$(echo "$PATH_IN_S3" | sed 's|^/*||')  # strip leading slash

    # Enforce that PATH_TO_UPLOAD ends with PATH_IN_S3
    # Unless PATH_IN_S3_CLEAN is empty which means we sync the bucket
    if [ -n "$PATH_IN_S3_CLEAN" ]; then
        if [[ "$PATH_TO_UPLOAD_ABS" != *"/$PATH_IN_S3_CLEAN" && "$PATH_TO_UPLOAD_ABS" != "$PATH_IN_S3_CLEAN" ]]; then
            echo "`date`: ERROR – PATH_IN_S3 ('$PATH_IN_S3') must exactly match the trailing path of '$PATH_TO_UPLOAD_ABS'. Exiting..."
            return 1
        fi
    fi

    # Authenticate
    $PORTAL_HOME/scripts/authenticate_service_account.sh eks

    if [ -f "$PATH_TO_UPLOAD" ]; then
        FILE_NAME=$(basename "$PATH_TO_UPLOAD")
        SOURCE_DIR=$(dirname "$PATH_TO_UPLOAD_ABS")
        TARGET_S3_DIR=$(dirname "$PATH_IN_S3_CLEAN")

        aws s3 sync "$SOURCE_DIR/" "s3://$BUCKET_NAME/$TARGET_S3_DIR/" \
            --exclude "*" --include "$FILE_NAME" --profile saml

    elif [ -d "$PATH_TO_UPLOAD" ]; then
        aws s3 sync "$PATH_TO_UPLOAD" "s3://$BUCKET_NAME/$PATH_IN_S3_CLEAN" \
            --delete \
            --exclude "*.log" \
            --exclude "*.jfr" \
            --exclude "repository.sqlite" \
            --exclude ".git/*" \
            --exclude ".gitattributes" \
            --exclude ".gitignore" \
            --exclude "*parquet*" \
            --profile saml
    else
        echo "`date`: '$PATH_TO_UPLOAD' is neither a file nor a directory, exiting..."
        return 1
    fi

    if [ $? -ne 0 ]; then
        echo "`date`: Failed to upload '$PATH_TO_UPLOAD' to S3, exiting..."
        return 1
    fi
}

# Uploads a file or directory to an S3 bucket (from local to S3).
# If a directory is provided, files removed locally will be deleted from S3.
# Exits 1 on failure.
#
# Args:
#   $1 PATH_TO_UPLOAD  Local file or directory to upload (must exist).
#   $2 PATH_IN_S3      S3 key prefix within the bucket (no s3:// prefix). Must match
#                      the trailing path of PATH_TO_UPLOAD, unless empty to sync the
#                      bucket root (e.g. upload_to_s3 "$DMP_DATA_HOME" "" "mskimpact-databricks").
#   $3 BUCKET_NAME     S3 bucket name (e.g. mskimpact-databricks).
function upload_to_s3() {
    if ! try_upload_to_s3 "$1" "$2" "$3"; then
        exit 1
    fi
}

# Syncs a file or directory from S3 to a local path.
# Files not in S3 will be removed from the local destination (for directories).
# Returns 0 on success, 1 on failure (does not exit the shell).
#
# Args:
#   $1 PATH_TO_OVERWRITE  Local file or directory to sync into (must already exist; cannot be "/").
#   $2 PATH_IN_S3         S3 key prefix within the bucket (no s3:// prefix). Must match
#                         the trailing path of PATH_TO_OVERWRITE, unless empty to sync the
#                         bucket root (e.g. download_from_s3 "$DMP_DATA_HOME" "" "mskimpact-databricks").
#   $3 BUCKET_NAME        S3 bucket name (e.g. mskimpact-databricks).
function try_download_from_s3() {
    PATH_TO_OVERWRITE="$1"
    PATH_IN_S3="$2"
    BUCKET_NAME="$3"

    # Normalize local path
    if [ -z "$PATH_TO_OVERWRITE" ]; then
        echo "`date`: PATH_TO_OVERWRITE is empty, exiting..."
        return 1
    fi

    PATH_TO_OVERWRITE_ABS=$(realpath "$PATH_TO_OVERWRITE" 2>/dev/null)
    if [ $? -ne 0 ]; then
        echo "`date`: '$PATH_TO_OVERWRITE' does not exist, exiting..."
        return 1
    fi

    if [ "$PATH_TO_OVERWRITE_ABS" == "/" ]; then
        echo "`date`: Refusing to sync to root directory '/', exiting..."
        return 1
    fi

    # Clean PATH_IN_S3
    PATH_IN_S3_CLEAN=$(echo "$PATH_IN_S3" | sed 's|^/*||')

    # Enforce suffix match if PATH_IN_S3 is non-empty
    if [ -n "$PATH_IN_S3_CLEAN" ]; then
        if [[ "$PATH_TO_OVERWRITE_ABS" != *"/$PATH_IN_S3_CLEAN" && "$PATH_TO_OVERWRITE_ABS" != "$PATH_IN_S3_CLEAN" ]]; then
            echo "`date`: ERROR – PATH_IN_S3 ('$PATH_IN_S3') must exactly match the trailing path of '$PATH_TO_OVERWRITE_ABS'. Exiting..."
            return 1
        fi
    fi

    # Authenticate
    $PORTAL_HOME/scripts/authenticate_service_account.sh eks

    if [ -d "$PATH_TO_OVERWRITE_ABS" ]; then
        # Downloading a directory
        S3_SOURCE="s3://$BUCKET_NAME/$PATH_IN_S3_CLEAN"
        aws s3 sync "$S3_SOURCE" "$PATH_TO_OVERWRITE_ABS" \
            --delete \
            --exclude "*.log" \
            --exclude "*.jfr" \
            --exclude "repository.sqlite" \
            --exclude ".git/*" \
            --exclude ".gitattributes" \
            --exclude ".gitignore" \
            --exclude "*parquet*" \
            --profile saml

    elif [ -f "$PATH_TO_OVERWRITE_ABS" ]; then
        # Downloading a single file
        FILE_NAME=$(basename "$PATH_TO_OVERWRITE_ABS")
        LOCAL_DIR=$(dirname "$PATH_TO_OVERWRITE_ABS")
        S3_SOURCE="s3://$BUCKET_NAME/$(dirname "$PATH_IN_S3_CLEAN")"

        aws s3 sync "$S3_SOURCE" "$LOCAL_DIR" \
            --exclude "*" --include "$FILE_NAME" --profile saml

    else
        echo "`date`: '$PATH_TO_OVERWRITE_ABS' is neither a file nor a directory, exiting..."
        return 1
    fi

    if [ $? -ne 0 ]; then
        echo "`date`: Failed to download from '$S3_SOURCE', exiting..."
        return 1
    fi
}

# Syncs a file or directory from S3 to a local path.
# Files not in S3 will be removed from the local destination (for directories).
# Exits 1 on failure.
#
# Args:
#   $1 PATH_TO_OVERWRITE  Local file or directory to sync into (must already exist; cannot be "/").
#   $2 PATH_IN_S3         S3 key prefix within the bucket (no s3:// prefix). Must match
#                         the trailing path of PATH_TO_OVERWRITE, unless empty to sync the
#                         bucket root (e.g. download_from_s3 "$DMP_DATA_HOME" "" "mskimpact-databricks").
#   $3 BUCKET_NAME        S3 bucket name (e.g. mskimpact-databricks).
function download_from_s3() {
    if ! try_download_from_s3 "$1" "$2" "$3"; then
        exit 1
    fi
}
