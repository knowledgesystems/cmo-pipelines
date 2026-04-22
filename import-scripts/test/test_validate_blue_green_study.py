#!/usr/bin/env python3
"""Unit tests for validate_blue_green_study.py."""

import os
import sys
import unittest
from io import StringIO
from unittest.mock import MagicMock, patch

# Stub clickhouse_connect so the module loads without the package installed
sys.modules['clickhouse_connect'] = MagicMock()
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import validate_blue_green_study as v

CURRENT_DB = 'cdb'
BASELINE_DB = 'bdb'
STUDY_ID    = 'test_study'
CURRENT_ID  = 1
BASELINE_ID = 2
STABLE_IDS  = [f'{STUDY_ID}_all', f'{STUDY_ID}_sequenced']

# ── indices into the default query sequence (2 stable_ids assumed) ───────────
# Each entry corresponds to one client.query() call made by run_checks(), in order.
I_SAMPLE_LIST_COUNT = 0   # scalar: sample_list count
I_ALL_CURRENT       = 1   # scalar: _all list size, current db
I_ALL_BASELINE      = 2   # scalar: _all list size, baseline db
I_STABLE_IDS        = 3   # rows:   all stable_ids for the study
I_LIST_COUNT_0      = 4   # scalar: member count for STABLE_IDS[0]
I_LIST_COUNT_1      = 5   # scalar: member count for STABLE_IDS[1]
I_GP_CURRENT        = 6   # scalar: genetic_profile count, current
I_GP_BASELINE       = 7   # scalar: genetic_profile count, baseline
I_PT_CURRENT        = 8   # scalar: patient count, current
I_PT_BASELINE       = 9   # scalar: patient count, baseline
I_S_CURRENT         = 10  # scalar: sample count, current
I_S_BASELINE        = 11  # scalar: sample count, baseline
I_MUT_PROF_CURRENT  = 12  # rows:   mutation genetic_profile_id, current
I_MUT_PROF_BASELINE = 13  # rows:   mutation genetic_profile_id, baseline
I_MUT_CURRENT       = 14  # scalar: mutation count, current  (skipped if profile absent)
I_MUT_BASELINE      = 15  # scalar: mutation count, baseline (skipped if profile absent)
I_SV_PROF_CURRENT   = 16  # rows:   SV genetic_profile_id, current
I_SV_PROF_BASELINE  = 17  # rows:   SV genetic_profile_id, baseline
I_SV_CURRENT        = 18  # scalar: SV count, current        (skipped if profile absent)
I_SV_BASELINE       = 19  # scalar: SV count, baseline       (skipped if profile absent)
I_CNA_PROF_CURRENT  = 20  # rows:   CNA genetic_profile_id, current
I_CNA_PROF_BASELINE = 21  # rows:   CNA genetic_profile_id, baseline
I_CNA_CURRENT       = 22  # scalar: CNA event count, current (skipped if profile absent)
I_CNA_BASELINE      = 23  # scalar: CNA event count, baseline
I_SEG_CURRENT       = 24  # scalar: segment count, current
I_SEG_BASELINE      = 25  # scalar: segment count, baseline


def qr(value):
    """Mock scalar query result: result_rows[0][0] == value."""
    m = MagicMock()
    m.result_rows = [[value]]
    return m


def qrows(*rows):
    """Mock multi-row query result (used by get_internal_id / get_genetic_profile_id)."""
    m = MagicMock()
    m.result_rows = list(rows)
    return m


def default_returns():
    """Full query sequence for the all-checks-pass baseline (2 stable_ids, profiles present)."""
    return [
        qr(5),                                    # I_SAMPLE_LIST_COUNT
        qr(1000),                                 # I_ALL_CURRENT
        qr(1000),                                 # I_ALL_BASELINE
        qrows([STABLE_IDS[0]], [STABLE_IDS[1]]), # I_STABLE_IDS
        qr(500),                                  # I_LIST_COUNT_0
        qr(300),                                  # I_LIST_COUNT_1
        qr(12),                                   # I_GP_CURRENT
        qr(12),                                   # I_GP_BASELINE
        qr(1000),                                 # I_PT_CURRENT
        qr(1000),                                 # I_PT_BASELINE
        qr(1000),                                 # I_S_CURRENT
        qr(1000),                                 # I_S_BASELINE
        qrows([42]),                              # I_MUT_PROF_CURRENT
        qrows([43]),                              # I_MUT_PROF_BASELINE
        qr(5000),                                 # I_MUT_CURRENT
        qr(5000),                                 # I_MUT_BASELINE
        qrows([44]),                              # I_SV_PROF_CURRENT
        qrows([45]),                              # I_SV_PROF_BASELINE
        qr(200),                                  # I_SV_CURRENT
        qr(200),                                  # I_SV_BASELINE
        qrows([46]),                              # I_CNA_PROF_CURRENT
        qrows([47]),                              # I_CNA_PROF_BASELINE
        qr(10000),                                # I_CNA_CURRENT
        qr(10000),                                # I_CNA_BASELINE
        qr(3000),                                 # I_SEG_CURRENT
        qr(3000),                                 # I_SEG_BASELINE
    ]


def make_client(overrides=None):
    """Build a mock ClickHouse client from the default sequence with optional overrides."""
    returns = default_returns()
    for idx, val in (overrides or {}).items():
        returns[idx] = val
    client = MagicMock()
    client.query.side_effect = returns
    return client


def run(client):
    """Invoke run_checks() with standard test parameters, capturing stdout."""
    buf = StringIO()
    with patch('sys.stdout', buf):
        failures = v.run_checks(
            client, CURRENT_DB, BASELINE_DB, STUDY_ID, CURRENT_ID, BASELINE_ID
        )
    return failures, buf.getvalue()


class TestRunChecks(unittest.TestCase):

    # ── baseline ─────────────────────────────────────────────────────────────

    def test_all_pass_returns_no_failures(self):
        failures, output = run(make_client())
        self.assertEqual(failures, [])
        self.assertNotIn('[FAIL]', output)

    def test_all_pass_prints_counts(self):
        _, output = run(make_client())
        self.assertIn('[OK]', output)
        self.assertIn('1000', output)

    # ── check 1: sample list exists ─────────────────────────────────────────

    def test_sample_list_count_zero_fails(self):
        failures, _ = run(make_client({I_SAMPLE_LIST_COUNT: qr(0)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('sample list count is 0', failures[0])

    def test_sample_list_count_nonzero_passes(self):
        failures, _ = run(make_client({I_SAMPLE_LIST_COUNT: qr(1)}))
        self.assertEqual(failures, [])

    # ── check 2: _all sample list size ──────────────────────────────────────

    def test_all_list_90pct_drop_fails(self):
        failures, _ = run(make_client({I_ALL_CURRENT: qr(100), I_ALL_BASELINE: qr(1000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('_all sample list dropped', failures[0])
        self.assertIn('1000', failures[0])
        self.assertIn('100', failures[0])

    def test_all_list_9pct_drop_passes(self):
        failures, _ = run(make_client({I_ALL_CURRENT: qr(910), I_ALL_BASELINE: qr(1000)}))
        self.assertEqual(failures, [])

    def test_all_list_zero_baseline_passes(self):
        # Zero baseline means new study — no drop check applied
        failures, _ = run(make_client({I_ALL_CURRENT: qr(0), I_ALL_BASELINE: qr(0)}))
        self.assertEqual(failures, [])

    # ── check 3: no empty sample lists ──────────────────────────────────────

    def test_empty_sample_list_fails(self):
        failures, _ = run(make_client({I_LIST_COUNT_0: qr(0)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('has 0 members', failures[0])
        self.assertIn(STABLE_IDS[0], failures[0])

    def test_second_list_empty_fails(self):
        failures, _ = run(make_client({I_LIST_COUNT_1: qr(0)}))
        self.assertEqual(len(failures), 1)
        self.assertIn(STABLE_IDS[1], failures[0])

    # ── check 4: genetic profile count (strict — no drop allowed) ───────────

    def test_genetic_profile_dropped_by_one_fails(self):
        failures, _ = run(make_client({I_GP_CURRENT: qr(11), I_GP_BASELINE: qr(12)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('genetic profile count dropped', failures[0])
        self.assertIn('12', failures[0])
        self.assertIn('11', failures[0])

    def test_genetic_profile_equal_passes(self):
        failures, _ = run(make_client({I_GP_CURRENT: qr(12), I_GP_BASELINE: qr(12)}))
        self.assertEqual(failures, [])

    def test_genetic_profile_increased_passes(self):
        failures, _ = run(make_client({I_GP_CURRENT: qr(13), I_GP_BASELINE: qr(12)}))
        self.assertEqual(failures, [])

    # ── check 5: patient count ───────────────────────────────────────────────

    def test_patient_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_PT_CURRENT: qr(100), I_PT_BASELINE: qr(1000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('patient count dropped', failures[0])

    def test_patient_count_9pct_drop_passes(self):
        failures, _ = run(make_client({I_PT_CURRENT: qr(910), I_PT_BASELINE: qr(1000)}))
        self.assertEqual(failures, [])

    # ── check 6: sample count ────────────────────────────────────────────────

    def test_sample_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_S_CURRENT: qr(100), I_S_BASELINE: qr(1000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('sample count dropped', failures[0])

    def test_sample_count_9pct_drop_passes(self):
        failures, _ = run(make_client({I_S_CURRENT: qr(910), I_S_BASELINE: qr(1000)}))
        self.assertEqual(failures, [])

    # ── check 7: mutation count ──────────────────────────────────────────────

    def test_mutation_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_MUT_CURRENT: qr(100), I_MUT_BASELINE: qr(1000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('mutation count dropped', failures[0])

    def test_mutation_profile_absent_skipped(self):
        # When the profile is absent in current, count queries are never issued.
        # Build an explicit sequence without the two mutation-count entries.
        returns = default_returns()
        returns[I_MUT_PROF_CURRENT] = qrows()        # empty → profile id = None
        del returns[I_MUT_BASELINE]                   # remove higher index first
        del returns[I_MUT_CURRENT]
        client = MagicMock()
        client.query.side_effect = returns
        failures, output = run(client)
        self.assertEqual(failures, [])
        self.assertIn('skipped', output)

    # ── check 8: structural variant count ────────────────────────────────────

    def test_sv_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_SV_CURRENT: qr(10), I_SV_BASELINE: qr(200)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('structural variant count dropped', failures[0])

    def test_sv_count_9pct_drop_passes(self):
        failures, _ = run(make_client({I_SV_CURRENT: qr(182), I_SV_BASELINE: qr(200)}))
        self.assertEqual(failures, [])

    # ── check 9: CNA event count ─────────────────────────────────────────────

    def test_cna_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_CNA_CURRENT: qr(100), I_CNA_BASELINE: qr(10000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('CNA event count dropped', failures[0])

    def test_cna_count_9pct_drop_passes(self):
        failures, _ = run(make_client({I_CNA_CURRENT: qr(9100), I_CNA_BASELINE: qr(10000)}))
        self.assertEqual(failures, [])

    # ── check 10: segment count ──────────────────────────────────────────────

    def test_segment_count_90pct_drop_fails(self):
        failures, _ = run(make_client({I_SEG_CURRENT: qr(100), I_SEG_BASELINE: qr(3000)}))
        self.assertEqual(len(failures), 1)
        self.assertIn('copy number segment count dropped', failures[0])

    def test_segment_count_9pct_drop_passes(self):
        failures, _ = run(make_client({I_SEG_CURRENT: qr(2730), I_SEG_BASELINE: qr(3000)}))
        self.assertEqual(failures, [])

    # ── multiple simultaneous failures ───────────────────────────────────────

    def test_multiple_failures_all_reported(self):
        failures, _ = run(make_client({
            I_PT_CURRENT: qr(100), I_PT_BASELINE: qr(1000),
            I_S_CURRENT:  qr(100), I_S_BASELINE:  qr(1000),
        }))
        self.assertEqual(len(failures), 2)


class TestMain(unittest.TestCase):

    def _run_main(self, current_id, baseline_id, check_results=None, color='green'):
        props = {
            'clickhouse_green_database_name': CURRENT_DB,
            'clickhouse_blue_database_name':  BASELINE_DB,
        }
        with patch('validate_blue_green_study.parse_properties', return_value=props), \
             patch('validate_blue_green_study.get_client', return_value=MagicMock()), \
             patch('validate_blue_green_study.get_internal_id',
                   side_effect=[current_id, baseline_id]), \
             patch('validate_blue_green_study.run_checks',
                   return_value=check_results or []), \
             patch('sys.stdout', StringIO()), \
             patch('sys.argv', ['validate_blue_green_study.py',
                                '--properties-file', 'dummy.properties',
                                '--study-id', STUDY_ID,
                                '--current-color', color]):
            with self.assertRaises(SystemExit) as ctx:
                v.main()
        return ctx.exception.code

    def test_all_checks_pass_exits_0(self):
        self.assertEqual(self._run_main(CURRENT_ID, BASELINE_ID, []), 0)

    def test_check_failure_exits_1(self):
        self.assertEqual(
            self._run_main(CURRENT_ID, BASELINE_ID,
                           ['patient count dropped from 1000 to 100']),
            1
        )

    def test_new_study_not_in_baseline_exits_0(self):
        # Study is in current DB but absent from baseline → new study, skip validation
        self.assertEqual(self._run_main(CURRENT_ID, baseline_id=None), 0)

    def test_study_missing_from_current_exits_1(self):
        self.assertEqual(self._run_main(current_id=None, baseline_id=BASELINE_ID), 1)


if __name__ == '__main__':
    unittest.main()
