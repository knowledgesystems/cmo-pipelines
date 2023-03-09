# DMP Updates Workflow

## 0. Pre-Update Steps
**********************************************************************

**Remove existing tmp files from $MSK_DMP_TMPDIR**

```
# -----------------------------------------------------------------------------------------------------------
# START DMP DATA FETCHING
echo $(date)

if [[ -d "$MSK_DMP_TMPDIR" && "$MSK_DMP_TMPDIR" != "/" ]] ; then
    rm -rf "$MSK_DMP_TMPDIR"/*
fi
```

**Check environment variables**

```
if [ -z $JAVA_BINARY ] | [ -z $GIT_BINARY ] | [ -z $PORTAL_HOME ] | [ -z $MSK_IMPACT_DATA_HOME ] ; then
    message="test could not run import-dmp-impact.sh: automation-environment.sh script must be run in order to set needed environment variables (like MSK_IMPACT_DATA_HOME, ...)"
    echo $message
    echo -e "$message" |  mail -s "fetch-dmp-data-for-import failed to run." $PIPELINES_EMAIL_LIST
    sendPreImportFailureMessageMskPipelineLogsSlack "$message"
    exit 2
fi
```

**Refresh CDD and OncoTree Cache**

```
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
        sendPreImportFailureMessageMskPipelineLogsSlack "$message"
        CDD_RECACHE_FAIL=1
    fi
    bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh --oncotree-only
    if [ $? -gt 0 ]; then
        message="Failed to refresh ONCOTREE cache!"
        echo $message
        echo -e "$message" | mail -s "ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        sendPreImportFailureMessageMskPipelineLogsSlack "$message"
        ONCOTREE_RECACHE_FAIL=1
    fi
    if [[ $CDD_RECACHE_FAIL -gt 0 || $ONCOTREE_RECACHE_FAIL -gt 0 ]] ; then
        echo "Oncotree and/or CDD recache failed! Exiting..."
        exit 2
    fi
fi
```

**Update DMP Git repository**

```
# fetch clinical data from data repository
echo "fetching updates from dmp repository..."
$JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source dmp --run-date latest
if [ $? -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "Git Failure: DMP repository update"
    exit 2
fi
```

## 1. REDCap Exports
**********************************************************************
*NOTES:*

```
# -----------------------------------------------------------------------------------------------------------
# PRE-DATA-FETCH REDCAP EXPORTS

# export data_clinical and data_clinical_supp_date for each project from redcap (starting point)
# if export fails:
# don't re-import into redcap (would wipe out days worth of data)
# consider failing import
```

**REDCAP | Export Clinical Supp Date Added REDCap projects**

*MSKIMPACT*

```
printTimeStampedDataProcessingStepMessage "export of supp date files from redcap"
echo "exporting impact data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_supp_date_cbioportal_added
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_IMPACT_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap export of mskimpact_supp_date_cbioportal_added"
fi
```

*HEMEPACT*

```
echo "exporting hemepact data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_HEME_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap export of hemepact_data_clinical_supp_date"
fi
```

*RAINDANCE*

```
echo "exporting raindance data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_RAINDANCE_DATA_HOME mskraindance_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_RAINDANCE_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap export of mskraindance_data_clinical_supp_date"
fi
```

*ARCHER*

```
echo "exporting archer data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME mskarcher_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_ARCHER_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER Redcap export of mskarcher_data_clinical_supp_date"
fi
```

*ACCESS*

```
echo "exporting access data_clinical_supp_date.txt from redcap"
export_project_from_redcap $MSK_ACCESS_DATA_HOME mskaccess_data_clinical_supp_date
if [ $? -gt 0 ] ; then
    EXPORT_SUPP_DATE_ACCESS_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS Redcap export of mskaccess_data_clinical_supp_date"
fi
```

**REDCAP | Export CVR Clinical REDCap projects**

*NOTES:*

```
# IF WE CANCEL ANY IMPORT, LET REDCAP GET AHEAD OF CURRENCY, BUT DON'T LET THE REPOSITORY HEAD ADVANCE [REVERT]
```

*MSKIMPACT*

```
printTimeStampedDataProcessingStepMessage "export of cvr clinical files from redcap"
echo "exporting impact data_clinical.txt from redcap"
export_project_from_redcap $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_cvr
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_IMPACT=1
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap export of mskimpact_data_clinical_cvr"
fi
```

*HEMEPACT*

```
echo "exporting heme data_clinical.txt from redcap"
export_project_from_redcap $MSK_HEMEPACT_DATA_HOME hemepact_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_HEME=1
    sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap export of hemepact_data_clinical_cvr"
fi
```

*RAINDANCE*

```
echo "exporting raindance data_clinical.txt from redcap"
export_project_from_redcap $MSK_RAINDANCE_DATA_HOME mskraindance_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_RAINDANCE=1
    sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap export of mskraindance_data_clinical_cvr"
fi
```

*ARCHER*

```
echo "exporting archer data_clinical.txt from redcap"
export_project_from_redcap $MSK_ARCHER_UNFILTERED_DATA_HOME mskarcher_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_ARCHER=1
    sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER Redcap export of mskarcher_data_clinical_cvr"
fi
```

*ACCESS*

```
echo "exporting access data_clinical.txt from redcap"
export_project_from_redcap $MSK_ACCESS_DATA_HOME mskaccess_data_clinical
if [ $? -gt 0 ] ; then
    IMPORT_STATUS_ACCESS=1
    sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS Redcap export of mskaccess_data_clinical_cvr"
fi
```

*NOTES: DMP repository is not tracking any changes at this point but the exported REDCap files for the CVR clinical data and Supp Date Added data should be present in their respective study directories*

**********************************************************************

***[GITHUB | DMP Repository State: 0](#github-dmp-repository-state-0)***

*Untracked files*
* Supp Date Added files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

**********************************************************************

## 2. MSKIMPACT Updates
**********************************************************************
*NOTES:*

```
# -----------------------------------------------------------------------------------------------------------
# MSKIMPACT DATA FETCHES
# TODO: move other pre-import/data-fetch steps here (i.e exporting raw files from redcap)
```

**DARWIN CAISIS GBM CLINICAL AND TIMELINE FETCH**

```
printTimeStampedDataProcessingStepMessage "MSKIMPACT data processing"
# fetch darwin caisis data
printTimeStampedDataProcessingStepMessage "Darwin CAISIS fetch for mskimpact"
$JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -s mskimpact -d $MSK_IMPACT_DATA_HOME -c
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Darwin CAISIS Fetch"
else
    FETCH_DARWIN_CAISIS_FAIL=0
    echo "committing darwin caisis data"
    cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Darwin CAISIS"
fi
```

> $JAVA_BINARY $JAVA_DARWIN_FETCHER_ARGS -s mskimpact -d $MSK_IMPACT_DATA_HOME -c

**********************************************************************

***[GITHUB | DMP Repository State: 1](#github-dmp-repository-state-1)***

*Untracked files*
* CAISIS Clinical and Timeline files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_supp_caisis_gbm.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_imaging_caisis_gbm.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_specimen_caisis_gbm.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_status_caisis_gbm.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_surgery_caisis_gbm.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_treatment_caisis_gbm.txt`
* Supp Date Added files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*
> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 1](#github-dmp-repository-state-1) are not affected by `reset`**

*SUCCESS*
> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Darwin CAISIS"

**--> Untracked files from [GITHUB | DMP Repository State: 1](#github-dmp-repository-state-1) in `$MSK_IMPACT_DATA_HOME` and CAISIS clinical and timeline output files are tracked and added during this commit.**

**********************************************************************

**CVR CLINICAL AND GENOMIC FETCH**

```
if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    # fetch new/updated IMPACT samples using CVR Web service   (must come after git fetching)
    printTimeStampedDataProcessingStepMessage "CVR fetch for mskimpact"
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -i mskimpact -r 150 $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR fetch failed!"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Fetch"
        IMPORT_STATUS_IMPACT=1
    else
        # sanity check for empty allele counts
        bash $PORTAL_HOME/scripts/test_if_impact_has_lost_allele_count.sh
        if [ $? -gt 0 ] ; then
            echo "Empty allele count sanity check failed! MSK-IMPACT will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT empty allele count sanity check"
            IMPORT_STATUS_IMPACT=1
        else
            # check for PHI
            $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_data.json
            if [ $? -gt 0 ] ; then
                echo "PHI attributes found in $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_data.json! MSK-IMPACT will not be imported!"
                cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
                sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT PHI attributes scan failed on $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_data.json"
                IMPORT_STATUS_IMPACT=1
            else
                FETCH_CVR_IMPACT_FAIL=0
                echo "committing cvr data"
                cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: CVR"
            fi
            # identify samples that need to be requeued or removed from data set due to CVR Part A or Part C consent status changes
            $PYTHON_BINARY $PORTAL_HOME/scripts/cvr_consent_status_checker.py -c $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt -m $MSK_IMPACT_DATA_HOME/data_mutations_extended.txt
        fi
    fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -i mskimpact -r 150 $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 2](#github-dmp-repository-state-2)***

*Modified files*
* CVR clinical and genomic (somatic) files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt`
    * `$MSK_IMPACT_PRIVATE_DATA_HOME/cvr_data.json`
    * `$MSK_IMPACT_DATA_HOME/data_CNA.txt`
    * `$MSK_IMPACT_DATA_HOME/data_sv.txt`
    * `$MSK_IMPACT_DATA_HOME/data_gene_matrix.txt`
    * `$MSK_IMPACT_DATA_HOME/data_mutations_extended.txt`
    * `$MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt`
    * `$MSK_IMPACT_DATA_HOME/mskimpact_data_cna_hg19.seg`
    * `$MSK_IMPACT_DATA_HOME/cvr/seq_date.txt`
    * `$MSK_IMPACT_DATA_HOME/cvr/zero_variant_whitelist.txt`

*Untracked files*
* Supp Date Added files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 2](#github-dmp-repository-state-2) are not affected by `reset`. Changes to modified files in `$MSK_IMPACT_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_IMPACT_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_IMPACT_DATA_HOME` with "FAILURE" step above.

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: CVR"

**--> Modified files from [GITHUB | DMP Repository State: 2](#github-dmp-repository-state-2) in `$MSK_IMPACT_DATA_HOME` are added to this commit.**

**********************************************************************

**CVR GERMLINE GENOMIC FETCH**

```
    # fetch new/updated IMPACT germline samples using CVR Web service   (must come after normal cvr fetching)
    printTimeStampedDataProcessingStepMessage "CVR germline fetch for mskimpact"
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -g -i mskimpact $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Germline fetch failed!"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Germline Fetch"
        IMPORT_STATUS_IMPACT=1
        #override the success of the tumor sample cvr fetch with a failed status
        FETCH_CVR_IMPACT_FAIL=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_gml_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_gml_data.json! MSK-IMPACT will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT PHI attributes scan failed on $MSK_IMPACT_PRIVATE_DATA_HOME/cvr_gml_data.json"
            IMPORT_STATUS_IMPACT=1
            #override the success of the tumor sample cvr fetch with a failed status
            FETCH_CVR_IMPACT_FAIL=1
        else
            echo "committing CVR germline data"
            cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: CVR Germline"
        fi
    fi
fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_IMPACT_DATA_HOME -n data_clinical_mskimpact_data_clinical_cvr.txt -g -i mskimpact $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 3](#github-dmp-repository-state-3)***

*Modified files*
* CVR clinical and genomic (germline) files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt`
    * `$MSK_IMPACT_DATA_HOME/cvr_gml_data.json`
    * `$MSK_IMPACT_DATA_HOME/data_sv.txt`
    * `$MSK_IMPACT_DATA_HOME/data_mutations_extended.txt`

*Untracked files*
* Supp Date Added files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 3](#github-dmp-repository-state-3) are not affected by `reset`. Changes to modified files in `$MSK_IMPACT_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_IMPACT_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_IMPACT_DATA_HOME` with "FAILURE" step above.

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: CVR Germline"

**--> Modified files from [GITHUB | DMP Repository State: 3](#github-dmp-repository-state-3) in `$MSK_IMPACT_DATA_HOME` are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH**

```
# fetch ddp demographics data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for mskimpact"
mskimpact_dmp_pids_file=$MSK_DMP_TMPDIR/mskimpact_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | sort | uniq > $mskimpact_dmp_pids_file
MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $MSKIMPACT_REDCAP_BACKUP/data_clinical_mskimpact_data_clinical_ddp_demographics.txt)
if [ $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact -p $mskimpact_dmp_pids_file -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_IMPACT_DATA_HOME -r $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT DDP Demographics Fetch"
else
    FETCH_DDP_IMPACT_FAIL=0
    echo "committing ddp data"
    cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: DDP Demographics"
fi
```

> $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact -p $mskimpact_dmp_pids_file -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_IMPACT_DATA_HOME -r $MSKIMPACT_DDP_DEMOGRAPHICS_RECORD_COUNT

**********************************************************************

***[GITHUB | DMP Repository State: 4](#github-dmp-repository-state-4)***

*Modified files*
* DDP Demographics files:
    * `$MSK_IMPACT_DATA_HOME/ddp/ddp_vital_status.txt`
    * `$MSK_IMPACT_DATA_HOME/ddp/ddp_naaccr.txt`

*Untracked files*
* DDP Demographics files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_ddp.txt`
* Supp Date Added files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 4](#github-dmp-repository-state-4) are not affected by `reset`. Changes to modified files in `$MSK_IMPACT_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: DDP Demographics"

**--> Modified files and untracked from [GITHUB | DMP Repository State: 4](#github-dmp-repository-state-4) in `$MSK_IMPACT_DATA_HOME`  are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH FOR PEDIATRIC PATIENTS**

```
# fetch ddp data for pediatric cohort
if [ $FETCH_DDP_IMPACT_FAIL -eq 0 ] ; then
    awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PED_IND"] == "Yes") { print $(f["PATIENT_ID"]) } }' $MSK_IMPACT_DATA_HOME/data_clinical_ddp.txt | sort | uniq > $MSK_DMP_TMPDIR/mskimpact_ped_patient_list.txt
    # override default ddp clinical filename so that pediatric demographics file does not conflict with mskimpact demographics file
    DDP_PEDIATRIC_PROP_OVERRIDES="-Dddp.clinical_filename=data_clinical_ddp_pediatrics.txt"
    $JAVA_BINARY $DDP_PEDIATRIC_PROP_OVERRIDES $JAVA_DDP_FETCHER_ARGS -o $MSK_IMPACT_DATA_HOME -p $MSK_DMP_TMPDIR/mskimpact_ped_patient_list.txt -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f diagnosis,radiation,chemotherapy,surgery,survival
    if [ $? -gt 0 ] ; then
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Pediatric DDP Fetch"
    else
        FETCH_DDP_IMPACT_FAIL=0
        echo "committing Pediatric DDP data"
        cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Pediatric DDP demographics/timeline"
    fi
fi
```

> $JAVA_BINARY $DDP_PEDIATRIC_PROP_OVERRIDES $JAVA_DDP_FETCHER_ARGS -o $MSK_IMPACT_DATA_HOME -p $MSK_DMP_TMPDIR/mskimpact_ped_patient_list.txt -s $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt -f diagnosis,radiation,chemotherapy,surgery,survival

**********************************************************************

***[GITHUB | DMP Repository State: 5](#github-dmp-repository-state-5)***

*Untracked files*
* Pediatric DDP Demographics files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_ddp_pediatrics.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_radiation.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_surgery.txt`
* DDP Demographics files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_ddp.txt`
* Supp Date Added files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 5](#github-dmp-repository-state-5) are not affected by `reset`.**

*SUCCESS:*

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Pediatric DDP demographics/timeline"

**--> Untracked files from [GITHUB | DMP Repository State: 5](#github-dmp-repository-state-5) in `$MSK_IMPACT_DATA_HOME` are added to this commit.**

**********************************************************************

**CRDB FETCH**

*NOTES: There is no need to commit changes to the data repository since CRDB generated files are stored directly in REDCap*

```
if [ $PERFORM_CRDB_FETCH -gt 0 ] ; then
    # fetch CRDB data
    printTimeStampedDataProcessingStepMessage "CRDB fetch for mskimpact"
    $JAVA_BINARY $JAVA_CRDB_FETCHER_ARGS --directory $MSK_IMPACT_DATA_HOME
    # no need for data repository update/commit ; CRDB generated files are stored in redcap and not git
    if [ $? -gt 0 ] ; then
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT CRDB Fetch"
    else
        FETCH_CRDB_IMPACT_FAIL=0
    fi
else
    FETCH_CRDB_IMPACT_FAIL=0
fi
```

## 3. HEMEPACT Updates
**********************************************************************

**CVR CLINICAL AND GENOMIC FETCH**

```
# -----------------------------------------------------------------------------------------------------------
# HEMEPACT DATA FETCHES
printTimeStampedDataProcessingStepMessage "HEMEPACT data processing"

if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    # fetch new/updated heme samples using CVR Web service (must come after git fetching). Threshold is set to 50 since heme contains only 190 samples (07/12/2017)
    printTimeStampedDataProcessingStepMessage "CVR fetch for hemepact"
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_HEMEPACT_DATA_HOME -n data_clinical_hemepact_data_clinical.txt -i mskimpact_heme -r 50 $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR heme fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT CVR Fetch"
        IMPORT_STATUS_HEME=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_HEMEPACT_PRIVATE_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_HEMEPACT_PRIVATE_DATA_HOME/cvr_data.json! HEMEPACT will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT PHI attributes scan failed on $MSK_HEMEPACT_PRIVATE_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_HEME=1
        else
            FETCH_CVR_HEME_FAIL=0
            echo "committing cvr data for heme"
            cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest HEMEPACT dataset"
        fi
    fi
fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_HEMEPACT_DATA_HOME -n data_clinical_hemepact_data_clinical.txt -i mskimpact_heme -r 50 $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 6](#github-dmp-repository-state-6)***

*Modified files*
* CVR clinical and genomic (somatic) files:
    * `$MSK_HEMEPACT_PRIVATE_DATA_HOME/cvr_data.json`
    * `$MSK_HEMEPACT_DATA_HOME/data_CNA.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_sv.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_gene_matrix.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_mutations_extended.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_nonsignedout_mutations.txt`
    * `$MSK_HEMEPACT_DATA_HOME/mskimpact_heme_data_cna_hg19.seg`
    * `$MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt`
    * `$MSK_HEMEPACT_DATA_HOME/cvr/zero_variant_whitelist.txt`

*Untracked files*
* Supp Date Added files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 6](#github-dmp-repository-state-6) are not affected by `reset`. Changes to modified files in `$MSK_HEMEPACT_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_HEMEPACT_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_HEMEPACT_DATA_HOME` with "FAILURE" step above.

> cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest HEMEPACT dataset"

**--> Modified files and untracked from [GITHUB | DMP Repository State: 6](#github-dmp-repository-state-6) in `$MSK_HEMEPACT_DATA_HOME` are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH**

```
# fetch ddp demographics data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for hemepact"
mskimpact_heme_dmp_pids_file=$MSK_DMP_TMPDIR/mskimpact_heme_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt | sort | uniq > $mskimpact_heme_dmp_pids_file
HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $HEMEPACT_REDCAP_BACKUP/data_clinical_hemepact_data_clinical_ddp_demographics.txt)
if [ $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact_heme -p $mskimpact_heme_dmp_pids_file -s $MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_HEMEPACT_DATA_HOME -r $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT DDP Demographics Fetch"
else
    FETCH_DDP_HEME_FAIL=0
    echo "committing ddp data"
    cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: DDP Demographics"
fi
```

> $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskimpact_heme -p $mskimpact_heme_dmp_pids_file -s $MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_HEMEPACT_DATA_HOME -r $HEMEPACT_DDP_DEMOGRAPHICS_RECORD_COUNT

**********************************************************************

***[GITHUB | DMP Repository State: 7](#github-dmp-repository-state-7)***

*Untracked files*
* DDP Demographics files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_ddp.txt`
* Supp Date Added files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 7](#github-dmp-repository-state-7) are not affected by `reset`.**

*SUCCESS:*

> cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: DDP Demographics"

**--> Untracked files from [GITHUB | DMP Repository State: 7](#github-dmp-repository-state-7) in `$MSK_HEMEPACT_DATA_HOME` are added to this commit.**

**********************************************************************

## 4. RAINDANCE Updates
**********************************************************************

**CVR CLINICAL AND GENOMIC FETCH**

```
# -----------------------------------------------------------------------------------------------------------
# RAINDANCE DATA FETCHES
printTimeStampedDataProcessingStepMessage "RAINDANCE data processing"

if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    # fetch new/updated raindance samples using CVR Web service (must come after git fetching). The -s flag skips segment data fetching
    printTimeStampedDataProcessingStepMessage "CVR fetch for raindance"
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_RAINDANCE_DATA_HOME -n data_clinical_mskraindance_data_clinical.txt -s -i mskraindance $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR raindance fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE CVR Fetch"
        IMPORT_STATUS_RAINDANCE=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_RAINDANCE_PRIVATE_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_RAINDANCE_PRIVATE_DATA_HOME/cvr_data.json! RAINDANCE will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE PHI attributes scan failed on $MSK_RAINDANCE_PRIVATE_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_RAINDANCE=1
        else
            FETCH_CVR_RAINDANCE_FAIL=0
            cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest RAINDANCE dataset"
        fi
    fi
fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_RAINDANCE_DATA_HOME -n data_clinical_mskraindance_data_clinical.txt -s -i mskraindance $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 8](#github-dmp-repository-state-8)***

*Modified files*
* CVR clinical and genomic (somatic) files:
    * `$MSK_RAINDANCE_PRIVATE_DATA_HOME/cvr_data.json`
    * `$MSK_RAINDANCE_DATA_HOME/data_gene_matrix.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_mutations_extended.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_nonsignedout_mutations.txt`
    * `$MSK_RAINDANCE_DATA_HOME/cvr/seq_date.txt`
    * `$MSK_RAINDANCE_DATA_HOME/cvr/zero_variant_whitelist.txt`

*Untracked files*
* Supp Date Added files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 8](#github-dmp-repository-state-8) are not affected by `reset`. Changes to modified files in `$MSK_RAINDANCE_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_RAINDANCE_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_RAINDANCE_DATA_HOME` with "FAILURE" step above.

> cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest RAINDANCE dataset"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 8](#github-dmp-repository-state-8) in `$MSK_RAINDANCE_DATA_HOME` are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH**

```
# fetch ddp demographics data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for raindance"
mskraindance_dmp_pids_file=$MSK_DMP_TMPDIR/mskraindance_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt | sort | uniq > $mskraindance_dmp_pids_file
RAINDANCE_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $RAINDANCE_REDCAP_BACKUP/data_clinical_mskraindance_data_clinical_ddp_demographics.txt)
if [ $RAINDANCE_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    RAINDANCE_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskraindance -p $mskraindance_dmp_pids_file -s $MSK_RAINDANCE_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_RAINDANCE_DATA_HOME -r $RAINDANCE_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE DDP Demographics Fetch"
else
    FETCH_DDP_RAINDANCE_FAIL=0
    echo "committing ddp data"
    cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: DDP Demographics"
fi
```

> $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskraindance -p $mskraindance_dmp_pids_file -s $MSK_RAINDANCE_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_RAINDANCE_DATA_HOME -r $RAINDANCE_DDP_DEMOGRAPHICS_RECORD_COUNT

**********************************************************************

***[GITHUB | DMP Repository State: 9](#github-dmp-repository-state-9)***

*Untracked files*
* DDP Demographics files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_ddp.txt`
* Supp Date Added files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 9](#github-dmp-repository-state-9) are not affected by `reset`.**

*SUCCESS:*

> cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: DDP Demographics"

**--> Untracked files from [GITHUB | DMP Repository State: 9](#github-dmp-repository-state-9) in `$MSK_RAINDANCE_DATA_HOME` are added to this commit.**

**********************************************************************

## 4. ARCHER Updates
**********************************************************************

**CVR CLINICAL AND GENOMIC FETCH**

```
# -----------------------------------------------------------------------------------------------------------
# ARCHER DATA FETCHES
printTimeStampedDataProcessingStepMessage "ARCHER data processing"

if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    # fetch new/updated archer samples using CVR Web service (must come after git fetching).
    printTimeStampedDataProcessingStepMessage "CVR fetch for archer"
    # archer has -b option to block warnings for samples with zero variants (all samples will have zero variants)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_ARCHER_UNFILTERED_DATA_HOME -n data_clinical_mskarcher_data_clinical.txt -i mskarcher -s -b $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR Archer fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED CVR Fetch"
        IMPORT_STATUS_ARCHER=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_ARCHER_UNFILTERED_PRIVATE_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_ARCHER_UNFILTERED_PRIVATE_DATA_HOME/cvr_data.json! UNLINKED_ARCHER will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER PHI attributes scan failed on $MSK_ARCHER_UNFILTERED_PRIVATE_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_ARCHER=1
        else
            FETCH_CVR_ARCHER_FAIL=0
            cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED dataset"
        fi
    fi
fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_ARCHER_UNFILTERED_DATA_HOME -n data_clinical_mskarcher_data_clinical.txt -i mskarcher -s -b $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 10](#github-dmp-repository-state-10)***

*Modified files*
* CVR clinical and genomic (somatic) files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr_data.json`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_sv.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_gene_matrix.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/seq_date.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt`

*Untracked files*

* Supp Date Added files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 10](#github-dmp-repository-state-10) are not affected by `reset`. Changes to modified files in `$MSK_ARCHER_UNFILTERED_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_ARCHER_UNFILTERED_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_ARCHER_UNFILTERED_DATA_HOME` with "FAILURE" step above.

> cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED dataset"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 10](#github-dmp-repository-state-10) in `$MSK_ARCHER_UNFILTERED_DATA_HOME` are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH**

```
# fetch ddp demographics data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for archer"
mskarcher_dmp_pids_file=$MSK_DMP_TMPDIR/mskarcher_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt | sort | uniq > $mskarcher_dmp_pids_file
ARCHER_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $ARCHER_REDCAP_BACKUP/data_clinical_mskarcher_data_clinical_ddp_demographics.txt)
if [ $ARCHER_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    ARCHER_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskarcher -p $mskarcher_dmp_pids_file -s $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_ARCHER_UNFILTERED_DATA_HOME -r $ARCHER_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED DDP Demographics Fetch"
else
    FETCH_DDP_ARCHER_FAIL=0
    echo "committing ddp data"
    cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: DDP Demographics"
fi
```

> $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskarcher -p $mskarcher_dmp_pids_file -s $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_ARCHER_UNFILTERED_DATA_HOME -r $ARCHER_DDP_DEMOGRAPHICS_RECORD_COUNT


**********************************************************************

***[GITHUB | DMP Repository State: 11](#github-dmp-repository-state-11)***

*Untracked files*
* DDP Demographics files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_ddp.txt`
* Supp Date Added files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 11](#github-dmp-repository-state-11) are not affected by `reset`.**

*SUCCESS:*

> cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: DDP Demographics"

**--> Untracked files from [GITHUB | DMP Repository State: 11](#github-dmp-repository-state-11) in `$MSK_ARCHER_UNFILTERED_DATA_HOME` are added to this commit.**

**********************************************************************

## 5. ACCESS Updates
**********************************************************************

**CVR CLINICAL AND GENOMIC FETCH**

```
# -----------------------------------------------------------------------------------------------------------
# ACCESS DATA FETCHES
printTimeStampedDataProcessingStepMessage "ACCESS data processing"

if [ $IMPORT_STATUS_ACCESS -eq 0 ] ; then
    # fetch new/updated access samples using CVR Web service (must come after git fetching).
    printTimeStampedDataProcessingStepMessage "CVR fetch for access"
    # access has -b option to block warnings for samples with zero variants (all samples will have zero variants)
    $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_ACCESS_DATA_HOME -n data_clinical_mskaccess_data_clinical.txt -i mskaccess -s -b $CVR_TEST_MODE_ARGS
    if [ $? -gt 0 ] ; then
        echo "CVR ACCESS fetch failed!"
        echo "This will not affect importing of mskimpact"
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS CVR Fetch"
        IMPORT_STATUS_ACCESS=1
    else
        # check for PHI
        $PYTHON_BINARY $PORTAL_HOME/scripts/phi-scanner.py -a $PIPELINES_CONFIG_HOME/properties/fetch-cvr/phi-scanner-attributes.txt -j $MSK_ACCESS_PRIVATE_DATA_HOME/cvr_data.json
        if [ $? -gt 0 ] ; then
            echo "PHI attributes found in $MSK_ACCESS_PRIVATE_DATA_HOME/cvr_data.json! ACCESS will not be imported!"
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS PHI attributes scan failed on $MSK_ACCESS_PRIVATE_DATA_HOME/cvr_data.json"
            IMPORT_STATUS_ACCESS=1
        else
            FETCH_CVR_ACCESS_FAIL=0
            cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ACCESS dataset"
        fi
    fi
fi
```

> $JAVA_BINARY $JAVA_CVR_FETCHER_ARGS -d $MSK_ACCESS_DATA_HOME -n data_clinical_mskaccess_data_clinical.txt -i mskaccess -s -b $CVR_TEST_MODE_ARGS

**********************************************************************

***[GITHUB | DMP Repository State: 12](#github-dmp-repository-state-12)***

*Modified files*
* CVR clinical and genomic (somatic) files:
    * `$MSK_ACCESS_PRIVATE_DATA_HOME/cvr_data.json`
    * `$MSK_ACCESS_DATA_HOME/data_CNA.txt`
    * `$MSK_ACCESS_DATA_HOME/data_sv.txt`
    * `$MSK_ACCESS_DATA_HOME/data_gene_matrix.txt`
    * `$MSK_ACCESS_DATA_HOME/data_mutations_extended.txt`
    * `$MSK_ACCESS_DATA_HOME/data_nonsignedout_mutations.txt`
    * `$MSK_ACCESS_DATA_HOME/cvr/seq_date.txt`
    * `$MSK_ACCESS_DATA_HOME/cvr/zero_var_whitelist.txt`

*Untracked files*

* Supp Date Added files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`
* CVR Clinical files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 12](#github-dmp-repository-state-12) are not affected by `reset`. Changes to modified files in `$MSK_ACCESS_DATA_HOME` are unstaged.**

*SUCCESS:*

`$MSK_ACCESS_DATA_HOME` is scanned for dropped allele counts and PHI. If either of these checks fail then script will discard changes to tracked files in `$MSK_ACCESS_DATA_HOME` with "FAILURE" step above.

> cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ACCESS dataset"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 12](#github-dmp-repository-state-12) in `$MSK_ACCESS_DATA_HOME` are added to this commit.**

**********************************************************************

**DDP DEMOGRAPHICS DATA FETCH**

```
# fetch ddp demographics data
printTimeStampedDataProcessingStepMessage "DDP demographics fetch for acess"
mskaccess_dmp_pids_file=$MSK_DMP_TMPDIR/mskaccess_patient_list.txt
awk -F'\t' 'NR==1 { for (i=1; i<=NF; i++) { f[$i] = i } }{ if ($f["PATIENT_ID"] != "PATIENT_ID") { print $(f["PATIENT_ID"]) } }' $MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt | sort | uniq > $mskaccess_dmp_pids_file
ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$(wc -l < $ACCESS_REDCAP_BACKUP/data_clinical_mskaccess_data_clinical_ddp_demographics.txt)
if [ $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT -le $DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT ] ; then
    ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT=$DEFAULT_DDP_DEMOGRAPHICS_ROW_COUNT
fi

$JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskaccess -p $mskaccess_dmp_pids_file -s $MSK_ACCESS_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_ACCESS_DATA_HOME -r $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT
if [ $? -gt 0 ] ; then
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS DDP Demographics Fetch"
else
    FETCH_DDP_ACCESS_FAIL=0
    echo "committing ddp data"
    cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ACCESS Dataset: DDP Demographics"
fi
```

> $JAVA_BINARY $JAVA_DDP_FETCHER_ARGS -c mskaccess -p $mskaccess_dmp_pids_file -s $MSK_ACCESS_DATA_HOME/cvr/seq_date.txt -f survival -o $MSK_ACCESS_DATA_HOME -r $ACCESS_DDP_DEMOGRAPHICS_RECORD_COUNT

**********************************************************************

***[GITHUB | DMP Repository State: 13](#github-dmp-repository-state-13)***

*Untracked files*
* DDP Demographics files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_ddp.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from [GITHUB | DMP Repository State: 13](#github-dmp-repository-state-13) are not affected by `reset`.**

*SUCCESS:*

> cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add ./* ; $GIT_BINARY commit -m "Latest ACCESS Dataset: DDP Demographics"

**--> Untracked files from [GITHUB | DMP Repository State: 13](#github-dmp-repository-state-13) in `$MSK_ACCESS_DATA_HOME` are added to this commit.**

**********************************************************************

## 6. Cancer Type Case Lists and Supp Date Added Clinical Updates
**********************************************************************
*NOTES:*

```
# -----------------------------------------------------------------------------------------------------------
# GENERATE CANCER TYPE CASE LISTS AND SUPP DATE ADDED FILES
# NOTE: Even though cancer type case lists are not needed for MSKIMPACT, HEMEPACT for the portal
# since they are imported as part of MSKSOLIDHEME - the LYMPHOMASUPERCOHORT subsets these source
# studies by CANCER_TYPE and ONCOTREE_CODE so we want to keep these fields up-to-date which is
# accomplished by running the 'addCancerTypeCaseLists' function
```

**Cancer Type Case Lists and Supp Date Added Clinical Updates**

*MSKIMPACT*

```
# add "DATE ADDED" info to clinical data for MSK-IMPACT
if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact" "data_clinical_mskimpact_data_clinical_cvr.txt"
    cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_IMPACT_DATA_HOME "data_clinical_mskimpact_data_clinical_cvr.txt" "data_clinical_mskimpact_supp_date_cbioportal_added.txt"
        cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add data_clinical_mskimpact_supp_date_cbioportal_added.txt ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: SUPP DATE ADDED"
    fi
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
fi
```

> addCancerTypeCaseLists $MSK_IMPACT_DATA_HOME "mskimpact" "data_clinical_mskimpact_data_clinical_cvr.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 14](#github-dmp-repository-state-14)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_IMPACT_DATA_HOME/case_lists`

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 14](#github-dmp-repository-state-14) in `$MSK_IMPACT_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> addDateAddedData $MSK_IMPACT_DATA_HOME "data_clinical_mskimpact_data_clinical_cvr.txt" "data_clinical_mskimpact_supp_date_cbioportal_added.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 15](#github-dmp-repository-state-15)***

*Modified files*
* Supp Date Added file
    * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt`

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add data_clinical_mskimpact_supp_date_cbioportal_added.txt ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: SUPP DATE ADDED"

**--> Modified files from [GITHUB | DMP Repository State: 15](#github-dmp-repository-state-15) in `$MSK_IMPACT_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from  [GITHUB | DMP Repository State: 14](#github-dmp-repository-state-14) and  [GITHUB | DMP Repository State: 15](#github-dmp-repository-state-15) are not affected by `reset`. Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**

*HEMEPACT*

```
# add "DATE ADDED" info to clinical data for HEMEPACT
if [ $IMPORT_STATUS_HEME -eq 0 ] && [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_HEMEPACT_DATA_HOME "mskimpact_heme" "data_clinical_hemepact_data_clinical.txt"
    cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_HEME_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_HEMEPACT_DATA_HOME "data_clinical_hemepact_data_clinical.txt" "data_clinical_hemepact_data_clinical_supp_date.txt"
        cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add data_clinical_hemepact_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: SUPP DATE ADDED"
    fi
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
fi
```

> addCancerTypeCaseLists $MSK_HEMEPACT_DATA_HOME "mskimpact_heme" "data_clinical_hemepact_data_clinical.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 16](#github-dmp-repository-state-16)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_HEMEPACT_DATA_HOME/case_lists`

> cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 16](#github-dmp-repository-state-16) in `$MSK_HEMEPACT_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> addDateAddedData $MSK_HEMEPACT_DATA_HOME "data_clinical_hemepact_data_clinical.txt" "data_clinical_hemepact_data_clinical_supp_date.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 17](#github-dmp-repository-state-17)***

*Modified files*
* Supp Date Added file
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`

> cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add data_clinical_hemepact_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: SUPP DATE ADDED"

**--> Modified files from [GITHUB | DMP Repository State: 17](#github-dmp-repository-state-17) in `$MSK_HEMEPACT_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from  [GITHUB | DMP Repository State: 16](#github-dmp-repository-state-16) and  [GITHUB | DMP Repository State: 17](#github-dmp-repository-state-17) are not affected by `reset`. Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**

*ARCHER*

```
# add "DATE ADDED" info to clinical data for ARCHER
if [[ $IMPORT_STATUS_ARCHER -eq 0 && $FETCH_CVR_ARCHER_FAIL -eq 0 ]] ; then
    addCancerTypeCaseLists $MSK_ARCHER_UNFILTERED_DATA_HOME "mskarcher" "data_clinical_mskarcher_data_clinical.txt"
    cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_ARCHER_UNFILTERED_DATA_HOME "data_clinical_mskarcher_data_clinical.txt" "data_clinical_mskarcher_data_clinical_supp_date.txt"
        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add data_clinical_mskarcher_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: SUPP DATE ADDED"
    fi
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
fi
```

> addCancerTypeCaseLists $MSK_ARCHER_UNFILTERED_DATA_HOME "mskarcher" "data_clinical_mskarcher_data_clinical.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 18](#github-dmp-repository-state-18)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/case_lists`

> cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 18](#github-dmp-repository-state-18) in `$MSK_ARCHER_UNFILTERED_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> addDateAddedData $MSK_ARCHER_UNFILTERED_DATA_HOME "data_clinical_mskarcher_data_clinical.txt" "data_clinical_mskarcher_data_clinical_supp_date.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 19](#github-dmp-repository-state-19)***

*Modified files*
* Supp Date Added file
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`

> cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add data_clinical_mskarcher_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: SUPP DATE ADDED"

**--> Modified files from [GITHUB | DMP Repository State: 19](#github-dmp-repository-state-19) in `$MSK_ARCHER_UNFILTERED_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from  [GITHUB | DMP Repository State: 18](#github-dmp-repository-state-18) and  [GITHUB | DMP Repository State: 19](#github-dmp-repository-state-19) are not affected by `reset`. Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**


*RAINDANCE*

```
# generate case lists by cancer type and add "DATE ADDED" info to clinical data for RAINDANCE
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] && [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_RAINDANCE_DATA_HOME "mskraindance" "data_clinical_mskraindance_data_clinical.txt"
    cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_RAINDANCE_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_RAINDANCE_DATA_HOME "data_clinical_mskraindance_data_clinical.txt" "data_clinical_mskraindance_data_clinical_supp_date.txt"
        cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add data_clinical_mskraindance_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: SUPP DATE ADDED"
    fi
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
fi
```

> addCancerTypeCaseLists $MSK_RAINDANCE_DATA_HOME "mskraindance" "data_clinical_mskraindance_data_clinical.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 20](#github-dmp-repository-state-20)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_RAINDANCE_DATA_HOME/case_lists`

> cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 20](#github-dmp-repository-state-20) in `$MSK_RAINDANCE_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> addDateAddedData $MSK_RAINDANCE_DATA_HOME "data_clinical_mskraindance_data_clinical.txt" "data_clinical_mskraindance_data_clinical_supp_date.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 21](#github-dmp-repository-state-21)***

*Modified files*
* Supp Date Added file
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`

> cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add data_clinical_mskraindance_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: SUPP DATE ADDED"

**--> Modified files from [GITHUB | DMP Repository State: 21](#github-dmp-repository-state-21) in `$MSK_RAINDANCE_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from  [GITHUB | DMP Repository State: 20](#github-dmp-repository-state-20) and  [GITHUB | DMP Repository State: 21](#github-dmp-repository-state-21) are not affected by `reset`. Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**


*ACCESS*

```
# generate case lists by cancer type and add "DATE ADDED" info to clinical data for ACCESS
if [ $IMPORT_STATUS_ACCESS -eq 0 ] && [ $FETCH_CVR_ACCESS_FAIL -eq 0 ] ; then
    addCancerTypeCaseLists $MSK_ACCESS_DATA_HOME "mskaccess" "data_clinical_mskaccess_data_clinical.txt"
    cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest ACCESS Dataset: Case Lists"
    if [ $EXPORT_SUPP_DATE_ACCESS_FAIL -eq 0 ] ; then
        addDateAddedData $MSK_ACCESS_DATA_HOME "data_clinical_mskaccess_data_clinical.txt" "data_clinical_mskaccess_data_clinical_supp_date.txt"
        cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add data_clinical_mskaccess_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest ACCESS Dataset: SUPP DATE ADDED"
    fi
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
fi
```

> addCancerTypeCaseLists $MSK_ACCESS_DATA_HOME "mskaccess" "data_clinical_mskaccess_data_clinical.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 22](#github-dmp-repository-state-22)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_ACCESS_DATA_HOME/case_lists`

> cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest ACCESS Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 22](#github-dmp-repository-state-22) in `$MSK_ACCESS_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> addDateAddedData $MSK_ACCESS_DATA_HOME "data_clinical_mskaccess_data_clinical.txt" "data_clinical_mskaccess_data_clinical_supp_date.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 23](#github-dmp-repository-state-23)***

*Modified files*
* Supp Date Added file
    * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`

> cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add data_clinical_mskaccess_data_clinical_supp_date.txt ; $GIT_BINARY commit -m "Latest ACCESS Dataset: SUPP DATE ADDED"

**--> Modified files from [GITHUB | DMP Repository State: 23](#github-dmp-repository-state-23) in `$MSK_ACCESS_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Untracked files from  [GITHUB | DMP Repository State: 22](#github-dmp-repository-state-22) and  [GITHUB | DMP Repository State: 23](#github-dmp-repository-state-23) are not affected by `reset`. Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**

## 7. Additional Processing Before Updating REDCap Projects
**********************************************************************
*NOTES:*

```
# -----------------------------------------------------------------------------------------------------------
# ADDITIONAL PROCESSING

# ARCHER structural variants into MSKIMPACT, HEMEPACT
# we maintain a file with the list of ARCHER samples to exclude from any merges/subsets involving
# ARCHER data to prevent duplicate structural variant events ($MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt)

MAPPED_ARCHER_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt
```

*MSKIMPACT-linked ARCHER structural variant events merge*

```
# add linked ARCHER_UNFILTERED structural variants to MSKIMPACT
if [ $IMPORT_STATUS_IMPACT -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] && [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    # Merge ARCHER_UNFILTERED structural variant data into the MSKIMPACT cohort
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge_archer_structural_variants.py --archer-structural-variants $MSK_ARCHER_UNFILTERED_DATA_HOME/data_sv.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --structural-variants-filename $MSK_IMPACT_DATA_HOME/data_sv.txt --clinical-filename $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt --mapped-archer-samples-filename $MAPPED_ARCHER_SAMPLES_FILE --study-id "mskimpact"
    if [ $? -gt 0 ] ; then
        ARCHER_MERGE_IMPACT_FAIL=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    else
        $GIT_BINARY add $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME ; $GIT_BINARY commit -m "Adding ARCHER_UNFILTERED structural variants to MSKIMPACT"
    fi
fi
```

> $PYTHON_BINARY $PORTAL_HOME/scripts/merge_archer_structural_variants.py --archer-structuralr-_variants $MSK_ARCHER_UNFILTERED_DATA_HOME/data_sv.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --structuralr-_variants-filename $MSK_IMPACT_DATA_HOME/data_sv.txt --clinical-filename $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt --mapped-archer-samples-filename $MAPPED_ARCHER_SAMPLES_FILE --study-id "mskimpact"

**********************************************************************

***[GITHUB | DMP Repository State: 24](#github-dmp-repository-state-24)***

*Modified files*
* ARCHER-linked Structural Variant Event files:
    * `$MSK_IMPACT_DATA_HOME/data_sv.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt` (aka `$MAPPED_ARCHER_SAMPLES_FILE`)

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 24](#github-dmp-repository-state-24) are unstaged.**

*SUCCESS:*

> $GIT_BINARY add $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME ; $GIT_BINARY commit -m "Adding ARCHER_UNFILTERED structural variants to MSKIMPACT"

**--> Modified files from [GITHUB | DMP Repository State: 24](#github-dmp-repository-state-24) in `$DMP_DATA_HOME` are added to this commit.**

**********************************************************************

*HEMEPACT-linked ARCHER structural variant events merge*

```
# add linked ARCHER_UNFILTERED structural variants to HEMEPACT
if [ $IMPORT_STATUS_HEME -eq 0 ] && [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] && [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    # Merge ARCHER_UNFILTERED structural variant data into the HEMEPACT cohort
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge_archer_structural_variants.py --archer-structural-variants $MSK_ARCHER_UNFILTERED_DATA_HOME/data_sv.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --structural-variants-filename $MSK_HEMEPACT_DATA_HOME/data_sv.txt --clinical-filename $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt --mapped-archer-samples-filename $MAPPED_ARCHER_SAMPLES_FILE --study-id "mskimpact_heme"
    if [ $? -gt 0 ] ; then
        ARCHER_MERGE_HEME_FAIL=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    else
        $GIT_BINARY add $MAPPED_ARCHER_SAMPLES_FILE $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY commit -m "Adding ARCHER_UNFILTERED structural variants to HEMEPACT"
    fi
fi
```

> $PYTHON_BINARY $PORTAL_HOME/scripts/merge_archer_structural_variants.py --archer-structural-variants $MSK_ARCHER_UNFILTERED_DATA_HOME/data_sv.txt --linked-cases-filename $MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/linked_cases.txt --structural-variants-filename $MSK_HEMEPACT_DATA_HOME/data_sv.txt --clinical-filename $MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt --mapped-archer-samples-filename $MAPPED_ARCHER_SAMPLES_FILE --study-id "mskimpact_heme"

**********************************************************************

***[GITHUB | DMP Repository State: 25](#github-dmp-repository-state-25)***

*Modified files*
* ARCHER-linked Structural Variant Event files:
    * `$MSK_HEMEPACT_DATA_HOME/data_sv.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt` (aka `$MAPPED_ARCHER_SAMPLES_FILE`)

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 25](#github-dmp-repository-state-25) are unstaged.**

*SUCCESS:*

> $GIT_BINARY add $MAPPED_ARCHER_SAMPLES_FILE $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY commit -m "Adding ARCHER_UNFILTERED structural variants to HEMEPACT"

**--> Modified files from [GITHUB | DMP Repository State: 25](#github-dmp-repository-state-25) in `$DMP_DATA_HOME` are added to this commit.**

**********************************************************************

*Generate MSKIMPACT master list to use for filtering dropped samples and patients from supplemental clinical and timeline files.*

```
# create MSKIMPACT master sample list for filtering dropped samples/patients from supp files
# gets index of SAMPLE_ID from file (used in case SAMPLE_ID index changes)
SAMPLE_ID_COLUMN=`sed -n $"1s/\t/\\\n/gp" $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | grep -nx "SAMPLE_ID" | cut -d: -f1`
SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME="$MSK_DMP_TMPDIR/sample_masterlist_for_filtering.txt"
grep -v '^#' $MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt | awk -v SAMPLE_ID_INDEX="$SAMPLE_ID_COLUMN" -F '\t' '{if ($SAMPLE_ID_INDEX != "SAMPLE_ID") print $SAMPLE_ID_INDEX;}' > $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME
if [ $(wc -l < $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME) -eq 0 ] ; then
    echo "ERROR! Sample masterlist $SAMPLE_MASTER_LIST_FOR_FILTERING_FILENAME is empty. Skipping patient/sample filtering for mskimpact!"
    GENERATE_MASTERLIST_FAIL=1
fi
```

## 8. Import Projects Into REDCap
**********************************************************************
*NOTES:*

```
# -----------------------------------------------------------------------------------------------------------
# IMPORT PROJECTS INTO REDCAP

# import newly fetched files into redcap
printTimeStampedDataProcessingStepMessage "redcap import for all cohorts"
```

*MSKIMPACT*

* **CRDB**
    * only executes if `$PERFORM_CRDB_FETCH` enabled and `$FETCH_CRDB_IMPACT_FAIL=0`

```
## MSKIMPACT imports

# imports mskimpact crdb data into redcap
if [ $PERFORM_CRDB_FETCH -gt 0 ] && [ $FETCH_CRDB_IMPACT_FAIL -eq 0 ] ; then
    import_crdb_to_redcap
    if [ $? -gt 0 ] ; then
        #NOTE: we have decided to allow import of mskimpact project to proceed even when CRDB data has been lost from redcap (not setting IMPORT_STATUS_IMPACT)
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT CRDB Redcap Import - Recovery Of Redcap Project Needed!"
    fi
fi
```

* **DARWIN CAISIS**

```
# imports mskimpact darwin data into redcap
if [ $FETCH_DARWIN_CAISIS_FAIL -eq 0 ] ; then
    import_mskimpact_darwin_caisis_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Darwin CAISIS Redcap Import"
    fi
fi
```

* **DDP**

```
# imports mskimpact ddp data into redcap
if [ $FETCH_DDP_IMPACT_FAIL -eq 0 ] ; then
    import_mskimpact_ddp_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT DDP Redcap Import"
    fi
fi
```

* **CVR AND CLINICAL SUPP DATE ADDED**

```
# imports mskimpact cvr data into redcap
if [ $FETCH_CVR_IMPACT_FAIL -eq 0 ] ; then
    import_mskimpact_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_IMPACT_FAIL -eq 0 ] ; then
        import_mskimpact_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi
```

*HEMEPACT*

* **DDP**

```
## HEMEPACT imports

if [ $FETCH_DDP_HEME_FAIL -eq 0 ] ; then
   import_hemepact_ddp_to_redcap
   if [ $? -gt 0 ] ; then
       IMPORT_STATUS_HEME=1
       sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT DDP Redcap Import"
   fi
fi
```

* **CVR AND CLINICAL SUPP DATE ADDED**

```
# imports hemepact cvr data into redcap
if [ $FETCH_CVR_HEME_FAIL -eq 0 ] ; then
    import_hemepact_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_HEME_FAIL -eq 0 ] ; then
        import_hemepact_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi
```

*ARCHER*

* **DDP**

```
## ARCHER imports

if [ $FETCH_DDP_ARCHER_FAIL -eq 0 ] ; then
   import_archer_ddp_to_redcap
   if [ $? -gt 0 ] ; then
       IMPORT_STATUS_ARCHER=1
       sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED DDP Redcap Import"
   fi
fi
```

* **CVR AND CLINICAL SUPP DATE ADDED**

```
# imports archer cvr data into redcap
if [ $FETCH_CVR_ARCHER_FAIL -eq 0 ] ; then
    import_archer_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_ARCHER_FAIL -eq 0 ] ; then
        import_archer_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi
```

*RAINDANCE*

* **DDP**

```
## RAINDANCE imports

if [ $FETCH_DDP_RAINDANCE_FAIL -eq 0 ] ; then
   import_raindance_ddp_to_redcap
   if [ $? -gt 0 ] ; then
       IMPORT_STATUS_RAINDANCE=1
       sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE DDP Redcap Import"
   fi
fi
```

* **CVR AND CLINICAL SUPP DATE ADDED**

```
# imports raindance cvr data into redcap
if [ $FETCH_CVR_RAINDANCE_FAIL -eq 0 ] ; then
    import_raindance_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_RAINDANCE_FAIL -eq 0 ] ; then
        import_raindance_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi
```

*ACCESS*

* **DDP**

```
## ACCESS imports

if [ $FETCH_DDP_ACCESS_FAIL -eq 0 ] ; then
   import_access_ddp_to_redcap
   if [$? -gt 0 ] ; then
       IMPORT_STATUS_ACCESS=1
       sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS DDP Redcap Import"
   fi
fi
```

* **CVR AND CLINICAL SUPP DATE ADDED**

```
# imports access cvr data into redcap
if [ $FETCH_CVR_ACCESS_FAIL -eq 0 ] ; then
    import_access_cvr_to_redcap
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ACCESS=1
        sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS CVR Redcap Import"
    fi
    if [ $EXPORT_SUPP_DATE_ACCESS_FAIL -eq 0 ] ; then
        import_access_supp_date_to_redcap
        if [ $? -gt 0 ] ; then
            sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS Supp Date Redcap Import. Project is now empty, data restoration required"
        fi
    fi
fi

echo "Import into redcap finished"
```

## 9. Remove Raw Clinical and Timeline Files
**********************************************************************
*NOTES: Raw clinical and timeline files are removed from each DMP cohort study directory. A `git rm -f <filename>` is performed on any file not matching `data_clinical_patient.txt`, `data_clinical_sample.txt`, and `data_timeline.txt`*

*Remove Raw Clinical and Timeline Files*

```
# -------------------------------------------------------------
# REMOVE RAW CLINICAL AND TIMELINE FILES

echo "removing raw clinical & timeline files for mskimpact"
remove_raw_clinical_timeline_data_files $MSK_IMPACT_DATA_HOME

echo "removing raw clinical & timeline files for hemepact"
remove_raw_clinical_timeline_data_files $MSK_HEMEPACT_DATA_HOME

echo "removing raw clinical & timeline files for mskraindance"
remove_raw_clinical_timeline_data_files $MSK_RAINDANCE_DATA_HOME

echo "removing raw clinical & timeline files for mskarcher"
remove_raw_clinical_timeline_data_files $MSK_ARCHER_UNFILTERED_DATA_HOME

echo "removing raw clinical & timeline files for mskaccess"
remove_raw_clinical_timeline_data_files $MSK_ACCESS_DATA_HOME

# commit raw file cleanup - study staging directories should only contain files for portal import
$GIT_BINARY commit -m "Raw clinical and timeline file cleanup: MSKIMPACT, HEMEPACT, RAINDANCE, ARCHER, ACCESS"
```

> remove_raw_clinical_timeline_data_files $MSK_IMPACT_DATA_HOME
> remove_raw_clinical_timeline_data_files $MSK_HEMEPACT_DATA_HOME
> remove_raw_clinical_timeline_data_files $MSK_RAINDANCE_DATA_HOME
> remove_raw_clinical_timeline_data_files $MSK_ARCHER_UNFILTERED_DATA_HOME
> remove_raw_clinical_timeline_data_files $MSK_ACCESS_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 26](#github-dmp-repository-state-26)***

*Deleted files*
* Raw Clinical and Timeline files:
    * MSKIMPACT:
        * `$MSK_IMPACT_DATA_HOME/data_clinical_ddp.txt`
        * `$MSK_IMPACT_DATA_HOME/data_clinical_ddp_pediatrics.txt`
        * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_data_clinical_cvr.txt`
        * `$MSK_IMPACT_DATA_HOME/data_clinical_mskimpact_supp_date_cbioportal_added.txt`
        * `$MSK_IMPACT_DATA_HOME/data_clinical_supp_caisis_gbm.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_chemotherapy.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_radiation.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_ddp_surgery.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_imaging_caisis_gbm.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_specimen_caisis_gbm.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_status_caisis_gbm.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_surgery_caisis_gbm.txt`
        * `$MSK_IMPACT_DATA_HOME/data_timeline_treatment_caisis_gbm.txt`
    * HEMEPACT:
        * `$MSK_HEMEPACT_DATA_HOME/data_clinical_ddp.txt`
        * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical.txt`
        * `$MSK_HEMEPACT_DATA_HOME/data_clinical_hemepact_data_clinical_supp_date.txt`
    * RAINDANCE
        * `$MSK_RAINDANCE_DATA_HOME/data_clinical_ddp.txt`
        * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical.txt`
        * `$MSK_RAINDANCE_DATA_HOME/data_clinical_mskraindance_data_clinical_supp_date.txt`
    * ARCHER:
        * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_ddp.txt`
        * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical.txt`
        * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_mskarcher_data_clinical_supp_date.txt`
    * ACCESS:
        * `$MSK_ACCESS_DATA_HOME/data_clinical_ddp.txt`
        * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical.txt`
        * `$MSK_ACCESS_DATA_HOME/data_clinical_mskaccess_data_clinical_supp_date.txt`

> $GIT_BINARY commit -m "Raw clinical and timeline file cleanup: MSKIMPACT, HEMEPACT, RAINDANCE, ARCHER, ACCESS"

**--> Deleted files from [GITHUB | DMP Repository State: 26](#github-dmp-repository-state-26) in `$DMP_DATA_HOME` are removed in this commit.**

**********************************************************************

## 10. Clinical and Timeline cBioPortal Staging Files REDCap Exports
**********************************************************************
*NOTES:*

```
# -------------------------------------------------------------
# REDCAP EXPORTS - CBIO STAGING FORMATS
printTimeStampedDataProcessingStepMessage "export of redcap data"
```
**Clinical and Timeline cBioPortal Staging Files REDCap Exports**

*MSKIMPACT*

```
## MSKIMPACT export

printTimeStampedDataProcessingStepMessage "export of redcap data for mskimpact"
if [ $IMPORT_STATUS_IMPACT -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_ddp_demographics_pediatrics,mskimpact_timeline_radiation_ddp,mskimpact_timeline_chemotherapy_ddp,mskimpact_timeline_surgery_ddp,mskimpact_pediatrics_sample_supp,mskimpact_pediatrics_patient_supp,mskimpact_pediatrics_supp_timeline
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_IMPACT=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT Redcap Export"
    else
        if [ $GENERATE_MASTERLIST_FAIL -eq 0 ] ; then
            $PYTHON_BINARY $PORTAL_HOME/scripts/filter_dropped_samples_patients.py -s $MSK_IMPACT_DATA_HOME/data_clinical_sample.txt -p $MSK_IMPACT_DATA_HOME/data_clinical_patient.txt -t $MSK_IMPACT_DATA_HOME/data_timeline.txt -f $MSK_DMP_TMPDIR/sample_masterlist_for_filtering.txt
            if [ $? -gt 0 ] ; then
                cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            else
                cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Clinical and Timeline"
            fi
        fi
        touch $MSK_IMPACT_CONSUME_TRIGGER
    fi
fi
```

> export_stable_id_from_redcap mskimpact $MSK_IMPACT_DATA_HOME mskimpact_data_clinical_ddp_demographics_pediatrics,mskimpact_timeline_radiation_ddp,mskimpact_timeline_chemotherapy_ddp,mskimpact_timeline_surgery_ddp,mskimpact_pediatrics_sample_supp,mskimpact_pediatrics_patient_supp,mskimpact_pediatrics_supp_timeline

**********************************************************************

***[GITHUB | DMP Repository State: 27](#github-dmp-repository-state-27)***

* Modified files:
    * `$MSK_IMPACT_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt`
    * `$MSK_IMPACT_DATA_HOME/data_timeline.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 27](#github-dmp-repository-state-27) are unstaged.**

*SUCCESS:*

If `$GENERATE_MASTERLIST_FAIL=0` then MSKIMPACT patients and samples which are not in the master list file `$MSK_DMP_TMPDIR/sample_masterlist_for_filtering.txt` are dropped from `data_clinical_patient.txt`, `data_clinical_sample.txt`, and `data_timeline.txt`. If this is successful then modified files are added to the following commit.*

> cd $MSK_IMPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKIMPACT Dataset: Clinical and Timeline"

**--> Modified files from [GITHUB | DMP Repository State: 27](#github-dmp-repository-state-27) in `$MSK_IMPACT_DATA_HOME` are added to this commit.**

**********************************************************************

*HEMEPACT*

```
## HEMEPACT export

printTimeStampedDataProcessingStepMessage "export of redcap data for hemepact"
if [ $IMPORT_STATUS_HEME -eq 0 ] ; then
    export_stable_id_from_redcap mskimpact_heme $MSK_HEMEPACT_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_HEME=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "HEMEPACT Redcap Export"
    else
        touch $MSK_HEMEPACT_CONSUME_TRIGGER
        cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: Clinical and Timeline"
    fi
fi
```

> export_stable_id_from_redcap mskimpact_heme $MSK_HEMEPACT_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 28](#github-dmp-repository-state-28)***

* Modified files:
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_HEMEPACT_DATA_HOME/data_clinical_sample.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 28](#github-dmp-repository-state-28) are unstaged.**

*SUCCESS:*

> cd $MSK_HEMEPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest HEMEPACT Dataset: Clinical and Timeline"

**--> Modified files from [GITHUB | DMP Repository State: 28](#github-dmp-repository-state-28) in `$MSK_HEMEPACT_DATA_HOME` are added to this commit.**

**********************************************************************

*RAINDANCE*

```
## RAINDANCE export

printTimeStampedDataProcessingStepMessage "export of redcap data for raindance"
if [ $IMPORT_STATUS_RAINDANCE -eq 0 ] ; then
    export_stable_id_from_redcap mskraindance $MSK_RAINDANCE_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_RAINDANCE=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "RAINDANCE Redcap Export"
    else
        touch $MSK_RAINDANCE_IMPORT_TRIGGER
        cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: Clinical and Timeline"
    fi
fi
```

> export_stable_id_from_redcap mskraindance $MSK_RAINDANCE_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 29](#github-dmp-repository-state-29)***

* Modified files:
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_RAINDANCE_DATA_HOME/data_clinical_sample.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 29](#github-dmp-repository-state-29) are unstaged.**

*SUCCESS:*

> cd $MSK_RAINDANCE_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RAINDANCE Dataset: Clinical and Timeline"

**--> Modified files from [GITHUB | DMP Repository State: 29](#github-dmp-repository-state-29) in `$MSK_RAINDANCE_DATA_HOME` are added to this commit.**

**********************************************************************

*ARCHER*

```
## ARCHER export

printTimeStampedDataProcessingStepMessage "export of redcap data for archer"
if [ $IMPORT_STATUS_ARCHER -eq 0 ] ; then
    export_stable_id_from_redcap mskarcher $MSK_ARCHER_UNFILTERED_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ARCHER=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "ARCHER_UNFILTERED Redcap Export"
    else
        # we want to remove MSI_COMMENT, MSI_SCORE, and MSI_TYPE from the data clinical file
        archer_data_clinical_tmp_file=$(mktemp $MSK_DMP_TMPDIR/archer_data_clinical_patient.txt.XXXXXX)
        $PYTHON_BINARY $PORTAL_HOME/scripts/filter_clinical_data.py --clinical-file $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_sample.txt --exclude-columns MSI_COMMENT,MSI_SCORE,MSI_TYPE > $archer_data_clinical_tmp_file
        if [ $? -eq 0 ] ; then
            mv $archer_data_clinical_tmp_file $MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_sample.txt
        fi
        touch $MSK_ARCHER_IMPORT_TRIGGER
        cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Clinical and Timeline"
    fi
fi
```

> export_stable_id_from_redcap mskarcher $MSK_ARCHER_UNFILTERED_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 30](#github-dmp-repository-state-30)***

* Modified files:
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_ARCHER_UNFILTERED_DATA_HOME/data_clinical_sample.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 30](#github-dmp-repository-state-30) are unstaged.**

*SUCCESS:*

If REDCap export is successful then select columns (MSI_COMMENT, MSI_SCORE, MSI_TYPE) are removed from `data_clinical_sample.txt`

> cd $MSK_ARCHER_UNFILTERED_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest ARCHER_UNFILTERED Dataset: Clinical and Timeline"

**--> Modified files from [GITHUB | DMP Repository State: 30](#github-dmp-repository-state-30) in `$MSK_ARCHER_UNFILTERED_DATA_HOME` are added to this commit.**

**********************************************************************

*ACCESS*

```
## ACCESS export

printTimeStampedDataProcessingStepMessage "export of redcap data for access"
if [ $IMPORT_STATUS_ACCESS -eq 0 ] ; then
    export_stable_id_from_redcap mskaccess $MSK_ACCESS_DATA_HOME
    if [ $? -gt 0 ] ; then
        IMPORT_STATUS_ACCESS=1
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
        sendPreImportFailureMessageMskPipelineLogsSlack "ACCESS Redcap Export"
    else
        touch $MSK_ACCESS_CONSUME_TRIGGER
        cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest ACCESS Dataset: Clinical and Timeline"
    fi
fi
```

> export_stable_id_from_redcap mskaccess $MSK_ACCESS_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 31](#github-dmp-repository-state-31)***

* Modified files:
    * `$MSK_ACCESS_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_ACCESS_DATA_HOME/data_clinical_sample.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 31](#github-dmp-repository-state-31) are unstaged.**

*SUCCESS:*

> cd $MSK_ACCESS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest ACCESS Dataset: Clinical and Timeline"

**--> Modified files from [GITHUB | DMP Repository State: 31](#github-dmp-repository-state-31) in `$MSK_ACCESS_DATA_HOME` are added to this commit.**

**********************************************************************

## 11. Unlinked ARCHER  Data Processing
**********************************************************************
*NOTES:*

```
# -------------------------------------------------------------
# UNLINKED ARCHER DATA PROCESSING
# NOTE: This processing should only occur if (1) PROCESS_UNLINKED_ARCHER_STUDY=1 and
# we wish to import only the ARCHER cases which are not linked to MSKIMPACT or HEMEPACT cases
```

*Unlinked ARCHER  Data Processing*

```
if [ $PROCESS_UNLINKED_ARCHER_STUDY -eq 1 ] ; then
    # process data for UNLINKED_ARCHER
    if [[ $IMPORT_STATUS_ARCHER -eq 0 && $FETCH_CVR_ARCHER_FAIL -eq 0 ]] ; then
        # attempt to subset archer unfiltered w/same excluded archer samples list used for MIXEDPACT, MSKSOLIDHEME
        $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_ARCHER_DATA_HOME -i mskarcher -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_ARCHER_UNFILTERED_DATA_HOME
        if [ $? -gt 0 ] ; then
            echo "UNLINKED_ARCHER subset failed! Study will not be updated in the portal."
            sendPreImportFailureMessageMskPipelineLogsSlack "UNLINKED_ARCHER subset"
            echo $(date)
            cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            UNLINKED_ARCHER_SUBSET_FAIL=1
            IMPORT_STATUS_ARCHER=1
        else
            echo "UNLINKED_ARCHER subset successful! Creating cancer type case lists..."
            echo $(date)
            # add metadata headers and overrides before importing
            $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskarcher -f $MSK_ARCHER_DATA_HOME/data_clinical*
            if [ $? -gt 0 ] ; then
                echo "Error: Adding metadata headers for UNLINKED_ARCHER failed! Study will not be updated in portal."
                cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
                IMPORT_STATUS_ARCHER=1
            else
                # commit updates and generated case lists
                cd $MSK_ARCHER_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest UNLINKED_ARCHER Dataset"
                addCancerTypeCaseLists $MSK_ARCHER_DATA_HOME "mskarcher" "data_clinical_sample.txt" "data_clinical_patient.txt"
                cd $MSK_ARCHER_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest UNLINKED_ARCHER Dataset: Case Lists"
                cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
            fi
        fi
    fi
fi

# trigger import for archer if all pre-import and processing steps succeeded
if [[ $IMPORT_STATUS_ARCHER -eq 0 && $FETCH_CVR_ARCHER_FAIL -eq 0 ]] ; then
    touch $MSK_ARCHER_IMPORT_TRIGGER
fi
```

> $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_ARCHER_DATA_HOME -i mskarcher -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_ARCHER_UNFILTERED_DATA_HOME

**********************************************************************

***[GITHUB | DMP Repository State: 32](#github-dmp-repository-state-32)***

* Modified files:
    * `$MSK_ARCHER_DATA_HOME/data_sv.txt`
    * `$MSK_ARCHER_DATA_HOME/data_clinical_patient.txt`
    * `$MSK_ARCHER_DATA_HOME/data_clinical_sample.txt`
    * `$MSK_ARCHER_DATA_HOME/data_gene_matrix.txt`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 32](#github-dmp-repository-state-32) are unstaged.**

*SUCCESS:*

Clinical attribute metadata headers are added to `data_clinical_patient.txt` and `data_clinical_sample.txt`. If this step fails then script will unstage changes to the modified files in `$MSK_ARCHER_DATA_HOME` with "FAILURE" step above.

> cd $MSK_ARCHER_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest UNLINKED_ARCHER Dataset"

**--> Modified files from [GITHUB | DMP Repository State: 32](#github-dmp-repository-state-32) in `$MSK_ARCHER_DATA_HOME` are added to this commit.**

**********************************************************************

> addCancerTypeCaseLists $MSK_ARCHER_DATA_HOME "mskarcher" "data_clinical_sample.txt" "data_clinical_patient.txt"

**********************************************************************

***[GITHUB | DMP Repository State: 33](#github-dmp-repository-state-33)***

*Modified and (occassionally) Untracked files*
* Cancer Type case lists:
    * `$MSK_ARCHER_DATA_HOME/case_lists`

> cd $MSK_ARCHER_DATA_HOME ; $GIT_BINARY add case_lists ; $GIT_BINARY commit -m "Latest UNLINKED_ARCHER Dataset: Case Lists"

**--> Modified files and untracked files from [GITHUB | DMP Repository State: 33](#github-dmp-repository-state-33) in `$MSK_ARCHER_DATA_HOME/case_lists` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modifications to the clinical sample file may occur during the generation of case lists - we do not want to keep these changes. This `reset` will unstage those changes.**

## 12. MIXEDPACT and MSKSOLIDHEME Updates
**********************************************************************
*NOTES:*

```
#--------------------------------------------------------------
## MERGE STUDIES FOR MIXEDPACT, MSKSOLIDHEME:
#   (1) MSK-IMPACT, HEMEPACT, RAINDANCE, ARCHER, and ACCESS (MIXEDPACT)
#   (1) MSK-IMPACT, HEMEPACT, ARCHER, and ACCESS (MSKSOLIDHEME)
```

* *Create temporary meta files for merge script use.*

```
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
```

*MIXEDPACT*

```
printTimeStampedDataProcessingStepMessage "merge of MSK-IMPACT, HEMEPACT, RAINDANCE, ARCHER, ACCESS data for MIXEDPACT"
# MIXEDPACT merge and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_MIXEDPACT_DATA_HOME -i mixedpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_RAINDANCE_DATA_HOME $MSK_ARCHER_UNFILTERED_DATA_HOME $MSK_ACCESS_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MIXEDPACT merge failed! Study will not be updated in the portal."
    echo $(date)
    MIXEDPACT_MERGE_FAIL=1
else
    echo "MIXEDPACT merge successful!"
    echo $(date)
fi
```

*MSKSOLIDHEME*

```
printTimeStampedDataProcessingStepMessage "merge of MSK-IMPACT, HEMEPACT, ACCESS data for MSKSOLIDHEME"
# MSKSOLIDHEME merge and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_SOLID_HEME_DATA_HOME -i mskimpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_ACCESS_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MSKSOLIDHEME merge failed! Study will not be updated in the portal."
    echo $(date)
    MSK_SOLID_HEME_MERGE_FAIL=1
    # we rollback/clean git after the import of MSKSOLIDHEME (if merge or import fails)
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
```

* *Cleanup temporary meta files created*

```
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
```



> Commands affecting MIXEDPACT data
>
> $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_MIXEDPACT_DATA_HOME -i mixedpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_RAINDANCE_DATA_HOME $MSK_ARCHER_UNFILTERED_DATA_HOME $MSK_ACCESS_DATA_HOME
>
> Commands affecting MSKSOLIDHEME data
>
> $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_SOLID_HEME_DATA_HOME -i mskimpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_ACCESS_DATA_HOME
>
> $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical*
>
> addCancerTypeCaseLists $MSK_SOLID_HEME_DATA_HOME "mskimpact" "data_clinical_sample.txt" "data_clinical_patient.txt"

```
# commit or revert changes for MIXEDPACT
if [ $MIXEDPACT_MERGE_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "MIXEDPACT merge"
    echo "MIXEDPACT merge failed! Reverting data to last commit."
    cd $MSK_MIXEDPACT_DATA_HOME ; $GIT_BINARY checkout -- .
else
    echo "Committing MIXEDPACT data"
    cd $MSK_MIXEDPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MIXEDPACT dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 34](#github-dmp-repository-state-34)***

* Modified files:
    * MIXEDPACT Merge:
        * `$MSK_MIXEDPACT_DATA_HOME/data_CNA.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_sv.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_clinical_patient.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_gene_matrix.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_mutations_extended.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_mutations_manual.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_nonsignedout_mutations.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/data_timeline.txt`
        * `$MSK_MIXEDPACT_DATA_HOME/mixedpact_data_cna_hg19.seg`
    * MSKSOLIDHEME Merge:
        * `$MSK_SOLID_HEME_DATA_HOME/data_CNA.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_sv.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_clinical_patient.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_gene_matrix.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_mutations_extended.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_mutations_manual.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_nonsignedout_mutations.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_timeline.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/mskimpact_data_cna_hg19.seg`

*FAILURE:*

> cd $MSK_MIXEDPACT_DATA_HOME ; $GIT_BINARY checkout -- .

**--> Modified files from [GITHUB | DMP Repository State: 34](#github-dmp-repository-state-34) in `$MSK_MIXEDPACT_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_MIXEDPACT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MIXEDPACT dataset"

**--> Modified files from [GITHUB | DMP Repository State: 34](#github-dmp-repository-state-34) in `$MSK_MIXEDPACT_DATA_HOME` are added to this commit.**

**********************************************************************

```
# commit or revert changes for MSKSOLIDHEME
if [ $MSK_SOLID_HEME_MERGE_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKSOLIDHEME merge"
    echo "MSKSOLIDHEME merge and/or updates failed! Reverting data to last commit."
    cd $MSK_SOLID_HEME_DATA_HOME ; $GIT_BINARY checkout -- .
else
    echo "Committing MSKSOLIDHEME data"
    cd $MSK_SOLID_HEME_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKSOLIDHEME dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 35](#github-dmp-repository-state-35)***

* Modified files:
    * MSKSOLIDHEME Merge:
        * `$MSK_SOLID_HEME_DATA_HOME/data_CNA.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_sv.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_clinical_patient.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_gene_matrix.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_mutations_extended.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_mutations_manual.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_nonsignedout_mutations.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/data_timeline.txt`
        * `$MSK_SOLID_HEME_DATA_HOME/mskimpact_data_cna_hg19.seg`

*FAILURE:*

> cd $MSK_SOLID_HEME_DATA_HOME ; $GIT_BINARY checkout -- .

**--> Modified files from [GITHUB | DMP Repository State: 35](#github-dmp-repository-state-35) in `$MSK_SOLID_HEME_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_SOLID_HEME_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKSOLIDHEME dataset"

**--> Modified files from [GITHUB | DMP Repository State: 35](#github-dmp-repository-state-35) in `$MSK_SOLID_HEME_DATA_HOME` are added to this commit.**

**********************************************************************

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

Any modified files not added to commit from [GITHUB | DMP Repository State: 34](#github-dmp-repository-state-34) and [GITHUB | DMP Repository State: 35](#github-dmp-repository-state-35) will be unstaged.






# TODO: DOCUMENT REMAINING STEPS

## 13. Affiliate Cohort Updates
**********************************************************************
*NOTES:*

```
#--------------------------------------------------------------
# AFFILIATE COHORTS
printTimeStampedDataProcessingStepMessage "subset of affiliate cohorts"
```

**INSTITUTE-SPECIFIC SUBSETS**
```
## Subset MIXEDPACT on INSTITUTE for institute specific impact studies

# subset the mixedpact study for Queens Cancer Center, Lehigh Valley, Kings County Cancer Center, Miami Cancer Institute, and Hartford Health Care
```

*KINGSCOUNTY*

```
# KINGSCOUNTY subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_kingscounty -o=$MSK_KINGS_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Kings County Cancer Center" -s=$MSK_DMP_TMPDIR/kings_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Kings County subset failed! Study will not be updated in the portal."
    MSK_KINGS_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_KINGS_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Kings County subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_KINGS_SUBSET_FAIL=1
    else
        echo "MSK Kings County subset successful!"
        addCancerTypeCaseLists $MSK_KINGS_DATA_HOME "msk_kingscounty" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_KINGS_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for KINGSCOUNTY
if [ $MSK_KINGS_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "KINGSCOUNTY subset"
    echo "KINGSCOUNTY subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing KINGSCOUNTY data"
    cd $MSK_KINGS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest KINGSCOUNTY dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 36](#github-dmp-repository-state-36)***

* Modified files:
    * `$MSK_KINGS_DATA_HOME/*data*`
    * `$MSK_KINGS_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 36](#github-dmp-repository-state-36) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_KINGS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest KINGSCOUNTY dataset"

**--> Modified files from [GITHUB | DMP Repository State: 36](#github-dmp-repository-state-36) in `$MSK_KINGS_DATA_HOME` are added to this commit.**

**********************************************************************

*LEHIGHVALLEY*

```
# LEHIGHVALLEY subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_lehighvalley -o=$MSK_LEHIGH_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Lehigh Valley Health Network" -s=$MSK_DMP_TMPDIR/lehigh_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Lehigh Valley subset failed! Study will not be updated in the portal."
    MSK_LEHIGH_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_LEHIGH_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Lehigh Valley subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_LEHIGH_SUBSET_FAIL=1
    else
        echo "MSK Lehigh Valley subset successful!"
        addCancerTypeCaseLists $MSK_LEHIGH_DATA_HOME "msk_lehighvalley" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_LEHIGH_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for LEHIGHVALLEY
if [ $MSK_LEHIGH_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "LEHIGHVALLEY subset"
    echo "LEHIGHVALLEY subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing LEHIGHVALLEY data"
    cd $MSK_LEHIGH_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest LEHIGHVALLEY dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 37](#github-dmp-repository-state-37)***

* Modified files:
    * `$MSK_LEHIGH_DATA_HOME/*data*`
    * `$MSK_LEHIGH_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 37](#github-dmp-repository-state-37) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_LEHIGH_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest LEHIGHVALLEY dataset"

**--> Modified files from [GITHUB | DMP Repository State: 37](#github-dmp-repository-state-37) in `$MSK_LEHIGH_DATA_HOME` are added to this commit.**

**********************************************************************

*QUEENSCANCERCENTER*

```
# QUEENSCANCERCENTER subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_queenscancercenter -o=$MSK_QUEENS_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Queens Cancer Center,Queens Hospital Cancer Center" -s=$MSK_DMP_TMPDIR/queens_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Queens Cancer Center subset failed! Study will not be updated in the portal."
    MSK_QUEENS_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_QUEENS_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Queens Cancer Center subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_QUEENS_SUBSET_FAIL=1
    else
        echo "MSK Queens Cancer Center subset successful!"
        addCancerTypeCaseLists $MSK_QUEENS_DATA_HOME "msk_queenscancercenter" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_QUEENS_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for QUEENSCANCERCENTER
if [ $MSK_QUEENS_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "QUEENSCANCERCENTER subset"
    echo "QUEENSCANCERCENTER subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing QUEENSCANCERCENTER data"
    cd $MSK_QUEENS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest QUEENSCANCERCENTER dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 38](#github-dmp-repository-state-38)***

* Modified files:
    * `$MSK_QUEENS_DATA_HOME/*data*`
    * `$MSK_QUEENS_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 38](#github-dmp-repository-state-38) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_QUEENS_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest QUEENSCANCERCENTER dataset"

**--> Modified files from [GITHUB | DMP Repository State: 38](#github-dmp-repository-state-38) in `$MSK_QUEENS_DATA_HOME` are added to this commit.**

**********************************************************************

*MIAMICANCERINSTITUTE*

```
# MIAMICANCERINSTITUTE subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_miamicancerinstitute -o=$MSK_MCI_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Miami Cancer Institute" -s=$MSK_DMP_TMPDIR/mci_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Miami Cancer Institute subset failed! Study will not be updated in the portal."
    MSK_MCI_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_MCI_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Miami Cancer Institute subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_MCI_SUBSET_FAIL=1
    else
        echo "MSK Miami Cancer Institute subset successful!"
        addCancerTypeCaseLists $MSK_MCI_DATA_HOME "msk_miamicancerinstitute" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_MCI_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for MIAMICANCERINSTITUTE
if [ $MSK_MCI_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "MIAMICANCERINSTITUTE subset"
    echo "MIAMICANCERINSTITUTE subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing MIAMICANCERINSTITUTE data"
    cd $MSK_MCI_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MIAMICANCERINSTITUTE dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 39](#github-dmp-repository-state-39)***

* Modified files:
    * `$MSK_MCI_DATA_HOME/*data*`
    * `$MSK_MCI_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 39](#github-dmp-repository-state-39) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_MCI_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MIAMICANCERINSTITUTE dataset"

**--> Modified files from [GITHUB | DMP Repository State: 39](#github-dmp-repository-state-39) in `$MSK_MCI_DATA_HOME` are added to this commit.**

**********************************************************************

*HARTFORDHEALTHCARE*

```
# HARTFORDHEALTHCARE subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_hartfordhealthcare -o=$MSK_HARTFORD_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Hartford Healthcare" -s=$MSK_DMP_TMPDIR/hartford_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Hartford Healthcare subset failed! Study will not be updated in the portal."
    MSK_HARTFORD_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_HARTFORD_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Hartford Healthcare subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_HARTFORD_SUBSET_FAIL=1
    else
        echo "MSK Hartford Healthcare subset successful!"
        addCancerTypeCaseLists $MSK_HARTFORD_DATA_HOME "msk_hartfordhealthcare" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_HARTFORD_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for HARTFORDHEALTHCARE
if [ $MSK_HARTFORD_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "HARTFORDHEALTHCARE subset"
    echo "HARTFORDHEALTHCARE subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing HARTFORDHEALTHCARE data"
    cd $MSK_HARTFORD_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest HARTFORDHEALTHCARE dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 40](#github-dmp-repository-state-40)***

* Modified files:
    * `$MSK_HARTFORD_DATA_HOME/*data*`
    * `$MSK_HARTFORD_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 40](#github-dmp-repository-state-40) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_HARTFORD_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest HARTFORDHEALTHCARE dataset"

**--> Modified files from [GITHUB | DMP Repository State: 40](#github-dmp-repository-state-40) in `$MSK_HARTFORD_DATA_HOME` are added to this commit.**

**********************************************************************

*RALPHLAUREN*

```
# RALPHLAUREN subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_ralphlauren -o=$MSK_RALPHLAUREN_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Ralph Lauren Center" -s=$MSK_DMP_TMPDIR/ralphlauren_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Ralph Lauren subset failed! Study will not be updated in the portal."
    MSK_RALPHLAUREN_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_RALPHLAUREN_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Ralph Lauren subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_RALPHLAUREN_SUBSET_FAIL=1
    else
        echo "MSK Ralph Lauren subset successful!"
        addCancerTypeCaseLists $MSK_RALPHLAUREN_DATA_HOME "msk_ralphlauren" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_RALPHLAUREN_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for RALPHLAUREN
if [ $MSK_RALPHLAUREN_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "RALPHLAUREN subset"
    echo "RALPHLAUREN subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing RALPHLAUREN data"
    cd $MSK_RALPHLAUREN_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RALPHLAUREN dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 41](#github-dmp-repository-state-41)***

* Modified files:
    * `$MSK_RALPHLAUREN_DATA_HOME/*data*`
    * `$MSK_RALPHLAUREN_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 41](#github-dmp-repository-state-41) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_RALPHLAUREN_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RALPHLAUREN dataset"

**--> Modified files from [GITHUB | DMP Repository State: 41](#github-dmp-repository-state-41) in `$MSK_RALPHLAUREN_DATA_HOME` are added to this commit.**

**********************************************************************

*RIKENGENESISJAPAN*

```
# RIKENGENESISJAPAN subset
bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=msk_rikengenesisjapan -o=$MSK_RIKENGENESISJAPAN_DATA_HOME -d=$MSK_MIXEDPACT_DATA_HOME -f="INSTITUTE=Riken Genesis" -s=$MSK_DMP_TMPDIR/rikengenesisjapan_subset.txt -c=$MSK_MIXEDPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSK Tailor Med Japan subset failed! Study will not be updated in the portal."
    MSK_RIKENGENESISJAPAN_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_RIKENGENESISJAPAN_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSK Tailor Med Japan subset clinical attribute filtering step failed! Study will not be updated in the portal."
        MSK_RIKENGENESISJAPAN_SUBSET_FAIL=1
    else
        echo "MSK Tailor Med Japan subset successful!"
        addCancerTypeCaseLists $MSK_RIKENGENESISJAPAN_DATA_HOME "msk_rikengenesisjapan" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for RIKENGENESISJAPAN
if [ $MSK_RIKENGENESISJAPAN_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "RIKENGENESISJAPAN subset"
    echo "RIKENGENESISJAPAN subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing RIKENGENESISJAPAN data"
    cd $MSK_RIKENGENESISJAPAN_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RIKENGENESISJAPAN dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 42](#github-dmp-repository-state-42)***

* Modified files:
    * `$MSK_RIKENGENESISJAPAN_DATA_HOME/*data*`
    * `$MSK_RIKENGENESISJAPAN_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 42](#github-dmp-repository-state-42) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_RIKENGENESISJAPAN_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest RIKENGENESISJAPAN dataset"

**--> Modified files from [GITHUB | DMP Repository State: 42](#github-dmp-repository-state-42) in `$MSK_RIKENGENESISJAPAN_DATA_HOME` are added to this commit.**

**********************************************************************

**PEDIATRIC COHORT SUBSET**

```
#--------------------------------------------------------------
# Subset MSKIMPACT on PED_IND for MSKIMPACT_PED cohort
printTimeStampedDataProcessingStepMessage "subset of mskimpact for mskimpact_ped cohort"
# rsync mskimpact data to tmp ped data home and overwrite clinical/timeline data with redcap
# export of mskimpact (with ped specific data and without caisis) into tmp ped data home directory for subsetting
# NOTE: the importer uses the java_tmp_dir/study_identifier for writing temp meta and data files so we should use a different
# tmp directory for subsetting purposes to prevent any conflicts and allow easier debugging of any issues that arise
MSKIMPACT_PED_TMP_DIR=$MSK_DMP_TMPDIR/mskimpact_ped_tmp
rsync -a $MSK_IMPACT_DATA_HOME/* $MSKIMPACT_PED_TMP_DIR
export_stable_id_from_redcap mskimpact $MSKIMPACT_PED_TMP_DIR mskimpact_clinical_caisis,mskimpact_timeline_surgery_caisis,mskimpact_timeline_status_caisis,mskimpact_timeline_treatment_caisis,mskimpact_timeline_imaging_caisis,mskimpact_timeline_specimen_caisis,mskimpact_data_clinical_ddp_demographics,mskimpact_timeline_chemotherapy_ddp,mskimpact_timeline_surgery_ddp
if [ $? -gt 0 ] ; then
    echo "MSKIMPACT redcap export for MSKIMPACT_PED failed! Study will not be updated in the portal"
    sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED redcap export"
    MSKIMPACT_PED_SUBSET_FAIL=1
else
    bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=mskimpact_ped -o=$MSKIMPACT_PED_DATA_HOME -d=$MSKIMPACT_PED_TMP_DIR -f="PED_IND=Yes" -s=$MSK_DMP_TMPDIR/mskimpact_ped_subset.txt -c=$MSKIMPACT_PED_TMP_DIR/data_clinical_patient.txt
    if [ $? -gt 0 ] ; then
        echo "MSKIMPACT_PED subset failed! Study will not be updated in the portal."
        MSKIMPACT_PED_SUBSET_FAIL=1
    else
        filter_derived_clinical_data $MSKIMPACT_PED_DATA_HOME
        if [ $? -gt 0 ] ; then
            echo "MSKIMPACT_PED subset clinical attribute filtering step failed! Study will not be updated in the portal."
            MSKIMPACT_PED_SUBSET_FAIL=1
        else
            echo "MSKIMPACT_PED subset successful!"
            addCancerTypeCaseLists $MSKIMPACT_PED_DATA_HOME "mskimpact_ped" "data_clinical_sample.txt" "data_clinical_patient.txt"
            touch $MSKIMPACT_PED_IMPORT_TRIGGER
        fi
    fi

    # commit or revert changes for MSKIMPACT_PED
    if [ $MSKIMPACT_PED_SUBSET_FAIL -gt 0 ] ; then
        sendPreImportFailureMessageMskPipelineLogsSlack "MSKIMPACT_PED subset"
        echo "MSKIMPACT_PED subset and/or updates failed! Reverting data to last commit."
        cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
    else
        echo "Committing MSKIMPACT_PED data"
        cd $MSKIMPACT_PED_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKIMPACT_PED dataset"
    fi
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 43](#github-dmp-repository-state-43)***

* Modified files:
    * `$MSKIMPACT_PED_DATA_HOME/*data*`
    * `$MSKIMPACT_PED_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 43](#github-dmp-repository-state-43) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSKIMPACT_PED_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest MSKIMPACT_PED dataset"

**--> Modified files from [GITHUB | DMP Repository State: 43](#github-dmp-repository-state-43) in `$MSKIMPACT_PED_DATA_HOME` are added to this commit.**

**********************************************************************

**SCLC COHORT SUBSET**

```
#--------------------------------------------------------------
printTimeStampedDataProcessingStepMessage "subset of mskimpact for sclc cohort"
# Subset MSKIMPACT on ONCOTREE_CODE for SCLC cohort

bash $PORTAL_HOME/scripts/subset-impact-data.sh -i=sclc_mskimpact_2017 -o=$MSK_SCLC_DATA_HOME -d=$MSK_IMPACT_DATA_HOME -f="ONCOTREE_CODE=SCLC" -s=$MSK_DMP_TMPDIR/sclc_subset.txt -c=$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt
if [ $? -gt 0 ] ; then
    echo "MSKIMPACT SCLC subset failed! Study will not be updated in the portal."
    SCLC_MSKIMPACT_SUBSET_FAIL=1
else
    filter_derived_clinical_data $MSK_SCLC_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "MSKIMPACT SCLC subset clinical attribute filtering step failed! Study will not be updated in the portal."
        SCLC_MSKIMPACT_SUBSET_FAIL=1
    else
        echo "MSKIMPACT SCLC subset successful!"
        addCancerTypeCaseLists $MSK_SCLC_DATA_HOME "sclc_mskimpact_2017" "data_clinical_sample.txt" "data_clinical_patient.txt"
        touch $MSK_SCLC_IMPORT_TRIGGER
    fi
fi

# commit or revert changes for SCLCMSKIMPACT
if [ $SCLC_MSKIMPACT_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "SCLCMSKIMPACT subset"
    echo "SCLCMSKIMPACT subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing SCLCMSKIMPACT data"
    cd $MSK_SCLC_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest SCLCMSKIMPACT dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 44](#github-dmp-repository-state-44)***

* Modified files:
    * `$MSK_SCLC_DATA_HOME/*data*`
    * `$MSK_SCLC_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 44](#github-dmp-repository-state-44) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $MSK_SCLC_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest SCLCMSKIMPACT dataset"

**--> Modified files from [GITHUB | DMP Repository State: 44](#github-dmp-repository-state-44) in `$MSK_SCLC_DATA_HOME` are added to this commit.**

**********************************************************************

**LYMPHOMASUPERCOHORT SUBSET**

```
#--------------------------------------------------------------
printTimeStampedDataProcessingStepMessage "subset and merge of mskimpact, hemepact for lymphoma_super_cohort_fmi_msk"
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
LYMPHOMA_FILTER_CRITERIA="CANCER_TYPE=Blastic Plasmacytoid Dendritic Cell Neoplasm,Histiocytosis,Hodgkin Lymphoma,Leukemia,Mastocytosis,Mature B-Cell Neoplasms,Mature T and NK Neoplasms,Myelodysplastic Syndromes,Myelodysplastic/Myeloproliferative Neoplasms,Myeloproliferative Neoplasms,Non-Hodgkin Lymphoma;ONCOTREE_CODE=AITL,ALCL,ALCLALKN,ALCLALKP,AML,AMLCBFBMYH11,AMLCEBPA,AMLDEKNUP214,AMLGATA2MECOM,AMLMRC,AMLNOS,AMLNPM1,AMLRUNX1,AMLRUNX1RUNX1T1,AMML,APLPMLRARA,BCL,BIALCL,BL,CHL,CLLSLL,CLPDNK,CML,CMLBCRABL1,CMML,CMML0,CMML1,CMML2,DLBCL,DLBCLNOS,EBVDLBCLNOS,ECD,EMALT,ET,ETMF,FL,HCL,HDCN,HGBCLMYCBCL2,HS,LCH,LPL,MALTL,MBCL,MCBCL,MCL,MDS,MDSEB,MDSEB1,MDSEB2,MDSID5Q,MDSMD,MDSMPNU,MDSRS,MDSRSMD,MDSRSSLD,MDSSLD,MEITL,MGUS,MPALBNOS,MPALTNOS,MYCF,MZL,NLPHL,NPTLTFH,ONCOTREE_CODE,PCGDTCL,PCLPD,PCM,PCNSL,PLBL,PMBL,PMF,PTCL,PV,RDD,SEZS,SLL,SM,SMAHN,SMZL,SPB,SS,TAML,THRLBCL,TLGL,TMDS,TMN,TNKL,TPLL,WM"
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_IMPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT. Skipping merge and update of lymphoma super cohort!"
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py --study-id="lymphoma_super_cohort_fmi_msk" --clinical-file="$MSK_HEMEPACT_DATA_HOME/data_clinical_sample.txt" --filter-criteria="$LYMPHOMA_FILTER_CRITERIA" --subset-filename="$MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt"
if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of lymphoma samples from MSK-IMPACT Heme. Skipping merge and update of lymphoma super cohort!"
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

# check sizes of subset files before attempting to merge data using these subsets
grep -v '^#' $FMI_BATLEVI_DATA_HOME/data_clinical_sample.txt | awk -F '\t' '{if ($2 != "SAMPLE_ID") print $2;}' > $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from source Foundation study"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from MSKIMPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $MSK_DMP_TMPDIR/mskimpact_lymphoma_subset.txt >> $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
fi

if [[ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 && $(wc -l < $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt) -eq 0 ]] ; then
    echo "ERROR! Subset list $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt is empty. Skipping merge and update of lymphoma super cohort!"
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT subset from HEMEPACT produced empty list"
    LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
else
    cat $MSK_DMP_TMPDIR/mskimpact_heme_lymphoma_subset.txt >> $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt
fi

# merge data from mskimpact and hemepact lymphoma subsets with FMI BAT study
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -eq 0 ] ; then
    $PYTHON_BINARY $PORTAL_HOME/scripts/merge.py  -d $LYMPHOMA_SUPER_COHORT_DATA_HOME -i "lymphoma_super_cohort_fmi_msk" -m "true" -s $MSK_DMP_TMPDIR/lymphoma_subset_samples.txt $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $FMI_BATLEVI_DATA_HOME
    if [ $? -gt 0 ] ; then
        echo "Lymphoma super cohort subset failed! Lymphoma super cohort study will not be updated in the portal."
        LYMPHOMA_SUPER_COHORT_SUBSET_FAIL=1
    else
        filter_derived_clinical_data $LYMPHOMA_SUPER_COHORT_DATA_HOME
        if [ $? -gt 0 ] ; then
            echo "Lymphoma super subset clinical attribute filtering step failed! Study will not be updated in the portal."
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
    rm -f $LYMPHOMA_SUPER_COHORT_DATA_HOME/*genie* $LYMPHOMA_SUPER_COHORT_DATA_HOME/seq_date.txt
fi

# check that meta_SV.txt is actually an empty file before deleting from IMPACT and HEMEPACT studies
if [ $(wc -l < $MSK_IMPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_IMPACT_DATA_HOME/meta_SV.txt
fi

if [ $(wc -l < $MSK_HEMEPACT_DATA_HOME/meta_SV.txt) -eq 0 ] ; then
    rm $MSK_HEMEPACT_DATA_HOME/meta_SV.txt
fi

# commit or revert changes for Lymphoma super cohort
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -gt 0 ] ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "LYMPHOMASUPERCOHORT merge"
    echo "Lymphoma super cohort subset and/or updates failed! Reverting data to last commit."
    cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard
else
    echo "Committing Lymphoma super cohort data"
    cd $LYMPHOMA_SUPER_COHORT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest Lymphoma Super Cohort dataset"
fi
```

**********************************************************************

***[GITHUB | DMP Repository State: 45](#github-dmp-repository-state-45)***

* Modified files:
    * `$LYMPHOMA_SUPER_COHORT_DATA_HOME/*data*`
    * `$LYMPHOMA_SUPER_COHORT_DATA_HOME/case_lists`

*FAILURE:*

> cd $DMP_DATA_HOME ; $GIT_BINARY reset HEAD --hard

**--> Modified files from [GITHUB | DMP Repository State: 45](#github-dmp-repository-state-45) in `$DMP_DATA_HOME` are unstaged.**

*SUCCESS:*

> cd $LYMPHOMA_SUPER_COHORT_DATA_HOME ; $GIT_BINARY add * ; $GIT_BINARY commit -m "Latest Lymphoma Super Cohort dataset"

**--> Modified files from [GITHUB | DMP Repository State: 45](#github-dmp-repository-state-45) in `$LYMPHOMA_SUPER_COHORT_DATA_HOME` are added to this commit.**

**********************************************************************

## 14. Push All Commits to GIT Repository
**********************************************************************

```
#--------------------------------------------------------------
# GIT PUSH
printTimeStampedDataProcessingStepMessage "push of dmp data updates to git repository"
# check updated data back into git
GIT_PUSH_FAIL=0
cd $DMP_DATA_HOME ; $GIT_BINARY push origin
if [ $? -gt 0 ] ; then
    GIT_PUSH_FAIL=1
    sendPreImportFailureMessageMskPipelineLogsSlack "GIT PUSH :fire: - address ASAP!"
fi
```

## 15. Email Reports
**********************************************************************

```
#--------------------------------------------------------------
# Emails for failed processes

EMAIL_BODY="Failed to push outgoing changes to Git - address ASAP!"
# send email if failed to push outgoing changes to git
if [ $GIT_PUSH_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "[URGENT] GIT PUSH FAILURE" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="The MSKIMPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_IMPACT -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT Fetch Failure: Import" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="The HEMEPACT study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_HEME -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "HEMEPACT Fetch Failure: Import" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="The RAINDANCE study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_RAINDANCE -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "RAINDANCE Fetch Failure: Import" $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="The ARCHER study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_ARCHER -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "ARCHER Fetch Failure: Import" $email_list
fi

EMAIL_BODY="The ACCESS study failed fetch. The original study will remain on the portal."
# send email if fetch fails
if [ $IMPORT_STATUS_ACCESS -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "ACCESS Fetch Failure: Import" $email_list
fi

EMAIL_BODY="Failed to merge ARCHER structural variant events into MSKIMPACT"
if [ $ARCHER_MERGE_IMPACT_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MSKIMPACT-ARCHER Merge Failure: Study will be updated without new ARCHER structural variant events." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to merge ARCHER structural variant events into HEMEPACT"
if [ $ARCHER_MERGE_HEME_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "HEMEPACT-ARCHER Merge Failure: Study will be updated without new ARCHER structural variant events." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset UNLINKED_ARCHER data from ARCHER_UNFILTERED"
if [ $UNLINKED_ARCHER_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "UNLINKED_ARCHER Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, ARCHER, and RAINDANCE data. Merged study will not be updated."
if [ $MIXEDPACT_MERGE_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIXEDPACT Merge Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to merge MSK-IMPACT, HEMEPACT, and ARCHER data. Merged study will not be updated."
if [ $MSK_SOLID_HEME_MERGE_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MSKSOLIDHEME Merge Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Kings County Cancer Center data. Subset study will not be updated."
if [ $MSK_KINGS_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "KINGSCOUNTY Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Lehigh Valley data. Subset study will not be updated."
if [ $MSK_LEHIGH_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "LEHIGHVALLEY Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Queens Cancer Center data. Subset study will not be updated."
if [ $MSK_QUEENS_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "QUEENSCANCERCENTER Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Miami Cancer Institute data. Subset study will not be updated."
if [ $MSK_MCI_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "MIAMICANCERINSTITUTE Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Hartford Healthcare data. Subset study will not be updated."
if [ $MSK_HARTFORD_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "HARTFORDHEALTHCARE Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset Ralph Lauren Center data. Subset study will not be updated."
if [ $MSK_RALPHLAUREN_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "RALPHLAUREN Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset MSK Tailor Med Japan data. Subset study will not be updated."
if [ $MSK_RIKENGENESISJAPAN_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "RIKENGENESISJAPAN Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset MSKIMPACT_PED data. Subset study will not be updated."
if [ $MSKIMPACT_PED_SUBSET_FAIL -gt 0 ]; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSKIMPACT_PED Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset MSKIMPACT SCLC data. Subset study will not be updated."
if [ $SCLC_MSKIMPACT_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "SCLCMSKIMPACT Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi

EMAIL_BODY="Failed to subset LYMPHOMASUPERCOHORT data. Subset study will not be updated."
if [ $LYMPHOMA_SUPER_COHORT_SUBSET_FAIL -gt 0 ] ; then
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "LYMPHOMASUPERCOHORT Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST
fi
```
