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

    def validate_file_exists(self):
        return os.path.exists(self.file_path)
    
    # Define other base-level file checks here
    
    def load_file(self, sep="\t", float_precision="round_trip", na_filter=False, low_memory=False, **opts):
        start_read = 0
        with open(self.file_path, "r") as f:
            # Assumes that commented lines are at the start of the file
            for line_count, line in enumerate(f):
                if not line.startswith("#"):
                    start_read = line_count
                    break

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

    # Sub-classes must define this method
    @abstractmethod
    def run_validate_checks(self):
        ...

    # Top-level method to validate file
    # Sub-classes need to define run_validate_checks()
    def validate(self):
        # Validate that the file exists before running any checks
        assert self.validate_file_exists(), f"File {self.file_path} does not exist"

        # TODO assert statements for all steps ?

        # Load file contents into dataframe
        self.load_file()
        
        # Run all validation checks
        self.run_validate_checks()

        # Write out validated data
        self.write_to_file()

        # Clear df from memory
        self.clear_df()

    def write_to_file(self, sep="\t", quoting=csv.QUOTE_NONE, **opts):
        opts["index"] = opts.get(
            "index", False
        )  # by default, don't write the index column to the output
        self.df.to_csv(
            self.output_file_path,
            sep=sep,
            quoting=quoting,
            **opts,
        )

    def clear_df(self):
        del self.df


class CDMSampleFileValidator(FileValidator):
    def __init__(self, file_path, output_file_path=""):
        super().__init__(file_path, output_file_path)
    
    def validate_sids_match_pids(self):
        non_matching = self.df.query(f"PATIENT_ID != SAMPLE_ID.str.extract('(P-\\d*)-*')[0]")
        print(non_matching)
        self.df.drop(non_matching.index)

        if non_matching.size > 0:
            logging.warn(f"The following {non_matching.size} records were dropped due to mismatched patient and sample IDs: {non_matching}")

    # TODO options to run specific checks?
    def run_validate_checks(self):
        self.validate_sids_match_pids()


class CDMFileValidator:
    def __init__(self, sample_file_path):
        self.sample_file_validator = CDMSampleFileValidator(sample_file_path)

    def validate_sample_file(self):
        self.sample_file_validator.validate()

    # Define other file type validation functions here

    def validate(self):
        self.validate_sample_file()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(prog="validation_utils_py3.py")
    parser.add_argument(
        "-v",
        "--validation-type",
        dest="validation_type",
        choices=["cdm"], # Add here as more validators implemented
        action="store",
        required=True,
        help="Type of validation to run. Accepted values: ['cdm', ]",
    )
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
    #file_type = args.file_type
    file_path = args.file_path

    if validation_type == "cdm":
        cdm_validator = CDMFileValidator(file_path)
        cdm_validator.validate()
