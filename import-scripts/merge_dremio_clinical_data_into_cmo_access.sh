#!/usr/bin/env bash

# This script runs in either a dev mode or a prod mode. If running in dev mode, a separate dev mode
# clone of cmo-access is used as a source for cmo-access study data, otherwise the main (cmo-access)
# repo is used. Source files from cmo-access are copied into a temporary directory for mergining with
# data from dremio/SMILE. The dremio/SMILE data is then extracted using a go program which queries
# dremio for the relevant clinical/timeline data. merge.py is used to merge dremio/SMILE data into
# the cmo-access study and this merger is copied back into the source data repository. Then a
# symlink is created based on the study identifier and importing is done. The persistence cache is
# cleared. Finally cleanup and notification is handled.
# For prod mode runs, the updated data is commited back into the cmo-access repository, and there is
# a separate check then to make sure that while data was being fetched from dremio and merged, no
# new commits were pushed into the source repository (otherwise we cancel today's run)

FLOCK_FILEPATH="/data/portal-cron/cron-lock/merge_dremio_clinical_data_into_cmo_access.lock"
{
    echo $(date)

    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        echo "Failure : could not acquire lock for $FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    AUTOMATION_SCRIPT_FILEPATH=/data/portal-cron/scripts/automation-environment.sh
    DMP_IMPORT_VARS_AND_FUNCTIONS_FILEPATH=/data/portal-cron/scripts/dmp-import-vars-functions.sh

    # set up enivornment variables and temp directory
    if ! [ -f $AUTOMATION_SCRIPT_FILEPATH ] ; then
        message="automation-environment.sh could not be found, exiting..." >&2
        echo ${message}
        exit 1
    fi

    source $AUTOMATION_SCRIPT_FILEPATH
    source $DMP_IMPORT_VARS_AND_FUNCTIONS_FILEPATH

    if [ -z "$PORTAL_HOME" ] ; then
        message="could not run merge_dremio_clinical_data_into_cmo_access.sh : automation-environment.sh script must be run in order to set needed environment variables"
        echo ${message} >&2
        exit 1
    fi

    DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR=/data/portal-cron/tmp/merge_dremio_clinical_to_cmo_access
    DREMIO_CLINICAL_STAGING_DIRECTORY="${DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR}/dremio_cfdna_data"
    CMO_ACCESS_STAGING_INPUT_DIRECTORY="${DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR}/cmo_access_data"
    DREMIO_CLINICAL_OUTPUT_DIRECTORY="${DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR}/merged_study_data"
    DREMIO_CREDENTIALS_FILE="$PORTAL_HOME/pipelines-credentials/eks-account.credentials"
    DREMIO_USERNAME=""
    DREMIO_PASSWORD=""
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-cmo-importer.jar"
    ENABLE_DEBUGGING=0
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$CMO_ACCESS_TMPDIR -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
    RUNMODE_PROD="runmode_prod"
    RUNMODE_DEV="runmode_dev"
    runmode=$RUNMODE_DEV
    CMO_ACCESS_PROD_DATA_HOME="$PORTAL_DATA_HOME/cmo-access"
    CMO_ACCESS_DEV_DATA_HOME="$PORTAL_DATA_HOME/cmo-access-dev-dup-clone"
    clone_homedir="$CMO_ACCESS_DEV_DATA_HOME"
    active_cmo_access_data_home="$CMO_ACCESS_DEV_DATA_HOME"
    PROD_CANCER_STUDY_IDENTIFIER="mixed_msk_cfdna_research_access"
    DEV_CANCER_STUDY_IDENTIFIER="mixed_msk_cfdna_research_access_dremio_dev"
    active_cancer_study_identifier="$DEV_CANCER_STUDY_IDENTIFIER"
    IMPORT_DEV_SYMLINK_FILEPATH="$CMO_ACCESS_PROD_DATA_HOME/mixed_msk_cfdna_research_access_dremio_dev"
    IMPORT_PROD_SYMLINK_FILEPATH="$CMO_ACCESS_PROD_DATA_HOME/mixed_msk_cfdna_research_access"
    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    current_changeset_hash_at_head=""

    function set_runmode_active_vars() {
        if [ "$runmode" == "$RUNMODE_PROD" ] ; then
            active_cmo_access_data_home="$CMO_ACCESS_PROD_DATA_HOME"
            active_cancer_study_identifier="$PROD_CANCER_STUDY_IDENTIFIER"
        else
            active_cmo_access_data_home="$CMO_ACCESS_DEV_DATA_HOME"
            active_cancer_study_identifier="$DEV_CANCER_STUDY_IDENTIFIER"
        fi
    }

    function set_runmode_from_args() {
        runmode=$RUNMODE_DEV
        lastarg=""
        for arg in $@ ; do
            if [ "$arg" == "--runmode=prod" ] ; then
                runmode=$RUNMODE_PROD
            fi
            if [ "$arg" == "prod" ] && [ "$lastarg" == "--runmode" ] ; then
                runmode=$RUNMODE_PROD
            fi
            if [ "$arg" == "--runmode=dev" ] ; then
                runmode=$RUNMODE_DEV
            fi
            if [ "$arg" == "dev" ] && [ "$lastarg" == "--runmode" ] ; then
                runmode=$RUNMODE_DEV
            fi
            lastarg="$arg"
        done
        set_runmode_active_vars
    }

    function safe_delete_directory_recursively() {
        dir_path=$(readlink -f "$1")
        if [[ -d "$dir_path" && "$dir_path" != "/" ]] ; then
            rm -rf "$dir_path"
        fi
    }

    function prepare_tempdirs() {
        if [ ! -d $DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR ] ; then
            mkdir $DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR
            if [ $? -ne 0 ] ; then
                message="error : required temp directory does not exist and could not be created : $DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR"
                echo ${message} >&2
                exit 1
            fi
        else
            # clear the old contents
            for entry in "$DREMIO_CLINICAL_TO_CMO_ACCESS_TMPDIR"/* ; do
                safe_delete_directory_recursively $entry
            done
        fi
        mkdir "${DREMIO_CLINICAL_STAGING_DIRECTORY}"
        mkdir "${CMO_ACCESS_STAGING_INPUT_DIRECTORY}"
        if ! mkdir "${DREMIO_CLINICAL_OUTPUT_DIRECTORY}" ; then
            echo "could not create tmp directories, for example ${DREMIO_CLINICAL_OUTPUT_DIRECTORY}" >&2
            exit 1
        fi
    }

    function set_dremio_credentials() {
        if ! [ -r $DREMIO_CREDENTIALS_FILE ] ; then
            echo "$DREMIO_CREDENTIALS_FILE could not be read, exiting..."
            exit 1
        fi
        DREMIO_USERNAME="$(grep -rh eks.account.name $DREMIO_CREDENTIALS_FILE | sed 's/eks.account.name=//g')"
        DREMIO_PASSWORD="$(grep -rh eks.password $DREMIO_CREDENTIALS_FILE | sed 's/eks.password=//g')"
        if [ -z $DREMIO_PASSWORD ] ; then
            echo "DREMIO PASSWORD could not be obtained from $DREMIO_CREDENTIALS_FILE, exiting..." >&2
            exit 1
        fi
    }

    function set_clone_homedir() {
        clone_homedir="$CMO_ACCESS_DEV_DATA_HOME"
        if [ "$runmode" == "$RUNMODE_PROD" ] ; then
            clone_homedir="$CMO_ACCESS_PROD_DATA_HOME"
        fi
    }

    function update_git_repository_before_rsync() {
        if ! $GIT_BINARY -C "${clone_homedir}" pull ; then
            echo "unable to pull updates to data repository : ${clone_homedir}, exiting..." >&2
            exit 1
        fi
        if ! $GIT_BINARY -C "${clone_homedir}" reset HEAD --hard ; then
            echo "unable to reset to head of data repository : ${clone_homedir}, exiting..." >&2
            exit 1
        fi

    }

    function get_source_data() {
        set_dremio_credentials
        update_git_repository_before_rsync
        if ! rsync -a "${clone_homedir}/mixed_MSK_cfDNA_RESEARCH_ACCESS/" "${CMO_ACCESS_STAGING_INPUT_DIRECTORY}" ; then
            echo "unable to rsync data from data repository ${clone_homedir} to staging area ${CMO_ACCESS_STAGING_INPUT_DIRECTORY}, exiting..." >&2
            exit 1
        fi
        old_dir=$(pwd)
        cd "${DREMIO_CLINICAL_STAGING_DIRECTORY}"
        if ! /data/portal-cron/bin/convert_extract_cfdna --host=tlvidreamcord1.mskcc.org --port=32010 --user=${DREMIO_USERNAME} --pass=${DREMIO_PASSWORD} --sampleFile="${CMO_ACCESS_STAGING_INPUT_DIRECTORY}"/data_clinical_sample.txt ; then
            echo "go program convert_extract_cfdna exited with non-zero status, exiting..." >&2
            exit 1
        fi
        cd "$old_dir"
    }

    function merge_clinical_data() {
        if ! ${PYTHON_BINARY} ${PORTAL_HOME}/scripts/merge.py --output-directory "${DREMIO_CLINICAL_OUTPUT_DIRECTORY}" --study-id mixed_msk_cfdna_research_access --merge-clinical true --file-type-list clinical_patient,clinical_sample,timeline "${CMO_ACCESS_STAGING_INPUT_DIRECTORY}" "${DREMIO_CLINICAL_STAGING_DIRECTORY}" ; then
            echo "merge.py exiting with non-zero status while merging dremio output into cmo-access study, exiting..." >&2
            exit 1
        fi
    }

    function rsync_merged_files_to_active_repo() {
        destination_dirpath="${clone_homedir}/mixed_MSK_cfDNA_RESEARCH_ACCESS"
        if ! rsync -a "${DREMIO_CLINICAL_OUTPUT_DIRECTORY}/" "${destination_dirpath}" ; then
            echo "rsync of files from ${DREMIO_CLINICAL_OUTPUT_DIRECTORY} to ${destination_dirpath} failed, exiting..." >&2
            exit 1
        fi
    }

    function set_current_changeset_hash_at_head() {
        current_changeset_hash_at_head=$($GIT_BINARY -C "${clone_homedir}" log | head -n 1 | cut -f 2 -d $" ")
    }

    function rsync_and_commit_updated_study_files() {
        if ! [ "$runmode" == "$RUNMODE_PROD" ] ; then
            # only rsync files when running in dev mode - skip commit
            rsync_merged_files_to_active_repo
            return 0
        fi 
        set_current_changeset_hash_at_head
        current_changeset_hash_at_head_previous="$current_changeset_hash_at_head"
        if ! $GIT_BINARY -C "${clone_homedir}" pull ; then
            echo "git pull on ${clone_homedir} failed, exiting..." >&2
            exit 1
        fi
        if ! $GIT_BINARY -C "${clone_homedir}" reset HEAD --hard ; then
            echo "git reset on ${clone_homedir} failed, exiting..." >&2
            exit 1
        fi
        set_current_changeset_hash_at_head
        if ! [ "$current_changeset_hash_at_head_previous" == "$current_changeset_hash_at_head" ] ; then
            # there has been commits added since we began this run - report and exit
            echo "The repository at ${clone_homedir} has been modified during the time that merging occurred. Exiting..." >&2
            exit 1
        fi
        rsync_merged_files_to_active_repo
        if ! $GIT_BINARY -C "${clone_homedir}" add "${clone_homedir}/mixed_MSK_cfDNA_RESEARCH_ACCESS"/* ; then
            echo "git add on ${clone_homdir} failed, exiting..." >&2
            exit 1
        fi
        if ! $GIT_BINARY -C "${clone_homedir}" commit -m "Latest clinical data from Dremio/SMILE" ; then
            echo "git commit on ${clone_homdir} failed, exiting..." >&2
            exit 1
        fi
        if ! $GIT_BINARY -C "${clone_homedir}" push origin ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "GIT PUSH (cmo_access) :fire: - address ASAP!"
            echo "git push origin on ${clone_homdir} failed, exiting..." >&2
            exit 1
        fi
    }

    function import_cmo_access_study() {
        portal_name="cmo-access-dremio-dev-portal"
        import_symlink_filepath="$IMPORT_DEV_SYMLINK_FILEPATH"
        if [ "$runmode" == "$RUNMODE_PROD" ] ; then
            portal_name="cmo-access-portal"
            import_symlink_filepath="$IMPORT_PROD_SYMLINK_FILEPATH"
        fi
        import_symlink_actual_target="${clone_homedir}/mixed_MSK_cfDNA_RESEARCH_ACCESS"
        rm -f "$IMPORT_SYMLINK_FILEPATH"
        ln -s "$import_symlink_actual_target" "$import_symlink_filepath" # symlink needed because the datasource search is based on study id, which is all lower case
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
        if ! $JAVA_BINARY -Xmx32G $JAVA_IMPORTER_ARGS --update-study-data --portal $portal_name --use-never-import --disable-redcap-export --notification-file "$cmo_access_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc ; then
            echo "CMO-ACCESS import failed!"
            sendImportFailureMessageMskPipelineLogsSlack "CMO-ACCESS import failed!"
            EMAIL_BODY="CMO-ACCESS import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: cmo_access" $PIPELINES_EMAIL_LIST
            exit 1
        fi
        rm -f "$IMPORT_SYMLINK_FILEPATH"
        num_studies_updated=$(cat $CMO_ACCESS_TMPDIR/num_studies_updated.txt)
        # clear persistence cache
        if [[ $num_studies_updated -gt 0 ]]; then
            echo "'$num_studies_updated' studies have been updated, clearing persistence cache for cmo-access portal..."
            if ! clearPersistenceCachesForMskPortals ; then
                sendClearCacheFailureMessage cmo-access merge_dremio_clinical_data_into_cmo_access.sh
            fi
        else
            echo "No studies have been updated, not clearing persistence cache for cmo-access portal..."
        fi
        # import ran and either failed or succeeded
        echo "sending notification email.."
        #TODO we cannot rebuild importer currently, so use the mskimpact-portal which causes an email to be sent to our own group email only
        $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal mskimpact-portal --notification-file "$cmo_access_notification_file"
    }

    function remove_tempdirs() {
        safe_delete_directory_recursively "${DREMIO_CLINICAL_STAGING_DIRECTORY}"
        safe_delete_directory_recursively "${CMO_ACCESS_STAGING_INPUT_DIRECTORY}"
        safe_delete_directory_recursively "${DREMIO_CLINICAL_OUTPUT_DIRECTORY}"
    }

    function close_log_and_exit() {
        echo "Fetching and importing of CMO-ACCESS complete!"
        echo $(date)
    }

    function main() {
        set_runmode_from_args $@
        set_clone_homedir
        prepare_tempdirs
        get_source_data
        merge_clinical_data
        rsync_and_commit_updated_study_files
        import_cmo_access_study
        remove_tempdirs
        close_log_and_exit
    }

    main $@

} {flock_fd}>$FLOCK_FILEPATH
