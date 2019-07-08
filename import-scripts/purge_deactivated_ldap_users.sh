#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/purge_deactivated_ldap_users.lock"
IMPORT_PORTAL_USERS_FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-portal-users.lock"

(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    # wait for import_portal_users to complete and release lock if it is running
    flock --exclusive $import_portal_users_flock_fd

    LDAPLOGFILENAME="$PORTAL_HOME/logs/purge-deactivated-ldap-users.log"
    LDAPTMPDIRECTORY="$PORTAL_HOME/tmp/ldap"

    echo "### Removing deactivated LDAP users from Google spreadsheet and database" >> "$LDAPLOGFILENAME"
    date >> "$LDAPLOGFILENAME"
    $PYTHON_BINARY $PORTAL_HOME/scripts/purge_deactivated_ldap_users.py --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat  --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.gdac.ldap --tmp-directory $LDAPTMPDIRECTORY >> "$LDAPLOGFILENAME" 2>&1
) {my_flock_fd}>$MY_FLOCK_FILEPATH {import_portal_users_flock_fd}>$IMPORT_PORTAL_USERS_FLOCK_FILEPATH

exit 0
