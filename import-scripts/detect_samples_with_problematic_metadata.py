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

def sample_missing_required_metadata(metadata, problematic_metadata):
    required_fields=["gene-panel"]
    for required_field in required_fields:
        if not required_field in metadata:
            return True
        if metadata[required_field] is None:
            problematic_metadata["gene-panel"] = None
            return True
        if required_field == "gene-panel" and metadata[required_field].casefold() in {"unknown"}:
            problematic_metadata["gene-panel"] = metadata[required_field]
            return True
    return False

def find_samples_with_problematic_metadata(input_filehandle):
    samples_with_problematic_metadata = set()
    file_data_lines = input_filehandle.read()
    file_data_json = json.loads(file_data_lines)
    if "results" in file_data_json:
        results = file_data_json["results"]
        for sample_id in results:
            sample_results = results[sample_id]
            if "meta-data" not in sample_results:
                sys.stderr.write("sample %s has no metadata and therefore is missing the annotation for gene-panel - adding to consume list\n" % (sample_id))
                samples_with_problematic_metadata.add(sample_id)
                continue
            problematic_metadata = {}
            if sample_missing_required_metadata(sample_results["meta-data"], problematic_metadata):
                samples_with_problematic_metadata.add(sample_id)
                sys.stderr.write("adding sample %s to consume list because of this problematic metadata:\n%s\n" % (sample_id, problematic_metadata))
                continue
    return samples_with_problematic_metadata

def main():
    (input_filepath, output_filepath) = parse_args()
    input_filehandle = sys.stdin
    if input_filepath:
        input_filehandle = open(input_filepath, "r+")
    output_filehandle = sys.stdout
    if output_filepath:
        output_filehandle = open(output_filepath, "w+")
    samples_to_consume = find_samples_with_problematic_metadata(input_filehandle)
    for sample_id in sorted(samples_to_consume):
        output_filehandle.write("%s\n" % (sample_id))

if __name__ == "__main__":
    main()
