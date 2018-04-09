#!/bin/bash

CVR_TEST_MODE_ARGS=""
JAVA_DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182"
JAVA_TMPDIR="$PORTAL_HOME/tmp/import-cron-dmp-msk"
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $JAVA_DEBUG_ARGS -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$JAVA_TMPDIR -Dhttp.nonProxyHosts=draco.mskcc.org|pidvudb1.mskcc.org|phcrdbd2.mskcc.org|dashi-dev.cbio.mskcc.org|pipelines.cbioportal.mskcc.org|localhost"
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
PERFORM_CRDB_FETCH=0  # if zero, no CRDB fetch or import to redcap will occur

## FUNCTIONS

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
    $PYTHON_BINARY $PORTAL_HOME/scripts/oncotree_code_converter.py --oncotree-url "http://oncotree.mskcc.org/oncotree/" --oncotree-version $ONCOTREE_VERSION_TO_USE --clinical-file $FILEPATH_1 --force
    $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file-list="$CLINICAL_FILE_LIST" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="CANCER_TYPE"
    if [ "$STUDY_ID" == "mskimpact" ] || [ "$STUDY_ID" == "mixedpact" ] ; then
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

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" https://hooks.slack.com/services/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
}

# Function for alerting slack channel of successful imports
function sendSuccessMessageMskPipelineLogsSlack {
    STUDY_ID=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK cBio pipelines import success: $STUDY_ID\", \"icon_emoji\": \":tada:\"}" https://hooks.slack.com/services/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
}

# Function for restarting MSK tomcats
# TODO obviously restartMSKTomcats and restartSchultzTomcats should really be one function ...
function restartMSKTomcats {
    # redeploy war
    echo "Requesting redeployment of msk portal war..."
    echo $(date)
    TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
    TOMCAT_HOST_USERNAME=cbioportal_importer
    TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_msk_tomcat_restarts_key
    TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/msk-tomcat-restart
    TOMCAT_SERVER_PRETTY_DISPLAY_NAME="MSK Tomcat" # e.g. Public Tomcat
    TOMCAT_SERVER_DISPLAY_NAME="msk-tomcat" # e.g. schultz-tomcat
    SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
    declare -a failed_restart_server_list
    for server in ${TOMCAT_HOST_LIST[@]} ; do
        if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
            failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
        fi
    done
    if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
        EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
    fi
}

# Function for restarting Schultz tomcats
# TODO obviously restartMSKTomcats and restartSchultzTomcats should really be one function ...
function restartSchultzTomcats {
    # redeploy war
    echo "Requesting redeployment of schultz portal war..."
    echo $(date)
    TOMCAT_HOST_LIST=(dashi.cbio.mskcc.org dashi2.cbio.mskcc.org)
    TOMCAT_HOST_USERNAME=cbioportal_importer
    TOMCAT_HOST_SSH_KEY_FILE=${HOME}/.ssh/id_rsa_schultz_tomcat_restarts_key
    TOMCAT_SERVER_RESTART_PATH=/srv/data/portal-cron/schultz-tomcat-restart
    TOMCAT_SERVER_PRETTY_DISPLAY_NAME="Schultz Tomcat" # e.g. Public Tomcat
    TOMCAT_SERVER_DISPLAY_NAME="schultz-tomcat" # e.g. schultz-tomcat
    SSH_OPTIONS="-i ${TOMCAT_HOST_SSH_KEY_FILE} -o BATCHMODE=yes -o ConnectTimeout=3"
    declare -a failed_restart_server_list
    for server in ${TOMCAT_HOST_LIST[@]} ; do
        if ! ssh ${SSH_OPTIONS} ${TOMCAT_HOST_USERNAME}@${server} touch ${TOMCAT_SERVER_RESTART_PATH} ; then
            failed_restart_server_list[${#failed_restart_server_list[*]}]=${server}
        fi
    done
    if [ ${#failed_restart_server_list[*]} -ne 0 ] ; then
        EMAIL_BODY="Attempt to trigger a restart of the $TOMCAT_SERVER_DISPLAY_NAME server on the following hosts failed: ${failed_restart_server_list[*]}"
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "$TOMCAT_SERVER_PRETTY_DISPLAY_NAME Restart Error : unable to trigger restart" $email_list
    fi
}

# Function for import project data into redcap
function import_project_to_redcap {
    filename=$1
    project_title=$2
    $JAVA_HOME/bin/java $JAVA_SSL_ARGS -jar $PORTAL_HOME/lib/redcap_pipeline.jar -i --filename ${filename} --redcap-project-title ${project_title}
    if [ $? -ne 0 ] ; then
        #log error
        echo "Failed to import file ${filename} into redcap project ${project_title}"
        return 1
    fi
}

# Function for exporting redcap project
function export_project_from_redcap {
    directory=$1
    project_title=$2
    $JAVA_HOME/bin/java $JAVA_SSL_ARGS -jar $PORTAL_HOME/lib/redcap_pipeline.jar -e -r -d ${directory} --redcap-project-title ${project_title}
    if [ $? -ne 0 ] ; then
        #log error
        echo "Failed to export project ${project_title} from redcap into directory ${directory}"
        return 1
    fi
}

# Function for exporting redcap projects (merged to standard cbioportal format) by stable id
function export_stable_id_from_redcap {
    stable_id=$1
    directory=$2
    $JAVA_HOME/bin/java $JAVA_SSL_ARGS -jar $PORTAL_HOME/lib/redcap_pipeline.jar -e -s ${stable_id} -d ${directory}
    if [ $? -ne 0 ] ; then
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

function import_mskimpact_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt mskimpact_supp_date_cbioportal_added ; then return_value=1 ; fi
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

function import_hemepact_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt hemepact_data_clinical_supp_date ; then return_value=1 ; fi
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

function import_raindance_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt mskraindance_data_clinical_supp_date ; then return_value=1 ; fi
    return $return_value
}

# Function for importing archer darwin files to redcap
function import_archer_darwin_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_DATA_HOME/data_clinical_supp_darwin_demographics.txt mskarcher_data_clinical_supp_darwin_demographics ; then return_value=1 ; fi
    return $return_value
}

# Function for importing archer cvr files to redcap
function import_archer_cvr_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_DATA_HOME/data_clinical_mskarcher_data_clinical.txt mskarcher_data_clinical ; then return_value=1 ; fi
    return $return_value
}

function import_archer_supp_date_to_redcap {
    return_value=0
    if ! import_project_to_redcap $MSK_ARCHER_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt mskarcher_data_clinical_supp_date ; then return_value=1 ; fi
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
# -----------------------------------------------------------------------------------------------------------

echo $(date)

email_list="cbioportal-pipelines@cbio.mskcc.org"

if [[ -d "$JAVA_TMPDIR" && "$JAVA_TMPDIR" != "/" ]] ; then
    rm -rf "$JAVA_TMPDIR"/*
fi

if [ -z $JAVA_HOME ] | [ -z $HG_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_IMPACT_DATA_HOME ] ; then
    message="test could not run import-dmp-impact.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_IMPACT_DATA_HOME, ...)"
    echo ${message}
    echo -e "${message}" |  mail -s "import-dmp-impact-data failed to run." $email_list
    sendFailureMessageMskPipelineLogsSlack "${message}"
    exit 2
fi

now=$(date "+%Y-%m-%d-%H-%M-%S")
mskimpact_notification_file=$(mktemp $JAVA_TMPDIR/mskimpact-portal-update-notification.$now.XXXXXX)
mskheme_notification_file=$(mktemp $JAVA_TMPDIR/mskheme-portal-update-notification.$now.XXXXXX)
mskraindance_notification_file=$(mktemp $JAVA_TMPDIR/mskraindance-portal-update-notification.$now.XXXXXX)
mixedpact_notification_file=$(mktemp $JAVA_TMPDIR/mixedpact-portal-update-notification.$now.XXXXXX)
mskarcher_notification_file=$(mktemp $JAVA_TMPDIR/mskarcher-portal-update-notification.$now.XXXXXX)
kingscounty_notification_file=$(mktemp $JAVA_TMPDIR/kingscounty-portal-update-notification.$now.XXXXXX)
lehighvalley_notification_file=$(mktemp $JAVA_TMPDIR/lehighvalley-portal-update-notification.$now.XXXXXX)
queenscancercenter_notification_file=$(mktemp $JAVA_TMPDIR/queenscancercenter-portal-update-notification.$now.XXXXXX)
miamicancerinstitute_notification_file=$(mktemp $JAVA_TMPDIR/miamicancerinstitute-portal-update-notification.$now.XXXXXX)
hartfordhealthcare_notification_file=$(mktemp $JAVA_TMPDIR/hartfordhealthcare-portal-update-notification.$now.XXXXXX)
lymphoma_super_cohort_notification_file=$(mktemp $JAVA_TMPDIR/lymphoma-super-cohort-portal-update-notification.$now.XXXXXX)
sclc_mskimpact_notification_file=$(mktemp $JAVA_TMPDIR/sclc-mskimpact-portal-update-notification.$now.XXXXXX)

# fetch clinical data mercurial
echo "fetching updates from msk-impact repository..."
$JAVA_HOME/bin/java $JAVA_IMPORTER_ARGS -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source dmp-clinical-data-mercurial --run-date latest
if [ $? -gt 0 ] ; then
    sendFailureMessageMskPipelineLogsSlack "Fetch Msk-impact from Mercurial Failure"
    exit 2
fi

# -----------------------------------------------------------------------------------------------------------

DB_VERSION_FAIL=0
IMPORT_STATUS_IMPACT=0
IMPORT_STATUS_HEME=0
IMPORT_STATUS_RAINDANCE=0
IMPORT_STATUS_ARCHER=0

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
FETCH_CVR_IMPACT_FAIL=1
FETCH_CVR_HEME_FAIL=1
FETCH_CVR_RAINDANCE_FAIL=1
FETCH_CVR_ARCHER_FAIL=1

MIXEDPACT_MERGE_FAIL=0
MSK_KINGS_SUBSET_FAIL=0
MSK_QUEENS_SUBSET_FAIL=0
MSK_LEHIGH_SUBSET_FAIL=0
MSK_MCI_SUBSET_FAIL=0
MSK_HARTFORD_SUBSET_FAIL=0
SCLC_MSKIMPACT_SUBSET_FAIL=0
LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=0

MIXEDPACT_ADD_HEADER_FAIL=0
MSK_KINGS_ADD_HEADER_FAIL=0
MSK_QUEENS_ADD_HEADER_FAIL=0
MSK_LEHIGH_ADD_HEADER_FAIL=0
MSK_MCI_ADD_HEADER_FAIL=0
MSK_HARTFORD_ADD_HEADER_FAIL=0
SCLC_MSKIMPACT_ADD_HEADER_FAIL=0
LYMPHOMA_SUPER_COHORT_ADD_HEADER_FAIL=0

IMPORT_FAIL_MIXEDPACT=0
IMPORT_FAIL_KINGS=0
IMPORT_FAIL_LEHIGH=0
IMPORT_FAIL_QUEENS=0
IMPORT_FAIL_MCI=0
IMPORT_FAIL_HARTFORD=0
IMPORT_FAIL_SCLC_MSKIMPACT=0
IMPORT_FAIL_LYMPHOMA=0
GENERATE_MASTERLIST_FAIL=0

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
export_project_from_redcap $MSK_ARCHER_DATA_HOME mskarcher_data_clinical_supp_date
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
export_project_from_redcap $MSK_ARCHER_DATA_HOME mskarcher_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_ARCHER=1
    sendFailureMessageMskPipelineLogsSlack "ARCHER Redcap export of mskarcher_data_clinical_cvr"
fi

# -------------------------------- all mskimpact project data fetches -----------------------------------

if [ $PERFORM_CRDB_FETCH -ne 0 ] ; then
    # fetch CRDB data
    echo "fetching CRDB data"
    echo $(date)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/crdb_fetcher.jar -stage $MSK_IMPACT_DATA_HOME
    # no need for hg update/commit ; CRDB generated files are stored in redcap and not mercurial
    if [ $? -gt 0 ] ; then
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT CRDB Fetch"
    else
        FETCH_CRDB_IMPACT_FAIL=0
    fi
else
    FETCH_CRDB_IMPACT_FAIL=0
fi

# fetch Darwin data
echo "fetching Darwin impact data"
echo $(date)
$JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_IMPACT_DATA_HOME -s mskimpact
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT DARWIN Fetch"
else
    FETCH_DARWIN_IMPACT_FAIL=0
    echo "committing darwin data"
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin impact"
fi

if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    # fetch new/updated IMPACT samples using CVR Web service   (must come after mercurial fetching)
    echo "fetching samples from CVR Web service  ..."
    echo $(date)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -i mskimpact -r 150 $CVR_TEST_MODE_ARGS
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
            FETCH_CVR_IMPACT_FAIL=0
            echo "committing cvr data"
            cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: CVR"
        fi
    fi

    # fetch new/updated IMPACT germline samples using CVR Web service   (must come after normal cvr fetching)
    echo "fetching CVR GML data ..."
    echo $(date)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -g -i mskimpact $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Germline fetch failed!"
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Germline Fetch"
        IMPORT_STATUS_IMPACT=1
        #override the success of the tumor sample cvr fetch with a failed status
        FETCH_CVR_IMPACT_FAIL=1
    else
        echo "committing CVR germline data"
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: CVR Germline"
    fi
fi

# -------------------------------- all hemepact project data fetches -----------------------------------

echo "fetching Darwin heme data"
echo $(date)
$JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_HEMEPACT_DATA_HOME -s mskimpact_heme
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT DARWIN Fetch"
else
    FETCH_DARWIN_HEME_FAIL=0
    echo "committing darwin data for heme"
    cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin heme"
fi

if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    # fetch new/updated heme samples using CVR Web service (must come after mercurial fetching). Threshold is set to 50 since heme contains only 190 samples (07/12/2017)
    echo "fetching CVR heme data..."
    echo $(date)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_HEMEPACT_DATA_HOME -n data_clinical_hemepact_data_clinical.txt -i mskimpact_heme -r 50 $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR heme fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT CVR Fetch"
        IMPORT_STATUS_HEME=1
    else
        FETCH_CVR_HEME_FAIL=0
        echo "committing cvr data for heme"
        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY commit -m "Latest heme dataset"
    fi
fi

# -------------------------------- all raindance project data fetches -----------------------------------

echo "fetching Darwin raindance data"
echo $(date)
$JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_RAINDANCE_DATA_HOME -s mskraindance
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "RAINDANCE DARWIN Fetch"
else
    FETCH_DARWIN_RAINDANCE_FAIL=0
    echo "commting darwin data for raindance"
    cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin raindance"
fi

if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    # fetch new/updated raindance samples using CVR Web service (must come after mercurial fetching). The -s flag skips segment data fetching
    echo "fetching CVR raindance data..."
    echo $(date)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_RAINDANCE_DATA_HOME -n data_clinical_mskraindance_data_clinical.txt -s -i mskraindance $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR raindance fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "RAINDANCE CVR Fetch"
        IMPORT_STATUS_RAINDANCE=1
    else
        FETCH_CVR_RAINDANCE_FAIL=0
        # raindance does not provide copy number or fusions data.
        echo "removing unused files"
        cd $MSK_RAINDANCE_DATA_HOME ; rm -f data_CNA.txt data_fusions.txt data_SV.txt mskraindance_data_cna_hg19.seg
        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY commit -m "Latest Raindance dataset"
    fi
fi

# -------------------------------- all archer project data fetches -----------------------------------

echo "fetching Darwin archer data"
echo $(date)
$JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_ARCHER_DATA_HOME -s mskarcher
if [ $? -gt 0 ] ; then
    cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
    sendFailureMessageMskPipelineLogsSlack "ARCHER DARWIN Fetch"
else
    FETCH_DARWIN_ARCHER_FAIL=0
    echo " committing darwin data for archer"
    cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin archer"
fi

if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    # fetch new/updated archer samples using CVR Web service (must come after mercurial fetching).
    echo "fetching CVR archer data..."
    echo $(date)
    # archer has -b option to block warnings for samples with zero variants (all samples will have zero variants)
    $JAVA_HOME/bin/java -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_ARCHER_DATA_HOME -n data_clinical_mskarcher_data_clinical.txt -i mskarcher -s -b $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Archer fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "ARCHER CVR Fetch"
        IMPORT_STATUS_ARCHER=1
    else
        FETCH_CVR_ARCHER_FAIL=0
        # mskarcher does not provide copy number, mutations, or seg data, renaming gene matrix file until we get the mskarcher gene panel imported
        echo "Removing unused files"
        cd $MSK_ARCHER_DATA_HOME ; rm -f data_CNA.txt data_mutations_* mskarcher_data_cna_hg19.seg
        cd $MSK_ARCHER_DATA_HOME ; mv data_gene_matrix.txt ignore_data_gene_matrix.txt
        cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY commit -m "Latest archer dataset"
    fi
fi

# -------------------------------- Generate all case lists and supp date files -----------------------------------

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for MSK-IMPACT
if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact" "data_clinical_mskimpact_data_clinical_cvr.txt"
    cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_IMPACT_DATA_HOME "data_clinical_mskimpact_data_clinical_cvr.txt" "data_clinical_mskimpact_supp_date_cbioportal_added.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for HEMEPACT
if [ $IMPORT_STATUS_HEME -eq 0 ] && [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_HEMEPACT_DATA_HOME "mskimpact_heme" "data_clinical_hemepact_data_clinical.txt"
    cd $MSK_HEMEPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_HEME_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_HEMEPACT_DATA_HOME "data_clinical_hemepact_data_clinical.txt" "data_clinical_hemepact_data_clinical_supp_date.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for RAINDANCE
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] && [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_RAINDANCE_DATA_HOME "mskraindance" "data_clinical_mskraindance_data_clinical.txt"
    cd $MSK_RAINDANCE_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_RAINDANCE_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_RAINDANCE_DATA_HOME "data_clinical_mskraindance_data_clinical.txt" "data_clinical_mskraindance_data_clinical_supp_date.txt"
    fi
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for ARCHER
if [ $IMPORT_STATUS_ARCHER -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_ARCHER_DATA_HOME "mskarcher" "data_clinical_mskarcher_data_clinical.txt"
    cd $MSK_ARCHER_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Latest ARCHER Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_ARCHER_DATA_HOME "data_clinical_mskarcher_data_clinical.txt" "data_clinical_mskarcher_data_clinical_supp_date.txt"
    fi
fi

# -------------------------------- Additional processing -----------------------------------

if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    # Merge Archer fusion data into the impact cohort
    $PYTHON_BINARY $PORTAL_HOME/scripts/archer_fusions_merger.py --archer-fusions $MSK_ARCHER_DATA_HOME/data_fusions.txt --linked-mskimpact-cases-filename $MSK_ARCHER_DATA_HOME/linked_mskimpact_cases.txt --msk-fusions $MSK_IMPACT_DATA_HOME/data_fusions.txt --clinical-filename $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt --archer-samples-filename $JAVA_TMPDIR/archer_ids.txt
    cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY forget data_clinical* ; $HG_BINARY forget data_timeline* ; $HG_BINARY commit -m "Adding ARCHER fusions to MSKIMPACT"
fi

# gets index of SAMPLE_ID from file (used in case SAMPLE_ID index changes)
SAMPLE_ID_COLUMN=`sed -n $"1s/\t/\\\n/gp" $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | grep -nx "SAMPLE_ID" | cut -d: -f1`
# generates sample masterlist for filtering dropped samples/patients from supp files
SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME="$JAVA_TMPDIR/sample_masterlist_for_filtering.txt"
grep -v '^#' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | awk -v SAMPLE_ID_INDEX="$SAMPLE_ID_COLUMN" -F '\t' '{if ($SAMPLE_ID_INDEX != "SAMPLE_ID") print $SAMPLE_ID_INDEX;}' > $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME
if [ $(wc -l < $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME) -eq 0 ] ; then
    echo "ERROR! Sample masterlist $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME is empty. Skipping patient/sample filtering for mskimpact!"
    GENERATE_MASTERLIST_FAIL=1
fi

# -------------------------------- Import projects into redcap -----------------------------------

# import newly fetched files into redcap
echo "Starting import into redcap"

if [ $PERFORM_CRDB_FETCH -ne 0 ] ; then
    # imports crdb data into redcap
    echo "importing mskimpact related clinical files into redcap"
    if [ $FETCH_CRDB_IMPACT_FAIL -eq 0 ] ; then
        import_crdb_to_redcap
        if [ $? -gt 0 ] ; then
            #TODO: maybe implement retry loop here
            #NOTE: we have decided to allow import of msk-impact project to proceed even when CRDB data has been lost from redcap (not setting IMPORT_STATUS_IMPACT)
            sendFailureMessageMskPipelineLogsSlack "Mskimpact CRDB Redcap Import - Recovery Of Redcap Project Needed!"
        fi
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
        sendFailureMessageMskPipelineLogsSlack "Archer Darwin Redcap Import"
    fi
fi

# imports archer cvr data into redcap
if [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] ; then
    import_archer_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        sendFailureMessageMskPipelineLogsSlack "Archer CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ] ; then
        import_archer_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendFailureMessageMskPipelineLogsSlack "Mskimpact Supp Date Redcap Import. Project is now empty, data restoration required"
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
remove_raw_clinical_timeline_data_files $MSK_ARCHER_DATA_HOME

# -------------------------------------------------------------
# export data in standard cbioportal mode from redcap

echo "exporting impact data from redcap"
if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact $MSK_IMPACT_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "Mskimpact Redcap Export"
    else
        if [ $GENERATE_MASTERLIST_FAIL -eq 0 ] ; then
            $PYTHON_BINARY $PORTAL_HOME/scripts/filter_dropped_samples_patients.py -s $MSK_IMPACT_DATA_HOME/data_clinical_sample.txt -p $MSK_IMPACT_DATA_HOME/data_clinical_patient.txt -t $MSK_IMPACT_DATA_HOME/data_timeline.txt -f $JAVA_TMPDIR/sample_masterlist_for_filtering.txt
        fi
        cd $MSK_IMPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Clinical and Timeline"
    fi
fi

echo "exporting hemepact data from redcap"
if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact_heme $MSK_HEMEPACT_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        cd $MSK_HEMEPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "Hemepact Redcap Export"
    else
        cd $MSK_HEMEPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Clinical and Timeline"
    fi
fi

echo "exporting raindance data from redcap"
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    export_stable_id_from_redcap mskraindance $MSK_RAINDANCE_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        cd $MSK_RAINDANCE_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "Raindance Redcap Export"
    else
        cd $MSK_RAINDANCE_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Clinical and Timeline"
    fi
fi

echo "exporting archer data from redcap"
if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    export_stable_id_from_redcap mskarcher $MSK_ARCHER_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        cd $MSK_ARCHER_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
        sendFailureMessageMskPipelineLogsSlack "Archer Redcap Export"
    else
        cd $MSK_ARCHER_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest ARCHER Dataset: Clinical and Timeline"
    fi
fi

#--------------------------------------------------------------

# check database version before importing anything
echo "Checking if database version is compatible"
echo $(date)
$JAVA_HOME/bin/java $JAVA_IMPORTER_ARGS -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --check-db-version
if [ $? -gt 0 ] ; then
    echo "Database version expected by portal does not match version in database!"
    sendFailureMessageMskPipelineLogsSlack "MSK DMP Importer DB version check"
    DB_VERSION_FAIL=1
    IMPORT_STATUS_IMPACT=1
    IMPORT_STATUS_HEME=1
    IMPORT_STATUS_ARCHER=1
    IMPORT_STATUS_RAINDANCE=1
fi

if [ $DB_VERSION_FAIL -eq 0 ] ; then
    # import into portal database
    echo "importing cancer type updates into msk portal database..."
    $JAVA_HOME/bin/java $JAVA_IMPORTER_ARGS -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    if [ $? -gt 0 ] ; then
        sendFailureMessageMskPipelineLogsSlack "Cancer type updates"
    fi
fi

# Temp study importer arguments
# (1): cancer study id [ mskimpact | mskimpact_heme | mskraindance | mskarcher | mixedpact | msk_kingscounty | msk_lehighvalley | msk_queenscancercenter | msk_miamicancerinstitute | msk_hartfordhealthcare | lymphoma_super_cohort_fmi_msk ]
# (2): temp study id [ temporary_mskimpact | temporary_mskimpact_heme | temporary_mskraindance | temporary_mskarcher | temporary_mixedpact | temporary_msk_kingscounty | temporary_msk_lehighvalley | temporary_msk_queenscancercenter | temporary_msk_miamicancerinstitute | temporary_msk_hartfordhealthcare | temporary_lymphoma_super_cohort_fmi_msk]
# (3): backup study id [ yesterday_mskimpact | yesterday_mskimpact_heme | yesterday_mskraindance | yesterday_mskarcher | yesterday_mixedpact | yesterday_msk_kingscounty | yesterday_msk_lehighvalley | yesterday_msk_queenscancercenter | yesterday_msk_miamicancerinstitute | yesterday_msk_hartfordhealthcare | yesterday_lymphoma_super_cohort_fmi_msk]
# (4): portal name [ mskimpact-portal | mskheme-portal | mskraindance-portal | mskarcher-portal | mixedpact-portal |  msk-kingscounty-portal | msk-lehighvalley-portal | msk-queenscancercenter-portal | msk-mci-portal | msk-hartford-portal | msk-fmi-lymphoma-portal ]
# (5): study path [ $MSK_IMPACT_DATA_HOME | $MSK_HEMEPACT_DATA_HOME | $MSK_RAINDANCE_DATA_HOME | $MSK_ARCHER_DATA_HOME | $MSK_MIXEDPACT_DATA_HOME | $MSK_KINGS_DATA_HOME | $MSK_LEHIGH_DATA_HOME | $MSK_QUEENS_DATA_HOME | $MSK_MCI_DATA_HOME | $MSK_HARTFORD_DATA_HOME | $LYMPHOMA_SUPER_COHORT_DATA_HOME ]
# (6): notification file [ $mskimpact_notification_file | $mskheme_notification_file | $mskraindance_notification_file | $mixedpact_notification_file | $kingscounty_notification_file | $lehighvalley_notification_file | $queenscancercenter_notification_file | $miamicancerinstitute_notification_file | $hartfordhealthcare_notification_file | $lymphoma_super_cohort_notification_file ]
# (7): tmp directory
# (8): email list
# (9): oncotree version [ oncotree_candidate_release | oncotree_latest_stable ]
# (10): importer jar
# (11): transcript overrides source [ uniprot | mskcc ]

## TEMP STUDY IMPORT: MSKIMPACT
RESTART_AFTER_IMPACT_IMPORT=0
if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact" --temp-study-id="temporary_mskimpact" --backup-study-id="yesterday_mskimpact" --portal-name="mskimpact-portal" --study-path="$MSK_IMPACT_DATA_HOME" --notification-file="$mskimpact_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        RESTART_AFTER_IMPACT_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "MSKIMPACT"
    else
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT import"
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing mskimpact - database version is not compatible"
    else
        echo "Not importing mskimpact - something went wrong with a fetch"
    fi
fi

## TOMCAT RESTART
# restart tomcat only if the MSK-IMPACT update was succesful
if [ $RESTART_AFTER_IMPACT_IMPORT -eq 0 ] ; then
    echo "Failed to update MSK-IMPACT - next tomcat restart will execute after successful updates to other MSK clinical pipelines and/or MSK affiliate studies..."
    echo $(date)
else
    restartMSKTomcats
fi

# set 'RESTART_AFTER_DMP_PIPELINES_IMPORT' flag to 1 if RAINDANCE, ARCHER, HEMEPACT, or MIXEDPACT succesfully update
RESTART_AFTER_DMP_PIPELINES_IMPORT=0

## TEMP STUDY IMPORT: MSKIMPACT_HEME
if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact_heme" --temp-study-id="temporary_mskimpact_heme" --backup-study-id="yesterday_mskimpact_heme" --portal-name="mskheme-portal" --study-path="$MSK_HEMEPACT_DATA_HOME" --notification-file="$mskheme_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "HEMEPACT"
    else
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT import"
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing mskimpact_heme - database version is not compatible"
    else
        echo "Not importing mskimpact_heme - something went wrong with a fetch"
    fi
fi

## TEMP STUDY IMPORT: MSKRAINDANCE
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskraindance" --temp-study-id="temporary_mskraindance" --backup-study-id="yesterday_mskraindance" --portal-name="mskraindance-portal" --study-path="$MSK_RAINDANCE_DATA_HOME" --notification-file="$mskraindance_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "RAINDANCE"
    else
        sendFailureMessageMskPipelineLogsSlack "RAINDANCE import"
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing mskraindance - database version is not compatible"
    else
        echo "Not importing mskraindance - something went wrong with a fetch"
    fi
fi

# TEMP STUDY IMPORT: MSKARCHER
if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskarcher" --temp-study-id="temporary_mskarcher" --backup-study-id="yesterday_mskarcher" --portal-name="mskarcher-portal" --study-path="$MSK_ARCHER_DATA_HOME" --notification-file="$mskarcher_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "ARCHER"
    else
        sendFailureMessageMskPipelineLogsSlack "ARCHER import"
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing mskarcher - database version is not compatible"
    else
        echo "Not importing mskarcher - something went wrong with a fetch"
    fi
fi

## MSK-IMPACT, HEMEPACT, RAINDANCE, and ARCHER merge
echo "Beginning merge of MSK-IMPACT, HEMEPACT, RAINDANCE, ARCHER data..."
echo $(date)

#TODO: The next block of touch checks can be made into a loop
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_ARCHER_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_ARCHER_DATA_HOME/meta_SV.txt
fi

# merge data from both directories and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_MIXEDPACT_DATA_HOME -i mixedpact -m "true" -e $JAVA_TMPDIR/archer_ids.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_RAINDANCE_DATA_HOME $MSK_ARCHER_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MIXEDPACT merge failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "MIXEDPACT merge"
    echo $(date)
    MIXEDPACT_MERGE_FAIL=1
    # we rollback/clean mercurial after the import of mixedpact (if merge or import fails)
else
    # if merge successful then copy case lists from MSK-IMPACT directory and change stable id
    echo "MIXEDPACT merge successful! Creating cancer type case lists..."
    echo $(date)
    # add metadata headers and overrides before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mixedpact -f $MSK_MIXEDPACT_DATA_HOME/data_clinical*
    addCancerTypeCaseLists $MSK_MIXEDPACT_DATA_HOME "mixedpact" "data_clinical_sample.txt" "data_clinical_patient.txt"
fi

# check that meta_SV.txt are actually empty files before deleting from IMPACT, HEME, and RAINDANCE studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

# update MIXEDPACT in portal only if merge and case list updates were succesful and metadata headers were added
if [ $MIXEDPACT_MERGE_FAIL -eq 0 ] && [ $MIXEDPACT_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing MIXEDPACT study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mixedpact" --temp-study-id="temporary_mixedpact" --backup-study-id="yesterday_mixedpact" --portal-name="mixedpact-portal" --study-path="$MSK_MIXEDPACT_DATA_HOME" --notification-file="$mixedpact_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_MIXEDPACT=1
        sendFailureMessageMskPipelineLogsSlack "MIXEDPACT import"
    else
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "MIXEDPACT"
    fi
else
    echo "Something went wrong with merging clinical studies."
    IMPORT_FAIL_MIXEDPACT=1
fi

# commit or revert changes for MIXEDPACT
if [ $IMPORT_FAIL_MIXEDPACT -gt 0 ] ; then
    echo "MIXEDPACT merge and/or updates failed! Reverting data to last commit."
    cd $MSK_MIXEDPACT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing MIXEDPACT data"
    cd $MSK_MIXEDPACT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest MIXEDPACT dataset"
fi
## END MSK-IMPACT, HEMEPACT, and RAINDANCE merge

## TOMCAT RESTART
# Restart will only execute if at least one of these studies succesfully updated.
#   MSKIMPACT_HEME
#   MSKRAINDANCE
#   MSKARCHER
#   MIXEDPACT
if [ $RESTART_AFTER_DMP_PIPELINES_IMPORT -eq 0 ] ; then
    echo "Failed to update HEMEPACT, RAINDANCE, ARCHER, and MIXEDPACT - next tomcat restart will execute after successful updates to MSK affiliate studies..."
    echo $(date)
else
    restartMSKTomcats
fi

## Subset MIXEDPACT on INSTITUTE for institute specific impact studies

# subset the mixedpact study for Queens Cancer Center, Lehigh Valley, Kings County Cancer Center, Miami Cancer Institute, and Hartford Health Care
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_kingscounty -o=$MSK_KINGS_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Kings County Cancer Center" -s=$JAVA_TMPDIR/kings_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Kings County subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "KINGSCOUNTY subset"
    MSK_KINGS_SUBSET_FAIL=1
else
    echo "MSK Kings County subset successful!"
    addCancerTypeCaseLists $MSK_KINGS_DATA_HOME "msk_kingscounty" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $MSK_KINGS_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        MSK_KINGS_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for KINGSCOUNTY."
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_lehighvalley -o=$MSK_LEHIGH_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Lehigh Valley Health Network" -s=$JAVA_TMPDIR/lehigh_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Lehigh Valley subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY subset"
    MSK_LEHIGH_SUBSET_FAIL=1
else
    echo "MSK Lehigh Valley subset successful!"
    addCancerTypeCaseLists $MSK_LEHIGH_DATA_HOME "msk_lehighvalley" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $MSK_LEHIGH_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        MSK_LEHIGH_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for LEHIGHVALLEY."
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_queenscancercenter -o=$MSK_QUEENS_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Queens Cancer Center,Queens Hospital Cancer Center" -s=$JAVA_TMPDIR/queens_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Queens Cancer Center subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER subset"
    MSK_QUEENS_SUBSET_FAIL=1
else
    echo "MSK Queens Cancer Center subset successful!"
    addCancerTypeCaseLists $MSK_QUEENS_DATA_HOME "msk_queenscancercenter" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $MSK_QUEENS_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        MSK_QUEENS_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for QUEENSCANCERCENTER."
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_miamicancerinstitute -o=$MSK_MCI_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Miami Cancer Institute" -s=$JAVA_TMPDIR/mci_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Miami Cancer Institute subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE subset"
    MSK_MCI_SUBSET_FAIL=1
else
    echo "MSK Miami Cancer Institute subset successful!"
    addCancerTypeCaseLists $MSK_MCI_DATA_HOME "msk_miamicancerinstitute" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $MSK_MCI_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        MSK_MCI_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for MIAMICANCERINSTITUTE."
    fi
fi

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_hartfordhealthcare -o=$MSK_HARTFORD_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Hartford Healthcare" -s=$JAVA_TMPDIR/hartford_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Hartford Healthcare subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE subset"
    MSK_HARTFORD_SUBSET_FAIL=1
else
    echo "MSK Hartford Healthcare subset successful!"
    addCancerTypeCaseLists $MSK_HARTFORD_DATA_HOME "msk_hartfordhealthcare" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $MSK_HARTFORD_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        MSK_HARTFORD_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for HARTFORDHEALTHCARE."
    fi
fi

# set 'RESTART_AFTER_MSK_AFFILIATE_IMPORT' flag to 1 if Kings County, Lehigh Valley, Queens Cancer Center, Miami Cancer Institute, or Lymphoma super cohort succesfully update
RESTART_AFTER_MSK_AFFILIATE_IMPORT=0
# update msk_kingscounty in portal only if subset was successful and metadata headers were added
if [ $MSK_KINGS_SUBSET_FAIL -eq 0 ] && [ $MSK_KINGS_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing msk_kingscounty study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_kingscounty" --temp-study-id="temporary_msk_kingscounty" --backup-study-id="yesterday_msk_kingscounty" --portal-name="msk-kingscounty-portal" --study-path="$MSK_KINGS_DATA_HOME" --notification-file="$kingscounty_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_KINGS=1
        sendFailureMessageMskPipelineLogsSlack "KINGSCOUNTY import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "KINGSCOUNTY"
    fi
else
    echo "Something went wrong with subsetting clinical studies for KINGSCOUNTY."
    IMPORT_FAIL_KINGS=1
fi
# commit or revert changes for KINGSCOUNTY
if [ $IMPORT_FAIL_KINGS -gt 0 ] ; then
    echo "KINGSCOUNTY subset and/or updates failed! Reverting data to last commit."
    cd $MSK_KINGS_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing KINGSCOUNTY data"
    cd $MSK_KINGS_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest KINGSCOUNTY dataset"
fi

# update msk_lehighvalley in portal only if subset was successful and metadata headers were added
if [ $MSK_LEHIGH_SUBSET_FAIL -eq 0 ] && [ $MSK_LEHIGH_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing msk_lehighvalley study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_lehighvalley" --temp-study-id="temporary_msk_lehighvalley" --backup-study-id="yesterday_msk_lehighvalley" --portal-name="msk-lehighvalley-portal" --study-path="$MSK_LEHIGH_DATA_HOME" --notification-file="$lehighvalley_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_LEHIGH=1
        sendFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "LEHIGHVALLEY"
    fi
else
    echo "Something went wrong with subsetting clinical studies for LEHIGHVALLEY."
    IMPORT_FAIL_LEHIGH=1
fi
# commit or revert changes for LEHIGHVALLEY
if [ $IMPORT_FAIL_LEHIGH -gt 0 ] ; then
    echo "LEHIGHVALLEY subset and/or updates failed! Reverting data to last commit."
    cd $MSK_LEHIGH_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing LEHIGHVALLEY data"
    cd $MSK_LEHIGH_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest LEHIGHVALLEY dataset"
fi

# update msk_queenscancercenter in portal only if subset was successful and metadata headers were added
if [ $MSK_QUEENS_SUBSET_FAIL -eq 0 ] && [ $MSK_QUEENS_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing msk_queenscancercenter study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_queenscancercenter" --temp-study-id="temporary_msk_queenscancercenter" --backup-study-id="yesterday_msk_queenscancercenter" --portal-name="msk-queenscancercenter-portal" --study-path="$MSK_QUEENS_DATA_HOME" --notification-file="$queenscancercenter_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_QUEENS=1
        sendFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "QUEENSCANCERCENTER"
    fi
else
    echo "Something went wrong with subsetting clinical studies for QUEENSCANCERCENTER."
    IMPORT_FAIL_QUEENS=1
fi
# commit or revert changes for QUEENSCANCERCENTER
if [ $IMPORT_FAIL_QUEENS -gt 0 ] ; then
    echo "QUEENSCANCERCENTER subset and/or updates failed! Reverting data to last commit."
    cd $MSK_QUEENS_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing QUEENSCANCERCENTER data"
    cd $MSK_QUEENS_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest QUEENSCANCERCENTER dataset"
fi

# update msk_miamicancerinstitute in portal only if subset was successful and metadata headers were added
if [ $MSK_MCI_SUBSET_FAIL -eq 0 ] && [ $MSK_MCI_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing msk_miamicancerinstitute study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_miamicancerinstitute" --temp-study-id="temporary_msk_miamicancerinstitute" --backup-study-id="yesterday_msk_miamicancerinstitute" --portal-name="msk-mci-portal" --study-path="$MSK_MCI_DATA_HOME" --notification-file="$miamicancerinstitute_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_MCI=1
        sendFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE"
    fi
else
    echo "Something went wrong with subsetting clinical studies for MIAMICANCERINSTITUTE."
    IMPORT_FAIL_MCI=1
fi
# commit or revert changes for MIAMICANCERINSTITUTE
if [ $IMPORT_FAIL_MCI -gt 0 ] ; then
    echo "MIAMICANCERINSTITUTE subset and/or updates failed! Reverting data to last commit."
    cd $MSK_MCI_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing MIAMICANCERINSTITUTE data"
    cd $MSK_MCI_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest MIAMICANCERINSTITUTE dataset"
fi

# update msk_hartfordhealthcare in portal only if subset was successful and metadata headers were added
if [ $MSK_HARTFORD_SUBSET_FAIL -eq 0 ] && [ $MSK_HARTFORD_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing msk_hartfordhealthcare study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_hartfordhealthcare" --temp-study-id="temporary_msk_hartfordhealthcare" --backup-study-id="yesterday_msk_hartfordhealthcare" --portal-name="msk-hartford-portal" --study-path="$MSK_HARTFORD_DATA_HOME" --notification-file="$hartfordhealthcare_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_HARTFORD=1
        sendFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE"
    fi
else
    echo "Something went wrong with subsetting clinical studies for HARTFORDHEALTHCARE."
    IMPORT_FAIL_HARTFORD=1
fi
# commit or revert changes for HARTFORDHEALTHCARE
if [ $IMPORT_FAIL_HARTFORD -gt 0 ] ; then
    echo "HARTFORDHEALTHCARE subset and/or updates failed! Reverting data to last commit."
    cd $MSK_HARTFORD_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing HARTFORDHEALTHCARE data"
    cd $MSK_HARTFORD_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest HARTFORDHEALTHCARE dataset"
fi

## END Subset MIXEDPACT on INSTITUTE
#-------------------------------------------------------------------------------------------------------------------------------------
# Subset MSKIMPACT on ONCOTREE_CODE for SCLC cohort

RESTART_AFTER_SCLC_IMPORT=0
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=sclc_mskimpact_2017 -o=$MSK_SCLC_DATA_HOME -m=$MSK_IMPACT_DATA_HOME -f="ONCOTREE_CODE=SCLC" -s=$JAVA_TMPDIR/sclc_subset.txt -c=data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSKIMPACT SCLC subset failed! Study will not be updated in the portal."
    sendFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT subset"
    SCLC_MSKIMPACT_SUBSET_FAIL=1
else
    echo "MSKIMPACT SCLC subset successful!"
    addCancerTypeCaseLists $MSK_SCLC_DATA_HOME "sclc_mskimpact_2017" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # add metadata headers before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s sclc_mskimpact_2017 -f $MSK_SCLC_DATA_HOME/data_clinical*
    if [ $? -gt 0 ] ; then
        SCLC_MSKIMPACT_ADD_HEADER_FAIL=1
        echo "Something went wrong while adding metadata headers for MSKIMPACT SCLC."
    fi
fi

# update sclc_mskimpact_2017 in portal only if subset was successful and metadata headers were added
if [ $SCLC_MSKIMPACT_SUBSET_FAIL -eq 0 ] && [ $SCLC_MSKIMPACT_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing sclc_mskimpact_2017 study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="sclc_mskimpact_2017" --temp-study-id="temporary_sclc_mskimpact_2017" --backup-study-id="yesterday_sclc_mskimpact_2017" --portal-name="msk-sclc-portal" --study-path="$MSK_SCLC_DATA_HOME" --notification-file="$sclc_mskimpact_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_SCLC_MSKIMPACT=1
        sendFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT import"
    else
        RESTART_AFTER_SCLC_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "SCLCMSKIMPACT"
    fi
else
    echo "Something went wrong with subsetting clinical studies for SCLCMSKIMPACT."
    IMPORT_FAIL_SCLC_MSKIMPACT=1
fi
# commit or revert changes for SCLCMSKIMPACT
if [ $IMPORT_FAIL_SCLC_MSKIMPACT -gt 0 ] ; then
    echo "SCLCMSKIMPACT subset and/or updates failed! Reverting data to last commit."
    cd $MSK_SCLC_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing SCLCMSKIMPACT data"
    cd $MSK_SCLC_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest SCLCMSKIMPACT dataset"
fi
# END Subset MSKIMPACT on ONCOTREE_CODE for SCLC cohort
#-------------------------------------------------------------------------------------------------------------------------------------
# Create lymphoma "super" cohort
# Subset MSK-IMPACT and HEMEPACT by Cancer Type

# first touch meta_clinical.txt and meta_SV.txt in mskimpact, hemepact, and fmibat if not already exists - need these to generate merged subsets
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ] ; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

# **************************************** ORDER OF SUBSET
# now subset sample files with lymphoma cases from mskimpact and hemepact
LYMPHOMA_FILTER_CRITERIA="CANCER_TYPE=Non-Hodgkin Lymphoma,Hodgkin Lymphoma"
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$JAVA_TMPDIR/mskimpact_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_HEMEPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$JAVA_TMPDIR/mskimpact_heme_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT Heme. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

# check sizes of subset files before attempting to merge data using these subsets
grep -v '^#' $FMI_BATLEVI_DATA_HOME/data_clinical_sample.txt | awk -F '\t' '{if ($2 != "SAMPLE_ID") print $2;}' > $JAVA_TMPDIR/lymphoma_subset_samples.txt
if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $JAVA_TMPDIR/lymphoma_subset_samples.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $JAVA_TMPDIR/lymphoma_subset_samples.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from source Foundation study"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $JAVA_TMPDIR/mskimpact_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $JAVA_TMPDIR/mskimpact_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $JAVA_TMPDIR/mskimpact_lymphoma_subset.txt >> $JAVA_TMPDIR/lymphoma_subset_samples.txt
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $JAVA_TMPDIR/mskimpact_heme_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $JAVA_TMPDIR/mskimpact_heme_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $JAVA_TMPDIR/mskimpact_heme_lymphoma_subset.txt >> $JAVA_TMPDIR/lymphoma_subset_samples.txt
fi

# merge data from mskimpact and hemepact lymphoma subsets with FMI BAT study
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ] ; then
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py  -d $LYMPHOMA_SUPER_COHORT_DATA_HOME -i "lymphoma_super_cohort_fmi_msk" -m "true" -s $JAVA_TMPDIR/lymphoma_subset_samples.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $FMI_BATLEVI_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "Lymphoma super cohort subset failed! Lymphoma super cohort study will not be updated in the portal."
        sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT merge"
        LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
    else
        # add metadata headers before importing
        $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -f $LYMPHOMA_SUPER_COHORT_DATA_HOME/data_clinical*
        if [ $? -gt 0 ] ; then
            LYMPHOMA_SUPER_COHORT_ADD_HEADER_FAIL=1
            echo "Something went wrong while adding metadata headers for LYMPHOMASUPERCOHORT."
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

# attempt to import if merge and subset successful and metadata headers were added
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ] && [ $LYMPHOMA_SUPER_COHORT_ADD_HEADER_FAIL -eq 0 ] ; then
    echo "Importing lymphoma 'super' cohort study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="lymphoma_super_cohort_fmi_msk" --temp-study-id="temporary_lymphoma_super_cohort_fmi_msk" --backup-study-id="yesterday_lymphoma_super_cohort_fmi_msk" --portal-name="msk-fmi-lymphoma-portal" --study-path="$LYMPHOMA_SUPER_COHORT_DATA_HOME" --notification-file="$lymphoma_super_cohort_notification_file" --tmp-directory="$JAVA_TMPDIR" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ] ; then
        IMPORT_FAIL_LYMPHOMA=1
        sendFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT import"
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
        sendSuccessMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT"
    fi
else
    echo "Something went wrong with subsetting clinical studies for Lymphoma super cohort."
    IMPORT_FAIL_LYMPHOMA=1
fi

# commit or revert changes for Lymphoma super cohort
if [ $IMPORT_FAIL_LYMPHOMA -gt 0 ] ; then
    echo "Lymphoma super cohort subset and/or updates failed! Reverting data to last commit."
    cd $LYMPHOMA_SUPER_COHORT_DATA_HOME ; $HG_BINARY update -C ; find . -name "*.orig" -delete
else
    echo "Committing Lymphoma super cohort data"
    cd $LYMPHOMA_SUPER_COHORT_DATA_HOME ; find . -name "*.orig" -delete ; $HG_BINARY add * ; $HG_BINARY commit -m "Latest Lymphoma Super Cohort dataset"
fi

## TOMCAT RESTART
# Restart will only execute if at least one of these studies succesfully updated.
#   MSK_KINGSCOUNTY
#   MSK_LEHIGHVALLEY
#   MSK_QUEENSCANCERCENTER
#   MSK_MIAMICANCERINSTITUTE
#   MSK_HARTFORDHEALTHCARE
#   LYMPHOMASUPERCOHORT
#   SCLCMSKIMPACT

if [ $RESTART_AFTER_MSK_AFFILIATE_IMPORT -eq 0 ] ; then
    echo "Failed to update all MSK affiliate studies"
else
    restartMSKTomcats
fi

## SCHULTZ TOMCAT RESTART
# Restart only if sclc_mskimpact_2017 import succeeded
if [ $RESTART_AFTER_SCLC_IMPORT -eq 0 ] ; then
    echo "Failed to update SCLC MSKIMPCAT cohort"
else
    restartSchultzTomcats
fi

# check updated data back into mercurial
echo "Pushing DMP-IMPACT updates back to msk-impact repository..."
echo $(date)
cd $MSK_IMPACT_DATA_HOME ; $HG_BINARY push

### FAILURE EMAIL ###

EMAIL_BODY="The MSKIMPACT database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Update Failure: DB version is incompatible" $email_list
fi

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

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, and RAINDANCE data. Merged study will not be updated."
if [ $MIXEDPACT_MERGE_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIXEDPACT Merge Failure: Study will not be updated." $email_list
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

EMAIL_BODY="Failed to subset MSKIMPACT SCLC data. Subset study will not be updated."
if [ $SCLC_MSKIMPACT_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "SCLCMSKIMPACT Subset Failure: Study will not be updated." $email_list
fi

echo "Fetching and importing of clinical datasets complete!"
echo $(date)


exit 0
