#!/bin/bash

echo $(date)

# setting up the environment on the machine
# -------------------------
PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh
PATH_TO_TEMPO_JOB_CONFIG=/data/portal-cron/scripts/tempo-environment.sh

# set up enivornment variables and temp directory
if ! [ -f $PATH_TO_AUTOMATION_SCRIPT ] || ! [ -f $PATH_TO_TEMPO_JOB_CONFIG ]; then
    message="automation-environment.sh and/or tempo-environment.sh could not be found, exiting..."
    echo ${message}
    exit 2
fi

source $PATH_TO_AUTOMATION_SCRIPT
source $PATH_TO_TEMPO_JOB_CONFIG
source $PORTAL_HOME/scripts/clear-persistence-cache-shell-functions.sh
# -------------------------

# TODO: to be set by Airflow DAG
# -------------------------
IMPORTER_JAR_FILENAME=$PORTAL_HOME/lib/triage-cmo-importer.jar
#IMPORTER_JAR_FILENAME=$1
# -------------------------

# Additional env vars
# -------------------------
importer_notification_file=$(mktemp $TEMPO_TMPDIR/importer-update-notification.$now.XXXXXX)
TEMPO_TMPDIR=/data/portal-cron/tmp/import-cron-tempo
JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$TEMPO_TMPDIR -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"

# temporary workaround to enable importing a study from arbitrary path
# needed because importer looks for "data sources" under PORTAL_DATA_HOME
# not checking in files into an actual data source
# to be redone with whole refactoring of how we integrate datasources 
PORTAL_DATA_HOME=/data/portal-cron/tmp/import-tempo/
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
# -------------------------

# Flags for individual steps required for successful import
CDD_ONCOTREE_RECACHE_FAIL=0
DB_VERSION_FAIL=0
DATABRICKS_EXPORT_FAIL=0
IMPORT_SUCCESS=0
CLEAR_PERSISTENCE_CACHE_SUCCESS=0

# Refresh CDD/Oncotree
if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
    # refresh cdd and oncotree cache
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
    if [ $? -gt 0 ]; then
        CDD_ONCOTREE_RECACHE_FAIL=1
        message="Failed to refresh CDD and/or ONCOTREE cache during CRDB PDX import!"
        echo $message
    fi
fi

# Database Check
echo "Checking if database version is compatible"
$JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
if [ $? -eq 0 ]
then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
fi

# Create temp directory needed for import
if [ ! -d $TEMPO_TMPDIR ] ; then
    mkdir $TEMPO_TMPDIR
    if [ $? -ne 0 ] ; then
        message="error : required temp directory does not exist and could not be created : $TEMPO_TMPDIR"
        echo ${message}
        exit 2
    fi
fi
if [[ -d "$TEMPO_TMPDIR" && "$TEMPO_TMPDIR" != "/" ]] ; then
    rm -rf "$TEMPO_TMPDIR"/*
fi

# Pull TEMPO dataset from Databricks
echo "Pulling TEMPO dataset from Databricks"
$PORTAL_HOME/scripts/cbioportal-databricks-gateway --catalog=$DATABRICKS_CATALOG --directory=$STUDY_DIRECTORY --host=$DATABRICKS_HOST --path=$DATABRICKS_PATH --port=$DATABRICKS_PORT --schema=$DATABRICKS_SCHEMA --token=$DATABRICKS_TOKEN 
if [ $? -gt 0 ]; then
	DATABRICKS_EXPORT_FAIL=1
    message="Failed to export study from Databricks"
    echo $message
	exit 2
fi

# Additional processing 
# Add clinical data metadata headers
# Filter data issues - to be removed
# Generate caselists
# -------------------------
python /data/portal-cron/scripts/add_clinical_attribute_metadata_headers.py -f /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo/data_clinical_sample.txt
python /data/portal-cron/scripts/add_clinical_attribute_metadata_headers.py -f /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo/data_clinical_patient.txt
$PYTHON_BINARY /data/portal-cron/scripts/merge.py -e /home/cbioportal_importer/tempo_subset_list --output-directory /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo --study-id msk_tempo --cancer-type mixed /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo
python /data/portal-cron/scripts/generate_case_lists.py -i msk_tempo -s /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo -c /data/portal-cron/scripts/case_list_config.tsv  -d /data/portal-cron/tmp/import-tempo/pipelines-testing/studies/msk_tempo/case_lists
# -------------------------

if [[ $DB_VERSION_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 && $DATABRICKS_EXPORT_FAIL -eq 0 ]] ; then
        echo "importing study data to database using $IMPORTER_JAR_FILENAME ..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --update-study-data --portal hot-deploy --use-never-import --update-worksheet --notification-file "$importer_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source mskcc
        if [ $? -ne 0 ]; then
            echo "$IMPORTER_JAR_LABEL import failed!"
        else
			echo "This was a success"
            IMPORT_SUCCESS=1
        fi
        num_studies_updated=`cat $TEMPO_TMPDIR/num_studies_updated.txt`
        # clear persistence cache (note : this script is constructing studies for the msk portal, including mskimpact sample data - that is why the msk portal cache is cleared)
	# # TODO: configure pipelines5 server to handle cache resets
        if [[ $IMPORT_SUCCESS -ne 0 && $num_studies_updated -gt 0 ]]; then
            echo "'$num_studies_updated' studies have been updated, clearing persistence cache for a portal ..."
	    	exit 0
            if ! clearPersistenceCachesForMskPortals ; then
                sendClearCacheFailureMessage msk import-pdx-data.sh
            else
                CLEAR_PERSISTENCE_CACHE_SUCCESS=1
            fi
        else
            echo "No studies have been updated, not clearing persistence cache for msk portal..."
            CLEAR_PERSISTENCE_CACHE_SUCCESS=1
        fi
fi
exit 0
