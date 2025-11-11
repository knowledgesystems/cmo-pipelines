#!/bin/bash
aws eks update-kubeconfig --region us-east-1 --name cbioportal-prod --profile saml
kubectl get pods
echo "exit status $?"
