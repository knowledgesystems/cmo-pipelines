#!/usr/bin/env bash

COHORT=$1
TMP_DIR="/data/portal-cron/tmp/preconsume_problematic_samples"
CVR_FETCH_PROPERTIES_FILEPATH="/data/portal-cron/git-repos/pipelines-configuration/properties/fetch-cvr/application.properties"
CVR_USERNAME=$(grep 'dmp.user_name' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_PASSWORD=$(grep 'dmp.password' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_TUMOR_SERVER=$(grep 'dmp.server_name' ${CVR_FETCH_PROPERTIES_FILEPATH} | head -n 1 | sed -E s/[^=][^=]*=//)
CVR_CREATE_SESSION_URL="${CVR_TUMOR_SERVER}create_session/${CVR_USERNAME}/${CVR_PASSWORD}/0"
CVR_FETCH_URL_PREFIX=""
CVR_IMPACT_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_variants"
CVR_HEME_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_heme_variants"
CVR_ARCHER_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_archer_retrieve_variants"
CVR_ACCESS_FETCH_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_retrieve_access_variants"
CVR_CONSUME_SAMPLE_URL_PREFIX="${CVR_TUMOR_SERVER}cbio_consume_sample"
FETCH_OUTPUT_FILEPATH="$TMP_DIR/cvr_data_${COHORT}.json"
CONSUME_IDS_FILEPATH="$TMP_DIR/${COHORT}_consume.ids"
PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH="$TMP_DIR/problematic_event_consume_${COHORT}.ids"
PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH="$TMP_DIR/problematic_metadata_consume_${COHORT}.ids"
CONSUME_ATTEMPT_OUTPUT_FILEPATH="$TMP_DIR/consume_attempt_output_${COHORT}.json"
DETECT_SAMPLES_WITH_NULL_DP_AD_FIELDS_SCRIPT_FILEPATH=/data/portal-cron/scripts/detect_samples_with_null_dp_ad_fields.py
DETECT_SAMPLES_WITH_PROBLEMATIC_METADATA_SCRIPT_FILEPATH=/data/portal-cron/scripts/detect_samples_with_problematic_metadata.py
CVR_MONITOR_SLACK_URI_FILE="/data/portal-cron/pipelines-credentials/cvr-monitor-webhook-uri"
CVR_MONITOR_URI=$(cat "$CVR_MONITOR_SLACK_URI_FILE")

function make_tmp_dir_if_necessary() {
    if ! [ -d "$TMP_DIR" ] ; then
        if ! mkdir -p "$TMP_DIR" ; then
            echo "Error : could not create tmp directory '$TMP_DIR'" >&2
            exit 1
        fi
    fi
}

function check_args() {
    if [[ -z $COHORT ]] || [[ "$COHORT" != "mskimpact" && "$COHORT" != "mskimpact_heme" && "$COHORT" != "mskaccess" && "$COHORT" != "mskarcher" ]]; then
        usage
        exit 1
    fi
}

function usage {
    echo "preconsume_problematic_samples.sh \$COHORT_ID"
    echo -e "\t\$COHORT_ID                      one of: ['mskimpact', 'mskimpact_heme', 'mskaccess', 'mskarcher']"
}

function set_cvr_fetch_url_prefix() {
    if [ "$COHORT" == "mskimpact" ] ; then
        CVR_FETCH_URL_PREFIX=$CVR_IMPACT_FETCH_URL_PREFIX
    elif [ "$COHORT" == "mskimpact_heme" ] ; then
        CVR_FETCH_URL_PREFIX=$CVR_HEME_FETCH_URL_PREFIX
    elif [ "$COHORT" == "mskarcher" ] ; then
        CVR_FETCH_URL_PREFIX=$CVR_ARCHER_FETCH_URL_PREFIX
    elif [ "$COHORT" == "mskaccess" ] ; then
        CVR_FETCH_URL_PREFIX=$CVR_ACCESS_FETCH_URL_PREFIX
    fi
}

function fetch_currently_queued_samples() {
    dmp_token=$(curl $CVR_CREATE_SESSION_URL | grep session_id | sed -E 's/",[[:space:]]*$//' | sed -E 's/.*"//')
    curl "${CVR_FETCH_URL_PREFIX}/${dmp_token}/0" > ${FETCH_OUTPUT_FILEPATH}
}

function detect_samples_with_problematic_events() {
    $DETECT_SAMPLES_WITH_NULL_DP_AD_FIELDS_SCRIPT_FILEPATH ${FETCH_OUTPUT_FILEPATH} ${PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH}
}

function detect_samples_with_problematic_metadata() {
    $DETECT_SAMPLES_WITH_PROBLEMATIC_METADATA_SCRIPT_FILEPATH ${FETCH_OUTPUT_FILEPATH} ${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}
}

function exit_if_no_problems_detected() {
    if [ ! -s ${PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH} ] && [ ! -s ${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH} ] ; then
        echo "no problematic samples detected .. exiting"
        exit 0
    fi
}

function register_successful_consumption() {
    sample_id="$1"
    type_of_problem="$2"
    if [ "$type_of_problem" == "e" ] ; then
        succeeded_to_consume_problematic_events_sample_list+="${sample_id}\n"
    else
        if [ "$type_of_problem" == "m" ] ; then
            succeeded_to_consume_problematic_metadata_sample_list+="${sample_id}\n"
        else
            echo "error : type_of_problem flag was neither 'e' (event problem) or 'm' (metadata problem) .. unable to report and skipping"
        fi
    fi
}

function register_failed_consumption() {
    sample_id="$1"
    type_of_problem="$2"
    if [ "$type_of_problem" == "e" ] ; then
        failed_to_consume_problematic_events_sample_list+="${sample_id}\n"
    else
        if [ "$type_of_problem" == "m" ] ; then
            failed_to_consume_problematic_metadata_sample_list+="${sample_id}\n"
        else
            echo "error : type_of_problem flag was neither 'e' (event problem) or 'm' (metadata problem) .. unable to report and skipping"
        fi
    fi
}

function attempt_to_consume_problematic_sample() {
    dmp_token="$1"
    sample_id="$2"
    type_of_problem="$3" # pass 'e' for event problems and 'm' for metadata problems
    HTTP_STATUS=$(curl -sSL -w '%{http_code}' -o "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" "${CVR_CONSUME_SAMPLE_URL_PREFIX}/${dmp_token}/${sample_id}")
    if [[ $HTTP_STATUS =~ ^2 ]] ; then
        if ! grep '"error": "' "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" ; then
            if grep --silent 'affectedRows": 1' "$CONSUME_ATTEMPT_OUTPUT_FILEPATH" ; then
                register_successful_consumption "${sample_id}" "$type_of_problem"
                continue
            fi
        fi
    fi
    register_failed_consumption "${sample_id}" "$type_of_problem"
}

function attempt_to_consume_problematic_samples() {
    dmp_token=$(curl $CVR_CREATE_SESSION_URL | grep session_id | sed -E 's/",[[:space:]]*$//' | sed -E 's/.*"//')
    while read sample_id ; do
        attempt_to_consume_problematic_sample "$dmp_token" "$sample_id" "e"
    done < ${PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH}
    while read sample_id ; do
        attempt_to_consume_problematic_sample "$dmp_token" "$sample_id" "m"
    done < ${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}
}

function consume_hardcoded_samples() {
    rm -f ${PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH} ${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}
    touch ${PROBLEMATIC_EVENT_CONSUME_IDS_FILEPATH}
    touch ${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}

    if [ "$COHORT" == "mskimpact" ] ; then
        echo "P-0025907-N01-IM6" >> "${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}"
    fi
    if [ -f "${PROBLEMATIC_METADATA_CONSUME_IDS_FILEPATH}" ] ; then
        attempt_to_consume_problematic_samples
    fi
}

function log_actions() {
    date
    echo -e "${COHORT^^} Problematic Samples"
    echo -e "consumed the following samples with problematic events:\n${succeeded_to_consume_problematic_events_sample_list[*]}"
    echo -e "attempted but failed to consume the following samples with problematic events:\n${failed_to_consume_problematic_events_sample_list[*]}"
    echo -e "consumed the following samples with problematic metadata:\n${succeeded_to_consume_problematic_metadata_sample_list[*]}"
    echo -e "attempted but failed to consume the following samples with problematic metadata:\n${failed_to_consume_problematic_metadata_sample_list[*]}"
    echo
}

function post_slack_message() {
    MESSAGE="<@U02D0Q0RWUE> <@U03FERRJ6SE> ${COHORT^^} Problematic Samples \nWarning : \nThe following samples have been preemptively consumed before fetch because they contained events which required a value for fields {normal_dp, normal_ad, tumor_dp, tumor_ad} but contained no value in one or more of these fields.\nSuccessfully Consumed :\n${succeeded_to_consume_problematic_events_sample_list[*]}"
    if [ ${#failed_to_consume_problematic_events_sample_list[@]} -gt 0 ]; then
        MESSAGE="${MESSAGE} Attempted Unsuccessfully To Consume :\n${failed_to_consume_problematic_events_sample_list[*]}"
    fi
    MESSAGE="${MESSAGE}Warning : the following samples have been preemptively consumed before fetch because they contained problematic metadata where the gene-panel property was unset or had value UNKNOWN.\nSuccessfully Consumed :\n${succeeded_to_consume_problematic_metadata_sample_list[*]}"
    if [ ${#failed_to_consume_problematic_metadata_sample_list[@]} -gt 0 ]; then
        MESSAGE="${MESSAGE} Attempted Unsuccessfully To Consume :\n${failed_to_consume_problematic_metadata_sample_list[*]}"
    fi
    curl -X POST -H 'Content-type: application/json' --data '{"blocks": [ { "type": "section", "text": { "type": "mrkdwn", "text": "'"$MESSAGE"'" } } ] }' "$CVR_MONITOR_URI"
}

date
check_args
make_tmp_dir_if_necessary
failed_to_consume_problematic_events_sample_list=() # temporary code
succeeded_to_consume_problematic_events_sample_list=() # temporary code
failed_to_consume_problematic_metadata_sample_list=() # temporary code
succeeded_to_consume_problematic_metadata_sample_list=() # temporary code
set_cvr_fetch_url_prefix
consume_hardcoded_samples # temporary code
fetch_currently_queued_samples
detect_samples_with_problematic_events
detect_samples_with_problematic_metadata
exit_if_no_problems_detected
failed_to_consume_problematic_events_sample_list=()
succeeded_to_consume_problematic_events_sample_list=()
failed_to_consume_problematic_metadata_sample_list=()
succeeded_to_consume_problematic_metadata_sample_list=()
attempt_to_consume_problematic_samples
log_actions
post_slack_message
