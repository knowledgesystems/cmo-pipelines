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


def dropped_too_much(dest, source):
    if source == 0:
        return False
    return dest < source * DROP_THRESHOLD


def report(label, dest, source=None, failed=False):
    if source is None:
        status = "FAIL" if failed else "OK"
        print(f"  {label + ':':<28} {dest}  [{status}]")
    else:
        arrow = f"{source} → {dest}"
        status = "FAIL" if failed else "OK"
        print(f"  {label + ':':<28} {arrow}  [{status}]")


def run_checks(client, dest_db, source_db, study_id, dest_id, source_id):
    failures = []

    # 1. Sample list exists
    dest_sl_count = scalar(client, f"SELECT count(*) FROM {dest_db}.sample_list WHERE cancer_study_id = {dest_id}")
    failed = dest_sl_count == 0
    report("sample lists", dest_sl_count, failed=failed)
    if failed:
        failures.append(f"sample list count is 0 for dest study '{study_id}'")

    # 2. _all sample list size
    def all_list_count(db, sid):
        return scalar(client,
            f"SELECT count(*) FROM {db}.sample_list_list sl "
            f"INNER JOIN {db}.sample_list s ON s.list_id = sl.list_id "
            f"WHERE s.stable_id = '{sid}_all'")

    dest_all = all_list_count(dest_db, study_id)
    source_all = all_list_count(source_db, study_id)
    failed = dropped_too_much(dest_all, source_all)
    report("_all list size", dest_all, source_all, failed=failed)
    if failed:
        failures.append(
            f"_all sample list dropped from {source_all} to {dest_all} "
            f"(>{int((1-DROP_THRESHOLD)*100)}% drop)"
        )

    # 3. All sample lists non-empty
    stable_ids = [
        row[0] for row in client.query(
            f"SELECT stable_id FROM {dest_db}.sample_list WHERE cancer_study_id = {dest_id}"
        ).result_rows
    ]
    empty_lists = []
    for sid in stable_ids:
        count = scalar(client,
            f"SELECT count(*) FROM {dest_db}.sample_list_list sl "
            f"INNER JOIN {dest_db}.sample_list s ON s.list_id = sl.list_id "
            f"WHERE s.stable_id = '{sid}'")
        if count == 0:
            empty_lists.append(sid)
            failures.append(f"sample list '{sid}' has 0 members")
    report("empty sample lists", len(empty_lists), failed=bool(empty_lists))

    # 4. Genetic profile count (strict: must not decrease at all)
    dest_gp = scalar(client, f"SELECT count(*) FROM {dest_db}.genetic_profile WHERE cancer_study_id = {dest_id}")
    source_gp = scalar(client, f"SELECT count(*) FROM {source_db}.genetic_profile WHERE cancer_study_id = {source_id}")
    failed = dest_gp < source_gp
    report("genetic profiles", dest_gp, source_gp, failed=failed)
    if failed:
        failures.append(f"genetic profile count dropped from {source_gp} to {dest_gp}")

    # 5. Patient count
    dest_pt = scalar(client, f"SELECT count(*) FROM {dest_db}.patient WHERE cancer_study_id = {dest_id}")
    source_pt = scalar(client, f"SELECT count(*) FROM {source_db}.patient WHERE cancer_study_id = {source_id}")
    failed = dropped_too_much(dest_pt, source_pt)
    report("patients", dest_pt, source_pt, failed=failed)
    if failed:
        failures.append(f"patient count dropped from {source_pt} to {dest_pt}")

    # 6. Sample count
    def sample_count(db, internal_id):
        return scalar(client,
            f"SELECT count(*) FROM {db}.sample s "
            f"LEFT JOIN {db}.patient p ON s.patient_id = p.internal_id "
            f"WHERE p.cancer_study_id = {internal_id}")

    dest_s = sample_count(dest_db, dest_id)
    source_s = sample_count(source_db, source_id)
    failed = dropped_too_much(dest_s, source_s)
    report("samples", dest_s, source_s, failed=failed)
    if failed:
        failures.append(f"sample count dropped from {source_s} to {dest_s}")

    # 7. Mutation count
    dest_mut_gp = get_genetic_profile_id(client, dest_db, dest_id, 'MUTATION_EXTENDED')
    source_mut_gp = get_genetic_profile_id(client, source_db, source_id, 'MUTATION_EXTENDED')
    if dest_mut_gp is not None and source_mut_gp is not None:
        dest_mut = scalar(client, f"SELECT count(*) FROM {dest_db}.mutation WHERE genetic_profile_id = {dest_mut_gp}")
        source_mut = scalar(client, f"SELECT count(*) FROM {source_db}.mutation WHERE genetic_profile_id = {source_mut_gp}")
        failed = dropped_too_much(dest_mut, source_mut)
        report("mutations", dest_mut, source_mut, failed=failed)
        if failed:
            failures.append(f"mutation count dropped from {source_mut} to {dest_mut}")
    else:
        print(f"  {'mutations:':<28} skipped (profile absent in one DB)")

    # 8. Structural variant count
    dest_sv_gp = get_genetic_profile_id(client, dest_db, dest_id, 'STRUCTURAL_VARIANT')
    source_sv_gp = get_genetic_profile_id(client, source_db, source_id, 'STRUCTURAL_VARIANT')
    if dest_sv_gp is not None and source_sv_gp is not None:
        dest_sv = scalar(client, f"SELECT count(*) FROM {dest_db}.structural_variant WHERE genetic_profile_id = {dest_sv_gp}")
        source_sv = scalar(client, f"SELECT count(*) FROM {source_db}.structural_variant WHERE genetic_profile_id = {source_sv_gp}")
        failed = dropped_too_much(dest_sv, source_sv)
        report("structural variants", dest_sv, source_sv, failed=failed)
        if failed:
            failures.append(f"structural variant count dropped from {source_sv} to {dest_sv}")
    else:
        print(f"  {'structural variants:':<28} skipped (profile absent in one DB)")

    # 9. CNA event count
    dest_cna_gp = get_genetic_profile_id(client, dest_db, dest_id, 'COPY_NUMBER_ALTERATION', 'DISCRETE')
    source_cna_gp = get_genetic_profile_id(client, source_db, source_id, 'COPY_NUMBER_ALTERATION', 'DISCRETE')
    if dest_cna_gp is not None and source_cna_gp is not None:
        def cna_event_count(db, gpid):
            return scalar(client,
                f"SELECT countIf(val != '0') FROM ("
                f"SELECT arrayJoin(splitByChar(',', `values`)) AS val "
                f"FROM {db}.genetic_alteration WHERE genetic_profile_id = {gpid})")
        dest_cna = cna_event_count(dest_db, dest_cna_gp)
        source_cna = cna_event_count(source_db, source_cna_gp)
        failed = dropped_too_much(dest_cna, source_cna)
        report("CNA events", dest_cna, source_cna, failed=failed)
        if failed:
            failures.append(f"CNA event count dropped from {source_cna} to {dest_cna}")
    else:
        print(f"  {'CNA events:':<28} skipped (profile absent in one DB)")

    # 10. Segment count
    dest_seg = scalar(client, f"SELECT count(*) FROM {dest_db}.copy_number_seg WHERE cancer_study_id = {dest_id}")
    source_seg = scalar(client, f"SELECT count(*) FROM {source_db}.copy_number_seg WHERE cancer_study_id = {source_id}")
    failed = dropped_too_much(dest_seg, source_seg)
    report("segments", dest_seg, source_seg, failed=failed)
    if failed:
        failures.append(f"copy number segment count dropped from {source_seg} to {dest_seg}")

    return failures


def main():
    parser = argparse.ArgumentParser(description='Validate blue/green ClickHouse study counts.')
    parser.add_argument('--properties-file', required=True)
    parser.add_argument('--study-id', required=True)
    parser.add_argument('--dest-color', required=True, choices=['blue', 'green'])
    args = parser.parse_args()

    props = parse_properties(args.properties_file)
    source_color = 'green' if args.dest_color == 'blue' else 'blue'
    dest_db = props[f'clickhouse_{args.dest_color}_database_name']
    source_db = props[f'clickhouse_{source_color}_database_name']

    client = get_client(props)

    dest_id = get_internal_id(client, dest_db, args.study_id)
    if dest_id is None:
        print(f"ERROR: study '{args.study_id}' not found in dest ({args.dest_color}) database '{dest_db}'")
        sys.exit(1)

    source_id = get_internal_id(client, source_db, args.study_id)
    if source_id is None:
        print(f"Study '{args.study_id}' not found in source ({source_color}) database '{source_db}' -- new study, skipping validation")
        sys.exit(0)

    print(f"Validating '{args.study_id}': {args.dest_color} (dest) vs {source_color} (source)")
    failures = run_checks(client, dest_db, source_db, args.study_id, dest_id, source_id)

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
