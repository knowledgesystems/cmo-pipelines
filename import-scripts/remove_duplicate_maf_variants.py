#!/usr/bin/env python3
"""remove_duplicate_maf_variants.py

Python 3 port of remove-duplicate-maf-variants.py.

Removes duplicate MAF records based on the 8 key columns the cBioPortal
validator uses to flag duplicate mutations:
    Entrez_Gene_Id, Chromosome, Start_Position, End_Position,
    Variant_Classification, Tumor_Seq_Allele2, Tumor_Sample_Barcode, HGVSp_Short

Two strategies for picking the record to keep:
    vaf    (default) keeps the record with the highest VAF,
           VAF = t_alt_count / (t_ref_count + t_alt_count);
           falls back to the first record when VAF cannot be calculated
    first  keeps the first record seen (validator / datahub curation tools semantics)

Record order of the input file is preserved. Key values are whitespace-trimmed
before comparison.

Usage:
    python3 remove_duplicate_maf_variants.py -i data_mutations.txt -o deduped.txt [--strategy vaf|first]
    python3 remove_duplicate_maf_variants.py -i data_mutations.txt --in-place [--strategy vaf|first]
"""
import argparse
import os
import sys

KEY_COLUMNS = ["Entrez_Gene_Id", "Chromosome", "Start_Position", "End_Position", "Variant_Classification", "Tumor_Seq_Allele2", "Tumor_Sample_Barcode", "HGVSp_Short"]
T_REF_COUNT_COLUMN = "t_ref_count"
T_ALT_COUNT_COLUMN = "t_alt_count"
STRATEGY_VAF = "vaf"
STRATEGY_FIRST = "first"
STRATEGIES = [STRATEGY_VAF, STRATEGY_FIRST]


def calculate_vaf(record, t_refc_index, t_altc_index):
    """VAF for a record line, or None if it cannot be calculated."""
    columns = record.rstrip("\n").split("\t")
    try:
        t_alt = int(columns[t_altc_index])
        t_ref = int(columns[t_refc_index])
        return t_alt / (t_alt + t_ref)
    except (ValueError, IndexError, ZeroDivisionError):
        return None


def pick_record(key, records, strategy, t_refc_index, t_altc_index):
    """The single record to keep for a duplicate group."""
    if strategy == STRATEGY_FIRST or len(records) == 1:
        return records[0]
    best_record = records[0]
    best_vaf = None
    for record in records:
        vaf = calculate_vaf(record, t_refc_index, t_altc_index)
        if vaf is None:
            columns = record.rstrip("\n").split("\t")
            print("ERROR: VAF cannot be calculated for the variant : " + key, file=sys.stderr)
            print("The t_ref_count is: %s and t_alt_count is: %s" % (columns[t_refc_index], columns[t_altc_index]), file=sys.stderr)
            continue
        if best_vaf is None or vaf > best_vaf:
            best_vaf = vaf
            best_record = record
    return best_record


def remove_duplicate_variants(maf_data, strategy, t_refc_index, t_altc_index):
    """(kept records in input order, number of dropped records)."""
    kept = []
    dropped = 0
    for key, records in maf_data.items():
        kept.append(pick_record(key, records, strategy, t_refc_index, t_altc_index))
        dropped += len(records) - 1
    return kept, dropped


def build_key(data, key_columns_index):
    return "\t".join(data[index].strip() for index in key_columns_index)


def process_maf_file(maf_filename, out_filename, strategy):
    comments = []
    header = None
    key_columns_index = []
    t_refc_index = None
    t_altc_index = None
    maf_data = {}  # insertion ordered

    with open(maf_filename, "r") as maf_file:
        for line in maf_file:
            if header is None and line.startswith("#"):
                comments.append(line)
            elif header is None:
                # first non-comment line is the header
                header = line
                header_cols = line.rstrip("\r\n").split("\t")
                missing = [column for column in KEY_COLUMNS if column not in header_cols]
                if missing:
                    print("ERROR: MAF header is missing key columns: " + ", ".join(missing), file=sys.stderr)
                    sys.exit(2)
                key_columns_index = [header_cols.index(column) for column in KEY_COLUMNS]
                if strategy == STRATEGY_VAF:
                    if T_REF_COUNT_COLUMN not in header_cols or T_ALT_COUNT_COLUMN not in header_cols:
                        print('ERROR: strategy "%s" needs %s and %s columns, use --strategy %s' % (STRATEGY_VAF, T_REF_COUNT_COLUMN, T_ALT_COUNT_COLUMN, STRATEGY_FIRST), file=sys.stderr)
                        sys.exit(2)
                    t_refc_index = header_cols.index(T_REF_COUNT_COLUMN)
                    t_altc_index = header_cols.index(T_ALT_COUNT_COLUMN)
            else:
                if not line.strip():
                    continue
                data = line.rstrip("\r\n").split("\t")
                try:
                    reference_key = build_key(data, key_columns_index)
                except IndexError:
                    print("ERROR: record has fewer columns than the header, keeping as is: " + line.rstrip("\n")[:80], file=sys.stderr)
                    reference_key = line
                maf_data.setdefault(reference_key, []).append(line)

    if header is None:
        print("ERROR: no header line found in " + maf_filename, file=sys.stderr)
        sys.exit(2)

    kept, dropped = remove_duplicate_variants(maf_data, strategy, t_refc_index, t_altc_index)
    tmp_filename = out_filename + ".tmp"
    with open(tmp_filename, "w") as datafile:
        datafile.writelines(comments)
        datafile.write(header)
        datafile.writelines(kept)
    os.replace(tmp_filename, out_filename)
    print("MAF file with %d duplicate variants removed is written to: %s" % (dropped, out_filename))
    return dropped


def main():
    parser = argparse.ArgumentParser(description="Remove duplicate MAF records on the validator's 8 key columns.")
    parser.add_argument("-i", "--input-maf-file", dest="input_maf_file", required=True)
    parser.add_argument("-o", "--output-maf-file", dest="output_maf_file", help="output path (required unless --in-place)")
    parser.add_argument("--in-place", dest="in_place", action="store_true", help="rewrite the input file")
    parser.add_argument("-s", "--strategy", dest="strategy", choices=STRATEGIES, default=STRATEGY_VAF, help="which duplicate to keep (default: %s)" % (STRATEGY_VAF))
    args = parser.parse_args()

    if args.in_place:
        out_filename = args.input_maf_file
    elif args.output_maf_file:
        out_filename = args.output_maf_file
    else:
        parser.error("one of --output-maf-file or --in-place is required")
    if not os.path.isfile(args.input_maf_file):
        print("ERROR: input MAF file not found: " + args.input_maf_file, file=sys.stderr)
        sys.exit(2)
    process_maf_file(args.input_maf_file, out_filename, args.strategy)


if __name__ == "__main__":
    main()
