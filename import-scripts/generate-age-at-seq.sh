#Put a ddp fetch into a script - genie
#Run the ddp fetcher in age at seq mode
#Ideally just write out the age at seq - but a clinical might be written out

# MSKIMPACT DDP Fetch - took like 2.5 hours
# wrote out
# data_clinical_ddp_age_at_seq.txt
# data_clinical_ddp.txt
# ddp/ddp_naaccr.txt
# ddp/ddp_vital_status.txt

echo "exporting impact data_clinical_mskimpact_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: MSKIMPACT Redcap export of mskimpact_data_clinical_ddp_demographics"
fi

echo "exporting impact data_clinical.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_cvr
if [ $? -gt 0 ] ; then
    echo "ERROR: MSKIMPACT Redcap export of mskimpact_data_clinical_cvr"
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for mskimpact"
mskimpact_dmp_pids_file=$MSK_IMPACT_DATA_HOME/mskimpact_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | sort | uniq > $mskimpact_dmp_pids_file
MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSKIMPACT_REDCAP_BACKUP/data_clinical_mskimpact_data_clinical_ddp_demographics.txt)
if [ $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact -p $mskimpact_dmp_pids_file -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_IMPACT_DATA_HOME -r $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    echo "ERROR: MSKIMPACT DDP Demographics Fetch"
fi

# --------------------------------------------------------------------------------------------------------------
# HEMEPACT DDP Fetch

echo "exporting heme data_clinical_hemepact_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: HEMEPACT Redcap export of hemepact_data_clinical_ddp_demographics"
fi

echo "exporting heme data_clinical.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical
if [ $? -gt 0 ] ; then
    echo "ERROR: HEMEPACT Redcap export of hemepact_data_clinical_cvr"
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for hemepact"
mskimpact_heme_dmp_pids_file=$MSK_HEMEPACT_DATA_HOME/mskimpact_heme_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt | sort | uniq > $mskimpact_heme_dmp_pids_file
HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_ddp_demographics.txt)
if [ $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact_heme -p $mskimpact_heme_dmp_pids_file -s $MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_HEMEPACT_DATA_HOME -r $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT DDP Demographics Fetch"
fi

# --------------------------------------------------------------------------------------------------------------
# ACCESS DDP Fetch

echo "exporting access data_clinical_mskaccess_data_clinical_ddp_demographics.txt from redcap"
export_project_from_redcap $MSK_ACCESS_DATA_HOME mskaccess_data_clinical_ddp_demographics
if [ $? -gt 0 ] ; then
    echo "ERROR: ACCESS Redcap export of mskaccess_data_clinical_ddp_demographics"
fi

echo "exporting access data_clinical.txt from redcap"
export_project_from_redcap $MSK_ACCESS_DATA_HOME mskaccess_data_clinical
if [ $? -gt 0 ] ; then
    echo "ERROR: ACCESS Redcap export of mskaccess_data_clinical_cvr"
fi

printTimeStampedDataProcessingStepMessage "DDP demographics fetch for access"
mskaccess_dmp_pids_file=$MSK_ACCESS_DATA_HOME/mskaccess_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt | sort | uniq > $mskaccess_dmp_pids_file
ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_ddp_demographics.txt)
if [ $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskaccess -p $mskaccess_dmp_pids_file -s $MSK_ACCESS_DATA_HOME/cvr/seq_date.txt -f ageAtSeqDate -o $MSK_ACCESS_DATA_HOME -r $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS DDP Demographics Fetch"
fi

# --------------------------------------------------------------------------------------------------------------
# Combine age at seq files and merge into clinical sample file

AGE_AT_SEQ_FILENAME="data_clinical_ddp_age_at_seq.txt"
MSK_ACCESS_AGE_AT_SEQ="$MSK_ACCESS_DATA_HOME/$AGE_AT_SEQ_FILENAME"
MSK_HEMEPACT_AGE_AT_SEQ="$MSK_HEMEPACT_DATA_HOME/$AGE_AT_SEQ_FILENAME"
MSK_IMPACT_AGE_AT_SEQ="$MSK_IMPACT_DATA_HOME/$AGE_AT_SEQ_FILENAME"
MERGED_SEQ_DATE="./merged_age_at_seq.txt"

SAMPLE_INPUT_FILEPATH="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"
SAMPLE_OUTPUT_FILEPATH="$./data_clinical_sample.txt"
KEY_COLUMNS="SAMPLE_ID PATIENT_ID"

$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$MSK_ACCESS_AGE_AT_SEQ" "$MSK_HEMEPACT_AGE_AT_SEQ" "$MSK_IMPACT_AGE_AT_SEQ" -o "$MERGED_AGE_AT_SEQ" -m outer &&
$PYTHON3_BINARY $PORTAL_HOME/scripts/combine_files_py3.py -i "$SAMPLE_INPUT_FILEPATH" "$MERGED_AGE_AT_SEQ" -o "$SAMPLE_OUTPUT_FILEPATH" -c $KEY_COLUMNS -m left
