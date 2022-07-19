#!/usr/bin/env python3

import json
import os
import sys

def usage(status):
    sys.stderr.write("usage : %s with no arguments reads from stdin and writes to stdout\n        or : \n        %s path_to_input_json_file path_to_output_text_file" % (sys.argv[0], sys.argv[0]))
    sys.exit(status)

def can_write(output_filepath):
    with open(output_filepath, "w+") as f:
        f.close()
        return True
    return False

def parse_args():
    if len(sys.argv) == 1:
        return None, None
    if len(sys.argv) == 3:
        input_filepath = sys.argv[1]
        if not os.path.exists(input_filepath):
            sys.stderr.write("error : input file %s does not exist" % (input_filepath))
            sys.exit(1)
        output_filepath = sys.argv[2]
        if not os.path.exists(os.path.dirname(output_filepath)):
            sys.stderr.write("error : output directory for %s does not exist" % (output_filepath))
            sys.exit(1)
        if not can_write(output_filepath):
            sys.stderr.write("error : cannot write to output path %s" % (output_filepath))
            sys.exit(1)
        return input_filepath, output_filepath

def event_missing_dp_ad(event):
    required_fields=["normal_ad", "normal_dp", "tumor_ad", "tumor_dp"]
    for required_field in required_fields:
        if not required_field in event:
            return True
        if event[required_field] is None:
            return True
    return False

def events_missing_dp_ad(section_events):
    bad_events = []
    for event in section_events:
        if event_missing_dp_ad(event):
            bad_events.append(event)
    return bad_events

def find_samples_with_bad_events(input_filehandle):
    samples_with_bad_events = set()
    data_sections_requiring_dp_ad = set(("snp-indel-exonic", "snp-indel-exonic-np", "snp-indel-silent", "snp-indel-silent-np"))
    data_sections_not_requiring_dp_ad = set(("cnv-arm-variants", "cnv-intragenic-variants", "cnv-variants", "meta-data", "sv-variants"))
    file_data_lines = input_filehandle.read()
    file_data_json = json.loads(file_data_lines)
    if "results" in file_data_json:
        results = file_data_json["results"]
        for sample_id in results:
            sample_results = results[sample_id]
            for data_section in sample_results:
                if data_section in data_sections_not_requiring_dp_ad:
                    continue
                if data_section in data_sections_requiring_dp_ad:
                    bad_events = events_missing_dp_ad(sample_results[data_section])
                    if len(bad_events) > 0:
                        samples_with_bad_events.add(sample_id)
                        sys.stderr.write("adding sample %s to consume list because of these malformed %s events:\n%s\n" % (sample_id, data_section, bad_events))
                    continue
                sys.stderr.write("warning : encountered data section %s -- of which it is not known whether the required fields for snp data are required in this section")
                continue
    return samples_with_bad_events

def main():
    (input_filepath, output_filepath) = parse_args()
    input_filehandle = sys.stdin
    if input_filepath:
        input_filehandle = open(input_filepath, "r+")
    output_filehandle = sys.stdout
    if output_filepath:
        output_filehandle = open(output_filepath, "w+")
    samples_to_consume = find_samples_with_bad_events(input_filehandle)
    for sample_id in sorted(samples_to_consume):
        output_filehandle.write("%s\n" % (sample_id))

if __name__ == "__main__":
    main()
