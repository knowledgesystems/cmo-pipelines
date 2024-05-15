#!/usr/bin/env bash

# This script defines function send_slack_message_to_channel()
# The function will send a message payload to a slack channel (selected by an
# argument $1 containing the channel name.). Argument $2 is either 'string'
# or 'blocks' which chooses the payload type, and argument $3 contains the
# particular content to send. 'blocks' payloads can contain markdown.
#
# Examples of usage:
#   send_slack_message_to_channel "#msk-pipeline-logs" "string" "new message with emoji :apple:"
#   send_slack_message_to_channel "#msk-pipeline-logs" "blocks" "new message tagging user <@U060QQ9ML>"
#
# There is a 1 to 1 mapping of chanel names to Slack incoming webhook URLs, which is
# read from a file at $PORTAL_HOME/pipelines-credentials/slack_webhooks_map.txt which
# must be present on the system when using this function. The format of the file is
# plain text with pairs of adjacent lines (channel_name, webhook_url)

SLACK_WEBHOOKS_MAP_FILE="$PORTAL_HOME/pipelines-credentials/slack_webhooks_map.txt"

unset channel_name_to_webhook_url
declare -A channel_name_to_webhook_url

function parse_webhook_file() {
    if ! [[ -r "$SLACK_WEBHOOKS_MAP_FILE" ]] ; then
        echo "Error : unable to read slack webhook map file $SLACK_WEBHOOKS_MAP_FILE" >&2
        return 1
    fi
    while read -r channel_name ; do
        if [ -z "$channel_name" ] || [[ ${channel_name:0:1} != "#" ]] ; then
            echo "Warning : slack webhook map file $SLACK_WEBHOOKS_MAP_FILE is malformatted. Ignoring line \"$channel_name\" because every channel name is expected to begin with the # character." >&2
            continue
        fi
        read -r webhook_url
        if [ -z "$webhook_url" ] ; then
            echo "Warning : slack webhook map file $SLACK_WEBHOOKS_MAP_FILE has no webhook url defined for channel \"$channel_name\"" >&2
            continue
        fi
        channel_name_to_webhook_url["$channel_name"]="$webhook_url"
    done < "$SLACK_WEBHOOKS_MAP_FILE"
}

function http_status_is_success() {
    status="$1"
    if [[ ${#status} -lt 3 ]] || [[ ${status:0:1} != "2" ]] ; then
        return 1 # not success code
    else
        return 0 # success code
    fi
}

# Send a message payload of selected type to a named slack channel.
# Argument channel_name is used to select the intended WEBHOOK_URL
function send_slack_message_to_channel() {
    channel_name="$1"
    payload_type="$2"
    message_text="$3"
    # with the new version of webhooks
    #   - the destination channel is determined by the web app and can no longer be overridden in requests using payload field "channel"
    #   - the username of the sender is determined by the web app and can no longer be overridden in requests using payload field "username"
    #   - the sender icon is determined by the web app and can no longer be overridden in requests using payload field "icon_emoji"
    case "$payload_type" in
        string)
            payload_json="{\"text\": \"$message_text\"}"
            ;;
        blocks)
            payload_json="{\"blocks\": [ { \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"$message_text\" } } ] }"
            ;;
        *)
            echo "Error : function send_slack_message_to_channel called for unrecognized message payload type \"$payload_type\" ... message not sent : $message_text" >&2
            return 1
            ;;
    esac
    channel_webhook_url=${channel_name_to_webhook_url["$channel_name"]}
    if [ -z "$channel_webhook_url" ] ; then
        echo "Error : function send_slack_message_to_channel called for unrecognized channel \"$channel_name\" ... message not sent : $message_text" >&2
        return 1
    fi
    http_status=`curl -s -o /dev/null -w "%{http_code}" --data-urlencode payload="$payload_json" $channel_webhook_url`
    if ! http_status_is_success "$http_status" ; then
        echo "Error : send_slack_message_to_channel attempted to send message : $message_text to channel \"$channel_name\" but failed to receive a success status code from curl." >&2
        return 1
    fi
    return 0
}

if [ -z "$PORTAL_HOME" ] ; then
    echo "Error : slack-message-functions.sh cannot be run without setting the PORTAL_HOME environment variable. (Use automation-environment.sh)"
    # uninitialized map : clean up
    unset SLACK_WEBHOOKS_MAP_FILE channel_name_to_webhook_url
    unset -f send_slack_message_to_channel parse_webhook_file http_status_is_success
else
    parse_webhook_file
fi
