# run all unit tests with:
#     import-scripts> python -m unittest discover
#
# Author: Avery Wang

import unittest
import filecmp
import tempfile
import os
import io
import sys
import shutil

import clinicalfile_utils
from validation_utils import *

class TestValidationUtils(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        resource_dir = "test/resources/validation_utils/"
        cls.test_data = os.path.join(resource_dir, "test_data/")
        # move all data into a temporary directory for manipulation
        cls.temp_dir = os.path.join(resource_dir, tempfile.mkdtemp())
        if os.path.isdir(cls.temp_dir):
            shutil.rmtree(cls.temp_dir)
        shutil.copytree(cls.test_data, cls.temp_dir)

    @classmethod
    def tearDownClass(cls):
        #clean up copied tempdir/data
        shutil.rmtree(cls.temp_dir)

    def test_resolve_source_study_path(self):
        """
            Test that duplicate records (rows) no longer exist
            Records identified by value in specified "identifier" column
        """
        remove_duplicate_rows(os.path.join(self.temp_dir, "data_gene_matrix.txt"), "SAMPLE_ID")
        self.assertTrue(self.no_duplicate_records_present(os.path.join(self.temp_dir, "data_gene_matrix.txt"), "SAMPLE_ID"))

    def no_duplicate_records_present(self, filename, unique_key):
        unique_keys = set()
        header_processed = False
        with open(filename, "r") as f:
            for line in f.readlines():
                if line.startswith("#"):
                    continue
                data = line.rstrip('\n').split('\t')
                if not header_processed:
                    index = data.index(unique_key)
                    header_processed = True
                    continue
                if data[index] in unique_keys:
                    return False
                else:
                    unique_keys.add(data[index])
        return True

if __name__ == '__main__':
    unittest.main()
