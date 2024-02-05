#!/usr/bin/env bash
#
# replace patient ids present in a clnical file in the cmo-access repository
# with the perferred patient id based on a input provided id mapping table.

# global variables
TAB=$'\t'
PATIENT_ID_MAPPING_FILTERED_FILENAME="patient_id_mapping_filtered.txt"
PATIENT_ID_MAPPING_AMBIGUOUS_FILENAME="patient_id_mapping_ambiguous.txt"
unset input_clinical_filepath
unset output_clinical_dirpath
unset patient_id_mapping_filepath
unset patient_id_mapping_filtered_filepath
unset patient_id_mapping_ambiguous_filepath
unset PROGRAM_NAME
input_clinical_filepath=""
output_clinical_filepath=""
patient_id_mapping_filepath=""
patient_id_mapping_filtered_filepath=""
patient_id_mapping_ambiguous_filepath=""
PROGRAM_NAME="$0"

function usage() {
    echo "Usage: $PROGRAM_NAME input_clinical_file output_directory patient_id_mapping_file" >&2
}

# validate and parse arguments
if [[ ${#@} -eq 1 ]] ; then
    if [[ "$1" == "-h" || "$1" == "--help" ]] ; then
        usage
        exit 0
    fi
fi
if [[ ${#@} -ne 3 ]] ; then
    usage
    exit 1
fi
input_clinical_filepath="$1"
output_clinical_dirpath="$2"
patient_id_mapping_filepath="$3"
if ! [[ -r "$input_clinical_filepath" ]] ; then
    echo "Error: expected a readable input file as input_clinical_file. Unable to read file '$input_clinical_filepath'" >&2
    exit 1
fi
if ! [[ -w "$output_clinical_dirpath" && -d "$output_clinical_dirpath" ]] ; then
    echo "Error: expected a writable output directory as output_directory. Unable to write to '$input_clinical_filepath'" >&2
    exit 1
fi
if ! [[ -r "$patient_id_mapping_filepath" ]] ; then
    echo "Error: expected a readable input file as patient_id_mapping_file. Unable to read file '$patient_id_mapping_filepath'" >&2
    exit 1
fi
patient_id_mapping_filtered_filepath="$output_clinical_dirpath/$PATIENT_ID_MAPPING_FILTERED_FILENAME"
patient_id_mapping_ambiguous_filepath="$output_clinical_dirpath/$PATIENT_ID_MAPPING_AMBIGUOUS_FILENAME"
output_clinical_filename="$(basename $input_clinical_filepath).updated"
output_clinical_filepath="$output_clinical_dirpath/$output_clinical_filename"

# filter patient id mappings of all rows missing either dmp-id or cmo-id
if ! cat "$patient_id_mapping_filepath" | egrep -v "^[[:space:]]" | egrep -v "[[:space:]]$" > "$patient_id_mapping_filtered_filepath" ; then
    echo "could not filter $patient_id_mapping_filepath of rows missing a dmp id" >&2
    exit 1
fi

# generate list of ambiguous cmo patient ids (have multiple associated dmp ids)
if ! cat "$patient_id_mapping_filtered_filepath" | sort | uniq | cut -f2 | sort | uniq -d > "$patient_id_mapping_ambiguous_filepath" ; then
    echo "could not find and output ambiguous cases from file $patient_id_mapping_filtered_filepath" >&2
    exit 1
fi

# read in the mapping of cmo patient ids to dmp patient ids and store in associative array
unset ambiguous_cmo_patient_id
declare -A ambiguous_cmo_patient_id
while read -r cmo_patient_id ; do
    ambiguous_cmo_patient_id[$cmo_patient_id]=1
done < "$patient_id_mapping_ambiguous_filepath"
unset dmp_id_for_cmo_id
declare -A dmp_id_for_cmo_id
while IFS="" read -r line ; do
    line_regex="^([^$TAB][^$TAB]*)$TAB([^$TAB][^$TAB]*)\$"
    if ! [[ "$line" =~ $line_regex ]] ; then
        echo "malformatted line in $patient_id_mapping_filtered_filepath : $line" >&2
        exit 1
    fi
    dmp_patient_id=${BASH_REMATCH[1]}
    cmo_patient_id=${BASH_REMATCH[2]}
    if ! [ -z ${ambiguous_cmo_patient_id["$cmo_patient_id"]} ] ; then
        continue; # skip ambiguous cmo_patient_ids
    fi
    dmp_id_for_cmo_id["$cmo_patient_id"]="$dmp_patient_id"
done < "$patient_id_mapping_filtered_filepath"

#replace any cmo-patient-ids in clinical sample file with associated dmp-patient-id
unset headerline
unset patient_id_colnum
while IFS="" read -r line ; do
    header_regex="^#"
    if [[ "$line" =~ $header_regex ]] ; then
        # simply output metadata headers
        echo "$line"
        continue
    fi
    if [ -z "$headerline" ] ; then
        # set the header
        headerline="$line"
        echo "$headerline"
        # find the column number of PATIENT_ID
        colnum=1
        scanheader="$headerline"
        while ! [ -z "$scanheader" ] ; do
            line_regex="^([^$TAB][^$TAB]*)$TAB(.*)\$"
            if ! [[ "$scanheader" =~ $line_regex ]] ; then
                # no tab found - examine final column
                if [[ "$scanheader" == "PATIENT_ID" ]] ; then
                    patient_id_colnum=$colnum
                    scanheader="" # scan is done
                fi
                break
            fi
            this_header="${BASH_REMATCH[1]}"
            rest_of_line="${BASH_REMATCH[2]}"
            if [[ "$this_header" == "PATIENT_ID" ]] ; then
                patient_id_colnum=$colnum
                scanheader="" # scan is done
            else
                colnum=$(($colnum+1))
                scanheader="$rest_of_line"
            fi
        done
        if [ -z $patient_id_colnum ] ; then
            echo "could not find column header PATIENT_ID in file $input_clinical_filepath" >&2
            exit 1
        fi
        continue
    fi
    # process and output each data line
    rest_of_line="$line"
    colnum=1
    while true ; do
        line_regex="^([^$TAB]*)$TAB(.*)\$"
        if ! [[ "$rest_of_line" =~ $line_regex ]] ; then
            # no tab found - output final column
            if [[ $colnum -eq $patient_id_colnum ]] ; then
                patient_id="$rest_of_line"
                replacement_patient_id="${dmp_id_for_cmo_id[$patient_id]}"
                if ! [ -z $replacement_patient_id ] ; then
                    echo "$replacement_patient_id"
                else
                    echo "$patient_id"
                fi
            else
                echo "$rest_of_line"
            fi
            break
        fi
        this_value="${BASH_REMATCH[1]}"
        rest_of_line="${BASH_REMATCH[2]}"
        if [[ $colnum -eq $patient_id_colnum ]] ; then
            replacement_patient_id="${dmp_id_for_cmo_id[$this_value]}"
            if ! [ -z $replacement_patient_id ] ; then
                echo -n "$replacement_patient_id$TAB"
            else
                echo -n "$this_value$TAB"
            fi
        else
            echo -n "$this_value$TAB"
        fi
        colnum=$(($colnum+1))
    done
done < "$input_clinical_filepath" > "$output_clinical_filepath"
