#! /usr/bin/env python
# ------------------------------------------------------------------------------
# Utility script which adds metadata headers to specified file(s)
# Metadata is pulled from the clinical data dictionary web service (cdd)
# Four lines added at the top (display name, dscriptions, datatype, priority)
# Changes are only made if all input files are valid
# ------------------------------------------------------------------------------

from clinicalfile_utils import write_data, write_header_line, get_header
import argparse
import json
import os
import requests
import shutil
import sys
import tempfile

# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout
DATATYPE_KEY = 'datatype'
DESCRIPTION_KEY = 'description'
DISPLAY_NAME_KEY = 'display_name'
COLUMN_HEADER_KEY = 'column_header'
ATTRIBUTE_TYPE_KEY = 'attribute_type'
PRIORITY_KEY = 'priority'
OVERRIDDEN_STUDY_NAME_KEY = 'name'
DEFAULT_URL = "http://oncotree.mskcc.org/cdd/api/"

PATIENT_CLINICAL_FILE_PATTERN = "data_clinical_patient.txt"
SAMPLE_CLINICAL_FILE_PATTERN = "data_clinical_sample.txt"

def check_valid_studyid(study_id, base_cdd_url):
    query = base_cdd_url + "cancerStudies"
    response = requests.get(query)
    check_response_returned(response)
    response_as_json = json.loads(response.text)
    if study_id not in [overridden_study[OVERRIDDEN_STUDY_NAME_KEY] for overridden_study in response_as_json]:
        print >> ERROR_FILE, 'Invalid study id: ' + study_id + ", there is no associated attributes in CDD"
        sys.exit(2)

def check_response_returned(response):
    if response.status_code != 200:
        print >> ERROR_FILE, "Error code returned from CDD, aborting..."
        sys.exit(2)

def get_clinical_attribute_metadata_from_cdd(study_id, header, base_cdd_url):
    metadata_mapping = {}
    response = requests.post(base_cdd_url + "?cancerStudy=" + study_id if study_id else base_cdd_url, json=list(header))
    check_response_returned(response)
    response_as_json = json.loads(response.text)
    for entry in response_as_json:
        normalized_column_header =  entry[COLUMN_HEADER_KEY]
        display_name = entry[DISPLAY_NAME_KEY]
        description =  entry[DESCRIPTION_KEY]
        datatype = entry[DATATYPE_KEY]
        attribute_type = entry[ATTRIBUTE_TYPE_KEY]
        priority = entry[PRIORITY_KEY]
        metadata_mapping[normalized_column_header] = {
                'DISPLAY_NAME' : display_name,
                'DESCRIPTIONS' : description,
                'DATATYPE' : datatype,
                'ATTRIBUTE_TYPE' : attribute_type,
                'PRIORITY' : priority}
    return metadata_mapping

def write_headers(header, metadata_dictionary, output_file, is_mixed_attribute_types_format):
    name_line = []
    description_line = []
    datatype_line = []
    attribute_type_line = []
    priority_line = []
    for attribute in header:
        if attribute in metadata_dictionary:
            name_line.append(metadata_dictionary[attribute]['DISPLAY_NAME'])
            description_line.append(metadata_dictionary[attribute]['DESCRIPTIONS'])
            datatype_line.append(metadata_dictionary[attribute]['DATATYPE'])
            attribute_type_line.append(metadata_dictionary[attribute]['ATTRIBUTE_TYPE'])
            priority_line.append(metadata_dictionary[attribute]['PRIORITY'])
        else:
            # if attribute is not defined in cdd, use defaults
            name_line.append(attribute.replace("_", " ").title())
            description_line.append(attribute.replace("_", " ").title())
            datatype_line.append('STRING')
            attribute_type_line.append('SAMPLE')
            priority_line.append('1')
    write_header_line(name_line, output_file)
    write_header_line(description_line, output_file)
    write_header_line(datatype_line, output_file)
    # if patient and sample attributes are in file, print attribute type metadata header
    if len(set(attribute_type_line)) > 0 and is_mixed_attribute_types_format:
        write_header_line(attribute_type_line, output_file)
    write_header_line(priority_line, output_file)

def check_if_mixed_attribute_types_format(filename):
    # determined by filename
    base_filename = os.path.basename(filename)
    if base_filename in [PATIENT_CLINICAL_FILE_PATTERN, SAMPLE_CLINICAL_FILE_PATTERN]:
        return False
    return True

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--files", nargs = "+", help = "file(s) to add metadata headers", required = True)
    parser.add_argument("-s", "--study-id", help = "study id for specific overrides", required = False)
    parser.add_argument("-c", "--cdd-url", help = "the url for the cdd web application, default is http://oncotree.mskcc.org/cdd/api/", required = False)
    args = parser.parse_args()
    clinical_files = args.files
    study_id = args.study_id
    cdd_url = args.cdd_url
    # change base url if specified (i.e for testing)
    if cdd_url:
        base_cdd_url = cdd_url
    else:
        base_cdd_url = DEFAULT_URL
    # check file (args) validity and return error if any file fails check
    missing_clinical_files = [clinical_file for clinical_file in clinical_files if not os.path.exists(clinical_file)]
    if len(missing_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not found: ' + ', '.join(missing_clinical_files)
        sys.exit(2)
    not_writable_clinical_files = [clinical_file for clinical_file in clinical_files if not os.access(clinical_file,os.W_OK)]
    if len(not_writable_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not writable: ' + ', '.join(not_writable_clinical_files)
        sys.exit(2)
    if (study_id):
        check_valid_studyid(study_id, base_cdd_url)
    metadata_dictionary = {}
    all_attributes = set()
    # get a set of attributes used across all input files
    for clinical_file in clinical_files:
        all_attributes = all_attributes.union(get_header(clinical_file))
    metadata_dictionary = get_clinical_attribute_metadata_from_cdd(study_id, all_attributes, base_cdd_url)
    # check metadata is defined for all attributes in CDD
    if len(metadata_dictionary.keys()) != len(all_attributes):
        print >> ERROR_FILE, 'Error, metadata not found for attribute(s): ' + ', '.join(all_attributes.difference(metadata_dictionary.keys()))
    for clinical_file in clinical_files:
        # create temp file to write to
        temp_file, temp_file_name = tempfile.mkstemp()
        header = get_header(clinical_file)
        is_mixed_attribute_types_format = check_if_mixed_attribute_types_format(clinical_file)
        write_headers(header, metadata_dictionary, temp_file, is_mixed_attribute_types_format)
        write_data(clinical_file, temp_file)
        os.close(temp_file)
        # replace original file with new file
        shutil.move(temp_file_name, clinical_file)

if __name__ == '__main__':
    main()
