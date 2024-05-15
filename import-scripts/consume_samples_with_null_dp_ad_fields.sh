#!/usr/bin/env bash

TMP_DIR="/data/portal-cron/tmp/consume_null_dp_ad_samples"
CVR_FETCH_PROPERTIES_FILEPATH="/data/portal-cron/git-repos/pipelines-configuration/properties/fetch-cvr/application.properties"
CVR_USERNAME=$(grep 'dmp.user_name' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_PASSWORD=$(grep 'dmp.password' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_TUMOR_SERVER=$(grep 'dmp.server_name' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_CREATE_SESSION_URL="${CVR_TUMOR_SERVER}create_session/${CVR_USERNAME}/${CVR_PASSWORD}/0"
CVR_IMPACT_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_variants"
CVR_HEME_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_heme_variants"
CVR_ACCESS_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_access_variants"
CVR_CONSUME_SAMPLE_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_consume_sample"
IMPACT_FETCH_OUTPUT_FILEPATH="$TMP_DIR/cvr_data_impact.json"
HEME_FETCH_OUTPUT_FILEPATH="$TMP_DIR/cvr_data_heme.json"
ACCESS_FETCH_OUTPUT_FILEPATH="$TMP_DIR/cvr_data_access.json"
IMPACT_CONSUME_IDS_FILEPATH="$TMP_DIR/impact_consume.ids"
HEME_CONSUME_IDS_FILEPATH="$TMP_DIR/heme_consume.ids"
ACCESS_CONSUME_IDS_FILEPATH="$TMP_DIR/access_consume.ids"
COMBINED_CONSUME_IDS_FILEPATH="$TMP_DIR/combined_consume.ids"
CONSUME_ATTEMPT_OUTPUT_FILEPATH="$TMP_DIR/consume_attempt_output.json"
DETECT_SAMPLES_TO_CONSUME_SCRIPT_FILEPATH=/data/portal-cron/scripts/detect_samples_with_null_dp_ad_fields.py

source "$PORTAL_HOME/scripts/slack-message-functions.sh"

function make_tmp_dir_if_necessary() {
    if ! [ -d "$TMP_DIR" ] ; then
        if ! mkdir -p "$TMP_DIR" ; then
            echo "Error : could not create tmp directory '$TMP_DIR'" >&2
            exit 1
        fi
    fi
}

function fetch_currently_queued_samples() {
    dmp_token=$(curl $CVR_CREATE_SESSION_URL | grep session_id | sed -E 's/",[[:space:]]*$//' | sed -E 's/.*"//')
    curl "${CVR_IMPACT_FETCH_URL_PREFIX}/${dmp_token}/0" > ${IMPACT_FETCH_OUTPUT_FILEPATH}
    curl "${CVR_HEME_FETCH_URL_PREFIX}/${dmp_token}/0" > ${HEME_FETCH_OUTPUT_FILEPATH}
    curl "${CVR_ACCESS_FETCH_URL_PREFIX}/${dmp_token}/0" > ${ACCESS_FETCH_OUTPUT_FILEPATH}
}

function detect_samples_with_malformed_events() {
    $DETECT_SAMPLES_TO_CONSUME_SCRIPT_FILEPATH ${IMPACT_FETCH_OUTPUT_FILEPATH} ${IMPACT_CONSUME_IDS_FILEPATH}
    $DETECT_SAMPLES_TO_CONSUME_SCRIPT_FILEPATH ${HEME_FETCH_OUTPUT_FILEPATH} ${HEME_CONSUME_IDS_FILEPATH}
    $DETECT_SAMPLES_TO_CONSUME_SCRIPT_FILEPATH ${ACCESS_FETCH_OUTPUT_FILEPATH} ${ACCESS_CONSUME_IDS_FILEPATH}
    cat ${IMPACT_CONSUME_IDS_FILEPATH} ${HEME_CONSUME_IDS_FILEPATH} ${ACCESS_CONSUME_IDS_FILEPATH} > ${COMBINED_CONSUME_IDS_FILEPATH}
}

function exit_if_no_malformed_events_detected() {
    if [ ! -s ${COMBINED_CONSUME_IDS_FILEPATH} ] ; then
        echo "no problematic samples detected .. exiting"
        exit 0
    fi
}

function attempt_to_consume_samples() {
    dmp_token=$(curl $CVR_CREATE_SESSION_URL | grep session_id | sed -E 's/",[[:space:]]*$//' | sed -E 's/.*"//')
    while read sample_id ; do
        HTTP_STATUS=$(curl -sSL -w '%{http_code}' -o "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" "${CVR_CONSUME_SAMPLE_URL_PREFIX}/${dmp_token}/${sample_id}")
        if [[ $HTTP_STATUS =~ ^2 ]] ; then
            if ! grep '"error": "' "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" ; then
                if grep --silent 'affectedRows": 1' "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" ; then
                    succeeded_to_consume_sample_list+="${sample_id}\n"
                    continue
                fi
            fi
        fi
        failed_to_consume_sample_list+="${sample_id}\n"
    done < ${COMBINED_CONSUME_IDS_FILEPATH}
}

function consume_hardcoded_samples() {
    rm -f "${COMBINED_CONSUME_IDS_FILEPATH}"
    echo "P-0025907-N01-IM6" > "${COMBINED_CONSUME_IDS_FILEPATH}"
    if [ -f "${COMBINED_CONSUME_IDS_FILEPATH}" ] ; then
        attempt_to_consume_samples
    fi
}

function log_actions() {
    date
    echo -e "consumed the following samples with malformed events:\n${succeeded_to_consume_sample_list[*]}"
    echo -e "attempted but failed to consume the following samples with malformed events:\n${failed_to_consume_sample_list[*]}"
    echo
}

function post_slack_message() {
    MESSAGE="<@U02D0Q0RWUE> <@U03FERRJ6SE> Warning : the following samples have been preemptively consumed before fetch because they contained events which required a value for fields {normal_dp, normal_ad, tumor_dp, tumor_ad} but contained no value in one or more of these fields.\nSuccessfully Consumed :\n${succeeded_to_consume_sample_list[*]}"
    if [ ${#failed_to_consume_sample_list[@]} -gt 0 ]; then
        MESSAGE="${MESSAGE} Attempted Unsuccessfully To Consume :\n${failed_to_consume_sample_list[*]}"
    fi
    send_slack_message_to_channel "#msk-cvr" "blocks" "$MESSAGE"
}

date
make_tmp_dir_if_necessary
failed_to_consume_sample_list=() # temporary code
succeeded_to_consume_sample_list=() # temporary code
consume_hardcoded_samples # temporary code
fetch_currently_queued_samples
detect_samples_with_malformed_events
exit_if_no_malformed_events_detected
failed_to_consume_sample_list=()
succeeded_to_consume_sample_list=()
attempt_to_consume_samples
log_actions
post_slack_message
