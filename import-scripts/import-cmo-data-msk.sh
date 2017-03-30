#!/bin/bash

# set necessary env variables with automation-environment.sh

tmp=$PORTAL_HOME/tmp/import-cron-cmo-msk
if [[ -d "$tmp" && "$tmp" != "/" ]] ; then
	rm -rf "$tmp"/*
fi
email_list="heinsz@mskcc.org, sheridar@mskcc.org, grossb1@mskcc.org, ochoaa@mskcc.org, wilsonm2@mskcc.org"
now=$(date "+%Y-%m-%d-%H-%M-%S")
msk_automation_notification_file=$(mktemp $tmp/msk-automation-portal-update-notification.$now.XXXXXX)

DB_VERSION_FAIL=0
# check database version before importing anything
echo "Checking if database version is compatible"
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --check-db-version
if [ $? -gt 0 ]
then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

if [ $DB_VERSION_FAIL -eq 0 ]
then
    # import vetted studies into MSK portal
    echo "importing cancer type updates into msk portal database..."
    $JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-cmo-importer.jar org.mskcc.cbio.importer.Admin --import-types-of-cancer
    echo "importing study data into msk portal database..."
    $JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184 -Xmx64g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-cmo-importer.jar org.mskcc.cbio.importer.Admin --update-study-data --portal msk-automation-portal --update-worksheet --notification-file "$msk_automation_notification_file" --use-never-import
    num_studies_updated=`cat $tmp/num_studies_updated.txt`

    # redeploy war
    if [ $num_studies_updated -gt 0 ]
    then
	    echo "'$num_studies_updated' studies have been updated, requesting redeployment of msk portal war..."
    	ssh -i $HOME/.ssh/id_rsa_tomcat_restarts_key cbioportal_importer@dashi.cbio.mskcc.org touch /srv/data/portal-cron/schultz-tomcat-restart
	    echo "'$num_studies_updated' studies have been updated (no longer need to restart schultz-tomcat server...)"
    else
	    #echo "No studies have been updated, skipping redeploy of msk portal war..."
    	echo "No studies have been updated.."
    fi
fi

EMAIL_BODY="The GDAC database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]
then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "GDAC Update Failure: DB version is incompatible" $email_list
fi

$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27184 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-cmo-importer.jar org.mskcc.cbio.importer.Admin --send-update-notification --portal msk-automation-portal --notification-file "$msk_automation_notification_file"

if [[ -d "$tmp" && "$tmp" != "/" ]] ; then
	rm -rf "$tmp"/*
fi
