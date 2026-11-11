"""Check shell callers stop after preprocessing failures and preserve their inputs."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]


class TestPreprocessingShellFailures(unittest.TestCase):
    def test_dmp_failure_propagation(self):
        source = (SCRIPTS / 'dmp-import-vars-functions.sh').read_text()
        function = re.search(r'^function addCancerTypeCaseLists \{\n.*?^\}',
                             source, re.MULTILINE | re.DOTALL).group(0)
        for stage, status, calls in [('conversion', 7, 1), ('cancer', 8, 2),
                                     ('consent', 9, 3), ('none', 0, 3)]:
            with self.subTest(stage=stage), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                study = root / 'study with spaces'
                lists = study / 'case_lists'
                lists.mkdir(parents=True)
                existing = lists / 'existing.txt'
                existing.write_text('original\n')
                log = root / 'calls.jsonl'
                stub = root / 'python'
                stub.write_text('#!' + sys.executable + '\n' + '''import json, os, sys
args = sys.argv[1:]
with open(os.environ['CALL_LOG'], 'a') as out:
    out.write(json.dumps(args) + '\\n')
stage = ('conversion' if args[0].endswith('oncotree_code_converter.py') else
         'consent' if '--attribute=PARTC_CONSENTED_12_245' in args else 'cancer')
if stage == os.environ['FAIL_STAGE']:
    sys.exit({'conversion': 7, 'cancer': 8, 'consent': 9}[stage])
''')
                stub.chmod(0o700)
                env = dict(os.environ, PYTHON_BINARY=str(stub), PYTHON3_BINARY=str(stub),
                           PORTAL_HOME=str(root), ONCOTREE_VERSION_TO_USE='test',
                           CALL_LOG=str(log), FAIL_STAGE=stage)
                result = subprocess.run(['bash', '-c', function +
                                         '\naddCancerTypeCaseLists "$1" mskimpact clinical.txt ""',
                                         'test', str(study)], env=env, stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE)
                self.assertEqual(status, result.returncode, result.stderr)
                commands = [json.loads(line) for line in log.read_text().splitlines()]
                self.assertEqual(calls, len(commands))
                self.assertIn(str(study / 'clinical.txt'), commands[0])
                if stage == 'conversion':
                    self.assertEqual('original\n', existing.read_text())

    def test_tmp_az_outputs_and_failure_status(self):
        columns = ['Hugo_Symbol', 'Entrez_Gene_Id', 'Chromosome', 'Start_Position',
                   'End_Position', 'Variant_Classification', 'Tumor_Seq_Allele2',
                   'Tumor_Sample_Barcode', 'HGVSp_Short', 't_ref_count', 't_alt_count']
        row = ['TP53', '7157', '17', '10', '10', 'Missense_Mutation', 'T', 'S1',
               'p.A1V', '0', '5']
        expected = '\t'.join(columns) + '\n' + '\t'.join(row) + '\n'
        maf = expected + '\t'.join(row) + '\n'
        for second_exists in (True, False):
            with self.subTest(second_exists=second_exists), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / 'scripts').symlink_to(SCRIPTS, target_is_directory=True)
                first = root / 'data_mutations_extended.txt'
                second = root / 'data_nonsignedout_mutations.txt'
                first.write_text(maf)
                if second_exists:
                    second.write_text(maf)
                env = dict(os.environ, PORTAL_HOME=str(root), PYTHON3_BINARY=sys.executable,
                           AZ_MSK_IMPACT_DATA_HOME=str(root))
                result = subprocess.run(['bash', str(SCRIPTS / 'tmp-az.sh')], env=env,
                                        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                self.assertEqual(0 if second_exists else 1, result.returncode, result.stderr)
                self.assertEqual(expected if second_exists else maf, first.read_text())
                if second_exists:
                    self.assertEqual(expected, second.read_text())
                    self.assertFalse((root / 'data_mutations_extended_merged.txt').exists())
                    self.assertFalse((root / 'data_nonsignedout_mutations_merged.txt').exists())
                else:
                    self.assertFalse(second.exists())
