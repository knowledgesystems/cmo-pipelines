#!/usr/bin/env bash

if [ -z "$PORTAL_HOME" ] | [ -z "$PIPELINES_CONFIG_HOME" ] ; then
    echo "monitor-crontab-version.sh could not be run: missing environment variables must be set using automation-environment.sh" >&2
    exit 1
fi

GITHUB_PIPELINES_CRONTAB_URL="https://api.github.com/repos/knowledgesystems/cmo-pipelines/contents/import-scripts/pipelines/mycrontab"
GITHUB_PIPELINES_EKS_CRONTAB_URL="https://api.github.com/repos/knowledgesystems/cmo-pipelines/contents/import-scripts/pipelines_eks/mycrontab"
GITHUB_AUTHORIZATION=$(cat $PIPELINES_CONFIG_HOME/git/git-credentials | sed 's/https:\/\///; s/@github\.com//')
CURRENT_CRONTAB_FILE="$PORTAL_HOME/tmp/current_crontab"
GITHUB_CRONTAB_FILE="$PORTAL_HOME/tmp/git_crontab"
REQUIRED_USERNAME="cbioportal_importer"
PIPELINES_HOST_SUFFIX="pipelines.cbioportal.mskcc.org"
PIPELINES_EKS_HOST_SUFFIX=".mskcc.root.mskcc.org"

function send_email_notification() {
    diff_report=$1
    ### FAILURE EMAIL ###
    EMAIL_BODY="The current (pipelines) crontab is out of sync with the crontab in github.\n\n $diff_report"
    echo -e "Sending email\n$EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Alert: Crontab out of sync on $HOSTNAME" cbioportal-pipelines@cbioportal.org
}

if [ "$USER" != "$REQUIRED_USERNAME" ] ; then
    echo "monitor-crontab-version.sh must be run as $REQUIRED_USERNAME" >&2
    exit 1
fi

# returns status zero (success) if string ends with suffix
function has_suffix() {
    string=$1
    suffix=$2
    [[ "$string" =~ .*"$suffix"$ ]]
}

if has_suffix "$HOSTNAME" "$PIPELINES_HOST_SUFFIX" ; then
    GITHUB_CRONTAB_URL="$GITHUB_PIPELINES_CRONTAB_URL"
else
    if has_suffix "$HOSTNAME" "$PIPELINES_EKS_HOST_SUFFIX" ; then
        GITHUB_CRONTAB_URL=$GITHUB_PIPELINES_EKS_CRONTAB_URL
    else
        echo "error : monitor-crontab-version.sh must be run on either $PIPELINES_HOST_SUFFIX or an eks node with hostname like *$PIPELINES_EKS_HOST_SUFFIX" >&2
        exit 1
    fi
fi

rm -f $PORTAL_HOME/tmp/git_crontab
rm -f $PORTAL_HOME/tmp/current_crontab

curl -f -u "$GITHUB_AUTHORIZATION" -H "Accept: application/vnd.github.v4.raw" -L -o $GITHUB_CRONTAB_FILE $GITHUB_CRONTAB_URL
if [ $? -gt 0 ] ; then
    echo "There was an error retrieving crontab from github -- curl returned a non-zero exit status" >&2
    exit 1
fi
crontab -l > $CURRENT_CRONTAB_FILE

diff_output=$(diff $GITHUB_CRONTAB_FILE $CURRENT_CRONTAB_FILE)
if [ $? -gt 0 ] ; then
    send_email_notification "$diff_output"
fi

rm -f $PORTAL_HOME/tmp/git_crontab
rm -f $PORTAL_HOME/tmp/current_crontab
