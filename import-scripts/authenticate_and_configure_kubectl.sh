#!/usr/bin/env bash

unset cluster_id_to_account_id
declare -A cluster_id_to_account_id
unset cluster_id_to_kubeconfig_filename
declare -A cluster_id_to_kubeconfig_filename
DEFAULT_CONFIG_FILEPATH="/data/portal-cron/pipelines-credentials/authenticate_and_configure_kubectl.conf"
config_filepath="$DEFAULT_CONFIG_FILEPATH"
config_filepath_dir="$(dirname $config_filepath)"
AUTHENTICATE_ACCOUNT_SCRIPT_FILEPATH="/data/portal-cron/scripts/authenticate_service_account.sh"
DEFAULT_KUBECONFIG_FILEPATH="/home/cbioportal_importer/.kube/config"

function usage() {
    echo "usage: authenticate_and_configure_kubectl.sh cluster_id [config_filepath]"
    echo "    valid cluster_id values seem to be:"
    for cluster_id in "${!cluster_id_to_account_id[@]}" ; do
        echo "    - $cluster_id"
    done
    echo "    config_filepath defaults to '$DEFAULT_CONFIG_FILEPATH'"
}

function config_line_should_be_ignored() {
    local config_line="$1"
    local COMMENT_REGEX='^[[:space:]]*#'
    local BLANK_LINE_REGEX='^[[:space:]]*$'
    [[ $config_line =~ $COMMENT_REGEX || $config_line =~ $BLANK_LINE_REGEX ]]
}

# read_config will parse a config file mappings, including to linked kubeconfig files
# which are expected to be in the same directory as the config file. An example config
# file might look like:
#       # format is three string values separated by <TAB>
#       # first value is cluster_id, second is cluster_account_id, third is the cluster's kube/config file
#       # Any line starting with # or empty after discarding leading whitespace is ignored
#       #
#       publicprod	public	public-cluster-kubeconfig
#       publicargocd	public	publicargocd-cluster-kubeconfig
#       eksprod	eks	eks-cluster-kubeconfig
#       eksargocd	eks	eksargocd-cluster-kubeconfig
function read_config() {
    unset line_value
    while IFS="" read -r line ; do
        if config_line_should_be_ignored "$line" ; then
            continue
        fi
        TAB=$(echo -e "\x09")
        IFS="$TAB" read -ra line_value <<< "$line"
        if [ ${#line_value[@]} -lt 3 ] ; then
            echo "error: too few tab characters detected on config line requiring format val1<TAB>val2<TAB> : $line" >&2
            exit 2
        fi
        if [ ${#line_value[@]} -gt 3 ] ; then
            echo "error: too few tab characters detected on config line requiring format val1<TAB>val2<TAB> : $line" >&2
            exit 2
        fi
        STRIP_WHITESPACE_REGEX='^[[:space:]]*(.*[[:print:]])[[:space:]]*$'
        if ! [[ ${line_value[0]} =~ $STRIP_WHITESPACE_REGEX ]] ; then
            echo "error: no value given before first <TAB> in config line : $line" >&2
            exit 2
        fi
        local cluster_id="${BASH_REMATCH[1]}"
        if ! [[ ${line_value[1]} =~ $STRIP_WHITESPACE_REGEX ]] ; then
            echo "error: no value given before second <TAB> in config line : $line" >&2
            exit 2
        fi
        local cluster_account_id="${BASH_REMATCH[1]}"
        if ! [[ ${line_value[2]} =~ $STRIP_WHITESPACE_REGEX ]] ; then
            echo "error: no value given after second <TAB> in config line : $line" >&2
            exit 2
        fi
        local kubeconfig_filename="${BASH_REMATCH[1]}"
        cluster_id_to_account_id[$cluster_id]="$cluster_account_id"
        cluster_id_to_kubeconfig_filename[$cluster_id]="$kubeconfig_filename"
    done < "$config_filepath"
}

function authenticate_account() {
    local account_id="$1"
    if ! $AUTHENTICATE_ACCOUNT_SCRIPT_FILEPATH $account_id ; then
        echo "failed to authenticate via aws account with id $account_id"
        exit 1
    fi
}

function overwrite_default_kubeconfig_file() {
    local kubeconfig_filename="$1"
    if ! cp -a "${config_filepath_dir}/${kubeconfig_filename}" "$DEFAULT_KUBECONFIG_FILEPATH" ; then
        echo "failed to copy selected kubeconfig file to $DEFAULT_KUBECONFIG_FILEPATH"
        exit 1
    fi
}

function validate_args() {
    local cluster_id="$1"
    echo "validating $cluster_id"
    if [ -z "$cluster_id" ] ; then
        usage
        exit 1
    fi
    if [ -z "${cluster_id_to_kubeconfig_filename[$cluster_id]}" ] ; then
        echo "error: '$cluster_id' is not a recognized cluster id" >&2
        usage
        exit 1
    fi
}

function main() {
    local cluster_id="$1"
    if ! [ -z "$2" ] ; then
        config_filepath="$2"
        config_filepath_dir="$(dirname $config_filepath)"
        echo "using config file '$config_filepath'"
    fi
    read_config
    validate_args "$cluster_id"
    authenticate_account "${cluster_id_to_account_id[$cluster_id]}"
    overwrite_default_kubeconfig_file "${cluster_id_to_kubeconfig_filename[$cluster_id]}"
}

main $@
