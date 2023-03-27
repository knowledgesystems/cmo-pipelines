"""Provides unit tests for the filter_non_somatic_events_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from filter_non_somatic_events_py3 import FilteredFileWriter, EventType


class TestFilterNonSomaticEvents(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = 'test-py3/resources/filter_non_somatic_events'

    def test_mutation_wo_pathscore(self):
        self.compare_expected_output_to_actual('mutation_wo_pathscore', EventType.MUTATION)

    def test_mutation_w_pathscore(self):
        self.compare_expected_output_to_actual('mutation_w_pathscore', EventType.MUTATION)

    def test_sv(self):
        self.compare_expected_output_to_actual('sv', EventType.STRUCTURAL_VARIANT)

    def compare_expected_output_to_actual(self, sub_dir, event_type):
        base_filepath = 'data_mutations_extended.txt' if event_type == EventType.MUTATION else 'data_sv.txt'
        input_data_path = os.path.join(TestFilterNonSomaticEvents.base_dir, sub_dir, base_filepath)
        output_data_path = os.path.join(
            TestFilterNonSomaticEvents.base_dir, sub_dir, 'generated_' + base_filepath
        )
        expected_data_path = os.path.join(
            TestFilterNonSomaticEvents.base_dir, sub_dir, 'expected_' + base_filepath
        )

        # Filter the file
        filtered_file_writer = FilteredFileWriter(input_data_path, output_data_path, event_type)

        try:
            filtered_file_writer.write()
        except IndexError:
            os.remove(output_data_path)
            raise

        # Read output file and compare it to expected output
        with open(expected_data_path, 'r') as expected_out:
            with open(output_data_path, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up patient output file
        os.remove(output_data_path)


if __name__ == '__main__':
    unittest.main()
