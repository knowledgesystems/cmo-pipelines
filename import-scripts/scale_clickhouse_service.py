#!/usr/bin/env python3

import argparse
import json
import requests
import time

def make_parser():
    parser = argparse.ArgumentParser(
        description="Scale a Clickhouse service for import operations",
        epilog="Example: scale_clickhouse_service.py -d public --large"
    )
    parser.add_argument('config_file', help="path to configuration file")
    parser.add_argument("-d", "--database", type=str, required=True, help="short name of database for import {public, genie, msk, triage}")
    parser.add_argument("-l", "--large", action="store_true", help="scale database to import(larger) size")
    parser.add_argument("-s", "--small", action="store_true", help="scale database to non-import(standard) size")
    parser.add_argument("-w", "--max-wait-seconds", type=int, default=600, help="maximum seconds to wait for scaling (0 skips the check). scale-up fails on timeout")
    return parser

def read_config(config_filepath):
    with open(config_filepath, "r", encoding="utf-8") as file:
        config = json.load(file)
    return config

def validate_config_or_exit(config):
    required_elements = ['database_identifier_list', 'database_to_clickhouse_service_map', 'clickhouse_service_to_id_map', 'clickhouse_service_to_small_replica_count_map', 'clickhouse_service_to_large_replica_count_map', 'clickhouse_service_to_small_replica_memory_gb_map', 'clickhouse_service_to_large_replica_memory_gb_map', 'clickhouse_service_to_max_memory_query_url', 'conf_map']
    for element in required_elements:
        if not element in config:
            raise RuntimeError(f"the parsed config file did not contain required element: {element}");
    conf_map = config['conf_map']
    required_elements = ['api_key_id', 'api_key_secret', 'organization_id', 'clickhouse_cloud_api_base_url']
    for element in required_elements:
        if not element in conf_map:
            raise RuntimeError(f"the parsed config file did not contain required element under conf_map: {element}");

def validate_args_or_exit(config, args):
    validate_config_or_exit(config)
    if not args.database in config['database_identifier_list'] or not args.database in config['database_to_clickhouse_service_map']:
        raise RuntimeError(f"the passed in database {args.database} is not defined in the config_file")
    if args.max_wait_seconds < 0:
        raise ValueError("--max-wait-seconds must be nonnegative")
    if not (args.large ^ args.small):
        raise RuntimeError(f"it is required to choose either --small or --large")

def scaled_hosts(config, args, target, timeout):
    service = config['database_to_clickhouse_service_map'][args.database]
    url = config['clickhouse_service_to_max_memory_query_url'][service]
    auth = (config['conf_map']['api_key_id'], config['conf_map']['api_key_secret'])
    try:
        response = requests.get(url, auth=auth, timeout=timeout)
    except (requests.Timeout, requests.ConnectionError) as error:
        print(f"Replica query failed ({type(error).__name__})")
        return set()
    if not response.ok:
        print(f"Replica query returned HTTP {response.status_code}")
        return set()

    # The saved clusterAllReplicas query returns one memory setting per host.
    rows = response.json()['data']
    hosts = {row['host'] for row in rows}
    if len(hosts) != target['replica_count'] or len(rows) != len(hosts):
        print(f"Expected {target['replica_count']} distinct replicas, received {len(hosts)} in {len(rows)} rows")
        return set()
    target_bytes = target['memory_gb'] * 1024 ** 3
    for row in rows:
        memory = int(row['value'])
        if abs(memory - target_bytes) > 0.15 * target_bytes:
            print(f"Replica {row['host']} reports {memory} bytes, target {target_bytes}")
            return set()
    return hosts

def wait_for_scaling_to_complete(config, args, target):
    if args.max_wait_seconds == 0:
        return
    deadline = time.monotonic() + args.max_wait_seconds
    previous_hosts = set()
    consecutive_polls = 0
    required_polls = 3 if args.large else 1
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        hosts = scaled_hosts(config, args, target, timeout=min(30, remaining))
        consecutive_polls = consecutive_polls + 1 if hosts == previous_hosts else 1
        previous_hosts = hosts
        if hosts:
            print(f"Ready replicas {sorted(hosts)} (poll {consecutive_polls}/{required_polls})")
            if consecutive_polls >= required_polls and time.monotonic() <= deadline:
                print("scaling done")
                return
        time.sleep(max(0, min(10, deadline - time.monotonic())))
    if args.large:
        raise RuntimeError(f"ClickHouse replicas for {args.database} were not ready within {args.max_wait_seconds}s")

def clickhouse_cloud_service_url(config, args):
    service = config['database_to_clickhouse_service_map'][args.database]
    service_id = config['clickhouse_service_to_id_map'][service]
    org_id = config['conf_map']['organization_id']
    clickhouse_cloud_api_base_url = config['conf_map']['clickhouse_cloud_api_base_url']
    return f"{clickhouse_cloud_api_base_url}/organizations/{org_id}/services/{service_id}/replicaScaling"

def find_service_parameters(config, args) -> {}:
    service = config['database_to_clickhouse_service_map'][args.database]
    if args.small:
        replica_count = config['clickhouse_service_to_small_replica_count_map'][service]
        memory_gb = config['clickhouse_service_to_small_replica_memory_gb_map'][service]
    else:
        replica_count = config['clickhouse_service_to_large_replica_count_map'][service]
        memory_gb = config['clickhouse_service_to_large_replica_memory_gb_map'][service]
    return {'memory_gb' : memory_gb, 'replica_count' : replica_count}

def scale_service(config, args):
    url = clickhouse_cloud_service_url(config, args)
    api_key_id = config['conf_map']['api_key_id']
    api_key_secret = config['conf_map']['api_key_secret']
    service_parameters = find_service_parameters(config, args)
    payload = {
        "autoscalingMode": "vertical",
        "minReplicaMemoryGb": service_parameters['memory_gb'],
        "maxReplicaMemoryGb": service_parameters['memory_gb'],
        "numReplicas": service_parameters['replica_count'],
        "idleScaling": False
    }
    response = requests.patch(
        url,
        json=payload,
        auth=requests.auth.HTTPBasicAuth(api_key_id, api_key_secret),
        headers={"Content-Type": "application/json"},
        timeout=30,
    )
    response.raise_for_status()
    wait_for_scaling_to_complete(config, args, service_parameters)

if __name__ == "__main__":
    parser = make_parser()
    args = parser.parse_args()
    config = read_config(args.config_file)
    validate_args_or_exit(config, args)
    scale_service(config, args)
