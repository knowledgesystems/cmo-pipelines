import copy
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest

from dags import public_rollout as rollout


class RolloutTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        self.manifest = {'schema_version': 1, 'studies': [
            dict(study_id=s, key=f'staging/{s}.tar.gz', sha256='a' * 64,
                 status='passed', validator_exit_code=0) for s in ('a', 'b', 'c')]}

    def test_selection_is_repeatable_and_independent_of_record_order(self):
        first = rollout.select_sample(self.manifest, 2, 'seed')
        self.manifest['studies'].reverse()
        second = rollout.select_sample(self.manifest, 2, 'seed')
        self.assertEqual(first['selected_study_ids'], second['selected_study_ids'])

    def test_standby_target_rejects_live_unknown_host_and_unlimited_timeout(self):
        manage = dict(clickhouse_blue_database_name='blue_db', clickhouse_green_database_name='green_db',
                      clickhouse_server_host_name='example.org')
        app = {'spring.datasource.url': 'jdbc:clickhouse://example.org:8443/green_db?ssl=true&socket_timeout=600000'}
        self.assertEqual(('green', 'green_db'), rollout.standby_target(manage, 'blue_db : current production database', app))
        self.assertEqual(('green', 'green_db'), rollout.standby_target(manage, 'blue : current production database', app))
        for live, url in [('unexpected blue output', app['spring.datasource.url']),
                          ('blue_db', app['spring.datasource.url'].replace('green_db', 'blue_db')),
                          ('blue_db', app['spring.datasource.url'].replace('example.org', 'elsewhere.org')),
                          ('blue_db', app['spring.datasource.url'].replace('600000', '0')),
                          ('blue_db', app['spring.datasource.url'].replace('ssl=true', 'ssl=false'))]:
            with self.subTest(live=live, url=url), self.assertRaises(ValueError):
                rollout.standby_target(manage, live, {'spring.datasource.url': url})

    def test_never_select_rejected_or_silently_shrink_sample(self):
        self.manifest['studies'][0].update(status='rejected', validator_exit_code=1)
        selected = rollout.select_sample(self.manifest, 2, 'seed')
        self.assertNotIn('a', selected['selected_study_ids'])
        with self.assertRaises(ValueError):
            rollout.select_sample(self.manifest, 3, 'seed')

    def test_infrastructure_errors_block_selection(self):
        self.manifest['studies'][0]['status'] = 'error'
        with self.assertRaises(ValueError):
            rollout.select_sample(self.manifest, 1, 'seed')

    def test_selection_must_match_requested_ids_and_staging_keys(self):
        selected = rollout.select_sample(self.manifest, 2, 'seed')
        with self.assertRaises(ValueError):
            rollout.selected_entries(selected, ['different'])
        selected['studies'][0]['key'] = 'other/a.tar.gz'
        selected['selected_study_ids'] = ['a']
        with self.assertRaises(ValueError):
            rollout.selected_entries(selected)

    def test_duplicate_records_rejected(self):
        self.manifest['studies'].append(copy.deepcopy(self.manifest['studies'][0]))
        with self.assertRaises(ValueError):
            rollout.select_sample(self.manifest, 2, 'seed')

    def test_missing_null_and_modified_references_fail_closed(self):
        hashes = {}
        for name in rollout.REFERENCE_FILES:
            data = [{'id': 'test'}]
            if name == 'info.json':
                data = {'portalVersion': '1.0.0'}
            if name == 'genesets_version.json':
                data = 'v1'
            (self.root / name).write_text(json.dumps(data))
            hashes[name] = rollout.sha256(self.root / name)
        rollout.verify_references(self.root, hashes)
        for name in rollout.REFERENCE_FILES:
            incomplete = dict(hashes)
            del incomplete[name]
            with self.assertRaises(ValueError):
                rollout.verify_references(self.root, incomplete)
        (self.root / 'genes.json').write_text('null')
        with self.assertRaises(ValueError):
            rollout.verify_references(self.root, hashes)
        hashes['genes.json'] = rollout.sha256(self.root / 'genes.json')
        with self.assertRaises(ValueError):
            rollout.verify_references(self.root, hashes)

    def test_validator_fingerprint_detects_changed_rules(self):
        (self.root / 'case_list_config.tsv').write_text('rules')
        expected = rollout.validator_files(self.root)
        rollout.verify_validator(self.root, expected)
        (self.root / 'case_list_config.tsv').write_text('different rules')
        with self.assertRaises(ValueError):
            rollout.verify_validator(self.root, expected)

    def make_archive(self, member, data=b'test', symlink=False):
        path = self.root / 'study.tar.gz'
        with tarfile.open(path, 'w:gz') as stream:
            entry = tarfile.TarInfo(member)
            if symlink:
                entry.type = tarfile.SYMTYPE
                entry.linkname = '/etc/passwd'
                stream.addfile(entry)
            else:
                entry.size = len(data)
                stream.addfile(entry, io.BytesIO(data))
        return path

    def test_archive_hash_mismatch_rejected_before_extraction(self):
        path = self.make_archive('meta_study.txt')
        with self.assertRaises(ValueError), rollout.verified_archive(path, '0' * 64):
            self.fail('Extraction must not be reached')

    def test_unsafe_tar_members_and_links_rejected(self):
        for name in ('../escape', '/escape', 'study/../../escape'):
            path = self.make_archive(name)
            with self.assertRaises(ValueError), rollout.verified_archive(path, rollout.sha256(path)):
                self.fail('Unsafe extraction must not be reached')
        path = self.make_archive('link', symlink=True)
        with self.assertRaises(ValueError), rollout.verified_archive(path, rollout.sha256(path)):
            self.fail('Link extraction must not be reached')

    def test_study_identity_and_cleanup(self):
        path = self.make_archive('study/meta_study.txt', b'cancer_study_identifier: study\n')
        record = dict(study_id='study', key=path.name, sha256=rollout.sha256(path))
        with rollout.study_input(self.root, record) as study:
            self.assertTrue((study / 'meta_study.txt').exists())
        self.assertFalse(study.exists())
        record['study_id'] = 'wrong'
        with self.assertRaises(ValueError), rollout.study_input(self.root, record):
            self.fail('Wrong study accepted')

    def test_extracted_bytes_are_isolated_from_later_source_changes(self):
        path = self.make_archive('data.txt', b'validated bytes')
        with rollout.verified_archive(path, rollout.sha256(path)) as contents:
            self.make_archive('data.txt', b'different bytes')
            self.assertEqual(b'validated bytes', (contents / 'data.txt').read_bytes())

    def test_manifest_hash_is_required(self):
        path = self.root / 'manifest.json'
        path.write_text(json.dumps(self.manifest))
        self.assertEqual(self.manifest, rollout.read_manifest(path, rollout.sha256(path)))
        with self.assertRaises(ValueError):
            rollout.read_manifest(path, None)

    def test_validation_uses_pinned_references_without_no_portal_checks(self):
        command = rollout.validation_command('python', '/importer', '/study', '/refs', '/report.html')
        self.assertNotIn('-n', command)
        self.assertIn('-p', command)
        self.assertEqual('/refs/oncotree.json', command[command.index('--oncotree-file') + 1])


if __name__ == '__main__':
    unittest.main()
