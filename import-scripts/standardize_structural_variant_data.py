#!/usr/bin/env python

""" standardize_structural_variant_data.py
This script standardizes a structural variant data file by:
    * Removing records with invalid genes, ie, a record with no values for all of the
    following fields:
        - Site1_Hugo_Symbol
        - Site2_Hugo_Symbol
        - Site1_Entrez_Gene_Id
        - Site2_Entrez_Gene_Id
    * Standardizing the 'Sample_ID' column to 'Sample_Id'

Usage:
    python standardize_structural_variant_data.py --filename $INPUT_STRUCTURAL_VARIANT_FILE
Example:
    python standardize_structural_variant_data.py --filename /path/to/data_sv.txt

"""

import sys
import argparse
import os

import validation_utils

ERROR_FILE = sys.stderr


def main():
    # Receive path to clinical file as an argument
    parser = argparse.ArgumentParser(prog='standardize_structural_variant_data.py')
    parser.add_argument(
        '-f',
        '--filename',
        dest='filename',
        action='store',
        required=True,
        help='path to structural_variant file',
    )

    args = parser.parse_args()
    filename = args.filename

    # Check that the structural variant file exists
    if not os.path.exists(filename):
        print >> ERROR_FILE, 'No such file: ' + filename
        parser.print_help()

    try:
        validation_utils.standardize_sv_file(filename)
    except ValueError as error:
        print >> ERROR_FILE, 'Unable to write standardized structural_variant data: ' + error
        sys.exit(2)


if __name__ == '__main__':
    main()
