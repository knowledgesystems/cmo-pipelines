#!/usr/bin/env python3

import argparse
from clinicalfile_utils import (
    add_metadata_for_attribute, parse_file, get_all_metadata_lines,
    get_comments, get_header, get_metadata_header_line_order, split_tab_fields,
)
import fileinput
import json
import os
import re
import sys
import urllib.request
import urllib.error

# globals
DEFAULT_ONCOTREE_BASE_URL = 'https://oncotree.mskcc.org/'
DEFAULT_ONCOTREE_VERSION = 'oncotree_latest_stable'
DEFAULT_FORCE_CANCER_TYPE_FROM_ONCOTREE = False
CANCER_TYPE = 'CANCER_TYPE'
CANCER_TYPE_DETAILED = 'CANCER_TYPE_DETAILED'
ONCOTREE_CODE = 'ONCOTREE_CODE'
SAMPLE_ID = 'SAMPLE_ID'

samples_that_have_undefined_oncotree_codes = []

# functions

def audit_clinical_file(oncotree_mappings, clinical_filename):
    """Count retired codes and conflicting populated labels without modifying the file."""
    findings = {"stale_codes": {}, "cancer_type_mismatches": {}, "cancer_type_detailed_mismatches": {}}
    header = get_header(clinical_filename)
    if ONCOTREE_CODE not in header:
        raise ValueError("%s column not found in %s" % (ONCOTREE_CODE, clinical_filename))
    attributes = [
        (CANCER_TYPE, "cancer_type_mismatches"),
        (CANCER_TYPE_DETAILED, "cancer_type_detailed_mismatches"),
    ]
    for data in parse_file(clinical_filename, allow_empty_values=True):
        oncotree_code = data.get(ONCOTREE_CODE, "").strip()
        if existing_data_is_not_available(oncotree_code):
            continue
        if oncotree_code not in oncotree_mappings:
            findings["stale_codes"][oncotree_code] = findings["stale_codes"].get(oncotree_code, 0) + 1
            continue
        oncotree_code_info = oncotree_mappings[oncotree_code]
        for attribute, bucket in attributes:
            existing_data = data.get(attribute, "").strip()
            if existing_data_is_not_available(existing_data):
                continue
            expected = oncotree_code_info[attribute]
            if existing_data != expected:
                key = (oncotree_code, existing_data, expected)
                findings[bucket][key] = findings[bucket].get(key, 0) + 1
    return findings

def audit_has_findings(findings):
    return any(findings[bucket] for bucket in findings)

def report_audit_findings(findings, clinical_filename, out=sys.stdout):
    if not audit_has_findings(findings):
        out.write("%s: no oncotree drift\n" % (clinical_filename))
        return
    out.write("%s: oncotree drift found\n" % (clinical_filename))
    if findings["stale_codes"]:
        out.write("  stale ONCOTREE_CODE values (absent from oncotree):\n")
        for oncotree_code, count in sorted(findings["stale_codes"].items(), key=lambda item: (-item[1], item[0])):
            out.write("    %s\t%d samples\n" % (oncotree_code, count))
    for bucket, attribute in (("cancer_type_mismatches", CANCER_TYPE), ("cancer_type_detailed_mismatches", CANCER_TYPE_DETAILED)):
        if findings[bucket]:
            out.write("  %s out of date (file value -> oncotree value):\n" % (attribute))
            for (oncotree_code, existing_data, expected), count in sorted(findings[bucket].items(), key=lambda item: (-item[1], item[0])):
                out.write("    %s\t%s\t%s\t%d samples\n" % (oncotree_code, existing_data, expected, count))

def extract_oncotree_code_mappings_from_oncotree_json(oncotree_json):
    oncotree_code_to_info = {}
    oncotree_response = json.loads(oncotree_json)
    for node in oncotree_response:
        if not node['code']:
            sys.stderr.write('Encountered oncotree node without oncotree code : ' + node + '\n')
            continue
        oncotree_code = node['code']
        main_type = node['mainType']
        cancer_type = str('NA')
        if main_type:
            cancer_type = str(main_type)
        cancer_type_detailed = str(node['name'])
        if not cancer_type_detailed:
            cancer_type_detailed = str('NA')
        oncotree_code_to_info[oncotree_code] = { CANCER_TYPE : cancer_type, CANCER_TYPE_DETAILED : cancer_type_detailed }
    return oncotree_code_to_info

def get_oncotree_code_mappings(oncotree_tumortype_api_endpoint_url):
    oncotree_raw_response = urllib.request.urlopen(oncotree_tumortype_api_endpoint_url).read()
    return extract_oncotree_code_mappings_from_oncotree_json(oncotree_raw_response)

def get_oncotree_code_info(oncotree_code, oncotree_code_mappings):
    if not oncotree_code in oncotree_code_mappings:
        return { CANCER_TYPE : str('NA'), CANCER_TYPE_DETAILED: str('NA') }
    return oncotree_code_mappings[oncotree_code]

def format_output_line(fields):
    """Return text; Python 3 handles UTF-8 when writing the file."""
    return '\t'.join(fields) if fields else ''

def existing_data_is_not_available(data):
    if not data:
        return True
    data_upper = data.strip().upper()
    if len(data_upper) == 0:
        return True
    if data_upper in ['NA','N/A','NOT AVAILABLE']:
        return True
    return False

def process_clinical_file(oncotree_mappings, clinical_filename, force_cancer_type_from_oncotree):
    """ Insert cancer type/cancer type detailed in the clinical file """
    first = True
    metadata_headers_processed = False
    header = []
    original_header = get_header(clinical_filename)
    if ONCOTREE_CODE not in original_header:
        raise ValueError("%s column not found in %s" % (ONCOTREE_CODE, clinical_filename))
    file_has_metadata_headers = len(get_comments(clinical_filename)) in (4, 5)

    # same logic checking for Cancer Type/ Cancer Type Detailed but applied to metadata headers
    if file_has_metadata_headers:
        metadata_lines = get_all_metadata_lines(clinical_filename)
        metadata_line_order = get_metadata_header_line_order(clinical_filename)
        if CANCER_TYPE not in original_header:
            add_metadata_for_attribute(CANCER_TYPE, metadata_lines)
        if CANCER_TYPE_DETAILED not in original_header:
            add_metadata_for_attribute(CANCER_TYPE_DETAILED, metadata_lines)

    # Python docs: "if the keyword argument inplace=1 is passed to fileinput.input()
    # or to the FileInput constructor, the file is moved to a backup
    # file and standard output is directed to the input file"
    f = fileinput.input(clinical_filename, inplace = 1, encoding="utf-8")
    try:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('#'):
                if file_has_metadata_headers and not metadata_headers_processed:
                    metadata_headers_processed = True
                    for metadata_type in metadata_line_order:
                        print("\t".join(metadata_lines[metadata_type]))
                continue
            if first:
                first = False
                header = split_tab_fields(line)
                if CANCER_TYPE not in header:
                    header.append(CANCER_TYPE)
                if CANCER_TYPE_DETAILED not in header:
                    header.append(CANCER_TYPE_DETAILED)
                print('\t'.join(header))
                continue
            data = split_tab_fields(line)
            oncotree_code = data[header.index(ONCOTREE_CODE)]
            if not oncotree_code or not oncotree_code in oncotree_mappings:
                samples_that_have_undefined_oncotree_codes.append(data[header.index(SAMPLE_ID)])
            oncotree_code_info = get_oncotree_code_info(oncotree_code, oncotree_mappings)
            # Handle the case if CANCER_TYPE or CANCER_TYPE_DETAILED has to be appended to the header.
            # Separate try-except in case one of the fields exists and the other doesn't
            try:
                existing_data = data[header.index(CANCER_TYPE)]
                if force_cancer_type_from_oncotree or existing_data_is_not_available(existing_data):
                    data[header.index(CANCER_TYPE)] = oncotree_code_info[CANCER_TYPE]
            except IndexError:
                data.append(oncotree_code_info[CANCER_TYPE])
            try:
                existing_data = data[header.index(CANCER_TYPE_DETAILED)]
                if force_cancer_type_from_oncotree or existing_data_is_not_available(existing_data):
                    data[header.index(CANCER_TYPE_DETAILED)] = oncotree_code_info[CANCER_TYPE_DETAILED]
            except IndexError:
                data.append(oncotree_code_info[CANCER_TYPE_DETAILED])
            print(format_output_line(data))
    finally:
        f.close()

def report_failures_to_match_oncotree_code():
    if len(samples_that_have_undefined_oncotree_codes) > 0:
        sys.stderr.write('WARNING: Could not find an oncotree code match for the following samples:\n')
        sys.stderr.write('         (default value of NA was inserted for CANCER_TYPE and CANCER_TYPE_DETAILED for oncotree code match failures)\n')
        for sample_id in samples_that_have_undefined_oncotree_codes:
            sys.stderr.write('        ' + sample_id + '\n')

def construct_oncotree_url(oncotree_base_url, oncotree_version):
    """ test that oncotree_version exists, then construct url for web API query """
    oncotree_api_base_url = oncotree_base_url.rstrip('/') + '/api/'
    oncotree_versions_api_url = oncotree_api_base_url + 'versions'
    oncotree_versions_raw_response = ''
    try:
        oncotree_versions_raw_response = urllib.request.urlopen(oncotree_versions_api_url)
    except urllib.error.HTTPError as err:
        #error trying to access oncotree api .. url must be bad
        sys.stderr.write('ERROR: failure during attempt to access oncotree through base url ' + oncotree_base_url + '\n')
        sys.stderr.write('        failure during access of versions web service (' + oncotree_versions_api_url + ')\n')
        sys.stderr.write('        http status code returned: ' + str(err.code) + '\n')
        sys.exit(3)
    oncotree_version_response = json.load(oncotree_versions_raw_response)
    found_versions = []
    for version in oncotree_version_response:
        if version['api_identifier'] == oncotree_version:
            #version exists
            return oncotree_api_base_url + 'tumorTypes?version=' + oncotree_version
        else:
            found_versions.append(version['api_identifier'] + ' (' + version['description'] + ')')
    sys.stderr.write('ERROR: oncotree version ' + oncotree_version + ' was not found in the list of available versions:')
    for version in found_versions:
        sys.stderr.write('\t' + version + '\n')
    sys.exit(1)

def exit_with_error_if_file_is_not_accessible(filename, need_write=True):
    if not os.path.exists(filename):
        sys.stderr.write('ERROR: file cannot be found: ' + filename + '\n')
        sys.exit(2)
    read_write_error = False
    if not os.access(filename, os.R_OK):
        sys.stderr.write('ERROR: file permissions do not allow reading: ' + filename + '\n')
        read_write_error = True
    if need_write and not os.access(filename, os.W_OK):
        sys.stderr.write('ERROR: file permissions do not allow writing: ' + filename + '\n')
        read_write_error = True
    if read_write_error:
        sys.exit(2)

def main():
    """
    Parses a clinical file with a ONCOTREE_CODE column and add/update the CANCER_TYPE and CANCER_TYPE_DETAILED columns inplace
    with values from an oncotree instance.
    """

    parser = argparse.ArgumentParser()
    parser.add_argument('-c', '--clinical-file', action = 'store', dest = 'clinical_file', required = True, help = 'Path to the clinical file')
    parser.add_argument('-o', '--oncotree-url', action = 'store', dest = 'oncotree_base_url', required = False, help = 'The url of the oncotree web application (default is https://oncotree.mskcc.org/)')
    parser.add_argument('-v', '--oncotree-version', action = 'store', dest = 'oncotree_version', required = False, help = 'The oncotree version to use (default is oncotree_latest_stable)')
    parser.add_argument('-f', '--force', action = 'store_true', dest = 'force_cancer_type_from_oncotree', required = False, help = 'When given, all CANCER_TYPE/CANCER_TYPE_DETAILED values in the input file are overwritten based on oncotree code. When not given, only empty or NA values are overwritten.')
    parser.set_defaults(oncotree_base_url = DEFAULT_ONCOTREE_BASE_URL, oncotree_version = DEFAULT_ONCOTREE_VERSION, force_cancer_type_from_oncotree = DEFAULT_FORCE_CANCER_TYPE_FROM_ONCOTREE)
    parser.add_argument("-a", "--audit", action="store_true", help="Report OncoTree drift without changing the file; exit 1 when drift is found")
    args = parser.parse_args()
    clinical_filename = args.clinical_file
    exit_with_error_if_file_is_not_accessible(clinical_filename, need_write=not args.audit)
    oncotree_url = construct_oncotree_url(args.oncotree_base_url, args.oncotree_version)
    oncotree_mappings = get_oncotree_code_mappings(oncotree_url)
    if args.audit:
        findings = audit_clinical_file(oncotree_mappings, clinical_filename)
        report_audit_findings(findings, clinical_filename)
        sys.exit(1 if audit_has_findings(findings) else 0)
    process_clinical_file(oncotree_mappings, clinical_filename, args.force_cancer_type_from_oncotree)
    report_failures_to_match_oncotree_code()
    sys.exit(0)
if __name__ == '__main__':
    main()
