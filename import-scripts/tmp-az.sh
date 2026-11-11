#!/usr/bin/env bash

function remove_duplicate_maf_variants() {
    MUTATIONS_EXTD_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended.txt"
    NSOUT_MUTATIONS_INPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations.txt"

    MUTATIONS_EXTD_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_mutations_extended_merged.txt"
    NSOUT_MUTATIONS_OUTPUT_FILEPATH="$AZ_MSK_IMPACT_DATA_HOME/data_nonsignedout_mutations_merged.txt"

    # Remove duplicate variants from MAF files
    "$PYTHON3_BINARY" "$PORTAL_HOME/scripts/remove-duplicate-maf-variants.py" -i "$MUTATIONS_EXTD_INPUT_FILEPATH" -o "$MUTATIONS_EXTD_OUTPUT_FILEPATH" &&
    "$PYTHON3_BINARY" "$PORTAL_HOME/scripts/remove-duplicate-maf-variants.py" -i "$NSOUT_MUTATIONS_INPUT_FILEPATH" -o "$NSOUT_MUTATIONS_OUTPUT_FILEPATH" &&

    # Rewrite mutation files with updated data
    mv "$MUTATIONS_EXTD_OUTPUT_FILEPATH" "$MUTATIONS_EXTD_INPUT_FILEPATH" &&
    mv "$NSOUT_MUTATIONS_OUTPUT_FILEPATH" "$NSOUT_MUTATIONS_INPUT_FILEPATH"
}

# Remove duplicate variants from MAF files
if ! remove_duplicate_maf_variants ; then
    echo "ERROR: Failed to remove duplicate variants from MAF files for AstraZeneca MSK-IMPACT. Exiting." >&2
    exit 1
fi

