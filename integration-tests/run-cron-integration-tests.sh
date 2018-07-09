#!/bin/bash
TESTING_DIRECTORY=/var/lib/jenkins/tempdir
REDCAP_EXPORTS_DIRECTORY=$TESTING_DIRECTORY/redcap_exports
LIB_DIRECTORY=$TESTING_DIRECTORY/lib
CMO_PIPELINES_DIRECTORY="$(pwd)"
CMO_REDCAP_DIRECTORY=$CMO_PIPELINES_DIRECTORY/redcap
CMO_INTEGRATION_TESTS_DIRECTORY=$CMO_PIPELINES_DIRECTORY/integration-tests
REDCAP_JAR=$CMO_REDCAP_DIRECTORY/redcap_pipeline/target/redcap_pipeline.jar
TEST_SUCCESS=0

mkdir -p $REDCAP_EXPORTS_DIRECTORY $LIB_DIRECTORY
echo "Running integration tests!"
echo "Copying application.properties from jenkins holding area to cmo-pipelines"
sh $CMO_INTEGRATION_TESTS_DIRECTORY/set_application_properties.sh

echo "Building jars and copying into lib directory"
cd $CMO_REDCAP_DIRECTORY ; mvn install -DskipTests=true; mv $REDCAP_JAR $LIB_DIRECTORY

cd $CMO_INTEGRATION_TESTS_DIRECTORY
python scan-for-expected-redcap-projects.py -e expected_study_project_list.txt -t $REDCAP_EXPORTS_DIRECTORY -j $LIB_DIRECTORY/redcap_pipeline.jar
if [ $? -gt 0 ] ; then
    echo "Test failed - ID mappings project in redcap differs from expected"
    TEST_SUCCESS=0
else
    echo "Test success - ID mapping project in redcap matches expected"
    TEST_SUCCESS=1
fi

rm -rf $TESTING_DIRECTORY
if [ $TEST_SUCCESS -eq 0 ] ; then
    exit 1
fi
exit 0
