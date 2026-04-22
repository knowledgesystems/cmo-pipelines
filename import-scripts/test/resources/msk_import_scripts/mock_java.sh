#!/usr/bin/env bash
# Mock JAVA_BINARY — returns configurable exit codes per operation.
#
# Global control vars:
#   MOCK_JAVA_DBVERSION_EXIT      — exit code for --check-db-version (default 0)
#   MOCK_JAVA_UPDATE_STUDY_EXIT   — exit code for --update-study-data (default 0)
#   MOCK_JAVA_FETCH_EXIT          — exit code for --fetch-data (default 0)
#
# Per-portal override for --update-study-data:
#   MOCK_JAVA_UPDATE_STUDY_EXIT_<PORTAL_UPPER>
#   e.g. MOCK_JAVA_UPDATE_STUDY_EXIT_MSK_SOLID_HEME_PORTAL=1

# Parse --portal <name> from args (space-separated form)
portal_name=""
args=("$@")
for i in "${!args[@]}"; do
    if [ "${args[$i]}" = "--portal" ] ; then
        next=$(( i + 1 ))
        portal_name="${args[$next]:-}"
        break
    fi
done

for arg in "$@"; do
    case $arg in
        --check-db-version)
            exit "${MOCK_JAVA_DBVERSION_EXIT:-0}" ;;
        --update-study-data)
            if [ -n "$portal_name" ] ; then
                portal_upper=$(echo "$portal_name" | tr '[:lower:]' '[:upper:]' | tr '-' '_')
                var="MOCK_JAVA_UPDATE_STUDY_EXIT_${portal_upper}"
                if [ -n "${!var+x}" ] ; then
                    exit "${!var}"
                fi
            fi
            exit "${MOCK_JAVA_UPDATE_STUDY_EXIT:-0}" ;;
        --import-types-of-cancer|--send-update-notification|--validate-temp-study|--delete-cancer-study|--rename-cancer-study)
            exit 0 ;;
        --fetch-data)
            exit "${MOCK_JAVA_FETCH_EXIT:-0}" ;;
    esac
done
exit 0
