#!/usr/bin/env bash

# clear-persistence-caches-for-changed-studies.sh
# ------------------------------------------
# This script is intended to run frequently in the cbioportal_importer service account.
# Parallel executions are expected and mutual exclusion is provided with flock as appropriate.
# This script will clear persistence cache in the cBioPortal system. If the database/portal
# is configured to allow per-study cache clearing through a CACHE_API_KEY and api/cache
# endpoint, that approach will be used to clear persistence cache for studies which have been
# very recently altered. If not, the entire cache is cleared through an external kubernetes
# reset script which purges the entire cache for the portal. This second approach will also
# be automatically used if the per-study approach is attempted and builds up a backlog of
# tasks which grow too stale.
#
# Implementation:
# Trigger files are crated in a fixed directory to represent tasks which must be performed.
# To allow operation across multiple databases, study-specific cache clearing triggers are
# located in subdirectories beneath the main directory (named by database label).
# There are two phases of operation for every process executing this script
# - PHASE ONE : (the main lock must frist be obtained before beginning this phase)
#   - clean up status files and triggers for work completed by other processes
#   - scan all managed databases
#       - if full cache clear is triggered, nothing else needs to be scheduled
#       - otherwise, look at the database cancer_study records and see if any changed
#               since the last time this script was executed. If so, create the
#               appropriate trigger files to reset per-study caches or the full cache
#       - overwrite the previous saved state of the database with the state just captured
# - PHASE TWO : (multiple processes can be executing this phase in parallel)
#   - scan all managed databases
#       - find any triggered cache clearing task to do which is not marked as
#               completed, and for which the task-specific flock has not been already
#               acquired by another process. Prioritize full cache clearing tasks.
#       - acquire the flock for the task
#       - perform the task (via the api for studies, or the kuberentes script for full)
#       - if the taks completed successfully, create a task status file showing success
#
# Example and explanation:
# For managing the resetting of caches during imports of studies into the public cBioPortal
# database, imagine that nothing stale is in the persistence caches and this script is being
# run every minute on a linux server monitoring the database and caces. A file like this will
# be present and will contain a list of all studies in the database, including the timestamp
# in the cancer_study table field IMPORT_DATE:
# /data/portal-cron/tmp/clear-persistence-cache/public_study_info
# As the database is not changing, no cache resetting will be triggered during each run.
# Then, a rapid change to the database occurs in which two studies with study_ids 'd1' and
# 'd2' have been deleted and another study 'm1' has been re-imported, causing an
# update to the IMPORT_DATE timestamp.
# During the next PHASE ONE execution, the current cancer_study info will be downloaded from
# the databse into file: /data/portal-cron/tmp/clear-persistence-cache/public_study_info_new
# and these two files will be compared, leading to an array of newly modified studies to be
# generated, which will include [d1, d2, m1]
# Scenario 1 : Per-study cache resets are available through the portal cache api...
# In this case, trigger files will be generated in the "public" subdirectory:
# /data/portal-cron/tmp/clear-persistence-cache/public/d1.needs_invalidation
# /data/portal-cron/tmp/clear-persistence-cache/public/d2.needs_invalidation
# /data/portal-cron/tmp/clear-persistence-cache/public/m1.needs_invalidation
# This captures the work which needs to be done to clear the persistence cache of stale data
# and now the file public_study_info_new is renamed to overwrite the old public_study_info.
# Note : while any execution of this script is active in PHASE ONE, it holds an exclusive
# file lock on this top level lockfile:
# /data/portal-cron/cron-lock/clear_redis_persistence_caches.lock
# and other attempts to execute the script will exit before starting PHASE ONE. Once the
# running process completes PHASE ONE, it releases the lock to allow future runs to
# proceed into PHASE ONE and PHASE TWO (where actual cache clearing will occur)
# When PHASE TWO is running, the process will look at all trigger files and will select
# one to work on. Lets say this process chooses to work on resetting the cache for d1.
# It will acquire a lock on the d1 trigger file (locking out future executions from
# selecting this task) and will call the api endpoint in the portal backend to purge the
# persistence cache of entries related to study d1. It may wait for a while for this to
# complete. When it eventually completes it will mark the task complete by creating a
# status file with name:
# /data/portal-cron/tmp/clear-persistence-cache/public/d1.invalidation_succeeded
# and a future PHASE ONE execution will clean up this pair of trigger/status files.
# If the API call were to fail, it would not create the status file but would release the
# lock, making the trigger again eligable to be re-attempted.
# After a minute, a second execution will begin and may select the task trigger to
# purge the cache for d2 during PHASE TWO. And these two processes may still be in
# progress when a third execution reaches PHASE TWO and begins purging the cache for
# study m1. Eventually these three purges will complete and the status files will be
# generated, eventually leading to a fully purged cache and purged trigger/status files.
# Scenario 2 : Per-study cache resets are not available
# In this case, per-study triggers are not created. Instead if there are any number of
# cancer studies which should have stale cache data cleared, a single trigger file is
# created to indicate that the entire persistence cache content should be invalidated:
# /data/portal-cron/tmp/clear-persistence-cache/public/CACHE_RESET_NEEDED
# Then public_study_new_info will be renamed to overwrite the old public_study_info.
# When PHASE TWO is running, the process will select this (only) trigger file and use
# an external script to trigger a kubernetes based total cache reset. If that is
# successful, the succeeded status file is created:
# /data/portal-cron/tmp/clear-persistence-cache/public/CACHE_RESET_SUCCEEDED
# Note : under Scenario 1 above, if any trigger file is not resolved within a
# maximum time duration limit, the script will replace all trigger files for the
# involved database with a single trigger file for a total cache reset.

# constants and global variable declarations
# ------------------------------------------

FLOCK_DIR="/data/portal-cron/cron-lock"
MY_FLOCK_FILEPATH="$FLOCK_DIR/clear_redis_persistence_caches.lock"
TMP_DIR_ROOT="/data/portal-cron/tmp/clear-persistence-cache"
MYSQL_COMMAND_FILE_TO_GET_STUDY_LIST="$TMP_DIR_ROOT/get_study_list_with_info.sql"
unset PORTAL_DB_LIST
PORTAL_DB_LIST="public"
#PORTAL_DB_LIST="public genie-public"
unset PORTAL_DB_TO_CONFIG_MAP_FILE
declare -A PORTAL_DB_TO_CONFIG_MAP_FILE
PORTAL_DB_TO_CONFIG_MAP_FILE["public"]="/data/portal-cron/git-repos/portal-configuration/k8s-config-vars/public/config_map.yaml"
PORTAL_DB_TO_CONFIG_MAP_FILE["genie-public"]="/data/portal-cron/git-repos/portal-configuration/k8s-config-vars/public/config_map_genie_public.yaml"
unset PORTAL_DB_TO_PORTAL_API_BASE_URI
declare -A PORTAL_DB_TO_PORTAL_API_BASE_URI
PORTAL_DB_TO_PORTAL_API_BASE_URI["public"]="https://www.cbioportal.org/api"
PORTAL_DB_TO_PORTAL_API_BASE_URI["genie-public"]="https://genie.cbioportal.org/api"
unset portal_db_to_cache_api_key
declare -A portal_db_to_cache_api_key
portal_db_to_cache_api_key["public"]="unknown"
portal_db_to_cache_api_key["genie-public"]="unknown"
unset portal_db_to_full_cache_clearing_function
declare -A portal_db_to_full_cache_clearing_function
portal_db_to_full_cache_clearing_function["public"]="clearPersistenceCachesForPublicPortals"
portal_db_to_full_cache_clearing_function["genie-public"]="clearPersistenceCachesForGeniePortals"
MAXIMUM_TIME_FOR_STUDY_RESET_BEFORE_ESCALATION=480  # 480 seconds (8 minutes)
CACHE_RESET_NEEDED_FILENAME="CACHE_RESET_NEEDED"
CACHE_RESET_SUCCEEDED_FILENAME="CACHE_RESET_SUCCEEDED"
STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX="needs_invalidation"
STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX="invalidation_succeeded"
unset newly_modified_study_list
declare -a newly_modified_study_list
unset selected_cache_reset_work_task_trigger_filepath
selected_cache_reset_work_task_trigger_filepath=""
unset selected_cache_reset_work_task_db
selected_cache_reset_work_task_db=""
EXIT_STATUS_FOR_TERMINATION=86

# load property extraction functions and persistence cache clearning functions
unset extracted_properties
declare -Ax extracted_properties
source /data/portal-cron/scripts/extract-properties-from-file-functions.sh
source /data/portal-cron/scripts/clear-persistence-cache-shell-functions.sh

# function definitions
# ------------------------------------------

# creates a directory if it does not already exist
# usage : create_directory_if_necessary path_of_direcdtory_to_be_created
function create_directory_if_necessary() {
    dir="$1"
    if ! [ -d $dir ] ; then
        if ! mkdir -p $dir ; then
            echo "Failure : could not create missing directory $dir" >&2
            exit $EXIT_STATUS_FOR_TERMINATION # this only exits from subshell
        fi
    fi
}

# creates a pre-located file with appropriate select statement as content
# usage : create_mysql_command_file_if_needed
function create_mysql_command_file_if_needed() {
    if ! [ -f "$MYSQL_COMMAND_FILE_TO_GET_STUDY_LIST" ] ; then
        command_string="select cancer_study_identifier, cancer_study_id, import_date from cancer_study order by cancer_study_identifier, cancer_study_id;"
        if ! echo "$command_string" > "$MYSQL_COMMAND_FILE_TO_GET_STUDY_LIST" ; then
            echo "Failure : could not create mysql command file $MYSQL_COMMAND_FILE_TO_GET_STUDY_LIST" >&2
            exit $EXIT_STATUS_FOR_TERMINATION # this only exits from subshell
        fi
    fi
}

# creates a mysql defaults file for use with the mysql client program
# usage : write_mysql_defaults_file file_path_to_write user_name password hostname
function write_mysql_defaults_file() {
    filepath="$1"
    db_user="$2"
    db_password="$3"
    db_host="$4"
    rm -f "$filepath"
    if ! ( umask 077 ; echo -e "[client]\nuser=\"$db_user\"\npassword=\"$db_password\"\nhost=\"$db_host\"" > "$filepath" ) ; then
        echo "Failure : could not write mysql defaults file '$filepath'" >&2
        exit $EXIT_STATUS_FOR_TERMINATION # this only exits from subshell
    fi
}

# writes a study list with import_dates to a file for a specified database (uses mysql client)
# helper files written as well (mysql command file, and mysql defaults file)
# usage : write_current_study_list_from_db db_label output_filepath
function write_current_study_list_from_db() {
    db="$1"
    output_filepath="$2"
    create_mysql_command_file_if_needed
    extractPropertiesFromConfigMapFile ${PORTAL_DB_TO_CONFIG_MAP_FILE[$db]} DB_USER DB_PASSWORD DB_HOST DB_PORTAL_DB_NAME
    db_user="${extracted_properties[DB_USER]}"
    db_password="${extracted_properties[DB_PASSWORD]}"
    db_host="${extracted_properties[DB_HOST]}"
    db_name="${extracted_properties[DB_PORTAL_DB_NAME]}"
    mysql_defaults_filepath="$TMP_DIR_ROOT/${db}_mysql_defaults.cnf"
    write_mysql_defaults_file "$mysql_defaults_filepath" "$db_user" "$db_password" "$db_host"
    if ! mysql --defaults-extra-file=$mysql_defaults_filepath $db_name < $MYSQL_COMMAND_FILE_TO_GET_STUDY_LIST > $output_filepath ; then
        echo "Warning : mysql client command exited with non-zero status" >&2
        return 1
    fi
    return 0
}

# compares two lists (snapshots) of study info for deleted/added/modified studies and sets newly_modified_study_list environment variable with results
# usage : find_studies_deleted_added_or_changed filepath_with_old_file_info filepath_with_new_info
function find_studies_deleted_added_or_changed() {
    previous_study_info_filepath="$1"
    new_study_info_filepath="$2"
    tmp_modified_study_filepath=$( mktemp --tmpdir=$TMP_DIR_ROOT tmp_modified_study_filepath.tmpXXXXXXXX)
    diff "$previous_study_info_filepath" "$new_study_info_filepath" | grep -E "^[<>] " | cut -f 1 | cut -c 3- | sort | uniq > "$tmp_modified_study_filepath"
    newly_modified_study_list=()
    while read line ; do
        newly_modified_study_list+=( $line )
    done < "$tmp_modified_study_filepath"
    rm -f $tmp_modified_study_filepath
}

# looks for pairs of matching trigger/succeeded status files and deletes them
# usage : clear_completed_triggers db_label
function clear_completed_triggers() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    cache_reset_needed_filepath="$tmp_study_subdir/$CACHE_RESET_NEEDED_FILENAME"
    cache_reset_succeeded_filepath="$tmp_study_subdir/$CACHE_RESET_SUCCEEDED_FILENAME"
    if [ -f "$cache_reset_succeeded_filepath" ] ; then
        #after a successful reset of the cache, all triggers and statys for the database can be removed
        rm -f "$cache_reset_succeeded_filepath" "$cache_reset_needed_filepath" "$tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX" "$tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX"
    fi
    for filepath in $tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX ; do
        if [ -f "$filepath" ] ; then
            filename="$(basename -- $filepath)"
            filerootname="${filename%.*}"
            trigger_filepath="$tmp_study_subdir/$filerootname.$STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX"
            succeeded_filepath="$tmp_study_subdir/$filerootname.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX"
            rm -f "$trigger_filepath" "$succeeded_filepath"
        fi
    done
}

# wipes the study specific files in the trigger directory and creates a trigger to reset the whole cache for the database
# usage : trigger_db_as_needing_reset db_label
function trigger_db_as_needing_reset() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    cache_reset_needed_filepath="$tmp_study_subdir/$CACHE_RESET_NEEDED_FILENAME"
    rm -f $tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX $tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX
    touch "$cache_reset_needed_filepath"
}

# returns 0 if full cache reset trigger is present
# usage : database_is_triggered_as_needing_reset db_label
function database_is_triggered_as_needing_reset() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    cache_reset_needed_filepath="$tmp_study_subdir/$CACHE_RESET_NEEDED_FILENAME"
    if [ -f "$cache_reset_needed_filepath" ] ; then
        return 0
    fi
    return 1
}

# examines age of each trigger file in specified db tmp_subdir and returns 0 if any is older than MAXIMUM_TIME_FOR_STUDY_RESET_BEFORE_ESCALATION
# usage : any_trigger_has_exceeded_maximum_wait db_label
function any_trigger_has_exceeded_maximum_wait() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    # get current epoc seconds
    tmp_modified_filepath=$( mktemp --tmpdir=$TMP_DIR_ROOT tmp_timestamp_filepath.tmpXXXXXXXX)
    now_seconds_since_epoc=$( stat -c%Y "$tmp_modified_filepath" )
    rm -f "$tmp_modified_filepath"
    # get oldest trigger file epoc seconds
    for triggerfilepath in $tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX ; do
        if [ -f "$triggerfilepath" ] ; then
            trigger_seconds_since_epoc=$( stat -c%Y "$triggerfilepath" )
            age_in_seconds=$(( $now_seconds_since_epoc - $trigger_seconds_since_epoc ))
            if [ $age_in_seconds -gt $MAXIMUM_TIME_FOR_STUDY_RESET_BEFORE_ESCALATION ] ; then
                return 0
            fi
        fi
    done
    return 1
}

# tests whether there is a defined cache api key and cache api uri for the database
# usage : database_can_clear_per_study_cache db_label
function database_can_clear_per_study_cache() {
    db="$1"
    if [ -z ${portal_db_to_cache_api_key[$db]} ] ; then
        # cannot clear study without cache api key
        return 1
    fi
    if [ -z ${PORTAL_DB_TO_PORTAL_API_BASE_URI[$db]} ] ; then
        # cannot clear study without api path
        return 1
    fi
    return 0
}

# gets the latest study info for a specified db and sees if anything has changed since the last execution of this script (trigger files created as side effect)
# usage : search_for_studies_with_new_modifications_in_db db_label
function search_for_studies_with_new_modifications_in_db() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    create_directory_if_necessary "$tmp_study_subdir"
    previous_study_info_from_db_filepath="$TMP_DIR_ROOT/${db}_study_info"
    new_study_info_from_db_filepath="$TMP_DIR_ROOT/${db}_study_info_new"
    if ! [ -f "$previous_study_info_from_db_filepath" ] ; then
        # if we have lost the previous study info, we cannot tell what has been modified. Prime the study info and trigger a full cache reset
        if ! write_current_study_list_from_db "$db" "$previous_study_info_from_db_filepath" ; then
            return 1
        fi
        trigger_db_as_needing_reset "$db"
    fi
    if ! write_current_study_list_from_db "$db" "$new_study_info_from_db_filepath" ; then
        return 1
    fi
    find_studies_deleted_added_or_changed "$previous_study_info_from_db_filepath" "$new_study_info_from_db_filepath"
}

# creates the trigger file for a study cache reset
# usage : trigger_study_as_needing_reset db_label study_id
function trigger_study_as_needing_reset() {
    db="$1"
    study_id="$2"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    study_reset_needed_filepath="$tmp_study_subdir/$study_id.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX"
    touch "$study_reset_needed_filepath"
}

# extracts the cache api key from a configmap yaml file. The key is stored in a global associative array
# usage : extract_cache_clearing_api_key db_label
function extract_cache_clearing_api_key() {
    db="$1"
    extractPropertiesFromConfigMapFile ${PORTAL_DB_TO_CONFIG_MAP_FILE[$db]} CACHE_API_KEY
    portal_db_to_cache_api_key[$db]="${extracted_properties[CACHE_API_KEY]}"
}

# replaces the previous study list snapshot with the latest (called after any necessary triggers have been created)
# usage : overwrite_previous_with_new_study_info db_label
function overwrite_previous_with_new_study_info() {
    db="$1"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    previous_study_info_from_db_filepath="$TMP_DIR_ROOT/${db}_study_info"
    new_study_info_from_db_filepath="$TMP_DIR_ROOT/${db}_study_info_new"
    if [ -f "$new_study_info_from_db_filepath" ] ; then
        mv "$new_study_info_from_db_filepath" "$previous_study_info_from_db_filepath"
    fi
}

# phase one top level logic - sets up / cleans up / creates the task triggers and status directories/content for every managed database
# also has logic to override study cache reset with a full cache reset when task backlog gets stale
function detect_and_maintain_needed_actions_for_databases() {
    create_directory_if_necessary "$TMP_DIR_ROOT"
    for portal_db in $PORTAL_DB_LIST ; do
        clear_completed_triggers "$portal_db"
        if ! database_is_triggered_as_needing_reset "$portal_db" ; then
            if any_trigger_has_exceeded_maximum_wait "$portal_db"; then
                trigger_db_as_needing_reset "$portal_db"
                continue # if a full cache reset is triggered, no other examinations are needed in this database
            fi
            if ! search_for_studies_with_new_modifications_in_db $portal_db ; then
                continue # if database access failed or timed out, skip this datbase during this execution
            fi
            # determine study cache clearning api key for database
            extract_cache_clearing_api_key "$portal_db"
            if database_can_clear_per_study_cache "$portal_db" ; then
                for study_id in "${newly_modified_study_list[@]}" ; do
                    trigger_study_as_needing_reset "$portal_db" "$study_id"
                done
            else
                if ! [ -z "${newly_modified_study_list[@]}" ] ; then
                    trigger_db_as_needing_reset "$portal_db"
                fi
            fi
            overwrite_previous_with_new_study_info "$portal_db"
        fi
    done
}

# tests whether an exclusive write file lock is obtainable for a file (provides mechanism for mutual exclusion of task workers)
# usage : flock_available_for_filepath filepath
function flock_available_for_filepath() {
    filepath="$1"
    if ! [ -f "$filepath" ] ; then
        return 1 # trigger does not exist
    fi
    (   # enter subshell
        if flock --nonblock --exclusive $filepath_fd ; then
            exit 0 # trigger exists and lock is available - exit from subshell
        fi
        exit 1 # trigger exists but lock is not available (another process is working on it) - exit from subshell
    ) {filepath_fd}>"$filepath"
    return $?
}

# looks for pending work which is available and sets globals selected_cache_reset_work_task_db and selected_cache_reset_work_task_trigger_filepath if anything is found
# cache reset tasks are preferred to study reset tasks
# usage : select_cache_reset_work_task_trigger db_label
function select_cache_reset_work_task_trigger() {
    # iterate through work task trigger files - skip any locked (in progress) tasks. completed task triggers should already have been cleared, so assume none are complete but catch race failure later in perform_selected_cache_reset_task
    # first check if any database reset task is pending
    for portal_db in $PORTAL_DB_LIST ; do
        selected_cache_reset_work_task_db="$portal_db"
        db_cache_reset_filepath="$TMP_DIR_ROOT/$portal_db/$CACHE_RESET_NEEDED_FILENAME"
        if flock_available_for_filepath "$db_cache_reset_filepath" ; then
            selected_cache_reset_work_task_trigger_filepath="$db_cache_reset_filepath"
            return 0
        fi
    done
    # second check if any study reset task is pending
    for portal_db in $PORTAL_DB_LIST ; do
        selected_cache_reset_work_task_db="$portal_db"
        tmp_study_subdir="$TMP_DIR_ROOT/$portal_db"
        for filepath in $tmp_study_subdir/*.$STUDY_CACHE_INVALIDATION_NEEDED_SUFFIX ; do
            if [ -f "$filepath" ] ; then
                if flock_available_for_filepath "$filepath" ; then
                    selected_cache_reset_work_task_trigger_filepath="$filepath"
                    return 0
                fi
            fi
        done
    done
    # nothing is pending
    return 1
}

# clears the persistence cache, either using the cache api for study cache reset tasks, or the kubectl based script for full database cache clearing tasks
# proper function depends on having the selected cache reset work task set in global environment variables
# usage : perform_selected_cache_reset_task
function perform_selected_cache_reset_task() {
    # we are already in a subshell (from the caller) and hold the lock for selected_cache_reset_work_task_trigger_filepath
    # handle full cache reset if triggered
    trigger_filename="$(basename -- $selected_cache_reset_work_task_trigger_filepath)"
    if [ "$trigger_filename" == "$CACHE_RESET_NEEDED_FILENAME" ] ; then
        # call the appropriate kubernetes full cache reset
        db_clear_cache_function=${portal_db_to_full_cache_clearing_function[$selected_cache_reset_work_task_db]}
        date
        echo "calling $db_clear_cache_function"
        if ! $db_clear_cache_function ; then
            sendClearCacheFailureMessage "$selected_cache_reset_work_task_db" clear-persistence-caches-for-changed-studies.sh
            return 1
        fi
        return 0
    fi
    # handle study cache reset if triggered and able
    if ! database_can_clear_per_study_cache "$selected_cache_reset_work_task_db" ; then
        return 1 # not able - fail
    fi
    # trigger is per-study cache reset (by deduction)
    study_id="${trigger_filename%.*}"
    cache_api_key="${portal_db_to_cache_api_key[$selected_cache_reset_work_task_db]}"
    cache_api_base_uri="${PORTAL_DB_TO_PORTAL_API_BASE_URI[$selected_cache_reset_work_task_db]}"
    curl -X DELETE -H "X-API-KEY:$cache_api_key" "$cache_api_base_uri/cache/$study_id" | grep -q "Flushed"
    return $?
}

# creates a succeeded status file which corresponds to the specified trigger filepath
# usage : mark_selected_cache_reset_task_succeeded db_label trigger_filepath
function mark_selected_cache_reset_task_succeeded() {
    db="$1"
    trigger_filepath="$2"
    tmp_study_subdir="$TMP_DIR_ROOT/$db"
    filename="$(basename -- $trigger_filepath)"
    # handle full cache reset
    if [ "$filename" == "$CACHE_RESET_NEEDED_FILENAME" ] ; then
        succeeded_filepath="$tmp_study_subdir/$CACHE_RESET_SUCCEEDED_FILENAME"
        touch "$succeeded_filepath"
        return 0
    fi
    # handle per-study cache reset
    filerootname="${filename%.*}"
    succeeded_filepath="$tmp_study_subdir/$filerootname.$STUDY_CACHE_INVALIDATION_SUCCEEDED_SUFFIX"
    touch "$succeeded_filepath"
    return 0
}

# main script execution
# ------------------------------------------

# PHASE ONE : Derive list of studies which require cache clearing - generate or update the list of cache clearing work needed.
(   # enter subshell
    # check lock so that if a previous script execution is still in PHASE ONE, this execution exits
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        date
        echo "Failure : could not acquire lock for $MY_FLOCK_FILEPATH : exiting to avoid interfering with another process in detection phase" >&2
        exit $EXIT_STATUS_FOR_TERMINATION # this only exits from subshell
    fi
    detect_and_maintain_needed_actions_for_databases
) {my_flock_fd}>$MY_FLOCK_FILEPATH
if [ $? -eq $EXIT_STATUS_FOR_TERMINATION ] ; then
    exit 1
fi

# PHASE TWO : now that the main lock is released, look for a single cache reset task to perform and do it
select_cache_reset_work_task_trigger
if [ -z $selected_cache_reset_work_task_trigger_filepath ] ; then
    exit 0 # no work to be done
fi
(   # enter subshell
    # obtain lock for selected task (or exit if some other process just obtained the lock)
    if ! flock --nonblock --exclusive $task_flock_fd ; then
        date
        echo "Failure : could not acquire lock for $selected_cache_reset_work_task_trigger_filepath : exiting to avoid interfering with another process working on that task" >&2
        exit $EXIT_STATUS_FOR_TERMINATION # this only exits from subshell
    fi
    if perform_selected_cache_reset_task ; then
        mark_selected_cache_reset_task_succeeded "$selected_cache_reset_work_task_db" "$selected_cache_reset_work_task_trigger_filepath"
    fi
) {task_flock_fd}>$selected_cache_reset_work_task_trigger_filepath
if [ $? -eq $EXIT_STATUS_FOR_TERMINATION ] ; then
    exit 1
fi
exit 0
