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

function subset_consented_patients() {
    STUDY_ID="$1"
    INPUT_DIR="$2"
    OUTPUT_DIR="$3"
    SUBSET_FILE=$(mktemp -q)

    # Generate subset of Part A consented patients
    $PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py \
        --study-id="$STUDY_ID" \
        --clinical-file="$INPUT_DIR/data_clinical_patient.txt" \
        --filter-criteria="PARTA_CONSENTED_12_245=YES" \
        --subset-filename="$SUBSET_FILE"

    if [ $? -gt 0 ] ; then
        report_error "ERROR: Failed to subset consented patients. Exiting."
    fi

    # Write out the subsetted data
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
        --study-id="$STUDY_ID" \
        --subset="$SUBSET_FILE" \
        --output-directory="$OUTPUT_DIR" \
        --merge-clinical="true" \
        $INPUT_DIR

}

# TODO come back
function rename_file() {
    ORIGINAL_FILENAME="$1"
    NEW_FILENAME="$2"

    if ! mv "$ORIGINAL_FILENAME" "$NEW_FILENAME" ; then
        return 1
    fi
}

function filter_clinical_cols() {
    INPUT_FILE="$1"
    ATTRIBUTES_TO_DELIVER="$2"

    INPUT_FILEPATH="$INPUT_FILE"
    OUTPUT_FILEPATH="$INPUT_FILE.filtered"
    filter_clinical_attribute_columns "$INPUT_FILEPATH" "$ATTRIBUTES_TO_DELIVER" "$OUTPUT_FILEPATH"
}

function rename_cdm_clinical_cols() {
    # Rename clinical patient attributes coming from CDM:
        # CURRENT_AGE_DEID -> AGE_CURRENT
        # GENDER -> SEX

    INPUT_FILE="$1"
    OUTPUT_FILE="$INPUT_FILE.renamed"

    sed -e '1s/CURRENT_AGE_DEID/AGE_CURRENT/' -e '1s/GENDER/SEX/' $INPUT_FILE > $OUTPUT_FILE &&
    mv "$OUTPUT_FILE" "$INPUT_FILE"
}

function add_metadata_headers() {


    # Calling merge.py strips out metadata headers from our clinical files - add them back in
    CDD_URL="https://cdd.cbioportal.mskcc.org/api/"
    INPUT_FILENAMES="$AZ_MSK_IMPACT_DATA_HOME/data_clinical_sample.txt $AZ_MSK_IMPACT_DATA_HOME/data_clinical_patient.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $INPUT_FILENAMES -c "$CDD_URL" -s mskimpact
}


