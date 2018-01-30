# run all unit tests with:
#     import-scripts> python -m unittest discover
#
# TODO this only tests process_clinical_file, other methods should be tested too
# TODO fix the removing data bug
#
# Author: Manda Wilson

import unittest
import tempfile
import os.path
import os

from oncotree_code_converter import *

class TestOncotreeCodeConverter(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        resource_dir = "test/resources/oncotree_code_converter/"
        cls.oncotree_filename = os.path.join(resource_dir, "tumor_types.txt")
        cls.set_up_oncotree() # read file and initialize cls.oncotree
        cls.data_clinical_no_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_no_cancer_type.txt")
        cls.data_clinical_processed_filename = os.path.join(resource_dir, "data_clinical_sample_processed.txt")
        cls.data_clinical_with_meta_headers_filename = os.path.join(resource_dir, "data_clinical_sample_with_meta_headers.txt")
        cls.data_clinical_oncotree_last_column_filename = os.path.join(resource_dir, "data_clinical_sample_oncotree_last_column.txt")
        cls.data_clinical_incorrect_cancer_type_filename = os.path.join(resource_dir, "data_clinical_sample_incorrect_cancer_type.txt")
        cls.data_clinical_no_oncotree_code_filename = os.path.join(resource_dir, "data_clinical_sample_no_oncotree_code.txt")
        cls.temp_files = []

    def test_get_oncotree(self):
        self.assertGreaterEqual(len(self.oncotree), 654)

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

        process_clinical_file(self.oncotree, temp_data_clinical_filename)

        # repopen since it has been written to
        with open(temp_data_clinical_filename, 'r') as temp_data_clinical_file:
            processed_clinical_data = temp_data_clinical_file.read()

        return original_clinical_data, processed_clinical_data
   
    @classmethod
    def set_up_oncotree(cls):
        with open(cls.oncotree_filename, 'r') as oncotree_file:
            cls.oncotree = oncotree_file.read().split('\n')

    @classmethod
    def tearDownClass(cls):
        for filename in cls.temp_files:
            if os.path.exists(filename):
                os.remove(filename)        
        

if __name__ == '__main__':
    unittest.main()
