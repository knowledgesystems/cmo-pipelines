#!/usr/bin/env bash

FULL_REPLICA_COUNT=3
unset GENIE_BLUE_DEPLOYMENT_LIST
unset GENIE_GREEN_DEPLOYMENT_LIST
declare -a GENIE_BLUE_DEPLOYMENT_LIST
declare -a GENIE_GREEN_DEPLOYMENT_LIST
GENIE_BLUE_DEPLOYMENT_LIST+=('cbioportal-backend-genie-public-blue')
GENIE_BLUE_DEPLOYMENT_LIST+=('cbioportal-backend-genie-private-blue')
GENIE_GREEN_DEPLOYMENT_LIST+=('cbioportal-backend-genie-public-green')
GENIE_GREEN_DEPLOYMENT_LIST+=('cbioportal-backend-genie-private-green')
REPLICA_READY_CHECK_PAUSE_SECONDS=20
REPLICA_READY_CHECK_MAX_CHECKCOUNT=8
tmp="/data/portal-cron/tmp/import-cron-genie"
KS_K8S_DEPL_REPO_DIRPATH="/data/portal-cron/git-repos/only_for_use_by_genie_import_script/knowledgesystems-k8s-deployment"

function usage() {
    echo "usage: import-genie-data-transfer-deployment-color.sh cluster-management-file destination-color"
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

function check_that_cluster_config_git_repo_is_current() {
    $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH git pull
    $GIT_BINARY -C $KS_K8S_DEPL_REPO_DIRPATH git status
    #TODO: ensure that clone status shows up to date with master branch
    #TODO: check that deployment configurations match those in yaml files
    #- kubectl diff -f public-eks/cbioportal-prod/cbioportal_backend_genie_private_blue.yaml
    #- kubectl diff -f public-eks/cbioportal-prod/cbioportal_backend_genie_private_green.yaml
    #- kubectl diff -f public-eks/cbioportal-prod/cbioportal_backend_genie_public_green.yaml
    #- kubectl diff -f public-eks/cbioportal-prod/cbioportal_backend_genie_public_blue.yaml
    #TODO: check that ingress rules in yaml match those in prodution (not edited but failure to commit/push)
    # - kubectl diff -f public-eks/cbioportal-prod/shared-services/ingress/cbio-ingress.yml
    # report errors on discrepancies and exit with failure
}

function all_replicas_ready() {
    DEPLOYMENT_COLOR=$1
    DEPLOYMENT_CHECK_OUTPUT_FILEPATH="$tmp/all_replicas_ready_output.txt"
    if [ $DEPLOYMENT_COLOR == 'blue' ] ; then
        kubectl get deployments ${GENIE_BLUE_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            kubectl get deployments ${GENIE_GREEN_DEPLOYMENT_LIST[@]} > $DEPLOYMENT_CHECK_OUTPUT_FILEPATH
        else
            echo "Error : invalid argument '$DEPLOYMENT_COLOR' passed to all_replicas_ready()" >&2
            exit 1
        fi
    fi
    headers_read=0
    while IFS= read -r line; do
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

function scale_deployment_to_N_replicas() {
    DEPLOYMENT_COLOR=$1
    NUM_REPLICAS=$2
    full_minus_1=$(($FULL_REPLICA_COUNT-1))
    if [ $DEPLOYMENT_COLOR == 'blue' ] ; then
        kubectl --kubeconfig $PUBLIC_CLUSTER_KUBECONFIG scale deployment --replicas $NUM_REPLICAS ${GENIE_BLUE_DEPLOYMENT_LIST[@]}
    else
        if [ $DEPLOYMENT_COLOR == 'green' ] ; then
            kubectl --kubeconfig $PUBLIC_CLUSTER_KUBECONFIG scale deployment --replicas $NUM_REPLICAS ${GENIE_GREEN_DEPLOYMENT_LIST[@]}
        else
            echo "Error : invalid argument '$DEPLOYMENT_COLOR' passed to scale_deployment_to_full_minus_1_replicas()" >&2
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
    echo "Error : scaling for '$DEPLOYMENT_COLOR' deployments failed to reach the ready state after $REPLICA_READY_CHECK_MAX_CHECKCOUNT iterations with a period of $REPLICA_READY_CHECK_PAUSE_SECONDS seconds. Exiting"
    exit 1
}

function switchover_ingress_rules_to_updated_database_deployment() {
    # recently we have checked that the k8s config repo is up to date and matches the production specs
    # TODO switch over ingress rules to the new deployment
    # - alter selected services in ingress rules for genie.cbioportal.org and genie-private.cbioportal.org in cbio-ingress.yaml
    echo "switching traffic over to the updated database deployment"
}

function check_in_changes_to_kubernetes_into_github() {
    #TODO: implement commit and push .. report problems as error
    echo "checking in configuration changes to github"
}

function main() {
    validate_arguments $@
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    DESTINATION_COLOR=$2
    if [ "$DESTINATION_COLOR" == "blue" ] ; then
        SOURCE_COLOR="green"
    else
        SOURCE_COLOR="blue"
    fi
    source /data/portal-cron/scripts/automation-environment.sh
    source /data/portal-cron/scripts/clear-persistence-cache-shell-functions.sh
    echo "starting import-genie-data-transfer-deployment-color.sh"
    check_current_color_is $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $SOURCE_COLOR
    /data/portal-cron/scripts/authenticate_service_account.sh public
    declare -x PUBLIC_CLUSTER_KUBECONFIG="/data/portal-cron/pipelines-credentials/public-cluster-config"
    check_that_cluster_config_git_repo_is_current
    FULL_MINUS_1=$((FULL_REPLICA_COUNT-1))
    echo "starting up initial minimal deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR $FULL_MINUS_1
    scale_deployment_to_N_replicas $DESTINATION_COLOR 1
    echo "increasing deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR 1
    scale_deployment_to_N_replicas $DESTINATION_COLOR $FULL_MINUS_1
    echo "synchronizing user tables"
    /data/portal-cron/scripts/synchronize_user_tables_between_databases.sh /data/portal-cron/pipelines-credentials/manage_genie_database_update_tools.properties $SOURCE_COLOR $DESTINATION_COLOR
    # TODO program a smart wait for cache clearing to complete
    clearPersistenceCachesForGeniePortals
    switchover_ingress_rules_to_updated_database_deployment $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH $DESTINATION_COLOR
    echo "fully scaling deployment of $DESTINATION_COLOR"
    scale_deployment_to_N_replicas $SOURCE_COLOR 0
    scale_deployment_to_N_replicas $DESTINATION_COLOR $FULL_REPLICA_COUNT
    /data/portal-cron/scripts/set_update_process_state.sh /data/portal-cron/pipelines-credentials/manage_genie_database_update_tools.properties complete
    check_in_changes_to_kubernetes_into_github
}

main $@
