#!/usr/bin/env bash

app_name="$(basename $0)"

KUBECTL_BINARY=kubectl
if ! which $KUBECTL_BINARY > /dev/null 2>&1 ; then
    echo "Error : $app_name requires $KUBECTL_BINARY, which was not found in the current PATH"
    exit 1
fi

unset portal_to_deployment_map

declare -A portal_to_deployment_map
portal_to_deployment_map["public"]="cbioportal-spring-boot"
portal_to_deployment_map["genie-public"]="cbioportal-backend-genie-public"
portal_to_deployment_map["genie-private"]="cbioportal-backend-genie-private"
portal_to_deployment_map["genie-archive"]="cbioportal-backend-genie-archive"
portal_to_deployment_map["triage"]="eks-triage"
# portal_to_deployment_map["msk"]="MSK_PORTAL_DEPLOYMENT_NAME_GOES_HERE"
unset portal_to_url
declare -A portal_to_url
portal_to_url["public"]="https://cbioportal.org"
portal_to_url["genie-public"]="https://genie.cbioportal.org"
portal_to_url["genie-private"]="https://genie-private.cbioportal.org"
portal_to_url["genie-archive"]=""
portal_to_url["triage"]="https://triage.cbioportal.mskcc.org"
# portal_to_url["msk"]="LIST_OF_REDIS_SERVICES_GOES_HERE"

CACHE_API_KEY=`cat $CACHE_API_KEY_FILE`

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in ${!portal_to_deployment_map[@]} ; do
        echo "	$portal"
    done
}

portal_id=$1
if [ -z "$portal_id" ] ; then
    echo "usage : $app_name <portal id> [--preserve-cache]"
    print_portal_id_values
    exit 1
fi
preserve_cache_flag=$2
if [ -n "$preserve_cache_flag" ] ; then
    if [ ! "$preserve_cache_flag" == "--preserve-cache" ] ; then
        echo "usage : $app_name <portal id> [--preserve-cache]"
        print_portal_id_values
        exit 1
    fi
fi
deployment_id=${portal_to_deployment_map[$portal_id]}
if [ -z "$deployment_id" ] ; then
    echo "invalid portal_id : $portal_id"
    print_portal_id_values
fi

/data/portal-cron/scripts/authenticate_service_account.sh 
$KUBECTL_BINARY set env deployment $deployment_id --env="LAST_RESTART=$(date)"

if [ -z "$preserve_cache_flag" ] ; then
    portal_url=${portal_to_url[$portal_id]}
    if [ -n "$portal_url" ] ; then
        curl -X DELETE "$portal_url/api/cache?springManagedCache=true" -H "accept: text/plain" -H "X-API-KEY: $CACHE_API_KEY"
    fi
fi
