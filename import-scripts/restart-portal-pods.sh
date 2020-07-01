#!/usr/bin/env bash

app_name="$(basename $0)"

KUBECTL_BINARY=kubectl
if ! which $KUBECTL_BINARY > /dev/null 2>&1 ; then
    echo "Error : $app_name requires $KUBECTL_BINARY, which was not found in the current PATH"
    exit 1
fi

declare -A portal_to_deployment_map
portal_to_deployment_map["public"]="cbioportal-spring-boot"
portal_to_deployment_map["genie-public"]="cbioportal-backend-genie-public"
portal_to_deployment_map["genie-private"]="cbioportal-backend-genie-private"
portal_to_deployment_map["genie-archive"]="cbioportal-backend-genie-archive"
## TODO : uncomment the following line when we have defined a kubernetes cbioportal-backend-msk deployment
# portal_to_deployment_map["msk"]="cbioportal-backend-msk"

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in ${!portal_to_deployment_map[@]} ; do
        echo "	$portal"
    done
}

portal_id=$1
if [ -z $portal_id ] ; then
    echo "usage : $app_name <portal id>"
    print_portal_id_values
    exit 1
fi
deployment_id=${portal_to_deployment_map[$portal_id]}
if [ -z "$deployment_id" ] ; then
    echo "invalid portal_id : $portal_id"
    print_portal_id_values
fi

$KUBECTL_BINARY set env deployment $deployment_id --env="LAST_RESTART=$(date)"
