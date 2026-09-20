#!/usr/bin/env python3
"""Prepare references, recheck case lists, validate and select public candidates.

All outputs are new local artifacts. Does not upload, deploy, import or swap.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from dags.public_rollout import (REFERENCE_FILES, sha256, verify_references, validator_files,
                                reference_input, study_input, validation_command, select_sample)


def write_json(path, value):
    with Path(path).open('x') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')


def capture(args):
    out = args.output
    out.mkdir(parents=True, exist_ok=False)
    seed = args.seed_references
    def query(sql):
        return subprocess.check_output(['clickhouse-client', '--config-file', str(args.clickhouse_config),
                                        '--query', sql])
    def rows(sql):
        return json.loads(query(sql + ' FORMAT JSON'))['data']
    state_sql = "SELECT * FROM publicdb_update_management_database.update_status WHERE portal_database='public'"
    state = rows(state_sql)
    if len(state) != 1 or state[0]['update_process_status'] != 'complete':
        raise ValueError('Public reference capture requires an idle, completed management state')
    color = state[0]['current_database_in_production']
    if color not in ('blue', 'green'):
        raise ValueError('Unknown public database color')
    db = 'cbioportal_public_' + color
    seed_manifest = json.loads((seed / 'manifest.json').read_text())
    if seed_manifest['database'] != db:
        raise ValueError('Public database changed since preprocessing reference capture')
    # Prove preprocessing and validation resolve the same gene/alias inventory.
    for name, sql in {
        'gene_table.tsv': f'SELECT entrez_gene_id, hugo_gene_symbol FROM {db}.gene ORDER BY entrez_gene_id, hugo_gene_symbol FORMAT TabSeparated',
        'gene_alias_table.tsv': f'SELECT entrez_gene_id, gene_alias FROM {db}.gene_alias ORDER BY entrez_gene_id, gene_alias FORMAT TabSeparated',
    }.items():
        data = query(sql)
        if data != (seed / name).read_bytes():
            raise ValueError(f'Public {name} changed since preprocessing; reprepare against new references')
        (out / name).write_bytes(data)
    genes = []
    for line in (seed / 'gene_table.tsv').read_text().splitlines():
        entrez, symbol = line.split('\t')
        genes.append({'entrezGeneId': int(entrez), 'hugoGeneSymbol': symbol})
    aliases = []
    for line in (seed / 'gene_alias_table.tsv').read_text().splitlines():
        entrez, alias = line.split('\t')
        aliases.append({'entrezGeneId': int(entrez), 'alias': alias})
    write_json(out / 'genes.json', genes)
    write_json(out / 'genesaliases.json', aliases)
    write_json(out / 'cancer-types.json', rows(
        f'SELECT type_of_cancer_id AS cancerTypeId, name, dedicated_color AS dedicatedColor, '
        f'short_name AS shortName, parent FROM {db}.type_of_cancer ORDER BY type_of_cancer_id'))
    write_json(out / 'genesets.json', rows(f'SELECT external_id AS genesetId FROM {db}.geneset ORDER BY external_id'))
    info = rows(f'SELECT geneset_version, gene_table_version FROM {db}.info')
    if len(info) != 1:
        raise ValueError('Expected exactly one database version row')
    write_json(out / 'genesets_version.json', info[0]['geneset_version'])
    panels = rows(f'SELECT internal_id, stable_id, description FROM {db}.gene_panel ORDER BY stable_id')
    members = rows(f'SELECT internal_id, gene_id FROM {db}.gene_panel_list ORDER BY internal_id, gene_id')
    by_panel = {}
    for row in members:
        by_panel.setdefault(row['internal_id'], []).append({'entrezGeneId': int(row['gene_id'])})
    write_json(out / 'gene-panels.json', [dict(genePanelId=p['stable_id'], description=p['description'],
                                            genes=by_panel.get(p['internal_id'], [])) for p in panels])
    with urllib.request.urlopen('https://www.cbioportal.org/api/info', timeout=60) as response:
        portal_info = json.load(response)
    if (portal_info.get('genesetVersion') != info[0]['geneset_version'] or
            portal_info.get('geneTableVersion') != info[0]['gene_table_version']):
        raise ValueError('Public API and database reference versions disagree')
    write_json(out / 'info.json', portal_info)
    shutil.copyfile(seed / 'oncotree.json', out / 'oncotree.json')
    shutil.copyfile(args.importer / 'case_list_config.tsv', out / 'case_list_config.tsv')
    if rows(state_sql) != state:
        raise ValueError('Management state changed during reference capture')
    hashes = {p.name: sha256(p) for p in out.iterdir()}
    verify_references(out, hashes, args.importer)
    write_json(out / 'manifest.json', dict(database=db, captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        seed_reference_manifest_sha256=sha256(seed / 'manifest.json'),
        oncotree_version=seed_manifest['oncotree_version'], sha256=hashes))
    print(json.dumps({'references': str(out), 'database': db, 'files': len(hashes)}))


def archive(source, target, arcname):
    with tarfile.open(target, 'w:gz') as stream:
        stream.add(source, arcname=arcname)


def prepare(args):
    """Recheck only case-list generation on copies of the already prepared archives."""
    original = json.loads((args.batch / 'manifest.json').read_text())
    args.output.mkdir(parents=True, exist_ok=False)
    staging = args.output / 'staging'
    staging.mkdir()
    batch_dir = staging / '_batches' / args.output.name
    batch_dir.mkdir(parents=True)
    refs_meta = json.loads((args.references / 'manifest.json').read_text())
    verify_references(args.references, refs_meta['sha256'], args.importer)
    # Non-study suffix prevents study-discovery code mistaking this for a study.
    ref_archive = batch_dir / 'references.bundle'
    # Flat archive, so -p sees the expected JSON filenames immediately.
    with tarfile.open(ref_archive, 'w:gz') as stream:
        for path in sorted(args.references.iterdir()):
            stream.add(path, arcname=path.name)
    records = []
    for uploaded in original['uploads']:
        key = uploaded['key']
        study_id = Path(key).name.removesuffix('.tar.gz')
        entry = dict(study_id=study_id, key=Path(key).name, sha256=uploaded['sha256'])
        with study_input(args.batch / 'archives', entry) as study:
            before = {str(p.relative_to(study)): sha256(p) for p in study.rglob('*') if p.is_file()}
            cases = study / 'case_lists'
            cases.mkdir(exist_ok=True)
            log = batch_dir / (study_id + '-case-lists.log')
            with log.open('x') as output:
                subprocess.run([sys.executable, str(args.generator), '--study-dir', str(study),
                                '--case-list-dir', str(cases), '--case-list-config-file',
                                str(args.references / 'case_list_config.tsv'), '--normalize-tcga-barcodes'],
                               stdout=output, stderr=subprocess.STDOUT, check=True)
            after = {str(p.relative_to(study)): sha256(p) for p in study.rglob('*') if p.is_file()}
            if any(after.get(name) != digest for name, digest in before.items()):
                raise ValueError(f'Gap filling changed or deleted existing data: {study_id}')
            added = sorted(set(after) - set(before))
            if any(not name.startswith('case_lists/') for name in added):
                raise ValueError(f'Unexpected non-case-list output: {study_id}')
            target = staging / Path(key).name
            if added:
                archive(study, target, study_id)
            else:
                # Preserve the exact validated/uploaded bytes of unchanged archives.
                shutil.copyfile(args.batch / 'archives' / Path(key).name, target)
            records.append(dict(study_id=study_id, key=key, sha256=sha256(target),
                                original_sha256=entry['sha256'], added_case_lists=added, status='pending'))
            print(f'{study_id}: added {len(added)} case lists', flush=True)
    manifest = dict(schema_version=1, batch_id=args.output.name, bucket=original['bucket'],
                    datahub_commit=original['datahub_commit'],
                    source_batch_manifest_sha256=sha256(args.batch / 'manifest.json'),
                    generator_sha256=sha256(args.generator), validator_files=validator_files(args.importer),
                    references=dict(key=str(ref_archive.relative_to(args.output)), sha256=sha256(ref_archive),
                                    files={name: refs_meta['sha256'][name] for name in REFERENCE_FILES}),
                    studies=records)
    write_json(args.output / 'candidates.json', manifest)


def validate(args):
    manifest = json.loads(args.manifest.read_text())
    output = args.output
    output.mkdir(parents=True, exist_ok=False)
    def run(entry, refs):
        result = dict(entry)
        study_log = output / entry['study_id']
        study_log.mkdir()
        try:
            with study_input(args.root, entry) as study:
                command = validation_command(args.python, args.importer, study, refs, study_log / 'report.html')
                with (study_log / 'validator.log').open('x') as log:
                    process = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT)
            code = process.returncode
            result.update(validator_exit_code=code,
                          status='passed' if code in (0, 3) else 'rejected' if code == 1 else 'error')
            result['rejection_reason'] = None if code in (0, 3) else f'Validator exit {code}; see {entry["study_id"]}/validator.log'
        except Exception as error:
            result.update(status='error', validator_exit_code=None, rejection_reason=str(error))
        result['log_directory'] = entry['study_id']
        return result
    with reference_input(args.root, manifest, args.importer) as refs:
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            futures = [pool.submit(run, entry, refs) for entry in manifest['studies']]
            results = []
            for future in as_completed(futures):
                result = future.result()
                results.append(result)
                write_json(output / (result['study_id'] + '.json'), result)
                print(f'{result["study_id"]}: {result["status"]}', flush=True)
    manifest['studies'] = sorted(results, key=lambda r: r['study_id'])
    manifest['validated_at'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    add_diagnostics(manifest, output)
    write_json(output / 'validation-manifest.json', manifest)
    if any(r['status'] == 'error' for r in results):
        raise SystemExit('Validation infrastructure errors; no sample may be selected')


def add_diagnostics(manifest, logs):
    for entry in manifest['studies']:
        log = logs / entry['study_id'] / 'validator.log'
        if log.is_file():
            entry['validator_log_sha256'] = sha256(log)
            with log.open() as stream:
                errors = [line.rstrip() for line in stream if line.startswith(('ERROR:', 'CRITICAL:'))]
            entry['error_summary'] = errors[:10]
            entry['error_message_count'] = len(errors)
            if entry['status'] == 'rejected' and errors:
                entry['rejection_reason'] = '\n'.join(errors[:10])


def report(args):
    manifest = json.loads(args.manifest.read_text())
    add_diagnostics(manifest, args.logs)
    write_json(args.output, manifest)


def select(args):
    manifest = select_sample(json.loads(args.manifest.read_text()), args.count, args.seed)
    write_json(args.output, manifest)
    params = dict(database='public', study_prefix='staging',
                  skip_tasks=['transfer_deployment_color'],
                  cancer_study_ids=manifest['selected_study_ids'],
                  rollout_manifest_key=args.manifest_key, rollout_manifest_sha256=sha256(args.output))
    write_json(args.output.with_suffix('.params.json'), params)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    p = commands.add_parser('capture-references')
    p.add_argument('--seed-references', type=Path, required=True)
    p.add_argument('--clickhouse-config', type=Path, required=True)
    p.add_argument('--importer', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.set_defaults(run=capture)
    p = commands.add_parser('prepare-case-lists')
    for option in ('batch', 'references', 'generator', 'importer', 'output'):
        p.add_argument('--' + option, type=Path, required=True)
    p.set_defaults(run=prepare)
    p = commands.add_parser('validate')
    for option in ('manifest', 'root', 'importer', 'output'):
        p.add_argument('--' + option, type=Path, required=True)
    p.add_argument('--python', default=sys.executable)
    p.add_argument('--workers', type=int, default=2)
    p.set_defaults(run=validate)
    p = commands.add_parser('report')
    for option in ('manifest', 'logs', 'output'):
        p.add_argument('--' + option, type=Path, required=True)
    p.set_defaults(run=report)
    p = commands.add_parser('select')
    p.add_argument('--manifest', type=Path, required=True)
    p.add_argument('--count', type=int, default=50)
    p.add_argument('--seed', required=True)
    p.add_argument('--manifest-key', required=True)
    p.add_argument('--output', type=Path, required=True)
    p.set_defaults(run=select)
    args = parser.parse_args()
    args.run(args)


if __name__ == '__main__':
    main()
