#!/usr/bin/env bash

export CMO_EMAIL_LIST="cbioportal-cmo-importer@cbio.mskcc.org"
export PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbio.mskcc.org"
export ALL_DATA_SOURCES="bic-mskcc private impact impact-MERGED knowledge-systems-curated-studies immunotherapy datahub datahub_shahlab msk-mind-datahub dmp"

unset DATA_SOURCE_NAME_TO_START_LOG_MESSAGE
declare -Ax DATA_SOURCE_NAME_TO_START_LOG_MESSAGE
unset DATA_SOURCE_NAME_TO_EXTRA_IMPORTER_ARGS
declare -Ax DATA_SOURCE_NAME_TO_EXTRA_IMPORTER_ARGS
unset DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE
declare -Ax DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE
unset DATA_SOURCE_NAME_TO_EMAIL_BODY
declare -Ax DATA_SOURCE_NAME_TO_EMAIL_BODY
unset DATA_SOURCE_NAME_TO_EMAIL_SUBJECT
declare -Ax DATA_SOURCE_NAME_TO_EMAIL_SUBJECT
unset DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT
declare -Ax DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT
#set default strings for fetches
for data_source in ${ALL_DATA_SOURCES[*]} ; do
    DATA_SOURCE_NAME_TO_START_LOG_MESSAGE[$data_source]="fetching updates from ${data_source}..."
    DATA_SOURCE_NAME_TO_EXTRA_IMPORTER_ARGS[$data_source]=""
    DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE[$data_source]="$data_source fetch failed!"
    DATA_SOURCE_NAME_TO_EMAIL_BODY[$data_source]="The $data_source data fetch failed."
    DATA_SOURCE_NAME_TO_EMAIL_SUBJECT[$data_source]="Data fetch failure: $data_source"
    DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT[$data_source]="$PIPELINES_EMAIL_LIST"
done
#set exceptions for fetches
for data_source in "bic-mskcc" ; do
    DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE[$data_source]="CMO ($data_source) fetch failed!"
    DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT[$data_source]="$CMO_EMAIL_LIST"
    DATA_SOURCE_NAME_TO_EMAIL_BODY[$data_source]="The CMO ($data_source) data fetch failed. Imports into Triage and production WILL NOT HAVE UP-TO-DATE DATA until this is resolved.\n\n*** DO NOT MARK STUDIES FOR IMPORT INTO msk-automation-portal. ***\n\n*** DO NOT MERGE ANY STUDIES until this has been resolved. Please uncheck any merged studies in the cBio Portal Google document. ***\n\nYou may keep projects marked for import into Triage in the cBio Portal Google document. Triage studies will be reimported once there has been a successful data fetch.\n\nPlease don't hesitate to ask if you have any questions."
    DATA_SOURCE_NAME_TO_EMAIL_SUBJECT[$data_source]="Data fetch failure: CMO ($data_source)"
done
DATA_SOURCE_NAME_TO_START_LOG_MESSAGE[knowledge-systems-curated-studies]="fetching updates from cbio-portal-data..."
DATA_SOURCE_NAME_TO_EXTRA_IMPORTER_ARGS[bic-mskcc]="--update-worksheet"
DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE[knowledge-systems-curated-studies]="cbio-portal-data fetch failed!"
DATA_SOURCE_NAME_TO_EMAIL_BODY[knowledge-systems-curated-studies]="The cbio-portal-data data fetch failed."
DATA_SOURCE_NAME_TO_EMAIL_SUBJECT[knowledge-systems-curated-studies]="Data fetch failure: cbio-portal-data"

function fetch_updates_in_data_source {
    data_source=$1
    start_log_msg=$2
    extra_importer_args=$3
    failure_log_msg=$4
    email_body=$5
    email_subject=$6
    email_recipient=$7

    echo $start_log_msg
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source $data_source --run-date latest $extra_importer_args
    if [ $? -ne 0 ]; then
        echo $failure_log_msg
        echo -e "Sending email $email_body"
        echo -e "$email_body" | mail -s "$email_subject" $email_recipient
        return 1
    fi
    return 0
}

# failed_data_source_fetches will contain a list of all data source fetches which failed after calling fetch_updates_in_data_sources()
unset failed_data_source_fetches
declare -ax failed_data_source_fetches

function fetch_updates_in_data_sources {
    failed_data_source_fetches=()
    data_source_list=$*
    # fetch updates in data sources
    for data_source in $data_source_list ; do
        #check if there is a typo in the data source name, and log / skip
        if [ -z ${DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT[$data_source]} ] ; then
            echo "error: asked to fetch unrecognized data source : $data_source"
            failed_data_source_fetches+=("$data_source")
            continue
        fi
        fetch_updates_in_data_source \
                $data_source \
                "${DATA_SOURCE_NAME_TO_START_LOG_MESSAGE[$data_source]}" \
                "${DATA_SOURCE_NAME_TO_EXTRA_IMPORTER_ARGS[$data_source]}" \
                "${DATA_SOURCE_NAME_TO_FAILURE_LOG_MESSAGE[$data_source]}" \
                "${DATA_SOURCE_NAME_TO_EMAIL_BODY[$data_source]}" \
                "${DATA_SOURCE_NAME_TO_EMAIL_SUBJECT[$data_source]}" \
                "${DATA_SOURCE_NAME_TO_EMAIL_RECIPIENT[$data_source]}"
        if [ $? -ne 0 ] ; then
            failed_data_source_fetches+=("$data_source")
        fi
    done
}

export -f fetch_updates_in_data_source
export -f fetch_updates_in_data_sources
