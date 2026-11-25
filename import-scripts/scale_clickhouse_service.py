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
    parser.add_argument("-w", "--max-wait-seconds", type=int, default=600, help="maximum wait time for scaling to complete before returning. expired time does not result in error")
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
    if not (args.large ^ args.small):
        raise RuntimeError(f"it is required to choose either --small or --large")

def service_is_scaled(config, args, target_max_memory_gb):
    service = config['database_to_clickhouse_service_map'][args.database]
    url = config['clickhouse_service_to_max_memory_query_url'][service]
    api_key_id = config['conf_map']['api_key_id']
    api_key_secret = config['conf_map']['api_key_secret']
    response = requests.get(url, auth=(api_key_id, api_key_secret), timeout=30)
    if response.ok:
        data = response.json()['data']
        for host in data:
            current_max_memory_bytes = int(host['value'])
            target_max_memory_bytes = target_max_memory_gb * 1024 * 1024 * 1024
            difference_factor = float(abs(current_max_memory_bytes - target_max_memory_bytes)) / float(target_max_memory_bytes)
            print(f"replica response max memory bytes {current_max_memory_bytes}, target_max_memory_bytes {target_max_memory_bytes}, dfact {difference_factor}")
            if difference_factor > 0.15:
                print("scaling not done yet")
                return False
        print("scaling done")
        return True
    return False

def wait_for_scaling_to_complete(config, args, target_max_memory_gb):
    wait_start_time = int(time.time())
    wait_exit_time = wait_start_time + args.max_wait_seconds
    polling_period = 10
    while int(time.time()) + polling_period < wait_exit_time:
        time.sleep(polling_period)
        if service_is_scaled(config, args, target_max_memory_gb):
            return

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
    wait_for_scaling_to_complete(config, args, service_parameters['memory_gb'])

if __name__ == "__main__":
    parser = make_parser()
    args = parser.parse_args()
    config = read_config(args.config_file)
    validate_args_or_exit(config, args)
    scale_service(config, args)
