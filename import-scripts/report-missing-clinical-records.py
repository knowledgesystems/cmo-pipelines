import sys
import os
import optparse
import re
import shutil

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# TCGA GLOBALS
TCGA_BARCODE_PREFIX = 'TCGA'
TCGA_SAMPLE_BARCODE_PATTERN = '(TCGA-\w*-\w*-\d*).*$'
TCGA_PATIENT_BARCODE_PATTERN = '(TCGA-\w*-\w*).*$'
TCGA_SAMPLE_TYPE_CODE_PATTERN = 'TCGA-\w*-\w*-(\d*).*$'

TCGA_SAMPLE_TYPE_CODES = {
    '01':'Primary Solid Tumor',
    '02':'Recurrent Solid Tumor',
    '03':'Primary Blood Tumor',
    '04':'Recurrent Blood Tumor',
    '06':'Metastatic',
    '10':'Blood Derived Normal',
    '11':'Solid Tissues Normal'
}

# FILENAME PATTERNS AND DATATYPES
META_STUDY_FILENAME = 'meta_study.txt'

GENERAL_CLINICAL_META_FILE_PATTERN = 'meta_clinical.txt'
GENERAL_CLINICAL_DATA_FILE_PATTERN = 'data_clinical.txt'
GENERAL_CLINICAL_DATATYPE = 'CLINICAL'
CLINICAL_PATIENT_META_FILE_KEYWORD = 'patient'
CLINICAL_PATIENT_DATATYPE = 'PATIENT_ATTRIBUTES'
CLINICAL_SAMPLE_META_FILE_KEYWORD = 'sample'
CLINICAL_SAMPLE_DATATYPE = 'SAMPLE_ATTRIBUTES'

MAF_GENERAL_FILE_PATTERN = 'data_mutations_extended.txt'
MAF_UNIPROT_FILE_PATTERN = 'data_mutations_uniprot.txt'
MAF_MSKCC_FILE_PATTERN = 'data_mutations_mskcc.txt'
MAF_DATATYPE = 'MAF'

SEG_DATATYPE = 'SEG'

CASE_LIST_DIRNAME = 'case_lists'
CASES_ALL_FILENAME = 'cases_all.txt'
CASE_LIST_DATATYPE = 'CASE_LIST'

# FOR ORGANIZING DATA FILES BY DATATYPE
GENETIC_ALTERATION_TYPES = {
    'PROFILE':['COPY_NUMBER_ALTERATION', 'PROTEIN_LEVEL', 'MRNA_EXPRESSION', 'METHYLATION'],
    'NORMAL':['MUTATION_EXTENDED', 'SV', 'CLINICAL'],
    'CASE_INDEPENDENT':['MUTSIG', 'GISTIC_GENES_AMP', 'GISTIC_GENES_DEL']
}

# SAMPLE AND PATIENT GLOBALS
STUDY_SAMPLES = {}
EXISTING_CLINICAL_SAMPLES_SET = set()
EXISTING_CLINICAL_PATIENTS_SET = set()

# OUTPUT FILE FOR REPORTING SAMPLES/PATIENTS MISSING CLINICAL INFO
MISSING_CLINICAL_SAMPLES_REPORT_FILENAME = 'samples_missing_clinical_info.txt'

# LIST TO DIFFERENTIATE BETWEEN CASE IDS AND STANDARD COLUMN HEADERS
NON_CASE_IDS = [
    'MIRNA',
    'LOCUS',
    'ID',
    'GENE SYMBOL',
    'ENTREZ_GENE_ID',
    'HUGO_SYMBOL',
    'LOCUS ID',
    'CYTOBAND',
    'COMPOSITE.ELEMENT.REF',
    'HYBRIDIZATION REF',
]

def report_missing_clinical_records(study_directory, remove_normal_records, add_missing_records, output_data_directory):
    """
        Main function. Reports samples and patients in genomic data files that are missing from clinical data files.

        Parameters:
        study_directory:        input study directory to process
        remove_normal_records:    option to remove normal samples from clinical and genomic data files (TCGA samples only)
        add_missing_records:     option to automatically add samples/patients that are missing clinical information into the clinical data file(s)
        output_data_directory:    output directory to write the processed data files and report(s)
    """

    # load all samples from given study directory - returns dict of meta filename {meta file properties, sample_set}
    study_files = load_all_samples_for_study(study_directory)
    samples_missing_clinical_info = {'compiled_missing_clincal_sample_set':set()}

    print >> OUTPUT_FILE, 'report_missing_clinical_records(), INFO: Comparing sample lists from genomic files to samples found in clinical files...'

    # flags for indicating whether normal samples were found in data and whether any non-normal samples are missing clinical info
    normal_samples_found = False
    non_normal_samples_missing_clinical_info_found = False

    # for each meta file loaded, determine which samples are missing from clinical sample/patient sets
    for meta_file,properties in study_files.items():
        # we already know what samples are in clinical files so these can be ignored along w/case independent data files
        if properties['genetic_alteration_type'] == 'CLINICAL' or properties['genetic_alteration_type'] in GENETIC_ALTERATION_TYPES['CASE_INDEPENDENT']:
            continue

        # potential patient ids can come from timeline data files
        is_patient_id = (properties['datatype'] == CLINICAL_PATIENT_DATATYPE)

        # missing samples = samples in sample set that are not in the EXISTING_CLINICAL_SAMPLES_SET
        missing_samples = [sample_id for sample_id in properties['sample_set'] if not sample_id in EXISTING_CLINICAL_SAMPLES_SET]

        # if no missing samples then continue
        if len(missing_samples) == 0:
            continue

        # identify normal samples, as well as non-normal samples that are missing clinical info
        normal_samples = [sample_id for sample_id in missing_samples if is_normal_sample(sample_id, is_patient_id)]
        non_normal_missing_samples = [sample_id for sample_id in missing_samples if not is_normal_sample(sample_id, is_patient_id)]

        # if no non-normal samples found then report that all samples missing clinical info were normal types and will not be added
        if len(non_normal_missing_samples) == 0:
            print >> OUTPUT_FILE, 'report_missing_clinical_records(), WARN: All samples missing clinical info in', properties['data_filename'], 'are NORMAL sample types! These samples will NOT be added to clinical files even if --add-missing-records mode is enabled.'
        else:
            print >> OUTPUT_FILE, 'report_missing_clinical_records(), INFO: Found', len(non_normal_missing_samples), 'samples missing clinical info in:', properties['data_filename']
            non_normal_samples_missing_clinical_info_found = True # set to true since non-normal samples were found
            # update samples_missing_clinical_info and add missing sample set to main 'study_files' dict
            update_samples_missing_clinical_info(samples_missing_clinical_info, non_normal_missing_samples)
            study_files[meta_file]['missing_samples'] = set(non_normal_missing_samples)

        # if normal samples found then update main study files dict
        if len(normal_samples) > 0:
            normal_samples_found = True # set to true since normal samples were found
            study_files[meta_file]['normal_sample_set'] = set(normal_samples)

    # if compiled missing clinical sample set is not empty then generate a report
    if len(samples_missing_clinical_info['compiled_missing_clincal_sample_set']) > 0:
        generate_samples_missing_clinical_info_report(output_data_directory, samples_missing_clinical_info)
    else:
        print >> OUTPUT_FILE, 'report_missing_clinical_records(), INFO: No samples missing clinical data to report.'

    # if normal samples were found and the option to remove normal records is set to true then go ahead and remove from data file(s)
    if normal_samples_found:
        if remove_normal_records:
            find_and_remove_normal_samples(study_directory, study_files, output_data_directory)
        else:
            print >> OUTPUT_FILE, 'report_missing_clinical_records(), WARN: Normal samples found but --remove-normal-records mode is not enabled. Data files will retain normal data.'

    # if non-normal samples found and option to add missing records is true then go ahead and add them to clinical data file(s)
    if non_normal_samples_missing_clinical_info_found:
        if add_missing_records:
            add_missing_clinical_records(study_files, output_data_directory, samples_missing_clinical_info['compiled_missing_clincal_sample_set'])
        else:
            if normal_samples_found:
                print >> OUTPUT_FILE, 'report_missing_clinical_records(), WARN: No missing samples to add since only normal samples were found to be missing from clinical data and thus will not be added to clinical file(s).'
                if not remove_normal_records:
                    print >> OUTPUT_FILE, '* NOTE *: Consider running script with the --remove-normal-records mode enabled to cleanup the data'
    else:
        print >> OUTPUT_FILE, 'report_missing_clinical_records(), Clinical data files are not missing any samples. No changes to make.'

def update_samples_missing_clinical_info(samples_missing_clinical_info, non_normal_missing_samples):
    """
        Update samples_missing_clinical_info dict with non-normal missing sample set and update 'compiled_missing_clincal_sample_set'.
    """
    samples_missing_clinical_info[meta_file] = set(non_normal_missing_samples)
    samples_missing_clinical_info['compiled_missing_clincal_sample_set'].update(non_normal_missing_samples)
    return samples_missing_clinical_info


def add_missing_clinical_records(study_files, output_data_directory, compiled_missing_clincal_sample_set):
    """
        Adds samples/patients that are missing clinical info to the clinical file(s).
    """
    print >> OUTPUT_FILE, '\nAdding patients/samples missing from clinical data files...'

    # create new case list directory if not already exists
    new_case_list_dir = os.path.join(output_data_directory, CASE_LIST_DIRNAME)
    if not os.path.exists(new_case_list_dir):
        os.mkdir(new_case_list_dir)


    for meta_file,properties in study_files.items():
        # if new meta or data file does not already exist in output directory then that means that no attempt to remove
        # normals from data was made and we can go ahead and copy over the data and meta files to the new output directory
        new_meta_filename = os.path.join(output_data_directory, os.path.basename(meta_file))
        if not os.path.exists(new_meta_filename) and not GENERAL_CLINICAL_META_FILE_PATTERN in meta_file and properties['datatype'] != CASE_LIST_DATATYPE:
            shutil.copy(meta_file, new_meta_filename)

        # resolve new data filename
        if properties['datatype'] == CASE_LIST_DATATYPE:
            new_data_filename = os.path.join(new_case_list_dir, os.path.basename(properties['data_filename']))
        else:
            new_data_filename = os.path.join(output_data_directory, os.path.basename(properties['data_filename']))

        # if new data file does not already exist then go ahead and copy over file
        if not os.path.exists(new_data_filename):
            shutil.copy(properties['data_filename'], new_data_filename)

        # we are not making any modifications to non-clinical files - continue
        if properties['genetic_alteration_type'] != 'CLINICAL':
            continue

        print >> OUTPUT_FILE, 'add_missing_clinical_records(), Adding missing patient/sample records to:', new_data_filename
        header = get_file_header(new_data_filename)

        # basically, we are loading the existing data and we are going to append to it - there's probably a better way to do this
        data_file = open(new_data_filename, 'rU')
        filedata = [line for line in data_file.readlines()]
        data_file.close()
        data_file = open(new_data_filename, 'w')
        data_file.write(''.join(filedata))

        # for each sample that is in compiled missing clinical sample set, add it to the new data file
        for sample_id in compiled_missing_clincal_sample_set:
            data = dict(zip(header, ['' for column in header]))
            patient_id = get_stable_id(sample_id, True)    # resolve patient id for sample

            # do not want to add duplicate patient records if patient already exists in file - this could potentially happen if a patient
            # has multiple samples but only one sample exists in the clinical data
            if patient_id in EXISTING_CLINICAL_PATIENTS_SET and properties['datatype'] == CLINICAL_PATIENT_DATATYPE:
                continue
            if sample_id in EXISTING_CLINICAL_SAMPLES_SET:
                continue

            # set the patient id column (always present regardless of clinical data file type)
            data['PATIENT_ID'] = patient_id

            # only add sample if SAMPLE_ID in file header
            if 'SAMPLE_ID' in header:
                # sample id is already in standardized format by now - don't need to get the stable id again
                data['SAMPLE_ID'] = sample_id
            sample_data = map(lambda x: data.get(x,''), header)
            data_file.write('\n' + '\t'.join(sample_data))
        data_file.close()


def find_and_remove_normal_samples(study_directory, study_files, output_data_directory):
    """
        Finds and removes normal samples from data file(s).
    """
    print >> OUTPUT_FILE, '\nRemoving normals from data files in:', study_directory, '- processed files will be written to:', output_data_directory

    # create new case list directory if not already exists
    new_case_list_dir = os.path.join(output_data_directory, CASE_LIST_DIRNAME)
    if not os.path.exists(new_case_list_dir):
        os.mkdir(new_case_list_dir)

    # for each meta file, data - remove normal samples if normal sample set is non-empty
    for meta_file,properties in study_files.items():
        normal_samples = properties.get('normal_sample_set', set())
        if properties['datatype'] == CASE_LIST_DATATYPE:
            new_data_filename = os.path.join(new_case_list_dir, os.path.basename(properties['data_filename']))
        else:
            new_data_filename = os.path.join(output_data_directory, os.path.basename(properties['data_filename']))

        # always copy over the meta file unless it's generic clinical meta file (meta_clinical.txt) or a case list
        # since case lists can have normal samples removed from them
        if not GENERAL_CLINICAL_META_FILE_PATTERN in meta_file and properties['datatype'] != CASE_LIST_DATATYPE:
            shutil.copy(meta_file, os.path.join(output_data_directory, os.path.basename(meta_file)))

        # write or copy over files to output directory sans normal samples
        if len(normal_samples) == 0 or properties['datatype'] == CLINICAL_PATIENT_DATATYPE:
            # if no normal samples found or if a clinical patient file then there's nothing to do - copy file over to output directory
            print >> OUTPUT_FILE, 'find_and_remove_normal_samples(), Nothing to remove from:', properties['data_filename']
            if properties['datatype'] == CASE_LIST_DATATYPE:
                shutil.copy(properties['data_filename'], new_data_filename)
            else:
                shutil.copy(properties['data_filename'], new_data_filename)
        else:
            # remove normal samples from case lists and data files accordingly
            if properties['datatype'] == CASE_LIST_DATATYPE:
                remove_normal_samples_from_case_list(new_data_filename, properties)
            else:
                remove_normal_samples_from_data_file(new_data_filename, properties)


def remove_normal_samples_from_case_list(new_data_filename, properties):
    """
        Remove normal samples from case list file.
    """
    print >> OUTPUT_FILE, 'remove_normal_samples_from_case_list(), Removing normals from:', properties['data_filename']

    data_file = open(properties['data_filename'], 'rU')
    new_data_file = open(new_data_filename, 'w')
    for line in data_file.readlines():
        if not line.startswith('case_list_ids'):
            new_data_file.write(line)
        else:
            # filter out samples found in the normal sample set
            filtered_sample_list = [sample_id for sample_id in properties['sample_set'] if not sample_id in properties['normal_sample_set']]
            new_data_file.write('case_list_ids: ' + '\t'.join(filtered_sample_list))
    new_data_file.close()
    data_file.close()


def remove_normal_samples_from_data_file(new_data_filename, properties):
    """
        Remove normal samples from data file.
    """
    print >> OUTPUT_FILE, 'remove_normal_samples_from_data_file(), Removing normals from:', properties['data_filename']

    data_file = open(properties['data_filename'], 'rU')
    new_data_file = open(new_data_filename, 'w')

    # remove normal sample records by row if 'NORMAL' or 'SEG' datatype (i.e., MAF, Structural Variants) or by column if 'PROFILE' data type
    if properties['genetic_alteration_type'] in GENETIC_ALTERATION_TYPES['NORMAL'] or properties['datatype'] == SEG_DATATYPE:
        header_added = False
        for line in data_file.readlines():
            # keep comments
            if line.startswith('#'):
                new_data_file.write(line)
                continue
            # add header if not already added
            if not header_added:
                new_data_file.write(line)
                header_added = True
                continue
            # add data row if row does not contain a normal sample id
            data = map(str.strip, line.split('\t'))
            if len([value for value in data if value in properties['normal_sample_set']]) > 0:
                continue
            new_data_file.write(line)
    else:
        # 'PROFILE' datatype files get filtered by column, not row
        header = get_file_header(properties['data_filename'])
        filtered_header = [column for column in header if not column in properties['normal_sample_set']]
        new_data_file.write('\t'.join(filtered_header))
        for line in data_file.readlines()[1:]:
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            filtered_data = map(lambda x: data.get(x, ''), filtered_header)
            new_data_file.write('\n' + '\t'.join(filtered_data))
    new_data_file.close()
    data_file.close()


def generate_samples_missing_clinical_info_report(output_data_directory, samples_missing_clinical_info):
    """
        Write report of samples missing clinical info to given study directory.
    """
    output_filename = os.path.join(output_data_directory, MISSING_CLINICAL_SAMPLES_REPORT_FILENAME)
    output_file = open(output_filename, 'w')

    # store the compiled list of samples missing clinical info first, then the remaining files/missing sample sets
    output_file.write('compiled_missing_clincal_sample_set\t' + ','.join(list(samples_missing_clinical_info['compiled_missing_clincal_sample_set'])) + '\n')
    for meta_file,missing_samples in samples_missing_clinical_info.items():
        if meta_file != 'compiled_missing_clincal_sample_set':
            output_file.write(meta_file + '\t' + ','.join(list(missing_samples)))
    output_file.close()
    print >> OUTPUT_FILE, 'generate_samples_missing_clinical_info_report(), Saved missing clinical records report to:', output_filename


def load_all_samples_for_study(study_directory):
    """
        Loads all samples/patients from study directory and stores them in STUDY_SAMPLES.

        Outputs dict 'study_files':
            key = study filename (meta filename)
            values = {
                * properties from meta file (i.e., datatype, genetic_aleration_type, data_filename, etc.) *
                sample_set = set of samples loaded from 'data_filename'
            }
    """
    print >> OUTPUT_FILE, 'Processing data files in cancer study path:', study_directory

    study_files = organize_study_files(study_directory)
    for meta_file,properties in study_files.items():
        if properties['genetic_alteration_type'] in GENETIC_ALTERATION_TYPES['CASE_INDEPENDENT']:
            print >> OUTPUT_FILE, 'load_all_samples_for_study(), WARN: Data file is not dependent on samples - file will be skipped:', meta_file
            continue

        # load samples from data file or case list file
        if properties['datatype'] == CASE_LIST_DATATYPE:
            samples = load_samples_from_case_list(properties)
        else:
            samples = load_samples_from_data_file(study_directory, properties)
            # if clinical sample or general clinical file then add samples to EXISTING_CLINICAL_SAMPLES_SET
            if properties['datatype'] in [GENERAL_CLINICAL_DATATYPE, CLINICAL_SAMPLE_DATATYPE]:
                EXISTING_CLINICAL_SAMPLES_SET.update(samples)
        # update study_files dict with sample set loaded
        study_files[meta_file]['sample_set'] = samples
    return study_files


def load_samples_from_data_file(study_directory, properties):
    """
        Loads samples from data file.
    """
    print >> OUTPUT_FILE, 'load_samples_from_data_file(), Loading samples from data file:', properties['data_filename']
    is_patient_id = False
    samples = set()
    header = get_file_header(properties['data_filename'])
    if properties['genetic_alteration_type'] in GENETIC_ALTERATION_TYPES['NORMAL'] or properties['datatype'] == SEG_DATATYPE:
        if properties['genetic_alteration_type'] == 'CLINICAL':
            # keep track of patient ids seen in clinical data files
            update_clinical_patients_seen(properties['data_filename'], header)
            if not 'SAMPLE_ID' in header:
                case_id_column = 'PATIENT_ID'
                is_patient_id = True
            else:
                case_id_column = 'SAMPLE_ID'
        elif 'Tumor_Sample_Barcode' in header:
            case_id_column = 'Tumor_Sample_Barcode'
        # covers SV file
        elif 'Sample_ID' in header:
            case_id_column = 'Sample_ID'
        elif properties['datatype'] == SEG_DATATYPE:
            case_id_column = 'ID'
        else:
            print >> ERROR_FILE, 'load_samples_from_data_file(), ERROR: Do not know how to extract samples from file:', properties['data_filename']
            print >> ERROR_FILE, 'load_samples_from_data_file(), ERROR: File does not contain key column for clinical files (PATIENT_ID and/or SAMPLE_ID) or mutation file (Tumor_Sample_Barcode) or structural variant file (SampleId)'
            sys.exit(2)
        data_file = open(properties['data_filename'], 'rU')
        data_reader = [l for l in data_file.readlines() if not l.startswith('#')][1:]
        for line in data_reader:
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            samples.add(data[case_id_column])
        data_file.close()
    else:
        samples.update([column for column in header if not column.upper() in NON_CASE_IDS])
    sample_set = set(map(lambda x: get_stable_id(x.strip(), is_patient_id), samples))
    return sample_set


def update_clinical_patients_seen(data_filename, header):
    """
        Update EXISTING_CLINICAL_PATIENTS_SET.
    """
    data_file = open(data_filename, 'rU')
    data_reader = [l for l in data_file.readlines() if not l.startswith('#')][1:]
    for line in data_reader:
        data = dict(zip(header, map(str.strip, line.split('\t'))))
        EXISTING_CLINICAL_PATIENTS_SET.add(get_stable_id(data['PATIENT_ID'], True))
    data_file.close()


def load_samples_from_case_list(properties):
    """
        Loads samples from case list file.
    """
    print >> OUTPUT_FILE, 'load_samples_from_case_list(), Loading samples from case list:', properties['data_filename']
    samples = map(lambda x: get_stable_id(x.strip(), False), properties['case_list_ids'].split('\t'))
    return set(samples)


def organize_study_files(study_directory):
    """
        Organizes meta and data files by genetic alteration type
    """
    study_files = {}
    for f in os.listdir(study_directory):
        # skip data files and meta_study.txt file
        if META_STUDY_FILENAME in f:
            continue
        if not 'meta' in f and not CASE_LIST_DIRNAME in f:
            continue
        # load properties from meta file and case lists
        full_filepath = os.path.join(study_directory, f)
        if 'meta' in f:
            study_files[full_filepath] = load_properties(study_directory, full_filepath, False)
        elif CASE_LIST_DIRNAME in f:
            for clf in os.listdir(full_filepath):
                case_list_filename = os.path.join(full_filepath, clf)
                study_files[case_list_filename] = load_properties(study_directory, case_list_filename, True)
    if GENERAL_CLINICAL_DATA_FILE_PATTERN in os.listdir(study_directory):
        study_files[GENERAL_CLINICAL_META_FILE_PATTERN] = {'datatype':'CLINICAL', 'genetic_alteration_type':'CLINICAL', 'data_filename':GENERAL_CLINICAL_DATA_FILE_PATTERN}
    return study_files


def load_properties(study_directory, filename, is_case_list):
    """
        Loads properties as dictionary.
    """
    properties = {}
    data_file = open(filename, 'rU')
    for line in data_file:
        prop_name = line.split(':')[0]
        prop_value = line.split(':')[1].strip()
        properties[prop_name] = prop_value
    data_file.close()

    if is_case_list:
        properties['datatype'] = CASE_LIST_DATATYPE
        properties['genetic_alteration_type'] = CASE_LIST_DATATYPE
        properties['data_filename'] = filename
    else:
        properties['data_filename'] = os.path.join(study_directory, properties['data_filename'])

    # if MAF meta file then choose uniprot or mskcc maf if exists if regular MAF file does not exist
    # both mskcc and uniprot MAFs should contain the same samples so the selection is arbitrary
    if properties['datatype'] == 'MAF':
        if not os.path.exists(properties['data_filename']):
            for maf_filename in [MAF_GENERAL_FILE_PATTERN, MAF_MSKCC_FILE_PATTERN, MAF_UNIPROT_FILE_PATTERN]:
                if os.path.exists(os.path.join(study_directory, maf_filename)):
                    properties['data_filename'] = os.path.join(study_directory, maf_filename)
                    break

    return properties

def get_patient_id(case_id):
    """
        Resolves patient id if case id is TCGA.
        Otherwise returns case id given.
    """
    if case_id.startswith(TCGA_BARCODE_PREFIX):
        m = re.search(TCGA_PATIENT_BARCODE_PATTERN, case_id)
        return m.group(1)
    return case_id

def get_sample_id(case_id):
    """
        Resolves sample id if case id is TCGA.
        Otherwise returns case id given.
    """
    if case_id.startswith(TCGA_BARCODE_PREFIX):
        try:
            m = re.search(TCGA_SAMPLE_BARCODE_PATTERN, case_id)
            return m.group(1)
        except AttributeError:
            print >> ERROR_FILE, 'get_sample_id(), ERROR: Invalid TCGA sample id format: ' + case_id + ' - this ID cannot be parsed into a standard TCGA sample ID format'
            sys.exit(2)

    return case_id

def get_stable_id(case_id, is_patient_id):
    """
        Calls get_patient_id() or get_sample_id().
    """
    if is_patient_id:
        return get_patient_id(case_id)
    return get_sample_id(case_id)

def is_normal_sample(case_id, is_patient_id):
    """
        Determines whether given case id is a normal sample type if case id matches TCGA pattern.
        This is done by parsing out the sample type codes described here:
            https://gdc.cancer.gov/resources-tcga-users/tcga-code-tables/sample-type-codes
    """
    if is_patient_id or not case_id.startswith(TCGA_BARCODE_PREFIX):
        return False
    m = re.search(TCGA_SAMPLE_TYPE_CODE_PATTERN, case_id)
    sample_type = TCGA_SAMPLE_TYPE_CODES.get(m.group(1), 'Primary Solid Tumor')
    if 'NORMAL' in sample_type.upper():
        return True
    return False

def get_file_header(filename):
    """
        Returns the file header.
    """
    data_file = open(filename, 'rU')
    filedata = [l for l in data_file.readlines() if not l.startswith('#')]
    data_file.close()
    header = map(str.strip, filedata[0].split('\t'))
    return header

def usage(parser):
    parser.print_help()
    sys.exit(2)

def main():
    # get command line stuff
    parser = optparse.OptionParser()
    parser.add_option('-d', '--study-directory', action = 'store', dest = 'studydir', help = "path to study directory")
    parser.add_option('-a', '--add-missing-records', action="store_true", dest = "addmissing", default=False, help = "flag for adding missing records to clinical files")
    parser.add_option('-r', '--remove-normal-records', action="store_true", dest = "removenorms", default=False, help = "flag for removing normal records from data files")
    parser.add_option('-o', '--output-data-directory', action = 'store', dest = 'outputdir', help = "output directory used after removing normal records from data")

    (options, args) = parser.parse_args()
    study_directory = options.studydir
    add_missing_records = options.addmissing
    remove_normal_records = options.removenorms
    output_data_directory = options.outputdir

    if not study_directory:
        print >> ERROR_FILE, 'main(), ERROR: Study directory must be provided!\n'
        usage(parser)
    if not output_data_directory and (add_missing_records or remove_normal_records):
        print >> ERROR_FILE, 'main(), ERROR: Output data directory must be provided when --add-missing-records and/or --remove-normal-records are enabled.\n'
        usage(parser)

    if not os.path.exists(study_directory):
        print >> ERROR_FILE, 'main(), ERROR: No such directory: ' + study_directory, '\n'
        usage(parser)

    report_missing_clinical_records(study_directory, remove_normal_records, add_missing_records, output_data_directory)


if __name__ == '__main__':
    main()
