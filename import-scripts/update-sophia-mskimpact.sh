#!/usr/bin/env bash

# File containing list of patients should be passed in as argument
export SUBSET_FILE="$1"
export COHORT_NAME="$2"
export CURRENT_DATE="$(date '+%m.%d.%y')"

export SOPHIA_MSK_IMPACT_DATA_HOME="$SOPHIA_DATA_HOME/$COHORT_NAME"
export SOPHIA_TMPDIR="$SOPHIA_MSK_IMPACT_DATA_HOME/tmp"

# Patient and sample attributes that we want to deliver in our data
DELIVERED_PATIENT_ATTRIBUTES="PATIENT_ID PARTC_CONSENTED_12_245 RACE SEX ETHNICITY"
DELIVERED_SAMPLE_ATTRIBUTES="SAMPLE_ID PATIENT_ID CANCER_TYPE SAMPLE_TYPE SAMPLE_CLASS METASTATIC_SITE PRIMARY_SITE CANCER_TYPE_DETAILED GENE_PANEL SAMPLE_COVERAGE TUMOR_PURITY ONCOTREE_CODE MSI_SCORE MSI_TYPE SOMATIC_STATUS ARCHER CVR_TMB_COHORT_PERCENTILE CVR_TMB_SCORE CVR_TMB_TT_COHORT_PERCENTILE SEQ_DATE"

# Duplicate columns that we want to filter out of MAF files
MUTATIONS_EXTENDED_COLS_TO_FILTER="amino_acid_change,cdna_change,transcript,COMMENTS,Comments,comments,Matched_Norm_Sample_Barcode"
NONSIGNEDOUT_COLS_TO_FILTER="amino_acid_change,cdna_change,transcript,Comments,comments,Matched_Norm_Sample_Barcode"

# Stores an array of clinical attributes found in the data + attributes we want to filter, respectively
unset clinical_attributes_in_file
declare -gA clinical_attributes_in_file=() 
unset clinical_attributes_to_filter_arg
declare -g clinical_attributes_to_filter_arg="unset"

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
source $PORTAL_HOME/scripts/filter-clinical-arg-functions.sh

function report_error() {
    # Error message provided as an argument
    error_message="$1"
    echo -e "$error_message"
    exit 1
}

function pull_latest_data_from_sophia_git_repo() {
    (   # Executed in a subshell to avoid changing the actual working directory
        # If any statement fails, the return value of the entire expression is the failure status
        cd $SOPHIA_DATA_HOME &&
        $GIT_BINARY fetch &&
        $GIT_BINARY reset origin/main --hard &&
        $GIT_BINARY lfs pull &&
        $GIT_BINARY clean -f -d
    )
}

function push_updates_to_sophia_git_repo() {
    (   # Executed in a subshell to avoid changing the actual working directory
        # If any statement fails, the return value of the entire expression is the failure status
        cd $SOPHIA_DATA_HOME &&
        $GIT_BINARY add * &&
        $GIT_BINARY commit -m "$COHORT_NAME data $CURRENT_DATE" &&
        $GIT_BINARY push origin
    )
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
    filenames_to_deliver[DMP_IDs.txt]+=1

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

function add_seq_date_to_sample_file() {
    SEQ_DATE_FILENAME="cvr/seq_date.txt"
    MSK_ACCESS_SEQ_DATE="$MSK_ACCESS_DATA_HOME/$SEQ_DATE_FILENAME"
    MSK_HEMEPACT_SEQ_DATE="$MSK_HEMEPACT_DATA_HOME/$SEQ_DATE_FILENAME"
    MSK_IMPACT_SEQ_DATE="$MSK_IMPACT_DATA_HOME/$SEQ_DATE_FILENAME"
    MERGED_SEQ_DATE="$SOPHIA_MSK_IMPACT_DATA_HOME/merged_seq_date.txt"

    MERGED_SEQ_DATE_FORMATTED="$SOPHIA_MSK_IMPACT_DATA_HOME/merged_seq_date_formatted.txt"
    DATE_COLUMN="SEQ_DATE"

    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    KEY_COLUMNS="SAMPLE_ID PATIENT_ID"

    # Merge all 3 seq date files
    $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$MSK_ACCESS_SEQ_DATE" "$MSK_HEMEPACT_SEQ_DATE" "$MSK_IMPACT_SEQ_DATE" -o "$MERGED_SEQ_DATE" -m outer &&
	# Convert SEQ_DATE format to YYYY-MM-DD
    $PYTHON3_BINARY $PORTAL_HOME/scripts/convert_date_col_format_py3.py -i "$MERGED_SEQ_DATE" -o "$MERGED_SEQ_DATE_FORMATTED" -c "$DATE_COLUMN" &&
    # Add SEQ_DATE to sample file
    $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$SAMPLE_INPUT_FILEPATH" "$MERGED_SEQ_DATE_FORMATTED" -o "$SAMPLE_OUTPUT_FILEPATH" -c $KEY_COLUMNS -m left &&

    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"

    # Metadata headers will be added back in a later function call
}

function filter_clinical_cols() {
    PATIENT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.filtered"
    filter_clinical_attribute_columns "$PATIENT_INPUT_FILEPATH" "$DELIVERED_PATIENT_ATTRIBUTES" "$PATIENT_OUTPUT_FILEPATH"

    # Determine which columns to exclude in the sample file
    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    filter_clinical_attribute_columns "$SAMPLE_INPUT_FILEPATH" "$DELIVERED_SAMPLE_ATTRIBUTES" "$SAMPLE_OUTPUT_FILEPATH"
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
    NSOUT_MUTATIONS_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt"

    MUTATIONS_EXTD_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended_merged.txt"
    NSOUT_MUTATIONS_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout_merged.txt"

    # Remove duplicate variants from MAF files
    # CVR data can contain duplicates for a gene and its alias
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$MUTATIONS_EXTD_INPUT_FILEPATH" -o "$MUTATIONS_EXTD_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$NSOUT_MUTATIONS_INPUT_FILEPATH" -o "$NSOUT_MUTATIONS_OUTPUT_FILEPATH" &&

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
    $PYTHON3_BINARY $PORTAL_HOME/scripts/transpose_cna_py3.py "$DATA_CNA_INPUT_FILEPATH"
}

function remove_sequenced_samples_header() {
    MAF_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MAF_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.tmp"

    # This removes the sequenced_samples header from the MAF
    awk '!/^#sequenced_samples:/' "$MAF_INPUT_FILEPATH" > "$MAF_OUTPUT_FILEPATH" && mv "$MAF_OUTPUT_FILEPATH" "$MAF_INPUT_FILEPATH"
}

# Pull latest from Sophia repo (sophia-data)
printTimeStampedDataProcessingStepMessage "Pull of Sophia MSK-IMPACT data updates"
if ! pull_latest_data_from_sophia_git_repo ; then
    report_error "ERROR: Failed git pull for Sophia MSK-IMPACT. Exiting."
fi

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

# Add SEQ_DATE to clinical sample file
if ! add_seq_date_to_sample_file; then
    report_error "ERROR: Failed to add SEQ_DATE column to clinical sample file for Sophia MSK-IMPACT. Exiting."
fi

# Filter clinical attribute columns from clinical files
if ! filter_clinical_cols ; then
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

# Zip data files for easier data transfer
zip "$SOPHIA_MSK_IMPACT_DATA_HOME/sophia-$COHORT_NAME-data-$CURRENT_DATE.zip" $SOPHIA_MSK_IMPACT_DATA_HOME/*data*

# Push updated data to GitHub
if ! push_updates_to_sophia_git_repo ; then
    report_error "ERROR: Failed git push for AstraZeneca MSK-IMPACT. Exiting."
fi

printTimeStampedDataProcessingStepMessage "Cleaning up untracked files from Sophia repo"
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $SOPHIA_DATA_HOME