#!/usr/bin/env python

import argparse
import csv
import os
import re
import shutil
import subprocess
import sys
from clinicalfile_utils import *

#------------------------------------------------------------------------------------------------------------
OUTPUT_FILE = sys.stderr

# data directory and repo names
CMO_ROOT_DIRECTORY = "/data/portal-cron/cbio-portal-data/bic-mskcc/"
DATAHUB_REPO_NAME = "datahub"
CRDB_FETCH_SOURCE_ID= 'crdb_pdx_raw_data'

# columns in destination-to-source mapping file
PATIENT_ID_KEY = "PATIENT_ID"
SOURCE_STUDY_ID_KEY = "SOURCE_STUDY_ID"
CLINICAL_ANNOTATION_KEY = "CLINICAL_ANNOTATION"
CLINICAL_ATTRIBUTE_KEY = "CLINICAL_ATTRIBUTE"
DESTINATION_STUDY_ID_KEY = "DESTINATION_STUDY_ID"
DESTINATION_PATIENT_ID_KEY = "DESTINATION_PATIENT_ID"

# global dicts
DESTINATION_TO_MISSING_METAFILES_MAP = {}
MISSING_SOURCE_STUDIES = set()
MISSING_DESTINATION_STUDIES = set()
SKIPPED_SOURCE_STUDIES = {}
MULTIPLE_RESOLVED_STUDY_PATHS = {}
DESTINATION_STUDY_STATUS_FLAGS = {}

# DESTINATION_STUDY_STATUS_FLAGS dict keys
MERGE_GENOMIC_FILES_SUCCESS = "MERGE_GENOMIC_FILES_SUCCESS"
SUBSET_CLINICAL_FILES_SUCCESS = "SUBSET_CLINICAL_FILES_SUCCESS"
HAS_ALL_METAFILES = "HAS_ALL_METAFILES"

# trigger file suffixes
TRIGGER_FILE_COMMIT_SUFFIX = "_commit_triggerfile"
TRIGGER_FILE_REVERT_SUFFIX = "_revert_triggerfile"

# minimally required columns in final header
REQUIRED_CLINICAL_COLUMNS = ["SAMPLE_ID", "PATIENT_ID", "ONCOTREE_CODE"]

# other globals
PDX_ID_COLUMN = "PDX_ID"
DISPLAY_SAMPLE_NAME_COLUMN = "DISPLAY_SAMPLE_NAME"
HGVSP_SHORT_COLUMN = "HGVSp_Short"

# meta/data filename patterns
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

# meta/data file mapping
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

# TODO: Potentially automatically create destination directory + create new row in portal config + import into triage

#------------------------------------------------------------------------------------------------------------
# classes

class Patient():
    def __init__(self, source_pid, destination_pid):
        self.source_pid = source_pid
        self.destination_pid = destination_pid

class SourceMappings():
    def __init__(self, patients = None, clinical_annotations = None):
        self.patients = patients if patients else []
        self.clinical_annotations = clinical_annotations if clinical_annotations else []

    def get_patient_for_source_pid(self, source_pid):
        for patient in self.patients:
            if patient.source_pid == source_pid:
                return patient

    def get_patient_for_destination_pid(self, destination_pid):
        for patient in self.patients:
            if patient.destination_pid == destination_pid:
                return patient

    def add_patient(self, patient):
        self.patients.append(patient)

    def add_clinical_annotation(self, clinical_annotation):
        self.clinical_annotations.append(clinical_annotation)

#------------------------------------------------------------------------------------------------------------
# STEP 1: General Setup - loading mappings into dictionaries to work with.

def parse_file(file):
    '''
        Returns a list of dictionaries, each dictionary representing a row in the file
        where keys are the column headers.

        Skips rows that do not contain the same number of fields as the header
    '''
    records = []
    with open(file, 'r') as f:
        reader = csv.DictReader(f, delimiter = "\t")
        for record in reader:
            if all([value for value in record.values()]):
                records.append(record)
    return records

def create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, root_directory):
    '''
        Creates a dictionary representation of the mappings per destination-source pairing which is
        passed around for subsetting/merging/ etc. Takes parse_file() output as input (list of dictionaries)

        Input arguments:

        1. 'destination_source_patient_mapping_records' record content descriptions:
            - destination: destination study id (and final/processed directory)
            - source: source study id
            - source_pid: patient id as represented in source study
            - destination_pid: patient id as it should be represented in destination study *

            * destination patient id may differ from its corresponding patient id in source study

        2. 'destination_source_clinical_annotation_mapping_records' record content descriptions:
            - destination: destination study id
            - source: source study id
            - clinical_annotation: clinical attribute to import from source study clinical data

        3. root_directory: data directory where new/processed studies are written to

        Example output 'destination_to_source_mapping':
            { DESTINATION_1 : { SOURCE_1 : SourceMappings([PID_1, PID_2...], [CLINATTR_1, CLINATTR_2]),
                                SOURCE_2 : SourceMappings([PID_3, ...], ...) },
              DESTINATION_2 : { SOURCE_1 : SourceMappings([PID_5, PID_6...]) }
            }
    '''
    destination_to_source_mapping = {}
    # load in all patients associated with source study per destination
    for record in destination_source_patient_mapping_records:
        destination = record[DESTINATION_STUDY_ID_KEY]
        source = record[SOURCE_STUDY_ID_KEY]
        source_pid = record[PATIENT_ID_KEY]
        destination_pid = record[DESTINATION_PATIENT_ID_KEY]

        destination_directory = os.path.join(root_directory, destination)

        # skip if destination dir does not exist
        if not os.path.isdir(destination_directory):
            MISSING_DESTINATION_STUDIES.add(destination)
            print >> OUTPUT_FILE, "%s cannot be found. This study will not be generated until the directory for this study is created in mercurial and marked in google spreadsheets" % (destination_directory)
            continue

        # init dict records for destination if not already encountered
        if destination not in DESTINATION_STUDY_STATUS_FLAGS:
            DESTINATION_STUDY_STATUS_FLAGS[destination] = { MERGE_GENOMIC_FILES_SUCCESS : False, SUBSET_CLINICAL_FILES_SUCCESS : False, HAS_ALL_METAFILES : False }
        if destination not in SKIPPED_SOURCE_STUDIES:
            SKIPPED_SOURCE_STUDIES[destination] = set()
        if destination not in destination_to_source_mapping:
            destination_to_source_mapping[destination] = {}
        if source not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][source] = SourceMappings()

        #  udpate 'destination_to_source_mapping' with source + source patient
        destination_to_source_mapping[destination][source].add_patient(Patient(source_pid, destination_pid))
        # now subsetting crdb-fetched files at beginning
        if CRDB_FETCH_SOURCE_ID not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][CRDB_FETCH_SOURCE_ID] = SourceMappings()
        destination_to_source_mapping[destination][CRDB_FETCH_SOURCE_ID].add_patient(Patient(source_pid, destination_pid))

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

        Ideally each source id will only resolve to one study path. If multiple study
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
    if len(source_paths) == 2 and any([True for source_path in source_paths if DATAHUB_REPO_NAME in source_path]):
        print >> OUTPUT_FILE, "Datahub and one other source directory resolved for source id: " + source_id + ", using datahub source directory."
        return [source_path for source_path in source_paths if DATAHUB_REPO_NAME in source_path][0]
    elif len(source_paths) >= 2:
        print >> OUTPUT_FILE, "Multiple directories resolved for source id: " + source_id
        MULTIPLE_RESOLVED_STUDY_PATHS[source_id] = source_paths
    else:
        print >> OUTPUT_FILE, "Source directory path not found for " + source_id
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
    for source_id_to_sourcemappings in destination_to_source_mapping.values():
        source_ids.update(source_id_to_sourcemappings.keys())

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
# STEP 2: Subsetting source directory files into destination/source sub-directory.

def subset_source_directories_for_destination_studies(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    '''
        For every destination study to generate:
            - Calls the subset script for every source study making up destination study.
            - Subset script is passed a list of patient IDs (either the source patient id or destination/mapped patient id) to subset
                the source study data by. Source patient IDs are used for CRDB PDX patients.
            - If subset script fails on source study, adds source study to list of skipped source studies for destination study.


    '''
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        for source, sourcemappings in source_id_to_sourcemappings.items():
            # working_source_subdirectory is a working subdirectory matching the source study id under the destination directory
            # which acts as a workspace while the subsetting is being executed
            working_source_subdirectory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]

            # create directory if not already exists
            if not os.path.isdir(working_source_subdirectory):
                os.makedirs(working_source_subdirectory)

            # build comma-delimited patient list to pass to the subset script
            if source_directory:
                if source == CRDB_FETCH_SOURCE_ID:
                    patient_list = ','.join([patient.destination_pid for patient in sourcemappings.patients])
                else:
                    patient_list = ','.join([patient.source_pid for patient in sourcemappings.patients])

                clinical_file_pattern_to_use = get_clinical_file_pattern_to_use(source_directory)
                subset_source_directories_call = generate_bash_subset_call(lib, destination, working_source_subdirectory, source_directory, patient_list, clinical_file_pattern_to_use)
                subset_source_directories_status = subprocess.call(subset_source_directories_call, shell = True)

                # studies which cannot be subsetted are marked to be skipped when merging
                if subset_source_directories_status != 0:
                    SKIPPED_SOURCE_STUDIES[destination].add(source)
                    shutil.rmtree(working_source_subdirectory)
            else:
                print >> OUTPUT_FILE, "Error, source path for %s could not be found, skipping..." % (source)

def get_clinical_file_pattern_to_use(source_directory):
    '''
        Returns the clinical filename pattern to use (either data_clinical_sample.txt or data_clinical.txt)
        based on which one exists in source directory.
    '''
    for clinical_file in [CLINICAL_SAMPLE_FILE_PATTERN, CLINICAL_FILE_PATTERN]:
        if os.path.isfile(os.path.join(source_directory, clinical_file)):
            return clinical_file

#------------------------------------------------------------------------------------------------------------
# STEP 3: Intermediary data processing - filter clinical annotations and rename patient ids.

def remove_unwanted_clinical_annotations_in_subsetted_source_studies(destination_to_source_mapping, root_directory):
    '''
        Wrapper which applies "filter-clinical-annotation" step to every source-destination pair.

        Note: This step is not applied to the CRDB-fetched files because we want to keep ALL CRDB-fetched clinical annotations
    '''
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        for source, source_mapping in source_id_to_sourcemappings.items():
            if source != CRDB_FETCH_SOURCE_ID and source not in SKIPPED_SOURCE_STUDIES[destination]:
                source_subdirectory = os.path.join(root_directory, destination, source)
                filter_clinical_annotations(source_subdirectory, source_mapping.clinical_annotations)

def get_filtered_header(header, clinical_annotations):
    '''
        Returns filtered header containing only case id columns and the clinical annotations specified.
    '''
    # init the filtered header with the required clinical columns
    filtered_header = [column for column in header if column in REQUIRED_CLINICAL_COLUMNS]
    for column in header:
        # add columns to filtered_header if in set of clinical annotations and not already in header
        # the second check is done to ensure that duplicate columns are not added by accident
        if column in clinical_annotations and not column in filtered_header:
            filtered_header.append(column)
    return filtered_header

def filter_clinical_annotations(source_subdirectory, clinical_annotations):
    '''
        Given a directory, remove all clinical attributes across all clinical files that are not specified in clinical annotations.

        Note: "PATIENT_ID", "SAMPLE_ID", "ONCOTREE_CODE" are never removed because they are required for merging and importing.
    '''
    clinical_files = [os.path.join(source_subdirectory, filename) for filename in os.listdir(source_subdirectory) if is_clinical_file(filename)]
    for clinical_file in clinical_files:
        to_write = []
        header = get_header(clinical_file)
        filtered_header = get_filtered_header(header, clinical_annotations)
        if not filtered_header:
            continue
        # gets index of the "filtered attributes" in the header
        # writing out record columns by index (deals with metadata headers as well)
        attribute_indices = [header.index(attribute) for attribute in filtered_header]
        with open(clinical_file, "r") as f:
            for line in f:
                data = line.rstrip("\n").split("\t")
                data_to_write = [data[index] for index in attribute_indices]
                to_write.append("\t".join(data_to_write))
        write_data_list_to_file(clinical_file, to_write)

def convert_source_to_destination_pids_in_subsetted_source_studies(destination_to_source_mapping, root_directory):
    '''
        For every source-destination pair, convert CMO patient ids to DMP patient ids across all clinical files.

        This is needed since we are mergining clinical files between different data sources (i.e., DMP and CMO) instead of simply
        replacing subsetted/merged clinical files with CRDB-fetched clinical files.

        Note: Subsetted CMO clinical files have CMO patient id to sample id mappings. These must be converted
            to DMP/destination patient id from source destination mapping file (stored in 'destination_to_source_mapping')
        Ex:
            >>> Source 1 (CRDB/DMP)
                DMP_PATIENT_1: Sample 1
                DMP_PATIENT_2: Sample 2

            >>> Source 2 (CMO)
                CMO_PATIENT_1: Sample 1
                CMO_PATIENT_2: Sample 2

        Some samples can match to different patient ids in the same destination study. These discrepancies are merged.
    '''
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        for source, source_mapping in source_id_to_sourcemappings.items():
             if source not in SKIPPED_SOURCE_STUDIES[destination]:
                source_subdirectory = os.path.join(root_directory, destination, source)
                convert_source_to_destination_pids_in_clinical_files(source_subdirectory, source_mapping)

def convert_source_to_destination_pids_in_clinical_files(source_subdirectory, source_mapping):
    '''
        Updates source patient ids with the designated patient id to use for the final/processed destination study.
    '''
    clinical_files = [os.path.join(source_subdirectory, filename) for filename in  os.listdir(source_subdirectory) if is_clinical_file(filename)]
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
                    data[pid_index] = source_mapping.get_patient_for_source_pid(source_pid).destination_pid
                # passthrough for metadata headers/headers
                except:
                    pass
                to_write.append('\t'.join(data))
        write_data_list_to_file(clinical_file, to_write)

#------------------------------------------------------------------------------------------------------------
# STEP 4: Merge all source directory files (clinical and genomic) across destination/source subdirectories (destination/source1, destination/source2, destination/source3).

def merge_source_directories_for_destination_studies(destination_to_source_mapping, root_directory, lib):
    '''
       For every destination study, calls the merge script to merge the subsetted data coming from the
       destination studys' source directories.

       The source studies are merged to the root_directory/destination_directory.
    '''
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        destination_directory = os.path.join(root_directory, destination)
        # exclude studies which weren't successfully subsetted
        # allows study to still be updated/committed even if some CRDB-fetched mappings are invalid
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_id_to_sourcemappings if source not in SKIPPED_SOURCE_STUDIES[destination]]
        source_subdirectory_list = ' '.join(source_subdirectories)
        for source_subdirectory in source_subdirectories:
            # touch missing meta files for merge script functionality
            touch_missing_metafiles(source_subdirectory)
        merge_source_subdirectories_call = generate_merge_call(lib, destination, destination_directory, source_subdirectory_list)
        merge_source_subdirectories_status = subprocess.call(merge_source_subdirectories_call, shell = True)
        if merge_source_subdirectories_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][MERGE_GENOMIC_FILES_SUCCESS] = True

#------------------------------------------------------------------------------------------------------------
# STEP 5: Merge legacy and current clinical files (data_clinical_patient/sample.txt) with data_clinical.txt.

def merge_clinical_files(destination_to_source_mapping, root_directory, lib):
    '''
        Calls the merge clinical files script to merge the clinical files in the destination directory.

        This is to resolve having clinical files of mixed formats, meaning that data_clinical.txt and
        data_clinical_sample.txt do not co-exist in the final/processed data set for the destination study.
    '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        merge_clinical_files_call = generate_merge_clinical_files_call(lib, destination, destination_directory)
        subprocess.call(merge_clinical_files_call, shell = True)

#------------------------------------------------------------------------------------------------------------
# STEP 6: Subset CRDB-fetched timeline files and overwrite existing subsetted/merged timeline files.

def subset_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib):
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        patient_list = ','.join([patient.destination_pid for sourcemappings in source_id_to_sourcemappings.values() for patient in sourcemappings.patients])
        # destination directory is main study directory
        destination_directory = os.path.join(root_directory, destination)
        temp_directory = os.path.join(destination_directory, "tmp")
        os.mkdir(temp_directory)
        subset_timeline_file_call = generate_bash_subset_call(lib, destination, temp_directory, crdb_fetch_directory, patient_list, CLINICAL_SAMPLE_FILE_PATTERN)
        subset_timeline_file_status = subprocess.call(subset_timeline_file_call, shell = True)
        if subset_timeline_file_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][SUBSET_CLINICAL_FILES_SUCCESS] = True
            os.rename(os.path.join(temp_directory, TIMELINE_FILE_PATTERN), os.path.join(destination_directory, TIMELINE_FILE_PATTERN))
        shutil.rmtree(temp_directory)
#------------------------------------------------------------------------------------------------------------
# STEP 7: Post-processing.

def post_process_and_cleanup_destination_study_data(destination_to_source_mapping, root_directory):
    '''
        Runs all of the post-process and cleanup steps on the destination studies.
    '''
    remove_hgvsp_short_column(destination_to_source_mapping, root_directory)
    add_display_sample_name_column(destination_to_source_mapping, root_directory)
    # remove temp subset files after subsetting calls complete
    remove_temp_subset_files(destination_to_source_mapping, root_directory)
    # clean up source subdirectories from destination study paths after merges are complete
    remove_source_subdirectories(destination_to_source_mapping, root_directory)

def add_display_sample_name_column(destination_to_source_mapping, root_directory):
    '''
        Adds DISPLAY_SAMPLE_ID to clinical sample file to support frontend feature which allows
        value in DISPLAY_SAMPLE_ID to be displayed over value in SAMPLE_ID. This is to allow PDX
        sample ids to be displayed as the sample ids if PDX ids are available.

        Empty values for DISPLAY_SAMPLE_NAME will default to values in SAMPLE_ID.
    '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        clinical_file = os.path.join(destination_directory, CLINICAL_SAMPLE_FILE_PATTERN)
        header = get_header(clinical_file)

        if PDX_ID_COLUMN in header:
            # get ordered metadata lines with DISPLAY_SAMPLE_NAME added as well.
            to_write = get_ordered_metadata_with_display_sample_name_attribute(clinical_file)
            pdx_id_index = header.index(PDX_ID_COLUMN)
            header_processed = False
            with open(clinical_file, "r") as f:
                for line in f.readlines():
                    # skip metadata headers - already processed
                    if line.startswith("#"):
                        continue
                    data = line.rstrip("\n").split('\t')
                    # add new attribute/column to the end of header
                    if not header_processed:
                        data.append(DISPLAY_SAMPLE_NAME_COLUMN)
                        header_processed = True
                    # add PDX_ID to end of data (lines up with DISPLAY_SAMPLE_NAME)
                    else:
                        data.append(data[pdx_id_index])
                    to_write.append('\t'.join(data))
            write_data_list_to_file(clinical_file, to_write)

def get_ordered_metadata_with_display_sample_name_attribute(clinical_file):
    '''
        Returns ordered clinical metadata with the DISPLAY_SAMPLE_NAME metadata added as well.
    '''
    # get headers and add DISPLAY_SAMPLE_NAME default metadata
    ## TODO: change to hit CDD(?)
    all_metadata_lines = get_all_metadata_lines(clinical_file)
    add_metadata_for_attribute(DISPLAY_SAMPLE_NAME_COLUMN, all_metadata_lines)

    ordered_metadata_lines = []
    for metadata_header_type in get_metadata_header_line_order(clinical_file):
        ordered_metadata_lines.append('\t'.join(all_metadata_lines[metadata_header_type]))
    return ordered_metadata_lines


def remove_hgvsp_short_column(destination_to_source_mapping, root_directory):
    '''
        Removes HGSVp_Short column from MAF to force annotation on the fly during import.

        This is necessary since CMO studies are not annotated prior to import but IMPACT studies are annotated.
        A merge of CMO and IMPACT studies results in partially annotated MAF which the importer will interpret as
        "annotated" due to the presence of the HGVSp_Short column.
    '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        maf = os.path.join(destination_directory, MUTATION_FILE_PATTERN)
        header = get_header(maf)
        if HGVSP_SHORT_COLUMN in header:
            hgvsp_short_index = header.index(HGVSP_SHORT_COLUMN)
            maf_to_write = []
            with open(maf, "rU") as maf_file:
                for line in maf_file.readlines():
                    if line.startswith("#"):
                        maf_to_write.append(line.rstrip("\n"))
                    else:
                        # remove data from record in HGVSp_Short column
                        record = line.rstrip("\n").split('\t')
                        del record[hgvsp_short_index]
                        maf_to_write.append('\t'.join(record))
            write_data_list_to_file(maf, maf_to_write)

#------------------------------------------------------------------------------------------------------------
# Functions for logging and notifications.

def generate_import_trigger_files(destination_to_source_mapping, temp_directory):
    '''
        Generates appropriate import trigger file based on whether destination
        study is okay to import or if data should be reverted
    '''
    for destination in destination_to_source_mapping:
        import_valid = all([success_status for success_status in DESTINATION_STUDY_STATUS_FLAGS[destination].values()])

        # select appropriate trigger file suffix
        if import_valid:
            trigger_file_suffix = TRIGGER_FILE_COMMIT_SUFFIX
        else:
            trigger_file_suffix = TRIGGER_FILE_REVERT_SUFFIX
        trigger_filename = os.path.join(temp_directory, destination + trigger_file_suffix)
        # creates empty trigger file
        open(trigger_filename, 'a').close()

def generate_warning_file(temp_directory, warning_file):
    '''
        Generates warning file to be emailed to pipelines and pdx-pipelines team.
    '''
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
                    success_code_message.append("%s study failed because it was unable to merge genomic files from the source studies" % (destination))
                elif not success_code_map[SUBSET_CLINICAL_FILES_SUCCESS]:
                    success_code_message.append("%s study failed because it was unable to subset crdb-pdx clinical/timeline files" % (destination))
                elif not success_code_map[HAS_ALL_METAFILES]:
                    success_code_message.append("%s study failed because the following metafiles are missing\n\t%s" % (destination, "\n\t".join(DESTINATION_TO_MISSING_METAFILES_MAP[destination])))
                else:
                    success_code_message.append("%s study failed for an unknown reason" % (destination))
        if success_code_message:
            warning_file.write("The following studies were unable to be created:\n\t")
            warning_file.write("\n\t".join(success_code_message))
#------------------------------------------------------------------------------------------------------------
# Help functions for handling meta files.

def identify_missing_metafiles_for_destination_studies(destination_to_source_mapping, root_directory):
    '''
        For all specified destination studies, check for missing metafiles and add to a global map for reporting if necessary.
    '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        missing_metafiles = get_missing_metafiles_in_directory(destination_directory)
        # if missing_metafiles is empty, study passed metafile status check
        # else store missing_metafile names for final error/warning message
        if not missing_metafiles:
            DESTINATION_STUDY_STATUS_FLAGS[destination][HAS_ALL_METAFILES] = True
        else:
            DESTINATION_TO_MISSING_METAFILES_MAP[destination] = missing_metafiles

def get_matching_metafile_name(filename):
    '''
        Given a filename (e.g data_clinical.txt) return matching metafile name.
        If there is no matching metafile - function returns None.
    '''
    metafile_name = None
    if SEG_HG18_FILE_PATTERN in filename:
        metafile_name = filename.replace(SEG_HG18_FILE_PATTERN, SEG_HG18_META_PATTERN)
    elif SEG_HG19_FILE_PATTERN in filename:
        metafile_name = filename.replace(SEG_HG19_FILE_PATTERN, SEG_HG19_META_PATTERN)
    else:
        if filename in FILE_TO_METAFILE_MAP:
            metafile_name = FILE_TO_METAFILE_MAP[filename]
    return metafile_name

def get_missing_metafiles_in_directory(directory):
    '''
        Goes through all files in a given directory and check for matching  metafiles.
        Files which do not require a metafile are ignored.
    '''
    expected_metafiles = [get_matching_metafile_name(file) for file in os.listdir(directory)]
    missing_metafiles = [metafile for metafile in expected_metafiles if metafile and not os.path.exists(os.path.join(directory, metafile))]
    return missing_metafiles

def touch_missing_metafiles(directory):
    '''
        Assumes pre-condition: provided directory starts off without metafiles.
        Does not check whether metafiles are exist
        Metafiles are touched for datafiles found in the directory
    '''
    for file in os.listdir(directory):
        metafile_name = get_matching_metafile_name(file)
        if metafile_name:
            touch_metafile_call = "touch " + os.path.join(directory, metafile_name)
            subprocess.call(touch_metafile_call, shell = True)

#------------------------------------------------------------------------------------------------------------
# Helper functions for removing extra / temporary files before import.

def remove_file_if_exists(destination_directory, filename):
    ''' Removes file if exists. '''
    file_to_remove = os.path.join(destination_directory, filename)
    if os.path.exists(file_to_remove):
        os.remove(file_to_remove)

def remove_merged_timeline_files(destination_to_source_mapping, root_directory):
    ''' Removes merged timeline files if they exist. '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, TIMELINE_FILE_PATTERN)

def remove_temp_subset_files(destination_to_source_mapping, root_directory):
    ''' Removes temporary subset files if exists. '''
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, "temp_subset.txt")

def remove_source_subdirectories(destination_to_source_mapping, root_directory):
    ''' Removes working source subdirectories from destination study path. '''
    for destination, source_id_to_sourcemappings in destination_to_source_mapping.items():
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_id_to_sourcemappings if source not in SKIPPED_SOURCE_STUDIES[destination]]
        for source_subdirectory in source_subdirectories:
            shutil.rmtree(source_subdirectory)

#------------------------------------------------------------------------------------------------------------
# General helper function for writing data to a file.

def write_data_list_to_file(filename, data_list):
    '''
        Writes data to file where 'data_list' is a
        list of formatted data to write to given file.
    '''
    with open(filename, "w") as f:
            f.write('\n'.join(data_list) + "\n")

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

    # STEP 1: General Setup - loading mappings into dictionaries to work with.
    # create a dictionary mapping (destination - source) to a SourceMappings object
    # SourceMappings object contains a list of patients to subset and list of clinical attributes to pull
    destination_to_source_mapping = create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, root_directory)
    source_id_to_path_mapping = create_source_id_to_path_mapping(destination_to_source_mapping, data_source_directories, crdb_fetch_directory)

    # STEP 2: Subsetting source directory files into destination/source sub-directory.
    # subset everything including clinical files
    subset_source_directories_for_destination_studies(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib)

    # STEP 3: Intermediary data processing - filter clinical annotations and rename patient ids.
    # filter out - rewrite clinical files to only include wanted columns and rename pids
    remove_unwanted_clinical_annotations_in_subsetted_source_studies(destination_to_source_mapping, root_directory)
    convert_source_to_destination_pids_in_subsetted_source_studies(destination_to_source_mapping, root_directory)

    # STEP 4: Merge all source directory files (clinical and genomic) across destination/source subdirectories (destination/source1, destination/source2, destination/source3).
    # merge everything together - including legacy and current clinical files
    merge_source_directories_for_destination_studies(destination_to_source_mapping, root_directory, lib)

    # STEP 5: Merge legacy and current clinical files (data_clinical_patient/sample.txt) with data_clinical.txt.
    merge_clinical_files(destination_to_source_mapping, root_directory, lib)

    # STEP 6: Subset CRDB-fetched timeline files and overwrite existing subsetted/merged timeline files.
    # overwrite timeline with CRDB-timeline subset
    subset_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib)

    # STEP 7: Post-processing.
    # post-processing destination study data
    post_process_and_cleanup_destination_study_data(destination_to_source_mapping, root_directory)

    # generate all logging and trigger files
    identify_missing_metafiles_for_destination_studies(destination_to_source_mapping, root_directory)
    generate_import_trigger_files(destination_to_source_mapping, temp_directory)
    generate_warning_file(temp_directory, warning_file)

if __name__ == '__main__':
    main()
