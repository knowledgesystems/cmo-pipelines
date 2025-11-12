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

function load_color_swap_config() {
    if ! [ -f "$COLOR_SWAP_CONFIG_FILEPATH" ] || ! [ -r "$COLOR_SWAP_CONFIG_FILEPATH" ] ; then
        echo "Error : unable to read config file '$COLOR_SWAP_CONFIG_FILEPATH'" >&2
        exit 1
    fi
    if ! command -v "$YQ_BINARY" >/dev/null 2>&1 ; then
        echo "Error : unable to locate yq binary '$YQ_BINARY' required to read config file" >&2
        exit 1
    fi
    SERVICE_ACCOUNT=$(read_scalar '.service_account')
    CLUSTER_KUBECONFIG=$(read_scalar '.cluster_cfg')
    TEMP_DIR_PATH=$(read_scalar '.temp_dir_path')
    INGRESS_NAME=$(read_scalar '.ingress_name')
    read_array BLUE_SERVICE_LIST '.blue_deployment_list'
    read_array GREEN_SERVICE_LIST '.green_deployment_list'
    mkdir -p "$TEMP_DIR_PATH"
}

function usage() {
    echo "usage: verify-management-state.sh db-properties-file color-swap-config-file"
    exit 1
}

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

function output_production_color_from_management_database() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    GET_DATABASE_CURRENTLY_IN_PRODUCTION_SCRIPT="/data/portal-cron/scripts/get_database_currently_in_production.sh"
    CURRENT_DATABASE_OUTPUT_FILEPATH="$TEMP_DIR_PATH/get_current_database_output.txt"
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

function output_production_color_from_kubernetes_cluster() {
    /data/portal-cron/scripts/authenticate_service_account.sh "$SERVICE_ACCOUNT" >&2
    ingress_output_filepath="$(mktemp "$TEMP_DIR_PATH/ingress_output_XXXXXXXX.yaml")"
    kubectl --kubeconfig "$CLUSTER_KUBECONFIG" get ingress --output=yaml "$INGRESS_NAME" > "$ingress_output_filepath"
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
    rm "$ingress_output_filepath"
    return 0
}

function compare_state_and_report() {
    actual_production_color="$1"
    management_database_color="$2"
    if ! [ "$management_database_color" == "$actual_production_color" ] ; then
        echo "Warning : management database state DOES NOT MATCH actual cluster ingress." >&2
        echo "              management database color is : $management_database_color" >&2
        echo "              actual cluster color (by ingress rules) : $actual_production_color" >&2
        exit 1
    fi
    echo "management database state matches actual cluster ingress. color is $actual_production_color"
    return 0
}

function main() {
    MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH=$1
    COLOR_SWAP_CONFIG_FILEPATH=$2
    validate_arguments $@
    source /data/portal-cron/scripts/automation-environment.sh
    load_color_swap_config
    echo "starting verify-management-state.sh"
    actual_production_color=$(output_production_color_from_kubernetes_cluster)
    management_database_color=$(output_production_color_from_management_database $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
    compare_state_and_report "$actual_production_color" "$management_database_color"
    return 0
}

main $@
