#!/bin/bash

tmp=$PORTAL_HOME/tmp/update-gene-data
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
email_list="cbioportal-pipelines@cbio.mskcc.org"
GENE_DATA_UPDATER_JAR_FILENAME="$PORTAL_HOME/lib/gene_data_updater.jar"
JAVA_GENE_DATA_UPDATER_ARGS="$JAVA_PROXY_ARGS -jar $GENE_DATA_UPDATER_JAR_FILENAME"

echo "Updating portal configuration repository..."
cd $PORTAL_CONFIG_HOME; $HG_BINARY pull -u 

cd $tmp
echo "Downloading gene data resources from NCBI..."
export http_proxy='http://jxi2.mskcc.org:8080'
wget ftp.ncbi.nih.gov:gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz
wget ftp.ncbi.nih.gov:/genomes/Homo_sapiens/GFF/ref_GRCh38.p12_top_level.gff3.gz
gunzip $tmp/Homo_sapiens.gene_info.gz
gunzip $tmp/ref_GRCh38.p12_top_level.gff3.gz
echo "Finished!"

sed -i 's/^#//; s/\"//' $tmp/Homo_sapiens.gene_info
if [[ !  -f $tmp/Homo_sapiens.gene_info || $(wc -l < $tmp/Homo_sapiens.gene_info) -eq 0 ]]; then
    echo "Error downloading Homo_sapiens.gene_info from NCBI. Exiting..."
    exit 1
fi

if [[ !  -f $tmp/ref_GRCh38.p12_top_level.gff3 || $(wc -l < $tmp/ref_GRCh38.p12_top_level.gff3) -eq 0 ]]; then
    echo "Error downloading ref_GRCh38.p12_top_level.gff3 from NCBI. Exiting..."
    exit 1
fi


function runGeneUpdatePipeline {
    DATABASE_NAME=$1
    if [ -z $DATABASE_NAME ]; then
        echo "Database name must be provided! Exiting..."
        exit 1
    fi
    export DATABASE_NAME=$DATABASE_NAME

    echo "Starting gene data update job on database: $DATABASE_NAME"
    $JAVA_BINARY $JAVA_GENE_DATA_UPDATER_ARGS -d $tmp/Homo_sapiens.gene_info -l $tmp/ref_GRCh38.p12_top_level.gff3 -n $tmp/gene-update-notification.txt
    if [ $? -ne 0 ]; then 
        echo "Error updating gene data"
        echo -e "Error updating gene data." | mail -s "Gene Data Update Failure: $DATABASE_NAME" $email_list
    else
        echo "Success!"
        echo "Emailing results to email list..."
        if [[ -f $tmp/gene-update-notification.txt && $(wc -l < $tmp/gene-update-notification.txt) -gt 0 ]]; then
            cat $tmp/gene-update-notification.txt | mail -s "Gene Data Update Results: $DATABASE_NAME" $email_list
        else
            EMAIL_BODY="Error loading $tmp/gene-update-notification.txt"
            echo -e $EMAIL_BODY | mail -s "Gene Data Update Results: $DATABASE_NAME" $email_list
        fi
    fi
}

# run gene update pipeline for each database
export SPRING_CONFIG_LOCATION=$PORTAL_CONFIG_HOME/properties/update-gene/application.properties
runGeneUpdatePipeline "cgds_gdac"
runGeneUpdatePipeline "cgds_public"
runGeneUpdatePipeline "cgds_triage"
runGeneUpdatePipeline "cgds_genie"

export SPRING_CONFIG_LOCATION=$PORTAL_CONFIG_HOME/properties/update-gene/application-pancan.properties
runGeneUpdatePipeline "cgds_pancan"

echo "Restarting triage-tomcat server..."
TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Triage Tomcat 7"
TOMCAT_SERVER_DISPLAY_NAME="triage-tomcat7"
if ! /usr/bin/sudo /etc/init.d/triage-tomcat7 restart ; then
    EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server failed"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $pipeline_email_list
fi

echo "Restarting msk-tomcat servers..."
TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
TOMCAT_HOST_USERNAME=cbioportal_importer
TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_msk_tomcat_restarts_key
TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/msk-tomcat-restart
TOMCAT_SERVER_PRETTY_DISPLAY_NAME="MSK Tomcat" # e.g. Public Tomcat
TOMCAT_SERVER_DISPLAY_NAME="msk-tomcat" # e.g. schultz-tomcat
SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
declare -a failed_restart_server_list
for server in ${TOMCAT_HOST_LIST[@]}; do
    if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
        failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
    fi
done
if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
    EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
fi

echo "Restarting public-tomcat servers..."
TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
TOMCAT_HOST_USERNAME=cbioportal_importer
TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_public_tomcat_restarts_key
TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/public-tomcat-restart
TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Public Tomcat" # e.g. Public Tomcat
TOMCAT_SERVER_DISPLAY_NAME="public-tomcat" # e.g. schultz-tomcat
SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
declare -a failed_restart_server_list
for server in ${TOMCAT_HOST_LIST[@]}; do
    if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
        failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
    fi
done
if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
    EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
fi

echo "Restarting pancan-tomcat servers..."
TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
TOMCAT_HOST_USERNAME=cbioportal_importer
TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_pancan_tomcat_restarts_key
TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/pancan-tomcat-restart
TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Pancan Tomcat" # e.g. Public Tomcat
TOMCAT_SERVER_DISPLAY_NAME="pancan-tomcat" # e.g. schultz-tomcat
SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
declare -a failed_restart_server_list
for server in ${TOMCAT_HOST_LIST[@]}; do
    if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
        failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
    fi
done
if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
    EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
fi



echo "Removing gene data files downloaded..."
rm $tmp/Homo_sapiens.gene_info
rm $tmp/ref_GRCh38.p12_top_level.gff3
