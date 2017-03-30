#!/bin/bash

restful_logfile_date=$1
cbioportal_logfile_date=`echo $restful_logfile_date | sed -nr 's:(\w\w)/(\w\w)/(\w\w\w\w):\3-\1-\2:p'`

restful_logins_file=$2
msk_portal_logfile=/srv/www/msk-tomcat/tomcat7/logs/mskcc-portal.log.$cbioportal_logfile_date.gz

echo "Getting restful login count for $msk_portal_logfile from dashi.cbo.mskcc.org..."
dashi_login_count=$(ssh -i $HOME/.ssh/id_rsa_restful_login_counter_key cbioportal_importer@dashi.cbio.mskcc.org /data/portal-cron/scripts/cbioportal-restful-login-counter-worker.sh $msk_portal_logfile)

echo "Getting restful login count for $msk_portal_logfile from dashi2.cbo.mskcc.org..."
dashi2_login_count=$(ssh -i $HOME/.ssh/id_rsa_restful_login_counter_key cbioportal_importer@dashi2.cbio.mskcc.org /data/portal-cron/scripts/cbioportal-restful-login-counter-worker.sh $msk_portal_logfile)

total_num=$((dashi_login_count + dashi2_login_count))

echo "Writing restful login count for $restful_logfile_date to $restful_logins_file"
echo "$restful_logfile_date,$total_num" >> $restful_logins_file

echo "Copying $restful_logins_file to http server..."
scp -i $HOME/.ssh/id_rsa_restful_login_counter_key $restful_logins_file cbioportal_importer@ramen.cbio.mskcc.org:/srv/www/html/stats/cis-logins

