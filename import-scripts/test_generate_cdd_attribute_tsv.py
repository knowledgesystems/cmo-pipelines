import unittest
import requests
import os

from generate_cdd_attribute_tsv import *

class TestGenerateCDDAttributeTSV(unittest.TestCase): 
    def test_github(self):
        results = generate_cdd_attribute_tsv.get_github_credentials("credential.txt")
        self.assertIsInstance(results, tuple)
        for i in results:
            self.assertIsInstance(i, str)
    
    def test_uri_dict(self):
        username, password = get_github_credentials("credential.txt")
        urifile = requests.get(generate_cdd_attribute_tsv.GITHUB_URI_MAPPINGS_FILE_URL, auth=(username, password), headers=generate_cdd_attribute_tsv.HEADERS)
        results = create_uri_dictionary(urifile)
        self.assertIsInstance(results, dict)
        for k, v in results.items():
            self.assertIsInstance(k, str)
            self.assertIsInstance(v, str)
    
    def test_ascii(self):
        test_char = ["a", "b", "1", "#", "/"]
        for char in test_char:
            self.assertTrue(is_ASCII(char))
        test_char = ["Â", "‐", "‒", "〝"]
        for char in test_char:
            self.assertFalse(is_ASCII(char))
        
    def test_load_attributes(self):
        username, password = get_github_credentials("credential.txt")
        urifile = requests.get(GITHUB_URI_MAPPINGS_FILE_URL, auth=(username, password), headers=HEADERS)
        uri_dict = create_uri_dictionary(urifile)
        results = load_new_cdd_attributes("new_cdd_attributes.tsv", uri_dict)
        for i in results:
            self.assertIsInstance(i, dict)
            for k, v in i.items():
                self.assertIsInstance(k, str)
                self.assertIsInstance(v, str)
            key_list = ["ATTRIBUTE_TYPE", "CONCEPT_ID", "DESCRIPTIONS", "DISPLAY_NAME", "NORMALIZED_COLUMN_HEADER", "PRIORITY"]
            for key in key_list:
                self.assertIn(key, i.keys())
  
    def test_create_data(self):
        new_uri_mapping_file_path = os.path.join(".", generate_cdd_attribute_tsv.NEW_URI_MAPPING_FILENAME)
        results = create_data(new_uri_mapping_file_path, generate_cdd_attribute_tsv.GITHUB_URI_MAPPINGS_FILE_URL)
        key_list = ["message", "sha", "content", "path"]
        for k in key_list:
            self.assertIn(k, results.keys())

if __name__ == "__main__":
    unittest.main()