#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-portal-users.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        exit 0
    fi

    USERSGENIELOGFILENAME="$PORTAL_HOME/logs/import-user-dashi-genie.log"
    GMAIL_USERNAME=`grep gmail_username $GMAIL_CREDS_FILE | sed 's/^.*=//g'`
    GMAIL_PASSWORD=`grep gmail_password $GMAIL_CREDS_FILE | sed 's/^.*=//g'`

    echo "### Starting import" >> "$USERSGENIELOGFILENAME"
    date >> "$USERSGENIELOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.genie.aws --send-email-confirm false --sender GENIE --ssl-ca $PORTAL_HOME/pipelines-credentials/pipelines-genie-db-aws-rds-combined-ca-bundle.pem --gmail-username $GMAIL_USERNAME --gmail-password $GMAIL_PASSWORD >> "$USERSGENIELOGFILENAME" 2>&1
    CGDS_GENIE_IMPORT_STATUS=$?

    echo "### Starting import" >> "$USERSGENIELOGFILENAME"
    date >> "$USERSGENIELOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.genie.archive --send-email-confirm false --sender GENIE --ssl-ca $PORTAL_HOME/pipelines-credentials/pipelines-genie-db-aws-rds-combined-ca-bundle.pem --gmail-username $GMAIL_USERNAME --gmail-password $GMAIL_PASSWORD >> "$USERSGENIELOGFILENAME" 2>&1
    CGDS_GENIE_ARCHIVE_IMPORT_STATUS=$?
    
    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    FAILED_DATABASES=""
    if [[ $CGDS_GENIE_IMPORT_STATUS -ne 0 ]] ; then
        FAILED_DATABASES="$FAILED_DATABASES cgds_genie"
    fi
    if [[ $CGDS_GENIE_ARCHIVE_IMPORT_STATUS -ne 0 ]] ; then
        FAILED_DATABASES="$FAILED_DATABASES genie_archive"
    fi
    if ! [ -z "$FAILED_DATABASES" ] ; then
        MINUTES_NOW=$(date "+%M")
        if [ $MINUTES_NOW == "00" ] ; then
            EMAIL_BODY="Failure importing users into$FAILED_DATABASES"
            echo -e $EMAIL_BODY | mail -s "import-portal-users failure :$FAILED_DATABASES" $PIPELINES_EMAIL_LIST
        fi
    fi

) {my_flock_fd}>$MY_FLOCK_FILEPATH

exit 0
