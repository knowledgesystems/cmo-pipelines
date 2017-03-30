#!/bin/bash

source $(dirname $0)/monitor.properties

testCase=$1
randomString="$RANDOM$RANDOM$RANDOM$RANDOM"
documentTmpFilename="$TOMCAT_MONITOR_TMP_DIRECTORY/document_tmp$randomString"
stderrTmpFilename="$TOMCAT_MONITOR_TMP_DIRECTORY/stderr_tmp$randomString"
queryPage1="http://${SAML_TEST_HOSTNAME}/${SAML_TEST_QUERY_PAGE_1}"
queryPage2="http://${SAML_TEST_HOSTNAME}/${SAML_TEST_QUERY_PAGE_2}"

if ! [ -d $TOMCAT_MONITOR_TMP_DIRECTORY ] ; then
	mkdir $TOMCAT_MONITOR_TMP_DIRECTORY
fi
if [ -z $testCase ] ; then
	testCase='1'
fi
queryPage=$queryPage1 #default
if [ $testCase == '1' ] ; then queryPage=$queryPage1; fi
if [ $testCase == '2' ] ; then queryPage=$queryPage2; fi
wget --timeout=30 --tries=1 --no-check-certificate --max-redirect=7 --output-document $documentTmpFilename $queryPage 2> $stderrTmpFilename
wgetReturnCode=$?
#check if all redirects are to the correct host
declare -a words
while read -t 3 -a words; do
	if [[ ${words[0]} == "Location:" ]] ; then
		if ! [[ ${words[1]} =~ ://${SAML_TEST_HOSTNAME}[:/] ]] ; then
			rm -f $documentTmpFilename # (Keep stderr to examine later) $stderrTmpFilename
			exit 2 #web server redirected to another host [This is the observed problem]
		fi
	fi
done < $stderrTmpFilename;
rm -f $documentTmpFilename
if [ $wgetReturnCode -ne '0' ] ; then
	exit 1 #web server was unable to produce the saml login page
else
	rm -f $stderrTmpFilename
fi
exit 0
