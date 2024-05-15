"""Provides unit tests for the validation_utils_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from validation_utils_py3 import CDMSampleFileValidator


class TestCDMSampleFileValidation(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = "test-py3/resources/validation_utils/cdm_sample_file"

    def test_sample_file_no_changes(self):
        sub_dir = "no_changes"
        self.compare_expected_output_to_actual(sub_dir)

    def test_sample_file_mismatched_ids(self):
        sub_dir = "mismatched_ids"
        self.compare_expected_output_to_actual(sub_dir)

    def compare_expected_output_to_actual(self, sub_dir):
        input_sample_file = os.path.join(TestCDMSampleFileValidation.base_dir, sub_dir, "data_clinical_sample.txt")
        output_file = os.path.join(TestCDMSampleFileValidation.base_dir, sub_dir, "data_clinical_sample_output.txt")
        expected_file = os.path.join(TestCDMSampleFileValidation.base_dir, sub_dir, "data_clinical_sample_expected.txt")
        sample_file_validator = CDMSampleFileValidator(input_sample_file, output_file_path=output_file)

        try:
            sample_file_validator.validate()
        except (KeyError, ValueError):  # TODO fix error types
            if os.path.exists(output_file):
                os.remove(output_file)
            raise

        # Read output file and compare it to expected output
        with open(expected_file, "r") as expected_out:
            with open(output_file, "r") as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up output file
        os.remove(output_file)


if __name__ == "__main__":
    unittest.main()
