import argparse
import sys
import csv
import os
import subprocess
import re
from clinicalfile_utils import *

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

PATIENT_ID_KEY="PATIENT_ID"
SAMPLE_ID_KEY="SAMPLE_ID"

# updates existing records with information from supplemental file
# will add new records for those found in supplemental file but not in clinical file
def expand_clinical_data(clinical_filename, supplemental_data_map, fields, patient_mode):
    key = SAMPLE_ID_KEY
    if patient_mode:
        key = PATIENT_ID_KEY

    clinical_file_header = get_header(clinical_filename)
    # loop through fields specifies by user and add to existing header
    for supplemental_header_field in fields:
        if supplemental_header_field in supplemental_data_map.values()[0].keys() and supplemental_header_field not in clinical_file_header:
	    clinical_file_header.append(supplemental_header_field)
    
    # skipping metadata headers
    if has_metadata_headers(clinical_filename):
        number_of_headers = 3
    if is_old_format: 
        number_of_headers = 4
    
    data_file = open(clinical_filename, 'rU')
    for row_index in range(number_of_headers):
        data_file.next()
    data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
    output_data = ['\t'.join(clinical_file_header)]
    # load data from clinical_filename and write data to output directory
    # updates existing record with corresponding record from supplemental data
    # removes corresponding record from supplemtnal data
    for line in data_reader:
        line.update(supplemental_data_map.get(line[key].strip(), {}))
        supplemental_data_map.pop(line[key], None)
	data = map(lambda v: line.get(v,''), clinical_file_header)
	output_data.append('\t'.join(data))
    data_file.close()

    # add new records (everything in supplemental file) - existing records were removed in previous step
    for record in supplemental_data_map.values():
        data = map(lambda v: record.get(v,''), clinical_file_header)
        output_data.append('\t'.join(data))	

    # write data to output file
    output_file = open(clinical_filename, 'w')
    output_file.write('\n'.join(output_data))
    output_file.close()

# loads data from supplemental clinical file into a dictionary
# keys are SAMPLE_ID/PATIENT_ID depending on mode its run i.e PATIENT v SAMPLE ID as primary key
def load_supplemental_clinical_data(supplemental_clinical_filename, fields, patient_mode):
    supplemental_data_dictionary = {}
    key = SAMPLE_ID_KEY
    if patient_mode:
        key = PATIENT_ID_KEY
    header = get_header(supplemental_clinical_filename)
    if not all([True if field in header else False for field in fields]):
        print "Specified fields could not be found in supplemental file, aborting..."
        exit(1)
    supplemental_clinical_file = open(supplemental_clinical_filename, 'rU')
    data_reader = [line for line in supplemental_clinical_file.readlines() if not line.startswith('#')]
    for line in data_reader[1:]:
	line = dict(zip(header, map(str.strip, line.split('\t'))))
	supplemental_data_dictionary[line[key].strip()] = dict({(k,v) for k,v in line.items() if k in fields})
    supplemental_clinical_file.close()
    return supplemental_data_dictionary

def generate_add_metadata_header_call(clinical_file, lib):
    add_metadata_header_call = 'python ' + lib + '/add_clinical_attribute_metadata_headers.py -f ' + clinical_file 
    return add_metadata_header_call

def add_metadata_header(clinical_file, lib):
    add_metadata_headers_call = generate_add_metadata_header_call(clinical_file, lib)
    add_metadata_headers_status = subprocess.call(add_metadata_headers_call, shell = True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--clinical-file", help = "file to be altered", required = True)
    parser.add_argument("-s", "--supplemental-file", help = "source for supplemental data", required = True)
    parser.add_argument("-p", "--patient-mode", help = "patient mode if clinical files have patient as unique identifiers", action = "store_true")
    parser.add_argument("-f", "--fields", help = "fields to be added to clinical-file")
    parser.add_argument("-l", "--lib", help = "directory with data curation tools", required = True)
    args = parser.parse_args()

    clinical_file = args.clinical_file
    supplemental_file = args.supplemental_file
    patient_mode = args.patient_mode
    fields = args.fields.split(",")
    lib = args.lib

    if not os.path.isfile(clinical_file):
        print "clinical-file, " + clinical_file + " cannot be found, aborting..."
        exit(1)
    if not os.path.isfile(supplemental_file):
        print "supplemental-file, " + supplemental_file + " cannot be found, aborting..."
        exit(1)
    if not os.path.isdir(lib):
        print "scripts directory, " + lib + " cannot be found, aborting..."
        exit(1)

    # load supplemental data from the clinical supp file
    supplemental_data_map = load_supplemental_clinical_data(supplemental_file, fields, patient_mode)
    expand_clinical_data(clinical_file, supplemental_data_map, fields, patient_mode)
    # re-add metadata headers - must be called seperately since new fields might also be added
    add_metadata_header(clinical_file, lib) 

if __name__ == '__main__':
	main()
