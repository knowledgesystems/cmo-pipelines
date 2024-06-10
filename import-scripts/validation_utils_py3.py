#!/usr/bin/env python3

""" validation_utils_py3.py
This script can be used to validate / format a supported study.

This differs from the existing validation tool used by the curators, in that it performs different checks--
the curators' tool is more general-purpose and suited to all studies published to the public + internal portals.
This one is more specific to MSK-IMPACT and derivative studies (eg CDM, AZ/Sophia).

There is also a validation_utils.py file in this directory, but that one is written in Python 2
and contains a bunch of one-off functions rather than being a cohesive tool of its own.

Usage:
    python3 validation_utils_py3.py --validation-type $VALIDATION_TYPE --file-path $FILE_PATH
Example:
    python3 validation_utils_py3.py --validation-type cdm --file-path data_clinical_sample.txt 
"""


from abc import ABC, abstractmethod
import argparse
import csv
import logging
import os
import sys
import pandas as pd

import re

LOG = logging.getLogger(__name__)

def check(description):
    """
    Decorator to be used on each check performed by a validator.
    """
    
    def decorator(fn):
        def fn_wrapper(*args, **kw):
            logging.info(f"Starting check: {description}")
            fn(*args, **kw)
        
        return fn_wrapper
    
    return decorator

class ValidatorMixin(ABC):
    """
    Abstract base class for study validators. Override as needed for your pipeline.
    """

    def __init__(self, study_dir):
        assert os.path.isdir(study_dir), f"{study_dir} does not exist"
        self.study_dir = study_dir
        self.num_errors = 0
        self.num_warnings = 0

    @abstractmethod
    def validate_study(self):
        """
        Abstract method that must be defined by sub-classes.
        Should contain all validation checks required for the study.
        """

    def load_file(
        self,
        file_path,
        parse_header=False,
        sep="\t",
        float_precision="round_trip",
        na_filter=False,
        low_memory=False,
        **opts,
    ):
        """
        Loads the given file into a pandas dataframe.
        Accepts an arbitrary number of arguments to pass to the pandas read_table function,
        with defaults for the following arguments:
            parse_header=False : When true, parse and return a list of commented lines starting with '#' @ the beginning of the file
            sep="\t" : Tab delimiter for the input file
            float_precision="round_trip" : Prevent floating point rounding
            na_filter=False : Prevent interpreting of NA values
            low_memory=False : Prevent mixed type inference
        """
        file_path = os.path.join(self.study_dir, file_path)
        
        header = []
        start_read = 0
        if parse_header:
            with open(file_path, "r") as f:
                # Assumes that commented lines are at the start of the file
                for line_count, line in enumerate(f):
                    if not line.startswith("#"):
                        start_read = line_count
                        break
                    header.append(line.strip())

        df = pd.read_table(
            file_path,
            sep=sep,
            float_precision=float_precision,
            na_filter=na_filter,
            low_memory=low_memory,
            skiprows=start_read,
            **opts,
        )

        return (header, df) if parse_header else df

    def write_to_file(
        self,
        file_path,
        df,
        header=None,
        sep="\t",
        mode="a",
        quoting=csv.QUOTE_NONE,
        index=False,
        **opts
    ):
        """
        Writes the validated file contents out to a file.
        Accepts an arbitrary number of arguments to pass to the pandas to_csv function,
        with defaults for the following arguments:
            header=None : Header lines to write before the contents on the dataframe
            sep="\t" : Tab delimiter for the output file
            mode="a" : Append to the end of the output file if it exists (so that we do not overrwite the file header)
            quoting=csv.QUOTE_NONE : Prevent pandas from adding extra quotes into string fields
            index=False : By default, don't write the index column to the output
        """
        file_path = os.path.join(self.study_dir, file_path)

        # Write header to file
        if header:
            with open(file_path, "w") as f:
                for line in header:
                    f.write(f"{line}\n")

        # Write data to file
        df.to_csv(
            file_path,
            sep=sep,
            mode=mode,
            quoting=quoting,
            index=index,
            **opts,
        )
        
    def error(self, message):
        LOG.error(message)
        self.num_errors += 1
    
    def warning(self, message):
        LOG.warning(message)
        self.num_warnings += 1

class CDMValidator(ValidatorMixin):
    """
    Class to validate all CDM data. Currently, this class only validates the clinical sample file.
    """

    @check("Patient IDs match sample IDs in clinical sample file")
    def validate_sample_file_sids_match_pids(self, out_fname=None):
        """
        Extracts the patient ID from the SAMPLE_ID column and verifies that it matches the PATIENT_ID column for each
        row in the dataframe. If the two do not match, the row is removed.
        """
        header, df = self.load_file("data_clinical_sample.txt", parse_header=True)
        
        non_matching = df.query(
            "PATIENT_ID != SAMPLE_ID.str.extract('(P-[0-9]*)-*')[0]"
        )
        df = df.drop(index=non_matching.index)

        num_mismatched = len(non_matching.index)
        if num_mismatched > 0:
            self.warning(
                f"The following {num_mismatched} records were dropped due to mismatched patient and sample IDs:\n{non_matching}"
            )
        
        out_fname = out_fname or "data_clinical_sample.txt"
        self.write_to_file(out_fname, df, header=header)

    def validate_study(self):
        self.validate_sample_file_sids_match_pids()

class AZValidator(ValidatorMixin):
    """
    Validates all AstraZeneca study data.
    """
    
    def validate_study(self):
        self.validate_gene_panels_present()

    @check("Gene panels referenced by gene matrix file are all present")
    def validate_gene_panels_present(self, gene_panel_dir=None):
        """
        Checks that the gene panels referenced in data_gene_matrix.txt are present in the gene panels directory.
        """
        if not gene_panel_dir:
            gene_panel_dir = os.path.realpath(os.path.join(self.study_dir, "..", "gene_panels"))
        
        # Get unique list of referenced gene panels
        df = self.load_file("data_gene_matrix.txt")
        required_panels = set()
        required_panels.update(df['mutations'])
        required_panels.update(df['cna'])
        required_panels.update(df['structural_variants'])
        
        # Get list of gene panels we actually have
        actual_panels = self.load_gene_panel_ids(gene_panel_dir)
        
        if not required_panels.issubset(actual_panels):
            missing_panels = required_panels - actual_panels
            self.error(f"Could not find the required gene panels: {missing_panels}")
    
    def load_gene_panel_ids(self, gene_panel_dir):
        stable_ids = []
        
        for basename in os.listdir(gene_panel_dir):
            file = os.path.join(gene_panel_dir, basename)
            if not os.path.isfile(file) or not re.match(r"data_gene_panel_.*\.txt", basename):
                continue
            with open(file, 'r') as fh:
                first_line = fh.readline()
            m = re.match(r"stable_id: (.*)", first_line)
            if not m:
                self.error(f"Could not parse stable id from gene panel file: {file}")
                continue
            stable_id = m.group(1).strip()
            stable_ids.append(stable_id)
        
        if len(set(stable_ids)) != len(stable_ids):
            self.warning("Found duplicate stable ids. Please check the gene panel files.")
        return set(stable_ids)
    
def main():
    # Setup logging
    # TODO output logs as JSON for better Datadog integration? see: https://docs.datadoghq.com/logs/log_collection/python/
    LOG_FILE = "/data/portal-cron/logs/validation_utils_py3.log"
    logging.basicConfig(filename=LOG_FILE,
                        # Setting level=DEBUG results in too much spam from Datadog trace statements
                        level=logging.INFO,
                        format='[%(asctime)s] {%(filename)s:%(lineno)d} %(levelname)s - %(message)s')
    
    # Parse arguments
    parser = argparse.ArgumentParser(prog="validation_utils_py3.py")
    parser.add_argument(
        "-v",
        "--validation-type",
        choices=["cdm", "az"],  # Add here as more validators are implemented
        required=True,
        help="Type of validation to run",
    )
    parser.add_argument(
        "-s",
        "--study-dir",
        required=True,
        help="Path to study directory",
    )

    args = parser.parse_args()
    validation_type = args.validation_type
    study_dir = os.path.realpath(args.study_dir)
    
    LOG.info(f"Starting {validation_type} validation of study at {study_dir}")

    # Set up validator
    validator_cls = {
        "cdm": CDMValidator,
        "az": AZValidator
    }[validation_type]
    validator = validator_cls(study_dir)
    
    # Run validation
    validator.validate_study()
    num_errors = validator.num_errors
    num_warnings = validator.num_warnings

    LOG.info(f"Finished {validation_type} validation")
    LOG.info(f"Total errors: {num_errors}")
    LOG.info(f"Total warnings: {num_warnings}")
    sys.exit(num_errors)

if __name__ == "__main__":
    main()
