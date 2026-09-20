"""Pinned public-study inputs shared by the batch CLI and Airflow tasks.

No credentials, database writes, uploads, or network access in this module.
"""
from contextlib import contextmanager
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import tarfile
import tempfile
from urllib.parse import urlsplit, parse_qs

REFERENCE_FILES = ('info.json', 'cancer-types.json', 'genes.json', 'genesaliases.json',
                   'genesets.json', 'genesets_version.json', 'gene-panels.json',
                   'oncotree.json', 'case_list_config.tsv')


def standby_target(manage, live_output, application):
    """Fail closed before activating JDBC or derived-table settings."""
    blue = manage['clickhouse_blue_database_name']
    green = manage['clickhouse_green_database_name']
    if not blue or not green or blue == green:
        raise ValueError('Blue and green database names must be distinct')
    live = live_output.strip().split(':', 1)[0].strip()
    live = {'blue': blue, 'green': green}.get(live, live)
    if live not in (blue, green):
        raise ValueError('Management returned an unknown production database')
    color, target = ('green', green) if live == blue else ('blue', blue)
    url = urlsplit(application['spring.datasource.url'].removeprefix('jdbc:'))
    query = parse_qs(url.query)
    if url.scheme != 'clickhouse' or url.hostname != manage['clickhouse_server_host_name']:
        raise ValueError('JDBC and management ClickHouse hosts differ')
    if url.path.strip('/') != target or target == live:
        raise ValueError('JDBC destination is not the configured standby database')
    if query.get('ssl') != ['true']:
        raise ValueError('Public standby JDBC connection must use TLS')
    timeout = query.get('socket_timeout', [])
    if len(timeout) != 1 or not timeout[0].isdigit() or not 0 < int(timeout[0]) <= 3600000:
        raise ValueError('JDBC socket_timeout must be explicit, positive and at most one hour')
    return color, target


def sha256(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def checked_path(root, key):
    path = PurePosixPath(key)
    if not key or path.is_absolute() or '..' in path.parts or '\\' in key:
        raise ValueError(f'Unsafe relative path: {key!r}')
    target = Path(root) / key
    if not target.resolve().is_relative_to(Path(root).resolve()):
        raise ValueError(f'Path escapes input root: {key!r}')
    return target


def require_hash(path, expected):
    if not re.fullmatch('[0-9a-f]{64}', expected or '') or sha256(path) != expected:
        raise ValueError(f'SHA-256 mismatch: {path}')


def read_manifest(path, expected):
    # Read once: the parsed bytes must be the bytes whose hash we checked.
    raw = Path(path).read_bytes()
    if hashlib.sha256(raw).hexdigest() != expected:
        raise ValueError('Manifest SHA-256 mismatch')
    result = json.loads(raw)
    if result.get('schema_version') != 1:
        raise ValueError('Unsupported rollout manifest schema')
    return result


def validator_files(importer):
    root = Path(importer)
    return {p.name: sha256(p) for p in sorted(root.iterdir())
            if p.is_file() and p.suffix in ('.py', '.tsv')}


def verify_validator(importer, expected):
    if not expected or validator_files(importer) != expected:
        raise ValueError('Validator files differ from the validated candidate')


def verify_references(root, expected, importer=None):
    for name in REFERENCE_FILES:
        if name not in expected:
            raise ValueError(f'Reference manifest is missing {name}')
        path = Path(root) / name
        require_hash(path, expected[name])
        if name.endswith('.json'):
            value = json.loads(path.read_text())
            # Empty lists are valid for a portal with no gene sets/panels. Missing
            # or null files are not: core would silently disable those checks.
            if value is None:
                raise ValueError(f'Null reference: {name}')
            if name == 'info.json':
                if not isinstance(value, dict) or not value.get('portalVersion'):
                    raise ValueError('Missing portal version')
            elif name == 'genesets_version.json':
                if not isinstance(value, str) or not value.strip():
                    raise ValueError('Missing gene-set version')
            elif not isinstance(value, list):
                raise ValueError(f'Expected reference array: {name}')
            elif name in ('genes.json', 'genesaliases.json', 'cancer-types.json', 'oncotree.json') and not value:
                raise ValueError(f'Empty reference: {name}')
    if importer is not None:
        require_hash(Path(importer) / 'case_list_config.tsv', expected['case_list_config.tsv'])


@contextmanager
def verified_archive(path, expected):
    """Copy, hash, then extract that private copy. Never extract live S3 bytes."""
    with tempfile.TemporaryDirectory(prefix='public-rollout-') as tmp:
        root = Path(tmp)
        archive = root / 'input.tar.gz'
        shutil.copyfile(path, archive)
        require_hash(archive, expected)
        contents = root / 'contents'
        contents.mkdir()
        with tarfile.open(archive, 'r:gz') as stream:
            seen = set()
            for member in stream:
                target = checked_path(contents, member.name)
                if target in seen or not (member.isfile() or member.isdir()):
                    raise ValueError(f'Unsupported or duplicate archive member: {member.name}')
                seen.add(target)
                if member.isdir():
                    target.mkdir(parents=True, exist_ok=True)
                else:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    with stream.extractfile(member) as source, target.open('xb') as output:
                        shutil.copyfileobj(source, output)
        yield contents


@contextmanager
def study_input(root, entry):
    study_id = entry['study_id']
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', study_id):
        raise ValueError(f'Unsafe study ID: {study_id!r}')
    with verified_archive(checked_path(root, entry['key']), entry['sha256']) as contents:
        entries = list(contents.iterdir())
        if len(entries) == 1 and entries[0].is_dir():
            contents = entries[0]
        values = {}
        for line in (contents / 'meta_study.txt').read_text().splitlines():
            if ':' in line and not line.lstrip().startswith('#'):
                key, value = line.split(':', 1)
                values[key.strip()] = value.strip()
        if values.get('cancer_study_identifier') != study_id:
            raise ValueError(f'Archive study ID differs from manifest: {study_id}')
        yield contents


@contextmanager
def reference_input(root, manifest, importer):
    ref = manifest['references']
    verify_validator(importer, manifest['validator_files'])
    with verified_archive(checked_path(root, ref['key']), ref['sha256']) as contents:
        verify_references(contents, ref['files'], importer)
        yield contents


def validation_command(python, importer, study, references, html):
    # Direct entry point preserves INVALID vs PROBLEMS OCCURRED exit codes.
    return [str(python), str(Path(importer) / 'validateData.py'), '-s', str(study),
            '-p', str(references), '--oncotree-file', str(Path(references) / 'oncotree.json'),
            '--html', str(html), '-v']


def selected_entries(manifest, requested=None):
    selected = manifest.get('selected_study_ids', [])
    if not selected or len(set(selected)) != len(selected):
        raise ValueError('Selection must contain unique, explicit study IDs')
    if requested is not None and list(requested) != selected:
        raise ValueError('Requested studies differ from the pinned selection')
    records = manifest['studies']
    by_id = {r['study_id']: r for r in records}
    if len(by_id) != len(records):
        raise ValueError('Duplicate study records')
    result = []
    for study_id in selected:
        record = by_id[study_id]
        if record.get('status') != 'passed' or record.get('validator_exit_code') not in (0, 3):
            raise ValueError(f'Selected study did not pass validation: {study_id}')
        if record['key'] != f'staging/{study_id}.tar.gz':
            raise ValueError(f'Selected input is outside staging: {study_id}')
        result.append(record)
    return result


def select_sample(manifest, count, seed):
    if count < 1 or not seed:
        raise ValueError('A positive sample size and nonempty seed are required')
    if any(r.get('status') not in ('passed', 'rejected') for r in manifest['studies']):
        raise ValueError('Resolve incomplete/error validations before sampling')
    eligible = sorted(r['study_id'] for r in manifest['studies'] if r['status'] == 'passed')
    if len(set(eligible)) != len(eligible) or len(eligible) < count:
        raise ValueError(f'Need {count} distinct passing studies; have {len(set(eligible))}')
    # Hash-ranking is stable across Python versions and input ordering.
    ranked = sorted(eligible, key=lambda sid: (hashlib.sha256((seed + '\0' + sid).encode()).hexdigest(), sid))
    result = dict(manifest, selection_seed=seed, selection_algorithm='sha256-rank-v1',
                  selected_study_ids=ranked[:count])
    selected_entries(result)
    return result
