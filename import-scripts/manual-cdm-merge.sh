# assumes data for import is on hpc3 at /gpfs/mindphidata/cdm_repos/dev/data/<cohort>
# TODO initiate s3 push first to dev bucket

sh $PORTAL_HOME/scripts/pull-cdm-data.sh # TODO this needs to change to work for dev bucket

# TODO i think these scripts need to be generalized
sh $PORTAL_HOME/scripts/merge-cdm-data.sh mskimpact $MSK_CHORD_DATA_HOME/mskimpact $MSK_IMPACT_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "Error: CDM merge for MSKIMPACT"
fi

sh $PORTAL_HOME/scripts/merge-cdm-data.sh mskimpact_heme $MSK_CHORD_DATA_HOME/mskimpact_heme $MSK_HEMEPACT_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "Error: CDM merge for HEMEPACT"
fi

sh $PORTAL_HOME/scripts/merge-cdm-data.sh mskarcher $MSK_CHORD_DATA_HOME/mskarcher $MSK_ARCHER_UNFILTERED_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "Error: CDM merge for ARCHER"
fi

sh $PORTAL_HOME/scripts/merge-cdm-data.sh mskaccess $MSK_CHORD_DATA_HOME/mskaccess $MSK_ACCESS_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "Error: CDM merge for ACCESS"
fi


source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

# TODO set up more official dirs
MSK_ARCHER_UNFILTERED_DATA_HOME="/data2/chennac/cbio-portal-data/dmp/mskarcher"
MSK_SOLID_HEME_DATA_HOME="/data2/chennac/cdm-import/msk_solid_heme"
MSK_IMPACT_DATA_HOME="/data2/chennac/cdm-import/mskimpact"
MSK_HEMEPACT_DATA_HOME="/data2/chennac/cdm-import/mskimpact_heme"
MSK_ACCESS_DATA_HOME="/data2/chennac/cdm-import/mskaccess"

MAPPED_ARCHER_SAMPLES_FILE=$MSK_ARCHER_UNFILTERED_DATA_HOME/cvr/mapped_archer_samples.txt

# TODO change output dir from MSK_SOLID_HEME_DATA_HOME

printTimeStampedDataProcessingStepMessage "merge of MSK-IMPACT, HEMEPACT, ACCESS data for MSKSOLIDHEME"
# MSKSOLIDHEME merge and check exit code
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py -d $MSK_SOLID_HEME_DATA_HOME -i mskimpact -m "true" -e $MAPPED_ARCHER_SAMPLES_FILE $MSK_IMPACT_DATA_HOME $MSK_HEMEPACT_DATA_HOME $MSK_ACCESS_DATA_HOME
if [ $? -gt 0 ] ; then
    echo "MSKSOLIDHEME merge failed! Study will not be updated in the portal."
    echo $(date)
    # we rollback/clean git after the import of MSKSOLIDHEME (if merge or import fails)
else
    echo "MSKSOLIDHEME merge successful! Creating cancer type case lists..."
    echo $(date)
    # add metadata headers and overrides before importing
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
    $PYTHON_BINARY $PORTAL_HOME/scripts/add_clinical_attribute_metadata_headers.py -s mskimpact -f $MSK_SOLID_HEME_DATA_HOME/data_clinical_patient.txt -i $PORTAL_HOME/scripts/cdm_metadata.json
    if [ $? -gt 0 ] ; then
        echo "Error: Adding metadata headers for MSKSOLIDHEME failed! Study will not be updated in portal."
    fi
    addCancerTypeCaseLists $MSK_SOLID_HEME_DATA_HOME "mskimpact" "data_clinical_sample.txt" "data_clinical_patient.txt"
    # Merge CDM timeline files
    sh $PORTAL_HOME/scripts/merge-cdm-timeline-files.sh mskimpact
    if [ $? -gt 0 ] ; then
        echo "Error: CDM timeline file merge for MSKSOLIDHEME"
    fi
fi

# TODO 