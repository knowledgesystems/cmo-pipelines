#!/usr/bin/env bash

# import-public-transfer-deployment-color.sh
#
# switch the (updated) standby public web app deployments to become the new production public web app deployments
# and move the prior prodution deployment into a standby state
#
# the main required argument is the color ('green' or 'blue') which should become the new production deployment
# the main steps executed will be:
# - validate arguments
# - scale down the replicas on the production deployment slightly, scale up the standy deployment. Allow time for pod readiness.
# - scale down the replicas on the production deployment almost fully, scale up the standy deployment. Allow time for pod readiness.
# - syncronize the public user tables from the current production database to the current standby database
# - clear the persistence caches for the current standby database
# - switch the ingress rules to route website traffic to the incoming (standby until now) deployment
# - mark the update process as complete in the management database table (implicitly changes user signup to the new production db)
# - scale down the prior production deployment fully, scale up the new production deployment fully. Allow time for pod readiness.
# - construct and check in to github repo the altered kubernetes configuration files

BETA_DEPLOYMENT_IS_ACTIVE="no"
unset BLUE_DEPLOYMENT_LIST
unset GREEN_DEPLOYMENT_LIST
declare -a BLUE_DEPLOYMENT_LIST
declare -a GREEN_DEPLOYMENT_LIST
BLUE_DEPLOYMENT_LIST+=('cbioportal-backend-public-blue')
BLUE_DEPLOYMENT_LIST+=('cbioportal-backend-master-blue')
if ["$BETA_DEPLOYMENT_IS_ACTIVE" == "yes"] ; then
    BLUE_DEPLOYMENT_LIST+=('cbioportal-backend-public-beta-blue')
fi
GREEN_DEPLOYMENT_LIST+=('cbioportal-backend-public-green')
GREEN_DEPLOYMENT_LIST+=('cbioportal-backend-master-green')
if ["$BETA_DEPLOYMENT_IS_ACTIVE" == "yes"] ; then
    GREEN_DEPLOYMENT_LIST+=('cbioportal-backend-public-beta-green')
fi
declare -A DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP=()
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-public-blue']='4'
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-public-green']='4'
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-public-beta-blue']='1'
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-public-beta-green']='1'
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-master-blue']='1'
DEPLOYMENT_TO_FULL_REPLICA_COUNT_MAP['cbioportal-backend-master-green']='1'
declare -A DEPLOYMENT_TO_YAML_FILEPATH_MAP=()
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-public-blue']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_public_blue.yaml'
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-public-beta-blue']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_public_beta_blue.yaml'
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-master-blue']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_master_blue.yaml'
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-public-green']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_public_green.yaml'
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-public-beta-green']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_public_beta_green.yaml'
DEPLOYMENT_TO_YAML_FILEPATH_MAP['cbioportal-backend-master-green']='argocd/aws/203403084713/clusters/cbioportal-prod/apps/cbioportal/cbioportal_backend_master_green.yaml'
INGRESS_YAML_FILEPATH='argocd/aws/203403084713/clusters/cbioportal-prod/apps/ingress/cbio-ingress.yml'
REPLICA_READY_CHECK_PAUSE_SECONDS=20
REPLICA_READY_CHECK_MAX_CHECKCOUNT=15
tmp="/data/portal-cron/tmp/import-cron-public"
KS_K8S_DEPL_REPO_DIRPATH="/data/portal-cron/git-repos/only_for_use_by_public_import_script/knowledgesystems-k8s-deployment"

function usage() {
    echo "usage: import-public-transfer-deployment-color.sh cluster-management-file destination-color"
    echo "       where destination-color is one of {'green', 'blue'}"
    exit 1
}

function validate_arguments() {
    if [ $# -ne "2" ] ; then
        usage
    fi
    if ! [ -f $1 ] || ! [ -r $1 ] ; then
        echo "Error : unable to read file '$1'" >&2
        usage
    fi
    if ! [ $2 == "blue" ] && ! [ $2 == "green" ] ; then
        echo "Error : destination-color '$2' is unrecognized. It must be 'green' or 'blue'" >&2
        usage
    fi
}

function check_current_color_is() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    SOURCE_COLOR=$2
    GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="/data/portal-cron/scripts/get_database_currently_in_production.sh"
    CURRENT_DATABASE_OUTPUT_FILEPATH="$tmp/get_current_database_output_before_switch.txt"
    rm -f $CURRENT_DATABASE_OUTPUT_FILEPATH
    if ! $GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH > $CURRENT_DATABASE_OUTPUT_FILEPATH; then
        echo "Error during determination of the destination database color" >&2
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
        if [ "$destination_database_color" == "unset" ] ; then
            echo "Error during determination of the destination database color" >&2
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
    kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath" > /dev/null 2>&1
    diff_status=$?
    if [ $diff_status -eq 1 ] ; then
        # mismatch
        echo "when checking for currency of yaml specificiations in file '$full_yaml_filepath', these differences were found:"
        kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath"
        return 1
    fi
    if [ $diff_status -gt 1 ] ; then
        # error
        echo "an error occurred when checking the currency of yaml specificiations in file '$full_yaml_filepath'. output:"
        kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG diff -f "$full_yaml_filepath"
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
    DEPLOYMENT_CHECK_OUTPUT_FILEPATH="$tmp/all_replicas_ready_output.txt"
    if [ $DEPLOYMENT_COLOR == 'blue' ] ; then
        kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG get deployments ${BLUE_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG get deployments ${GREEN_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
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
            kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
            pos=$(($pos+1))
        done
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            local pos=0
            while [ "$pos" -lt "${#GREEN_DEPLOYMENT_LIST[@]}" ] ; do
                deployment="${GREEN_DEPLOYMENT_LIST[$pos]}"
                replica_count_string_to_integer "$deployment" "$NUM_REPLICAS"
                replica_count=$?
                kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG scale deployment --replicas $replica_count "$deployment"
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

function yaml_line_is_comment() {
    line=$1
    COMMENT_REGEX='^[[:space:]]*#'
    if [[ "$line" =~ $COMMENT_REGEX ]] ; then
        return 0
    fi
    return 1
}

function yaml_line_is_top_level_section() {
    line=$1
    SECTION_REGEX='^[^[:space:]#].*:'
    if [[ "$line" =~ $SECTION_REGEX ]] ; then
        return 0
    fi
    return 1
}

function yaml_line_is_host_line() {
    line=$1
    HOST_REGEX='^[^[:alpha:]#]*host:'
    if [[ "$line" =~ $HOST_REGEX ]] ; then
        return 0
    fi
    return 1
}

function yaml_host_line_references_host() {
    line=$1
    host_name=$2
    prefix="${line%%:*}:"
    prefix_length=${#prefix}
    suffix=${line:$prefix_length}
    for word in $suffix ; do
        if [ "$word" == "$host_name" ] ; then
            return 0
        fi
    return 1
    done
}

function yaml_line_is_service_line() {
    line=$1
    SERVICE_REGEX='^[^[:alpha:]#]*service:'
    if [[ "$line" =~ $SERVICE_REGEX ]] ; then
        return 0
    fi
    return 1
}

function yaml_line_is_name_line() {
    line=$1
    NAME_REGEX='^[^[:alpha:]#]*name:'
    if [[ "$line" =~ $NAME_REGEX ]] ; then
        return 0
    fi
    return 1
}

function yaml_line_is_replicas_line() {
    line=$1
    REPLICAS_REGEX='^[^[:alpha:]#]*replicas:'
    if [[ "$line" =~ $REPLICAS_REGEX ]] ; then
        return 0
    fi
    return 1
}

function output_replaced_name_line() {
    line="$1"
    service_name="$2"
    echo "${line%%:*}: ${service_name}"
}

function output_replaced_replicas_line() {
    line="$1"
    replica_count="$2"
    echo "${line%%:*}: ${replica_count}"
}

function expected_file_change_count_verified() {
    local original_yaml_filepath="$1"
    local updated_yaml_filepath="$2"
    local expected_changed_line_count="$3"
    local original_yaml_linecount=0
    local updated_yaml_linecount=0
    local observed_changed_line_count=0
    local original_yaml_read=0
    local updated_yaml_read=0
    local original_yaml_at_eof=0
    local updated_yaml_at_eof=0
    local original_line=""
    local updated_line=""
    local read_and_unread_empty_comparison_count=0
    while [ $original_yaml_at_eof -eq 0 ] || [ $updated_yaml_at_eof -eq 0 ] ; do
        original_yaml_read=0
        updated_yaml_read=0
        original_line=""
        updated_line=""
        if [ $original_yaml_at_eof -eq 0 ] ; then
            original_yaml_read=1
            if ! IFS='' read -r -u $original_yaml_fd original_line ; then
                original_yaml_at_eof=1
                if [ -n "$original_line" ] ; then
                    original_yaml_linecount=$(($original_yaml_linecount+1))
                fi
            else
                original_yaml_linecount=$(($original_yaml_linecount+1))
            fi
        fi
        if [ $updated_yaml_at_eof -eq 0 ] ; then
            updated_yaml_read=1
            if ! IFS='' read -r -u $updated_yaml_fd updated_line ; then
                updated_yaml_at_eof=1
                if [ -n "$updated_line" ] ; then
                    updated_yaml_linecount=$(($updated_yaml_linecount+1))
                fi
            else
                updated_yaml_linecount=$(($updated_yaml_linecount+1))
            fi
        fi
        if ! [ "$original_line" == "$updated_line" ] ; then
            observed_changed_line_count=$(($observed_changed_line_count+1))
        else
            # the single final comparison between an unread (and empty) line from a file which ended without a terminal newline
            # and the read (and empty) line from a file which ended with a terminal newline is not counted as a difference
            if [ "$original_yaml_read" -ne "$updated_yaml_read" ] ; then
                # they must both be empty string in order to have matched
                read_and_unread_empty_comparison_count=$(($read_and_unread_empty_comparison_count+1))
                # ignore the first such case
                if [ "$read_and_unread_empty_comparison_count" -gt 1 ] ; then
                    # apparently one or more blank lines were added to the end of one of the files
                    observed_changed_line_count=$(($observed_changed_line_count+1))
                fi
            fi
        fi
    done {original_yaml_fd}<"$original_yaml_filepath" {updated_yaml_fd}<"$updated_yaml_filepath"
    echo "when comparing files, $observed_changed_line_count different lines were observed"
    if [ "$observed_changed_line_count" -ne "$expected_changed_line_count" ] ; then
        echo "Error : expected to make $expected_changed_line_count line changes in $yaml_filepath, but $observed_changed_line_count changes were observed" >&2
        return 1
    fi
    return 0
}

function output_yaml_line_indent_length() {
    line="$1"
    line_prefix=${line%%[^ ]*}
    echo ${#line_prefix}
}

function indent_change_has_exited_block() {
    line="$1"
    block_indent="$2"
    line_indent=$(output_yaml_line_indent_length "$line")
    if [ "$line_indent" -le "$block_indent" ] ; then
        return 0 # exited from the block
    else
        return 1 # still within block
    fi
}

function switchover_ingress_rules_to_destination_database_deployment() {
    DESTINATION_COLOR=$1
    # rewrite yaml files
    public_cbioportal_org_service_name="cbioportal-backend-public-blue"
    public_beta_cbioportal_org_service_name="cbioportal-backend-public-beta-blue"
    master_cbioportal_org_service_name="cbioportal-backend-master-blue"
    if [ "$DESTINATION_COLOR" == "green" ] ; then
        public_cbioportal_org_service_name="cbioportal-backend-public-green"
        public_beta_cbioportal_org_service_name="cbioportal-backend-public-beta-green"
        master_cbioportal_org_service_name="cbioportal-backend-master-green"
    else
        if ! [ "$DESTINATION_COLOR" == "blue" ] ; then
            echo "Warning : switchover_ingress_rules_to_destination_database_deployment called with unrecognized color argument : $DESTINATION_COLOR. 'blue' will be used instead."
        fi
    fi
    yaml_filepath="$KS_K8S_DEPL_REPO_DIRPATH/${INGRESS_YAML_FILEPATH}"
    updated_yaml_filepath="$yaml_filepath.updated"
    rm -f "$updated_yaml_filepath"
    inside_spec="no"
    inside_host_public_cbioportal_org="no"
    inside_host_public_beta_cbioportal_org="no"
    inside_host_master_cbioportal_org="no"
    inside_service="no"
    service_indent=0
    while IFS='' read -r line || [ -n "$line" ] ; do # -n "$line" will allow processing of lines which reach EOF before encountering newline
        if yaml_line_is_comment "$line" ; then
            echo "$line"
            continue
        fi
        if yaml_line_is_top_level_section "$line" ; then
            inside_spec="no"
            inside_host_public_cbioportal_org="no"
            inside_host_public_beta_cbioportal_org="no"
            inside_host_master_cbioportal_org="no"
            inside_service="no"
            if [ "${line:0:5}" == 'spec:' ] ; then
                inside_spec="yes"
            fi
            echo "$line"
            continue
        fi
        if yaml_line_is_host_line "$line" ; then
            inside_host_public_cbioportal_org="no"
            inside_host_public_beta_cbioportal_org="no"
            inside_host_master_cbioportal_org="no"
            inside_service="no"
            if yaml_host_line_references_host "$line" "www.cbioportal.org" ; then
                inside_host_public_cbioportal_org="yes"
            else
                if yaml_host_line_references_host "$line" "beta.cbioportal.org" ; then
                    inside_host_public_beta_cbioportal_org="yes"
                else
                    if yaml_host_line_references_host "$line" "master.cbioportal.org" ; then
                        inside_host_master_cbioportal_org="yes"
                    fi
                fi
            fi
            echo "$line"
            continue
        fi
        if [ "$inside_service" == "yes" ] && indent_change_has_exited_block "$line" $service_indent ; then
            inside_service="no"
        fi
        if [ "$inside_spec" == "yes" ] ; then
            if [ "$inside_host_public_cbioportal_org" == "yes" ] || [ "$inside_host_public_beta_cbioportal_org" == "yes" ] || [ "$inside_host_master_cbioportal_org" == "yes" ] ; then
                if yaml_line_is_service_line "$line" ; then
                    inside_service="yes"
                    service_indent=$(output_yaml_line_indent_length "$line")
                    echo "$line"
                    continue
                fi
                if [ "$inside_service" == "yes" ] && yaml_line_is_name_line "$line" ; then
                    if [ "$inside_host_public_cbioportal_org" == "yes" ] ; then
                        output_replaced_name_line "$line" "$public_cbioportal_org_service_name"
                        continue
                    fi
                    if [ "$inside_host_public_beta_cbioportal_org" == "yes" ] ; then
                        if ["$BETA_DEPLOYMENT_IS_ACTIVE" == "yes"] ; then
                            output_replaced_name_line "$line" "$public_beta_cbioportal_org_service_name"
                            continue
                        fi
                    fi
                    if [ "$inside_host_master_cbioportal_org" == "yes" ] ; then
                        output_replaced_name_line "$line" "$master_cbioportal_org_service_name"
                        continue
                    fi
                fi
            fi
            echo "$line"
            continue
        fi
        echo "$line"
    done < "$yaml_filepath" > "$updated_yaml_filepath"
    expected_changed_line_count=${#BLUE_DEPLOYMENT_LIST[@]}
    if ! expected_file_change_count_verified "$yaml_filepath" "$updated_yaml_filepath" "$expected_changed_line_count" ; then
        exit 1
    fi
    echo "switching traffic over to the updated database deployment"
    mv "$updated_yaml_filepath" "$yaml_filepath"
    kubectl --kubeconfig $PUBLICARGOCD_CLUSTER_KUBECONFIG apply -f "$yaml_filepath"
}

function adjust_replica_count_in_deployment_yaml_file() {
    local yaml_filepath=$1
    local replica_count=$2
    updated_yaml_filepath="$yaml_filepath.updated"
    rm -f "$updated_yaml_filepath"
    inside_spec="no"
    while IFS='' read -r line || [ -n "$line" ] ; do # -n "$line" will allow processing of lines which reach EOF before encountering newline
        if yaml_line_is_comment "$line" ; then
            echo "$line"
            continue
        fi
        if yaml_line_is_top_level_section "$line" ; then
            if [ "${line:0:5}" == 'spec:' ] ; then
                inside_spec="yes"
            else
                inside_spec="no"
            fi
            echo "$line"
            continue
        fi
        if [ "$inside_spec" == "yes" ] ; then
            if yaml_line_is_replicas_line "$line" ; then
                output_replaced_replicas_line "$line" "$replica_count"
                continue
            fi
        fi
        echo "$line"
    done < "$yaml_filepath" > "$updated_yaml_filepath"
    expected_changed_line_count=1
    if ! expected_file_change_count_verified "$yaml_filepath" "$updated_yaml_filepath" "$expected_changed_line_count" ; then
        echo "Warning : backend deployment has been scaled in the kubernetes cluster, but corresponding changes have not successfully been made to $yaml_filepath."
        echo "          The repository is now out of sync and must be manually corrected, and the cause of the failure to update must be addressed in code."
        rm "$updated_yaml_filepath"
    else
        mv "$updated_yaml_filepath" "$yaml_filepath"
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
    commit_message_string="public import $date_string"
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
    DESTINATION_COLOR=$2

    # phase : initialize environment and validate arguments and current state
    validate_arguments $@
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        SOURCE_COLOR="green"
    else
        SOURCE_COLOR="blue"
    fi
    source /data/portal-cron/scripts/automation-environment.sh
    source /data/portal-cron/scripts/clear-persistence-cache-shell-functions.sh
    echo "starting import-public-transfer-deployment-color.sh"
    check_current_color_is $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $SOURCE_COLOR
    check_that_git_repo_clone_is_current
    /data/portal-cron/scripts/authenticate_service_account.sh public
    check_that_git_repo_clone_matches_cluster_config

    # phase : scale the deployments to get ready to switch traffic
    FULL_MINUS_1=$(($FULL_REPLICA_COUNT-1))
    echo "starting up initial minimal deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "almost_full"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "almost_none"
    echo "increasing deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "almost_none"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "almost_full"

    # phase : put the destination color deployments into production and mark process state as completed
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        clearPersistenceCachesForPublicBluePortals
    else 
        clearPersistenceCachesForPublicGreenPortals
    fi
    # TODO program a smart wait for cache clearing to complete
    sleep 3
    switchover_ingress_rules_to_destination_database_deployment $DESTINATION_COLOR
    /data/portal-cron/scripts/set_update_process_state.sh "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" complete
    if [ "$SOURCE_COLOR" == "blue" ] ; then
        clearPersistenceCachesForPublicBluePortals
    else 
        clearPersistenceCachesForPublicGreenPortals
    fi

    # phase : scale the destination color deployments fully up an the source fully down. Commit cluster changes to the configuration repo.
    echo "fully scaling deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR "none"
    scale_deployment_to_N_replicas $DESTINATION_COLOR "full"
    adjust_replica_counts_in_deployment_yaml_files $DESTINATION_COLOR
    check_in_changes_to_kubernetes_into_github
}

main $@
