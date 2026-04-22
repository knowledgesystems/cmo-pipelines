#!/usr/bin/env python3

import argparse
import sys
import clickhouse_connect

DROP_THRESHOLD = 0.90


def parse_properties(path):
    props = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, _, value = line.partition('=')
                props[key.strip()] = value.strip()
    return props


def get_client(props):
    secure = 'secure=true' in props.get('clickhouse_server_additional_args', '')
    return clickhouse_connect.get_client(
        host=props['clickhouse_server_host_name'],
        # Hardcode for now; clickhouse_server_port=9440 defined in the file was the native TCP protocol being used by Sling
        port=8443,
        username=props['clickhouse_server_username'],
        password=props['clickhouse_server_password'],
        secure=secure,
    )


def scalar(client, query):
    result = client.query(query)
    return result.result_rows[0][0]


def get_internal_id(client, db, study_id):
    rows = client.query(
        f"SELECT cancer_study_id FROM {db}.cancer_study WHERE cancer_study_identifier = '{study_id}'"
    ).result_rows
    return rows[0][0] if rows else None


def get_genetic_profile_id(client, db, internal_id, alteration_type, datatype=None):
    q = (f"SELECT genetic_profile_id FROM {db}.genetic_profile "
         f"WHERE cancer_study_id = {internal_id} "
         f"AND genetic_alteration_type = '{alteration_type}'")
    if datatype:
        q += f" AND datatype = '{datatype}'"
    rows = client.query(q).result_rows
    return rows[0][0] if len(rows) == 1 else None


def dropped_too_much(current, baseline):
    if baseline == 0:
        return False
    return current < baseline * DROP_THRESHOLD


def run_checks(client, current_db, baseline_db, study_id, current_id, baseline_id):
    failures = []

    # 1. Sample list exists
    current_sl_count = scalar(client, f"SELECT count(*) FROM {current_db}.sample_list WHERE cancer_study_id = {current_id}")
    if current_sl_count == 0:
        failures.append(f"sample list count is 0 for current study '{study_id}'")

    # 2. _all sample list size
    def all_list_count(db, sid):
        return scalar(client,
            f"SELECT count(*) FROM {db}.sample_list_list sl "
            f"INNER JOIN {db}.sample_list s ON s.list_id = sl.list_id "
            f"WHERE s.stable_id = '{sid}_all'")

    current_all = all_list_count(current_db, study_id)
    baseline_all = all_list_count(baseline_db, study_id)
    if dropped_too_much(current_all, baseline_all):
        failures.append(
            f"_all sample list dropped from {baseline_all} to {current_all} "
            f"(>{int((1-DROP_THRESHOLD)*100)}% drop)"
        )

    # 3. All sample lists non-empty
    stable_ids = [
        row[0] for row in client.query(
            f"SELECT stable_id FROM {current_db}.sample_list WHERE cancer_study_id = {current_id}"
        ).result_rows
    ]
    for sid in stable_ids:
        count = scalar(client,
            f"SELECT count(*) FROM {current_db}.sample_list_list sl "
            f"INNER JOIN {current_db}.sample_list s ON s.list_id = sl.list_id "
            f"WHERE s.stable_id = '{sid}'")
        if count == 0:
            failures.append(f"sample list '{sid}' has 0 members")

    # 4. Genetic profile count (strict: must not decrease at all)
    current_gp = scalar(client, f"SELECT count(*) FROM {current_db}.genetic_profile WHERE cancer_study_id = {current_id}")
    baseline_gp = scalar(client, f"SELECT count(*) FROM {baseline_db}.genetic_profile WHERE cancer_study_id = {baseline_id}")
    if current_gp < baseline_gp:
        failures.append(f"genetic profile count dropped from {baseline_gp} to {current_gp}")

    # 5. Patient count
    current_pt = scalar(client, f"SELECT count(*) FROM {current_db}.patient WHERE cancer_study_id = {current_id}")
    baseline_pt = scalar(client, f"SELECT count(*) FROM {baseline_db}.patient WHERE cancer_study_id = {baseline_id}")
    if dropped_too_much(current_pt, baseline_pt):
        failures.append(f"patient count dropped from {baseline_pt} to {current_pt}")

    # 6. Sample count
    def sample_count(db, internal_id):
        return scalar(client,
            f"SELECT count(*) FROM {db}.sample s "
            f"LEFT JOIN {db}.patient p ON s.patient_id = p.internal_id "
            f"WHERE p.cancer_study_id = {internal_id}")

    current_s = sample_count(current_db, current_id)
    baseline_s = sample_count(baseline_db, baseline_id)
    if dropped_too_much(current_s, baseline_s):
        failures.append(f"sample count dropped from {baseline_s} to {current_s}")

    # 7. Mutation count
    current_mut_gp = get_genetic_profile_id(client, current_db, current_id, 'MUTATION_EXTENDED')
    baseline_mut_gp = get_genetic_profile_id(client, baseline_db, baseline_id, 'MUTATION_EXTENDED')
    if current_mut_gp is not None and baseline_mut_gp is not None:
        current_mut = scalar(client, f"SELECT count(*) FROM {current_db}.mutation WHERE genetic_profile_id = {current_mut_gp}")
        baseline_mut = scalar(client, f"SELECT count(*) FROM {baseline_db}.mutation WHERE genetic_profile_id = {baseline_mut_gp}")
        if dropped_too_much(current_mut, baseline_mut):
            failures.append(f"mutation count dropped from {baseline_mut} to {current_mut}")

    # 8. Structural variant count
    current_sv_gp = get_genetic_profile_id(client, current_db, current_id, 'STRUCTURAL_VARIANT')
    baseline_sv_gp = get_genetic_profile_id(client, baseline_db, baseline_id, 'STRUCTURAL_VARIANT')
    if current_sv_gp is not None and baseline_sv_gp is not None:
        current_sv = scalar(client, f"SELECT count(*) FROM {current_db}.structural_variant WHERE genetic_profile_id = {current_sv_gp}")
        baseline_sv = scalar(client, f"SELECT count(*) FROM {baseline_db}.structural_variant WHERE genetic_profile_id = {baseline_sv_gp}")
        if dropped_too_much(current_sv, baseline_sv):
            failures.append(f"structural variant count dropped from {baseline_sv} to {current_sv}")

    # 9. CNA event count
    current_cna_gp = get_genetic_profile_id(client, current_db, current_id, 'COPY_NUMBER_ALTERATION', 'DISCRETE')
    baseline_cna_gp = get_genetic_profile_id(client, baseline_db, baseline_id, 'COPY_NUMBER_ALTERATION', 'DISCRETE')
    if current_cna_gp is not None and baseline_cna_gp is not None:
        def cna_event_count(db, gpid):
            return scalar(client,
                f"SELECT countIf(val != '0') FROM ("
                f"SELECT arrayJoin(splitByChar(',', `values`)) AS val "
                f"FROM {db}.genetic_alteration WHERE genetic_profile_id = {gpid})")
        current_cna = cna_event_count(current_db, current_cna_gp)
        baseline_cna = cna_event_count(baseline_db, baseline_cna_gp)
        if dropped_too_much(current_cna, baseline_cna):
            failures.append(f"CNA event count dropped from {baseline_cna} to {current_cna}")

    # 10. Segment count
    current_seg = scalar(client, f"SELECT count(*) FROM {current_db}.copy_number_seg WHERE cancer_study_id = {current_id}")
    baseline_seg = scalar(client, f"SELECT count(*) FROM {baseline_db}.copy_number_seg WHERE cancer_study_id = {baseline_id}")
    if dropped_too_much(current_seg, baseline_seg):
        failures.append(f"copy number segment count dropped from {baseline_seg} to {current_seg}")

    return failures


def main():
    parser = argparse.ArgumentParser(description='Validate blue/green ClickHouse study counts.')
    parser.add_argument('--properties-file', required=True)
    parser.add_argument('--study-id', required=True)
    parser.add_argument('--current-color', required=True, choices=['blue', 'green'])
    args = parser.parse_args()

    props = parse_properties(args.properties_file)
    opposite_color = 'green' if args.current_color == 'blue' else 'blue'
    current_db = props[f'clickhouse_{args.current_color}_database_name']
    baseline_db = props[f'clickhouse_{opposite_color}_database_name']

    client = get_client(props)

    current_id = get_internal_id(client, current_db, args.study_id)
    if current_id is None:
        print(f"ERROR: study '{args.study_id}' not found in current ({args.current_color}) database '{current_db}'")
        sys.exit(1)

    baseline_id = get_internal_id(client, baseline_db, args.study_id)
    if baseline_id is None:
        print(f"Study '{args.study_id}' not found in baseline ({opposite_color}) database '{baseline_db}' -- new study, skipping validation")
        sys.exit(0)

    print(f"Validating '{args.study_id}': {args.current_color} (current) vs {opposite_color} (baseline)")
    failures = run_checks(client, current_db, baseline_db, args.study_id, current_id, baseline_id)

    if failures:
        print(f"VALIDATION FAILED ({len(failures)} issue(s)):")
        for f in failures:
            print(f"  - {f}")
        sys.exit(1)
    else:
        print("Validation passed")
        sys.exit(0)


if __name__ == '__main__':
    main()
