#! /usr/bin/env python

""" merge_ddp_files.py
This script merges an arbitrary number of DDP files *of the same format* into one combined file.
Its primary use is to merge DDP files from MSK-IMPACT, HEMEPACT, ACCESS to generate merged DDP files
for the msk_solid_heme cohort. This merged DDP file is only used for GENIE cohort creation.
This script requires a clinical file (patient or sample) containing the masterlist of patients
included in the study.
Usage:
    python merge_ddp_files.py --ddp-files $DDP_FILE1 $DDP_FILE2 <...> --clinical-file $CLINICAL_FILE --output-file $OUTPUT_FILE
Example:
    python merge_ddp_files.py --ddp-files mskimpact/ddp/ddp_naaccr.txt mskaccess/ddp/ddp_naaccr.txt mskimpact_heme/ddp/ddp_naaccr.txt \
        --clinical-file msk_solid_heme/data_clinical_patient.txt --output-file merged_ddp_naaccr.txt
"""

import sys
import argparse
import os

import clinicalfile_utils

ERROR_FILE = sys.stderr


def merge_ddp_files(ddp_files, clinical_file, output_file):
    # Get set of patients from clinical file
    patient_id_set = clinicalfile_utils.get_value_set_for_clinical_attribute(clinical_file, 'PATIENT_ID')

    header = []
    patient_id_index = None
    to_write = []

    # Process each DDP file
    for file in ddp_files:
        header_processed = False

        with open(file, 'r') as f:
            for line in f.readlines():
                data = line.rstrip('\n').split('\t')
                if line.startswith('#'):
                    # Skip metadata headers
                    continue
                else:
                    if not header_processed:
                        # Store header information
                        if not header:
                            header = data
                            patient_id_index = clinicalfile_utils.get_index_for_column(header, 'PATIENT_ID')
                            if patient_id_index == -1:
                                raise IndexError('Unable to find PATIENT_ID column in DDP file %s' % (file))
                            to_write.append(line.rstrip('\n'))

                        # Check if header matches headers of other DDP files
                        if header and data != header:
                            raise ValueError('Cannot merge DDP file with differing header: %s.' % (file))

                        header_processed = True
                        continue

                    # Only process the 'PATIENT_ID' column
                    # Check if patient id is in masterlist and isn't a duplicate
                    patient_id = data[patient_id_index]
                    line_to_write = '\t'.join(data)
                    if patient_id in patient_id_set and line_to_write not in to_write:
                        to_write.append(line_to_write)

    clinicalfile_utils.write_data_list_to_file(output_file, to_write)


def main():
    parser = argparse.ArgumentParser(prog='merge_ddp_files.py')
    parser.add_argument(
        '-d',
        '--ddp-files',
        dest='ddp_files',
        action='store',
        required=True,
        nargs='+',
        help='paths to DDP files to merge',
    )
    parser.add_argument(
        '-c',
        '--clinical-file',
        dest='clinical_file',
        action='store',
        required=True,
        help='path to clinical file containing patient masterlist',
    )
    parser.add_argument(
        '-o',
        '--output-file',
        dest='output_file',
        action='store',
        required=True,
        help='output path for merged DDP file',
    )

    args = parser.parse_args()
    ddp_files = args.ddp_files
    clinical_file = args.clinical_file
    output_file = args.output_file

    # Check that the DDP files exist
    for file in ddp_files:
        if not os.path.exists(file):
            print >> ERROR_FILE, 'No such DDP file: ' + file
            parser.print_help()

    # Check that the clinical file exists
    if not os.path.exists(clinical_file):
        print >> ERROR_FILE, 'No such clinical file: ' + file
        parser.print_help()

    # Merge the DDP files
    merge_ddp_files(ddp_files, clinical_file, output_file)


if __name__ == '__main__':
    main()
