#!/bin/bash

# take snapshot of REDCap projects for MSKIMPACT, HEMEPACT, ARCHER
echo $(date)
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
SLACK_PIPELINES_MONITOR_URL=`cat $SLACK_URL_FILE`

# flags for REDCap export status
MSKIMPACT_REDCAP_EXPORT_FAIL=0
HEMEPACT_REDCAP_EXPORT_FAIL=0
ARCHER_REDCAP_EXPORT_FAIL=0
ACCESS_REDCAP_EXPORT_FAIL=0

MSKIMPACT_VALIDATION_FAIL=0
HEMEPACT_VALIDATION_FAIL=0
ARCHER_VALIDATION_FAIL=0
ACCESS_VALIDATION_FAIL=0

# -----------------------------------------------------------------------------------------------------------
# FUNCTIONS

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"REDCap backup failed: $MESSAGE\", \"icon_emoji\": \":fire:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# Function for alerting slack channel of successful imports
function sendSuccessMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"REDCap data backup succeeded! $MESSAGE\", \"icon_emoji\": \":tada:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# Validate exported REDCap data
function validateRedcapExportForStudy {
    # (1): Input directory
    input_directory=$1
    invalid_files=""

    # go through each data file that was exported and check that file contains more than one line (more than just a header)
    for f in $input_directory/data*; do
        if [ ! $(wc -l < $f) -gt 1 ]; then
            if [[ "$f" != *data_clinical_mskarcher_data_clinical_ddp_age_at_seq.txt ]]; then
                invalid_files="$invalid_files\t$f\n"
            fi
        fi
    done

    if [ ! -z "$invalid_files" ]; then
        echo "ERROR:  validateRedcapExportForStudy(), No data was written to file(s):"
        echo -e "$invalid_files"
        return 1
    fi
    return 0
}

# -----------------------------------------------------------------------------------------------------------
# REDCAP EXPORTS
REDCAP_JAR_FILENAME="$PORTAL_HOME/lib/redcap_pipeline.jar"
JAVA_REDCAP_PIPELINE_ARGS="$JAVA_SSL_ARGS -jar $REDCAP_JAR_FILENAME"
# update snapshot repo with latest changes
cd $REDCAP_BACKUP_DATA_HOME; $GIT_BINARY pull ; $GIT_BINARY clean -fd

# export and commit MSKIMPACT REDCap data
echo "Exporting MSKIMPACT REDCap data..."
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskimpact -d $MSKIMPACT_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for MSKIMPACT! Aborting any changes made during export..."
    cd $MSKIMPACT_REDCAP_BACKUP; $GIT_BINARY checkout -- .
    MSKIMPACT_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT export"
else
    validateRedcapExportForStudy $MSKIMPACT_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of MSKIMPACT REDCap snapshot failed! Aborting any changes made during export..."
        MSKIMPACT_VALIDATION_FAIL=1
        cd $MSKIMPACT_REDCAP_BACKUP; $GIT_BINARY checkout -- .
        MSKIMPACT_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT validation"
    else
        echo "Committing MSKIMPACT REDCap data snapshot"
        cd $MSKIMPACT_REDCAP_BACKUP; $GIT_BINARY add -A . ; $GIT_BINARY commit -m "MSKIMPACT REDCap Snapshot"
    fi
fi

# export and commit HEMEPACT REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskimpact_heme -d $HEMEPACT_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for HEMEPACT! Aborting any changes made during export..."
    cd $HEMEPACT_REDCAP_BACKUP; $GIT_BINARY checkout -- .
    HEMEPACT_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT export"
else
    validateRedcapExportForStudy $HEMEPACT_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of HEMEPACT REDCap snapshot failed! Aborting any changes made during export..."
        HEMEPACT_VALIDATION_FAIL=1
        cd $HEMEPACT_REDCAP_BACKUP; $GIT_BINARY checkout -- .
        HEMEPACT_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT validation"
    else
        echo "Committing HEMEPACT REDCap data snapshot"
        cd $HEMEPACT_REDCAP_BACKUP; $GIT_BINARY add -A . ; $GIT_BINARY commit -m "HEMEPACT REDCap Snapshot"
    fi
fi

# export and commit ARCHER REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskarcher -d $ARCHER_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for ARCHER! Aborting any changes made during export..."
    cd $ARCHER_REDCAP_BACKUP; $GIT_BINARY checkout -- .
    ARCHER_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "ARCHER export"
else
    validateRedcapExportForStudy $ARCHER_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of ARCHER REDCap snapshot failed! Aborting any changes made during export..."
        ARCHER_VALIDATION_FAIL=1
        cd $ARCHER_REDCAP_BACKUP; $GIT_BINARY checkout -- .
        ARCHER_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "ARCHER validation"
    else
        echo "Committing ARCHER REDCap data snapshot"
        cd $ARCHER_REDCAP_BACKUP; $GIT_BINARY add -A . ; $GIT_BINARY commit -m "ARCHER REDCap Snapshot"
    fi
fi

# export and commit ACCESS REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskaccess -d $ACCESS_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for ACCESS! Aborting any changes made during export..."
    cd $ACCESS_REDCAP_BACKUP; $GIT_BINARY checkout -- .
    ACCESS_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "ACCESS export"
else
    validateRedcapExportForStudy $ACCESS_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of ACCESS REDCap snapshot failed! Aborting any changes made during export..."
        ACCESS_VALIDATION_FAIL=1
        cd $ACCESS_REDCAP_BACKUP; $GIT_BINARY checkout -- .
        ACCESS_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "ACCESS validation"
    else
        echo "Committing ACCESS REDCap data snapshot"
        cd $ACCESS_REDCAP_BACKUP; $GIT_BINARY add -A . ; $GIT_BINARY commit -m "ACCESS REDCap Snapshot"
    fi
fi

# push outgoing changesets to snapshot repo
echo "Pushing REDCap snapshot back to git repository..."
echo $(date)
cd $REDCAP_BACKUP_DATA_HOME; $GIT_BINARY push

# slack successful backup message
if [[ $MSKIMPACT_REDCAP_EXPORT_FAIL -eq 0 && $MSKIMPACT_VALIDATION_FAIL -eq 0 && $HEMEPACT_REDCAP_EXPORT_FAIL -eq 0 && $HEMEPACT_VALIDATION_FAIL -eq 0 && $ARCHER_REDCAP_EXPORT_FAIL -eq 0 && $ARCHER_VALIDATION_FAIL -eq 0 && $ACCESS_REDCAP_EXPORT_FAIL -eq 0 && $ACCESS_VALIDATION_FAIL -eq 0 ]]; then
    sendSuccessMessageMskPipelineLogsSlack "ALL studies"
else
    if [[ $MSKIMPACT_REDCAP_EXPORT_FAIL -eq 0 && $MSKIMPACT_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "MSKIMPACT"
    fi
    if [[ $HEMEPACT_REDCAP_EXPORT_FAIL -eq 0 && $HEMEPACT_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "HEMEPACT"
    fi
    if [[ $ARCHER_REDCAP_EXPORT_FAIL -eq 0 && $ARCHER_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "ARCHER"
    fi
    if [[ $ACCESS_REDCAP_EXPORT_FAIL -eq 0 && $ACCESS_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "ACCESS"
    fi
fi

# -----------------------------------------------------------------------------------------------------------
# SEND EMAILS

# send emails for export failures
EMAIL_BODY="Failed to backup MSKIMPACT REDCap data"
if [ $MSKIMPACT_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: MSKIMPACT REDCap Backup Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to backup HEMEPACT REDCap data"
if [ $HEMEPACT_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: HEMEPACT REDCap Backup Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to backup ARCHER REDCap data"
if [ $ARCHER_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ARCHER REDCap Backup Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to backup ACCESS REDCap data"
if [ $ACCESS_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ACCESS REDCap Backup Failure" $PIPELINES_EMAIL_LIST
fi

# send emails for validation failures
EMAIL_BODY="Validation of MSKIMPACT REDCap data failed"
if [ $MSKIMPACT_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: MSKIMPACT REDCap Data Validation Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Validation of HEMEPACT REDCap data failed"
if [ $HEMEPACT_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: HEMEPACT REDCap Data Validation Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Validation of ARCHER REDCap data failed"
if [ $ARCHER_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ARCHER REDCap Data Validation Failure" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Validation of ACCESS REDCap data failed"
if [ $ACCESS_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ACCESS REDCap Data Validation Failure" $PIPELINES_EMAIL_LIST
fi
