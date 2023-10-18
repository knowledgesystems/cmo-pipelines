#!/bin/bash

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh

echo $(date)

if ! [ -d "$MSK_DMP_TMPDIR" ] ; then
    if ! mkdir -p "$MSK_DMP_TMPDIR" ; then
        echo "Error : could not create tmp directory '$MSK_DMP_TMPDIR'" >&2
        exit 1
    fi
fi
if [[ -d "$MSK_DMP_TMPDIR" && "$MSK_DMP_TMPDIR" != "/" ]] ; then
    rm -rf "$MSK_DMP_TMPDIR"/*
fi

if [ -z $JAVA_BINARY ] | [ -z $GIT_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_SHAHLAB_DATA_HOME ] | [ -z $MSK_SPECTRUM_COHORT_DATA_HOME ] ; then
    message="test could not run update-msk-spectrum-cohort.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_SHAHLAB_DATA_HOME, ...)"
    echo $message
    echo -e "$message" |  mail -s "update-msk-spectrum-cohort failed to run." $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$message"
    exit 2
fi

source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

IMPORT_FAIL=0
mskspectrum_notification_file=$(mktemp $MSK_DMP_TMPDIR/mskspectrum-portal-update-notification.$now.XXXXXX)
# update msk-spectrum github repo
fetch_updates_in_data_sources "datahub_shahlab"

# update mskspectrum cohort in portal
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal msk-spectrum-portal --notification-file $mskspectrum_notification_file --oncotree-version $ONCOTREE_VERSION_TO_USE --transcript-overrides-source mskcc --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "MSKSPECTRUM update failed!"
    IMPORT_FAIL=1
    EMAIL_BODY="MSKSPECTRUM update failed"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Update failure: MSKSPECTRUM" $CMO_EMAIL_LIST
fi

# get num studies updated
if [[ $? -eq 0 && -f "$TMP_DIRECTORY/num_studies_updated.txt" ]]; then
    num_studies_updated=`cat $TMP_DIRECTORY/num_studies_updated.txt`
else
    num_studies_updated=0
fi

# clear persistence cache
if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
    echo "'$num_studies_updated' studies have been updated, clearing persistence cache for msk portals..."
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk update-msk-spectrum-cohort.sh
    fi
else
    echo "No studies have been updated, not clearing persistence cache for msk portals..."
fi

# clean up msk-spectrum repo and send notification file
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/datahub_shahlab
$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal msk-spectrum-portal --notification-file "$mskspectrum_notification_file"
