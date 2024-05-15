#!/bin/bash

if [ ! -f $PORTAL_HOME/scripts/automation-environment.sh ] ; then
  echo "`date`: Unable to locate automation_env, exiting..."
  exit 1
fi

source $PORTAL_HOME/scripts/automation-environment.sh

$PORTAL_HOME/scripts/authenticate_service_account.sh eks
aws s3 sync s3://cdm-deliverable/msk-chord $MSK_CHORD_DATA_HOME --profile saml
if [ $? -ne 0 ] ; then
  echo "`date`: Failed to download CDM annotated data from s3://cdm-deliverable, exiting..."
fi
