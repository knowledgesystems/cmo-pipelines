#!/usr/bin/env bash
# Records the state argument to MOCK_CALL_LOG so tests can assert what was called.
# Control var: MOCK_SET_STATE_EXIT (default 0)
state="$2"
if [ -n "$MOCK_CALL_LOG" ] ; then
    echo "set_update_process_state:${state}" >> "$MOCK_CALL_LOG"
fi
exit "${MOCK_SET_STATE_EXIT:-0}"
