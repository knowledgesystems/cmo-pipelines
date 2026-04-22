#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "import-pdx-data" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_PDX_EXIT:-0}"
