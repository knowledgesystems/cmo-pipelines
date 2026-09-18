# run all python3 unit tests with:
#     import-scripts> python3 -m unittest discover test-py3
#
# Author: Manda Wilson
import io
import os
import os.path
import tempfile
import unittest

from oncotree_code_converter import *


class TestOncotreeCodeConverter(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        resource_dir = "test-py3/resources/oncotree_code_converter/"
        cls.oncotree_json_filename = os.path.join(resource_dir, "mock_oncotree_tumortypes_api_output_flat.json")
        cls.expected_oncotree_code_mappings_filename = os.path.join(resource_dir, "oncotree_code_mappings_expected.txt")
        with io.open(cls.oncotree_json_filename, "r", encoding="utf8") as oncotree_json_file:
            cls.oncotree_mappings = extract_oncotree_code_mappings_from_oncotree_json(oncotree_json_file.read())
        cls.data_clinical_no_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_no_cancer_type.txt")
        cls.data_clinical_processed_filename = os.path.join(resource_dir, "data_clinical_sample_processed.txt")
        cls.data_clinical_with_meta_headers_filename = os.path.join(resource_dir, "data_clinical_sample_with_meta_headers.txt")
        cls.data_clinical_oncotree_last_column_filename = os.path.join(resource_dir, "data_clinical_sample_oncotree_last_column.txt")
        cls.data_clinical_incorrect_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_incorrect_cancer_type.txt")
        cls.data_clinical_no_oncotree_code_filename = os.path.join(resource_dir, "data_clinical_sample_no_oncotree_code.txt")
        cls.temp_files = []

    @classmethod
    def tearDownClass(cls):
        for filename in cls.temp_files:
            if os.path.exists(filename):
                os.remove(filename)

    def test_extract_oncotree_code_mappings_from_oncotree_json(self):
        with io.open(self.expected_oncotree_code_mappings_filename, "r", encoding="utf8") as expected_file:
            expected_lines = expected_file.read().strip("\n").split("\n")
        for mappings_line in expected_lines:
            oncotree_code, expected_cancer_type_detailed, expected_cancer_type = mappings_line.split("\t")[:3]
            self.assertIn(oncotree_code, self.oncotree_mappings, "Failed to extract info for oncotree code: %s" % (oncotree_code))
            self.assertEqual(expected_cancer_type, self.oncotree_mappings[oncotree_code]["CANCER_TYPE"])
            self.assertEqual(expected_cancer_type_detailed, self.oncotree_mappings[oncotree_code]["CANCER_TYPE_DETAILED"])

    def test_process_clinical_file(self):
        original, processed = self.call_process_clinical_file(self.data_clinical_no_cancer_type_filename)
        with io.open(self.data_clinical_processed_filename, "r", encoding="utf8") as expected_file:
            self.assertEqual(expected_file.read().split("\n"), processed.split("\n"))

    def test_process_clinical_file_with_incorrect_cancer_type(self):
        original, processed = self.call_process_clinical_file(self.data_clinical_incorrect_cancer_type_filename)
        with io.open(self.data_clinical_processed_filename, "r", encoding="utf8") as expected_file:
            self.assertEqual(expected_file.read().split("\n"), processed.split("\n"))

    def test_process_clinical_file_with_meta_headers(self):
        original, processed = self.call_process_clinical_file(self.data_clinical_with_meta_headers_filename)
        self.assertEqual(len(original.split("\n")), len(processed.split("\n")))

    def test_process_clinical_file_no_oncotree_code(self):
        with self.assertRaises(ValueError):
            self.call_process_clinical_file(self.data_clinical_no_oncotree_code_filename)

    def test_process_clinical_file_oncotree_code_last_column(self):
        original, processed = self.call_process_clinical_file(self.data_clinical_oncotree_last_column_filename)
        self.assertEqual(len(original.split("\n")), len(processed.split("\n")))

    def test_audit_clinical_file(self):
        # NA cancer types are not drift; UNKNOWN_CODE is stale; blank code ignored; file untouched
        with io.open(self.data_clinical_incorrect_cancer_type_filename, "r", encoding="utf8") as clinical_file:
            original = clinical_file.read()
        findings = audit_clinical_file(self.oncotree_mappings, self.data_clinical_incorrect_cancer_type_filename)
        self.assertEqual({"UNKNOWN_CODE": 1}, findings["stale_codes"])
        self.assertEqual({}, findings["cancer_type_mismatches"])
        self.assertEqual({}, findings["cancer_type_detailed_mismatches"])
        self.assertTrue(audit_has_findings(findings))
        with io.open(self.data_clinical_incorrect_cancer_type_filename, "r", encoding="utf8") as clinical_file:
            self.assertEqual(original, clinical_file.read())
        # a populated cancer type differing from oncotree is counted per sample; matching values are not drift
        prad = self.oncotree_mappings["PRAD"]
        with tempfile.NamedTemporaryFile(mode="w", prefix="__", suffix=".tmp", delete=False, encoding="utf8") as temp_file:
            self.temp_files.append(temp_file.name)
            temp_file.write("SAMPLE_ID\tONCOTREE_CODE\tCANCER_TYPE\tCANCER_TYPE_DETAILED\n")
            temp_file.write("S1\tPRAD\tWrong Type\tNA\n")
            temp_file.write("S2\tPRAD\tWrong Type\tNA\n")
            temp_file.write("S3\tPRAD\t%s\t%s\n" % (prad["CANCER_TYPE"], prad["CANCER_TYPE_DETAILED"]))
            temp_file.write("S4\tPRAD\tNA\tWrong Detailed\n")
        findings = audit_clinical_file(self.oncotree_mappings, temp_file.name)
        self.assertEqual({}, findings["stale_codes"])
        self.assertEqual({("PRAD", "Wrong Type", prad["CANCER_TYPE"]): 2}, findings["cancer_type_mismatches"])
        self.assertEqual({("PRAD", "Wrong Detailed", prad["CANCER_TYPE_DETAILED"]): 1}, findings["cancer_type_detailed_mismatches"])
        report = io.StringIO()
        report_audit_findings(findings, temp_file.name, out=report)
        self.assertIn("oncotree drift found", report.getvalue())
        self.assertIn("PRAD\tWrong Type\t%s\t2 samples" % (prad["CANCER_TYPE"]), report.getvalue())

    def test_audit_clinical_file_no_drift(self):
        prad = self.oncotree_mappings["PRAD"]
        with tempfile.NamedTemporaryFile(mode="w", prefix="__", suffix=".tmp", delete=False, encoding="utf8") as temp_file:
            self.temp_files.append(temp_file.name)
            temp_file.write("#Sample\tCode\tType\tDetailed\n#Sample\tCode\tType\tDetailed\n#STRING\tSTRING\tSTRING\tSTRING\n#1\t1\t1\t1\n")
            temp_file.write("SAMPLE_ID\tONCOTREE_CODE\tCANCER_TYPE\tCANCER_TYPE_DETAILED\n")
            temp_file.write("S1\tPRAD\t%s\t%s\n" % (prad["CANCER_TYPE"], prad["CANCER_TYPE_DETAILED"]))
            temp_file.write("S2\t\tNA\tNA\n")
            temp_file.write("S3\tPRAD\tNA\t\n")
        findings = audit_clinical_file(self.oncotree_mappings, temp_file.name)
        self.assertFalse(audit_has_findings(findings), findings)
        report = io.StringIO()
        report_audit_findings(findings, temp_file.name, out=report)
        self.assertIn("no oncotree drift", report.getvalue())

    def test_process_clinical_file_adds_metadata_columns(self):
        # 4 metadata rows + missing CANCER_TYPE columns: headers and metadata rows both gain the two attributes
        with tempfile.NamedTemporaryFile(mode="w", prefix="__", suffix=".tmp", delete=False, encoding="utf8") as temp_file:
            self.temp_files.append(temp_file.name)
            temp_file.write("#Sample\tCode\n#Sample\tCode\n#STRING\tSTRING\n#1\t1\n")
            temp_file.write("SAMPLE_ID\tONCOTREE_CODE\nS1\tPRAD\n")
        process_clinical_file(self.oncotree_mappings, temp_file.name, False)
        with io.open(temp_file.name, "r", encoding="utf8") as processed:
            lines = processed.read().split("\n")
        prad = self.oncotree_mappings["PRAD"]
        self.assertEqual("#Sample\tCode\tCancer Type\tCancer Type Detailed", lines[0])
        self.assertEqual("#STRING\tSTRING\tSTRING\tSTRING", lines[2])
        self.assertEqual("#1\t1\t1\t1", lines[3])
        self.assertEqual("SAMPLE_ID\tONCOTREE_CODE\tCANCER_TYPE\tCANCER_TYPE_DETAILED", lines[4])
        self.assertEqual("S1\tPRAD\t%s\t%s" % (prad["CANCER_TYPE"], prad["CANCER_TYPE_DETAILED"]), lines[5])

    def test_audit_clinical_file_no_oncotree_code(self):
        with self.assertRaises(ValueError):
            audit_clinical_file(self.oncotree_mappings, self.data_clinical_no_oncotree_code_filename)

    def call_process_clinical_file(self, data_clinical_filename):
        with io.open(data_clinical_filename, "r", encoding="utf8") as data_clinical_file:
            original = data_clinical_file.read()
        # process_clinical_file modifies the file, so work on a temporary copy
        with tempfile.NamedTemporaryFile(mode="w", prefix="__", suffix=".tmp", delete=False, encoding="utf8") as temp_file:
            self.temp_files.append(temp_file.name)
            temp_file.write(original)
        process_clinical_file(self.oncotree_mappings, temp_file.name, True)
        with io.open(temp_file.name, "r", encoding="utf8") as temp_file_reopened:
            return original, temp_file_reopened.read()


if __name__ == "__main__":
    unittest.main()
