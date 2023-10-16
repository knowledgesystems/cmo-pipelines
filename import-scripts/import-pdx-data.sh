#!/bin/bash

echo $(date)

PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh
# PIPELINES_EMAIL_LIST receives low level emails (fail to recache oncotree, fail to clear persistence cache, ...)
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
# PDX_EMAIL_LIST receives a daily summary email of import statistics and problems
PDX_EMAIL_LIST="cbioportal-pdx-importer@cbioportal.org"
CRDB_PDX_TMPDIR=/data/portal-cron/tmp/import-cron-pdx-msk
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
shopt -s nullglob
declare -a study_list

# Functions

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"$MESSAGE\", \"icon_emoji\": \":tired_face:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# Function for alerting slack channel of successful imports
function sendSuccessMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"$MESSAGE\", \"icon_emoji\": \":tada:\"}" $SLACK_PIPELINES_MONITOR_URL
}

function purgeOrigFilesUnderDirectory {
    search_dir=$1
    find "$search_dir" -name "*.orig" -delete
}

function addRemoveFilesUnderDirectory {
    search_dir=$1
    purgeOrigFilesUnderDirectory "$search_dir"
    ( cd $search_dir ; $GIT_BINARY add -A . )
}

function commitAllRepositoryChanges {
    repository_subdirectory=$1
    changeset_log_message=$2
    $GIT_BINARY -C "$repository_subdirectory" commit -m "$changeset_log_message"
}

function pushAllChangesets {
    repository_subdirectory=$1
    $GIT_BINARY -C "$repository_subdirectory" push
}


function purgeAllFileModifications {
    repository_subdirectory=$1
    $GIT_BINARY -C "$repository_subdirectory" reset HEAD --hard
}

function cleanAllUntrackedFiles {
    repository_subdirectory=$1
    $GIT_BINARY -C "$repository_subdirectory" clean -fd
}

function cleanUpEntireRepository {
    repository_subdirectory=$1
    purgeAllFileModifications "$repo_subdirectory"
    cleanAllUntrackedFiles "$repo_subdirectory"
}

function revertModifiedFilesUnderDirectory {
    repository_subdirectory=$1
    $GIT_BINARY -C "$repository_subdirectory" checkout -- .
}

function find_trigger_files_for_existing_studies {
    suffix=$1
    suffix_length=${#suffix}
    unset study_list
    study_list_index=0
    for filepath in $CRDB_PDX_TMPDIR/*${suffix} ; do
        filename="${filepath##*/}"
        filename_length=${#filename}
        study_directory_length=$(( $filename_length - $suffix_length ))
        study_directory=${filename:0:$study_directory_length}
        if [ -d $PDX_DATA_HOME/$study_directory ] ; then
            study_list[$study_list_index]=$study_directory
            study_list_index=$(( $study_list_index + 1 ))
        else
            echo "error : trigger file $filename found for non-existent study : $PDX_HOME/$study_directory"
        fi
    done
}

function find_studies_to_be_committed {
    find_trigger_files_for_existing_studies "_commit_triggerfile"
}

function find_studies_to_be_reverted {
    find_trigger_files_for_existing_studies "_revert_triggerfile"
}

# set up enivornment variables and temp directory
if ! [ -f $PATH_TO_AUTOMATION_SCRIPT ] ; then
    message="automation-environment.sh could not be found, exiting..."
    echo ${message}
    echo -e "${message}" |  mail -s "import-pdx-data failed to run." $PIPELINES_EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "CRDB PDX Pipeline Failure"
    exit 2
fi

source $PATH_TO_AUTOMATION_SCRIPT

if [ -z "$PORTAL_HOME" ] | [ -z "$BIC_LEGACY_DATA_HOME" ] | [ -z "$CMO_ARGOS_DATA_HOME" ] | [ -z "$PRIVATE_DATA_HOME" ] | [ -z "$PDX_DATA_HOME" ] | [ -z "$GIT_BINARY" ] | [ -z "$PYTHON_BINARY" ] | [ -z "$DATAHUB_DATA_HOME" ] | [ -z "$ANNOTATOR_JAR" ] | [ -z "$CASE_LIST_CONFIG_FILE"  ] ; then
    message="could not run import-pdx-data.sh: automation-environment.sh script must be run in order to set needed environment variables (like BIC_LEGACY_DATA_HOME, PDX_DATA_HOME, ANNOTATOR_JAR, CASE_LIST_CONFIG_FILE,...)"
    echo ${message}
    echo -e "${message}" |  mail -s "import-pdx-data failed to run." $PIPELINES_EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "CRDB PDX Pipeline Failure"
    exit 2
fi

source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

if [ ! -d $CRDB_PDX_TMPDIR ] ; then
    mkdir $CRDB_PDX_TMPDIR
    if [ $? -ne 0 ] ; then
        message="error : required temp directory does not exist and could not be created : $CRDB_PDX_TMPDIR"
        echo ${message}
        echo -e "${message}" |  mail -s "import-pdx-data failed to run." $PIPELINES_EMAIL_LIST
        sendFailureMessageMskPipelineLogsSlack "CRDB PDX Pipeline Failure"
        exit 2
    fi
fi
if [[ -d "$CRDB_PDX_TMPDIR" && "$CRDB_PDX_TMPDIR" != "/" ]] ; then
    rm -rf "$CRDB_PDX_TMPDIR"/*
fi

IMPORTER_JAR_LABEL=CMO
IMPORTER_JAR_FILENAME=$PORTAL_HOME/lib/msk-cmo-importer.jar
IMPORTER_DEBUG_PORT=27182
CRDB_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/crdb_fetcher.jar"
importer_notification_file=$(mktemp $CRDB_PDX_TMPDIR/importer-update-notification.$now.XXXXXX)
SLACK_PIPELINES_MONITOR_URL=`cat $SLACK_URL_FILE`
ENABLE_DEBUGGING=0
java_debug_args=""
if [ $ENABLE_DEBUGGING != "0" ] ; then
    java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$IMPORTER_DEBUG_PORT"
fi
JAVA_CRDB_FETCHER_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED -jar $CRDB_FETCHER_JAR_FILENAME"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$CRDB_PDX_TMPDIR -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
SUBSET_AND_MERGE_WARNINGS_FILENAME="subset_and_merge_pdx_studies_warnings.txt"
# status flags (set to 1 when each stage is successfully completed)
CRDB_PDX_FETCH_SUCCESS=0
CRDB_PDX_SUBSET_AND_MERGE_SUCCESS=0
BIC_MSKCC_LEGACY_DATA_SOURCE_FETCH_SUCCESS=0
CMO_ARGOS_DATA_SOURCE_FETCH_SUCCESS=0
PRIVATE_DATA_SOURCE_FETCH_SUCESS=0
PDX_DATA_SOURCE_FETCH_SUCCESS=0
DATAHUB_DATA_SOURCE_FETCH_SUCCESS=0
ALL_DATA_SOURCE_FETCH_SUCCESS=0
IMPORT_SUCCESS=0
CLEAR_PERSISTENCE_CACHE_SUCCESS=0

CDD_ONCOTREE_RECACHE_FAIL=0
if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
    # refresh cdd and oncotree cache
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
    if [ $? -gt 0 ]; then
        CDD_ONCOTREE_RECACHE_FAIL=1
        message="Failed to refresh CDD and/or ONCOTREE cache during CRDB PDX import!"
        echo $message
        echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
    fi
fi

DB_VERSION_FAIL=0
# check database version before importing anything
echo "Checking if database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ]
then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

# importer data source fetch step
echo "fetching updates from bic-mskcc-legacy repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source bic-mskcc-legacy --run-date latest --update-worksheet
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch BIC-MSKCC-LEGACY Studies From Git Failure"
else
    BIC_MSKCC_LEGACY_DATA_SOURCE_FETCH_SUCCESS=1
fi

echo "fetching updates from cmo-argos repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source cmo-argos --run-date latest --update-worksheet
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch CMO Argos Studies From Git Failure"
else
    CMO_ARGOS_DATA_SOURCE_FETCH_SUCCESS=1
fi

echo "fetching updates from private repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source private --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch Private Studies From Git Failure"
else
    PRIVATE_DATA_SOURCE_FETCH_SUCESS=1
fi

echo "fetching updates from datahub repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source datahub --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch Datahub Studies From Git Failure"
else
    DATAHUB_DATA_SOURCE_FETCH_SUCCESS=1
fi

echo "fetching updates from pdx repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source pdx --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch PDX Studies From Git Failure"
else
    PDX_DATA_SOURCE_FETCH_SUCCESS=1
fi


if [[ $BIC_MSKCC_LEGACY_DATA_SOURCE_FETCH_SUCCESS -eq 1 && $PRIVATE_DATA_SOURCE_FETCH_SUCESS -eq 1 && $PDX_DATA_SOURCE_FETCH_SUCCESS -eq 1 && $DATAHUB_DATA_SOURCE_FETCH_SUCCESS -eq 1 && $CMO_ARGOS_DATA_SOURCE_FETCH_SUCCESS -eq 1 ]] ; then
    # udpate status for email
    ALL_DATA_SOURCE_FETCH_SUCCESS=1
    echo "fetching pdx data fom crdb"
    $JAVA_BINARY $JAVA_CRDB_FETCHER_ARGS --pdx --directory $CRDB_FETCHER_PDX_HOME
    if [ $? -ne 0 ] ; then
        echo "error: crdb_pdx_fetch failed"
        sendFailureMessageMskPipelineLogsSlack "Fetch CRDB PDX Failure"
        cleanUpEntireRepository $CRDB_FETCHER_PDX_HOME
    else
        addRemoveFilesUnderDirectory $CRDB_FETCHER_PDX_HOME
        commitAllRepositoryChanges $CRDB_FETCHER_PDX_HOME "CRDB PDX Fetch"
        CRDB_PDX_FETCH_SUCCESS=1
    fi
fi

# TEMP (done): transform project_ids in test data to stable study ids (add tranform of stable id)

# construct destination studies from source studies
# call subsetting/constuction python script (add touch a trigger file for successful subset/merge) (add subroutine which creates process command) (touch needed meta files for the generated data files)
if [ $CRDB_PDX_FETCH_SUCCESS -ne 0 ] ; then
    mapping_filename="source_to_destination_mappings.txt"
    clinical_annotation_mapping_filename="clinical_annotations_mappings.txt"
    scripts_directory="$PORTAL_HOME/scripts"
    $PYTHON_BINARY $PORTAL_HOME/scripts/subset_and_merge_crdb_pdx_studies.py --mapping-file $mapping_filename --root-directory $PDX_DATA_HOME --lib $scripts_directory --data-source-directories $DATAHUB_DATA_HOME,$BIC_LEGACY_DATA_HOME,$CMO_ARGOS_DATA_HOME,$PRIVATE_DATA_HOME,$DMP_DATA_HOME --fetch-directory $CRDB_FETCHER_PDX_HOME --temp-directory $CRDB_PDX_TMPDIR --warning-file $SUBSET_AND_MERGE_WARNINGS_FILENAME --clinical-annotation-mapping-file $clinical_annotation_mapping_filename --annotator $ANNOTATOR_JAR --sample-lists-config $CASE_LIST_CONFIG_FILE
    if [ $? -ne 0 ] ; then
        echo "error: subset_and_merge_crdb_pdx_studies.py exited with non zero status"
        sendFailureMessageMskPipelineLogsSlack "CRDB PDX Subset-And-Merge Script Failure"
        cleanUpEntireRepository $CRDB_FETCHER_PDX_HOME
    else
        CRDB_PDX_SUBSET_AND_MERGE_SUCCESS=1
    fi
fi

if [ $CRDB_PDX_SUBSET_AND_MERGE_SUCCESS -ne 0 ] ; then
    # check trigger files and do appropriate data source operations
    find_studies_to_be_reverted
    index=0
    while [ $index -lt ${#study_list} ] ; do
        revertModifiedFilesUnderDirectory "$PDX_DATA_HOME/${study_list[$index]}"
        index=$(( $index + 1 ))
    done
    find_studies_to_be_committed
    index=0
    while [ $index -lt ${#study_list} ] ; do
        addRemoveFilesUnderDirectory "$PDX_DATA_HOME/${study_list[$index]}"
        index=$(( $index + 1 ))
    done
    commitAllRepositoryChanges $CRDB_FETCHER_PDX_HOME "CRDB PDX Subset and Merge"
fi

# push changesets to data source - this will commit to them regardless of whether import succeeds, or partially succeeds, or fails
pushAllChangesets $CRDB_FETCHER_PDX_HOME

#TODO : make this smarter .. to only import if the destination study has changed (i.e. alter the spreadsheet checkmarks)
#TODO : check if we can reuse the pdx-portal column
if [ $CRDB_PDX_SUBSET_AND_MERGE_SUCCESS -ne 0 ] ; then
    # import if all went well (only if trigger file is present)
    # if the database version is correct and ALL fetches succeed, then import
    if [[ $DB_VERSION_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
        echo "importing study data to database using $IMPORTER_JAR_FILENAME ..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --update-study-data --portal crdb-pdx-portal --use-never-import --update-worksheet --notification-file "$importer_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -ne 0 ]; then
            echo "$IMPORTER_JAR_LABEL import failed!"
            EMAIL_BODY="$IMPORTER_JAR_LABEL import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: $IMPORTER_JAR_LABEL" $PIPELINES_EMAIL_LIST
            sendFailureMessageMskPipelineLogsSlack "CRDB PDX Failure During Import"
        else
            IMPORT_SUCCESS=1
        fi
        num_studies_updated=`cat $CRDB_PDX_TMPDIR/num_studies_updated.txt`
        # clear persistence cache (note : this script is constructing studies for the msk portal, including mskimpact sample data - that is why the msk portal cache is cleared)
        if [[ $IMPORT_SUCCESS -ne 0 && $num_studies_updated -gt 0 ]]; then
            echo "'$num_studies_updated' studies have been updated, clearing persistence cache for msk portal ..."
            if ! clearPersistenceCachesForMskPortals ; then
                sendClearCacheFailureMessage msk import-pdx-data.sh
            else
                CLEAR_PERSISTENCE_CACHE_SUCCESS=1
            fi
        else
            echo "No studies have been updated, not clearing persistence cache for msk portal..."
            CLEAR_PERSISTENCE_CACHE_SUCCESS=1
        fi
    fi
fi

# send appropriate email message to PDX_EMAIL_LIST
echo "sending notification email.."
EMAIL_MESSAGE_FILE="$CRDB_PDX_TMPDIR/pdx_summary_email_body.txt"
EMAIL_SUBJECT="CRDB PDX cBioPortal import failure"
rm -f $EMAIL_MESSAGE_FILE
if [ $ALL_DATA_SOURCE_FETCH_SUCCESS -eq 0 ] ; then
    echo -e "The import of CRDB PDX studies did not occur today due to a failure to update the git repositories used to hold study data." >> "$EMAIL_MESSAGE_FILE"
else
    if [ $CRDB_PDX_FETCH_SUCCESS -eq 0 ] ; then
        echo -e "The import of CRDB PDX studies did not occur today due to a failure to download PDX data from the CRDB database server." >> "$EMAIL_MESSAGE_FILE"
    else
        if [ $CRDB_PDX_SUBSET_AND_MERGE_SUCCESS -eq 0 ] ; then
            echo -e "The import of CRDB PDX studies did not occur today due to a failure during the subsetting and merging of source study data according to the source_to_destination_mappings.txt file downloaded from CRDB database server." >> "$EMAIL_MESSAGE_FILE"
        else
            if [ $IMPORT_SUCCESS -eq 0 ] ; then
                echo -e "The import of CRDB PDX studies was attempted but failed during the importing process." >> "$EMAIL_MESSAGE_FILE"
            else
                if [ $CLEAR_PERSISTENCE_CACHE_SUCCESS -eq 0 ] ; then
                    echo -e "The import of CRDB PDX studies completed successfully, however due to a technical problem the website may not display the latest data." >> "$EMAIL_MESSAGE_FILE"
                else
                    echo -e "The import of CRDB PDX studies completed successfully." >> "$EMAIL_MESSAGE_FILE"
                    EMAIL_SUBJECT="CRDB PDX cBioPortal nightly import status"
                    sendSuccessMessageMskPipelineLogsSlack "CRDB PDX Pipeline Success"
                fi
            fi
        fi
    fi
fi

# append any warnings from the subset and merge script
if [ -s "$CRDB_PDX_TMPDIR/$SUBSET_AND_MERGE_WARNINGS_FILENAME" ] ; then
    echo -e "\n" >> "$EMAIL_MESSAGE_FILE"
    echo -e "Warnings generated by the subset and merge of pdx cmo studies:" >> "$EMAIL_MESSAGE_FILE"
    echo -e "--------------------------------------------------------------" >> "$EMAIL_MESSAGE_FILE"
    cat "$CRDB_PDX_TMPDIR/$SUBSET_AND_MERGE_WARNINGS_FILENAME" >> "$EMAIL_MESSAGE_FILE"
fi
# append any warnings from the importer notification file
if [ -s "$importer_notification_file" ] ; then
    echo -e "\n" >> "$EMAIL_MESSAGE_FILE"
    echo -e "Output generated by the cBioPortal importer:" >> "$EMAIL_MESSAGE_FILE"
    echo -e "--------------------------------------------" >> "$EMAIL_MESSAGE_FILE"
    cat "$importer_notification_file" >> "$EMAIL_MESSAGE_FILE"
fi

validation_report_attachments=""
for validation_report in $(find $CRDB_PDX_TMPDIR -name "*-validation.html"); do
    validation_report_attachments+=" -a $validation_report"
done

echo -e "Sending email:"
cat "$EMAIL_MESSAGE_FILE"
cat "$EMAIL_MESSAGE_FILE" | mailx -s "$EMAIL_SUBJECT" $validation_report_attachments $PDX_EMAIL_LIST

echo "Cleaning up any untracked files from MSK-PDX import..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME/bic-mskcc-legacy $PORTAL_DATA_HOME/cmo-argos $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/datahub $PORTAL_DATA_HOME/crdb_pdx

exit 0
