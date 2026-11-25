#!/usr/bin/env bash

# check for needed environment variables
if [ -z "$PORTAL_HOME" ] || [ -z "$YQ_BINARY" ] || [ -z "$GIT_BINARY" ] ; then
    echo "one or more required environment variables {PORTAL_HOME, YQ_BINARY, GIT_BINARY} are not set. Consider running automation_environment.sh" >&2
    exit 1
fi

# non-local environment variables in use
unset PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH
declare -A PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH
unset PORTAL_DB_NAME_TO_RELATIVE_CONFIGMAP_YAML_FILEPATH
declare -A PORTAL_DB_NAME_TO_RELATIVE_CONFIGMAP_YAML_FILEPATH
unset PORTAL_DB_NAME_TO_DATA_ITEM_KEY
declare -A PORTAL_DB_NAME_TO_DATA_ITEM_KEY

function usage() {
    echo "usage: set_extra_cbioportal_configmap_color.sh portal-db-name color" >&2
    if [ ${#PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[@]} -gt 0 ] ; then
        echo "         valid portal-db-names seem to be:"
        for portal_db_name in ${!PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[@]} ; do
            echo "             $portal_db_name"
        done
    fi
    echo "         color must be in {blue, green}" >&2
}

function load_configuration_or_exit_with_error() {
    local CONFIGURATION_FILEPATH="$PORTAL_HOME/pipelines-credentials/set_extra_cbioportal_configmap_color.yaml"
    if ! [ -f "$CONFIGURATION_FILEPATH" ] ; then
        echo "error: could not find configuration file $CONFIGURATION_FILEPATH" >&2
        exit 1
    fi
    local portal_db_names=()
    while IFS= read -r line || [ -n "$line" ] ; do
        portal_db_names+=(${line:2})
    done < <($YQ_BINARY "keys()" "$CONFIGURATION_FILEPATH")
    local pos=0
    while [ $pos -lt ${#portal_db_names[@]} ] ; do
        local portal_db_name="${portal_db_names[$pos]}"
        local query_spec=".$portal_db_name.portal_configuration_dirpath"
        IFS= read -r val < <($YQ_BINARY $query_spec "$CONFIGURATION_FILEPATH")
        PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[$portal_db_name]="$val"
        query_spec=".$portal_db_name.relative_configmap_yaml_filepath"
        IFS= read -r val < <($YQ_BINARY $query_spec "$CONFIGURATION_FILEPATH")
        PORTAL_DB_NAME_TO_RELATIVE_CONFIGMAP_YAML_FILEPATH[$portal_db_name]="$val"
        query_spec=".$portal_db_name.data_item_key"
        IFS= read -r val < <($YQ_BINARY $query_spec "$CONFIGURATION_FILEPATH")
        PORTAL_DB_NAME_TO_DATA_ITEM_KEY[$portal_db_name]="$val"
        pos=$(($pos+1))
    done
    #validate
    if [ ${#portal_db_names[@]} -lt 1 ] ; then
        echo "error: no portal databaes names were defined in $CONFIGURATION_FILEPATH" >&2
        usage
        exit 1
    fi
    local pos=0
    while [ $pos -lt ${#portal_db_names[@]} ] ; do
        local portal_db_name="${portal_db_names[$pos]}"
        if [ -z "${PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[$portal_db_name]}" ] || \
                [ -z "${PORTAL_DB_NAME_TO_RELATIVE_CONFIGMAP_YAML_FILEPATH[$portal_db_name]}" ] || \
                [ -z "${PORTAL_DB_NAME_TO_DATA_ITEM_KEY[$portal_db_name]}" ] ; then
            echo "error: one or more required value is missing in section $portal_db_name of $CONFIGURATION_FILEPATH" >&2
            usage
            exit 1
        fi
        pos=$(($pos+1))
    done
}

function validate_arguments_or_exit_with_error() {
    local portal_db_name="$1"
    local color="$2"
    local kubeconfig_filepath="$3"
    if [ -z "${PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[$portal_db_name]}" ] ; then
        echo "error: portal_db_name '$portal_db_name' is not valid" >&2
        usage
        exit 1
    fi
    if ! [ "$color" == "blue" ] ; then
        if ! [ "$color" == "green" ] ; then
            echo "error: color '$color' is not valid" >&2
            usage
            exit 1
        fi
    fi
	if [ -z "$kubeconfig_filepath" ] ; then
		echo "error: required kubeconfig file not provided" >&2
		usage
		exit 1
	fi
	if ! [ -f "$kubeconfig_filepath" ] ; then
		echo "error: provided kubeconfig file does not exist : $kubeconfig_filepath" >&2
		usage
		exit 1
	fi
}

function git_repo_clone_is_current() {
    local git_repo_dirpath="$1"
    # note : the next statement defines a multi-line string
    expected_up_to_date_status_report="On branch main
Your branch is up to date with 'origin/main'."
    $GIT_BINARY -C $git_repo_dirpath checkout main > /dev/null 2>&1
    $GIT_BINARY -C $git_repo_dirpath pull > /dev/null 2>&1
    status_report=$($GIT_BINARY -C $git_repo_dirpath status | head -n 2)
    if [ "$expected_up_to_date_status_report" == "$status_report" ] ; then
        return 0
    else
        echo "git repository does not appear to be current"
        echo "expected response from command '$GIT_BINARY -C $git_repo_dirpath status' was:"
        echo "$expected_up_to_date_status_report"
        echo "however, response actually received was:"
        echo "$status_report"
        echo nomatch
        return 1
    fi
}

function check_that_git_repo_clone_is_current_or_exit_with_error() {
    local git_repo_dirpath="$1"
    if ! git_repo_clone_is_current $git_repo_dirpath ; then
        exit 1
    fi
}

function alter_configmap_yaml_file() {
    local git_repo_dirpath="$1"
    local git_repo_relative_configmap_filepath="$2"
    local data_item_key="$3"
    local color="$4"
    local configmap_filepath="$git_repo_dirpath/$git_repo_relative_configmap_filepath"
    if ! "$YQ_BINARY" --inplace "( .data.\"$data_item_key\" ) = \"$color\"" "$configmap_filepath" ; then
        echo "error: failed to update $configmap_filepath" >&2
        exit 1
    fi
    return 0
}

function apply_altered_configmap_yaml_file() {
    local git_repo_dirpath="$1"
    local git_repo_relative_configmap_filepath="$2"
	local kubeconfig_filepath="$3"
    local configmap_filepath="$git_repo_dirpath/$git_repo_relative_configmap_filepath"
    echo "applying configmap yaml file"
    if ! kubectl --kubeconfig "$kubeconfig_filepath" apply -f "$configmap_filepath" ; then
        echo "Warning : received non-zero exit status for command kubectl --kubeconfig $kubeconfig_filepath apply -f $configmap_filepath"
    fi
    return 0
}

function commit_and_push_changes() {
    local git_repo_dirpath="$1"
    local git_repo_relative_configmap_filepath="$2"
    local portal_db_name="$3"
    local configmap_filepath="$git_repo_dirpath/$git_repo_relative_configmap_filepath"
    echo "committing and pushing changes to github repo"
    if ! $GIT_BINARY -C $git_repo_dirpath add "$git_repo_relative_configmap_filepath" >/dev/null 2>&1 ; then
        echo "warning : failure when adding file $git_repo_relative_configmap_filepath to changeset" >&2
    fi
    local date_string=$(date +%Y-%m-%d)
    local GIT_COMMIT_MSG="$portal_db_name import"
    local commit_message_string="$GIT_COMMIT_MSG $date_string"
    if ! $GIT_BINARY -C $git_repo_dirpath commit -m "$commit_message_string" >/dev/null 2>&1 ; then
        echo "warning : failure when committing changes to git repository clone" >&2
    fi
    if ! $GIT_BINARY -C $git_repo_dirpath pull --rebase >/dev/null 2>&1 ; then
        echo "warning : failure when preparing to push changes (during git pull --rebase)" >&2
    fi
    if ! $GIT_BINARY -C $git_repo_dirpath push >/dev/null 2>&1 ; then
        echo "warning : failure when pushing changes to git repository" >&2
    fi
    return 0
}

function set_color_in_configmap() {
    local portal_db_name="$1"
    local color="$2"
	local kubeconfig_filepath="$3"
    local git_repo_dirpath="${PORTAL_DB_NAME_TO_PORTAL_CONFIGURATION_DIRPATH[$portal_db_name]}"
    local git_repo_relative_configmap_filepath="${PORTAL_DB_NAME_TO_RELATIVE_CONFIGMAP_YAML_FILEPATH[$portal_db_name]}"
    local data_item_key="${PORTAL_DB_NAME_TO_DATA_ITEM_KEY[$portal_db_name]}"
    check_that_git_repo_clone_is_current_or_exit_with_error "$git_repo_dirpath"
    alter_configmap_yaml_file "$git_repo_dirpath" "$git_repo_relative_configmap_filepath" "$data_item_key" "$color"
    apply_altered_configmap_yaml_file "$git_repo_dirpath" "$git_repo_relative_configmap_filepath" "$kubeconfig_filepath"
    commit_and_push_changes "$git_repo_dirpath" "$git_repo_relative_configmap_filepath" "$portal_db_name"
    return 0
}

function main() {
    load_configuration_or_exit_with_error
    local portal_db_name="$1"
    local color="$2"
	local kubeconfig_filepath="$3"
    validate_arguments_or_exit_with_error "$portal_db_name" "$color" "$kubeconfig_filepath"
    local exit_status=0
    if ! set_color_in_configmap "$portal_db_name" "$color" "$kubeconfig_filepath"; then
        exit_status=1
    fi
    return $exit_status
}

main "$1" "$2" "$3"
