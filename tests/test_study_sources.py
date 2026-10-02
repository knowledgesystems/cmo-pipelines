import ast
import io
from pathlib import Path
from types import SimpleNamespace
import tarfile
import tempfile
import unittest
from unittest.mock import Mock, patch

from dags import study_sources as sources

NEW_BUCKET = next(bucket for bucket in sources.S3_SOURCES if bucket != sources.DEFAULT_BUCKET)
DAG = Path(__file__).resolve().parents[1] / 'dags/import_public_hackathon.py'


def dag_functions(*names, **extra):
    nodes = []
    for node in ast.walk(ast.parse(DAG.read_text())):
        if isinstance(node, ast.FunctionDef) and node.name in names:
            node.decorator_list = []
            nodes.append(node)
    env = dict(DEFAULT_BUCKET=sources.DEFAULT_BUCKET, S3_SOURCES=sources.S3_SOURCES,
               bucket_mount=sources.bucket_mount, parse_study_source=sources.parse_study_source,
               study_selections=sources.study_selections, verified_study_directory=sources.verified_study_directory,
               AirflowException=ValueError, logger=Mock(), json=__import__('json'))
    env.update(extra)
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(DAG), 'exec'), env)
    return env


class TestStudySources(unittest.TestCase):
    def test_discovery_preserves_bucket_identity_across_pages(self):
        def pages(Bucket, **kwargs):
            return [{'Contents': [{'Key': 'study.tar.gz'}, {'Key': 'ignore.txt'}]},
                    {'Contents': [{'Key': 'study.tar.gz'}],
                     'CommonPrefixes': [{'Prefix': 'folder/'}, {'Prefix': 'staging/'}]}]
        s3 = Mock()
        s3.get_paginator.return_value.paginate.side_effect = pages
        choices = sources.discover_studies(s3)
        self.assertEqual(sorted(f's3://{bucket}/{study}' for bucket in sources.S3_SOURCES
                                for study in ('study', 'folder')), choices)
        s3.get_paginator.return_value.paginate.side_effect = PermissionError('denied')
        with self.assertRaises(PermissionError):
            sources.discover_studies(s3)

    def test_mix_sources_and_reject_duplicate_destinations_and_unknown_paths(self):
        self.assertEqual([f's3://{sources.DEFAULT_BUCKET}/old', f's3://{NEW_BUCKET}/new'],
                         sources.study_selections(['old', f's3://{NEW_BUCKET}/new']))
        for choices in ([f's3://{sources.DEFAULT_BUCKET}/study', f's3://{NEW_BUCKET}/study'],
                        ['study', 'study'], ['s3://unknown/study'], ['../study'],
                        [f's3://{NEW_BUCKET}/study/extra'], [f's3://{NEW_BUCKET}/study?key=x'], 'study'):
            with self.subTest(choices=choices), self.assertRaises(ValueError):
                sources.study_selections(choices)

    def test_same_named_archives_are_read_only_from_selected_bucket(self):
        env = dag_functions('_study_mount', '_study_data_path')
        with tempfile.TemporaryDirectory() as tmp:
            catalog = {bucket: {'mount': str(Path(tmp) / str(i))}
                       for i, bucket in enumerate(sources.S3_SOURCES)}
            for bucket, config in catalog.items():
                mount = Path(config['mount']) / 'staging'
                mount.mkdir(parents=True)
                with tarfile.open(mount / 'study.tar.gz', 'w:gz') as archive:
                    for name, text in [('meta_study.txt', 'cancer_study_identifier: study\n'),
                                       ('source.txt', bucket)]:
                        raw = text.encode()
                        member = tarfile.TarInfo('study/' + name)
                        member.size = len(raw)
                        archive.addfile(member, io.BytesIO(raw))
            with patch.dict(sources.S3_SOURCES, catalog, clear=True):
                for bucket in catalog:
                    import shutil
                    extracted = env['_study_data_path'](f's3://{bucket}/study', 'staging')
                    try:
                        self.assertEqual(bucket, (Path(extracted) / 'source.txt').read_text())
                    finally:
                        shutil.rmtree(extracted)
                self.assertIsNone(env['_study_data_path'](f's3://{NEW_BUCKET}/missing', 'staging'))

    def test_preflight_accepts_two_buckets_and_rejects_missing_source_before_cloning(self):
        env = dag_functions('verify_studies_exist', _rollout_manifest=lambda params: None,
                            _study_prefix=lambda params: '', SKIPPABLE_TASK_IDS=())
        with tempfile.TemporaryDirectory() as tmp:
            catalog = {bucket: {'mount': str(Path(tmp) / str(i))}
                       for i, bucket in enumerate(sources.S3_SOURCES)}
            for config in catalog.values():
                Path(config['mount']).mkdir()
            (Path(catalog[sources.DEFAULT_BUCKET]['mount']) / 'old.tar.gz').touch()
            (Path(catalog[NEW_BUCKET]['mount']) / 'new.tar.gz').touch()
            with patch.dict(sources.S3_SOURCES, catalog, clear=True):
                env['_study_mount'] = lambda prefix, bucket: sources.bucket_mount(bucket)
                selected = ['old', f's3://{NEW_BUCKET}/new']
                self.assertEqual(sources.study_selections(selected), env['verify_studies_exist'](selected))
                with self.assertRaisesRegex(ValueError, 'not found'):
                    env['verify_studies_exist'](['new'])

    def test_metadata_identity_must_match_the_selected_study(self):
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / 'meta_study.txt').write_text('cancer_study_identifier: different\n')
            with self.assertRaisesRegex(ValueError, 'does not match'):
                sources.verified_study_directory(tmp, 'study')

    def test_pinned_manifest_cannot_substitute_a_bucket(self):
        from dags.public_rollout import selected_entries
        env = dag_functions('_selected_rollout_entries', selected_entries=selected_entries)
        manifest = {'selected_study_ids': ['study'], 'studies': [dict(study_id='study',
                    key='staging/study.tar.gz', sha256='a' * 64, status='passed', validator_exit_code=0)]}
        self.assertEqual(manifest['studies'], env['_selected_rollout_entries'](manifest, ['study']))
        with self.assertRaisesRegex(ValueError, 'differs from pinned'):
            env['_selected_rollout_entries'](manifest, [f's3://{NEW_BUCKET}/study'])
        manifest['studies'][0]['bucket'] = NEW_BUCKET
        self.assertEqual(manifest['studies'], env['_selected_rollout_entries'](manifest, [f's3://{NEW_BUCKET}/study']))

    def test_dropdown_upgrades_legacy_ids_without_collapsing_two_sources(self):
        variable = Mock()
        env = dag_functions('_available_study_ids', Variable=variable,
                            STUDY_LIST_VARIABLE_KEY=sources.STUDY_LIST_VARIABLE_KEY)
        variable.get.side_effect = [None, '["study"]']
        self.assertEqual([f's3://{sources.DEFAULT_BUCKET}/study'], env['_available_study_ids']())
        variable.get.side_effect = None
        choices = [f's3://{bucket}/study' for bucket in sources.S3_SOURCES]
        variable.get.return_value = __import__('json').dumps(choices)
        self.assertEqual(choices, env['_available_study_ids']())

    def test_failed_second_bucket_does_not_publish_partial_catalog(self):
        path = DAG.with_name('refresh_study_list.py')
        node = next(n for n in ast.walk(ast.parse(path.read_text()))
                    if isinstance(n, ast.FunctionDef) and n.name == 'fetch_and_store_study_ids')
        node.decorator_list = []
        s3, variable = Mock(), Mock()
        s3.get_paginator.return_value.paginate.side_effect = [
            [{'Contents': [{'Key': 'study.tar.gz'}]}], PermissionError('second bucket denied')]
        env = dict(discover_studies=sources.discover_studies, Variable=variable)
        exec(compile(ast.Module(body=[node], type_ignores=[]), str(path), 'exec'), env)
        with patch.dict('sys.modules', boto3=SimpleNamespace(client=lambda name: s3)):
            with self.assertRaises(PermissionError):
                env['fetch_and_store_study_ids']()
        variable.set.assert_not_called()
