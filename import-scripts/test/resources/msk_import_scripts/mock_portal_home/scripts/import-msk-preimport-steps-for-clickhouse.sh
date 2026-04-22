#!/usr/bin/env bash
# Writes status to $1 immediately (mocks the background preimport script).
output_status_filepath="$1"
echo "${MOCK_PREIMPORT_STATUS:-yes}" > "$output_status_filepath"
exit 0
