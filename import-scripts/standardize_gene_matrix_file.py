import sys
import argparse
import os

import validation_utils

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

def main():
    # get command line stuff
    parser = argparse.ArgumentParser(prog="standardize_gene_panel_matrix_file.py")
    parser.add_argument('-g', '--gene-panel-matrix-filename', dest='gene_panel_matrix_filename', action='store', required=True, help='path to gene_panel_matrix file')

    args = parser.parse_args()
    gene_panel_matrix_filename = args.gene_panel_matrix_filename

    # check arguments
    if not os.path.exists(gene_panel_matrix_filename):
        print >> ERROR_FILE, "No such file: " + gene_panel_matrix_filename
        parser.print_help()

    # Standardizes a given gene matrix file
    # Removes duplicate rows
    # Fills in blanks with NA
    try:
        validation_utils.standardize_gene_matrix_file(gene_panel_matrix_filename)
    except ValueError as error:
        print >> ERROR_FILE, "Unable to standardize gene matrix file:", error
        sys.exit(2)

if __name__ == '__main__':
    main()
