from pathlib import Path
import tempfile
import unittest

from normalize_tcga_barcodes import get_sample_id, normalize_study


class TestNormalizeTcgaBarcodes(unittest.TestCase):
    def test_java_sample_id_rules(self):
        for original, expected in [
            ('TCGA-A1-A0SB-01A-11D-A142-09', 'TCGA-A1-A0SB-01'),
            ('TCGA-A1-A0SB-Tumor', 'TCGA-A1-A0SB-01'),
            ('TCGA-A1-A0SB-Normal', 'TCGA-A1-A0SB-11'),
            ('TCGA-A1-A0SB', 'TCGA-A1-A0SB-01'),
            ('P-0000001-T01-XXX', 'P-0000001-T01-XXX'),
        ]:
            self.assertEqual(expected, get_sample_id(original))

    def test_consistent_study_ids_without_changing_patient_ids_or_merging_rows(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'case_lists').mkdir()
            sample, normalized = 'TCGA-A1-A0SB-01A-11D', 'TCGA-A1-A0SB-01'
            contents = {
                'data_clinical_sample.txt': '#Échantillon\r\nSAMPLE_ID\tPATIENT_ID\r\n' + sample + '\tTCGA-A1-A0SB\r\n',
                'data_clinical_patient.txt': 'PATIENT_ID\tNOTE\nTCGA-A1-A0SB\tTCGA-A1-A0SB-Tumor\n',
                'data_CNA.txt': 'Hugo_Symbol\t' + sample + '\tMSK-1\nTP53\t0\t1\n',
                'data_mutations.txt': '#sequenced_samples: ' + sample + '\nTumor_Sample_Barcode\tMatched_Norm_Sample_Barcode\tNOTE\n' +
                    sample + '\tTCGA-A1-A0SB-Normal\tTCGA-A1-A0SB-Tumor\n' + sample + '\tNA\tNA\n',
                'data_sv.txt': 'Sample_Id\tSV_Status\n' + sample + '\tSOMATIC\n',
                'data_segments.seg': 'ID\tchrom\n' + sample + '\t1\n',
                'data_timeline.txt': 'PATIENT_ID\tSAMPLE_ID\nTCGA-A1-A0SB\t' + sample + '\n',
                'sequenced_samples.txt': sample + '\nMSK-1\n',
                'case_lists/cases_all.txt': 'case_list_ids: ' + sample + '\tMSK-1\n',
            }
            for filename, body in contents.items():
                (root / filename).write_bytes(body.encode('utf-8'))
            self.assertEqual(8, normalize_study(root))
            for filename in contents:
                body = (root / filename).read_bytes().decode('utf-8')
                self.assertNotIn(sample, body)
                if filename != 'data_clinical_patient.txt':
                    self.assertIn(normalized, body)
            self.assertEqual(contents['data_clinical_patient.txt'], (root / 'data_clinical_patient.txt').read_text())
            mutations = (root / 'data_mutations.txt').read_text().splitlines()
            self.assertEqual(normalized + '\tTCGA-A1-A0SB-11\tTCGA-A1-A0SB-Tumor', mutations[2])
            self.assertEqual(4, len(mutations))
            self.assertIn(b'\r\n', (root / 'data_clinical_sample.txt').read_bytes())
            timestamps = {p: p.stat().st_mtime_ns for p in root.rglob('*.txt')}
            self.assertEqual(0, normalize_study(root))
            self.assertEqual(timestamps, {p: p.stat().st_mtime_ns for p in timestamps})

    def test_collisions_leave_all_sources_unchanged(self):
        for filename, body in [
            ('data_clinical_sample.txt', 'SAMPLE_ID\nTCGA-A1-A0SB-01A\nTCGA-A1-A0SB-01B\n'),
            ('data_CNA.txt', 'Hugo_Symbol\tTCGA-A1-A0SB-01A\tTCGA-A1-A0SB-01B\n'),
        ]:
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                untouched = root / 'aaa.maf'
                untouched.write_text('Tumor_Sample_Barcode\nTCGA-A1-A0SB-01A\n')
                (root / filename).write_text(body)
                before = {p: p.read_bytes() for p in root.iterdir()}
                with self.assertRaisesRegex(ValueError, 'normalize to the same sample'):
                    normalize_study(root)
                self.assertEqual(before, {p: p.read_bytes() for p in root.iterdir()})
