#!/usr/bin/env bash

# data_source_repo_clone_manager.sh : a script which uses git operations to
#     manage (update, cleanup) source data repositories for cBioPortal import.
#
#     Positional arguments:
#     The first argument is the filepath to a config file which identifies
#         and provides needed meta information for:
#         - hosts (where import processes may be run)
#         - data source repositories (which may need to be managed)
#         - destination databases (which may be imported to)
#     The second argument is a command keyword {'pull' or 'cleanup'}
#     The third argument identifies the destination database for import
#     All remaining arguments identify the specific repositories to manage

########################################
# script global variables
########################################

#---------------------------------------
# command line argument holding vars
unset arg_config_filepath
unset arg_command
unset arg_destination_database_id
unset arg_repository_ids
arg_config_filepath=""
arg_command=""
arg_destination_database_id=""
declare -a arg_repository_ids

#---------------------------------------
# config file content holding vars
unset host_id_to_can_connect_to_msk_network
unset host_id_to_self_reported_hostname
unset host_id_to_ssh_dns_hostname
unset host_id_to_incoming_ssh_username
unset host_id_to_ssh_private_key_local_filepath
unset all_repositories
unset repository_id_to_local_path_for_remote_rsync
unset repository_id_to_limited_scope_subdirectory
unset repository_and_host_to_clone_dirpath
unset destination_database_id_to_importer_host
unset destination_database_id_to_source_repo_list
declare -A host_id_to_can_connect_to_msk_network
declare -A host_id_to_self_reported_hostname
declare -A host_id_to_ssh_dns_hostname
declare -A host_id_to_incoming_ssh_username
declare -A host_id_to_ssh_private_key_local_filepath
declare -A all_repositories
declare -A repository_id_to_limited_scope_subdirectory
declare -A repository_and_host_to_clone_dirpath
declare -A repository_id_to_local_path_for_remote_rsync
declare -A destination_database_id_to_importer_host
declare -A destination_database_id_to_source_repo_list

#---------------------------------------
# config file parsing / buffering vars
unset host_object_buffer_host_id
unset host_object_buffer_can_connect_to_msk_network
unset host_object_buffer_self_reported_hostname
unset host_object_buffer_ssh_dns_hostname
unset host_object_buffer_incoming_ssh_username
unset host_object_buffer_ssh_private_key_local_filepath
unset repository_object_buffer_repository_id
unset repository_object_buffer_local_path_for_remote_rsync
unset repository_object_buffer_limited_scope_subdirectory
unset repository_object_buffer_host_to_clone_dirpath_map
unset database_destination_object_buffer_database_destination_id
unset database_destination_object_buffer_importer_host
unset database_destination_object_buffer_source_repositories
host_object_buffer_host_id=""
host_object_buffer_can_connect_to_msk_network=""
host_object_buffer_self_reported_hostname=""
host_object_buffer_ssh_dns_hostname=""
host_object_buffer_incoming_ssh_username=""
host_object_buffer_ssh_private_key_local_filepath=""
repository_object_buffer_repository_id=""
repository_object_buffer_local_path_for_remote_rsync=""
repository_object_buffer_limited_scope_subdirectory=""
declare -A repository_object_buffer_host_to_clone_dirpath_map
database_destination_object_buffer_database_destination_id=""
database_destination_object_buffer_importer_host=""
declare -a database_destination_object_buffer_source_repositories

#---------------------------------------
# general global variables
unset determined_local_host_id
unset determined_importer_host_id
determined_local_host_id=""
determined_importer_host_id=""

#---------------------------------------
# return value passing vars
unset determined_indent
unset determined_space_suffix_lenth
determined_indent=0
determined_space_suffix_length=0
########################################
# function definitions
########################################

#---------------------------------------
# arg parsing functions

function usage() {
    echo "data_source_repo_clone_manager.sh config_filepath command destination_database_id repo_id..."
    echo "    destination_database_id must be a database defined in the config file"
    echo "    command can be 'pull' to reset mentioned repositories to a clean state on the default branch with all"
    echo "       available updates applied, or can be 'cleanup' which cleans and prunes the mentioned repositories"
    echo "    repo_id is one or more repository identifiers defined in the config file, or 'all_sources'"
    echo "        to indicate that all valid data source repositories for the destination database should be updated."
} 

function parse_repository_arg() {
    while [ ${#@} -gt 0 ] ; do
        arg_repository_ids+=($1)
        shift 1
    done
}

function parse_args() {
    local arg_count=${#@}
    if [ $arg_count -lt 4 ] ; then
        usage
        return 1
    fi
    arg_config_filepath=$1
    arg_command=$2
    arg_destination_database_id=$3
    shift 3
    while [ ${#@} -gt 0 ] ; do
        # do not preserve spaces within a repository arg .. split into separate arguments
        parse_repository_arg $1
        shift 1
    done
}

########################################
# config file parsing functions

# config file is formatted in a restricted yaml format. A full yaml parser is not
# included, so the indents and overall order and format must be maintained in
# order to successfully parse the file

function determine_line_indent() {
    local line=$1
    determined_indent=0
    while [ $determined_indent -lt ${#line} ] ; do
        if ! [ "${line:$determined_indent:1}" == " " ] ; then
            break
        fi
        determined_indent=$((determined_indent+1))
    done
}

function determine_space_suffix_length() {
    local line=$1
    determined_space_suffix_length=0
    pos=$((${#line}-1))
    while [ $pos -ge 0 ] ; do
        local pos=$((${#line}-1-$determined_space_suffix_length))
        if ! [ "${line:$pos:1}" == " " ] ; then
            break
        fi
        pos=$(($pos-1))
        determined_space_suffix_length=$(($determined_space_suffix_length+1))
    done
}

function config_line_is_comment_or_blank() {
    local line_without_indent=$1
    if [ ${#line_without_indent} -eq 0 ] ; then
        return 0 # line is blank
    fi
    if [ ${line_without_indent:0:1} == "#" ] ; then
        return 0 # line is comment
    fi
    return 1
}
    
function config_line_is_doc_start() {
    local line=$1
    [ "$line" == "---" ]
}

function config_line_is_section_start() {
    local indent=$1
    local line_without_indent=$2
    local section_id=$3
    [ $indent -eq 0 ] && [ "$line_without_indent" == "$section_id:" ]
}

function config_line_is_object_start() {
    local indent=$1
    local line_without_indent=$2
    [ $indent -eq 2 ] && [ "$line_without_indent" == "-" ]
}

function config_line_is_clone_dirpath_map() {
    local indent=$1
    local line_without_indent=$2
    [ $indent -eq 4 ] && [ "$line_without_indent" == "repository_clone_dirpath_per_host_map:" ]
}

function config_line_is_all_recognized_sources_array() {
    local indent=$1
    local line_without_indent=$2
    local section_id=$3
    [ $indent -eq 4 ] && [ "$line_without_indent" == "all_recognized_source_repositories:" ]
}

# record_object_in_buffer() transfers the parsed content held in the buffered object
# global variables, and converts and stores an equivalent representation in the config
# file content holding variables. The section which was actively being parsed must be
# identified in the arguemnts to the function
function record_object_in_buffer() {
    within_hosts=$1
    within_repositories=$2
    within_database_destinations=$3
    if [ $within_hosts == "y" ] ; then
        if [ ${#host_object_buffer_host_id} -eq 0 ]  ; then
            echo "Error : an object of type host was missing a defined host_id in $arg_config_filepath" >&2
            return 1
        fi
        host_id_to_can_connect_to_msk_network["$host_object_buffer_host_id"]="$host_object_buffer_can_connect_to_msk_network"
        host_id_to_self_reported_hostname["$host_object_buffer_host_id"]="$host_object_buffer_self_reported_hostname"
        host_id_to_ssh_dns_hostname["$host_object_buffer_host_id"]="$host_object_buffer_ssh_dns_hostname"
        host_id_to_incoming_ssh_username["$host_object_buffer_host_id"]="$host_object_buffer_incoming_ssh_username"
        host_id_to_ssh_private_key_local_filepath["$host_object_buffer_host_id"]="$host_object_buffer_ssh_private_key_local_filepath"
        return 0
    fi
    if [ $within_repositories == "y" ] ; then
        if [ ${#repository_object_buffer_repository_id} -eq 0 ]  ; then
            echo "Error : an object of type repository was missing a defined repository_id in $arg_config_filepath" >&2
            return 1
        fi
        all_repositories[$repository_object_buffer_repository_id]=1
        repository_id_to_local_path_for_remote_rsync[$repository_object_buffer_repository_id]="$repository_object_buffer_local_path_for_remote_rsync"
        repository_id_to_limited_scope_subdirectory[$repository_object_buffer_repository_id]="$repository_object_buffer_limited_scope_subdirectory"
        local host_list=${!repository_object_buffer_host_to_clone_dirpath_map[*]}
        for host in $host_list ; do
            clone_dirpath="${repository_object_buffer_host_to_clone_dirpath_map[$host]}"
            composite_key="$repository_object_buffer_repository_id:$host"
            repository_and_host_to_clone_dirpath[$composite_key]="$clone_dirpath"
        done
        return 0
    fi
    if [ $within_database_destinations == "y" ] ; then
        if [ ${#database_destination_object_buffer_database_destination_id} -eq 0 ]  ; then
            echo "Error : an object of type destination database was missing a defined database destination id in $arg_config_filepath" >&2
            return 1
        fi
        destination_database_id_to_importer_host[$database_destination_object_buffer_database_destination_id]="$database_destination_object_buffer_importer_host"
        supported_source_repo_list="${database_destination_object_buffer_source_repositories[*]}"
        destination_database_id_to_source_repo_list[$database_destination_object_buffer_database_destination_id]="${supported_source_repo_list}"
        return 0
    fi
    echo "Error : an unrecognized object type was encountered in config file $arg_config_filepath" >&2
    return 1
}

# clear_object_buffer() blanks out the needed buffered object global variables to prepare
# for parsing the next object from the config file.
function clear_object_buffer() {
    local within_hosts=$1
    local within_repositories=$2
    local within_database_destinations=$3
    if [ $within_hosts == "y" ] ; then
        host_object_buffer_host_id=""
        host_object_buffer_can_connect_to_msk_network=""
        host_object_buffer_self_reported_hostname=""
        host_object_buffer_ssh_dns_hostname=""
        host_object_buffer_incoming_ssh_username=""
        host_object_buffer_ssh_private_key_local_filepath=""
        return 0
    fi
    if [ $within_repositories == "y" ] ; then
        repository_object_buffer_repository_id=""
        repository_object_buffer_local_path_for_remote_rsync=""
        repository_object_buffer_limited_scope_subdirectory=""
        unset repository_object_buffer_host_to_clone_dirpath_map
        declare -Ag repository_object_buffer_host_to_clone_dirpath_map
        return 0
    fi
    if [ $within_database_destinations == "y" ] ; then
        database_destination_object_buffer_database_destination_id=""
        database_destination_object_buffer_importer_host=""
        database_destination_object_buffer_source_repositories=()
        return 0
    fi
    echo "Error : attempt to clear unknown object buffer while parsing $arg_config_filepath" >&2
    return 1
}

# capture_setting_in_object_buffer() take a single line from the config file
# and transfers a meaningful configuration setting from the line into the
# appropriate buffered object global variable. The section (or subsection)
# which is actively being parsed must be identified in the arguments to the
# function. The line content (with separated indent offset) is also passed in.
function capture_setting_in_object_buffer() {
    local within_hosts=$1
    local within_repositories=$2
    local within_database_destinations=$3
    local within_clone_dirpath_map=$4
    local within_all_recognized_sources=$5
    local indent=$6
    local line_without_indent=$7
    # most data lines are of the form 'key: val', so split by colon and trim
    local key=${line_without_indent%%:*}
    local val=${line_without_indent##*:}
    determine_line_indent "$key"
    determine_space_suffix_length "$key"
    local trimmed_key_len=$((${#key}-$determined_indent-$determined_space_suffix_length))
    local trimmed_key=${key:$determined_indent:$trimmed_key_len}
    determine_line_indent "$val"
    determine_space_suffix_length "$val"
    local trimmed_val_len=$((${#val}-$determined_indent-$determined_space_suffix_length))
    local trimmed_val=${val:$determined_indent:$trimmed_val_len}
    # determine the section being parsed an look for a relevant config setting
    # save config setting in a buffer variable
    if [ $within_hosts == "y" ] ; then
        if ! [ $indent -eq 4 ] ; then
            echo "Error : config file indent not what expected within host object : '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi
        if [ "$key" == 'host_id' ] ; then
            host_object_buffer_host_id="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'host_can_connect_to_msk_network' ] ; then
            host_object_buffer_can_connect_to_msk_network="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'host_self_reported_hostname' ] ; then
            host_object_buffer_self_reported_hostname="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'host_ssh_dns_hostname' ] ; then
            host_object_buffer_ssh_dns_hostname="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'host_incoming_ssh_username' ] ; then
            host_object_buffer_incoming_ssh_username="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'host_ssh_private_key_local_filepath' ] ; then
            host_object_buffer_ssh_private_key_local_filepath="$trimmed_val"
            return 0
        fi
        echo "Error : config file contains unrecognized setting in host object : '$line_without_indent' in config file $arg_config_filepath" >&2
        return 1
    fi
    if [ $within_clone_dirpath_map == "y" ] ; then
        if ! [ $indent -eq 6 ] ; then
            echo "Error : config file indent not what expected within repository object clone dirpath map : '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi
        # key must be a defined host (this is why host objects must come before repositories)
        if [ "${#host_id_to_self_reported_hostname[$trimmed_key]}" -eq 0 ] ; then
            echo "Error : config file has an unrecognized host name as a key in the clone dirpath map : '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi 
        repository_object_buffer_host_to_clone_dirpath_map[$trimmed_key]="$trimmed_val"
        continue
    fi
    if [ $within_repositories == "y" ] ; then
        # prior conditional handled any clone_dirpath_map lines - handle others here
        if ! [ $indent -eq 4 ] ; then
            echo "Error : config file indent not what expected within repository object : '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi
        if [ "$key" == 'repository_id' ] ; then
            repository_object_buffer_repository_id="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'repository_local_path_for_remote_rsync' ] ; then
            if ! [ "$trimmed_val" == "none" ] ; then
                repository_object_buffer_local_path_for_remote_rsync="$trimmed_val"
            fi
            return 0
        fi
        if [ "$key" == 'repository_clone_limited_scope_subdirectory' ] ; then
            if ! [ "$trimmed_val" == "none" ] ; then
                repository_object_buffer_limited_scope_subdirectory="$trimmed_val"
            fi
            return 0
        fi
        echo "Error : config file contains unrecognized setting in repository object : '$line_without_indent' in config file $arg_config_filepath" >&2
        return 1
    fi
    if [ $within_all_recognized_sources == "y" ] ; then
        if ! [ $indent -eq 6 ] ; then
            echo "Error : config file indent not what expected within database destination object all recongnized sources repositories: '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi
        # without a colon, the entire line winds up in trimmed_val
        local truncated=""
        local repo_id=""
        if [ "${trimmed_val:0:2}" == "- " ] ; then
            truncated=${trimmed_val:2}
            repo_id="$(echo $truncated)" # discard remaining leading spaces
        fi
        # key must be a defined repository (this is why repository objects must come before database_destinations)
        local repo_defined="${all_repositories[$repo_id]}"
        if ! [ ${#repo_defined} -gt 0 ] ; then
            echo "Error: repository identifier '$repo_id' is not a recognized repository reference in the destination databases in config file $arg_config_filepath" >&2
            return 1
        fi
        database_destination_object_buffer_source_repositories+=($repo_id)
        continue
    fi
    if [ $within_database_destinations == "y" ] ; then
        # prior conditional handled any clone_dirpath_map lines - handle others here
        if ! [ $indent -eq 4 ] ; then
            echo "Error : config file indent not what expected within database_destination object : '$line_without_indent' in config file $arg_config_filepath" >&2
            return 1
        fi
        if [ "$key" == 'database_destination_id' ] ; then
            database_destination_object_buffer_database_destination_id="$trimmed_val"
            return 0
        fi
        if [ "$key" == 'configured_importer_host' ] ; then
                database_destination_object_buffer_importer_host="$trimmed_val"
            return 0
        fi
        echo "Error : config file contains unrecognized setting in database destination object : '$line_without_indent' in config file $arg_config_filepath" >&2
        return 1
    fi
    echo "Error : config file contains unrecognized line '$line_without_indent' in config file $arg_config_filepath" >&2
    return 1
}

# parse_config_file() iterates through the config file line by line. It
# picks out lines which indicate a transition to a new section or new object
# and alters the parsing state accordingly (consuming the line). Otherwise
# it relies on capture_setting_in_object_buffer() to process lines with
# actual configuration content.
function parse_config_file() {
    if ! [ -r "$arg_config_filepath" ] ; then
        echo "Error: cannot read configfile $arg_config_filepath" >&2
        return 1
    fi
    local doc_started="n"
    local within_hosts="n"
    local within_repositories="n"
    local within_database_destinations="n"
    local within_object="n"
    local within_clone_dirpath_map="n"
    local clone_dirpath_map_seen="n"
    local within_all_recognized_sources="n"
    local all_recognized_sources_seen="n"
    while IFS= read -r line; do
        determine_line_indent "$line"
        local indent=$determined_indent
        line_without_indent=${line:$indent}
        if config_line_is_comment_or_blank "$line_without_indent" ; then
            continue # ignore comments and blank lines
        fi
        if config_line_is_doc_start "$line" ; then
            if [ $doc_started == "y" ] ; then
                echo "Error: document start occurred more than once in $arg_config_filepath" >&2
                return 1
            else
                doc_started="y"
                continue
            fi
        fi
        if ! [ $doc_started == 'y' ] ; then
            echo "Error: meaningful document content appeared before document start '---' in $arg_config_filepath" >&2
            return 1
        fi
        if config_line_is_section_start $indent "$line_without_indent" "hosts" ; then
            if [ $within_object == "y" ] ; then
                record_object_in_buffer $within_hosts $within_repositories $within_database_destinations
            fi
            within_hosts="y"
            within_repositories="n"
            within_database_destinations="n"
            within_object="n"
            within_clone_dirpath_map="n"
            clone_dirpath_map_seen="n"
            within_all_recognized_sources="n"
            all_recognized_sources_seen="n"
            continue
        fi
        if config_line_is_section_start $indent "$line_without_indent" "repositories" ; then
            if [ $within_object == "y" ] ; then
                record_object_in_buffer $within_hosts $within_repositories $within_database_destinations
            fi
            within_hosts="n"
            within_repositories="y"
            within_database_destinations="n"
            within_object="n"
            within_clone_dirpath_map="n"
            clone_dirpath_map_seen="n"
            within_all_recognized_sources="n"
            all_recognized_sources_seen="n"
            continue
        fi
        if config_line_is_section_start $indent "$line_without_indent" "database_destinations" ; then
            if [ $within_object == "y" ] ; then
                record_object_in_buffer $within_hosts $within_repositories $within_database_destinations
            fi
            within_hosts="n"
            within_repositories="n"
            within_database_destinations="y"
            within_object="n"
            within_clone_dirpath_map="n"
            clone_dirpath_map_seen="n"
            within_all_recognized_sources="n"
            all_recognized_sources_seen="n"
            continue
        fi
        if [ $indent -eq 0 ] ; then
            echo "Error: content is not within a recognized section: '$line' in $arg_config_filepath" >&2
            return 1
        fi
        if config_line_is_object_start $indent "$line_without_indent" ; then
            if [ $within_object == "y" ] ; then
                # we have seen a previous object and are about to start parsing a new one
                record_object_in_buffer $within_hosts $within_repositories $within_database_destinations
            fi
            # get ready to parse a new object
            clear_object_buffer $within_hosts $within_repositories $within_database_destinations
            within_clone_dirpath_map="n"
            clone_dirpath_map_seen="n"
            within_all_recognized_sources="n"
            all_recognized_sources_seen="n"
            within_object="y"
            continue
        fi
        if config_line_is_clone_dirpath_map $indent "$line_without_indent" ; then
            if [ $clone_dirpath_map_seen == "y" ] ; then
                echo "Error: more than one repository_clone_dirpath_per_host_map entry exists for a repository defined in $arg_config_filepath" >&2
                return 1
            fi
            clone_dirpath_map_seen="y"
            within_clone_dirpath_map="y"
            continue
        fi
        if [ $indent -ne 6 ] ; then
            within_clone_dirpath_map="n"
        fi
        if config_line_is_all_recognized_sources_array $indent "$line_without_indent" ; then
            if [ $all_recognized_sources_seen == "y" ] ; then
                echo "Error: more than one all_recognized_source_repositories entry exists for a destination database defined in $arg_config_filepath" >&2
                return 1
            fi
            all_recognized_sources_seen="y"
            within_all_recognized_sources="y"
            continue
        fi
        if [ $indent -ne 6 ] ; then
            within_all_recognized_sources="n"
        fi
        if ! capture_setting_in_object_buffer $within_hosts $within_repositories $within_database_destinations $within_clone_dirpath_map $within_all_recognized_sources $indent "$line_without_indent" ; then
            return 1
        fi
    done < $arg_config_filepath
    if [ $within_object == "y" ] ; then
        # record any parsed object in the buffer when the parse reaches end of file
        record_object_in_buffer $within_hosts $within_repositories $within_database_destinations
    fi
}

#---------------------------------------
# Other function definitions

function validate_args() {
    # check command keyword
    if ! [ "$arg_command" == 'pull' ] && ! [ "$arg_command" == 'cleanup' ] ; then
        echo "Error: command argument '$arg_command' is not in {'pull','cleanup'}" >&2
        return 1
    fi
    # check destination_database_id (so we know what kind of import we are about to do and where)
    importer_host="${destination_database_id_to_importer_host["$arg_destination_database_id"]}"
    if [ ${#importer_host} -eq 0 ] ; then
        echo "Error: destination_database_id '$arg_destination_database_id' is not defined in the config file $arg_config_filepath" >&2
        return 1
    fi
    repo_id_array_length=${#arg_repository_ids[*]}
    repo_id_pos=0
    while [ $repo_id_pos -lt $repo_id_array_length ] ; do
        local repo_id=${arg_repository_ids[$repo_id_pos]}
        local repo_defined="${all_repositories[$repo_id]}"
        if ! [ ${#repo_defined} -gt 0 ] ; then
            echo "Error: repository identifier '$repo_id' is not defined in the config file $arg_config_filepath" >&2
            return 1
        fi
        repo_id_pos=$(($repo_id_pos+1))
    done
}

function determine_local_host_id() {
    local hostname_output=$(hostname)
    for host_id in ${!host_id_to_self_reported_hostname[*]} ; do
        retrieved_hostname="${host_id_to_self_reported_hostname[$host_id]}" 
        if [ "$retrieved_hostname" == "$hostname_output" ] ; then
            determined_local_host_id="$host_id"
            return 0
        fi
    done
    return 1
}

function determine_importer_host_id() {
    retrieved_host_id="${destination_database_id_to_importer_host[$arg_destination_database_id]}" 
    if [ -n "$retrieved_host_id" ] ; then
        determined_importer_host_id="$retrieved_host_id"
        return 0
    fi
    return 1
}

function determine_host_ids() {
    determine_local_host_id && determine_importer_host_id
}

function import_will_run_locally() {
    [ "$determined_local_host_id" == "$determined_importer_host_id" ]
}

function repository_is_fully_cloned_locally() {
    destination_repository_path=$1
    if [ -e "$destination_repository_path/.git" ] ; then
        return 0
    else
        return 1
    fi
}

function clean_repository_clone_on_current_branch() {
    local repository_dirpath=$1
    if ! git -C "$repository_dirpath" checkout -- . ; then
        echo "Error: could not execute 'git checkout -- .' in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" clean -fd ; then
        echo "Error: could not execute 'git clean -fd' in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" reset --hard HEAD ; then
        echo "Error: could not execute 'git reset --hard HEAD' in clone at $repository_dirpath" >&2
        return 1
    fi
}

function switch_clone_to_default_branch_and_pull_updates() {
    local repository_dirpath=$1
    default_branchline="$(git -C $repository_dirpath remote show origin | grep -E '^  HEAD branch:')"
    default_branch="${default_branchline##*: }"
    if [ ${#default_branch} -eq 0 ] ; then
        echo "Error: could not determine default branch of repository in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" checkout $default_branch ; then
        echo "Error: could not execute 'git checkout $default_branch' in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" reset --hard origin/$default_branch; then
        echo "Error: could not execute 'git reset --hard origin/$default_branch' in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" pull origin $default_branch; then
        echo "Error: could not execute 'git pull origin $default_branch' in clone at $repository_dirpath" >&2
        return 1
    fi
    if ! git -C "$repository_dirpath" lfs pull origin $default_branch; then
        echo "Error: could not execute 'git lfs pull origin $default_branch' in clone at $repository_dirpath" >&2
        return 1
    fi
}

function rsync_clone_data_to_remote_host() {
    local local_repository_path=$1
    local destination_repository_path=$2
    local importer_host_id=$3
    local limited_scope_subdirectory=$4
    local importer_dns_hostname="${host_id_to_ssh_dns_hostname[$importer_host_id]}"
    local importer_ssh_username="${host_id_to_incoming_ssh_username[$importer_host_id]}"
    local importer_ssh_key_filepath="${host_id_to_ssh_private_key_local_filepath[$importer_host_id]}"
    rsync -a --progress --rsh="ssh -i '$importer_ssh_key_filepath'" --exclude=".git" --delete "$local_repository_path/$limited_scope_subdirectory" $importer_ssh_username@$importer_dns_hostname:$destination_repository_path
    return $?
}

function prune_repository_clone_on_current_branch() {
    local repository_dirpath=$1
    if ! git -C "$repository_dirpath" lfs prune ; then
        echo "Error: could not execute 'git lfs prune' in clone at $repository_dirpath" >&2
        return 1
    fi
}

function execute_command_for_repository() {
    local repository_id=$1
    local composite_key="$repository_id:$determined_importer_host_id"
    local destination_repository_path="${repository_and_host_to_clone_dirpath[$composite_key]}"
    if import_will_run_locally ; then
        if ! repository_is_fully_cloned_locally "$destination_repository_path" ; then
            echo "Local data for repository $repository_id contains no '.git' subdirectory."
            echo "  This means that the local host has no access to the github server and updates come from a remote server. Skipping."
            return 0
        fi
        if ! clean_repository_clone_on_current_branch "$destination_repository_path" ; then
            echo "Error: could not reset repository $repository_id to a clean state" >&2
            return 1
        fi
        if [ "$arg_command" == "pull" ] ; then
            switch_clone_to_default_branch_and_pull_updates "$destination_repository_path"
            return $?
        else
            # if not pull, we must be doing "prune"
            prune_repository_clone_on_current_branch "$destination_repository_path"
            return $?
        fi
    else
        local limited_scope_subdirectory="${repository_id_to_limited_scope_subdirectory[$repository_id]}"
        if [ "${#destination_repository_path}" -eq 0 ] ; then
            echo "repository $repository_id on host $determined_importer_host_id is managed locally on the remote machine. Skipping."
            return 0
        fi
        local local_repository_path="${repository_id_to_local_path_for_remote_rsync[$repository_id]}"
        if [ "${#local_repository_path}" -eq 0 ] ; then
            echo "repository $repository_id cannot be rsynced from this server. Skipping."
            return 0
        fi
        if ! clean_repository_clone_on_current_branch "$local_repository_path" ; then
            echo "Error: could not reset repository $repository_id to a clean state" >&2
            return 1
        fi
        if [ "$arg_command" == "pull" ] ; then
            switch_clone_to_default_branch_and_pull_updates "$local_repository_path" &&
            rsync_clone_data_to_remote_host "$local_repository_path" "$destination_repository_path" "$determined_importer_host_id" "$limited_scope_subdirectory"
            return $?
        else
            # if not pull, we must be doing "prune"
            prune_repository_clone_on_current_branch "$local_repository_path"
            return $?
        fi
    fi
}

function execute_command() {
    return_status=0
    for repository_id in ${arg_repository_ids[*]} ; do
        if ! execute_command_for_repository $repository_id ; then
            return_status=1
        fi
    done
    return $return_status
}

main() {
    parse_args "$@" &&
    parse_config_file &&
    validate_args &&
    determine_host_ids &&
    execute_command
}

main "$@"
