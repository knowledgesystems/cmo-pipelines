#!/bin/bash

CLIENT_SECRETS_FILE="$PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json"
CLIENT_CREDENTIALS_FILE="$PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat"

$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $CLIENT_SECRETS_FILE --creds-file $CLIENT_CREDENTIALS_FILE --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi.gdac --send-email-confirm true > $PORTAL_HOME/logs/import-user-dashi-gdac.log

$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $CLIENT_SECRETS_FILE --creds-file $CLIENT_CREDENTIALS_FILE --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi2 --send-email-confirm true > $PORTAL_HOME/logs/import-user-dashi2.log

$PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $CLIENT_SECRETS_FILE --creds-file $CLIENT_CREDENTIALS_FILE --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi.gdac --send-email-confirm true > $PORTAL_HOME/logs/update-studies-dashi-gdac.log
