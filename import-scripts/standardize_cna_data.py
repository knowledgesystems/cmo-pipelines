#!/usr/bin/env python

""" standardize_cna_data.py
This script rewrites a CNA file to replace all blank values with 'NA'.

Usage:
    python standardize_cna_data.py --filename $INPUT_CNA_FILE
Example:
    python standardize_cna_data.py --filename /path/to/data_cna.txt

"""

import sys
import argparse
import os

import validation_utils

ERROR_FILE = sys.stderr


def main():
    # Receive path to clinical file as an argument
    parser = argparse.ArgumentParser(prog='standardize_cna_data.py')
    parser.add_argument(
        '-f',
        '--filename',
        dest='filename',
        action='store',
        required=True,
        help='path to data_cna.txt file',
    )

    args = parser.parse_args()
    filename = args.filename

    # Check that the CNA file exists
    if not os.path.exists(filename):
        print >> ERROR_FILE, 'No such file: ' + filename
        parser.print_help()

    # Standardize blank values in the CNA file to 'NA'
    try:
        validation_utils.standardize_cna_file(filename)
    except ValueError as error:
        print >> ERROR_FILE, 'Unable to write standardized CNA data: ' + error
        sys.exit(2)


if __name__ == '__main__':
    main()
