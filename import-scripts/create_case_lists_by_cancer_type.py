#! /usr/bin/env python

# author: Zack Heins

# ---------------------------------------------------------------
# Selects sample sets based on clinical attribute values and
# outputs case lists (one case list file per distinct value)
#
# This script scans any number of clinical files, storing
# all clinical atributes in 2 maps/dictionaries:
#     SAMPLE_ID -->  {attribute_name, attribute_value}
#     PATIENT_ID --> {attribute_name, attribute_value}
# Any inconsistencies in attribute values are ignored (the
# first encountered value is chosen).
#
# If the attribute is present in the SAMPLE_ID map keys then
# a case list is generated for each distinct value in that
# attribute. Otherwise if the attribute is present in the
# PATIENT_ID map keys then a case list is generated for each
# distinct value in that attribute, adding all samples which
# belong to PATIENT_ID
# 
# As a special case, if the attribute is
# PART_C_CONSENTED_12_245 then only the value "YES" is
# written to a case list, with a name containing "germline"
# ---------------------------------------------------------------

# ---------------------------------------------------------------
# imports
import os
import sys
import getopt
import csv

# ---------------------------------------------------------------
# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# ---------------------------------------------------------------
# functions

# ---------------------------------------------------------------
def count_header_lines(filename):
    count = 0
    with open(filename, 'rU') as f:
        for line in f:
            if line.startswith('#'):
                count += 1
            else:
                break
    return count

# ---------------------------------------------------------------
def insert_into_set_dictionary(dictionary, key, value):
    if key in dictionary:
        dictionary[key].add(value)
    else:
        dictionary[key] = {value} # initialize single value set
# ---------------------------------------------------------------
# creates the case list dictionary
# key = cancer_type
# value = list of sids
def create_case_lists_map(clinical_files, attribute, selected_values = None):
    attribute_value_to_sample_id_map = {}
    attribute_value_to_patient_id_map = {}
    patient_id_to_sample_id_map = {}
    attribute_is_a_patient_attribute = False # will be a patient attribute if ever seen without a sample_id
    
    # read all clinical files
    for clinical_filename in clinical_files:
        number_of_header_lines = count_header_lines(clinical_filename);
        clinical_file = open(clinical_filename,'rU')
        header_lines = [clinical_file.readline() for linenumber in range(0, number_of_header_lines)]
        reader = csv.DictReader(clinical_file,dialect='excel-tab')
        for row in reader:
            sample_id = None
            patient_id = None
            attribute_value = None
            if 'SAMPLE_ID' in row:
                sample_id = row['SAMPLE_ID']
            if 'PATIENT_ID' in row:
                patient_id = row['PATIENT_ID']
            if attribute in row:
                attribute_value = row[attribute]
                if not sample_id:
                    attribute_is_a_patient_attribute = True
            #find any patient to sample mapping
            if sample_id and patient_id:
                insert_into_set_dictionary(patient_id_to_sample_id_map, patient_id, sample_id)
            #find all attribute mappings
            if not attribute_value or (selected_values and attribute_value not in selected_values):
                continue
            if sample_id:
                insert_into_set_dictionary(attribute_value_to_sample_id_map, attribute_value, sample_id)
                continue
            if patient_id:
                insert_into_set_dictionary(attribute_value_to_patient_id_map, attribute_value, patient_id)
        clinical_file.close()
            
    #construct case list map
    case_list_map = {}
    if attribute_is_a_patient_attribute:
        for attribute_value in attribute_value_to_patient_id_map:
            case_list_map[attribute_value] = set()
            for patient in attribute_value_to_patient_id_map[attribute_value]:
                for sample in patient_id_to_sample_id_map[patient]:
                    case_list_map[attribute_value].add(sample);
    else:
        for attribute_value in attribute_value_to_sample_id_map:
            case_list_map[attribute_value] = attribute_value_to_sample_id_map[attribute_value]
    return case_list_map

# ---------------------------------------------------------------
# writes the file to case_lists directory inside the directory
def write_case_list_files(clinical_file_map, output_directory, study_id, name, description, value_filter, filename = None):
    for attribute_value,ids in clinical_file_map.iteritems():
        if value_filter is None or value_filter == attribute_value:
            if filename is None:
                attribute_value_no_spaces = attribute_value.replace(' ','_').replace(',','').replace('/','')
            else:
                attribute_value_no_spaces = filename
                attribute_value = filename
            case_list_file = open(os.path.abspath(output_directory + '/' + 'case_list_' + attribute_value_no_spaces + '.txt'),'w')
            stable_id = study_id + '_' + attribute_value_no_spaces
            case_list_name = name + attribute_value
            case_list_description = description + attribute_value
            case_list_ids = '\t'.join(list(set(ids)))
            case_list_file.write('cancer_study_identifier: ' + study_id + '\n' +
                                    'stable_id: ' + stable_id + '\n' + 
                                    'case_list_name: ' + case_list_name + '\n' + 
                                    'case_list_description: ' + case_list_description + '\n' +
                                    'case_list_ids: ' + case_list_ids)   

# ---------------------------------------------------------------
# gets clin file and processes it 
def create_case_lists(clinical_file_name, output_directory, study_id, attribute):
    if attribute == 'CANCER_TYPE':
        case_lists_map = create_case_lists_map(clinical_file_name, attribute)
        # We do not want to filter off the value of the attribute in the case of case lists by cancer type - we want them all, so pass None as the filter
        write_case_list_files(case_lists_map, output_directory, study_id, 'Tumor Type: ', 'All tumors with cancer type ', None)
    elif attribute == 'PARTC_CONSENTED_12_245':
        case_lists_map = create_case_lists_map(clinical_file_name, attribute, 'YES')
        # If we want germline case list, the value of the column 'PART_C_CONSENTED_12_245' will be YES or NO. Samples with YES for that value are the ones to add to the list
        write_case_list_files(case_lists_map, output_directory, study_id, 'Tumors with germline data PARTC_CONSENTED_12_245 ', 'Tumors with germline data available PARTC_CONSENTED_12_245 ', 'YES', 'germline')
    else:
        print >> ERROR_FILE, "Error : case lists cannot be generated for attribute " + attribute
        sys.exit(2)


# ---------------------------------------------------------------
def display_usage():
    print >> OUTPUT_FILE, 'create_case_lists_by_cancer_type.py\n    --clinical-file-list <clinical_file_path1, clinical_file_path2, ...>\n    --output-directory <path/to/output/directory>\n    --study-id <cancer_study_identifier>\n    --attribute <clinical column to base case list categories on>\n'

# ---------------------------------------------------------------
def main():
    # parse command line
    try:
        opts,args = getopt.getopt(sys.argv[1:],'',['clinical-file-list=', 'output-directory=', 'study-id=', 'attribute='])
    except getopt.error,msg:
        print >> ERROR_FILE,msg
        display_usage()
        sys.exit(2)

    clinical_file_list_string = ''
    output_directory = ''
    study_id = ''
    attribute = ''

    # process options
    for o, a in opts:
        if o == '--clinical-file-list':
            clinical_file_list_string = a
        elif o == '--output-directory':
            output_directory = a
        elif o == '--study-id':
            study_id = a
        elif o == '--attribute':
            attribute = a
    
    if clinical_file_list_string == '' or output_directory == '' or study_id == '' or attribute == '':
        display_usage()
        sys.exit(2)

    # process list arguemnts
    clinical_files = [x.strip() for x in clinical_file_list_string.strip().split(',')]
    
    # check existence of file
    error_found = False
    for clinical_file in clinical_files:
        if not os.path.exists(os.path.abspath(clinical_file)):
            error_found = True
            print >> ERROR_FILE, 'clinical file cannot be found: ' + clinical_file
    if not os.path.isdir(os.path.abspath(output_directory)):
        error_found = True
        print >> ERROR_FILE, 'directory cannot be found or is not a directory: ' + output_directory
    if error_found:
        sys.exit(2)

    create_case_lists(clinical_files, output_directory, study_id, attribute)

# ---------------------------------------------------------------
if __name__ == '__main__':
    main()
