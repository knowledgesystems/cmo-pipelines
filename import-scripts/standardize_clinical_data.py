import sys
import argparse
import os

import clinicalfile_utils

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

def main():
    # get command line stuff
    parser = argparse.ArgumentParser(prog="standardize_datafile.py")
    parser.add_argument('-f', '--filename', dest='filename', action='store', required=True, help='path to clinical file')

    args = parser.parse_args()
    filename = args.filename

    # check arguments
    if not os.path.exists(filename):
        print >> ERROR_FILE, "No such file: " + filename
        parser.print_help()

    # remove columns from the clinical file
    try:
        clinicalfile_utils.write_standardized_columns(filename, OUTPUT_FILE)
    except ValueError as error:
        print >> ERROR_FILE, "Unable to write standardized file. Exiting..."
        sys.exit(2) 

if __name__ == '__main__':
    main()
