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
portal_to_deployment_map["hgnc"]="eks-hgnc"
portal_to_deployment_map["devdb"]="eks-devdb"
portal_to_deployment_map["msk"]="eks-msk"
portal_to_deployment_map["msk-beta"]="eks-msk-beta"
portal_to_deployment_map["private"]="eks-private"
portal_to_deployment_map["sclc"]="eks-sclc"

unset portal_to_cache_service_list
declare -A portal_to_cache_service_list
portal_to_cache_service_list["public"]="cbioportal-public-persistence-redis-master cbioportal-public-persistence-redis-slave"
portal_to_cache_service_list["genie-public"]="cbioportal-persistence-redis-genie-master cbioportal-persistence-redis-genie-slave"
portal_to_cache_service_list["genie-private"]="cbioportal-persistence-redis-genie-master cbioportal-persistence-redis-genie-slave"
portal_to_cache_service_list["genie-archive"]=""
portal_to_cache_service_list["triage"]="triage-cbioportal-persistence-redis-master triage-cbioportal-persistence-redis-replicas"
portal_to_cache_service_list["hgnc"]=""
portal_to_cache_service_list["devdb"]=""
portal_to_cache_service_list["msk"]="cbioportal-persistence-redis-master cbioportal-persistence-redis-replicas"
portal_to_cache_service_list["msk-beta"]="cbioportal-persistence-redis-master cbioportal-persistence-redis-replicas"
portal_to_cache_service_list["private"]="cbioportal-persistence-redis-master cbioportal-persistence-redis-replicas"
portal_to_cache_service_list["sclc"]="cbioportal-persistence-redis-master cbioportal-persistence-redis-replicas"

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in ${!portal_to_deployment_map[@]} ; do
        echo "  $portal"
    done
}

portal_id=$1
KUBECONFIG_ARG=""
if [ "$portal_id" == "public" ] || [ "$portal_id" == "genie-private" ] || [ "$portal_id" == "genie-archive" ] || [ "$portal_id" == "genie-public" ] ; then
    if ! [ -z $PUBLIC_CLUSTER_KUBECONFIG ] ; then
        KUBECONFIG_ARG="--kubeconfig $PUBLIC_CLUSTER_KUBECONFIG"
    fi
fi

if [ -z "$portal_id" ] ; then
    echo "usage : $app_name <portal id>"
    print_portal_id_values
    exit 1
fi

deployment_id=${portal_to_deployment_map[$portal_id]}
if [ -z "$deployment_id" ] ; then
    echo "invalid portal_id : $portal_id"
    print_portal_id_values
    exit 1
fi

/data/portal-cron/scripts/authenticate_service_account.sh 
$KUBECTL_BINARY $KUBECONFIG_ARG set env deployment $deployment_id --env="LAST_RESTART=$(date)"
webapp_restart_status=$?
if [ $webapp_restart_status -ne 0 ] ; then
    echo "warning : the attempt to restart portal '$portal_id' failed : $KUBECTL_BINARY returned status $webapp_restart_status" >&2
    echo "          nonetheless, a command to clear the persistence cache will now occur." >&2
fi

cache_service_list=${portal_to_cache_service_list[$portal_id]}
if [ -n "$cache_service_list" ] ; then
    for cache_service in $cache_service_list ; do
        $KUBECTL_BINARY $KUBECONFIG_ARG set env statefulset $cache_service --env="LAST_RESTART=$(date)"
        cache_reset_status=$?
        if [ $cache_reset_status -ne 0 ] ; then
            echo "error encountered during attempt to clear portal persistence cache for portal $portal_id : $KUBECTL_BINARY returned status $cache_reset_status" >&2
            exit 1
        fi
    done
else
    echo portal $portal_id has no persistence cache service. skipping.
    exit 0
fi
exit 0
