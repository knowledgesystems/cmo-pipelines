#!/usr/bin/env bash
# Mock import-temp-study.sh — parses --study-id= and returns a configurable exit code.
# Set MOCK_IMPORT_TEMP_STUDY_EXIT_<STUDYID_UPPER> to control per-study behavior.
# e.g. MOCK_IMPORT_TEMP_STUDY_EXIT_MSKIMPACT=1  → mskimpact (MSKSOLIDHEME) fails

study_id=""
for arg in "$@"; do
    case $arg in
        --study-id=*) study_id="${arg#*=}" ;;
    esac
done

study_id_upper=$(echo "$study_id" | tr '[:lower:]' '[:upper:]' | tr '-' '_')
var_name="MOCK_IMPORT_TEMP_STUDY_EXIT_${study_id_upper}"
exit "${!var_name:-0}"
