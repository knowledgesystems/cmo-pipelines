"""Provides unit tests for the standardize_clinical_data.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test
"""

import unittest
import os

from clinicalfile_utils import write_standardized_columns

class TestStandardizeClinicalData(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = 'test/resources/standardize_clinical_data'

    def test_clinical_patient(self):
        sub_dir = 'clinical_patient'
        input_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'data_clinical_patient.txt')
        output_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'generated_data_clinical_patient.txt')
        expected_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'expected_data_clinical_patient.txt')
        self.compare_expected_output_to_actual(input_data_path, output_data_path, expected_data_path)

    def test_clinical_sample(self):
        sub_dir = 'clinical_sample'
        input_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'data_clinical_sample.txt')
        output_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'generated_data_clinical_sample.txt')
        expected_data_path = os.path.join(TestStandardizeClinicalData.base_dir, sub_dir, 'expected_data_clinical_sample.txt')
        self.compare_expected_output_to_actual(input_data_path, output_data_path, expected_data_path)

    def compare_expected_output_to_actual(self, input_data_path, output_data_path, expected_data_path):
        try:
            with open(output_data_path, 'w') as output_data:
                write_standardized_columns(input_data_path, output_data)
        except ValueError:
            os.remove(output_data_path)
            raise

        # Read output file and compare it to expected output
        with open(expected_data_path, 'r') as expected_out:
            with open(output_data_path, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up sample output file
        os.remove(output_data_path)


if __name__ == '__main__':
    unittest.main()
