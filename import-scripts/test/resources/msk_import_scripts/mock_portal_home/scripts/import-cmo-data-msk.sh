#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "import-cmo-data-msk" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_CMO_MSK_EXIT:-0}"
