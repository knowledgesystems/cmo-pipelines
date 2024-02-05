#!/usr/bin/env python3

"""cbioportal clinical data file metadata header merge/transfer program

This program reads a set of clinical data files in order to construct
a dictionary of metadata headers for any present clinical attribute
columns. It then will read an input file which is missing the metadata
headers, will attach the appropriate metadata headers from the
constructed dictionary, and output the clinical data file with attached
metadata.

Args are positional.
Arg 1 is a filepath to the main input file. This file should be a file
    in the cbioportal file format for clinical attribute data
    (such as data_clinical_sample.txt, or data_clinical_patient.txt),
    with column name headers but missing metadata headers.
Arg 2 is a filepath to which the output should be written. The written
    file will have the same contents as the main input file, with
    attached metadata headers added.
Arg 3,4,... are a list of filepaths to any number of cbioportal clinical
    files which do have attached metadata headers, collectively
    covering all of the attribute headers present in the input file.
    These files are parsed by this program to construct a dictionary
    of clinical attribute metadata, which will be used to produce
    the attached metadata in the output file.

All files must be tab-delimited and rectangular (each line contains
the same number of columns as other lines). This program will properly
handle processing of files of a single type. There are three types of
clinical attribute files : patient-centric (data_clinical_patient.txt),
sample-centric (data_clinical_sample.txt) or combination (data_clinical
.txt). The first two types have 4 lines of attached metadata headers,
and the third has 5. Do not mix file types when running this program.

The main use case for this program is for re-attaching the clinical
attribute metadata headers when using the merge.py script to merge
together clinical data from multiple studies. The output of merge.py
will be lacking clinical attribute metadata lines, but if all studies
which were merged contained clinical attribute metadata and used the
same types of files, this script will re-attach the metadata attributes
present in the source studies.

If there is ambiguity (such as two studies having differing metadata
for the same clinical attribute), whichever study comes first in the
list of supplied metadata-containing filepaths (arg 3,4,...) will be
determative when constructing the metadata dictionary.

------------------------------------------------------------------------
"""

import sys

def print_usage():
    sys.stdout.write('usage: merge_clinical_metadata_headers_py3.py input_filepath output_filepath [metadata_header_containing_filepath ...]\n')

def validate_args(argv):
    # exit if argument count has no provided metadata_header_containing_filepath
    if len(argv) < 4:
        print_usage()
        sys.exit(1)

def verify_column_length(line, expected_len):
    # exit if a line does not have the expected number of columns
    if len(line.rstrip('\r\n').split('\t')) != expected_len:
        raise Exception(f'expected {expected_len} columns but found otherwise in line "{line}"')

def read_metadata_for_headers_in_file(header_to_metadata_map, filepath):
    """adds metadata for each attribute in filepath into map

    filepath is opened and headers are read. For each column, if the
    column header is not present yet in header_to_metadata_map, add
    into the map a list of metadata values for the column header.
    """

    # get the metadata and column headers as lines
    input_file = open(filepath, 'r')
    metadata_header_lines = []
    header_column_line = None
    while not header_column_line:
        next_line = input_file.readline()
        if not next_line:
            intput_file.close()
            sys.stderr.write(f'error : could not find header columns in metadata header containing file "{filepath}"\n')
            sys.exit(1)
        if next_line[0:1] != '#':
            header_column_line = next_line
        else:
            metadata_header_lines.append(next_line[1:]) # strip off the comment character
    input_file.close()
    # break lines by tab into columns, and collect all metadata into a list of lists (by line)
    header_column_list = header_column_line.rstrip('\r\n').split('\t')
    metadata_value_list_list = []
    for metadata_header_line in metadata_header_lines:
        verify_column_length(metadata_header_line, len(header_column_list))
        metadata_value_list = metadata_header_line.rstrip('\r\n').split('\t')
        metadata_value_list_list.append(metadata_value_list)
    # combine metadata by column and add to dictionary (header_to_metadata_map)
    column_index = 0
    for header_column in header_column_list:
        if header_column in header_to_metadata_map:
            column_index = column_index + 1
            continue # if header column metadata is already defined (from previous file) skip
        metadata_list_for_column = []
        for metadata_value_list in metadata_value_list_list:
            metadata_list_for_column.append(metadata_value_list[column_index])
        header_to_metadata_map[header_column] = metadata_list_for_column
        column_index = column_index + 1

def read_metadata_for_headers_in_files(metadata_header_containing_filepath_list):
    """construct and return metadata dictionary

    iterate through all metadata_header_containing_filepath files,
    adding metadata to dictionary (header_to_metadata_map)
    """

    header_to_metadata_map = {}
    for filepath in metadata_header_containing_filepath_list:
        read_metadata_for_headers_in_file(header_to_metadata_map, filepath)
    return header_to_metadata_map

def read_header_columns_from_file(filepath):
    """find and return the list of headers from a file

    skip any metadata and return a list of attribute column names
    """

    input_file = open(filepath, 'r')
    header_column_line = None
    while not header_column_line:
        next_line = input_file.readline()
        if not next_line:
            intput_file.close()
            sys.stderr.write(f'error : could not find header columns in input file "{filepath}"\n')
            sys.exit(1)
        if next_line[0:1] != '#':
            header_column_line = next_line
    input_file.close()
    header_column_list = header_column_line.rstrip('\r\n').split('\t')
    return header_column_list

def verify_all_metadata_headers_have_the_same_length(header_to_metadata_header_map):
    """ check that the dictionary has uniform metadata linecount

    insure that user did not mix 4-line and 5-line
    metadata_header_containing files when constructing the dictionary.
    """

    metadata_header_length = None
    reference_column_header = None
    for column_header in header_to_metadata_header_map:
        if not metadata_header_length:
            reference_column_header = column_header
            metadata_header_length = len(header_to_metadata_header_map[column_header])
        else:
            if metadata_header_length != len(header_to_metadata_header_map[column_header]):
                sys.stderr.write(f'error : after encountering column {reference_column_header} with {metadata_header_length} metadata header values, column {column_header} was encountered with a different number\n')
                sys.stderr.write(str(header_to_metadata_header_map[column_header]))
                sys.stderr.write('\n')
                sys.exit(1)

def verify_all_columns_have_defined_metadata(header_column_list, header_to_metadata_header_map):
    #check that each header column is in the constructed dictionary
    for header_column in header_column_list:
        if header_column not in header_to_metadata_header_map:
            sys.stderr.write(f'error : header column "{header_column}" in input_filepath has no defined metadata values in any of the metadata_header_containing files provided\n')

def write_metadata_headers(output_file, header_column_list, header_to_metadata_header_map):
    """write the metadata lines for header columns to ouput_file

    construct each metadata header line by iterating through
    header_column_list and looking up the appropriate metadata value
    from the dictionary. Also insure that the dictionary has uniform
    length (metadata header linecount), and use the dictionary to
    determine how many header lines to output. Insure no column is
    missing metadata.
    """

    verify_all_metadata_headers_have_the_same_length(header_to_metadata_header_map)
    verify_all_columns_have_defined_metadata(header_column_list, header_to_metadata_header_map)
    number_of_metadata_header_lines = len(header_to_metadata_header_map[header_column_list[0]])
    for line_number in range(0, number_of_metadata_header_lines):
        metadata_value_list = []
        for header_column in header_column_list:
            metadata_header_lines_for_column = header_to_metadata_header_map[header_column]
            metadata_value_list.append(metadata_header_lines_for_column[line_number])
        output_file.write("#")
        output_file.write("\t".join(metadata_value_list))
        output_file.write("\n")

def write_header_columns_and_data(output_file, input_filepath):
    """copy the contents of the input file to the output file

    no metadata is expected in the inputfile, so the input file
    should contain only the header columns and the data lines.
    """

    input_file = open(input_filepath, "r")
    output_file.write(input_file.read()) # transmit the entire input file into the output file
    input_file.close()

def write_output_file(output_filepath, input_filepath, header_column_list, header_to_metadata_header_map):
    """output the constructed metadata lines, then the input file

    insure that at least one column exists (fail on empty files)
    """

    if len(header_column_list) == 0:
        sys.stderr.write(f'error : no columns detected in input_filepath\n')
        sys.exit(1)
    output_file = open(output_filepath, "w")
    write_metadata_headers(output_file, header_column_list, header_to_metadata_header_map)
    write_header_columns_and_data(output_file, input_filepath)
    output_file.close()

def main():
    """handle arguments, construct dictionary, produce output

    construct the dictionary from the metadata_header_containing_files,
    obtain the list of header columns from the input file, and write
    output.
    """

    validate_args(sys.argv)
    input_filepath = sys.argv[1]
    output_filepath = sys.argv[2]
    metadata_header_containing_filepath_list = sys.argv[3:]
    header_to_metadata_header_map = read_metadata_for_headers_in_files(metadata_header_containing_filepath_list)
    header_column_list = read_header_columns_from_file(input_filepath)
    write_output_file(output_filepath, input_filepath, header_column_list, header_to_metadata_header_map)

if __name__ == '__main__':
    main()
