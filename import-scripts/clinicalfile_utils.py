import os
import linecache

DISPLAY_NAME = "DISPLAY_NAME"
DESCRIPTION = "DESCRIPTION"
DATATYPE = "DATATYPE"
ATTRIBUTE_TYPE = "ATTRIBUTE_TYPE"
PRIORITY = "PRIORITY"
METADATA_PREFIX = "#"

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


def write_metadata_headers(metadata_lines, clinical_filename):
    print '\t'.join(metadata_lines[DISPLAY_NAME]).replace('\n', '')
    print '\t'.join(metadata_lines[DESCRIPTION]).replace('\n', '')
    print '\t'.join(metadata_lines[DATATYPE]).replace('\n', '')
    if has_legacy_clinical_metadata_headers(clinical_filename):
        print '\t'.join(metadata_lines[ATTRIBUTE_TYPE]).replace('\n', '')
    print '\t'.join(metadata_lines[PRIORITY]).replace('\n', '')


def write_header_line(line, output_file):
    os.write(output_file, '#')
    os.write(output_file, '\t'.join(line))
    os.write(output_file, '\n')


def write_data(data_file, output_file):
    with open(data_file) as source_file:
        for line in source_file:
            if not line.startswith("#"):
                os.write(output_file, line)


def is_clinical_file(filename):
    return "data_clinical" in filename and filename.endswith(".txt")
