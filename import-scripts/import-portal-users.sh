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
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true >> "$USERSDASHILOGFILENAME" 2>&1

    echo "### Starting import" >> "$USERSGENIELOGFILENAME"
    date >> "$USERSGENIELOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.genie --send-email-confirm true --sender GENIE >> "$USERSGENIELOGFILENAME" 2>&1
) {my_flock_fd}>$MY_FLOCK_FILEPATH {purge_deactivated_ldap_users_flock_fd}>$PURGE_DEACTIVATED_LDAP_USERS_FLOCK_FILEPATH

exit 0
