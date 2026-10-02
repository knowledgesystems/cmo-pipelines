# run all unit tests with:
#     import-scripts> python -m unittest discover test
#
# TODO this only tests process_clinical_file, other methods should be tested too
# TODO fix the removing data bug
# TODO add some unit tests which explore the different modes related to --force option and overwriting
#
# Author: Manda Wilson

import unittest
import tempfile
import os.path
import os
import io

from oncotree_code_converter import *

class TestOncotreeCodeConverter(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        resource_dir = "test/resources/oncotree_code_converter/"
        cls.oncotree_filename = os.path.join(resource_dir, "tumor_types.txt")
        cls.oncotree_json_filename = os.path.join(resource_dir, "mock_oncotree_tumortypes_api_output_flat.json")
        cls.expected_oncotree_code_mappings_filename = os.path.join(resource_dir, "oncotree_code_mappings_expected.txt")
        cls.set_up_oncotree() # read file and initialize cls.oncotree_mappings
        cls.data_clinical_no_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_no_cancer_type.txt")
        cls.data_clinical_processed_filename = os.path.join(resource_dir, "data_clinical_sample_processed.txt")
        cls.data_clinical_with_meta_headers_filename = os.path.join(resource_dir, "data_clinical_sample_with_meta_headers.txt")
        cls.data_clinical_oncotree_last_column_filename = os.path.join(resource_dir, "data_clinical_sample_oncotree_last_column.txt")
        cls.data_clinical_incorrect_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_incorrect_cancer_type.txt")
        cls.data_clinical_no_oncotree_code_filename = os.path.join(resource_dir, "data_clinical_sample_no_oncotree_code.txt")
        cls.temp_files = []

    def generate_cancer_type_mismatch_error_msg(self, actual_cancer_type, expected_cancer_type, oncotree_code, attribute):
        return "Actual %s '%s' does not match expected '%s' for oncotree code '%s'" % (attribute, actual_cancer_type, expected_cancer_type, oncotree_code)

    def test_extract_oncotree_code_mappings_from_oncotree_json(self):
        with io.open(self.expected_oncotree_code_mappings_filename, 'r', encoding='utf8') as expected_oncotree_mappings_file:
            expected_oncotree_mappings_raw = expected_oncotree_mappings_file.read().strip('\n').split('\n')
        for mappings_line in expected_oncotree_mappings_raw:
            fields = mappings_line.split('\t')
            oncotree_code = fields[0]
            expected_cancer_type = fields[2]
            expected_cancer_type_detailed = fields[1]
            try:
                oncotree_node_info = self.oncotree_mappings[oncotree_code]
            except KeyError:
                self.fail("Failed to extract info for oncotree code: %s" % (oncotree_code))
            actual_cancer_type = oncotree_node_info['CANCER_TYPE']
            actual_cancer_type_detailed = oncotree_node_info['CANCER_TYPE_DETAILED']
            self.assertIsNotNone(actual_cancer_type, "Failed to set CANCER_TYPE for oncotree code: '%s'" % (oncotree_code))
            self.assertIsNotNone(actual_cancer_type_detailed, "Failed to set CANCER_TYPE_DETAILED for oncotree code: '%s'" % (oncotree_code))
            self.assertEqual(actual_cancer_type, expected_cancer_type, self.generate_cancer_type_mismatch_error_msg(actual_cancer_type, expected_cancer_type, oncotree_code, 'CANCER_TYPE'))
            self.assertEqual(actual_cancer_type_detailed, expected_cancer_type_detailed, self.generate_cancer_type_mismatch_error_msg(actual_cancer_type_detailed, expected_cancer_type_detailed, oncotree_code, 'CANCER_TYPE_DETAILED'))

    def test_process_clinical_file(self):
        original_clinical_data, processed_clinical_data = self.call_process_clinical_file(self.data_clinical_no_cancer_type_filename)
        with open(self.data_clinical_processed_filename, 'r') as data_clinical_processed_file:
            expected_processed_clinical_data = data_clinical_processed_file.read()
            self.assertEqual(expected_processed_clinical_data.split("\n"), processed_clinical_data.split("\n"))

    def test_process_clinical_file_with_incorrect_cancer_type(self):
        original_clinical_data, processed_clinical_data = self.call_process_clinical_file(self.data_clinical_incorrect_cancer_type_filename)
        with open(self.data_clinical_processed_filename, 'r') as data_clinical_processed_file:
            expected_processed_clinical_data = data_clinical_processed_file.read()
            self.assertEqual(expected_processed_clinical_data.split("\n"), processed_clinical_data.split("\n"))

    def test_process_clinical_file_with_meta_headers(self):
        original_clinical_data, processed_clinical_data = self.call_process_clinical_file(self.data_clinical_with_meta_headers_filename)
        # make sure we didn't lose any lines (data is deleted if there is a problem)
        self.assertEqual(len(original_clinical_data.split("\n")), len(processed_clinical_data.split("\n")))

    def test_process_clinical_file_no_oncotree_code(self):
        # we expect a ValueError if there is no ONCOTREE_CODE column
        with self.assertRaises(ValueError) as context:
            self.call_process_clinical_file(self.data_clinical_no_oncotree_code_filename)

    def test_process_clinical_file_fail_if_oncotree_code_last_column(self):
        # this previously fail because ONCOTREE_CODE has a \n at the end in the header
        # a print statement in process_clinical_file revealed:
        # header is: ['SAMPLE_ID', 'PATIENT_ID', 'ONCOTREE_CODE\n', 'CANCER_TYPE', 'CANCER_TYPE_DETAILED']
        original_clinical_data, processed_clinical_data = self.call_process_clinical_file(self.data_clinical_oncotree_last_column_filename)
        # make sure we didn't lose any lines (data is deleted if there is a problem)
        self.assertEqual(len(original_clinical_data.split("\n")), len(processed_clinical_data.split("\n")))

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
        original_clinical_data = ""
        processed_clinical_data = ""
        temp_data_clinical_filename = ""

        with open(data_clinical_filename, 'r') as data_clinical_file:
            original_clinical_data = data_clinical_file.read()

        # make a temporary copy of this file, the file is modified by process_clinical_file
        with tempfile.NamedTemporaryFile(mode='w', prefix='__', suffix='.tmp', delete=False) as temp_data_clinical_file:
            temp_data_clinical_filename = temp_data_clinical_file.name
            self.temp_files.append(temp_data_clinical_filename) # make sure we clean up even if there are exceptions
            temp_data_clinical_file.write(original_clinical_data)

        process_clinical_file(self.oncotree_mappings, temp_data_clinical_filename, True)

        # repopen since it has been written to
        with open(temp_data_clinical_filename, 'r') as temp_data_clinical_file:
            processed_clinical_data = temp_data_clinical_file.read()

        return original_clinical_data, processed_clinical_data

    @classmethod
    def set_up_oncotree(cls):
        with io.open(cls.oncotree_json_filename, 'r', encoding='utf8') as oncotree_json_file:
            oncotree_json = oncotree_json_file.read()
        cls.oncotree_mappings = extract_oncotree_code_mappings_from_oncotree_json(oncotree_json)

    @classmethod
    def tearDownClass(cls):
        for filename in cls.temp_files:
            if os.path.exists(filename):
                os.remove(filename)

if __name__ == '__main__':
    unittest.main()
