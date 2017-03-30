#!/bin/bash

source $(dirname $0)/monitor.properties

labelDefTerm=
hostDefTerm=
portDefTerm=
timeoutDefTerm=
emailDefTerm=
debugDefTerm=
if ! [ -z $TOMCAT_LABEL ] ; then labelDefTerm=" -Dtomcat.label=$TOMCAT_LABEL" ; fi
if ! [ -z $TOMCAT_AJP_HOST ] ; then hostDefTerm=" -Dtomcat.host=$TOMCAT_AJP_HOST" ; fi
if ! [ -z $TOMCAT_AJP_PORT ] ; then portDefTerm=" -Dtomcat.ajpport=$TOMCAT_AJP_PORT" ; fi
if ! [ -z $JK_TEST_PING_TIMEOUT ] ; then timeoutDefTerm=" -Dping.timeout=$JK_TEST_PING_TIMEOUT" ; fi
if ! [ -z $JK_TEST_NOTIFY_EMAIL_LIST ] ; then emailDefTerm=" -Dnotice.email=$JK_TEST_NOTIFY_EMAIL_LIST" ; fi
if ! [ -z $JK_TEST_DEBUG_MODE ] ; then debugDefTerm=" -Ddebug.mode=$JK_TEST_DEBUG_MODE" ; fi
if [ -z $JAVA_HOME ] ; then JAVA_HOME="/usr" ; fi
JAVA_COMMAND="$JAVA_HOME/bin/java"
${JAVA_COMMAND} -cp $JK_TEST_CLASS_PATH $labelDefTerm $hostDefTerm $portDefTerm $timeoutDefTerm $emailDefTerm $debugDefTerm JKTester
returnCode=$?
if [ $returnCode -eq '0' ] || [ $returnCode -eq '1' ] ; then
	exit $returnCode
else
	echo JKTest could not be completed: exit_code:$returnCode
	return 0
fi
