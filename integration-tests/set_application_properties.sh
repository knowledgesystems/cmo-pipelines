#!/bin/bash

JENKINS_USER_HOME_DIRECTORY=/home/jenkins-user
JENKINS_PROPERTIES_DIRECTORY=$JENKINS_USER_HOME_DIRECTORY/pipelines-configuration/properties
JENKINS_PIPELINES_CREDENTIALS=$JENKINS_USER_HOME_DIRECTORY/pipelines-credentials
JENKINS_GIT_CREDENTIALS=$JENKINS_USER_HOME_DIRECTORY/git/git-credentials
JENKINS_SSH_CREDENTIALS_FILE=/var/lib/jenkins/id_rsa_jenkins_user
JENKINS_HOME_DIRECTORY=/var/lib/jenkins
TESTING_DIRECTORY=/home/jenkins-user/tempdir

# CMO_PIPELINES_DIRECTORY is starting directory - location on jenkins machine where github repo is cloned
CMO_PIPELINES_DIRECTORY="$(pwd)"
APPLICATION_PROPERTIES=application.properties

rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PROPERTIES_DIRECTORY/redcap-pipeline/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/redcap/redcap_pipeline/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PROPERTIES_DIRECTORY/fetch-cvr/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/cvr/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PROPERTIES_DIRECTORY/fetch-darwin/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/darwin/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PROPERTIES_DIRECTORY/fetch-crdb/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/crdb/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PROPERTIES_DIRECTORY/fetch-ddp/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/ddp/ddp_pipeline/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_PIPELINES_CREDENTIALS/application-secure.properties $CMO_PIPELINES_DIRECTORY/ddp/ddp_pipeline/src/main/resources
rsync --rsh="ssh -i $JENKINS_SSH_CREDENTIALS_FILE" jenkins-user@localhost:$JENKINS_GIT_CREDENTIALS $JENKINS_HOME_DIRECTORY
