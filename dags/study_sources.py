"""Configured read-only S3 study sources shared by discovery and imports."""
from pathlib import Path
import re
from urllib.parse import urlsplit

DEFAULT_BUCKET = 'sc-203403084713-pp-4rxlzd426npxu-bucket-kswubqqre3jr'
STUDY_LIST_VARIABLE_KEY = 'available_study_sources'
S3_SOURCES = {
    DEFAULT_BUCKET: {'mount': '/mnt/s3-data', 'claim': 'databricks-s3-pvc'},
    'cbioportal-import-studies-203403084713': {
        'mount': '/mnt/import-studies', 'claim': 'import-studies-s3-pvc'},
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


def study_selections(selections):
    """Reject duplicate destinations before validation or database cloning."""
    if not isinstance(selections, (list, tuple)):
        raise ValueError('Study selections must be an array')
    result, seen = [], set()
    for selection in selections:
        bucket, study_id = parse_study_source(selection)
        if study_id in seen:
            raise ValueError(f'Study {study_id} selected more than once; choose one bucket per study')
        seen.add(study_id)
        result.append(f's3://{bucket}/{study_id}')
    return result


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
