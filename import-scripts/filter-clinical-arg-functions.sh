unset clinical_attributes_in_file
declare -gA clinical_attributes_in_file=()
unset clinical_attributes_to_filter_arg
declare -g clinical_attributes_to_filter_arg="unset"

function find_clinical_attribute_header_line_from_file() {
    # Path to clinical file taken as an argument
    clinical_attribute_filepath="$1"

    # Results are stored in this variable
    declare -g clinical_attribute_header_line="unset"

    # Error if clinical file cannot be read
    if ! [ -r "$clinical_attribute_filepath" ] ; then
        echo "error: cannot read file $clinical_attribute_filepath" >&2
        return 1
    fi

    # Search file for header line
    while read -r line ; do
        if [ ${#line} -eq 0 ] ; then
            echo "error: first uncommented line in $clinical_attribute_filepath was empty" >&2
            return 1
        fi
        if ! [ ${line:0:1} == "#" ] ; then
            clinical_attribute_header_line=$line
            break
        fi
    done < "$clinical_attribute_filepath"
    if [ "$clinical_attribute_header_line" == "unset" ] ; then
        echo "error: unable to find header line in $clinical_attribute_filepath" >&2
        return 1
    fi
}

function find_clinical_attributes_in_file() {
    # Path to clinical file taken as an argument
    clinical_attribute_filepath="$1"

    # Results (array of clinical attributes) are stored in this global array
    clinical_attributes_in_file=() 
    if ! find_clinical_attribute_header_line_from_file "$clinical_attribute_filepath" ; then
        return 1
    fi
    for attribute in $clinical_attribute_header_line ; do
        clinical_attributes_in_file[$attribute]+=1
    done
}

function find_clinical_attributes_to_filter_arg() {
    # Path to clinical file taken as an argument
    clinical_attribute_filepath="$1"

    # Attributes to deliver in the clinical file
    attributes_to_deliver="$2"

    declare -A clinical_attributes_to_filter=()
    if ! find_clinical_attributes_in_file "$clinical_attribute_filepath" ; then
        return 1
    fi

    # Populate delivered attributes for given file type
    unset delivered_attributes
    declare -A delivered_attributes=()
    for attribute in $attributes_to_deliver ; do
        delivered_attributes[$attribute]+=1
    done

    # Determine which clinical attributes we need to filter based on the attributes found in the file
    for attribute in ${!clinical_attributes_in_file[@]} ; do
        if [ -z ${delivered_attributes[$attribute]} ] ; then
            clinical_attributes_to_filter[$attribute]+=1
        fi
    done

    # Put the list attributes we want to filter in a comma separated string
    clinical_attributes_to_filter_arg=""
    list_size=0
    for attribute in ${!clinical_attributes_to_filter[@]} ; do
        clinical_attributes_to_filter_arg="$clinical_attributes_to_filter_arg$attribute"
        list_size=$(($list_size+1))
        if [ "$list_size" -lt ${#clinical_attributes_to_filter[@]} ] ; then
            clinical_attributes_to_filter_arg="$clinical_attributes_to_filter_arg,"
        fi
    done
}

function filter_clinical_attribute_columns() {
    # Path to clinical file taken as an argument
    clinical_filepath="$1"

    # Attributes to deliver in the clinical file
    attributes_to_deliver="$2"

    # Temporary output file, removed at end of function
    tmp_processing_file=$(mktemp -q)

    # Determine which columns to exclude in the patient file
    if ! find_clinical_attributes_to_filter_arg "$clinical_filepath" "$attributes_to_deliver" ; then
        return 1
    fi
    excluded_header_field_list="$clinical_attributes_to_filter_arg"

    # Filter out the columns we want to exclude
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$clinical_filepath" -e "$excluded_header_field_list" > "$tmp_processing_file"
    if [ $? -gt 0 ] ; then
        echo "Failed to filter columns from clinical file $clinical_filepath, see output at $tmp_processing_file"
        return 1
    fi
    mv "$tmp_processing_file" "$clinical_filepath"
}

function filter_cdm_clinical_elements() {
    # Removes CDM elements from a given study
    # Path to study directory
    study_dir="$1"
    patient_input_filepath="$study_dir/data_clinical_patient.txt"
    sample_input_filepath="$study_dir/data_clinical_sample.txt"

    # Clinical elements to maintain
    patient_attributes="PATIENT_ID GENDER RACE ETHNICITY CURRENT_AGE_DEID OS_MONTHS OS_STATUS OTHER_PATIENT_ID PARTA_CONSENTED_12_245 PARTC_CONSENTED_12_245"
    sample_attributes="SAMPLE_ID PATIENT_ID MONTH_ADDED WEEK_ADDED CANCER_TYPE SAMPLE_TYPE SAMPLE_CLASS METASTATIC_SITE PRIMARY_SITE CANCER_TYPE_DETAILED GENE_PANEL SO_COMMENTS SAMPLE_COVERAGE TUMOR_PURITY ONCOTREE_CODE MSI_COMMENT MSI_SCORE MSI_TYPE INSTITUTE SOMATIC_STATUS ARCHER CVR_TMB_COHORT_PERCENTILE CVR_TMB_SCORE CVR_TMB_TT_COHORT_PERCENTILE PATH_SLIDE_EXISTS MSK_SLIDE_ID CYCLE_THRESHOLD"

    # Filter columns from patient file
    if ! filter_clinical_attribute_columns "$patient_input_filepath" "$patient_attributes" ; then
        echo "Failed to filter clinical patient attributes"
        return 1
    fi

    # Filter columns from sample file
    if ! filter_clinical_attribute_columns "$sample_input_filepath" "$sample_attributes" ; then
        echo "Failed to filter clinical sample attributes"
        return 1
    fi
}
