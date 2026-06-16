#!/usr/bin/env bash
# Unit tests for MSK import scripts.
# Exercises branching logic (exit codes, abandoned vs postimport) across all constituent scripts.
# Usage: bash import-scripts/test/test_msk_import_scripts.sh
# Exit code = number of failures (0 = all pass).

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IMPORT_SCRIPTS_DIR="$REPO_ROOT/import-scripts"
MOCK_RESOURCES_DIR="$SCRIPT_DIR/resources/msk_import_scripts"
MOCK_SCRIPTS_DIR="$MOCK_RESOURCES_DIR"  # mock_java.sh lives here

failure_count=0

# ---------------------------------------------------------------------------
# Assertion helpers
# ---------------------------------------------------------------------------

assert_exit_code() {
    local expected="$1" actual="$2" test_name="$3"
    if [ "$actual" -eq "$expected" ] ; then
        echo "$test_name ... pass"
    else
        echo "$test_name (expected exit $expected, got $actual) ... FAIL"
        failure_count=$(( failure_count + 1 ))
    fi
}

assert_nonzero_exit() {
    local actual="$1" test_name="$2"
    if [ "$actual" -ne 0 ] ; then
        echo "$test_name ... pass"
    else
        echo "$test_name (expected nonzero exit, got 0) ... FAIL"
        failure_count=$(( failure_count + 1 ))
    fi
}

assert_file_contains() {
    local file="$1" pattern="$2" test_name="$3"
    if grep -q "$pattern" "$file" 2>/dev/null ; then
        echo "$test_name ... pass"
    else
        echo "$test_name (expected '$pattern' in $file) ... FAIL"
        failure_count=$(( failure_count + 1 ))
    fi
}

assert_file_not_contains() {
    local file="$1" pattern="$2" test_name="$3"
    if ! grep -q "$pattern" "$file" 2>/dev/null ; then
        echo "$test_name ... pass"
    else
        echo "$test_name (expected '$pattern' NOT in $file, but it was) ... FAIL"
        failure_count=$(( failure_count + 1 ))
    fi
}

# ---------------------------------------------------------------------------
# Setup / teardown
# ---------------------------------------------------------------------------

MOCK_TMP=""
MOCK_CALL_LOG=""
MOCK_PORTAL_HOME=""

setup() {
    MOCK_PORTAL_HOME="$MOCK_RESOURCES_DIR/mock_portal_home"
    MOCK_TMP="$(mktemp -d)"
    MOCK_CALL_LOG="$(mktemp)"

    # Create subdirs that scripts expect under MSK_DMP_TMPDIR etc.
    mkdir -p \
        "$MOCK_TMP/msk-dmp" \
        "$MOCK_TMP/separate_working_directory_for_dmp" \
        "$MOCK_TMP/import-cron-dmp-wrapper" \
        "$MOCK_TMP/import-cron-cmo-msk"

    # Create the checkpoint file so import-dmp-impact-data.sh doesn't loop
    CHECKPOINT_FILEPATH="$MOCK_TMP/msk-import-script-go-ahead"
    touch "$CHECKPOINT_FILEPATH"

    # flock is Linux-only; provide a mock that always succeeds
    mkdir -p "$MOCK_TMP/bin"
    printf '#!/usr/bin/env bash\nexit 0\n' > "$MOCK_TMP/bin/flock"
    chmod +x "$MOCK_TMP/bin/flock"
    export PATH="$MOCK_TMP/bin:$PATH"

    # Export all variables needed by scripts
    export PORTAL_HOME="$MOCK_PORTAL_HOME"
    export MOCK_TMP
    export MOCK_CALL_LOG
    export MOCK_SCRIPTS_DIR
    export CHECKPOINT_FILEPATH
    export JAVA_BINARY="$MOCK_SCRIPTS_DIR/mock_java.sh"
    export GIT_BINARY="true"
    export PYTHON_BINARY="true"
    export JAVA_PROXY_ARGS=""
    export JAVA_SSL_ARGS=""
    export JAVA_DD_AGENT_ARGS=""
    export java_debug_args=""
    export PIPELINES_CONFIG_HOME="$MOCK_PORTAL_HOME/properties"
    export GMAIL_CREDS_FILE="/dev/null"
    export ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
    export SLACK_URL_FILE="/dev/null"
    export PIPELINES_EMAIL_LIST="test@example.com"
    export CMO_EMAIL_LIST="test@example.com"
    export MSK_IMPACT_DATA_HOME="$MOCK_TMP/mskimpact"
    export MSK_DMP_TMPDIR="$MOCK_TMP/msk-dmp"
    export PORTAL_DATA_HOME="$MOCK_TMP"

    # Preimport/postimport script directory overrides
    export PORTAL_SCRIPTS_DIRECTORY="$MOCK_PORTAL_HOME/scripts"
    export MANAGE_DATABASE_TOOL_PROPERTIES_FILEPATH="$MOCK_PORTAL_HOME/pipelines-credentials/manage_msk_clickhouse_database_update_tools.properties"
    # flock lock files — must be in a writable location
    export FLOCK_FILEPATH="$MOCK_TMP/cmo.lock"

    # Reset all MOCK_* control vars to defaults (success)
    unset MOCK_JAVA_DBVERSION_EXIT
    unset MOCK_JAVA_UPDATE_STUDY_EXIT
    unset MOCK_JAVA_FETCH_EXIT
    unset MOCK_DATA_SOURCE_MGR_EXIT
    unset MOCK_SET_STATE_EXIT
    unset MOCK_VERIFY_MGMT_EXIT
    unset MOCK_FETCH_DMP_EXIT
    unset MOCK_DMP_IMPACT_EXIT
    unset MOCK_CMO_MSK_EXIT
    unset MOCK_PDX_EXIT
    unset MOCK_SPECTRUM_EXIT
    unset MOCK_EXTRACT_EXIT
    unset MOCK_PREIMPORT_STATUS
    unset MOCK_CURRENT_DB_COLOR
    unset MOCK_DROP_TABLES_EXIT
    unset MOCK_CLONE_DB_EXIT
    unset MOCK_DOWNLOAD_SQL_EXIT
    unset MOCK_CREATE_DERIVED_EXIT
    unset MOCK_TRANSFER_COLOR_EXIT
    # Per-portal update-study-data mock exits (keyed on --portal arg, uppercased with - → _)
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_SOLID_HEME_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSKARCHER_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_KINGSCOUNTY_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_LEHIGHVALLEY_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_QUEENSCANCERCENTER_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_MCI_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_HARTFORD_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_RALPHLAUREN_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_TAILORMEDJAPAN_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_SCLC_PORTAL
    unset MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_FMI_LYMPHOMA_PORTAL
}

teardown() {
    rm -rf "$MOCK_TMP"
    rm -f "$MOCK_CALL_LOG"
}

# Touches all DMP study trigger files so every sub-import is attempted
touch_all_dmp_triggers() {
    touch \
        "$MOCK_TMP/MSK_SOLID_HEME_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_ARCHER_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_KINGS_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_LEHIGH_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_QUEENS_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_MCI_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_HARTFORD_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_RALPHLAUREN_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER" \
        "$MOCK_TMP/MSK_SCLC_IMPORT_TRIGGER" \
        "$MOCK_TMP/LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER"
}

run_script() {
    bash "$@" >/dev/null 2>&1
    echo $?
}

# ---------------------------------------------------------------------------
# import-dmp-impact-data.sh
# ---------------------------------------------------------------------------

echo "--- import-dmp-impact-data.sh ---"

setup
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_exit_code 0 "$result" "dmp: no triggers present → all skipped → exit 0"
teardown

setup
touch_all_dmp_triggers
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_exit_code 0 "$result" "dmp: all triggers + all succeed → exit 0"
teardown

setup
export MOCK_JAVA_DBVERSION_EXIT=1
touch_all_dmp_triggers
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: db version check fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_SOLID_HEME_PORTAL=1
touch "$MOCK_TMP/MSK_SOLID_HEME_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: MSKSOLIDHEME import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSKARCHER_PORTAL=1
touch "$MOCK_TMP/MSK_ARCHER_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: ARCHER import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_KINGSCOUNTY_PORTAL=1
touch "$MOCK_TMP/MSK_KINGS_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: KINGSCOUNTY import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_LEHIGHVALLEY_PORTAL=1
touch "$MOCK_TMP/MSK_LEHIGH_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: LEHIGHVALLEY import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_QUEENSCANCERCENTER_PORTAL=1
touch "$MOCK_TMP/MSK_QUEENS_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: QUEENSCANCERCENTER import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_SCLC_PORTAL=1
touch "$MOCK_TMP/MSK_SCLC_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: SCLCMSKIMPACT import fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_FMI_LYMPHOMA_PORTAL=1
touch "$MOCK_TMP/LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_nonzero_exit "$result" "dmp: LYMPHOMA import fails → exit nonzero"
teardown

setup
export SKIP_AFFILIATE_STUDIES_IMPORT=1
touch "$MOCK_TMP/MSK_SOLID_HEME_IMPORT_TRIGGER"
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-dmp-impact-data.sh")
assert_exit_code 0 "$result" "dmp: SKIP_AFFILIATE_STUDIES_IMPORT=1, msksolidheme succeeds → exit 0"
teardown

# ---------------------------------------------------------------------------
# import-cmo-data-msk.sh
# ---------------------------------------------------------------------------

echo "--- import-cmo-data-msk.sh ---"

setup
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-cmo-data-msk.sh")
assert_exit_code 0 "$result" "cmo: all succeed → exit 0"
teardown

setup
export MOCK_JAVA_DBVERSION_EXIT=1
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-cmo-data-msk.sh")
assert_nonzero_exit "$result" "cmo: db version check fails → exit nonzero"
teardown

setup
export MOCK_DATA_SOURCE_MGR_EXIT=1
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-cmo-data-msk.sh")
assert_nonzero_exit "$result" "cmo: data source fetch fails → exit nonzero"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT=1
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-cmo-data-msk.sh")
assert_nonzero_exit "$result" "cmo: import command fails → exit nonzero"
teardown

# ---------------------------------------------------------------------------
# import-msk-extract-projects.sh
# ---------------------------------------------------------------------------

echo "--- import-msk-extract-projects.sh ---"

setup
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-msk-extract-projects.sh")
assert_exit_code 0 "$result" "extract: all succeed → exit 0"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT=1
result=$(run_script "$IMPORT_SCRIPTS_DIR/import-msk-extract-projects.sh")
assert_nonzero_exit "$result" "extract: import fails → exit nonzero"
teardown

# ---------------------------------------------------------------------------
# update-msk-spectrum-cohort.sh
# ---------------------------------------------------------------------------

echo "--- update-msk-spectrum-cohort.sh ---"

setup
result=$(run_script "$IMPORT_SCRIPTS_DIR/update-msk-spectrum-cohort.sh")
assert_exit_code 0 "$result" "spectrum: all succeed → exit 0"
teardown

setup
export MOCK_JAVA_UPDATE_STUDY_EXIT=1
result=$(run_script "$IMPORT_SCRIPTS_DIR/update-msk-spectrum-cohort.sh")
assert_nonzero_exit "$result" "spectrum: import fails → exit nonzero"
teardown

# ---------------------------------------------------------------------------
# import-msk-preimport-steps-for-clickhouse.sh
# ---------------------------------------------------------------------------

echo "--- import-msk-preimport-steps-for-clickhouse.sh ---"

setup
status_file="$MOCK_TMP/preimport-status"
bash "$IMPORT_SCRIPTS_DIR/import-msk-preimport-steps-for-clickhouse.sh" "$status_file" >/dev/null 2>&1
result=$?
assert_exit_code 0 "$result" "preimport: all succeed → exit 0"
assert_file_contains "$status_file" "yes" "preimport: all succeed → status file contains 'yes'"
teardown

setup
export MOCK_SET_STATE_EXIT=1
status_file="$MOCK_TMP/preimport-status"
bash "$IMPORT_SCRIPTS_DIR/import-msk-preimport-steps-for-clickhouse.sh" "$status_file" >/dev/null 2>&1
result=$?
assert_nonzero_exit "$result" "preimport: set_update_process_state fails → exit nonzero"
assert_file_contains "$status_file" "no" "preimport: set_update_process_state fails → status file contains 'no'"
teardown

setup
export MOCK_DROP_TABLES_EXIT=1
status_file="$MOCK_TMP/preimport-status"
bash "$IMPORT_SCRIPTS_DIR/import-msk-preimport-steps-for-clickhouse.sh" "$status_file" >/dev/null 2>&1
result=$?
assert_nonzero_exit "$result" "preimport: drop_tables fails → exit nonzero"
assert_file_contains "$status_file" "no" "preimport: drop_tables fails → status file contains 'no'"
teardown

setup
export MOCK_CLONE_DB_EXIT=1
status_file="$MOCK_TMP/preimport-status"
bash "$IMPORT_SCRIPTS_DIR/import-msk-preimport-steps-for-clickhouse.sh" "$status_file" >/dev/null 2>&1
result=$?
assert_nonzero_exit "$result" "preimport: clone_db fails → exit nonzero"
assert_file_contains "$status_file" "no" "preimport: clone_db fails → status file contains 'no'"
teardown

# ---------------------------------------------------------------------------
# import-msk-postimport-steps-for-clickhouse.sh
# ---------------------------------------------------------------------------
# NOTE: the script's main() currently does not propagate failure exit codes —
# the tests below document expected behavior once that bug is fixed.

echo "--- import-msk-postimport-steps-for-clickhouse.sh ---"

setup
bash "$IMPORT_SCRIPTS_DIR/import-msk-postimport-steps-for-clickhouse.sh" >/dev/null 2>&1
result=$?
assert_exit_code 0 "$result" "postimport: all succeed → exit 0"
teardown

setup
export MOCK_CREATE_DERIVED_EXIT=1
bash "$IMPORT_SCRIPTS_DIR/import-msk-postimport-steps-for-clickhouse.sh" >/dev/null 2>&1
result=$?
assert_nonzero_exit "$result" "postimport: create_derived_tables fails → exit nonzero"
teardown

setup
export MOCK_TRANSFER_COLOR_EXIT=1
bash "$IMPORT_SCRIPTS_DIR/import-msk-postimport-steps-for-clickhouse.sh" >/dev/null 2>&1
result=$?
assert_nonzero_exit "$result" "postimport: transfer_color fails → exit nonzero"
teardown

# ---------------------------------------------------------------------------
# fetch-and-import-dmp-data-wrapper.sh
# ---------------------------------------------------------------------------
# Wrapper tests use mock constituent scripts in $PORTAL_HOME/scripts/ and
# assert on MOCK_CALL_LOG to verify postimport vs abandoned state transitions.

echo "--- fetch-and-import-dmp-data-wrapper.sh ---"

run_wrapper() {
    # The wrapper uses flock, which needs the lock file dir to exist.
    local lock_file="$MOCK_TMP/wrapper.lock"
    MY_FLOCK_FILEPATH="$lock_file" \
    bash "$IMPORT_SCRIPTS_DIR/fetch-and-import-dmp-data-wrapper.sh" >/dev/null 2>&1
}

setup
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "postimport" "wrapper: all succeed → postimport called"
teardown

setup
export MOCK_DMP_IMPACT_EXIT=1
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: dmp-impact fails → abandoned called"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: dmp-impact fails → postimport NOT called"
teardown

setup
export MOCK_CMO_MSK_EXIT=1
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: import-cmo-data-msk fails → abandoned called"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: import-cmo-data-msk fails → postimport NOT called"
teardown

setup
export MOCK_SPECTRUM_EXIT=1
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: update-msk-spectrum-cohort fails → abandoned called"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: update-msk-spectrum-cohort fails → postimport NOT called"
teardown

setup
export MOCK_EXTRACT_EXIT=1
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: import-msk-extract-projects fails → abandoned called"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: import-msk-extract-projects fails → postimport NOT called"
teardown

setup
export MOCK_PREIMPORT_STATUS=no
run_wrapper
assert_file_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: preimport fails → abandoned called"
assert_file_not_contains "$MOCK_CALL_LOG" "import-dmp-impact-data" "wrapper: preimport fails → DMP imports skipped"
assert_file_not_contains "$MOCK_CALL_LOG" "import-cmo-data-msk" "wrapper: preimport fails → CMO import skipped"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: preimport fails → postimport NOT called"
teardown

setup
export MOCK_VERIFY_MGMT_EXIT=1
run_wrapper
assert_file_not_contains "$MOCK_CALL_LOG" "import-dmp-impact-data" "wrapper: verify-management fails → all imports skipped"
assert_file_not_contains "$MOCK_CALL_LOG" "postimport" "wrapper: verify-management fails → postimport NOT called"
assert_file_not_contains "$MOCK_CALL_LOG" "set_update_process_state:abandoned" "wrapper: verify-management fails → abandoned NOT called (update state never valid)"
teardown

# ---------------------------------------------------------------------------

echo ""
if [ "$failure_count" -eq 0 ] ; then
    echo "All tests passed."
else
    echo "$failure_count test(s) FAILED."
fi

exit $failure_count
