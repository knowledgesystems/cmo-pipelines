#!/usr/bin/env python3
#
# Copyright (c) 2018 Memorial Sloan Kettering Cancer Center.
# This library is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
# documentation provided hereunder is on an "as is" basis, and
# Memorial Sloan Kettering Cancer Center
# has no obligations to provide maintenance, support,
# updates, enhancements or modifications.  In no event shall
# Memorial Sloan Kettering Cancer Center
# be liable to any party for direct, indirect, special,
# incidental or consequential damages, including lost profits, arising
# out of the use of this software and its documentation, even if
# Memorial Sloan Kettering Cancer Center
# has been advised of the possibility of such damage.
#
#
# This is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# ------------------------------------------------------------------------------
# Python 3 port of generate_case_lists.py.
#
# Generates case lists given a cBioPortal study directory containing genomic
# files, a directory to write the case list files to, a cancer study stable id,
# and a tab delimited case lists configuration file with the following columns:
#   CASE_LIST_FILENAME
#   STAGING_FILENAME - one file, a union ("a|b|c") or an intersection ("a&b&c") of files
#   META_STABLE_ID - should contain placeholder <CANCER_STUDY> to be replaced with the study id
#   META_CASE_LIST_CATEGORY
#   META_CANCER_STUDY_ID
#   META_CASE_LIST_NAME
#   META_CASE_LIST_DESCRIPTION - may contain placeholder <NUM_CASES> which will be
#     replaced with the number of cases
#
# Behaviour aligned with the importer (FileUtilsImpl.generateCaseLists) and the
# datahub-study-curation-tools jar-case-list-generator:
#   - config fields are trimmed, blank config lines skipped
#   - staging filenames resolve case-insensitively (config data_CNA.txt matches data_cna.txt)
#   - case ids keep first-seen order (deterministic output)
#   - optional TCGA barcode standardization (--normalize-tcga-barcodes)
#   - study id defaults to cancer_study_identifier in meta_study.txt
#   - existing case lists are gap-filled only unless --overwrite
#
# To get usage:
#   python3 generate_case_lists.py -h
#
# Authors: Avery Wang and Manda Wilson
# ------------------------------------------------------------------------------
import argparse
import os
import os.path
import re
import sys

CASE_LIST_CONFIG_HEADER_COLUMNS = ["CASE_LIST_FILENAME", "STAGING_FILENAME", "META_STABLE_ID", "META_CASE_LIST_CATEGORY", "META_CANCER_STUDY_ID", "META_CASE_LIST_NAME", "META_CASE_LIST_DESCRIPTION"]
CASE_LIST_UNION_DELIMITER = "|"
CASE_LIST_INTERSECTION_DELIMITER = "&"
MUTATION_STAGING_GENERAL_PREFIX = "data_mutations"
SEQUENCED_SAMPLES_FILENAME = "sequenced_samples.txt"
MUTATION_CASE_LIST_META_HEADER = "sequenced_samples"
MUTATION_CASE_ID_COLUMN_HEADER = "Tumor_Sample_Barcode"
SAMPLE_ID_COLUMN_HEADER = "SAMPLE_ID"
ALTERNATE_SAMPLE_ID_COLUMN_HEADER = "Sample_ID"
SV_SAMPLE_ID_COLUMN_HEADER = "Sample_Id"
SAMPLE_ID_COLUMN_HEADERS = [MUTATION_CASE_ID_COLUMN_HEADER, SAMPLE_ID_COLUMN_HEADER, ALTERNATE_SAMPLE_ID_COLUMN_HEADER, SV_SAMPLE_ID_COLUMN_HEADER]
NON_CASE_IDS = frozenset(["MIRNA", "LOCUS", "ID", "GENE SYMBOL", "ENTREZ_GENE_ID", "HUGO_SYMBOL", "LOCUS ID", "CYTOBAND", "COMPOSITE.ELEMENT.REF", "HYBRIDIZATION REF"])
CANCER_STUDY_TAG = "<CANCER_STUDY>"
NUM_CASES_TAG = "<NUM_CASES>"
META_STUDY_FILENAME = "meta_study.txt"
CANCER_STUDY_IDENTIFIER_PROPERTY = "cancer_study_identifier"
TCGA_SAMPLE_BARCODE_REGEX = re.compile(r"^(TCGA-\w\w-\w\w\w\w-\d\d).*$")


def log(verbose, message):
    if verbose:
        print("LOG: " + message)


def get_sample_id(barcode):
    """Standardizes TCGA sample barcodes the way the importer (StableIdUtil.getSampleId) does:
    "Tumor" -> "01", "Normal" -> "11", truncate to TCGA-XX-XXXX-NN, patient-only barcodes get "-01".
    Non-TCGA ids pass through untouched."""
    if not barcode.startswith("TCGA"):
        return barcode
    if "Tumor" in barcode:
        cleaned = barcode.replace("Tumor", "01")
    elif "Normal" in barcode:
        cleaned = barcode.replace("Normal", "11")
    else:
        cleaned = barcode
    parts = cleaned.split("-")
    if len(parts) < 4:
        return barcode + "-01"
    sample_id = "-".join(parts[:4])
    match = TCGA_SAMPLE_BARCODE_REGEX.match(sample_id)
    return match.group(1) if match else sample_id


def resolve_staging_file(study_dir, staging_filename):
    """Full path of staging_filename in study_dir. Falls back to a case-insensitive
    match (config says data_CNA.txt, study has data_cna.txt). None if nothing matches."""
    full_path = os.path.join(study_dir, staging_filename)
    if os.path.isfile(full_path):
        return full_path
    lowered = staging_filename.lower()
    if os.path.isdir(study_dir):
        for name in sorted(os.listdir(study_dir)):
            candidate = os.path.join(study_dir, name)
            if name.lower() == lowered and os.path.isfile(candidate):
                return candidate
    return None


def get_study_id_from_meta_study(study_dir):
    """cancer_study_identifier from study_dir/meta_study.txt, or None."""
    meta_study_full_path = os.path.join(study_dir, META_STUDY_FILENAME)
    if not os.path.isfile(meta_study_full_path):
        return None
    with open(meta_study_full_path, "r") as meta_study_file:
        for line in meta_study_file:
            if line.startswith(CANCER_STUDY_IDENTIFIER_PROPERTY + ":"):
                return line.split(":", 1)[1].strip()
    return None


def ordered_union(case_list, additional_cases):
    """Appends cases not already in case_list, preserving first-seen order."""
    seen = set(case_list)
    for case_id in additional_cases:
        if case_id not in seen:
            seen.add(case_id)
            case_list.append(case_id)
    return case_list


def ordered_intersection(case_list, other_cases):
    """Cases of case_list also in other_cases, preserving order."""
    keep = set(other_cases)
    return [case_id for case_id in case_list if case_id in keep]


def read_case_list_config(case_list_config_filename):
    """Yields dicts keyed by CASE_LIST_CONFIG_HEADER_COLUMNS; fields trimmed, blank lines skipped."""
    with open(case_list_config_filename, "r") as case_list_config_file:
        header = case_list_config_file.readline().rstrip("\r\n").split("\t")
        header = [column.strip() for column in header]
        for column in CASE_LIST_CONFIG_HEADER_COLUMNS:
            if column not in header:
                print("ERROR: column '%s' is not in '%s'" % (column, case_list_config_filename), file=sys.stderr)
                sys.exit(2)
        for line in case_list_config_file:
            line = line.rstrip("\r\n")
            if not line.strip():
                continue
            fields = [field.strip() for field in line.split("\t")]
            if len(fields) < len(header):
                fields += [""] * (len(header) - len(fields))
            yield dict(zip(header, fields))


def read_metadata(path):
    values = {}
    if os.path.isfile(path):
        with open(path) as stream:
            for line in stream:
                if not line.lstrip().startswith('#') and ':' in line:
                    key, value = line.split(':', 1)
                    values[key.strip()] = value.strip()
    return values


def generate_case_lists(case_list_config_filename, case_list_dir, study_dir, study_id, overwrite=False, verbose=False, normalize_tcga_barcodes=False):
    virtual_all = read_metadata(os.path.join(study_dir, META_STUDY_FILENAME)).get('add_global_case_list', '').lower() == 'true'
    existing = [read_metadata(os.path.join(case_list_dir, name))
                for name in os.listdir(case_list_dir)
                if not (name.startswith('.') or name.endswith('~'))
                and os.path.isfile(os.path.join(case_list_dir, name))]
    existing_ids = {values.get('stable_id') for values in existing}
    existing_categories = {values.get('case_list_category') for values in existing
                           if values.get('cancer_study_identifier') == study_id
                           and values.get('stable_id', '').startswith(study_id + '_')
                           and values.get('case_list_ids', '').strip()}
    for config in read_case_list_config(case_list_config_filename):
        stable_id = config['META_STABLE_ID'].replace(CANCER_STUDY_TAG, study_id)
        if virtual_all and stable_id == study_id + '_all':
            continue
        if stable_id in existing_ids and not overwrite:
            continue
        category = config['META_CASE_LIST_CATEGORY']
        # The importer still requires these primary stable IDs for profiled-sample
        # semantics. Category equivalence only substitutes additional generated roles.
        primary_id = stable_id in {study_id + suffix for suffix in ('_all', '_sequenced', '_cna')}
        if (not primary_id and category and category != 'other'
                and category in existing_categories and not overwrite):
            continue
        case_list_filename = config["CASE_LIST_FILENAME"]
        staging_filename_list = config["STAGING_FILENAME"]
        case_list_file_full_path = os.path.join(case_list_dir, case_list_filename)

        # union (like all cases) is checked first, then intersection (like complete or cna-seq)
        union_case_list = CASE_LIST_UNION_DELIMITER in staging_filename_list
        intersection_case_list = (not union_case_list) and CASE_LIST_INTERSECTION_DELIMITER in staging_filename_list
        delimiter = CASE_LIST_UNION_DELIMITER if union_case_list else CASE_LIST_INTERSECTION_DELIMITER
        staging_filenames = [name.strip() for name in staging_filename_list.split(delimiter) if name.strip()]
        log(verbose, "generate_case_lists(), staging filenames: %s" % (",".join(staging_filenames)))

        # if this is intersection all staging files must exist
        if intersection_case_list and not all(resolve_staging_file(study_dir, name) is not None for name in staging_filenames):
            continue

        case_set = []
        num_staging_files_processed = 0
        for staging_filename in staging_filenames:
            log(verbose, "generate_case_lists(), processing staging file '%s'" % (staging_filename))
            case_list = get_case_list_from_staging_file(study_dir, staging_filename, verbose)
            if len(case_list) == 0:
                log(verbose, "generate_case_lists(), no cases in '%s', skipping..." % (staging_filename))
                continue
            if normalize_tcga_barcodes:
                case_list = [get_sample_id(case_id) for case_id in case_list]
            if intersection_case_list:
                if num_staging_files_processed == 0:
                    case_set = ordered_union([], case_list)
                else:
                    case_set = ordered_intersection(case_set, case_list)
            else:
                case_set = ordered_union(case_set, case_list)
            num_staging_files_processed += 1

        if len(case_set) == 0:
            log(verbose, "generate_case_lists(), case_set.size() == 0, skipping call to write_case_list_file()...")
            continue
        # do not write out an intersection unless we've processed all the files required
        if intersection_case_list and num_staging_files_processed != len(staging_filenames):
            log(verbose, "generate_case_lists(), number of staging files processed (%d) != number of staging files required (%d) for '%s', skipping call to write_case_list_file()..." % (num_staging_files_processed, len(staging_filenames), case_list_filename))
            continue
        if os.path.exists(case_list_file_full_path) and not overwrite:
            raise ValueError("Required case-list filename '%s' is occupied by an unrelated list; "
                             "preserve it and resolve the stable-ID/category conflict explicitly"
                             % case_list_filename)
        log(verbose, "generate_case_lists(), calling write_case_list_file()...")
        write_case_list_file(config, study_id, case_list_file_full_path, case_set, verbose)
        existing_ids.add(stable_id)
        existing_categories.add(category)


def get_case_list_from_staging_file(study_dir, staging_filename, verbose):
    log(verbose, "get_case_list_from_staging_file(), '%s'" % (staging_filename))
    case_set = []

    # if we are processing mutations data and a SEQUENCED_SAMPLES_FILENAME exists, use it
    if MUTATION_STAGING_GENERAL_PREFIX in staging_filename.lower():
        sequenced_samples_full_path = os.path.join(study_dir, SEQUENCED_SAMPLES_FILENAME)
        if os.path.isfile(sequenced_samples_full_path):
            log(verbose, "get_case_list_from_staging_file(), '%s' exists, calling get_case_list_from_sequenced_samples_file()" % (SEQUENCED_SAMPLES_FILENAME))
            return get_case_list_from_sequenced_samples_file(sequenced_samples_full_path, verbose)

    staging_file_full_path = resolve_staging_file(study_dir, staging_filename)
    if staging_file_full_path is None:
        return []

    with open(staging_file_full_path, "r") as staging_file:
        id_column_index = 0
        process_header = True
        for line in staging_file:
            line = line.rstrip("\r\n")
            if line.startswith("#"):
                if line.startswith("#" + MUTATION_CASE_LIST_META_HEADER + ":"):
                    # split on any whitespace: tabs, single spaces, consecutive spaces
                    return ordered_union([], line[len(MUTATION_CASE_LIST_META_HEADER) + 2:].strip().split())
                continue
            values = line.split("\t")
            if process_header:
                id_column_headers = [column for column in SAMPLE_ID_COLUMN_HEADERS if column in values]
                if not id_column_headers:
                    # not a MAF/clinical/SV file: the header itself holds the case ids
                    log(verbose, "get_case_list_from_staging_file(), no sample id column in header, we assume it contains sample ids...")
                    ordered_union(case_set, [value for value in values if value.upper() not in NON_CASE_IDS])
                    break
                id_column_index = values.index(id_column_headers[0])
                log(verbose, "get_case_list_from_staging_file(), samples ids in column with index: %d" % (id_column_index))
                process_header = False
                continue
            if not line.strip():
                continue
            if id_column_index >= len(values):
                raise ValueError("%s: data row has no column %d: %s" % (staging_filename, id_column_index, line[:80]))
            ordered_union(case_set, [values[id_column_index]])

    return case_set


def get_case_list_from_sequenced_samples_file(sequenced_samples_full_path, verbose):
    log(verbose, "get_case_list_from_sequenced_samples_file, '%s'" % (sequenced_samples_full_path))
    case_set = []
    with open(sequenced_samples_full_path, "r") as sequenced_samples_file:
        for line in sequenced_samples_file:
            case_id = line.rstrip("\r\n")
            if case_id:
                ordered_union(case_set, [case_id])
    log(verbose, "get_case_list_from_sequenced_samples_file, case set size: %d" % (len(case_set)))
    return case_set


def write_case_list_file(config, study_id, case_list_full_path, case_set, verbose):
    log(verbose, "write_case_list_file(), '%s'" % (case_list_full_path))
    stable_id = config["META_STABLE_ID"].replace(CANCER_STUDY_TAG, study_id)
    case_list_description = config["META_CASE_LIST_DESCRIPTION"].replace(NUM_CASES_TAG, str(len(case_set)))
    with open(case_list_full_path, "w") as case_list_file:
        case_list_file.write("cancer_study_identifier: " + study_id + "\n")
        case_list_file.write("stable_id: " + stable_id + "\n")
        case_list_file.write("case_list_name: " + config["META_CASE_LIST_NAME"] + "\n")
        case_list_file.write("case_list_description: " + case_list_description + "\n")
        case_list_file.write("case_list_category: " + config["META_CASE_LIST_CATEGORY"] + "\n")
        case_list_file.write("case_list_ids: " + "\t".join(case_set) + "\n")


def parse_generate_case_list_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--case-list-config-file", action="store", dest="case_list_config_file", required=True, help='Path to the case list configuration file.  An example can be found in "test-py3/resources/generate_case_lists/case_list_config.tsv"')
    parser.add_argument("-d", "--case-list-dir", action="store", dest="case_list_dir", required=True, help="Path to the directory in which the case list files should be written")
    parser.add_argument("-s", "--study-dir", action="store", dest="study_dir", required=True, help="The directory that contains the cancer study genomic files")
    parser.add_argument("-i", "--study-id", action="store", dest="study_id", required=False, help="The cancer study stable id (default: cancer_study_identifier from meta_study.txt in the study directory)")
    parser.add_argument("-t", "--normalize-tcga-barcodes", action="store_true", dest="normalize_tcga_barcodes", required=False, help="When given, TCGA sample barcodes are standardized the way the importer does (TCGA-XX-XXXX-NN); other ids are untouched")
    parser.add_argument("-o", "--overwrite", action="store_true", dest="overwrite", required=False, help="When given, overwrite the case list files")
    parser.add_argument("-v", "--verbose", action="store_true", dest="verbose", required=False, help="When given, be verbose")
    return parser


def main(args):
    parser = parse_generate_case_list_args()
    case_list_config_filename = args.case_list_config_file
    case_list_dir = args.case_list_dir
    study_dir = args.study_dir
    study_id = args.study_id

    log(args.verbose, "case_list_config_file='%s' case_list_dir='%s' study_dir='%s' study_id='%s' overwrite='%s' normalize_tcga_barcodes='%s'" % (case_list_config_filename, case_list_dir, study_dir, study_id, args.overwrite, args.normalize_tcga_barcodes))

    if not os.path.isfile(case_list_config_filename):
        print("ERROR: case list configuration file '%s' does not exist or is not a file" % (case_list_config_filename), file=sys.stderr)
        parser.print_help()
        sys.exit(2)
    if not os.path.isdir(case_list_dir):
        print("ERROR: case list file directory '%s' does not exist or is not a directory" % (case_list_dir), file=sys.stderr)
        parser.print_help()
        sys.exit(2)
    if not os.path.isdir(study_dir):
        print("ERROR: study directory '%s' does not exist or is not a directory" % (study_dir), file=sys.stderr)
        parser.print_help()
        sys.exit(2)
    if not study_id:
        study_id = get_study_id_from_meta_study(study_dir)
        if not study_id:
            print("ERROR: no --study-id given and no %s found in %s in study directory '%s'" % (CANCER_STUDY_IDENTIFIER_PROPERTY, META_STUDY_FILENAME, study_dir), file=sys.stderr)
            parser.print_help()
            sys.exit(2)

    generate_case_lists(case_list_config_filename, case_list_dir, study_dir, study_id, args.overwrite, args.verbose, args.normalize_tcga_barcodes)


if __name__ == "__main__":
    main(parse_generate_case_list_args().parse_args())
