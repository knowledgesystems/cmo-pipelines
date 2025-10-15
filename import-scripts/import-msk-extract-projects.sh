#!/bin/bash

echo $(date)

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

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

# Get the current production database color
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/get_database_currently_in_production.sh"
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_database_update_tools.properties"
current_production_database_color=$($GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
destination_database_color="unset"
if [ ${current_production_database_color:0:4} == "blue" ] ; then
    destination_database_color="green"
fi
if [ ${current_production_database_color:0:5} == "green" ] ; then
    destination_database_color="blue"
fi
if [ "$destination_database_color" == "unset" ] ; then
    echo "Error during determination of the destination database color" >&2
    exit 1
fi

DATA_SOURCE_MANAGER_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/data_source_repo_clone_manager.sh"
DATA_SOURCE_MANAGER_CONFIG_FILEPATH="$PORTAL_HOME/pipelines-credentials/importer-data-source-manager-config.yaml"
MSK_IMPORTER_JAR_FILENAME="/data/portal-cron/lib/msk-dmp-$destination_database_color-importer.jar"
MSK_JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS $JAVA_DD_AGENT_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$MSK_DMP_TMPDIR -ea -cp $MSK_IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
# ROB : TODO : refactor the next line so that repo fetching and cleaning is independent of the importer jar (which requires the embedded database name to exist in order to function)
IMPORT_FAIL=0
ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
DATA_SOURCES_TO_BE_FETCHED="extract-projects"
extract_projects_notification_file=$(mktemp $tmp/import-msk-extract-projects-notification.$now.XXXXXX)

# grab data from extract-projects repos
$DATA_SOURCE_MANAGER_SCRIPT_FILEPATH $DATA_SOURCE_MANAGER_CONFIG_FILEPATH pull $DATA_SOURCES_TO_BE_FETCHED

# import data into msk portal
$JAVA_BINARY -Xmx64g $MSK_JAVA_IMPORTER_ARGS --update-study-data --portal extract-projects-to-msk-portal --notification-file $extract_projects_notification_file --oncotree-version $ONCOTREE_VERSION_TO_USE --transcript-overrides-source mskcc --disable-redcap-export
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

# clean up extract-projects repo and send notification file
$DATA_SOURCE_MANAGER_SCRIPT_FILEPATH $DATA_SOURCE_MANAGER_CONFIG_FILEPATH cleanup $DATA_SOURCES_TO_BE_FETCHED
EMAIL_BODY=`cat $extract_projects_notification_file`
echo -e "The following Extract projects have been added or updated in the MSK cBioPortal:\n\n$EMAIL_BODY" | mail -r "cbioportal-pipelines@cbioportal.org" -s "Updates to MSK cBioPortal (Extract Projects)" $PIPELINES_EMAIL_LIST
