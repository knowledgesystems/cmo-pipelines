import os
import linecache

# returns header as list of attributes
def get_header(file):
    header = []
    with open(file, "r") as header_source:
        for line in header_source:
            if not line.startswith("#"):
                header = line.rstrip().split('\t')
                break
    return header

# old format is defined as a file containing 5 header lines (PATIENT and SAMPLE attributes in same file)
def is_old_format(file):
    return all([linecache.getline(file, header_line).startswith("#") for header_line in range(1,6)])

# get existing priority mapping in a given file
def get_priority_mapping(file):
    priority_mapping = {}
    if is_old_format(file):
        attributes = linecache.getline(file, 6).rstrip().split('\t')
        priorities = linecache.getline(file, 5).rstrip().replace("#", "").split('\t')
    else:
        attributes = linecache.getline(file, 5).rstrip().split('\t')
        priorities = linecache.getline(file, 4).rstrip().replace("#", "").split('\t')
    for i in range(len(attributes)):
        priority_mapping[attributes[i]] = priorities[i]
    return priority_mapping

def write_header_line(line, output_file):
    os.write(output_file, '#')
    os.write(output_file, '\t'.join(line))
    os.write(output_file, '\n')

def write_data(file, output_file):
    with open(file) as source_file:
        for line in source_file:
            if not line.startswith("#"):
                os.write(output_file, line)
