#!/usr/bin/env bash

app_name="$(basename $0)"

KUBECTL_BINARY=kubectl
if ! which $KUBECTL_BINARY > /dev/null 2>&1 ; then
    echo "Error : $app_name requires $KUBECTL_BINARY, which was not found in the current PATH"
    exit 1
fi

CLUSTER_ID_DIGITS="digits-eks"
CLUSTER_ID_KS="knowledgesystems-kubernetes"

unset portal_to_cluster_map
declare -A portal_to_cluster_map
portal_to_cluster_map["public"]="$CLUSTER_ID_KS"
portal_to_cluster_map["genie-public-blue"]="$CLUSTER_ID_KS"
portal_to_cluster_map["genie-public-green"]="$CLUSTER_ID_KS"
portal_to_cluster_map["genie-private-blue"]="$CLUSTER_ID_KS"
portal_to_cluster_map["genie-private-green"]="$CLUSTER_ID_KS"
portal_to_cluster_map["crdc"]="$CLUSTER_ID_KS"
portal_to_cluster_map["triage"]="$CLUSTER_ID_DIGITS"
portal_to_cluster_map["hgnc"]="$CLUSTER_ID_DIGITS"
portal_to_cluster_map["msk"]="$CLUSTER_ID_DIGITS"
portal_to_cluster_map["msk-beta"]="$CLUSTER_ID_DIGITS"
portal_to_cluster_map["private"]="$CLUSTER_ID_DIGITS"
portal_to_cluster_map["sclc"]="$CLUSTER_ID_DIGITS"

unset portal_to_deployment_map
declare -A portal_to_deployment_map
portal_to_deployment_map["public"]="cbioportal-spring-boot"
portal_to_deployment_map["genie-public-blue"]="cbioportal-backend-genie-public-blue"
portal_to_deployment_map["genie-public-green"]="cbioportal-backend-genie-public-green"
portal_to_deployment_map["genie-private-blue"]="cbioportal-backend-genie-private-blue"
portal_to_deployment_map["genie-private-green"]="cbioportal-backend-genie-private-green"
portal_to_deployment_map["crdc"]="cbioportal-backend-nci"
portal_to_deployment_map["triage"]="eks-triage"
portal_to_deployment_map["hgnc"]="eks-hgnc"
portal_to_deployment_map["msk"]="eks-msk"
portal_to_deployment_map["msk-beta"]="eks-msk-beta"
portal_to_deployment_map["private"]="eks-private"
portal_to_deployment_map["sclc"]="eks-sclc"

# TODO : the cache service basename and cache database number could be extracted from the kubernetes deployment and config map

unset portal_to_cache_service_basename
declare -A portal_to_cache_service_basename
portal_to_cache_service_basename["public"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["genie-public-blue"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-public-green"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-private-blue"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-private-green"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["crdc"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["triage"]="triage-cbioportal-persistence-redis"
portal_to_cache_service_basename["hgnc"]=""
portal_to_cache_service_basename["msk"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["msk-beta"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["private"]="cbioportal-persistence-redis"
portal_to_cache_service_basename["sclc"]="cbioportal-persistence-redis"

unset portal_to_cache_database_number
declare -A portal_to_cache_database_number
portal_to_cache_database_number["public"]="8"
portal_to_cache_database_number["genie-public-blue"]="1"
portal_to_cache_database_number["genie-public-green"]="2"
portal_to_cache_database_number["genie-private-blue"]="3"
portal_to_cache_database_number["genie-private-green"]="4"
portal_to_cache_database_number["crdc"]="7"
portal_to_cache_database_number["triage"]="1"
portal_to_cache_database_number["hgnc"]="unassigned"
portal_to_cache_database_number["msk"]=1
portal_to_cache_database_number["msk-beta"]=1
portal_to_cache_database_number["private"]=1
portal_to_cache_database_number["sclc"]=2

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in ${!portal_to_deployment_map[@]} ; do
        echo "  $portal"
    done
}

function database_number_is_valid() {
    number=$1
    v=0
    while [ $v -lt 16 ] ; do
        if [ "$number" == "$v" ] ; then
            return 0 # valid
        fi
        v=$(($v+1))
    done
    return 1 # not valid
}

portal_id=$1
if [ -z "$portal_id" ] ; then
    echo "usage : $app_name <portal id>"
    print_portal_id_values
    exit 1
fi

cluster_id=${portal_to_cluster_map[$portal_id]}
deployment_id=${portal_to_deployment_map[$portal_id]}
if [ -z "$cluster_id" ] || [ -z "$deployment_id" ]; then
    echo "invalid portal_id : $portal_id"
    print_portal_id_values
    exit 1
fi

unset KUBECONFIG_ARG
if [ "$cluster_id" == "$CLUSTER_ID_DIGITS" ] ; then
    /data/portal-cron/scripts/authenticate_service_account.sh eks
else
    if ! [ -z $PUBLIC_CLUSTER_KUBECONFIG ] ; then
        /data/portal-cron/scripts/authenticate_service_account.sh public
        KUBECONFIG_ARG="--kubeconfig $PUBLIC_CLUSTER_KUBECONFIG"
    fi
fi

$KUBECTL_BINARY $KUBECONFIG_ARG set env deployment $deployment_id --env="LAST_RESTART=$(date)"
webapp_restart_status=$?
if [ $webapp_restart_status -ne 0 ] ; then
    echo "warning : the attempt to restart portal '$portal_id' failed : $KUBECTL_BINARY returned status $webapp_restart_status" >&2
    echo "          nonetheless, a command to clear the persistence cache will now occur." >&2
fi

cache_service_basename=${portal_to_cache_service_basename[$portal_id]}
cache_leader_pod_name="${cache_service_basename}-master-0"
cache_database_number=${portal_to_cache_database_number[$portal_id]}
if [ -n "$cache_service_basename" ] && database_number_is_valid "$cache_database_number" ; then
    $KUBECTL_BINARY $KUBECONFIG_ARG exec -t -n "default" "$cache_leader_pod_name" -- "/bin/bash" -c "redis-cli -a \$REDIS_PASSWORD -n $cache_database_number FLUSHDB"
    cache_flush_status=$?
    if [ $cache_flush_status -ne 0 ] ; then
        echo "error encountered during attempt to clear portal persistence cache for portal $portal_id : $KUBECTL_BINARY returned status $cache_flush_status" >&2
        exit 1
    fi
else
    echo portal $portal_id has no persistence cache service or has no valid database number. skipping.
    exit 0
fi
exit 0
