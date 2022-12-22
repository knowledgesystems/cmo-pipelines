#!/usr/bin/env bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/oncokb-annotator.sh"
(
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        exit 0
    fi

    source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

    source $PORTAL_HOME/scripts/set-data-source-environment-vars.sh

    DELIVERED_FILE_LIST="""
        data_clinical_sample.oncokb.pdf
        data_clinical_sample.oncokb.txt.gz
        data_clinical_sample_somatic.oncokb.pdf
        data_clinical_sample_somatic.oncokb.txt.gz
        data_CNA.oncokb.txt.gz
        data_mutations_extended.oncokb.txt.gz
        data_mutations_extended_somatic.oncokb.txt.gz
        data_sv.oncokb.txt.gz
        README.md
    """

    function send_failure_messages() {
        email_message=$1
        email_subject=$2
        slack_message=$3
        echo $email_message
        echo -e "$email_message" |  mail -s "$email_subject" $PIPELINES_EMAIL_LIST
        sendPreImportFailureMessageMskPipelineLogsSlack "$slack_message"
    }

    function oncokb_annotator_code_is_up_to_date() {
        code_dir="$ONCOKB_ANNOTATOR_HOME"
        git -C "$code_dir" fetch
        diff_output=$(git -C "$code_dir" diff origin/master | head -n 1)
        if [ -z "$diff_output" ] ; then
            return 0
        fi
        return 1
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

    function gzip_oncokb_annotated_files() {
        echo $(date)
        echo "Gzipping data files for OncoKB Annotation for MSKSOLIDHEME"
        if ! gzip -7 --force $ONCOKB_OUT_DIR/data*.txt ; then
            message="Error during attempt to gzip $ONCOKB_OUT_DIR data files"
            send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
            exit 1
        fi
    }

    function rsync_latest_oncokb_annotated_msk_impact_to_clone() {
        echo $(date)
        echo "Rsyncing data files to clone of oncokb-annotated-msk-impact git repository"
        rsync --exclude "/data/portal-cron/git-repos/oncokb-annotated-msk-impact/.git" -a $ONCOKB_OUT_DIR/ $ONCOKB_ANNOTATED_GIT_DIR
    }

    function commit_and_push_latest_oncokb_annotated_msk_impact() {
        echo $(date)
        echo "Committing (amending) and pushing data file changes to git repository"
        wd=$(pwd)
        cd $ONCOKB_ANNOTATED_GIT_DIR
        # Create and push a git amended changeset
        for filename in $DELIVERED_FILE_LIST ; do
            if ! git add $filename ; then
                message="Error during attempt to 'git add $filename' in repository oncokb-annotated-msk-impact"
                send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
                exit 1
            fi
        done
        if ! git commit --amend --message "latest oncokb annotations of mskimpact" ; then
            message="Error occurred during 'git commit --amend' ..."
            send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
            exit 1
        fi
        if ! git push --force ; then
            message="Error occurred during 'git push --force'"
            send_failure_messages "$message" "oncokb-annotator failed to run." "$message"
            exit 1
        fi
        cd $wd
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
    ONCOKB_ANNOTATED_GIT_DIR="/data/portal-cron/git-repos/oncokb-annotated-msk-impact"

    # scripts
    MAF_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/MafAnnotator.py"
    SV_ANNOTATOR_SCRIPT="$ONCOKB_ANNOTATOR_HOME/StructuralVariantAnnotator.py"
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

    SOURCE_SV_FILE="$MSK_SOLID_HEME_DATA_HOME/data_sv.txt"
    STAGING_SV_FILE="$STAGING_DIR/data_sv.txt"
    ONCOKB_SV_FILE="$ONCOKB_OUT_DIR/data_sv.oncokb.txt"

    SOURCE_CNA_FILE="$MSK_SOLID_HEME_DATA_HOME/data_CNA.txt"
    STAGING_CNA_FILE="$STAGING_DIR/data_CNA.txt"
    ONCOKB_CNA_FILE="$ONCOKB_OUT_DIR/data_CNA.oncokb.txt"

    ONCOKB_PDF_NUMBER=100
    ONCOKB_PDF_FILE="$ONCOKB_OUT_DIR/data_clinical_sample.oncokb.pdf"
    ONCOKB_SOMATIC_PDF_FILE="$ONCOKB_OUT_DIR/data_clinical_sample_somatic.oncokb.pdf"

    ONCOKB_ANNOTATOR_README_FILE="$ONCOKB_OUT_DIR/README"
    GITHUB_README_FILE="$ONCOKB_OUT_DIR/README.md"

    # Make sure necessary environment variables are set before running
    if [ -z $PYTHON3_BINARY ] || [ -z $GIT_BINARY ] || [ -z $ONCOKB_ANNOTATOR_HOME ] || [ -z $DMP_DATA_HOME ] || [ -z $MSK_SOLID_HEME_DATA_HOME ] || [ -z $ONCOKB_TOKEN_FILE ] || [ -z $PORTAL_HOME ] ; then
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

    # check if oncokb_annotator code is up to date and log if not
    if ! oncokb_annotator_code_is_up_to_date ; then
        echo "WARNING : oncokb_annotator code repository clone is out of date at $ONCOKB_ANNOTATOR_HOME"
    fi

    # Pause, up to some time limit for dmp imports to complete
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Checking that dmp import pipeline run is complete..."
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
        cp -a $SOURCE_CNA_FILE $STAGING_CNA_FILE
        cp -a $SOURCE_SV_FILE $STAGING_SV_FILE
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

        $PYTHON3_BINARY $MAF_ANNOTATOR_SCRIPT -b $ONCOKB_TOKEN -i $STAGING_MAF_FILE -o $ONCOKB_MAF_FILE -c $STAGING_SAMPLE_FILE -a $PREVIOUS_ANNOTATED_MAF_ARGS
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate MAF, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi

        # if a previous annotated maf was created during this step then remove it from directory
        if [ -f "$ONCOKB_PREVIOUS_MAF_FILE" ] ; then
            rm -f $ONCOKB_PREVIOUS_MAF_FILE
        fi
    fi

    # Annotating SV
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning SV annotation..."
        $PYTHON3_BINARY $SV_ANNOTATOR_SCRIPT -b ONCOKB_TOKEN -i STAGING_SV_FILE -o ONCOKB_SV_FILE -c $STAGING_SAMPLE_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate SV file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Annotating CNA
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning CNA annotation..."
        $PYTHON3_BINARY $CNA_ANNOTATOR_SCRIPT -b $ONCOKB_TOKEN -i $STAGING_CNA_FILE -o $ONCOKB_CNA_FILE -c $STAGING_SAMPLE_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate CNA file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Annotating Clinical
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning clinical annotation..."
        $PYTHON3_BINARY $CLINICAL_ANNOTATOR_SCRIPT -i $STAGING_SAMPLE_FILE -o $ONCOKB_SAMPLE_FILE -a $ONCOKB_MAF_FILE,$ONCOKB_CNA_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate clinical file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating Clinical PDF
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning clinical PDF generation..."
        $PYTHON3_BINARY $PDF_SCRIPT -i $ONCOKB_SAMPLE_FILE -o $ONCOKB_PDF_FILE -n $ONCOKB_PDF_NUMBER
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
        $PYTHON3_BINARY $CLINICAL_ANNOTATOR_SCRIPT -i $STAGING_SAMPLE_FILE -o $ONCOKB_SOMATIC_SAMPLE_FILE -a $ONCOKB_SOMATIC_MAF_FILE,$ONCOKB_CNA_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to annotate somatic clinical file, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating Somatic Clinical PDF
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning somatic clinical PDF generation..."
        $PYTHON3_BINARY $PDF_SCRIPT -i $ONCOKB_SOMATIC_SAMPLE_FILE -o $ONCOKB_SOMATIC_PDF_FILE -n $ONCOKB_PDF_NUMBER
        if [ $? -ne 0 ] ; then
            echo "Failed to generate somatic clinical pdf, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        fi
    fi

    # Generating README
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        echo $(date)
        echo "Beginning README generation..."
        $PYTHON3_BINARY $README_SCRIPT -o $ONCOKB_ANNOTATOR_README_FILE
        if [ $? -ne 0 ] ; then
            echo "Failed to generate README, exiting..."
            ONCOKB_ANNOTATION_SUCCESS=0
        else
            echo '# MSK Clinical Sequencing Cohort (MSKCC)' > $GITHUB_README_FILE
            echo >> $GITHUB_README_FILE
            echo 'Please follow the [publication guidelines](http://cmo.mskcc.org/cmo/initiatives/msk-impact/) when using these data in abstracts or journal articles. Manuscripts involving pan-cancer and other large analyses will undergo a biostatistical review prior to submission. If you would like assistance identifying a collaborator in Biostatistics, please contact [Mithat Gonen (gonenm@mskcc.org)](mailto:Mithat%20Gonen%3cgonenm@mskcc.org%3e). For questions regarding the results shown here, please contact [Mike Berger (bergerm1@mskcc.org)](mailto:Mike%20Berger%3cbergerm1@mskcc.org%3e), [Ahmet Zehir (zehira@mskcc.org)](mailto:Ahmet%20Zehir%3czehira@mskcc.org%3e), or [Nikolaus Schultz (schultzn@mskcc.org)](mailto:Nikolaus%20Schultz%3cschultzn@mskcc.org%3e").' >> $GITHUB_README_FILE
            echo >> $GITHUB_README_FILE
            echo '### :warning: These data are available to MSK investigators only and are not to be published or shared with anyone outside of MSK without permission.' >> $GITHUB_README_FILE
            echo >> $GITHUB_README_FILE
            cat $ONCOKB_ANNOTATOR_README_FILE >> $GITHUB_README_FILE
            echo >> $GITHUB_README_FILE
            rm $ONCOKB_ANNOTATOR_README_FILE
        fi
    fi

    # Only commit if all steps succeeded
    if [ $ONCOKB_ANNOTATION_SUCCESS -eq 1 ] ; then
        gzip_oncokb_annotated_files
        rsync_latest_oncokb_annotated_msk_impact_to_clone
        commit_and_push_latest_oncokb_annotated_msk_impact
    else
        message="OncoKB Annotation failed for MSKSOLIDHEME. Check logs for more details."
        send_failure_messages "$message" "MSKSOLIDHEME OncoKB Annotation Failure" "MSKSOLIDHEME OncoKB Annotation"
    fi

    # Cleanup copied staging files if they exist
    rm -f $STAGING_SAMPLE_FILE
    rm -f $STAGING_CNA_FILE
    rm -f $STAGING_SV_FILE
    rm -f $STAGING_MAF_FILE

    echo $(date)
    echo "OncoKB annotations complete"
) {my_flock_fd}>$FLOCK_FILEPATH

exit 0
