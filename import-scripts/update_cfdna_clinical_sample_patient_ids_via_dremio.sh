#!/usr/bin/env bash

CLINICAL_SAMPLE_FILEPATH="/data/portal-cron/cbio-portal-data/cmo-access/mixed_MSK_cfDNA_RESEARCH_ACCESS/data_clinical_sample.txt"
UPDATED_CLINICAL_SAMPLE_FILEPATH="data_clinical_sample_replaced.txt"
PATIENT_ID_MAPPING_FILEPATH="patient_id_mapping.txt"
PATIENT_ID_MAPPING_FILTERED_FILEPATH="patient_id_mapping_filtered.txt"
PATIENT_ID_MAPPING_AMBIGUOUS_FILEPATH="patient_id_mapping_ambiguous.txt"
TAB=$'\t'

# filter patient id mappings of all rows missing either dmp-id or cmo-id
if ! cat "$PATIENT_ID_MAPPING_FILEPATH" | egrep -v "^[[:space:]]" | egrep -v "[[:space:]]$" > "$PATIENT_ID_MAPPING_FILTERED_FILEPATH" ; then
    echo "could not filter $PATIENT_ID_MAPPING_FILEPATH of rows missing a dmp id" >&2
    exit 1
fi

# generate list of ambiguous cmo patient ids (have multiple associated dmp ids)
if ! cat "$PATIENT_ID_MAPPING_FILTERED_FILEPATH" | sort | uniq | cut -f2 | sort | uniq -d > "$PATIENT_ID_MAPPING_AMBIGUOUS_FILEPATH" ; then
    echo "could not findfilter $PATIENT_ID_MAPPING_FILEPATH of rows missing a dmp id" >&2
    exit 1
fi

# read in the mapping of cmo patient ids to dmp patient ids and store in associative array
unset ambiguous_cmo_patient_id
declare -A ambiguous_cmo_patient_id
while read -r cmo_patient_id ; do
    ambiguous_cmo_patient_id[$cmo_patient_id]=1
done < "$PATIENT_ID_MAPPING_AMBIGUOUS_FILEPATH"
unset dmp_id_for_cmo_id
declare -A dmp_id_for_cmo_id
while IFS="" read -r line ; do
    line_regex="^([^$TAB][^$TAB]*)$TAB([^$TAB][^$TAB]*)\$"
    if ! [[ "$line" =~ $line_regex ]] ; then
        echo "malformatted line in $PATIENT_ID_MAPPING_FILTERED_FILEPATH : $line" >&2
        exit 1
    fi
    dmp_patient_id=${BASH_REMATCH[1]}
    cmo_patient_id=${BASH_REMATCH[2]}
    if ! [ -z ${ambiguous_cmo_patient_id["$cmo_patient_id"]} ] ; then
        continue; # skip ambiguous cmo_patient_ids
    fi
    dmp_id_for_cmo_id["$cmo_patient_id"]="$dmp_patient_id"
done < "$PATIENT_ID_MAPPING_FILTERED_FILEPATH"

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
            echo "could not find column header PATIENT_ID in file $CLINICAL_SAMPLE_FILEPATH" >&2
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
done < "$CLINICAL_SAMPLE_FILEPATH" > "$UPDATED_CLINICAL_SAMPLE_FILEPATH"
