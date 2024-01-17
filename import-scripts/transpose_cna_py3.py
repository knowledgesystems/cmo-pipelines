import argparse
import os
import pandas as pd


def transpose_and_write_cna_file(data_cna_file):
    df = pd.read_csv(data_cna_file, sep='\t', comment='#')
    df = df.rename(columns={'Hugo_Symbol': 'SAMPLE_ID'})
    df = df.T
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
