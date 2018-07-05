#!/bin/bash

PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh
JENKINS_HOSTNAME=jenkins_hostname
JENKINS_PROPERTIES_DIRECTORY=/home/jenkins-user/pipelines-configuration/properties
JENKINS_PIPELINES_CREDENTIALS=/home/jenkins-user/pipelines-credentials
JENKINS_GIT_DIRECTORY=/home/jenkins-user/git
# Function for alerting slack channel that something failed
function sendFailureMessageMskPipelineLogsSlack {
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"$1\", \"icon_emoji\": \":boom:\"}" https://hooks.slack.com/services/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
}

if ! [ -f $PATH_TO_AUTOMATION_SCRIPT ] ; then
    echo "automation-environment.sh could not be found, exiting..."
    exit 2
fi

. $PATH_TO_AUTOMATION_SCRIPT
PROPERTIES_DIRECTORY=$PIPELINES_CONFIG_HOME/properties/
PIPELINES_CREDENTIALS=$PORTAL_HOME/pipelines-credentials/
GIT_CREDENTIALS=$PORTAL_CONFIG_HOME/git/git-credentials

cd $PROPERTIES_DIRECTORY
git pull
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when pulling pipelines-configuration git repo"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a $PROPERTIES_DIRECTORY $JENKINS_HOSTNAME:$JENKINS_PROPERTIES_DIRECTORY
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing properties to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a $PIPELINES_CREDENTIALS $JENKINS_HOSTNAME:$JENKINS_PIPELINES_CREDENTIALS
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing pipelines-credentials to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

rsync -a $GIT_CREDENTIALS $JENKINS_HOSTNAME:$JENKINS_GIT_DIRECTORY
if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when rsync-ing git-credentials to jenkins machine"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

ssh $JENKINS_HOSTNAME /bin/bash << EOF
	chmod -R 700 $JENKINS_PROPERTIES_DIRECTORY $JENKINS_PIPELINES_CREDENTIALS $JENKINS_GIT_DIRECTORY &&
	find $JENKINS_PROPERTIES_DIRECTORY -type f -exec chmod 600 {} \; &&
	find $JENKINS_PIPELINES_CREDENTIALS -type f -exec chmod 600 {} \; &&
	find $JENKINS_GIT_DIRECTORY -type f -exec chmod 600 {} \;
EOF

if [ $? -ne 0 ] ; then
    FAILURE_MESSAGE="Something went wrong when setting permissions for properties/credentials"
    sendFailureMessageMskPipelineLogsSlack "$FAILURE_MESSAGE"
    exit 1
fi

echo "Successfully synced properties/credentials to jenkins machine"
