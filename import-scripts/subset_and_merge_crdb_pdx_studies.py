#!/bin/python
import argparse
import csv
import os
import re
import shutil
import subprocess
import sys
from clinicalfile_utils import *

PATIENT_ID_KEY = "PATIENT_ID"
SOURCE_STUDY_ID_KEY = "SOURCE_STUDY_ID"
CLINICAL_ANNOTATION_KEY = "CLINICAL_ANNOTATION"
CLINICAL_ATTRIBUTE_KEY = "CLINICAL_ATTRIBUTE"
DESTINATION_STUDY_ID_KEY = "DESTINATION_STUDY_ID"
DESTINATION_PATIENT_ID_KEY = "DESTINATION_PATIENT_ID"
CMO_ROOT_DIRECTORY = "/data/portal-cron/cbio-portal-data/bic-mskcc/"
DATAHUB_NAME = "datahub"

MERGE_GENOMIC_FILES_SUCCESS = "MERGE_GENOMIC_FILES_SUCCESS"
SUBSET_CLINICAL_FILES_SUCCESS = "SUBSET_CLINICAL_FILES_SUCCESS"
HAS_ALL_METAFILES = "HAS_ALL_METAFILES"
HAS_SPECIFIED_CLINICAL_ANNOTATIONS = "test"

TRIGGER_FILE_COMMIT_SUFFIX = "_commit_triggerfile"
TRIGGER_FILE_REVERT_SUFFIX = "_revert_triggerfile"

TRIGGER_FILE_COMMIT_SUFFIX = "_commit_triggerfile"
TRIGGER_FILE_REVERT_SUFFIX = "_revert_triggerfile"

SEG_HG18_FILE_PATTERN = '_data_cna_hg18.seg'
SEG_HG18_META_PATTERN = '_meta_cna_hg18_seg.txt'
SEG_HG19_FILE_PATTERN = '_data_cna_hg19.seg'
SEG_HG19_META_PATTERN = '_meta_cna_hg19_seg.txt'
MUTATION_FILE_PATTERN = 'data_mutations_extended.txt'
MUTATION_META_PATTERN = 'meta_mutations_extended.txt'
CNA_FILE_PATTERN = 'data_CNA.txt'
CNA_META_PATTERN = 'meta_CNA.txt'
CLINICAL_FILE_PATTERN = 'data_clinical.txt'
CLINICAL_META_PATTERN = 'meta_clinical.txt'
LOG2_FILE_PATTERN = 'data_log2CNA.txt'
LOG2_META_PATTERN = 'meta_log2CNA.txt'
EXPRESSION_FILE_PATTERN = 'data_expression.txt'
EXPRESSION_META_PATTERN = 'meta_expression.txt'
FUSION_FILE_PATTERN = 'data_fusions.txt'
FUSION_META_PATTERN = 'meta_fusions.txt'
METHYLATION450_FILE_PATTERN = 'data_methylation_hm450.txt'
METHYLATION450_META_PATTERN = 'meta_methylation_hm450.txt'
METHYLATION27_FILE_PATTERN = 'data_methylation_hm27.txt'
METHYLATION27_META_PATTERN = 'meta_methylation_hm27.txt'
METHYLATION_GB_HMEPIC_FILE_PATTERN = 'data_methylation_genebodies_hmEPIC.txt'
METHYLATION_GB_HMEPIC_META_PATTERN = 'meta_methylation_genebodies_hmEPIC.txt'
METHYLATION_PROMOTERS_HMEPIC_FILE_PATTERN = 'data_methylation_promoters_hmEPIC.txt'
METHYLATION_PROMOTERS_HMEPIC_META_PATTERN = 'meta_methylation_promoters_hmEPIC.txt'
METHYLATION_GB_WGBS_FILE_PATTERN = 'data_methylation_genebodies_wgbs.txt'
METHYLATION_GB_WGBS_META_PATTERN = 'meta_methylation_genebodies_wgbs.txt'
METHYLATION_PROMOTERS_WGBS_FILE_PATTERN = 'data_methylation_promoters_wgbs.txt'
METHYLATION_PROMOTERS_WGBS_META_PATTERN = 'meta_methylation_promoters_wgbs.txt'
RNASEQ_EXPRESSION_FILE_PATTERN = 'data_RNA_Seq_expression_median.txt'
RNASEQ_EXPRESSION_META_PATTERN = 'meta_RNA_Seq_expression_median.txt'
RPPA_FILE_PATTERN = 'data_rppa.txt'
RPPA_META_PATTERN = 'meta_rppa.txt'
TIMELINE_FILE_PATTERN = 'data_timeline.txt'
TIMELINE_META_PATTERN = 'meta_timeline.txt'
CLINICAL_PATIENT_FILE_PATTERN = 'data_clinical_patient.txt'
CLINICAL_PATIENT_META_PATTERN = 'meta_clinical_patient.txt'
CLINICAL_SAMPLE_FILE_PATTERN = 'data_clinical_sample.txt'
CLINICAL_SAMPLE_META_PATTERN = 'meta_clinical_sample.txt'
GENE_MATRIX_FILE_PATTERN = 'data_gene_matrix.txt'
GENE_MATRIX_META_PATTERN = 'meta_gene_matrix.txt'
SV_FILE_PATTERN = 'data_SV.txt'
SV_META_PATTERN = 'meta_SV.txt'
FUSIONS_GML_FILE_PATTERN = 'data_fusions_gml.txt'
FUSIONS_GML_META_PATTERN = 'meta_fusions_gml.txt'

FILE_TO_METAFILE_MAP = { MUTATION_FILE_PATTERN : MUTATION_META_PATTERN,
    CNA_FILE_PATTERN : CNA_META_PATTERN,
    LOG2_FILE_PATTERN : LOG2_META_PATTERN,
    SEG_HG18_FILE_PATTERN : SEG_HG18_META_PATTERN,
    SEG_HG19_FILE_PATTERN : SEG_HG19_META_PATTERN,
    METHYLATION27_FILE_PATTERN : METHYLATION27_META_PATTERN,
    METHYLATION450_FILE_PATTERN : METHYLATION450_META_PATTERN,
    METHYLATION_GB_HMEPIC_FILE_PATTERN : METHYLATION_GB_HMEPIC_META_PATTERN,
    METHYLATION_PROMOTERS_HMEPIC_FILE_PATTERN : METHYLATION_PROMOTERS_HMEPIC_META_PATTERN,
    METHYLATION_GB_WGBS_FILE_PATTERN : METHYLATION_GB_WGBS_META_PATTERN,
    METHYLATION_PROMOTERS_WGBS_FILE_PATTERN : METHYLATION_PROMOTERS_WGBS_META_PATTERN,
    FUSION_FILE_PATTERN : FUSION_META_PATTERN,
    RPPA_FILE_PATTERN : RPPA_META_PATTERN,
    EXPRESSION_FILE_PATTERN : EXPRESSION_META_PATTERN,
    RNASEQ_EXPRESSION_FILE_PATTERN : RNASEQ_EXPRESSION_META_PATTERN,
    CLINICAL_FILE_PATTERN : CLINICAL_META_PATTERN,
    CLINICAL_PATIENT_FILE_PATTERN : CLINICAL_PATIENT_META_PATTERN,
    CLINICAL_SAMPLE_FILE_PATTERN : CLINICAL_SAMPLE_META_PATTERN,
    GENE_MATRIX_FILE_PATTERN : GENE_MATRIX_META_PATTERN,
    SV_FILE_PATTERN : SV_META_PATTERN,
    TIMELINE_FILE_PATTERN : TIMELINE_META_PATTERN,
    FUSIONS_GML_FILE_PATTERN : FUSIONS_GML_META_PATTERN }

DESTINATION_STUDY_STATUS_FLAGS = {}
DESTINATION_TO_MISSING_METAFILES_MAP = {}
MISSING_SOURCE_STUDIES = set()
MISSING_DESTINATION_STUDIES = set()
SKIPPED_SOURCE_STUDIES = {}
MULTIPLE_RESOLVED_STUDY_PATHS = {}

IMPACT_STUDY_ID = 'msk_solid_heme'
CRDB_FETCH_SOURCE_ID= 'crdb_pdx_raw_data'

CASE_ID_COLS = ["SAMPLE_ID", "PATIENT_ID", "ONCOTREE_CODE"]
# TO TRACK WHETHER OR NOT TO IMPORT (TRIGGER FILES)
# for each step (i.e subset sources/genomic data, merging, subset crdb-pdx clinical) - set a status flag in a map
# at the end, evaluate status flags in map for each destination study
# if all status flags are successful - touch destination study trigger  file

# TODO: check if destination directory exists, if not thorw an error
# Potentially automatically create destination directory + create new row in portal config + import into triage

#------------------------------------------------------------------------------------------------------------
class Patient():
    def __init__(self, cmo_pid, dmp_pid):
        self.cmo_pid = cmo_pid
        self.dmp_pid = dmp_pid

# Which patients and clinical annotations are wanted for a source
class SourceMappings():
    def __init__(self):
        self.patients = []
        self.clinical_annotations = []

    def get_patient_for_cmo_pid(self, cmo_pid):
        for patient in self.patients:
            if patient.cmo_pid == cmo_pid:
                return patient

    def get_patient_for_dmp_pid(self, dmp_pid):
        for patient in self.patients:
            if patient.dmp_pid == dmp_pid:
                return patient

    def add_patient(self, patient):
        self.patients.append(patient)

    def add_clinical_annotation(self, clinical_annotation):
        self.clinical_annotations.append(clinical_annotation)

#------------------------------------------------------------------------------------------------------------
# Functions for general setup - loading mappings into a dictionaries to work with

# returns list of dictionaries, where each dictionary represents a row (keys are column headers)
# will skip rows that do not contain same number of columns as header
def parse_file(file):
    records = []
    with open(file, 'r') as f:
        reader = csv.DictReader(f, delimiter = "\t")
        for record in reader:
            if all([value for value in record.values()]):
                records.append(record)
    return records

# create a dictionary representation that can be used for subsetting
# takes parse_file() output as input (list of dictionaries)
# output: { DESTINATION_1 : { SOURCE_1 : [ PID_1, PID2, PID3...],
#                             SOURCE_2 : [ PID_4, ...] },
#           mixed_pdx_aacf : { ke_07_83_b : [ P_000001, ...] }}
def create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, root_directory):
    destination_to_source_mapping = {}
    # load in all patients associated with source study per destination
    for record in destination_source_patient_mapping_records:
        destination = record[DESTINATION_STUDY_ID_KEY]
        source = record[SOURCE_STUDY_ID_KEY]
        cmo_pid = record[PATIENT_ID_KEY]
        dmp_pid = record[DESTINATION_PATIENT_ID_KEY]

        destination_directory = os.path.join(root_directory, destination)
        if not os.path.isdir(destination_directory):
            MISSING_DESTINATION_STUDIES.add(destination)
            print destination_directory + " cannot be found. This study will not be generated until this study is created in mercurial and marked in google spreadsheets"
            continue
        if destination not in DESTINATION_STUDY_STATUS_FLAGS:
            DESTINATION_STUDY_STATUS_FLAGS[destination] = { MERGE_GENOMIC_FILES_SUCCESS : False, SUBSET_CLINICAL_FILES_SUCCESS : False, HAS_ALL_METAFILES : False, HAS_SPECIFIED_CLINICAL_ANNOTATIONS : True }
        if destination not in SKIPPED_SOURCE_STUDIES:
            SKIPPED_SOURCE_STUDIES[destination] = set()
        if destination not in destination_to_source_mapping:
            destination_to_source_mapping[destination] = {}
        if source not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][source] = SourceMappings()
        destination_to_source_mapping[destination][source].add_patient(Patient(cmo_pid, dmp_pid))
        # now subsetting crdb-fetched files at beginning
        if CRDB_FETCH_SOURCE_ID not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][CRDB_FETCH_SOURCE_ID] = SourceMappings()
        destination_to_source_mapping[destination][CRDB_FETCH_SOURCE_ID].add_patient(Patient(cmo_pid, dmp_pid))

    # load in all clinical attributes associated with source study per destination
    for record in destination_source_clinical_annotation_mapping_records:
        destination = record[DESTINATION_STUDY_ID_KEY]
        source = record[SOURCE_STUDY_ID_KEY]
        clinical_annotation = record[CLINICAL_ATTRIBUTE_KEY]
        try:
            destination_to_source_mapping[destination][source].add_clinical_annotation(clinical_annotation)
        # exception thrown if destination/source SourceMappings never initialized
        # ignore because that's a non-existent mapping
        except KeyError:
            continue
    return destination_to_source_mapping

def resolve_source_study_path(source_id, data_source_directories):
    '''
        Finds all potential source paths for given source id in each data source directory.

        Ideally each source id will only resolve to one study path. If multipe study
        paths are found then the source id is non-unique with respect to the data source
        directories and will be reprorted.

        An exception is made for a study that resolves to two distinct data source directories
        where one of the resolved data sources is 'datahub'. In these cases the resolved 'datahub'
        cancer study path will be given priority over the other data source directory
        that a source id resolved to.

        For cmo studies, the cancer study path may be resolved by splitting the cancer study
        identifier (source id) on the first three underscores.

        Ex: ke_07_83_b --> $CMO_ROOT_DIRECTORY/ke/07/83/b
    '''
    source_paths = []
    for data_source_directory in data_source_directories:
        # find study by source id in root directory
        source_path = os.path.join(data_source_directory, source_id)
        if os.path.isdir(source_path):
            source_paths.append(source_path)
        # find study by assuming study id is path representation (first three underscores represent directory hierarchy)
        split_source_id = source_id.split("_", 3)
        source_path = os.path.join(data_source_directory, *split_source_id)
        if os.path.isdir(source_path):
            source_paths.append(source_path)
    # only one path found, return value
    if len(source_paths) == 1:
        return source_paths[0]
    # multiple paths found, source id is non-unique. Report error for warning file
    if len(source_paths) == 2 and any([True for source_path in source_paths if DATAHUB_NAME in source_path]):
        print "Datahub and one other source directory resolved for source id: " + source_id + ", using datahub source directory."
        return [source_path for source_path in source_paths if DATAHUB_NAME in source_path][0]
    elif len(source_paths) >= 2:
        print "Multiple directories resolved for source id: " + source_id
        MULTIPLE_RESOLVED_STUDY_PATHS[source_id] = source_paths
    else:
        print "Source directory path not found for " + source_id
        MISSING_SOURCE_STUDIES.add(source_id)
    return None

def create_source_id_to_path_mapping(destination_to_source_mapping, data_source_directories, crdb_fetch_directory):
    '''
        Maps source id to cancer study path.

        Input:
            destination_to_source_mapping = {
                key = "destination"
                value = {
                    key = "source_id"
                    value = SourceMappings()
                }
            }

        Output:
            source_id_to_path_mapping = {
                key = "source_id"
                value = "path/to/cancer/study"
            }
    '''
    source_ids = set()
    for source_to_source_mappings in destination_to_source_mapping.values():
        source_ids.update(source_to_source_mappings.keys())

    source_id_to_path_mapping = {}
    for source_id in source_ids:
        # special case handling for establishing crdb fetch source directory
        if source_id == CRDB_FETCH_SOURCE_ID:
            source_id_to_path_mapping[source_id] = crdb_fetch_directory
            continue;
        # resolved source path 'None' handled by resolve_source_study_path(...)
        source_id_to_path_mapping[source_id] = resolve_source_study_path(source_id, data_source_directories)
    return source_id_to_path_mapping

#------------------------------------------------------------------------------------------------------------
# generates files containing sample-ids linked to specified patient-ids (by destination-source)
# placed in corresponding directories - multiple source per destination
# i.e (/home/destination_study/source_1/subset_list, home/destination_study/source_2/subset_list)
# not currently used -- covered by merge script (but might be needed later on)
def generate_all_subset_sample_lists(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        for source, patients in source_to_source_mappings.items():
            # destination directory is a working subdirectory matching the source
            destination_directory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            if not os.path.isdir(destination_directory):
                os.makedirs(destination_directory)
            if source_directory:
                patient_list = ','.join([patient.cmo_pid for patient in patients])
                subset_script_call = generate_python_subset_call(lib, destination, destination_directory, source_directory, patient_list)
                subprocess.call(subset_script_call, shell = True)
            else:
                print "ERROR: source path for " + source + " could not be found, skipping..."

def get_clinical_file_pattern_to_use(source_directory):
    for clinical_file in [CLINICAL_SAMPLE_FILE_PATTERN, CLINICAL_FILE_PATTERN]:
        if os.path.isfile(os.path.join(source_directory, clinical_file)):
            return clinical_file

#------------------------------------------------------------------------------------------------------------
# subsets source directory files (clinical and genomic) into destination/source sub-directory
def subset_source_directories(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        for source, source_mappings in source_to_source_mappings.items():
            # working source subdirectory is a working subdirectory matching the source under the destination directory
            working_source_subdirectory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            if not os.path.isdir(working_source_subdirectory):
                os.makedirs(working_source_subdirectory)
            if source_directory:
                if source == CRDB_FETCH_SOURCE_ID:
                    patient_list = ','.join([patient.dmp_pid for patient in source_mappings.patients])
                else:
                    patient_list = ','.join([patient.cmo_pid for patient in source_mappings.patients])
                clinical_file_pattern_to_use = get_clinical_file_pattern_to_use(source_directory)
                subset_source_directories_call = generate_bash_subset_call(lib, destination, working_source_subdirectory, source_directory, patient_list, clinical_file_pattern_to_use)
                subset_source_directories_status = subprocess.call(subset_source_directories_call, shell = True)
                # studies which cannot be subsetted are marked to be skipped when merging
                if subset_source_directories_status != 0:
                    SKIPPED_SOURCE_STUDIES[destination].add(source)
                    shutil.rmtree(working_source_subdirectory)
            else:
                print "Error, source path for " + source + " could not be found, skipping..."

#------------------------------------------------------------------------------------------------------------
# merge all source directory files (clinical and genomic) across destination/source subdirectores (destination1/source1, destination1/source2, destination1/source3)
def merge_source_directories(destination_to_source_mapping, root_directory, lib):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        destination_directory = os.path.join(root_directory, destination)
        # exclude studies which weren't successfully subsetted
        # allows study to still be updated/committed even if some CRDB-fetched mappings are invalid
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_to_source_mappings if source not in SKIPPED_SOURCE_STUDIES[destination]]
        source_subdirectory_list = ' '.join(source_subdirectories)
        for source_subdirectory in source_subdirectories:
            touch_missing_metafiles(source_subdirectory)
        merge_source_subdirectories_call = generate_merge_call(lib, destination, destination_directory, source_subdirectory_list)
        merge_source_subdirectories_status = subprocess.call(merge_source_subdirectories_call, shell = True)
        if merge_source_subdirectories_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][MERGE_GENOMIC_FILES_SUCCESS] = True

# for all destination directories - merge clinical files if destination has both legacy and current clinical files (data_clinical.txt + data_clinical_patient/sample.txt)
def merge_clinical_files(destination_to_source_mapping, root_directory, lib):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        merge_clinical_files_call = generate_merge_clinical_files_call(lib, destination, destination_directory)
        print merge_clinical_files_call
        subprocess.call(merge_clinical_files_call, shell = True)

# subsets clinical files from crdb-pdx fetch directory into the top level destination directory
def subset_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        patient_list = ','.join([patient.dmp_pid for source_mappings in source_to_source_mappings.values() for patient in source_mappings.patients])
        # destination directory is main study directory
        destination_directory = os.path.join(root_directory, destination)
        temp_directory = os.path.join(destination_directory, "tmp")
        os.mkdir(temp_directory)
        subset_timeline_file_call = generate_bash_subset_call(lib, temp_directory, "/home/wanga5/", crdb_fetch_directory, patient_list, CLINICAL_SAMPLE_FILE_PATTERN)
        subset_timeline_file_status = subprocess.call(subset_timeline_file_call, shell = True)
        if subset_timeline_file_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][SUBSET_CLINICAL_FILES_SUCCESS] = True
            os.rename(os.path.join(temp_directory, "data_timeline.txt"), os.path.join(destination_directory, "data_timeline.txt"))
        os.rmdir(temp_directory)

def generate_import_trigger_files(destination_to_source_mapping, temp_directory):
    for destination in destination_to_source_mapping:
        import_valid = all([success_status for success_status in DESTINATION_STUDY_STATUS_FLAGS[destination].values()])
        triggerfilesuffix=TRIGGER_FILE_REVERT_SUFFIX
        if import_valid:
            triggerfilesuffix=TRIGGER_FILE_COMMIT_SUFFIX
        trigger_filename = os.path.join(temp_directory, destination + triggerfilesuffix)
        # creates empty trigger file
        open(trigger_filename, 'a').close()

def generate_warning_file(temp_directory, warning_file):
    warning_filename = os.path.join(temp_directory, warning_file)
    with open(warning_filename, "w") as warning_file:
        if MISSING_DESTINATION_STUDIES:
            warning_file.write("CRDB PDX mapping file contained the following destination studies which have not yet been created:\n  ")
            warning_file.write("\n  ".join(MISSING_DESTINATION_STUDIES))
            warning_file.write("\n\n")
        if MISSING_SOURCE_STUDIES:
            warning_file.write("CRDB PDX mapping file contained the following source studies which could not be found:\n  ")
            warning_file.write("\n  ".join(MISSING_SOURCE_STUDIES))
            warning_file.write("\n\n")
        if [source_study for skipped_source_studies in SKIPPED_SOURCE_STUDIES.values() for source_study in skipped_source_studies]:
            warning_file.write("CRDB PDX mapping file contained the following source studies which could not be processed - most likely due to an unknown patient id in a source study:\n ")
            warning_file.write("\n ".join(set([source_study for skipped_source_studies in SKIPPED_SOURCE_STUDIES.values() for source_study in skipped_source_studies])))
            warning_file.write("\n\n")
        if len(MULTIPLE_RESOLVED_STUDY_PATHS) > 0:
            warning_file.write("CRDB PDX mapping file contained source studies which mapped to multiple data source directories:\n")
            for source_id,source_paths in MULTIPLE_RESOLVED_STUDY_PATHS.items():
                warning_file.write("\t" + source_id + ": " + ','.join(source_paths) + "\n")
            warning_file.write("\n\n")

        success_code_message = []
        for destination, success_code_map in DESTINATION_STUDY_STATUS_FLAGS.items():
            if not all(success_code_map.values()):
                if not success_code_map[MERGE_GENOMIC_FILES_SUCCESS]:
                    success_code_message.append(destination + " study failed because it was unable to merge genomic files from the source studies")
                elif not success_code_map[SUBSET_CLINICAL_FILES_SUCCESS]:
                    success_code_message.append(destination + " study failed because it was unable to subset crdb-pdx clinical/timeline files")
                elif not success_code_map[HAS_ALL_METAFILES]:
                    success_code_message.append(destination + "study failed because there are missing the following metafiles" + "\n     " + '\n     '.join(DESTINATION_TO_MISSING_METAFILES_MAP[destination]))
                else:
                    success_code_message.append(destination + " study failed for an unknown reason")
        if success_code_message:
            warning_file.write("The following studies were unable to be created:\n  ")
            warning_file.write("\n  ".join(success_code_message))

#------------------------------------------------------------------------------------------------------------
# Processing functions that occur after subsetting source studies - but before merging into destination studies

# For source studies per destination - select for clinical annotations to keep as specified by CRDB fetched clinical annotations mapping file
def remove_unwanted_clinical_annotations_in_subsetted_source_studies(destination_to_source_mapping, root_directory):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        for source, source_mapping in source_to_source_mappings.items():
            if not source == CRDB_FETCH_SOURCE_ID and source not in SKIPPED_SOURCE_STUDIES[destination]:
                source_subdirectory = os.path.join(root_directory, destination, source)
                filter_clinical_annotations(source_subdirectory, source_mapping.clinical_annotations)

def get_filtered_header(header, clinical_annotations):
    '''
        Returns filtered header containing only case id columns and the clinical annotations specified.
    '''
    # init the filtered header with the case id columns
    filtered_header = [column for column in header if column in CASE_ID_COLS]
    for column in header:
        # add columns to filtered_header if in set of clinical annotations and not already in header
        # the second check is done to ensure that duplicate columns are not added by accident
        if column in clinical_annotations and not column in filtered_header:
            filtered_header.append(column)
    return filtered_header

# Given a directory - remove all clinical attributes in clinical files not specified in clinical_annotations
# "PATIENT_ID" and "SAMPLE_ID" are never removed - because they are needed for "merging" in downstream steps
def filter_clinical_annotations(source_subdirectory, clinical_annotations):
    clinical_files = [os.path.join(source_subdirectory, filename) for filename in  os.listdir(source_subdirectory) if "data_clinical" in filename]
    for clinical_file in clinical_files:
        to_write = []
        header = get_header(clinical_file)
        filtered_header = get_filtered_header(header, clinical_annotations)
        if not filtered_header:
            continue
        attribute_indices = [header.index(attribute) for attribute in filtered_header]
        with open(clinical_file, "r") as f:
            for line in f:
                data = line.rstrip("\n").split("\t")
                data_to_write = [data[index] for index in attribute_indices]
                to_write.append('\t'.join(data_to_write)) 
        with open(clinical_file, "w") as f:
            f.write('\n'.join(to_write) + "\n")

# For source studies per destination - convert CMO patient ids to DMP patient ids in clinical files
# Needed because we are merging clinical files (from all sources and not just overwriting with CRDB-fetched clinical files)
# Subsetted CMO clinical files have CMO patient id to sample mappings - convert these to DMP patient id specified in source to destination mapping file
def convert_cmo_to_dmp_pids_in_subsetted_source_studies(destination_to_source_mapping, root_directory):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        for source, source_mapping in source_to_source_mappings.items():
             if source not in SKIPPED_SOURCE_STUDIES[destination]:
                source_subdirectory = os.path.join(root_directory, destination, source)
                convert_cmo_to_dmp_pids_in_clinical_files(source_subdirectory, source_mapping)

# Converts pids across all clinical files in a directory (defined as files with "data_clinical" in name)
def convert_cmo_to_dmp_pids_in_clinical_files(source_subdirectory, source_mapping):   
    clinical_files = [os.path.join(source_subdirectory, filename) for filename in  os.listdir(source_subdirectory) if "data_clinical" in filename]
    for clinical_file in clinical_files:
        to_write = []
        header = get_header(clinical_file)
        pid_index = header.index("PATIENT_ID")
        # load data to write out - patient id column replaced with what's specified in "DESTINATION_PATIENT_ID" in source mapping file
        # same line written if no mapping is available
        with open(clinical_file, "r") as f:
            for line in f:
                data = line.rstrip("\n").split("\t")
                try:   
                    source_pid = data[pid_index]
                    data[pid_index] = source_mapping.get_patient_for_cmo_pid(source_pid).dmp_pid
                except:
                    pass
                to_write.append('\t'.join(data))
        with open(clinical_file, "w") as f:
            f.write('\n'.join(to_write) + "\n")

#------------------------------------------------------------------------------------------------------------
# Functions for handling metafile processing

# goes through all destination studies and checks for missing metafiles
# missing metafiles are added to global map (destination : [ list of missing metafiles ]
def get_all_destination_to_missing_metafiles_mapping(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        missing_metafiles = get_missing_metafiles_in_directory(destination_directory)
        # if missing_metafiles is empty, study passed metafile status check
        # else store missing_metafile names for final error/warning message
        if not missing_metafiles:
            DESTINATION_STUDY_STATUS_FLAGS[destination][HAS_ALL_METAFILES] = True
        else:
            DESTINATION_TO_MISSING_METAFILES_MAP[destination] = missing_metafiles

# returns None if there no matching metafile
def get_matching_metafile_name(filename):
    metafile_name = None
    if SEG_HG18_FILE_PATTERN in filename:
        metafile_name = filename.replace(SEG_HG18_FILE_PATTERN, SEG_HG18_META_PATTERN)
    elif SEG_HG19_FILE_PATTERN in filename:
        metafile_name = filename.replace(SEG_HG19_FILE_PATTERN, SEG_HG19_META_PATTERN)
    else:
        if filename in FILE_TO_METAFILE_MAP:
            metafile_name = FILE_TO_METAFILE_MAP[filename]
    return metafile_name

# goes through all files in a directory and checks if corresponding metafiles exist
# files which do not require a metafile are ignored
def get_missing_metafiles_in_directory(directory):
    expected_metafiles = [get_matching_metafile_name(file) for file in os.listdir(directory)]
    missing_metafiles = [metafile for metafile in expected_metafiles if metafile and not os.path.exists(os.path.join(directory, metafile))]
    return missing_metafiles

# assumes directory starts off without metafiles
# does not check whether metafiles already exist
# metafiles are touched as long as a matching datafile is found in the directory
def touch_missing_metafiles(directory):
    for file in os.listdir(directory):
        metafile_name = get_matching_metafile_name(file)
        if metafile_name:
            touch_metafile_call = "touch " + os.path.join(directory, metafile_name)
            subprocess.call(touch_metafile_call, shell = True)

#------------------------------------------------------------------------------------------------------------
# Utility functions for post-processing/cleanup - generally right before import

# needed because IMPACT study is being automatically pulled in and need to be added to the existing data_clinical_sample.txt
# data_clinical_sample.txt per destination study  is originally subsetted from CRDB-PDX fetched file and does not contain IMPACT samples
# unmapped IMPACT samples are not mapped to their patients/shown as seperate patients in the portal
# not currently needed - IMPACT now treated as any other source study
def add_patient_sample_records(destination_to_source_mapping, root_directory, lib):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        impact_source_subdirectory = os.path.join(root_directory, destination, IMPACT_STUDY_ID)
        add_clinical_records_call = generate_add_clinical_records_call(lib, destination_directory, impact_source_subdirectory)
        add_clinical_records_status = subprocess.call(add_clinical_records_call, shell = True)

# needed because CMO studies are unannotated but IMPACT studies are
# a merge of CMO + IMPACT studies results in a partially annotated MAF
# (IMPACT study is pulled in whenever patient-ids are DMP ids)
# annotator assumes MAF is annotated because HGVSp_short column is present - CMO samples never annotated
def remove_hgvsp_short_column(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        maf = os.path.join(destination_directory, MUTATION_FILE_PATTERN)
        if "HGVSp_Short" in get_header(maf):
            hgvsp_short_index = get_header(maf).index("HGVSp_Short")
            maf_to_write = []
            maf_file = open(maf, "rU")
            for line in maf_file:
                if line.startswith("#"):
                    maf_to_write.append(line.rstrip("\n"))
                else:
                    record = line.rstrip("\n").split('\t')
                    maf_to_write.append('\t'.join(record[0:hgvsp_short_index] + record[hgvsp_short_index + 1:]))
            maf_file.close()
            new_file = open(maf, "w")
            new_file.write('\n'.join(maf_to_write))
            new_file.close()

def remove_file_if_exists(destination_directory, filename):
    file_to_remove = os.path.join(destination_directory, filename)
    if os.path.exists(file_to_remove):
        os.remove(file_to_remove)

def remove_merged_timeline_files(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, TIMELINE_FILE_PATTERN)

def remove_temp_subset_files(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, "temp_subset.txt")

def remove_source_subdirectories(destination_to_source_mapping, root_directory):
    for destination, source_to_source_mappings in destination_to_source_mapping.items():
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_to_source_mappings if source not in SKIPPED_SOURCE_STUDIES[destination]]
        for source_subdirectory in source_subdirectories:
            shutil.rmtree(source_subdirectory)

#------------------------------------------------------------------------------------------------------------
# Functions for generating executable commands

def generate_python_subset_call(lib, cancer_study_id, destination_directory, source_directory, patient_list):
    python_subset_call = 'python ' + lib + '/generate-clinical-subset.py --study-id=' + cancer_study_id + ' --clinical-file=' + source_directory + '/data_clinical.txt --filter-criteria="PATIENT_ID=' + patient_list + '" --subset-filename=' + destination_directory + "/subset_file.txt"
    return python_subset_call

def generate_bash_subset_call(lib, cancer_study_id, destination_directory, source_directory, patient_list, source_sample_file):
    bash_subset_call = 'bash ' + lib + '/subset-impact-data.sh -i=' + cancer_study_id + ' -o=' + destination_directory + ' -f="PATIENT_ID=' + patient_list + '" -s=' + destination_directory + '/temp_subset.txt -d=' + source_directory + ' -c=' + os.path.join(source_directory, source_sample_file) + ' -p=' + lib
    return bash_subset_call

def generate_merge_call(lib, cancer_study_id, destination_directory, subdirectory_list):
    merge_call = 'python ' + lib + '/merge.py -d ' + destination_directory + ' -i ' + cancer_study_id + ' -m "true" ' + subdirectory_list
    return merge_call

def generate_merge_clinical_files_call(lib, cancer_study_id, destination_directory):
    merge_clinical_files_call = 'python ' + lib + '/merge_clinical_files.py -d ' + destination_directory + ' -s ' + cancer_study_id
    return merge_clinical_files_call

# used in sample-mode because data_clinical_sample.txt is being extended - sample id should be used as primary key for each record
def generate_add_clinical_records_call(lib, destination_directory, impact_source_subdirectory):
    add_clinical_records_call = 'python ' + lib + '/add_clinical_records.py -c ' + destination_directory + '/data_clinical_sample.txt -s ' + impact_source_subdirectory + '/data_clinical_sample.txt -f "PATIENT_ID,SAMPLE_ID,ONCOTREE_CODE" -l' + lib
    return add_clinical_records_call

#------------------------------------------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--clinical-annotation-mapping-file", help = "CRDB-fetched file containing clinical attributes per source/id destination/id", required = True)
    parser.add_argument("-d", "--data-source-directories", help = "comma-delimited root directories to search all data source directories", required = True)
    parser.add_argument("-f", "--fetch-directory", help = "directory where crdb-pdx data is stored", required = True)
    parser.add_argument("-l", "--lib", help = "directory containing subsetting/merge scripts (i.e cmo-pipelines/import-scripts)", required = True)
    parser.add_argument("-m", "--mapping-file", help = "CRDB-fetched file containing mappings from souce/id to destination/id", required = True)
    parser.add_argument("-r", "--root-directory", help = "root directory for all new studies (i.e dmp to mskimpact, hemepact, raindance...", required = True)
    parser.add_argument("-t", "--temp-directory", help = "temp directory to store trigger files", required = True)
    parser.add_argument("-w", "--warning-file", help = "file to store all warnings/errors for email", required = True)

    args = parser.parse_args()
    data_source_directories = map(str.strip, args.data_source_directories.split(','))
    crdb_fetch_directory = args.fetch_directory
    destination_to_source_mapping_filename = os.path.join(crdb_fetch_directory, args.mapping_file)
    clinical_annotation_mapping_filename = os.path.join(crdb_fetch_directory, args.clinical_annotation_mapping_file)
    lib = args.lib
    root_directory = args.root_directory
    temp_directory = args.temp_directory
    warning_file = args.warning_file

    # parse the two mapping files provided (which patients and which clinical attributes)
    destination_source_patient_mapping_records = parse_file(destination_to_source_mapping_filename)
    destination_source_clinical_annotation_mapping_records = parse_file(clinical_annotation_mapping_filename)

    # create a dictionary mapping (destination - source) to a SourceMappings object
    # SourceMappings object contains a list of patients to subset and list of clinical attributes to pull
    destination_to_source_mapping = create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, root_directory)
    source_id_to_path_mapping = create_source_id_to_path_mapping(destination_to_source_mapping, data_source_directories, crdb_fetch_directory)

    # subset everything including clinical files
    subset_source_directories(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib)

    # filter out - rewrite clinical files to only include wanted columns
    remove_unwanted_clinical_annotations_in_subsetted_source_studies(destination_to_source_mapping, root_directory)
    convert_cmo_to_dmp_pids_in_subsetted_source_studies(destination_to_source_mapping, root_directory)

    # merge everything together
    merge_source_directories(destination_to_source_mapping, root_directory, lib)
    remove_source_subdirectories(destination_to_source_mapping, root_directory)

    # merge legacy and new format clinical files
    merge_clinical_files(destination_to_source_mapping, root_directory, lib)
    subset_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib)
    #add_patient_sample_records(destination_to_source_mapping, root_directory, lib)
    remove_hgvsp_short_column(destination_to_source_mapping, root_directory)
    remove_temp_subset_files(destination_to_source_mapping, root_directory)
    get_all_destination_to_missing_metafiles_mapping(destination_to_source_mapping, root_directory)
    generate_import_trigger_files(destination_to_source_mapping, temp_directory)
    generate_warning_file(temp_directory, warning_file)

if __name__ == '__main__':
    main()