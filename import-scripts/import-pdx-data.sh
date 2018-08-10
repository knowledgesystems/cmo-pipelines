#!/bin/bash

echo $(date)

PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh
email_list="cbioportal-pipelines@cbio.mskcc.org"
#TODO: restore the following
#CRDB_PDX_TMPDIR=/data/portal-cron/tmp/import-cron-pdx-msk
CRDB_PDX_TMPDIR=/data/sheridan/temp
CRDB_PDX_FETCH_SUCCESS=0
CRDB_PDX_SUBSET_AND_MERGE_SUCCESS=0
hg_rootdir="uninitialized"
shopt -s nullglob
declare -a modified_file_list
declare -a study_list

#TODO delete this testing set
PDX_DATA_HOME=/data/sheridan/cbio-portal-data/crdb_pdx
# Functions

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines pre-import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" https://hooks.slack.com/services/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
}

function setMercurialRootDirForDirectory {
    search_dir=$1
    SCRATCH_FILENAME=$( mktemp --tmpdir=$CRDB_PDX_TMPDIR scratchfile.tmpXXXXXXXX )
    # find root of mercurial repository
    rm -f $SCRATCH_FILENAME
    ( cd $search_dir ; $HG_BINARY root > $SCRATCH_FILENAME )
    read hg_rootdir < $SCRATCH_FILENAME
    rm -f $SCRATCH_FILENAME
}

function purgeOrigFilesUnderDirectory {
    search_dir=$1
    find $search_dir -name "*.orig" -delete
}

function addRemoveFilesUnderDirectory {
    search_dir=$1
    purgeOrigFilesUnderDirectory "$search_dir"
    ( cd $search_dir ; $HG_BINARY addremove . )
}

function commitAllMercurialChanges {
    any_repo_subdirectory=$1
    mercurial_log_message=$2
    setMercurialRootDirForDirectory "$any_repo_subdirectory"
    $HG_BINARY --repository "$hg_rootdir" commit -m "$mercurial_log_message"
}

function purgeAllMercurialChanges {
    any_repo_subdirectory=$1
    setMercurialRootDirForDirectory "$any_repo_subdirectory"
    $HG_BINARY --repository "$hg_rootdir" update -C
}

function cleanAllUntrackedFiles {
    any_repo_subdirectory=$1
    setMercurialRootDirForDirectory "$any_repo_subdirectory"
    $HG_BINARY --repository "$hg_rootdir" purge --files
}

function cleanUpEntireMercurialRepository {
    any_repo_subdirectory=$1
    purgeAllMercurialChanges "$any_repo_subdirectory"
    cleanAllUntrackedFiles "$any_repo_subdirectory"
}

function revertModifiedFilesUnderDirectory {
    search_dir=$1
    setMercurialRootDirForDirectory "$search_dir"
    # find all modified files in mercurial repository
    $HG_BINARY --repository "$hg_rootdir" status --modified | cut -f2 -d$" " > $SCRATCH_FILENAME
    unset modified_file_list
    readarray -t modified_file_list < $SCRATCH_FILENAME
    rm -f $SCRATCH_FILENAME
    # search for all modified files within search directory, and revert
    search_dir_slash="$search_dir/"
    search_dir_prefix_length=${#search_dir_slash}
    index=0
    while [ $index -lt ${#modified_file_list} ] ; do
        absolute_filename="$hg_rootdir/${modified_file_list[$index]}"
        filename_prefix=${absolute_filename:0:$search_dir_prefix_length}
        if [ $filename_prefix == $search_dir_slash ] ; then
            ( cd $search_dir ; $HG_BINARY revert --no-backup $absolute_filename )
        fi
        index=$(( $index + 1 ))
    done
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
    echo "automation-environment.sh could not be found, exiting..."
    exit 2
fi

. $PATH_TO_AUTOMATION_SCRIPT

if [ -z $BIC_DATA_HOME ] | [ -z $PDX_DATA_HOME ] | [ -z $HG_BINARY ] | [ -z $PYTHON_BINARY ] ; then
    message="could not run import-pdx-data.sh: automation-environment.sh script must be run in order to set needed environment variables (like BIC_DATA_HOME, PDX_DATA_HOME, ...)"
    echo ${message}
    echo -e "${message}" |  mail -s "import-pdx-data failed to run." $email_list
    sendFailureMessageMskPipelineLogsSlack "${message}"
    exit 2
fi

if [ ! -d $CRDB_PDX_TMPDIR ] ; then
    mkdir $CRDB_PDX_TMPDIR
    if [ $? -ne 0 ] ; then
        echo "error : required temp directory does not exist and could not be created : $CRDB_PDX_TMPDIR"
        exit 2
    fi
fi

IMPORTER_JAR_FILENAME=$PORTAL_HOME/lib/msk-cmo-importer.jar
SUBSET_AND_MERGE_WARNINGS_FILENAME="subset_and_merge_pdx_studies_warnings.txt"

# refresh cdd and oncotree cache
CDD_ONCOTREE_RECACHE_FAIL=0
bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
if [ $? -gt 0 ]; then
    CDD_ONCOTREE_RECACHE_FAIL=1
    message="Failed to refresh CDD and/or ONCOTREE cache during CRDB PDX import!"
    echo $message
    echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $pipeline_email_list
fi

DB_VERSION_FAIL=0
# check database version before importing anything
echo "Checking if database version is compatible"
$JAVA_HOME/bin/java $JAVA_PROXY_ARGS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin --check-db-version
if [ $? -gt 0 ]
then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

MERCURIAL_FETCH_VIA_IMPORTER_SUCCESS=0
# importer mercurial fetch step
echo "fetching updates from bic-mskcc repository..."
$JAVA_HOME/bin/java $JAVA_IMPORTER_ARGS -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin --fetch-data --data-source --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch BIC-MSKCC Studies From Mercurial Failure"
else
    echo "fetching updates from pdx repository..."
    $JAVA_HOME/bin/java $JAVA_IMPORTER_ARGS -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin --fetch-data --data-source pdx --run-date latest
    if [ $? -gt 0 ] ; then
        sendFailureMessageMskPipelineLogsSlack "Fetch PDX Studies From Mercurial Failure"
    else
        MERCURIAL_FETCH_VIA_IMPORTER_SUCCESS=1
    fi
fi

if [ $MERCURIAL_FETCH_VIA_IMPORTER_SUCCESS -ne 0 ] ; then
    echo "fetching pdx data fom crdb"
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/crdb_fetcher.jar --pdx --staging $CRDB_FETCHER_PDX_HOME
    if [ $? -ne 0 ] ; then
        echo "error: crdb_pdx_fetch failed"
        sendFailureMessageMskPipelineLogsSlack "Fetch CRDB PDX Failure"
        cleanUpEntireMercurialRepository $CRDB_FETCHER_PDX_HOME
    else
        addRemoveFilesUnderDirectory $CRDB_FETCHER_PDX_HOME
        commitAllMercurialChanges $CRDB_FETCHER_PDX_HOME "CRDB PDX Fetch"
        CRDB_PDX_FETCH_SUCCESS=1
    fi
fi

# TEMP (done): transform project_ids in test data to stable study ids (add tranform of stable id)

# construct destination studies from source studies
# call subsetting/constuction python script (add touch a trigger file for successful subset/merge) (add subroutine which creates process command) (touch needed meta files for the generated data files)
if [ $CRDB_PDX_FETCH_SUCCESS -ne 0 ] ; then
    mapping_filename="source_to_destination_mappings.txt"
    scripts_directory="$PORTAL_HOME/scripts"
    #TODO: add input root dir and remove hardcoded reference in merge script
    $PYTHON_BINARY $PORTAL_HOME/scripts/subset-and-merge-crdb-pdx-studies.py --mapping-file $mapping_filename --root-directory $PDX_DATA_HOME --lib $scripts_directory --cmo-root-directory $BIC_DATA_HOME --fetch-directory $CRDB_FETCHER_PDX_HOME --temp-directory $CRDB_PDX_TMPDIR --warning-filename $SUBSET_AND_MERGE_WARNINGS_FILENAME
    if [ $? -ne 0 ] ; then
        echo "error: subset-and-merge-crdb-pdx-studies.py exited with non zero status"
        sendFailureMessageMskPipelineLogsSlack "CRDB PDX subset-and-merge-crdb-pdx-studies.py script failure"
        cleanUpEntireMercurialRepository $CRDB_FETCHER_PDX_HOME
    else
        CRDB_PDX_SUBSET_AND_MERGE_SUCCESS=1
    fi
fi

if [ $CRDB_PDX_SUBSET_AND_MERGE_SUCCESS -ne 0 ] ; then
    # check trigger files and do appropriate hg add and hg purge
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
fi

exit 2

#TODO : make this smarter .. to only import if the destination study has changed (alter the spreadsheet checkmarks)
#TODO : check if we can reuse the pdx-portal column
if [ $CRDB_PDX_SUBSET_AND_MERGE_SUCCESS -ne 0 ] ; then
    # import if all went well (only if trigger file is present)

    # if the database version is correct and ALL fetches succeed, then import
    if [[ $DB_VERSION_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]] ; then
        echo "importing study data into triage portal database..."
        IMPORT_FAIL=0
        $JAVA_HOME/bin/java $JAVA_PROXY_ARGS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx32G -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin --update-study-data --portal crbd-pdx-portal --use-never-import --update-worksheet --notification-file "$triage_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -gt 0 ]; then
            echo "Triage import failed!"
            IMPORT_FAIL=1
            EMAIL_BODY="Triage import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: triage" $pipeline_email_list
        fi
        num_studies_updated=`cat $tmp/num_studies_updated.txt`

        # redeploy war
        if [[ $IMPORT_FAIL -eq 0 && $num_studies_updated -gt 0 ]]; then
            TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Triage Tomcat"
            TOMCAT_SERVER_DISPLAY_NAME="triage-tomcat"
            #echo "'$num_studies_updated' studies have been updated, redeploying triage-portal war..."
            echo "'$num_studies_updated' studies have been updated.  Restarting triage-tomcat server..."
            if ! /usr/bin/sudo /etc/init.d/triage-tomcat restart ; then
                EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server failed"
                echo -e "Sending email $EMAIL_BODY"
                echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $pipeline_email_list
            fi
            #echo "'$num_studies_updated' studies have been updated (no longer need to restart triage-tomcat server...)"
        else
            echo "No studies have been updated, skipping redeploy of triage-portal war..."
        fi

        # import ran and either failed or succeeded
        echo "sending notification email.."
        $JAVA_HOME/bin/java $JAVA_PROXY_ARGS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27183 -Xmx16g -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin --send-update-notification --portal triage-portal --notification-file "$triage_notification_file"
    fi
fi














########ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
########JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182"
########JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$MSK_DMP_TMPDIR -Dhttp.nonProxyHosts=draco.mskcc.org|pidvudb1.mskcc.org|phcrdbd2.mskcc.org|dashi-dev.cbio.mskcc.org|pipelines.cbioportal.mskcc.org|localhost"
########
########## FUNCTIONS
########
######### Function to generate case lists by cancer type
########function addCancerTypeCaseLists {
########    STUDY_DATA_DIRECTORY=$1
########    STUDY_ID=$2
########    # accept 1 or 2 data_clinical filenames
########    FILENAME_1="$3"
########    FILENAME_2="$4"
########    FILEPATH_1="$STUDY_DATA_DIRECTORY/$FILENAME_1"
########    FILEPATH_2="$STUDY_DATA_DIRECTORY/$FILENAME_2"
########    CLINICAL_FILE_LIST="$FILEPATH_1, $FILEPATH_2"
########    if [ -z "$FILENAME_2" ] ; then
########        CLINICAL_FILE_LIST="$FILEPATH_1"
########    fi
########    # remove current case lists and run oncotree converter before creating new cancer case lists
########    rm $STUDY_DATA_DIRECTORY/case_lists/*
########    $PYTHON_BINARY $PORTAL_HOME/scripts/oncotree_code_converter.py --oncotree-url "http://oncotree.mskcc.org/" --oncotree-version $ONCOTREE_VERSION_TO_USE --clinical-file $FILEPATH_1 --force
########    $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file-list="$CLINICAL_FILE_LIST" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="CANCER_TYPE"
########    if [ "$STUDY_ID" == "mskimpact" ] || [ "$STUDY_ID" == "mixedpact" ] || [ "$STUDY_ID" == "msk_solid_heme" ] ; then
########       $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file-list="$CLINICAL_FILE_LIST" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="PARTC_CONSENTED_12_245"
########    fi
########}
########
######### Function for adding "DATE ADDED" information to clinical data
########function addDateAddedData {
########    STUDY_DATA_DIRECTORY=$1
########    DATA_CLINICAL_FILENAME=$2
########    DATA_CLINICAL_SUPP_DATE_FILENAME=$3
########    # add "date added" to clinical data file
########    $PYTHON_BINARY $PORTAL_HOME/scripts/update-date-added.py --date-added-file=$STUDY_DATA_DIRECTORY/$DATA_CLINICAL_SUPP_DATE_FILENAME --clinical-file=$STUDY_DATA_DIRECTORY/$DATA_CLINICAL_FILENAME
########}
########
########
######### Function for removing raw clinical and timeline files from study directory
########function remove_raw_clinical_timeline_data_files {
########    STUDY_DIRECTORY=$1
########    # remove raw clinical files except patient and sample cbio format clinical files
########    for f in $STUDY_DIRECTORY/data_clinical*; do
########        if [[ $f != *"data_clinical_patient.txt"* && $f != *"data_clinical_sample.txt"* ]] ; then
########            rm -f $f
########        fi
########    done
########    # remove raw timeline files except cbio format timeline file
########    for f in $STUDY_DIRECTORY/data_timeline*; do
########        if [ $f != *"data_timeline.txt"* ] ; then
########            rm -f $f
########        fi
########    done
########}
########
######### Function for filtering columns from derived studies' clinical data
########function filter_derived_clinical_data {
########    STUDY_DIRECTORY=$1
########    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_patient.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST &&
########    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_sample.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST
########}
########
######### -----------------------------------------------------------------------------------------------------------
########if [[ -d "$MSK_DMP_TMPDIR" && "$MSK_DMP_TMPDIR" != "/" ]] ; then
########    rm -rf "$MSK_DMP_TMPDIR"/*
########fi
########
########if [ -z $JAVA_HOME ] | [ -z $HG_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_IMPACT_DATA_HOME ] ; then
########    message="test could not run import-dmp-impact.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_IMPACT_DATA_HOME, ...)"
########    echo ${message}
########    echo -e "${message}" |  mail -s "fetch-dmp-data-for-import failed to run." $email_list
########    sendFailureMessageMskPipelineLogsSlack "${message}"
########    exit 2
########fi
########
######### refresh cdd and oncotree cache - by default this script will attempt to
######### refresh the CDD and ONCOTREE cache but we should check both exit codes
######### independently because of the various dependencies we have for both services
########CDD_RECACHE_FAIL=0; ONCOTREE_RECACHE_FAIL=0
########bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --cdd-only
########if [ $? -gt 0 ]; then
########    message="Failed to refresh CDD cache!"
########    echo $message
########    echo -e "$message" | mail -s "CDD cache failed to refresh" $email_list
########    sendFailureMessageMskPipelineLogsSlack "$message"
########    CDD_RECACHE_FAIL=1
########fi
########bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --oncotree-only
########if [ $? -gt 0 ]; then
########    message="Failed to refresh ONCOTREE cache!"
########    echo $message
########    echo -e "$message" | mail -s "ONCOTREE cache failed to refresh" $email_list
########    sendFailureMessageMskPipelineLogsSlack "$message"
########    ONCOTREE_RECACHE_FAIL=1
########fi
########if [[ $CDD_RECACHE_FAIL -gt 0 || $ONCOTREE_RECACHE_FAIL -gt 0 ]] ; then
########    echo "Oncotree and/or CDD recache failed! Exiting..."
########    exit 2
########fi
########
########
######### -------------------------------- Generate all case lists and supp date files -----------------------------------
########
######### generate case lists by cancer type and add "DATE ADDED" info to clinical data for MSK-IMPACT
########if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
########    addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact" "data_clinical_mskimpact_data_clinical_cvr.txt"
########    cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Case Lists"
########    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
########        addDateAddedData $MSK_IMPACT_DATA_HOME "data_clinical_mskimpact_data_clinical_cvr.txt" "data_clinical_mskimpact_supp_date_cbioportal_added.txt"
########    fi
########fi
########
#########--------------------------------------------------------------
########## Merge studies for MIXEDPACT, MSKSOLIDHEME:
#########   (1) MSK-IMPACT, HEMEPACT, RAINDANCE, and ARCHER (MIXEDPACT)
#########   (1) MSK-IMPACT, HEMEPACT, and ARCHER (MSKSOLIDHEME)
########
######### touch meta_SV.txt files if not already exist
########if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ] ; then
########    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
########fi
########
########
######### check that meta_SV.txt is actually an empty file before deleting from IMPACT and HEMEPACT studies
########if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
########    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
########fi
########
########if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
########    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
########fi
#########--------------------------------------------------------------
######### Email for failed processes
########
########EMAIL_BODY="The MSKIMPACT study failed fetch. The original study will remain on the portal."
######### send email if fetch fails
########if [ $IMPORT_STATUS_IMPACT -gt 0 ] ; then
########    echo -e "Sending email $EMAIL_BODY"
########    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Fetch Failure: Import" $email_list
########fi
########
