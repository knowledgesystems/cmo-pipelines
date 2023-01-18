#!/usr/bin/env python3

""" anonymize_age_at_seq_with_cap_py3.py
This script reads clinical data files containing attributes AGE_CURRENT (patient), OS_MONTHS (patient),
and AGE_AT_SEQUENCING_REPORTED_YEARS (sample). It will alter the value of AGE_CURRENT so that it does
not exceed the upper age limit provided. It will alter the value of OS_MONTHS to meet a specified decimal
precision limit. It will also alter AGE_AT_SEQUENCING_REPORTED_YEARS so that it never exceeds the upper
age limit and that the sum of AGE_AT_SEQUENCING_REPORTED_YEARS + OS_MONTHS does not exceed the limit.

Usage:
    python3 anonymize_age_at_seq_with_cap_py3.py $INPUT_PATIENT_FILE_PATH $OUTPUT_PATIENT_FILE_PATH $INPUT_SAMPLE_FILE_PATH $OUTPUT_SAMPLE_FILE_PATH [ --upper-age-limit $UPPER_AGE_LIMIT ] [ --os-months-precision $OS_MONTHS_PRECISION ]
Example:
    python3 anonymize_age_at_seq_with_cap_py3.py \
            path/to/az_mskimpact/data_clinical_patient.txt \
            path/to/az_mskimpact/data_clinical_patient_output.txt \
            path/to/az_mskimpact/data_clinical_sample.txt \
            path/to/az_mskimpact/data_clinical_sample_output.txt \
            --upper-age-limit 89
            --os-months-precision 2
"""

import math
import argparse
from generate_az_study_changelog_py3 import DataHandler


class LineProcessor:
    """Base LineProcessor class"""

    def __init__(self, patient_os_years_map, col_indices, output_file_handle):
        self.patient_os_years_map = patient_os_years_map
        self.col_indices = col_indices
        self.output_file_handle = output_file_handle
        self.header_was_written = False

    def line_is_commented(self, line):
        """Determines if the given line in the file is a comment.

        Args:
            line (string): A line from the input file

        Returns:
            boolean: True or False, indicating whether the line is a comment
        """
        return line.strip()[0] == '#'

    def find_required_column_index(self, col_indices, attribute_name, kind_of_file):
        """Checks if a required column is found in map of attribute_name -> column_index

        Args:
            col_indices (dict): Map of attribute_name -> column_index
            attribute_name (string): Name of attribute
            kind_of_file (string): Description of filetype, used in error message

        Raises:
            IndexError: When attribute_name not found in col_indices

        Returns:
            int: Index of the required column, attribute_name, in the file
        """
        if attribute_name not in col_indices:
            raise IndexError('Unable to find ' + attribute_name + ' column in ' + kind_of_file + ' file')
        return col_indices[attribute_name]

    def write_data_line(self, cols):
        """Writes a tab delimited line of data.

        Args:
            cols (list): List containing the contents of the line we want to write out in a tab-delimited format.
        """
        self.output_file_handle.write('\t'.join(cols) + '\n')


class PatientLineProcessor(LineProcessor):
    """Maps PATIENT_ID to OS_YEARS, truncates OS_MONTHS attribute to a specified decimal precision,
    and caps AGE_CURRENT according to limits"""

    def __init__(
        self,
        patient_os_years_map,
        col_indices,
        output_file_handle,
        upper_age_limit,
        os_months_precision,
    ):
        super().__init__(patient_os_years_map, col_indices, output_file_handle)
        self.upper_age_limit = upper_age_limit
        self.os_months_precision = os_months_precision

    def apply_age_current_limit(self, cols, patient_id_col_index, age_current_col_index):
        """Applies age limits to the 'AGE_CURRENT' column:
            If 'AGE_CURRENT' > upper_age_limit, this function will overwrite the value of
                'AGE_CURRENT' to '>{upper_age_limit}'.

        Args:
            cols (array of string): The column values from a line from the clinical patient file
            patient_id_col_index (int): Index of the 'PATIENT_ID' column
            age_current_col_index (int): Index of the 'AGE_CURRENT' column
        """
        patient_id_value = cols[patient_id_col_index]
        age_current_value = cols[age_current_col_index].strip()

        # Don't process blank age values
        if not age_current_value:
            return

        # Handle age values that are already capped
        if age_current_value[0] == '>':
            age_current_value = age_current_value[1:]

        # Convert age to an integer
        try:
            age_current_value = int(age_current_value)
        except ValueError:
            # Leave non-numeric values alone (perhaps NA)
            return

        # Apply age limit if applicable
        if age_current_value > self.upper_age_limit:
            cols[age_current_col_index] = '>' + str(self.upper_age_limit)

    def apply_os_months_precision_limit(self, cols, os_months_col_index, os_months_precision):
        """Truncates OS_MONTHS to specified decimal precision (os_months_precision).

        Args:
            cols (array of string): The column values from a line from the clinical patient file
            os_months_col_index (int): Index of the 'OS_MONTHS' column
            os_months_precision (int): Max number of decimal digits that 'OS_MONTHS' value should contain
        """
        os_months_value = cols[os_months_col_index]

        # Don't process non-numeric values
        try:
            os_months_value = float(os_months_value)
        except ValueError:
            return

        # Output to specified precision
        cols[os_months_col_index] = f'{os_months_value:.{os_months_precision}f}'

    def map_patient_id_to_os_years(self, cols, patient_id_col_index, os_months_col_index):
        """Computes os_years from os_months and adds the value to patient_os_years_map,
        which maps PATIENT_ID -> OS_YEARS.

        Args:
            cols (array of string): The column values from a line from the clinical patient file
            patient_id_col_index (int): Index of the 'PATIENT_ID' column
            os_months_col_index (int): Index of the 'OS_MONTHS' column
        """
        patient_id_value = cols[patient_id_col_index]
        os_months_value = cols[os_months_col_index]

        try:
            os_months_value = float(os_months_value)
        except ValueError:
            # Non-numeric os_month values are treated as zero
            os_months_value = 0

        self.patient_os_years_map[patient_id_value] = math.trunc(os_months_value / 12.0)

    def process(self, line):
        """Process each line, constructs map of PATIENT_ID to OS_YEARS, applies age caps to AGE_CURRENT,
        and applies precision limit to OS_MONTHS.

        Args:
            line (string): A line from the clinical patient file

        Raises:
            IndexError: If 'PATIENT_ID' column is not found in clinical patient file
            IndexError: If 'OS_MONTHS' column is not found in clinical patient file
        """
        if self.line_is_commented(line):
            self.output_file_handle.write(line)
            return

        if not self.header_was_written:
            self.output_file_handle.write(line)
            self.header_was_written = True
            return

        patient_id_col_index = self.find_required_column_index(
            self.col_indices, 'PATIENT_ID', 'clinical patient'
        )
        os_months_col_index = self.find_required_column_index(
            self.col_indices, 'OS_MONTHS', 'clinical patient'
        )
        age_current_col_index = self.find_required_column_index(
            self.col_indices, 'AGE_CURRENT', 'clinical patient'
        )
        cols = line.rstrip('\n').split('\t')
        self.map_patient_id_to_os_years(cols, patient_id_col_index, os_months_col_index)
        self.apply_age_current_limit(cols, patient_id_col_index, age_current_col_index)
        self.apply_os_months_precision_limit(cols, os_months_col_index, self.os_months_precision)
        self.write_data_line(cols)


class PatientFileWriter:
    """Reads the patient clincal file, constructs a map from patient_id to os_years, applies age limits to AGE_CURRENT,
    and writes the clinical patient file with os_months_precision"""

    def __init__(self, input_file_path, output_file_path, upper_age_limit, os_months_precision):
        self.input_file_path = input_file_path
        self.output_file_path = output_file_path
        self.upper_age_limit = upper_age_limit
        self.os_months_precision = os_months_precision
        self.data_handler = DataHandler(input_file_path)
        self.col_indices = self.data_handler.get_col_indices({'PATIENT_ID', 'OS_MONTHS', 'AGE_CURRENT'})
        self.patient_os_years_map = {}

    def write(self):
        """Reads the patient clincal file, constructs a map from patient_id to os_years, applies age limits to AGE_CURRENT,
        and writes the clinical patient file with os_months_precision"""
        with open(self.input_file_path, 'r') as input_file_handle:
            with open(self.output_file_path, 'w') as output_file_handle:
                line_reader = PatientLineProcessor(
                    self.patient_os_years_map,
                    self.col_indices,
                    output_file_handle,
                    self.upper_age_limit,
                    self.os_months_precision,
                )
                for line in input_file_handle:
                    line_reader.process(line)


class SampleLineProcessor(LineProcessor):
    """Handles the processing of each line - capping ages according to limits"""

    def __init__(self, patient_os_years_map, col_indices, output_file_handle, upper_age_limit):
        super().__init__(patient_os_years_map, col_indices, output_file_handle)
        self.upper_age_limit = upper_age_limit

    def apply_age_column_limits(self, cols, age_col_index, patient_id_col_index):
        """Applies age limits to the 'AGE_AT_SEQ_REPORTED_YEARS' column:
            If 'AGE_AT_SEQ_REPORTED_YEARS' + OS_MONTHS (in years) is > upper_age_limit, this function will
                overwrite the value of 'AGE_AT_SEQ_REPORTED_YEARS' to upper_age_limit - OS_MONTHS (in years).

        Args:
            cols (list): Data values from the given line of the clinical sample file
            age_col_index (int): Index of the 'AGE_AT_SEQ_REPORTED_YEARS' column
            patient_id_col_index (int): Index of the 'PATIENT_ID' column

        Raises:
            IndexError: When a patient in the clinical sample file does not have a corresponding entry in the clinical patient file
        """
        age_value = cols[age_col_index].strip()
        patient_id_value = cols[patient_id_col_index]

        # Don't process blank age values
        if not age_value:
            return

        # Handle age values that are already capped
        if age_value[0] == '>':
            age_value = age_value[1:]

        # Convert age to an integer
        try:
            age_value = int(age_value)
        except ValueError:
            # Leave non-numeric values alone (perhaps NA)
            return

        if not patient_id_value in self.patient_os_years_map:
            raise IndexError(
                'Unable to find OS_MONTHS value for patient '
                + patient_id_value
                + ' in the patient clinical file even though the patient is mentioned in the sample clinical file'
            )

        # Apply age limit if applicable
        os_years_value = self.patient_os_years_map[patient_id_value]
        adjusted_age_value = age_value + os_years_value
        if adjusted_age_value > self.upper_age_limit:
            output_age_inequality_value = self.upper_age_limit - os_years_value
            output_age_inequality_value = max(output_age_inequality_value, 0)
            cols[age_col_index] = '>' + str(output_age_inequality_value)

    def process(self, line):
        """Process each line and apply relevant age limits.

        Args:
            line (string): A line from the input file

        Raises:
            IndexError: If 'AGE_AT_SEQ_REPORTED_YEARS' column is not found in clinical sample file
            IndexError: If 'PATIENT_ID' column is not found in clinical patient file
        """
        if self.line_is_commented(line):
            self.output_file_handle.write(line)
            return

        if not self.header_was_written:
            self.output_file_handle.write(line)
            self.header_was_written = True
            return

        age_col_index = self.find_required_column_index(
            self.col_indices, 'AGE_AT_SEQ_REPORTED_YEARS', 'clinical sample'
        )
        patient_id_col_index = self.find_required_column_index(
            self.col_indices, 'PATIENT_ID', 'clinical patient'
        )
        cols = line.rstrip('\n').split('\t')
        self.apply_age_column_limits(cols, age_col_index, patient_id_col_index)
        self.write_data_line(cols)


class SampleFileWriter:
    """Reads the input sample file and writes the (output) age limited sample file"""

    def __init__(self, input_file_path, output_file_path, upper_age_limit, patient_os_years_map):
        self.input_file_path = input_file_path
        self.output_file_path = output_file_path
        self.upper_age_limit = upper_age_limit
        self.patient_os_years_map = patient_os_years_map
        self.data_handler = DataHandler(input_file_path)
        self.col_indices = self.data_handler.get_col_indices({'AGE_AT_SEQ_REPORTED_YEARS', 'PATIENT_ID'})

    def write(self):
        """Reads the input sample file and writes the (output) age limited sample file"""
        with open(self.input_file_path, 'r') as input_file_handle:
            with open(self.output_file_path, 'w') as output_file_handle:
                line_processor = SampleLineProcessor(
                    self.patient_os_years_map,
                    self.col_indices,
                    output_file_handle,
                    self.upper_age_limit,
                )
                for line in input_file_handle:
                    line_processor.process(line)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='transform a clinical sample file so that specified age limits are applied to age_at_seq_reported'
    )
    parser.add_argument('input_patient_file_path', help='Path to the input clinical patient file')
    parser.add_argument('output_patient_file_path', help='Path to which the output patient file is written')
    parser.add_argument('input_sample_file_path', help='Path to the input clinical sample file')
    parser.add_argument('output_sample_file_path', help='Path to which the output sample file is written')
    parser.add_argument(
        '--upper-age-limit',
        '-u',
        dest='upper_age_limit',
        help='ages above this (integer) limit will be replaced with ">{upper_age_limit}"',
        required=False,
        default=89,
        type=int,
    )
    parser.add_argument(
        '--os-months-precision',
        '-o',
        dest='os_months_precision',
        help='Digits of precision for which to specify OS_MONTHS attribute',
        required=False,
        default=2,
        type=int,
    )

    args = parser.parse_args()

    input_patient_file_path = args.input_patient_file_path
    output_patient_file_path = args.output_patient_file_path
    input_sample_file_path = args.input_sample_file_path
    output_sample_file_path = args.output_sample_file_path
    upper_age_limit = args.upper_age_limit
    os_months_precision = args.os_months_precision

    if upper_age_limit < 0:
        raise ValueError('upper_age_limit is negative')
    if os_months_precision < 0:
        raise ValueError('os_months_precision is negative')

    # Transform the patient file
    patient_file_reader = PatientFileWriter(
        input_patient_file_path,
        output_patient_file_path,
        upper_age_limit,
        os_months_precision,
    )
    patient_file_reader.write()

    # Transform the clinical sample file
    sample_file_writer = SampleFileWriter(
        input_sample_file_path,
        output_sample_file_path,
        upper_age_limit,
        patient_file_reader.patient_os_years_map,
    )
    sample_file_writer.write()
