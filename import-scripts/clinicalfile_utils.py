import os
import linecache

# returns header as list of attributes
def get_header(file):
    header_source = open(file, "r")
    for line in header_source:
        if not line.startswith("#"):
            header = line.rstrip().split('\t')
            break
    header_source.close()
    return header

# get existing priority mapping in a given file
def get_priority_mapping(file):
    priority_mapping = {}
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
