#!/bin/python
import argparse
import os
import subprocess
import sys

PATIENT_ID_KEY = "PATIENT_ID"
SOURCE_STUDY_ID_KEY = "SOURCE_STUDY_ID"
DESTINATION_STUDY_ID_KEY = "DESTINATION_STUDY_ID"
DESTINATION_PATIENT_ID_KEY = "DESTINATION_PATIENT_ID"
CMO_ROOT_DIRECTORY = "/data/portal-cron/cbio-portal-data/bic-mskcc/"

# TODO: check if destination directory exists, if not thorw an error
# Potentially automatically create destination directory + create new row in portal config + import into triage
class Patient():
    def __init__(self, cmo_pid, dmp_pid):
        self.cmo_pid = cmo_pid
        self.dmp_pid = dmp_pid

# returns list of dictionaries, where each dictionary represents a row (keys are column headers)
# will skip rows that do not contain same number of columns as header
def parse_file(file):
    f = open(file, "r")
    header_processed = False
    records = []
    for line in f.readlines():
        data = line.rstrip().split("\t")
        if not header_processed:
            header = data
            header_processed = True
        else:
            try:           
            	records.append({header[index] : data[index] for index in range(len(header))})
            except: 
                print "ERROR: missing value in a column for the following record: " + line
    return records

# create a dictionary representation that can be used for subsetting
# takes parse_file() output as input (list of dictionaries)
# output: { DESTINATION_1 : { SOURCE_1 : [ PID_1, PID2, PID3...], 
#                             SOURCE_2 : [ PID_4, ...] },
#           mixed_pdx_aacf : { ke_07_83_b : [ P_000001, ...] }}
def create_destination_to_source_mapping(records):
    destination_to_source_mapping = {}
    for record in records:
        destination = record[DESTINATION_STUDY_ID_KEY]
        source = record[SOURCE_STUDY_ID_KEY]
        cmo_pid = record[PATIENT_ID_KEY]
        dmp_pid = record[DESTINATION_PATIENT_ID_KEY]
        if destination not in destination_to_source_mapping:
            destination_to_source_mapping[destination] = {}
        if source not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][source] = []
        destination_to_source_mapping[destination][source].append(Patient(cmo_pid, dmp_pid))
    return destination_to_source_mapping
           
#TODO: refine this function in case >1 path found
# split cancer study identifer on first three underscores to create path
# { ke_07_83_b : CMO_ROOT_DIRECTORY/ke/07/83/b } 
def create_source_id_to_path_mapping(destination_to_source_mapping):
    source_id_to_path_mapping = {}
    source_ids = set()
    for source_to_patients_map in destination_to_source_mapping.values():
        source_ids.update(source_to_patients_map.keys())
    for source_id in source_ids:
        # assuming source_id/cancer study id is path representation (first three underscores represent directory hierarchy)
        split_source_id = source_id.split("_", 3)
        source_path = os.path.join(CMO_ROOT_DIRECTORY, *split_source_id)
	if not os.path.isdir(source_path):
	    print "Source directory path not found for " + source_id
            source_id_to_path_mapping[source_id] = None
        else:
            source_id_to_path_mapping[source_id] = source_path
    return source_id_to_path_mapping

# generates files containing sample-ids linked to specified patient-ids (by destination-source)
# placed in corresponding directories - multiple source per destination
# i.e (/home/destination_study/source_1/subset_list, home/destination_study/source_2/subset_list)
def generate_all_subset_sample_lists(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib): 
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # destination directory is a working subdirectory matching the source
            destination_directory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            if not os.path.isdir(destination_directory):
		subprocess.call("mkdir -p " + destination_directory, shell = True)
            if source_directory:
                subset_script_call = generate_python_subset_call(lib, destination, destination_directory, source_directory, patients)
                print subset_script_call + "\n\n"
                # subprocess.call(subset_script_call, shell = True)
            else: 
                print "ERROR: source path for " + source + " could not be found, skipping..."

def generate_python_subset_call(lib, cancer_study_id, destination_directory, source_directory, patient_list):
    to_return = 'python ' + lib + '/generate-clinical-subset.py --study-id=' + cancer_study_id + ' --clinical-file=' + source_directory + '/data_clinical.txt --filter-criteria="PATIENT_ID=' + ','.join([patient.cmo_pid for patient in patient_list]) + '" --subset-filename=' + destination_directory + "/subset_file.txt"
    return to_return

def generate_bash_subset_call
# subsets clinical files from working directory into the top level destination directory
# TODO: touch metafiles?
def subset_clinical_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # destination directory is main study directory
            destination_directory = root_directory + destination
            subset_clinical_files_call = 'bash ' + lib + '/subset-impact-data.sh -i=' + destination + ' -o=' + destination_directory + ' -f="PATIENT_ID=' + ','.join([patient.dmp_pid for patient in patients]) + '" -s=' + destination_directory + '/temp_subset.txt -d=' + crdb_fetch_directory + ' -c=' + crdb_fetch_directory + '/data_clinical_sample.txt'
            print subset_clinical_files_call
            # subprocess.call(subset_clinical_files_call, shell = True)

# subsets source genomic files into destination/source sub-directory
def subset_genomic_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # destination directory is a working subdirectory matching the source
            destination_directory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            subset_genomic_files_call = 'bash ' + lib + '/subset-impact-data.sh -i=' + destination + ' -o=' + destination_directory + ' -f="PATIENT_ID=' + ','.join([patient.dmp_pid for patient in patients]) + '" -s=' + destination_directory + '/temp_subset.txt -d=' + source_directory + ' -c=' + source_directory + '/data_clinical.txt' 
            print subset_genomic_files_call
            # subprocess.call(subset_genomic_files_call, shell = True)

# TODO: remove or convert this to replace sample-ids if needed
'''
def convert_patient_ids(destination_to_source_mapping):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # constructs the correct destination subdirectory per source/destination
            destination_directory = "/home/wanga5/testing_directory/" + destination + "/" + source
            # constructs PID conversion map (cmo -> dmp) specific to this destination/source pairing
            cmo_to_dmp_pid_map = {patient.cmo_pid : patient.dmp_pid for patient in patients}
            for file in [filename if filename.contains("data_clinical") or filename.contains("data_timeline") for filename in os.listdir(destination_directory)]:
               # open file:
               # find column with PATIENT_ID
               # replace and write out new file
'''

def merge_genomic_files(destination_to_source_mapping, root_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        destination_directory = os.path.join(root_directory, destination)
        list_of_subdirectories = ' '.join([os.path.join(root_directory, destination, source) for source in source_to_patients_map])
        merge_call = 'python' + lib + '/merge.py -d ' + destination_directory + ' -i ' + destination + '-m "true" ' + list_of_subdirectories 
        print merge_call
        # subprocess.call(merge_call, shell = True) 
    
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--file", help = "CRDB-fetched file containing mappings from souce/id to destination/id", required = True)
    parser.add_argument("-l", "--lib", help = "directory containing subsetting/merge scripts (i.e cmo-pipelines/import-scripts)", required = True) 
    parser.add_argument("-r", "--root-directory", help = "root directory for all new studies (i.e dmp to mskimpact, hemepact, raindance...", required = True)    
    args = parser.parse_args()
    destination_to_source_mapping_filename = args.file
    lib = args.lib
    root_directory = args.root_directory

    records = parse_file(destination_to_source_mapping_filename) 
    destination_to_source_mapping = create_destination_to_source_mapping(records)
    source_id_to_path_mapping = create_source_id_to_path_mapping(destination_to_source_mapping)
    generate_all_subset_sample_lists(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib)
    
    subset_clinical_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, lib) 
main()
