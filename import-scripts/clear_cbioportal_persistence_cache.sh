#!/usr/bin/env bash

app_name="$(basename "$0")"

KUBECTL_BINARY=kubectl
if ! which "$KUBECTL_BINARY" > /dev/null 2>&1 ; then
    echo "Error : $app_name requires $KUBECTL_BINARY, which was not found in the current PATH"
    exit 1
fi

PROFILE_ID_PUBLIC="automation_public"
PROFILE_ID_EKS="automation_eks"

CLUSTER_ID_PUBLIC="public"
CLUSTER_ID_PUBLICARGOCD="publicargocd"
CLUSTER_ID_EKS="eks"
CLUSTER_ID_EKSARGOCD="eksargocd"

unset cluster_to_profile_map
declare -A cluster_to_profile_map
cluster_to_profile_map["$CLUSTER_ID_PUBLIC"]="$PROFILE_ID_PUBLIC"
cluster_to_profile_map["$CLUSTER_ID_PUBLICARGOCD"]="$PROFILE_ID_PUBLIC"
cluster_to_profile_map["$CLUSTER_ID_EKS"]="$PROFILE_ID_EKS"
cluster_to_profile_map["$CLUSTER_ID_EKSARGOCD"]="$PROFILE_ID_EKS"

unset cluster_to_kubeconfig_filepath
declare -A cluster_to_kubeconfig_filepath
cluster_to_kubeconfig_filepath["$CLUSTER_ID_PUBLIC"]="$PUBLIC_CLUSTER_KUBECONFIG"
cluster_to_kubeconfig_filepath["$CLUSTER_ID_PUBLICARGOCD"]="$PUBLICARGOCD_CLUSTER_KUBECONFIG"
cluster_to_kubeconfig_filepath["$CLUSTER_ID_EKS"]="$EKS_CLUSTER_KUBECONFIG"
cluster_to_kubeconfig_filepath["$CLUSTER_ID_EKSARGOCD"]="$EKSARGOCD_CLUSTER_KUBECONFIG"

unset portal_to_cluster_map
declare -A portal_to_cluster_map

# public importer node
portal_to_cluster_map["public-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["public-green"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["public-beta-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["public-beta-green"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["master-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["master-green"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["clickhouse-only-db-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["clickhouse-only-db-green"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["genie-public-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["genie-public-green"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["genie-private-blue"]="$CLUSTER_ID_PUBLICARGOCD"
portal_to_cluster_map["genie-private-green"]="$CLUSTER_ID_PUBLICARGOCD"

# pipelines3
portal_to_cluster_map["triage"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["hgnc"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["msk-beta-blue"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["msk-beta-green"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["msk-blue"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["msk-green"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["private-blue"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["private-green"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["sclc-blue"]="$CLUSTER_ID_EKS"
portal_to_cluster_map["sclc-green"]="$CLUSTER_ID_EKS"

unset portal_to_deployment_map
declare -A portal_to_deployment_map
# importer node
portal_to_deployment_map["public-blue"]="cbioportal-backend-public-blue"
portal_to_deployment_map["public-green"]="cbioportal-backend-public-green"
portal_to_deployment_map["public-beta-blue"]="cbioportal-backend-public-beta-blue"
portal_to_deployment_map["public-beta-green"]="cbioportal-backend-public-beta-green"
portal_to_deployment_map["master-blue"]="cbioportal-backend-master-blue"
portal_to_deployment_map["master-green"]="cbioportal-backend-master-green"
portal_to_deployment_map["clickhouse-only-db-blue"]="cbioportal-backend-clickhouse-only-db-blue"
portal_to_deployment_map["clickhouse-only-db-green"]="cbioportal-backend-clickhouse-only-db-green"
portal_to_deployment_map["genie-public-blue"]="cbioportal-backend-genie-public-blue"
portal_to_deployment_map["genie-public-green"]="cbioportal-backend-genie-public-green"
portal_to_deployment_map["genie-private-blue"]="cbioportal-backend-genie-private-blue"
portal_to_deployment_map["genie-private-green"]="cbioportal-backend-genie-private-green"
# pipelines3
portal_to_deployment_map["triage"]="eks-triage"
portal_to_deployment_map["hgnc"]="eks-hgnc"
portal_to_deployment_map["msk-beta-blue"]="eks-msk-beta-blue"
portal_to_deployment_map["msk-beta-green"]="eks-msk-beta-green"
portal_to_deployment_map["msk-blue"]="eks-msk-blue"
portal_to_deployment_map["msk-green"]="eks-msk-green"
portal_to_deployment_map["private-blue"]="eks-private-blue"
portal_to_deployment_map["private-green"]="eks-private-green"
portal_to_deployment_map["sclc-blue"]="eks-sclc-blue"
portal_to_deployment_map["sclc-green"]="eks-sclc-green"

# TODO : the cache service basename and cache database number could be extracted from the kubernetes deployment and config map

unset portal_to_cache_service_basename
declare -A portal_to_cache_service_basename
# importer node
portal_to_cache_service_basename["public-blue"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["public-green"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["public-beta-blue"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["public-beta-green"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["master-blue"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["master-green"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["clickhouse-only-db-blue"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["clickhouse-only-db-green"]="cbioportal-public-persistence-redis"
portal_to_cache_service_basename["genie-public-blue"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-public-green"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-private-blue"]="cbioportal-genie-persistence-redis"
portal_to_cache_service_basename["genie-private-green"]="cbioportal-genie-persistence-redis"
# pipelines3
portal_to_cache_service_basename["triage"]="triage-cbioportal-persistence-redis"
portal_to_cache_service_basename["hgnc"]=""
portal_to_cache_service_basename["msk-beta-blue"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["msk-beta-green"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["msk-blue"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["msk-green"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["private-blue"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["private-green"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["sclc-blue"]="eks-msk-cbioportal-persistence-redis"
portal_to_cache_service_basename["sclc-green"]="eks-msk-cbioportal-persistence-redis"

unset portal_to_cache_database_number
declare -A portal_to_cache_database_number
# importer node
portal_to_cache_database_number["public-blue"]="8"
portal_to_cache_database_number["public-green"]="9"
portal_to_cache_database_number["public-beta-blue"]="6"
portal_to_cache_database_number["public-beta-green"]="7"
portal_to_cache_database_number["master-blue"]="10"
portal_to_cache_database_number["master-green"]="11"
portal_to_cache_database_number["clickhouse-only-db-blue"]="12"
portal_to_cache_database_number["clickhouse-only-db-green"]="13"
portal_to_cache_database_number["genie-public-blue"]="1"
portal_to_cache_database_number["genie-public-green"]="2"
portal_to_cache_database_number["genie-private-blue"]="3"
portal_to_cache_database_number["genie-private-green"]="4"
# pipelines3
portal_to_cache_database_number["triage"]="1"
portal_to_cache_database_number["hgnc"]="unassigned"
portal_to_cache_database_number["msk-beta-blue"]="6"
portal_to_cache_database_number["msk-beta-green"]="7"
portal_to_cache_database_number["msk-blue"]="8"
portal_to_cache_database_number["msk-green"]="9"
portal_to_cache_database_number["private-blue"]="2"
portal_to_cache_database_number["private-green"]="3"
portal_to_cache_database_number["sclc-blue"]="4"
portal_to_cache_database_number["sclc-green"]="5"

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in "${!portal_to_deployment_map[@]}" ; do
        echo "  $portal"
    done
}

function database_number_is_valid() {
    local number="$1"
    local v=0
    while [ $v -lt 16 ] ; do
        if [ "$number" == "$v" ] ; then
            return 0 # valid
        fi
        v=$((v+1))
    done
    return 1 # not valid
}

portal_id="$1"
if [ -z "$portal_id" ] ; then
    echo "usage : $app_name <portal id>"
    print_portal_id_values
    exit 1
fi

cluster_id="${portal_to_cluster_map[$portal_id]}"
profile_id="${cluster_to_profile_map[$cluster_id]}"
deployment_id="${portal_to_deployment_map[$portal_id]}"
kubeconfig_filepath="${cluster_to_kubeconfig_filepath[$cluster_id]}"
if [ -z "$cluster_id" ] || [ -z "$deployment_id" ] ; then
    echo "invalid portal_id : $portal_id"
    print_portal_id_values
    exit 1
fi
if [ -z "$profile_id" ] ; then
    echo "unable to map cluster_id '$cluster_id' to aws profile"
    exit 1
fi
if [ -z "$kubeconfig_filepath" ] ; then
    echo "unable to map cluster_id '$cluster_id' to kubeconfig"
    exit 1
fi

# REAUTHENTICATE
if [ "$profile_id" == "$PROFILE_ID_PUBLIC" ] ; then
    /data/portal-cron/scripts/authenticate_service_account.sh public
elif [ "$profile_id" == "$PROFILE_ID_EKS" ] ; then
    /data/portal-cron/scripts/authenticate_service_account.sh eks
else
    echo "invalid profile_id: '$profile_id'"
    exit 1
fi

kubeconfig_arg="--kubeconfig=$kubeconfig_filepath"

"$KUBECTL_BINARY" $kubeconfig_arg set env deployment "$deployment_id" --env="LAST_RESTART=$(date)"
webapp_restart_status=$?
if [ $webapp_restart_status -ne 0 ] ; then
    echo "warning : the attempt to restart portal '$portal_id' failed : $KUBECTL_BINARY returned status $webapp_restart_status" >&2
    echo "          nonetheless, a command to clear the persistence cache will now occur." >&2
fi

cache_service_basename="${portal_to_cache_service_basename[$portal_id]}"
cache_leader_pod_name="${cache_service_basename}-master-0"
cache_database_number="${portal_to_cache_database_number[$portal_id]}"
if [ -n "$cache_service_basename" ] && database_number_is_valid "$cache_database_number" ; then
    "$KUBECTL_BINARY" $kubeconfig_arg exec -t -n "default" "$cache_leader_pod_name" -- "/bin/bash" -c "redis-cli -a \$REDIS_PASSWORD -n $cache_database_number FLUSHDB"
    cache_flush_status=$?
    if [ $cache_flush_status -ne 0 ] ; then
        echo "error encountered during attempt to clear portal persistence cache for portal $portal_id : $KUBECTL_BINARY returned status $cache_flush_status" >&2
        exit 1
    fi
else
    echo "portal $portal_id has no persistence cache service or has no valid database number. skipping."
    exit 0
fi
exit 0
