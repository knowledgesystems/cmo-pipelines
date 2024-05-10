from abc import ABC, abstractmethod
import logging
import os
import pandas as pd


class ValidationUtils:
    # Should this require a study directory or individual file paths?
    def __init__(self):
        self.patient_file_validator = None
        self.sample_file_validator = SampleFileValidator("file_path")

    def validate_patient_file(self):
        self.patient_file_validator.validate()

    def validate_sample_file(self):
        self.sample_file_validator.validate()

    # Define other file type validation functions here

    def validate(self):
        self.validate_patient_file() 
        self.validate_sample_file()

        # Call other file type validation functions here


class FileValidator(ABC):
    def __init__(self, file_path):
        self.file_path = file_path
        self.df = None

    def validate_file_exists(self):
        return os.path.exists(self.file_path)
    
    # Define other base-level file checks here
    
    # TODO change args to **opts?
    def load_file(self, sep="\t", float_precision="round_trip", na_filter=False, low_memory=False):
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
            float_precision=float_precision,
            na_filter=na_filter,
            low_memory=low_memory,
            skiprows=start_read
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

        # TODO return values vv ?

        # Load file contents into dataframe
        self.load_file()
        
        # Run all validation checks
        self.run_validate_checks()

        # Write out validated data
        self.write_to_file()

        # Clear df from memory
        self.clear_df()

    def write_to_file(self, **opts): # TODO add quote opt somewhere ?
        opts["index"] = opts.get(
            "index", False
        )  # by default, don't write the index column to the output
        self.df.to_csv(
            self.file_path,
            sep="\t",
            **opts,
        )

    def clear_df(self):
        del self.df


class SampleFileValidator(FileValidator):
    def __init__(self, file_path):
        super().__init__(file_path)
    
    def validate_sids_match_pids(self):
        non_matching = self.df.query(self.df["PATIENT_ID"] != self.df["SAMPLE_ID"].split("-")[0])
        self.df.drop(non_matching.index)

        logging.warn(f"The following {non_matching.size} records were dropped due to mismatched patient and sample IDs: {non_matching}")

    # TODO options to run specific checks?
    def run_validate_checks(self):
        self.validate_sids_match_pids()
