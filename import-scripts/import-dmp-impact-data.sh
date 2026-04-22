#!/bin/bash

# associative array for extracting properties from importer.properties
declare -Ax extracted_properties

if [ -z "$PORTAL_HOME" ] ; then
    echo "Error : import-dmp-impact-data.sh cannot be run without setting the PORTAL_HOME environment variable. (Use automation-environment.sh)"
    exit 1
fi

SKIP_AFFILIATE_STUDIES_IMPORT=0
SKIP_SCLC_MSKIMPACT_IMPORT=0
SKIP_LYMPHOMA_IMPORT=0

# localize global variables / jar names and functions
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh
source $PORTAL_HOME/scripts/extract-properties-from-file-functions.sh

# -----------------------------------------------------------------------------------------------------------
# START IMPORTS

echo $(date)

# Pause with checkpoint
CHECKPOINT_FILEPATH="${CHECKPOINT_FILEPATH:-/data/portal-cron/tmp/msk-import-script-go-ahead}"
while true ; do
    if [ -f $CHECKPOINT_FILEPATH ] ; then
        break
    fi
    echo "pausing at checkpoint"
    sleep 10 * 60
done

echo $(date)

PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"

if [ -z $JAVA_BINARY ] | [ -z $MSK_IMPACT_DATA_HOME ] ; then
    message="could not run import-dmp-impact-data.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_IMPACT_DATA_HOME, ...)"
    echo ${message}
    echo -e "${message}" |  mail -s "import-dmp-impact-data failed to run." $PIPELINES_EMAIL_LIST
    sendImportFailureMessageMskPipelineLogsSlack "${message}"
    exit 2
fi

MSK_DMP_IMPORT_PROPERTIES_FILE="$PIPELINES_CONFIG_HOME/properties/import-dmp/importer.properties"
extractPropertiesFromFile "$MSK_DMP_IMPORT_PROPERTIES_FILE" db.user db.password db.host db.portal_db_name
if [ $? -ne 0 ] ; then
    echo "warning : could not read database properties from property file $MSK_DMP_IMPORT_PROPERTIES_FILE"
    echo "    archer import is likely to fail because adjustment of mutations will not be possible"
fi
DMP_DB_HOST=${extracted_properties[db.host]}
DMP_DB_USER=${extracted_properties[db.user]}
DMP_DB_PASSWORD=${extracted_properties[db.password]}
DMP_DB_DATABASE_NAME=${extracted_properties[db.portal_db_name]}

if ! [ -d "$MSK_DMP_TMPDIR" ] ; then
    if ! mkdir -p "$MSK_DMP_TMPDIR" ; then
        echo "Error : could not create tmp directory '$MSK_DMP_TMPDIR'" >&2
        exit 1
    fi
fi

now=$(date "+%Y-%m-%d-%H-%M-%S")
msk_solid_heme_notification_file=$(mktemp $MSK_DMP_TMPDIR/msk-solid-heme-portal-update-notification.$now.XXXXXX)
mskarcher_notification_file=$(mktemp $MSK_DMP_TMPDIR/mskarcher-portal-update-notification.$now.XXXXXX)
kingscounty_notification_file=$(mktemp $MSK_DMP_TMPDIR/kingscounty-portal-update-notification.$now.XXXXXX)
lehighvalley_notification_file=$(mktemp $MSK_DMP_TMPDIR/lehighvalley-portal-update-notification.$now.XXXXXX)
queenscancercenter_notification_file=$(mktemp $MSK_DMP_TMPDIR/queenscancercenter-portal-update-notification.$now.XXXXXX)
miamicancerinstitute_notification_file=$(mktemp $MSK_DMP_TMPDIR/miamicancerinstitute-portal-update-notification.$now.XXXXXX)
hartfordhealthcare_notification_file=$(mktemp $MSK_DMP_TMPDIR/hartfordhealthcare-portal-update-notification.$now.XXXXXX)
ralphlauren_notification_file=$(mktemp $MSK_DMP_TMPDIR/ralphlauren-portal-update-notification.$now.XXXXXX)
rikengenesisjapan_notification_file=$(mktemp $MSK_DMP_TMPDIR/msk-rikengenesisjapan-portal-update-notification.$now.XXXXXX)
lymphoma_super_cohort_notification_file=$(mktemp $MSK_DMP_TMPDIR/lymphoma-super-cohort-portal-update-notification.$now.XXXXXX)
sclc_mskimpact_notification_file=$(mktemp $MSK_DMP_TMPDIR/sclc-mskimpact-portal-update-notification.$now.XXXXXX)
mskimpact_ped_notification_file=$(mktemp $MSK_DMP_TMPDIR/mskimpact-ped-update-notification.$now.XXXXXX)

# -----------------------------------------------------------------------------------------------------------

# Get the current production database color
GET_DB_IN_PROD_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/get_database_currently_in_production.sh"
MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="/data/portal-cron/pipelines-credentials/manage_msk_clickhouse_database_update_tools.properties"
current_production_database_color=$($GET_DB_IN_PROD_SCRIPT_FILEPATH $MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH)
destination_database_color="unset"
if [ ${current_production_database_color:0:4} == "blue" ] ; then
    destination_database_color="green"
fi
if [ ${current_production_database_color:0:5} == "green" ] ; then
    destination_database_color="blue"
fi
if [ "$destination_database_color" == "unset" ] ; then
    echo "Error during determination of the destination database color" >&2
    exit 1
fi

MSK_IMPORTER_JAR_FILENAME="/data/portal-cron/lib/msk-clickhouse-importer-$destination_database_color.jar"
MSK_JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS $JAVA_DD_AGENT_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$MSK_DMP_TMPDIR -Dlog4j.appender.a.File=/data/portal-cron/logs/msk-dmp-clickhouse-importer.log -ea -cp $MSK_IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
VALIDATE_BLUE_GREEN_STUDY_SCRIPT_FILEPATH="$PORTAL_HOME/scripts/validate_blue_green_study.py"

# Runs --update-study-data for a portal, then validates the named study against
# the baseline (production) DB. Returns nonzero if either step fails.
function import_and_validate() {
    local study_id="$1"
    local portal_name="$2"
    local notification_file="$3"
    $JAVA_BINARY -Xmx64g $MSK_JAVA_IMPORTER_ARGS --update-study-data --portal "$portal_name" --notification-file "$notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc --disable-redcap-export
    if [ $? -ne 0 ] ; then
        return 1
    fi
    $PYTHON_BINARY $VALIDATE_BLUE_GREEN_STUDY_SCRIPT_FILEPATH --properties-file "$MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH" --study-id "$study_id" --current-color "$destination_database_color"
    return $?
}

DB_VERSION_FAIL=0

# Imports assumed to fail until imported successfully
IMPORT_FAIL_MSKSOLIDHEME=0
IMPORT_FAIL_ARCHER=0
IMPORT_FAIL_KINGS=0
IMPORT_FAIL_LEHIGH=0
IMPORT_FAIL_QUEENS=0
IMPORT_FAIL_MCI=0
IMPORT_FAIL_HARTFORD=0
IMPORT_FAIL_RALPHLAUREN=0
IMPORT_FAIL_RIKENGENESISJAPAN=0
IMPORT_FAIL_MSKIMPACT_PED=0
IMPORT_FAIL_SCLC_MSKIMPACT=0
IMPORT_FAIL_LYMPHOMA=0

# -------------------------------------------------------------
# check database version before importing anything
printTimeStampedDataProcessingStepMessage "database version compatibility check"
$JAVA_BINARY $MSK_JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ] ; then
    echo "Database version expected by portal does not match version in database!"
    sendImportFailureMessageMskPipelineLogsSlack "MSK DMP Importer DB version check"
    DB_VERSION_FAIL=1
fi

if [ $DB_VERSION_FAIL -eq 0 ] ; then
    # import into portal database
    echo "importing cancer type updates into msk portal database..."
    $JAVA_BINARY -Xmx16g $MSK_JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    if [ $? -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "Cancer type updates"
    fi
fi

CLEAR_CACHES_AFTER_IMPACT_IMPORT=0
# IMPORT: MSKSOLIDHEME
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_SOLID_HEME_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import of MSKSOLIDHEME (will be renamed MSKIMPACT) study"
    if import_and_validate "mskimpact" "msk-solid-heme-portal" "$msk_solid_heme_notification_file" ; then
        consumeSamplesAfterSolidHemeImport
        CLEAR_CACHES_AFTER_IMPACT_IMPORT=1
    else
        IMPORT_FAIL_MSKSOLIDHEME=1
    fi
    rm $MSK_SOLID_HEME_IMPORT_TRIGGER
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing MSKSOLIDHEME - database version is not compatible"
    else
        echo "Not importing MSKSOLIDHEME - something went wrong with merging clinical studies"
    fi
fi
if [ $IMPORT_FAIL_MSKSOLIDHEME -gt 0 ] ; then
    sendImportFailureMessageMskPipelineLogsSlack "MSKSOLIDHEME import"
else
    sendImportSuccessMessageMskPipelineLogsSlack "MSKSOLIDHEME"
fi

#### ROB : 2025_08_17 - persistence cache reset will now happen at the color transition instead
##### clear persistence cache only if the MSKSOLIDHEME update was succesful
####if [ $CLEAR_CACHES_AFTER_IMPACT_IMPORT -eq 0 ] ; then
####    echo "Failed to update MSKSOLIDHEME - we will clear the persistence cache after successful updates to MSK affiliate studies..."
####    echo $(date)
####else
####    if ! clearPersistenceCachesForMskPortals ; then
####        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
####    fi
####fi
####
##### set 'CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT' flag to 1 if ARCHER succesfully updates
####CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT=0

# IMPORT: MSKARCHER
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_ARCHER_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for mskarcher"
    if import_and_validate "mskarcher" "mskarcher-portal" "$mskarcher_notification_file" ; then
        consumeSamplesAfterArcherImport
####        CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT=1
    else
        IMPORT_FAIL_ARCHER=1
    fi
    rm $MSK_ARCHER_IMPORT_TRIGGER
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing MSKARCHER - database version is not compatible"
    else
        echo "Not importing MSKARCHER - something went wrong with a fetch"
    fi
fi
if [ $IMPORT_FAIL_ARCHER -gt 0 ] ; then
    sendImportFailureMessageMskPipelineLogsSlack "ARCHER import"
else
    sendImportSuccessMessageMskPipelineLogsSlack "ARCHER"
fi

##### clear persistence cache only if MSKARCHER update was successful
####if [ $CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT -eq 0 ] ; then
####    echo "Failed to update ARCHER - we will clear the persistence cache after successful updates to MSK affiliate studies..."
####    echo $(date)
####else
####    if ! clearPersistenceCachesForMskPortals ; then
####        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
####    fi
####fi

## END MSK DMP cohorts imports

#-------------------------------------------------------------------------------------------------------------------------------------
##### set 'CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT' flag to 1 if Kings County, Lehigh Valley, Queens Cancer Center, Miami Cancer Institute, MSKIMPACT Ped, or Lymphoma super cohort succesfully update
####CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=0

if ! [[ $SKIP_AFFILIATE_STUDIES_IMPORT == '1' ]] ; then
    # IMPORT: KINGSCOUNTY
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_KINGS_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_kingscounty"
        if ! import_and_validate "msk_kingscounty" "msk-kingscounty-portal" "$kingscounty_notification_file" ; then
            IMPORT_FAIL_KINGS=1
        fi
        rm $MSK_KINGS_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing KINGSCOUNTY - database version is not compatible"
        else
            echo "Not importing KINGSCOUNTY - something went wrong with subsetting clinical studies for KINGSCOUNTY."
        fi
    fi
    if [ $IMPORT_FAIL_KINGS -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "KINGSCOUNTY import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "KINGSCOUNTY"
    fi

    # IMPORT: LEHIGHVALLEY
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_LEHIGH_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_lehighvalley"
        if ! import_and_validate "msk_lehighvalley" "msk-lehighvalley-portal" "$lehighvalley_notification_file" ; then
            IMPORT_FAIL_LEHIGH=1
        fi
        rm $MSK_LEHIGH_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing LEHIGHVALLEY - database version is not compatible"
        else
            echo "Not importing LEHIGHVALLEY - something went wrong with subsetting clinical studies for LEHIGHVALLEY."
        fi
    fi
    if [ $IMPORT_FAIL_LEHIGH -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "LEHIGHVALLEY"
    fi

    # IMPORT: QUEENSCANCERCENTER
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_QUEENS_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_queenscancercenter"
        if ! import_and_validate "msk_queenscancercenter" "msk-queenscancercenter-portal" "$queenscancercenter_notification_file" ; then
            IMPORT_FAIL_QUEENS=1
        fi
        rm $MSK_QUEENS_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing QUEENSCANCERCENTER - database version is not compatible"
        else
            echo "Not importing QUEENSCANCERCENTER - something went wrong with subsetting clinical studies for QUEENSCANCERCENTER."
        fi
    fi
    if [ $IMPORT_FAIL_QUEENS -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "QUEENSCANCERCENTER"
    fi

    # IMPORT: MIAMICANCERINSTITUTE
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_MCI_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_miamicancerinstitute"
        if ! import_and_validate "msk_miamicancerinstitute" "msk-mci-portal" "$miamicancerinstitute_notification_file" ; then
            IMPORT_FAIL_MCI=1
        fi
        rm $MSK_MCI_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing MIAMICANCERINSTITUTE - database version is not compatible"
        else
            echo "Not importing MIAMICANCERINSTITUTE - something went wrong with subsetting clinical studies for MIAMICANCERINSTITUTE."
        fi
    fi
    if [ $IMPORT_FAIL_MCI -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE"
    fi

    # IMPORT: HARTFORDHEALTHCARE
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_HARTFORD_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_hartfordhealthcare"
        if ! import_and_validate "msk_hartfordhealthcare" "msk-hartford-portal" "$hartfordhealthcare_notification_file" ; then
            IMPORT_FAIL_HARTFORD=1
        fi
        rm $MSK_HARTFORD_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing HARTFORDHEALTHCARE - database version is not compatible"
        else
            echo "Not importing HARTFORDHEALTHCARE - something went wrong with subsetting clinical studies for HARTFORDHEALTHCARE."
        fi
    fi
    if [ $IMPORT_FAIL_HARTFORD -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE"
    fi

    # IMPORT: RALPHLAUREN
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_RALPHLAUREN_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_ralphlauren"
        if ! import_and_validate "msk_ralphlauren" "msk-ralphlauren-portal" "$ralphlauren_notification_file" ; then
            IMPORT_FAIL_RALPHLAUREN=1
        fi
        rm $MSK_RALPHLAUREN_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing RALPHLAUREN - database version is not compatible"
        else
            echo "Not importing RALPHLAUREN - something went wrong with subsetting clinical studies for RALPHLAUREN."
        fi
    fi
    if [ $IMPORT_FAIL_RALPHLAUREN -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "RALPHLAUREN import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "RALPHLAUREN"
    fi

    # IMPORT: RIKENGENESISJAPAN
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for msk_rikengenesisjapan"
        if ! import_and_validate "msk_rikengenesisjapan" "msk-tailormedjapan-portal" "$rikengenesisjapan_notification_file" ; then
            IMPORT_FAIL_RIKENGENESISJAPAN=1
        fi
        rm $MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing RIKENGENESISJAPAN - database version is not compatible"
        else
            echo "Not importing RIKENGENESISJAPAN - something went wrong with subsetting clinical studies for RIKENGENESISJAPAN."
        fi
    fi
    if [ $IMPORT_FAIL_RIKENGENESISJAPAN -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "RIKENGENESISJAPAN import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "RIKENGENESISJAPAN"
    fi

    ## END Institute affiliate imports
fi

#-------------------------------------------------------------------------------------------------------------------------------------
####CLEAR_CACHES_AFTER_SCLC_IMPORT=0

if ! [[ $SKIP_SCLC_MSKIMPACT_IMPORT == '1' ]] ; then
    # IMPORT: SCLCMSKIMPACT
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_SCLC_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for sclc_mskimpact_2017 study"
        if ! import_and_validate "sclc_mskimpact_2017" "msk-sclc-portal" "$sclc_mskimpact_notification_file" ; then
            IMPORT_FAIL_SCLC_MSKIMPACT=1
        fi
        rm $MSK_SCLC_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing SCLCMSKIMPACT - database version is not compatible"
        else
            echo "Not importing SCLCMSKIMPACT - something went wrong with subsetting clinical studies for SCLCMSKIMPACT."
        fi
    fi
    if [ $IMPORT_FAIL_SCLC_MSKIMPACT -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "SCLCMSKIMPACT"
    fi
fi

# END SCLCMSKIMPACT import

if ! [[ $SKIP_LYMPHOMA_IMPORT == '1' ]] ; then
    # IMPORT: LYMPHOMASUPERCOHORT
    if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER ] ; then
        printTimeStampedDataProcessingStepMessage "import for lymphoma_super_cohort_fmi_msk study"
        if ! import_and_validate "lymphoma_super_cohort_fmi_msk" "msk-fmi-lymphoma-portal" "$lymphoma_super_cohort_notification_file" ; then
            IMPORT_FAIL_LYMPHOMA=1
        fi
        rm $LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER
    else
        if [ $DB_VERSION_FAIL -gt 0 ] ; then
            echo "Not importing LYMPHOMASUPERCOHORT - database version is not compatible"
        else
            echo "Not importing LYMPHOMASUPERCOHORT - something went wrong with subsetting clinical studies for Lymphoma super cohort."
        fi
    fi
    if [ $IMPORT_FAIL_LYMPHOMA -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT import"
    else
        sendImportSuccessMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT"
    fi
fi

##### clear persistence cache only if at least one of these studies succesfully updated.
#####   MSK_KINGSCOUNTY
#####   MSK_LEHIGHVALLEY
#####   MSK_QUEENSCANCERCENTER
#####   MSK_MIAMICANCERINSTITUTE
#####   MSK_HARTFORDHEALTHCARE
#####   MSK_RALPHLAUREN
#####   LYMPHOMASUPERCOHORT
#####   SCLCMSKIMPACT
####if [ $CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT -eq 0 ] ; then
####    echo "Failed to update all MSK affiliate studies"
####else
####    if ! clearPersistenceCachesForMskPortals ; then
####        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
####    fi
####fi

##### clear persistence cache only if sclc_mskimpact_2017 update was successful
####if [ $CLEAR_CACHES_AFTER_SCLC_IMPORT -eq 0 ] ; then
####    echo "Failed to update SCLC MSKIMPCAT cohort"
####else
####    if ! clearPersistenceCachesForExternalPortals ; then
####        sendClearCacheFailureMessage external import-dmp-impact-data.sh
####    fi
####fi

### FAILURE EMAIL ###
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ] ; then
    EMAIL_BODY="The MSKIMPACT database version is incompatible. Imports will be skipped until database is updated."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
fi

echo "Fetching and importing of clinical datasets complete!"
echo $(date)

echo "Cleaning up any untracked files in $PORTAL_DATA_HOME/dmp..."
bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $DMP_DATA_HOME

# Convert data_CNA.txt to narrow format
# Copy this to S3, then remove the file
$PORTAL_HOME/scripts/convert_cna_to_narrow_py3.py "$MSK_SOLID_HEME_DATA_HOME/data_CNA.txt" "$MSK_SOLID_HEME_DATA_HOME/data_CNA_narrow.txt"
if [ $? -ne 0 ] ; then
    echo "warning : could not convert msk_solid_heme data_CNA.txt to data_CNA_narrow.txt"
fi

uploadToS3OrSendFailureMessage "$DMP_DATA_HOME" "" "mskimpact-databricks"

# now remove the narrow format data cna file we just created
rm "$MSK_SOLID_HEME_DATA_HOME/data_CNA_narrow.txt"

exit $(( DB_VERSION_FAIL | IMPORT_FAIL_MSKSOLIDHEME | IMPORT_FAIL_ARCHER | IMPORT_FAIL_KINGS | IMPORT_FAIL_LEHIGH | IMPORT_FAIL_QUEENS | IMPORT_FAIL_MCI | IMPORT_FAIL_HARTFORD | IMPORT_FAIL_RALPHLAUREN | IMPORT_FAIL_RIKENGENESISJAPAN | IMPORT_FAIL_MSKIMPACT_PED | IMPORT_FAIL_SCLC_MSKIMPACT | IMPORT_FAIL_LYMPHOMA ))
