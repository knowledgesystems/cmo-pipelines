#!/usr/bin/env bash
# Sets all environment variables that real scripts expect from automation-environment.sh.
# MOCK_SCRIPTS_DIR must be set by the test before this is sourced.

JAVA_BINARY="${MOCK_SCRIPTS_DIR}/mock_java.sh"
GIT_BINARY="true"
PYTHON_BINARY="true"
JAVA_PROXY_ARGS=""
JAVA_SSL_ARGS=""
JAVA_DD_AGENT_ARGS=""
java_debug_args=""
PIPELINES_CONFIG_HOME="${PORTAL_HOME}/properties"
GMAIL_CREDS_FILE="/dev/null"
ONCOTREE_VERSION_TO_USE="oncotree_candidate_release"
SLACK_URL_FILE="/dev/null"
PIPELINES_EMAIL_LIST="test@example.com"
CMO_EMAIL_LIST="test@example.com"
