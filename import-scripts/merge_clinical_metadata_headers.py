#!/usr/bin/env python3

import sys
#import optparse
#import os
#import shutil
#import re
#import csv

def usage():
    sys.stdout.write('usage: merge_clinical_metadata_headers.py input_filepath output_filepath [metadata_header_containing_filepath ...]\n')

def validate_args(argv):
    if len(argv) < 4:
        usage()
        sys.exit(1)

def verify_column_length(line, expected_len):
    if len(line.rstrip('\r\n').split('\t')) != expected_len:
        raise Exception(f'expected {expected_len} columns but found otherwise in line "{line}"')

def read_metadata_for_headers_in_file(header_to_metadata_map, filepath):
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
    header_column_list = header_column_line.rstrip('\r\n').split('\t')
    metadata_value_list_list = []
    for metadata_header_line in metadata_header_lines:
        verify_column_length(metadata_header_line, len(header_column_list))
        metadata_value_list = metadata_header_line.rstrip('\r\n').split('\t')
        metadata_value_list_list.append(metadata_value_list)
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
    # we have now populated the metadata values for any unencountered header column into the header_to_metadata_map    

def read_metadata_for_headers_in_files(metadata_header_containing_filepath_list):
    header_to_metadata_map = {}
    for filepath in metadata_header_containing_filepath_list:
        read_metadata_for_headers_in_file(header_to_metadata_map, filepath)
    return header_to_metadata_map

def read_header_columns_from_file(filepath):
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

def vefiry_all_columns_have_defined_metadata(header_column_list, header_to_metadata_header_map):
    for header_column in header_column_list:
        if header_column not in header_to_metadata_header_map:
            sys.stderr.write(f'error : header column "{header_column}" in input_filepath has no defined metadata values in any of the metadata_header_containing files provided\n')

def write_metadata_headers(output_file, header_column_list, header_to_metadata_header_map):
    verify_all_metadata_headers_have_the_same_length(header_to_metadata_header_map)
    vefiry_all_columns_have_defined_metadata(header_column_list, header_to_metadata_header_map)
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
    input_file = open(input_filepath, "r")
    output_file.write(input_file.read()) # transmit the entire input file into the output file
    input_file.close()

def write_output_file(output_filepath, input_filepath, header_column_list, header_to_metadata_header_map):
    if len(header_column_list) == 0:
        sys.stderr.write(f'error : no columns detected in input_filepath\n')
        sys.exit(1)
    output_file = open(output_filepath, "w")
    write_metadata_headers(output_file, header_column_list, header_to_metadata_header_map)
    write_header_columns_and_data(output_file, input_filepath)
    output_file.close()

def main():
    validate_args(sys.argv)
    input_filepath = sys.argv[1]
    output_filepath = sys.argv[2]
    metadata_header_containing_filepath_list = sys.argv[3:]
    header_to_metadata_header_map = read_metadata_for_headers_in_files(metadata_header_containing_filepath_list)
    header_column_list = read_header_columns_from_file(input_filepath)
    write_output_file(output_filepath, input_filepath, header_column_list, header_to_metadata_header_map)

if __name__ == '__main__':
    main()
