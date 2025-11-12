#!/usr/bin/env python

""" standardize_clinical_data.py
This script rewrites a clinical/timeline file to replace all NA placeholders/blank values with 'NA'.
Does not make same adjustment to the 'SAMPLE_ID' column (to avoid creating an actual sample tagged 'NA').

Usage:
    python standardize_clinical_data.py --filename $INPUT_CLINICAL_FILE
Example:
    python standardize_clinical_data.py --filename /path/to/data_clinical_patient.txt

"""

import sys
import argparse
import os

import clinicalfile_utils

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout


def main():
    # Receive path to clinical file as an argument
    parser = argparse.ArgumentParser(prog='standardize_clinical_data.py')
    parser.add_argument('-f', '--filename', dest='filename', action='store', required=True, help='path to clinical file')

    args = parser.parse_args()
    filename = args.filename

    # Check that the clinical file exists
    if not os.path.exists(filename):
        print >> ERROR_FILE, 'No such file: ' + filename
        parser.print_help()

    # Standardize blank values in the clinical file to 'NA'
    try:
        clinicalfile_utils.write_standardized_columns(filename, OUTPUT_FILE)
    except ValueError as error:
        print >> ERROR_FILE, 'Unable to write standardized clinical data: ' + error
        sys.exit(2)


if __name__ == '__main__':
    main()
