#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "import-msk-extract-projects" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_EXTRACT_EXIT:-0}"
