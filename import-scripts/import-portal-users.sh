#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-portal-users.lock"
PURGE_DEACTIVATED_LDAP_USERS_FLOCK_FILEPATH="/data/portal-cron/cron-lock/purge_deactivated_ldap_users.lock"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        exit 0
    fi

    # abort if purge_deactivated_ldap_users.sh is currently executing
    if ! flock --nonblock --exclusive $purge_deactivated_ldap_users_flock_fd ; then
        exit 0
    fi

    USERSGENIELOGFILENAME="$PORTAL_HOME/logs/import-user-dashi-genie.log"
    USERSDASHILOGFILENAME="$PORTAL_HOME/logs/import-user-dashi-gdac.log"

    echo "### Starting import" >> "$USERSDASHILOGFILENAME"
    date >> "$USERSDASHILOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true >> "$USERSDASHILOGFILENAME" 2>&1
    CGDS_GDAC_IMPORT_STATUS=$?

    echo "### Starting import" >> "$USERSGENIELOGFILENAME"
    date >> "$USERSGENIELOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.genie.aws --send-email-confirm true --sender GENIE --ssl-ca $PORTAL_HOME/pipelines-credentials/pipelines-genie-db-aws-rds-combined-ca-bundle.pem >> "$USERSGENIELOGFILENAME" 2>&1
    CGDS_GENIE_IMPORT_STATUS=$?

    echo "### Starting import" >> "$USERSGENIELOGFILENAME"
    date >> "$USERSGENIELOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PIPELINES_CONFIG_HOME/google-docs/client_secrets.json --creds-file $PIPELINES_CONFIG_HOME/google-docs/creds.dat --properties-file $PIPELINES_CONFIG_HOME/properties/import-users/portal.properties.dashi.genie.archive --send-email-confirm false --sender GENIE --ssl-ca $PORTAL_HOME/pipelines-credentials/pipelines-genie-db-aws-rds-combined-ca-bundle.pem >> "$USERSGENIELOGFILENAME" 2>&1
    CGDS_GENIE_ARCHIVE_IMPORT_STATUS=$?
    
    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbio.mskcc.org"
    FAILED_DATABASES=""
    if [[ $CGDS_GDAC_IMPORT_STATUS -ne 0 ]] ; then
        FAILED_DATABASES="$FAILED_DATABASES cgds_gdac"
    fi
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

) {my_flock_fd}>$MY_FLOCK_FILEPATH {purge_deactivated_ldap_users_flock_fd}>$PURGE_DEACTIVATED_LDAP_USERS_FLOCK_FILEPATH

exit 0
