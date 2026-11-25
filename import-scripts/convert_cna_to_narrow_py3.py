#!/usr/bin/env python3

""" convert_cna_to_narrow_py3.py
This script converts a data_CNA.txt file from matrix format (Hugo_Symbol rows, Sample ID columns)
to a narrow format with columns: SAMPLE_ID, Hugo_Symbol, Alteration.

Usage:
    python3 convert_cna_to_narrow_py3.py $INPUT_DATA_CNA_FILE_PATH [$OUTPUT_FILE_PATH]
Example:
    python3 convert_cna_to_narrow_py3.py path/to/data_cna.txt path/to/output.txt
"""

import argparse
import os
import pandas as pd


def convert_cna_to_narrow(data_cna_file, output_file=None):
    # Reads CNA file, skipping comments
    df = pd.read_csv(data_cna_file, sep='\t', comment='#')
    
    # Ensure Hugo_Symbol column exists
    if 'Hugo_Symbol' not in df.columns:
        raise ValueError("Input file must contain a 'Hugo_Symbol' column")
    
    # Set Hugo_Symbol as index for easier melting
    df = df.set_index('Hugo_Symbol')
    
    # Convert from wide format to narrow format
    # This creates a dataframe with Hugo_Symbol (index), SAMPLE_ID (columns), and Alteration (values)
    df_narrow = df.reset_index().melt(
        id_vars=['Hugo_Symbol'],
        var_name='SAMPLE_ID',
        value_name='Alteration'
    )
    
    # Reorder columns to match desired format: SAMPLE_ID, Hugo_Symbol, Alteration
    df_narrow = df_narrow[['SAMPLE_ID', 'Hugo_Symbol', 'Alteration']]
    
    # Determine output file path
    if output_file is None:
        # If no output file specified, create one based on input file name
        base_name = os.path.splitext(data_cna_file)[0]
        output_file = f"{base_name}_narrow.txt"
    
    # Write the narrow format dataframe to the output file
    df_narrow.to_csv(output_file, sep='\t', index=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Convert data_CNA.txt file from matrix format to narrow format (SAMPLE_ID, Hugo_Symbol, Alteration)'
    )
    parser.add_argument('data_cna_file', help='Path to input data_CNA.txt file')
    parser.add_argument('output_file', nargs='?', default=None, help='Path to output file (optional, defaults to input_file_narrow.txt)')
    args = parser.parse_args()
    data_cna_file = args.data_cna_file
    output_file = args.output_file

    # Ensure that data_CNA.txt file exists
    if not os.path.exists(data_cna_file):
        raise FileNotFoundError(f'File not found at {data_cna_file}')

    convert_cna_to_narrow(data_cna_file, output_file)
