#!/usr/bin/env python
import argparse
import os
import sys

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
