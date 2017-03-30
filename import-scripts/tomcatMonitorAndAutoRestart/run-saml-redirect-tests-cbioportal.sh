#!/bin/bash

source $(dirname $0)/monitor.properties

failureCase=0
for x in 1 2 ; do
	${TOMCAT_MONITOR_BASE_DIRECTORY}/run-saml-redirect-test.sh $x
	returnCode=$?
	if [ $returnCode -eq '2' ] ; then
		failureCase=$x
		break
	fi
done
if [ $failureCase -ne '0' ] ; then
	mailSubject=${PORTAL_LABEL}_saml_redirect_failure_$failureCase 
	echo "The ${TOMCAT_LABEL} running on host ${TOMCAT_HOST_LABEL} forwarded a request by wget (not logged in) to a saml login page on a different server. Tomcat may need to be restarted." > $SAML_TEST_REDIRECT_FAIL_EMAIL_BODY_FILENAME
	mail -s $mailSubject $SAML_TEST_NOTIFY_EMAIL_LIST < $SAML_TEST_REDIRECT_FAIL_EMAIL_BODY_FILENAME
	rm -f $SAML_TEST_REDIRECT_FAIL_EMAIL_BODY_FILENAME
fi
exit $failureCase
