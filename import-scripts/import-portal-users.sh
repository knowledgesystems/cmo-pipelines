#!/bin/bash

# we need this file for the tomcat restart funcions
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

# email_list is referenced in dmp-import-vars-funcions.sh
# in event tomcat server cannot be restarted
email_list="cbioportal-pipelines@cbio.mskcc.org"

USERSDASHILOGFILENAME="$PORTAL_HOME/logs/import-user-dashi-gdac.log"
USERSDASHI2LOGFILENAME="$PORTAL_HOME/logs/import-user-dashi2.log"
CANCERSTUDIESLOGFILENAME="$PORTAL_HOME/logs/update-studies-dashi-gdac.log"

echo "### Starting import" >> "$USERSDASHILOGFILENAME"
date >> "$USERSDASHILOGFILENAME"
$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true >> "$USERSDASHILOGFILENAME" 2>&1

echo "### Starting import" >> "$USERSDASHI2LOGFILENAME"
date >> "$USERSDASHI2LOGFILENAME"
$PYTHON_BINARY $PORTAL_HOME/scripts/importUsers.py --port 3306 --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi2 --send-email-confirm true >> "$USERSDASHI2LOGFILENAME" 2>&1

echo "### Starting import" >> "$CANCERSTUDIESLOGFILENAME"
date >> "$CANCERSTUDIESLOGFILENAME"
$PYTHON_BINARY $PORTAL_HOME/scripts/updateCancerStudies.py --secrets-file $PORTAL_DATA_HOME/portal-configuration/google-docs/client_secrets.json --creds-file $PORTAL_DATA_HOME/portal-configuration/google-docs/creds.dat --properties-file $PORTAL_HOME/cbio-portal-data/portal-configuration/properties/import-users/portal.properties.dashi.gdac --send-email-confirm true >> "$CANCERSTUDIESLOGFILENAME" 2>&1

restartMSKTomcats
restartSchultzTomcats
