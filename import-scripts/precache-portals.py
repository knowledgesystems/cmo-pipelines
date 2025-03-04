from dateutil.parser import parse
from multiprocessing.dummy import Pool
import argparse
import datetime
import json
import os
import requests
import subprocess
import sys
import time

DEPLOYMENTS = ["cbioportal-backend-genie-public",
                "cbioportal-backend-genie-private",
                "cbioportal-backend-master"]

DEPLOYMENT_TO_URL_MAP = {"cbioportal-backend-genie-public":"genie",
                         "cbioportal-backend-genie-private":"genie-private"
                        }

PROTECTED_PORTALS=["cbioportal-backend-genie-public",
                    "cbioportal-backend-genie-private",
                    "cbioportal-backend-genie-private"]

MINUTES_BETWEEN_CHECK = 5
MAX_NUMBER_OF_ATTEMPTS = 18
# ----------------------------------------------------------
# Functions wrapping k8s/timing checks

def authenticate_service_account():
    try:
        subprocess.check_output(["/data/portal-cron/scripts/authenticate_service_account.sh public"])
    except:
        print "Attempt to authenticate to k8s cluster failed with non-zero exit status, exiting..."
        sys.exit(1)

# get pods associated with deployment
def get_deployed_pods(deployment_name):
    deployed_pods = subprocess.check_output(["/bin/bash", "-c", "kubectl get pods | grep %s | sed 's/\s\s*/\t/g' | cut -f1" % (deployment_name)]).rstrip().split("\n")
    return deployed_pods

# return datetime object representing last deployment restart
def get_last_deployment_restart_datetime(deployment_name):
    last_restart_time =  subprocess.check_output(["/bin/bash", "-c", "kubectl describe deployment %s | grep 'LAST_RESTART'" % (deployment_name)]).strip().split()[2:]
    return parse(" ".join(last_restart_time))

def get_last_pod_start_datetime(deployment):
    pod_names = get_deployed_pods(deployment)
    pod_start_datetimes = []
    for pod_name in pod_names:
        pod_start_time = subprocess.check_output(["/bin/bash", "-c", "kubectl describe pod %s | grep -C1 'State.*Running' | grep 'Started'" % (pod_name)]).strip().split()[1:]
        pod_start_datetimes.append(parse(" ".join(pod_start_time)))
    return sorted(pod_start_datetimes, reverse = True)[0]

# check if restart occurred since last restart (minute offset should be set according to how frequently script is ran)
# use date command so timezone is auto-included
def restart_occurred_since_last_check(last_deployment_restart_datetime):
    current_datetime = parse(subprocess.check_output(["/bin/bash", "-c", "date +'%b %d %H:%M:%S %z %Y'"]))
    last_check_datetime = current_datetime - datetime.timedelta(minutes=MINUTES_BETWEEN_CHECK)
    return last_deployment_restart_datetime > last_check_datetime

def is_valid_deployments(deployments):
    return all([True if deployment in DEPLOYMENTS else False for deployment in deployments])
# ----------------------------------------------------------
# Functions for interacting with cbio api

# get base url depending on deployment 
def get_cbio_url(deployment):
    return "http://www.cbioportal.org" if deployment == "cbioportal-backend-master" else "http://%s.cbioportal.org" % (DEPLOYMENT_TO_URL_MAP[deployment])

# get DAT for submitting requests for private portal
def get_authorization_header(deployments):
    if all([deployment not in PROTECTED_PORTALS for deployment in deployments]):
        return None 
    if "DATA_ACCESS_TOKEN" not in os.environ:
        print "No value set for environment variable DATA_ACCESS_TOKEN. Unable to submit requests, exiting..."
        sys.exit(1)
    return {"Authorization" : "Bearer %s" % (os.environ["DATA_ACCESS_TOKEN"])}

def get_cancer_studies(cbio_url, authorization_header):
    cancer_study_ids = []
    study_api_response = requests.get("%s/api/studies?direction=ASC&pageNumber=0&pageSize=10000000&projection=SUMMARY" % (cbio_url), headers = authorization_header).json()
    for study in study_api_response:
        cancer_study_ids.append(study["studyId"])
    return cancer_study_ids

def cache_structural_variant_genes(cbio_url, post_body, authorization_header):
    requests.post("%s/api/structuralvariant-genes/fetch" % (cbio_url), headers = authorization_header, json = post_body)

def cache_mutated_genes(cbio_url, post_body, authorization_header):
    requests.post("%s/api/mutated-genes/fetch" % (cbio_url), headers = authorization_header,  json = post_body)

def cache_cna_genes(cbio_url, post_body, authorization_header):
    requests.post("%s/api/cna-genes/fetch" % (cbio_url), headers = authorization_header, json = post_body)

# construct JSON post body for study view endpoints
# {sampleIdentifiers: [{sampleId: S01, study: study1}, {sampleId: S02, study: study1}...]}
def construct_sample_list_post_body(cbio_url, cancer_study, authorization_header):
    samples = []
    sample_api_response = requests.get("%s/api/studies/%s/samples?direction=ASC&pageNumber=0&pageSize=10000000&projection=SUMMARY" % (cbio_url, cancer_study), headers = authorization_header).json()
    for sample in sample_api_response:
        samples.append({"sampleId" : sample["sampleId"], "studyId":sample["studyId"]})
    post_body = json.loads(json.dumps({"sampleIdentifiers" : samples}))
    return post_body

def precache_slow_endpoints(cbio_url, post_body, authorization_header):
    pool = Pool(5)
    pool.apply_async(cache_structural_variant_genes, [cbio_url, post_body, authorization_header])
    pool.apply_async(cache_mutated_genes, [cbio_url, post_body, authorization_header])
    pool.apply_async(cache_cna_genes, [cbio_url, post_body, authorization_header])
# ----------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--deployments", help = "comma seperated list of k8s deplyoments for precaching", required = True)
    args = parser.parse_args()
    
    deployments = map(str.strip, args.deployments.split(','))
    if not is_valid_deployments(deployments):
        print "Invalid deployments supplied, exiting..."
        sys.exit(1)

    authorization_header = get_authorization_header(deployments)
    # run check and precache for every cbio deployment (e.g genie, genie-private,...)
    for deployment in deployments:
        cbio_url = get_cbio_url(deployment) 
        try:
            last_restart_datetime = get_last_deployment_restart_datetime(deployment)
        # exception thrown for cases where deployment was deleted and applied (instead of restarted through script)
        # no LAST_RESTART set yet
        # look for pod start time instead
        except:
            try:
                last_restart_datetime = get_last_pod_start_datetime(deployment)
            except:
                print "Unable to get a restart time... exiting."
                sys.exit(1)
        # if restart happened since last time we ran the check we need to precache
        if restart_occurred_since_last_check(last_restart_datetime):
            # wait until pod has started up...
            # max time to wait is three minutes
            for attempt_count in range(MAX_NUMBER_OF_ATTEMPTS):
                try:
                    if (requests.get("%s/api/info" % cbio_url, headers = authorization_header).status_code == requests.codes.ok):
                        break
                except requests.exceptions.ConnectionError:
                    # if there is a failure to get a connection, continue in the retry loop
                    pass
                time.sleep(10)
            cancer_study_ids = get_cancer_studies(cbio_url, authorization_header)
            for cancer_study in cancer_study_ids:
                post_body = construct_sample_list_post_body(cbio_url, cancer_study, authorization_header)
                precache_slow_endpoints(cbio_url, post_body, authorization_header)
        else:
            print "no need to fill in cache"

main()
