#!/bin/bash

# associative array for extracting properties from importer.properties
declare -Ax extracted_properties

if [ -z "$PORTAL_HOME" ] ; then
    echo "Error : import-dmp-impact-data.sh cannot be run without setting the PORTAL_HOME environment variable. (Use automation-environment.sh)"
    exit 1
fi
# localize global variables / jar names and functions
source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh
source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh
source $PORTAL_HOME/scripts/extract-properties-from-file-functions.sh

# -----------------------------------------------------------------------------------------------------------
# START IMPORTS

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

if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
    # refresh cdd and oncotree cache - by default this script will attempt to
    # refresh the CDD and ONCOTREE cache but we should check both exit codes
    # independently because of the various dependencies we have for both services
    CDD_RECACHE_FAIL=0;
    ONCOTREE_RECACHE_FAIL=0
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --cdd-only
    if [ $? -gt 0 ]; then
        message="Failed to refresh CDD cache!"
        echo $message
        echo -e "$message" | mail -s "CDD cache failed to refresh" $PIPELINES_EMAIL_LIST
        sendImportFailureMessageMskPipelineLogsSlack "$message"
        CDD_RECACHE_FAIL=1
    fi
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --oncotree-only
    if [ $? -gt 0 ]; then
        message="Failed to refresh ONCOTREE cache!"
        echo $message
        echo -e "$message" | mail -s "ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        sendImportFailureMessageMskPipelineLogsSlack "$message"
        ONCOTREE_RECACHE_FAIL=1
    fi
    if [[ $CDD_RECACHE_FAIL -ne 0 || $ONCOTREE_RECACHE_FAIL -ne 0 ]] ; then
        echo "Oncotree and/or CDD recache failed! Exiting..."
        exit 2
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

DB_VERSION_FAIL=0

# Imports assumed to fail until imported successfully
IMPORT_FAIL_MSKSOLIDHEME=1
IMPORT_FAIL_ARCHER=1
IMPORT_FAIL_KINGS=1
IMPORT_FAIL_LEHIGH=1
IMPORT_FAIL_QUEENS=1
IMPORT_FAIL_MCI=1
IMPORT_FAIL_HARTFORD=1
IMPORT_FAIL_RALPHLAUREN=1
IMPORT_FAIL_RIKENGENESISJAPAN=1
IMPORT_FAIL_MSKIMPACT_PED=1
IMPORT_FAIL_SCLC_MSKIMPACT=1
IMPORT_FAIL_LYMPHOMA=1
GENERATE_MASTERLIST_FAIL=0
MERCURIAL_PUSH_FAIL=0

# -------------------------------------------------------------
# check database version before importing anything
printTimeStampedDataProcessingStepMessage "database version compatibility check"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -gt 0 ] ; then
    echo "Database version expected by portal does not match version in database!"
    sendImportFailureMessageMskPipelineLogsSlack "MSK DMP Importer DB version check"
    DB_VERSION_FAIL=1
fi

if [ $DB_VERSION_FAIL -eq 0 ] ; then
    # import into portal database
    echo "importing cancer type updates into msk portal database..."
    $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
    if [ $? -gt 0 ] ; then
        sendImportFailureMessageMskPipelineLogsSlack "Cancer type updates"
    fi
fi

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

CLEAR_CACHES_AFTER_IMPACT_IMPORT=0
# TEMP STUDY IMPORT: MSKSOLIDHEME
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_SOLID_HEME_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import of MSKSOLIDHEME (will be renamed MSKIMPACT) study"
    # this usage is a little different -- we are comparing the backup-study-id "yesterday_mskimpact" because we will be renaming this imported study to mskimpact after a successful import
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact" --temp-study-id="temporary_mskimpact" --backup-study-id="yesterday_mskimpact" --portal-name="msk-solid-heme-portal" --study-path="$MSK_SOLID_HEME_DATA_HOME" --notification-file="$msk_solid_heme_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        consumeSamplesAfterSolidHemeImport
        CLEAR_CACHES_AFTER_IMPACT_IMPORT=1
        IMPORT_FAIL_MSKSOLIDHEME=0
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


# clear persistence cache only if the MSKSOLIDHEME update was succesful
if [ $CLEAR_CACHES_AFTER_IMPACT_IMPORT -eq 0 ] ; then
    echo "Failed to update MSKSOLIDHEME - we will clear the persistence cache after successful updates to MSK affiliate studies..."
    echo $(date)
else
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
    fi
fi

# set 'CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT' flag to 1 if ARCHER succesfully updates
CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT=0

# TEMP STUDY IMPORT: MSKARCHER
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_ARCHER_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for mskarcher"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskarcher" --temp-study-id="temporary_mskarcher" --backup-study-id="yesterday_mskarcher" --portal-name="mskarcher-portal" --study-path="$MSK_ARCHER_DATA_HOME" --notification-file="$mskarcher_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        consumeSamplesAfterArcherImport
        CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT=1
        IMPORT_FAIL_ARCHER=0
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

# clear persistence cache only if MSKARCHER update was successful
if [ $CLEAR_CACHES_AFTER_DMP_PIPELINES_IMPORT -eq 0 ] ; then
    echo "Failed to update ARCHER - we will clear the persistence cache after successful updates to MSK affiliate studies..."
    echo $(date)
else
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
    fi
fi

## END MSK DMP cohorts imports

#-------------------------------------------------------------------------------------------------------------------------------------
# set 'CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT' flag to 1 if Kings County, Lehigh Valley, Queens Cancer Center, Miami Cancer Institute, MSKIMPACT Ped, or Lymphoma super cohort succesfully update
CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=0

# TEMP STUDY IMPORT: KINGSCOUNTY
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_KINGS_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_kingscounty"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_kingscounty" --temp-study-id="temporary_msk_kingscounty" --backup-study-id="yesterday_msk_kingscounty" --portal-name="msk-kingscounty-portal" --study-path="$MSK_KINGS_DATA_HOME" --notification-file="$kingscounty_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_KINGS=0
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

# TEMP STUDY IMPORT: LEHIGHVALLEY
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_LEHIGH_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_lehighvalley"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_lehighvalley" --temp-study-id="temporary_msk_lehighvalley" --backup-study-id="yesterday_msk_lehighvalley" --portal-name="msk-lehighvalley-portal" --study-path="$MSK_LEHIGH_DATA_HOME" --notification-file="$lehighvalley_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_LEHIGH=0
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

# TEMP STUDY IMPORT: QUEENSCANCERCENTER
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_QUEENS_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_queenscancercenter"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_queenscancercenter" --temp-study-id="temporary_msk_queenscancercenter" --backup-study-id="yesterday_msk_queenscancercenter" --portal-name="msk-queenscancercenter-portal" --study-path="$MSK_QUEENS_DATA_HOME" --notification-file="$queenscancercenter_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_QUEENS=0
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

# TEMP STUDY IMPORT: MIAMICANCERINSTITUTE
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_MCI_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_miamicancerinstitute"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_miamicancerinstitute" --temp-study-id="temporary_msk_miamicancerinstitute" --backup-study-id="yesterday_msk_miamicancerinstitute" --portal-name="msk-mci-portal" --study-path="$MSK_MCI_DATA_HOME" --notification-file="$miamicancerinstitute_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_MCI=0
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

# TEMP STUDY IMPORT: HARTFORDHEALTHCARE
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_HARTFORD_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_hartfordhealthcare"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_hartfordhealthcare" --temp-study-id="temporary_msk_hartfordhealthcare" --backup-study-id="yesterday_msk_hartfordhealthcare" --portal-name="msk-hartford-portal" --study-path="$MSK_HARTFORD_DATA_HOME" --notification-file="$hartfordhealthcare_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_HARTFORD=0
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

# TEMP STUDY IMPORT: RALPHLAUREN
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_RALPHLAUREN_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_ralphlauren"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_ralphlauren" --temp-study-id="temporary_msk_ralphlauren" --backup-study-id="yesterday_msk_ralphlauren" --portal-name="msk-ralphlauren-portal" --study-path="$MSK_RALPHLAUREN_DATA_HOME" --notification-file="$ralphlauren_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_RALPHLAUREN=0
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

# TEMP STUDY IMPORT: RIKENGENESISJAPAN
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for msk_rikengenesisjapan"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_rikengenesisjapan" --temp-study-id="temporary_msk_rikengenesisjapan" --backup-study-id="yesterday_msk_rikengenesisjapan" --portal-name="msk-tailormedjapan-portal" --study-path="$MSK_RIKENGENESISJAPAN_DATA_HOME" --notification-file="$rikengenesisjapan_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_RIKENGENESISJAPAN=0
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

#-------------------------------------------------------------------------------------------------------------------------------------
# TEMP STUDY IMPORT: MSKIMPACT_PED
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSKIMPACT_PED_IMPORT_TRIGGER ]; then
    printTimeStampedDataProcessingStepMessage "import for mskimpact_ped study"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact_ped" --temp-study-id="temporary_mskimpact_ped" --backup-study-id="yesterday_mskimpact_ped" --portal-name="msk-ped-portal" --study-path="$MSKIMPACT_PED_DATA_HOME" --notification-file="$mskimpact_ped_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ]; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_MSKIMPACT_PED=0
    fi
    rm $MSKIMPACT_PED_IMPORT_TRIGGER
else
    if [ $DB_VERSION_FAIL -gt 0 ] ; then
        echo "Not importing MSKIMPACT_PED - database version is not compatible"
    else
        echo "Not importing MSKIMPACT_PED - something went wrong with subsetting clinical studies for MSKIMPACT_PED."
    fi
fi
if [ $IMPORT_FAIL_MSKIMPACT_PED -gt 0 ] ; then
    sendImportFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED import"
else
    sendImportSuccessMessageMskPipelineLogsSlack "MSKIMPACT_PED"
fi

## END MSKIMPACT_PED import

#-------------------------------------------------------------------------------------------------------------------------------------
CLEAR_CACHES_AFTER_SCLC_IMPORT=0
# TEMP STUDY IMPORT: SCLCMSKIMPACT
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $MSK_SCLC_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for sclc_mskimpact_2017 study"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="sclc_mskimpact_2017" --temp-study-id="temporary_sclc_mskimpact_2017" --backup-study-id="yesterday_sclc_mskimpact_2017" --portal-name="msk-sclc-portal" --study-path="$MSK_SCLC_DATA_HOME" --notification-file="$sclc_mskimpact_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_SCLC_IMPORT=1
        IMPORT_FAIL_SCLC_MSKIMPACT=0
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

# END SCLCMSKIMPACT import

#-------------------------------------------------------------------------------------------------------------------------------------
# TEMP STUDY IMPORT: LYMPHOMASUPERCOHORT
if [ $DB_VERSION_FAIL -eq 0 ] && [ -f $LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER ] ; then
    printTimeStampedDataProcessingStepMessage "import for lymphoma_super_cohort_fmi_msk study"
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="lymphoma_super_cohort_fmi_msk" --temp-study-id="temporary_lymphoma_super_cohort_fmi_msk" --backup-study-id="yesterday_lymphoma_super_cohort_fmi_msk" --portal-name="msk-fmi-lymphoma-portal" --study-path="$LYMPHOMA_SUPER_COHORT_DATA_HOME" --notification-file="$lymphoma_super_cohort_notification_file" --tmp-directory="$MSK_DMP_TMPDIR" --email-list="$PIPELINES_EMAIL_LIST" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ] ; then
        CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT=1
        IMPORT_FAIL_LYMPHOMA=0
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

# clear persistence cache only if at least one of these studies succesfully updated.
#   MSK_KINGSCOUNTY
#   MSK_LEHIGHVALLEY
#   MSK_QUEENSCANCERCENTER
#   MSK_MIAMICANCERINSTITUTE
#   MSK_HARTFORDHEALTHCARE
#   MSK_RALPHLAUREN
#   LYMPHOMASUPERCOHORT
#   SCLCMSKIMPACT
if [ $CLEAR_CACHES_AFTER_MSK_AFFILIATE_IMPORT -eq 0 ] ; then
    echo "Failed to update all MSK affiliate studies"
else
    if ! clearPersistenceCachesForMskPortals ; then
        sendClearCacheFailureMessage msk import-dmp-impact-data.sh
    fi
fi

# clear persistence cache only if sclc_mskimpact_2017 update was successful
if [ $CLEAR_CACHES_AFTER_SCLC_IMPORT -eq 0 ] ; then
    echo "Failed to update SCLC MSKIMPCAT cohort"
else
    if ! clearPersistenceCachesForExternalPortals ; then
        sendClearCacheFailureMessage external import-dmp-impact-data.sh
    fi
fi

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
exit 0
