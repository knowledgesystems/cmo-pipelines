#! /usr/bin/env python

""" standardize_mutations_data.py
This script rewrites a mutations file standardize all 'NCBI_Build' values to include the prefix 'GRCh'.
Usage:
    python standardize_mutations_data.py --filename $INPUT_MUTATIONS_FILE
Example:
    python standardize_mutations_data.py --filename /path/to/data_mutations.txt
"""

import sys
import argparse
import os

import validation_utils

ERROR_FILE = sys.stderr


def main():
    # Receive path to clinical file as an argument
    parser = argparse.ArgumentParser(prog='standardize_mutations_data.py')
    parser.add_argument(
        '-f',
        '--filename',
        dest='filename',
        action='store',
        required=True,
        help='path to data_mutations.txt file',
    )

    args = parser.parse_args()
    filename = args.filename

    # Check that the mutations file exists
    if not os.path.exists(filename):
        print >> ERROR_FILE, 'No such file: ' + filename
        parser.print_help()

    # Standardize the mutations file
    try:
        validation_utils.standardize_mutations_file(filename)
    except ValueError as error:
        print >> ERROR_FILE, 'Unable to write standardized mutations data: ' + error
        sys.exit(2)


if __name__ == '__main__':
    main()
