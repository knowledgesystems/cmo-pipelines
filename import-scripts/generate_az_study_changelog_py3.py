#!/usr/bin/env python3

"""Changelog Summary Generator

This script generates a summary of changes made to the clinical patient and
sample files for MSK Impact in the most recent update to the study. Note that
this script has been written specifically for the MSK Impact study and has not
been tested for use with other studies.

For use with Git LFS files, the following value must be set in your git config
prior to running the script:
    git config diff.lfs.textconv cat

This script requires that `pandas` and `GitPython` be installed within the
Python environment you are running this script in.

Usage:
    python3 generate_az_study_changelog_py3.py $DATA_REPO_PATH \
        --output-filename $OUTPUT_FILENAME \
        --output-dir $OUTPUT_DIR

Example:
    python3 generate_az_study_changelog_py3.py /path/to/az_mskimpact/

When `--output-filename` and `--output-dir` are not provided, the summary file
is written to `$DATA_REPO_PATH/changelog_summary.txt` by default.

Sample output:

    Changelog Summary, 2022-11-21

    Total number of patients: 21
    New patients: 3
    Deleted patients: 0

    Total samples: 23
    New samples: 0
    Deleted samples: 0

    Bladder Cancer
        Total patients: 2
        New patients: 0
        Deleted patients: 0

        Total samples: 2
        New samples: 0
        Deleted samples: 0

    ...

Unit tests can be run with the following command:
    python -m unittest discover test-py3

"""

import os
import argparse
from collections import defaultdict
from datetime import datetime
import git
import pandas as pd


class DataHandler:
    """Generic data handler class. Provides helper functions for reading
    clinical patient and sample data files."""

    def __init__(self, data_path):
        self.data_path = data_path

    def get_col_indices(self, col_list):
        """Maps columns in the data file to integer indices, for use in later indexing.

        Args:
            col_list (list): Column names to search for the index of

        Returns:
            dict: Map of column names -> integer index of the column in the file
        """
        # Return dict of interested column name -> integer index of column
        ret_map = {}

        with open(self.data_path, 'r') as f:
            for line in f:
                # Ignore header lines
                if line[0] == '#':
                    continue

                col_names = [col.strip() for col in line.split('\t')]
                break

            for col_idx, col_name in enumerate(col_names):
                if col_name in col_list:
                    ret_map[col_name] = col_idx

        return ret_map

    def is_git_diff_header(self, line_tokens, mode):
        """Determines if the given git diff line belongs to the git diff header.

        Args:
            line_tokens (list): Tab delimited values from the git diff line
            mode (string): '+' or '-' as indicated by git diff

        Returns:
            boolean: True or False, indicating if the line belongs to the diff header
        """
        return len(line_tokens) == 1 or mode not in {'-', '+'}

    def is_commented_line(self, line_tokens):
        """Determines if the given git diff line is commented out and should not be processed.

        Args:
            line_tokens (list): Tab delimited values from the git diff line

        Returns:
            boolean: True or False, indicating if the line is commented
        """
        return line_tokens[0][0] == '#'

    def is_data_header_line(self, line_tokens):
        """Determines if the given git diff line contains column names for the data file.
        This line will be marked as 'added' on first commit, but we do not want to process it
        for the changelog summary.

        Args:
            line_tokens (list): Tab delimited values from the git diff line

        Returns:
            boolean: True or False, indicating if the line is the data header line
        """
        return 'PATIENT_ID' in line_tokens

    def next_git_diff_line(self):
        """Parses each line of git diff output.

        Yields:
            (string, list):
                string: '+' or '-', indicating whether git marked the line as added or deleted from the file
                list: String data values from the tab-delimited line
        """
        g = git.Git(os.path.dirname(self.data_path))

        # Add the file with -N flag (--intent-to-add), so that in the case of
        # a new file (untracked) we are still able to view the diff info
        g.add('-N', self.data_path)

        # Obtain git diff for the file
        diff_info = g.diff('--unified=0', '--', self.data_path)

        # Check if there is a git diff to process
        if not diff_info:
            return

        lines = diff_info.split('\n')
        for line in lines:
            tokens = line.split('\t')

            # Git prepends each line of git diff with a '+' or '-'
            # We will use this to correctly parse the changes to each line

            mode = tokens[0][0]
            tokens[0] = tokens[0][1:]

            # Only want to process the file contents (tab-delimited data)
            # Ignore commented lines - they are tab delimited, and will be marked as added on first commit
            # Ignore row that contains column names - this will be marked as added on first commit
            if self.is_git_diff_header(tokens, mode) or self.is_commented_line(tokens) or self.is_data_header_line(tokens):
                continue

            yield mode, tokens


class PatientDataHandler(DataHandler):
    """Handles reading and processing of clinical patient data. Inherits from the generic DataHandler class."""

    def __init__(self, data_path):
        super().__init__(data_path)

        # Total number of patients in the study
        self.total_patient_count = 0

        # Disjoint sets containing patient IDs of added, deleted, and modified patients, respectively
        self.added_patient_ids = set()
        self.deleted_patient_ids = set()
        self.modified_patient_ids = set()

    def get_modified_patient_ids(self):
        """Provides IDs of patients that were modified in the most recent
        git commit to the clinical patient file.

        Returns:
            DictView: View of keys from modified_patients dict
        """
        return self.modified_patient_ids

    def count_total_patients(self):
        """Uses pandas to read the clinical patient file. Stores total number of patients
        in class member set self.total_patients.
        """
        # Read relevant columns from patient file
        df = pd.read_csv(self.data_path, sep='\t', comment='#', usecols=['PATIENT_ID'])

        # Store total number of patients
        self.total_patient_count = len(df)

    def process_patient_data(self):
        """Processes git diff output for the clinical patient file. Stores the patient IDs
        of all patients that were added, deleted, or modified in the latest commit to the patient file.
        """
        # Get total count of patients from the patient file
        self.count_total_patients()

        # Process file and get column index for PATIENT_ID
        # We have to do this because we can't get this info from the git diff
        col_indices = self.get_col_indices({'PATIENT_ID'})

        # Keep track of patient IDs that are marked as added/deleted in git diff
        # We will use set arithmetic to separate out the modified patients after
        git_added_patient_ids = set()
        git_deleted_patient_ids = set()

        # Process each line of the git diff for new or deleted patients
        for mode, tokens in self.next_git_diff_line():
            patient_id = tokens[col_indices['PATIENT_ID']]

            # Add to relevant set depending on whether git has prepended the line with a + or -
            if mode == '-':
                git_deleted_patient_ids.add(patient_id)
            elif mode == '+':
                git_added_patient_ids.add(patient_id)

        # Use set arithmetic operations to determine which patients were modified, added, and deleted
        # NOTE: In the future, will want to keep track of whether patients were actually modified or just moved in the file
        self.modified_patient_ids = git_added_patient_ids.intersection(git_deleted_patient_ids)
        self.added_patient_ids = git_added_patient_ids.difference(git_deleted_patient_ids)
        self.deleted_patient_ids = git_deleted_patient_ids.difference(git_added_patient_ids)


class Sample:
    """Stores relevant data associated with a sample from the clinical sample file."""

    def __init__(self, patient_id, cancer_type='', sample_type='', sample_class=''):
        self.patient_id = patient_id
        self.cancer_type = cancer_type
        self.sample_type = sample_type
        self.sample_class = sample_class


class SampleDataHandler(DataHandler):
    """Handles reading and processing of clinical sample data. Inherits from the generic DataHandler class."""

    def __init__(self, data_path):
        super().__init__(data_path)

        # Will store 3 columns for each sample from the sample file:
        #   SAMPLE_ID
        #   PATIENT_ID
        #   CANCER_TYPE
        # Used to determine when a modified patient (for ex, a cancer type change) indicates a 'deleted patient'
        self.sample_df = pd.DataFrame()
        self.unknown_cancer_type_label = 'Unknown Cancer Type'

        # Dict of <cancer_type> -> int patient count
        self.cancer_type_to_patient_count = {}

        # Dict of <cancer_type> -> int sample count
        self.cancer_type_to_sample_count = {}

        # Samples marked as added or deleted by git diff
        # <sample_id> -> Sample obj
        self.git_added_samples = {}
        self.git_deleted_samples = {}

        # Dicts containing samples that were added, deleted, or modified, respectively
        # <sample_id> -> Sample obj
        self.added_samples = {}
        self.deleted_samples = {}
        self.modified_samples = {}

    def populate_sample_set(self):
        """Uses pandas to read the clinical sample file. Stores the total number of samples along with
        the total number of patients and samples per cancer type.
        """
        # Read relevant columns from patient file
        self.sample_df = pd.read_csv(
            self.data_path,
            sep='\t',
            comment='#',
            usecols=['PATIENT_ID', 'SAMPLE_ID', 'CANCER_TYPE'],
        )

        self.sample_df['CANCER_TYPE'].fillna(self.unknown_cancer_type_label, inplace=True)

        # Get the number of patients + samples for each cancer type
        self.cancer_type_to_patient_count = self.sample_df.groupby("CANCER_TYPE")["PATIENT_ID"].nunique().to_dict()
        self.cancer_type_to_sample_count = self.sample_df.groupby("CANCER_TYPE")["SAMPLE_ID"].nunique().to_dict()

    def process_sample_data(self):
        """Processes git diff output for the clinical sample file. Stores the sample IDs
        of all samples that were added, deleted, or modified in the latest change to the sample file.
        """
        self.populate_sample_set()

        # Process header and get indices for needed data columns
        col_names = {
            'PATIENT_ID',
            'SAMPLE_ID',
            'CANCER_TYPE',
            'SAMPLE_TYPE',
            'SAMPLE_CLASS',
        }
        col_indices = self.get_col_indices(col_names)

        # Process each line of the git diff for new or deleted patients
        for mode, tokens in self.next_git_diff_line():
            patient_id = tokens[col_indices['PATIENT_ID']]
            sample_id = tokens[col_indices['SAMPLE_ID']]
            cancer_type = (
                tokens[col_indices['CANCER_TYPE']]
                if tokens[col_indices['CANCER_TYPE']]
                else self.unknown_cancer_type_label
            )
            sample_type = tokens[col_indices['SAMPLE_TYPE']]
            sample_class = tokens[col_indices['SAMPLE_CLASS']]
            current_sample = Sample(
                patient_id,
                cancer_type=cancer_type,
                sample_type=sample_type,
                sample_class=sample_class,
            )

            if mode == '-':
                self.git_deleted_samples[sample_id] = current_sample
            elif mode == '+':
                self.git_added_samples[sample_id] = current_sample

        # Store git added/deleted IDs in sets for use in dict comprehension below
        git_deleted_sample_ids = set(self.git_deleted_samples.keys())
        git_added_sample_ids = set(self.git_added_samples.keys())

        # NOTE: In the future, will want to keep track of whether samples were actually modified or just moved in the file
        self.modified_samples = {
            k: self.git_added_samples[k] for k in git_added_sample_ids.intersection(git_deleted_sample_ids)
        }
        self.added_samples = {
            k: self.git_added_samples[k] for k in git_added_sample_ids.difference(git_deleted_sample_ids)
        }
        self.deleted_samples = {
            k: self.git_deleted_samples[k] for k in git_deleted_sample_ids.difference(git_added_sample_ids)
        }

    def get_modified_sample_ids(self):
        """Provides IDs of samples that were modified in the most recent
        change to the clinical sample file.

        Returns:
            DictView: View of keys from modified_samples dict
        """
        return self.modified_samples.keys()

    def cancer_type_changed(self, sample_id):
        """Indicates whether the cancer type for a given sample has changed.

        Args:
            sample_id (string): The unique identifier representing a certain sample

        Returns:
            boolean: True if the cancer type has changed, False otherwise
        """
        if sample_id not in self.git_added_samples or sample_id not in self.git_deleted_samples:
            return False

        return self.git_added_samples[sample_id].cancer_type != self.git_deleted_samples[sample_id].cancer_type

    def patient_new_for_cancer_type(self, sample, sample_id):
        """Determine if a patient should be marked as "new" for the cancer type associated with
        the current sample

        Args:
            sample (Sample): An object of the Sample class
            sample_id (string): The unique identifier representing a certain sample

        Returns:
            boolean: True or False, indicating whether the patient is new for the cancer type
        """
        # Determine if patient is new for the cancer type by checking other samples associated with this patient + cancer type
        samples_for_cancer_type = self.samples_for_patient_and_cancer_type(sample.patient_id, sample.cancer_type)
        samples_for_cancer_type.discard(sample_id)

        # If any of the other samples associated with this patient + cancer type
        # are not from cancer type changes, then the patient is not new for this cancer type
        for other_sample_id in samples_for_cancer_type:
            if not self.cancer_type_changed(other_sample_id):
                return False

        return True

    def samples_for_patient_and_cancer_type(self, patient_id, cancer_type):
        """Returns the a set of sample IDs for a given patient of a given cancer type.

        Args:
            patient_id (string): Patient ID of the given patient
            cancer_type (string): Type of cancer

        Returns:
            set: Set of sample IDs for the patient of the given cancer type
        """

        samples_for_patient_cancer_type = self.sample_df.loc[
            (self.sample_df['PATIENT_ID'] == patient_id) & (self.sample_df['CANCER_TYPE'] == cancer_type)
        ]

        return set(samples_for_patient_cancer_type['SAMPLE_ID'])


class CancerTypeAggregated:
    """Stores aggregated patient and sample data for a cancer type."""

    def __init__(self):
        # These need to be sets so that new and deleted patients aren't double counted
        # (since patients can have multiple samples of same Cancer Type)
        self.total_patient_count = 0
        self.new_patients = set()
        self.deleted_patients = set()

        # Counts of new, deleted, and modified samples
        self.total_sample_count = 0
        self.new_sample_count = 0
        self.deleted_sample_count = 0

        # Map of sample type name -> total # of new samples of that type
        self.sample_type = defaultdict(int)

        # Map of sample class name -> total # of new samples of that class
        self.sample_class = defaultdict(int)


class Changelog:
    """The "driver" class for generating a changelog for the patient and sample files."""

    def __init__(self, patient_data_path, sample_data_path):
        self.patient_data_path = patient_data_path
        self.sample_data_path = sample_data_path

        self.patient_data_handler = PatientDataHandler(self.patient_data_path)
        self.sample_data_handler = SampleDataHandler(self.sample_data_path)

        # Will store data in the following format: <cancer_type> -> CancerTypeAggregated obj
        self.aggregated_data = defaultdict(CancerTypeAggregated)

    def generate_changelog(self, output_path):
        """Generates a summary changelog for the given study by processing patient data,
        processing sample data, aggregating the data by cancer type, and writing the aggregated
        data to an output file.

        Args:
            output_path (string): The path to the output file.
        """
        # Process patient data
        self.patient_data_handler.process_patient_data()

        # Process sample_data
        self.sample_data_handler.process_sample_data()

        # Organize data by cancer type
        self.aggregate_data()

        # Write output file
        self.write_output_data(output_path)

    def get_num_modified_patients(self):
        """Needed for unit tests. Returns the number of modified patients in
        the most recent change to the clinical patient file, where "modified"
        refers to a patient whose attributes have been changed/updated in the patient file.

        Returns:
            int: Number of modified patients
        """
        return len(self.patient_data_handler.get_modified_patient_ids())

    def get_num_modified_samples(self):
        """Needed for unit tests. Returns the number of modified samples in
        the most recent change to the clinical sample file, where "modified"
        refers to a sample whose attributes have been changed/updated in the sample file.

        Returns:
            int: Number of modified samples
        """
        return len(self.sample_data_handler.get_modified_sample_ids())

    def aggregate_new_patient(self, cancer_type, patient_id):
        """Aggregates a new patient for a given cancer type in the
        aggregate data structure.

        Args:
            cancer_type (string): Type of cancer
            patient_id (string): Patient ID of the given patient
        """
        self.aggregated_data[cancer_type].new_patients.add(patient_id)

    def aggregate_new_sample(self, sample):
        """Aggregates a new sample in the aggregate data structure using
        the sample's cancer type, sample type, and sample class attributes.

        Args:
            sample (Sample): Object of the Sample type
        """
        self.aggregated_data[sample.cancer_type].new_sample_count += 1
        self.aggregated_data[sample.cancer_type].sample_type[sample.sample_type] += 1
        self.aggregated_data[sample.cancer_type].sample_class[sample.sample_class] += 1

    def aggregate_deleted_patient(self, cancer_type, patient_id):
        """Aggregates a deleted patient for a given cancer type in the
        aggregate data structure.

        Args:
            cancer_type (string): Type of cancer
            patient_id (string): Patient ID of the given patient
        """
        self.aggregated_data[cancer_type].deleted_patients.add(patient_id)

    def aggregate_deleted_sample(self, cancer_type):
        """Aggregates a deleted sample in the aggregate data structure using
        the sample's cancer type.

        Args:
            cancer_type (string): Type of cancer
        """
        self.aggregated_data[cancer_type].deleted_sample_count += 1

    def process_new_sample(self, sample, sample_id):
        """Processes data from a new sample and aggregates by cancer type.

        Args:
            sample (Sample): An object of the Sample class (defined above)
        """
        # Check whether the patient associated with this sample is from a new patient
        # NOTE: A new patient with samples of multiple cancer types would show as new for each cancer type
        # NOTE: An existing patient with a sample of a new cancer type will be marked as new for this cancer type
        if (
            sample.patient_id in self.patient_data_handler.added_patient_ids
            or self.sample_data_handler.patient_new_for_cancer_type(sample, sample_id)
        ):
            self.aggregate_new_patient(sample.cancer_type, sample.patient_id)

        # Mark sample as a new sample for this cancer type
        self.aggregate_new_sample(sample)

    def process_deleted_sample(self, sample):
        """Processes data from a deleted sample and aggregates by cancer type.

        Args:
            sample  (Sample): An object of the Sample class (defined above)
        """
        # Note whether the patient associated with this sample is from a deleted patient
        # NOTE: A new patient with samples of multiple cancer types would show as deleted for each cancer type
        if sample.patient_id in self.patient_data_handler.deleted_patient_ids:
            self.aggregate_deleted_patient(sample.cancer_type, sample.patient_id)

        self.aggregate_deleted_sample(sample.cancer_type)

    def process_cancer_type_change(self, sample_id, sample):
        """Processes a cancer type change for a given sample.

        Args:
            sample_id (string): The Sample ID for the given sample
            sample (Sample): An object of the Sample class (defined above)
        """
        # Determine if patient is new for the cancer type by checking other
        # samples associated with the patient + cancer type
        patient_is_new_for_cancer_type = True
        if sample.patient_id in self.aggregated_data[sample.cancer_type].new_patients:
            patient_is_new_for_cancer_type = False
        else:
            patient_is_new_for_cancer_type = self.sample_data_handler.patient_new_for_cancer_type(sample, sample_id)

        # Mark patient and sample as new for current cancer type if appropriate
        if patient_is_new_for_cancer_type:
            self.aggregate_new_patient(sample.cancer_type, sample.patient_id)
        self.aggregate_new_sample(sample)

        # ---------------------------------------------------------------

        # Get previous cancer type from the git diff
        prev_cancer_type = self.sample_data_handler.git_deleted_samples[sample_id].cancer_type

        # Determine if patient can was 'deleted' from previous cancer type
        # by checking if it has other samples with that cancer type
        patient_was_deleted_from_prev_cancer_type = (
            len(self.sample_data_handler.samples_for_patient_and_cancer_type(sample.patient_id, prev_cancer_type)) == 0
        )

        # Mark patient and sample as removed from prev cancer type if appropriate
        if patient_was_deleted_from_prev_cancer_type:
            self.aggregate_deleted_patient(prev_cancer_type, sample.patient_id)
        self.aggregate_deleted_sample(prev_cancer_type)

    def aggregate_data(self):
        """Aggregates new, deleted, and modified patients and samples by cancer type."""
        # Aggregate new samples
        for sample_id, sample in self.sample_data_handler.added_samples.items():
            self.process_new_sample(sample, sample_id)

        # Aggregate deleted samples
        for sample in self.sample_data_handler.deleted_samples.values():
            self.process_deleted_sample(sample)

        # Aggregate total number of samples per cancer type
        for (
            cancer_type,
            sample_count,
        ) in self.sample_data_handler.cancer_type_to_sample_count.items():
            self.aggregated_data[cancer_type].total_sample_count = sample_count

        # Aggregate total number of patients per cancer type
        for (
            cancer_type,
            patient_count,
        ) in self.sample_data_handler.cancer_type_to_patient_count.items():
            self.aggregated_data[cancer_type].total_patient_count = patient_count

        # Aggregate modified samples
        for sample_id, sample in self.sample_data_handler.modified_samples.items():
            # Check whether the cancer type has changed
            if self.sample_data_handler.cancer_type_changed(sample_id):
                self.process_cancer_type_change(sample_id, sample)

    def write_output_data(self, output_path):
        """Writes out patient and sample data (aggregated by cancer type) to a file.

        Args:
            output_path (string): The path to the output file.
        """
        f = open(output_path, 'w')

        todays_date = datetime.today().strftime('%Y-%m-%d')
        f.write(f'Changelog Summary, {todays_date}\n\n')

        f.write(f'Total patients: {self.patient_data_handler.total_patient_count}\n')
        f.write(f'New patients: {len(self.patient_data_handler.added_patient_ids)}\n')
        f.write(f'Deleted patients: {len(self.patient_data_handler.deleted_patient_ids)}\n\n')

        f.write(f'Total samples: {len(self.sample_data_handler.sample_df)}\n')
        f.write(f'New samples: {len(self.sample_data_handler.added_samples)}\n')
        f.write(f'Deleted samples: {len(self.sample_data_handler.deleted_samples)}')

        for cancer_type, data in sorted(self.aggregated_data.items()):
            f.write(f'\n\n{cancer_type}\n')

            f.write(f'\tTotal patients: {data.total_patient_count}\n')
            f.write(f'\tNew patients: {len(data.new_patients)}\n')
            f.write(f'\tDeleted patients: {len(data.deleted_patients)}\n\n')

            f.write(f'\tTotal samples: {data.total_sample_count}\n')
            f.write(f'\tNew samples: {data.new_sample_count}\n')

            if data.sample_type:
                f.write('\t\tSample type:\n')
            for sample_type, num_sample_type in sorted(data.sample_type.items()):
                f.write(f'\t\t\t{sample_type}: {num_sample_type}\n')

            if data.sample_class:
                f.write('\t\tSample class:\n')
            for sample_class, num_sample_class in sorted(data.sample_class.items()):
                f.write(f'\t\t\t{sample_class}: {num_sample_class}\n')

            f.write(f'\tDeleted samples: {data.deleted_sample_count}')

        f.write('\n')
        f.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate changelog summary for new clinical patient and sample data')
    parser.add_argument('data_repo_path', help='Path to location of data repository')
    parser.add_argument(
        '--output-dir',
        '-d',
        dest='output_dir',
        help='Optional argument to specify output directory. If not provided, output will be written to data_repo_path',
    )
    parser.add_argument(
        '--output-filename',
        '-f',
        dest='output_filename',
        default='changelog_summary.txt',
        help='Optional argument to specify output filename. Defaults to \'changelog_summary.txt\'',
    )

    args = parser.parse_args()

    data_repo_path = args.data_repo_path
    output_dir = args.output_dir
    output_filename = args.output_filename

    # Store absolute path to data repository
    data_repo_path = os.path.abspath(data_repo_path)

    # Ensure that data repository exists
    if not os.path.exists(data_repo_path):
        raise FileNotFoundError(f'Data repository directory not found at {data_repo_path}')

    # Ensure that patient and sample data files exist for the given study
    patient_data_path = os.path.join(data_repo_path, 'data_clinical_patient.txt')
    sample_data_path = os.path.join(data_repo_path, 'data_clinical_sample.txt')

    if not os.path.exists(patient_data_path):
        raise FileNotFoundError(f'Patient data not found at {patient_data_path}')

    if not os.path.exists(sample_data_path):
        raise FileNotFoundError(f'Sample data not found at {sample_data_path}')

    # ---------------------------------------------------------------------------------

    # If provided, create the output directory
    if output_dir is not None:
        out_dir = os.path.abspath(output_dir)

        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
    # Else the output directory will be the data repo path
    else:
        output_dir = data_repo_path

    output_path = os.path.join(output_dir, output_filename)

    # Generate the changelog file for the given data
    changelog_generator = Changelog(patient_data_path, sample_data_path)
    changelog_generator.generate_changelog(output_path)
