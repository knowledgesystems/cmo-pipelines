import tempfile
import unittest
from pathlib import Path

from generate_case_lists import CASE_LIST_CONFIG_HEADER_COLUMNS, generate_case_lists


class CaseListParityTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.study = Path(self.tmp.name)
        self.cases = self.study / 'case_lists'
        self.cases.mkdir()
        self.config = self.study / 'config.tsv'
        (self.study / 'meta_study.txt').write_text('cancer_study_identifier: test\n')

    def configure(self, source, suffix='_all'):
        self.config.write_text('\t'.join(CASE_LIST_CONFIG_HEADER_COLUMNS) + '\n' +
            '\t'.join(['cases.txt', source, '<CANCER_STUDY>' + suffix,
                       'all_cases_in_study', '<CANCER_STUDY>', 'Cases', '<NUM_CASES>']) + '\n')

    def generate(self, overwrite=False):
        generate_case_lists(str(self.config), str(self.cases), str(self.study),
                            'test', overwrite=overwrite)

    def test_virtual_all_is_never_generated_even_with_overwrite(self):
        (self.study / 'meta_study.txt').write_text(
            'cancer_study_identifier: test\nadd_global_case_list: true\n')
        (self.study / 'data.txt').write_text('SAMPLE_ID\nS1\n')
        self.configure('data.txt')
        self.generate()
        self.generate(overwrite=True)
        self.assertEqual([], list(self.cases.iterdir()))

    def test_custom_filename_with_existing_stable_id_is_preserved(self):
        (self.study / 'data.txt').write_text('SAMPLE_ID\nS1\n')
        original = 'stable_id: test_all\ncase_list_ids: CURATED\n'
        (self.cases / 'curated.txt').write_text(original)
        self.configure('data.txt')
        self.generate()
        self.assertFalse((self.cases / 'cases.txt').exists())
        self.assertEqual(original, (self.cases / 'curated.txt').read_text())

    def test_empty_intersection_does_not_restart(self):
        for name, sample in [('a', 'A'), ('b', 'B'), ('c', 'C')]:
            (self.study / name).write_text('SAMPLE_ID\n' + sample + '\n')
        self.configure('a&b&c')
        self.generate()
        self.assertFalse((self.cases / 'cases.txt').exists())

    def test_canonical_mutation_config_generates_sequenced(self):
        self.config = Path(__file__).resolve().parents[1] / 'case_list_config.tsv'
        (self.study / 'data_mutations.txt').write_text('Tumor_Sample_Barcode\nS1\n')
        self.generate()
        self.assertIn('case_list_ids: S1', (self.cases / 'cases_sequenced.txt').read_text())


if __name__ == '__main__':
    unittest.main()
