"""Provides unit tests for the generate_az_study_changelog_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from generate_az_study_changelog_py3 import Changelog


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
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('move_patient_up')

    def test_move_patient_down(self):
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('move_patient_down')

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
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('move_sample_up')

    def test_move_sample_down(self):
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('move_sample_down')

    def test_reorder_patients(self):
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('reorder_patients')

    def test_reorder_samples(self):
        # Reordering rows is no longer reported as a change
        self.compare_expected_output_to_actual('reorder_samples')

    def test_new_files(self):
        # Tests changelog output for clinical files that are newly added (no previous version)
        self.compare_expected_output_to_actual('new_files')

    def test_cancer_type_changes(self):
        self.compare_expected_output_to_actual(
            'cancer_type_changes', expected_modified_sample_count=5
        )

    def _fixture_path(self, sub_dir, name):
        """Returns the path to a fixture file, or None when it does not exist
        (a missing previous_* file means there is no prior version to compare against)."""
        path = os.path.join(TestChangelog.base_dir, sub_dir, name)
        return path if os.path.exists(path) else None

    def compare_expected_output_to_actual(
        self,
        sub_dir,
        expected_modified_patient_count=0,
        expected_modified_sample_count=0,
    ):
        previous_patient_path = self._fixture_path(sub_dir, 'previous_data_clinical_patient.txt')
        current_patient_path = self._fixture_path(sub_dir, 'current_data_clinical_patient.txt')
        previous_sample_path = self._fixture_path(sub_dir, 'previous_data_clinical_sample.txt')
        current_sample_path = self._fixture_path(sub_dir, 'current_data_clinical_sample.txt')
        output_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'changelog_summary.txt'
        )
        expected_out_path = os.path.join(
            TestChangelog.base_dir, sub_dir, 'expected_changelog_summary.txt'
        )

        changelog_generator = Changelog(
            previous_patient_path,
            current_patient_path,
            previous_sample_path,
            current_sample_path,
        )
        changelog_generator.generate_changelog(output_path)

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
