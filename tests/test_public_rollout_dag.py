"""Exercise the real task bodies without installing/running an Airflow scheduler."""
import ast
import logging
import os
import signal
import subprocess
import sys
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

from dags import public_rollout


class TaskTests(unittest.TestCase):
    def test_diagnostic_image_and_import_only_tracing(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_public_hackathon.py'
        wanted = {'K8S_IMAGE_VALIDATE', '_POD_OVERRIDE_VALIDATE', '_POD_OVERRIDE_IMPORT'}
        nodes = [node for node in ast.parse(source.read_text()).body
                 if (isinstance(node, ast.FunctionDef) and node.name == '_make_cbioportal_pod_override')
                 or (isinstance(node, ast.Assign) and any(
                     isinstance(target, ast.Name) and target.id in wanted for target in node.targets))]
        env = dict(k8s=SimpleNamespace(V1EnvVar=SimpleNamespace,
                                      V1ResourceRequirements=SimpleNamespace),
                   _SAML2AWS_ENV=SimpleNamespace(name='SAML2AWS_SKIP', value='true'),
                   _pod_override=lambda **kwargs: kwargs)
        exec(compile(ast.Module(body=nodes, type_ignores=[]), str(source), 'exec'), env)
        for name in ('_POD_OVERRIDE_VALIDATE', '_POD_OVERRIDE_IMPORT'):
            self.assertEqual(env[name]['image'], 'ghcr.io/cbioportal/containerized-importer-core@sha256:59fd2371e1e05245dde8e9a123feb832cc288cfa55619d6d941e66a2a4fbc040')
        validation_env = {v.name: v.value for v in env['_POD_OVERRIDE_VALIDATE']['env']}
        import_env = {v.name: v.value for v in env['_POD_OVERRIDE_IMPORT']['env']}
        self.assertNotIn('JAVA_TOOL_OPTIONS', validation_env)
        self.assertEqual(import_env['JAVA_TOOL_OPTIONS'], '-Dcbio.jdbc.diagnostics=true')
        self.assertEqual(import_env['JAVA_OPTS'], '-Xmx22g')
        self.assertEqual(env['_POD_OVERRIDE_IMPORT']['resources'].limits, {'memory': '26Gi'})

    def setUp(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_public_hackathon.py'
        tree = ast.parse(source.read_text())
        nodes = []
        wanted = {'_rollout_manifest', '_rollout_entry', 'pull_and_validate_study',
                  'collect_valid_studies', 'import_into_standby_database'}
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name in wanted:
                node.decorator_list = []
                nodes.append(node)
        self.assertEqual(len(nodes), len(wanted))
        @contextmanager
        def references(*args):
            yield Path('/references')
        @contextmanager
        def study(*args):
            yield Path('/study')
        self.env = dict(Path=Path, AirflowException=RuntimeError,
                        sys=SimpleNamespace(executable='python'),
                        VALIDATE_SCRIPT_PATH='/importer/validateStudies.py',
                        IMPORT_SCRIPT_PATH='/importer/metaImport.py', S3_MOUNT_PATH='/s3',
                        selected_entries=public_rollout.selected_entries,
                        validation_command=public_rollout.validation_command,
                        reference_input=references, study_input=study,
                        _skip_if_requested=Mock(), _activate_standby_properties=Mock(),
                        _run_and_stream=Mock(return_value=SimpleNamespace(returncode=0)))
        exec(compile(ast.Module(body=nodes, type_ignores=[]), str(source), 'exec'), self.env)
        self.manifest = dict(selected_study_ids=['study'], studies=[dict(study_id='study',
            key='staging/study.tar.gz', sha256='a' * 64, status='passed', validator_exit_code=0)])
        self.load_manifest = self.env['_rollout_manifest']
        self.env['_rollout_manifest'] = Mock(return_value=self.manifest)

    def test_public_requires_manifest(self):
        with self.assertRaisesRegex(RuntimeError, 'require a pinned'):
            self.load_manifest({'database': 'public'})
        self.assertIsNone(self.load_manifest({'database': 'containerized'}))

    def test_public_requires_explicit_no_swap(self):
        self.env['_study_prefix'] = lambda params: 'staging'
        with self.assertRaisesRegex(RuntimeError, 'requires transfer_deployment_color'):
            self.load_manifest({'database': 'public', 'rollout_manifest_key': 'manifest.json'})

    def test_command_streaming_and_timeout(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_public_hackathon.py'
        node = next(n for n in ast.parse(source.read_text()).body
                    if isinstance(n, ast.FunctionDef) and n.name == '_run_and_stream')
        env = dict(os=os, signal=signal, subprocess=subprocess,
                   logger=logging.getLogger(self.id()))
        exec(compile(ast.Module(body=[node], type_ignores=[]), str(source), 'exec'), env)
        result = env['_run_and_stream']([sys.executable, '-c', 'print("hello")'], timeout=5)
        self.assertEqual(result.stdout, 'hello')
        with self.assertRaises(subprocess.TimeoutExpired):
            env['_run_and_stream']([sys.executable, '-c', 'import time; time.sleep(10)'], timeout=0.1)

    def test_collection_cannot_drop_a_preselected_study(self):
        with self.assertRaises(ValueError):
            self.env['collect_valid_studies']([None], params={'database': 'public'})
        self.assertEqual(['study'], self.env['collect_valid_studies'](['study']))

    def test_import_uses_references_and_failure_is_not_ignored(self):
        self.env['import_into_standby_database'](['study'], {'database': 'public'})
        command = self.env['_run_and_stream'].call_args.args[0]
        self.assertNotIn('-n', command)
        self.assertIn('-p', command)
        self.assertIn('--oncotree-file', command)
        self.env['_run_and_stream'].return_value.returncode = 1
        with self.assertRaisesRegex(RuntimeError, 'Validator-passing study failed import'):
            self.env['import_into_standby_database'](['study'], {'database': 'public'})

    def test_reference_failure_prevents_importer_activation(self):
        self.env['reference_input'] = Mock(side_effect=ValueError('Reference mismatch'))
        with self.assertRaisesRegex(ValueError, 'Reference mismatch'):
            self.env['import_into_standby_database'](['study'], {'database': 'public'})
        self.env['_activate_standby_properties'].assert_not_called()
        self.env['_run_and_stream'].assert_not_called()

    def test_validation_failure_cannot_filter_out_a_selected_study(self):
        with patch.object(Path, 'mkdir'):
            self.env['_run_and_stream'].return_value.returncode = 3
            self.assertEqual('study', self.env['pull_and_validate_study']('study'))
            self.env['_run_and_stream'].return_value.returncode = 1
            with self.assertRaisesRegex(RuntimeError, 'Previously passing study'):
                self.env['pull_and_validate_study']('study')


if __name__ == '__main__':
    unittest.main()
