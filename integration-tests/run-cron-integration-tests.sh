#!/bin/bash

JENKINS_USER_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_PIPELINES_CREDENTIALS=$JENKINS_USER_HOME_DIRECTORY/pipelines-credentials
TESTING_DIRECTORY=$JENKINS_USER_HOME_DIRECTORY/tempdir

if [ ! -d $TESTING_DIRECTORY ] ; then
    mkdir -p $TESTING_DIRECTORY
fi
TESTING_DIRECTORY_TEMP=$(mktemp -d $TESTING_DIRECTORY/cron-integration.XXXXXX)
REDCAP_EXPORTS_DIRECTORY=$TESTING_DIRECTORY_TEMP/redcap_exports
LIB_DIRECTORY=$TESTING_DIRECTORY_TEMP/lib
CMO_PIPELINES_DIRECTORY="$(pwd)"
CMO_REDCAP_DIRECTORY=$CMO_PIPELINES_DIRECTORY/redcap
CMO_INTEGRATION_TESTS_DIRECTORY=$CMO_PIPELINES_DIRECTORY/integration-tests
REDCAP_JAR=$CMO_REDCAP_DIRECTORY/redcap_pipeline/target/redcap_pipeline.jar
SSL_TRUSTSTORE=$JENKINS_PIPELINES_CREDENTIALS/AwsSsl.truststore
SSL_TRUSTSTORE_PASSWORD=`cat $JENKINS_PIPELINES_CREDENTIALS/AwsSsl.truststore.password` 
TEST_SUCCESS=0

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    send_slack_message_to_channel "#msk-pipeline-logs" "string" "Redcap ID mappings integration test failed! :face_palm: Please fix before the production run."
}

mkdir -p $REDCAP_EXPORTS_DIRECTORY $LIB_DIRECTORY
echo "Running integration tests!"
echo "Copying application.properties from jenkins holding area to cmo-pipelines"
sh $CMO_INTEGRATION_TESTS_DIRECTORY/set_application_properties.sh

echo "Building jars and copying into lib directory"
cd $CMO_REDCAP_DIRECTORY ; mvn install -DskipTests=true; mv $REDCAP_JAR $LIB_DIRECTORY

cd $CMO_INTEGRATION_TESTS_DIRECTORY
python scan-for-expected-redcap-projects.py -e expected_study_project_list.txt -t $REDCAP_EXPORTS_DIRECTORY -j $LIB_DIRECTORY/redcap_pipeline.jar -s $SSL_TRUSTSTORE -p $SSL_TRUSTSTORE_PASSWORD

if [ $? -gt 0 ] ; then
    echo "Test failed - ID mappings project in redcap differs from expected"
    sendFailureMessageMskPipelineLogsSlack
    TEST_SUCCESS=0
else
    echo "Test success - ID mapping project in redcap matches expected"
    TEST_SUCCESS=1
fi

rm -rf $TESTING_DIRECTORY_TEMP
if [ $TEST_SUCCESS -eq 0 ] ; then
    exit 1
fi
exit 0
