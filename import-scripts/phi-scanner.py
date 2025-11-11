# Scans a JSON file for a set of attributes known to contain PHI.
# If any attributes are found, outputs to stderr a count of the instances
# of each attribute found and then exits with a non-zero exit status.
# Attributes are defined in an input file.
# June 5, 2018
# Author: Manda Wilson

import argparse
import os.path
import sys
import json

# from https://github.com/russellballestrini/nested-lookup/blob/master/nested_lookup/nested_lookup.py
# modified for case insensitivity
def nested_lookup(key, document, wild=False, with_keys=False):
    """Lookup a key in a nested document, return a list of values"""
    if with_keys:
        d = defaultdict(list)
        for k, v in _nested_lookup(key, document, wild=wild, with_keys=with_keys):
            d[k].append(v)
        return d
    return list(_nested_lookup(key, document, wild=wild, with_keys=with_keys))

def _nested_lookup(key, document, wild=False, with_keys=False):
    """Lookup a key in a nested document, yield a value"""
    if isinstance(document, list):
        for d in document:
            for result in _nested_lookup(key, d, wild=wild, with_keys=with_keys):
                yield result

    if isinstance(document, dict):
        for k, v in document.iteritems():
            if key.lower() == k.lower() or (wild and key.lower() in k.lower()):
                if with_keys:
                    yield k, v
                else:
                    yield v
            elif isinstance(v, dict):
                for result in _nested_lookup(key, v, wild=wild, with_keys=with_keys):
                    yield result
            elif isinstance(v, list):
                for d in v:
                    for result in _nested_lookup(key, d, wild=wild, with_keys=with_keys):
                        yield result

def scan(attributes_filename, json_filename):
    attributes = []
    with open(attributes_filename) as attributes_file:
        attributes = [line.rstrip('\n') for line in attributes_file]

    data = {}
    with open(json_filename, 'r') as json_file:
        data = json.load(json_file)

    errors = []
    for attribute in attributes:
        matches = nested_lookup(attribute, data)
        if matches:
            errors.append("ERROR: found '%d' instances of attribute '%s' in '%s'" % (len(matches), attribute, json_filename))

    if errors:
        for error in errors:
            print >>sys.stderr, error
        sys.exit(1)

def exit_with_error_if_file_is_not_accessible(filename):
    if not os.path.exists(filename):
        sys.stderr.write('ERROR: file cannot be found: ' + filename + '\n')
        sys.exit(2)
    read_write_error = False
    if not os.access(filename, os.R_OK):
        sys.stderr.write('ERROR: file permissions do not allow reading: ' + filename + '\n')
        read_write_error = True
    if not os.access(filename, os.W_OK):
        sys.stderr.write('ERROR: file permissions do not allow writing: ' + filename + '\n')
        read_write_error = True
    if read_write_error:
        sys.exit(2)

def main():
    """
    Scans a JSON file for a set of predefined PHI attributes.
    """

    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--attribute-file', action = 'store', dest = 'attributes_filename', required = True, help = 'Path to the PHI attribute definition file')
    parser.add_argument('-j', '--json-file', action = 'store', dest = 'json_filename', required = True, help = 'Path to the JSON file to be scanned')

    args = parser.parse_args()
    attributes_filename = args.attributes_filename
    json_filename = args.json_filename
    exit_with_error_if_file_is_not_accessible(attributes_filename)
    exit_with_error_if_file_is_not_accessible(json_filename)
    scan(attributes_filename, json_filename)

if __name__ == '__main__':
    main()
