import sys
import argparse
import os

import clinicalfile_utils

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

def main():
    # get command line stuff
    parser = argparse.ArgumentParser(prog="filter_clinical_data.py")
    parser.add_argument('-c', '--clinical-file', dest='clinical_filename', action='store', required=True, help='path to clinical file')
    parser.add_argument('-e', '--exclude-columns', dest='exclude_column_names', action='store', required=True, help='comma-delimited list of columns to remove from the clinical file')

    args = parser.parse_args()
    clinical_filename = args.clinical_filename
    exclude_column_names = args.exclude_column_names.split(',')

    # check arguments
    if not os.path.exists(clinical_filename):
        print >> ERROR_FILE, "No such file: " + clinical_filename
        parser.print_help()

    # remove columns from the clinical file
    try:
        clinicalfile_utils.write_and_exclude_columns(clinical_filename, exclude_column_names, OUTPUT_FILE)
    except ValueError as error:
        print >> ERROR_FILE, "One or more of the input columns was not found in the header:", error
        print >> ERROR_FILE, "No columns have been removed from the clinical file"
        sys.exit(2) 

if __name__ == '__main__':
    main()
