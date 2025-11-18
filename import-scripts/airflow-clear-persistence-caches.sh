#!/bin/bash

# Script for clearing cBioPortal persistence cache from Airflow
# This script is only called for the triage portal, which still uses the MySQL database.
# For the ClickHouse portals, clearing the persistence cache is done as part of the transfer deployment step

PORTAL_DATABASE=$1
PORTAL_SCRIPTS_DIRECTORY=$2

source "$PORTAL_SCRIPTS_DIRECTORY/automation-environment.sh"
source "$PORTAL_SCRIPTS_DIRECTORY/clear-persistence-cache-shell-functions.sh"

echo "Clearing persistence caches for $PORTAL_DATABASE portal..."
case "$PORTAL_DATABASE" in
    triage)
        clearPersistenceCachesForTriagePortals
        ;;
    *)
        echo "Unrecognized portal database: $PORTAL_DATABASE" >&2
        exit 1
        ;;
esac
echo "Persistence caches for $PORTAL_DATABASE cleared successfully"
