#!/usr/bin/env bash

PORTAL_NAME="$1"
NOTIFICATION_FILEPATH="$2"

if [ -z "$PORTAL_NAME" ] || [ -z "$NOTIFICATION_FILEPATH" ] || [ $# -gt 2 ]  ; then
    echo "usage: $0 portal_name notification_filepath" >&2
    exit 1
fi

if ! [ -r "$NOTIFICATION_FILEPATH" ] ; then
    echo "Error: notification file '$NOTIFICATION_FILEPATH' could not be read" >&2
    exit 1
fi

source /data/portal-cron/scripts/automation-environment.sh
source /data/portal-cron/scripts/color-config-parsing-functions.sh

declare -A portal_to_email_settings
EMAIL_CONFIGURATION_FILE="/data/portal-cron/pipelines-credentials/email-import-notifiction-after-import.yaml"

if ! read_map_from_yaml "portal_to_email_settings" "$EMAIL_CONFIGURATION_FILE" ".portal_to_email_map" ; then
    echo "Error: could not read portal_to_email_map from configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi
email_settings=${portal_to_email_settings["$PORTAL_NAME"]}

declare -A email_properties
if ! read_map_from_yaml "email_properties" "$EMAIL_CONFIGURATION_FILE" ".$email_settings" ; then
    echo "Error: could not read email settings for $email_settings from configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi

sender="${email_properties["sender"]}"
recipient="${email_properties["recipient"]}"
subject="${email_properties["subject"]}"
body="${email_properties["body"]}"
if [ -z "$sender" ] ; then
    echo "Error: 'sender' property was empty or missing for email settings $email_settings in configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi
if [ -z "$recipient" ] ; then
    echo "Error: 'recipient' property was empty or missing for email settings $email_settings in configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi
if [ -z "$subject" ] ; then
    echo "Error: 'subject' property was empty or missing for email settings $email_settings in configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi
if [ -z "$body" ] ; then
    echo "Error: 'body' property was empty or missing for email settings $email_settings in configuration file '$EMAIL_CONFIGURATION_FILE'" >&2
    exit 1
fi
RANDOM_SUFFIX=".tmp$RANDOM$RANDOM"
FULL_NOTIFICATION_FILEPATH="$NOTIFICATION_FILEPATH$RANDOM_SUFFIX"
rm -f "$FULL_NOTIFICATION_FILEPATH"
echo "$body" > "$FULL_NOTIFICATION_FILEPATH"
cat "$NOTIFICATION_FILEPATH" >> "$FULL_NOTIFICATION_FILEPATH"
echo "----------------------------------------" >>"$FULL_NOTIFICATION_FILEPATH"
echo "Do not use \"Reply\" with this message. Instead, reach out to the data engineering team or compose a new email to '$sender'" >> "$FULL_NOTIFICATION_FILEPATH"

echo "executing: mail -s \"$subject\" -q \"$FULL_NOTIFICATION_FILEPATH\" \"$recipient\" <<< \"\""
if ! mail -s "$subject" -q "$FULL_NOTIFICATION_FILEPATH" "$recipient" <<< "" ; then
    echo "Error during attempt to send email with command 'mail -s \"$subject\" -q \"$FULL_NOTIFICATION_FILEPATH\" \"$recipient\" <<< \"\"'" >&2
    rm -f "$FULL_NOTIFICATION_FILEPATH"
    exit 1
fi
# wait for child mail process to complete
MY_PID=$$
while ps --ppid $MY_PID -o command= | grep -v 'grep' | grep 'mail' ; do
    sleep 0.5
done
rm -f "$FULL_NOTIFICATION_FILEPATH"
exit 0
