# run all unit tests with:
#     import-scripts> python -m unittest discover test
#
# Author: Manda Wilson

import unittest
import os.path
import tempfile
import shutil
import filecmp

from generate_case_lists import *

class TestGenerateCaseLists(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp()
        cls.study_dir = "test/resources/generate_case_lists/"
        cls.case_list_config_file = os.path.join(cls.study_dir, "case_list_config.tsv")
        cls.non_existent_file = "this_should_not_exist.txt"
        cls.sample_id_column_file = "data_clinical.txt"
        cls.cases_in_header_file = "case_list_in_header.txt"
        cls.maf_file = "case_list_maf.txt" # not not include "data_mutations" in the file name, or it will read sequenced_samples.txt instead
        cls.sequenced_samples_in_meta_header_file = "case_list_sequenced_samples_in_meta_header.txt"
        cls.read_sequenced_samples_file_instead_of_this_file = "case_list_data_mutations_maf.txt"

    def test_get_case_list_from_staging_file_no_file(self):
        # if file doesn't exist we should get empty list of cases
        non_existent_file_full_path = os.path.join(self.study_dir, self.non_existent_file)
        self.assertTrue(not os.path.isfile(non_existent_file_full_path), "'%s' file exists when it should not" % (non_existent_file_full_path))
        case_list = get_case_list_from_staging_file(self.study_dir, self.non_existent_file, False)
        self.assertTrue(isinstance(case_list, list), "Expected get_case_list_from_staging_file() to return a list but it returned '%s'" % (type(case_list)))
        self.assertEqual(0, len(case_list), msg="Expected an empty case list when reading a file that doesn't exist, but got %d cases" % (len(case_list)))

    def test_get_case_list_from_staging_file_sample_id_column(self):
        # cases read from SAMPLE_ID column
        case_list = get_case_list_from_staging_file(self.study_dir, self.sample_id_column_file, False)
        self.assertEqual(sorted(case_list), sorted(["P-0000001-T01-XXX", "P-0000002-T02-XYZ", "P-0000003-T01-YYY", "P-0000004-T02-ZZZ", "P-0000005-T02-ZYZ", "P-0000006-T02-XXX"]))

    def test_get_case_list_from_staging_file_cases_in_header(self):
        # cases read from header
        # exclude known column headers that are not cases (e.g. "GENE")
        case_list = get_case_list_from_staging_file(self.study_dir, self.cases_in_header_file, False)
        self.assertEqual(sorted(case_list), sorted(["CASE_1", "CASE_2", "CASE_3", "CASE_4"]))

    def test_get_case_list_from_staging_file_maf(self):
        # cases read from Tumor_Sample_Barcode column
        # filename does NOT contain "data_mutations" so it should not get cases from sequenced_samples.txt
        case_list = get_case_list_from_staging_file(self.study_dir, self.maf_file, False)
        self.assertEqual(sorted(case_list), sorted(["CASE1", "CASE2", "CASE3"]))

    def test_get_case_list_from_staging_file_sequenced_samples_in_header(self):
        # cases read from "#sequenced_samples:" in meta header
        # cases are separated by tabs, single spaces, and consecutive spaces
        # file also include additional comment lines to be ignored
        case_list = get_case_list_from_staging_file(self.study_dir, self.sequenced_samples_in_meta_header_file, False)
        self.assertEqual(sorted(case_list), sorted(["A", "B", "C", "D", "E"]))

    def test_get_case_list_from_staging_file_has_sequenced_samples_file(self):
        # cases read from sequenced_samples.txt
        # filename contains "data_mutations" and sequenced_samples.txt exists, so sequenced_samples.txt is the source
        case_list = get_case_list_from_staging_file(self.study_dir, self.read_sequenced_samples_file_instead_of_this_file, False)
        self.assertEqual(sorted(case_list), sorted(["A", "B", "C", "C1", "E", "C2", "G", "H", "I"]))

    def test_generate_case_lists(self):
        # confirm we don't have any files in temp directory yet
        all_files_in_temp_dir = [f for f in os.listdir(self.temp_dir) if os.path.isfile(os.path.join(self.temp_dir, f))]
        self.assertEquals(0, len(all_files_in_temp_dir), msg="Expecting no files in '%s' but found %d" % (self.temp_dir, len(all_files_in_temp_dir)))

        # generate case files in temporary directory
        generate_case_lists(self.case_list_config_file, self.temp_dir, self.study_dir, "TESTING_STUDY", False, False)

        # make sure we have all expected files
        all_files_in_temp_dir = [f for f in os.listdir(self.temp_dir) if os.path.isfile(os.path.join(self.temp_dir, f))]
        expected_files = ["cases_all.txt", # union of cases from data_CNA.txt, sequenced_samples.txt, data_clinical.txt
            "cases_sequenced.txt", # generally union of cases but in this case it is only sequenced_samples.txt (which replaces data_mutations_extended.txt)
            "cases_cna.txt", # single file data_CNA.txt
            "cases_cnaseq.txt"] # intersection of cases from data_CNA.txt and sequenced_samples.txt
        self.assertEquals(sorted(expected_files), sorted(all_files_in_temp_dir))

        # check each file against expected version
        for expected_file in expected_files:
            actual_file_full_path = os.path.join(self.temp_dir, expected_file)
            expected_file_full_path = os.path.join(self.study_dir, "expected_" + expected_file)
            actual_file_contents = ""
            with open(actual_file_full_path, 'r') as actual_file:
                actual_file_contents = actual_file.read()
            self.assertTrue(filecmp.cmp(actual_file_full_path, expected_file_full_path), msg="Actual file '%s' differs from '%s' expected file.  Actual file contains: \"%s\"" % (actual_file_full_path, expected_file_full_path, actual_file_contents))

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)

if __name__ == '__main__':
    unittest.main()
