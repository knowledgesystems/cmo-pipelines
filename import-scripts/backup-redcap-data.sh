#!/bin/bash

# take snapshot of REDCap projects for MSKIMPACT, RAINDANCE, HEMEPACT, ARCHER
echo $(date)
email_list="cbioportal-pipelines@cbio.mskcc.org"

# flags for REDCap export status
MSKIMPACT_REDCAP_EXPORT_FAIL=0
RAINDANCE_REDCAP_EXPORT_FAIL=0
HEMEPACT_REDCAP_EXPORT_FAIL=0
ARCHER_REDCAP_EXPORT_FAIL=0

MSKIMPACT_VALIDATION_FAIL=0
RAINDANCE_VALIDATION_FAIL=0
HEMEPACT_VALIDATION_FAIL=0
ARCHER_VALIDATION_FAIL=0

# -----------------------------------------------------------------------------------------------------------
# FUNCTIONS

# Function for alerting slack channel of any failures
function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"REDCap backup failed: $MESSAGE\", \"icon_emoji\": \":fire:\"}" https://hooks.slack.com/services/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
}

# Function for alerting slack channel of successful imports
function sendSuccessMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"REDCap data backup succeeded! $MESSAGE\", \"icon_emoji\": \":tada:\"}" https://hooks.slack.com/services/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
}

# Validate exported REDCap data
function validateRedcapExportForStudy {
    # (1): Input directory
    input_directory=$1
    invalid_files=""

    # go through each data file that was exported and check that file contains more than one line (more than just a header)
    for f in $input_directory/data*; do
        if [ ! $(wc -l < $f) -gt 1 ]; then
            invalid_files="$invalid_files\t$f\n"
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
JAVA_REDCAP_PIPELINE_ARGS="-jar $REDCAP_JAR_FILENAME"
# update mercurial repo with latest changes
$HG_BINARY pull -u

# export and commit MSKIMPACT REDCap data
echo "Exporting MSKIMPACT REDCap data..."
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskimpact -d $MSKIMPACT_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for MSKIMPACT! Aborting any changes made during export..."
    cd $MSKIMPACT_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
    MSKIMPACT_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "MSKIMPACT export"
else
    validateRedcapExportForStudy $MSKIMPACT_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of MSKIMPACT REDCap snapshot failed! Aborting any changes made during export..."
        MSKIMPACT_VALIDATION_FAIL=1
        cd $MSKIMPACT_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
        MSKIMPACT_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "MSKIMPACT validation"
    else
        echo "Committing MSKIMPACT REDCap data snapshot"
        cd $MSKIMPACT_REDCAP_BACKUP; $HG_BINARY commit -m "MSKIMPACT REDCap Snapshot"
    fi
fi

# export and commit RAINDANCE REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskraindance -d $RAINDANCE_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for RAINDANCE! Aborting any changes made during export..."
    cd $RAINDANCE_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
    RAINDANCE_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "RAINDANCE export"
else
    validateRedcapExportForStudy $RAINDANCE_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of RAINDANCE REDCap snapshot failed! Aborting any changes made during export..."
        RAINDANCE_VALIDATION_FAIL=1
        cd $RAINDANCE_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
        RAINDANCE_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "RAINDANCE validation"
    else
        echo "Committing RAINDANCE REDCap data snapshot"
        cd $RAINDANCE_REDCAP_BACKUP; $HG_BINARY commit -m "RAINDANCE REDCap Snapshot"
    fi
fi

# export and commit HEMEPACT REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskimpact_heme -d $HEMEPACT_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for HEMEPACT! Aborting any changes made during export..."
    cd $HEMEPACT_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
    HEMEPACT_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "HEMEPACT export"
else
    validateRedcapExportForStudy $HEMEPACT_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of HEMEPACT REDCap snapshot failed! Aborting any changes made during export..."
        HEMEPACT_VALIDATION_FAIL=1
        cd $HEMEPACT_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
        HEMEPACT_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "HEMEPACT validation"
    else
        echo "Committing HEMEPACT REDCap data snapshot"
        cd $HEMEPACT_REDCAP_BACKUP; $HG_BINARY commit -m "HEMEPACT REDCap Snapshot"
    fi
fi

# export and commit ARCHER REDCap data
$JAVA_BINARY $JAVA_REDCAP_PIPELINE_ARGS -e -r -s mskarcher -d $ARCHER_REDCAP_BACKUP
if [ $? -gt 0 ]; then
    echo "Failed to export REDCap data snapshot for ARCHER! Aborting any changes made during export..."
    cd $ARCHER_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
    ARCHER_REDCAP_EXPORT_FAIL=1
    sendFailureMessageMskPipelineLogsSlack "ARCHER export"
else
    validateRedcapExportForStudy $ARCHER_REDCAP_BACKUP
    if [ $? -gt 0 ]; then
        echo "Validation of ARCHER REDCap snapshot failed! Aborting any changes made during export..."
        ARCHER_VALIDATION_FAIL=1
        cd $ARCHER_REDCAP_BACKUP; $HG_BINARY update -C; rm *.orig
        ARCHER_REDCAP_EXPORT_FAIL=1
        sendFailureMessageMskPipelineLogsSlack "ARCHER validation"
    else
        echo "Committing ARCHER REDCap data snapshot"
        cd $ARCHER_REDCAP_BACKUP; $HG_BINARY commit -m "ARCHER REDCap Snapshot"
    fi
fi

# push outgoing changesets to mercurial repo
echo "Pushing REDCap snapshot back to mercurial repository..."
echo $(date)
cd $REDCAP_BACKUP_DATA_HOME; $HG_BINARY push

# slack successful backup message
if [[ $MSKIMPACT_REDCAP_EXPORT_FAIL -eq 0 && $MSKIMPACT_VALIDATION_FAIL -eq 0 && $RAINDANCE_REDCAP_EXPORT_FAIL -eq 0 && $RAINDANCE_VALIDATION_FAIL -eq 0 && $HEMEPACT_REDCAP_EXPORT_FAIL -eq 0 && $HEMEPACT_VALIDATION_FAIL -eq 0 && $ARCHER_REDCAP_EXPORT_FAIL -eq 0 && $ARCHER_VALIDATION_FAIL -eq 0 ]]; then
    sendSuccessMessageMskPipelineLogsSlack "ALL studies"
else
    if [[ $MSKIMPACT_REDCAP_EXPORT_FAIL -eq 0 && $MSKIMPACT_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "MSKIMPACT"
    fi
    if [[ $RAINDANCE_REDCAP_EXPORT_FAIL -eq 0 && $RAINDANCE_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "RAINDANCE"
    fi
    if [[ $HEMEPACT_REDCAP_EXPORT_FAIL -eq 0 && $HEMEPACT_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "HEMEPACT"
    fi
    if [[ $ARCHER_REDCAP_EXPORT_FAIL -eq 0 && $ARCHER_VALIDATION_FAIL -eq 0 ]] ; then
        sendSuccessMessageMskPipelineLogsSlack "ARCHER"
    fi
fi

# -----------------------------------------------------------------------------------------------------------
# SEND EMAILS

# send emails for export failures
EMAIL_BODY="Failed to backup MSKIMPACT REDCap data"
if [ $MSKIMPACT_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: MSKIMPACT REDCap Backup Failure" $email_list
fi

EMAIL_BODY="Failed to backup RAINDANCE REDCap data"
if [ $RAINDANCE_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: RAINDANCE REDCap Backup Failure" $email_list
fi

EMAIL_BODY="Failed to backup HEMEPACT REDCap data"
if [ $HEMEPACT_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: HEMEPACT REDCap Backup Failure" $email_list
fi

EMAIL_BODY="Failed to backup ARCHER REDCap data"
if [ $ARCHER_REDCAP_EXPORT_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ARCHER REDCap Backup Failure" $email_list
fi

# send emails for validation failures
EMAIL_BODY="Validation of MSKIMPACT REDCap data failed"
if [ $MSKIMPACT_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: MSKIMPACT REDCap Data Validation Failure" $email_list
fi

EMAIL_BODY="Validation of RAINDANCE REDCap data failed"
if [ $RAINDANCE_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: RAINDANCE REDCap Data Validation Failure" $email_list
fi

EMAIL_BODY="Validation of HEMEPACT REDCap data failed"
if [ $HEMEPACT_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: HEMEPACT REDCap Data Validation Failure" $email_list
fi

EMAIL_BODY="Validation of ARCHER REDCap data failed"
if [ $ARCHER_VALIDATION_FAIL -gt 0 ]; then
    echo "Sending email $EMAIL_BODY"
    echo $EMAIL_BODY | mail -s "[URGENT]: ARCHER REDCap Data Validation Failure" $email_list
fi
