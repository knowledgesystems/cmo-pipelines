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
# portal_to_deployment_map["msk"]="MSK_PORTAL_DEPLOYMENT_NAME_GOES_HERE"
unset portal_to_cache_service_list
declare -A portal_to_cache_service_list
portal_to_cache_service_list["public"]="cbioportal-public-persistence-redis-master cbioportal-public-persistence-redis-slave"
portal_to_cache_service_list["genie-public"]="cbioportal-persistence-redis-genie-master cbioportal-persistence-redis-genie-slave"
portal_to_cache_service_list["genie-private"]="cbioportal-persistence-redis-genie-master cbioportal-persistence-redis-genie-slave"
portal_to_cache_service_list["genie-archive"]=""
# portal_to_cache_service_list["msk"]="LIST_OF_REDIS_SERVICES_GOES_HERE"

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
    cache_service_list=${portal_to_cache_service_list[$portal_id]}
    if [ -n "$cache_service_list" ] ; then
        for cache_service in $cache_service_list ; do
            $KUBECTL_BINARY set env statefulset $cache_service --env="LAST_RESTART=$(date)"
        done
    fi
fi
