#!/usr/bin/env bash

# transfer-deployment-color.sh
#
# switch the (updated) standby msk web app deployments to become the new production msk web app deployments
# and move the prior prodution deployment into a standby state
#
# the main required argument is the color ('green' or 'blue') which should become the new production deployment
# the main steps executed will be:
# - validate arguments
# - scale down the replicas on the production deployment slightly, scale up the standy deployment. Allow time for pod readiness.
# - scale down the replicas on the production deployment almost fully, scale up the standy deployment. Allow time for pod readiness.
# - clear the persistence caches for the current standby database
# - switch the ingress rules to route website traffic to the incoming (standby until now) deployment
# - scale down the prior production deployment fully, scale up the new production deployment fully. Allow time for pod readiness.
# - construct and check in to github repo the altered kubernetes configuration files

#######################################
# Global Variable Definitions
#######################################
declare -a BLUE_DEPLOYMENT_LIST=() # kubernetes deployment names for all connected blue deployments
declare -a GREEN_DEPLOYMENT_LIST=() # kubernetes deployment names for all connected green deployments
declare -A DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP=() # { cbioportal-backend-public-blue: 2 } means 2 replicas when fully deployed
declare -A DEPLOYMENT_TO_CACHE_WARMING_POLICY=() # how much to warm the cache (e.g. 'initial', 'studylist') per deployment
declare -A DEPLOYMENT_TO_YAML_FILEPATH_MAP=() # relative path from KS_K8S_DEPL_REPO_DIRPATH to a yaml file specifying the deployment resource for each named deployment
declare -A HOST_TO_SERVICE_MAP_BLUE=() # the name of the blue service connected to each deployed host/website
declare -A HOST_TO_SERVICE_MAP_GREEN=() # see prior comment
declare -A HOST_TO_INGRESS_NAME_MAP=() # for each host connected to the database, the name of the ingress resource it uses
declare -A HOST_TO_INGRESS_TYPE_MAP=() # the type of each ingress resource mapped (either 'ingress' or 'ingressroute')
declare -A HOST_TO_INGRESS_YAML_FILEPATH_MAP=() # relative path from KS_K8S_DEPL_REPO_DIRPATH to a yaml file specifying the primary ingress resource related to the host
#######################################
# Other Global Variable List
#######################################
# set in automation-environment.sh
#   YQ_BINARY
# set in color swap config file (see load_color_swap_config())
#   SERVICE_ACCOUNT
#   CLUSTER_KUBECONFIG
#   TEMP_DIR_PATH
#   KS_K8S_DEPL_REPO_DIRPATH
#   REPLICA_READY_CHECK_PAUSE_SECONDS
#   REPLICA_READY_CHECK_MAX_CHECKCOUNT
#   WARM_CACHES
#   SYNCHRONIZE_USER_TABLES
#   CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION
#   CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION
#   GIT_COMMIT_MSG
#   CACHE_WARMING_POD_LIST_FILEPATH
#   CACHE_WARMING_POD_SUBLIST_FILEPATH
#   CACHE_WARMING_USERNAME
#   CACHE_WARMING_PASSWORD

#######################################
# Load needed config properties into global variables
# Globals:
#   YQ_BINARY path to yq executable
#   SERVICE_ACCOUNT the name of the service account selector for use with aws authentication
#   CLUSTER_KUBECONFIG path to a .kube/config style function defining the kubectl cluster to interact with
#   TEMP_DIR_PATH
#   KS_K8S_DEPL_REPO_DIRPATH the path to the directory contain the clone of knowledgesystems-k8s-deployments.
#           This repository clone is used and also updated (changes are checked in to github with `git push`)
#   REPLICA_READY_CHECK_PAUSE_SECONDS pause period between checks if deployment changes are complete
#   REPLICA_READY_CHECK_MAX_CHECKCOUNT maximum limit of deployment change completion check cycles
#   WARM_CACHES 'true' if this database requires cache warming during blue/green color swapping
#   SYNCHRONIZE_USER_TABLES 'true' if this database requires the user and authorities tables to be synchronized
#           During blue/green color swapping. (records will be copied from old to new color at time of swap)
#   CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION bash function (e.g. clearPersistenceCachesForPublicBluePortals) to be
#           called to clear the blue caches. These are defined in clear-persistence-cache-shell-functions.sh
#   CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION see prior comment
#   GIT_COMMIT_MSG text to be prepended to the date in the git commit log message when pushing changes to github
#           (e.g. "public import" becomes "public import 2026-03-16")
#   BLUE_DEPLOYMENT_LIST an array holding all the blue deployment/service names connected to a database
#   GREEN_DEPLOYMENT_LIST an array holding all the green deployment/service names connected to a database
#   DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP # { cbioportal-backend-public-blue: 2 } means 2 replicas when fully deployed
#   DEPLOYMENT_TO_YAML_FILEPATH_MAP relative path from KS_K8S_DEPL_REPO_DIRPATH to a yaml file specifying the
#           deployment resource for each named deployment
#   HOST_TO_SERVICE_MAP_BLUE the name of the blue service connected to each deployed host/website
#   HOST_TO_SERVICE_MAP_GREEN see prior comment
#   HOST_TO_INGRESS_NAME_MAP for each host connected to the database, the name of the ingress resource it uses
#   HOST_TO_INGRESS_TYPE_MAP the type of each ingress resource mapped (either 'ingress' or 'ingressroute')
#   HOST_TO_INGRESS_YAML_FILEPATH_MAP relative path from KS_K8S_DEPL_REPO_DIRPATH to a yaml file specifying the
#           primary ingress resource related to the host
#   CACHE_WARMING_POD_LIST_FILEPATH a temporary file location for dumping/reading all pods
#   CACHE_WARMING_POD_SUBLIST_FILEPATH a temporary file location for dumping/reading deployment pods
#   CACHE_WARMING_USERNAME username for basic authentication allowing queries to warm caches
#   CACHE_WARMING_PASSWORD password for basic authentication allowing queries to warm caches
#   DEPLOYMENT_TO_CACHE_WARMING_POLICY # how much to warm the cache (e.g. 'initial', 'studylist') per deployment
#   UPDATES_DISABLED when this is not 'false', verify-management-state.sh will exit early with a failure code
# Arguments:
#   path to color swap config file to access kubernetes cluster configuration resources
# Output:
#   none, except for error messages to stderr
# Side effects:
#   global variables set
#######################################
function load_color_swap_config() {
    local config_path="$1"
    if ! [ -f "$config_path" ] || ! [ -r "$config_path" ] ; then
        echo "Error : unable to read config file '$config_path'" >&2
        exit 1
    fi
    SERVICE_ACCOUNT=$(read_scalar_from_yaml "$config_path" '.service_account')
    CLUSTER_KUBECONFIG=$(read_scalar_from_yaml "$config_path" '.cluster_cfg')
    TEMP_DIR_PATH=$(read_scalar_from_yaml "$config_path" '.temp_dir_path')
    KS_K8S_DEPL_REPO_DIRPATH=$(read_scalar_from_yaml "$config_path" '.ks_k8s_depl_repo_dirpath')
    REPLICA_READY_CHECK_PAUSE_SECONDS=$(read_scalar_from_yaml "$config_path" '.replica_ready_check_pause_seconds')
    REPLICA_READY_CHECK_MAX_CHECKCOUNT=$(read_scalar_from_yaml "$config_path" '.replica_ready_check_max_checkcount')
    WARM_CACHES=$(read_scalar_from_yaml "$config_path" '.warm_caches')
    SYNCHRONIZE_USER_TABLES=$(read_scalar_from_yaml "$config_path" '.synchronize_user_tables')
    CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION=$(read_scalar_from_yaml "$config_path" '.clear_persistence_caches_blue_function')
    CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION=$(read_scalar_from_yaml "$config_path" '.clear_persistence_caches_green_function')
    GIT_COMMIT_MSG=$(read_scalar_from_yaml "$config_path" '.git_commit_msg')
    read_array_from_yaml BLUE_DEPLOYMENT_LIST "$config_path" '.blue_deployment_list'
    read_array_from_yaml GREEN_DEPLOYMENT_LIST "$config_path" '.green_deployment_list'
    read_map_from_yaml DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP "$config_path" '.deployment_to_full_replica_count_map'
    read_map_from_yaml DEPLOYMENT_TO_YAML_FILEPATH_MAP "$config_path" '.deployment_to_yaml_filepath_map'
    read_map_from_yaml HOST_TO_SERVICE_MAP_BLUE "$config_path" '.host_to_service_map.blue'
    read_map_from_yaml HOST_TO_SERVICE_MAP_GREEN "$config_path" '.host_to_service_map.green'
    read_map_from_yaml HOST_TO_INGRESS_NAME_MAP "$config_path" '.host_to_ingress_name_map'
    read_map_from_yaml HOST_TO_INGRESS_TYPE_MAP "$config_path" '.host_to_ingress_type_map'
    read_map_from_yaml HOST_TO_INGRESS_YAML_FILEPATH_MAP "$config_path" '.host_to_ingress_yaml_filepath_map'
    if [ "$WARM_CACHES" == "true" ]; then
        CACHE_WARMING_POD_LIST_FILEPATH=$(read_scalar_from_yaml "$config_path" '.cache_warming_pod_list_filepath')
        CACHE_WARMING_POD_SUBLIST_FILEPATH=$(read_scalar_from_yaml "$config_path" '.cache_warming_pod_sublist_filepath')
        CACHE_WARMING_USERNAME=$(read_scalar_from_yaml "$config_path" '.cache_warming_username')
        CACHE_WARMING_PASSWORD=$(read_scalar_from_yaml "$config_path" '.cache_warming_password')
        read_map_from_yaml DEPLOYMENT_TO_CACHE_WARMING_POLICY "$config_path" '.deployment_to_cache_warming_policy'
    fi
}

function usage() {
    echo "usage: transfer-deployment-color.sh db-properties-file color-swap-config-file destination-color"
    echo "       where destination-color is one of {'green', 'blue'}"
    exit 1
}

#######################################
# Exit with error if main() was called with inappropriate arguments
# Gobals:
#   none
# Arguments:
#   all arguements received by main(), 3 expected:
#     MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH
#     COLOR_SWAP_CONFIG_FILEPATH
#     DESTINATION_COLOR
# Output:
#   none, except for error messages to stderr
# Side effects:
#   process exit if argument count is wrong or file paths are not readable
#   or color is outside of {'blue', 'green'}
#######################################
function validate_arguments() {
    if [ $# -ne "3" ] ; then
        usage
    fi
    if ! [ -f $1 ] || ! [ -r $1 ] ; then
        echo "Error : unable to read file '$1'" >&2
        usage
    fi
    if ! [ -f $2 ] || ! [ -r $2 ] ; then
        echo "Error : unable to read file '$2'" >&2
        usage
    fi
    if ! [ $3 == "blue" ] && ! [ $3 == "green" ] ; then
        echo "Error : destination-color '$3' is unrecognized. It must be 'green' or 'blue'" >&2
        usage
    fi
}

#######################################
# Exit with error if production database color does not match argument
# Gobals:
#   none
# Arguments:
#   MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH
#   SOURCE_COLOR
# Output:
#   none, except for error messages to stderr
# Side effects:
#   process exit if SOURCE_COLOR is not the production database
#######################################
function check_current_color_is() {
    local MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    local SOURCE_COLOR=$2
    local GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="/data/portal-cron/scripts/get_database_currently_in_production.sh"
    local CURRENT_DATABASE_OUTPUT_FILEPATH="$TEMP_DIR_PATH/get_current_database_output_before_switch.txt"
    rm -f $CURRENT_DATABASE_OUTPUT_FILEPATH
    if ! $GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH > $CURRENT_DATABASE_OUTPUT_FILEPATH; then
        echo "Error during determination of the source database color" >&2
        exit 1
    else
        current_production_database_color=$(tail -n 1 "$CURRENT_DATABASE_OUTPUT_FILEPATH")
        current_database_color="unset"
        if [ ${current_production_database_color:0:4} == "blue" ] ; then
            current_database_color="blue"
        fi
        if [ ${current_production_database_color:0:5} == "green" ] ; then
            current_database_color="green"
        fi
        if [ "$current_database_color" == "unset" ] ; then
            echo "Error during determination of the source database color" >&2
            exit 1
        fi
    fi
    rm -f $CURRENT_DATABASE_OUTPUT_FILEPATH
    if ! [ "$SOURCE_COLOR" == "$current_database_color" ] ; then
        echo "Error : expected current deployment color to be '$SOURCE_COLOR', however current deployment color is '$current_database_color'. Exiting." >&2
        exit 1
    fi
}

#######################################
# Detects if git repo clone is up to date with origin/master
# Gobals:
#   KS_K8S_DEPL_REPO_DIRPATH
# Arguments:
#   none
# Output:
#   none, except for error messages to stderr
# Side effects:
#   none
# Returns:
#   0 if current, 1 otherwise
#######################################
function git_repo_clone_is_current() {
    # note : the next statement defines a multi-line string
    expected_up_to_date_status_report="On branch master
Your branch is up to date with 'origin/master'."
    $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH checkout master > /dev/null 2>&1
    $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH pull > /dev/null 2>&1
    status_report=$($GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH status | head -n 2)
    if [ "$expected_up_to_date_status_report" == "$status_report" ] ; then
        return 0
    else
        echo "git repository does not appear to be current"
        echo "expected response from command '$GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH status' was:"
        echo "$expected_up_to_date_status_report"
        echo "however, response actually received was:"
        echo "$status_report"
        echo nomatch
        return 1
    fi
}

#######################################
function check_that_git_repo_clone_is_current() {
    if ! git_repo_clone_is_current ; then
        exit 1
    fi
}

#######################################
# Detects if one kubernetes yaml file matches what is configured in the cluster
# Gobals:
#   KS_K8S_DEPL_REPO_DIRPATH
#   CLUSTER_KUBECONFIG
# Arguments:
#   yaml_filepath (relative to the root directory of the repository clone)
# Output:
#   none, except for error messages to stderr
# Side effects:
#   none
# Returns:
#   0 if no differences detected, 1 otherwise
#######################################
function yaml_file_is_current_with_production() {
    local yaml_filepath=$1
    local full_yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
    kubectl --kubeconfig $CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath" > /dev/null 2>&1
    local diff_status=$?
    if [ $diff_status -eq 1 ] ; then
        # mismatch
        echo "when checking for currency of yaml specificiations in file '$full_yaml_filepath', these differences were found:"
        kubectl --kubeconfig $CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath"
        return 1
    fi
    if [ $diff_status -gt 1 ] ; then
        # error
        echo "an error occurred when checking the currency of yaml specificiations in file '$full_yaml_filepath'. output:"
        kubectl --kubeconfig $CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath"
        return 1
    fi
    return 0
}

#######################################
# Detects if all relevant kubernetes yaml files match what is configured in the cluster
# Gobals:
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   KS_K8S_DEPL_REPO_DIRPATH
#   DEPLOYMENT_TO_YAML_FILEPATH_MAP
#   HOST_TO_INGRESS_YAML_FILEPATH_MAP
# Arguments:
#   yaml_filepath (relative to the root directory of the repository clone)
# Output:
#   none, except for error messages to stderr
# Side effects:
#   none
# Returns:
#   0 if no differences detected, 1 otherwise
#######################################
function git_repo_clone_matches_cluster_config() {
    local all_yaml_files_are_current="yes"
    local pos=0
    local deployment
    local yaml_filepath
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${BLUE_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! yaml_file_is_current_with_production "$yaml_filepath" ; then
            echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
            all_yaml_files_are_current="no"
        fi
        pos=$(($pos+1))
    done
    pos=0
    while [ $pos -lt ${#GREEN_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${GREEN_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! yaml_file_is_current_with_production "$yaml_filepath" ; then
            echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
            all_yaml_files_are_current="no"
        fi
        pos=$(($pos+1))
    done
    for yaml_filepath in ${HOST_TO_INGRESS_YAML_FILEPATH_MAP[*]} ; do
        if ! yaml_file_is_current_with_production "$yaml_filepath" ; then
            echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
            all_yaml_files_are_current="no"
        fi
    done
    if ! [ "$all_yaml_files_are_current" == "yes" ] ; then
        echo "current master branch of kubernetes yaml repo does not match the production environment"
        return 1
    fi
    return 0
}

#######################################
function check_that_git_repo_clone_matches_cluster_config() {
    if ! git_repo_clone_matches_cluster_config ; then
        exit 1
    fi
}

#######################################
# Detects if all relevant deployments in the kubernetes cluster show all replicas "ready"
# Gobals:
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   TEMP_DIR_PATH
#   CLUSTER_KUBECONFIG
#   BASH_REMATCH (shell built-in global for RE processing)
# Arguments:
#   deployment_color to test (only deployments of this color are tested)
# Output:
#   none, except for error messages to stderr
# Side effects:
#   none
# Returns:
#   0 if all deployments fully 'ready', 1 otherwise
#######################################
function all_replicas_ready() {
    local deployment_color=$1
    local DEPLOYMENT_CHECK_OUTPUT_FILEPATH="$TEMP_DIR_PATH/all_replicas_ready_output.txt"
    # query relevant deployments with kubectl and capture output
    if [ $deployment_color == 'blue' ] ; then
        kubectl --kubeconfig $CLUSTER_KUBECONFIG get deployments ${BLUE_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    else
        if [ $deployment_color == 'green' ] ; then
            kubectl --kubeconfig $CLUSTER_KUBECONFIG get deployments ${GREEN_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
        else
            echo "Error : invalid argument '$deployment_color' passed to all_replicas_ready()" >&2
            exit 1
        fi
    fi
    # analyze captured output
    local headers_read=0
    while IFS='' read -r line || [ -n "$line" ] ; do # -n "$line" will allow processing of lines which reach EOF before encountering newline
        if [ $headers_read -eq 0 ] ; then
            headers_read=1
        else
            local PARSE_LINE_REGEX='^([[:graph:]]*)[[:space:]]*([[:digit:]]*)/([[:digit:]])*.*'
            if ! [[ $line =~ $PARSE_LINE_REGEX ]] ; then
                echo "Error : failure to parse format of get deployment output : $line" >&2
                exit 1
            fi
            local deployment_name=${BASH_REMATCH[1]}
            local ready_pods=${BASH_REMATCH[2]}
            local total_pods=${BASH_REMATCH[3]}
            if [ $ready_pods -ne $total_pods ] ; then
                rm -f $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
                return 1 # not all ready yet
            fi
        fi
    done < $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    rm -f $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    return 0 # all ready
}

#######################################
# Detects if all relevant deployments in the kubernetes cluster show all replicas "ready"
# Gobals:
#   DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP
# Arguments:
#   deployment_name
#   replica_count_string from among {'none', 'almost_none', 'almost_full', 'full'}
# Output:
#   none
# Side effects:
#   none
# Returns:
#   the number of desired replicas for the specified deployment
#    full -> none,almost_none,almost_full,full
#     0 -> 0,0,0,0
#     1 -> 0,0,1,1
#     2 -> 0,1,1,2
#     3 -> 0,1,2,3
#     4 -> 0,1,3,4
#     5 -> 0,1,4,5
#######################################
function replica_count_string_to_integer() {
    local deployment_name=$1
    local replica_count_string=$2
    local full_replica_count=${DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP[$deployment_name]}
    if [ "$full_replica_count" -gt 255 ] || [ "$full_replica_count" -lt 0 ] ; then
        echo "Error : full replica count $full_replica_count for deployment $deployment_name is out of range [0,255]" >&2
        exit 1
    fi
    if [ "$replica_count_string" == "none" ] ; then
        return 0
    fi
    if [ "$replica_count_string" == "almost_none" ] ; then
        if [ "$full_replica_count" -le 1 ] ; then
            return 0
        fi
        return 1
    fi
    if [ "$replica_count_string" == "almost_full" ] ; then
        if [ "$full_replica_count" -eq 0 ] ; then
            return 0
        fi
        if [ "$full_replica_count" -eq 1 ] ; then
            return 1
        fi
        return $(($full_replica_count-1))
    fi
    if [ "$replica_count_string" == "full" ] ; then
        return $full_replica_count
    fi
    return -1
}

#######################################
# Scale all kuberenets deployments related to a color and wait for ready
# Gobals:
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   CLUSTER_KUBECONFIG
#   REPLICA_READY_CHECK_MAX_CHECKCOUNT
#   REPLICA_READY_CHECK_PAUSE_SECONDS
# Arguments:
#   deployment_name
#   replica_count_string from among {'none', 'almost_none', 'almost_full', 'full'}
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   kubernetes cluster configuration changed, pods killed or launched
#######################################
function scale_deployment_to_N_replicas() {
    local deployment_color=$1
    local num_replicas=$2 # "none", "almost_none", "almost_full, or "full"
    # scale all deployments with kubectl
    local deployment
    local replica_count
    local pos
    if [ $deployment_color == 'blue' ] ; then
        pos=0
        while [ "$pos" -lt "${#BLUE_DEPLOYMENT_LIST[@]}" ] ; do
            deployment="${BLUE_DEPLOYMENT_LIST[$pos]}"
            replica_count_string_to_integer "$deployment" "$num_replicas"
            replica_count=$?
            kubectl --kubeconfig $CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
            pos=$(($pos+1))
        done
    else
        if [ $deployment_color == 'green' ] ; then
            pos=0
            while [ "$pos" -lt "${#GREEN_DEPLOYMENT_LIST[@]}" ] ; do
                deployment="${GREEN_DEPLOYMENT_LIST[$pos]}"
                replica_count_string_to_integer "$deployment" "$num_replicas"
                replica_count=$?
                kubectl --kubeconfig $CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
                pos=$(($pos+1))
            done
        else
            echo "Error : invalid argument '$deployment_color' passed to scale_deployment_to_N_replicas()" >&2
            exit 1
        fi
    fi
    # now wait for replicas to become ready
    local check_count=0
    while [ $check_count -lt $REPLICA_READY_CHECK_MAX_CHECKCOUNT ] ; do
        sleep $REPLICA_READY_CHECK_PAUSE_SECONDS
        if all_replicas_ready $deployment_color ; then
            return 0
        fi
        check_count=$(($check_count+1))
    done
    echo "Error : scaling for '$deployment_color' deployments failed to reach the ready state after $REPLICA_READY_CHECK_MAX_CHECKCOUNT iterations with a period of $REPLICA_READY_CHECK_PAUSE_SECONDS seconds. Exiting" >&2
    exit 1
}

#######################################
# predicate function : should cache warming phase occur under specified policy?
# Gobals:
#   none
# Arguments:
#   cache_warming_policy from among {'initial', 'studylist'}
#   current_phase from among {'initial', 'study_list'}
# Output:
#   none
# Side effects:
#   none
# Returns:
#   0 when cache warming phase is necessary by policy, otherwise 1
# Note:
#   this function supports planned expansion of cache warming to new phases
#######################################
function cache_warming_stage_should_happen() {
    local cache_warming_policy="$1"
    local current_phase="$2"
    case "$current_phase" in
        "initial")
            if [ "$cache_warming_policy" == "initial" ] || [ "$cache_warming_policy" == "studylist" ] ; then
                return 0
            else
                return 1
            fi
            ;;
        "studylist")
            if [ "$cache_warming_policy" == "studylist" ] ; then
                return 0
            else
                return 1
            fi
            ;;
        *)
            return 1
            ;;
    esac
}

#######################################
# make api requests to warm backend cache of a single pod - wait for resopnse only if asked to do so
# Gobals:
#   CLUSTER_KUBECONFIG
#   CACHE_WARMING_USERNAME
#   CACHE_WARMING_PASSWORD
#   BASH_REMATCH (shell built-in global for RE processing)
# Arguments:
#   podname (target of curl requests)
#   should_wait_for_response (value 'wait_for_response' causes wait, otherwise no wait)
# Output:
#   none
# Side effects:
#   pods receive http requests for study list, which launches response thread in replica
#           persistence level caches are automatically populated by pods
#######################################
function run_cache_warming_study_query() {
    local podname="$1"
    local should_wait_for_response="$2"
    if [ "$should_wait_for_response" == "wait_for_response" ] ; then
        kubectl --kubeconfig "$CLUSTER_KUBECONFIG" exec "$podname" -- /bin/bash -c 'session_header=$(curl "http://localhost:8888/j_spring_security_check?j_username='"$CACHE_WARMING_USERNAME"'&j_password='"$CACHE_WARMING_PASSWORD"'" -I 2>/dev/null | grep "Set-Cookie") ; COOKIE_RE="Set-Cookie: ([[:graph:]]+);.*" ; [[ $session_header =~ $COOKIE_RE ]] ; header_value="Cookie: ${BASH_REMATCH[1]}" ; cacheoutfile="/tmp/cachewarmer.txt" ; cache_is_warm="no" ; attempts_remaining=4 ; while true ; do rm -f $cacheoutfile ; curl --fail --max-time 10 --connect-timeout 3 --header "$header_value" "http://localhost:8888/api/studies" > $cacheoutfile 2>/dev/null ; if grep -q mskimpact $cacheoutfile ; then cache_is_warm="yes" ; break ; fi ; if [ $attempts_remaining -eq 0 ] ; then break ; fi ; attempts_remaining=$(($attempts_remaining-1)) ; sleep 60 ; done ; curl --header "$header_value" "https://cbioportal.mskcc.org/j_spring_security_logout/" 2>/dev/null ; if [ $cache_is_warm == "yes" ] ; then echo "cache is warm" ; exit 0 ; fi ; echo "cache is cold" ; exit 1'
    else
        kubectl --kubeconfig "$CLUSTER_KUBECONFIG" exec "$podname" -- /bin/bash -c 'session_header=$(curl "http://localhost:8888/j_spring_security_check?j_username='"$CACHE_WARMING_USERNAME"'&j_password='"$CACHE_WARMING_PASSWORD"'" -I 2>/dev/null | grep "Set-Cookie") ; COOKIE_RE="Set-Cookie: ([[:graph:]]+);.*" ; [[ $session_header =~ $COOKIE_RE ]] ; header_value="Cookie: ${BASH_REMATCH[1]}" ; curl --fail --max-time 3 --connect-timeout 3 --header "$header_value" "http://localhost:8888/api/studies" > /dev/null 2>/dev/null ; sleep 1 ; curl --header "$header_value" "https://cbioportal.mskcc.org/j_spring_security_logout/" 2>/dev/null ; exit 0'
    fi
}

#######################################
# perform multi(now 2)-phase cache warming based on per-deployment policies
# Gobals:
#   CLUSTER_KUBECONFIG
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   DEPLOYMENT_TO_CACHE_WARMING_POLICY
#   CACHE_WARMING_POD_LIST_FILEPATH
#   CACHE_WARMING_POD_SUBLIST_FILEPATH
# Arguments:
#   DESTINATION_COLOR
# Output:
#   timestamped progress messages to stdout
# Side effects:
#   pods receive http requests for study list, which launches response thread in replica
#           persistence level caches are automatically populated by pods
#######################################
function attempt_to_warm_caches_of_incoming_deployments() {
    local DESTINATION_COLOR=$1
    echo "attempting to warm caches"
    rm -f "$CACHE_WARMING_POD_LIST_FILEPATH"
    kubectl --kubeconfig "$CLUSTER_KUBECONFIG" get pods > "$CACHE_WARMING_POD_LIST_FILEPATH"
    local deployment_name
    local cache_warming_policy
    local podname
    local pos
    # initial stage of warming - make study list query without waiting for reply
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        # this code assumes that the blue deployment list length is equal to the green length
        deployment_name=${BLUE_DEPLOYMENT_LIST[$pos]}
        if [ $DESTINATION_COLOR == "green" ] ; then
            deployment_name=${GREEN_DEPLOYMENT_LIST[$pos]}
        fi
        cache_warming_policy=${DEPLOYMENT_TO_CACHE_WARMING_POLICY[$deployment_name]}
        if cache_warming_stage_should_happen $cache_warming_policy "initial" ; then
            rm -f "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
            cat "$CACHE_WARMING_POD_LIST_FILEPATH" | grep "$deployment_name" > "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
            while IFS='' read -r line || [ -n "$line" ] ; do
                podname=${line%% *}
                date
                echo "trying query_only warming on pod $podname"
                run_cache_warming_study_query "$podname" "query_only"
                break # only one pod from the group needs to be queried to warm the cache
            done < "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
        fi
        pos=$(($pos+1))
    done
    # second stage of warming - make study list query and wait for reply
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        # this code assumes that the blue deployment list length is equal to the green length
        local deployment_name=${BLUE_DEPLOYMENT_LIST[$pos]}
        if [ $DESTINATION_COLOR == "green" ] ; then
            deployment_name=${GREEN_DEPLOYMENT_LIST[$pos]}
        fi
        cache_warming_policy=${DEPLOYMENT_TO_CACHE_WARMING_POLICY[$deployment_name]}
        if cache_warming_stage_should_happen $cache_warming_policy "studylist" ; then
            rm -f "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
            cat "$CACHE_WARMING_POD_LIST_FILEPATH" | grep "$deployment_name" > "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
            while IFS='' read -r line || [ -n "$line" ] ; do
                podname=${line%% *}
                date
                echo "trying wait_for_response warming on pod $podname"
                run_cache_warming_study_query "$podname" "wait_for_response"
                break # only one pod from the group needs to be queried to warm the cache
            done < "$CACHE_WARMING_POD_SUBLIST_FILEPATH"
        fi
        pos=$(($pos+1))
    done
}

#######################################
# modify an ingressroute yaml file to set all services connected to a route to the destination color
# Gobals:
#   YQ_BINARY 
# Arguments:
#   yaml filepath to file which should be updated
#   destination color ('blue' or 'green')
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   file at the designated path is modified on the filesystem <servicebasename>-blue replaced with <servicebasename>-green or vice versa
#######################################
function rewrite_ingressroute_file_for_new_color() {
    local ingress_yaml_filepath=$1
    local DESTINATION_COLOR=$2
    # get the full list of routes based on the "match" property
    unset route_match_array
    declare -a route_match_array
    readarray -t route_match_array < <($YQ_BINARY "select(documentIndex == 0) | .spec.routes[].match" $ingress_yaml_filepath)
    # rewrite each route's service to switch it to the destination color
    local source_color='blue' # the other color which is not destination
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        source_color='green'
    fi
    local pos=0
    while [ $pos -lt ${#route_match_array[*]} ] ; do
        route_match="${route_match_array[$pos]}"
        # get the service linked to the route match
        IFS='' read -r existing_service_name < <($YQ_BINARY "select(documentIndex == 0) | .spec.routes[] | select(.match == \"$route_match\" ) | .services[].name" $ingress_yaml_filepath | head -n 1)
        pos=$(($pos+1))
        if [ -z $existing_service_name ] ; then
            continue # skip cases where no service is defined for the route
        fi
        local service_without_color=${existing_service_name//"$source_color"}
        local length_difference=$((${#existing_service_name}-${#service_without_color}))
        if [ $length_difference -ne ${#source_color} ] ; then
            continue # skip cases where zero or more than one instances of the color appear in the service name
        fi
        # overwrite the service name
        local rewritten_service_name=${existing_service_name//$source_color/$DESTINATION_COLOR}
        if ! "$YQ_BINARY" --inplace "(select(documentIndex == 0) | .spec.routes[] | select(.match == \"$route_match\") | .services[0].name) = \"$rewritten_service_name\"" "$ingress_yaml_filepath" ; then
            echo "Error : failed to update service mapping for route match '$route_match' in $ingress_yaml_filepath" >&2
            exit 1
        fi
    done
    unset route_match_array
}

#######################################
# modify an ingress yaml file to set a specified host's service to a specifie service
# Gobals:
#   YQ_BINARY 
# Arguments:
#   yaml filepath to file which should be updated
#   name of host which will be updated with a new service
#   name of service to which the updated host should route traffic
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   file at the designated path is modified on the filesystem
#   process exit if error occurs
#######################################
function rewrite_ingress_file_for_new_host_service() {
    local ingress_yaml_filepath=$1
    local host=$2
    local service=$3
    if ! "$YQ_BINARY" --inplace "(.spec.rules[] | select(.host == \"$host\") | .http.paths[].backend.service.name) = \"$service\"" "$ingress_yaml_filepath" ; then
        echo "Error : failed to update service mapping for host '$host' in $ingress_yaml_filepath" >&2
        exit 1
    fi
    local rule_count
    rule_count=$("$YQ_BINARY" "[.spec.rules[] | select(.host == \"$host\")] | length" "$ingress_yaml_filepath")
    if [ "$rule_count" -eq 0 ]; then
        echo "Error : expected to find host '$host' in $ingress_yaml_filepath" >&2
        exit 1
    fi
    local name_count
    name_count=$("$YQ_BINARY" "[.spec.rules[] | select(.host == \"$host\") | .http.paths[].backend.service.name] | length" "$ingress_yaml_filepath")
    if [ "$name_count" -eq 0 ]; then
        echo "Error : expected at least one service entry for host '$host' in $ingress_yaml_filepath" >&2
        exit 1
    fi
    local mismatch_count
    mismatch_count=$("$YQ_BINARY" "[.spec.rules[] | select(.host == \"$host\") | .http.paths[].backend.service.name | select(. != \"$service\")] | length" "$ingress_yaml_filepath")
    if [ "$mismatch_count" -gt 0 ]; then
        echo "Error : service name update for host '$host' did not complete as expected in $ingress_yaml_filepath" >&2
        exit 1
    fi
}

#######################################
# modify all related ingress resources to route traffic to the services of the specified destination color
# Gobals:
#   YQ_BINARY 
#   HOST_TO_SERVICE_MAP_BLUE
#   HOST_TO_SERVICE_MAP_GREEN
#   KS_K8S_DEPL_REPO_DIRPATH
#   HOST_TO_INGRESS_YAML_FILEPATH_MAP
#   HOST_TO_INGRESS_TYPE_MAP
# Arguments:
#   destination color ('blue' or 'green')
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   all related ingress resource yaml files are udpated appropriately to route traffic to the specified destination color
#   the kubernetes cluster is reconfigured using "kubectl apply -f <file>" for all related ingress resource yaml files
#######################################
function switchover_ingress_rules_to_destination_database_deployment() {
    local DESTINATION_COLOR=$1
    local host_map_name="HOST_TO_SERVICE_MAP_BLUE"

    if [ "$DESTINATION_COLOR" == "green" ] ; then
        host_map_name="HOST_TO_SERVICE_MAP_GREEN"
    elif [ "$DESTINATION_COLOR" != "blue" ] ; then
        echo "Warning : switchover_ingress_rules_to_destination_database_deployment called with unrecognized color argument : $DESTINATION_COLOR. 'blue' will be used instead."
        host_map_name="HOST_TO_SERVICE_MAP_BLUE"
    fi

    local host_map_keys=()
    eval "host_map_keys=(\"\${!${host_map_name}[@]}\")"
    if [ "${#host_map_keys[@]}" -eq 0 ] ; then
        echo "Error : host-to-service map for color '$DESTINATION_COLOR' is empty in configuration" >&2
        exit 1
    fi

    local ingress_yaml_filepath_relative
    local ingress_yaml_filepath
    local ingress_type
    local host
    local escaped_host
    local service
    for host in "${host_map_keys[@]}"; do
        ingress_yaml_filepath_relative="${HOST_TO_INGRESS_YAML_FILEPATH_MAP[$host]}"
        ingress_yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/$ingress_yaml_filepath_relative"
        ingress_type="${HOST_TO_INGRESS_TYPE_MAP[$host]}"
        if [ "$ingress_type" == "ingressroute" ] ; then
            rewrite_ingressroute_file_for_new_color "$ingress_yaml_filepath" "$DESTINATION_COLOR"
        else
            escaped_host=$(printf '%q' "$host")
            eval "service=\${${host_map_name}[$escaped_host]}"
            rewrite_ingress_file_for_new_host_service "$ingress_yaml_filepath" "$host" "$service"
        fi
    done

    echo "switching traffic over to the updated database deployment"
    unset yaml_files_already_applied
    declare -A yaml_files_already_applied
    for ingress_yaml_filepath_relative in ${HOST_TO_INGRESS_YAML_FILEPATH_MAP[*]} ; do
        if [ -z ${yaml_files_already_applied[$ingress_yaml_filepath_relative]} ] ; then
            ingress_yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/$ingress_yaml_filepath_relative"
            if ! kubectl --kubeconfig $CLUSTER_KUBECONFIG apply -f "$ingress_yaml_filepath" ; then
                echo "Warning : received non-zero exit status for command kubectl --kubeconfig $CLUSTER_KUBECONFIG apply -f $ingress_yaml_filepath"
            fi
            yaml_files_already_applied[$ingress_yaml_filepath_relative]="done"
        fi
    done
    unset yaml_files_already_applied
}

#######################################
# modify a deployment yaml file to set the count for replicas
# Gobals:
#   YQ_BINARY 
# Arguments:
#   yaml filepath to file which should be updated
#   desired replica count
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   file at the designated path is modified on the filesystem
#######################################
function adjust_replica_count_in_deployment_yaml_file() {
    local yaml_filepath=$1
    local replica_count=$2
    if ! "$YQ_BINARY" --inplace "(select(.kind == \"Deployment\") | .spec.replicas) = $replica_count" "$yaml_filepath" ; then
        echo "Warning : backend deployment has been scaled in the kubernetes cluster, but corresponding changes have not successfully been made to $yaml_filepath." >&2
        echo "          Attempt to update replicas with yq failed. The repository is now out of sync and must be manually corrected." >&2
        return
    fi
    local updated_value
    updated_value=$("$YQ_BINARY" "select(.kind == \"Deployment\") | .spec.replicas" "$yaml_filepath" 2>/dev/null)
    if [ "$updated_value" != "$replica_count" ]; then
        echo "Warning : backend deployment has been scaled in the kubernetes cluster, but corresponding changes have not successfully been made to $yaml_filepath." >&2
        echo "          The repository is now out of sync and must be manually corrected, and the cause of the failure to update must be addressed in code." >&2
        return
    fi
}

#######################################
# modify all related deployment yaml files to have the appropriate count of replicas based on destination color
# Gobals:
#   KS_K8S_DEPL_REPO_DIRPATH
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   DEPLOYMENT_TO_YAML_FILEPATH_MAP
# Arguments:
#   destination color ('blue' or 'green')
# Output:
#   none
# Side effects:
#   all related deployment yaml files are updated on the filesystem
#######################################
function adjust_replica_counts_in_deployment_yaml_files() {
    local DESTINATION_COLOR=$1
    # the destination color will receive "full" replicas and the other will receive "none" replicas
    local blue_replica_count="none"
    local green_replica_count="none"
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        blue_replica_count="full"
    fi
    if [ "$DESTINATION_COLOR" == "green" ] ; then
        green_replica_count="full"
    fi
    local pos
    local deployment
    local yaml_filepath
    local replicas_int
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        deployment="${BLUE_DEPLOYMENT_LIST[$pos]}"
        yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        replica_count_string_to_integer "$deployment" "$blue_replica_count"
        replicas_int=$?
        adjust_replica_count_in_deployment_yaml_file "$yaml_filepath" "$replicas_int"
        pos=$(($pos+1))
    done
    pos=0
    while [ $pos -lt ${#GREEN_DEPLOYMENT_LIST[@]} ] ; do
        deployment="${GREEN_DEPLOYMENT_LIST[$pos]}"
        yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        replica_count_string_to_integer "$deployment" "$green_replica_count"
        replicas_int=$?
        adjust_replica_count_in_deployment_yaml_file "$yaml_filepath" "$replicas_int"
        pos=$(($pos+1))
    done
}

#######################################
# all involved yaml file changes (ingress, deployment, ...) are committed to a git changeset, which is pushed to the remote repo
# Gobals:
#   KS_K8S_DEPL_REPO_DIRPATH
#   BLUE_DEPLOYMENT_LIST
#   GREEN_DEPLOYMENT_LIST
#   DEPLOYMENT_TO_YAML_FILEPATH_MAP
#   HOST_TO_INGRESS_YAML_FILEPATH_MAP
#   GIT_COMMIT_MSG 
# Arguments:
#   destination color ('blue' or 'green')
# Output:
#   none if successful, errors to stderr if encountered
# Side effects:
#   the local git repository clone is updated with all changes.
#   the changes are pushed to github.
#######################################
function check_in_changes_to_kubernetes_into_github() {
    local pos
    local deployment
    local yaml_filepath
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${BLUE_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH add "$yaml_filepath" >/dev/null 2>&1 ; then
            echo "warning : failure when adding file $yaml_filepath to changeset" >&2
        fi
        pos=$(($pos+1))
    done
    pos=0
    while [ $pos -lt ${#GREEN_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${GREEN_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH add "$yaml_filepath" >/dev/null 2>&1 ; then
            echo "warning : failure when adding file $yaml_filepath to changeset" >&2
        fi
        pos=$(($pos+1))
    done
    for ingress_yaml_filepath_relative in ${HOST_TO_INGRESS_YAML_FILEPATH_MAP[*]} ; do
        if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH add "$ingress_yaml_filepath_relative" >/dev/null 2>&1 ; then
            echo "warning : failure when adding file $ingress_yaml_filepath_relative to changeset" >&2
        fi
    done
    local date_string=$(date +%Y-%m-%d)
    local commit_message_string="$GIT_COMMIT_MSG $date_string"
    if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH commit -m "$commit_message_string" >/dev/null 2>&1 ; then
        echo "warning : failure when committing changes to git repository clone" >&2
    fi
    if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH pull --rebase >/dev/null 2>&1 ; then
        echo "warning : failure when preparing to push changes (during git pull --rebase)" >&2
    fi
    if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH push >/dev/null 2>&1 ; then
        echo "warning : failure when pushing changes to git repository" >&2
    fi
    return 0
}

#######################################
# execute a staged switchover of the kubernetes cluster to the destination color and push changes to github repo
# Gobals:
#   SERVICE_ACCOUNT
#   SYNCHRONIZE_USER_TABLES
#   CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION
#   CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION
#   WARM_CACHES
# Arguments:
#   filepath to the datatbase managment tool properties file
#   filepath to the datatbase database color swap properties file
#   destination color ('blue' or 'green')
# Output:
#   progress messages as stages progress. errors to stderr if encountered
# Side effects:
#   the kubernetes cluster is gradually switched to deployments using the destination color database
#   persistence caches are cleared appropriately during switchover
#   warming of the persistence caches may be attempted in the destination color database deployments
#   the destination color database is modified to add any new users who registered while the transfer was being prepared
#   network traffic is switched to flow to the destination color deployments when ready
#   the local git repository clone is updated with all changes.
#   the changes are pushed to github.
# Returns:
#   0 if process runs to completion successfully, otherwise non-zero
# Note:
#   once the ingress traffic switch is going to happen, problems are reported but script completion is attempted despite errors
#######################################
function main() {
    local MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    local COLOR_SWAP_CONFIG_FILEPATH=$2
    local DESTINATION_COLOR=$3

    # phase : initialize environment and validate arguments and current state
    validate_arguments $@
    source /data/portal-cron/scripts/automation-environment.sh
    source /data/portal-cron/scripts/clear-persistence-cache-shell-functions.sh
    source /data/portal-cron/scripts/color-config-parsing-functions.sh
    load_color_swap_config "$COLOR_SWAP_CONFIG_FILEPATH"

    echo "starting transfer-deployment-color.sh"
    local SOURCE_COLOR="blue"
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        SOURCE_COLOR="green"
    fi
    check_current_color_is $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $SOURCE_COLOR
    check_that_git_repo_clone_is_current
    /data/portal-cron/scripts/authenticate_service_account.sh "$SERVICE_ACCOUNT"
    check_that_git_repo_clone_matches_cluster_config

    # phase : scale the deployments to get ready to switch traffic
    echo "starting up initial minimal deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "almost_full"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "almost_none"
    echo "increasing deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "almost_none"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "almost_full"

    # phase : put the destination color deployments into production and mark process state as completed
    if [ "$SYNCHRONIZE_USER_TABLES" == "true" ]; then
        echo "synchronizing user tables"
        /data/portal-cron/scripts/synchronize_user_tables_between_databases.sh "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" $SOURCE_COLOR $DESTINATION_COLOR
    fi
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        "$CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION" skip-slack
    else
        "$CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION" skip-slack
    fi
    # TODO program a smart wait for cache clearing to complete
    sleep 10
    if [ "$WARM_CACHES" == "true" ]; then
        attempt_to_warm_caches_of_incoming_deployments $DESTINATION_COLOR
    fi
    switchover_ingress_rules_to_destination_database_deployment $DESTINATION_COLOR
    /data/portal-cron/scripts/set_update_process_state.sh "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" complete
    if [ "$SOURCE_COLOR" == "blue" ] ; then
        "$CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION" send-slack
    else
        "$CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION" send-slack
    fi

    # phase : scale the destination color deployments fully up an the source fully down. Commit cluster changes to the configuration repo.
    echo "fully scaling deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "none"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "full"
    adjust_replica_counts_in_deployment_yaml_files $DESTINATION_COLOR
    check_in_changes_to_kubernetes_into_github
}

main $@
