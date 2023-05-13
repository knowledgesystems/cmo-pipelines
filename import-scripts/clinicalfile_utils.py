import linecache
import os
import re

DISPLAY_NAME = "DISPLAY_NAME"
DESCRIPTION = "DESCRIPTION"
DATATYPE = "DATATYPE"
ATTRIBUTE_TYPE = "ATTRIBUTE_TYPE"
PRIORITY = "PRIORITY"
METADATA_PREFIX = "#"

def is_clinical_file(filename):
    return (re.match('data_clinical[_a-z]*.txt', os.path.basename(filename)) != None)

def parse_file(data_file, allow_empty_values):
    """
        Returns a list of dictionaries, each dictionary representing a row in the file
        where keys are the column headers.

        If 'allow_empty_values' is True then function will not skip rows that contain empty values.

        If 'False' then rows where there are empty values are skipped.
    """
    header = get_header(data_file)
    records = []
    with open(data_file, 'r') as f:
        header_processed = False
        for line in f.readlines():
            if line.startswith(METADATA_PREFIX):
                continue
            if not header_processed:
                header_processed = True
                continue
            record = dict(zip(header, line.rstrip("\n").split("\t")))
            if allow_empty_values or (not allow_empty_values and all([value for value in record.values()])):
                records.append(record)
    return records

def get_header(data_file):
    """
        Returns header from file as a tab-delimited list of columns.
    """
    header = []
    with open(data_file, "r") as header_source:
        for line in header_source:
            if not line.startswith("#"):
                header = line.rstrip().split('\t')
                break
    return header

def get_comments(data_file):
    """
        Returns comments from file.
    """
    comments = []
    with open(data_file, "rU") as data_reader:
        for line in data_reader:
            if line.startswith("#"):
                comments.append(line.rstrip("\n"))
            else:
                break
    return comments

# old format is defined as a file containing 5 header lines (PATIENT and SAMPLE attributes in same file)
def has_legacy_clinical_metadata_headers(clinical_file):
    """
        Determines whether clinical file contains metadata header lines in legacy format.

        Legacy format contains 5 metadata header lines:
            1. display names
            2. descriptions
            3. datatypes
            4. attribute types
            5. priorities
    """
    metadata_headers = []
    with open(clinical_file, 'rU') as f:
        for line in f.readlines():
            if line.startswith(METADATA_PREFIX):
                metadata_headers.append(line)
                continue
            break
    return (len(metadata_headers) == 5)

def get_metadata_mapping(clinical_file, attribute_line):
    metadata_mapping = {}
    metadata = linecache.getline(clinical_file, attribute_line).rstrip().replace("#", "").split('\t')
    attributes = get_header(clinical_file)
    for i in range(len(attributes)):
        metadata_mapping[attributes[i]] = metadata[i]
    return metadata_mapping

def get_display_name_mapping(clinical_file):
    return get_metadata_mapping(clinical_file, 1)

def get_description_mapping(clinical_file):
    return get_metadata_mapping(clinical_file, 2)

def get_datatype_mapping(clinical_file):
    return get_metadata_mapping(clinical_file, 3)

def get_priority_mapping(clinical_file):
    if has_legacy_clinical_metadata_headers(clinical_file):
        return get_metadata_mapping(clinical_file, 5)
    else:
        return get_metadata_mapping(clinical_file, 4)

def get_attribute_type_mapping(clinical_file):
    if has_legacy_clinical_metadata_headers(clinical_file):
        return get_metadata_mapping(clinical_file, 4)
    else:
        attribute_type_mapping = {}
        attributes = get_header(clinical_file)
        for attribute in attributes:
            attribute_type_mapping[attribute] = "NA"
        return attribute_type_mapping

def has_metadata_headers(clinical_file):
    return all([linecache.getline(clinical_file, header_line).startswith("#") for header_line in range(1, 5)])

def get_display_name_line(clinical_file):
    return linecache.getline(clinical_file, 1).rstrip().split('\t')

def get_description_line(clinical_file):
    return linecache.getline(clinical_file, 2).rstrip().split('\t')

def get_datatype_line(clinical_file):
    return linecache.getline(clinical_file, 3).rstrip().split('\t')

def get_priority_line(clinical_file):
    if has_legacy_clinical_metadata_headers(clinical_file):
        return linecache.getline(clinical_file, 5).rstrip().split('\t')
    else:
        return linecache.getline(clinical_file, 4).rstrip().split('\t')

def get_attribute_type_line(clinical_file):
    if has_legacy_clinical_metadata_headers(clinical_file):
        return linecache.getline(clinical_file, 4).rstrip().split('\t')
    else:
        return []

def add_metadata_for_attribute(attribute, all_metadata_lines):
    all_metadata_lines[DISPLAY_NAME].append(attribute.replace("_", " ").title())
    all_metadata_lines[DESCRIPTION].append(attribute.replace("_", " ").title())
    all_metadata_lines[DATATYPE].append("STRING")
    all_metadata_lines[ATTRIBUTE_TYPE].append("SAMPLE")
    all_metadata_lines[PRIORITY].append("1")

def get_all_metadata_lines(clinical_file):
    all_metadata_lines = {DISPLAY_NAME: get_display_name_line(clinical_file),
                          DESCRIPTION: get_description_line(clinical_file),
                          DATATYPE: get_datatype_line(clinical_file),
                          ATTRIBUTE_TYPE: get_attribute_type_line(clinical_file),
                          PRIORITY: get_priority_line(clinical_file)}
    return all_metadata_lines

def get_all_metadata_mappings(clinical_file):
    all_metadata_mapping = {DISPLAY_NAME: get_display_name_mapping(clinical_file),
                            DESCRIPTION: get_description_mapping(clinical_file),
                            DATATYPE: get_datatype_mapping(clinical_file),
                            ATTRIBUTE_TYPE: get_attribute_type_mapping(clinical_file),
                            PRIORITY: get_priority_mapping(clinical_file)}
    return all_metadata_mapping

# return list representing order metadata header lines to write
def get_metadata_header_line_order(clinical_file):
    to_return = [DISPLAY_NAME,
                 DESCRIPTION,
                 DATATYPE,
                 ATTRIBUTE_TYPE,
                 PRIORITY]
    if not has_legacy_clinical_metadata_headers(clinical_file):
        to_return.remove(ATTRIBUTE_TYPE)
    return to_return

def get_ordered_metadata_and_add_new_attribute(clinical_file, new_attribute):
    """
        Returns ordered clinical metadata with new attribute metadata added as well.
    """
    ## TODO: change to hit CDD(?)
    all_metadata_lines = get_all_metadata_lines(clinical_file)
    add_metadata_for_attribute(new_attribute, all_metadata_lines)

    ordered_metadata_lines = []
    for metadata_header_type in get_metadata_header_line_order(clinical_file):
        ordered_metadata_lines.append('\t'.join(all_metadata_lines[metadata_header_type]))
    return ordered_metadata_lines

def write_metadata_headers(metadata_lines, clinical_filename):
    print('\t'.join(metadata_lines[DISPLAY_NAME]).replace('\n', ''))
    print('\t'.join(metadata_lines[DESCRIPTION]).replace('\n', ''))
    print('\t'.join(metadata_lines[DATATYPE]).replace('\n', ''))
    if has_legacy_clinical_metadata_headers(clinical_filename):
        print('\t'.join(metadata_lines[ATTRIBUTE_TYPE]).replace('\n', ''))
    print('\t'.join(metadata_lines[PRIORITY]).replace('\n', ''))

def write_header_line(line, output_file):
    os.write(output_file, '#')
    os.write(output_file, '\t'.join(line))
    os.write(output_file, '\n')

def write_data(data_file, output_file):
    with open(data_file) as source_file:
        for line in source_file:
            if not line.startswith("#"):
                os.write(output_file, line)

def write_standardized_columns(clinical_filename, output_file):
    """
        Rewrites a file (assumed clinical/timeline) and replaces
        all NA placeholders/blanks with 'NA'. Does not make same adjustment
        to the SAMPLE_ID column (to avoid creating an actual sample tagged 'NA')
    """
    header = get_header(clinical_filename)

    try:
        sample_id_index = header.index('SAMPLE_ID')
    except ValueError:
        sample_id_index = -1
    
    with open(clinical_filename) as clinical_file:
        for line in clinical_file:
            line = line.rstrip('\n')
            row = line.split('\t')
            to_write = []
            for index, field in enumerate(row):
                if index == sample_id_index:
                    to_write.append(field)
                    continue
                to_write.append(standardize_clinical_datum(field))
            output_file.write('\t'.join(to_write))
            output_file.write('\n')
    
def write_and_exclude_columns(clinical_filename, exclude_column_names, output_file):
    header = get_header(clinical_filename)
    columns_to_remove_indexes = get_indexes_for_columns(header, exclude_column_names)

    with open(clinical_filename) as clinical_file:
        for line in clinical_file:
            line = line.rstrip('\n')
            row = line.split('\t')
            output_file.write('\t'.join([field for index, field in enumerate(row) if index not in columns_to_remove_indexes]))
            output_file.write('\n')

def duplicate_existing_attribute_to_new_attribute(clinical_file, existing_attribute_name, new_attribute_name):
    """
        Duplicates a specified attribute in clinical file (including values) under a new specified attribute name.
        The new (duplicated) attribute is added as the last column in the file.
    """

    to_write = []
    header = get_header(clinical_file)
    header_processed = False

    if existing_attribute_name in header:
        if has_metadata_headers(clinical_file):
            to_write = get_ordered_metadata_and_add_new_attribute(clinical_file, new_attribute_name)
        header = get_header(clinical_file)
        existing_attribute_index = header.index(existing_attribute_name)
        with open(clinical_file, "r") as f:
            for line in f.readlines():
                # skip metadata headers - already processed
                if line.startswith("#"):
                    continue
                data = line.rstrip("\n").split('\t')
                # add new attribute/column to the end of header
                if not header_processed:
                    data.append(new_attribute_name)
                    header_processed = True
                # add value from existing attribute to end of data (lines up with new attribute)
                else:
                    data.append(data[existing_attribute_index])
                to_write.append('\t'.join(data))
        write_data_list_to_file(clinical_file, to_write)

def get_value_set_for_clinical_attribute(clinical_file, clinical_attribute):
    """
        Returns a set containing all values found in a specific column of the clinical file.
        Values are taken column matching 'clinical_attribute' argument (case-specific)
        e.g 'get_value_set_for_clinical_attribute("clinical_file", "SAMPLE_ID")'
            will return a set with all sample ids in "clinical_file"
    """
    value_set = set()
    if clinical_attribute not in get_header(clinical_file):
        raise KeyError("%s is not an attribute in %s" % (clinical_attribute, clinical_file))
    for row in parse_file(clinical_file, True):
        value_set.add(row[clinical_attribute])
    return value_set

def standardize_clinical_datum(val):
    """
        Standardizes any NA/blank placeholder value ('', 'NA', 'N/A', None) to 'NA'
    """
    try:
        vfixed = val.strip()
    except AttributeError:
        vfixed = 'NA'
    if vfixed in ['', 'NA', 'N/A', None]:
        return 'NA'
    return vfixed

def write_data_list_to_file(filename, data_list):
    """
        Writes data to file where 'data_list' is a
        list of formatted data to write to given file.
    """
    with open(filename, "w") as f:
            f.write('\n'.join(data_list) + "\n")

def get_index_for_column(header, column_name):
    """
        Get index for an optional column (might be missing)
        If column is not present, return -1 (similar to find() function)
    """
    column_index = -1
    try:
        column_index = header.index(column_name)
    except ValueError:
        pass
    return column_index

def get_indexes_for_columns(header, column_names):
    """
        Given a list of column names, return a list of indexes.
        If one or more columns is not present, throws ValueError.
    """
    return [header.index(c) for c in column_names]
