"""Configured read-only S3 study sources shared by discovery and imports."""
from pathlib import Path
import re
from urllib.parse import urlsplit

DEFAULT_BUCKET = 'sc-203403084713-pp-4rxlzd426npxu-bucket-kswubqqre3jr'
STUDY_LIST_VARIABLE_KEY = 'available_study_sources'
S3_SOURCES = {
    DEFAULT_BUCKET: {'mount': '/mnt/s3-data', 'claim': 'databricks-s3-pvc'},
    'cdsi-curation-unpublished': {
        'mount': '/mnt/import-studies', 'claim': 'cdsi-curation-unpublished-s3-pvc'},
    'cdsi-testing': {
        'mount': '/mnt/testing-studies', 'claim': 'testing-studies-s3-pvc'},
}


def parse_study_source(selection):
    """Legacy bare IDs refer explicitly to the original bucket."""
    if not isinstance(selection, str):
        raise ValueError('Study selections must be strings')
    selection = selection.strip()
    if selection.startswith('s3://'):
        url = urlsplit(selection)
        bucket, study_id = url.netloc, url.path.removeprefix('/')
        if url.query or url.fragment:
            raise ValueError(f'Invalid study source: {selection}')
    else:
        bucket, study_id = DEFAULT_BUCKET, selection
    if bucket not in S3_SOURCES:
        raise ValueError(f'Unconfigured S3 bucket: {bucket}')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', study_id):
        raise ValueError(f'Invalid study ID: {study_id!r}')
    return bucket, study_id


def bucket_mount(bucket=DEFAULT_BUCKET):
    return Path(S3_SOURCES[bucket]['mount'])


def discover_studies(s3):
    """List every source before publishing a replacement dropdown catalog."""
    found = set()
    for bucket in S3_SOURCES:
        for page in s3.get_paginator('list_objects_v2').paginate(Bucket=bucket, Delimiter='/'):
            names = [p['Prefix'].rstrip('/') for p in page.get('CommonPrefixes', [])
                     if p['Prefix'].rstrip('/') not in {'lfs', 'embeddings', 'staging'}]
            names += [obj['Key'][:-7] for obj in page.get('Contents', [])
                      if obj['Key'].endswith('.tar.gz')]
            for name in names:
                parse_study_source(f's3://{bucket}/{name}')
                found.add(f's3://{bucket}/{name}')
    return sorted(found)


def verified_study_directory(directory, study_id):
    values = {}
    for line in (Path(directory) / 'meta_study.txt').read_text().splitlines():
        if ':' in line and not line.lstrip().startswith('#'):
            key, value = line.split(':', 1)
            values[key.strip()] = value.strip()
    if values.get('cancer_study_identifier') != study_id:
        raise ValueError(f'Study metadata does not match selected ID: {study_id}')
    return str(directory)


def search_buckets(buckets):
    if not isinstance(buckets, (list, tuple)) or not buckets:
        raise ValueError('Select at least one bucket to search')
    if any(bucket not in S3_SOURCES for bucket in buckets):
        raise ValueError('Buckets to search must be configured S3 buckets')
    buckets = list(dict.fromkeys(buckets))
    return buckets


def resolve_studies(study_ids, buckets):
    """Resolve plain IDs to exactly one location before any database mutation."""
    buckets = search_buckets(buckets)
    if not isinstance(study_ids, (list, tuple)) or not study_ids:
        raise ValueError('Select at least one study ID')
    resolved, seen = [], set()
    for study_id in study_ids:
        if not isinstance(study_id, str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', study_id):
            raise ValueError(f'Invalid study ID: {study_id!r}; select a plain study ID')
        if study_id in seen:
            raise ValueError(f'Study {study_id} selected more than once')
        seen.add(study_id)
        locations = []
        for bucket in buckets:
            root = bucket_mount(bucket)
            paths = [root / f'{study_id}.tar.gz', root / study_id]
            for path in paths:
                exists = path.is_file() if path.name.endswith('.tar.gz') else path.is_dir()
                if exists:
                    locations.append((bucket, path))
        if not locations:
            raise ValueError(f'Study {study_id} not found in selected buckets: {buckets}')
        if len(locations) > 1:
            paths = [f's3://{bucket}/{path.relative_to(bucket_mount(bucket))}' for bucket, path in locations]
            raise ValueError(f'Ambiguous study {study_id}; found in multiple locations: {paths}')
        resolved.append(f's3://{locations[0][0]}/{study_id}')
    return resolved
