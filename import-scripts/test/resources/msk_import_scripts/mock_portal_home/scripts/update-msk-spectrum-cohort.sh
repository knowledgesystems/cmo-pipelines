#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "update-msk-spectrum-cohort" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_SPECTRUM_EXIT:-0}"
