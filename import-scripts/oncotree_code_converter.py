#!/usr/bin/env python3
"""oncotree_code_converter.py

Python 3 port of oncotree_code_converter.py.

Parses a clinical file with an ONCOTREE_CODE column and adds/updates the
CANCER_TYPE and CANCER_TYPE_DETAILED columns in place with values from an
oncotree instance. With --audit nothing is written: stale ONCOTREE_CODE values
(absent from oncotree) and populated CANCER_TYPE / CANCER_TYPE_DETAILED values
that differ from oncotree are reported instead, exit code 1 when drift is found.

Usage:
    python3 oncotree_code_converter.py -c data_clinical_sample.txt [--force] [-o URL] [-v VERSION]
    python3 oncotree_code_converter.py -c data_clinical_sample.txt --audit
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

DEFAULT_ONCOTREE_BASE_URL = "https://oncotree.mskcc.org/"
DEFAULT_ONCOTREE_VERSION = "oncotree_latest_stable"
DEFAULT_FORCE_CANCER_TYPE_FROM_ONCOTREE = False
CANCER_TYPE = "CANCER_TYPE"
CANCER_TYPE_DETAILED = "CANCER_TYPE_DETAILED"
ONCOTREE_CODE = "ONCOTREE_CODE"
SAMPLE_ID = "SAMPLE_ID"
NOT_AVAILABLE_VALUES = ["NA", "N/A", "NOT AVAILABLE"]

samples_that_have_undefined_oncotree_codes = []


def get_header(clinical_filename):
    """Column headers: the first non-'#' line of the file."""
    with open(clinical_filename, "r", encoding="utf-8") as clinical_file:
        for line in clinical_file:
            if not line.startswith("#"):
                return line.rstrip("\r\n").split("\t")
    return []


def get_metadata_rows(clinical_filename):
    """Leading '#' rows of a clinical file (metadata headers), without line endings."""
    rows = []
    with open(clinical_filename, "r", encoding="utf-8") as clinical_file:
        for line in clinical_file:
            if not line.startswith("#"):
                break
            rows.append(line.rstrip("\r\n"))
    return rows


def has_metadata_headers(metadata_rows):
    """4 rows (display name, description, datatype, priority) or the legacy 5 (with attribute types)."""
    return len(metadata_rows) in (4, 5)


def add_metadata_for_attribute(attribute, metadata_rows):
    """Appends metadata for a new STRING attribute to each metadata row, in place."""
    title = attribute.replace("_", " ").title()
    metadata_rows[0] += "\t" + title
    metadata_rows[1] += "\t" + title
    metadata_rows[2] += "\tSTRING"
    if len(metadata_rows) == 5:
        metadata_rows[3] += "\tSAMPLE"
    metadata_rows[-1] += "\t1"


def extract_oncotree_code_mappings_from_oncotree_json(oncotree_json):
    oncotree_code_to_info = {}
    for node in json.loads(oncotree_json):
        if not node.get("code"):
            sys.stderr.write("Encountered oncotree node without oncotree code : %s\n" % (node))
            continue
        cancer_type = node.get("mainType") or "NA"
        cancer_type_detailed = node.get("name") or "NA"
        oncotree_code_to_info[node["code"]] = {CANCER_TYPE: cancer_type, CANCER_TYPE_DETAILED: cancer_type_detailed}
    return oncotree_code_to_info


def get_oncotree_code_mappings(oncotree_tumortype_api_endpoint_url):
    with urllib.request.urlopen(oncotree_tumortype_api_endpoint_url, timeout=60) as response:
        oncotree_raw_response = response.read().decode("utf-8")
    return extract_oncotree_code_mappings_from_oncotree_json(oncotree_raw_response)


def get_oncotree_code_info(oncotree_code, oncotree_code_mappings):
    if oncotree_code not in oncotree_code_mappings:
        return {CANCER_TYPE: "NA", CANCER_TYPE_DETAILED: "NA"}
    return oncotree_code_mappings[oncotree_code]


def format_output_line(fields):
    return "\t".join(fields)


def existing_data_is_not_available(data):
    if not data:
        return True
    data_upper = data.strip().upper()
    return len(data_upper) == 0 or data_upper in NOT_AVAILABLE_VALUES


def process_clinical_file(oncotree_mappings, clinical_filename, force_cancer_type_from_oncotree):
    """Insert cancer type/cancer type detailed in the clinical file, in place."""
    original_header = get_header(clinical_filename)
    if ONCOTREE_CODE not in original_header:
        raise ValueError("%s column not found in %s" % (ONCOTREE_CODE, clinical_filename))
    metadata_rows = get_metadata_rows(clinical_filename)
    if has_metadata_headers(metadata_rows):
        if CANCER_TYPE not in original_header:
            add_metadata_for_attribute(CANCER_TYPE, metadata_rows)
        if CANCER_TYPE_DETAILED not in original_header:
            add_metadata_for_attribute(CANCER_TYPE_DETAILED, metadata_rows)

    output_lines = list(metadata_rows)
    header = []
    first = True
    with open(clinical_filename, "r", encoding="utf-8") as clinical_file:
        for line in clinical_file:
            line = line.rstrip("\r\n")
            if line.startswith("#"):
                continue
            if first:
                first = False
                header = line.split("\t")
                if CANCER_TYPE not in header:
                    header.append(CANCER_TYPE)
                if CANCER_TYPE_DETAILED not in header:
                    header.append(CANCER_TYPE_DETAILED)
                output_lines.append("\t".join(header))
                continue
            data = line.split("\t")
            oncotree_code = data[header.index(ONCOTREE_CODE)]
            if not oncotree_code or oncotree_code not in oncotree_mappings:
                samples_that_have_undefined_oncotree_codes.append(data[header.index(SAMPLE_ID)] if SAMPLE_ID in header else oncotree_code)
            oncotree_code_info = get_oncotree_code_info(oncotree_code, oncotree_mappings)
            for attribute in (CANCER_TYPE, CANCER_TYPE_DETAILED):
                index = header.index(attribute)
                if index < len(data):
                    if force_cancer_type_from_oncotree or existing_data_is_not_available(data[index]):
                        data[index] = oncotree_code_info[attribute]
                else:
                    data.append(oncotree_code_info[attribute])
            output_lines.append(format_output_line(data))

    tmp_filename = clinical_filename + ".tmp"
    with open(tmp_filename, "w", encoding="utf-8") as output_file:
        for line in output_lines:
            output_file.write(line + "\n")
    os.replace(tmp_filename, clinical_filename)


def audit_clinical_file(oncotree_mappings, clinical_filename):
    """Read-only check of a clinical file against oncotree. Returns a dict with
    'stale_codes': {code: sample count} for ONCOTREE_CODE values absent from oncotree,
    'cancer_type_mismatches': {(code, file value, oncotree value): sample count},
    'cancer_type_detailed_mismatches': {(code, file value, oncotree value): sample count}.
    Blank/NA codes and blank/NA cancer type values are not reported."""
    findings = {"stale_codes": {}, "cancer_type_mismatches": {}, "cancer_type_detailed_mismatches": {}}
    header = get_header(clinical_filename)
    if ONCOTREE_CODE not in header:
        raise ValueError("%s column not found in %s" % (ONCOTREE_CODE, clinical_filename))
    oncotree_code_index = header.index(ONCOTREE_CODE)
    attribute_indexes = [
        (header.index(CANCER_TYPE) if CANCER_TYPE in header else None, CANCER_TYPE, "cancer_type_mismatches"),
        (header.index(CANCER_TYPE_DETAILED) if CANCER_TYPE_DETAILED in header else None, CANCER_TYPE_DETAILED, "cancer_type_detailed_mismatches"),
    ]
    with open(clinical_filename, "r", encoding="utf-8") as clinical_file:
        header_seen = False
        for line in clinical_file:
            line = line.rstrip("\r\n")
            if line.startswith("#"):
                continue
            if not header_seen:
                header_seen = True
                continue
            data = line.split("\t")
            if oncotree_code_index >= len(data):
                continue
            oncotree_code = data[oncotree_code_index].strip()
            if existing_data_is_not_available(oncotree_code):
                continue
            if oncotree_code not in oncotree_mappings:
                findings["stale_codes"][oncotree_code] = findings["stale_codes"].get(oncotree_code, 0) + 1
                continue
            oncotree_code_info = oncotree_mappings[oncotree_code]
            for index, attribute, bucket in attribute_indexes:
                if index is None or index >= len(data):
                    continue
                existing_data = data[index].strip()
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


def report_failures_to_match_oncotree_code():
    if samples_that_have_undefined_oncotree_codes:
        sys.stderr.write("WARNING: Could not find an oncotree code match for the following samples:\n")
        sys.stderr.write("         (default value of NA was inserted for CANCER_TYPE and CANCER_TYPE_DETAILED for oncotree code match failures)\n")
        for sample_id in samples_that_have_undefined_oncotree_codes:
            sys.stderr.write("        " + sample_id + "\n")


def construct_oncotree_url(oncotree_base_url, oncotree_version):
    """Check that oncotree_version exists, then construct the tumorTypes API url."""
    oncotree_api_base_url = oncotree_base_url.rstrip("/") + "/api/"
    oncotree_versions_api_url = oncotree_api_base_url + "versions"
    try:
        with urllib.request.urlopen(oncotree_versions_api_url, timeout=60) as response:
            oncotree_version_response = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as err:
        sys.stderr.write("ERROR: failure during attempt to access oncotree through base url " + oncotree_base_url + "\n")
        sys.stderr.write("        failure during access of versions web service (" + oncotree_versions_api_url + ")\n")
        sys.stderr.write("        http status code returned: " + str(err.code) + "\n")
        sys.exit(3)
    found_versions = []
    for version in oncotree_version_response:
        if version["api_identifier"] == oncotree_version:
            return oncotree_api_base_url + "tumorTypes?version=" + oncotree_version
        found_versions.append(version["api_identifier"] + " (" + version["description"] + ")")
    sys.stderr.write("ERROR: oncotree version " + oncotree_version + " was not found in the list of available versions:\n")
    for version in found_versions:
        sys.stderr.write("\t" + version + "\n")
    sys.exit(1)


def exit_with_error_if_file_is_not_accessible(filename, need_write=True):
    if not os.path.exists(filename):
        sys.stderr.write("ERROR: file cannot be found: " + filename + "\n")
        sys.exit(2)
    read_write_error = False
    if not os.access(filename, os.R_OK):
        sys.stderr.write("ERROR: file permissions do not allow reading: " + filename + "\n")
        read_write_error = True
    if need_write and not os.access(filename, os.W_OK):
        sys.stderr.write("ERROR: file permissions do not allow writing: " + filename + "\n")
        read_write_error = True
    if read_write_error:
        sys.exit(2)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[1])
    parser.add_argument("-c", "--clinical-file", dest="clinical_file", required=True, help="Path to the clinical file")
    parser.add_argument("-o", "--oncotree-url", dest="oncotree_base_url", default=DEFAULT_ONCOTREE_BASE_URL, help="The url of the oncotree web application (default is %s)" % (DEFAULT_ONCOTREE_BASE_URL))
    parser.add_argument("-v", "--oncotree-version", dest="oncotree_version", default=DEFAULT_ONCOTREE_VERSION, help="The oncotree version to use (default is %s)" % (DEFAULT_ONCOTREE_VERSION))
    parser.add_argument("-f", "--force", action="store_true", dest="force_cancer_type_from_oncotree", default=DEFAULT_FORCE_CANCER_TYPE_FROM_ONCOTREE, help="When given, all CANCER_TYPE/CANCER_TYPE_DETAILED values in the input file are overwritten based on oncotree code. When not given, only empty or NA values are overwritten.")
    parser.add_argument("-a", "--audit", action="store_true", dest="audit", help="When given, the clinical file is not modified: stale ONCOTREE_CODE values and CANCER_TYPE/CANCER_TYPE_DETAILED values differing from oncotree are reported instead. Exit code is 1 when drift is found.")
    args = parser.parse_args()

    exit_with_error_if_file_is_not_accessible(args.clinical_file, need_write=not args.audit)
    oncotree_url = construct_oncotree_url(args.oncotree_base_url, args.oncotree_version)
    oncotree_mappings = get_oncotree_code_mappings(oncotree_url)
    if args.audit:
        findings = audit_clinical_file(oncotree_mappings, args.clinical_file)
        report_audit_findings(findings, args.clinical_file)
        sys.exit(1 if audit_has_findings(findings) else 0)
    process_clinical_file(oncotree_mappings, args.clinical_file, args.force_cancer_type_from_oncotree)
    report_failures_to_match_oncotree_code()


if __name__ == "__main__":
    main()
