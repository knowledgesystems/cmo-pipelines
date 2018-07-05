import argparse
import sys
import clinicalfile_utils
import json
import requests

REDCAP_EXPORTED_FILENAME = "redcap_exported_filename"
DATASOURCE_FETCHED_FILENAME = "fetched_filename"
HEADERS = {"Accept": "application/vnd.github.v4.raw"}
GITHUB_ISSUES_BASE_URL = "https://api.github.com/repos/knowledgesystems/cmo-pipelines/issues"
GITHUB_LABELS_ENDPOINT = "/labels"
LABELS_NAME_KEY = "name"

# TO DO: change this to load mapping file as dictionary with header as keys
def get_redcap_project_to_file_mappings(mapping_file):
    mapping_to_return = {}
    with open(mapping_file) as f:
        f.readline()
        for line in f:
            data = line.rstrip().split("\t")
            fetcher_name = data[0]
            fetched_filename = data[2]
            redcap_project = data[3]
            exported_filename = data[4]
            if fetcher_name not in redcap_project_to_file_mappings:
                redcap_project_to_file_mappings[fetcher_name] = {}
            redcap_project_to_file_mappings[fetcher_name][redcap_project][REDCAP_EXPORTED_FILENAME] = redcap_directory + exported_filename
            redcap_project_to_file_mappings[fetcher_name][redcap_project][DATASOURCE_FETCHED_FILENAME] = fetch_directory + fetched_filename
    return redcap_project_to_file_mappings

def get_github_credentials(credentials_file):
    credentials_line = open(credentials_file,"rb").read().rstrip()
    credentials_line = credentials_line.replace("https://", "") 
    credentials_line = credentials_line.replace("@github.com", "")
    credentials = credentials_line.split(":")
    return (credentials[0], credentials[1]) 

def get_labels_on_pull_request(username, password, pull_request_number):
    url = GITHUB_ISSUES_BASE_URL + "/" + pull_request_number + GITHUB_LABELS_ENDPOINT
    github_json_response = requests.get(url, auth = (username, password))
    github_response = json.loads(github_json_response.text)
    label_names = [label["LABELS_NAME_KEY"] for label in github_response]
    return label_names

# function for figuring out which fetcher we are testing (hit git-api?)
# def get_fetcher_to_test():
def main(): 
    parser = argparse.ArgumentParser()
    parser.add_argument("-r", "--redcap-directory", help = "Path to directory where exported redcap projects were saved", required = True)
    parser.add_argument("-f", "--fetch-directory", help = "Path to directory where fetches were saved", required = True)
    parser.add_argument("-g", "--credentials-file", help = "File containing git credentials", required = True)
    parser.add_argument("-n", "--number", help = "PR number", required = True)
    args = parser.parse_args()

    redcap_directory = args.redcap_directory
    fetch_directory = args.fetch_directory
    credentials_file = args.credentials_file
    pull_request_number = str(args.number)

    pull_request_labels = get_labels_on_pull_request(username, password, pull_request_number)

#    redcap_project_to_file_mappings = get_redcap_project_to_file_mappings("fetcher-file-project-map.txt", redcap_directory, fetch_directory)

main()
    
         
