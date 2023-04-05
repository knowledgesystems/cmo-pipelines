# UTILITY FOR SUBSETTING IMPACT DATA

# (1): study id
# (2): output directory
# (3): input data directory
# (4): data filter criteria to subset IMPACT data with (either SEQ_DATE or <ATTRIBUTE_NAME>=[ATTRIBUTE_VAL1,ATTRIBUTE_VAL2,...])
# (5): output subset filename
# (6): data_clinical filename containing attribute being filtered in (4)

for i in "$@"; do
case $i in
    -i=*|--study-id=*)
    STUDY_ID="${i#*=}"
    shift # past argument=value
    ;;
    -o=*|--output-directory=*)
    OUTPUT_DIRECTORY="${i#*=}"
    shift # past argument=value
    ;;
    -d=*|--input-directory=*)
    INPUT_DIRECTORY="${i#*=}"
    shift # past argument=value
    ;;
    -f=*|--filter-criteria=*)
    FILTER_CRITERIA="${i#*=}"
    shift # past argument=value
    ;;
    -s=*|--subset-filename=*)
    SUBSET_FILENAME="${i#*=}"
    shift # past argument=value
    ;;
    -p=*|--portal-scripts-directory=*)
    PORTAL_SCRIPTS_DIRECTORY="${i#*=}"
    shift # past argument=value
    ;;
    -c=*|--clinical-filename=*)
    CLINICAL_FILENAME="${i#*=}"
    shift # past argument=value
    ;;
    *)
      # default option
      echo "This option does not exist!  " "${i#*=}"
    ;;
esac
done
echo "Input arguments: "
echo -e "\tSTUDY_ID="$STUDY_ID
echo -e "\tOUTPUT_DIRECTORY="$OUTPUT_DIRECTORY
echo -e "\tINPUT_DIRECTORY="$INPUT_DIRECTORY
echo -e "\tFILTER_CRITERIA="$FILTER_CRITERIA
echo -e "\tSUBSET_FILENAME="$SUBSET_FILENAME
echo -e "\tCLINICAL_FILENAME="$CLINICAL_FILENAME
if [ -z $PORTAL_SCRIPTS_DIRECTORY ]; then
    PORTAL_SCRIPTS_DIRECTORY="$PORTAL_HOME/scripts"
fi
echo -e "\tPORTAL_SCRIPTS_DIRECTORY="$PORTAL_SCRIPTS_DIRECTORY

# status flags
GEN_SUBSET_LIST_FAILURE=0
MERGE_SCRIPT_FAILURE=0
ADD_METADATA_HEADERS_FAILURE=0

if [ $STUDY_ID == "genie" ]; then
    # in the case of genie data, the input data directory must be the mskimpact data home, where we expect to see ddp_naaccr.txt
    # copy the ddp genie files to the output directory with different filenames
    cp $INPUT_DIRECTORY/ddp/ddp_naaccr.txt $OUTPUT_DIRECTORY/data_clinical_supp_patient.txt
    cut -f1,2 $CLINICAL_FILENAME | grep -v "^#" > $OUTPUT_DIRECTORY/data_clinical_supp_sample.txt

    # the contents of the cvr/seq_date.txt from IMPACT and HEMEPACT must be merged for subsetting to be done correctly by the sample date of sequencing
    # it is assumed that the data directories of mskimpact and hemepact are known - confirm that these globals are known
    if [[  -z "$MSK_IMPACT_DATA_HOME" || -z "$MSK_HEMEPACT_DATA_HOME" ]] ; then
        echo "Data directories for MSKIMPACT AND HEMEPACT must be known for GENIE subset to work. Please make sure 'MSK_IMPACT_DATA_HOME' and 'MSK_HEMEPACT_DATA_HOME' exist and are valid directories in your system environment."
        exit 2
    fi
    # confirm that cvr sub-directory exists in the input directory before attempting to merge contents of IMPACT and HEMEPACT seq_date.txt files
    if [ ! -d $INPUT_DIRECTORY/cvr ] ; then
        mkdir $INPUT_DIRECTORY/cvr
    fi
    head -1 $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt > $INPUT_DIRECTORY/cvr/seq_date.txt; cat $MSK_IMPACT_DATA_HOME/cvr/seq_date.txt $MSK_HEMEPACT_DATA_HOME/cvr/seq_date.txt | grep -v SAMPLE_ID >> $INPUT_DIRECTORY/cvr/seq_date.txt

    # run the generate clinical subset script to generate list of sample ids to subset from impact data - subset of sample ids will be written to given $SUBSET_FILENAME
    # supp clinical sample and supp clinical patient files will be filtered to the samples/patients meeting the SEQ_DATE filter criteria
    echo "Generating subset list from $INPUT_DIRECTORY/cvr/seq_date.txt using filter criteria $FILTER_CRITERIA..."
    $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/generate-clinical-subset.py --study-id="genie" --clinical-file="$OUTPUT_DIRECTORY/data_clinical_supp_sample.txt" --clinical-supp-file="$INPUT_DIRECTORY/cvr/seq_date.txt" --filter-criteria="$FILTER_CRITERIA" --subset-filename="$SUBSET_FILENAME" --anonymize-date='true' --clinical-patient-file="$OUTPUT_DIRECTORY/data_clinical_supp_patient.txt"
    if [ $? -gt 0 ] ; then
        GEN_SUBSET_LIST_FAILURE=1
    else
        # starting in Nov 2018 releases, all vital status information will be removed from patient file and placed in a separate file:
        # get the patients from the filtered set of patients from the generate-clinical-subset.py call and expand file with the vital status columns
        cut -f1 $OUTPUT_DIRECTORY/data_clinical_supp_patient.txt > $OUTPUT_DIRECTORY/vital_status.txt
        $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/expand-clinical-data.py --study-id="genie" --clinical-file="$OUTPUT_DIRECTORY/vital_status.txt" --clinical-supp-file="$INPUT_DIRECTORY/ddp/ddp_vital_status.txt" --fields="YEAR_CONTACT,YEAR_DEATH,INT_CONTACT,INT_DOD,DEAD" --identifier-column-name="PATIENT_ID"
        if [ $? -gt 0 ] ; then
            echo "Failed to expand $OUTPUT_DIRECTORY/vital_status.txt with YEAR_CONTACT, YEAR_DEATH, INT_CONTACT, INT_DOD, DEAD from $INPUT_DIRECTORY/ddp/ddp_vital_status.txt. Exiting..."
            exit 2
        fi
        # expand data_clinical_supp_sample.txt with ONCOTREE_CODE, SAMPLE_TYPE, GENE_PANEL from data_clinical.txt
        echo "Expanding sample clinical data with ONCOTREE_CODE,SAMPLE_TYPE,GENE_PANEL from $INPUT_DIRECTORY/data_clinical_sample.txt"
        $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/expand-clinical-data.py --study-id="genie" --clinical-file="$OUTPUT_DIRECTORY/data_clinical_supp_sample.txt" --clinical-supp-file="$INPUT_DIRECTORY/data_clinical_sample.txt" --fields="ONCOTREE_CODE,SAMPLE_TYPE,GENE_PANEL" --identifier-column-name="SAMPLE_ID"
        if [ $? -gt 0 ] ; then
            echo "Failed to expand $OUTPUT_DIRECTORY/data_clinical_supp_sample.txt with ONCOTREE_CODE, SAMPLE_TYPE, GENE_PANEL from $INPUT_DIRECTORY/data_clinical_sample.txt. Exiting..."
            exit 2
        fi

        # add age at seq report using cvr/seq_date.txt
        echo "Adding AGE_AT_SEQ_REPORT to $OUTPUT_DIRECTORY/data_clinical_supp_sample.txt"
        $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/add-age-at-seq-report.py --clinical-output-file="$OUTPUT_DIRECTORY/data_clinical_supp_sample.txt" --clinical-sample-file="$INPUT_DIRECTORY/data_clinical_sample.txt" --convert-to-days="true"
        if [ $? -gt 0 ] ; then
            echo "Failed to add AGE_AT_SEQ_REPORT to $OUTPUT_DIRECTORY/data_clinical_supp_sample.txt using $INPUT_DIRECTORY/data_clinical_sample.txt. Exiting..."
            exit 2l
        fi

        # rename GENE_PANEL to SEQ_ASSAY_ID in data_clinical_supp_sample.txt
        sed -i.bak 's/GENE_PANEL/SEQ_ASSAY_ID/' $OUTPUT_DIRECTORY/data_clinical_supp_sample.txt
        # remove temp files created
        rm $OUTPUT_DIRECTORY/*.bak

        # generate subset of impact data using the subset file generated above
        echo "Subsetting data from $INPUT_DIRECTORY..."
        $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/merge.py  -d $OUTPUT_DIRECTORY -i "genie" -s "$SUBSET_FILENAME" -x "true" $INPUT_DIRECTORY
        if [ $? -gt 0 ] ; then
            MERGE_SCRIPT_FAILURE=1
        else
            # remove germline mutations from maf
            grep -v "GERMLINE" $OUTPUT_DIRECTORY/data_mutations_extended.txt > $OUTPUT_DIRECTORY/data_mutations_extended.txt.tmp
            grep "^Hugo_Symbol" $OUTPUT_DIRECTORY/data_mutations_extended.txt.tmp > $OUTPUT_DIRECTORY/data_mutations_extended.txt
            grep -E -v "^#|^Hugo_Symbol" $OUTPUT_DIRECTORY/data_mutations_extended.txt.tmp | sort | uniq >> $OUTPUT_DIRECTORY/data_mutations_extended.txt
        fi
    fi
else
    # touch meta clinical if not already exists only if input dir has data_clinical.txt
    if [[ -f $INPUT_DIRECTORY/data_clinical.txt && ! -f $INPUT_DIRECTORY/meta_clinical.txt ]] ; then
        touch $INPUT_DIRECTORY/meta_clinical.txt
    fi

    # generate subset list of sample ids based on filter criteria and subset MSK-IMPACT using generated list in $SUBSET_FILENAME
    echo "Generating subset list from $CLINICAL_FILENAME using filter criteria $FILTER_CRITERIA..."
    $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/generate-clinical-subset.py --study-id="$STUDY_ID" --clinical-file="$CLINICAL_FILENAME" --filter-criteria="$FILTER_CRITERIA" --subset-filename="$SUBSET_FILENAME"
    if [ $? -gt 0 ]; then
        GEN_SUBSET_LIST_FAILURE=1
    else
        echo "Subsetting data from $INPUT_DIRECTORY..."
        $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/merge.py -d $OUTPUT_DIRECTORY -i "$STUDY_ID" -s "$SUBSET_FILENAME" -x "true" -m "true" $INPUT_DIRECTORY
        if [ $? -gt 0 ]; then
            MERGE_SCRIPT_FAILURE=1
        else
            # add clinical meta data headers if clinical sample file exists
            if [ -f $OUTPUT_DIRECTORY/data_clinical_sample.txt ]; then
                echo "Adding clinical attribute meta data headers..."
                $PYTHON_BINARY $PORTAL_SCRIPTS_DIRECTORY/add_clinical_attribute_metadata_headers.py -f $OUTPUT_DIRECTORY/data_clinical*
                if [ $? -gt 0 ]; then
                    ADD_METADATA_HEADERS_FAILURE=1
                fi
            fi
        fi
    fi

    # remove temp meta_clinical.txt if created
    if [[ -f $INPUT_DIRECTORY/meta_clinical.txt && $(wc -l < $INPUT_DIRECTORY/meta_clinical.txt) -eq 0 ]] ; then
        rm $INPUT_DIRECTORY/meta_clinical.txt
    fi
fi
# report errors
if [ $GEN_SUBSET_LIST_FAILURE -ne 0 ] ; then
    echo "Error while attempting to generate subset of sample ids by filter criteria $FILTER_CRITERIA"
fi
if [ $MERGE_SCRIPT_FAILURE -ne 0 ] ; then
    echo "Error while trying to subset data from $INPUT_DIRECTORY using subset list from $SUBSET_FILENAME"
fi
if [ $ADD_METADATA_HEADERS_FAILURE -ne 0 ] ; then
    echo "Error while attempting to add clinical attribute meta data headers to $OUTPUT_DIRECTORY/data_clinical*"
fi
# exit accordingly
if [[ $GEN_SUBSET_LIST_FAILURE -eq 0 && $MERGE_SCRIPT_FAILURE -eq 0 && $ADD_METADATA_HEADERS_FAILURE -eq 0 ]] ; then
    echo "Successfully subset data from $INPUT_DIRECTORY by filter criteria $FILTER_CRITERIA"
    exit 0
else
    exit 1
fi
