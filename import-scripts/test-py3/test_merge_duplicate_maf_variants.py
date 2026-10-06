"""Tests for the new MAF merger's selection rules and command-line output."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'merge-duplicate-maf-variants.py'
SPEC = importlib.util.spec_from_file_location('maf_merge', SCRIPT)
MAF = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MAF)


class TestMergeDuplicateMafVariants(unittest.TestCase):
    def test_java_preferences_and_centers(self):
        columns = ['Matched_Norm_Sample_Barcode', 'Validation_Status', 'Mutation_Status', 'Center', 't_alt_count']
        first = ['TCGA-AA-1234-11A', 'Unknown', 'Somatic', 'NA', '2']
        cases = [(['TCGA-AA-1234-11A', 'Unknown', 'Somatic', 'NA', '9'], first),
                 (['TCGA-AA-1234-10A', 'Unknown', 'Somatic', 'NA', '1'],
                  ['TCGA-AA-1234-10A', 'Unknown', 'Somatic', 'NA', '1']),
                 (['TCGA-AA-1234-11A', 'VALIDATED', 'Somatic', 'NA', '1'],
                  ['TCGA-AA-1234-11A', 'VALIDATED', 'Somatic', 'NA', '1']),
                 (['TCGA-AA-1234-11A', 'Unknown', 'Germline', 'NA', '1'],
                  ['TCGA-AA-1234-11A', 'Unknown', 'Germline', 'NA', '1'])]
        for second, expected in cases:
            with self.subTest(second=second):
                result = MAF.merge_duplicate_records(['\t'.join(first), '\t'.join(second)], columns)
                self.assertEqual(expected, result.rstrip('\n').split('\t'))
        records = ['N\tUnknown\tSomatic\tB;NA\t2', 'N\tValid\tSomatic\tA\t1',
                   'N\tValid\tGermline\tC\t1']
        self.assertEqual('N\tValid\tGermline\tA;B;C\t1\n',
                         MAF.merge_duplicate_records(records, columns))

    def test_cli_retains_one_duplicate_without_using_read_counts(self):
        for counts in ([], ['t_ref_count', 't_alt_count']):
            with self.subTest(counts=counts), tempfile.TemporaryDirectory() as directory:
                source, output = Path(directory) / 'in.maf', Path(directory) / 'out.maf'
                # The Java parser accepts the first non-comment header in any column order.
                columns = MAF.KEY_COLUMNS + ['Hugo_Symbol'] + counts
                row = ['1', '1', '10', '10', 'Missense_Mutation', 'T', 'S1', 'p.A1V', 'GENE1']
                if counts:
                    row += ['8', '2']
                source.write_text('#maf\n' + '\t'.join(columns) + '\n' + ('\t'.join(row) + '\n') * 2)
                subprocess.run([sys.executable, str(SCRIPT), '-i', str(source), '-o', str(output)],
                               check=True, capture_output=True)
                self.assertEqual(['#maf', '\t'.join(columns), '\t'.join(row)], output.read_text().splitlines())
