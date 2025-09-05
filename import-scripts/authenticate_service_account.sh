#!/usr/bin/env bash

unset cluster_account_id_to_credentials_filename
declare -A cluster_account_id_to_credentials_filename
DEFAULT_CONFIG_FILEPATH="/data/portal-cron/pipelines-credentials/authenticate_service_account.conf"
config_filepath="$DEFAULT_CONFIG_FILEPATH"
config_filepath_dir="$(dirname $config_filepath)"

function usage() {
    echo "usage: authenticate_service_account.sh cluster_account_id [config_filepath]"
    echo "    valid cluster_account_id values seem to be:"
    for cluster_account_id in "${!cluster_account_id_to_credentials_filename[@]}" ; do
        local account_credentials_filepath="$(output_credentials_filepath_for_account_id $cluster_account_id)"
        local account_number="$(output_property_from_credentials_filepath $account_credentials_filepath eks.account.number)"
        echo "    - $cluster_account_id (account $account_number)"
    done
    echo "    config_filepath defaults to '$DEFAULT_CONFIG_FILEPATH'"
}

function load_automation_environment() {
    local PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh
    if ! [ -f "$PATH_TO_AUTOMATION_SCRIPT" ] ; then
        echo "error: $PATH_TO_AUTOMATION_SCRIPT could not be found. exiting..." >&2
        exit 2
    fi
    if ! [ -r "$PATH_TO_AUTOMATION_SCRIPT" ] ; then
        echo "error: $PATH_TO_AUTOMATION_SCRIPT was found but is not readable. exiting..." >&2
        exit 2
    fi
    . $PATH_TO_AUTOMATION_SCRIPT
}

function config_line_should_be_ignored() {
    local config_line="$1"
    local COMMENT_REGEX='^[[:space:]]*#'
    local BLANK_LINE_REGEX='^[[:space:]]*$'
    [[ $config_line =~ $COMMENT_REGEX || $config_line =~ $BLANK_LINE_REGEX ]]
}

# read_config will parse a config file mapping account ids to credential filenames
# which are expected to be in the same directory as the config file. An example config
# file might look like:
#       # format is two string values separated by a single <TAB>
#       # first value is cluster_account_id, second is credentials_filename
#       # Any line starting with # or empty after discarding leading whitespace is ignored
#       #
#       public	public-account.credentials
#       eks	eks-account.credentials
function read_config() {
    unset line_value
    while IFS="" read -r line ; do
        if config_line_should_be_ignored "$line" ; then
            continue
        fi
        TAB=$(echo -e "\x09")
        IFS="$TAB" read -ra line_value <<< "$line"
        if [ ${#line_value[@]} -eq 1 ] ; then
            echo "error: no tab character detected on config line requiring format val1<TAB>val2 : $line" >&2
            exit 2
        fi
        if [ ${#line_value[@]} -gt 2 ] ; then
            echo "error: multiple tab characters detected on config line requiring format val1<TAB>val2 : $line" >&2
            exit 2
        fi
        STRIP_WHITESPACE_REGEX='^[[:space:]]*(.*[[:print:]])[[:space:]]*$'
        if ! [[ ${line_value[0]} =~ $STRIP_WHITESPACE_REGEX ]] ; then
            echo "error: no value given before <TAB> in config line : $line" >&2
            exit 2
        fi
        local cluster_account_id="${BASH_REMATCH[1]}"
        if ! [[ ${line_value[1]} =~ $STRIP_WHITESPACE_REGEX ]] ; then
            echo "error: no value given after <TAB> in config line : $line" >&2
            exit 2
        fi
        local credentials_filename="${BASH_REMATCH[1]}"
        cluster_account_id_to_credentials_filename[$cluster_account_id]="$credentials_filename"
    done < "$config_filepath"
}

function validate_args() {
    local cluster_account_id="$1"
    echo "validating $cluster_account_id"
    if [ -z "$cluster_account_id" ] ; then
        usage
        exit 1
    fi
    if [ -z "${cluster_account_id_to_credentials_filename[$cluster_account_id]}" ] ; then
        echo "error: '$cluster_account_id' is not a recognized cluster account id" >&2
        usage
        exit 1
    fi
}

function output_string_with_prefix_removed() {
    local string="$1"
    local prefix="$2"
    echo "${string##$prefix}"
}

function output_property_from_credentials_filepath() {
    credentials_filepath="$1"
    property_name="$2"
    if ! [ -f "$credentials_filepath" ] ; then
        echo "error: credentials file $credentials_filepath could not be found." >&2
        exit 2
    fi
    if ! [ -r "$credentials_filepath" ] ; then
        echo "error: credentials file $credentials_filepath was found but is not readable." >&2
        exit 2
    fi
    local line_with_property=""
    line_with_property="$(cat "$credentials_filepath" | grep "$property_name")"
    if [ -z $line_with_property ] ; then
        echo "error: could not find property '$property_name' in file '$credentials_filepath'"
        exit 2
    fi
    output_string_with_prefix_removed "$line_with_property" "$property_name="
}

function read_account_credentials() {
    local credentials_filepath="$config_filepath_dir/${cluster_account_id_to_credentials_filename[$cluster_account_id]}"
    if ! [ -f "$credentials_filepath" ] ; then
        echo "error: credentials file $credentials_filepath could not be found. exiting..." >&2
        exit 2
    fi
    if ! [ -r "$credentials_filepath" ] ; then
        echo "error: credentials file $credentials_filepath was found but is not readable. exiting..." >&2
        exit 2
    fi
    local aws_account_number="$(grep -rh eks.account.number $credentials_filepath | sed 's/eks.account.number=//')"
    local aws_role="$(grep -rh eks.role $credentials_filepath | sed 's/eks.role=//')"
    saml2aws_username="$(grep -rh eks.account.name $credentials_filepath | sed 's/eks.account.name=//')"
    saml2aws_password="$(grep -rh eks.password $credentials_filepath | sed 's/eks.password=//')"
    saml2aws_role="arn:aws:iam::$aws_account_number:role/$aws_role"
    if [ -z $aws_account_number ] ; then
        echo "error: missing credential value for eks.account.number in $credentials_filepath. exiting..." >&2
        exit 2
    fi
    if [ -z $aws_role ] ; then
        echo "error: missing credential value for eks.role in $credentials_filepath. exiting..." >&2
        exit 2
    fi
    if [ -z $saml2aws_password ] ; then
        echo "error: missing credential value for eks.password in $credentials_filepath. exiting..." >&2
        exit 2
    fi
    if [ -z $saml2aws_username ] ; then
        echo "error: missing credential value for eks.account.name in $credentials_filepath. exiting..." >&2
        exit 2
    fi
}

function check_that_docker_image_is_available() {
    if [ -z "$(docker images -q saml2aws)" ] ; then
        echo "error: docker image for saml2aws is not available in the local docker image collection. exiting..." >&2
        exit 3
    fi
}

function create_saml2aws_hidden_file_if_needed() {
    if [ ! -f "$HOME/.saml2aws" ]; then
        touch "$HOME/.saml2aws"
    fi
}

function output_credentials_filepath_for_account_id() {
    local cluser_account_id="$1"
    local account_credentials_filename="${cluster_account_id_to_credentials_filename[$cluster_account_id]}"
    local account_credentials_filepath="$config_filepath_dir/$account_credentials_filename"
    echo "$account_credentials_filepath"
}

function authenticate_with_saml2aws() {
    local cluser_account_id="$1"
    local SESSION_DURATION_FOR_TOKEN_SECONDS=28800
    local saml2aws_profile="automation_$cluster_account_id"
    local account_credentials_filepath="$(output_credentials_filepath_for_account_id $cluster_account_id)"
    local aws_account_number="$(output_property_from_credentials_filepath $account_credentials_filepath eks.account.number)"
    local aws_role="$(output_property_from_credentials_filepath $account_credentials_filepath eks.role)"
    local saml2aws_role="arn:aws:iam::$aws_account_number:role/$aws_role"
    local saml2aws_username="$(output_property_from_credentials_filepath $account_credentials_filepath eks.account.name)"
    local saml2aws_password="$(output_property_from_credentials_filepath $account_credentials_filepath eks.password)"
    if [ -z "$aws_account_number" ] || [ -z "$aws_role" ] || [ -z "$saml2aws_username" ] || [ -z "$saml2aws_password" ] ; then
        echo "error: failed to extract account number, role, username, and password from '$account_credentials_filepath'" >&2
        exit 2
    fi
    set -euo pipefail
    docker run --rm -i \
        -u "$(id -u):$(id -g)" \
        -v "$HOME/.saml2aws:/saml2aws/.saml2aws" \
        -v "$HOME/.aws:/saml2aws/.aws" \
        saml2aws login --force --skip-prompt --mfa=Auto --profile=$saml2aws_profile \
        --role="$saml2aws_role" --username="$saml2aws_username" --password="$saml2aws_password" \
        --session-duration=$SESSION_DURATION_FOR_TOKEN_SECONDS
    docker_status_code=$?
    set +uo pipefail
    if [ $docker_status_code -ne 0 ] ; then
        echo "Warning : aws authentication failed. Returned status code was $docker_status_code" >&2
        exit $docker_status_code
    fi
    if ! [ "$cluster_account_id" == "eks" ] ; then
        return 0
    fi
    # FOR TEMPORARY BACKWARDS COMPATIBILITY .. reauthenticate under the default profile
    docker run --rm -i \
        -u "$(id -u):$(id -g)" \
        -v "$HOME/.saml2aws:/saml2aws/.saml2aws" \
        -v "$HOME/.aws:/saml2aws/.aws" \
        saml2aws login --force --skip-prompt --mfa=Auto \
        --role="$saml2aws_role" --username="$saml2aws_username" --password="$saml2aws_password" \
        --session-duration=$SESSION_DURATION_FOR_TOKEN_SECONDS
}

function main() {
    local cluster_account_id="$1"
    if ! [ -z "$2" ] ; then
        config_filepath="$2"
        config_filepath_dir="$(dirname $config_filepath)"
        echo "using config file '$config_filepath'"
    fi
    load_automation_environment
    read_config
    validate_args "$cluster_account_id"
    check_that_docker_image_is_available
    create_saml2aws_hidden_file_if_needed
    authenticate_with_saml2aws "$cluster_account_id"
}

main $@
