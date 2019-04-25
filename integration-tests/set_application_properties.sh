#!/bin/bash

JENKINS_USER_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_PROPERTIES_DIRECTORY=$JENKINS_USER_HOME_DIRECTORY/pipelines-configuration/properties
JENKINS_PIPELINES_CREDENTIALS=$JENKINS_USER_HOME_DIRECTORY/pipelines-credentials

# CMO_PIPELINES_DIRECTORY is starting directory - location on jenkins machine where github repo is cloned
CMO_PIPELINES_DIRECTORY="$(pwd)"
APPLICATION_PROPERTIES=application.properties

rsync $JENKINS_PROPERTIES_DIRECTORY/redcap-pipeline/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/redcap/redcap_pipeline/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/fetch-cvr/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/cvr/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/fetch-darwin/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/darwin/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/fetch-crdb/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/crdb/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/fetch-ddp/$APPLICATION_PROPERTIES $CMO_PIPELINES_DIRECTORY/ddp/ddp_pipeline/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/fetch-ddp/*.json $CMO_PIPELINES_DIRECTORY/ddp/ddp_pipeline/src/main/resources
rsync $JENKINS_PIPELINES_CREDENTIALS/application-secure.properties $CMO_PIPELINES_DIRECTORY/ddp/ddp_pipeline/src/main/resources

cp $JENKINS_USER_HOME_DIRECTORY/jobs/genome-nexus-annotation-pipeline/workspace/annotationPipeline/target/annotationPipeline*.jar $CMO_PIPELINES_DIRECTORY/annotator.jar

