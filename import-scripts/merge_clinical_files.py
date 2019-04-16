#! /usr/bin/env python
# ------------------------------------------------------------------------------
# Utility script which takes a study directory
# Resolves any cases where study directory contains both legacy and current clinical file formats (mixed, seperate)
# data_clinical.txt (mixed) is parsed and added to data_clinical_sample.txt/data_clinical_patient.txt based on CDD attribute type
# ------------------------------------------------------------------------------

import argparse
import json
import os
import requests
import shutil
import sys
import tempfile
from clinicalfile_utils import *

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

DEFAULT_CDD_BASE_URL = "http://oncotree.mskcc.org/cdd/api/"
ATTRIBUTE_TYPE_FIELD = "attribute_type"
COLUMN_HEADER_FIELD = "column_header"
DATATYPE_FIELD = "datatype"
DESCRIPTION_FIELD = "description"
DISPLAY_NAME_FIELD = "display_name"
PRIORITY_FIELD = "priority"

PATIENT_ID_FIELD = "PATIENT_ID"
SAMPLE_ID_FIELD = "SAMPLE_ID"
CASE_ID_COLS = [SAMPLE_ID_FIELD, PATIENT_ID_FIELD]

MIXED_ATTRIBUTE_TYPE = "MIXED"
SAMPLE_ATTRIBUTE_TYPE = "SAMPLE"
PATIENT_ATTRIBUTE_TYPE = "PATIENT"
METADATA_HEADER_PREFIX = "#"

NULL_EMPTY_VALUES = ['NA', 'N/A', '', None]

def get_header_to_attribute_type_map(study_id, header, base_cdd_url):
    '''
        Returns a dictionary of attribute to attribute type.

        map = {
            attribiute_id:attribute_type
        }

        - attribute_id: the normalized clinical attribute id
        - attribute_type: [PATIENT | SAMPLE]
    '''
    metadata_mapping = {}
    response = requests.post(base_cdd_url + "?cancerStudy=" + study_id if has_overrides(study_id, base_cdd_url) else base_cdd_url, json=list(header))
    check_response_returned(response)
    response_as_json = json.loads(response.text)

    attribute_type_map = {}
    for attribute_data in response_as_json:
        attribute_type_map[attribute_data[COLUMN_HEADER_FIELD]] = attribute_data[ATTRIBUTE_TYPE_FIELD]
    return attribute_type_map

# return a dictionary of attributes in list to all their metadata
def get_attribute_to_metadata_map(study_id, header, base_cdd_url):
    metadata_mapping = {}
    response = requests.post(base_cdd_url + "?cancerStudy=" + study_id if has_overrides(study_id, base_cdd_url) else base_cdd_url, json=list(header))
    check_response_returned(response)
    response_as_json = json.loads(response.text)
    for attribute in response_as_json:
        metadata_mapping[attribute[COLUMN_HEADER_FIELD]] = attribute
    return metadata_mapping

# check whether provided study id has overrides available/is valid
def has_overrides(study_id, base_cdd_url):
    response = requests.get(base_cdd_url + "cancerStudies")
    check_response_returned(response)
    response_as_json = json.loads(response.text)
    return study_id in [cancer_study["name"] for cancer_study in response_as_json]

def check_response_returned(response):
    if response.status_code != 200:
        print >> ERROR_FILE, "Error code returned from CDD, aborting..."
        sys.exit(2)

def get_default_header_by_type(file_attribute_type):
    if file_attribute_type in [MIXED_ATTRIBUTE_TYPE, SAMPLE_ATTRIBUTE_TYPE]:
        return [SAMPLE_ID_FIELD, PATIENT_ID_FIELD]
    else:
        return [PATIENT_ID_FIELD]

def load_clinical_file_to_map(clinical_file, file_attribute_type):
    '''
        Loads clinical data into map.
        map = {
            key = [sample id | patient id]
            value = {
                attr1:value1,
                attr2:value2,
                ... etc ...
            }
        }

        Returns file header and clinical data map.
    '''
    if not os.path.isfile(clinical_file):
        return get_default_header_by_type(file_attribute_type), {}
    header = get_header(clinical_file)
    headers_processed = False
    clinical_data_map = {}
    with open(clinical_file) as data_file:
        for line in data_file:
            if line.startswith(METADATA_HEADER_PREFIX) or len(line.rstrip()) == 0:
                continue
            if not headers_processed:
                headers_processed = True
                continue
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            try:
                # if sample id in clinical data map then it is duplicated in file - report and exit
                if data[SAMPLE_ID_FIELD] in clinical_data_map:
                    print >> ERROR_FILE, "Malformatted file: duplicate sample (%s) found in %s, exiting..." % (data[SAMPLE_ID_FIELD], clinical_file)
                    sys.exit(2)
                clinical_data_map[data[SAMPLE_ID_FIELD]] = data
            except KeyError as e:
                clinical_data_map[data[PATIENT_ID_FIELD]] = data
    return header, clinical_data_map

def update_sample_patient_headers_with_mixed_header(study_id, base_cdd_url, mixed_header, sample_header, patient_header):
    '''
        Organizes the mixed clinical file header by sample / patient attribute types.

        Takes into consideration whether a sample-level attribute is already in the patient header from data_clinical_patient.txt
        or if a patient-level attribute is already in the sample header from data_clinical_sample.txt.
    '''
    sample_attributes_as_patient_attributes = []
    patient_attribites_as_sample_attributes = []

    header_to_attribute_type_map = get_header_to_attribute_type_map(study_id, mixed_header, base_cdd_url)
    for attribute in header_to_attribute_type_map.keys():
        # skip attributes if they are a case id column
        if attribute in CASE_ID_COLS:
            continue

        orig_attribute_type = header_to_attribute_type_map[attribute]
        # if attribute does not already exist in either header then go ahead and
        # add to the default header list
        # i.e., patient_header if type is 'PATIENT', sample_header if type is 'SAMPLE'
        if not attribute in sample_header and not attribute in patient_header:
            if orig_attribute_type == SAMPLE_ATTRIBUTE_TYPE:
                sample_header.append(attribute)
            else:
                patient_header.append(attribute)
            continue

        # if attribute exists in sample header but its default type in CDD is 'PATIENT'
        # then update attribute type in 'header_to_attribute_type' map and add to
        # 'sample_attributes_as_patient_attributes' list for reporting
        if attribute in sample_header:
            if orig_attribute_type != SAMPLE_ATTRIBUTE_TYPE:
                patient_attribites_as_sample_attributes.append(attribute)
                header_to_attribute_type_map[attribute] = SAMPLE_ATTRIBUTE_TYPE
        elif attribute in patient_header:
            if orig_attribute_type != PATIENT_ATTRIBUTE_TYPE:
                sample_attributes_as_patient_attributes.append(attribute)
                header_to_attribute_type_map[attribute] = PATIENT_ATTRIBUTE_TYPE
        else:
            print >> ERROR_FILE, "Attribute '%s' not found in either header but was not added to the header corresponding to its attribute type '%s'" % (attribute, orig_attribute_type)
            sys.exit(2)

    # report sample attributes that will be treated as patient attributes if any
    if len(sample_attributes_as_patient_attributes) > 0:
        print >> OUTPUT_FILE, "\nThe following sample-level attributes will be treated as patient-level attributes since these attributes already exist in the patient clinical file header:"
        for attribute in sample_attributes_as_patient_attributes:
            print >> OUTPUT_FILE, "\t%s" % (attribute)
    # report patient attributes that will be treated as sample attributes if any
    if len(patient_attribites_as_sample_attributes) > 0:
        print >> OUTPUT_FILE, "\nThe following patient-level attributes will be treated as sample-level attributes since these attributes already exist in the sample clinical file header:"
        for attribute in patient_attribites_as_sample_attributes:
            print >> OUTPUT_FILE, "\t%s" % (attribute)

    return sample_header,patient_header

def resolve_clinical_datum(case_id, attribute, existing_value, new_value):
    '''
        Resolves clinical datum.
    '''
    if existing_value == new_value:
        return existing_value
    if is_null_type(existing_value):
        return new_value
    # new_value and existing_value are different
    # but we only want to keep the existing one
    # report that the new value is being rejected
    print >> OUTPUT_FILE, "Ignoring new value (%s) for existing value (%s) and attribute (%s) for case ID '%s'" % (new_value, existing_value, attribute, case_id)
    return existing_value


def load_mixed_clinical_file_to_map(mixed_clinical_file, sample_header, clinical_sample_map, patient_header, clinical_patient_map, study_id, base_cdd_url):
    '''
        Load mixed clinical file to a clinical_data_map.

        Updates patient and sample header with columns from the mixed clinical file.

        If a sample header in the mixed file exists as a patient header in data_clinical_patient.txt then the data for the sample header is
        merged to the clinical patient file and vice versa.
    '''
    mixed_header,clinical_data_map = load_clinical_file_to_map(mixed_clinical_file, MIXED_ATTRIBUTE_TYPE)

    # update sample_header and patient_header with mixed_header contents
    sample_header,patient_header = update_sample_patient_headers_with_mixed_header(study_id, base_cdd_url, mixed_header, sample_header, patient_header)

    # only need to add data to clinical_patient_map if patient header size is > 1
    # (meaning it has real attribute aside from 'PATIENT_ID')
    add_patient_data = (len(patient_header) > 1)

    # iterate through every record in data_clinical.txt -
    # new samples/patients automatically added
    # for existing samples, extend existing record with new attributes - old attributes ignored
    # TODO: log cases where attributes are overwritten or switched
    for sample_id, clinical_record in clinical_data_map.items():
        # update sample clinical data
        if not sample_id in clinical_sample_map.keys():
            clinical_sample_map[sample_id] = dict(zip(sample_header, map(lambda x: clinical_record.get(x, 'NA'), sample_header)))
        else:
            existing_sample_data = clinical_sample_map.get(sample_id, {})
            for attribute in sample_header:
                # nothing to update if attribute is PATIENT_ID, SAMPLE_ID
                if attribute in CASE_ID_COLS:
                    continue
                current_value = clinical_record.get(attribute, 'NA')
                existing_value = existing_sample_data.get(attribute, 'NA')
                existing_sample_data[sample_id] = resolve_clinical_datum(sample_id, attribute, existing_value, current_value)
            clinical_sample_map[sample_id] = existing_sample_data

        # if there isn't any patient data to update then skip
        if not add_patient_data:
            continue

        # udpate patient clinical data
        patient_id = clinical_record[PATIENT_ID_FIELD]
        if not patient_id in clinical_patient_map.keys():
            clinical_patient_map[patient_id] = dict(zip(patient_header, map(lambda x: clinical_record.get(x, 'NA'), patient_header)))
        else:
            existing_patient_data = clinical_patient_map.get(patient_id, {})
            for attribute in patient_header:
                if attribute in CASE_ID_COLS:
                    continue
                current_value = clinical_record.get(attribute, 'NA')
                existing_value = existing_patient_data.get(attribute, 'NA')
                existing_patient_data[patient_id] = resolve_clinical_datum(patient_id, attribute, existing_value, current_value)
            clinical_patient_map[patient_id] = existing_patient_data

def is_null_type(attribute_value):
    return (attribute_value in NULL_EMPTY_VALUES)

def overwrite_clinical_file(header, clinical_data_map, clinical_file, study_id, base_cdd_url):
    print >> OUTPUT_FILE, "Updating clinical file: %s" % (clinical_file)
    attribute_to_metadata_map = get_attribute_to_metadata_map(study_id, header, base_cdd_url)
    temp_file, temp_file_name = tempfile.mkstemp()
    os.close(temp_file)
    with open(temp_file_name, "w") as f:
        f.write('#' + '\t'.join(map(lambda x: attribute_to_metadata_map.get(x)[DISPLAY_NAME_FIELD], header)) + '\n')
        f.write('#' + '\t'.join(map(lambda x: attribute_to_metadata_map.get(x)[DESCRIPTION_FIELD], header)) + '\n')
        f.write('#' + '\t'.join(map(lambda x: attribute_to_metadata_map.get(x)[DATATYPE_FIELD], header)) + '\n')
        f.write('#' + '\t'.join(map(lambda x: attribute_to_metadata_map.get(x)[PRIORITY_FIELD], header)) + '\n')
        f.write('\t'.join(header) + '\n')
        for record in clinical_data_map.values():
            formatted_data = map(lambda x: record.get(x,''), header)
            f.write('\t'.join(formatted_data) + '\n')
    shutil.move(temp_file_name, clinical_file)

def usage(parser, message):
    if message:
        print >> ERROR_FILE, message
    parser.print_help()
    sys.exit(2)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--directory", help = "directory containing clinical file(s) to merge", required = True)
    parser.add_argument("-s", "--study-id", default = "", help = "[optional] study id for specific overrides in CDD", required = False)
    parser.add_argument("-c", "--cdd-url", default = DEFAULT_CDD_BASE_URL, help = "[optional] the url for the CDD web application [default = http://oncotree.mskcc.org/cdd/api/]", required = False)
    args = parser.parse_args()

    cdd_url = args.cdd_url
    directory = args.directory
    study_id = args.study_id

    if not os.path.exists(directory):
        print >> ERROR_FILE, "No such directory '%s' - exiting..." % (directory)
        sys.exit(2)

    clinical_sample_file = os.path.join(directory, "data_clinical_sample.txt")
    clinical_patient_file = os.path.join(directory, "data_clinical_patient.txt")
    legacy_clinical_file = os.path.join(directory, "data_clinical.txt")

    # if a mix of clinical file formats does not exist in directory then nothing to do - exit
    if not (os.path.isfile(legacy_clinical_file) and (os.path.isfile(clinical_sample_file) or os.path.isfile(clinical_patient_file))):
        print >> ERROR_FILE, "Directory (" + directory + ") does not have a mix of clinical file formats. Nothing to do..."
        sys.exit(0)

    sample_header, clinical_sample_map = load_clinical_file_to_map(clinical_sample_file, SAMPLE_ATTRIBUTE_TYPE)
    patient_header, clinical_patient_map = load_clinical_file_to_map(clinical_patient_file, PATIENT_ATTRIBUTE_TYPE)
    load_mixed_clinical_file_to_map(legacy_clinical_file, sample_header, clinical_sample_map, patient_header, clinical_patient_map, study_id, cdd_url)
    overwrite_clinical_file(sample_header, clinical_sample_map, clinical_sample_file, study_id, cdd_url)
    if len(patient_header) > 1:
        overwrite_clinical_file(patient_header, clinical_patient_map, clinical_patient_file, study_id, cdd_url)
    else:
        print >> OUTPUT_FILE, "Only PATIENT_ID in patient header, nothing to update..."
    print >> OUTPUT_FILE, "Removing legacy clinical file: %s" % (legacy_clinical_file)
    os.remove(legacy_clinical_file)

if __name__ == '__main__':
    main()
