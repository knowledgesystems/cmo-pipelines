"""Provides unit tests for the anonymize_age_at_seq_with_cap_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from anonymize_age_at_seq_with_cap_py3 import PatientFileWriter, SampleFileWriter


class TestAnonymizeAgeAtSeq(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = 'test-py3/resources/anonymize_age_at_seq_with_cap'

    def test_gt_upper_age_limit(self):
        self.compare_expected_output_to_actual('gt_upper_age_limit')

    def test_capped_above_upper_age_limit(self):
        self.compare_expected_output_to_actual('capped_above_upper_age_limit')

    def test_in_between_age_limits(self):
        self.compare_expected_output_to_actual('in_between_age_limits')

    def test_blank_ages(self):
        self.compare_expected_output_to_actual('blank_ages')

    def test_na_ages(self):
        self.compare_expected_output_to_actual('na_ages')

    def test_patient_id_not_in_patient_file(self):
        self.assertRaises(
            IndexError, self.compare_expected_output_to_actual, 'patient_id_not_in_patient_file'
        )

    def test_os_months_column_missing(self):
        self.assertRaises(IndexError, self.compare_expected_output_to_actual, 'os_months_column_missing')

    def test_patient_id_column_missing(self):
        self.assertRaises(IndexError, self.compare_expected_output_to_actual, 'patient_id_column_missing')

    def test_age_at_seq_column_missing(self):
        self.assertRaises(IndexError, self.compare_expected_output_to_actual, 'age_at_seq_column_missing')

    def test_age_current_column_missing(self):
        self.assertRaises(IndexError, self.compare_expected_output_to_actual, 'age_current_column_missing')

    def test_os_months_gt_max_digits(self):
        self.compare_expected_output_to_actual('os_months_gt_max_digits')

    def test_os_months_lt_max_digits(self):
        self.compare_expected_output_to_actual('os_months_lt_max_digits')

    def test_os_months_eq_max_digits(self):
        self.compare_expected_output_to_actual('os_months_eq_max_digits')

    def test_os_months_integers(self):
        self.compare_expected_output_to_actual('os_months_integers')

    def compare_expected_output_to_actual(self, sub_dir, upper_age_limit=89, os_months_precision=2):
        patient_data_path = os.path.join(TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'data_clinical_patient.txt')
        output_patient_data_path = os.path.join(
            TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'generated_data_clinical_patient.txt'
        )
        expected_patient_data_path = os.path.join(
            TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'expected_data_clinical_patient.txt'
        )
        sample_data_path = os.path.join(TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'data_clinical_sample.txt')
        output_sample_data_path = os.path.join(
            TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'generated_data_clinical_sample.txt'
        )
        expected_sample_data_path = os.path.join(
            TestAnonymizeAgeAtSeq.base_dir, sub_dir, 'expected_data_clinical_sample.txt'
        )

        # Read the patient file
        patient_file_reader = PatientFileWriter(
            patient_data_path, output_patient_data_path, upper_age_limit, os_months_precision
        )

        try:
            patient_file_reader.write()
        except IndexError:
            os.remove(output_patient_data_path)
            raise

        # Read output file and compare it to expected output
        with open(expected_patient_data_path, 'r') as expected_out:
            with open(output_patient_data_path, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up patient output file
        os.remove(output_patient_data_path)

        # Transform the clinical sample file
        sample_file_writer = SampleFileWriter(
            sample_data_path,
            output_sample_data_path,
            upper_age_limit,
            patient_file_reader.patient_os_years_map,
        )

        try:
            sample_file_writer.write()
        except (ValueError, IndexError):
            os.remove(output_sample_data_path)
            raise

        # Read output file and compare it to expected output
        with open(expected_sample_data_path, 'r') as expected_out:
            with open(output_sample_data_path, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up sample output file
        os.remove(output_sample_data_path)


if __name__ == '__main__':
    unittest.main()
