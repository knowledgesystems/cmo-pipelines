#!/usr/bin/env bash

DBS=("cgds_triage" "keycloak" "redcap")
LOCAL_BACKUP_DIR=/data/mysql-dumps

if [ -z "$PORTAL_HOME" ] ; then
    export PORTAL_HOME=/data/portal-cron
fi
source "$PORTAL_HOME/scripts/slack-message-functions.sh"

for db in ${DBS[@]}; do
  DUMP_FAILURE=0
  echo "Backing up: '${db}'"

  PORTAL_INFO_TABLE=$(mysql --login-path=mysql_localhost -e "SHOW TABLES LIKE 'info';" $db | tail -n1)
  if [ ! -z "$PORTAL_INFO_TABLE" ]; then
    DB_SCHEMA_VERSION=".v$(mysql --login-path=mysql_localhost -e "SELECT db_schema_version FROM info;" $db | tail -n1)"
  else
    DB_SCHEMA_VERSION=""
  fi
  SQLDUMP_FILENAME=${db}.$(date +%Y%m%d)${DB_SCHEMA_VERSION}.sql.gz
  SQLDUMP_FULLPATH=${LOCAL_BACKUP_DIR}/$SQLDUMP_FILENAME

  # The return status of a pipeline is the exit status of the last command, unless the pipefail option is enabled. If pipefail is enabled, the pipeline's return status is the value of the last (rightmost) command to exit with a non-zero status, or zero if all commands exit successfully.
  $(set -o pipefail && mysqldump --login-path=mysql_localhost --quick $db | gzip > $SQLDUMP_FULLPATH)
  if [ $? -eq 0 ]; then
    echo "Successfully dumped: '${SQLDUMP_FILENAME}'"
    /data/portal-cron/git-repos/portal-configuration/eks-cluster/pipelines/authenticate_service_account.sh eks
    aws s3 cp ${SQLDUMP_FULLPATH} s3://cbioportal-backups/${SQLDUMP_FILENAME} --profile automation_eks
    if [ $? -ne 0 ]; then
    echo "ERROR: failed to cp '${SQLDUMP_FILENAME}' to S3"
        DUMP_FAILURE=1
    fi

    # delete files older than 14 days that match this pattern
    echo "Deleting files in '$LOCAL_BACKUP_DIR' that match '$db.[0-9]{8}*.sql.gz' and are >= 14 days old."
    find $LOCAL_BACKUP_DIR -type f -mtime +13 -name "$db.[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]*.sql.gz" -delete
  else
    echo "ERROR: failed to dump '${SQLDUMP_FILENAME}'"
    DUMP_FAILURE=1
    echo "Deleting invalid dump file '${SQLDUMP_FILENAME}'"
    rm -r "${SQLDUMP_FULLPATH}"
  fi

  if [ $DUMP_FAILURE -eq 0 ]; then
    echo "Dump was successful"
    send_slack_message_to_channel "#mskcc-sysadmin" "string" "eks-pipelines backed up local db (${db})"
  else
    echo "Dump failed"
    send_slack_message_to_channel "#mskcc-sysadmin" "string" "ERROR: eks-pipelines failed to back up local db :fire: (${db})"
  fi
done
