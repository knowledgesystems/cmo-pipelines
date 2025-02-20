#!/usr/bin/env python3

""" combine_files_py3.py
This script merges an arbitrary number of files into one combined file.
Its primary uses:
- generate CDM deliverable by merging clinical sample file with sequencing date data
- merge and subset CDM timeline files (legacy merge.py script is not efficient enough)
- merge cvr/seq_date.txt files from MSK-IMPACT, HEMEPACT, ACCESS to generate a merged seq_date file
for the msk_solid_heme cohort. This merged seq_date.txt file is only used for the Sophia cohort.
- merge DDP files from MSK-IMPACT, HEMEPACT, ACCESS to generate merged DDP files
for the msk_solid_heme cohort. This merged DDP file is only used for GENIE cohort creation.
Usage:
    python3 combine_files_py3.py --input-files $FILE1 $FILE2 <...> --output-file $OUTPUT_FILE
Example:
    python3 combine_files_py3.py --input-files mskimpact/ddp/ddp_naaccr.txt mskaccess/ddp/ddp_naaccr.txt mskimpact_heme/ddp/ddp_naaccr.txt \
        --output-file merged_ddp_naaccr.txt
"""

import sys
import argparse
import os
import pandas as pd
from functools import reduce
import csv

ERROR_FILE = sys.stderr


def write_tsv(df, path, **opts):
    """
    Writes a TSV file to the given path.
    """
    opts["index"] = opts.get(
        "index", False
    )  # by default, don't write the index column to the output
    df.to_csv(
        path,
        sep="\t",
        **opts,
    )


def combine_files(input_files, output_file, sep="\t", columns=None, merge_type="inner", drop_na=False):
    data_frames = []
    for file in input_files:
        # Determine which line to start reading from
        start_read = 0
        with open(file, "r") as f:
            # Assumes that commented lines are at the start of the file
            for line_count, line in enumerate(f):
                if not line.startswith("#"):
                    start_read = line_count
                    break

        df = pd.read_table(
            file,
            sep=sep,
            float_precision="round_trip",
            na_filter=False,
            low_memory=False,
            skiprows=start_read,
            dtype='object' # this is to prevent ints from being converted to floats
        )
        data_frames.append(df)

    df_merged = reduce(
        lambda left, right: pd.merge(left, right, on=columns, how=merge_type), data_frames
    )

    # Drop rows with blank/NA values if specified
    if drop_na:
        df_merged.dropna(axis=0, inplace=True)

    # Drop duplicate rows
    df_merged.drop_duplicates(inplace=True)

    # Write out the combined file (if not empty)
    if not df_merged.empty:
        write_tsv(
            df_merged,
            output_file,
            quoting=csv.QUOTE_NONE,
        )


def main():
    parser = argparse.ArgumentParser(prog="combine_files_py3.py")
    parser.add_argument(
        "-i",
        "--input-files",
        dest="input_files",
        action="store",
        required=True,
        nargs="+",
        help="paths to files to combine",
    )
    parser.add_argument(
        "-o",
        "--output-file",
        dest="output_file",
        action="store",
        required=True,
        help="output path for combined file",
    )
    parser.add_argument(
        "-s",
        "--separator",
        dest="sep",
        action="store",
        default="\t",
        help="Character or regex pattern to treat as the delimiter",
    )
    parser.add_argument(
        "-c",
        "--columns",
        dest="columns",
        action="store",
        default=None,
        nargs="+",
        help="Column or index level names to join on. If None, defaults to the intersection of the columns in both DataFrames",
    )
    parser.add_argument(
        "-m",
        "--merge-type",
        dest="merge_type",
        action="store",
        default="inner",
        help="Type of merge: {left, right, outer, inner, cross}, default: inner",
    )
    parser.add_argument(
        "-d",
        "--drop-na",
        dest="drop_na",
        action="store_true",
        default=False,
        help="Whether to drop rows with empty/NA values",
    )

    args = parser.parse_args()
    input_files = args.input_files
    output_file = args.output_file
    sep = args.sep
    columns = args.columns
    merge_type = args.merge_type
    drop_na = args.drop_na

    # Check that the input files exist
    for file in input_files:
        if not os.path.exists(file):
            print(f"No such file: {file}", file=ERROR_FILE)
            parser.print_help()

    # Combine the files
    combine_files(input_files, output_file, sep=sep, columns=columns, merge_type=merge_type, drop_na=drop_na)


if __name__ == "__main__":
    main()
