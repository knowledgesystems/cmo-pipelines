import argparse
import io
from minio import Minio
import urllib3


def insert_hardcoded():
    ACCESS_KEY = ''
    SECRET_KEY = ''

    # required for self-signed certs
    httpClient = urllib3.PoolManager(cert_reqs='CERT_REQUIRED', ca_certs='certificate.crt')

    # Create secure client with access key and secret key
    client = Minio(
        "tllihpcmind6:9000",
        access_key=ACCESS_KEY,
        secret_key=SECRET_KEY,
        secure=True,
        http_client=httpClient,
    )

    result = client.put_object(
        'cdm-data',
        'cbioportal/test-obj',
        io.BytesIO(b"hello"),
        5,
    )

    # list objects in a bucket
    for ii in client.list_objects('cdm-data'):
        print(ii.__dict__)


def get_client(ca_certs, endpoint, access_key, secret_key):
    # Required for self-signed certs
    httpClient = urllib3.PoolManager(cert_reqs='CERT_REQUIRED', ca_certs=ca_certs)

    # Create secure client with access key and secret key
    client = Minio(
        endpoint=endpoint,
        access_key=access_key,
        secret_key=secret_key,
        secure=True,
        http_client=httpClient,
    )
    return client


def insert(client, bucket_name, object_name, file_path):
    result = client.fput_object(
        bucket_name=bucket_name, object_name=object_name, file_path=file_path
    )

    print(
        f'created {result.object_name} object; etag: {result.etag}, version-id: {result.version_id}'
    )


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Insert arbitrary object(s) into arbitrary minIO bucket'
    )
    parser.add_argument('--ca-certs', '-c', required=True, help='Path to certificate')
    parser.add_argument('--endpoint', '-e', required=True, help='Path to minIO endpoint')
    parser.add_argument('--access-key', '-a', required=True, help='Path to access key')
    parser.add_argument('--secret-key', '-s', required=True, help='Path to secret key')
    parser.add_argument('--bucket-name', '-b', required=True, help='minIO bucket name')
    parser.add_argument('--object-name', '-o', required=True, help='minIO object name')
    parser.add_argument('--file-path', '-f', required=True, help='Path to file to insert')
    args = parser.parse_args()

    client = get_client(args.ca_certs, args.endpoint, args.access_key, args.secret_key)
    insert(client, args.bucket_name, args.object_name, args.file_path)
