#!/usr/bin/env python
# ------------------------------------------------------------------------------
# Utility script which adds metadata headers to the specified clinical file(s).
#
# Metadata is loaded exclusively from a JSON file (-i / --independent-metadata-file)
# that maps normalized column header names to their metadata objects. Each entry
# in the JSON file must have the following keys:
#   DISPLAY_NAME, DESCRIPTIONS, DATATYPE, ATTRIBUTE_TYPE, PRIORITY
#
# Default behavior for attributes not found in the JSON file:
#   DISPLAY_NAME  : attribute name with underscores replaced by spaces, title-cased
#   DESCRIPTIONS  : same as DISPLAY_NAME default
#   DATATYPE      : STRING
#   ATTRIBUTE_TYPE: SAMPLE
#   PRIORITY      : 1
#
# The script adds four/five metadata headers lines (display name, description, datatype, priority, attribute type).
# The fifth line (attribute type) is added when the file is not split between patient/sample, based on the
# clinical file name (i.e., not data_clinical_patient.txt or data_clinical_sample.txt).
#
# Changes are only made if all input files are valid (exist and are writable).
# ------------------------------------------------------------------------------

from clinicalfile_utils import write_data, write_header_line, get_header
import argparse
import json
import os
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

PATIENT_CLINICAL_FILE_PATTERN = "data_clinical_patient.txt"
SAMPLE_CLINICAL_FILE_PATTERN = "data_clinical_sample.txt"

def get_metadata_dictionary(all_attributes, metadata_file):
    """Load metadata for the given attributes from the JSON metadata file.

    Returns a dict mapping normalized column header -> metadata object for
    every attribute that appears in the file. Attributes absent from the file
    are omitted and will receive default values at write time.
    """
    metadata_dictionary = {}
    f = open(metadata_file, "r")
    all_metadata = json.load(f)
    f.close()
    for normalized_column_header in all_attributes:
        if normalized_column_header in all_metadata:
            metadata = all_metadata[normalized_column_header]
            metadata_dictionary[normalized_column_header] = {
                    'DISPLAY_NAME' : metadata['DISPLAY_NAME'],
                    'DESCRIPTIONS' : metadata['DESCRIPTIONS'],
                    'DATATYPE' : metadata['DATATYPE'],
                    'ATTRIBUTE_TYPE' : metadata['ATTRIBUTE_TYPE'],
                    'PRIORITY' : metadata['PRIORITY']
                    }
    return metadata_dictionary

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
            # attribute not found in metadata file -- apply defaults
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
    parser.add_argument("-i", "--independent-metadata-file", help = "a JSON file containing a map from normalized column header to metadata object", required = True)
    args = parser.parse_args()
    clinical_files = args.files
    metadata_file = args.independent_metadata_file
    if not os.path.exists(metadata_file):
        print >> ERROR_FILE, 'Metadata file not found: ' + metadata_file
        sys.exit(2)
    # check file (args) validity and return error if any file fails check
    missing_clinical_files = [clinical_file for clinical_file in clinical_files if not os.path.exists(clinical_file)]
    if len(missing_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not found: ' + ', '.join(missing_clinical_files)
        sys.exit(2)
    not_writable_clinical_files = [clinical_file for clinical_file in clinical_files if not os.access(clinical_file,os.W_OK)]
    if len(not_writable_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not writable: ' + ', '.join(not_writable_clinical_files)
        sys.exit(2)
    all_attributes = set()
    # get a set of attributes used across all input files
    for clinical_file in clinical_files:
        all_attributes = all_attributes.union(get_header(clinical_file))
    # load metadata for all attributes from the JSON file; missing attributes get defaults at write time
    metadata_dictionary = get_metadata_dictionary(all_attributes, metadata_file)
    missing_attributes = all_attributes.difference(metadata_dictionary.keys())
    if missing_attributes:
        print >> ERROR_FILE, 'Warning: metadata not found for attribute(s), defaults will be used: ' + ', '.join(missing_attributes)
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
