#!/bin/bash

# function for sending notification emails
send_email_notification() {
    diff_report=$1
    ### FAILURE EMAIL ###
    EMAIL_BODY="The current (pipelines) crontab is out of sync with the crontab in github.\n\n $diff_report"
    echo -e "Sending email\n$EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Alert: Crontab out of sync on $HOSTNAME" cbioportal-pipelines@cbio.mskcc.org
}

if [ -z $PORTAL_HOME ] | [ -z $PIPELINES_CONFIG_HOME ] ; then
    echo "monitor-crontab-version.sh could not be run: missing environment variables must be set using automation-environment.sh"
    exit 1
fi

GITHUB_CRONTAB_URL="https://api.github.com/repos/knowledgesystems/cmo-pipelines/contents/import-scripts/mycrontab"
GITHUB_AUTHORIZATION=$(cat $PIPELINES_CONFIG_HOME/git/git-credentials | sed 's/https:\/\///; s/@github\.com//')
CURRENT_CRONTAB_FILE="$PORTAL_HOME/tmp/current_crontab"
GITHUB_CRONTAB_FILE="$PORTAL_HOME/tmp/git_crontab"
REQUIRED_USERNAME="cbioportal_importer"
REQUIRED_HOSTNAME="pipelines.cbioportal.mskcc.org"

if [ $USER != $REQUIRED_USERNAME ] ; then
    echo "monitor-crontab-version.sh must be run as $REQUIRED_USERNAME"
    exit 1
fi

if [ $HOSTNAME != $REQUIRED_HOSTNAME ] ; then
    echo "monitor-crontab-version.sh  must be run on $HOSTNAME"
    exit 1
fi

rm -f $PORTAL_HOME/tmp/git_crontab
rm -f $PORTAL_HOME/tmp/current_crontab

curl -f -u "$GITHUB_AUTHORIZATION" -H "Accept: application/vnd.github.v4.raw" -L -o $GITHUB_CRONTAB_FILE $GITHUB_CRONTAB_URL
if [ $? -gt 0 ] ; then
    echo "There was an error retrieving crontab from github -- curl returned a non-zero exit status"
    exit 1
fi
crontab -l > $CURRENT_CRONTAB_FILE

diff_output=$(diff $GITHUB_CRONTAB_FILE $CURRENT_CRONTAB_FILE)
if [ $? -gt 0 ] ; then
    send_email_notification "$diff_output"
fi

rm -f $PORTAL_HOME/tmp/git_crontab
rm -f $PORTAL_HOME/tmp/current_crontab
