# bash functions for parsing values from a color swap config file
#
# these functions are intended for use in other scripts which require access to values in a color swap config file, which is in a yaml-like format
# this file should be "sourced" in the client script with a command such as "source color-config-parsing-functions.sh"
# correct behavior requires the definition of environment variable YQ_BINARY containing a filepath to executable tool 'yq'

#######################################
# Exit process if YQ_BINARY does not point to an executable.
# Gobals:
#   YQ_BINARY
# Arguments:
#   None
# Side effects:
#   process terminated on error
#######################################
function exit_with_error_if_yq_unavailable() {
    if [ -z "$YQ_BINARY" ] ; then
        echo "Error : environment YQ_BINARY is undefined. This must contain a valid filepath to the yq executable."
        exit 1
    fi
    if ! [ -f "$YQ_BINARY" ] ; then
        echo "Error : environment YQ_BINARY does not contain a valid filepath to the yq executable."
        exit 1
    fi
    if ! [ -x "$YQ_BINARY" ] ; then
        echo "Error : environment YQ_BINARY contains a path to a file which is not executable."
        exit 1
    fi
}

#######################################
# Use yq to find a scalar value.
# Gobals:
#   YQ_BINARY
# Arguments:
#   filepath to the config file
#   yq element_path specifier to find the value
# Output:
#   the scalar value as a string to stdout
# Side effects:
#   process terminated on error
#######################################
function read_scalar_from_yaml() {
    local filepath="$1"
    local element_path="$2"
    exit_with_error_if_yq_unavailable
    local value=$("$YQ_BINARY" -r "$element_path" "$filepath")
    if [ "$value" == "null" ] || [ -z "$value" ] ; then
        echo "Error : missing required scalar '$element_path' in $filepath" >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

#######################################
# Use yq to find an array of values and return them in a global array variable
# Gobals:
#   YQ_BINARY
#   a global variable with a name provided by the caller
# Arguments:
#   the name of a global variable to return the retrieved array values in
#   filepath to the config file
#   yq element_path specifier to find the array of values
# Side effects:
#   process terminated on error
#######################################
function read_array_from_yaml() {
    local dest_var="$1"
    local filepath="$2"
    local element_path="$3"
    exit_with_error_if_yq_unavailable
    local array_path="${element_path}[]"
    local type=$("$YQ_BINARY" -r "$element_path | type" "$filepath")
    if [ "$type" != "!!seq" ] ; then
        echo "Error : expected array at '$element_path' in $filepath" >&2
        exit 1
    fi
    unset "$dest_var"
    declare -g -a "$dest_var"
    readarray -t "$dest_var" < <("$YQ_BINARY" -r "$array_path" "$filepath")
}

#######################################
# Use yq to find an dictionary/map of values and return them in a global array variable
# Gobals:
#   YQ_BINARY
#   a global variable with a name provided by the caller
# Arguments:
#   the name of a global variable to return the retrieved key/value pairs in
#   filepath to the config file
#   yq element_path specifier to find the dictionary of key/value pairs
# Side effects:
#   process terminated on error
#######################################
function read_map_from_yaml() {
    local dest_var="$1"
    local filepath="$2"
    local element_path="$3"
    exit_with_error_if_yq_unavailable
    local type=$("$YQ_BINARY" -r "$element_path | type" "$filepath")
    if [ "$type" != "!!map" ] ; then
        echo "Error : expected map at '$element_path' in $filepath" >&2
        exit 1
    fi
    unset "$dest_var"
    declare -gA "$dest_var"
    local has_entries=0
    while IFS=$'\t' read -r entry_key entry_value ; do
        printf -v "$dest_var[$entry_key]" '%s' "$entry_value"
        has_entries=1
    done < <("$YQ_BINARY" -r "$element_path | to_entries[] | [.key, .value] | @tsv" "$filepath")
    if [ "$has_entries" -eq 0 ] ; then
        echo "Error : map at '$element_path' in $filepath must contain at least one entry" >&2
        exit 1
    fi
}
