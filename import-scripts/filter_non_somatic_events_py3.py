#!/usr/bin/env python3

"""Filter Non Somatic Events
This script reads a file of mutation data (data_mutations_extended.txt) or
structural variant data (data_sv.txt) and writes out a filtered copy of
the file where every event which is not noted as SOMATIC has been removed.

Usage:
    python3 filter_non_somatic_events_py3.py $INPUT_FILE_PATH $OUTPUT_FILE_PATH --event-type $FILE_TYPE \
Example:
    python3 filter_non_somatic_events_py3.py /path/to/az-msk-impact-2022/data_mutation_extended.txt \
        /path/to/az-msk-impact-2022/data_mutation_extended.txt.filtered --event-type mutation
"""

import argparse
from enum import Enum
from generate_az_study_changelog_py3 import DataHandler


class EventType(Enum):
    """An Enum class to represent mutation or structural variant event types."""

    MUTATION = 1
    STRUCTURAL_VARIANT = 2


class LineProcessor:
    """Handles the processing of each line - filtering for somatic events only"""

    def __init__(self, event_type, col_indices, output_file_handle):
        self.event_type = event_type
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
        return line[0] == '#'

    def process(self, line):
        """Process each line of the given file to remove all events that are not 'SOMATIC'.

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

        col_index = None
        if event_type == EventType.MUTATION:
            if 'Mutation_Status' not in self.col_indices:
                raise IndexError('Unable to find Mutation_status column in event file')
            col_index = self.col_indices['Mutation_Status']
        elif event_type == EventType.STRUCTURAL_VARIANT:
            if 'SV_Status' not in self.col_indices:
                raise IndexError('Unable to find SV_Status column in event file')
            col_index = self.col_indices['SV_Status']

        cols = line.split('\t')
        value = cols[col_index].rstrip('\n')
        if value.casefold() == 'SOMATIC'.casefold():
            self.output_file_handle.write(line)


class FilteredFileWriter:
    """Handles writing the filtered file containing only somatic events"""

    def __init__(self, input_file_path, output_file_path, event_type):
        self.input_file_path = input_file_path
        self.output_file_path = output_file_path
        self.event_type = event_type
        self.data_handler = DataHandler(input_file_path)
        self.col_indices = self.data_handler.get_col_indices({"Mutation_Status", "SV_Status"})

    def write(self):
        """Processes the input file and writes out a filtered version including only somatic events"""
        with open(input_file_path, "r") as input_file_handle:
            with open(output_file_path, "w") as output_file_handle:
                line_processor = LineProcessor(self.event_type, self.col_indices, output_file_handle)
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
    if not args.event_type:
        raise ValueError('Event type argument is missing')
    if args.event_type.casefold() == "mutation".casefold():
        event_type = EventType.MUTATION
    elif args.event_type.casefold() == "structural_variant".casefold():
        event_type = EventType.STRUCTURAL_VARIANT
    if event_type is None:
        raise ValueError(f'event type argument {args.event_type} not recognized or missing')

    # Filter the file
    filtered_file_writer = FilteredFileWriter(input_file_path, output_file_path, event_type)
    filtered_file_writer.write()
