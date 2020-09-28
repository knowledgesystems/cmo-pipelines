#! /usr/bin/env python
#
# Copyright (c) 2018 Memorial Sloan Kettering Cancer Center.
# This library is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
# documentation provided hereunder is on an "as is" basis, and
# Memorial Sloan Kettering Cancer Center
# has no obligations to provide maintenance, support,
# updates, enhancements or modifications.  In no event shall
# Memorial Sloan Kettering Cancer Center
# be liable to any party for direct, indirect, special,
# incidental or consequential damages, including lost profits, arising
# out of the use of this software and its documentation, even if
# Memorial Sloan Kettering Cancer Center
# has been advised of the possibility of such damage.
#
#
# This is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

# ------------------------------------------------------------------------------
# Script which creates a ready-for-import TSV file containing CDD attributes with new assigned URIs
# Github credentials must be passed in to get existing URI mappings
#
# Requires a TSV file containing the following columns: 
#   ATTRIBUTE_TYPE
#   DATATYPE
#   DESCRIPTIONS
#   DISPLAY_NAME
#   NORMALIZED_COLUMN_HEADER
#   PRIORITY
#
# NORMALIZED_COLUMN_HEADER must be filled in, rest are optional and will be filled in with defaults if empty
# URI mapping file will also be updated and automatically pushed to github repository
#
# Script will exit prematurely and do nothing if attribute in file is
# 1) already in CDD
# 2) invalid (starts with number, contains symbols, etc.)
#
# To get usage:
#   python generate_cdd_attribute_tsv.py -h
#
# Authors: Avery Wang
# ------------------------------------------------------------------------------
from clinicalfile_utils import *
import argparse
import base64
import datetime
import json
import re
import requests
import sys

GITHUB_URI_MAPPINGS_FILE_URL = "https://api.github.com/repos/cBioPortal/clinical-data-dictionary/contents/docs/resource_uri_to_clinical_attribute_mapping.txt"
HEADERS = {"Accept": "application/vnd.github.v4.raw"}
NUMBER_OF_DIGITS = 6

ATTRIBUTE_TYPE_KEY = "ATTRIBUTE_TYPE"
CONCEPT_ID_KEY = "CONCEPT_ID"
DATATYPE_KEY = "DATATYPE"
DESCRIPTIONS_KEY = "DESCRIPTIONS"
DISPLAY_NAME_KEY = "DISPLAY_NAME"
NORMALIZED_COLUMN_HEADER_KEY = "NORMALIZED_COLUMN_HEADER"
PRIORITY_KEY = "PRIORITY"

NEW_URI_MAPPING_FILENAME = "new_uri_mapping.txt"
NEW_CDD_ATTRIBUTE_TSV = "new_cdd_attributes.tsv"

# Extracts credentials for later use
def get_github_credentials(credentials_file):
    credentials_line = open(credentials_file,"r").read().rstrip()
    credentials_line = credentials_line.replace("https://", "") 
    credentials_line = credentials_line.replace("@github.com", "")
    credentials = credentials_line.split(":")
    return (credentials[0], credentials[1]) 

# Takes in the github URI mapping file and creates a dictionary from URI to normalized column header
def create_uri_dictionary(uri_mapping_file):
    uri_dictionary = {}
    uri_mappings = uri_mapping_file.text.split("\n")
    for uri_mapping in uri_mappings:
        if uri_mapping: 
            data = uri_mapping.split("\t")
            uri_dictionary[data[0].rstrip()] = data[1].rstrip()
    return uri_dictionary

# Looks for non-ASCII characters
# Uses 'isascii()' attribute for Java 3.7 and over
def is_ASCII(s):
    if (hasattr(s, 'isascii')):
        return s.isascii()
    for c in s:
        if (ord(c) > 128): 
            return False
    return True
 
# Given a tab delimited file - generate a list of dictionaries where
# each dictionary is an attribute {NORMALIZED_COLUMN_HEADER: normalized_column_header, DESCRIPTION: description, ...}
# also adds URI under CONCEPT_ID key
def load_new_cdd_attributes(new_attributes_file, uri_dictionary):
    header = get_header(new_attributes_file)
    try:
        normalized_column_header_position = header.index(NORMALIZED_COLUMN_HEADER_KEY)
    except:
        print(sys.stderr, "Error: file does not have normalized column header defined... aborting")
        sys.exit(2) 
    new_cdd_attributes = []
    invalid_attributes = []
    non_ascii_characters = []
    last_known_uri_number = get_last_known_uri_number(uri_dictionary)
    with open(new_attributes_file, "r") as f:
        # skip first line
        next(f)
        for line in f:
            if not line.rstrip():
                continue
            new_attribute = {}
            data = line.rstrip().split("\t")
            if data[normalized_column_header_position] in uri_dictionary.values() or not is_valid_attribute_name(data[normalized_column_header_position]):
                invalid_attributes.append(data[normalized_column_header_position])
            for position in range(len(header)):
                try:
                    if (is_ASCII(data[position])):
                        new_attribute[header[position]] = insert_value(header[position], data[position], data[normalized_column_header_position])
                    else:
                        non_ascii_characters.append([data[4], data[position]])
                except:
                    new_attribute[header[position]] = insert_value(header[position], "", data[normalized_column_header_position])
            new_attribute[CONCEPT_ID_KEY] = get_next_uri(last_known_uri_number)
            new_cdd_attributes.append(new_attribute)
            last_known_uri_number += 1
    if len(invalid_attributes) > 0:
        print(sys.stderr, "Invalid attributes (already exists/invalid name) in added: " + "\t".join(invalid_attributes) + "... aborting")
        sys.exit(2)
    if len(non_ascii_characters) > 0:
        print(sys.stderr, "Non-ASCII characters found in the following: ")
        for i in non_ascii_characters:
          print(i[0] + " --> " + i[1] )
        sys.exit(2)
    return new_cdd_attributes 

# boolean to check if new attributes follow naming conventions
def is_valid_attribute_name(attribute_name):
    return attribute_name[0].isalpha() and re.match(r'^\w+$', attribute_name) and attribute_name.upper() == attribute_name

# Determines whether a default value is needed and if so which one
def insert_value(key, value, normalized_column_header_name):
    if value is None or value.rstrip() == "":
        if key == ATTRIBUTE_TYPE_KEY:
            return "SAMPLE"
        elif key == CONCEPT_ID_KEY:
            return
        elif key == DATATYPE_KEY:
            return "STRING"
        elif key == DESCRIPTIONS_KEY:
            return normalized_column_header_name.replace("_", " ").title()
        elif key == DISPLAY_NAME_KEY:
            return normalized_column_header_name.replace("_", " ").title()
        elif key == PRIORITY_KEY:
            return "1"
        else: 
            print (sys.stderr, "No name is defined in normalized column header column... aborting")
            sys.exit(2)
    else:
        return value

# Get the last known (maximum) URI number
# Assumes URI is a single character followed by an integer
def get_last_known_uri_number(uri_dictionary):
    uri_list = sorted(uri_dictionary.keys())
    last_uri = uri_list[-1]
    last_uri_number = int(last_uri[1:])
    return last_uri_number

# Generates next URI string (CXXXXXX)
# Assumes URI is C followed by six digits
def get_next_uri(last_known_uri_number):
    next_uri_number = last_known_uri_number + 1
    next_uri = "C" + str(next_uri_number).zfill(NUMBER_OF_DIGITS)
    return next_uri

# Create the data being passed in to HTTP put request
def create_data(new_uri_mapping_file_path, github_file_url):
    current_date = datetime.datetime.now().strftime("%Y-%m-%d")
    base64_encoded_data = base64.b64encode(open(new_uri_mapping_file_path,"rb").read()) 
    to_return = {"message" : current_date + ": updated with new URIs",
        "sha" : get_sha(github_file_url),
        "content" : base64_encoded_data,
        "path" : new_uri_mapping_file_path
    }
    return to_return

# Gets hash of github file that new commit will be pushed to
def get_sha(github_file_url):
    request = requests.get(github_file_url)
    return request.json()["sha"]

# Add new URI mappings to dictionary for new attributes
def insert_new_uris(uri_dictionary, new_cdd_attributes):
    for attribute in new_cdd_attributes:
        uri_dictionary[attribute[CONCEPT_ID_KEY]] = attribute[NORMALIZED_COLUMN_HEADER_KEY]

# Push new URI mappings file into github 
def update_github_uri_mappings(uri_dictionary, output_directory, github_file_url, username, password):
    new_uri_mapping_file_path = os.path.join(output_directory, NEW_URI_MAPPING_FILENAME)
    new_uri_mapping_file = open(new_uri_mapping_file_path, "w")
    for uri in sorted(uri_dictionary.keys()):
        new_uri_mapping_file.write(uri + "\t" + uri_dictionary[uri] + "\n")
    new_uri_mapping_file.close()
    data = create_data(new_uri_mapping_file_path, github_file_url)
    session = requests.Session()
    session.auth = (username, password)
    response = session.put(github_file_url, data = json.dumps(data))
    if response.status_code != 200:
        print (sys.stderr, "Error encountered when pushing new mappings into github, returned status code: " + str(response.status_code) + "... aborting")
        sys.exit(2)
    print ("Finished updating uri mapping files in github")
    os.remove(new_uri_mapping_file_path)

# Writes out list of attributes into a tab-delimited file (with concept-ids)
def write_cdd_attribute_tsv(new_cdd_attributes, output_directory, new_attributes_file):
    new_cdd_attributes_tsv_path = os.path.join(output_directory, new_attributes_file)
    if os.path.isfile(new_cdd_attributes_tsv_path):
        os.remove(new_cdd_attributes_tsv_path)
    header = sorted(new_cdd_attributes[0].keys())
    new_cdd_attributes_tsv = open(new_cdd_attributes_tsv_path, "w")
    new_cdd_attributes_tsv.write("\t".join(header) + "\n")
    for attribute in new_cdd_attributes:
        new_cdd_attributes_tsv.write("\t".join([attribute[property] for property in sorted(new_cdd_attributes[0].keys())]) + "\n")
    new_cdd_attributes_tsv.close() 
    print ("Finished writing file to: " + new_cdd_attributes_tsv_path)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--attributes-file', action = 'store', dest = 'new_attributes_file', required = True, help = 'Tab delimited file containing new attributes for CDD"')
    parser.add_argument('-c', '--credentials-file', action = 'store', dest = 'credentials_file', required = True, help = 'Path to file containing github credentials')
    parser.add_argument('-d', '--output-directory', action = 'store', dest = 'output_directory', required = True, help = 'The directory where new file should be written to')
    args = parser.parse_args()

    new_attributes_file = args.new_attributes_file
    credentials_file = args.credentials_file
    output_directory = args.output_directory
   
    if not os.path.isfile(new_attributes_file):
        print (sys.stderr, "Error: attributes file " + new_attributes_file + " does not exist... aborting")
        sys.exit(2)
    if not os.path.isfile(credentials_file): 
        print (sys.stderr, "Error: credentials file " + credentials_file + " does not exist... aborting")
        sys.exit(2) 
    if not os.path.isdir(output_directory):
        print (sys.stderr, "Error: specified directory " + output_directory + " does not exist... aborting")
        sys.exit(2)

    username, password = get_github_credentials(credentials_file)
    try:
        urifile = requests.get(GITHUB_URI_MAPPINGS_FILE_URL, auth = (username, password), headers = HEADERS)
    except requests.exceptions.SSLError:
        print (sys.stderr, "OpenSSL version is outdated -- try running 'pip install 'requests[security]' or updating openssl version.")
        sys.exit(2)
    if urifile.status_code != 200:
        print (sys.stderr, "Error while retrieving existing URI mappings with returned status code: " + urifile.status_code + "... aborting")
        sys.exit(2)
    uri_dictionary = create_uri_dictionary(urifile)
    
    new_cdd_attributes = load_new_cdd_attributes(new_attributes_file, uri_dictionary)
    insert_new_uris(uri_dictionary, new_cdd_attributes)
    update_github_uri_mappings(uri_dictionary, output_directory, GITHUB_URI_MAPPINGS_FILE_URL, username, password)
    write_cdd_attribute_tsv(new_cdd_attributes, output_directory, NEW_CDD_ATTRIBUTE_TSV)

if __name__ == "__main__":
    main()
