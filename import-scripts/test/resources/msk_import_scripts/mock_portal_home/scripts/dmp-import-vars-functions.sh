#!/usr/bin/env bash
# Mock function library — all notification/consumption functions are no-ops.
# Trigger and data home vars are set to paths under $MOCK_TMP so tests can
# touch/remove them to control import gating.

function printTimeStampedDataProcessingStepMessage() { return 0; }
function sendImportFailureMessageMskPipelineLogsSlack() { return 0; }
function sendImportSuccessMessageMskPipelineLogsSlack() { return 0; }
function sendPreImportFailureMessageMskPipelineLogsSlack() { return 0; }
function consumeSamplesAfterSolidHemeImport() { return 0; }
function consumeSamplesAfterArcherImport() { return 0; }
function uploadToS3OrSendFailureMessage() { return 0; }

ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
PIPELINES_EMAIL_LIST="test@example.com"
CMO_EMAIL_LIST="test@example.com"

# Trigger files — test controls existence via touch/rm
MSK_SOLID_HEME_IMPORT_TRIGGER="${MOCK_TMP}/MSK_SOLID_HEME_IMPORT_TRIGGER"
MSK_ARCHER_IMPORT_TRIGGER="${MOCK_TMP}/MSK_ARCHER_IMPORT_TRIGGER"
MSK_KINGS_IMPORT_TRIGGER="${MOCK_TMP}/MSK_KINGS_IMPORT_TRIGGER"
MSK_LEHIGH_IMPORT_TRIGGER="${MOCK_TMP}/MSK_LEHIGH_IMPORT_TRIGGER"
MSK_QUEENS_IMPORT_TRIGGER="${MOCK_TMP}/MSK_QUEENS_IMPORT_TRIGGER"
MSK_MCI_IMPORT_TRIGGER="${MOCK_TMP}/MSK_MCI_IMPORT_TRIGGER"
MSK_HARTFORD_IMPORT_TRIGGER="${MOCK_TMP}/MSK_HARTFORD_IMPORT_TRIGGER"
MSK_RALPHLAUREN_IMPORT_TRIGGER="${MOCK_TMP}/MSK_RALPHLAUREN_IMPORT_TRIGGER"
MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER="${MOCK_TMP}/MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER"
MSK_SCLC_IMPORT_TRIGGER="${MOCK_TMP}/MSK_SCLC_IMPORT_TRIGGER"
LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER="${MOCK_TMP}/LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER"

# Data home dirs — just need to not be empty strings
MSK_DMP_TMPDIR="${MOCK_TMP}/msk-dmp"
MSK_SOLID_HEME_DATA_HOME="${MOCK_TMP}/solid-heme"
MSK_ARCHER_DATA_HOME="${MOCK_TMP}/archer"
MSK_KINGS_DATA_HOME="${MOCK_TMP}/kings"
MSK_LEHIGH_DATA_HOME="${MOCK_TMP}/lehigh"
MSK_QUEENS_DATA_HOME="${MOCK_TMP}/queens"
MSK_MCI_DATA_HOME="${MOCK_TMP}/mci"
MSK_HARTFORD_DATA_HOME="${MOCK_TMP}/hartford"
MSK_RALPHLAUREN_DATA_HOME="${MOCK_TMP}/ralphlauren"
MSK_RIKENGENESISJAPAN_DATA_HOME="${MOCK_TMP}/rikengenesisjapan"
MSKIMPACT_PED_DATA_HOME="${MOCK_TMP}/mskimpact-ped"
MSK_SCLC_DATA_HOME="${MOCK_TMP}/sclc"
LYMPHOMA_SUPER_COHORT_DATA_HOME="${MOCK_TMP}/lymphoma"
DMP_DATA_HOME="${MOCK_TMP}/dmp"
MSK_IMPACT_DATA_HOME="${MOCK_TMP}/mskimpact"
MSK_SHAHLAB_DATA_HOME="${MOCK_TMP}/shahlab"
MSK_SPECTRUM_COHORT_DATA_HOME="${MOCK_TMP}/spectrum"
TMP_DIRECTORY="${MOCK_TMP}"
PORTAL_DATA_HOME="${MOCK_TMP}"

JAVA_PROXY_ARGS=""
JAVA_SSL_ARGS=""
JAVA_DD_AGENT_ARGS=""
java_debug_args=""
now=$(date "+%Y-%m-%d-%H-%M-%S")
SLACK_URL_FILE="/dev/null"
CRDB_FETCHER_PDX_HOME="${MOCK_TMP}/crdb-pdx"
