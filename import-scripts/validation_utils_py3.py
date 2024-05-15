#!/usr/bin/env python3

""" validation_utils_py3.py
This script can be used to validate the format of an arbitrary study file.
As of 5/14/24 only CDM clinical sample file validation has been implemented but
this should be extended for future use-cases.

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
import pandas as pd


class FileValidator(ABC):
    def __init__(self, file_path, output_file_path=""):
        self.file_path = file_path
        self.output_file_path = output_file_path if output_file_path else file_path
        self.df = None
        self.header = []

    def validate_file_exists(self):
        return os.path.exists(self.file_path)

    # Define other base-level file checks here

    def load_file(
        self,
        sep="\t",
        float_precision="round_trip",
        na_filter=False,
        low_memory=False,
        **opts,
    ):
        """Loads the given file into a pandas dataframe.
        Accepts an arbitrary number of arguments to pass to the pandas read_table function,
        with defaults for the following arguments:
            sep="\t" : Tab delimiter for the input file
            float_precision="round_trip" : Prevent floating point rounding
            na_filter=False : Prevent interpreting of NA values
            low_memory=False : Prevent mixed type inference
        """
        start_read = 0
        with open(self.file_path, "r") as f:
            # Assumes that commented lines are at the start of the file
            for line_count, line in enumerate(f):
                if not line.startswith("#"):
                    start_read = line_count
                    break
                self.header.append(line.strip())

        self.df = pd.read_table(
            self.file_path,
            sep=sep,
            # Document these arguments
            float_precision=float_precision,
            na_filter=na_filter,
            low_memory=low_memory,
            skiprows=start_read,
            **opts,
        )

        # TODO define a return value?

    @abstractmethod
    def run_validate_checks(self):
        """Abstract method that must be defined by sub-classes.
        Should contain all validation checks required for file type.
        """
        ...

    def validate(self):
        """Top-level method to validate the given file by loading it into a dataframe,
        running all validation checks, writing the validated data to a file, and clearing the dataframe.
        Sub-classes need to define run_validate_checks().
        """

        # Validate that the file exists before running any checks
        assert self.validate_file_exists(), f"File {self.file_path} does not exist"

        # TODO assert statements for all steps

        # Load file contents into dataframe
        self.load_file()

        # Run all validation checks
        self.run_validate_checks()

        # Write out validated data
        self.write_to_file()

        # Clear df from memory
        self.clear_df()

    def write_to_file(
        self, sep="\t", mode="a", quoting=csv.QUOTE_NONE, index=False, **opts
    ):
        """Writes the validated file contents out to a file.
        Accepts an arbitrary number of arguments to pass to the pandas to_csv function,
        with defaults for the following arguments:
            sep="\t" : Tab delimiter for the output file
            mode="a" : Append to the end of the output file if it exists (so that we do not overrwite the file header)
            quoting=csv.QUOTE_NONE : Prevent pandas from adding extra quotes into string fields
            index=False : By default, don't write the index column to the output
        """

        # Write header to file
        with open(self.output_file_path, "w") as f:
            for line in self.header:
                f.write(f"{line}\n")

        # Write data to file
        self.df.to_csv(
            self.output_file_path,
            sep=sep,
            mode=mode,
            quoting=quoting,
            index=index,
            **opts,
        )

    def clear_df(self):
        del self.df


class CDMSampleFileValidator(FileValidator):
    """Class to validate the clinical sample file provided by CDM."""

    def __init__(self, file_path, output_file_path=""):
        super().__init__(file_path, output_file_path)

    def validate_sids_match_pids(self):
        """Extracts the patient ID from the SAMPLE_ID column and verifies that it matches the PATIENT_ID column for each
        row in the dataframe. If the two do not match, the row is removed.
        """
        non_matching = self.df.query(
            "PATIENT_ID != SAMPLE_ID.str.extract('(P-[0-9]*)-*')[0]"
        )
        self.df = self.df.drop(non_matching.index)

        if len(non_matching.index) > 0:
            logging.warning(
                f"The following {len(non_matching.index)} records were dropped due to mismatched patient and sample IDs:\n{non_matching}"
            )

    def run_validate_checks(self):
        # TODO Add options to run specific checks
        self.validate_sids_match_pids()


class CDMFileValidator:
    """Class to validate all CDM data. Currently, this class only validates the clinical sample file."""

    def __init__(self, sample_file_path):
        self.sample_file_validator = CDMSampleFileValidator(sample_file_path)

    def validate_sample_file(self):
        self.sample_file_validator.validate()

    # TODO Define other file type validation functions here

    def validate(self):
        self.validate_sample_file()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(prog="validation_utils_py3.py")
    parser.add_argument(
        "-v",
        "--validation-type",
        dest="validation_type",
        choices=["cdm"],  # Add here as more validators are implemented
        action="store",
        required=True,
        help="Type of validation to run. Accepted values: ['cdm', ]",
    )
    # May need this argument as more file types are implemented per validator
    """
    parser.add_argument(
        "-t",
        "--file-type",
        dest="file_type",
        choices=["sample"], # Add here as more file types implemented
        action="store",
        required=True,
        help="Type of file to validate. Accepted values: ['sample', ]",
    )
    """
    parser.add_argument(
        "-f",
        "--file-path",
        dest="file_path",
        action="store",
        required=True,
        help="Path to file",
    )

    args = parser.parse_args()
    validation_type = args.validation_type
    # file_type = args.file_type
    file_path = args.file_path

    if validation_type == "cdm":
        cdm_validator = CDMFileValidator(file_path)
        cdm_validator.validate()
