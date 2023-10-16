#!/bin/bash

# 0. acquire the exclusive file lock
# 1. reset cmo-access git repository to match the upstream main branch (using similar lagic to
#    the pipelines importer --fetch-data, but in script because of the different branch names.
# 2. extract a list of dmp patients in the study (only "P-" prefixed)
# 3. query the ddp server for associated timeline/clinical information on these patients
# 4. create meta files for the 3 timeline data files and copy timeine info into the data repository
# 5. fix case list files by overwriting stable_id and cancer_study_identifier
# 6. check importer compatibility with the database schema version
# 7. import the study to the msk-portal
# 8. clear the persistence cache for the msk-portal
# 9. send email notification of study import
# 10. commit changes/updates into the data repository clone
# 11. push changeset to the data repository

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/fetch-ddp-and-import-cmo-access-data.lock"
(
    echo $(date)

    # check lock so that executions of this script not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH -- the fetch from yesterday is still running."
        exit 1
    fi

    source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
    source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh

    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    CANCER_STUDY_IDENTIFIER="mixed_msk_cfdna_research_access"
    IMPORT_SYMLINK_FILEPATH="/data/portal-cron/cbio-portal-data/cmo-access/mixed_msk_cfdna_research_access"
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-cmo-importer.jar"
    ENABLE_DEBUGGING=0
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$CMO_ACCESS_TMPDIR -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    cmo_access_notification_file=$(mktemp $CMO_ACCESS_TMPDIR/cmo-access-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
    cmo_access_dmp_pids_filepath=$CMO_ACCESS_TMPDIR/cmo_access_patient_list.txt
    cmo_access_seq_date_filepath=$CMO_ACCESS_TMPDIR/cmo_access_seq_date.txt
    cmo_access_ddp_output_dirpath=$CMO_ACCESS_TMPDIR/ddp_output

    # -----------------------------------------------------------------------------------------------------------
    # START CMO-ACCESS DATA FETCHING
    echo $(date)

    if [ -z $JAVA_BINARY ] | [ -z $GIT_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $CMO_ACCESS_DATA_HOME ] ; then
        message="could not run fetch-ddp-and-import-cmo-access-data.sh: automation-environment.sh script must be run in order to set needed environment variables (like CMO_ACCESS_DATA_HOME, ...)"
        echo $message
        echo -e "$message" | mail -s "fetch-ddp-and-import-cmo-access-data.sh failed to run." $PIPELINES_EMAIL_LIST
        sendPreImportFailureMessageMskPipelineLogsSlack "$message"
        exit 1
    fi

    # create tmpdir if it does not exist, and wipe contents
    if [[ -d "$CMO_ACCESS_TMPDIR" ]] ; then
        if [[ "$CMO_ACCESS_TMPDIR" != "/" ]] ; then
            rm -rf "$CMO_ACCESS_TMPDIR"/*
        fi
    else
        mkdir "$CMO_ACCESS_TMPDIR"
    fi

    function fetch_git_repo_data_updates() {
        GIT_REPO_HOME="$1"
        GIT_BRANCH_NAME="$2"
        echo "fetching updates from cmo_access repository..."
        old_dir=$(pwd)
        if ! cd "$GIT_REPO_HOME" ; then
            echo "fetch_git_repo_data_updates() : error - could not change to directory $GIT_REPO_HOME"
            return 1
        fi
        # drop local changes to files
        if ! git checkout . ; then
            echo "fetch_git_repo_data_updates() : error - could not checkout default branch at $GIT_REPO_HOME"
            return 1
        fi
        # clean local files and directories which are not part of the repository
        if ! git clean -fd ; then
            echo "fetch_git_repo_data_updates() : error - could not git clean at $GIT_REPO_HOME"
            return 1
        fi
        # lose any unpushed local commits
        if ! git reset --hard "origin/$GIT_BRANCH_NAME" ; then
            echo "fetch_git_repo_data_updates() : error - could not git reset --hard origin/$GIT_BRANCH_NAME at $GIT_REPO_HOME"
            return 1
        fi
        # pull updates from repo
        git_pull_stdout_filepath=$(mktemp "$CMO_ACCESS_TMPDIR/git_pull.stdout_XXXXXX")
        if ! git pull origin "$GIT_BRANCH_NAME" > "$git_pull_stdout_filepath" ; then
            echo "fetch_git_repo_data_updates() : error - could not git pull origin $GIT_BRANCH_NAME at $GIT_REPO_HOME"
            return 1
        fi
        # always run git-lfs pull origin <branch> to ensure git-lfs managed file contents show actual data, not pointer file reference info
        if ! git-lfs pull origin "$GIT_BRANCH_NAME" ; then
            echo "fetch_git_repo_data_updates() : error - could not git-lfs pull origin $GIT_BRANCH_NAME at $GIT_REPO_HOME"
            return 1
        fi
        # updates might have been fetched already but still want to notify if updates were pulled or not
        if grep "Already up to date." "$git_pull_stdout_filepath" ; then
            echo "fetch_git_repo_data_updates() : we have the latest dataset at $GIT_REPO_HOME"
        else
            echo "fetch_git_repo_data_updates() : we pulled updates from git at $GIT_REPO_HOME"
        fi
        rm "$git_pull_stdout_filepath"
        cd "$old_dir"
        return 0
    }

    function fetch_ddp_demographics_and_timeline() {
        rm -f "$cmo_access_dmp_pids_filepath"
        rm -f "$cmo_access_seq_date_filepath"
        rm -rf "$cmo_access_ddp_output_dirpath"
        mkdir "$cmo_access_ddp_output_dirpath"
        echo "SAMPLE_ID	PATIENT_ID	SEQ_DATE" > "$cmo_access_seq_date_filepath" # create placeholder seq_date file
        egrep -v "^#" $CMO_ACCESS_DATA_HOME/data_clinical_patient.txt | awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' | egrep "^P-" | sort | uniq > $cmo_access_dmp_pids_filepath
        CMO_ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $cmo_access_dmp_pids_filepath)
        if [ $CMO_ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
            CMO_ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
        fi
        $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact -p $cmo_access_dmp_pids_filepath -s "$cmo_access_seq_date_filepath" -f diagnosis,radiation,chemotherapy,surgery,survival -o $cmo_access_ddp_output_dirpath -r $CMO_ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT
    }

    function correct_case_list_file() {
        CASE_LIST_FILEPATH="$1"
        temp_case_list_filepath=$(mktemp $CMO_ACCESS_TMPDIR/corrected_case_list.XXXXXX)
        CASE_LIST_SUFFIX_REGEX="_([^_]*)$"
        while read -r line ; do
            if [[ ${line:0:24} == "cancer_study_identifier:" ]] ; then
                echo "cancer_study_identifier: ${CANCER_STUDY_IDENTIFIER}"
                continue
            fi
            if [[ ${line:0:10} == "stable_id:" ]] ; then
                case_list_suffix="unknown"
                if [[ "$line" =~ $CASE_LIST_SUFFIX_REGEX ]] ; then
                    case_list_suffix="${BASH_REMATCH[1]}"
                fi
                echo "stable_id: ${CANCER_STUDY_IDENTIFIER}_${case_list_suffix}"
                continue
            fi
            echo "$line"
        done < $CASE_LIST_FILEPATH > "$temp_case_list_filepath"
        chmod --reference "$CASE_LIST_FILEPATH" "$temp_case_list_filepath"
        mv "$temp_case_list_filepath" "$CASE_LIST_FILEPATH"
    }

    function create_meta_timeline_file() {
        destination_dirpath="$1"
        data_filename="$2"
        meta_filepath="$destination_dirpath/meta_timeline${data_filename:13}"
        echo "cancer_study_identifier: $CANCER_STUDY_IDENTIFIER" > "$meta_filepath"
        echo "genetic_alteration_type: CLINICAL" >> "$meta_filepath"
        echo "datatype: TIMELINE" >> "$meta_filepath"
        echo "data_filename: $data_filename" >> "$meta_filepath"
    }

    function create_meta_timeline_files() {
        destination_dirpath="$1"
        create_meta_timeline_file "$destination_dirpath" "data_timeline_ddp_chemotherapy.txt"
        create_meta_timeline_file "$destination_dirpath" "data_timeline_ddp_radiation.txt"
        create_meta_timeline_file "$destination_dirpath" "data_timeline_ddp_surgery.txt"
    }

    function copy_timeline_files_into_source_repo() {
        source_dirpath="$1"
        destination_dirpath="$2"
        suffixes=( "ddp_chemotherapy" "ddp_radiation" "ddp_surgery" )
        for suffix in ${suffixes[*]} ; do
            cp -a "$source_dirpath/meta_timeline_$suffix.txt" "$destination_dirpath"
            cp -a "$source_dirpath/data_timeline_$suffix.txt" "$destination_dirpath"
        done
    }

    function fix_case_list_files() {
        for filepath in $CMO_ACCESS_DATA_HOME/case_lists/* ; do
            correct_case_list_file "$filepath"
        done
    }

    # -----------------------------------------------------------------------------------------------------------
    # DATA FETCHES
    printTimeStampedDataProcessingStepMessage "CMO-ACCESS data processing"
    if ! fetch_git_repo_data_updates "$CMO_ACCESS_DATA_HOME" main ; then
        sendPreImportFailureMessageMskPipelineLogsSlack "Git Failure: CMO-ACCESS repository update"
        exit 1
    fi
    # fetch ddp demographics data
    printTimeStampedDataProcessingStepMessage "DDP demographics fetch for CMO-ACCESS"
    if !  fetch_ddp_demographics_and_timeline ; then
        sendPreImportFailureMessageMskPipelineLogsSlack "ddp Fetch Failure: CMO-ACCESS "
        exit 1
    fi
    create_meta_timeline_files "$cmo_access_ddp_output_dirpath"
    copy_timeline_files_into_source_repo "$cmo_access_ddp_output_dirpath" "$CMO_ACCESS_DATA_HOME"
    fix_case_list_files
    # -----------------------------------------------------------------------------------------------------------
    # STUDY IMPORT
    echo $(date)
    # -------------------------------------------------------------
    printTimeStampedDataProcessingStepMessage "database version compatibility check"
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ] ; then
        echo "Database version expected by portal does not match version in database!"
        sendImportFailureMessageMskPipelineLogsSlack "MSK DMP Importer DB version check (CMO-ACCESS)"
        EMAIL_BODY="The CMO-ACCESS database version is incompatible. Imports will be skipped until database is updated."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
        exit 1
    fi
    echo "importing study data into msk-portal database..."
    ln -sf "$CMO_ACCESS_DATA_HOME" "$IMPORT_SYMLINK_FILEPATH" # symlink needed because the datasource search is based on study id, which is all lower case
    $JAVA_BINARY -Xmx32G $JAVA_IMPORTER_ARGS --update-study-data --portal cmo-access-portal --use-never-import --disable-redcap-export --notification-file "$cmo_access_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
    if [ $? -gt 0 ]; then
        echo "CMO-ACCESS import failed!"
        sendImportFailureMessageMskPipelineLogsSlack "CMO-ACCESS import failed!"
        EMAIL_BODY="CMO-ACCESS import failed"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Import failure: cmo_access" $PIPELINES_EMAIL_LIST
        exit 1
    fi
    rm -f "$IMPORT_SYMLINK_FILEPATH"
    num_studies_updated=`cat $CMO_ACCESS_TMPDIR/num_studies_updated.txt`
    # clear persistence cache
    if [[ $num_studies_updated -gt 0 ]]; then
        echo "'$num_studies_updated' studies have been updated, clearing persistence cache for cmo-access portal..."
        if ! clearPersistenceCachesForMskPortals ; then
            sendClearCacheFailureMessage cmo-access fetch-ddp-and-import-cmo-access-data.sh
        fi
    else
        echo "No studies have been updated, not clearing persitsence cache for cmo-access portal..."
    fi

    # import ran and either failed or succeeded
    echo "sending notification email.."
    ####TODO we cannot rebuild importer currently, so use the mskimpact-portal which causes an email to be sent to our own group email only
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal mskimpact-portal --notification-file "$cmo_access_notification_file"

    echo "committing ddp data"
    cd $CMO_ACCESS_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "update of ddp timeline data"

    #--------------------------------------------------------------
    # GIT PUSH
    printTimeStampedDataProcessingStepMessage "push of CMO-ACCESS data updates to git repository"
    # check updated data back into git
    GIT_PUSH_FAIL=0
    cd $CMO_ACCESS_DATA_HOME ; $GIT_BINARY push origin
    if [ $? -gt 0 ] ; then
        GIT_PUSH_FAIL=1
        sendPreImportFailureMessageMskPipelineLogsSlack "GIT PUSH (cmo_access) :fire: - address ASAP!"
        EMAIL_BODY="Failed to push cmo_access outgoing changes to Git - address ASAP!"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "[URGENT] GIT PUSH FAILURE" $PIPELINES_EMAIL_LIST
    fi

    echo "Fetching and importing of CMO-ACCESS complete!"
    echo $(date)

    echo "Cleaning up any untracked files in $PORTAL_DATA_HOME/cmo-access..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $CMO_ACCESS_DATA_HOME
    exit 0

) {my_flock_fd}>$MY_FLOCK_FILEPATH
