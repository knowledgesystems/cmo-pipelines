#!/bin/bash

source $(dirname $0)/monitor.properties

${TOMCAT_MONITOR_BASE_DIRECTORY}/run-jktest-cbioportal.sh
jkReturnCode=$?
${TOMCAT_MONITOR_BASE_DIRECTORY}/run-saml-redirect-tests-cbioportal.sh
samlReturnCode=$?

if [ $jkReturnCode -eq '0' ] && [ $samlReturnCode -eq '0' ] ; then
	echo 0$'\n'0 > $CONSECUTIVE_FAIL_COUNTFILE
else
	if ! [ -f $CONSECUTIVE_FAIL_COUNTFILE ] ; then
		rm -f $CONSECUTIVE_FAIL_COUNTFILE
		echo 0$'\n'0 > $CONSECUTIVE_FAIL_COUNTFILE
	fi
	{ read -t 3 currentjkcount ; read -t 3 currentsamlcount ; } < $CONSECUTIVE_FAIL_COUNTFILE
	if [ -z $currentjkcount ] ; then
		currentjkcount='0'
	fi
	if [ -z $currentsamlcount ] ; then
		currentsamlcount='0'
	fi
	if [ $jkReturnCode -ne '0' ] ; then
		currentjkcount=$(( $currentjkcount+1 ))
	else
		currentjkcount='0'
	fi
	if [ $samlReturnCode -ne '0' ] ; then
		currentsamlcount=$(( $currentsamlcount+1 ))
	else
		currentsamlcount='0'
	fi
	echo $currentjkcount$'\n'$currentsamlcount > $CONSECUTIVE_FAIL_COUNTFILE
fi
