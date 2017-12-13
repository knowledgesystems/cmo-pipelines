# imports
import argparse
import linecache
import os
import shutil
import sys
import tempfile

from clinicalfile_utils import *
#-------------------------------------------------------------
# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout
#-------------------------------------------------------------
# functions for creating custom maps
def createCBCustomPrioritiesMap():
    custom_priorities_map = {
        "AGE" : "0",
        "AGE_CURRENT" : "9",
        "DARWIN_PATIENT_AGE" : "9",
        "DARWIN_VITAL_STATUS" : "9",
        "DFS_MONTHS" : "0",
        "DFS_STATUS" : "0",
        "OS_MONTHS" : "0",
        "OS_STATUS" : "9",
        "SAMPLE_TYPE" : "9"
    }
    return custom_priorities_map

def createDefaultMskimpactPrioritiesMap():
    custom_priorities_map = {
        "12_245_PARTC_CONSENTED" : "1",
        "OS_STATUS" : "1",
        "SAMPLE_TYPE" : "1",
        "SEX" : "1",
    }
    return custom_priorities_map

def createSKCMCustomPrioritiesMap():
    custom_priorities_map = {
        "CYTOLYTIC_SCORE" : "79",
        "ESTIMATE_SCORE" : "79",
        "HLA_A" : "79",
        "HLA_B" : "79",
        "HLA_C" : "79",
        "IMMUNE_SCORE" : "79",
        "NEOAGCNT" : "79"
    }
    return custom_priorities_map

def createMelCell2016CustomPrioritiesMap():
    custom_priorities_map = {
        "AGE_AT_DIAGNOSIS" : "0",
        "BIOPSY_TIME" : "0",
        "CBEST_B_MEM" : "0",
        "CBEST_B_NAIVE" : "0",
        "CBEST_DENDRITIC_ACT" : "357",
        "CBEST_DENDRITIC_REST" : "0",
        "CBEST_EOSINOPHILS" : "0",
        "CBEST_MACRO_M0" : "359",
        "CBEST_MACRO_M1" : "359",
        "CBEST_MACRO_M2" : "359",
        "CBEST_MAST_ACT" : "0",
        "CBEST_MAST_REST" : "0",
        "CBEST_MONO" : "0",
        "CBEST_NEUTROPHILS" : "0",
        "CBEST_NK_ACT" : "0",
        "CBEST_NK_REST" : "0",
        "CBEST_PLASMA" : "0",
        "CBEST_T_CD4_MEM_ACT" : "358",
        "CBEST_T_CD4_MEM_REST" : "0",
        "CBEST_T_CD4_NAIVE" : "0",
        "CBEST_T_CD8" : "360",
        "CBEST_T_FOLLI_HEL" : "0",
        "CBEST_T_G_D" : "0",
        "CBEST_T_REG" : "0",
        "CIBERSORT_ABSOLUTE" : "368",
        "CIBERSORT_CORRELATION" : "0",
        "CIBERSORT_P" : "0",
        "CIBERSORT_RMSE" : "0",
        "CYTOLYTIC_SCORE" : "370",
        "DURABLE_CLINICAL_BENEFIT" : "304",
        "ESTIMATE_SCORE" : "366",
        "HLA_A" : "320",
        "HLA_B" : "319",
        "HLA_C" : "318",
        "IMMUNE_SCORE" : "369",
        "MUTATION_LOAD" : "390",
        "M_STAGE" : "303",
        "NEOAGCNT" : "380",
        "ONCOTREE_CODE" : "0",
        "OS_MONTHS" : "400",
        "OS_STATUS" : "400",
        "PATIENT_ID" : "1",
        "PREVIOUS_MAPKI" : "302",
        "PRIMARY_SITE" : "0",
        "SAMPLE_ID" : "1",
        "SEX" : "301",
        "STROMAL_SCORE" : "367",
        "TREATMENT" : "0"
    }
    return custom_priorities_map

def initialize_custom_priority_maps():
    custom_priorities_map = {
        'mskimpact' : createDefaultMskimpactPrioritiesMap(),
        'mel_cell_2016' : createMelCell2016CustomPrioritiesMap(),
        'mixedpact' : createCBCustomPrioritiesMap(),
        #'mskimpact' : createCBCustomPrioritiesMap(),
        'skcm_mskcc_2014' : createSKCMCustomPrioritiesMap(),
        'skcm_mskcc_2015' : createSKCMCustomPrioritiesMap()
    }
    return custom_priorities_map
#-------------------------------------------------------------
def reset_priorities(priority_mapping):
    for attribute in priority_mapping:
        priority_mapping[attribute] = "0"

# overwrites priority line with new custom priority line
def insert_custom_properties(file, priority_mapping, output_file, header):
    priority_line = []
    # fills in priority line (based on ordered header)
    for attribute in header:
        priority_line.append(priority_mapping[attribute])
    os.write(output_file, linecache.getline(file, 1))
    os.write(output_file, linecache.getline(file, 2))
    os.write(output_file, linecache.getline(file, 3))
    if is_old_format(file):
        os.write(output_file, linecache.getline(file,4))
    write_header_line(priority_line, output_file)
    write_data(file, output_file)
#-------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-r", "--reset", help = "reset priorities to 0 before applying overrides", action = "store_true")
    parser.add_argument("-s", "--study-id", help = "stable id to select custom overrides mapping", required = True)
    parser.add_argument("-f", "--files", nargs = "+", help = "files to apply custom overrides to", required = True)
    args = parser.parse_args()

    study_id = args.study_id
    clinical_files = args.files
    reset = args.reset

    # sets up custom priority map (overrides)
    custom_priority_maps = initialize_custom_priority_maps()
    if study_id not in custom_priority_maps:
        print >> ERROR_FILE, "No custom priorities associated with study_id"
        return
    else:
        custom_map = custom_priority_maps[study_id]
    # check file (args) validity and return error if any file fails check
    missing_clinical_files = [clinical_file for clinical_file in clinical_files if not os.path.exists(clinical_file)]
    if len(missing_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not found: ' + ', '.join(missing_clinical_files)
        sys.exit(2)
    not_writable_clinical_files = [clinical_file for clinical_file in clinical_files if not os.access(clinical_file,os.W_OK)]
    if len(not_writable_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not writable: ' + ', '.join(not_writable_clinical_files)
        sys.exit(2)
    missing_metadata_header_files = [clinical_file for clinical_file in clinical_files if not all([linecache.getline(clinical_file, x).startswith('#') for x in range(1,5)])]
    if len(missing_metadata_header_files) > 0:
        print >> ERROR_FILE, 'File(s) incorrectly formatted (missing metadata headers): ' + ', '.join(missing_metadata_header_files)
        sys.exit(2)
    missing_column_header_files = [clinical_file for clinical_file in clinical_files if len(get_header(clinical_file)) == 0]
    if len(missing_column_header_files) > 0:
        print >> ERROR_FILE, 'File(s) incorrectly formatted (missing column headers): ' + ', '.join(missing_column_header_files)
        sys.exit(2)
    for clinical_file in clinical_files:
        header = get_header(clinical_file)
        #get existing priority mappings - replace with provided overrides
        priority_mapping = get_priority_mapping(clinical_file)
        if reset:
            reset_priorities(priority_mapping)
        for attribute in custom_map:
            if attribute in priority_mapping.keys():
                priority_mapping[attribute] = custom_map[attribute]
        # create and write to temp file
        temp_file, temp_file_name = tempfile.mkstemp()
        insert_custom_properties(clinical_file, priority_mapping, temp_file, header)
        os.close(temp_file)
        # replace original file with new file
        shutil.move(temp_file_name, clinical_file)
#-------------------------------------------------------------
if __name__ == '__main__':
    main()
