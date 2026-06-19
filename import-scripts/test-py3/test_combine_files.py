"""Provides unit tests for the combine_files_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from combine_files_py3 import combine_files

class TestCombineFiles(unittest.TestCase):

    # Show the diff even if it is very big
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        cls.base_dir = 'test-py3/resources/combine_files'

    def test_duplicates(self):
        sub_dir = 'duplicates'
        ddp_files = [
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr1.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr2.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr3.txt'),
        ]
        merge_type='outer'
        self.compare_expected_output_to_actual(sub_dir, ddp_files, merge_type)
  
    def test_diff_headers(self):
        sub_dir = 'diff_headers'
        ddp_files = [
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_vital_status.txt'),
        ]
        merge_type='outer'
        self.compare_expected_output_to_actual(sub_dir, ddp_files, merge_type)

    def test_has_metadata_headers(self):
        sub_dir = 'has_metadata_headers'
        ddp_files = [
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_vital_status1.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_vital_status2.txt'),
        ]
        merge_type='outer'
        self.compare_expected_output_to_actual(sub_dir, ddp_files, merge_type)

    def test_maintain_int_datatypes(self):
        sub_dir = 'maintain_int_datatypes'
        ddp_files = [
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr1.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr2.txt'),
            os.path.join(TestCombineFiles.base_dir, sub_dir, 'ddp_naaccr3.txt'),
        ]
        merge_type='outer'
        self.compare_expected_output_to_actual(sub_dir, ddp_files, merge_type)

    def test_biobank_merge_both_have_aliquot_status_with_prefer_right(self):
        self._assert_biobank_merge_matches_expected(
            'biobank_clinical/both_overlap',
            prefer_right_columns=True,
        )

    def test_biobank_merge_aliquot_status_only_on_right_with_prefer_right(self):
        self._assert_biobank_merge_matches_expected(
            'biobank_clinical/right_only',
            prefer_right_columns=True,
        )

    def test_biobank_merge_aliquot_status_only_on_left_with_prefer_right(self):
        self._assert_biobank_merge_matches_expected(
            'biobank_clinical/left_only',
            prefer_right_columns=True,
        )

    def test_biobank_merge_both_have_aliquot_status_without_prefer_right_uses_suffixes(self):
        sub_dir = os.path.join(TestCombineFiles.base_dir, 'biobank_clinical/both_overlap')
        output_file = os.path.join(sub_dir, 'merged_without_prefer_right.txt')

        combine_files(
            [
                os.path.join(sub_dir, 'clinical_patient.txt'),
                os.path.join(sub_dir, 'biobank_clinical.txt'),
            ],
            output_file,
            merge_type='left',
            columns=['PATIENT_ID'],
            prefer_right_columns=False,
        )

        with open(output_file, 'r') as actual_out:
            header = actual_out.readline().strip().split('\t')
            self.assertIn('ALIQUOT_STATUS_x', header)
            self.assertIn('ALIQUOT_STATUS_y', header)
            self.assertNotIn('ALIQUOT_STATUS', header)

        os.remove(output_file)

    def _assert_biobank_merge_matches_expected(self, fixture_subdir, prefer_right_columns):
        sub_dir = os.path.join(TestCombineFiles.base_dir, fixture_subdir)
        output_file = os.path.join(sub_dir, 'merged.txt')
        expected_file = os.path.join(sub_dir, 'expected.txt')

        combine_files(
            [
                os.path.join(sub_dir, 'clinical_patient.txt'),
                os.path.join(sub_dir, 'biobank_clinical.txt'),
            ],
            output_file,
            merge_type='left',
            columns=['PATIENT_ID'],
            prefer_right_columns=prefer_right_columns,
        )

        with open(expected_file, 'r') as expected_out:
            with open(output_file, 'r') as actual_out:
                self.assertEqual(expected_out.read(), actual_out.read())

        os.remove(output_file)

    def compare_expected_output_to_actual(self, sub_dir, ddp_files, merge_type='inner', columns=None):
        output_file = os.path.join(TestCombineFiles.base_dir, sub_dir, 'merged.txt')
        expected_file = os.path.join(TestCombineFiles.base_dir, sub_dir, 'expected.txt')

        try:
            combine_files(ddp_files, output_file, merge_type=merge_type, columns=None)
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
