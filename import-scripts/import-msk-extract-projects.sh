#!/bin/bash

echo $(date)

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh

if [ -z $JAVA_BINARY ] | [ -z $GIT_BINARY ] | [ -z $PORTAL_HOME ] ; then
    message="import-msk-extract-projects.sh cannot be run without setting JAVA_BINARY, GIT_BINARY, PORTAL_HOME...)"
    echo $message
    echo -e "$message" |  mail -s "import-msk-extract-projects.sh failed to run." $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$message"
    exit 2
fi

tmp=$PORTAL_HOME/tmp/import-msk-extract-projects
if ! [ -d "$tmp" ] ; then
    if ! mkdir -p "$tmp" ; then
        echo "Error : could not create tmp directory '$tmp'" >&2
        exit 1
    fi
fi
if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi

source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

IMPORT_FAIL=0
ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
extract_projects_notification_file=$(mktemp $tmp/import-msk-extract-projects-notification.$now.XXXXXX)
# grab data from extract-projects repos
fetch_updates_in_data_sources "extract-projects"

# import data into msk portal
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal extract-projects-to-msk-portal --notification-file $extract_projects_notification_file --oncotree-version $ONCOTREE_VERSION_TO_USE --transcript-overrides-source mskcc --disable-redcap-export
if [ $? -gt 0 ]; then
    echo "MSK Extract projects import failed!"
    IMPORT_FAIL=1
    EMAIL_BODY="Extract projects import failed"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Update failure: MSK Extract Projects" $PIPELINES_EMAIL_LIST
fi

# get num studies updated
if [[ $IMPORT_FAIL -eq 0 && -f "$TMP_DIRECTORY/num_studies_updated.txt" ]]; then
    num_studies_updated=`cat $TMP_DIRECTORY/num_studies_updated.txt`
else
    num_studies_updated=0
fi

# clear persistence cache
if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
    echo "'$num_studies_updated' studies have been updated, clearing persistence cache for msk portals..."
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk import-msk-extract-projects.sh
    fi
else
    echo "No studies have been updated, not clearing persistence cache for msk portals..."
fi

# clean up extract-projects repo and send notification file
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/extract-projects
EMAIL_BODY=`cat $extract_projects_notification_file`
echo -e "The following Extract projects have been added or update in the MSK cBioPortal:\n\n$EMAIL_BODY" | mail -r "cbioportal-pipelines@cbioportal.org" -s "Updates to MSK cBioPortal (Extract Projects)" $PIPELINES_EMAIL_LIST
