import sys
import clinicalfile_utils

REDCAP_EXPORTED_FILENAME = "redcap_exported_filename"
DATASOURCE_FETCHED_FILENAME = "fetched_filename"

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

# function for figuring out which fetcher we are testing (hit git-api?)
# def get_fetcher_to_test():
def main() 
    parser = argparse.ArgumentParser()
    parser.add_argument("-r", "--redcap-directory", help = "Path to directory where exported redcap projects were saved", required = True)
    parser.add_argument("-f", "--fetch-directory", help = "Path to directory where fetches were saved", required = True)

    args = parser.parse_args()

    redcap_directory = args.redcap_directory
    fetch_directory = args.fetch_directory

    redcap_project_to_file_mappings = get_redcap_project_to_file_mappings("fetcher-file-project-map.txt", redcap_directory, fetch_directory)
     
