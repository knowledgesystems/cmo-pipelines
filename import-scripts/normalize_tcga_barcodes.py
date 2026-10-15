#!/usr/bin/env python3
"""Normalize TCGA sample IDs in study files before generating case lists."""

import argparse
import filecmp
import os
from pathlib import Path
import re
import shutil
import tempfile

TCGA_SAMPLE_BARCODE_REGEX = re.compile(r"^(TCGA-\w\w-\w\w\w\w-\d\d).*$")
SAMPLE_COLUMNS = {"sample_id", "sample", "sampleid", "tumor_sample_barcode",
                  "matched_norm_sample_barcode"}


def get_sample_id(barcode):
    """Match the Java importer's StableIdUtil.getSampleId rules."""
    if not barcode.startswith("TCGA"):
        return barcode
    if "Tumor" in barcode:
        cleaned = barcode.replace("Tumor", "01")
    elif "Normal" in barcode:
        cleaned = barcode.replace("Normal", "11")
    else:
        cleaned = barcode
    parts = cleaned.rstrip("-").split("-")  # Java split discards trailing empty fields.
    if len(parts) < 4:
        return barcode + "-01"
    sample_id = "-".join(parts[:4])
    match = TCGA_SAMPLE_BARCODE_REGEX.match(sample_id)
    return match.group(1) if match else sample_id


def check_collision(seen, original, normalized, path):
    if normalized in seen and seen[normalized] != original:
        raise ValueError("%s: '%s' and '%s' normalize to the same sample '%s'" %
                         (path, seen[normalized], original, normalized))
    seen[normalized] = original


def normalized_lines(path):
    indexes = None
    clinical_sample_index = None
    seen = {}
    with path.open(encoding="utf-8", newline="") as source:
        for line in source:
            body = line.rstrip("\r\n")
            ending = line[len(body):]
            if path.name == "sequenced_samples.txt":
                body = re.sub(r"\S+", lambda match: get_sample_id(match.group()), body)
            elif body.startswith(("#sequenced_samples:", "case_list_ids:")):
                prefix, values = body.split(":", 1)
                body = prefix + ":" + re.sub(r"\S+", lambda match: get_sample_id(match.group()), values)
            elif body and not body.startswith("#"):
                fields = body.split("\t")
                if indexes is None:
                    names = [field.lower() for field in fields]
                    indexes = [i for i, name in enumerate(names) if name in SAMPLE_COLUMNS
                               or (path.suffix == ".seg" and name == "id")]
                    if "sample_id" in names and ("clinical" in path.name or "patient_id" in names):
                        clinical_sample_index = names.index("sample_id")
                    # Matrix sample IDs are column headers, not row values.
                    matrix_seen = {}
                    for i, field in enumerate(fields):
                        if field.startswith("TCGA"):
                            normalized = get_sample_id(field)
                            check_collision(matrix_seen, field, normalized, path)
                            fields[i] = normalized
                else:
                    for i in indexes:
                        if i >= len(fields):
                            raise ValueError("%s: row is missing a sample-ID column" % path)
                        original = fields[i]
                        fields[i] = get_sample_id(original)
                        if i == clinical_sample_index:
                            check_collision(seen, original, fields[i], path)
                body = "\t".join(fields)
            yield body + ending


def normalize_study(study_dir):
    """Stage all changes first; reject collisions before replacing any source file."""
    study_dir = Path(study_dir)
    if not study_dir.is_dir():
        raise ValueError("Study directory does not exist: %s" % study_dir)
    paths = [path for path in study_dir.iterdir()
             if path.is_file() and path.suffix in (".txt", ".tsv", ".maf", ".seg")]
    paths.extend((study_dir / "case_lists").glob("*.txt"))
    staged = []
    try:
        for path in sorted(paths):
            if path.is_symlink():
                raise ValueError("Refusing to replace symbolic link: %s" % path)
            with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", newline="",
                                             dir=path.parent, prefix=".normalize-tcga-", delete=False) as output:
                temporary = Path(output.name)
                staged.append((path, temporary))
                output.writelines(normalized_lines(path))
            shutil.copymode(path, temporary)
            # Keep unchanged files (including their timestamps) untouched.
            if filecmp.cmp(path, temporary, shallow=False):
                temporary.unlink()
                staged.pop()
        for path, temporary in staged:
            os.replace(temporary, path)
        return len(staged)
    finally:
        for _, temporary in staged:
            temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__, epilog=
        "Updates top-level .txt/.tsv/.maf/.seg files and case_lists/*.txt in place. "
        "Patient IDs and non-TCGA IDs are unchanged; records are not merged.")
    parser.add_argument("-s", "--study-dir", required=True)
    args = parser.parse_args()
    try:
        print("Normalized TCGA sample IDs in %d files" % normalize_study(args.study_dir))
    except (ValueError, OSError, UnicodeError) as error:
        parser.exit(2, "ERROR: %s\n" % error)
