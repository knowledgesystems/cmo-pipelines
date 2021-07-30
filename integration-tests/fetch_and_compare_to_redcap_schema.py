#!/usr/bin/python

import argparse
import sys
import json
import os
import requests
import subprocess

# ---------------------------------------------
# Author: Avery Wang

# Script for integration testing of a pull request
# Determines which datasource schema to test based on tags on pull request
# Will fetch and export from redcap accordingly (i.e fetch from darwin and export all darwin based projects)
# Compares headers between datasource fetch and all related redcap projects
# Different headers imply mismatched data schema and returns false
# lib directory holding all jars must be provided (redcap_pipelines, ddp, cvr, crdb, darwin)
# seperate directories to hold fetched files and redcap exports must be provided
# ---------------------------------------------

REDCAP_EXPORTED_FILENAME = "redcap_exported_filename"
DATASOURCE_FETCHED_FILENAME = "fetched_filename"
HEADERS = {"Accept": "application/vnd.github.v4.raw"}
HTTPS_STARTER = "https://"
GITHUB_ISSUES_BASE_URL = "api.github.com/repos/knowledgesystems/cmo-pipelines/issues"
GITHUB_LABELS_ENDPOINT = "/labels"
LABELS_ID_KEY = "id"
SKIP_INTEGRATION_TESTS_LABEL = "1383619983"

LABEL_TO_TEST_MAPPING = {
    '984868388' : "crdb_fetcher",
#   '984867863' : "cvr_fetcher",
#   '984872074' : "darwin_fetcher",
    '984872969' : "ddp_fetcher"
}

# generates a mapping for fetchers to fetched and redcap-exported filenames
# used for determining which files need to be compared
# redcap_project_to_file_mapping:
# { "darwin_fetcher" : {
#       "/path/to/fetch/<darwin_demographics.txt>" : ["/path/to/redcap_export/<hemepact_darwin_demographics.txt>", "/path/to/redcap_export/<mskimapct_darwin_demographics.txt>", ...],
#       "<fetched file name2>" " ["<redcap project export>", ...]
#   }
# }
def get_fetched_file_to_redcap_files_mappings(mapping_file, redcap_directory, fetch_directory):
    mapping_to_return = {}
    with open(mapping_file) as f:
        f.readline()
        for line in f:
            if not line.startswith("#"):
                data = line.rstrip().split("\t")
                fetcher_name = data[0]
                fetched_filename = os.path.join(fetch_directory, data[2])
                exported_filename = data[4]
                if fetcher_name not in mapping_to_return:
                    mapping_to_return[fetcher_name] = {}
                if fetched_filename not in mapping_to_return[fetcher_name]:
                    mapping_to_return[fetcher_name][fetched_filename] = []
                mapping_to_return[fetcher_name][fetched_filename].append(os.path.join(redcap_directory, exported_filename))
    return mapping_to_return

# generates a mapping from redcap export filename to redcap project
# { "data_clinical_hemepact_data_clinical.txt" : "hemepact_data_clinical" }
def get_redcap_file_to_redcap_project_mappings(mapping_file, redcap_directory):
    mapping_to_return = {}
    with open(mapping_file) as f:
        f.readline()
        for line in f:
            if not line.startswith("#"):
                data = line.rstrip().split("\t")
                redcap_project = data[3]
                exported_filename = data[4]
                mapping_to_return[os.path.join(redcap_directory, exported_filename)] = redcap_project
    return mapping_to_return

# parser for github-credentials file to pull out username and password:
# expected format https://<username>:<password>@github.com
def get_github_credentials(credentials_file):
    credentials_line = open(credentials_file,"r").read().rstrip()
    credentials_line = credentials_line.replace("https://", "")
    credentials_line = credentials_line.replace("@github.com", "")
    credentials = credentials_line.split(":")
    return (credentials[0], credentials[1])

def get_github_pat(personal_access_token_file):
    token = open(personal_access_token_file, "r").read().rstrip()
    return token

# given a pull request number - return the associated tag-names/ids
def get_labels_on_pull_request(username, password, token, pull_request_number):
    url = HTTPS_STARTER + username + ":" + token + "@" + GITHUB_ISSUES_BASE_URL + "/" + pull_request_number + GITHUB_LABELS_ENDPOINT
    github_json_response = requests.get(url)
    github_response = json.loads(github_json_response.text)
    label_ids = [str(label[LABELS_ID_KEY]) for label in github_response if LABELS_ID_KEY in label]
    return label_ids

# exports all required redcap projects that need to be compared
# looks p project names inside secondary map of exported file name to redcap project
def export_redcap_projects(fetchers_to_test, fetched_file_to_redcap_file_mappings, redcap_file_to_redcap_project_mappings, redcap_directory, lib, truststore, truststore_password):
    for fetcher in fetchers_to_test:
        print "testing fetcher: " + fetcher
        for fetched_file, matching_redcap_exports in fetched_file_to_redcap_file_mappings[fetcher].items():
            for redcap_export in matching_redcap_exports:
                redcap_request = "java -Djavax.net.ssl.trustStore=" + truststore + "  -Djavax.net.ssl.trustStorePassword=" + truststore_password + " -jar " + os.path.join(lib, "redcap_pipeline.jar") + " -e -r -p " + redcap_file_to_redcap_project_mappings[redcap_export] + " -d " + redcap_directory
                redcap_status = subprocess.call(redcap_request, shell = True)
                if redcap_status > 0:
                    print "Redcap request: '" + redcap_request + "' returned a non-zero exit status"
                    sys.exit(1)
    print "Done exporting redcap projects"

def crdb_fetch(lib, fetch_directory):
    return "java -jar " + os.path.join(lib, "crdb_fetcher.jar") + " -d " + fetch_directory

# darwin fetch currently does not work because jenkins machine cannot access darwin
def darwin_fetch(lib, fetch_directory):
    return "java -jar " + os.path.join(lib, "darwin_fetcher.jar") + " -d " + fetch_directory + " -s mskimpact_heme -c 0"

def cvr_fetch(lib, fetch_directory, truststore, truststore_password):
    redcap_request = "java -Djavax.net.ssl.trustStore=" + truststore + "  -Djavax.net.ssl.trustStorePassword=" + truststore_password + " -jar " + os.path.join(lib, "redcap_pipeline.jar") + " -e -r -p hemepact_data_clinical -d " + fetch_directory
    redcap_status = subprocess.call(redcap_request, shell = True)
    if redcap_status > 0:
        print "Redcap request: '" + redcap_request + "' returned a non-zero exit status"
        sys.exit(1)
    mv_status = subprocess.call("mv " + os.path.join(fetch_directory, "data_clinical_hemepact_data_clinical.txt") + " " + os.path.join(fetch_directory, "data_clinical_cvr.txt"), shell = True)
    if mv_status > 0:
        print "Subprocess call returned a non-zero exit status"
        sys.exit(1)
    return "java -jar " + os.path.join(lib, "cvr_fetcher.jar") + " -d " + fetch_directory + " -n data_clinical_cvr.txt -i mskimpact_heme -r 50 -t"

def ddp_fetch(lib, fetch_directory):
    with open(os.path.join(fetch_directory, "test_patient_list.txt"), "w") as f:
        f.write("P-0000001")
    return "java -Demail.subject=\"[TEST] DDP Pipeline errors\" -jar " + os.path.join(lib, "ddp_fetcher.jar") + " -f survival,diagnosis -o " + fetch_directory + " -p " + os.path.join(fetch_directory, "test_patient_list.txt")

def ddp_pediatrics_fetch(lib, fetch_directory):
    with open(os.path.join(fetch_directory, "test_patient_list.txt"), "w") as f:
        f.write("P-0000001")
    return "java -Dddp.clinical_filename=data_clinical_ddp_pediatrics.txt -Demail.subject=\"[TEST] DDP Pipeline errors\" -jar " + os.path.join(lib, "ddp_fetcher.jar") + " -f survival,diagnosis,radiation,chemotherapy,surgery -o " + fetch_directory + " -p " + os.path.join(fetch_directory, "test_patient_list.txt")

# dictionary/switch-like function that calls fetch-function based on which fetchers are included in github tags
def fetch_data_source_files(fetchers_to_test, fetch_directory, lib, truststore, truststore_password):
    datasource_fetches = {
        "crdb_fetcher" : [crdb_fetch],
        "cvr_fetcher" : [cvr_fetch],
        "darwin_fetcher" : [darwin_fetch],
        "ddp_fetcher" : [ddp_fetch, ddp_pediatrics_fetch]
    }
    for fetcher in fetchers_to_test:
        fetcher_commands = [fetcher_command(lib, fetch_directory, truststore, truststore_password) if fetcher == "cvr_fetcher" else fetcher_command(lib, fetch_directory) for fetcher_command in datasource_fetches.get(fetcher, lambda: "Invalid fetcher key: " + fetcher)]
        for fetcher_command in fetcher_commands:
            print "calling: " +  fetcher_command
            fetcher_status = subprocess.call(fetcher_command, shell = True)
            if fetcher_status > 0:
                print "Fetcher call: '" + fetcher_command + "' returned a non-zero exit status"
                exit(1)

def verify_data_schema(fetchers_to_test, fetched_file_to_redcap_file_mappings):
    for fetcher in fetchers_to_test:
        for fetched_file, matching_redcap_exports in fetched_file_to_redcap_file_mappings[fetcher].items():
            for redcap_export in matching_redcap_exports:
                print "comparing headers in " + fetched_file + " to " + redcap_export
                if not headers_are_equal(fetched_file, redcap_export):
                    print "Error, data schema is different in redcap and datafetch. Fetch: " + fetched_file + " Redcap: " + redcap_export
                    print "Attributes found in one but not the other: " + ', '.join(set(get_header(fetched_file)) ^ set(get_header(redcap_export)))
                    sys.exit(1)
        print "Sucessful schema comparison for fetcher: " + fetcher

def headers_are_equal(fetched_file, redcap_export):
    fetched_file_header = set(get_header(fetched_file))
    redcap_export_header = set(get_header(redcap_export))
    return fetched_file_header == redcap_export_header

# returns header as list of attributes
def get_header(file):
    header = []
    with open(file, "r") as header_source:
        for line in header_source:
            if not line.startswith("#"):
                header = line.rstrip().split('\t')
                break
    return header

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--fetch-directory", help = "Path to directory where fetches were saved", required = True)
    parser.add_argument("-g", "--credentials-file", help = "File containing git credentials", required = True)
    parser.add_argument("-l", "--lib", help = "Path to directory where built jars were saved", required = True)
    parser.add_argument("-n", "--number", help = "PR number", required = True)
    parser.add_argument("-r", "--redcap-directory", help = "Path to directory where exported redcap projects were saved", required = True)
    parser.add_argument("-s", "--truststore", help = "Keystore with SSL certs", required = True)
    parser.add_argument("-p", "--truststore-password", help = "password for truststore", required = True)
    parser.add_argument("-t", "--git-token", help = "File containing git PAT", required = True)
    args = parser.parse_args()

    credentials_file = args.credentials_file
    fetch_directory = args.fetch_directory
    lib = args.lib
    pull_request_number = str(args.number)
    redcap_directory = args.redcap_directory
    truststore = args.truststore
    truststore_password = args.truststore_password
    personal_access_token_file = args.git_token

    # initialize fetched file to redcap exported file mappings for testing
    # which fetcher test to run will be determined by returned tags
    fetched_file_to_redcap_file_mappings = get_fetched_file_to_redcap_files_mappings("fetcher-file-project-map.txt", redcap_directory, fetch_directory)
    redcap_file_to_redcap_project_mappings = get_redcap_file_to_redcap_project_mappings("fetcher-file-project-map.txt", redcap_directory)

    # get the labels for a pull request from github (may include non-test related tags)
    username,password = get_github_credentials(credentials_file)
    git_token = get_github_pat(personal_access_token_file)
    pull_request_labels = get_labels_on_pull_request(username, password, git_token, pull_request_number)
    # returned github pull requests filtered to only include integration test tags (ddp, cvr, crdb)

    fetchers_to_test = [LABEL_TO_TEST_MAPPING[label] for label in pull_request_labels if label in LABEL_TO_TEST_MAPPING]
    # add all integration tests if none were labeled
    if len(fetchers_to_test) == 0:
        fetchers_to_test = LABEL_TO_TEST_MAPPING.values()
    # run tests if no skip-integration-tests label on PR
    if len(fetchers_to_test) > 0 and SKIP_INTEGRATION_TESTS_LABEL not in pull_request_labels:
        export_redcap_projects(fetchers_to_test, fetched_file_to_redcap_file_mappings, redcap_file_to_redcap_project_mappings, redcap_directory, lib, truststore, truststore_password)
        fetch_data_source_files(fetchers_to_test, fetch_directory, lib, truststore, truststore_password)
        verify_data_schema(fetchers_to_test, fetched_file_to_redcap_file_mappings)
    print "Test passed successfuly for all tagged fetchers"
    sys.exit(0)

main()
