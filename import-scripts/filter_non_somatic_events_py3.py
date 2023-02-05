#!/usr/bin/env python3

"""Filter Non Somatic Events
This script reads a file of mutation data (data_mutations_extended.txt) or
structural variant data (data_sv.txt) and writes out a filtered copy of
the file where every event which is not noted as SOMATIC has been removed.

Usage:
    python3 filter_non_somatic_events_py3.py $INPUT_FILE_PATH $OUTPUT_FILE_PATH --event-type $FILE_TYPE \
Example:
    python3 filter_non_somatic_events_py3.py /path/to/az_mskimpact/data_mutation_extended.txt \
        /path/to/az_mskimpact/data_mutation_extended.txt.filtered --event-type mutation
"""

import argparse
from enum import Enum
from generate_az_study_changelog_py3 import DataHandler

UNIQUE_MUTATION_EVENT_KEY_FIELDS = {
    'Hugo_Symbol',
    'Chromosome',
    'Start_Position',
    'End_Position',
    'Reference_Allele',
    'Tumor_Seq_Allele2',
    'Tumor_Sample_Barcode',
}

UNIQUE_STRUCTURAL_VARIANT_EVENT_KEY_FIELDS = {
    'Sample_ID',
    'Site1_Hugo_Symbol',
    'Site2_Hugo_Symbol',
    'Site1_Entrez_Gene_Id',
    'Site2_Entrez_Gene_Id',
    'Site1_Region_Number',
    'Site2_Region_Number',
    'Site1_Region',
    'Site2_Region',
    'Site1_Chromosome',
    'Site2_Chromosome',
    'Site1_Contig',
    'Site2_Contig',
    'Site1_Position',
    'Site2_Position',
    'Event_Info',
    'Breakpoint_Type',
    'Connection_Type',
}

REQUIRED_MUTATION_EVENT_FIELDS = UNIQUE_MUTATION_EVENT_KEY_FIELDS.union({'Mutation_Status'})

REQUIRED_STRUCTURAL_VARIANT_EVENT_FIELDS = UNIQUE_STRUCTURAL_VARIANT_EVENT_KEY_FIELDS.union({'SV_Status'})

ALL_REFERENCED_EVENT_FIELDS = REQUIRED_MUTATION_EVENT_FIELDS.union(REQUIRED_STRUCTURAL_VARIANT_EVENT_FIELDS)


class EventType(Enum):
    """An Enum class to represent mutation or structural variant event types."""

    MUTATION = 1
    STRUCTURAL_VARIANT = 2


class LineProcessor:
    """Functionality common to all line processors"""

    def __init__(self, event_type, col_indices):
        self.event_type = event_type
        self.col_indices = col_indices
        self.raise_exception_if_missing_required_fields()

    def raise_exception_if_missing_required_fields(self):
        """Checks that all required fields were found in the file for the given event type.

        Raises:
            IndexError: If any of the required fields are not found
        """
        required_field_set = set()
        if self.event_type == EventType.MUTATION:
            required_field_set = REQUIRED_MUTATION_EVENT_FIELDS
        if self.event_type == EventType.STRUCTURAL_VARIANT:
            required_field_set = REQUIRED_STRUCTURAL_VARIANT_EVENT_FIELDS

        missing_field_set = set()
        for field_name in required_field_set:
            if not field_name in self.col_indices:
                missing_field_set.add(field_name)
            if len(missing_field_set) > 0:
                missing_fields = ','.join(missing_field_set)
                raise IndexError(f'Unable to find required column(s) {missing_fields} in event file')

    def line_is_commented(self, line):
        """Determines if the given line in the file is a comment.

        Args:
            line (string): A line from the input file

        Returns:
            boolean: True or False, indicating whether the line is a comment
        """
        return line[0] == '#'

    def convert_line_to_fields(self, line):
        """Converts a tab-delimited data line to a list of values.

        Args:
            line (string): A line from the input file

        Returns:
            list: List of values from the line
        """
        return line.rstrip('\n').split('\t')

    def convert_line_to_field(self, field_index, line):
        """Returns a value of interest in a tab-delimited data line.

        Args:
            field_index (int): Index of the field of interest
            line (string): A line from the input file

        Returns:
            string: The value found at field_index
        """
        return self.convert_line_to_fields(line)[field_index]

    def compute_key_for_line(self, line):
        """Computes a unique key for the given line of data, using the unique
        key fields defined for the event type.

        Args:
            line (string): A line from the input file
        """
        unique_key_field_set = set()
        if self.event_type == EventType.MUTATION:
            unique_key_field_set = UNIQUE_MUTATION_EVENT_KEY_FIELDS
        if self.event_type == EventType.STRUCTURAL_VARIANT:
            unique_key_field_set = UNIQUE_STRUCTURAL_VARIANT_EVENT_KEY_FIELDS

        # Key will be string representation of the object
        fields = self.convert_line_to_fields(line)
        key_value_terms = []
        for key in sorted(unique_key_field_set):
            key_value_terms.append(key + '\t' + fields[self.col_indices[key]])
        return '\t'.join(key_value_terms)


class LineGermlineEventScanner(LineProcessor):
    """Registers the unique event keys for each event with germline status"""

    def __init__(self, event_type, col_indices, germline_event_key_set):
        super().__init__(event_type, col_indices)
        self.header_was_seen = False
        self.germline_event_key_set = germline_event_key_set

    def scan(self, line):
        """Scan data lines for all events which have status 'GERMLINE" and register them

        Args:
            line (string): A line from the input file

        Raises:
            IndexError: If any required column is not found in the input file
        """
        if self.line_is_commented(line):
            return
        if not self.header_was_seen:
            self.header_was_seen = True
            return

        status_col_index = None
        if event_type == EventType.MUTATION:
            status_col_index = self.col_indices['Mutation_Status']
        elif event_type == EventType.STRUCTURAL_VARIANT:
            status_col_index = self.col_indices['SV_Status']

        value = self.convert_line_to_field(status_col_index, line)
        if value.casefold() == 'GERMLINE'.casefold():
            self.germline_event_key_set.add(self.compute_key_for_line(line))


class LineFileWriter(LineProcessor):
    """Handles the processing of each line - filtering for somatic events only"""

    def __init__(self, event_type, col_indices, germline_event_key_set, output_file_handle):
        super().__init__(event_type, col_indices)
        self.output_file_handle = output_file_handle
        self.header_was_written = False
        self.germline_event_key_set = germline_event_key_set

    def process(self, line):
        """Process each line of the given file to remove all events that are not 'SOMATIC' or 'UNKNOWN'.

        Args:
            line (string): A line from the input file

        Raises:
            IndexError: If 'Mutation_Status' column is not found in mutation file
            IndexError: If 'SV_Status' column is not found in structural variant file
        """
        if self.line_is_commented(line):
            self.output_file_handle.write(line)
            return

        if not self.header_was_written:
            self.output_file_handle.write(line)
            self.header_was_written = True
            return

        line_key = self.compute_key_for_line(line)
        if not line_key in self.germline_event_key_set:
            self.output_file_handle.write(line)


class FilteredFileWriter:
    """Handles writing the filtered file containing only somatic events"""

    def __init__(self, input_file_path, output_file_path, event_type):
        self.input_file_path = input_file_path
        self.output_file_path = output_file_path
        self.event_type = event_type
        self.data_handler = DataHandler(input_file_path)
        self.col_indices = self.data_handler.get_col_indices(ALL_REFERENCED_EVENT_FIELDS)
        self.germline_event_keys = set()

    def write(self):
        """Processes the input file and writes out a filtered version including only somatic events"""
        # Scan for all germline events and record keys
        with open(input_file_path, 'r') as input_file_handle:
            line_germline_event_scanner = LineGermlineEventScanner(
                self.event_type, self.col_indices, self.germline_event_keys
            )
            for line in input_file_handle:
                line_germline_event_scanner.scan(line)

        # Read/write events, filtering those which match a germline event key
        with open(input_file_path, 'r') as input_file_handle:
            with open(output_file_path, 'w') as output_file_handle:
                line_processor = LineFileWriter(
                    self.event_type, self.col_indices, self.germline_event_keys, output_file_handle
                )
                for line in input_file_handle:
                    line_processor.process(line)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='filter non-somatic events from mutation or structural variant event files'
    )
    parser.add_argument('input_file_path', help='Path to the input event file')
    parser.add_argument('output_file_path', help='Path to which output is written')
    parser.add_argument(
        '--event-type',
        '-e',
        dest='event_type',
        help='valid event types : {"mutation", "structural_variant"}',
        required=True,
    )
    args = parser.parse_args()

    input_file_path = args.input_file_path
    output_file_path = args.output_file_path

    # Ensure that a recognizable event type code is input.
    event_type = None
    if args.event_type.casefold() == 'mutation'.casefold():
        event_type = EventType.MUTATION
    elif args.event_type.casefold() == 'structural_variant'.casefold():
        event_type = EventType.STRUCTURAL_VARIANT
    if event_type is None:
        raise ValueError(f'Event type argument {args.event_type} not recognized')

    # Filter the file
    filtered_file_writer = FilteredFileWriter(input_file_path, output_file_path, event_type)
    filtered_file_writer.write()
