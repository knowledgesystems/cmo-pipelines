#!/usr/bin/env bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/oncokb-annotator.sh"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        exit 0
    fi

    source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

    source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh

    function send_failure_messages () {
        email_message=$1
        email_subject=$2
        slack_message=$3
        echo $email_message
        echo -e "$email_message" |  mail -s "$email_subject" $PIPELINES_EMAIL_LIST
        sendPreImportFailureMessageMskPipelineLogsSlack "$slack_message"
    }

    function git_clone_is_up_to_date () {
        clone_dir=$1
        git -C "$clone_dir" fetch
        diff_output=$(git -C "$clone_dir" origin/master | head -n 1)
        if [ -z "$diff_output" ] ; then
            return 0
        fi
        return 1
    }

    function gzip_oncokb_annotated_files () {
        if ! gzip -7 --force $ONCOKB_OUT_DIR/data*.txt ; then
            message="Error during attempt to gzip $ONCOKB_OUT_DIR data files"
            send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
            exit 1
        fi
    }

    function rsync_oncokb_annotated_files () {
        CREATE_INDEX_SCRIPT="$PORTAL_HOME/scripts/create_web_directory_index_file.sh"
        TMP_DIR="/data/portal-cron/tmp/rsync_oncokb"
        INDEX_FILE="$TMP_DIR/index.html"
        TITLE_STRING="oncokb-annotation-msk-impact"
        BASE_URL="http://download.cbioportal.org/oncokb-annotated-msk-impact"
        REMOTE_HOST="ramen.cbio.mskcc.org"
        REMOTE_DIR="/data/public/oncokb-annotated-msk-impact"
        RSYNC_TARGET="$REMOTE_HOST:$REMOTE_DIR"
        RSYNC_IDENTITY_FILE="/home/cbioportal_importer/.ssh/id_rsa_restful_login_counter_key"
        RSYNC_RSH="--rsh=ssh -i $RSYNC_IDENTITY_FILE"
        $CREATE_INDEX_SCRIPT "$ONCOKB_OUT_DIR" "$TMP_DIR" "$BASE_URL" "$TITLE_STRING"
        rsync -a --rsh="ssh -i $RSYNC_IDENTITY_FILE" "$ONCOKB_OUT_DIR/" "$RSYNC_TARGET"
        rsync -a --rsh="ssh -i $RSYNC_IDENTITY_FILE" "$INDEX_FILE" "$RSYNC_TARGET"
        rm -f "$INDEX_FILE"
    }

    function dmp_import_run_is_happening() {
        CRONTAB_WRAPPER_SCRIPT_KEYWORD='/data/portal-cron/scripts/automation-environment.sh;/data/portal-cron/scripts/fetch-and-import-dmp-data-wrapper.sh'
        running_processes=$(ps aux | grep -v 'grep' | grep $CRONTAB_WRAPPER_SCRIPT_KEYWORD)
        if [ -z "$running_processes" ] ; then
            return 1 # the crontab execution is complete / no longer running
        fi
        return 0 # the crontab execution string is present
    }

    function wait_for_completion_of_dmp_import_run() {
        start_time_seconds=$(date +%s)
        MAXIMUM_WAIT_SECONDS=$1
        quit_time_seconds=$(($start_time_seconds + $MAXIMUM_WAIT_SECONDS))
        PROBE_DELAY_SECONDS=16
        while [ $(date +%s) -lt $quit_time_seconds ] ; do
            if ! dmp_import_run_is_happening ; then
                return 0 # any dmp import run which was in progress is now complete
            fi
            sleep $PROBE_DELAY_SECONDS
        done
        return 1 # completion of import did not happen before timeout
    }

    # retrieve oncokb token from file
    if [ ! -r $ONCOKB_TOKEN_FILE ]; then
        message="Could not run oncokb annotation script: token file '$ONCOKB_TOKEN_FILE' does not exist or is not readable."
        send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
        exit 2
    fi
    ONCOKB_TOKEN=$(cat $ONCOKB_TOKEN_FILE)
    if [ -z $ONCOKB_TOKEN ]; then
        message="Could not run oncokb annotation script: token file '$ONCOKB_TOKEN_FILE' is empty."
        send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
        exit 2
    fi

    # directories
    SOURCE_DIR="/data/portal-cron/cbio-portal-data/dmp/msk_solid_heme/oncokb"
    STAGING_DIR="/data2/portal-cron/postprocessing/msk_solid_heme"
    ONCOKB_OUT_DIR="$STAGING_DIR/oncokb"

    # scripts
    MAF_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/MafAnnotator.py"
    FUSION_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/FusionAnnotator.py"
    CNA_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/CnaAnnotator.py"
    CLINICAL_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/ClinicalDataAnnotator.py"
    PDF_SCRIPT="$ONCOKB_ANNOTATOR_HOME/OncoKBPlots.py"
    README_SCRIPT="$ONCOKB_ANNOTATOR_HOME/GenerateReadMe.py"

    # data files and parameters
    SOURCE_SAMPLE_FILE="$MSK_SOLID_HEME_DATA_HOME/data_clinical_sample.txt"
    STAGING_SAMPLE_FILE="$STAGING_DIR/data_clinical_sample.txt"
    ONCOKB_SAMPLE_FILE="$ONCOKB_OUT_DIR/data_clinical_sample.oncokb.txt"
    ONCOKB_SOMATIC_SAMPLE_FILE="$ONCOKB_OUT_DIR/data_clinical_sample_somatic.oncokb.txt"

    SOURCE_MAF_FILE="$MSK_SOLID_HEME_DATA_HOME/data_mutations_extended.txt"
    STAGING_MAF_FILE="$STAGING_DIR/data_mutations_extended.txt"
    ONCOKB_MAF_FILE="$ONCOKB_OUT_DIR/data_mutations_extended.oncokb.txt"
    ONCOKB_MAF_FILE_ZIPPED="${ONCOKB_MAF_FILE}.gz"
    ONCOKB_PREVIOUS_MAF_FILE="$ONCOKB_OUT_DIR/data_mutations_extended.oncokb.previous.txt"
    ONCOKB_SOMATIC_MAF_FILE="$ONCOKB_OUT_DIR/data_mutations_extended_somatic.oncokb.txt"

    SOURCE_FUSION_FILE="$MSK_SOLID_HEME_DATA_HOME/data_fusions.txt"
    STAGING_FUSION_FILE="$STAGING_DIR/data_fusions.txt"
    ONCOKB_FUSION_FILE="$ONCOKB_OUT_DIR/data_fusions.oncokb.txt"

    SOURCE_CNA_FILE="$MSK_SOLID_HEME_DATA_HOME/data_CNA.txt"
    STAGING_CNA_FILE="$STAGING_DIR/data_CNA.txt"
    ONCOKB_CNA_FILE="$ONCOKB_OUT_DIR/data_CNA.oncokb.txt"

    ONCOKB_PDF_NUMBER=100
    ONCOKB_PDF_FILE="$ONCOKB_OUT_DIR/data_clinical_sample.oncokb.pdf"
    ONCOKB_SOMATIC_PDF_FILE="$ONCOKB_OUT_DIR/data_clinical_sample_somatic.oncokb.pdf"

    ONCOKB_README_FILE="$ONCOKB_OUT_DIR/README"

    # Make sure necessary environment variables are set before running
    if [ -z $PYTHON_BINARY ] || [ -z $GIT_BINARY ] || [ -z $ONCOKB_ANNOTATOR_HOME ] || [ -z $DMP_DATA_HOME ] || [ -z $MSK_SOLID_HEME_DATA_HOME ] || [ -z $ONCOKB_URL ] || [ -z $CANCER_HOTSPOTS_URL ] || [ -z $ONCOKB_TOKEN_FILE ] || [ -z $PORTAL_HOME ] ; then
        message="Could not run oncokb annotation script: automation-environment.sh script must be run in order to set needed environment variables."
        send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
        exit 2
    fi

    REANNOTATE_MUTATIONS="false"
    # parse input arguments
    for i in "$@"; do
        case $i in
        -rm|--reannotate-mutations)
            REANNOTATE_MUTATIONS="true"
            shift
            ;;
        *)
            echo "error : unrecognized argument: ${i}" 1>&2
            exit 1
            ;;
        esac
    done

    echo $(date)
    echo "Staring oncokb-annotator run on $SOURCE_DIR"
    ONCOKB_ANNOTATION_SUCCESS=1

    # check if oncokb_annotator clone is up to date and log if not
    if ! git_clone_is_up_to_date "$ONCOKB_ANNOTATOR_HOME" ; then
        echo "WARNING : repository clone is out of date at $ONCOKB_ANNOTATOR_HOME"
    fi

    # Pause, up to some time limit for dmp imports to complete
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Checking that dmp import pipeline run in complete..."
        MAXIMUM_WAIT_SECONDS=$(( 6 * 3600 )) # 6 hours wait time
        wait_for_completion_of_dmp_import_run $MAXIMUM_WAIT_SECONDS
        if [ $? -ne 0 ] ; then
            echo "Failed to wait long enough for the dmp import pipeline run to complete, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Update DMP repo before running annotations
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Updating the DMP source repository..."
        fetch_updates_in_data_sources dmp
        if [ ${#failed_data_source_fetches[*]} -ne 0 ] ; then
            echo "Failed to update the DMP source repository, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Copying needed files to staging directory (to avoid future interference if dmp import runs while this script runs)
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Initializing working directory..."
        mkdir -p $STAGING_DIR
        mkdir -p $ONCOKB_OUT_DIR
        cp -a $SOURCE_SAMPLE_FILE $STAGING_SAMPLE_FILE
        cp -a $SOURCE_FUSION_FILE $STAGING_FUSION_FILE
        cp -a $SOURCE_CNA_FILE $STAGING_CNA_FILE
        cp -a $SOURCE_MAF_FILE $STAGING_MAF_FILE # maf is last because it takes the longest to copy
    fi

    # Annotating MAF
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning MAF annotation..."
        # if an oncokb annotated maf already exists then create a copy that can be used to speed up the annotation process
        PREVIOUS_ANNOTATED_MAF_ARGS=""
        if [ $REANNOTATE_MUTATIONS == "false" ] && [ -f "$ONCOKB_MAF_FILE_ZIPPED" ] ; then
            echo "Unzipping previously annotated MAF..."
            if gunzip --force "$ONCOKB_MAF_FILE_ZIPPED" ; then
                mv $ONCOKB_MAF_FILE $ONCOKB_PREVIOUS_MAF_FILE
                PREVIOUS_ANNOTATED_MAF_ARGS="-p $ONCOKB_PREVIOUS_MAF_FILE"
            else
                echo "WARNING : unable to unzip previously oncokb annotated maf ... reannotating all records"
            fi
        fi

        $PYTHON_BINARY $MAF_ANNOTATOR_SCRIPT -b $ONCOKB_TOKEN -i $STAGING_MAF_FILE -o $ONCOKB_MAF_FILE -c $STAGING_SAMPLE_FILE -u $ONCOKB_URL -v $CANCER_HOTSPOTS_URL $PREVIOUS_ANNOTATED_MAF_ARGS
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate MAF, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi

        # if a previous annotated maf was created during this step then remove it from directory
        if [ -f "$ONCOKB_PREVIOUS_MAF_FILE" ] ; then
            rm -f $ONCOKB_PREVIOUS_MAF_FILE
        fi
    fi

    # Annotating Fusions
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning fusions annotation..."
        $PYTHON_BINARY $FUSION_ANNOTATOR_SCRIPT -b $ONCOKB_TOKEN -i $STAGING_FUSION_FILE -o $ONCOKB_FUSION_FILE -c $STAGING_SAMPLE_FILE -u $ONCOKB_URL
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate fusion file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Annotating CNA
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning CNA annotation..."
        $PYTHON_BINARY $CNA_ANNOTATOR_SCRIPT -b $ONCOKB_TOKEN -i $STAGING_CNA_FILE -o $ONCOKB_CNA_FILE -c $STAGING_SAMPLE_FILE -u $ONCOKB_URL
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate CNA file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Annotating Clinical
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning clinical annotation..."
        $PYTHON_BINARY $CLINICAL_ANNOTATOR_SCRIPT -i $STAGING_SAMPLE_FILE -o $ONCOKB_SAMPLE_FILE -a $ONCOKB_MAF_FILE,$ONCOKB_FUSION_FILE,$ONCOKB_CNA_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate clinical file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating Clinical PDF
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning clinical PDF generation..."
        $PYTHON_BINARY $PDF_SCRIPT -i $ONCOKB_SAMPLE_FILE -o $ONCOKB_PDF_FILE -n $ONCOKB_PDF_NUMBER
        if [ $? -ne 0 ] ; then
            echo "Failed to generate clinical pdf, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Annotating Somatic Clinical
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning somatic clinical annotation..."
        # Generate somatic-only MAF by excluding lines including 'GERMLINE'
        awk -F'\t' '$26 != "GERMLINE"' $ONCOKB_MAF_FILE > $ONCOKB_SOMATIC_MAF_FILE
        $PYTHON_BINARY $CLINICAL_ANNOTATOR_SCRIPT -i $STAGING_SAMPLE_FILE -o $ONCOKB_SOMATIC_SAMPLE_FILE -a $ONCOKB_SOMATIC_MAF_FILE,$ONCOKB_FUSION_FILE,$ONCOKB_CNA_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate somatic clinical file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating Somatic Clinical PDF
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning somatic clinical PDF generation..."
        $PYTHON_BINARY $PDF_SCRIPT -i $ONCOKB_SOMATIC_SAMPLE_FILE -o $ONCOKB_SOMATIC_PDF_FILE -n $ONCOKB_PDF_NUMBER
        if [ $? -ne 0 ] ; then
            echo "Failed to generate somatic clinical pdf, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating README
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning README generation..."
        $PYTHON_BINARY $README_SCRIPT -o $ONCOKB_README_FILE -u $ONCOKB_URL
        if [ $? -ne 0 ] ; then
            echo "Failed to generate README, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Only commit if all steps succeeded
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Gzipping data files for OncoKB Annotation for MSKSOLIDHEME"
        gzip_oncokb_annotated_files
        echo $(date)
        echo "Running rsync to download.cbioportal.org for OncoKB Annotation for MSKSOLIDHEME"
        rsync_oncokb_annotated_files
    else
        message="OncoKB Annotation failed for MSKSOLIDHEME. Check logs for more details."
        send_failure_messages "$message" "MSKSOLIDHEME OncoKB Annotation Failure" "MSKSOLIDHEME OncoKB Annotation"
    fi

    # Cleanup copied staging files if they exist
    rm -f $STAGING_SAMPLE_FILE
    rm -f $STAGING_FUSION_FILE
    rm -f $STAGING_CNA_FILE
    rm -f $STAGING_MAF_FILE

    echo $(date)
    echo "OncoKB annotations complete"
) {my_flock_fd}>$FLOCK_FILEPATH

exit 0
