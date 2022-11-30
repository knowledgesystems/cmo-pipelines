#!/usr/bin/env bash

export AZ_DATA_HOME=$PORTAL_DATA_HOME/az-msk-impact-2022
export AZ_MSK_IMPACT_DATA_HOME=$AZ_DATA_HOME/mskimpact
export AZ_TMPDIR=$AZ_DATA_HOME/tmp

source $PORTAL_HOME/scripts/dmp-import-vars-functions.sh

function pull_latest_data_from_az_git_repo() {
    (   # executed in a subshell to avoid changing the actual working directory
        # if any statement fails, the return value of the entire expression is the failure status
        cd $AZ_DATA_HOME &&
        $GIT_BINARY fetch &&
        $GIT_BINARY reset origin/main --hard &&
        $GIT_BINARY lfs pull &&
        $GIT_BINARY clean -f -d
    )
}

function push_updates_to_az_git_repo() {
    (   # executed in a subshell to avoid changing the actual working directory
        # if any statement fails, the return value of the entire expression is the failure status
        cd $AZ_DATA_HOME &&
        $GIT_BINARY add * &&
        $GIT_BINARY commit -m "Latest AstraZeneca MSK-IMPACT dataset" &&
        $GIT_BINARY push origin
    )
}

# 1. Pull latest from AstraZeneca repo (mskcc/az-msk-impact-2022)
printTimeStampedDataProcessingStepMessage "pull of AstraZeneca data updates to git repository"

if ! pull_latest_data_from_az_git_repo ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "GIT PULL (az-msk-impact-2022) :fire: - address ASAP!"

    EMAIL_BODY="Failed to pull AstraZeneca incoming changes from Git - address ASAP!"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "[URGENT] GIT PULL FAILURE" $PIPELINES_EMAIL_LIST

    exit 1
fi

# ------------------------------------------------------------------------------------------------------------------------
# 2. Copy data from local clone of MSK Solid Heme repo to local clone of AZ repo

# Create temporary directory to store data before subsetting
if ! [ -d "$AZ_TMPDIR" ] ; then
    if ! mkdir -p "$AZ_TMPDIR" ; then
        echo "Error : could not create tmp directory '$AZ_TMPDIR'" >&2
        exit 1
    fi
fi
if [[ -d "$AZ_TMPDIR" && "$AZ_TMPDIR" != "/" ]] ; then
    rm -rf "$AZ_TMPDIR"/*
fi

cp -a $MSK_SOLID_HEME_DATA_HOME/* $AZ_TMPDIR

if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to copy MSK-IMPACT data to AstraZeneca repo. Skipping subset, merge, and update of AstraZeneca MSK-IMPACT!"
    sendPreImportFailureMessageMskPipelineLogsSlack "Copy MSK-IMPACT data to AstraZeneca repo"

    EMAIL_BODY="Failed to copy MSK-IMPACT data to AstraZeneca repo. Subset study will not be updated."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "MSK-IMPACT Data Copy Failure: Study will not be updated." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi

# ------------------------------------------------------------------------------------------------------------------------
# 3. Remove Part A non-consented patients + samples
printTimeStampedDataProcessingStepMessage "subset and merge of MSK-IMPACT Part A Consented patients for AstraZeneca"

# Generate subset of Part A consented patients from MSK-Impact
$PYTHON_BINARY $PORTAL_HOME/scripts/generate-clinical-subset.py \
    --study-id="mskimpact" \
    --clinical-file="$AZ_TMPDIR/data_clinical_patient.txt" \
    --filter-criteria="PARTA_CONSENTED_12_245=YES" \
    --subset-filename="$AZ_TMPDIR/part_a_subset.txt"

if [ $? -gt 0 ] ; then
    echo "ERROR! Failed to generate subset of MSK-IMPACT for AstraZeneca. Skipping merge and update of AstraZeneca MSK-IMPACT!"
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca subset generation from MSK-IMPACT"

    EMAIL_BODY="Failed to subset AstraZeneca MSK-IMPACT data. Subset study will not be updated."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "AstraZeneca MSK-IMPACT Subset Failure: Study will not be updated." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi

# Write out the subsetted data
$PYTHON_BINARY $PORTAL_HOME/scripts/merge.py \
    --study-id="mskimpact" \
    --subset="$AZ_TMPDIR/part_a_subset.txt" \
    --output-directory="$AZ_MSK_IMPACT_DATA_HOME" \
    --merge-clinical="true" \
    $AZ_TMPDIR

if [ $? -gt 0 ] ; then
    echo "Error! Failed to merge subset of MSK-IMPACT for AstraZeneca. Skipping update of AstraZeneca MSK-IMPACT!"
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca subset merge from MSK-IMPACT"

    EMAIL_BODY="Failed to merge subset of MSK-IMPACT for AstraZeneca"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "AstraZeneca MSK-IMPACT Merge Failure: Study will not be updated." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi

# Remove temporary directory now that the subset has been merged
if [[ -d "$AZ_TMPDIR" && "$AZ_TMPDIR" != "/" ]] ; then
    rm -rf "$AZ_TMPDIR" "$AZ_MSK_IMPACT_DATA_HOME/part_a_subset.txt"
fi

# ------------------------------------------------------------------------------------------------------------------------
# 4. Run changelog script
printTimeStampedDataProcessingStepMessage "generate changelog for AstraZeneca MSK-IMPACT updates"

$PYTHON3_BINARY $PORTAL_HOME/scripts/generate_az_study_changelog_py3.py $AZ_MSK_IMPACT_DATA_HOME

if [ $? -gt 0 ] ; then
    echo "Error! Failed to generate changelog summary for AstraZeneca MSK-Impact subset."
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca MSK-IMPACT changelog generation"

    EMAIL_BODY="Failed to generate changelog summary for AstraZeneca MSK-Impact subset"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "AstraZeneca MSK-IMPACT Changelog Failure: Changelog summary will not be provided." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi

# ------------------------------------------------------------------------------------------------------------------------
# 5. Filter germline events from mutation file and structural variant file
printTimeStampedDataProcessingStepMessage "filtering germline events for AstraZeneca MSK-IMPACT updates"

mutation_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
mutation_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $mutation_filepath $mutation_filtered_filepath --event-type mutation
if [ $? -gt 0 ] ; then
    echo "Error! Failed to filter germine events from mutation file for AstraZeneca MSK-Impact subset."
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca MSK-IMPACT mutation event filtering"

    EMAIL_BODY="Failed to filter germine events from mutation file for AstraZeneca MSK-Impact subset."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "AstraZeneca MSK-IMPACT Mutation Germline Filter Failure: Changelog summary will not be provided." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi
mv $mutation_filtered_filepath $mutation_filepath

mutation_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"
mutation_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $mutation_filepath $mutation_filtered_filepath --event-type mutation
if [ $? -gt 0 ] ; then
    echo "Error! Failed to filter germine events from nonsignedout mutation file for AstraZeneca MSK-Impact subset."
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca MSK-IMPACT mutation event filtering"

    EMAIL_BODY="Failed to filter germine events from nonsignedout mutation file for AstraZeneca MSK-Impact subset."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "AstraZeneca MSK-IMPACT Mutation Germline Filter Failure: Changelog summary will not be provided." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi
mv $mutation_filtered_filepath $mutation_filepath

sv_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_sv.txt"
sv_filtered_filepath="$AZ_MSK_IMPACT_DATA_HOME/data_sv.txt.filtered"
$PYTHON3_BINARY $PORTAL_HOME/scripts/filter_non_somatic_events_py3.py $sv_filepath $sv_filtered_filepath --event-type structural_variant

if [ $? -gt 0 ] ; then
    echo "Error! Failed to filter germine events from structural variant file for AstraZeneca MSK-Impact subset."
    sendPreImportFailureMessageMskPipelineLogsSlack "AstraZeneca MSK-IMPACT structural variant event filtering"

    EMAIL_BODY="Failed to filter germine events from structural variant file for AstraZeneca MSK-Impact subset."
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "AstraZeneca MSK-IMPACT Structural Variant Germline Filter Failure: Changelog summary will not be provided." $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi
mv $sv_filtered_filepath $sv_filepath

# ------------------------------------------------------------------------------------------------------------------------
# 6. Push the updates data to GitHub
printTimeStampedDataProcessingStepMessage "push of AstraZeneca data updates to git repository"

echo "Committing AstraZeneca MSK-IMPACT data"

if ! push_updates_to_az_git_repo ; then
    sendPreImportFailureMessageMskPipelineLogsSlack "GIT PUSH (az-msk-impact-2022) :fire: - address ASAP!"

    EMAIL_BODY="Failed to push AstraZeneca MSK-IMPACT outgoing changes to Git - address ASAP!"
    echo -e "Sending email $EMAIL_BODY"
    echo -e "$EMAIL_BODY" |  mail -s "[URGENT] GIT PUSH FAILURE" $PIPELINES_EMAIL_LIST

    cd $AZ_DATA_HOME ; $GIT_BINARY reset HEAD --hard

    exit 1
fi

# Send a message on success
sendImportSuccessMessageMskPipelineLogsSlack "ASTRAZENECA MSKIMPACT"
