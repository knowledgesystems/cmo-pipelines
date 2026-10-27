"""Characterize deployed deduplication before any separately agreed VAF change."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'remove-duplicate-maf-variants.py'
COLUMNS = ['Hugo_Symbol', 'Entrez_Gene_Id', 'Chromosome', 'Start_Position',
           'End_Position', 'Variant_Classification', 'Tumor_Seq_Allele2',
           'Tumor_Sample_Barcode', 'HGVSp_Short', 't_ref_count', 't_alt_count', 'Center']


def row(ref, alt, center='A', sample='S1'):
    return ['TP53', '7157', '17', '10', '10', 'Missense_Mutation', 'T', sample,
            'p.A1V', str(ref), str(alt), center]


class TestLegacyMafDeduplication(unittest.TestCase):
    def run_dedup(self, records):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'in.maf', Path(directory) / 'out.maf'
            prefix = '#maf\n' + '\t'.join(COLUMNS) + '\n'
            source.write_text(prefix + ''.join('\t'.join(record) + '\n' for record in records))
            result = subprocess.run([sys.executable, str(SCRIPT), '-i', str(source), '-o', str(output)],
                                    check=True, capture_output=True, text=True)
            actual = output.read_text()
            self.assertTrue(actual.startswith(prefix))
            return [line.split('\t') for line in actual[len(prefix):].splitlines()], result.stderr

    def test_singletons_are_unchanged_even_with_invalid_counts(self):
        for record in (row(10, 11), row('NA', 4), row(0, 0)):
            with self.subTest(record=record):
                self.assertEqual(([record], ''), self.run_dedup([record]))

    def test_positive_reference_counts_drop_entire_duplicate_group(self):
        self.assertEqual(([], ''), self.run_dedup([row(10, 11), row(10, 12)]))

    def test_zero_reference_keeps_first_tied_row_without_merging_centers(self):
        first, second = row(0, 1, 'A'), row(0, 5, 'B')
        for records in ([first, second], [second, first]):
            with self.subTest(records=records):
                self.assertEqual(([records[0]], ''), self.run_dedup(records))

    def test_invalid_counts_and_zero_denominators_are_all_retained(self):
        records = [row(5, 'NA'), row('NA', 4), row(0, 0), row(0, 0)]
        actual, errors = self.run_dedup(records)
        self.assertEqual(records, actual)
        self.assertEqual(4, errors.count('VAF cannot be calculated'))

    def test_invalid_rows_remain_alongside_selected_row(self):
        records = [row(0, 1), row(5, 'NA', 'B'), row(0, 5, 'C')]
        actual, errors = self.run_dedup(records)
        self.assertEqual(records[:2], actual)
        self.assertEqual(1, errors.count('VAF cannot be calculated'))

    def test_duplicate_key_is_raw_and_case_sensitive(self):
        records = [row(10, 11, sample='TCGA-AA-1234-01A'),
                   row(10, 12, sample='TCGA-AA-1234-01B'), row(10, 12, sample='s1'), row(10, 12)]
        self.assertEqual((records, ''), self.run_dedup(records))
