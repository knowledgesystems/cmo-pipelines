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

declare -a BLUE_DEPLOYMENT_LIST=()
declare -a GREEN_DEPLOYMENT_LIST=()
declare -A DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP=()
declare -A DEPLOYMENT_TO_CACHE_WARMING_POLICY=()
declare -A DEPLOYMENT_TO_YAML_FILEPATH_MAP=()
declare -A HOST_TO_SERVICE_MAP_BLUE=()
declare -A HOST_TO_SERVICE_MAP_GREEN=()

function read_scalar() {
    local key="$1"
    local value
    value=$("$YQ_BINARY" -r "$key" "$COLOR_SWAP_CONFIG_FILEPATH")
    if [ "$value" == "null" ] || [ -z "$value" ] ; then
        echo "Error : missing required scalar '$key' in $COLOR_SWAP_CONFIG_FILEPATH" >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

function read_array() {
    local dest_var="$1"
    local path="$2"
    local array_path="${path}[]"
    local file="$COLOR_SWAP_CONFIG_FILEPATH"
    local type=$("$YQ_BINARY" -r "$path | type" "$file")
    if [ "$type" != "!!seq" ] ; then
        echo "Error : expected array at '$path' in $file" >&2
        exit 1
    fi
    unset "$dest_var"
    declare -g -a "$dest_var"
    readarray -t "$dest_var" < <("$YQ_BINARY" -r "$array_path" "$file")
}

function read_map() {
    local dest_var="$1"
    local path="$2"
    local file="$COLOR_SWAP_CONFIG_FILEPATH"
    local type=$("$YQ_BINARY" -r "$path | type" "$file")
    if [ "$type" != "!!map" ] ; then
        echo "Error : expected map at '$path' in $file" >&2
        exit 1
    fi
    unset "$dest_var"
    declare -gA "$dest_var"
    local has_entries=0
    while IFS=$'\t' read -r entry_key entry_value ; do
        printf -v "$dest_var[$entry_key]" '%s' "$entry_value"
        has_entries=1
    done < <("$YQ_BINARY" -r "$path | to_entries[] | [.key, .value] | @tsv" "$file")
    if [ "$has_entries" -eq 0 ] ; then
        echo "Error : map at '$path' in $file must contain at least one entry" >&2
        exit 1
    fi
}

function load_color_swap_config() {
    local config_path="$1"
    if ! [ -f "$config_path" ] || ! [ -r "$config_path" ] ; then
        echo "Error : unable to read config file '$config_path'" >&2
        exit 1
    fi

    SERVICE_ACCOUNT=$(read_scalar '.service_account')
    CLUSTER_KUBECONFIG=$(read_scalar '.cluster_cfg')
    TEMP_DIR_PATH=$(read_scalar '.temp_dir_path')
    KS_K8S_DEPL_REPO_DIRPATH=$(read_scalar '.ks_k8s_depl_repo_dirpath')
    INGRESS_YAML_FILEPATH=$(read_scalar '.ingress_yaml_filepath')
    REPLICA_READY_CHECK_PAUSE_SECONDS=$(read_scalar '.replica_ready_check_pause_seconds')
    REPLICA_READY_CHECK_MAX_CHECKCOUNT=$(read_scalar '.replica_ready_check_max_checkcount')
    WARM_CACHES=$(read_scalar '.warm_caches')
    SYNCHRONIZE_USER_TABLES=$(read_scalar '.synchronize_user_tables')
    CLEAR_PERSISTENCE_CACHES_BLUE_FUNCTION=$(read_scalar '.clear_persistence_caches_blue_function')
    CLEAR_PERSISTENCE_CACHES_GREEN_FUNCTION=$(read_scalar '.clear_persistence_caches_green_function')
    GIT_COMMIT_MSG=$(read_scalar '.git_commit_msg')

    read_array BLUE_DEPLOYMENT_LIST '.blue_deployment_list'
    read_array GREEN_DEPLOYMENT_LIST '.green_deployment_list'
    read_map DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP '.deployment_to_full_replica_count_map'
    read_map DEPLOYMENT_TO_YAML_FILEPATH_MAP '.deployment_to_yaml_filepath_map'
    read_map HOST_TO_SERVICE_MAP_BLUE '.host_to_service_map.blue'
    read_map HOST_TO_SERVICE_MAP_GREEN '.host_to_service_map.green'

    if [ "$WARM_CACHES" == "true" ]; then
        CACHE_WARMING_POD_LIST_FILEPATH=$(read_scalar '.cache_warming_pod_list_filepath')
        CACHE_WARMING_POD_SUBLIST_FILEPATH=$(read_scalar '.cache_warming_pod_sublist_filepath')
        CACHE_WARMING_USERNAME=$(read_scalar '.cache_warming_username')
        CACHE_WARMING_PASSWORD=$(read_scalar '.cache_warming_password')
        read_map DEPLOYMENT_TO_CACHE_WARMING_POLICY '.deployment_to_cache_warming_policy'
    fi
}

function usage() {
    echo "usage: transfer-deployment-color.sh db-properties-file color-swap-config-file destination-color"
    echo "       where destination-color is one of {'green', 'blue'}"
    exit 1
}

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

function check_current_color_is() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    SOURCE_COLOR=$2
    GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="/data/portal-cron/scripts/get_database_currently_in_production.sh"
    CURRENT_DATABASE_OUTPUT_FILEPATH="$TEMP_DIR_PATH/get_current_database_output_before_switch.txt"
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

function check_that_git_repo_clone_is_current() {
    if ! git_repo_clone_is_current ; then
        exit 1
    fi
}

function yaml_file_is_current_with_production() {
    yaml_filepath=$1
    full_yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
    kubectl --kubeconfig $CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath" > /dev/null 2>&1
    diff_status=$?
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

function git_repo_clone_matches_cluster_config() {
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${BLUE_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! yaml_file_is_current_with_production "$yaml_filepath" ; then
            echo "current master branch of kubernetes yaml repo does not match the production environment"
            echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
            return 1
        fi
        pos=$(($pos+1))
    done
    pos=0
    while [ $pos -lt ${#GREEN_DEPLOYMENT_LIST[@]} ] ; do
        deployment=${GREEN_DEPLOYMENT_LIST[$pos]}
        yaml_filepath="${DEPLOYMENT_TO_YAML_FILEPATH_MAP[$deployment]}"
        if ! yaml_file_is_current_with_production "$yaml_filepath" ; then
            echo "current master branch of kubernetes yaml repo does not match the production environment"
            echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$yaml_filepath"
            return 1
        fi
        pos=$(($pos+1))
    done
    if ! yaml_file_is_current_with_production "$INGRESS_YAML_FILEPATH" ; then
        echo "current master branch of kubernetes yaml repo does not match the production environment"
        echo "mismatch exists in file $KS_K8S_DEPL_REPO_DIRPATH/$INGRESS_YAML_FILEPATH"
        return 1
    fi
    return 0
}

function check_that_git_repo_clone_matches_cluster_config() {
    if ! git_repo_clone_matches_cluster_config ; then
        exit 1
    fi
}

function all_replicas_ready() {
    DEPLOYMENT_COLOR=$1
    DEPLOYMENT_CHECK_OUTPUT_FILEPATH="$TEMP_DIR_PATH/all_replicas_ready_output.txt"
    if [ $DEPLOYMENT_COLOR == 'blue' ] ; then
        kubectl --kubeconfig $CLUSTER_KUBECONFIG get deployments ${BLUE_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            kubectl --kubeconfig $CLUSTER_KUBECONFIG get deployments ${GREEN_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
        else
            echo "Error : invalid argument '$DEPLOYMENT_COLOR' passed to all_replicas_ready()" >&2
            exit 1
        fi
    fi
    headers_read=0
    while IFS='' read -r line || [ -n "$line" ] ; do # -n "$line" will allow processing of lines which reach EOF before encountering newline
        if [ $headers_read -eq 0 ] ; then
            headers_read=1
        else
            parse_line_regex='^([[:graph:]]*)[[:space:]]*([[:digit:]]*)/([[:digit:]])*.*'
            if ! [[ $line =~ $parse_line_regex ]] ; then
                echo "Error : failure to parse format of get deployment output : $line" >&2
                exit 1
            fi
            deployment_name=${BASH_REMATCH[1]}
            ready_pods=${BASH_REMATCH[2]}
            total_pods=${BASH_REMATCH[3]}
            if [ $ready_pods -ne $total_pods ] ; then
                rm -f $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
                return 1 # not all ready yet
            fi
        fi
    done < $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    rm -f $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    return 0 # all ready
}

# returns count as return value
# 0 -> 0,0,0,0
# 1 -> 0,0,1,1
# 2 -> 0,1,1,2
# 3 -> 0,1,2,3
function replica_count_string_to_integer() {
    deployment_name=$1
    replica_count_string=$2
    full_replica_count=${DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP[$deployment_name]}
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

function scale_deployment_to_N_replicas() {
    DEPLOYMENT_COLOR=$1
    NUM_REPLICAS=$2 # "none", "almost_none", "almost_full, or "full"
    if [ $DEPLOYMENT_COLOR == 'blue' ] ; then
        local pos=0
        while [ "$pos" -lt "${#BLUE_DEPLOYMENT_LIST[@]}" ] ; do
            deployment="${BLUE_DEPLOYMENT_LIST[$pos]}"
            replica_count_string_to_integer "$deployment" "$NUM_REPLICAS"
            replica_count=$?
            kubectl --kubeconfig $CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
            pos=$(($pos+1))
        done
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            local pos=0
            while [ "$pos" -lt "${#GREEN_DEPLOYMENT_LIST[@]}" ] ; do
                deployment="${GREEN_DEPLOYMENT_LIST[$pos]}"
                replica_count_string_to_integer "$deployment" "$NUM_REPLICAS"
                replica_count=$?
                kubectl --kubeconfig $CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
                pos=$(($pos+1))
            done
        else
            echo "Error : invalid argument '$DEPLOYMENT_COLOR' passed to scale_deployment_to_N_replicas()" >&2
            exit 1
        fi
    fi
    # now wait for replicas to become ready
    check_count=0
    while [ $check_count -lt $REPLICA_READY_CHECK_MAX_CHECKCOUNT ] ; do
        sleep $REPLICA_READY_CHECK_PAUSE_SECONDS
        if all_replicas_ready $DEPLOYMENT_COLOR ; then
            return 0
        fi
        check_count=$(($check_count+1))
    done
    echo "Error : scaling for '$DEPLOYMENT_COLOR' deployments failed to reach the ready state after $REPLICA_READY_CHECK_MAX_CHECKCOUNT iterations with a period of $REPLICA_READY_CHECK_PAUSE_SECONDS seconds. Exiting" >&2
    exit 1
}

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

function run_cache_warming_study_query() {
    podname="$1"
    should_wait_for_response="$2"
    if [ "$should_wait_for_response" == "wait_for_response" ] ; then
        kubectl --kubeconfig "$CLUSTER_KUBECONFIG" exec "$podname" -- /bin/bash -c 'session_header=$(curl "http://localhost:8888/j_spring_security_check?j_username='"$CACHE_WARMING_USERNAME"'&j_password='"$CACHE_WARMING_PASSWORD"'" -I 2>/dev/null | grep "Set-Cookie") ; COOKIE_RE="Set-Cookie: ([[:graph:]]+);.*" ; [[ $session_header =~ $COOKIE_RE ]] ; header_value="Cookie: ${BASH_REMATCH[1]}" ; cacheoutfile="/tmp/cachewarmer.txt" ; cache_is_warm="no" ; attempts_remaining=4 ; while true ; do rm -f $cacheoutfile ; curl --fail --max-time 10 --connect-timeout 3 --header "$header_value" "http://localhost:8888/api/studies" > $cacheoutfile 2>/dev/null ; if grep -q mskimpact $cacheoutfile ; then cache_is_warm="yes" ; break ; fi ; if [ $attempts_remaining -eq 0 ] ; then break ; fi ; attempts_remaining=$(($attempts_remaining-1)) ; sleep 60 ; done ; curl --header "$header_value" "https://cbioportal.mskcc.org/j_spring_security_logout/" 2>/dev/null ; if [ $cache_is_warm == "yes" ] ; then echo "cache is warm" ; exit 0 ; fi ; echo "cache is cold" ; exit 1'
    else
        kubectl --kubeconfig "$CLUSTER_KUBECONFIG" exec "$podname" -- /bin/bash -c 'session_header=$(curl "http://localhost:8888/j_spring_security_check?j_username='"$CACHE_WARMING_USERNAME"'&j_password='"$CACHE_WARMING_PASSWORD"'" -I 2>/dev/null | grep "Set-Cookie") ; COOKIE_RE="Set-Cookie: ([[:graph:]]+);.*" ; [[ $session_header =~ $COOKIE_RE ]] ; header_value="Cookie: ${BASH_REMATCH[1]}" ; curl --fail --max-time 3 --connect-timeout 3 --header "$header_value" "http://localhost:8888/api/studies" > /dev/null 2>/dev/null ; sleep 1 ; curl --header "$header_value" "https://cbioportal.mskcc.org/j_spring_security_logout/" 2>/dev/null ; exit 0'
    fi
}

function attempt_to_warm_caches_of_incoming_deployments() {
    DESTINATION_COLOR=$1
    echo "attempting to warm caches"
    rm -f "$CACHE_WARMING_POD_LIST_FILEPATH"
    kubectl --kubeconfig "$CLUSTER_KUBECONFIG" get pods > "$CACHE_WARMING_POD_LIST_FILEPATH"
    # initial stage of warming - make study list query without waiting for reply
    pos=0
    while [ $pos -lt ${#BLUE_DEPLOYMENT_LIST[@]} ] ; do
        # this code assumes that the blue deployment list length is equal to the green length
        local deployment_name=${BLUE_DEPLOYMENT_LIST[$pos]}
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

function switchover_ingress_rules_to_destination_database_deployment() {
    local DESTINATION_COLOR=$1
    local ingress_yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/$INGRESS_YAML_FILEPATH"
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

    local host
    for host in "${host_map_keys[@]}"; do
        local escaped_host
        escaped_host=$(printf '%q' "$host")
        local service
        eval "service=\${${host_map_name}[$escaped_host]}"
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
    done

    echo "switching traffic over to the updated database deployment"
    kubectl --kubeconfig $CLUSTER_KUBECONFIG apply -f "$ingress_yaml_filepath"
}

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
    fi
}

function adjust_replica_counts_in_deployment_yaml_files() {
    local destination_color=$1
    local blue_replica_count="none"
    local green_replica_count="none"
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        blue_replica_count="full"
    fi
    if [ "$DESTINATION_COLOR" == "green" ] ; then
        green_replica_count="full"
    fi
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

function check_in_changes_to_kubernetes_into_github() {
    echo "checking in configuration changes to github"
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
    yaml_filepath="$INGRESS_YAML_FILEPATH"
    if ! $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH add "$yaml_filepath" >/dev/null 2>&1 ; then
        echo "warning : failure when adding file $yaml_filepath to changeset" >&2
    fi
    date_string=$(date +%Y-%m-%d)
    commit_message_string="$GIT_COMMIT_MSG $date_string"
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

function main() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    COLOR_SWAP_CONFIG_FILEPATH=$2
    DESTINATION_COLOR=$3

    # phase : initialize environment and validate arguments and current state
    validate_arguments $@
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        SOURCE_COLOR="green"
    else
        SOURCE_COLOR="blue"
    fi
    source /data/portal-cron/scripts/automation-environment.sh
    source /data/portal-cron/scripts/clear-persistence-cache-shell-functions.sh
    load_color_swap_config "$COLOR_SWAP_CONFIG_FILEPATH"

    echo "starting transfer-deployment-color.sh"
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
