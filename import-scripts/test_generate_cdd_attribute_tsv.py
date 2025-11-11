# -*- coding: utf-8 -*-
import unittest

from generate_cdd_attribute_tsv import *

class TestGenerateCDDAttributeTSV(unittest.TestCase):
    def test_ascii(self):
        test_char = ["a", "b", "1", "#", "/"]
        for char in test_char:
            self.assertTrue(is_ASCII(char))
        test_char = ["Â", "‐", "‒", "〝"]
        for char in test_char:
            self.assertFalse(is_ASCII(char))
if __name__ == "__main__":
    unittest.main()