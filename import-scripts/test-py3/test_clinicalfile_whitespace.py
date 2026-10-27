"""Regression coverage for the original header/metadata/data parsing contract."""
from pathlib import Path
import tempfile
import unittest

import clinicalfile_utils as clinical
from oncotree_code_converter import audit_clinical_file, process_clinical_file


class TestClinicalWhitespace(unittest.TestCase):
    def test_header_trims_whole_line_not_individual_fields(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'clinical.txt'
            for ending in ('\n', '\r\n'):
                with self.subTest(ending=repr(ending)):
                    path.write_bytes((' SAMPLE_ID \tPATIENT_ID \t' + ending).encode())
                    self.assertEqual([' SAMPLE_ID ', 'PATIENT_ID'], clinical.get_header(path))

    def test_data_preserves_spaces_and_trailing_empty_values(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'clinical.txt'
            path.write_bytes(b'SAMPLE_ID\tONCOTREE_CODE\tNOTE\r\n S1 \tPRAD \t\r\n')
            self.assertEqual([{'SAMPLE_ID': ' S1 ', 'ONCOTREE_CODE': 'PRAD ', 'NOTE': ''}],
                             clinical.parse_file(path, True))
            self.assertEqual([], clinical.parse_file(path, False))

    def test_metadata_trims_whole_line_and_comments_only_lose_newline(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'clinical.txt'
            path.write_text('# Sample \tCode \t\n# description \tCode description \t\n'
                            '#STRING\tSTRING\t\n#1\t1\t\nSAMPLE_ID\tONCOTREE_CODE\nS1\tPRAD\n')
            self.assertEqual('# Sample \tCode \t', clinical.get_comments(path)[0])
            metadata = clinical.get_all_metadata_lines(str(path))
            self.assertEqual(['# Sample ', 'Code'], metadata[clinical.DISPLAY_NAME])
            self.assertEqual(['# description ', 'Code description'], metadata[clinical.DESCRIPTION])

    def test_converter_preserves_data_spaces_and_trims_header_end(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'clinical.txt'
            path.write_bytes(b'SAMPLE_ID\tONCOTREE_CODE\tNOTE \t\r\nS1\tPRAD\t note \r\n')
            mapping = {'PRAD': {'CANCER_TYPE': 'Prostate Cancer', 'CANCER_TYPE_DETAILED': 'Prostate Adenocarcinoma'}}
            process_clinical_file(mapping, str(path), False)
            self.assertEqual('SAMPLE_ID\tONCOTREE_CODE\tNOTE\tCANCER_TYPE\tCANCER_TYPE_DETAILED\n'
                             'S1\tPRAD\t note \tProstate Cancer\tProstate Adenocarcinoma\n', path.read_text())

    def test_audit_reports_spaced_codes_without_rewriting(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'clinical.txt'
            original = b'SAMPLE_ID\tONCOTREE_CODE\nS1\tPRAD \n'
            path.write_bytes(original)
            findings = audit_clinical_file({'PRAD': {}}, str(path))
            self.assertEqual({'PRAD ': 1}, findings['stale_codes'])
            self.assertEqual(original, path.read_bytes())
