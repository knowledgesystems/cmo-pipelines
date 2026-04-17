#!/usr/bin/env bash

# file scope variables
THIS_SCRIPT_FILEPATH="/data/portal-cron/scripts/set-environment-vars-from-automation-script.sh"
unset user_specified_parameter_names
declare -a user_specified_parameter_names

function script_was_directly_executed() {
    [ "$0" == "$THIS_SCRIPT_FILEPATH" ]
}

function output_you_must_source_warning() {
    echo "this script fails to function when you directly execute it as a new process."
    echo "Instead you must use bash's 'source' or '.' to alter your current environment."
    echo "try :"
    echo "  source $THIS_SCRIPT_FILEPATH VARNAME1 VARNAME2 ... (or 'all')"
}

function set_user_selections() {
    while [ $# -gt 0 ] ; do
        user_specified_parameter_names+=("$1")
        shift
    done
}

function expression_contains_subshell() {
    [[ "$1" == *\$\(* ]] || [[ "$1" == *\`* ]]
}

function should_set_variable_with_name() {
    local varname=$1
    if [ ${user_specified_parameter_names[0]} == "all" ] ; then
        return 0
    fi
    local pos=0
    while [ $pos -lt ${#user_specified_parameter_names[*]} ] ; do
        if [ "${user_specified_parameter_names[$pos]}" == "$varname" ] ; then
            return 0
        fi
        pos=$(($pos+1))
    done
    return 1
}

function find_and_source_simple_environment_variable_settings() {
    local AUTOMATION_ENVIRONMENT_SCRIPT="/data/portal-cron/scripts/automation-environment.sh"
    if ! [ -r "$AUTOMATION_ENVIRONMENT_SCRIPT" ] ; then
        return 0 # no script found
    fi
    while IFS= read -r line; do
        local SIMPLE_ENV_ASSIGN_REGEX='^export[[:space:]]*([[:alnum:]_]*)[[:space:]]*=[[:space:]]*(.*)'
        if ! [[ "$line" =~ $SIMPLE_ENV_ASSIGN_REGEX ]] ; then
            continue # skip non-simple or indented assignments
        fi
        local varname="${BASH_REMATCH[1]}"
        local right_hand_side="${BASH_REMATCH[2]}"
        if ! should_set_variable_with_name "$varname" ; then
            continue # skip variables user does not want
        fi
        if expression_contains_subshell "$right_hand_side" ; then
            continue
        fi
        export $varname="$(eval "echo $right_hand_side")"
    done < "$AUTOMATION_ENVIRONMENT_SCRIPT"
    return 0
}

function cleanup_file_scope() {
    unset -f script_was_directly_executed
    unset -f output_you_must_source_warning
    unset -f set_user_selections
    unset -f expression_contains_subshell
    unset -f should_set_variable_with_name
    unset -f find_and_source_simple_environment_variable_settings
    unset -f main
    unset -f cleanup_file_scope
    unset THIS_SCRIPT_FILEPATH
    unset user_specified_parameter_names
}

function main() {
    if script_was_directly_executed ; then
        output_you_must_source_warning
        return 1
    fi
    set_user_selections $@
    find_and_source_simple_environment_variable_settings
}

if main $@ ; then
    echo "done"
    cleanup_file_scope
    [ 0 -eq 0 ] # generate zero exit status without using variables
else
    echo "error detected - check environment"
    cleanup_file_scope
    [ 0 -eq 1 ] # generate non-zero exit status without using variables
fi
