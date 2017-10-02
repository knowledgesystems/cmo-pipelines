#!/bin/bash

# set necessary env variables with automation-environment.sh

echo $(date)
tmp=$PORTAL_HOME/tmp/import-cron-dmp-msk
email_list="cbioportal-pipelines@cbio.mskcc.org"

if [[ -d "$tmp" && "$tmp" != "/" ]]; then
    rm -rf "$tmp"/*
fi

now=$(date "+%Y-%m-%d-%H-%M-%S")
ONCOTREE_VERSION_TO_USE=oncotree_candidate_release
mskimpact_notification_file=$(mktemp $tmp/mskimpact-portal-update-notification.$now.XXXXXX)
mskheme_notification_file=$(mktemp $tmp/mskheme-portal-update-notification.$now.XXXXXX)
mskraindance_notification_file=$(mktemp $tmp/mskraindance-portal-update-notification.$now.XXXXXX)
mixedpact_notification_file=$(mktemp $tmp/mixedpact-portal-update-notification.$now.XXXXXX)
mskarcher_notification_file=$(mktemp $tmp/mskarcher-portal-update-notification.$now.XXXXXX)
kingscounty_notification_file=$(mktemp $tmp/kingscounty-portal-update-notification.$now.XXXXXX)
lehighvalley_notification_file=$(mktemp $tmp/lehighvalley-portal-update-notification.$now.XXXXXX)
queenscancercenter_notification_file=$(mktemp $tmp/queenscancercenter-portal-update-notification.$now.XXXXXX)
miamicancerinstitute_notification_file=$(mktemp $tmp/miamicancerinstitute-portal-update-notification.$now.XXXXXX)
hartfordhealthcare_notification_file=$(mktemp $tmp/hartfordhealthcare-portal-update-notification.$now.XXXXXX)
lymphoma_super_cohort_notification_file=$(mktemp $tmp/lymphoma-super-cohort-portal-update-notification.$now.XXXXXX)


# fetch clinical data mercurial
echo "fetching updates from msk-impact repository..."
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --fetch-data --data-source dmp-clinical-data-mercurial --run-date latest

# lets clean clinical data files
echo "cleaning all clinical & timeline data files - replacing carriage returns with newlines..."
files=$(ls $MSK_IMPACT_DATA_HOME/data_clinical*)
files="$files $(ls $MSK_IMPACT_DATA_HOME/data_timeline*)"
for file in $files; do
    tmp_file="$file.tmp"
    tr '\r' '\n' < $file > $tmp_file
    mv $tmp_file $file
done

# commit these changes
cd $MSK_IMPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: newline fix"

# -----------------------------------------------------------------------------------------------------------
## FUNCTIONS

# Function to generate case lists by cancer type
function addCancerTypeCaseLists {
    STUDY_DATA_DIRECTORY=$1
    STUDY_ID=$2

    # remove current case lists and run oncotree converter before creating new cancer case lists
    rm $STUDY_DATA_DIRECTORY/case_lists/*
    $PYTHON_BINARY $PORTAL_HOME/scripts/oncotree_code_converter.py --oncotree-url "http://oncotree.mskcc.org/oncotree/api/tumor_types.txt" --clinical-file $STUDY_DATA_DIRECTORY/data_clinical.txt
    $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file="$STUDY_DATA_DIRECTORY/data_clinical.txt" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="CANCER_TYPE"
    if [ "$STUDY_ID" == "mskimpact" ] || [ "$STUDY_ID" == "mixedpact" ]; then
        $PYTHON_BINARY $PORTAL_HOME/scripts/create_case_lists_by_cancer_type.py --clinical-file="$STUDY_DATA_DIRECTORY/data_clinical.txt" --output-directory="$STUDY_DATA_DIRECTORY/case_lists" --study-id="$STUDY_ID" --attribute="12_245_PARTC_CONSENTED"
    fi
}

# Function for adding "DATE ADDED" information to clinical data
function addDateAddedData {
    STUDY_DATA_DIRECTORY=$1
    STUDY_ID=$2

    # add "date added" to clinical data file
    $PYTHON_BINARY $PORTAL_HOME/scripts/impact_timeline.py --hgrepo=$STUDY_DATA_DIRECTORY
    cd $STUDY_DATA_DIRECTORY; rm *.orig
    rm $STUDY_DATA_DIRECTORY/case_lists/*.orig
}

# Function for restarting tomcats
function restartTomcats {
    # redeploy war
    echo "Requesting redeployment of msk portal war..."
    echo $(date)
    ssh -i $HOME/.ssh/id_rsa_msk_tomcat_restarts_key cbioportal_importer@dashi.cbio.mskcc.org touch /srv/data/portal-cron/msk-tomcat-restart
    ssh -i $HOME/.ssh/id_rsa_msk_tomcat_restarts_key cbioportal_importer@dashi2.cbio.mskcc.org touch /srv/data/portal-cron/msk-tomcat-restart
}


# -----------------------------------------------------------------------------------------------------------
# fetch CRDB data
echo "fetching CRDB data"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/crdb_fetcher.jar -stage $MSK_IMPACT_DATA_HOME
cd $MSK_IMPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: CRDB"

# fetch Darwin data

echo "fetching Darwin impact data"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_IMPACT_DATA_HOME -s mskimpact
cd $MSK_IMPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin impact"

echo "fetching Darwin heme data"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_HEMEPACT_DATA_HOME -s mskimpact_heme
cd $MSK_HEMEPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin heme"

echo "fetching Darwin raindance data"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_RAINDANCE_DATA_HOME -s mskraindance
cd $MSK_RAINDANCE_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin raindance"

echo "fetching Darwin archer data"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/darwin_fetcher.jar -d $MSK_ARCHER_DATA_HOME -s mskarcher
cd $MSK_ARCHER_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Darwin archer"

DB_VERSION_FAIL=0
IMPORT_STATUS_IMPACT=0
IMPORT_STATUS_HEME=0
IMPORT_STATUS_RAINDANCE=0
IMPORT_STATUS_MIXEDPACT=0
IMPORT_STATUS_ARCHER=0
MERGE_FAIL=0
MSK_KINGS_SUBSET_FAIL=0
MSK_QUEENS_SUBSET_FAIL=0
MSK_LEHIGH_SUBSET_FAIL=0
MSK_MCI_SUBSET_FAIL=0
MSK_HARTFORD_SUBSET_FAIL=0
IMPORT_FAIL_MIXEDPACT=0
IMPORT_FAIL_KINGS=0
IMPORT_FAIL_LEHIGH=0
IMPORT_FAIL_QUEENS=0
IMPORT_FAIL_MCI=0
IMPORT_FAIL_HARTFORD=0
IMPORT_FAIL_LYMPHOMA=0
LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=0

# fetch new/updated IMPACT samples using CVR Web service   (must come after mercurial fetching)
echo "fetching samples from CVR Web service  ..."
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_IMPACT_DATA_HOME -i mskimpact -r 150
if [ $? -gt 0 ]; then
    echo "CVR fetch failed!"
    cd $MSK_IMPACT_DATA_HOME;$HG_BINARY update -C;rm *.orig
    IMPORT_STATUS_IMPACT=1
else
    # sanity check for empty allele counts
    bash $PORTAL_HOME/scripts/test_if_impact_has_lost_allele_count.sh
    if [ $? -gt 0 ]; then 
        echo "Empty allele count sanity check failed! MSK-IMPACT will not be imported!"
        cd $MSK_IMPACT_DATA_HOME;$HG_BINARY update -C;rm *.orig
        IMPORT_STATUS_IMPACT=1
    else
        echo "committing cvr data"
        cd $MSK_IMPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: CVR"
    fi
fi

# fetch new/updated IMPACT germline samples using CVR Web service   (must come after normal cvr fetching)
echo "fetching CVR GML data ..."
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_IMPACT_DATA_HOME -g -i mskimpact
if [ $? -gt 0 ]; then
    echo "CVR Germline fetch failed!"
    cd $MSK_IMPACT_DATA_HOME;$HG_BINARY update -C;rm *.orig
    IMPORT_STATUS_IMPACT=1
else
    echo "committing CVR germline data"
    cd $MSK_IMPACT_DATA_HOME;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: CVR Germline"
fi

# fetch new/updated raindance samples using CVR Web service (must come after mercurial fetching). The -s flag skips segment data fetching
echo "fetching CVR raindance data..."
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_RAINDANCE_DATA_HOME -s -i mskraindance
if [ $? -gt 0 ]; then
    echo "CVR raindance fetch failed!"
    echo "This will not affect importing of mskimpact"
    cd $MSK_RAINDANCE_DATA_HOME;$HG_BINARY update -C;rm *.orig
    IMPORT_STATUS_RAINDANCE=1
else
    # raindance does not provide copy number or fusions data.
    echo "removing unused files"
    cd $MSK_RAINDANCE_DATA_HOME; rm data_CNA.txt; rm data_fusions.txt; rm data_SV.txt; rm mskraindance_data_cna_hg19.seg
    cd $MSK_RAINDANCE_DATA_HOME;$HG_BINARY commit -m "Latest Raindance dataset"
fi

# fetch new/updated heme samples using CVR Web service (must come after mercurial fetching). Threshold is set to 50 since heme contains only 190 samples (07/12/2017)
echo "fetching CVR heme data..."
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_HEMEPACT_DATA_HOME -i mskimpact_heme -r 50
if [ $? -gt 0 ]; then
      echo "CVR heme fetch failed!"
      echo "This will not affect importing of mskimpact"
      cd $MSK_HEMEPACT_DATA_HOME;$HG_BINARY update -C;rm *.orig
      IMPORT_STATUS_HEME=1
else
      cd $MSK_HEMEPACT_DATA_HOME;$HG_BINARY commit -m "Latest heme dataset"
fi

# fetch new/updated archer samples using CVR Web service (must come after mercurial fetching).
echo "fetching CVR archer data..."
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -jar $PORTAL_HOME/lib/cvr_fetcher.jar -d $MSK_ARCHER_DATA_HOME -i mskarcher -s
if [ $? -gt 0 ]; then
    echo "CVR Archer fetch failed!"
    echo "This will not affect importing of mskimpact"
    cd $MSK_ARCHER_DATA_HOME;$HG_BINARY update -C;rm *.orig
    IMPORT_STATUS_ARCHER=1
else
    # mskarcher does not provide copy number, mutations, or seg data, renaming gene matrix file until we get the mskarcher gene panel imported
    echo "Removing unused files"
    cd $MSK_ARCHER_DATA_HOME; rm data_CNA.txt; rm data_mutations_*; rm mskarcher_data_cna_hg19.seg; mv data_gene_matrix.txt ignore_data_gene_matrix.txt
    cd $MSK_ARCHER_DATA_HOME;$HG_BINARY commit -m "Latest archer dataset"
fi

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for MSK-IMPACT
addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact"
cd $STUDY_DATA_DIRECTORY; $HG_BINARY add; $HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Case Lists"
addDateAddedData $MSK_IMPACT_DATA_HOME "mskimpact"
cd $STUDY_DATA_DIRECTORY;$HG_BINARY add;$HG_BINARY commit -m "Latest MSK-IMPACT Dataset: Sample Date Clinical File"

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for RAINDANCE
addCancerTypeCaseLists $MSK_RAINDANCE_DATA_HOME "mskraindance"
cd $STUDY_DATA_DIRECTORY; $HG_BINARY add; $HG_BINARY commit -m "Latest RAINDANCE Dataset: Case Lists"
addDateAddedData $MSK_RAINDANCE_DATA_HOME "mskraindance"
cd $STUDY_DATA_DIRECTORY;$HG_BINARY add;$HG_BINARY commit -m "Latest RAINDANCE Dataset: Sample Date Clinical File"

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for HEMEPACT
addCancerTypeCaseLists $MSK_HEMEPACT_DATA_HOME "mskimpact_heme"
cd $STUDY_DATA_DIRECTORY; $HG_BINARY add; $HG_BINARY commit -m "Latest HEMEPACT Dataset: Case Lists"
addDateAddedData $MSK_HEMEPACT_DATA_HOME "mskimpact_heme"
cd $STUDY_DATA_DIRECTORY;$HG_BINARY add;$HG_BINARY commit -m "Latest HEMEPACT Dataset: Sample Date Clinical File"

# generate case lists by cancer type and add "DATE ADDED" info to clinical data for ARCHER
addCancerTypeCaseLists $MSK_ARCHER_DATA_HOME "mskarcher"
cd $STUDY_DATA_DIRECTORY; $HG_BINARY add; $HG_BINARY commit -m "Latest ARCHER Dataset: Case Lists"
addDateAddedData $MSK_ARCHER_DATA_HOME "mskarcher"
cd $STUDY_DATA_DIRECTORY;$HG_BINARY add;$HG_BINARY commit -m "Latest ARCHER Dataset: Sample Date Clinical File"

# Merge Archer fusion data into the impact cohort
$PYTHON_BINARY $PORTAL_HOME/scripts/archer_fusions_merger.py --archer-fusions $MSK_ARCHER_DATA_HOME/data_fusions.txt --linked-mskimpact-cases-filename $MSK_ARCHER_DATA_HOME/linked_mskimpact_cases.txt --msk-fusions $MSK_IMPACT_DATA_HOME/data_fusions.txt --clinical-filename $MSK_IMPACT_DATA_HOME/data_clinical.txt --archer-samples-filename $tmp/archer_ids.txt
cd $MSK_IMPACT_DATA_HOME; $HG_BINARY add; $HG_BINARY commit -m "Adding ARCHER fusions to MSKIMPACT"

# check database version before importing anything
echo "Checking if database version is compatible"
echo $(date)
$JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --check-db-version
if [ $? -gt 0 ]; then
    echo "Database version expected by portal does not match version in database!"
    DB_VERSION_FAIL=1
    IMPORT_STATUS_IMPACT=1
fi

if [ $DB_VERSION_FAIL -eq 0 ]; then
    # import into portal database
    echo "importing cancer type updates into msk portal database..."
    $JAVA_HOME/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27182 -ea -Dspring.profiles.active=dbcp -Djava.io.tmpdir="$tmp" -cp $PORTAL_HOME/lib/msk-dmp-importer.jar org.mskcc.cbio.importer.Admin --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
fi

# Temp study importer arguments
# (1): cancer study id [ mskimpact | mskimpact_heme | mskraindance | mskarcher | mixedpact | msk_kingscounty | msk_lehighvalley | msk_queenscancercenter ]
# (2): temp study id [ temporary_mskimpact | temporary_mskimpact_heme | temporary_mskraindance | temporary_mskarcher | temporary_mixedpact | temporary_msk_kingscounty | temporary_msk_lehighvalley | temporary_msk_queenscancercenter ]
# (3): backup study id [ yesterday_mskimpact | yesterday_mskimpact_heme | yesterday_mskraindance | yesterday_mskarcher | yesterday_mixedpact | yesterday_msk_kingscounty | yesterday_msk_lehighvalley | yesterday_msk_queenscancercenter ]
# (4): portal name [ mskimpact-portal | mskheme-portal | mskraindance-portal | mskarcher-portal | mixedpact-portal |  msk-kingscounty-portal | msk-lehighvalley-portal | msk-queenscancercenter-portal ]
# (5): study path [ $MSK_IMPACT_DATA_HOME | $MSK_HEMEPACT_DATA_HOME | $MSK_RAINDANCE_DATA_HOME | $MSK_ARCHER_DATA_HOME | $MSK_MIXEDPACT_DATA_HOME | $MSK_KINGS_DATA_HOME | $MSK_LEHIGH_DATA_HOME | $MSK_QUEENS_DATA_HOME ]
# (6): notification file [ $mskimpact_notification_file | $mskheme_notification_file | $mskraindance_notification_file | $mixedpact_notification_file | $kingscounty_notification_file | $lehighvalley_notification_file | $queenscancercenter_notification_file ]
# (7): tmp directory
# (8): email list
# (9): oncotree version [ oncotree_candidate_release | oncotree_latest_stable ]
# (10): importer jar
# (11): transcript overrides source [ uniprot | mskcc ]

## TEMP STUDY IMPORT: MSKIMPACT
RESTART_AFTER_IMPACT_IMPORT=0
if [ $IMPORT_STATUS_IMPACT -eq 0 ]; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact" --temp-study-id="temporary_mskimpact" --backup-study-id="yesterday_mskimpact" --portal-name="mskimpact-portal" --study-path="$MSK_IMPACT_DATA_HOME" --notification-file="$mskimpact_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ]; then
        # set flag 'RESTART_AFTER_IMPACT_IMPORT' to 1 if MSKIMPACT updated successfully
        RESTART_AFTER_IMPACT_IMPORT=1
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo "Not importing mskimpact - database version is not compatible"
    else
        echo "Not importing mskimpact - something went wrong with a fetch"
    fi
fi

## TOMCAT RESTART
# restart tomcat only if the MSK-IMPACT update was succesful
if [ $RESTART_AFTER_IMPACT_IMPORT -eq 0 ]; then
    echo "Failed to update MSK-IMPACT - next tomcat restart will execute after successful updates to other MSK clinical pipelines and/or MSK affiliate studies..."
    echo $(date)
else
    restartTomcats
fi

# set 'RESTART_AFTER_DMP_PIPELINES_IMPORT' flag to 1 if RAINDANCE, ARCHER, HEMEPACT, or MIXEDPACT succesfully update
RESTART_AFTER_DMP_PIPELINES_IMPORT=0
## TEMP STUDY IMPORT: MSKIMPACT_HEME
if [ $IMPORT_STATUS_HEME -eq 0 ]; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskimpact_heme" --temp-study-id="temporary_mskimpact_heme" --backup-study-id="yesterday_mskimpact_heme" --portal-name="mskheme-portal" --study-path="$MSK_HEMEPACT_DATA_HOME" --notification-file="$mskheme_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ]; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo "Not importing mskimpact_heme - database version is not compatible"
    else
        echo "Not importing mskimpact_heme - something went wrong with a fetch"
    fi
fi

## TEMP STUDY IMPORT: MSKRAINDANCE
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ]; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskraindance" --temp-study-id="temporary_mskraindance" --backup-study-id="yesterday_mskraindance" --portal-name="mskraindance-portal" --study-path="$MSK_RAINDANCE_DATA_HOME" --notification-file="$mskraindance_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ]; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo "Not importing mskraindance - database version is not compatible"
    else
        echo "Not importing mskraindance - something went wrong with a fetch"
    fi
fi

# TEMP STUDY IMPORT: MSKARCHER
if [ $IMPORT_STATUS_ARCHER -eq 0 ]; then
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mskarcher" --temp-study-id="temporary_mskarcher" --backup-study-id="yesterday_mskarcher" --portal-name="mskarcher-portal" --study-path="$MSK_ARCHER_DATA_HOME" --notification-file="$mskarcher_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -eq 0 ]; then
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
    fi
else
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo "Not importing mskarcher - database version is not compatible"
    else
        echo "Not importing mskarcher - something went wrong with a fetch"
    fi
fi

## MSK-IMPACT, HEMEPACT, and RAINDANCE merge
echo "Beginning merge of MSK-IMPACT, HEMEPACT, RAINDANCE data..."
echo $(date)

# first touch meta_clinical.txt and meta_SV.txt in each directory if not already exists
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_IMPACT_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ]; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ]; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

# raindance doesn't have SV data so no need to touch that meta file
if [ ! -f $MSK_RAINDANCE_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_RAINDANCE_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_ARCHER_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_ARCHER_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_ARCHER_DATA_HOME/meta_SV.txt ]; then
    touch $MSK_ARCHER_DATA_HOME/meta_SV.txt
fi

# merge data from both directories and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_MIXEDPACT_DATA_HOME -i mixedpact -m "true" -e $tmp/archer_ids.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_RAINDANCE_DATA_HOME $MSK_ARCHER_DATA_HOME
if [ $? -gt 0 ]; then
    echo "MIXEDPACT merge failed! Study will not be updated in the portal."
    echo $(date)
    MERGE_FAIL=1
else
    # if merge successful then copy case lists from MSK-IMPACT directory and change stable id
    echo "MIXEDPACT merge successful! Creating cancer type case lists..."
    echo $(date)
    addCancerTypeCaseLists $MSK_MIXEDPACT_DATA_HOME "mixedpact"
fi

# check that meta_clinical.txt and meta_SV.txt are actually empty files before deleting from IMPACT, HEME, and RAINDANCE studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_IMPACT_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ]; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ]; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_RAINDANCE_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_RAINDANCE_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_ARCHER_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_ARCHER_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_ARCHER_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_ARCHER_DATA_HOME/meta_clinical.txt
fi

# update MIXEDPACT in portal only if merge and case list updates were succesful
if [ $MERGE_FAIL -eq 0 ]; then
    echo "Importing MIXEDPACT study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="mixedpact" --temp-study-id="temporary_mixedpact" --backup-study-id="yesterday_mixedpact" --portal-name="mixedpact-portal" --study-path="$MSK_MIXEDPACT_DATA_HOME" --notification-file="$mixedpact_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_MIXEDPACT=1
    else
        RESTART_AFTER_DMP_PIPELINES_IMPORT=1
    fi
else
    echo "Something went wrong with merging clinical studies."
    IMPORT_FAIL_MIXEDPACT=1
fi

# commit or revert changes for MIXEDPACT
if [ $IMPORT_FAIL_MIXEDPACT -gt 0 ]; then
    echo "MIXEDPACT merge and/or updates failed! Reverting data to last commit."
    cd $MSK_MIXEDPACT_DATA_HOME;$HG_BINARY update -C
    rm $MSK_MIXEDPACT_DATA_HOME/*.orig
    rm $MSK_MIXEDPACT_DATA_HOME/case_lists/*.orig
else
    echo "Committing MIXEDPACT data"
    cd $MSK_MIXEDPACT_DATA_HOME;$HG_BINARY add;$HG_BINARY commit -m "Latest MIXEDPACT dataset"
fi
## END MSK-IMPACT, HEMEPACT, and RAINDANCE merge

## TOMCAT RESTART
# Restart will only execute if at least one of these studies succesfully updated.
#   MSKIMPACT_HEME
#   MSKRAINDANCE
#   MSKARCHER
#   MIXEDPACT
if [ $RESTART_AFTER_DMP_PIPELINES_IMPORT -eq 0 ]; then
    echo "Failed to update HEMEPACT, RAINDANCE, ARCHER, and MIXEDPACT - next tomcat restart will execute after successful updates to MSK affiliate studies..."
    echo $(date)
else
    restartTomcats
fi

## Subset MIXEDPACT on INSTITUTE for institute specific impact studies

# first touch meta_clinical.txt in mixedpact if not already exists
if [ ! -f $MSK_MIXEDPACT_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_MIXEDPACT_DATA_HOME/meta_clinical.txt
fi

# subset the mixedpact study for Queens Cancer Center, Lehigh Valley, Kings County Cancer Center, Miami Cancer Institute, and Hartford Health Care
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_kingscounty -o=$MSK_KINGS_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Kings County Cancer Center" -s=$tmp/kings_subset.txt
if [ $? -gt 0 ]; then
    echo "MSK Kings County subset failed! Study will not be updated in the portal."
    MSK_KINGS_SUBSET_FAIL=1
else
    echo "MSK Kings County subset successful!"
    addCancerTypeCaseLists $MSK_KINGS_DATA_HOME "msk_kingscounty"
fi
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_lehighvalley -o=$MSK_LEHIGH_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Lehigh Valley Health Network" -s=$tmp/lehigh_subset.txt
if [ $? -gt 0 ]; then
    echo "MSK Lehigh Valley subset failed! Study will not be updated in the portal."
    MSK_LEHIGH_SUBSET_FAIL=1
else
    echo "MSK Lehigh Valley subset successful!"
    addCancerTypeCaseLists $MSK_LEHIGH_DATA_HOME "msk_lehighvalley"
fi
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_queenscancercenter -o=$MSK_QUEENS_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Queens Cancer Center,Queens Hospital Cancer Center" -s=$tmp/queens_subset.txt
if [ $? -gt 0 ]; then
    echo "MSK Queens Cancer Center subset failed! Study will not be updated in the portal."
    MSK_QUEENS_SUBSET_FAIL=1
else
    echo "MSK Queens Cancer Center subset successful!"
    addCancerTypeCaseLists $MSK_QUEENS_DATA_HOME "msk_queenscancercenter"
fi
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_miamicancerinstitute -o=$MSK_MCI_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Miami Cancer Institute" -s=$tmp/mci_subset.txt
if [ $? -gt 0 ]; then
    echo "MSK Miami Cancer Institute subset failed! Study will not be updated in the portal."
    MSK_MCI_SUBSET_FAIL=1
else
    echo "MSK Miami Cancer Institute subset successful!"
    addCancerTypeCaseLists $MSK_MCI_DATA_HOME "msk_miamicancerinstitute"
fi
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_hartfordhealthcare -o=$MSK_HARTFORD_DATA_HOME -m=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Hartford Healthcare" -s=$tmp/hartford_subset.txt
if [ $? -gt 0 ]; then
    echo "MSK Hartford Healthcare subset failed! Study will not be updated in the portal."
    MSK_HARTFORD_SUBSET_FAIL=1
else
    echo "MSK Hartford Healthcare subset successful!"
    addCancerTypeCaseLists $MSK_HARTFORD_DATA_HOME "msk_hartfordhealthcare"
fi

# remove the meta clinical file from mixedpact
if [ $(wc -l < $MSK_MIXEDPACT_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_MIXEDPACT_DATA_HOME/meta_clinical.txt
fi

# set 'RESTART_AFTER_MSK_AFFILIATE_IMPORT' flag to 1 if Kings County, Lehigh Valley, Queens Cancer Center, Miami Cancer Institute, or Lymphoma super cohort succesfully update
RESTART_AFTER_MSK_AFFILIATE_IMPORT=0
# update msk_kingscounty in portal only if subset was successful
if [ $MSK_KINGS_SUBSET_FAIL -eq 0 ]; then
    echo "Importing msk_kingscounty study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_kingscounty" --temp-study-id="temporary_msk_kingscounty" --backup-study-id="yesterday_msk_kingscounty" --portal-name="msk-kingscounty-portal" --study-path="$MSK_KINGS_DATA_HOME" --notification-file="$kingscounty_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_KINGS=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for KINGSCOUNTY."
    IMPORT_FAIL_KINGS=1
fi
# commit or revert changes for KINGSCOUNTY
if [ $IMPORT_FAIL_KINGS -gt 0 ]; then
    echo "KINGSCOUNTY subset and/or updates failed! Reverting data to last commit."
    cd $MSK_KINGS_DATA_HOME;$HG_BINARY update -C
    rm $MSK_KINGS_DATA_HOME/*.orig
    rm $MSK_KINGS_DATA_HOME/case_lists/*.orig
else
    echo "Committing KINGSCOUNTY data"
    cd $MSK_KINGS_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest KINGSCOUNTY dataset"
fi

# update msk_lehighvalley in portal only if subset was successful
if [ $MSK_LEHIGH_SUBSET_FAIL -eq 0 ]; then
    echo "Importing msk_lehighvalley study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_lehighvalley" --temp-study-id="temporary_msk_lehighvalley" --backup-study-id="yesterday_msk_lehighvalley" --portal-name="msk-lehighvalley-portal" --study-path="$MSK_LEHIGH_DATA_HOME" --notification-file="$lehighvalley_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_LEHIGH=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for LEHIGHVALLEY."
    IMPORT_FAIL_LEHIGH=1
fi
# commit or revert changes for LEHIGHVALLEY
if [ $IMPORT_FAIL_LEHIGH -gt 0 ]; then
    echo "LEHIGHVALLEY subset and/or updates failed! Reverting data to last commit."
    cd $MSK_LEHIGH_DATA_HOME;$HG_BINARY update -C
    rm $MSK_LEHIGH_DATA_HOME/*.orig
    rm $MSK_LEHIGH_DATA_HOME/case_lists/*.orig
else
    echo "Committing LEHIGHVALLEY data"
    cd $MSK_LEHIGH_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest LEHIGHVALLEY dataset"
fi

# update msk_queenscancercenter in portal only if subset was successful
if [ $MSK_QUEENS_SUBSET_FAIL -eq 0 ]; then
    echo "Importing msk_queenscancercenter study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_queenscancercenter" --temp-study-id="temporary_msk_queenscancercenter" --backup-study-id="yesterday_msk_queenscancercenter" --portal-name="msk-queenscancercenter-portal" --study-path="$MSK_QUEENS_DATA_HOME" --notification-file="$queenscancercenter_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_QUEENS=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for QUEENSCANCERCENTER."
    IMPORT_FAIL_QUEENS=1
fi
# commit or revert changes for QUEENSCANCERCENTER
if [ $IMPORT_FAIL_QUEENS -gt 0 ]; then
    echo "QUEENSCANCERCENTER subset and/or updates failed! Reverting data to last commit."
    cd $MSK_QUEENS_DATA_HOME;$HG_BINARY update -C
    rm $MSK_QUEENS_DATA_HOME/*.orig
    rm $MSK_QUEENS_DATA_HOME/case_lists/*.orig
else
    echo "Committing QUEENSCANCERCENTER data"
    cd $MSK_QUEENS_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest QUEENSCANCERCENTER dataset"
fi

# update msk_miamicancerinstitute in portal only if subset was successful
if [ $MSK_MCI_SUBSET_FAIL -eq 0 ]; then
    echo "Importing msk_miamicancerinstitute study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_miamicancerinstitute" --temp-study-id="temporary_msk_miamicancerinstitute" --backup-study-id="yesterday_msk_miamicancerinstitute" --portal-name="msk-mci-portal" --study-path="$MSK_MCI_DATA_HOME" --notification-file="$miamicancerinstitute_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_MCI=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for MIAMICANCERINSTITUTE."
    IMPORT_FAIL_MCI=1
fi
# commit or revert changes for MIAMICANCERINSTITUTE
if [ $IMPORT_FAIL_MCI -gt 0 ]; then
    echo "MIAMICANCERINSTITUTE subset and/or updates failed! Reverting data to last commit."
    cd $MSK_MCI_DATA_HOME;$HG_BINARY update -C
    rm $MSK_MCI_DATA_HOME/*.orig
    rm $MSK_MCI_DATA_HOME/case_lists/*.orig
else
    echo "Committing MIAMICANCERINSTITUTE data"
    cd $MSK_MCI_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest MIAMICANCERINSTITUTE dataset"
fi

# update msk_hartfordhealthcare in portal only if subset was successful
if [ $MSK_HARTFORD_SUBSET_FAIL -eq 0 ]; then
    echo "Importing msk_hartfordhealthcare study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="msk_hartfordhealthcare" --temp-study-id="temporary_msk_hartfordhealthcare" --backup-study-id="yesterday_msk_hartfordhealthcare" --portal-name="msk-hartford-portal" --study-path="$MSK_HARTFORD_DATA_HOME" --notification-file="$hartfordhealthcare_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_HARTFORD=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for HARTFORDHEALTHCARE."
    IMPORT_FAIL_HARTFORD=1
fi
# commit or revert changes for HARTFORDHEALTHCARE
if [ $IMPORT_FAIL_HARTFORD -gt 0 ]; then
    echo "HARTFORDHEALTHCARE subset and/or updates failed! Reverting data to last commit."
    cd $MSK_HARTFORD_DATA_HOME;$HG_BINARY update -C
    rm $MSK_HARTFORD_DATA_HOME/*.orig
    rm $MSK_HARTFORD_DATA_HOME/case_lists/*.orig
else
    echo "Committing HARTFORDHEALTHCARE data"
    cd $MSK_HARTFORD_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest HARTFORDHEALTHCARE dataset"
fi


## END Subset MIXEDPACT on INSTITUTE

# Create lymphoma "super" cohort
# Subset MSK-IMPACT and HEMEPACT by Cancer Type

# first touch meta_clinical.txt and meta_SV.txt in mskimpact, hemepact, and fmibat if not already exists - need these to generate merged subsets
if [ ! -f $MSK_IMPACT_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_IMPACT_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_IMPACT_DATA_HOME/meta_SV.txt ]; then
    touch $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt ]; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt
fi

if [ ! -f $MSK_HEMEPACT_DATA_HOME/meta_SV.txt ]; then
    touch $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ ! -f $FMI_BATLEVI_DATA_HOME/meta_clinical.txt ]; then
    touch $FMI_BATLEVI_DATA_HOME/meta_clinical.txt
fi

# now subset sample files with lymphoma cases from mskimpact and hemepact
LYMPHOMA_FILTER_CRITERIA="CANCER_TYPE=Non-Hodgkin Lymphoma,Hodgkin Lymphoma"
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_IMPACT_DATA_HOME/data_clinical.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$tmp/mskimpact_lymphoma_subset.txt"
if [ $? -gt 0 ]; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT. Skipping merge and update of lymphoma super cohort!"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_HEMEPACT_DATA_HOME/data_clinical.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$tmp/mskimpact_heme_lymphoma_subset.txt"
if [ $? -gt 0 ]; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT Heme. Skipping merge and update of lymphoma super cohort!"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

# check sizes of subset files before attempting to merge data using these subsets
grep -v '^#' $FMI_BATLEVI_DATA_HOME/data_clinical.txt | awk -F '\t' '{if ($2 != "SAMPLE_ID") print $2;}' > $tmp/lymphoma_subset_samples.txt
if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $tmp/lymphoma_subset_samples.txt) -eq 0 ]]; then
    echo "ERROR! Subset list $tmp/lymphoma_subset_samples.txt is empty. Skipping merge and update of lymphoma super cohort!"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $tmp/mskimpact_lymphoma_subset.txt) -eq 0 ]]; then
    echo "ERROR! Subset list $tmp/mskimpact_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $tmp/mskimpact_lymphoma_subset.txt >> $tmp/lymphoma_subset_samples.txt
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $tmp/mskimpact_heme_lymphoma_subset.txt) -eq 0 ]]; then
    echo "ERROR! Subset list $tmp/mskimpact_heme_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $tmp/mskimpact_heme_lymphoma_subset.txt >> $tmp/lymphoma_subset_samples.txt
fi

# merge data from mskimpact and hemepact lymphoma subsets with FMI BAT study
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ]; then 
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py  -d $LYMPHOMA_SUPER_COHORT_DATA_HOME -i "lymphoma_super_cohort_fmi_msk" -m "true" -s $tmp/lymphoma_subset_samples.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $FMI_BATLEVI_DATA_HOME
    if [ $? -gt 0 ]; then
        echo "Lymphoma super cohort subset failed! Lymphoma super cohort study will not be updated in the portal."
        LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
    fi
    # remove files we don't need for lymphoma super cohort
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/*genie*
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/seq_date.txt
fi

# check that meta_clinical.txt and meta_SV.txt are actually empty files before deleting from IMPACT, HEME, and RAINDANCE studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_IMPACT_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ]; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_clinical.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ]; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $FMI_BATLEVI_DATA_HOME/meta_clinical.txt) -eq 0 ]; then
    rm $FMI_BATLEVI_DATA_HOME/meta_clinical.txt
fi

# attempt to import if merge and subset successful
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ]; then
    echo "Importing lymphoma 'super' cohort study..."
    echo $(date)
    bash $PORTAL_HOME/scripts/import-temp-study.sh --study-id="lymphoma_super_cohort_fmi_msk" --temp-study-id="temporary_lymphoma_super_cohort_fmi_msk" --backup-study-id="yesterday_lymphoma_super_cohort_fmi_msk" --portal-name="msk-fmi-lymphoma-portal" --study-path="$LYMPHOMA_SUPER_COHORT_DATA_HOME" --notification-file="$lymphoma_super_cohort_notification_file" --tmp-directory="$tmp" --email-list="$email_list" --oncotree-version="${ONCOTREE_VERSION_TO_USE}" --importer-jar="$PORTAL_HOME/lib/msk-dmp-importer.jar" --transcript-overrides-source="mskcc"
    if [ $? -gt 0 ]; then
        IMPORT_FAIL_LYMPHOMA=1
    else
        RESTART_AFTER_MSK_AFFILIATE_IMPORT=1
    fi
else
    echo "Something went wrong with subsetting clinical studies for Lymphoma super cohort."
    IMPORT_FAIL_LYMPHOMA=1
fi

# commit or revert changes for Lymphoma super cohort
if [ $IMPORT_FAIL_LYMPHOMA -gt 0 ]; then
    echo "Lymphoma super cohort subset and/or updates failed! Reverting data to last commit."
    cd $LYMPHOMA_SUPER_COHORT_DATA_HOME;$HG_BINARY update -C
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/*.orig
    rm $LYMPHOMA_SUPER_COHORT_DATA_HOME/case_lists/*.orig
else
    echo "Committing Lymphoma super cohort data"
    cd $LYMPHOMA_SUPER_COHORT_DATA_HOME;$HG_BINARY add *;$HG_BINARY add case_lists/*;$HG_BINARY commit -m "Latest Lymphoma Super Cohort dataset"
fi

## TOMCAT RESTART
# Restart will only execute if at least one of these studies succesfully updated.
#   MSK_KINGSCOUNTY
#   MSK_LEHIGHVALLEY
#   MSK_QUEENSCANCERCENTER
if [ $RESTART_AFTER_MSK_AFFILIATE_IMPORT -eq 0 ]; then
    echo "Failed to update all MSK affiliate studies"
else
    restartTomcats
fi

# check updated data back into mercurial
echo "Pushing DMP-IMPACT updates back to msk-impact repository..."
echo $(date)
cd $MSK_IMPACT_DATA_HOME;$HG_BINARY push

### FAILURE EMAIL ###

EMAIL_BODY="The MSKIMPACT database version is incompatible. Imports will be skipped until database is updated."
# send email if db version isn't compatible
if [ $DB_VERSION_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Update Failure: DB version is incompatible" $email_list
fi

EMAIL_BODY="The MSKIMPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_IMPACT -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The HEMEPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_HEME -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "HEMEPACT Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The RAINDANCE study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_RAINDANCE -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "RAINDANCE Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The ARCHER study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_ARCHER -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "archer Fetch Failure: Import" $email_list
fi

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, and RAINDANCE data. Merged study will not be updated."
if [ $MERGE_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIXEDPACT Merge Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Kings County Cancer Center data. Subset study will not be updated."
if [ $MSK_KINGS_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "KINGSCOUNTY Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Lehigh Valley data. Subset study will not be updated."
if [ $MSK_LEHIGH_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "LEHIGHVALLEY Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Queens Cancer Center data. Subset study will not be updated."
if [ $MSK_QUEENS_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "QUEENSCANCERCENTER Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Miami Cancer Institute data. Subset study will not be updated."
if [ $MSK_MCI_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIAMICANCERINSTITUTE Subset Failure: Study will not be updated." $email_list
fi

EMAIL_BODY="Failed to subset Hartford Healthcare data. Subset study will not be updated."
if [ $MSK_HARTFORD_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "HARTFORDHEALTHCARE Subset Failure: Study will not be updated." $email_list
fi

echo "Fetching and importing of clinical datasets complete!"
echo $(date)
