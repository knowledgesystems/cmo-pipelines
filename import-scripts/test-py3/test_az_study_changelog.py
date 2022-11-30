"""Provides unit tests for the changelog.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os
from unittest.mock import patch

from generate_az_study_changelog_py3 import Changelog, DataHandler


class TestChangelog(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = "test-py3/resources/generate_az_study_changelog"

    def test_deleted_patient(self):
        # Deleted 4 patients and 4 samples
        self.compare_expected_output_to_actual('deleted_patient')

    def test_new_patient(self):
        # New patients: 3 (66, 76, 63)
        # New samples: 5
        self.compare_expected_output_to_actual('new_patient')

    def test_modified_patient(self):
        # Modified patients: 2 (15, 41)
        self.compare_expected_output_to_actual(
            'modified_patient', expected_modified_patient_count=2
        )

    def test_move_patient_up(self):
        self.compare_expected_output_to_actual(
            'move_patient_up', expected_modified_patient_count=2
        )

    def test_move_patient_down(self):
        self.compare_expected_output_to_actual(
            'move_patient_down', expected_modified_patient_count=2
        )

    def test_modified_sample(self):
        # Sample where cancer type has changed is counted in the modified sample count here
        # But in the output, they're shown as added and deleted to current and previous cancer types respectively
        self.compare_expected_output_to_actual(
            'modified_sample', expected_modified_sample_count=3
        )

    def test_deleted_sample(self):
        # Tests a deleted patient with deleted samples
        # As well as a deleted sample from a patient that is not deleted
        self.compare_expected_output_to_actual('deleted_sample')

    def test_new_sample(self):
        # Tests adding a new patient + samples
        # Tests adding a new sample to an existing patient
        self.compare_expected_output_to_actual(
            'new_sample', expected_modified_patient_count=1
        )

    def test_move_sample_up(self):
        self.compare_expected_output_to_actual(
            'move_sample_up', expected_modified_sample_count=2
        )

    def test_move_sample_down(self):
        self.compare_expected_output_to_actual(
            'move_sample_down', expected_modified_sample_count=2
        )

    def test_reorder_patients(self):
        self.compare_expected_output_to_actual(
            'reorder_patients', expected_modified_patient_count=17
        )

    def test_reorder_samples(self):
        self.compare_expected_output_to_actual(
            'reorder_samples', expected_modified_sample_count=22
        )

    def test_new_files(self):
        # Tests changelog output for clinical files that are newly added
        self.compare_expected_output_to_actual('new_files')

    def test_cancer_type_changes(self):
        self.compare_expected_output_to_actual(
            'cancer_type_changes', expected_modified_sample_count=5
        )

    def parse_git_line_tokens(self, git_path):
        git_lines = []

        with open(git_path, 'r') as git_file_handle:
            for line in git_file_handle:
                tokens = line.split('\t')

                # Only want to process the file contents (tab-delimited data)
                if len(tokens) == 1 or tokens[0][0] not in {'-', '+'}:
                    continue

                # Git prepends each line of git log with a '+' or '-'
                # We will use this to correctly parse the changes to each line
                mode = tokens[0][0]
                tokens[0] = tokens[0][1:]

                # Ignore commented lines - they are tab delimited, and will be marked as added on first commit
                if tokens[0][0] == '#':
                    continue

                # Ignore row that contains column names - this will be marked as added on first commit
                if 'PATIENT_ID' in tokens:
                    continue

                git_lines.append((mode, tokens))

        return git_lines

    def compare_expected_output_to_actual(
        self,
        sub_dir,
        expected_modified_patient_count=0,
        expected_modified_sample_count=0,
    ):
        patient_data_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'data_clinical_patient.txt'
        )
        sample_data_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'data_clinical_sample.txt'
        )
        output_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'changelog_summary.txt'
        )
        patient_git_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'data_clinical_patient_diff.txt'
        )
        sample_git_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'data_clinical_sample_diff.txt'
        )
        expected_out_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'expected_changelog_summary.txt'
        )

        changelog_generator = Changelog(patient_data_path, sample_data_path)

        # Get our fake git changes for our mocked method next_git_diff_line (below)
        patient_git_lines = self.parse_git_line_tokens(patient_git_path)
        sample_git_lines = self.parse_git_line_tokens(sample_git_path)

        # Mock the method that provides the diff with patch.object
        # so that we do not actually read git history
        with patch.object(
            DataHandler, "next_git_diff_line"
        ) as next_git_diff_line_mocked:
            next_git_diff_line_mocked.side_effect = [
                iter(patient_git_lines),
                iter(sample_git_lines),
            ]
            changelog_generator.generate_changelog(output_path)

            # Make sure method is called at least once
            next_git_diff_line_mocked.assert_called()

        # Read output file and compare it to expected output
        with open(expected_out_path, 'r') as expected_out:
            # Ignore date line
            expected_out.readline()
            expected = expected_out.read()
            with open(output_path, 'r') as actual_out:
                # Ignore date line
                actual_out.readline()
                actual = actual_out.read()
                self.assertEqual(expected, actual)

        self.assertEqual(
            expected_modified_patient_count,
            changelog_generator.get_num_modified_patients(),
        )
        self.assertEqual(
            expected_modified_sample_count,
            changelog_generator.get_num_modified_samples(),
        )

        # Clean up output file
        os.remove(output_path)


if __name__ == '__main__':
    unittest.main()
