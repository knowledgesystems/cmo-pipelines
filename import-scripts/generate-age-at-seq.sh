# Define temporary directories
TMP_DIR="/data/portal-cron/tmp/generate_age_at_seq"
MSK_IMPACT_TMP_DIR="$TMP_DIR/mskimpact"
MSK_HEMEPACT_TMP_DIR="$TMP_DIR/mskhemepact"
MSK_ACCESS_TMP_DIR="$TMP_DIR/mskaccess"

source /data/portal-cron/scripts/dmp-import-vars-functions.sh

# Create mskimpact tmp dir if necessary
if ! [ -d "$MSK_IMPACT_TMP_DIR" ] ; then
    if ! mkdir -p "$MSK_IMPACT_TMP_DIR" ; then
        echo "Error : could not create tmp directory '$MSK_IMPACT_TMP_DIR'" >&2
        exit 1
    fi
else
    # Remove files from last fetch
    rm -rf $MSK_IMPACT_TMP_DIR/*
fi

# Create hemepact tmp dir if necessary
if ! [ -d "$MSK_HEMEPACT_TMP_DIR" ] ; then
    if ! mkdir -p "$MSK_HEMEPACT_TMP_DIR" ; then
        echo "Error : could not create tmp directory '$MSK_HEMEPACT_TMP_DIR'" >&2
        exit 1
    fi
else
    # Remove files from last fetch
    rm -rf $MSK_HEMEPACT_TMP_DIR/*
fi

# Create mskaccess tmp dir if necessary
if ! [ -d "$MSK_ACCESS_TMP_DIR" ] ; then
    if ! mkdir -p "$MSK_ACCESS_TMP_DIR" ; then
        echo "Error : could not create tmp directory '$MSK_ACCESS_TMP_DIR'" >&2
        exit 1
    fi
else
    # Remove files from last fetch
    rm -rf $MSK_ACCESS_TMP_DIR/*
fi

# --------------------------------------------------------------------------------------------------------------
# MSKIMPACT DDP Fetch
echo "exporting impact data_clinical_mskimpact_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_IMPACT_TMP_DIR mskimpact_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: MSKIMPACT Redcap export of mskimpact_data_clinical_ddp_demographics"
    exit 1
fi

echo "exporting impact data_clinical.txt from redcap"
export_project_from_redcap $MSK_IMPACT_TMP_DIR mskimpact_data_clinical_cvr
if [ $? -gt 0 ] ; then
    echo "ERROR: MSKIMPACT Redcap export of mskimpact_data_clinical_cvr"
    exit 1
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for mskimpact"
mskimpact_dmp_pids_file=$MSK_IMPACT_TMP_DIR/mskimpact_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_TMP_DIR/data_clinical_mskimpact_data_clinical_cvr.txt | sort | uniq > $mskimpact_dmp_pids_file
MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSK_IMPACT_TMP_DIR/data_clinical_mskimpact_data_clinical_ddp_demographics.txt)
if [ $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact -p $mskimpact_dmp_pids_file -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_IMPACT_TMP_DIR -r $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    echo "ERROR: MSKIMPACT DDP Demographics Fetch"
    exit 1
fi

# --------------------------------------------------------------------------------------------------------------
# HEMEPACT DDP Fetch

echo "exporting heme data_clinical_hemepact_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_TMP_DIR hemepact_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: HEMEPACT Redcap export of hemepact_data_clinical_ddp_demographics"
    exit 1
fi

echo "exporting heme data_clinical.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_TMP_DIR hemepact_data_clinical
if [ $? -gt 0 ] ; then
    echo "ERROR: HEMEPACT Redcap export of hemepact_data_clinical_cvr"
    exit 1
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for hemepact"
mskimpact_heme_dmp_pids_file=$MSK_HEMEPACT_TMP_DIR/mskimpact_heme_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_HEMEPACT_TMP_DIR/data_clinical_hemepact_data_clinical.txt | sort | uniq > $mskimpact_heme_dmp_pids_file
HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSK_HEMEPACT_TMP_DIR/data_clinical_hemepact_data_clinical_ddp_demographics.txt)
if [ $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact_heme -p $mskimpact_heme_dmp_pids_file -s $MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_HEMEPACT_TMP_DIR -r $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    echo "ERROR: HEMEPACT DDP Demographics Fetch"
    exit 1
fi

# --------------------------------------------------------------------------------------------------------------
# ACCESS DDP Fetch

echo "exporting access data_clinical_mskaccess_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_ACCESS_TMP_DIR mskaccess_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: ACCESS Redcap export of mskaccess_data_clinical_ddp_demographics"
    exit 1
fi

echo "exporting access data_clinical.txt from redcap"
export_project_from_redcap $MSK_ACCESS_TMP_DIR mskaccess_data_clinical
if [ $? -gt 0 ] ; then
    echo "ERROR: ACCESS Redcap export of mskaccess_data_clinical_cvr"
    exit 1
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for access"
mskaccess_dmp_pids_file=$MSK_ACCESS_TMP_DIR/mskaccess_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_ACCESS_TMP_DIR/data_clinical_mskaccess_data_clinical.txt | sort | uniq > $mskaccess_dmp_pids_file
ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSK_ACCESS_TMP_DIR/data_clinical_mskaccess_data_clinical_ddp_demographics.txt)
if [ $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskaccess -p $mskaccess_dmp_pids_file -s $MSK_ACCESS_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_ACCESS_TMP_DIR -r $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    echo "ERROR: ACCESS DDP Demographics Fetch"
    exit 1
fi

# --------------------------------------------------------------------------------------------------------------
# Combine age at seq files and merge into msk_solid_heme clinical sample file

AGE_AT_SEQ_FILENAME="data_clinical_ddp_age_at_seq.txt"
MSK_IMPACT_AGE_AT_SEQ="$MSK_IMPACT_TMP_DIR/$AGE_AT_SEQ_FILENAME"
MSK_HEMEPACT_AGE_AT_SEQ="$MSK_HEMEPACT_TMP_DIR/$AGE_AT_SEQ_FILENAME"
MSK_ACCESS_AGE_AT_SEQ="$MSK_ACCESS_TMP_DIR/$AGE_AT_SEQ_FILENAME"
MERGED_AGE_AT_SEQ="$TMP_DIR/merged_age_at_seq.txt"

SAMPLE_INPUT_FILEPATH="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"
SAMPLE_OUTPUT_FILEPATH="$TMP_DIR/data_clinical_sample.txt"
KEY_COLUMNS="SAMPLE_ID PATIENT_ID"

$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$MSK_IMPACT_AGE_AT_SEQ" "$MSK_HEMEPACT_AGE_AT_SEQ" "$MSK_ACCESS_AGE_AT_SEQ" -o "$MERGED_AGE_AT_SEQ" -m outer &&
$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$SAMPLE_INPUT_FILEPATH" "$MERGED_AGE_AT_SEQ" -o "$SAMPLE_OUTPUT_FILEPATH" -c $KEY_COLUMNS -m left &&
$PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $SAMPLE_OUTPUT_FILEPATH -s mskimpact -i /data/portal-cron/scripts/cdm_metadata.json
