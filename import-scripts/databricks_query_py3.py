#!/usr/bin/env python3

""" databricks_query_py3.py
This script can:
- execute SQL commands 
- write local files to DataBricks Unity Catalog volumes, download files from volumes, and delete files from volumes.
To authenticate, you must specify DataBricks server hostname, HTTP path, and personal access token.

Usage:
   To execute a volume operation:
   python3 databricks_query_py3.py \
        --hostname $DATABRICKS_SERVER_HOSTNAME \
        --http-path $DATABRICKS_HTTP_PATH \
        --access-token $DATABRICKS_TOKEN \
        volume \
        --mode get|put|remove
        --input-path $INPUT_PATH \
        --output-path $OUTPUT_PATH

   To execute a SQL query:
   python3 databricks_query_py3.py \
        --hostname $DATABRICKS_SERVER_HOSTNAME \
        --http-path $DATABRICKS_HTTP_PATH \
        --access-token $DATABRICKS_TOKEN \
        query \
        --sql-query $SQL_QUERY \
        --output-path $OUTPUT_PATH

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

# Execute a SQL command and write the results to a tab-delimited file
def execute_sql_query(server_hostname, http_path, access_token, sql_query, output_path):
    with get_connection_personal_access_token(
        server_hostname=server_hostname,
        http_path=http_path,
        access_token=access_token
    ) as connection:
        with connection.cursor() as cursor:
            cursor.execute(sql_query)
            result = cursor.fetchall()
            header = True
            with open(output_path, 'w') as f:
                for row in result:
                    row_dict = row.asDict()
                    if header:
                        print("\t".join([column_name for column_name in row_dict.keys()]), file=f)
                        header = False
                    print("\t".join([row_value if row_value is not None else "" for row_value in row_dict.values()]), file=f)

# Execute a volume operation ('get', 'put', or 'remove')
def execute_volume_query(server_hostname, http_path, access_token, input_path, output_path, mode):
    # For writing local files to volumes and downloading files from volumes,
    # you must set the staging_allows_local_path argument to the path to the
    # local folder that contains the files to be written or downloaded.
    # For deleting files in volumes, you must also specify the
    # staging_allows_local_path argument, but its value is ignored,
    # so in that case its value can be set for example to an empty string.
    staging_allowed_local_path = ""
    if mode == "put":
        staging_allowed_local_path = os.path.dirname(input_path)
    elif mode == "get":
        staging_allowed_local_path = os.path.dirname(output_path)

    with get_connection_personal_access_token(
        server_hostname=server_hostname,
        http_path=http_path,
        access_token=access_token,
        staging_allowed_local_path=staging_allowed_local_path,
    ) as connection:
        with connection.cursor() as cursor:
            # Volume operations
            if mode == "put":
                # Write a local file to the specified path in a volume.
                # Specify OVERWRITE to overwrite any existing file in that path.
                cursor.execute(f"PUT '{input_path}' INTO '{output_path}' OVERWRITE")
            elif mode == "get":
                # Download a file from the specified path in a volume.
                cursor.execute(f"GET '{input_path}' TO '{output_path}'")
            elif mode == "remove":
                # Delete a file from the specified path in a volume.
                cursor.execute(f"REMOVE '{input_path}'")


def main():
    parser = argparse.ArgumentParser(prog="databricks_query_py3.py")
    
    # Define required arguments
    parser.add_argument("-s", "--server-hostname", dest="server_hostname", action="store", required=True, help="DataBricks server hostname")
    parser.add_argument("-p", "--http-path", dest="http_path", action="store", required=True, help="DataBricks HTTP path")
    parser.add_argument("-a", "--access-token", dest="access_token", action="store", required=True, help="DataBricks access token")

    # Define subparsers ('query' and 'volume')
    subparsers = parser.add_subparsers(help='subcommand help', dest='subcommand')

    # Query operation arguments
    query_parser = subparsers.add_parser('query', help='query help')
    query_parser.add_argument("-s", "--sql-query", dest="sql_query", action="store", required=True, help="SQL query to execute")
    query_parser.add_argument("-o", "--output-path", dest="output_path", action="store", required=True, help="Output path")

    # Volume operation arguments
    volume_parser = subparsers.add_parser('volume', help='volume help')
    volume_parser.add_argument("-m", "--mode", dest="mode", action="store", choices=["get", "put", "remove"], type=str.lower, required=True, help="Type of volume operation to execute")
    volume_parser.add_argument("-i", "--input-path", dest="input_path", action="store", required=True, help="Input path")
    volume_parser.add_argument("-o", "--output-path", dest="output_path", action="store", required=False, help="Output path")

    args = parser.parse_args()

    # Store required args
    server_hostname = args.server_hostname
    http_path = args.http_path
    access_token = args.access_token

    # If 'query' subcommand was provided, pass appropriate args to execute_sql_query function
    if args.subcommand == "query":
        sql_query = args.sql_query
        output_path = args.output_path
        execute_sql_query(server_hostname, http_path, access_token, sql_query, output_path)

    # If 'volume' subcommand was provided, pass appropriate args to execute_volume_query function
    elif args.subcommand == "volume":
        input_path = args.input_path
        output_path = args.output_path
        mode = args.mode

        # Check that correct combination of arguments were provided
        if (mode == "get" or mode == "put") and not output_path:
            parser.error(f"--output_path must be specified when --mode is 'get' or 'put'")

        # Check that the input file exists
        if mode == "put":
            if not os.path.exists(input_path):
                parser.error(f"No such file: {input_path}")

        execute_volume_query(server_hostname, http_path, access_token, input_path, output_path, mode)


if __name__ == "__main__":
    main()
