import unittest

from dags.public_v7_runtime import validate_cancer_types


class CancerTypeHierarchyTest(unittest.TestCase):
    def test_tissue_parent_terminates_a_chain_with_or_without_a_root_row(self):
        rows = [{"cancerTypeId": "aa", "parent": "soft_tissue"},
                {"cancerTypeId": "soft_tissue", "parent": "tissue"}]
        validate_cancer_types(rows)
        validate_cancer_types(rows + [{"cancerTypeId": "tissue", "parent": None}])

    def test_invalid_hierarchies_still_fail(self):
        for edges in ([], [("a", "tissue"), ("a", "tissue")],
                      [("a", "missing")], [("a", "b"), ("b", "a")]):
            with self.subTest(edges=edges), self.assertRaises(ValueError):
                validate_cancer_types([{"cancerTypeId": child, "parent": parent}
                                       for child, parent in edges])
