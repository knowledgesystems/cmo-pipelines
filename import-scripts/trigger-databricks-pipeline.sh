#!/bin/bash
set -euo pipefail

PIPELINE_PARAM=""
RESULT_STATE_CHECK_MAX_ATTEMPTS=5
RESULT_STATE_CHECK_PAUSE_SECONDS=5

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --job-id) JOB_ID="$2"; shift ;;
        --pipeline-param) PIPELINE_PARAM="$2"; shift ;;
        *) echo "Unknown parameter: $1"; exit 1 ;;
    esac
    shift
done

source /data/portal-cron/scripts/automation-environment.sh

# Read Databricks credentials from creds file
DATABRICKS_SERVER_HOSTNAME=`grep server_hostname $DATABRICKS_CREDS_FILE | sed 's/^.*=//g'`
DATABRICKS_TOKEN=`grep access_token $DATABRICKS_CREDS_FILE | sed 's/^.*=//g'`

# Trigger the Databricks job
HTTP_CODE=$(curl -s -o /tmp/databricks_response.json -w "%{http_code}" \
    -X POST \
    -H "Authorization: Bearer $DATABRICKS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"job_id\": $JOB_ID,
        \"job_parameters\": {
            \"pipeline_param\": \"$PIPELINE_PARAM\"
        }
    }" \
    "https://$DATABRICKS_SERVER_HOSTNAME/api/2.1/jobs/run-now")

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Error triggering Databricks job. HTTP status: $HTTP_CODE"
    cat /tmp/databricks_response.json
    exit 1
fi

RUN_ID=$(python3 -c "import json; print(json.load(open('/tmp/databricks_response.json'))['run_id'])")
echo "Successfully triggered Databricks job. Run ID: $RUN_ID"

# Get run details including the URL
curl -s -o /tmp/databricks_run_details.json \
    -X GET \
    -H "Authorization: Bearer $DATABRICKS_TOKEN" \
    "https://$DATABRICKS_SERVER_HOSTNAME/api/2.1/jobs/runs/get?run_id=$RUN_ID"

RUN_URL=$(python3 -c "import json; print(json.load(open('/tmp/databricks_run_details.json'))['run_page_url'])")
echo "View run details at: $RUN_URL"

# Poll until the job is complete
echo "Polling for job completion..."
while true; do
    HTTP_CODE=$(curl -s -o /tmp/databricks_run_status.json -w "%{http_code}" \
        -X GET \
        -H "Authorization: Bearer $DATABRICKS_TOKEN" \
        "https://$DATABRICKS_SERVER_HOSTNAME/api/2.1/jobs/runs/get?run_id=$RUN_ID")

    if [ "$HTTP_CODE" -ne 200 ]; then
        echo "URL: https://$DATABRICKS_SERVER_HOSTNAME/api/2.1/jobs/runs/get?run_id=$RUN_ID"
        echo "Error polling Databricks job status. HTTP status: $HTTP_CODE"
        cat /tmp/databricks_run_status.json
        exit 1
    fi

    LIFE_CYCLE_STATE=$(python3 -c "import json; print(json.load(open('/tmp/databricks_run_status.json'))['state']['life_cycle_state'])")
    echo "Job state: $LIFE_CYCLE_STATE"

    if [ "$LIFE_CYCLE_STATE" == "TERMINATED" ] || [ "$LIFE_CYCLE_STATE" == "SKIPPED" ] || [ "$LIFE_CYCLE_STATE" == "INTERNAL_ERROR" ]; then
        # result_state can lag behind life_cycle_state reaching TERMINATED
        # (e.g. multi-task jobs), so retry briefly instead of failing immediately.
        RESULT_STATE=""
        result_state_check_count=0
        while [ $result_state_check_count -lt $RESULT_STATE_CHECK_MAX_ATTEMPTS ]; do
            RESULT_STATE=$(python3 -c "import json; print(json.load(open('/tmp/databricks_run_status.json'))['state'].get('result_state', ''))")
            if [ -n "$RESULT_STATE" ]; then
                break
            fi
            sleep $RESULT_STATE_CHECK_PAUSE_SECONDS
            curl -s -o /tmp/databricks_run_status.json \
                -X GET \
                -H "Authorization: Bearer $DATABRICKS_TOKEN" \
                "https://$DATABRICKS_SERVER_HOSTNAME/api/2.1/jobs/runs/get?run_id=$RUN_ID"
            result_state_check_count=$(($result_state_check_count+1))
        done

        if [ -z "$RESULT_STATE" ]; then
            echo "Error: Databricks job reached life_cycle_state=$LIFE_CYCLE_STATE but result_state was not available after $RESULT_STATE_CHECK_MAX_ATTEMPTS attempts with a period of $RESULT_STATE_CHECK_PAUSE_SECONDS seconds. Exiting" >&2
            cat /tmp/databricks_run_status.json
            exit 1
        fi

        echo "Job finished with result: $RESULT_STATE"
        if [ "$RESULT_STATE" != "SUCCESS" ]; then
            echo "Databricks job failed. Check the Databricks UI for details."
            exit 1
        fi
        break
    fi

    sleep 30
done

echo "Databricks job completed successfully."