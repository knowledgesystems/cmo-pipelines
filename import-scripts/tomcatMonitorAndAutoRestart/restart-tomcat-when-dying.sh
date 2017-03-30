#! /bin/bash

source $(dirname $0)/monitor.properties

if [ -f $RESTART_LOCKOUT_PERIOD_COUNT_FILENAME ] ; then
	read -t 3 lockoutCount < $RESTART_LOCKOUT_PERIOD_COUNT_FILENAME
	if [ -z $lockoutCount ] ; then
		lockoutCount='0'
	fi
	newLockoutCount=$(( lockoutCount-1 ))
	if [ $newLockoutCount -lt '1' ] ; then
		rm -f $RESTART_LOCKOUT_PERIOD_COUNT_FILENAME
	else
		echo $newLockoutCount > $RESTART_LOCKOUT_PERIOD_COUNT_FILENAME
	fi
	exit 0;
fi
if ! [ -f $CONSECUTIVE_FAIL_COUNTFILE ] ; then
	exit 0;
fi
{ read -t 3 jkCount ; read -t 3 samlCount ; } < $CONSECUTIVE_FAIL_COUNTFILE
if [ -z $jkCount ] ; then
	jkCount='0'
fi
if [ -z $samlCount ] ; then
	samlCount='0'
fi
if [ $jkCount -gt $CONSECUTIVE_JK_FAIL_COUNT_LIMIT ] || [ $samlCount -gt $CONSECUTIVE_SAML_FAIL_COUNT_LIMIT ] ; then
	echo 0$'\n'0 > $CONSECUTIVE_FAIL_COUNTFILE
	$MANAGE_TOMCAT_SCRIPT stop && sleep $MANAGE_TOMCAT_SCRIPT_SLEEP_PERIOD && $MANAGE_TOMCAT_SCRIPT start
	echo $RESTART_LOCKOUT_PERIOD_COUNT_AFTER_RESTART > $RESTART_LOCKOUT_PERIOD_COUNT_FILENAME
	echo 0$'\n'0 > $CONSECUTIVE_FAIL_COUNTFILE
	mailSubject="${PORTAL_LABEL}_tomcat_auto_restart_notice"
	echo > $RESTART_NOTIFICATION_EMAIL_BODY_FILENAME
	if [ $jkCount -gt $CONSECUTIVE_JK_FAIL_COUNT_LIMIT ] ; then
		mailSubject="${PORTAL_LABEL}_tomcat_auto_restart_notice_jk"
		echo "The $TOMCAT_LABEL running on host $TOMCAT_HOST_LABEL has been automatically restarted due to failures to respond within 3 seconds to requests to the ajp13 port ($TOMCAT_AJP_PORT). Tests are run every 5 minutes and $jkCount consecutive failures have occurred." > $RESTART_NOTIFICATION_EMAIL_BODY_FILENAME
	fi
	if [ $samlCount -gt $CONSECUTIVE_SAML_FAIL_COUNT_LIMIT ] ; then
		mailSubject="${PORTAL_LABEL}_tomcat_auto_restart_notice_saml"
		echo "The $TOMCAT_LABEL running on host $TOMCAT_HOST_LABEL has been automatically restarted due to improper saml redirection. Tests are run every 5 minutes and $samlCount consecutive failures have occurred." > $RESTART_NOTIFICATION_EMAIL_BODY_FILENAME

	fi
	mail -s $mailSubject $NOTIFY_EMAIL_LIST < $RESTART_NOTIFICATION_EMAIL_BODY_FILENAME
	rm -f $RESTART_NOTIFICATION_EMAIL_BODY_FILENAME
fi
