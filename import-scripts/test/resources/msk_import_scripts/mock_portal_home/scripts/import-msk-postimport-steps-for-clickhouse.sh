#!/usr/bin/env bash
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "postimport" >> "$MOCK_CALL_LOG"
fi
exit 0
