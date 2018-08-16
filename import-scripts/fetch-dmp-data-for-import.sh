#!/bin/bash

ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
PERFORM_CRDB_FETCH=1
CVR_TEST_MODE_ARGS=""
CRDB_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/crdb_fetcher.jar"
CVR_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/cvr_fetcher.jar"
DARWIN_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/darwin_fetcher.jar"
DDP_FETCHER_JAR_FILENAME="$PORTAL_HOME/lib/ddp_fetcher.jar"
REDCAP_PIPELINE_JAR_FILENAME="$PORTAL_HOME/lib/redcap_pipeline.jar"
IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/msk-dmp-importer.jar"
JAVA_CRDB_FETCHER_ARGS="-jar $CRDB_FETCHER_JAR_FILENAME"
JAVA_CVR_FETCHER_ARGS="-jar $CVR_FETCHER_JAR_FILENAME"
JAVA_DARWIN_FETCHER_ARGS="-jar $DARWIN_FETCHER_JAR_FILENAME"
JAVA_DDP_FETCHER_ARGS="-jar $DDP_FETCHER_JAR_FILENAME"
JAVA_REDCAP_PIPELINE_ARGS="-jar $REDCAP_PIPELINE_JAR_FILENAME"
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$MSK_DMP_TMPDIR"

#default darwin demographics row count is 2 to allow minimum records written to be 1 in fetched Darwin Demographics result (allow 10% drop)
DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT=2

# Clinical attribute fields which should never be filtered because of empty content
FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST="PATIENT_ID,SAMPLE_ID,ONCOTREE_CODE,PARTA_CONSENTED_12_245,PARTC_CONSENTED_12_245"

# Flags indicating whether a study can be updated
IMPORT_STATUS_IMPACT=0
IMPORT_STATUS_HEME=0
IMPORT_STATUS_RAINDANCE=0
IMPORT_STATUS_ARCHER=0

# Flags for ARCHER fusions merge failure
ARCHER_MERGE_IMPACT_FAIL=0
ARCHER_MERGE_HEME_FAIL=0

# Assume export of supp date from RedCap succeded unless it failed
EXPORT_SUPP_DATE_IMPACT_FAIL=0
EXPORT_SUPP_DATE_HEME_FAIL=0
EXPORT_SUPP_DATE_RAINDANCE_FAIL=0
EXPORT_SUPP_DATE_ARCHER_FAIL=0

# Assume fetchers have failed until they complete successfully
FETCH_CRDB_IMPACT_FAIL=1
FETCH_DARWIN_IMPACT_FAIL=1
FETCH_DARWIN_HEME_FAIL=1
FETCH_DARWIN_RAINDANCE_FAIL=1
FETCH_DARWIN_ARCHER_FAIL=1
FETCH_DDP_IMPACT_FAIL=1
FETCH_DDP_HEME_FAIL=1
FETCH_DDP_RAINDANCE_FAIL=1
FETCH_DDP_ARCHER_FAIL=1
FETCH_CVR_IMPACT_FAIL=1
FETCH_CVR_HEME_FAIL=1
FETCH_CVR_RAINDANCE_FAIL=1
FETCH_CVR_ARCHER_FAIL=1

UNLINKED_ARCHER_SUBSET_FAIL=0
MIXEDPACT_MERGE_FAIL=0
MSK_SOLID_HEME_MERGE_FAIL=0
MSK_KINGS_SUBSET_FAIL=0
MSK_QUEENS_SUBSET_FAIL=0
MSK_LEHIGH_SUBSET_FAIL=0
MSK_MCI_SUBSET_FAIL=0
MSK_HARTFORD_SUBSET_FAIL=0
MSK_RALPHLAUREN_SUBSET_FAIL=0
MSK_TAILORMEDJAPAN_SUBSET_FAIL=0
MSKIMPACT_PED_SUBSET_FAIL=0
SCLC_MSKIMPACT_SUBSET_FAIL=0
LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=0

GENERATE_MASTERLIST_FAIL=0

## FUNCTIONS

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines pre-import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" https://hooks.slack.com/services/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
}

# Function to generate case lists by cancer type
function addCancerTypeCaseLists {
    STUDY_DATA_DIRECTORY=$1
    STUDY_ID=$2
    # accept 1 or 2 data_clinical filenames
    FILENAME_1="$3"
    FILENAME_2="$4"
    FILEPATH_1="$STUDY_DATA_DIRECTORY/$FILENAME_1"
    FILEPATH_2="$STUDY_DATA_DIRECTORY/$FILENAME_2"
    CLINICAL_FILE_LIST="$FILEPATH_1, $FILEPATH_2"
    if [ -z "$FILENAME_2" ] ; then
        CLINICAL_FILE_LIST="$FILEPATH_1"
    fi
    # remove current case lists and run oncotree converter before creating new cancer case lists
    rm $STUDY_DATA_DIRECTORY/case_lists/*
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
    filename=$1
    project_title=$2
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -i --filename $filename --redcap-project-title $project_title
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to import file $filename into redcap project $project_title"
        return 1
    fi
}

# Function for exporting redcap project
function export_project_from_redcap {
    directory=$1
    project_title=$2
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -d $directory --redcap-project-title $project_title
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to export project $project_title from redcap into directory $directory"
        return 1
    fi
}

# Function for exporting redcap projects (merged to standard cbioportal format) by stable id
function export_stable_id_from_redcap {
    stable_id=$1
    directory=$2
    list_of_ignored_projects=$3
    ignored_projects_argument=""
    if [ ! -z $list_of_ignored_projects ] ; then
        ignored_projects_argument="-m $list_of_ignored_projects"
    fi
    $JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -s ${stable_id} -d ${directory} ${ignored_projects_argument}
    if [ $? -gt 0 ] ; then
        #log error
        echo "Failed to export stable_id ${stable_id} from redcap into directory ${directory}"
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
function import_mskimpact_darwin_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_supp_caisis_gbm.txt mskimpact_clinical_caisis ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_supp_darwin_demographics.txt mskimpact_data_clinical_darwin_demographics ; then return_value=1 ; fi
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
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskimpact_timeline_chemotherapy_ddp; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_radiation.txt mskimpact_timeline_radiation_ddp ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_timeline_ddp_surgery.txt mskimpact_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing hemepact darwin files to redcap
function import_hemepact_darwin_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_supp_darwin_demographics.txt hemepact_data_clinical_supp_darwin_demographics ; then return_value=1 ; fi
    return $return_value
}

# Function for importing hemepact cvr files to redcap
function import_hemepact_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt hemepact_data_clinical ; then return_value=1 ; fi
    return $return_value
}

# Fucntion for importing hemepact supp date files to redcap
function import_hemepact_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt hemepact_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for import hemepact ddp files to redcap
function import_hemepact_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_ddp.txt hemepact_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt hemepact_timeline_chemotherapy_ddp; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_radiation.txt hemepact_timeline_radiation_ddp ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_timeline_ddp_surgery.txt hemepact_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing raindance darwin files to redcap
function import_raindance_darwin_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_clinical_supp_darwin_demographics.txt mskraindance_data_clinical_supp_darwin_demographics ; then return_value=1 ; fi
    return $return_value
}

# Function for importing raindance cvr files to redcap
function import_raindance_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt mskraindance_data_clinical ; then return_value=1 ; fi
    return $return_value
}

# Function for importin raindance supp date files to redcap
function import_raindance_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt mskraindance_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for import raindance ddp files to redcap
function import_raindance_ddp_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_clinical_ddp.txt mskraindance_data_clinical_ddp_demographics ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskraindance_timeline_chemotherapy_ddp; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_timeline_ddp_radiation.txt mskraindance_timeline_radiation_ddp ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_timeline_ddp_surgery.txt mskraindance_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for importing archer darwin files to redcap
function import_archer_darwin_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_supp_darwin_demographics.txt mskarcher_data_clinical_supp_darwin_demographics ; then return_value=1 ; fi
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
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_chemotherapy.txt mskarcher_timeline_chemotherapy_ddp; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_radiation.txt mskarcher_timeline_radiation_ddp ; then return_value=1 ; fi
    if ! import_project_to_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME/data_timeline_ddp_surgery.txt mskarcher_data_timeline_surgery_ddp ; then return_value=1 ; fi
    return $return_value
}

# Function for removing raw clinical and timeline files from study directory
function remove_raw_clinical_timeline_data_files {
    STUDY_DIRECTORY=$1
    # remove raw clinical files except patient and sample cbio format clinical files
    for f in $STUDY_DIRECTORY/data_clinical*; do
        if [[ $f != *"data_clinical_patient.txt"* && $f != *"data_clinical_sample.txt"* ]] ; then
            rm -f $f
        fi
    done
    # remove raw timeline files except cbio format timeline file
    for f in $STUDY_DIRECTORY/data_timeline*; do
        if [ $f != *"data_timeline.txt"* ] ; then
            rm -f $f
        fi
    done
}

# Function for filtering columns from derived studies' clinical data
function filter_derived_clinical_data {
    STUDY_DIRECTORY=$1
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_patient.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST &&
    $PYTHON_BINARY $PORTAL_HOME/scripts/filter_empty_columns.py --file $STUDY_DIRECTORY/data_clinical_sample.txt --keep-column-list $FILTER_EMPTY_COLUMNS_KEEP_COLUMN_LIST
}

# -----------------------------------------------------------------------------------------------------------
echo $(date)

email_list="cbioportal-pipelines@cbio.mskcc.org"

if [[ -d "$MSK_DMP_TMPDIR" && "$MSK_DMP_TMPDIR" != "/" ]] ; then
    rm -rf "$MSK_DMP_TMPDIR"/*
fi

if [ -z $JAVA_BINARY ] | [ -z $HG_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_IMPACT_DATA_HOME ] ; then
    message="test could not run import-dmp-impact.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_IMPACT_DATA_HOME, ...)"
    echo ${message}
    echo -e "${message}" |  mail -s "fetch-dmp-data-for-import failed to run." $email_list
    sendFailureMessageMskPipelineLogsSlack "${message}"
    exit 2
fi

# refresh cdd and oncotree cache - by default this script will attempt to
# refresh the CDD and ONCOTREE cache but we should check both exit codes
# independently because of the various dependencies we have for both services
CDD_RECACHE_FAIL=0; ONCOTREE_RECACHE_FAIL=0
bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --cdd-only
if [ $? -gt 0 ]; then
    message="Failed to refresh CDD cache!"
    echo $message
    echo -e "$message" | mail -s "CDD cache failed to refresh" $email_list
    sendFailureMessageMskPipelineLogsSlack "$message"
    CDD_RECACHE_FAIL=1
fi
bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --oncotree-only
if [ $? -gt 0 ]; then
    message="Failed to refresh ONCOTREE cache!"
    echo $message
    echo -e "$message" | mail -s "ONCOTREE cache failed to refresh" $email_list
    sendFailureMessageMskPipelineLogsSlack "$message"
    ONCOTREE_RECACHE_FAIL=1
fi
if [[ $CDD_RECACHE_FAIL -gt 0 || $ONCOTREE_RECACHE_FAIL -gt 0 ]] ; then
    echo "Oncotree and/or CDD recache failed! Exiting..."
    exit 2
fi

# fetch clinical data mercurial
echo "fetching updates from dmp repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source dmp --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch Msk-impact from Mercurial Failure"
    exit 2
fi

# -------------------------------- pre-data-fetch redcap exports -----------------------------------

# export data_clinical and data_clinical_supp_date for each project from redcap (starting point)
# if export fails:
# don't re-import into redcap (would wipe out days worth of data)
# consider failing import

echo "exporting impact data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_supp_date_cbioportal_added
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_IMPACT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap export of mskimpact_supp_date_cbioportal_added"
fi

echo "exporting hemepact data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_HEME_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap export of hemepact_data_clinical_supp_date"
fi

echo "exporting raindance data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_RAINDANCE_DATA_HOME mskraindance_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_RAINDANCE_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap export of mskraindance_data_clinical_supp_date"
fi

echo "exporting archer data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME mskarcher_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_ARCHER_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "ARCHER Redcap export of mskarcher_data_clinical_supp_date"
fi

# IF WE CANCEL ANY IMPORT, LET REDCAP GET AHEAD OF CURRENCY, BUT DON'T LET MERCURIAL ADVANCE [REVERT]

echo "exporting impact data_clinical.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_cvr
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_IMPACT=1
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap export of mskimpact_data_clinical_cvr"
fi

echo "exporting heme data_clinical.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_HEME=1
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap export of hemepact_data_clinical_cvr"
fi

echo "exporting raindance data_clinical.txt from redcap"
export_project_from_redcap $MSK_RAINDANCE_DATA_HOME mskraindance_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_RAINDANCE=1
    sendFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap export of mskraindance_data_clinical_cvr"
fi

echo "exporting archer data_clinical.txt from redcap"
export_project_from_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME mskarcher_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_ARCHER=1
    sendFailureMessageMskPipelineLogsSlack "ARCHER Redcap export of mskarcher_data_clinical_cvr"
fi

# -------------------------------- all mskimpact project data fetches -----------------------------------
# TODO: move other pre-import/data-fetch steps here (i.e exporting raw files from redcap)

# fetch Darwin data
echo "fetching Darwin impact data"
echo $(date)
# if darwin demographics row count less than or equal to default row count then set it to default value
MSKIMPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSKIMPACT_REDCAP_BACKUP/data_clinical_mskimpact_data_clinical_darwin_demographics.txt)
if [ $MSKIMPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT ] ; then
    MSKIMPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT
fi
$JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -s mskimpact -c $MSKIMPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT DARWIN Fetch"
else
    FETCH_DARWIN_IMPACT_FAIL=0
    echo "committing darwin data"
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: Darwin"
fi

if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    # fetch new/updated IMPACT samples using CVR Web service   (must come after mercurial fetching)
    echo "fetching samples from CVR Web service  ..."
    echo $(date)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -i mskimpact -r 150 $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR fetch failed!"
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Fetch"
        IMPORT_STATUS_IMPACT=1
    else
        # sanity check for empty allele counts
        bash $PORTAL_HOME/scripts/test_if_impact_has_lost_allele_count.sh
        if [ $? -gt 0 ] ; then
            echo "Empty allele count sanity check failed! MSK-IMPACT will not be imported!"
            cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
            sendFailureMessageMskPipelineLogsSlack "MSKIMPACT empty allele count sanity check"
            IMPORT_STATUS_IMPACT=1
        else
            # check for PHI
            $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_IMPACT_DATA_HOME/cvr_data.json
            if [ $? -gt 0 ] ; then
                echo "PHI attributes found in $MSK_IMPACT_DATA_HOME/cvr_data.json! MSK-IMPACT will not be imported!"
                cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
                sendFailureMessageMskPipelineLogsSlack "MSKIMPACT PHI attributes scan failed on $MSK_IMPACT_DATA_HOME/cvr_data.json"
                IMPORT_STATUS_IMPACT=1
            else
                FETCH_CVR_IMPACT_FAIL=0
                echo "committing cvr data"
                cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: CVR"
            fi
        fi
    fi

    # fetch new/updated IMPACT germline samples using CVR Web service   (must come after normal cvr fetching)
    echo "fetching CVR GML data ..."
    echo $(date)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -g -i mskimpact $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Germline fetch failed!"
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Germline Fetch"
        IMPORT_STATUS_IMPACT=1
        #override the success of the tumor sample cvr fetch with a failed status
        FETCH_CVR_IMPACT_FAIL=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_IMPACT_DATA_HOME/cvr_gml_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_IMPACT_DATA_HOME/cvr_gml_data.json! MSK-IMPACT will not be imported!"
            cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
            sendFailureMessageMskPipelineLogsSlack "MSKIMPACT PHI attributes scan failed on $MSK_IMPACT_DATA_HOME/cvr_gml_data.json"
            IMPORT_STATUS_IMPACT=1
            #override the success of the tumor sample cvr fetch with a failed status
            FETCH_CVR_IMPACT_FAIL=1
        else
            echo "committing CVR germline data"
            cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: CVR Germline"
        fi
    fi
fi

if [ $FETCH_DARWIN_IMPACT_FAIL -eq 0 ] ; then
    awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PED_IND"] == "Yes") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_DATA_HOME/data_clinical_supp_darwin_demographics.txt | sort | uniq > $MSK_DMP_TMPDIR/mskimpact_ped_patient_list.txt
    # For future use: when we want to replace darwin demographics attributes with ddp-fetched attributes instead of just for ped-cohort
    #awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | sort | uniq > $MSK_DMP_TMPDIR/mskimpact_patient_list.txt
    $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -o $MSK_IMPACT_DATA_HOME -s $MSK_DMP_TMPDIR/mskimpact_ped_patient_list.txt
    if [ $? -gt 0 ] ; then
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT DDP Fetch"
    else
        FETCH_DDP_IMPACT_FAIL=0
        echo "committing DDP data"
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: DDP demographics/timeline"
    fi
fi

if [ $PERFORM_CRDB_FETCH -gt 0 ] ; then
    # fetch CRDB data
    echo "fetching CRDB data"
    echo $(date)
    $JAVA_BINARY $JAVA_CRDB_FETCHER_ARGS --directory $MSK_IMPACT_DATA_HOME
    # no need for hg update/commit ; CRDB generated files are stored in redcap and not mercurial
    if [ $? -gt 0 ] ; then
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT CRDB Fetch"
    else
        FETCH_CRDB_IMPACT_FAIL=0
    fi
else
    FETCH_CRDB_IMPACT_FAIL=0
fi

# -------------------------------- all hemepact project data fetches -----------------------------------

echo "fetching Darwin heme data"
echo $(date)
# if darwin demographics row count less than or equal to default row count then set it to default value
HEMEPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $HEMEPACT_REDCAP_BACKUP/data_clinical_hemepact_data_clinical_supp_darwin_demographics.txt)
if [ $HEMEPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT ] ; then
    HEMEPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT
fi
$JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -d $MSK_HEMEPACT_DATA_HOME -s mskimpact_heme -c $HEMEPACT_DARWIN_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT DARWIN Fetch"
else
    FETCH_DARWIN_HEME_FAIL=0
    echo "committing darwin data for heme"
    cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Darwin"
fi

if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    # fetch new/updated heme samples using CVR Web service (must come after mercurial fetching). Threshold is set to 50 since heme contains only 190 samples (07/12/2017)
    echo "fetching CVR heme data..."
    echo $(date)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_HEMEPACT_DATA_HOME -n data_clinical_hemepact_data_clinical.txt -i mskimpact_heme -r 50 $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR heme fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT CVR Fetch"
        IMPORT_STATUS_HEME=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_HEMEPACT_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_HEMEPACT_DATA_HOME/cvr_data.json! HEMEPACT will not be imported!"
            cd $MSK_HEMEPACT_DATA_HOME; $HG_BINARY update -C ; find . -name "*.orig" -delete
            sendFailureMessageMskPipelineLogsSlack "HEMEPACT PHI attributes scan failed on $MSK_HEMEPACT_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_HEME=1
        else
            FETCH_CVR_HEME_FAIL=0
            echo "committing cvr data for heme"
            cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY commit -m "Latest HEMEPACT dataset"
        fi
    fi
fi

# For future use: when we want to replace darwin demographics attributes with ddp-fetched attributes
#if [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
#    awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt | sort | uniq > $MSK_DMP_TMPDIR/hemepact_patient_list.txt
#    $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -o $MSK_HEMEPACT_DATA_HOME -s $MSK_DMP_TMPDIR/hemepact_patient_list.txt
#    if [ $? -gt 0 ] ; then
#        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
#        sendFailureMessageMskPipelineLogsSlack "HEMEPACT DDP Fetch"
#    else
#        FETCH_DDP_HEME_FAIL=0
#        echo "committing DDP data for heme"
#        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: DDP demographics/timeline"
#    fi
#fi

# -------------------------------- all raindance project data fetches -----------------------------------

echo "fetching Darwin raindance data"
echo $(date)
# if darwin demographics row count less than or equal to default row count then set it to default value
RAINDANCE_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $RAINDANCE_REDCAP_BACKUP/data_clinical_mskraindance_data_clinical_supp_darwin_demographics.txt)
if [ $RAINDANCE_DARWIN_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT ] ; then
    RAINDANCE_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT
fi
$JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -d $MSK_RAINDANCE_DATA_HOME -s mskraindance -c $RAINDANCE_DARWIN_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "RAINDANCE DARWIN Fetch"
else
    FETCH_DARWIN_RAINDANCE_FAIL=0
    echo "commting darwin data for raindance"
    cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Darwin"
fi

if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    # fetch new/updated raindance samples using CVR Web service (must come after mercurial fetching). The -s flag skips segment data fetching
    echo "fetching CVR raindance data..."
    echo $(date)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_RAINDANCE_DATA_HOME -n data_clinical_mskraindance_data_clinical.txt -s -i mskraindance $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR raindance fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "RAINDANCE CVR Fetch"
        IMPORT_STATUS_RAINDANCE=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_RAINDANCE_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_RAINDANCE_DATA_HOME/cvr_data.json! RAINDANCE will not be imported!"
            cd $MSK_RAINDANCE_DATA_HOME; $HG_BINARY update -C ; find . -name "*.orig" -delete
            sendFailureMessageMskPipelineLogsSlack "RAINDANCE PHI attributes scan failed on $MSK_RAINDANCE_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_RAINDANCE=1
        else
            FETCH_CVR_RAINDANCE_FAIL=0
            cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY commit -m "Latest RAINDANCE dataset"
        fi
    fi
fi

# For future use: when we want to replace darwin demographics attributes with ddp-fetched attributes
#if [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
#    awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt | sort | uniq > $MSK_DMP_TMPDIR/mskraindance_patient_list.txt
#    $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -o $MSK_RAINDANCE_DATA_HOME -s $MSK_DMP_TMPDIR/mskraindance_patient_list.txt
#    if [ $? -gt 0 ] ; then
#        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
#        sendFailureMessageMskPipelineLogsSlack "RAINDANCE DDP Fetch"
#    else
#        FETCH_DDP_RAINDANCE_FAIL=0
#        echo "committing DDP data for raindance"
#        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: DDP demographics/timeline"
#    fi
#fi

# -------------------------------- all archer project data fetches -----------------------------------

echo "fetching Darwin archer data"
echo $(date)
# if darwin demographics row count less than or equal to default row count then set it to default value
ARCHER_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $ARCHER_REDCAP_BACKUP/data_clinical_mskarcher_data_clinical_supp_darwin_demographics.txt)
if [ $ARCHER_DARWIN_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT ] ; then
    ARCHER_DARWIN_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DARWIN_DEMOGRAPHICS_ROW_COUNT
fi
$JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -d $MSK_ARCHER_UNFILTERED_DATA_HOME -s mskarcher -c $ARCHER_DARWIN_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED DARWIN Fetch"
else
    FETCH_DARWIN_ARCHER_FAIL=0
    echo " committing darwin data for archer"
    cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Darwin"
fi

if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    # fetch new/updated archer samples using CVR Web service (must come after mercurial fetching).
    echo "fetching CVR archer data..."
    echo $(date)
    # archer has -b option to block warnings for samples with zero variants (all samples will have zero variants)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_ARCHER_UNFILTERED_DATA_HOME -n data_clinical_mskarcher_data_clinical.txt -i mskarcher -s -b $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Archer fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED CVR Fetch"
        IMPORT_STATUS_ARCHER=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr_data.json! UNLINKED_ARCHER will not be imported!"
            cd $MSK_ARCHER_UNFILTERED_DATA_HOME; $HG_BINARY update -C ; find . -name "*.orig" -delete
            sendFailureMessageMskPipelineLogsSlack "ARCHER PHI attributes scan failed on $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_ARCHER=1
        else
            FETCH_CVR_ARCHER_FAIL=0
            # renaming gene matrix file until we get the mskarcher gene panel imported
            cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; mv data_gene_matrix.txt ignore_data_gene_matrix.txt
            cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY commit -m "Latest ARCHER_UNFILTERED dataset"
        fi
    fi
fi

# For future use: when we want to replace darwin demographics attributes with ddp-fetched attributes
#if [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] ; then
#    awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt | sort | uniq > $MSK_DMP_TMPDIR/mskarcher_patient_list.txt
#    $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -o $MSK_ARCHER_UNFILTERED_DATA_HOME -s $MSK_DMP_TMPDIR/mskarcher_patient_list.txt
#    if [ $? -gt 0 ] ; then
#        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
#        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED DDP Fetch"
#    else
#        FETCH_DDP_ARCHER_FAIL=0
#        echo "committing DDP data for archer"
#        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: DDP demographics/timeline"
#    fi
#fi

# -------------------------------- Generate all case lists and supp date files -----------------------------------

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for MSK-IMPACT
if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact" "data_clinical_mskimpact_data_clinical_cvr.txt"
    cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY revert data_clinical* data_timeline* --no-backup ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_IMPACT_DATA_HOME "data_clinical_mskimpact_data_clinical_cvr.txt" "data_clinical_mskimpact_supp_date_cbioportal_added.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for HEMEPACT
if [ $IMPORT_STATUS_HEME -eq 0 ] && [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_HEMEPACT_DATA_HOME "mskimpact_heme" "data_clinical_hemepact_data_clinical.txt"
    cd $MSK_HEMEPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY revert data_clinical* data_timeline* --no-backup ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_HEME_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_HEMEPACT_DATA_HOME "data_clinical_hemepact_data_clinical.txt" "data_clinical_hemepact_data_clinical_supp_date.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for RAINDANCE
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] && [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_RAINDANCE_DATA_HOME "mskraindance" "data_clinical_mskraindance_data_clinical.txt"
    cd $MSK_RAINDANCE_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY revert data_clinical* data_timeline* --no-backup ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_RAINDANCE_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_RAINDANCE_DATA_HOME "data_clinical_mskraindance_data_clinical.txt" "data_clinical_mskraindance_data_clinical_supp_date.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for ARCHER
if [[ $IMPORT_STATUS_ARCHER -eq 0 && $FETCH_CVR_ARCHER_FAIL -eq 0 && $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ]] ; then
    addDateAddedData $MSK_ARCHER_UNFILTERED_DATA_HOME "data_clinical_mskarcher_data_clinical.txt" "data_clinical_mskarcher_data_clinical_supp_date.txt"
fi

# -------------------------------- Additional processing -----------------------------------
# we maintain a file with the list of ARCHER samples to exclude from any merges/subsets involving
# ARCHER data to prevent duplicate fusion events ($MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_fusion_samples.txt)
MAPPED_ARCHER_FUSION_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_fusion_samples.txt

# add linked ARCHER_UNFILTERED fusions to MSKIMPACT
if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    # Merge ARCHER_UNFILTERED fusion data into the MSKIMPACT cohort
    $PYTHON_BINARY $PORTAL_HOME/scripts/archer_fusions_merger.py --archer-fusions $MSK_ARCHER_UNFILTERED_DATA_HOME/data_fusions.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --fusions-filename $MSK_IMPACT_DATA_HOME/data_fusions.txt --clinical-filename $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt --mapped-archer-samples-filename $MAPPED_ARCHER_FUSION_SAMPLES_FILE --study-id "mskimpact"
    if [ $? -gt 0 ] ; then
        ARCHER_MERGE_IMPACT_FAIL=1
        cd $MSK_IMPACT_DATA_HOME; $HG_BINARY update -C ; find . -name "*.orig" -delete
    else
        cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Adding ARCHER_UNFILTERED fusions to MSKIMPACT"
    fi
fi

# add linked ARCHER_UNFILTERED fusions to HEMEPACT
if [ $IMPORT_STATUS_HEME -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] && [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    # Merge ARCHER_UNFILTERED fusion data into the HEMEPACT cohort
    $PYTHON_BINARY $PORTAL_HOME/scripts/archer_fusions_merger.py --archer-fusions $MSK_ARCHER_UNFILTERED_DATA_HOME/data_fusions.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --fusions-filename $MSK_HEMEPACT_DATA_HOME/data_fusions.txt --clinical-filename $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt --mapped-archer-samples-filename $MAPPED_ARCHER_FUSION_SAMPLES_FILE --study-id "mskimpact_heme"
    if [ $? -gt 0 ] ; then
        ARCHER_MERGE_HEME_FAIL=1
        cd $MSK_HEMEPACT_DATA_HOME; $HG_BINARY update -C ; find . -name "*.orig" -delete
    else
        cd $MSK_HEMEPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Adding ARCHER_UNFILTERED fusions to HEMEPACT"
    fi
fi

# gets index of SAMPLE_ID from file (used in case SAMPLE_ID index changes)
SAMPLE_ID_COLUMN=`sed -n $"1s/\t/\\\n/gp" $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | grep -nx "SAMPLE_ID" | cut -d: -f1`
# generates sample masterlist for filtering dropped samples/patients from supp files
SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME="$MSK_DMP_TMPDIR/sample_masterlist_for_filtering.txt"
grep -v '^#' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | awk -v SAMPLE_ID_INDEX="$SAMPLE_ID_COLUMN" -F '\t' '{if ($SAMPLE_ID_INDEX != "SAMPLE_ID") print $SAMPLE_ID_INDEX;}' > $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME
if [ $(wc -l < $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME) -eq 0 ] ; then
    echo "ERROR! Sample masterlist $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME is empty. Skipping patient/sample filtering for mskimpact!"
    GENERATE_MASTERLIST_FAIL=1
fi

# -------------------------------- Import projects into redcap -----------------------------------

# import newly fetched files into redcap
echo "Starting import into redcap"

if [ $PERFORM_CRDB_FETCH -gt 0 ] && [ $FETCH_CRDB_IMPACT_FAIL -eq 0 ] ; then
    import_crdb_to_redcap
    if [ $? -gt 0 ] ; then
        #NOTE: we have decided to allow import of mskimpact project to proceed even when CRDB data has been lost from redcap (not setting IMPORT_STATUS_IMPACT)
        sendFailureMessageMskPipelineLogsSlack "Mskimpact CRDB Redcap Import - Recovery Of Redcap Project Needed!"
    fi
fi

# imports mskimpact darwin data into redcap
if [ $FETCH_DARWIN_IMPACT_FAIL -eq 0 ] ; then
    import_mskimpact_darwin_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendFailureMessageMskPipelineLogsSlack "Mskimpact Darwin Redcap Import"
    fi
fi

# imports mskimpact ddp data into redcap
if [ $FETCH_DDP_IMPACT_FAIL -eq 0 ] ; then
    import_mskimpact_ddp_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendFailureMessageMskPipelineLogsSlack "Mskimpact DDP Redcap Import"
    fi
fi

# imports mskimpact cvr data into redcap
if [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    import_mskimpact_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendFailureMessageMskPipelineLogsSlack "Mskimpact CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
        import_mskimpact_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendFailureMessageMskPipelineLogsSlack "Mskimpact Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi

# imports hemepact darwin data into redcap
if [ $FETCH_DARWIN_HEME_FAIL -eq 0 ] ; then
    import_hemepact_darwin_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        sendFailureMessageMskPipelineLogsSlack "Hemepact Darwin Redcap Import"
    fi
fi

# For future use: imports hemepact ddp data into redcap
#if [ $FETCH_DDP_HEME_FAIL -eq 0 ] ; then
#    import_hemepact_ddp_to_redcap
#    if [$? -gt 0 ] ; then
#        IMPORT_STATUS_HEME=1
#        sendFailureMessageMskPipelineLogsSlack "Hemepact DDP Redcap Import"
#    fi
#fi

# imports hemepact cvr data into redcap
if [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    import_hemepact_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        sendFailureMessageMskPipelineLogsSlack "Hemepact CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_HEME_FAIL -eq 0 ] ; then
        import_hemepact_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendFailureMessageMskPipelineLogsSlack "Mskimpact Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi

# imports archer darwin data into redcap
if [ $FETCH_DARWIN_ARCHER_FAIL -eq 0 ] ; then
    import_archer_darwin_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED Darwin Redcap Import"
    fi
fi

# For future use: imports archer ddp data into redcap
#if [ $FETCH_DDP_ARCHER_FAIL -eq 0 ] ; then
#    import_archer_ddp_to_redcap
#    if [$? -gt 0 ] ; then
#        IMPORT_STATUS_ARCHER=1
#        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED DDP Redcap Import"
#    fi
#fi

# imports archer cvr data into redcap
if [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] ; then
    import_archer_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ] ; then
        import_archer_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi

# imports raindance darwin data into redcap
if [ $FETCH_DARWIN_RAINDANCE_FAIL -eq 0 ] ; then
    import_raindance_darwin_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        sendFailureMessageMskPipelineLogsSlack "Raindance Darwin Redcap Import"
    fi
fi

# For future use: imports raindance ddp data into redcap
#if [ $FETCH_DDP_RAINDANCE_FAIL -eq 0 ] ; then
#    import_raindance_ddp_to_redcap
#    if [$? -gt 0 ] ; then
#        IMPORT_STATUS_RAINDANCE=1
#        sendFailureMessageMskPipelineLogsSlack "RAINDANCE DDP Redcap Import"
#    fi
#fi

# imports raindance cvr data into redcap
if [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
    import_raindance_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        sendFailureMessageMskPipelineLogsSlack "Raindance CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_RAINDANCE_FAIL -eq 0 ] ; then
        import_raindance_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendFailureMessageMskPipelineLogsSlack "Raindance Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi
echo "Import into redcap finished"

# -------------------------------------------------------------
# remove extraneous files
echo "removing raw clinical & timeline files for mskimpact"
remove_raw_clinical_timeline_data_files $MSK_IMPACT_DATA_HOME

echo "removing raw clinical & timeline files for hemepact"
remove_raw_clinical_timeline_data_files $MSK_HEMEPACT_DATA_HOME

echo "removing raw clinical & timeline files for mskraindance"
remove_raw_clinical_timeline_data_files $MSK_RAINDANCE_DATA_HOME

echo "removing raw clinical & timeline files for mskarcher"
remove_raw_clinical_timeline_data_files $MSK_ARCHER_UNFILTERED_DATA_HOME

# -------------------------------------------------------------
# export data in standard cbioportal mode from redcap

echo "exporting impact data from redcap"
if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_ddp_demographics,mskimpact_timeline_radiation_ddp,mskimpact_timeline_chemotherapy_ddp,mskimpact_timeline_surgery_ddp
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap Export"
    else
        if [ $GENERATE_MASTERLIST_FAIL -eq 0 ] ; then
            $PYTHON_BINARY $PORTAL_HOME/scripts/filter_dropped_samples_patients.py -s $MSK_IMPACT_DATA_HOME/data_clinical_sample.txt -p $MSK_IMPACT_DATA_HOME/data_clinical_patient.txt -t $MSK_IMPACT_DATA_HOME/data_timeline.txt -f $MSK_DMP_TMPDIR/sample_masterlist_for_filtering.txt
            if [ $? -gt 0 ] ; then
                cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
            else
                cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest MSKIMPACT Dataset: Clinical and Timeline"
            fi
        fi
        touch $MSK_IMPACT_CONSUME_TRIGGER
    fi
fi

echo "exporting hemepact data from redcap"
if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact_heme $MSK_HEMEPACT_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap Export"
    else
        touch $MSK_HEMEPACT_CONSUME_TRIGGER
        cd $MSK_HEMEPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Clinical and Timeline"
    fi
fi

echo "exporting raindance data from redcap"
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    export_stable_id_from_redcap mskraindance $MSK_RAINDANCE_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap Export"
    else
        touch $MSK_RAINDANCE_IMPORT_TRIGGER
        cd $MSK_RAINDANCE_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Clinical and Timeline"
    fi
fi

echo "exporting archer data from redcap"
if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    export_stable_id_from_redcap mskarcher $MSK_ARCHER_UNFILTERED_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED Redcap Export"
    else
        touch $MSK_ARCHER_IMPORT_TRIGGER
        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Clinical and Timeline"
    fi
fi

# process data for UNLINKED_ARCHER
if [[ $IMPORT_STATUS_ARCHER -eq 0 && $FETCH_CVR_ARCHER_FAIL -eq 0 ]] ; then
    # attempt to subset archer unfiltered w/same excluded archer samples list used for MIXEDPACT, MSKSOLIDHEME
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_ARCHER_DATA_HOME -i mskarcher -m "true" -e $MAPPED_ARCHER_FUSION_SAMPLES_FILE $MSK_ARCHER_UNFILTERED_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "UNLINKED_ARCHER subset failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "UNLINKED_ARCHER subset"
        echo $(date)
        cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        UNLINKED_ARCHER_SUBSET_FAIL=1
    else
        echo "UNLINKED_ARCHER subset successful! Creating cancer type case lists..."
        echo $(date)
        # add metadata headers and overrides before importing
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskarcher -f $MSK_ARCHER_DATA_HOME/data_clinical*
        if [ $? -gt 0 ] ; then
            echo "Error: Adding metadata headers for UNLINKED_ARCHER failed! Study will not be updated in portal."
            cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        else
            # commit updates and generated case lists
            cd $MSK_ARCHER_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest UNLINKED_ARCHER Dataset"
            addCancerTypeCaseLists $MSK_ARCHER_DATA_HOME "mskarcher" "data_clinical_sample.txt" "data_clinical_patient.txt"
            cd $MSK_ARCHER_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY revert data_clinical* data_timeline* --no-backup ; $HG_BINARY commit -m "Latest UNLINKED_ARCHER Dataset: Case Lists"
            touch $MSK_ARCHER_IMPORT_TRIGGER
        fi
    fi
fi
#--------------------------------------------------------------
## Merge studies for MIXEDPACT, MSKSOLIDHEME:
#   (1) MSK-IMPACT, HEMEPACT, RAINDANCE, and ARCHER (MIXEDPACT)
#   (1) MSK-IMPACT, HEMEPACT, and ARCHER (MSKSOLIDHEME)

# touch meta_SV.txt files if not already exist
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_ARCHER_UNFILTERED_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_ARCHER_UNFILTERED_DATA_HOME/meta_SV.txt
fi

echo "Beginning merge of MSK-IMPACT, HEMEPACT, RAINDANCE, ARCHER data for MIXEDPACT..."
echo $(date)

# MIXEDPACT merge and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_MIXEDPACT_DATA_HOME -i mixedpact -m "true" -e $MAPPED_ARCHER_FUSION_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_RAINDANCE_DATA_HOME $MSK_ARCHER_UNFILTERED_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MIXEDPACT merge failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "MIXEDPACT merge"
    echo $(date)
    MIXEDPACT_MERGE_FAIL=1
else
    echo "MIXEDPACT merge successful!"
    echo $(date)
fi

if [ $MIXEDPACT_MERGE_FAIL -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "MIXEDPACT merge"
    echo "MIXEDPACT merge failed! Reverting data to last commit."
    cd $MSK_MIXEDPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    sendSuccessMessageMskPipelineLogsSlack "MIXEDPACT merge"
    echo "Committing MIXEDPACT data"
    cd $MSK_MIXEDPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest MIXEDPACT dataset"
fi

echo "Beginning merge of MSK-IMPACT, HEMEPACT, RAINDANCE, ARCHER data for MSKSOLIDHEME..."
echo $(date)

# MSKSOLIDHEME merge and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_SOLID_HEME_DATA_HOME -i mskimpact -m "true" -e $MAPPED_ARCHER_FUSION_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MSKSOLIDHEME merge failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "MSKSOLIDHEME merge"
    echo $(date)
    MSK_SOLID_HEME_MERGE_FAIL=1
    # we rollback/clean mercurial after the import of MSKSOLIDHEME (if merge or import fails)
else
    echo "MSKSOLIDHEME merge successful! Creating cancer type case lists..."
    echo $(date)
    # add metadata headers and overrides before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        echo "Error: Adding metadata headers for MSKSOLIDHEME failed! Study will not be updated in portal."
    else
        touch $MSK_SOLID_HEME_IMPORT_TRIGGER
    fi
    addCancerTypeCaseLists $MSK_SOLID_HEME_DATA_HOME "mskimpact" "data_clinical_sample.txt" "data_clinical_patient.txt"
fi

# check that meta_SV.txt are actually empty files before deleting from IMPACT, HEME, and ARCHER studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_ARCHER_UNFILTERED_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_ARCHER_UNFILTERED_DATA_HOME/meta_SV.txt
fi

#--------------------------------------------------------------
## Subset MIXEDPACT on INSTITUTE for institute specific impact studies

# subset the mixedpact study for Queens Cancer Center, Lehigh Valley, Kings County Cancer Center, Miami Cancer Institute, and Hartford Health Care
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_kingscounty -o=$MSK_KINGS_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Kings County Cancer Center" -s=$MSK_DMP_TMPDIR/kings_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Kings County subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "KINGSCOUNTY subset"
    MSK_KINGS_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_KINGS_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Kings County subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "KINGSCOUNTY subset"
        MSK_KINGS_SUBSET_FAIL=1
    else
        echo "MSK Kings County subset successful!"
        addCancerTypeCaseLists $MSK_KINGS_DATA_HOME "msk_kingscounty" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_KINGS_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_lehighvalley -o=$MSK_LEHIGH_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Lehigh Valley Health Network" -s=$MSK_DMP_TMPDIR/lehigh_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Lehigh Valley subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY subset"
    MSK_LEHIGH_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_LEHIGH_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Lehigh Valley subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY subset"
        MSK_LEHIGH_SUBSET_FAIL=1
    else
        echo "MSK Lehigh Valley subset successful!"
        addCancerTypeCaseLists $MSK_LEHIGH_DATA_HOME "msk_lehighvalley" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_LEHIGH_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_queenscancercenter -o=$MSK_QUEENS_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Queens Cancer Center,Queens Hospital Cancer Center" -s=$MSK_DMP_TMPDIR/queens_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Queens Cancer Center subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER subset"
    MSK_QUEENS_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_QUEENS_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Queens Cancer Center subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER subset"
        MSK_QUEENS_SUBSET_FAIL=1
    else
        echo "MSK Queens Cancer Center subset successful!"
        addCancerTypeCaseLists $MSK_QUEENS_DATA_HOME "msk_queenscancercenter" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_QUEENS_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_miamicancerinstitute -o=$MSK_MCI_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Miami Cancer Institute" -s=$MSK_DMP_TMPDIR/mci_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Miami Cancer Institute subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE subset"
    MSK_MCI_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_MCI_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Miami Cancer Institute subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE subset"
        MSK_MCI_SUBSET_FAIL=1
    else
        echo "MSK Miami Cancer Institute subset successful!"
        addCancerTypeCaseLists $MSK_MCI_DATA_HOME "msk_miamicancerinstitute" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_MCI_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_hartfordhealthcare -o=$MSK_HARTFORD_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Hartford Healthcare" -s=$MSK_DMP_TMPDIR/hartford_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Hartford Healthcare subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE subset"
    MSK_HARTFORD_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_HARTFORD_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Hartford Healthcare subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE subset"
        MSK_HARTFORD_SUBSET_FAIL=1
    else
        echo "MSK Hartford Healthcare subset successful!"
        addCancerTypeCaseLists $MSK_HARTFORD_DATA_HOME "msk_hartfordhealthcare" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_HARTFORD_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_ralphlauren -o=$MSK_RALPHLAUREN_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Ralph Lauren Center" -s=$MSK_DMP_TMPDIR/ralphlauren_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Ralph Lauren subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "RALPHLAUREN subset"
    MSK_RALPHLAUREN_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_RALPHLAUREN_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Ralph Lauren subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "RALPHLAUREN subset"
        MSK_RALPHLAUREN_SUBSET_FAIL=1
    else
        echo "MSK Ralph Lauren subset successful!"
        addCancerTypeCaseLists $MSK_RALPHLAUREN_DATA_HOME "msk_ralphlauren" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_RALPHLAUREN_IMPORT_TRIGGER
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_tailormedjapan -o=$MSK_TAILORMEDJAPAN_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Tailor Med Japan" -s=$MSK_DMP_TMPDIR/tailormedjapan_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Tailor Med Japan subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "TAILORMEDJAPAN subset"
    MSK_TAILORMEDJAPAN_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_TAILORMEDJAPAN_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Tailor Med Japan subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "TAILORMEDJAPAN subset"
        MSK_TAILORMEDJAPAN_SUBSET_FAIL=1
    else
        echo "MSK Tailor Med Japan subset successful!"
        addCancerTypeCaseLists $MSK_TAILORMEDJAPAN_DATA_HOME "msk_tailormedjapan" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_TAILORMEDJAPAN_IMPORT_TRIGGER
    fi
fi
#--------------------------------------------------------------

# Subset MSKIMPACT on PED_IND for MSKIMPACT_PED cohort
# rsync mskimpact data to tmp ped data home and overwrite clinical/timeline data with redcap
# export of mskimpact (with ped specific data and without caisis) into tmp ped data home directory for subsetting
# NOTE: the importer uses the java_tmp_dir/study_identifier for writing temp meta and data files so we should use a different
# tmp directory for subsetting purposes to prevent any conflicts and allow easier debugging of any issues that arise
MSKIMPACT_PED_TMP_DIR=$MSK_DMP_TMPDIR/mskimpact_ped_tmp
rsync -a $MSK_IMPACT_DATA_HOME/* $MSKIMPACT_PED_TMP_DIR
export_stable_id_from_redcap mskimpact $MSKIMPACT_PED_TMP_DIR mskimpact_clinical_caisis,mskimpact_timeline_surgery_caisis,mskimpact_timeline_status_caisis,mskimpact_timeline_treatment_caisis,mskimpact_timeline_imaging_caisis,mskimpact_timeline_specimen_caisis,mskimpact_timeline_chemotherapy_ddp,mskimpact_timeline_surgery_ddp
if [ $? -gt 0 ] ; then
    echo "MSKIMPACT redcap export for MSKIMPACT_PED failed! Study will not be updated in the portal"
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED redcap export"
    MSKIMPACT_PED_SUBSET_FAIL=1
else
    bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=mskimpact_ped -o=$MSKIMPACT_PED_DATA_HOME -d=$MSKIMPACT_PED_TMP_DIR -f="PED_IND=Yes" -s=$MSK_DMP_TMPDIR/mskimpact_ped_subset.txt -c=$MSKIMPACT_PED_TMP_DIR/data_clinical_patient.txt
    if [ $? -gt 0 ] ; then
        echo "MSKIMPACT_PED subset failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED subset"
        MSKIMPACT_PED_SUBSET_FAIL=1
    else
        filter_derived_clinical_data $MSKIMPACT_PED_DATA_HOME
        if [ $? -gt 0 ] ; then
            echo "MSKIMPACT_PED subset clinical attribute filtering step failed! Study will not be updated in the portal."
            sendFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED subset"
            MSKIMPACT_PED_SUBSET_FAIL=1
        else
            echo "MSKIMPACT_PED subset successful!"
            addCancerTypeCaseLists $MSKIMPACT_PED_DATA_HOME "mskimpact_ped" "data_clinical_sample.txt" "data_clinical_patient.txt"
            touch $MSKIMPACT_PED_IMPORT_TRIGGER
        fi
    fi
fi

#--------------------------------------------------------------

# Subset MSKIMPACT on ONCOTREE_CODE for SCLC cohort

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=sclc_mskimpact_2017 -o=$MSK_SCLC_DATA_HOME -d=$MSK_IMPACT_DATA_HOME -f="ONCOTREE_CODE=SCLC" -s=$MSK_DMP_TMPDIR/sclc_subset.txt -c=$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSKIMPACT SCLC subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT subset"
    SCLC_MSKIMPACT_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_SCLC_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSKIMPACT SCLC subset clinical attribute filtering step failed! Study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT subset"
        SCLC_MSKIMPACT_SUBSET_FAIL=1
    else
        echo "MSKIMPACT SCLC subset successful!"
        addCancerTypeCaseLists $MSK_SCLC_DATA_HOME "sclc_mskimpact_2017" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_SCLC_IMPORT_TRIGGER
    fi
fi
#--------------------------------------------------------------

# Create lymphoma "super" cohort
# Subset MSK-IMPACT and HEMEPACT by Cancer Type

# first touch meta_SV.txt in mskimpact, hemepact if not already exists - need these to generate merged subsets
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

# **************************************** ORDER OF SUBSET
# now subset sample files with lymphoma cases from mskimpact and hemepact
LYMPHOMA_FILTER_CRITERIA="CANCER_TYPE=Blastic Plasmacytoid Dendritic Cell Neoplasm,Histiocytosis,Hodgkin Lymphoma,Leukemia,Mastocytosis,Mature B-Cell Neoplasms,Mature T and NK Neoplasms,Myelodysplastic Syndromes,Myelodysplastic/Myeloproliferative Neoplasms,Myeloproliferative Neoplasms,Non-Hodgkin Lymphoma;ONCOTREE_CODE=DLBCL,BCL,SLL,TNKL,MALTL,MBCL,FL,SEZS"
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_HEMEPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT Heme. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

# check sizes of subset files before attempting to merge data using these subsets
grep -v '^#' $FMI_BATLEVI_DATA_HOME/data_clinical_sample.txt | awk -F '\t' '{if ($2 != "SAMPLE_ID") print $2;}' > $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from source Foundation study"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt >> $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt >> $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
fi

# merge data from mskimpact and hemepact lymphoma subsets with FMI BAT study
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ] ; then
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py  -d $LYMPHOMA_SUPER_COHORT_DATA_HOME -i "lymphoma_super_cohort_fmi_msk" -m "true" -s $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $FMI_BATLEVI_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "Lymphoma super cohort subset failed! Lymphoma super cohort study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT merge"
        LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
    else
        filter_derived_clinical_data $LYMPHOMA_SUPER_COHORT_DATA_HOME
        if [ $? -gt 0 ] ; then
            echo "Lymphoma super subset clinical attribute filtering step failed! Study will not be updated in the portal."
            sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT merge"
            LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
        else
            # add metadata headers before importing
            $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $LYMPHOMA_SUPER_COHORT_DATA_HOME/data_clinical*
            if [ $? -gt 0 ] ; then
                echo "Error: Adding metadata headers for LYMPHOMA_SUPER_COHORT failed! Study will not be updated in portal."
            else
                touch $LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER
            fi
        fi
    fi
    # remove files we don't need for lymphoma super cohort
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/*genie*
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/seq_date.txt
fi

# check that meta_SV.txt is actually an empty file before deleting from IMPACT and HEMEPACT studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi
#--------------------------------------------------------------
# Email for failed processes

EMAIL_BODY="The MSKIMPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_IMPACT -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The HEMEPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_HEME -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "HEMEPACT Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The RAINDANCE study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_RAINDANCE -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "RAINDANCE Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The ARCHER study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_ARCHER -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "archer Fetch Failure: Import" $email_list
fi

EMAIL_BODY="Failed to merge ARCHER fusion events into MSKIMPACT"
if [ $ARCHER_MERGE_IMPACT_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MSKIMPACT-ARCHER Merge Failure: Study will be updated without new ARCHER fusion events." $email_list
fi

EMAIL_BODY="Failed to merge ARCHER fusion events into HEMEPACT"
if [ $ARCHER_MERGE_HEME_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "HEMEPACT-ARCHER Merge Failure: Study will be updated without new ARCHER fusion events." $email_list
fi

EMAIL_BODY="Failed to subset UNLINKED_ARCHER data from ARCHER_UNFILTERED"
if [ $UNLINKED_ARCHER_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "UNLINKED_ARCHER Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, ARCHER, and RAINDANCE data. Merged study will not be updated."
if [ $MIXEDPACT_MERGE_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIXEDPACT Merge Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, and ARCHER data. Merged study will not be updated."
if [ $MSK_SOLID_HEME_MERGE_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MSKSOLIDHEME Merge Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Kings County Cancer Center data. Subset study will not be updated."
if [ $MSK_KINGS_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "KINGSCOUNTY Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Lehigh Valley data. Subset study will not be updated."
if [ $MSK_LEHIGH_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "LEHIGHVALLEY Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Queens Cancer Center data. Subset study will not be updated."
if [ $MSK_QUEENS_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "QUEENSCANCERCENTER Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Miami Cancer Institute data. Subset study will not be updated."
if [ $MSK_MCI_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIAMICANCERINSTITUTE Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Hartford Healthcare data. Subset study will not be updated."
if [ $MSK_HARTFORD_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "HARTFORDHEALTHCARE Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Ralph Lauren Center data. Subset study will not be updated."
if [ $MSK_RALPHLAUREN_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "RALPHLAUREN Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset MSK Tailor Med Japan data. Subset study will not be updated."
if [ $MSK_TAILORMEDJAPAN_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "TAILORMEDJAPAN Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset MSKIMPACT_PED data. Subset study will not be updated."
if [ $MSKIMPACT_PED_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT_PED Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset MSKIMPACT SCLC data. Subset study will not be updated."
if [ $SCLC_MSKIMPACT_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "SCLCMSKIMPACT Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset LYMPHOMASUPERCOHORT data. Subset study will not be updated."
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "LYMPHOMASUPERCOHORT Subset Failure: Study will not be updated." $email_list
fi
