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
        self.assertEqual(0, len(all_files_in_temp_dir), msg="Expecting no files in '%s' but found %d" % (self.temp_dir, len(all_files_in_temp_dir)))

        # generate case files in temporary directory
        generate_case_lists(self.case_list_config_file, self.temp_dir, self.study_dir, "TESTING_STUDY", False, False)

        # make sure we have all expected files
        all_files_in_temp_dir = [f for f in os.listdir(self.temp_dir) if os.path.isfile(os.path.join(self.temp_dir, f))]
        expected_files = ["cases_all.txt", # union of cases from data_CNA.txt, sequenced_samples.txt, data_clinical.txt
            "cases_sequenced.txt", # generally union of cases but in this case it is only sequenced_samples.txt (which replaces data_mutations_extended.txt)
            "cases_cna.txt", # single file data_CNA.txt
            "cases_cnaseq.txt"] # intersection of cases from data_CNA.txt and sequenced_samples.txt
        self.assertEqual(sorted(expected_files), sorted(all_files_in_temp_dir))

        # check each file against expected version
        for expected_file in expected_files:
            actual_file_full_path = os.path.join(self.temp_dir, expected_file)
            expected_file_full_path = os.path.join(self.study_dir, "expected_" + expected_file)
            actual_file_contents = ""
            with open(actual_file_full_path, 'r') as actual_file:
                actual_file_contents = actual_file.read()
            with open(expected_file_full_path) as expected_file:
                expected_lines = expected_file.read().splitlines()
            actual_lines = actual_file_contents.splitlines()
            self.assertEqual(expected_lines[:-1], actual_lines[:-1])
            self.assertEqual(set(expected_lines[-1].split(': ', 1)[1].split('\t')),
                             set(actual_lines[-1].split(': ', 1)[1].split('\t')))

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
        self.assertEqual(["C1", "C2", "C3"], sorted(get_case_list_from_staging_file(self.study_dir, "data_cna.txt", False)))

    def test_generate_case_lists_trims_config_and_normalizes_tcga(self):
        temp_dir = tempfile.mkdtemp()
        try:
            study_dir = os.path.join(temp_dir, "study")
            case_list_dir = os.path.join(study_dir, "case_lists")
            os.makedirs(case_list_dir)
            with open(os.path.join(study_dir, "data_cna.txt"), "w") as cna_file:
                cna_file.write("Hugo_Symbol\tTCGA-A1-A0SB-01A-11D\tTCGA-A1-A0SD-Tumor\n")
                cna_file.write("BRCA1\t0\t1\n")
            config_filename = os.path.join(temp_dir, "config.tsv")
            with open(config_filename, "w") as config_file:
                config_file.write("\t".join(CASE_LIST_CONFIG_HEADER_COLUMNS) + "\n")
                # padded fields + wrong-case staging filename + blank line
                config_file.write(" cases_cna.txt \t data_CNA.txt \t <CANCER_STUDY>_cna \t all_cases_with_cna_data \t<CANCER_STUDY>\t CNA \t CNA (<NUM_CASES> samples) \n\n")
            generate_case_lists(config_filename, case_list_dir, study_dir, "tcga_test")
            with open(os.path.join(case_list_dir, "cases_cna.txt")) as case_list_file:
                self.assertIn("TCGA-A1-A0SB-01A-11D", case_list_file.read())
            generate_case_lists(config_filename, case_list_dir, study_dir, "tcga_test", True, False, True)
            with open(os.path.join(case_list_dir, "cases_cna.txt")) as case_list_file:
                lines = case_list_file.read().split("\n")
            self.assertEqual("stable_id: tcga_test_cna", lines[1])
            self.assertEqual("case_list_name: CNA", lines[2])
            self.assertEqual("case_list_description: CNA (2 samples)", lines[3])
            self.assertEqual({"TCGA-A1-A0SB-01", "TCGA-A1-A0SD-01"}, set(lines[5].split(": ", 1)[1].split("\t")))
            # gap-fill only: a second run without overwrite leaves the file alone
            with open(os.path.join(case_list_dir, "cases_cna.txt"), "w") as case_list_file:
                case_list_file.write("keep me\n")
            generate_case_lists(config_filename, case_list_dir, study_dir, "tcga_test", False, False, True)
            with open(os.path.join(case_list_dir, "cases_cna.txt")) as case_list_file:
                self.assertEqual("keep me\n", case_list_file.read())
        finally:
            shutil.rmtree(temp_dir)

    def test_case_insensitive_intersection_and_trimmed_union(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config = os.path.join(temp_dir, "config.tsv")
            with open(config, "w") as config_file:
                config_file.write("\t".join(CASE_LIST_CONFIG_HEADER_COLUMNS) + "\n")
                for name, expression in (("intersection", "data_cna.txt & data_mutations_extended.txt"),
                                         ("union", "data_cna.txt | data_mutations_extended.txt")):
                    config_file.write("\t".join([name + ".txt", expression, "<CANCER_STUDY>_" + name,
                                                 "all_cases_in_study", "<CANCER_STUDY>", name, name]) + "\n")
            generate_case_lists(config, temp_dir, self.study_dir, "TESTING_STUDY")
            cna = set(get_case_list_from_staging_file(self.study_dir, "data_CNA.txt", False))
            seq = set(get_case_list_from_staging_file(self.study_dir, "data_mutations_extended.txt", False))
            for name, expected in (("intersection", cna & seq), ("union", cna | seq)):
                with open(os.path.join(temp_dir, name + ".txt")) as case_file:
                    ids = case_file.read().splitlines()[-1].split(": ", 1)[1].split("\t")
                self.assertEqual(expected, set(ids))

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)

if __name__ == '__main__':
    unittest.main()
