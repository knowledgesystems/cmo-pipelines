#!/bin/bash

# Temp study importer arguments
# (1): cancer study id [ mskimpact | mskarcher | msk_kingscounty | msk_lehighvalley | msk_queenscancercenter | msk_miamicancerinstitute | msk_hartfordhealthcare | msk_ralphlauren | msk_rikengenesisjapan | mskimpact_ped | sclc_mskimpact_2017 | lymphoma_super_cohort_fmi_msk ]
# (2): temp study id [ temporary_mskimpact | temporary_mskarcher | temporary_msk_kingscounty | temporary_msk_lehighvalley | temporary_msk_queenscancercenter | temporary_msk_miamicancerinstitute | temporary_msk_hartfordhealthcare | temporary_msk_ralphlauren | temporary_msk_rikengenesisjapan | temporary_mskimpact_ped | temporary_sclc_mskimpact_2017 | temporary_lymphoma_super_cohort_fmi_msk]
# (3): backup study id [ yesterday_mskimpact | yesterday_mskarcher | yesterday_msk_kingscounty | yesterday_msk_lehighvalley | yesterday_msk_queenscancercenter | yesterday_msk_miamicancerinstitute | yesterday_msk_hartfordhealthcare | yesterday_msk_ralphlauren | yesterday_msk_rikengenesisjapan | yesterday_mskimpact_ped | yesterday_sclc_mskimpact_2017 | yesterday_lymphoma_super_cohort_fmi_msk]
# (4): portal name [ msk-solid-heme-portal | mskarcher-portal |  msk-kingscounty-portal | msk-lehighvalley-portal | msk-queenscancercenter-portal | msk-mci-portal | msk-hartford-portal | msk-ralphlauren-portal | msk-tailormedjapan-portal | msk-ped-portal | msk-sclc-portal | msk-fmi-lymphoma-portal ]
# (5): study path [ $MSK_SOLID_HEME_DATA_HOME | $MSK_ARCHER_DATA_HOME | $MSK_KINGS_DATA_HOME | $MSK_LEHIGH_DATA_HOME | $MSK_QUEENS_DATA_HOME | $MSK_MCI_DATA_HOME | $MSK_HARTFORD_DATA_HOME | $MSK_RALPHLAUREN_DATA_HOME | $MSK_RIKENGENESISJAPAN_DATA_HOME | $MSKIMPACT_PED_DATA_HOME | $MSK_SCLC_DATA_HOME | $LYMPHOMA_SUPER_COHORT_DATA_HOME ]
# (6): notification file [ $msk_solid_heme_notification_file | $kingscounty_notification_file | $lehighvalley_notification_file | $queenscancercenter_notification_file | $miamicancerinstitute_notification_file | $hartfordhealthcare_notification_file | $ralphlauren_notification_file | $rikengenesisjapan_notification_file | $mskimpact_ped_notification_file | $sclc_mskimpact_notification_file | $lymphoma_super_cohort_notification_file ]
# (7): tmp directory
# (8): email list
# (9): oncotree version [ oncotree_candidate_release | oncotree_latest_stable ]
# (10): importer jar
# (11): transcript overrides source [ uniprot | mskcc ]

# Non-zero exit code status indication
# There are several flags that are checked during the execution of the temporary study import. If any flags are non-zero at the end
# of execution then an email is sent out to the email list provided for each non-zero flag and the script exits with a non-zero status.
# Flags:
#     IMPORT_FAIL            if non-zero indicates that the study failed to import under a temporary id
#     VALIDATION_FAIL        if non-zero indicates that the temporary study failed validation against the original study
#     DELETE_FAIL            if non-zero indicates that the backup study failed to delete
#     RENAME_BACKUP_FAIL     if non-zero indicates that the original study failed to rename to the backup study id
#     RENAME_FAIL            if non-zero indicates that the temporary study failed to rename to the original study id

function usage {
    echo "import-temp-study.sh"
    echo -e "\t-i | --study-id                      cancer study identifier"
    echo -e "\t-t | --temp-study-id                 temp study identifier"
    echo -e "\t-b | --backup-study-id               backup study identifier"
    echo -e "\t-p | --portal-name                   portal name"
    echo -e "\t-s | --study-path                    study path"
    echo -e "\t-n | --notification-file             notification file"
    echo -e "\t-d | --tmp-directory                 tmp directory"
    echo -e "\t-e | --email-list                    email list"
    echo -e "\t-o | --oncotree-version              oncotree version"
    echo -e "\t-j | --importer-jar                  importer jar"
    echo -e "\t-r | --transcript-overrides-source   transcript overrides source"
    echo -e "\t-a | --allow-redcap-export           allow redcap export during import (overrides --disable-redcap-export default)"
}

function sendFailureMessageMskPipelineLogsSlack {
    MESSAGE=$1
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"MSK temporary study import process failed: $MESSAGE\", \"icon_emoji\": \":tired_face:\"}" $SLACK_PIPELINES_MONITOR_URL
}

# set default value(s)
DISABLE_REDCAP_EXPORT_TERM="--disable-redcap-export"
ONCOTREE_VERSION_TERM=""

echo "Input arguments:"
for i in "$@"; do
case $i in
    -i=*|--study-id=*)
    CANCER_STUDY_IDENTIFIER="${i#*=}"
    echo -e "\tstudy id=$CANCER_STUDY_IDENTIFIER"
    shift
    ;;
    -t=*|--temp-study-id=*)
    TEMP_CANCER_STUDY_IDENTIFIER="${i#*=}"
    echo -e "\ttemp id=$TEMP_CANCER_STUDY_IDENTIFIER"
    shift
    ;;
    -b=*|--backup-study-id=*)
    BACKUP_CANCER_STUDY_IDENTIFIER="${i#*=}"
    echo -e "\tbackup id=$BACKUP_CANCER_STUDY_IDENTIFIER"
    shift
    ;;
    -p=*|--portal-name=*)
    PORTAL_NAME="${i#*=}"
    echo -e "\tportal name=$PORTAL_NAME"
    shift
    ;;
    -s=*|--study-path=*)
    STUDY_PATH="${i#*=}"
    echo -e "\tstudy path=$STUDY_PATH"
    shift
    ;;
    -n=*|--notification-file=*)
    NOTIFICATION_FILE="${i#*=}"
    echo -e "\tnotifcation file=$NOTIFICATION_FILE"
    shift
    ;;
    -d=*|--tmp-directory=*)
    TMP_DIRECTORY="${i#*=}"
    echo -e "\ttmp dir=$TMP_DIRECTORY"
    shift
    ;;
    -e=*|--email-list=*)
    EMAIL_LIST="${i#*=}"
    echo -e "\temail list=$EMAIL_LIST"
    shift
    ;;
    -o=*|--oncotree-version=*)
    ONCOTREE_VERSION_TERM="--oncotree-version ${i#*=}"
    echo -e "\toncotree version=${i#*=}"
    shift
    ;;
    -j=*|--importer-jar=*)
    IMPORTER_JAR_FILENAME="${i#*=}"
    echo -e "\timporter jar=$IMPORTER_JAR_FILENAME"
    shift
    ;;
    -r=*|--transcript-overrides-source=*)
    TRANSCRIPT_OVERRIDES_SOURCE="${i#*=}"
    echo -e "\ttranscript overrides source=$TRANSCRIPT_OVERRIDES_SOURCE"
    shift
    ;;
    -a|--allow-redcap-export)
    DISABLE_REDCAP_EXPORT_TERM=""
    echo -e "\tallow redcap export=true"
    shift
    ;;
    *)
    ;;
esac
done

if [[ -z $CANCER_STUDY_IDENTIFIER || -z $TEMP_CANCER_STUDY_IDENTIFIER || -z $BACKUP_CANCER_STUDY_IDENTIFIER || -z $PORTAL_NAME || -z $STUDY_PATH || -z $NOTIFICATION_FILE || -z $TMP_DIRECTORY || -z $EMAIL_LIST || -z $IMPORTER_JAR_FILENAME || -z $TRANSCRIPT_OVERRIDES_SOURCE ]]; then
    usage
    exit 1
fi

ENABLE_DEBUGGING=0
java_debug_args=""
if [ $ENABLE_DEBUGGING != "0" ] ; then
    java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182"
fi
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$TMP_DIRECTORY -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
GROUP_FOR_HIDING_BACKUP_STUDIES="KSBACKUP"
SLACK_PIPELINES_MONITOR_URL=`cat $SLACK_URL_FILE`

# define validator notification filename based on cancer study id, remove if already exists, touch new file
now=$(date "+%Y-%m-%d-%H-%M-%S")
VALIDATION_NOTIFICATION_FILENAME="$(mktemp $TMP_DIRECTORY/validation_$CANCER_STUDY_IDENTIFIER.$now.XXXXXX)"
if [ -f $VALIDATION_NOTIFICATION_FILENAME ]; then
    rm $VALIDATION_NOTIFICATION_FILENAME
fi
touch $VALIDATION_NOTIFICATION_FILENAME

# variables for import temp study status
IMPORT_FAIL=0
VALIDATION_FAIL=0
DELETE_FAIL=0
RENAME_BACKUP_FAIL=0
RENAME_FAIL=0
GROUPS_FAIL=0

# import study using temp id
echo "Importing study '$CANCER_STUDY_IDENTIFIER' as temporary study '$TEMP_CANCER_STUDY_IDENTIFIER'"
$JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal $PORTAL_NAME --notification-file $NOTIFICATION_FILE --temporary-id $TEMP_CANCER_STUDY_IDENTIFIER $ONCOTREE_VERSION_TERM --transcript-overrides-source $TRANSCRIPT_OVERRIDES_SOURCE $DISABLE_REDCAP_EXPORT_TERM
# we do not have to check the exit status here because if num_studies_updated != 1 we consider the import to have failed (we check num_studies_updated next)

# check number of studies updated before continuing
if [[ $? -eq 0 && -f "$TMP_DIRECTORY/num_studies_updated.txt" ]]; then
    num_studies_updated=`cat $TMP_DIRECTORY/num_studies_updated.txt`
else
    num_studies_updated=0
fi

if [ "$num_studies_updated" -ne 1 ]; then
    echo "Failed to import study '$CANCER_STUDY_IDENTIFIER'"
    IMPORT_FAIL=1
else
    # validate
    echo "Validating import..."
    $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --validate-temp-study --temp-study-id $TEMP_CANCER_STUDY_IDENTIFIER --original-study-id $CANCER_STUDY_IDENTIFIER --notification-file $VALIDATION_NOTIFICATION_FILENAME
    if [ $? -gt 0 ]; then
        echo "Failed to validate - deleting temp study '$TEMP_CANCER_STUDY_IDENTIFIER'"
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --delete-cancer-study --cancer-study-ids $TEMP_CANCER_STUDY_IDENTIFIER
        VALIDATION_FAIL=1
    else
        echo "Successful validation - renaming '$CANCER_STUDY_IDENTIFIER' and temp study '$TEMP_CANCER_STUDY_IDENTIFIER'"
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --delete-cancer-study --cancer-study-ids $BACKUP_CANCER_STUDY_IDENTIFIER
        if [ $? -gt 0 ]; then
            echo "Failed to delete backup study '$BACKUP_CANCER_STUDY_IDENTIFIER'!"
            DELETE_FAIL=1
        else
            echo "Renaming '$CANCER_STUDY_IDENTIFIER' to '$BACKUP_CANCER_STUDY_IDENTIFIER'"
            $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --rename-cancer-study --new-study-id $BACKUP_CANCER_STUDY_IDENTIFIER --original-study-id $CANCER_STUDY_IDENTIFIER
            if [ $? -gt 0 ]; then
                echo "Failed to rename existing '$CANCER_STUDY_IDENTIFIER' to backup study '$BACKUP_CANCER_STUDY_IDENTIFIER'!"
                RENAME_BACKUP_FAIL=1
            else
                echo "Updating groups of study '$BACKUP_CANCER_STUDY_IDENTIFIER' to '$GROUP_FOR_HIDING_BACKUP_STUDIES'"
                $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-groups --cancer-study-ids $BACKUP_CANCER_STUDY_IDENTIFIER --groups $GROUP_FOR_HIDING_BACKUP_STUDIES
                if [ $? -gt 0 ]; then
                    echo "Failed to change groups for backup study '$BACKUP_CANCER_STUDY_IDENTIFIER'!"
                    GROUPS_FAIL=1
                fi
                echo "Renaming temporary study '$TEMP_CANCER_STUDY_IDENTIFIER' to '$CANCER_STUDY_IDENTIFIER'"
                $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --rename-cancer-study --new-study-id $CANCER_STUDY_IDENTIFIER --original-study-id $TEMP_CANCER_STUDY_IDENTIFIER
                if [ $? -gt 0 ]; then
                    echo "Failed to rename temporary study '$TEMP_CANCER_STUDY_IDENTIFIER' to '$CANCER_STUDY_IDENTIFIER'!"
                    RENAME_FAIL=1
                fi
            fi
        fi
    fi
fi

### FAILURE EMAIL ###

EMAIL_BODY="The $CANCER_STUDY_IDENTIFIER study failed import. The original study will remain on the portal."
# send email if import fails
if [ $IMPORT_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: Import" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "$CANCER_STUDY_IDENTIFIER import as temp study"
fi

# send email if validation fails
if [ $VALIDATION_FAIL -gt 0 ]; then
    if [ $(wc -l < $VALIDATION_NOTIFICATION_FILENAME) -eq 0 ]; then
        EMAIL_BODY="The $CANCER_STUDY_IDENTIFIER study failed to pass the validation step in import process for some unknown reason. No data was saved to validation notification file $VALIDATION_NOTIFICATION_FILENAME. The original study will remain on the portal."
    else
        EMAIL_BODY=`cat $VALIDATION_NOTIFICATION_FILENAME`
    fi
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: Validation" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "$CANCER_STUDY_IDENTIFIER temp study validation"
fi

EMAIL_BODY="The $BACKUP_CANCER_STUDY_IDENTIFIER study failed to delete. $CANCER_STUDY_IDENTIFIER study did not finish updating."
if [ $DELETE_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: Deletion" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "$BACKUP_CANCER_STUDY_IDENTIFIER deletion"
fi

EMAIL_BODY="Failed to backup $CANCER_STUDY_IDENTIFIER to $BACKUP_CANCER_STUDY_IDENTIFIER via renaming. $CANCER_STUDY_IDENTIFIER study did not finish updating."
if [ $RENAME_BACKUP_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: Renaming backup" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "$CANCER_STUDY_IDENTIFIER rename to $BACKUP_CANCER_STUDY_IDENTIFIER"
fi

EMAIL_BODY="Failed to rename temp study $TEMP_CANCER_STUDY_IDENTIFIER to $CANCER_STUDY_IDENTIFIER. $CANCER_STUDY_IDENTIFIER study did not finish updating."
if [ $RENAME_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: CRITICAL!! Renaming" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "CRITICAL FAILURE: $TEMP_CANCER_STUDY_IDENTIFIER rename to $CANCER_STUDY_IDENTIFIER"
fi

EMAIL_BODY="Failed to update groups for backup study $BACKUP_CANCER_STUDY_IDENTIFIER."
if [ $GROUPS_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "$CANCER_STUDY_IDENTIFIER Update Failure: Groups update" $EMAIL_LIST
    sendFailureMessageMskPipelineLogsSlack "$CANCER_STUDY_IDENTIFIER groups update"
fi

# send notification file
# this contains the error or success message from import
# we only want to send the email on import failure
# or if everything succeeds
if [[ $IMPORT_FAIL -ne 0 || ($VALIDATION_FAIL -eq 0 && $DELETE_FAIL -eq 0 && $RENAME_BACKUP_FAIL -eq 0 && $RENAME_FAIL -eq 0 && $GROUPS_FAIL -eq 0) ]]; then
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal $PORTAL_NAME --notification-file $NOTIFICATION_FILE
fi

# determine if we need to exit with error code
if [[ $IMPORT_FAIL -ne 0 || $VALIDATION_FAIL -ne 0 || $DELETE_FAIL -ne 0 || $RENAME_BACKUP_FAIL -ne 0 || $RENAME_FAIL -ne 0 || $GROUPS_FAIL -ne 0 ]]; then
    echo "Update failed for study '$CANCER_STUDY_IDENTIFIER'"
    exit 1
else
    echo "Update successful for study '$CANCER_STUDY_IDENTIFIER'"
fi
