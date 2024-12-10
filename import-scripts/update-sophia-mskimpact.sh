#!/usr/bin/env bash

# File containing list of patients should be passed in as argument
export SUBSET_FILE="$1"
export COHORT_NAME="$2"
export CURRENT_DATE="$(date '+%m.%d.%y')"
export CDD_URL="https://cdd.cbioportal.mskcc.org/api/"

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
    echo -e "ERROR: $error_message for Sophia MSK-IMPACT. Exiting."
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

function setup_data_directories() {
    # Create temporary directory to store data before subsetting
    if ! [ -d "$SOPHIA_TMPDIR" ] ; then
        if ! mkdir -p "$SOPHIA_TMPDIR" ; then
            echo "Failed to create temporary directory"
            return 1
        fi
    fi

    # If tmp dir exists and is not empty, remove the contents
    if [[ -d "$SOPHIA_TMPDIR" && "$SOPHIA_TMPDIR" != "/" ]] ; then
        rm -rf "$SOPHIA_TMPDIR"/*
        if [ $? -gt 0 ] ; then
            echo "Failed to remove contents of temporary directory"
            return 1
        fi
    fi
}

function merge_solid_heme_and_archer() {
    # Merge solid heme and archer to use as source dataset
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
        --study-id="mskimpact" \
        --output-directory="$SOPHIA_TMPDIR" \
        --merge-clinical="true" \
        $MSK_SOLID_HEME_DATA_HOME $MSK_ARCHER_UNFILTERED_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "Failed to merge MSK_SOLID_HEME and MSKARCHER"
        return 1
    fi

    # Add clinical attribute headers
    INPUT_FILENAMES="$SOPHIA_TMPDIR/data_clinical_sample.txt $SOPHIA_TMPDIR/data_clinical_patient.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact
    if [ $? -gt 0 ] ; then
        echo "Failed to add clinical attribute metadata headers to MSK_SOLID_HEME and MSKARCHER merged dataset"
        return 1
    fi

    # Copy over metadata files
    cp $MSK_SOLID_HEME_DATA_HOME/meta_* $SOPHIA_TMPDIR
    if [ $? -gt 0 ] ; then
        echo "Failed to copy metadata files to MSK_SOLID_HEME and MSKARCHER merged dataset"
        return 1
    fi
}

function subset_consented_cohort_patients() {
    CONSENTED_SUBSET_FILE=$(mktemp -q)
    CONSENTED_COHORT_SUBSET_FILE=$(mktemp -q)

    # Generate subset of Part A consented patients from merged solid heme and archer
    $PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py \
        --study-id="mskimpact" \
        --clinical-file="$SOPHIA_TMPDIR/data_clinical_patient.txt" \
        --filter-criteria="PARTA_CONSENTED_12_245=YES" \
        --subset-filename="$CONSENTED_SUBSET_FILE"
    if [ $? -gt 0 ] ; then
        echo "Failed to generate list of consented patients"
        return 1
    fi

    # Get intersection of consented patients and patients in current cohort
    comm -12 <(sort $SUBSET_FILE) <(sort $CONSENTED_SUBSET_FILE) > $CONSENTED_COHORT_SUBSET_FILE
    if [ $? -gt 0 ] ; then
        echo "Failed to generate list of consented patients in $COHORT_NAME cohort"
        return 1
    fi

    # Write out the subsetted data
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
        --study-id="mskimpact" \
        --subset="$CONSENTED_COHORT_SUBSET_FILE" \
        --output-directory="$SOPHIA_MSK_IMPACT_DATA_HOME" \
        --merge-clinical="true" \
        $SOPHIA_TMPDIR
    if [ $? -gt 0 ] ; then
        echo "Failed to subset on list of consented patients"
        return 1
    fi
}

function generate_cohort() {
    merge_solid_heme_and_archer &&
    subset_consented_cohort_patients
}

function rename_files_in_delivery_directory() {
    # We want to rename:
    # mskimpact_data_cna_hg19.seg -> sophia_mskimpact_data_cna_hg19.seg
    # data_nonsignedout_mutations.txt -> data_mutations_non_signedout.txt

    unset filenames_to_rename
    declare -A filenames_to_rename

    # Data files to rename
    filenames_to_rename[mskimpact_data_cna_hg19.seg]=sophia_mskimpact_data_cna_hg19.seg
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

function filter_clinical_cols() {
    # Determine which columns to exclude in the patient file
    PATIENT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.filtered"
    if ! filter_clinical_attribute_columns "$PATIENT_INPUT_FILEPATH" "$DELIVERED_PATIENT_ATTRIBUTES" "$PATIENT_OUTPUT_FILEPATH" ; then
        echo "Failed to filter clinical patient attributes"
        return 1
    fi

    # Determine which columns to exclude in the sample file
    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    if ! filter_clinical_attribute_columns "$SAMPLE_INPUT_FILEPATH" "$DELIVERED_SAMPLE_ATTRIBUTES" "$SAMPLE_OUTPUT_FILEPATH" ; then
        echo "Failed to filter clinical sample attributes"
        return 1
    fi
}

function rename_cdm_clinical_attribute_columns() {
    # Rename clinical patient attributes coming from CDM:
    # CURRENT_AGE_DEID -> AGE_CURRENT
    # GENDER -> SEX

    PATIENT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.renamed"

    sed -e '1s/CURRENT_AGE_DEID/AGE_CURRENT/' -e '1s/GENDER/SEX/' $PATIENT_INPUT_FILEPATH > $PATIENT_OUTPUT_FILEPATH &&
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH"
}

function standardize_clinical_data() {
    PATIENT_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.standardized"
    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.standardized"

    # Standardize the clinical files to use NA for blank values
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_clinical_data.py -f "$PATIENT_INPUT_FILEPATH" > "$PATIENT_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_clinical_data.py -f "$SAMPLE_INPUT_FILEPATH" > "$SAMPLE_OUTPUT_FILEPATH" &&

    # Rewrite the patient and sample files with updated data
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH" &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
}

function add_seq_date_to_sample_file() {
    DATABRICKS_SERVER_HOSTNAME=`grep server_hostname $DATABRICKS_CREDS_FILE | sed 's/^.*=//g'`
    DATABRICKS_HTTP_PATH=`grep http_path $DATABRICKS_CREDS_FILE | sed 's/^.*=//g'`
    DATABRICKS_TOKEN=`grep access_token $DATABRICKS_CREDS_FILE | sed 's/^.*=//g'`
    DATABRICKS_SEQ_DATE_FILEPATH="/Volumes/cdsi_prod/cdm_impact_pipeline_prod/cdm-data/cbioportal/seq_date.txt"
    SEQ_DATE_FILEPATH="$CDSI_DATA_HOME/seq_date.txt"

    SAMPLE_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.with_seq_date"
    KEY_COLUMNS="SAMPLE_ID PATIENT_ID"

    # Download seq_date.txt file from DataBricks
    $PYTHON3_BINARY $PORTAL_HOME/scripts/databricks_query_py3.py \
        --hostname $DATABRICKS_SERVER_HOSTNAME \
        --http-path $DATABRICKS_HTTP_PATH \
        --access-token $DATABRICKS_TOKEN \
        --mode get \
        --input-file $DATABRICKS_SEQ_DATE_FILEPATH \
        --output-file $SEQ_DATE_FILEPATH
    if [ $? -gt 0 ] ; then
        echo "Failed to query DataBricks for SEQ_DATE file"
        return 1
    fi

    # Add SEQ_DATE column to sample file and overwrite the previous sample file
    $PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$SAMPLE_INPUT_FILEPATH" "$SEQ_DATE_FILEPATH" -o "$SAMPLE_OUTPUT_FILEPATH" -c $KEY_COLUMNS -m left &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
    if [ $? -gt 0 ] ; then
        echo "Failed to merge $SAMPLE_INPUT_FILEPATH and $SEQ_DATE_FILEPATH"
        return 1
    fi
}

function add_metadata_headers() {
    # Calling merge.py strips out metadata headers from our clinical files - add them back in
    INPUT_FILENAMES="$SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt $SOPHIA_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact
}

function standardize_cna_data() {
    # Standardize the CNA file to use NA for blank values
    DATA_CNA_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_CNA.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_cna_data.py -f "$DATA_CNA_INPUT_FILEPATH"
}

function transpose_cna_data() {
    # Transpose the CNA file so that sample IDs are contained in the first column instead of the header
    DATA_CNA_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_CNA.txt"
    $PYTHON3_BINARY $PORTAL_HOME/scripts/transpose_cna_py3.py "$DATA_CNA_INPUT_FILEPATH"
}

function standardize_mutations_data() {
    MUTATIONS_EXTD_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    NSOUT_MUTATIONS_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt"

    # Standardize the mutations files to check for valid values in the 'NCBI_Build' column
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_mutations_data.py -f "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_mutations_data.py -f "$NSOUT_MUTATIONS_INPUT_FILEPATH"
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

function filter_germline_events_from_maf() {
    MUTATION_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MUTATION_FILTERED_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.filtered"
    $PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $MUTATION_FILEPATH $MUTATION_FILTERED_FILEPATH --event-type mutation
    if [ $? -gt 0 ] ; then
        echo "Failed to filter germline events from mutation file"
        return 1
    fi
    mv $MUTATION_FILTERED_FILEPATH $MUTATION_FILEPATH

    MUTATION_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt"
    MUTATION_FILTERED_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_non_signedout.txt.filtered"
    $PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $MUTATION_FILEPATH $MUTATION_FILTERED_FILEPATH --event-type mutation
    if [ $? -gt 0 ] ; then
        echo "Failed to filter germline events from nonsignedout mutation file"
        return 1
    fi
    mv $MUTATION_FILTERED_FILEPATH $MUTATION_FILEPATH
}

function remove_sequenced_samples_header() {
    MAF_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MAF_OUTPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.tmp"

    # This removes the sequenced_samples header from the MAF
    awk '!/^#sequenced_samples:/' "$MAF_INPUT_FILEPATH" > "$MAF_OUTPUT_FILEPATH" && mv "$MAF_OUTPUT_FILEPATH" "$MAF_INPUT_FILEPATH"
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

function standardize_structural_variant_data() {
    DATA_SV_INPUT_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_sv.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_structural_variant_data.py -f "$DATA_SV_INPUT_FILEPATH"
}

function filter_germline_events_from_sv() {
    SV_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_sv.txt"
    SV_FILTERED_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_sv.txt.filtered"
    $PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $SV_FILEPATH $SV_FILTERED_FILEPATH --event-type structural_variant
    if [ $? -gt 0 ] ; then
        echo "Failed to filter germline events from structural variant file"
        return 1
    fi
    mv $SV_FILTERED_FILEPATH $SV_FILEPATH
}

function remove_duplicate_archer_events_from_sv() {
    SV_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_sv.txt"
    SV_FILTERED_FILEPATH="$SOPHIA_MSK_IMPACT_DATA_HOME/data_sv.txt.filtered"

    # Remove all lines that contain the string ' - Archer' in the 'Event_Info' column
    event_info_index=$(awk -F '\t' -v col='Event_Info' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $SV_FILEPATH)
    awk -F'\t' "\$$event_info_index !~ /-\sArcher$/" $SV_FILEPATH > $SV_FILTERED_FILEPATH && mv $SV_FILTERED_FILEPATH $SV_FILEPATH
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

function push_updates_to_sophia_git_repo() {
    (   # Executed in a subshell to avoid changing the actual working directory
        # If any statement fails, the return value of the entire expression is the failure status
        cd $SOPHIA_DATA_HOME &&
        $GIT_BINARY add * &&
        $GIT_BINARY commit -m "$COHORT_NAME data $CURRENT_DATE" &&
        $GIT_BINARY push origin
    )
}

function zip_files_for_delivery() {
    # Zip data files for easier data transfer
    zip "$SOPHIA_MSK_IMPACT_DATA_HOME/sophia-$COHORT_NAME-data-$CURRENT_DATE.zip" $SOPHIA_MSK_IMPACT_DATA_HOME/*data*
}

function cleanup_repo() {
    # Remove temporary directory now that the subset has been merged and post-processed
    if [[ -d "$SOPHIA_TMPDIR" && "$SOPHIA_TMPDIR" != "/" ]] ; then
        rm -rf "$SOPHIA_TMPDIR"
    fi

    # Clean up untracked files and LFS objects
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $SOPHIA_DATA_HOME
}

printTimeStampedDataProcessingStepMessage "Subset Sophia MSK-IMPACT"

# Pull latest from Sophia repo (sophia-data)
if ! pull_latest_data_from_sophia_git_repo ; then
    report_error "ERROR: Failed git pull"
fi

# Create temporary directories
if ! setup_data_directories ; then 
    report_error "Failed to set up data directories"
fi

# Merge solid heme + archer, subset consented patients from cohort
if ! generate_cohort ; then
    report_error "Failed to generate data"
fi

# Rename files that need to be renamed
if ! rename_files_in_delivery_directory ; then
    report_error "Failed to rename files"
fi

printTimeStampedDataProcessingStepMessage "Post-process clinical files for Sophia MSK-IMPACT"

# Filter clinical attribute columns from clinical files
if ! filter_clinical_cols ; then
    report_error "Failed to filter non-delivered clinical attribute columns for Sophia MSK-IMPACT"
fi

# Rename columns coming from CDM
if ! rename_cdm_clinical_attribute_columns ; then
    report_error "Failed to rename CDM clinical attribute columns"
fi

# Standardize blank clinical data values to NA
if ! standardize_clinical_data ; then
    report_error "ERROR: Failed to standardize blank clinical data values to NA"
fi

# Add SEQ_DATE to clinical sample file
if ! add_seq_date_to_sample_file; then
    report_error "ERROR: Failed to add SEQ_DATE column to clinical sample file"
fi

# Add metadata headers to clinical files
if ! add_metadata_headers ; then
    report_error "Failed to add metadata headers to clinical attribute files"
fi

printTimeStampedDataProcessingStepMessage "Post-process CNA file for Sophia MSK-IMPACT"

# Standardize blank CNA data values to NA
if ! standardize_cna_data ; then
    report_error "Failed to standardize blank CNA data values to NA"
fi

# Transpose CNA file
if ! transpose_cna_data ; then
    report_error "Failed to transpose CNA file"
fi

printTimeStampedDataProcessingStepMessage "Post-process MAF file for Sophia MSK-IMPACT"

# Standardize mutations files
if ! standardize_mutations_data ; then
    report_error "Failed to standardize mutations files"
fi

# Remove duplicate variants from MAF files
if ! remove_duplicate_maf_variants ; then
    report_error "Failed to remove duplicate variants from MAF files"
fi

# Filter germline events from mutation file and structural variant file
if ! filter_germline_events_from_maf ; then
    report_error "Failed to filter MAF germline events"
fi

# Remove sequenced_samples header from MAF
if ! remove_sequenced_samples_header ; then
    report_error "Failed to remove sequenced_samples header from MAF"
fi

# Filter replicated columns from MAF files
if ! filter_replicated_maf_columns ; then
    report_error "Failed to filter duplicated columns in MAF files"
fi

printTimeStampedDataProcessingStepMessage "Post-process SV file for Sophia MSK-IMPACT"

# Standardize structural variant data by removing records with invalid genes and standardizing the file header
if ! standardize_structural_variant_data ; then
    report_error "Failed to standardize structural variant data"
fi

# Filter germline events from mutation file and structural variant file
if ! filter_germline_events_from_sv ; then
    report_error "Failed to filter SV germline events"
fi

# Because we have already merged Archer samples, remove linked Archer events
if ! remove_duplicate_archer_events_from_sv ; then
    report_error "Failed to demove duplicate Archer events from SV file"
fi

printTimeStampedDataProcessingStepMessage "Finalize and validate study contents of Sophia MSK-IMPACT"

# Filter out files which are not delivered
if ! filter_files_in_delivery_directory ; then
    report_error "Failed to filter non-delivered files"
fi

printTimeStampedDataProcessingStepMessage "Push Sophia MSK-IMPACT to GitHub"

# Push updated data to GitHub
if ! push_updates_to_sophia_git_repo ; then
    report_error "Failed git push"
fi

# Compress files for easier download
if ! zip_files_for_delivery ; then
    report_error "Failed to compress study files"
fi

# Cleanup Sophia git repo
if ! cleanup_repo ; then
    report_error "Failed to cleanup git repository"
fi
