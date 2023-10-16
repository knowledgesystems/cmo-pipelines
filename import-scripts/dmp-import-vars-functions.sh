#!/bin/bash

## GLOBAL CONSTANTS
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
CVR_TEST_MODE_ARGS=""
PERFORM_CRDB_FETCH=0
PROCESS_UNLINKED_ARCHER_STUDY=0
CRDB_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/crdb_fetcher.jar"
CVR_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/cvr_fetcher.jar"
DARWIN_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/darwin_fetcher.jar"
DDP_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/ddp_fetcher.jar"
REDCAP_PIPELINE_JAR_FILENAME="$PORTAL_HOME/lib/redcap_pipeline.jar"
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-dmp-importer.jar"
JAVA_CRDB_FETCHER_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED -jar $CRDB_FETCHER_JAR_FILENAME"
JAVA_CVR_FETCHER_ARGS="-Xmx64g -jar $CVR_FETCHER_JAR_FILENAME"
JAVA_DARWIN_FETCHER_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED -jar $DARWIN_FETCHER_JAR_FILENAME"
JAVA_DDP_FETCHER_ARGS="-Xmx48g $JAVA_SSL_ARGS -jar $DDP_FETCHER_JAR_FILENAME"
JAVA_REDCAP_PIPELINE_ARGS="$JAVA_SSL_ARGS -jar $REDCAP_PIPELINE_JAR_FILENAME"
# the cvr server safety lockouts are no longer in use now that cvr timeout/retry loops are in effect
CVR_TUMOR_SERVER_SAFETY_LOCKOUT_PERIOD_START_HOUR_MINUTES=0115 #HHMM
CVR_TUMOR_SERVER_SAFETY_LOCKOUT_PERIOD_DURATION_MINUTES=75
CVR_GERMLINE_SERVER_SAFETY_LOCKOUT_PERIOD_START_HOUR_MINUTES=0115 #HHMM
CVR_GERMLINE_SERVER_SAFETY_LOCKOUT_PERIOD_DURATION_MINUTES=75
SAFETY_LOCKOUT_SLEEP_INTERVAL_SECONDS=30

java_debug_args=""
ENABLE_DEBUGGING=0
if [ $ENABLE_DEBUGGING != "0" ] ; then
    java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182"
fi
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$MSK_DMP_TMPDIR -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
SLACK_PIPELINES_MONITOR_URL=`cat $SLACK_URL_FILE`

DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT=2

# Clinical attribute fields which should never be filtered because of empty content
FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST="PATIENT_ID,SAMPLE_ID,ONCOTREE_CODE,PARTA_CONSENTED_12_245,PARTC_CONSENTED_12_245"

# -----------------------------------------------------------------------------------------------------------
## FUNCTIONS

# import needed function waitWhileWithinTimePeriod()
source $PORTAL_HOME/scripts/date-and-time-handling-functions.sh

# Function for alerting slack channel of any failures
function sendPreImportFailureMessageMskPipelineLogsSlack() {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines pre-import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# Function for alerting slack channel of any failures
function sendImportFailureMessageMskPipelineLogsSlack() {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# Function for alerting slack channel of successful imports
function sendImportSuccessMessageMskPipelineLogsSlack() {
    STUDY_ID=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines import success: $STUDY_ID\", \"icon_emoji\": \":tada:\"}" $SLACK_PIPELINES_MONITOR_URL
}

function printTimeStampedDataProcessingStepMessage {
    STEP_DESCRIPTION=$1
    echo -e "\n\n------------------------------------------------------------------------------------"
    echo "beginning $STEP_DESCRIPTION $(date)..."
}

function standardizeGenePanelMatrix {
    STUDY_DATA_DIRECTORY=$1
    GENE_PANEL_MATRIX_FILE="$STUDY_DATA_DIRECTORY/data_gene_matrix.txt"
    $PYTHON_BINARY $PORTAL_HOME/scripts/standardize_gene_matrix_file.py --gene-panel-matrix-filename $GENE_PANEL_MATRIX_FILE
}

# Function to generate case lists by cancer type
function addCancerTypeCaseLists {
    STUDY_DATA_DIRECTORY=$1
    STUDY_ID=$2
    # accept 1 or 2 data_clinical filenames
    FILENAME_1="$3" # this will be oncotree converted then added to the list
    FILENAME_2="$4" # this will not be converted but will be added to the list
    FILEPATH_1="$STUDY_DATA_DIRECTORY/$FILENAME_1"
    FILEPATH_2="$STUDY_DATA_DIRECTORY/$FILENAME_2"
    CLINICAL_FILE_LIST="$FILEPATH_1, $FILEPATH_2"
    if [ -z "$FILENAME_2" ] ; then
        CLINICAL_FILE_LIST="$FILEPATH_1"
    fi
    CASE_LISTS_DIRECTORY="$STUDY_DATA_DIRECTORY/case_lists"
    if ! [ -d "$STUDY_DATA_DIRECTORY" ] ; then
        echo "Error in addCancerTypeCaseLists : argument for STUDY_DATA_DIRECTORY '$STUDY_DATA_DIRECTORY' is not a directory" >&2
        return 1
    fi
    if ! [ -d "$CASE_LISTS_DIRECTORY" ] ; then
        mkdir -p "$CASE_LISTS_DIRECTORY"
        mkdir_status=$?
        if [ $mkdir_status -ne 0 ] ; then
            echo "Error in addCancerTypeCaseLists : could not create missing case_lists directory '$CASE_LISTS_DIRECTORY'" >&2
            return 1
        fi
    fi
    # remove current case lists and run oncotree converter before creating new cancer case lists
    rm -f $STUDY_DATA_DIRECTORY/case_lists/*
    $PYTHON_BINARY $PORTAL_HOME/scripts/oncotree_code_converter.py --oncotree-url "http://oncotree.mskcc.org/" --oncotree-version $ONCOTREE_VERSION_TO_USE --clinical-file $FILEPATH_1 --force
    $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file-list="$CLINICAL_FILE_LIST" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="CANCER_TYPE"
    if [ "$STUDY_ID" == "mskimpact" ] || [ "$STUDY_ID" == "mixedpact" ] || [ "$STUDY_ID" == "msk_solid_heme" ] ; then
       $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file-list="$CLINICAL_FILE_LIST" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="PARTC_CONSENTED_12_245"
    fi
}

# Function for adding "DATE ADDED" information to clinical data
function addDateAddedData {
    STUDY_DATA_DIRECTORY=$1
    DATA_CLINICAL_FILENAME=$2
    DATA_CLINICAL_SUPP_DATE_FILENAME=$3
    # add "date added" to clinical data file
    $PYTHON_BINARY $PORTAL_HOME/scripts/update-date-added.py --date-added-file=$STUDY_DATA_DIRECTORY/$DATA_CLINICAL_SUPP_DATE_FILENAME --clinical-file=$STUDY_DATA_DIRECTORY/$DATA_CLINICAL_FILENAME
}

# Function for import project data into redcap
function import_project_to_redcap {
    FILENAME=$1
    PROJECT_TITLE=$2
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -i --filename $FILENAME --redcap-project-title $PROJECT_TITLE
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to import file $FILENAME into redcap project $PROJECT_TITLE"
        return 1
    fi
}

# Function for exporting redcap project
function export_project_from_redcap {
    DIRECTORY=$1
    PROJECT_TITLE=$2
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -d $DIRECTORY --redcap-project-title $PROJECT_TITLE
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to export project $PROJECT_TITLE from redcap into directory $DIRECTORY"
        return 1
    fi
}

# Function for exporting redcap projects (merged to standard cbioportal format) by stable id
function export_stable_id_from_redcap {
    STABLE_ID=$1
    DIRECTORY=$2
    IGNORED_PROJECTS_LIST=$3
    IGNORED_PROJECTS_ARGUMENT=""
    if [ ! -z $IGNORED_PROJECTS_LIST ] ; then
        IGNORED_PROJECTS_ARGUMENT="-m $IGNORED_PROJECTS_LIST"
    fi
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -s $STABLE_ID -d $DIRECTORY $IGNORED_PROJECTS_ARGUMENT
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to export stable_id $STABLE_ID from REDCap into directory $DIRECTORY"
        return 1
    fi
}

# Function for importing crdb files to redcap
function import_crdb_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_supp_crdb_basic.txt mskimpact_crdb_basic ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_supp_crdb_survey.txt mskimpact_crdb_survey ; then return_value=1 ; fi
    return $return_value
}

# Function for importing mskimpact darwin files to redcap
function import_mskimpact_darwin_caisis_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_supp_caisis_gbm.txt mskimpact_clinical_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_imaging_caisis_gbm.txt mskimpact_timeline_imaging_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_specimen_caisis_gbm.txt mskimpact_timeline_specimen_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_status_caisis_gbm.txt mskimpact_timeline_status_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_surgery_caisis_gbm.txt mskimpact_timeline_surgery_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_treatment_caisis_gbm.txt mskimpact_timeline_treatment_caisis ; then return_value=1 ; fi
    return $return_value
}

# Function for importing mskimpact cvr files to redcap
function import_mskimpact_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt mskimpact_data_clinical_cvr ; then return_value=1 ; fi
    return $return_value
}

# Function for importing mskimpact supp date files to redcap
function import_mskimpact_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt mskimpact_supp_date_cbioportal_added ; then return_value=1 ; fi
    return $return_value
}

# Function for importing mskimpact ddp files to redcap
function import_mskimpact_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_ddp.txt mskimpact_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_ddp_age_at_seq.txt mskimpact_data_clinical_ddp_age_at_seq ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_ddp_pediatrics.txt mskimpact_data_clinical_ddp_demographics_pediatrics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskimpact_timeline_chemotherapy_ddp; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_radiation.txt mskimpact_timeline_radiation_ddp ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_surgery.txt mskimpact_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing hemepact cvr files to redcap
function import_hemepact_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt hemepact_data_clinical ; then return_value=1 ; fi
    return $return_value
}

# Function for importing hemepact supp date files to redcap
function import_hemepact_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt hemepact_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for import hemepact ddp files to redcap
function import_hemepact_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_ddp.txt hemepact_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_ddp_age_at_seq.txt hemepact_data_clinical_ddp_age_at_seq ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt hemepact_timeline_chemotherapy_ddp; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_radiation.txt hemepact_timeline_radiation_ddp ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_surgery.txt hemepact_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing archer cvr files to redcap
function import_archer_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt mskarcher_data_clinical ; then return_value=1 ; fi
    return $return_value
}

# Function for importing archer supp date files to redcap
function import_archer_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt mskarcher_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for import archer ddp files to redcap
function import_archer_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_ddp.txt mskarcher_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_ddp_age_at_seq.txt mskarcher_data_clinical_ddp_age_at_seq ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskarcher_timeline_chemotherapy_ddp; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_radiation.txt mskarcher_timeline_radiation_ddp ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_surgery.txt mskarcher_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing access cvr files to redcap
function import_access_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt mskaccess_data_clinical ; then return_value=1 ; fi
    return $return_value
}

# Function for importing access supp date files to redcap
function import_access_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt mskaccess_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for import access ddp files to redcap
function import_access_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_clinical_ddp.txt mskaccess_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_clinical_ddp_age_at_seq.txt mskaccess_data_clinical_ddp_age_at_seq ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskaccess_timeline_chemotherapy_ddp; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_timeline_ddp_radiation.txt mskaccess_timeline_radiation_ddp ; then return_value=1 ; fi
    # if ! import_project_to_redcap $MSK_ACCESS_DATA_HOME/data_timeline_ddp_surgery.txt mskaccess_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for removing raw clinical and timeline files from study directory
function remove_raw_clinical_timeline_data_files {
    STUDY_DIRECTORY=$1
    # use rm -f and $HG_BINARY rm -f to ensure that both tracked and untracked
    # raw clinical and timeline files are removed from the repository

    # remove raw clinical files except patient and sample cbio format clinical files
    for f in $STUDY_DIRECTORY/data_clinical*; do
        if [[ $f != *"data_clinical_patient.txt"* && $f != *"data_clinical_sample.txt"* ]] ; then
            $GIT_BINARY rm -f $f
        fi
    done
    # remove raw timeline files except cbio format timeline file
    for f in $STUDY_DIRECTORY/data_timeline*; do
        if [[ $f != *"data_timeline.txt"* ]] ; then
            $GIT_BINARY rm -f $f
        fi
    done
}

# Function for filtering columns from derived studies' clinical data
function filter_derived_clinical_data {
    STUDY_DIRECTORY=$1
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_patient.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_sample.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST
}

# this function can be called without arguments to wait out the dmp tumor server lockout period as defined by hardcoded globals
function waitOutDmpTumorServerInstabilityPeriod() {
    waitWhileWithinTimePeriod "$CVR_TUMOR_SERVER_SAFETY_LOCKOUT_PERIOD_START_HOUR_MINUTES" "$CVR_TUMOR_SERVER_SAFETY_LOCKOUT_PERIOD_DURATION_MINUTES" "$SAFETY_LOCKOUT_SLEEP_INTERVAL_SECONDS"
    return 0
}

# this function can be called without arguments to wait out the dmp germline server lockout period as defined by hardcoded globals
function waitOutDmpGermlineServerInstabilityPeriod() {
    waitWhileWithinTimePeriod "$CVR_GERMLINE_SERVER_SAFETY_LOCKOUT_PERIOD_START_HOUR_MINUTES" "$CVR_GERMLINE_SERVER_SAFETY_LOCKOUT_PERIOD_DURATION_MINUTES" "$SAFETY_LOCKOUT_SLEEP_INTERVAL_SECONDS"
    return 0
}

# Function for selecting the earlier of two instants in ISO 8601 format (which can be compared lexigraphically if both have the same timzone offset)
function find_earlier_instant() {
    instant1="$1"
    instant2="$2"
    if [[ "$instant1" < "$instant2" ]] ; then
        echo "$instant1"
    else
        echo "$instant2"
    fi
}

# Function for consuming fetched samples after successful import
function consumeSamplesAfterSolidHemeImport {
    drop_dead_instant_string=$(date --date="+3hours" -Iseconds) # 3 hours from now
    if [ -f $MSK_IMPACT_CONSUME_TRIGGER ] ; then
        echo "Consuming mskimpact tumor samples from cvr"
        $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -c $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_data.json -z $drop_dead_instant_string
        echo "Consuming mskimpact germline samples from cvr"
        $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -g -c $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_gml_data.json -z $drop_dead_instant_string
        rm -f $MSK_IMPACT_CONSUME_TRIGGER
    fi
    if [ -f $MSK_HEMEPACT_CONSUME_TRIGGER ] ; then
        echo "Consuming mskimpact_heme samples from cvr"
        $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -c $MSK_HEMEPACT_PRIVATE_DATA_HOME/cvr_data.json -z $drop_dead_instant_string
        rm -f $MSK_HEMEPACT_CONSUME_TRIGGER
    fi
    if [ -f $MSK_ACCESS_CONSUME_TRIGGER ] ; then
        echo "Consuming mskaccess samples from cvr"
        $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -c $MSK_ACCESS_PRIVATE_DATA_HOME/cvr_data.json -z $drop_dead_instant_string
        rm -f $MSK_ACCESS_CONSUME_TRIGGER
    fi
}

# Function for consuming fetched samples after successful archer import
function consumeSamplesAfterArcherImport {
    drop_dead_instant_string=$(date --date="+3hour" -Iseconds) # 3 hour from now
    if [ -f $MSK_ARCHER_CONSUME_TRIGGER ] ; then
        echo "Consuming archer samples from cvr"
        $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -c $MSK_ARCHER_UNFILTERED_PRIVATE_DATA_HOME/cvr_data.json -z $drop_dead_instant_string
        rm -f $MSK_ARCHER_CONSUME_TRIGGER
    fi
}
