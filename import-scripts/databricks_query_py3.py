#!/usr/bin/env python3

""" databricks_query_py3.py
This script can write local files to DataBricks Unity Catalog volumes, download files from volumes,
and delete files from volumes. To authenticate, you must specify DataBricks server hostname, HTTP path,
and personal access token.

Usage:
   python3 databricks_query_py3.py \
        --hostname $DATABRICKS_SERVER_HOSTNAME \
        --http-path $DATABRICKS_HTTP_PATH \
        --access-token $DATABRICKS_TOKEN \
        --mode get|put|remove \
        --input-file $INPUT_FILE \
        --output-file $OUTPUT_FILE
"""

from databricks import sql
import os, sys
import argparse

ERROR_FILE = sys.stderr


def get_connection_personal_access_token(server_hostname, http_path, access_token, staging_allowed_local_path=""):
    return sql.connect(
        server_hostname=server_hostname,
        http_path=http_path,
        access_token=access_token,
        staging_allowed_local_path=staging_allowed_local_path
    )

def execute_query(server_hostname, http_path, access_token, input_file, output_file, mode):
    # For writing local files to volumes and downloading files from volumes,
    # you must set the staging_allows_local_path argument to the path to the
    # local folder that contains the files to be written or downloaded.
    # For deleting files in volumes, you must also specify the
    # staging_allows_local_path argument, but its value is ignored,
    # so in that case its value can be set for example to an empty string.
    staging_allowed_local_path = ""
    if mode == "put":
        staging_allowed_local_path = os.path.dirname(input_file)
    elif mode == "get":
        staging_allowed_local_path = os.path.dirname(output_file)

    with get_connection_personal_access_token(
        server_hostname=server_hostname,
        http_path=http_path,
        access_token=access_token,
        staging_allowed_local_path=staging_allowed_local_path,
    ) as connection:
        with connection.cursor() as cursor:
            if mode == "put":
                # Write a local file to the specified path in a volume.
                # Specify OVERWRITE to overwrite any existing file in that path.
                cursor.execute(f"PUT '{input_file}' INTO '{output_file}' OVERWRITE")
            elif mode == "get":
                # Download a file from the specified path in a volume.
                cursor.execute(f"GET '{input_file}' TO '{output_file}'")
            elif mode == "remove":
                # Delete a file from the specified path in a volume.
                cursor.execute(f"REMOVE '{input_file}'")


def main():
    parser = argparse.ArgumentParser(prog="databricks_query_py3.py")
    parser.add_argument(
        "-s",
        "--server-hostname",
        dest="server_hostname",
        action="store",
        required=True,
        help="DataBricks server hostname",
    )
    parser.add_argument(
        "-p",
        "--http-path",
        dest="http_path",
        action="store",
        required=True,
        help="DataBricks HTTP path",
    )
    parser.add_argument(
        "-a",
        "--access-token",
        dest="access_token",
        action="store",
        required=True,
        help="DataBricks access token",
    )
    parser.add_argument(
        "-i",
        "--input-file",
        dest="input_file",
        action="store",
        required=True,
        help="paths to files to combine",
    )
    parser.add_argument(
        "-o",
        "--output-file",
        dest="output_file",
        action="store",
        required=False,
        help="output path for combined file",
    )
    parser.add_argument(
        "-m",
        "--mode",
        dest="mode",
        action="store",
        choices=["get", "put", "remove"],
        type=str.lower,
        required=True,
        help="",
    )

    args = parser.parse_args()
    server_hostname = args.server_hostname
    http_path = args.http_path
    access_token = args.access_token
    input_file = args.input_file
    output_file = args.output_file
    mode = args.mode

    # Check that correct combination of arguments were provided
    if (mode == "get" or mode == "put") and not output_file:
        parser.error(f"--output_file must be specified when --mode is 'get' or 'put'")

    # Check that the input files exist
    if mode == "put":
        if not os.path.exists(input_file):
            parser.error(f"No such file: {input_file}")

    # Execute the given DataBricks query
    execute_query(server_hostname, http_path, access_token, input_file, output_file, mode)


if __name__ == "__main__":
    main()
