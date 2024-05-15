#!/usr/bin/env bash

PATH_TO_AUTOMATION_SCRIPT=/data/portal-cron/scripts/automation-environment.sh

PIPELINES_HOSTNAME=pipelines.cbioportal.mskcc.org

# authentication doens't need to be run when running from pipelines
if [ `hostname` == $PIPELINES_HOSTNAME ] ; then
    exit 0
fi

if ! [ -f $PATH_TO_AUTOMATION_SCRIPT ] ; then
    echo "automation-environment.sh could not be found, exiting..."
    exit 2
fi

. $PATH_TO_AUTOMATION_SCRIPT

cluster_account=$1

EKS_ACCOUNT_CREDENTIALS_FILE=$PORTAL_HOME/pipelines-credentials/eks-account.credentials
PUBLIC_ACCOUNT_CREDENTIALS_FILE=$PORTAL_HOME/pipelines-credentials/public-account.credentials

if ! [[ -f $EKS_ACCOUNT_CREDENTIALS_FILE && -f $PUBLIC_ACCOUNT_CREDENTIALS_FILE ]] ; then
    echo "$EKS_ACCOUNT_CREDENTIALS_FILE and $PUBLIC_ACCOUNT_CREDENTIALS_FILE could not be found, exiting..."
    exit 2
fi

if ! [[ -r $EKS_ACCOUNT_CREDENTIALS_FILE && -r $PUBLIC_ACCOUNT_CREDENTIALS_FILE ]] ; then
   echo "$EKS_ACCOUNT_CREDENTIALS_FILE and $PUBLIC_ACCOUNT_CREDENTIALS_FILE could not be read, exiting..."
   exit 2
fi

unset TO_USE_CREDENTIALS_FILE
case $cluster_account in
    "eks")
      TO_USE_CREDENTIALS_FILE=$EKS_ACCOUNT_CREDENTIALS_FILE
      # copy kubeconfig over
      ;;
    "public")
      TO_USE_CREDENTIALS_FILE=$PUBLIC_ACCOUNT_CREDENTIALS_FILE
      ;;
    *)
      echo "Attempting to connect to unrecognized clusterr $cluster_account, exiting..."
      exit 2
      ;;
esac

SERVICE_ACCOUNT_NUMBER="$(grep -rh eks.account.number $TO_USE_CREDENTIALS_FILE | sed 's/eks.account.number=//g')"
SERVICE_ACCOUNT_ROLE="$(grep -rh eks.role $TO_USE_CREDENTIALS_FILE | sed 's/eks.role=//g')"
SERVICE_ACCOUNT_NAME="$(grep -rh eks.account.name $TO_USE_CREDENTIALS_FILE | sed 's/eks.account.name=//g')"
SERVICE_ACCOUNT_PASSWORD="$(grep -rh eks.password $TO_USE_CREDENTIALS_FILE | sed 's/eks.password=//g')"

if [ -z "$SERVICE_ACCOUNT_NUMBER" ] | [ -z "$SERVICE_ACCOUNT_NAME" ] | [ -z "$SERVICE_ACCOUNT_PASSWORD" ] | [ -z "$SERVICE_ACCOUNT_ROLE" ] ; then
    echo "Missing required K8S EKS environment variables , exiting..."
    exit 2
fi

set -euo pipefail

BASEDIR=$(dirname "$0")

if [ "$(docker images -q saml2aws)" = "" ]; then
    docker build -t saml2aws "$BASEDIR"
fi

if [ ! -f "$HOME/.saml2aws" ]; then
    touch "$HOME/.saml2aws"
fi
docker run --rm -i \
    -u "$(id -u):$(id -g)" \
    -v "$HOME/.saml2aws:/saml2aws/.saml2aws" \
    -v "$HOME/.aws:/saml2aws/.aws" \
    saml2aws --role arn:aws:iam::$SERVICE_ACCOUNT_NUMBER:role/$SERVICE_ACCOUNT_ROLE login --force --mfa=Auto --username=$SERVICE_ACCOUNT_NAME --password=$SERVICE_ACCOUNT_PASSWORD --skip-prompt \

set +uo pipefail
