#!/bin/bash

# set necessary env variables with automation-environment.sh

echo $(date)
tmp=$PORTAL_HOME/tmp/import-cron-cmo-triage
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
	rm -rf "$tmp"/*
fi

now=$(date "+%Y-%m-%d-%H-%M-%S")
triage_notification_file=$(mktemp $tmp/triage-portal-update-notification.$now.XXXXXX)

# fetch updates in CMO repository
echo "fetching updates from bic-mskcc..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source bic-mskcc --run-date latest --update-worksheet

# fetch updates in private repository
echo "fetching updates from private..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source private --run-date latest --update-worksheet

# fetch clinical data mercurial
echo "fetching updates from msk-impact repository..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source dmp-clinical-data-mercurial --run-date latest

# fetch clinical data grail 
echo "fetching updates from grail repository..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source grail --run-date latest

# fetch updates in CMO impact
echo "fetching updates from impact..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source impact --run-date latest --update-worksheet
echo "fetching updates from impact-MERGED..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183-Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source impact-MERGED --run-date latest --update-worksheet

# fetch updates in studies repository
echo "fetching updates from cbio-portal-data..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source knowledge-systems-curated-studies --run-date latest

# import data that requires QC into triage portal
echo "importing cancer type updates into triage portal database..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --import-types-of-cancer

$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --apply-overrides triage-portal

echo "importing study data into triage portal database..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx32G -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --update-study-data --portal triage-portal --update-worksheet --notification-file $triage_notification_file --use-never-import
num_studies_updated=`cat $tmp/num_studies_updated.txt`

# redeploy war
if [ $num_studies_updated -gt 0 ]
then
	#echo "'$num_studies_updated' studies have been updated, redeploying triage-portal war..."
	echo "'$num_studies_updated' studies have been updated.  Restarting triage-tomcat server..."
	sudo /etc/init.d/triage-tomcat7 restart
	#echo "'$num_studies_updated' studies have been updated (no longer need to restart triage-tomcat server...)"
else
	echo "No studies have been updated, skipping redeploy of triage-portal war..."
fi

$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/triage-cmo-importer.jar org.mskcc.cbio.importer.Admin --send-update-notification --portal triage-portal --notification-file $triage_notification_file

if [[ -d "$tmp" && "$tmp" != "/" ]]; then
	rm -rf "$tmp"/*
fi
