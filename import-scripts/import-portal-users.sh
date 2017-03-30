#!/bin/bash

$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi.gdac --send-email-confirm true > $PORTAL_HOME/logs/import-user-dashi-gdac.log

$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi2 --send-email-confirm true > $PORTAL_HOME/logs/import-user-dashi2.log

$PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/portal-cron/portal.properties.dashi.gdac --send-email-confirm true > $PORTAL_HOME/logs/update-studies-dashi-gdac.log
