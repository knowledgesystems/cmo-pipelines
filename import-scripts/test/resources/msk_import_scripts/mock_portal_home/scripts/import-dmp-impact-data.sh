#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "import-dmp-impact-data" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_DMP_IMPACT_EXIT:-0}"
