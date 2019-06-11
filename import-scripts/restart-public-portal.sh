#!/usr/bin/env bash

KUBECTL_BINARY=kubectl
if ! which $KUBECTL_BINARY > /dev/null 2>&1 ; then
    echo "Error : restart-public-portal.sh requires $KUBECTL_BINARY, which was not found in the current PATH"
    exit 1
fi

$KUBECTL_BINARY set env deployment cbioportal-spring-boot --env="LAST_RESTART=$(date)"
