#!/usr/bin/env bash

MAX_SEC_TO_EXPIRY_WITHOUT_WARNING=$((10*24*60*60)) # 10 days
TMP_DIR="/tmp/g2s_check"
G2S_HOSTNAME="g2s.genomenexus.org"
RESPONSE_FILENAME="$TMP_DIR/g2s_cert_response.txt"
PEM_FILENAME="$TMP_DIR/g2s_certs.pem"
CERTIFICATE_TEXT_FILENAME="$TMP_DIR/cert.txt"
G2S_MONITOR_SLACK_URI_FILE="/data/portal-cron/pipelines-credentials/g2s-monitor-webhook-uri"
G2S_MONITOR_URI=$(cat "$G2S_MONITOR_SLACK_URI_FILE")

function make_tmp_dir_if_necessary() {
    if ! [ -d "$TMP_DIR" ] ; then
        if ! mkdir -p "$TMP_DIR" ; then
            echo "Error : could not create tmp directory '$TMP_DIR'" >&2
            exit 1
        fi
    fi
}

function request_certificate() {
    if ! openssl s_client -showcerts -connect $G2S_HOSTNAME:443 < /dev/null > "$RESPONSE_FILENAME" 2> /dev/null ; then
        echo "error : could not fetch certificate from server $G2S_HOSTNAME"
        exit 1
    fi
}

function trim_certificate() {
    if ! sed -n -e '/-.BEGIN/,/-.END/ p' "$RESPONSE_FILENAME" > "$PEM_FILENAME" ; then
        echo "error : could not extract pem content from response from $G2S_HOSTNAME response"
        exit 1
    fi
}

function extract_certificate_text() {
    if ! openssl x509 -in "$PEM_FILENAME" -text > "$CERTIFICATE_TEXT_FILENAME" ; then
        echo "error : pem content from $G2S_HOSTNAME is not in recognizable format"
        exit 1
    fi
}

function extract_expiration_date() {
    unset CERT_EXPIRY_DATE
    CERT_EXPIRY_DATE=$(grep -A1 "Not Before" "$CERTIFICATE_TEXT_FILENAME" | head -n 2 | grep "Not After" | sed "s/.*[ ]:[ ]//")
    if [ -z "$CERT_EXPIRY_DATE" ] ; then
        echo "error : could not locate expiration date (Not After) from $G2S_HOSTNAME"
        exit 1
    fi
}

function computes_seconds_until_expiration() {
    unset SEC_TO_EXPIRY
    CERT_EXPIRY_SEC_SINCE_EPOC=$(date --date="$CERT_EXPIRY_DATE" +%s)
    CURRENT_SEC_SINCE_EPOC=$(date +%s)
    SEC_TO_EXPIRY=$(( $CERT_EXPIRY_SEC_SINCE_EPOC - $CURRENT_SEC_SINCE_EPOC ))
}

send_warning() {
    MESSAGE="Warning : the ssl certificate for host $G2S_HOSTNAME expires in $SEC_TO_EXPIRY seconds"
    curl -X POST -H 'Content-type: application/json' --data '{"username": "g2smonitor", "text": "'"$MESSAGE"'", "icon_emoji": ":waning_crescent_moon:"}' "$G2S_MONITOR_URI"
    #curl -X POST -H 'Content-type: application/json' --data-urlencode "payload={\"channel\": \"#g2s\", \"username\": \"g2s-monitor\", \"text\": \"$MESSAGE\", \"icon_emoji\": \":waning_crescent_moon:\"}" "$G2S_MONITOR_URI"
}

function send_warning_if_necessary() {
    if [ "$SEC_TO_EXPIRY" -lt "$MAX_SEC_TO_EXPIRY_WITHOUT_WARNING" ] ; then
        send_warning
    fi
}

function clean_up() {
    rm -f "$G2S_HOSTNAME" "$RESPONSE_FILENAME" "$PEM_FILENAME" "$CERTIFICATE_TEXT_FILENAME"
}

make_tmp_dir_if_necessary
request_certificate
trim_certificate
extract_certificate_text
extract_expiration_date
computes_seconds_until_expiration
send_warning_if_necessary
clean_up
