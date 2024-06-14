"""Provides unit tests for the validation_utils_py3.py script functionality.

To run the tests, execute the following command from the parent
directory of this script:
        python -m unittest discover test-py3
"""

import unittest
import os

from validation_utils_py3 import AZValidator


class TestAZValidation(unittest.TestCase):

    def test_gene_panel_missing(self):
        base_dir = "test-py3/resources/validation_utils/az_gene_panel"
        sub_dir = os.path.join(base_dir, "missing_panels")
        validator = AZValidator(study_dir=sub_dir)
        
        validator.validate_gene_panels_present(gene_panel_dir=sub_dir)
        num_errors = validator.num_errors
        
        self.assertGreater(num_errors, 0, "AZ validator should fail on missing gene panels")
    
    def test_all_gene_panels_present(self):
        base_dir = "test-py3/resources/validation_utils/az_gene_panel"
        sub_dir = os.path.join(base_dir, "all_panels_present")
        validator = AZValidator(study_dir=sub_dir)
        
        validator.validate_gene_panels_present(gene_panel_dir=sub_dir)
        num_errors = validator.num_errors
        
        self.assertEqual(num_errors, 0, "AZ validator should succeed if all gene panels are present")

if __name__ == "__main__":
    unittest.main()
