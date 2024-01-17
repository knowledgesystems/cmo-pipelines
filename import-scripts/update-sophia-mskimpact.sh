#!/usr/bin/env bash

source $PORTAL_HOME/scripts/automation-environment.sh
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

export SUBSET_FILE="$1"
export SOPHIA_REPO_NAME="sophia-data"
export SOPHIA_DATA_HOME="$PORTAL_DATA_HOME/$SOPHIA_REPO_NAME"
export SOPHIA_MSKIMPACT_STABLE_ID="sophia_mskimpact"
export SOPHIA_MSK_IMPACT_DATA_HOME="$SOPHIA_DATA_HOME/$SOPHIA_MSKIMPACT_STABLE_ID"
export SOPHIA_TMPDIR=$SOPHIA_DATA_HOME/tmp

# Patient and sample attributes that we want to deliver in our data
DELIVERED_PATIENT_ATTRIBUTES="PATIENT_ID PARTC_CONSENTED_12_245 RACE SEX ETHNICITY"
DELIVERED_SAMPLE_ATTRIBUTES="SAMPLE_ID PATIENT_ID CANCER_TYPE SAMPLE_TYPE SAMPLE_CLASS METASTATIC_SITE PRIMARY_SITE CANCER_TYPE_DETAILED GENE_PANEL SAMPLE_COVERAGE TUMOR_PURITY ONCOTREE_CODE MSI_SCORE MSI_TYPE SOMATIC_STATUS ARCHER CVR_TMB_COHORT_PERCENTILE CVR_TMB_SCORE CVR_TMB_TT_COHORT_PERCENTILE"

# Duplicate columns that we want to filter out of MAF files
MUTATIONS_EXTENDED_COLS_TO_FILTER="amino_acid_change,cdna_change,transcript,COMMENTS,Comments,comments,Matched_Norm_Sample_Barcode"
NONSIGNEDOUT_COLS_TO_FILTER="amino_acid_change,cdna_change,transcript,Comments,comments,Matched_Norm_Sample_Barcode"

# Stores an array of clinical attributes found in the data + attributes we want to filter, respectively
unset clinical_attributes_in_file
declare -gA clinical_attributes_in_file=() 
unset clinical_attributes_to_filter_arg
declare -g clinical_attributes_to_filter_arg="unset"

function report_error() {
    # Error message provided as an argument
    error_message="$1"
    echo -e "$error_message"
    exit 1
}

function filter_files_in_delivery_directory() {
    unset filenames_to_deliver
    declare -A filenames_to_deliver

    # Data files to deliver
    filenames_to_deliver[data_clinical_patient.txt]+=1
    filenames_to_deliver[data_clinical_sample.txt]+=1
    filenames_to_deliver[data_CNA.txt]+=1
    filenames_to_deliver[data_gene_matrix.txt]+=1
    filenames_to_deliver[data_mutations_extended.txt]+=1
    filenames_to_deliver[data_mutations_non_signedout.txt]+=1
    filenames_to_deliver[data_sv.txt]+=1
    filenames_to_deliver[sophia_mskimpact_data_cna_hg19.seg]+=1

    # Remove any files/directories that are not specified above
    for filepath in $SOPHIA_MSK_IMPACT_DATA_HOME/* ; do
        filename=$(basename $filepath)
        if [ -z ${filenames_to_deliver[$filename]} ] ; then
            if ! rm -rf $filepath ; then
                return 1
            fi
        fi
    done
    return 0
}

function rename_files_in_delivery_directory() {
    # We want to rename:
    # az_mskimpact_data_cna_hg19.seg -> sophia_mskimpact_data_cna_hg19.seg
    # data_nonsignedout_mutations.txt -> data_mutations_non_signedout.txt

    unset filenames_to_rename
    declare -A filenames_to_rename

    # Data files to rename
    filenames_to_rename[az_mskimpact_data_cna_hg19.seg]=sophia_mskimpact_data_cna_hg19.seg
    filenames_to_rename[data_nonsignedout_mutations.txt]=data_mutations_non_signedout.txt

    for original_filename in "${!filenames_to_rename[@]}"
    do
        if [ -f "$SOPHIA_MSK_IMPACT_DATA_HOME/$original_filename" ]; then
            if ! mv "$SOPHIA_MSK_IMPACT_DATA_HOME/$original_filename" "$SOPHIA_MSK_IMPACT_DATA_HOME/${filenames_to_rename[$original_filename]}" ; then
                return 1
            fi
        fi
    done
    return 0
}

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
    clinical_attribute_filepath=$1

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
    clinical_attribute_filepath=$1

    # Must be either "patient" or "sample"
    clinical_attribute_filetype=$2

    declare -A clinical_attributes_to_filter=()
    if ! find_clinical_attributes_in_file "$clinical_attribute_filepath" ; then
        return 1
    fi

    # Populate delivered attributes for given file type
    unset delivered_attributes
    declare -A delivered_attributes=()
    case $clinical_attribute_filetype in
        patient)
            for attribute in $DELIVERED_PATIENT_ATTRIBUTES ; do
                delivered_attributes[$attribute]+=1
            done
            ;;
        sample)
            for attribute in $DELIVERED_SAMPLE_ATTRIBUTES ; do
                delivered_attributes[$attribute]+=1
            done
            ;;
        *) 
            echo "error: illegal filetype passed to find_clinical_attributes_to_filter() : $clinical_attribute_filetype" >&2
            return 1
            ;;
    esac

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
    # Determine which columns to exclude in the patient file
    PATIENT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.filtered"
    find_clinical_attributes_to_filter_arg "$PATIENT_INPUT_FILEPATH" patient
    PATIENT_EXCLUDED_HEADER_FIELD_LIST="$clinical_attributes_to_filter_arg"

    # Determine which columns to exclude in the sample file
    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    find_clinical_attributes_to_filter_arg "$SAMPLE_INPUT_FILEPATH" sample
    SAMPLE_EXCLUDED_HEADER_FIELD_LIST="$clinical_attributes_to_filter_arg"
    
    # Filter out the columns we want to exclude in both files
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$PATIENT_INPUT_FILEPATH" -e "$PATIENT_EXCLUDED_HEADER_FIELD_LIST" > "$PATIENT_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$SAMPLE_INPUT_FILEPATH" -e "$SAMPLE_EXCLUDED_HEADER_FIELD_LIST" > "$SAMPLE_OUTPUT_FILEPATH" &&
    
    # Rewrite the patient and sample files with updated data
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH" &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
}

function add_seq_date_to_sample_file() {
    SEQ_DATE_FILENAME="cvr/seq_date.txt"
    MSK_ACCESS_SEQ_DATE="$MSK_ACCESS_DATA_HOME/$SEQ_DATE_FILENAME"
    MSK_HEMEPACT_SEQ_DATE="$MSK_HEMEPACT_DATA_HOME/$SEQ_DATE_FILENAME"
    MSK_IMPACT_SEQ_DATE="$MSK_IMPACT_DATA_HOME/$SEQ_DATE_FILENAME"
    MERGED_SEQ_DATE="./merged_seq_date.txt"

    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    KEY_COLUMNS="SAMPLE_ID PATIENT_ID"

    $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$MSK_ACCESS_SEQ_DATE" "$MSK_HEMEPACT_SEQ_DATE" "$MSK_IMPACT_SEQ_DATE" -o "$MERGED_SEQ_DATE" -m outer &&
	$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$SAMPLE_INPUT_FILEPATH" "$MERGED_SEQ_DATE" -o "$SAMPLE_OUTPUT_FILEPATH" -c "$KEY_COLUMNS" -m left &&

    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"

    # Metadata headers will be added back in a later function call
}

function filter_replicated_maf_columns() {
    MUTATIONS_EXTENDED_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MUTATIONS_EXTENDED_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.filtered"
    MUTATIONS_NONSIGNEDOUT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt"
    MUTATIONS_NONSIGNEDOUT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt.filtered"

    # Filter out the columns we want to exclude in both files
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$MUTATIONS_EXTENDED_INPUT_FILEPATH" -e "$MUTATIONS_EXTENDED_COLS_TO_FILTER" > "$MUTATIONS_EXTENDED_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$MUTATIONS_NONSIGNEDOUT_INPUT_FILEPATH" -e "$NONSIGNEDOUT_COLS_TO_FILTER" > "$MUTATIONS_NONSIGNEDOUT_OUTPUT_FILEPATH" &&

    # Rewrite the MAF files
    mv "$MUTATIONS_EXTENDED_OUTPUT_FILEPATH" "$MUTATIONS_EXTENDED_INPUT_FILEPATH" &&
    mv "$MUTATIONS_NONSIGNEDOUT_OUTPUT_FILEPATH" "$MUTATIONS_NONSIGNEDOUT_INPUT_FILEPATH"
}

function remove_duplicate_maf_variants() {
    MUTATIONS_EXTD_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    NSOUT_MUTATIONS_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"

    MUTATIONS_EXTD_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended_merged.txt"
    NSOUT_MUTATIONS_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations_merged.txt"

    # Remove duplicate variants from MAF files
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$NSOUT_MUTATIONS_INPUT_FILEPATH" &&

    # Rewrite mutation files with updated data
    mv "$MUTATIONS_EXTD_OUTPUT_FILEPATH" "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    mv "$NSOUT_MUTATIONS_OUTPUT_FILEPATH" "$NSOUT_MUTATIONS_INPUT_FILEPATH"
}

function add_metadata_headers() {
    # Calling merge.py strips out metadata headers from our clinical files - add them back in
    CDD_URL="http://cdd.cbioportal.mskcc.org/api/"
    INPUT_FILENAMES="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt $SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact
}

function transpose_cna_data() {
    # Transpose the CNA file so that sample IDs are contained in the first column instead of the header
    DATA_CNA_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_CNA.txt"
    $PYTHON3_BINARY transpose_cna.py "$DATA_CNA_INPUT_FILEPATH"
}

function remove_sequenced_samples_header() {
    MAF_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MAF_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.tmp"

    # This removes the sequenced_samples header from the MAF
    awk '!/^#sequenced_samples:/' "$MAF_INPUT_FILEPATH" > "$MAF_OUTPUT_FILEPATH" && mv "$MAF_OUTPUT_FILEPATH" "$MAF_INPUT_FILEPATH"
}

# Create temporary directory to store data before subsetting
if ! [ -d "$SOPHIA_TMPDIR" ] ; then
    if ! mkdir -p "$SOPHIA_TMPDIR" ; then
        report_error "ERROR: Failed to create temporary directory for Sophia MSK-IMPACT. Exiting."
    fi
fi
if [[ -d "$SOPHIA_TMPDIR" && "$SOPHIA_TMPDIR" != "/" ]] ; then
    rm -rf "$SOPHIA_TMPDIR"/*
fi

# Copy data from local clone of AZ repo to Sophia directory
cp -a $AZ_MSK_IMPACT_DATA_HOME/* $SOPHIA_TMPDIR
cp -aR $AZ_DATA_HOME/gene_panels $SOPHIA_DATA_HOME
cp -a README.pdf $SOPHIA_DATA_HOME

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to populate temporary directory for Sophia MSK-IMPACT. Exiting."
fi

# Post-process the dataset
printTimeStampedDataProcessingStepMessage "Subset Sophia MSK-IMPACT"

# Write out the subsetted data
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
    --study-id="az_mskimpact" \
    --subset="$SUBSET_FILE" \
    --output-directory="$SOPHIA_MSK_IMPACT_DATA_HOME" \
    --merge-clinical="true" \
    $SOPHIA_TMPDIR

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to write out subsetted data for Sophia MSK-IMPACT. Exiting."
fi

# Rename files that need to be renamed
if ! rename_files_in_delivery_directory ; then
    report_error "ERROR: Failed to rename files for Sophia MSK-IMPACT. Exiting."
fi

# Transpose CNA file
if ! transpose_cna_data ; then
    report_error "ERROR: Failed to transpose CNA file for Sophia MSK-IMPACT. Exiting."
fi

# Remove sequenced_samples header from MAF
if ! remove_sequenced_samples_header ; then
    report_error "ERROR: Failed to remove sequenced_samples header from MAF for Sophia MSK-IMPACT. Exiting."
fi

# Filter clinical attribute columns from clinical files
if ! filter_clinical_attribute_columns ; then
    report_error "ERROR: Failed to filter non-delivered clinical attribute columns for Sophia MSK-IMPACT. Exiting."
fi

# Filter replicated columns from MAF files
if ! filter_replicated_maf_columns ; then
    report_error "ERROR: Failed to filter duplicated columns in MAF files for Sophia MSK-IMPACT. Exiting."
fi

# Remove duplicate variants from MAF files
if ! remove_duplicate_maf_variants ; then
    report_error "ERROR: Failed to remove duplicate variants from MAF files for Sophia MSK-IMPACT. Exiting."
fi

# Add metadata headers to clinical files
if ! add_metadata_headers ; then
    report_error "ERROR: Failed to add metadata headers to clinical attribute files for Sophia MSK-IMPACT. Exiting."
fi

# Filter out files which are not delivered
if ! filter_files_in_delivery_directory ; then
    report_error "ERROR: Failed to filter non-delivered files for Sophia MSK-IMPACT. Exiting."
fi

# Remove temporary directory now that the subset has been merged and post-processed
if [[ -d "$SOPHIA_TMPDIR" && "$SOPHIA_TMPDIR" != "/" ]] ; then
    rm -rf "$SOPHIA_TMPDIR"
fi
