#!/usr/bin/env bash
EMAIL_LIST="cbioportal-pipelines@cbio.mskcc.org"
MAX_ATTEMPTS=5

function usage {
    echo "refresh-cdd-oncotree-cache.sh"
    echo -e "\t-h | --help                          prints usage statement and exits"
    echo -e "\t-c | --cdd-only                      refresh CDD cache only [cannot be used with --oncotree-only]"
    echo -e "\t-o | --oncotree-only                 refresh Oncotree cache only [cannot be used with --cdd-only]"
    exit 2
}

# set default value(s)
REFRESH_SERVICE="ALL"
CDD_ONLY=0
ONCOTREE_ONLY=0

for i in "$@"; do
case $i in
    -c|--cdd-only)
    CDD_ONLY=1
    REFRESH_SERVICE="CDD"
    shift
    ;;
    -o|--oncotree-only)
    ONCOTREE_ONLY=1
    REFRESH_SERVICE="ONCOTREE"
    shift
    ;;
    -h|--help)
        usage
    shift
    ;;
    *)
    ;;
esac
done

# validate input arguments
if [[ $CDD_ONLY -eq 1 && $ONCOTREE_ONLY -eq 1 ]]; then
    echo -e "ERROR: either no arguments or only one argument ('--cdd-only' or '--oncotree-only') can be passed!"
    usage
fi

## FUNCTIONS

# Function for alerting pipelines team via slack and email
function sendFailureMessageSlackEmail {
    MESSAGE_BODY=$1
    SUBJECT_MESSAGE=$2
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines recache process status: $MESSAGE_BODY\", \"icon_emoji\": \":fire:\"}" https://hooks.slack.com/services/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
    echo -e "$MESSAGE_BODY" | mail -s "$SUBJECT_MESSAGE" $EMAIL_LIST
}

# Function for alerting pipelines team via slack and email
function sendSuccessMessageSlackEmail {
    MESSAGE_BODY=$1
    SUBJECT_MESSAGE=$2
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines recache process status: $MESSAGE_BODY\", \"icon_emoji\": \":arrows_counterclockwise:\"}" https://hooks.slack.com/services/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
    echo -e "$MESSAGE_BODY" | mail -s "$SUBJECT_MESSAGE" $EMAIL_LIST
}

function checkServerHttpStatusCode {
    # CHECK SERVER STATUS
    # (1): SERVICE_NAME:    name of service [CDD, ONCOTREE]
    # (2): SERVER URL:      server url
    SERVICE_NAME=$1
    SERVER_URL=$2

    echo -e "\nChecking $SERVICE_NAME server HTTP status..."
    HTTP_STATUS_CODE=$(curl -s -o /dev/null -I -w "%{http_code}" $SERVER_URL/)
    # if empty string then set to default value (0)
    if [ -z "$HTTP_STATUS_CODE" ] ; then
        echo -e "\nERROR, checkServerHttpStatusCode(): Error while executing HTTP status check curl command. Setting status code to default value (0)..."
        HTTP_STATUS_CODE=0
    fi
    if [ $HTTP_STATUS_CODE -ne 200 ] ; then
        # send alert for service server status if not 200
        sendFailureMessageSlackEmail "$SERVICE_NAME server HTTP status = $HTTP_STATUS_CODE. Server might be down - please check ASAP." "$SERVICE_NAME server status issue"
    fi
    return $HTTP_STATUS_CODE
}

function checkServerEndpointResponse {
    # CHECK SERVER ENDPOINT RESPONSE
    # (1): SERVICE_NAME:  name of service [CDD, ONCOTREE]
    # (2): ENDPOINT:      endpoint to use
    # (3): SERVER_LIST:   list of server names as array
    SERVICE_NAME=$1
    ENDPOINT=$2
    server_args=("$@") # extract server list from server args
    SERVER_LIST="${server_args[@]:2}"

    return_value=1
    for server in ${SERVER_LIST[@]} ; do
        # check server http status code
        checkServerHttpStatusCode $SERVICE_NAME $server ; http_status_code=$?

        # if server is up then check endpoint response
        if [ $http_status_code -eq 200 ] ; then
            # check endpoint response
            URL="$server/$ENDPOINT"
            echo -e "\nChecking $SERVICE_NAME endpoint $ENDPOINT ($URL)..."
            curl --fail -X GET --header 'Accept: */*' $URL ; endpoint_response=$?
            if [ $endpoint_response -eq 0 ] ; then
                # at least one server and endpoint response succeeded, update return_value
                return_value=0
            fi
        fi
    done
    return $return_value
}

function refreshCache {
    # REFRESH CACHE FOR A GIVEN SERVICE
    # (1): SERVICE_NAME:  name of service [CDD, ONCOTREE]
    # (2): REATTEMPT:     number of reattempts to refresh cache if failure occurs
    # (3): ENDPOINT:      endpoint to use
    # (4): SERVER_LIST:   list of server names as array
    SERVICE_NAME=$1
    REATTEMPT=$2
    ENDPOINT=$3
    server_args=("$@") # extract server list from server args
    SERVER_LIST="${server_args[@]:3}"

    checkServerEndpointResponse $SERVICE_NAME $ENDPOINT ${SERVER_LIST[@]}; return_value=$?

    if [ $return_value -eq 1 ]; then
        if [ $REATTEMPT -gt 0 ] ; then
            # decrement number of reattempts left and wait 5 seconds before next attempt
            REATTEMPTS_LEFT=$(($REATTEMPT - 1))
            CURRENT_ATTEMPT=$(($MAX_ATTEMPTS - $REATTEMPTS_LEFT))

            echo -e "\nReattempting to refresh $SERVICE_NAME cache (reattempt # $CURRENT_ATTEMPT of $MAX_ATTEMPTS)..."
            sleep 5; refreshCache "$SERVICE_NAME" $REATTEMPTS_LEFT $ENDPOINT ${SERVER_LIST[@]}; return_value=$?
        else
            FAILURE_MESSAGE="Failed to refresh $SERVICE_NAME cache:\n\tendpoint=$ENDPOINT\n\tservers=${SERVER_LIST[@]}"
            SUBJECT_MESSAGE="Failed to refresh $SERVICE_NAME cache"
            sendFailureMessageSlackEmail "$FAILURE_MESSAGE" "$SUBJECT_MESSAGE"
        fi
    else
        echo -e "\n$SERVICE_NAME cache refresh successful!"
    fi
    return $return_value
}

function checkForValidStaleCache {
    # CHECK SERVERS FOR VALID STALE CACHE
    # REFRESH CACHE FOR A GIVEN SERVICE
    # (1): SERVICE_NAME:  name of service [CDD, ONCOTREE]
    # (2): ENDPOINT:      endpoint to use
    # (3): SERVER_LIST:   list of server names as array
    SERVICE_NAME=$1
    ENDPOINT=$2
    server_args=("$@") # extract server list from server args
    SERVER_LIST="${server_args[@]:2}"

    checkServerEndpointResponse $SERVICE_NAME $ENDPOINT ${SERVER_LIST[@]}; return_value=$?

    if [ $return_value -eq 1 ] ; then
        FAILURE_MESSAGE="Failed to fall back on stale cache for $SERVICE_NAME:\n\tendpoint=$ENDPOINT\n\tservers=${SERVER_LIST[@]}"
        FAILURE_MESSAGE="$FAILURE_MESSAGE\n$SERVICE_NAME needs to be refreshed manually!"
        SUBJECT_MESSAGE="[URGENT] $SERVICE_NAME cache must be reset manually"
        echo -e $FAILURE_MESSAGE
        sendFailureMessageSlackEmail "$FAILURE_MESSAGE" "$SUBJECT_MESSAGE"
    else
        SUCCESS_MESSAGE="Fallback on stale $SERVICE_NAME cache succeeded! $SERVICE_NAME may need manual refreshing eventually"
        SUBJECT_MESSAGE="$SERVICE_NAME cache fallback succeeded!"
        echo -e "$SUCCESS_MESSAGE"
        sendSuccessMessageSlackEmail "$SUCCESS_MESSAGE" "$SUBJECT_MESSAGE"
    fi
    return $return_value
}

function refreshCddCache {
    # attempt to recache CDD
    ENDPOINT="refreshCache"
    CDD_SERVER1="http://dashi.cbio.mskcc.org:8280/cdd/api"
    CDD_SERVER2="http://dashi2.cbio.mskcc.org:8280/cdd/api"
    CDD_SERVER_LIST=($CDD_SERVER1 $CDD_SERVER2)
    refreshCache "CDD" $MAX_ATTEMPTS $ENDPOINT ${CDD_SERVER_LIST[@]}; return_value=$?
    if [ $return_value -gt 0 ] ; then
        # query cdd for known clinical attribute and check response - if still failed then alert pipelines team
        # that subsequent imports will fail if they require new attributes not stored in cache
        echo -e "\nRecache of CDD attempt failed!"
        echo -e "\nChecking for valid stale cache to fallback on for CDD"
        KNOWN_WORKING_ENDPOINT="SAMPLE_ID"
        checkForValidStaleCache "CDD" $KNOWN_WORKING_ENDPOINT ${CDD_SERVER_LIST[@]}; return_value=$?
    fi
    return $return_value
}

function refreshOncotreeCache {
    # attempt to recache ONCOTREE
    ENDPOINT="api/refreshCache"
    ONCOTREE_SERVER1="http://dashi.cbio.mskcc.org:8280"
    ONCOTREE_SERVER2="http://dashi2.cbio.mskcc.org:8280"
    ONCOTREE_SERVER_LIST=($ONCOTREE_SERVER1 $ONCOTREE_SERVER2)
    refreshCache "ONCOTREE" $MAX_ATTEMPTS $ENDPOINT ${ONCOTREE_SERVER_LIST[@]}; return_value=$?
    if [ $return_value -gt 0 ] ; then
        # query oncotree for known oncotree code and check response - if still failed then alert pipelines team
        # that subsequent imports might fail if importer also fails to query oncotree service when
        # generating oncotree code cache
        echo -e "\nRecache of ONCOTREE attempt failed!"
        echo -e "\nChecking for valid stale cache to fallback on for ONCOTREE"
        KNOWN_WORKING_ENDPOINT="tumorTypes/search/code/TISSUE?exactMatch=true&levels=0,1"
        checkForValidStaleCache "ONCOTREE" $KNOWN_WORKING_ENDPOINT ${ONCOTREE_SERVER_LIST[@]}; return_value=$?
    fi
    return $return_value
}

## REFRESH CACHE FOR DESIRED SERVICE(S)

# NOTE: We only really care about stopping the imports when the return value for
#        'refreshOncotreeCache' is non-zero.
#
#         If the CDD service fails to recache then we are okay letting the
#        imports continue and we will see the error in the importer logs, email, and slack.
#
#        However, if the ONCOTREE service fails to recache AND the service failed
#        to fall back on a 'stale' cache then we want to prevent imports from running
#        so that the issue can be addressed immediately, perhaps by manually
#        restarting the oncotree tomcat. Therefore the 'return_value' will only be
#        overwritten by the 'refreshOncotreeCache' call, and not the 'refreshCddCache'
#        unless the --cdd-only option was passed.

return_value=0
case $REFRESH_SERVICE in
    ALL)
        refreshCddCache
        refreshOncotreeCache; return_value=$?
    ;;
    CDD)
        refreshCddCache; return_value=$?
    ;;
    ONCOTREE)
        refreshOncotreeCache; return_value=$?
    ;;
    *)
        # sanity checking that REFRESH_SERVICE gets resolved correctly
        echo -e "REFRESH_SERVICE is not set to valid value: $REFRESH_SERVICE not in [ALL | CDD | ONCOTREE]"
        usage
    ;;
esac
exit $return_value
