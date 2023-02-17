#!/usr/bin/env python
import os

import clinicalfile_utils
import generate_case_lists

SAMPLE_ID_COLUMN = "SAMPLE_ID"
SEQUENCED_SAMPLES_HEADER_TAG = "#sequenced_samples:"

def standardize_cna_file(cna_file):
    """
        Various processes for standardizing a CNA file.
        Other steps can be added in the future if necessary.
    """
    if not os.path.isfile(cna_file):
        print "Specified CNA file (%s) does not exist, no changes made..." % (cna_file)
        return
    fill_in_blank_cna_values(cna_file)

def fill_in_blank_cna_values(cna_file):
    """
        Fill in blank values with "NA" to pass validation
        e.g data_CNA.txt fails validation with blanks
    """
    header_processed = False
    header = []
    to_write = []
    ignored_columns = ["Hugo_Symbol", "Entrez_Gene_Id"]

    with open(cna_file, "r") as f:
        for line in f.readlines():
            data = line.rstrip('\n').split('\t')
            if line.startswith('#'):
                # automatically add commented out lines
                to_write.append(line.rstrip('\n'))
            else:
                if not header_processed:
                    header = data
                    to_write.append(line.rstrip('\n'))
                    header_processed = True
                    continue
                # create copy of list with "NA" replacing blank/empty spaces
                # no replacement occurs if there is a value OR value is in Hugo_Symbol/Entrez_Gene_Id column
                processed_data = [data_value if data_value or header[index] in ignored_columns else "NA" for index, data_value in list(enumerate(data))]
                to_write.append('\t'.join(processed_data))

    clinicalfile_utils.write_data_list_to_file(cna_file, to_write)

def standardize_gene_matrix_file(gene_matrix_file):
    """
        Various processes for standardizing a gene matrix file.
        Other steps can be added in the future if necessary.
    """
    if not os.path.isfile(gene_matrix_file):
        print "Specified gene matrix file (%s) does not exist, no changes made..." % (gene_matrix_file)
        return
    fill_in_blank_gene_panel_values(gene_matrix_file)
    remove_duplicate_rows(gene_matrix_file, "SAMPLE_ID")

def fill_in_blank_gene_panel_values(gene_matrix_file):
    """
        Fill in blank values to pass validation with the following logic:
            - blank CNA gene panels will filled in with the mutations gene panel
            - "NA" will be used for all other blank values (or if mutations gene panel is unavailable)
        e.g merges of cmo studies with newer studies results in blank cna gene panel for older samples
            because cmo studies did not have a cna gene panel column
    """
    header_processed = False
    header = []
    to_write = []
    mutations_gene_panel_index = -1
    cna_gene_panel_index = -1

    with open(gene_matrix_file, "r") as f:
        for line in f.readlines():
            data = line.rstrip('\n').split('\t')
            if line.startswith('#'):
                to_write.append(line.rstrip('\n'))
            else:
                if not header_processed:
                    header = data
                    mutations_gene_panel_index = clinicalfile_utils.get_index_for_column(header, 'mutations')
                    cna_gene_panel_index = clinicalfile_utils.get_index_for_column(header, 'cna')
                    to_write.append(line.rstrip('\n'))
                    header_processed = True
                    continue
                # create copy of list with mutations gene panel replacing cna gene panel
                # fill in with NA if mutations gene panel not available
                processed_data = []
                for index, data_value in enumerate(data):
                    if data_value:
                        processed_data.append(data_value)
                    # special case for cna gene panel (copy mutations gene panel if it exists)
                    elif mutations_gene_panel_index != -1 and index == cna_gene_panel_index:
                        processed_data.append(data[mutations_gene_panel_index])
                    # no mutations gene panel available OR any other blank values
                    else:
                        processed_data.append("NA")
                to_write.append('\t'.join(processed_data))

    clinicalfile_utils.write_data_list_to_file(gene_matrix_file, to_write)

def standardize_mutations_file(mutations_file):
    """
        Various processes for standardizing a mutations file.
        Other steps can be added in the future if necessary.
    """
    if not os.path.isfile(mutations_file):
        print "Specified mutations file (%s) does not exist, no changes made..." % (mutations_file)
        return
    fix_invalid_ncbi_build_values(mutations_file)

def fix_invalid_ncbi_build_values(mutations_file):
    """
        Checks for invalid NCBI_Build data values, ie, any data value
        not starting with prefix 'GRCh'. If an invalid value is found,
        'GRCh' is prepended to the value.
    """
    header_processed = False
    header = []
    to_write = []

    with open(mutations_file, "r") as f:
        ncbi_build_value_prefix = "GRCh"
        for line in f.readlines():
            data = line.rstrip("\n").split("\t")
            if line.startswith("#"):
                # Automatically add commented out lines
                to_write.append(line.rstrip("\n"))
            else:
                if not header_processed:
                    header = data
                    ncbi_build_index = clinicalfile_utils.get_index_for_column(header, "NCBI_Build")
                    if ncbi_build_index == -1:
                        print "NCBI_Build column not found in mutations file %s." % (mutations_file)
                        return
                    to_write.append(line.rstrip("\n"))
                    header_processed = True
                    continue
                # Only process the 'NCBI_Build' column
                # Prepend 'GRCh' to the data value if it doesn't already contain this prefix
                ncbi_value = data[ncbi_build_index]
                if ncbi_value and not ncbi_value.startswith(ncbi_build_value_prefix):
                    data[ncbi_build_index] = ncbi_build_value_prefix + ncbi_value
                to_write.append("\t".join(data))

    clinicalfile_utils.write_data_list_to_file(mutations_file, to_write)

def remove_duplicate_rows(filename, record_identifier_column):
    """
        Drop and log duplicate records - where records are identified by the values under specified column (record_identifier_column)
    """
    header_processed = False
    header = []
    to_write = []
    found_record_identifiers = set()
    record_identifier_column_index = -1

    with open(filename, "rU") as f:
        for line in f.readlines():
            if line.startswith('#'):
                to_write.append(line.rstrip('\n'))
                continue
            data = line.rstrip('\n').split('\t')
            if not header_processed:
                header = data
                record_identifier_column_index = clinicalfile_utils.get_index_for_column(header, record_identifier_column)
                if record_identifier_column_index == -1:
                    print "Specified column %s (unique record identifer) not found in file %s." % (record_identifier_column, filename)
                    return
                to_write.append(line.rstrip('\n'))
                header_processed = True
                continue
            # for non-header lines, only add if record identifer has not been seen previously
            record_identifier = data[record_identifier_column_index]
            if record_identifier in found_record_identifiers:
                print "Record already found with unique identifier: (%s), dropping following record: %s" % (record_identifier, '\t'.join(data))
                continue
            to_write.append(line.rstrip('\n'))
            found_record_identifiers.add(record_identifier)

    clinicalfile_utils.write_data_list_to_file(filename, to_write)

def generate_sequenced_samples_header(clinical_file):
    """
        Returns a formatted sequenced samples header containing all
        samples from 'SAMPLE_ID' column in clinical file.
    """
    samples = clinicalfile_utils.get_value_set_for_clinical_attribute(clinical_file, SAMPLE_ID_COLUMN)
    return "%s %s" % (SEQUENCED_SAMPLES_HEADER_TAG, " ".join(list(samples)))

def insert_maf_sequenced_samples_header(clinical_file, maf_file):
    """
        Adds the sequenced sample header to a specified MAF.
        The sequenced sample header is a generated list of all samples inside a specified
        clinical file (with appropriate header tag).
    """
    if not os.path.isfile(clinical_file):
        raise RuntimeError("%s cannot be found." % (clinical_file))
    if not os.path.isfile(maf_file):
        raise RuntimeError("%s cannot be found." % (maf_file))

    # get sequenced samples header from samples in clinical file
    sequenced_samples_header = generate_sequenced_samples_header(clinical_file)

    # load data from maf and insert the sequenced samples header as first row in file
    to_write = [sequenced_samples_header]
    with open(maf_file, "rU") as maf:
        for line in maf.readlines():
            # do not want to accidentally write two sequenced sample headers to file
            if line.startswith(SEQUENCED_SAMPLES_HEADER_TAG):
                continue
            to_write.append(line.rstrip("\n"))

    clinicalfile_utils.write_data_list_to_file(maf_file, to_write)

def call_generate_case_lists(case_list_config_file, case_list_dir, study_dir, study_id, overwrite = False, verbose = False):
    """
        Runs generate_case_lists python script and generates standard case lists
        for a specified study.
        i.e., cases_all.txt, cases_sequenced.txt, etc.

        Arguments passed into the function are equivalent to commandline arguments
        passed into the script.

        If overwrite is False, no new case lists will be generated (assuming case lists already exist).
    """
    specified_args = ['-c', case_list_config_file,
                      '-d', case_list_dir,
                      '-s', study_dir,
                      '-i', study_id]
    if overwrite:
        specified_args.append('-o')
    if verbose:
        specified_args.append('-v')
    parser = generate_case_lists.parse_generate_case_list_args()
    args = parser.parse_args(specified_args)
    try:
        generate_case_lists.main(args)
    # SystemExit because script calls sys.exit() on certain errors
    except SystemExit as e:
        print "Attempt to generate case lists failed with exit status: " + str(e.message)
        raise RuntimeError(e.message)
