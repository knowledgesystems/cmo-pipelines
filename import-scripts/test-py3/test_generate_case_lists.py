# run all python3 unit tests with:
#     import-scripts> python3 -m unittest discover test-py3
#
# Author: Manda Wilson
import filecmp
import os
import os.path
import shutil
import tempfile
import unittest

from generate_case_lists import *


class TestGenerateCaseLists(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp()
        cls.study_dir = "test-py3/resources/generate_case_lists/"
        cls.case_list_config_file = os.path.join(cls.study_dir, "case_list_config.tsv")
        cls.non_existent_file = "this_should_not_exist.txt"
        cls.sample_id_column_file = "data_clinical.txt"
        cls.cases_in_header_file = "case_list_in_header.txt"
        cls.maf_file = "case_list_maf.txt"  # must not include "data_mutations" in the file name, or it will read sequenced_samples.txt instead
        cls.sequenced_samples_in_meta_header_file = "case_list_sequenced_samples_in_meta_header.txt"
        cls.read_sequenced_samples_file_instead_of_this_file = "case_list_data_mutations_maf.txt"

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)

    def test_get_case_list_from_staging_file_no_file(self):
        non_existent_file_full_path = os.path.join(self.study_dir, self.non_existent_file)
        self.assertFalse(os.path.isfile(non_existent_file_full_path))
        case_list = get_case_list_from_staging_file(self.study_dir, self.non_existent_file, False)
        self.assertEqual([], case_list)

    def test_get_case_list_from_staging_file_sample_id_column(self):
        case_list = get_case_list_from_staging_file(self.study_dir, self.sample_id_column_file, False)
        self.assertEqual(["P-0000001-T01-XXX", "P-0000002-T02-XYZ", "P-0000003-T01-YYY", "P-0000004-T02-ZZZ", "P-0000005-T02-ZYZ", "P-0000006-T02-XXX"], case_list)

    def test_get_case_list_from_staging_file_cases_in_header(self):
        # exclude known column headers that are not cases (e.g. "GENE")
        case_list = get_case_list_from_staging_file(self.study_dir, self.cases_in_header_file, False)
        self.assertEqual(["CASE_1", "CASE_2", "CASE_3", "CASE_4"], case_list)

    def test_get_case_list_from_staging_file_maf(self):
        case_list = get_case_list_from_staging_file(self.study_dir, self.maf_file, False)
        self.assertEqual(["CASE1", "CASE2", "CASE3"], case_list)

    def test_get_case_list_from_staging_file_sequenced_samples_in_header(self):
        # cases separated by tabs, single spaces, and consecutive spaces; extra comment lines ignored
        case_list = get_case_list_from_staging_file(self.study_dir, self.sequenced_samples_in_meta_header_file, False)
        self.assertEqual(["A", "B", "C", "D", "E"], case_list)

    def test_get_case_list_from_staging_file_has_sequenced_samples_file(self):
        # filename contains "data_mutations" and sequenced_samples.txt exists, so sequenced_samples.txt is the source; order preserved
        case_list = get_case_list_from_staging_file(self.study_dir, self.read_sequenced_samples_file_instead_of_this_file, False)
        self.assertEqual(["A", "B", "C", "C1", "E", "C2", "G", "H", "I"], case_list)

    def test_get_sample_id_tcga_barcodes(self):
        self.assertEqual("TCGA-A1-A0SB-01", get_sample_id("TCGA-A1-A0SB-01A-11D-A142-09"))
        self.assertEqual("TCGA-A1-A0SB-01", get_sample_id("TCGA-A1-A0SB-Tumor"))
        self.assertEqual("TCGA-A1-A0SB-11", get_sample_id("TCGA-A1-A0SB-Normal"))
        self.assertEqual("TCGA-A1-A0SB-01", get_sample_id("TCGA-A1-A0SB"))
        self.assertEqual("P-0000001-T01-XXX", get_sample_id("P-0000001-T01-XXX"))

    def test_resolve_staging_file_case_insensitive(self):
        resolved = resolve_staging_file(self.study_dir, "data_cna.txt")
        self.assertIsNotNone(resolved)
        self.assertTrue(os.path.samefile(os.path.join(self.study_dir, "data_CNA.txt"), resolved))
        self.assertIsNone(resolve_staging_file(self.study_dir, self.non_existent_file))
        self.assertEqual(["C1", "C2", "C3"], get_case_list_from_staging_file(self.study_dir, "data_cna.txt", False))

    def test_generate_case_lists(self):
        self.assertEqual([], os.listdir(self.temp_dir))
        generate_case_lists(self.case_list_config_file, self.temp_dir, self.study_dir, "TESTING_STUDY", False, False)
        expected_files = ["cases_all.txt",        # union of cases from data_CNA.txt, sequenced_samples.txt, data_clinical.txt
                          "cases_sequenced.txt",  # only sequenced_samples.txt (which replaces data_mutations_extended.txt)
                          "cases_cna.txt",        # single file data_CNA.txt
                          "cases_cnaseq.txt"]     # intersection of cases from data_CNA.txt and sequenced_samples.txt
        self.assertEqual(sorted(expected_files), sorted(os.listdir(self.temp_dir)))
        for expected_file in expected_files:
            actual = os.path.join(self.temp_dir, expected_file)
            expected = os.path.join(self.study_dir, "expected_" + expected_file)
            with open(actual) as actual_file:
                self.assertTrue(filecmp.cmp(actual, expected, shallow=False), "%s differs from %s, actual: %r" % (actual, expected, actual_file.read()))

    def test_generate_case_lists_trims_config_and_normalizes_tcga(self):
        temp_dir = tempfile.mkdtemp()
        try:
            study_dir = os.path.join(temp_dir, "study")
            case_list_dir = os.path.join(study_dir, "case_lists")
            os.makedirs(case_list_dir)
            with open(os.path.join(study_dir, "meta_study.txt"), "w") as meta_study_file:
                meta_study_file.write("type_of_cancer: brca\ncancer_study_identifier: tcga_test\n")
            with open(os.path.join(study_dir, "data_cna.txt"), "w") as cna_file:
                cna_file.write("Hugo_Symbol\tTCGA-A1-A0SB-01A-11D\tTCGA-A1-A0SD-Tumor\n")
                cna_file.write("BRCA1\t0\t1\n")
            config_filename = os.path.join(temp_dir, "config.tsv")
            with open(config_filename, "w") as config_file:
                config_file.write("\t".join(CASE_LIST_CONFIG_HEADER_COLUMNS) + "\n")
                # padded fields + wrong-case staging filename + blank line
                config_file.write(" cases_cna.txt \t data_CNA.txt \t <CANCER_STUDY>_cna \t all_cases_with_cna_data \t<CANCER_STUDY>\t CNA \t CNA (<NUM_CASES> samples) \n\n")
            self.assertEqual("tcga_test", get_study_id_from_meta_study(study_dir))
            generate_case_lists(config_filename, case_list_dir, study_dir, "tcga_test", False, False, True)
            with open(os.path.join(case_list_dir, "cases_cna.txt")) as case_list_file:
                lines = case_list_file.read().split("\n")
            self.assertEqual("stable_id: tcga_test_cna", lines[1])
            self.assertEqual("case_list_name: CNA", lines[2])
            self.assertEqual("case_list_description: CNA (2 samples)", lines[3])
            self.assertEqual("case_list_ids: TCGA-A1-A0SB-01\tTCGA-A1-A0SD-01", lines[5])
            # A malformed occupied filename must be preserved and reported.
            with open(os.path.join(case_list_dir, "cases_cna.txt"), "w") as case_list_file:
                case_list_file.write("keep me\n")
            with self.assertRaisesRegex(ValueError, 'occupied by an unrelated list'):
                generate_case_lists(config_filename, case_list_dir, study_dir, "tcga_test", False, False, True)
            with open(os.path.join(case_list_dir, "cases_cna.txt")) as case_list_file:
                self.assertEqual("keep me\n", case_list_file.read())
        finally:
            shutil.rmtree(temp_dir)


if __name__ == "__main__":
    unittest.main()
