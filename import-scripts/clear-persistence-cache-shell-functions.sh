#!/usr/bin/env bash

if ! [ -n "$PORTAL_HOME" ] ; then
    echo "Error : clear-cache-shell-functions.sh cannot be run without setting the PORTAL_HOME environment variable. (Use automation-environment.sh)"
    exit 1
fi

source $PORTAL_HOME/scripts/slack-message-functions.sh

# Function for alerting slack channel of clear cache failures
function sendClearCacheFailureMessage() {
    CACHE_GROUP_NAME="$1"
    SOURCE_SCRIPT_NAME="$2"
    EMAIL_RECIPIENT="cbioportal-pipelines@cbioportal.org"
    EMAIL_SUBJECT="import failure resetting cache $CACHE_GROUP_NAME"
    EMAIL_BODY="Imported studies may not be visible in one or more of the $CACHE_GROUP_NAME portals.\n\nImport script '$SOURCE_SCRIPT_NAME' attempted to clear/reset the persistence cache for $CACHE_GROUP_NAME portals and a failure was reported. Until a successful cache clearing occurs for these portals, studies which were successfully imported may not yet be visible.\n"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$EMAIL_SUBJECT" "$EMAIL_RECIPIENT"
}

# returns the number of portals for which cache reset failed (0 = all succeeded)
function clearPersistenceCachesForPortals() {
    portal_list=$1
    exit_status=0
    for portal in $portal_list; do
        if ! $PORTAL_HOME/scripts/clear_cbioportal_persistence_cache.sh $portal ; then
            exit_status=$(($exit_status + 1))
        fi
    done
    return $exit_status
}

function clearPersistenceCachesForMskPortals() {
    all_msk_portals="msk msk-beta"
    clearPersistenceCachesForPortals "$all_msk_portals"
    send_slack_message_to_channel "#msk-pipeline-logs" "string" "MSK portal persistence cache was cleared :recycle:"
}

function clearPersistenceCachesForExternalPortals() {
    all_external_portals="private sclc"
    clearPersistenceCachesForPortals "$all_external_portals"
}

function clearPersistenceCachesForTriagePortals() {
    all_triage_portals="triage"
    clearPersistenceCachesForPortals "$all_triage_portals"
}

function clearPersistenceCachesForHgncPortals() {
    all_hgnc_portals="hgnc"
    clearPersistenceCachesForPortals "$all_hgnc_portals"
}

function clearPersistenceCachesForHgnc1938Portals() {
    all_hgnc_1938_portals="msk-beta"
    clearPersistenceCachesForPortals "$all_hgnc_1938_portals"
}

function clearPersistenceCachesForPublicPortals() {
    all_public_portals="public"
    clearPersistenceCachesForPortals "$all_public_portals"
}

function clearPersistenceCachesForGeniePortals() {
    all_genie_portals="genie-public genie-private"
    clearPersistenceCachesForPortals "$all_genie_portals"
}

function clearPersistenceCachesForGenieArchivePortals() {
    all_genie_archive_portals="genie-archive"
    clearPersistenceCachesForPortals "$all_genie_archive_portals"
}

function clearPersistenceCachesForCrdcPortals() {
    all_crdc_portals="crdc"
    clearPersistenceCachesForPortals "$all_crdc_portals"
}
