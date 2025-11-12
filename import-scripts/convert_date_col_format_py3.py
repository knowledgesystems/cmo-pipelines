#!/usr/bin/env python3

""" convert_date_col_format_py3.py
This script reads in a column containing a date value of a given format from a given file
and rewrites it as a different format.
Usage:
    python3 convert_date_col_format_py3.py --input-file $INPUT_FILE <...> --output-file $OUTPUT_FILE
Example:
    python3 convert_date_col_format_py3.py --input-file cvr/seq_date.txt --output-file seq_date_formatted.txt
"""

import sys
import argparse
import os
import pandas as pd

from combine_files_py3 import write_tsv

ERROR_FILE = sys.stderr


def convert_date_format(input_file, output_file, column, input_date_format, output_date_format, sep="\t"):
    df = pd.read_table(
        input_file,
        sep=sep,
        comment="#",
        float_precision="round_trip",
        na_filter=False,
        low_memory=False,
    )

    # Read in current date format
    # Errors in conversion will be ignored
    df[column] = pd.to_datetime(df[column], format=input_date_format, errors='coerce')

    # Change date format
    df[column] = df[column].dt.strftime(output_date_format)

    # Write out new file
    write_tsv(df, output_file)


def main():
    parser = argparse.ArgumentParser(prog="convert_date_col_format_py3.py")
    parser.add_argument(
        "-i",
        "--input-file",
        dest="input_file",
        action="store",
        required=True,
        help="input filepath",
    )
    parser.add_argument(
        "-o",
        "--output-file",
        dest="output_file",
        action="store",
        required=True,
        help="output filepath",
    )
    parser.add_argument(
        "-c",
        "--column",
        dest="column",
        action="store",
        required=True,
        help="date column to transform",
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
        "--input-date-format",
        dest="input_date_format",
        action="store",
        # Format from seq date file, ex: Mon, 12 Feb 2024 19:21:03 GMT
        default="%a, %d %b %Y %H:%M:%S %Z",
        help="Input date format string",
    )
    parser.add_argument(
        "--output-date-format",
        dest="output_date_format",
        action="store",
        # Ex: 2024-02-12
        default="%Y-%m-%d",
        help="Output date format string",
    )

    args = parser.parse_args()
    input_file = args.input_file
    output_file = args.output_file
    column = args.column
    input_date_format = args.input_date_format
    output_date_format = args.output_date_format
    sep = args.sep

    # Check that the input file exists
    if not os.path.exists(input_file):
        print(f"No such file: {input_file}", file=ERROR_FILE)
        parser.print_help()

    # Combine the files
    convert_date_format(input_file, output_file, column, input_date_format, output_date_format, sep)


if __name__ == "__main__":
    main()
