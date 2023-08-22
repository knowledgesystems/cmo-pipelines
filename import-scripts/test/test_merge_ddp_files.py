"""Provides unit tests for the test_merge_ddp_files.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test
"""

import unittest
import os

from merge_ddp_files import merge_ddp_files

class TestMergeDDPFiles(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = 'test/resources/merge_ddp_files'

    def test_duplicates(self):
        sub_dir = 'duplicates'
        ddp_files = [
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr1.txt'),
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr2.txt'),
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr3.txt'),
        ]
        self.compare_expected_output_to_actual(sub_dir, ddp_files)

    def test_not_on_masterlist(self):
        sub_dir = 'not_on_masterlist'
        ddp_files = [
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr1.txt'),
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr2.txt'),
        ]
        self.compare_expected_output_to_actual(sub_dir, ddp_files)

    def test_diff_headers(self):
        sub_dir = 'diff_headers'
        ddp_files = [
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr.txt'),
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_vital_status.txt'),
        ]
        self.assertRaises(ValueError, self.compare_expected_output_to_actual, sub_dir, ddp_files)

    def test_no_patientid(self):
        sub_dir = 'no_patientid'
        ddp_files = [
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_naaccr.txt'),
        ]
        self.assertRaises(IndexError, self.compare_expected_output_to_actual, sub_dir, ddp_files)

    def test_has_metadata_headers(self):
        sub_dir = 'has_metadata_headers'
        ddp_files = [
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_vital_status1.txt'),
            os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'ddp_vital_status2.txt'),
        ]
        self.compare_expected_output_to_actual(sub_dir, ddp_files)

    def compare_expected_output_to_actual(self, sub_dir, ddp_files):
        clinical_file = os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'data_clinical_patient.txt')
        output_file = os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'merged.txt')
        expected_file = os.path.join(TestMergeDDPFiles.base_dir, sub_dir, 'expected.txt')

        try:
            merge_ddp_files(ddp_files, clinical_file, output_file)
        except (KeyError, ValueError):
            if os.path.exists(output_file):
                os.remove(output_file)
            raise

        # Read output file and compare it to expected output
        with open(expected_file, 'r') as expected_out:
            with open(output_file, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        # Clean up sample output file
        os.remove(output_file)


if __name__ == '__main__':
    unittest.main()
