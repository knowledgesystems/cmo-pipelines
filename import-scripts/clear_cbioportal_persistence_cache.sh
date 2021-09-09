#!/usr/bin/env bash

app_name="$(basename $0)"

if [ -z "$PORTAL_HOME" ] ; then
    echo "error : required environment variable \$PORTAL_HOME is unset. Use automation_environment.sh." >&2
    exit 1
fi

CACHE_API_KEY_FILE=$PORTAL_HOME/pipelines-credentials/cache.api.key
if ! [ -r "$CACHE_API_KEY_FILE" ] ; then
    echo "error : CACHE_API_KEY_FILE is set to '$CACHE_API_KEY_FILE' but that path cannot be read." >&2
    exit 1
fi

unset portal_to_deployment_map
declare -A portal_to_deployment_map
portal_to_deployment_map["public"]="cbioportal-spring-boot"
portal_to_deployment_map["genie-public"]="cbioportal-backend-genie-public"
portal_to_deployment_map["genie-private"]="cbioportal-backend-genie-private"
portal_to_deployment_map["genie-archive"]="cbioportal-backend-genie-archive"
portal_to_deployment_map["triage"]="eks-triage"
portal_to_deployment_map["msk"]="eks-msk"
portal_to_deployment_map["msk-beta"]="eks-msk-beta"
portal_to_deployment_map["private"]="eks-private"
portal_to_deployment_map["su2c"]="eks-su2c"
portal_to_deployment_map["acc"]="eks-acc"
portal_to_deployment_map["glioma"]="eks-glioma"
portal_to_deployment_map["immunotherapy"]="eks-immunotherapy"
portal_to_deployment_map["kras"]="eks-kras"
portal_to_deployment_map["pdx"]="eks-pdx"
portal_to_deployment_map["poetic"]="eks-poetic"
portal_to_deployment_map["prostate"]="eks-prostate"
portal_to_deployment_map["sclc"]="eks-sclc"
portal_to_deployment_map["target"]="eks-target"
portal_to_deployment_map["triage"]="eks-triage"

unset portal_to_url
declare -A portal_to_url
portal_to_url["public"]="https://cbioportal.org"
portal_to_url["genie-public"]="https://genie.cbioportal.org"
portal_to_url["genie-private"]="https://genie-private.cbioportal.org"
portal_to_url["genie-archive"]="https://genie-archive.cbioportal.org"
portal_to_url["triage"]="https://triage.cbioportal.mskcc.org"
portal_to_url["msk"]="https://cbioportal.mskcc.org"
portal_to_url["msk-beta"]="https://msk-beta.cbioportal.mskcc.org"
portal_to_url["private"]="https://private.cbioportal.mskcc.org"
portal_to_url["su2c"]="https://su2c.cbioportal.mskcc.org"
portal_to_url["acc"]="https://acc.cbioportal.mskcc.org"
portal_to_url["glioma"]="https://glioma.cbioportal.mskcc.org"
portal_to_url["immunotherapy"]="https://immunotherapy.cbioportal.mskcc.org"
portal_to_url["kras"]="https://kras.cbioportal.mskcc.org"
portal_to_url["pdx"]="https://pdx.cbioportal.mskcc.org"
portal_to_url["poetic"]="https://poetic.cbioportal.mskcc.org"
portal_to_url["prostate"]="https://prostate.cbioportal.mskcc.org"
portal_to_url["sclc"]="https://sclc.cbioportal.mskcc.org"
portal_to_url["target"]="https://target.cbioportal.mskcc.org"
#TODO : delete these overrides .. these are in place for testing until our portals are made publicly visible
portal_to_url["msk"]="https://msk.cbioportal.aws.mskcc.org"
portal_to_url["msk-beta"]="https://beta.cbioportal.mskcc.org"
portal_to_url["private"]="https://private.cbioportal.aws.mskcc.org"
portal_to_url["su2c"]="https://su2c.cbioportal.aws.mskcc.org"
portal_to_url["acc"]="https://acc.cbioportal.aws.mskcc.org"
portal_to_url["glioma"]="https://glioma.cbioportal.aws.mskcc.org"
portal_to_url["immunotherapy"]="https://immunotherapy.cbioportal.aws.mskcc.org"
portal_to_url["kras"]="https://kras.cbioportal.aws.mskcc.org"
portal_to_url["pdx"]="https://pdx.cbioportal.aws.mskcc.org"
portal_to_url["poetic"]="https://poetic.cbioportal.aws.mskcc.org"
portal_to_url["prostate"]="https://prostate.cbioportal.aws.mskcc.org"
portal_to_url["sclc"]="https://sclc.cbioportal.aws.mskcc.org"
portal_to_url["target"]="https://target.cbioportal.aws.mskcc.org"

function print_portal_id_values() {
    echo "valid portal ids:"
    for portal in ${!portal_to_deployment_map[@]} ; do
        echo "  $portal"
    done
}

portal_id=$1
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

portal_url=${portal_to_url[$portal_id]}
if [ -n "$portal_url" ] ; then
    CACHE_API_KEY=`cat $CACHE_API_KEY_FILE`
    CACHE_API_URL="$portal_url/api/cache?springManagedCache=true"
    OK=200
    http_code=`curl -X DELETE -o /dev/null --silent --write-out '%{http_code}\n' "$CACHE_API_URL" -H "accept: text/plain" -H "X-API-KEY: $CACHE_API_KEY"`
    if [ "$http_code" -ne $OK ] ; then
        echo "error encountered during attempt to clear portal cache for portal $portal_id : received http status code $http_code" >&2
        exit 1
    fi
else
    echo portal $portal_id has no persistence cache reset url set. skipping.
    exit 0
fi
