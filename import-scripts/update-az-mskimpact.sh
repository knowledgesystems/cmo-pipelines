#!/usr/bin/env bash

export AZ_REPO_NAME="az-data"
export AZ_DATA_HOME="$PORTAL_DATA_HOME/$AZ_REPO_NAME"
export AZ_MSKIMPACT_STABLE_ID="az_mskimpact"
export AZ_MSK_IMPACT_DATA_HOME="$AZ_DATA_HOME/$AZ_MSKIMPACT_STABLE_ID"
export AZ_TMPDIR=$AZ_DATA_HOME/tmp

# Patient and sample attributes that we want to deliver in our data
DELIVERED_PATIENT_ATTRIBUTES="PATIENT_ID PARTC_CONSENTED_12_245 CURRENT_AGE_DEID RACE GENDER ETHNICITY OS_STATUS OS_MONTHS"
DELIVERED_SAMPLE_ATTRIBUTES="SAMPLE_ID PATIENT_ID CANCER_TYPE SAMPLE_TYPE SAMPLE_CLASS METASTATIC_SITE PRIMARY_SITE CANCER_TYPE_DETAILED GENE_PANEL SO_COMMENTS SAMPLE_COVERAGE TUMOR_PURITY ONCOTREE_CODE MSI_COMMENT MSI_SCORE MSI_TYPE SOMATIC_STATUS ARCHER CVR_TMB_COHORT_PERCENTILE CVR_TMB_SCORE CVR_TMB_TT_COHORT_PERCENTILE"

# Stores an array of clinical attributes found in the data + attributes we want to filter, respectively
unset clinical_attributes_in_file
declare -gA clinical_attributes_in_file=() 
unset clinical_attributes_to_filter_arg
declare -g clinical_attributes_to_filter_arg="unset"

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

function report_error() {
    # Error message provided as an argument
    error_message="$1"

    # Send Slack message and email reporting the error
    sendPreImportFailureMessageMskPipelineLogsSlack "$error_message"
    echo -e "Sending email $error_message"
    echo -e "$error_message" |  mail -s "[URGENT] AstraZeneca data delivery failure" $PIPELINES_EMAIL_LIST

    # Reset the local git repo and exit
    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    exit 1
}

function pull_latest_data_from_az_git_repo() {
    (   # Executed in a subshell to avoid changing the actual working directory
        # If any statement fails, the return value of the entire expression is the failure status
        cd $AZ_DATA_HOME &&
        $GIT_BINARY fetch &&
        $GIT_BINARY reset origin/main --hard &&
        $GIT_BINARY lfs pull &&
        $GIT_BINARY clean -f -d
    )
}

function push_updates_to_az_git_repo() {
    (   # Executed in a subshell to avoid changing the actual working directory
        # If any statement fails, the return value of the entire expression is the failure status
        cd $AZ_DATA_HOME &&
        $GIT_BINARY add * &&
        $GIT_BINARY commit -m "Latest AstraZeneca MSK-IMPACT dataset" &&
        $GIT_BINARY push origin
    )
}

function transfer_to_az_sftp_server() {
    # Get server credentials
    TRANSFER_KEY="/home/cbioportal_importer/.ssh/id_rsa_astrazeneca_sftp"
    SFTP_USER=$(cat $AZ_SFTP_USER_FILE)
    SERVICE_ENDPOINT=$(cat $AZ_SERVICE_ENDPOINT_FILE)

    # Connect and transfer data
    # With use of here-doc, there must be no leading whitespace until EOF
    sftp -i "$TRANSFER_KEY" "$SFTP_USER"@"$SERVICE_ENDPOINT" -b <<EOF
put -R "$AZ_DATA_HOME/az_mskimpact" "$AZ_REPO_NAME"
put -R "$AZ_DATA_HOME/gene_panels" "$AZ_REPO_NAME"
put "$AZ_DATA_HOME/README.md" "$AZ_REPO_NAME"
exit
EOF
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
    filenames_to_deliver[data_mutations_manual.txt]+=1
    filenames_to_deliver[data_nonsignedout_mutations.txt]+=1
    filenames_to_deliver[data_sv.txt]+=1
    filenames_to_deliver[az_mskimpact_data_cna_hg19.seg]+=1
    filenames_to_deliver[case_lists]+=1

    # Meta files to deliver
    filenames_to_deliver[meta_clinical_patient.txt]+=1
    filenames_to_deliver[meta_clinical_sample.txt]+=1
    filenames_to_deliver[meta_CNA.txt]+=1
    filenames_to_deliver[meta_gene_matrix.txt]+=1
    filenames_to_deliver[meta_mutations_extended.txt]+=1
    filenames_to_deliver[meta_study.txt]+=1
    filenames_to_deliver[meta_sv.txt]+=1
    filenames_to_deliver[az_mskimpact_meta_cna_hg19_seg.txt]+=1

    # Remove any files/directories that are not specified above
    for filepath in $AZ_MSK_IMPACT_DATA_HOME/* ; do
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
    # mskimpact_data_cna_hg19.seg -> az_mskimpact_data_cna_hg19.seg
    # mskimpact_meta_cna_hg19_seg.txt -> az_mskimpact_meta_cna_hg19_seg.txt

    unset filenames_to_rename
    declare -A filenames_to_rename

    # Data files to rename
    filenames_to_rename[mskimpact_data_cna_hg19.seg]=az_mskimpact_data_cna_hg19.seg
    filenames_to_rename[mskimpact_meta_cna_hg19_seg.txt]=az_mskimpact_meta_cna_hg19_seg.txt

    for original_filename in "${!filenames_to_rename[@]}"
    do
        if [ -f "$AZ_MSK_IMPACT_DATA_HOME/$original_filename" ]; then
            if ! mv "$AZ_MSK_IMPACT_DATA_HOME/$original_filename" "$AZ_MSK_IMPACT_DATA_HOME/${filenames_to_rename[$original_filename]}" ; then
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
    PATIENT_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.filtered"
    find_clinical_attributes_to_filter_arg "$PATIENT_INPUT_FILEPATH" patient
    PATIENT_EXCLUDED_HEADER_FIELD_LIST="$clinical_attributes_to_filter_arg"

    # Determine which columns to exclude in the sample file
    SAMPLE_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.filtered"
    find_clinical_attributes_to_filter_arg "$SAMPLE_INPUT_FILEPATH" sample
    SAMPLE_EXCLUDED_HEADER_FIELD_LIST="$clinical_attributes_to_filter_arg"
    
    # Filter out the columns we want to exclude in both files
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$PATIENT_INPUT_FILEPATH" -e "$PATIENT_EXCLUDED_HEADER_FIELD_LIST" > "$PATIENT_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py -c "$SAMPLE_INPUT_FILEPATH" -e "$SAMPLE_EXCLUDED_HEADER_FIELD_LIST" > "$SAMPLE_OUTPUT_FILEPATH" &&
    
    # Rewrite the patient and sample files with updated data
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH" &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
}

function rename_cdm_clinical_attribute_columns() {
    # Rename clinical patient attributes coming from CDM:
    # CURRENT_AGE_DEID -> AGE_CURRENT
    # GENDER -> SEX

    PATIENT_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.renamed"

    sed -e '1s/CURRENT_AGE_DEID/AGE_CURRENT/' -e '1s/GENDER/SEX/' $PATIENT_INPUT_FILEPATH > $PATIENT_OUTPUT_FILEPATH &&
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH"
}

function add_metadata_headers() {
    # Calling merge.py strips out metadata headers from our clinical files - add them back in
    CDD_URL="https://cdd.cbioportal.mskcc.org/api/"
    INPUT_FILENAMES="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt $AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact
}

function standardize_clinical_data() {
    PATIENT_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.standardized"
    SAMPLE_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.standardized"

    # Standardize the clinical files to use NA for blank values
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_clinical_data.py -f "$PATIENT_INPUT_FILEPATH" > "$PATIENT_OUTPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_clinical_data.py -f "$SAMPLE_INPUT_FILEPATH" > "$SAMPLE_OUTPUT_FILEPATH" &&

    # Rewrite the patient and sample files with updated data
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH" &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
}

function standardize_cna_data() {
    # Standardize the CNA file to use NA for blank values
    DATA_CNA_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_CNA.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_cna_data.py -f "$DATA_CNA_INPUT_FILEPATH"
}

function standardize_mutations_data() {
    MUTATIONS_EXTD_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    MUTATIONS_MAN_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_manual.txt"
    NSOUT_MUTATIONS_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"

    # Standardize the mutations files to check for valid values in the 'NCBI_Build' column
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_mutations_data.py -f "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_mutations_data.py -f "$MUTATIONS_MAN_INPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_mutations_data.py -f "$NSOUT_MUTATIONS_INPUT_FILEPATH"
}

function remove_duplicate_maf_variants() {
    MUTATIONS_EXTD_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    NSOUT_MUTATIONS_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"

    MUTATIONS_EXTD_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended_merged.txt"
    NSOUT_MUTATIONS_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations_merged.txt"

    # Remove duplicate variants from MAF files
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/remove-duplicate-maf-variants.py -i "$NSOUT_MUTATIONS_INPUT_FILEPATH" &&

    # Rewrite mutation files with updated data
    mv "$MUTATIONS_EXTD_OUTPUT_FILEPATH" "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    mv "$NSOUT_MUTATIONS_OUTPUT_FILEPATH" "$NSOUT_MUTATIONS_INPUT_FILEPATH"
}

function standardize_structural_variant_data() {
    DATA_SV_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_sv.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_structural_variant_data.py -f "$DATA_SV_INPUT_FILEPATH"
}

function anonymize_age_at_seq_with_cap() {
    PATIENT_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    PATIENT_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt.os_months_trunc"
    SAMPLE_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt"
    SAMPLE_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt.age_anonymized"
    UPPER_AGE_LIMIT=89
    OS_MONTHS_PRECISION=2

    # Anonymize AGE_AT_SEQUENCING_REPORTED_YEARS and truncate OS_MONTHS
    $PYTHON3_BINARY $PORTAL_HOME/scripts/anonymize_age_at_seq_with_cap_py3.py "$PATIENT_INPUT_FILEPATH" "$PATIENT_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH" "$SAMPLE_OUTPUT_FILEPATH" -u "$UPPER_AGE_LIMIT" -o "$OS_MONTHS_PRECISION" &&

    # Rewrite the patient and sample files with updated data
    mv "$PATIENT_OUTPUT_FILEPATH" "$PATIENT_INPUT_FILEPATH" &&
    mv "$SAMPLE_OUTPUT_FILEPATH" "$SAMPLE_INPUT_FILEPATH"
}

function generate_case_lists() {
    # Generate case lists based on our subset of patients + samples
    CASE_LIST_DIR="$AZ_MSK_IMPACT_DATA_HOME/case_lists"
    if ! [ -d "$CASE_LIST_DIR" ] ; then
        if ! mkdir -p "$CASE_LIST_DIR" ; then
            return 1
        fi
    fi
    $PYTHON_BINARY /data/portal-cron/scripts/generate_case_lists.py --case-list-config-file $CASE_LIST_CONFIG_FILE --case-list-dir $CASE_LIST_DIR --study-dir $AZ_MSK_IMPACT_DATA_HOME --study-id $AZ_MSKIMPACT_STABLE_ID -o
}

# ------------------------------------------------------------------------------------------------------------------------
# Pull latest from AstraZeneca repo (az-data)
printTimeStampedDataProcessingStepMessage "Pull of AstraZeneca MSK-IMPACT data updates"

if ! pull_latest_data_from_az_git_repo ; then
    report_error "ERROR: Failed git pull for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Copy data from local clone of MSK Solid Heme repo to local clone of AZ repo

# Create temporary directory to store data before subsetting
if ! [ -d "$AZ_TMPDIR" ] ; then
    if ! mkdir -p "$AZ_TMPDIR" ; then
        report_error "ERROR: Failed to create temporary directory for AstraZeneca MSK-IMPACT. Exiting."
    fi
fi
if [[ -d "$AZ_TMPDIR" && "$AZ_TMPDIR" != "/" ]] ; then
    rm -rf "$AZ_TMPDIR"/*
fi

cp -a $MSK_SOLID_HEME_DATA_HOME/* $AZ_TMPDIR

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to populate temporary directory for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Post-process the dataset

printTimeStampedDataProcessingStepMessage "Subset and merge of Part A Consented patients for AstraZeneca MSK-IMPACT"

# Generate subset of Part A consented patients from MSK-Impact
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py \
    --study-id="mskimpact" \
    --clinical-file="$AZ_TMPDIR/data_clinical_patient.txt" \
    --filter-criteria="PARTA_CONSENTED_12_245=YES" \
    --subset-filename="$AZ_TMPDIR/part_a_subset.txt"

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to subset consented patients for AstraZeneca MSK-IMPACT. Exiting."
fi

# Write out the subsetted data
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
    --study-id="mskimpact" \
    --subset="$AZ_TMPDIR/part_a_subset.txt" \
    --output-directory="$AZ_MSK_IMPACT_DATA_HOME" \
    --merge-clinical="true" \
    $AZ_TMPDIR

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to write out subsetted data for AstraZeneca MSK-IMPACT. Exiting."
fi

printTimeStampedDataProcessingStepMessage "Post-processing for AstraZeneca MSK-IMPACT"

# Rename files that need to be renamed
if ! rename_files_in_delivery_directory ; then
    report_error "ERROR: Failed to rename files for AstraZeneca MSK-IMPACT. Exiting."
fi

# Filter clinical attribute columns from clinical files
if ! filter_clinical_attribute_columns ; then
    report_error "ERROR: Failed to filter non-delivered clinical attribute columns for AstraZeneca MSK-IMPACT. Exiting."
fi

if ! rename_cdm_clinical_attribute_columns ; then
    report_error "ERROR: Failed to rename CDM clinical attribute columns for AstraZeneca MSK-IMPACT. Exiting."
fi

# Add metadata headers to clinical files
if ! add_metadata_headers ; then
    report_error "ERROR: Failed to add metadata headers to clinical attribute files for AstraZeneca MSK-IMPACT. Exiting."
fi

# Standardize blank clinical data values to NA
if ! standardize_clinical_data ; then
    report_error "ERROR: Failed to standardize blank clinical data values to NA for AstraZeneca MSK-IMPACT. Exiting."
fi

# Standardize blank CNA data values to NA
if ! standardize_cna_data ; then
    report_error "ERROR: Failed to standardize blank CNA data values to NA for AstraZeneca MSK-IMPACT. Exiting."
fi

# Standardize mutations files
if ! standardize_mutations_data ; then
    report_error "ERROR: Failed to standardize mutations files for AstraZeneca MSK-IMPACT. Exiting."
fi

# Remove duplicate variants from MAF files
if ! remove_duplicate_maf_variants ; then
    report_error "ERROR: Failed to remove duplicate variants from MAF files for AstraZeneca MSK-IMPACT. Exiting."
fi

# Standardize structural variant data by removing records with invalid genes and standardizing the file header
if ! standardize_structural_variant_data ; then
    report_error "ERROR: Failed to standardize structural variant data for AstraZeneca MSK-IMPACT. Exiting."
fi

# Anonymize ages
#if ! anonymize_age_at_seq_with_cap ; then
#    report_error "ERROR: Failed to anonymize AGE_AT_SEQUENCING_REPORTED_YEARS for AstraZeneca MSK-IMPACT. Exiting."
#fi

printTimeStampedDataProcessingStepMessage "Filter non-delivered files and include delivered meta files for AstraZeneca MSK-IMPACT"

# Filter out files which are not delivered
if ! filter_files_in_delivery_directory ; then
    report_error "ERROR: Failed to filter non-delivered files for AstraZeneca MSK-IMPACT. Exiting."
fi

# Remove temporary directory now that the subset has been merged and post-processed
if [[ -d "$AZ_TMPDIR" && "$AZ_TMPDIR" != "/" ]] ; then
    rm -rf "$AZ_TMPDIR" "$AZ_MSK_IMPACT_DATA_HOME/part_a_subset.txt"
fi

# ------------------------------------------------------------------------------------------------------------------------
# Run changelog script
printTimeStampedDataProcessingStepMessage "Generate changelog for AstraZeneca MSK-IMPACT"

$PYTHON3_BINARY $PORTAL_HOME/scripts/generate_az_study_changelog_py3.py $AZ_MSK_IMPACT_DATA_HOME

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to generate changelog summary for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Filter germline events from mutation file and structural variant file
printTimeStampedDataProcessingStepMessage "Filter germline events for AstraZeneca MSK-IMPACT"

mutation_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
mutation_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $mutation_filepath $mutation_filtered_filepath --event-type mutation
if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to filter germline events from mutation file for AstraZeneca MSK-IMPACT. Exiting."
fi
mv $mutation_filtered_filepath $mutation_filepath

mutation_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"
mutation_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $mutation_filepath $mutation_filtered_filepath --event-type mutation
if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to filter germline events from nonsignedout mutation file for AstraZeneca MSK-IMPACT. Exiting."
fi
mv $mutation_filtered_filepath $mutation_filepath

sv_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_sv.txt"
sv_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_sv.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $sv_filepath $sv_filtered_filepath --event-type structural_variant

if [ $? -gt 0 ] ; then
    report_error "ERROR: Failed to filter germline events from structural variant file for AstraZeneca MSK-IMPACT. Exiting."
fi
mv $sv_filtered_filepath $sv_filepath

# ------------------------------------------------------------------------------------------------------------------------
# Generate case list files
printTimeStampedDataProcessingStepMessage "Generate case list files for AstraZeneca MSK-IMPACT"

if ! generate_case_lists ; then
    report_error "ERROR: Failed to generate case lists for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Push the updated data to GitHub
printTimeStampedDataProcessingStepMessage "Push data updates to AstraZeneca MSK-IMPACT git repository"

if ! push_updates_to_az_git_repo ; then
    report_error "ERROR: Failed git push for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Push the updated data to AstraZeneca's SFTP server

printTimeStampedDataProcessingStepMessage "Transfer data updates to SFTP server for AstraZeneca MSK-IMPACT"

if ! transfer_to_az_sftp_server ; then
    report_error "ERROR: Failed to transfer data updates to SFTP server for AstraZeneca MSK-IMPACT. Exiting."
fi

# ------------------------------------------------------------------------------------------------------------------------
# Cleanup AZ git repo

printTimeStampedDataProcessingStepMessage "Cleaning up untracked files from AZ repo"
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $AZ_DATA_HOME

# Send a message on success
sendImportSuccessMessageMskPipelineLogsSlack "ASTRAZENECA MSKIMPACT"
