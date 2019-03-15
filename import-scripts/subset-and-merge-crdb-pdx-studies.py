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
DESTINATION_STUDY_ID_KEY = "DESTINATION_STUDY_ID"
DESTINATION_PATIENT_ID_KEY = "DESTINATION_PATIENT_ID"
CMO_ROOT_DIRECTORY = "/data/portal-cron/cbio-portal-data/bic-mskcc/"
DATAHUB_NAME = "datahub"

MERGE_GENOMIC_FILES_SUCCESS = "MERGE_GENOMIC_FILES_SUCCESS"
SUBSET_CLINICAL_FILES_SUCCESS = "SUBSET_CLINICAL_FILES_SUCCESS"
HAS_ALL_METAFILES = "HAS_ALL_METAFILES"

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
# TO TRACK WHETHER OR NOT TO IMPORT (TRIGGER FILES)
# for each step (i.e subset sources/genomic data, merging, subset crdb-pdx clinical) - set a status flag in a map
# at the end, evaluate status flags in map for each destination study
# if all status flags are successful - touch destination study trigger  file

# TODO: check if destination directory exists, if not thorw an error
# Potentially automatically create destination directory + create new row in portal config + import into triage
class Patient():
    def __init__(self, cmo_pid, dmp_pid):
        self.cmo_pid = cmo_pid
        self.dmp_pid = dmp_pid

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
def create_destination_to_source_mapping(records, root_directory):
    destination_to_source_mapping = {}
    for record in records:
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
            DESTINATION_STUDY_STATUS_FLAGS[destination] = { MERGE_GENOMIC_FILES_SUCCESS : False, SUBSET_CLINICAL_FILES_SUCCESS : False, HAS_ALL_METAFILES : False }
        if destination not in SKIPPED_SOURCE_STUDIES:
            SKIPPED_SOURCE_STUDIES[destination] = set()
        if destination not in destination_to_source_mapping:
            destination_to_source_mapping[destination] = {}
        if source not in destination_to_source_mapping[destination]:
            destination_to_source_mapping[destination][source] = []
        destination_to_source_mapping[destination][source].append(Patient(cmo_pid, dmp_pid))
        # special case code that adds impact study as a source if destination pid is dmp_pid (P-\d{7})
        if re.match(r'P-\d{7}\Z', dmp_pid):
            if IMPACT_STUDY_ID not in destination_to_source_mapping[destination]:
                destination_to_source_mapping[destination][IMPACT_STUDY_ID] = []
            destination_to_source_mapping[destination][IMPACT_STUDY_ID].append(Patient(dmp_pid, dmp_pid))
    return destination_to_source_mapping

def resolve_source_study_path(source_id, data_source_directories):
    # find all potential source paths for given source id in every cmo root directory
    # ideally each source id will only resolve to one study path and if multiple
    # study paths are found then the source id is non-unique to the cmo root directories
    # and will be reported

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


# split cancer study identifer on first three underscores to create path
# { ke_07_83_b : CMO_ROOT_DIRECTORY/ke/07/83/b }
def create_source_id_to_path_mapping(destination_to_source_mapping, data_source_directories, impact_root_directory):
    source_id_to_path_mapping = {}
    source_ids = set()
    for source_to_patients_map in destination_to_source_mapping.values():
        source_ids.update(source_to_patients_map.keys())
    for source_id in source_ids:
        # special case handling for establishing impact directory (not in cmo)
        if source_id == IMPACT_STUDY_ID:
            source_path = os.path.join(impact_root_directory, source_id)
            source_id_to_path_mapping[source_id] = source_path
            continue;
        # resolved source path 'None' handled by resolve_source_study_path(...)
        source_id_to_path_mapping[source_id] = resolve_source_study_path(source_id, data_source_directories)
    return source_id_to_path_mapping

def generate_python_subset_call(lib, cancer_study_id, destination_directory, source_directory, patient_list):
    python_subset_call = 'python ' + lib + '/generate-clinical-subset.py --study-id=' + cancer_study_id + ' --clinical-file=' + source_directory + '/data_clinical.txt --filter-criteria="PATIENT_ID=' + patient_list + '" --subset-filename=' + destination_directory + "/subset_file.txt"
    return python_subset_call

def generate_bash_subset_call(lib, cancer_study_id, destination_directory, source_directory, patient_list, source_sample_file):
    bash_subset_call = 'bash ' + lib + '/subset-impact-data.sh -i=' + cancer_study_id + ' -o=' + destination_directory + ' -f="PATIENT_ID=' + patient_list + '" -s=' + destination_directory + '/temp_subset.txt -d=' + source_directory + ' -c=' + os.path.join(source_directory, source_sample_file)
    return bash_subset_call

def generate_merge_call(lib, cancer_study_id, destination_directory, subdirectory_list):
    merge_call = 'python ' + lib + '/merge.py -d ' + destination_directory + ' -i ' + cancer_study_id + ' -m "true" ' + subdirectory_list
    return merge_call

# used in sample-mode because data_clinical_sample.txt is being extended - sample id should be used as primary key for each record
def generate_add_clinical_records_call(lib, destination_directory, impact_source_subdirectory):
    add_clinical_records_call = 'python ' + lib + '/add_clinical_records.py -c ' + destination_directory + '/data_clinical_sample.txt -s ' + impact_source_subdirectory + '/data_clinical_sample.txt -f "PATIENT_ID,SAMPLE_ID,ONCOTREE_CODE" -l' + lib
    return add_clinical_records_call

# generates files containing sample-ids linked to specified patient-ids (by destination-source)
# placed in corresponding directories - multiple source per destination
# i.e (/home/destination_study/source_1/subset_list, home/destination_study/source_2/subset_list)
# not currently used -- covered by merge script (but might be needed later on)
def generate_all_subset_sample_lists(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # destination directory is a working subdirectory matching the source
            destination_directory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            if not os.path.isdir(destination_directory):
                os.makedirs(destination_directory)
            if source_directory:
                patient_list = ','.join([patient.cmo_pid for patient in patient])
                subset_script_call = generate_python_subset_call(lib, destination, destination_directory, source_directory, patient_list)
                subprocess.call(subset_script_call, shell = True)
            else:
                print "ERROR: source path for " + source + " could not be found, skipping..."

# subsets source genomic files into destination/source sub-directory
def subset_genomic_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        for source, patients in source_to_patients_map.items():
            # destination directory is a working subdirectory matching the source
            destination_directory = os.path.join(root_directory, destination, source)
            source_directory = source_id_to_path_mapping[source]
            if not os.path.isdir(destination_directory):
                os.makedirs(destination_directory)
            if source_directory:
                patient_list = ','.join([patient.cmo_pid for patient in patients])
                if source == IMPACT_STUDY_ID:
                    subset_genomic_files_call = generate_bash_subset_call(lib, destination, destination_directory, source_directory, patient_list, "data_clinical_sample.txt")
                else:    
                    subset_genomic_files_call = generate_bash_subset_call(lib, destination, destination_directory, source_directory, patient_list, "data_clinical.txt")
                subset_genomic_files_status = subprocess.call(subset_genomic_files_call, shell = True)
                # studies which cannot be subsetted are marked to be skipped when mergin
                if subset_genomic_files_status != 0:
                    SKIPPED_SOURCE_STUDIES[destination].add(source)
                    shutil.rmtree(destination_directory)
            else:
                print "Error, source path for " + source + " could not be found, skipping..."

# merge all genomic files across destination/source subdirectores (destination1/source1, destination1/source2, destination1/source3)
def merge_genomic_files(destination_to_source_mapping, root_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        destination_directory = os.path.join(root_directory, destination)
        # exclude studies which weren't successfully subsetted
        # allows study to still be updated/committed even if some CRDB-fetched mappings are invalid
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_to_patients_map if source not in SKIPPED_SOURCE_STUDIES[destination]]
        source_subdirectory_list = ' '.join(source_subdirectories)
        for source_subdirectory in source_subdirectories:
            touch_missing_metafiles(source_subdirectory)
        merge_source_subdirectories_call = generate_merge_call(lib, destination, destination_directory, source_subdirectory_list)
        merge_source_subdirectories_status = subprocess.call(merge_source_subdirectories_call, shell = True)
        if merge_source_subdirectories_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][MERGE_GENOMIC_FILES_SUCCESS] = True

# needed because IMPACT study is being automatically pulled in and need to be added to the existing data_clinical_sample.txt
# data_clinical_sample.txt per destination study  is originally subsetted from CRDB-PDX fetched file and does not contain IMPACT samples
# unmapped IMPACT samples are not mapped to their patients/shown as seperate patients in the portal
def add_patient_sample_records(destination_to_source_mapping, root_directory, lib):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        impact_source_subdirectory = os.path.join(root_directory, destination, IMPACT_STUDY_ID)
        add_clinical_records_call = generate_add_clinical_records_call(lib, destination_directory, impact_source_subdirectory)
        add_clinical_records_status = subprocess.call(add_clinical_records_call, shell = True)
       
def remove_source_subdirectories(destination_to_source_mapping, root_directory):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        source_subdirectories = [os.path.join(root_directory, destination, source) for source in source_to_patients_map if source not in SKIPPED_SOURCE_STUDIES[destination]]
        for source_subdirectory in source_subdirectories:
            shutil.rmtree(source_subdirectory)

# needed because CMO studies are unannotated but IMPACT studies are
# a merge of CMO + IMPACT studies results in a partially annotated MAF
# (IMPACT study is pulled in whenever patient-ids are DMP ids)
# annotator assumes MAF is annotated because HGVSp_short column is present - CMO samples never annotated
def remove_hgvsp_short_column(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        maf = os.path.join(destination_directory, "data_mutations_extended.txt")
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
 
def remove_merged_clinical_timeline_files(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, "data_clinical.txt")
        remove_file_if_exists(destination_directory, "data_timeline.txt")

def remove_file_if_exists(destination_directory, filename):
    file_to_remove = os.path.join(destination_directory, filename)
    if os.path.exists(file_to_remove):
        os.remove(file_to_remove)

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

# assumes directory starts off without metafiles
# does not check whether metafiles already exist
# metafiles are touched as long as a matching datafile is found in the directory
def touch_missing_metafiles(directory):
    for file in os.listdir(directory):
        metafile_name = get_matching_metafile_name(file)
        if metafile_name:
            touch_metafile_call = "touch " + os.path.join(directory, metafile_name)
            subprocess.call(touch_metafile_call, shell = True)

def filter_temp_subset_files(destination_to_source_mapping, root_directory):
    for destination in destination_to_source_mapping:
        destination_directory = os.path.join(root_directory, destination)
        remove_file_if_exists(destination_directory, "temp_subset.txt")

# subsets clinical files from crdb-pdx fetch directory into the top level destination directory
def subset_clinical_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib):
    for destination, source_to_patients_map in destination_to_source_mapping.items():
        patient_list = ','.join([patient.dmp_pid for patients in source_to_patients_map.values() for patient in patients])
        # destination directory is main study directory
        destination_directory = os.path.join(root_directory, destination)
        subset_clinical_files_call = generate_bash_subset_call(lib, destination, destination_directory, crdb_fetch_directory, patient_list, "data_clinical_sample.txt")
        subset_clinical_files_status = subprocess.call(subset_clinical_files_call, shell = True)
        if subset_clinical_files_status == 0:
            DESTINATION_STUDY_STATUS_FLAGS[destination][SUBSET_CLINICAL_FILES_SUCCESS] = True

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

# goes through all files in a directory and checks if corresponding metafiles exist
# files which do not require a metafile are ignored
def get_missing_metafiles_in_directory(directory):
    expected_metafiles = [get_matching_metafile_name(file) for file in os.listdir(directory)]
    missing_metafiles = [metafile for metafile in expected_metafiles if metafile and not os.path.exists(os.path.join(directory, metafile))]
    return missing_metafiles

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

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--data-source-directories", help = "comma-delimited root directories to search all data source directories", required = True)
    parser.add_argument("-f", "--fetch-directory", help = "directory where crdb-pdx data is stored", required = True)
    parser.add_argument("-i", "--impact-root-directory", help = "msk_solid_heme (combined IMPACT merge) directory", required = True)
    parser.add_argument("-l", "--lib", help = "directory containing subsetting/merge scripts (i.e cmo-pipelines/import-scripts)", required = True)
    parser.add_argument("-m", "--mapping-file", help = "CRDB-fetched file containing mappings from souce/id to destination/id", required = True)
    parser.add_argument("-r", "--root-directory", help = "root directory for all new studies (i.e dmp to mskimpact, hemepact, raindance...", required = True)
    parser.add_argument("-t", "--temp-directory", help = "temp directory to store trigger files", required = True)
    parser.add_argument("-w", "--warning-file", help = "file to store all warnings/errors for email", required = True)

    args = parser.parse_args()
    data_source_directories = map(str.strip, args.data_source_directories.split(','))
    crdb_fetch_directory = args.fetch_directory
    impact_root_directory = args.impact_root_directory
    destination_to_source_mapping_filename = os.path.join(crdb_fetch_directory, args.mapping_file)
    lib = args.lib
    root_directory = args.root_directory
    temp_directory = args.temp_directory
    warning_file = args.warning_file

    records = parse_file(destination_to_source_mapping_filename)
    destination_to_source_mapping = create_destination_to_source_mapping(records, root_directory)
    source_id_to_path_mapping = create_source_id_to_path_mapping(destination_to_source_mapping, data_source_directories, impact_root_directory)
    subset_genomic_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, lib)
    merge_genomic_files(destination_to_source_mapping, root_directory, lib)
    remove_merged_clinical_timeline_files(destination_to_source_mapping, root_directory)
    subset_clinical_timeline_files(destination_to_source_mapping, source_id_to_path_mapping, root_directory, crdb_fetch_directory, lib)
    add_patient_sample_records(destination_to_source_mapping, root_directory, lib)
    remove_source_subdirectories(destination_to_source_mapping, root_directory)
    remove_hgvsp_short_column(destination_to_source_mapping, root_directory)
    filter_temp_subset_files(destination_to_source_mapping, root_directory)
    get_all_destination_to_missing_metafiles_mapping(destination_to_source_mapping, root_directory)
    generate_import_trigger_files(destination_to_source_mapping, temp_directory)
    generate_warning_file(temp_directory, warning_file)

main()
