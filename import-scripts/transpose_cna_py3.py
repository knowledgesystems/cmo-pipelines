#!/usr/bin/env python3

""" transpose_cna_py3.py
This script transposes the data_CNA.txt file so that sample IDs are in the first column rather than across the header.
This format was requested to improve ease of anonymizing sample IDs for the Sophia Genetics data.

Usage:
    python3 transpose_cna_py3.py $INPUT_DATA_CNA_FILE_PATH
Example:
    python3 transpose_cna_py3.py path/to/data_cna.txt \
"""

import argparse
import os
import pandas as pd


def transpose_and_write_cna_file(data_cna_file):
    # Reads CNA file, skipping comments
    df = pd.read_csv(data_cna_file, sep='\t', comment='#')

    # Renames the Hugo_Symbol column so that the column header is correct when transposed
    df = df.rename(columns={'Hugo_Symbol': 'SAMPLE_ID'})

    # Transposes the data frame
    df = df.T

    # Writes the transposed data frame to the original file path
    df.to_csv(data_cna_file, sep='\t', header=None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Transpose data_CNA.txt file so that sample IDs are in the first column rather than across the header'
    )
    parser.add_argument('data_cna_file', help='Path to location data_CNA.txt file')
    args = parser.parse_args()
    data_cna_file = args.data_cna_file

    # Ensure that data_CNA.txt file exists
    if not os.path.exists(data_cna_file):
        raise FileNotFoundError(f'File not found at {data_cna_file}')

    transpose_and_write_cna_file(data_cna_file)
