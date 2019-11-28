#!/bin/bash

tmp=$PORTAL_HOME/tmp/update-gene-data
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbio.mskcc.org"
TRUSTSTORE_PASSWORD=`cat $AWS_SSL_TRUSTSTORE_PASSWORD_FILE`
GENE_DATA_UPDATER_JAR_FILENAME="$PORTAL_HOME/lib/gene_data_updater.jar"
JAVA_GENE_DATA_UPDATER_ARGS="$JAVA_PROXY_ARGS -jar $GENE_DATA_UPDATER_JAR_FILENAME"
JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$AWS_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"

function attempt_wget_file {
    target_file_url=$1
    wget $target_file_url
    WGET_STATUS=$?
    if [ $WGET_STATUS -ne "0" ] ; then
        echo Error: download failed : wget $target_file_url
        exit 1
    fi
}

function attempt_gunzip_file {
    target_file=$1
    gunzip $target_file
    GUNZIP_STATUS=$?
    if [ $GUNZIP_STATUS -ne "0" ] ; then
        echo Error: unzip failed : gunzip $target_file
        exit 1
    fi
}

echo "Updating pipelines configuration repository..."
cd $PIPELINES_CONFIG_HOME; git reset --hard ; git checkout master ; git fetch origin master ; git pull origin master

if [ -d $tmp ] ; then
    cd $tmp
else
    echo "Error:: tmp directory ($tmp) does not exist"
    exit 1
fi
echo "Downloading gene data resources from NCBI..."
export http_proxy='http://jxi2.mskcc.org:8080'
attempt_wget_file "ftp.ncbi.nih.gov:gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz"
attempt_wget_file "ftp.ncbi.nih.gov:/genomes/Homo_sapiens/GFF/ref_GRCh38.p12_top_level.gff3.gz"
attempt_gunzip_file "$tmp/Homo_sapiens.gene_info.gz"
attempt_gunzip_file "$tmp/ref_GRCh38.p12_top_level.gff3.gz"
echo "Finished!"

sed -i 's/^#//; s/\"//' $tmp/Homo_sapiens.gene_info
if [[ ! -f $tmp/Homo_sapiens.gene_info || $(wc -l < $tmp/Homo_sapiens.gene_info) -eq 0 ]]; then
    echo "Error downloading Homo_sapiens.gene_info from NCBI. Exiting..."
    exit 1
fi

if [[ ! -f $tmp/ref_GRCh38.p12_top_level.gff3 || $(wc -l < $tmp/ref_GRCh38.p12_top_level.gff3) -eq 0 ]]; then
    echo "Error downloading ref_GRCh38.p12_top_level.gff3 from NCBI. Exiting..."
    exit 1
fi


function runGeneUpdatePipeline {
    DATABASE_NAME=$1
    USE_SSL=$2
    if [ -z $DATABASE_NAME ]; then
        echo "Database name must be provided! Exiting..."
        exit 1
    fi
    export DATABASE_NAME=$DATABASE_NAME

    JAVA_EXTRA_ARGS=""
    if [[ ! -z "$USE_SSL" && "$USE_SSL" == "true" ]] ; then
        echo "Using secure connection to database $DATABASE_NAME"
        JAVA_EXTRA_ARGS=$JAVA_SSL_ARGS
    fi

    echo "Starting gene data update job on database: $DATABASE_NAME"
    $JAVA_BINARY $JAVA_EXTRA_ARGS $JAVA_GENE_DATA_UPDATER_ARGS -d $tmp/Homo_sapiens.gene_info -l $tmp/ref_GRCh38.p12_top_level.gff3 -n $tmp/gene-update-notification.txt
    if [ $? -ne 0 ]; then
        echo "Error updating gene data"
        echo -e "Error updating gene data." | mail -s "Gene Data Update Failure: $DATABASE_NAME" $PIPELINES_EMAIL_LIST
    else
        echo "Success!"
        echo "Emailing results to email list..."
        if [[ -f $tmp/gene-update-notification.txt && $(wc -l < $tmp/gene-update-notification.txt) -gt 0 ]]; then
            cat $tmp/gene-update-notification.txt | mail -s "Gene Data Update Results: $DATABASE_NAME" $PIPELINES_EMAIL_LIST
        else
            EMAIL_BODY="Error loading $tmp/gene-update-notification.txt"
            echo -e $EMAIL_BODY | mail -s "Gene Data Update Results: $DATABASE_NAME" $PIPELINES_EMAIL_LIST
        fi
    fi
}

# run gene update pipeline for each MSK pipelines database
export SPRING_CONFIG_LOCATION=$PIPELINES_CONFIG_HOME/properties/update-gene/application.properties
runGeneUpdatePipeline "cgds_gdac"
runGeneUpdatePipeline "cgds_public"
runGeneUpdatePipeline "cgds_triage"
runGeneUpdatePipeline "cgds_genie"

# run gene update for AWS gdac database
export SPRING_CONFIG_LOCATION=$PIPELINES_CONFIG_HOME/properties/update-gene/application-aws-gdac.properties
runGeneUpdatePipeline "cgds_gdac" "true"

# run gene update for AWS public database
export SPRING_CONFIG_LOCATION=$PIPELINES_CONFIG_HOME/properties/update-gene/application-aws-public.properties
runGeneUpdatePipeline "public_test"

echo "Restarting triage-tomcat server..."
TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Triage Tomcat 7"
TOMCAT_SERVER_DISPLAY_NAME="triage-tomcat7"
if ! /usr/bin/sudo /etc/init.d/triage-tomcat7 restart ; then
    EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server failed"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $PIPELINES_EMAIL_LIST
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
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $PIPELINES_EMAIL_LIST
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
    echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $PIPELINES_EMAIL_LIST
fi

echo "Removing gene data files downloaded..."
rm $tmp/Homo_sapiens.gene_info
rm $tmp/ref_GRCh38.p12_top_level.gff3
