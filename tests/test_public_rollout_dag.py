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
from dags.study_sources import DEFAULT_BUCKET, bucket_mount, parse_study_source


class TaskTests(unittest.TestCase):
    def test_diagnostic_image_and_import_only_tracing(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_containerized_dag.py'
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
            self.assertEqual(env[name]['image'], 'ghcr.io/cbioportal/containerized-importer-core@sha256:b0b9f7b30db842a20dfbf145aeadaeb04419e6854b4a6b8c4633948219e83d98')
        validation_env = {v.name: v.value for v in env['_POD_OVERRIDE_VALIDATE']['env']}
        import_env = {v.name: v.value for v in env['_POD_OVERRIDE_IMPORT']['env']}
        self.assertNotIn('JAVA_TOOL_OPTIONS', validation_env)
        self.assertEqual(import_env['JAVA_TOOL_OPTIONS'], '-Dcbio.jdbc.diagnostics=true')
        self.assertEqual(import_env['JAVA_OPTS'], '-Xmx22g')
        self.assertEqual(env['_POD_OVERRIDE_IMPORT']['resources'].limits, {'memory': '26Gi'})

    def setUp(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_containerized_dag.py'
        tree = ast.parse(source.read_text())
        nodes = []
        wanted = {'_validate_skips', '_public_reference_manifest', 'pull_and_validate_study',
                  '_public_study_entries', '_input_root',
                  'collect_valid_studies', 'import_into_standby_database',
                  'create_derived_tables_in_standby_database', 'verify_studies_exist'}
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
                        parse_study_source=parse_study_source, DEFAULT_BUCKET=DEFAULT_BUCKET,
                        bucket_mount=bucket_mount,
                        validation_command=public_rollout.validation_command,
                        reference_input=references, study_input=study,
                        _skip_if_requested=Mock(), _activate_standby_properties=Mock(),
                        _run_and_stream=Mock(return_value=SimpleNamespace(returncode=0)))
        exec(compile(ast.Module(body=nodes, type_ignores=[]), str(source), 'exec'), self.env)
        self.manifest = dict(selected_study_ids=['study'], references={}, studies=[dict(study_id='study',
            key='study.tar.gz', sha256='a' * 64, status='passed', validator_exit_code=0)])
        self.env['_public_reference_manifest'] = Mock(return_value=self.manifest)
        self.env['_public_study_entries'] = Mock(return_value={'study': self.manifest['studies'][0]})

    def test_public_preflight_pins_selected_archive_without_manifest_parameters(self):
        from tempfile import TemporaryDirectory
        with TemporaryDirectory() as tmp:
            archive = Path(tmp) / 'study.tar.gz'
            archive.write_bytes(b'selected archive')
            source = f's3://{DEFAULT_BUCKET}/study'
            self.env.update(search_buckets=list, resolve_studies=Mock(return_value=[source]),
                            bucket_mount=lambda bucket: Path(tmp), SKIPPABLE_TASK_IDS=[],
                            checked_path=public_rollout.checked_path, sha256=public_rollout.sha256)
            ti = Mock()
            self.assertEqual(self.env['verify_studies_exist'](['study'], {'target': 'public'}, ti), [source])
            entry = ti.xcom_push.call_args.kwargs['value'][source]
            self.assertEqual(entry['sha256'], public_rollout.sha256(archive))

    def test_command_streaming_and_timeout(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_containerized_dag.py'
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
        with self.assertRaisesRegex(RuntimeError, 'Every selected'):
            self.env['collect_valid_studies']([None], params={'target': 'public'})
        self.assertEqual(['study'], self.env['collect_valid_studies'](['study'], {'target': 'public'}))

    def test_import_uses_references_and_failure_is_not_ignored(self):
        self.env['import_into_standby_database'](['study'], {'target': 'public'})
        command = self.env['_run_and_stream'].call_args.args[0]
        self.assertNotIn('-n', command)
        self.assertIn('-p', command)
        self.assertNotIn('--oncotree-file', command)
        self.env['_run_and_stream'].return_value.returncode = 1
        with self.assertRaisesRegex(RuntimeError, 'Validator-passing study failed import'):
            self.env['import_into_standby_database'](['study'], {'target': 'public'})

    def test_marks_only_imported_ids_available_after_rebuild(self):
        run = self.env['_run_and_stream']
        self.env['create_derived_tables_in_standby_database'](
            ['study', 's3://cdsi-testing/other'], {'target': 'public'})
        self.env['_activate_standby_properties'].assert_called_once_with('public')
        self.assertEqual([c.args[0] for c in run.call_args_list], [
            ['python', '/importer/metaImport.py', 'derive-tables', '--derived-table-sql', '/tmp/clickhouse.sql'],
            ['python', '/importer/cbioportalImporter.py', 'make-studies-available', '-ids', 'study,other']])

    def test_rebuild_and_availability_errors_fail_task(self):
        for codes, message in [([1], 'rebuild failed'), ([0, 1], 'mark studies available')]:
            with self.subTest(codes=codes):
                run = self.env['_run_and_stream']
                run.reset_mock()
                run.side_effect = [SimpleNamespace(returncode=c) for c in codes]
                with self.assertRaisesRegex(Exception, message):
                    self.env['create_derived_tables_in_standby_database'](['study'], {'target': 'public'})
                self.assertEqual(run.call_count, len(codes))

    def test_skipped_import_does_not_publish_selected_studies(self):
        self.env['create_derived_tables_in_standby_database'](
            ['study'], {'target': 'public', 'skip_tasks': ['import_into_standby_database']})
        self.env['_run_and_stream'].assert_called_once()

    def test_transfer_after_import_requires_rebuild(self):
        task_id = 'create_derived_tables_in_standby_database'
        self.env['SKIPPABLE_TASK_IDS'] = [task_id]
        with self.assertRaisesRegex(RuntimeError, 'before transferring traffic'):
            self.env['_validate_skips']({'skip_tasks': [task_id]})

    def test_reference_failure_prevents_importer_activation(self):
        self.env['reference_input'] = Mock(side_effect=ValueError('Reference mismatch'))
        with self.assertRaisesRegex(ValueError, 'Reference mismatch'):
            self.env['import_into_standby_database'](['study'], {'target': 'public'})
        self.env['_activate_standby_properties'].assert_not_called()
        self.env['_run_and_stream'].assert_not_called()

    def test_validation_failure_cannot_filter_out_a_selected_study(self):
        with patch.object(Path, 'mkdir'):
            self.env['_run_and_stream'].return_value.returncode = 3
            self.assertEqual('study', self.env['pull_and_validate_study']('study', {'target': 'public'}))
            self.env['_run_and_stream'].return_value.returncode = 1
            with self.assertRaisesRegex(RuntimeError, 'Selected study'):
                self.env['pull_and_validate_study']('study', {'target': 'public'})


if __name__ == '__main__':
    unittest.main()


class FailureReportingTests(unittest.TestCase):
    def setUp(self):
        source = Path(__file__).resolve().parents[1] / 'dags/import_containerized_dag.py'
        tree = ast.parse(source.read_text())
        node = next(n for n in ast.walk(tree)
                    if isinstance(n, ast.FunctionDef) and n.name == 'report_failure')
        node.decorator_list = []
        self.run_command = Mock()
        env = dict(AirflowException=RuntimeError, logging=logging,
                   _run_and_stream=self.run_command,
                   _script=lambda *args, **kwargs: 'mark-abandoned',
                   _manage_props_path=lambda database: database + '.properties')
        exec(compile(ast.Module(body=[node], type_ignores=[]), str(source), 'exec'), env)
        self.report_failure = env['report_failure']

    def test_failed_preflight_does_not_reset_another_import(self):
        # GIVEN a failed preflight and a clone that never ran
        run = Mock()
        run.get_task_instances.return_value = [SimpleNamespace(state='failed')]
        run.get_task_instance.side_effect = lambda name: SimpleNamespace(state='upstream_failed')

        # WHEN / THEN the DAG fails and shared state is not modified
        with self.assertRaisesRegex(RuntimeError, 'import tasks failed'):
            self.report_failure(ti=SimpleNamespace(get_dagrun=lambda: run), params={'target': 'public'})
        self.run_command.assert_not_called()

    def test_failed_import_after_clone_marks_abandoned_and_fails_run(self):
        # GIVEN an import failure after the clone completed
        run = Mock()
        run.get_task_instances.return_value = [SimpleNamespace(state='failed')]
        run.get_task_instance.side_effect = lambda name: SimpleNamespace(
            state='success' if name == 'clone_live_database_into_standby' else 'upstream_failed')

        # WHEN / THEN cleanup runs and the DAG still fails
        with self.assertRaisesRegex(RuntimeError, 'import tasks failed'):
            self.report_failure(ti=SimpleNamespace(get_dagrun=lambda: run), params={'target': 'public'})
        self.run_command.assert_called_once_with(['bash', '-c', 'mark-abandoned'], check=True)

    def test_failed_clone_does_not_overwrite_unknown_lock_owner(self):
        # GIVEN a failed clone whose lock ownership is unknown
        run = Mock()
        run.get_task_instances.return_value = [SimpleNamespace(state='failed')]
        run.get_task_instance.side_effect = lambda name: SimpleNamespace(state='failed')

        # WHEN / THEN the DAG fails without changing shared state
        with self.assertRaisesRegex(RuntimeError, 'import tasks failed'):
            self.report_failure(ti=SimpleNamespace(get_dagrun=lambda: run), params={'target': 'public'})
        self.run_command.assert_not_called()

    def test_failure_after_transfer_does_not_mark_switched_database_abandoned(self):
        # GIVEN traffic has switched but a later task failed
        run = Mock()
        run.get_task_instances.return_value = [SimpleNamespace(state='failed')]
        run.get_task_instance.return_value = SimpleNamespace(state='success')

        # WHEN / THEN the DAG fails without changing the switched state
        with self.assertRaisesRegex(RuntimeError, 'import tasks failed'):
            self.report_failure(ti=SimpleNamespace(get_dagrun=lambda: run), params={'target': 'public'})
        self.run_command.assert_not_called()

    def test_partially_failed_transfer_preserves_management_for_reconciliation(self):
        run = Mock()
        run.get_task_instances.return_value = [SimpleNamespace(state="failed")]
        run.get_task_instance.side_effect = lambda name: SimpleNamespace(
            state="failed" if name == "transfer_deployment_color" else "success")
        with self.assertRaisesRegex(RuntimeError, "import tasks failed"):
            self.report_failure(ti=SimpleNamespace(get_dagrun=lambda: run), params={"target": "public"})
        self.run_command.assert_not_called()
