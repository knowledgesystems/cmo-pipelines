"""Provides unit tests for the validation_utils_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from validation_utils_py3 import CDMValidator


class TestCDMValidation(unittest.TestCase):

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
        sub_dir = os.path.join(self.base_dir, sub_dir)
        output_file = os.path.join(sub_dir, "data_clinical_sample_output.txt")
        expected_file = os.path.join(sub_dir, "data_clinical_sample_expected.txt")
        cdm_validator = CDMValidator(study_dir=sub_dir)

        cdm_validator.validate_sample_file_sids_match_pids(out_fname="data_clinical_sample_output.txt")

        # Read output file and compare it to expected output
        with open(expected_file, "r") as expected_out:
            with open(output_file, "r") as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up output file
        os.remove(output_file)


if __name__ == "__main__":
    unittest.main()
