#!/bin/bash

PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh

if ! [ -f "$PATH_TO_AUTOMATION_SCRIPT" ] ; then
    echo "automation-environment.sh could not be found, exiting..."
    exit 2
fi

source "$PATH_TO_AUTOMATION_SCRIPT"

# jenkins server paths
JENKINS_SRV_HOSTNAME=jenkins_hostname_prod
JENKINS_SRV_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_SRV_PROPERTIES_DIRECTORY=$JENKINS_SRV_HOME_DIRECTORY/pipelines-configuration/properties
JENKINS_SRV_SCRIPTS_DIRECTORY=$JENKINS_SRV_HOME_DIRECTORY/pipelines-configuration/jenkins
JENKINS_SRV_PIPELINES_CREDENTIALS=$JENKINS_SRV_HOME_DIRECTORY/pipelines-credentials
JENKINS_SRV_GIT_CREDENTIALS=$JENKINS_SRV_HOME_DIRECTORY/git-credentials
# local jenkins staging paths
LOCAL_PROPERTIES_DIRECTORY=$PIPELINES_CONFIG_HOME/properties/
LOCAL_JENKINS_DIRECTORY=$PIPELINES_CONFIG_HOME/jenkins/
LOCAL_PIPELINES_CREDENTIALS=$PORTAL_HOME/pipelines-credentials/
LOCAL_GIT_CREDENTIALS=$PIPELINES_CONFIG_HOME/git/git-credentials

source "$PORTAL_HOME/scripts/slack-message-functions.sh"

# Function for alerting slack channel that something failed
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE="$1"
    send_slack_message_to_channel "#msk-pipeline-logs" "string" "$MESSAGE :boom:"
}

cd $LOCAL_PROPERTIES_DIRECTORY
git pull
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when pulling pipelines-configuration git repo"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a --delete $LOCAL_PROPERTIES_DIRECTORY $JENKINS_SRV_HOSTNAME:$JENKINS_SRV_PROPERTIES_DIRECTORY
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing properties to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a --delete $LOCAL_JENKINS_DIRECTORY $JENKINS_SRV_HOSTNAME:$JENKINS_SRV_SCRIPTS_DIRECTORY
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing jenkins scripts to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a --delete $LOCAL_PIPELINES_CREDENTIALS $JENKINS_SRV_HOSTNAME:$JENKINS_SRV_PIPELINES_CREDENTIALS
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing pipelines-credentials to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a --delete $LOCAL_GIT_CREDENTIALS $JENKINS_SRV_HOSTNAME:$JENKINS_SRV_HOME_DIRECTORY
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing git-credentials to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

ssh $JENKINS_SRV_HOSTNAME /bin/bash << EOF
    chmod -R 700 $JENKINS_SRV_PROPERTIES_DIRECTORY $JENKINS_SRV_PIPELINES_CREDENTIALS &&
    find $JENKINS_SRV_PROPERTIES_DIRECTORY -type f -exec chmod 600 {} \; &&
    find $JENKINS_SRV_PIPELINES_CREDENTIALS -type f -exec chmod 600 {} \; &&
    chmod 600 $JENKINS_SRV_GIT_CREDENTIALS;
EOF

if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when setting permissions for properties/credentials"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

echo "Successfully synced properties/credentials to jenkins machine"
