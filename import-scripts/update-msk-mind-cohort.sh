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

if [ -z $JAVA_BINARY ] | [ -z $GIT_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_MIND_DATA_HOME ] | [ -z $MSK_EXTRACT_COHORT_DATA_HOME ] ; then
    message="test could not run update-msk-mind-cohort.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_MIND_DATA_HOME, ...)"
    echo $message
    echo -e "$message" |  mail -s "update-msk-mind-cohort failed to run." $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$message"
    exit 2
fi

source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

IMPORT_FAIL=0
mskextract_notification_file=$(mktemp $MSK_DMP_TMPDIR/mskextract-portal-update-notification.$now.XXXXXX)
# update msk-mind github repo
fetch_updates_in_data_sources "msk-mind-datahub"

# fetch ddp timeline data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for MSKEXTRACT"
mskextract_dmp_pids_file=$MSK_DMP_TMPDIR/mskextract_patient_list.txt
grep -v "^#" $MSK_EXTRACT_COHORT_DATA_HOME/data_clinical_patient.txt | cut -f1 | grep -v "PATIENT_ID" | sort | uniq > $mskextract_dmp_pids_file
MSKEXTRACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $mskextract_dmp_pids_file)
if [ $MSKEXTRACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    MSKEXTRACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskextract -p $mskextract_dmp_pids_file -f diagnosis,radiation,chemotherapy,surgery,survival -o $MSK_EXTRACT_COHORT_DATA_HOME -r $MSKEXTRACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/msk-mind
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKEXTRACT DDP Timeline Fetch"
else
    echo "commit ddp timeline data for MSKEXTRACT"
    cd $MSK_MIND_DATA_HOME ; rm -f $MSK_EXTRACT_COHORT_DATA_HOME/data_clinical_ddp.txt ; $GIT_BINARY add $MSK_EXTRACT_COHORT_DATA_HOME/data_timeline* ; $GIT_BINARY commit -m "Latest MSKEXTRACT DDP timeline data" ; $GIT_BINARY push origin
fi

# update mskextract cohort in portal
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal msk-mind-portal --notification-file $mskextract_notification_file --oncotree-version $ONCOTREE_VERSION_TO_USE --transcript-overrides-source mskcc --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "MSKEXTRACT update failed!"
    IMPORT_FAIL=1
    EMAIL_BODY="MSKEXTRACT update failed"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Update failure: MSKEXTRACT" $CMO_EMAIL_LIST
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
        sendClearCacheFailureMessage msk update-msk-mind-cohort.sh
    fi
else
    echo "No studies have been updated, not clearing persistence cache for msk portals..."
fi

# clean up msk-mind repo and send notification file
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/msk-mind
$JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal msk-mind-portal --notification-file "$mskextract_notification_file"
