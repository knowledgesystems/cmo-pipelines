#!/bin/bash

TESTING_DIRECTORY=/var/lib/jenkins/tempdir
if [ ! -d $TESTING_DIRECTORY ] ; then
    mkdir -p $TESTING_DIRECTORY
fi
TESTING_DIRECTORY_TEMP=$(mktemp -d $TESTING_DIRECTORY/pr-integration.XXXXXX)
REDCAP_EXPORTS_DIRECTORY=$TESTING_DIRECTORY_TEMP/redcap_exports
FETCHED_FILES_DIRECTORY=$TESTING_DIRECTORY_TEMP/fetched_files
LIB_DIRECTORY=$TESTING_DIRECTORY_TEMP/lib
CMO_PIPELINES_DIRECTORY="$(pwd)"
CMO_REDCAP_DIRECTORY=$CMO_PIPELINES_DIRECTORY/redcap
CMO_CVR_DIRECTORY=$CMO_PIPELINES_DIRECTORY/cvr
CMO_CRDB_DIRECTORY=$CMO_PIPELINES_DIRECTORY/crdb
CMO_DDP_DIRECTORY=$CMO_PIPELINES_DIRECTORY/ddp
CMO_DARWIN_DIRECTORY=$CMO_PIPELINES_DIRECTORY/darwin
CMO_INTEGRATION_TESTS_DIRECTORY=$CMO_PIPELINES_DIRECTORY/integration-tests
REDCAP_JAR=$CMO_REDCAP_DIRECTORY/redcap_pipeline/target/redcap_pipeline.jar
CVR_JAR=$CMO_CVR_DIRECTORY/target/cvr_fetcher.jar
CRDB_JAR=$CMO_CRDB_DIRECTORY/target/crdb_fetcher.jar
DDP_JAR=$CMO_DDP_DIRECTORY/ddp_pipeline/target/ddp_fetcher.jar
DARWIN_JAR=$CMO_DARWIN_DIRECTORY/target/darwin_fetcher.jar
JENKINS_USER_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_PIPELINES_CREDENTIALS=$JENKINS_USER_HOME_DIRECTORY/pipelines-credentials
SSL_TRUSTSTORE=$JENKINS_PIPELINES_CREDENTIALS/AwsSsl.truststore
SSL_TRUSTSTORE_PASSWORD=`cat $JENKINS_PIPELINES_CREDENTIALS/AwsSsl.truststore.password` 
TEST_SUCCESS=0

mkdir -p $REDCAP_EXPORTS_DIRECTORY $FETCHED_FILES_DIRECTORY $LIB_DIRECTORY
echo "Running integration tests!"
echo "Copying application.properties from jenkins holding area to cmo-pipelines"
sh $CMO_INTEGRATION_TESTS_DIRECTORY/set_application_properties.sh

echo "Building jars and copying into lib directory"
cd $CMO_REDCAP_DIRECTORY ; mvn install -DskipTests=true; mv $REDCAP_JAR $LIB_DIRECTORY
cd $CMO_CVR_DIRECTORY ; mvn install -DskipTests=true; mv $CVR_JAR $LIB_DIRECTORY
cd $CMO_CRDB_DIRECTORY ; mvn install -DskipTests=true; mv $CRDB_JAR $LIB_DIRECTORY
cd $CMO_DDP_DIRECTORY ; mvn install -DskipTests=true; mv $DDP_JAR $LIB_DIRECTORY
cd $CMO_DARWIN_DIRECTORY ; mvn install -DskipTests=true; mv $DARWIN_JAR $LIB_DIRECTORY

cd $CMO_INTEGRATION_TESTS_DIRECTORY
echo "GIT_BRANCH is '$GIT_BRANCH'"
if [[ -n "$GIT_BRANCH" ]] ; then
    if [[ "$GIT_BRANCH" =~ ^origin/pull/([0-9]+)/head$ ]] ; then
        PR_NUMBER=${BASH_REMATCH[1]}
        echo "getting tags for pull-request $PR_NUMBER"
        python fetch_and_compare_to_redcap_schema.py -r $REDCAP_EXPORTS_DIRECTORY -f $FETCHED_FILES_DIRECTORY -g /var/lib/jenkins/git-credentials -n $PR_NUMBER -l $LIB_DIRECTORY -s $SSL_TRUSTSTORE -p $SSL_TRUSTSTORE_PASSWORD
        if [ $? -eq 0 ] ; then
            echo "Data schema matches in data fetch and redcap project"
            TEST_SUCCESS=1
        else
            echo "Data schema differs between data fetch and redcap project - refer to to jenkins console log"
            TEST_SUCCESS=0
        fi 
    else
        echo "No integration tests for GIT_BRANCH: '$GIT_BRANCH'"
        TEST_SUCCESS=1
    fi
else
    echo "Do not have GIT_BRANCH, nothing to do"
    TEST_SUCCESS=1
fi

rm -rf $TESTING_DIRECTORY_TEMP
if [ $TEST_SUCCESS -eq 0 ] ; then
   exit 1
fi

exit 0
