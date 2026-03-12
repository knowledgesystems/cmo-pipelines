#!/usr/bin/env bash

# verify-management-state.sh
#
# compare the current ingress situation in the cluster against the active color reported by the management
# database. exit with error if there are any discrepancies.
#
# the main steps executed will be:
# - capture the production situation (green or blue in use) from the ingress rules in the production cluster
# - capture the current database color according to the management database table
# - exit with error if they differ

declare -a BLUE_SERVICE_LIST=()
declare -a GREEN_SERVICE_LIST=()
declare -A HOST_TO_INGRESS_NAME_MAP=()
declare -A HOST_TO_INGRESS_TYPE_MAP=()

#######################################
# Load needed config properties into global variables
# Gobals:
#   COLOR_SWAP_CONFIG_FILEPATH 
#   YQ_BINARY path to yq executable
#   SERVICE_ACCOUNT the name of the service account selector for use with aws authentication
#   CLUSTER_KUBECONFIG path to a .kube/config style function defining the kubectl cluster to interact with
#   TEMP_DIR_PATH
#   HOST_TO_INGRESS_NAME_MAP for each host connected to the database, the name of the ingress resource it uses
#   HOST_TO_INGRESS_TYPE_MAP the type of each ingress resource mapped (either 'ingress' or 'ingressroute')
#   BLUE_SERVICE_LIST an array holding all the blue deployment/service names connected to a database
#   GREEN_SERVICE_LIST an array holding all the green deployment/service names connected to a database
#   UPDATES_DISABLED when this is not 'false', verify-management-state.sh will exit early with a failure code
# Arguments:
#   none
# Output:
#   none, except for error messages to stderr
# Side effects:
#   a directory at the provided TEMP_DIR_PATH location will be created if not yet existing
#######################################
function load_color_swap_config() {
    if ! [ -f "$COLOR_SWAP_CONFIG_FILEPATH" ] || ! [ -r "$COLOR_SWAP_CONFIG_FILEPATH" ] ; then
        echo "Error : unable to read config file '$COLOR_SWAP_CONFIG_FILEPATH'" >&2
        exit 1
    fi
    if ! command -v "$YQ_BINARY" >/dev/null 2>&1 ; then
        echo "Error : unable to locate yq binary '$YQ_BINARY' required to read config file" >&2
        exit 1
    fi
    SERVICE_ACCOUNT=$(read_scalar_from_yaml "$COLOR_SWAP_CONFIG_FILEPATH" '.service_account')
    CLUSTER_KUBECONFIG=$(read_scalar_from_yaml "$COLOR_SWAP_CONFIG_FILEPATH" '.cluster_cfg')
    TEMP_DIR_PATH=$(read_scalar_from_yaml "$COLOR_SWAP_CONFIG_FILEPATH" '.temp_dir_path')
    read_map_from_yaml HOST_TO_INGRESS_NAME_MAP "$COLOR_SWAP_CONFIG_FILEPATH" '.host_to_ingress_name_map'
    read_map_from_yaml HOST_TO_INGRESS_TYPE_MAP "$COLOR_SWAP_CONFIG_FILEPATH" '.host_to_ingress_type_map'
    read_array_from_yaml BLUE_SERVICE_LIST "$COLOR_SWAP_CONFIG_FILEPATH" '.blue_deployment_list'
    read_array_from_yaml GREEN_SERVICE_LIST "$COLOR_SWAP_CONFIG_FILEPATH" '.green_deployment_list'
    UPDATES_DISABLED=$(read_scalar_from_yaml "$COLOR_SWAP_CONFIG_FILEPATH" '.updates_disabled')
    mkdir -p "$TEMP_DIR_PATH"
}

function usage() {
    echo "usage: verify-management-state.sh db-properties-file color-swap-config-file"
    exit 1
}

#######################################
# Exit with error if main() was called with inappropriate arguments
# Gobals:
#   none
# Arguments:
#   all arguements received by main(), 2 expected:
#     MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH
#     COLOR_SWAP_CONFIG_FILEPATH
# Output:
#   none, except for error messages to stderr
# Side effects:
#   process exit if argument count is wrong or file paths are not readable
#######################################
function validate_arguments() {
    if [ $# -ne "2" ] ; then
        usage
    fi
    if ! [ -f "$1" ] || ! [ -r "$1" ] ; then
        echo "Error : unable to read config file '$1'" >&2
        usage
    fi
    if ! [ -f "$2" ] || ! [ -r "$2" ] ; then
        echo "Error : unable to read file '$2'" >&2
        usage
    fi
}

#######################################
# Output current in-production color based on update management database content
# Gobals:
#   none
# Arguments:
#   MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH path to file with properties for accessing update management database
# Output:
#   'blue' or 'green' depending on database contents
# Side effects:
#   process exit if color cannot be determined
#######################################
function output_production_color_from_management_database() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    local GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="/data/portal-cron/scripts/get_database_currently_in_production.sh"
    local CURRENT_DATABASE_OUTPUT_FILEPATH="$TEMP_DIR_PATH/get_current_database_output.txt"
    rm -f "$CURRENT_DATABASE_OUTPUT_FILEPATH"
    if ! $GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH > "$CURRENT_DATABASE_OUTPUT_FILEPATH"; then
        echo "Error during determination of the production database color from management database" >&2
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
        if [ "$current_production_database_color" == "unset" ] ; then
            echo "Error during determination of the production database color from management database" >&2
            exit 1
        fi
    fi
    rm -f "$CURRENT_DATABASE_OUTPUT_FILEPATH"
    echo "$current_database_color"
}

#######################################
# Exit process if UPDATES_DISABLED is not 'false'
# Gobals:
#   UPDATES_DISABLED a property from the color swap config file
# Arguments:
#   none
# Output:
#   none, except for error messages to stderr
# Side effects:
#   none
#######################################
function exit_if_updates_are_disabled() {
    if ! [ "$UPDATES_DISABLED" == "false" ] ; then
        echo "Updates are currently disabled for this database. When the maintenance period is over, 'updates_disabled' will be set to false in the color swap config file." >&2
        exit 1
    fi
}

#######################################
# Output current in-production color based on kubernetes cluster configuration
# Gobals:
#   SERVICE_ACCOUNT
#   TEMP_DIR_PATH
#   HOST_TO_INGRESS_NAME_MAP
#   BLUE_SERVICE_LIST
#   GREEN_SERVICE_LIST
# Arguments:
#   none
# Output:
#   'blue' or 'green' depending on output of all relevant ingress resources for database-linked portals
# Side effects:
#   process exit if color cannot be determined
#######################################
function output_production_color_from_kubernetes_cluster() {
    # dump all ingress rules into common file
    /data/portal-cron/scripts/authenticate_service_account.sh "$SERVICE_ACCOUNT" >&2
    # all related ingress files are concatenated into a common file, which will later be scanned for blue or green service names
    local ingress_output_filepath="$(mktemp "$TEMP_DIR_PATH/ingress_output_XXXXXXXX.yaml")"
    rm -f "$ingress_output_filepath"
    unset ingress_name_already_output
    declare -A ingress_name_already_output
    for host_name in ${!HOST_TO_INGRESS_NAME_MAP[*]} ; do
        local ingress_name="${HOST_TO_INGRESS_NAME_MAP[$host_name]}"
        local ingress_type="${HOST_TO_INGRESS_TYPE_MAP[$host_name]}"
        if [ -z ${ingress_name_already_output[$ingress_name]} ] ; then
            if !  kubectl --kubeconfig "$CLUSTER_KUBECONFIG" get $ingress_type --output=yaml "$ingress_name" >> "$ingress_output_filepath" ; then
                echo "Warning : received non-zero exit status for command kubectl --kubeconfig $CLUSTER_KUBECONFIG get $ingress_type --output=yaml $ingress_name" >&2
            fi
            ingress_name_already_output[$ingress_name]="done"
        fi
    done
    unset ingress_name_already_output
    # examine services in dump file
    found_blue_services="false"
    found_green_services="false"
    pos=0
    while [ $pos -lt ${#BLUE_SERVICE_LIST[*]} ] ; do
        if grep -q "${BLUE_SERVICE_LIST[$pos]}" "$ingress_output_filepath" ; then
            found_blue_services="true"
            break
        fi
        pos=$(($pos+1))
    done
    pos=0
    while [ $pos -lt ${#GREEN_SERVICE_LIST[*]} ] ; do
        if grep -q "${GREEN_SERVICE_LIST[$pos]}" "$ingress_output_filepath" ; then
            found_green_services="true"
            break
        fi
        pos=$(($pos+1))
    done
    if [ "$found_blue_services" == "true" ] && ! [ "$found_green_services" == "true" ] ; then
        echo "blue"
    fi
    if [ "$found_green_services" == "true" ] && ! [ "$found_blue_services" == "true" ] ; then
        echo "green"
    fi
    if [ "$found_blue_services" == "true" ] && [ "$found_green_services" == "true" ] ; then
        echo "error : a mixture of blue and green services are used in the ingress rules for the portal cluster" >&2
        exit 1
    fi
    if ! [ "$found_green_services" == "true" ] && ! [ "$found_blue_services" == "true" ] ; then
        echo "error : neither blue nor green services are used in the ingress rules for the portal cluster" >&2
        exit 1
    fi
    rm -f "$ingress_output_filepath"
    return 0
}

#######################################
# Compare production color from management database to color of kubernetes config and output report
# Gobals:
#   none
# Arguments:
#   actual_production_color from kubernetes ("blue" or "green")
#   management_database_color ("blue" or "green")
# Output:
#   success if colors match or warning/failure if colors mismatch
# Side effects:
#   none
#######################################
function compare_state_and_report() {
    local actual_production_color="$1"
    local management_database_color="$2"
    if ! [ "$management_database_color" == "$actual_production_color" ] ; then
        echo "Warning : management database state DOES NOT MATCH actual cluster ingress." >&2
        echo "              management database color is : $management_database_color" >&2
        echo "              actual cluster color (by ingress rules) : $actual_production_color" >&2
        exit 1
    fi
    echo "management database state matches actual cluster ingress. color is $actual_production_color"
    return 0
}

#######################################
# Verify that production color (based on update management database) matches production color (based on ingress rules in kubernetes cluster)
# Gobals / Arguments:
#   MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH path to database properties file to access the update management database
#   COLOR_SWAP_CONFIG_FILEPATH path to color swap config file to access kubernetes cluster configuration resources
# Output:
#   success if colors match or warning/failure if colors mismatch. also fail quickly if UPDATES_DISABLED property is not false
# Side effects:
#   none
#######################################
function main() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    COLOR_SWAP_CONFIG_FILEPATH=$2
    validate_arguments $@
    source /data/portal-cron/scripts/automation-environment.sh
    source /data/portal-cron/scripts/color-config-parsing-functions.sh
    load_color_swap_config
    echo "starting verify-management-state.sh"
    exit_if_updates_are_disabled
    actual_production_color=$(output_production_color_from_kubernetes_cluster)
    management_database_color=$(output_production_color_from_management_database $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
    compare_state_and_report "$actual_production_color" "$management_database_color"
    return 0
}

main $@
