#!/usr/bin/env python3

"""Changelog Summary Generator

This script generates a summary of changes made to the clinical patient and
sample files for MSK Impact in the most recent update to the study. Note that
this script has been written specifically for the MSK Impact study and has not
been tested for use with other studies.

The script compares two versions of each clinical file - a "previous" version
(v1) and a "current" version (v2) - and reports the patients and samples that
were added, deleted, or modified between them. Rows are matched by primary key
(PATIENT_ID for the patient file, SAMPLE_ID for the sample file), so reordering
rows within a file is not reported as a change.

This script requires that `pandas` be installed within the Python environment
you are running this script in.

Usage:
    python3 generate_az_study_changelog_py3.py \
        --current-patient $CURRENT_PATIENT_FILE \
        --current-sample $CURRENT_SAMPLE_FILE \
        --previous-patient $PREVIOUS_PATIENT_FILE \
        --previous-sample $PREVIOUS_SAMPLE_FILE \
        --output-filename $OUTPUT_FILENAME \
        --output-dir $OUTPUT_DIR

Example:
    python3 generate_az_study_changelog_py3.py \
        --current-patient /path/to/current/data_clinical_patient.txt \
        --current-sample /path/to/current/data_clinical_sample.txt \
        --previous-patient /path/to/previous/data_clinical_patient.txt \
        --previous-sample /path/to/previous/data_clinical_sample.txt

When `--previous-patient` / `--previous-sample` are omitted, every patient and
sample in the current files is reported as new (use this for a brand new study).

When `--output-filename` and `--output-dir` are not provided, the summary file
is written to `<dir of --current-patient>/changelog_summary.txt` by default.

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
import pandas as pd


def read_clinical_file(path):
    """Reads a clinical (patient or sample) data file into a DataFrame.

    All values are read as strings and missing cells are normalized to the empty
    string so that cell-by-cell comparison between two versions of a file is
    reliable (no float formatting drift, consistent handling of blank cells).

    Args:
        path (string or None): Path to the clinical data file, or None

    Returns:
        DataFrame or None: The parsed file, or None if `path` is None
    """
    if path is None:
        return None
    return pd.read_csv(path, sep='\t', comment='#', dtype=str).fillna('')


def diff_records(previous_df, current_df, key):
    """Compares two versions of a clinical data file, keyed by `key`.

    Args:
        previous_df (DataFrame or None): The previous (v1) version, or None
        current_df (DataFrame): The current (v2) version
        key (string): Name of the primary key column (unique within each file)

    Returns:
        (set, set, set):
            added: key values present in v2 but not v1
            deleted: key values present in v1 but not v2
            modified: key values present in both, with a differing value in at
                least one column shared by both versions
    """
    current_indexed = current_df.set_index(key)
    current_ids = set(current_indexed.index)

    # No previous version - treat every current record as newly added
    if previous_df is None:
        return current_ids, set(), set()

    previous_indexed = previous_df.set_index(key)
    previous_ids = set(previous_indexed.index)

    added = current_ids - previous_ids
    deleted = previous_ids - current_ids
    common = sorted(current_ids & previous_ids)

    modified = set()
    if common:
        shared_cols = [col for col in current_indexed.columns if col in previous_indexed.columns]
        previous_common = previous_indexed.loc[common, shared_cols]
        current_common = current_indexed.loc[common, shared_cols]

        # Row-wise comparison over shared columns; .values keeps this positional
        # so column ordering differences between versions don't matter
        row_changed = (previous_common.values != current_common.values).any(axis=1)
        modified = {key_value for key_value, changed in zip(common, row_changed) if changed}

    return added, deleted, modified


class DataHandler:
    """Generic data handler class. Provides helper functions for reading
    clinical patient and sample data files.

    Kept for use by other import scripts (filter_non_somatic_events_py3.py,
    anonymize_age_at_seq_with_cap_py3.py) that import it for get_col_indices."""

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


class ClinicalFileComparer:
    """Loads the previous (v1) and current (v2) versions of a clinical data file
    for the changelog subclasses to compare."""

    def __init__(self, previous_path, current_path):
        self.previous_path = previous_path
        self.current_path = current_path
        self.previous_df = read_clinical_file(previous_path)
        self.current_df = read_clinical_file(current_path)


class PatientDataHandler(ClinicalFileComparer):
    """Handles reading and processing of clinical patient data."""

    def __init__(self, previous_path, current_path):
        super().__init__(previous_path, current_path)

        # Total number of patients in the study
        self.total_patient_count = 0

        # Disjoint sets containing patient IDs of added, deleted, and modified patients, respectively
        self.added_patient_ids = set()
        self.deleted_patient_ids = set()
        self.modified_patient_ids = set()

    def get_modified_patient_ids(self):
        """Provides IDs of patients that were modified between the previous and
        current versions of the clinical patient file.

        Returns:
            set: Set of modified patient IDs
        """
        return self.modified_patient_ids

    def process_patient_data(self):
        """Compares the previous and current clinical patient files. Stores the patient IDs
        of all patients that were added, deleted, or modified, along with the total patient count.
        """
        # Store total number of patients from the current patient file
        self.total_patient_count = len(self.current_df)

        # Compare the two versions of the file, keyed by PATIENT_ID
        (
            self.added_patient_ids,
            self.deleted_patient_ids,
            self.modified_patient_ids,
        ) = diff_records(self.previous_df, self.current_df, 'PATIENT_ID')


class Sample:
    """Stores relevant data associated with a sample from the clinical sample file."""

    def __init__(self, patient_id, cancer_type='', sample_type='', sample_class=''):
        self.patient_id = patient_id
        self.cancer_type = cancer_type
        self.sample_type = sample_type
        self.sample_class = sample_class


class SampleDataHandler(ClinicalFileComparer):
    """Handles reading and processing of clinical sample data."""

    def __init__(self, previous_path, current_path):
        super().__init__(previous_path, current_path)

        # Will store 3 columns for each sample from the current sample file:
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

        # Sample rows from the current (v2) and previous (v1) versions of the file.
        # A modified sample appears in both dicts (its v2 row in curr_samples and
        # its v1 row in prev_samples).
        # <sample_id> -> Sample obj
        self.curr_samples = {}
        self.prev_samples = {}

        # Dicts containing samples that were added, deleted, or modified, respectively
        # <sample_id> -> Sample obj
        self.added_samples = {}
        self.deleted_samples = {}
        self.modified_samples = {}

    def populate_sample_set(self):
        """Reads the current clinical sample file. Stores the total number of patients
        and samples per cancer type.
        """
        # Keep just the columns needed for per-cancer-type aggregation
        self.sample_df = self.current_df[['PATIENT_ID', 'SAMPLE_ID', 'CANCER_TYPE']].copy()

        # Blank cancer type values are labeled so they aggregate together
        self.sample_df['CANCER_TYPE'] = self.sample_df['CANCER_TYPE'].replace('', self.unknown_cancer_type_label)

        # Get the number of patients + samples for each cancer type
        self.cancer_type_to_patient_count = self.sample_df.groupby("CANCER_TYPE")["PATIENT_ID"].nunique().to_dict()
        self.cancer_type_to_sample_count = self.sample_df.groupby("CANCER_TYPE")["SAMPLE_ID"].nunique().to_dict()

    def _row_to_sample(self, row):
        """Builds a Sample object from a clinical sample file row.

        Args:
            row (Series): A row from the sample DataFrame, indexed by column name

        Returns:
            Sample: Object of the Sample class
        """
        cancer_type = row['CANCER_TYPE'] if row['CANCER_TYPE'] else self.unknown_cancer_type_label
        return Sample(
            row['PATIENT_ID'],
            cancer_type=cancer_type,
            sample_type=row['SAMPLE_TYPE'],
            sample_class=row['SAMPLE_CLASS'],
        )

    def process_sample_data(self):
        """Compares the previous and current clinical sample files. Stores the sample IDs
        of all samples that were added, deleted, or modified.
        """
        self.populate_sample_set()

        # Compare the two versions of the file, keyed by SAMPLE_ID
        added, deleted, modified = diff_records(self.previous_df, self.current_df, 'SAMPLE_ID')

        # Capture the current row for every added or modified sample
        current_indexed = self.current_df.set_index('SAMPLE_ID')
        for sample_id in added | modified:
            self.curr_samples[sample_id] = self._row_to_sample(current_indexed.loc[sample_id])

        # Capture the previous row for every deleted or modified sample
        if self.previous_df is not None:
            previous_indexed = self.previous_df.set_index('SAMPLE_ID')
            for sample_id in deleted | modified:
                self.prev_samples[sample_id] = self._row_to_sample(previous_indexed.loc[sample_id])

        self.added_samples = {sample_id: self.curr_samples[sample_id] for sample_id in added}
        self.deleted_samples = {sample_id: self.prev_samples[sample_id] for sample_id in deleted}
        self.modified_samples = {sample_id: self.curr_samples[sample_id] for sample_id in modified}

    def get_modified_sample_ids(self):
        """Provides IDs of samples that were modified between the previous and
        current versions of the clinical sample file.

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
        if sample_id not in self.curr_samples or sample_id not in self.prev_samples:
            return False

        return self.curr_samples[sample_id].cancer_type != self.prev_samples[sample_id].cancer_type

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

    def __init__(self, previous_patient_path, current_patient_path, previous_sample_path, current_sample_path):
        self.patient_data_handler = PatientDataHandler(previous_patient_path, current_patient_path)
        self.sample_data_handler = SampleDataHandler(previous_sample_path, current_sample_path)

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
        """Needed for unit tests. Returns the number of modified patients
        between the previous and current versions of the clinical patient file,
        where "modified" refers to a patient whose attributes have been changed/updated.

        Returns:
            int: Number of modified patients
        """
        return len(self.patient_data_handler.get_modified_patient_ids())

    def get_num_modified_samples(self):
        """Needed for unit tests. Returns the number of modified samples
        between the previous and current versions of the clinical sample file,
        where "modified" refers to a sample whose attributes have been changed/updated.

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

        # Get previous cancer type from the previous version of the sample file
        prev_cancer_type = self.sample_data_handler.prev_samples[sample_id].cancer_type

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
    parser.add_argument(
        '--current-patient',
        dest='current_patient',
        required=True,
        help='Path to the current (v2) clinical patient file',
    )
    parser.add_argument(
        '--current-sample',
        dest='current_sample',
        required=True,
        help='Path to the current (v2) clinical sample file',
    )
    parser.add_argument(
        '--previous-patient',
        dest='previous_patient',
        default=None,
        help='Path to the previous (v1) clinical patient file. Omit for a brand new study.',
    )
    parser.add_argument(
        '--previous-sample',
        dest='previous_sample',
        default=None,
        help='Path to the previous (v1) clinical sample file. Omit for a brand new study.',
    )
    parser.add_argument(
        '--output-dir',
        '-d',
        dest='output_dir',
        help='Optional argument to specify output directory. Defaults to the directory of --current-patient',
    )
    parser.add_argument(
        '--output-filename',
        '-f',
        dest='output_filename',
        default='changelog_summary.txt',
        help='Optional argument to specify output filename. Defaults to \'changelog_summary.txt\'',
    )

    args = parser.parse_args()

    # Store absolute paths to the clinical data files
    current_patient_path = os.path.abspath(args.current_patient)
    current_sample_path = os.path.abspath(args.current_sample)
    previous_patient_path = os.path.abspath(args.previous_patient) if args.previous_patient else None
    previous_sample_path = os.path.abspath(args.previous_sample) if args.previous_sample else None

    # The previous patient and sample files must be provided together
    if (previous_patient_path is None) != (previous_sample_path is None):
        parser.error('--previous-patient and --previous-sample must be provided together')

    # Ensure that the provided data files exist
    required_files = [
        ('Current patient', current_patient_path),
        ('Current sample', current_sample_path),
    ]
    optional_files = [
        ('Previous patient', previous_patient_path),
        ('Previous sample', previous_sample_path),
    ]
    for label, path in required_files:
        if not os.path.exists(path):
            raise FileNotFoundError(f'{label} data file not found at {path}')
    for label, path in optional_files:
        if path is not None and not os.path.exists(path):
            raise FileNotFoundError(f'{label} data file not found at {path}')

    # ---------------------------------------------------------------------------------

    # If provided, create the output directory; otherwise write next to the current patient file
    if args.output_dir is not None:
        output_dir = os.path.abspath(args.output_dir)
        os.makedirs(output_dir, exist_ok=True)
    else:
        output_dir = os.path.dirname(current_patient_path)

    output_path = os.path.join(output_dir, args.output_filename)

    # Generate the changelog file for the given data
    changelog_generator = Changelog(
        previous_patient_path,
        current_patient_path,
        previous_sample_path,
        current_sample_path,
    )
    changelog_generator.generate_changelog(output_path)
