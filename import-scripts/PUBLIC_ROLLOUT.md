# Pinned public rollout inputs

The public path in `dags/import_public_hackathon.py` requires a hash-pinned
selection manifest. The legacy containerized path is unchanged when no manifest
is supplied. This branch is source preparation only: pushing it does not update
the Airflow submodule, Kubernetes secrets, images, or production traffic.

## Prerequisites

Use cbioportal-core candidate `fa9f69f36a6c37618a79a11dad42af96966d9e65`
or a deliberately revalidated successor. Use the Python 3 case-list generator
from cmo-pipelines `cd98aef313566fc6e37acd5ff940056141995865`, based on PR1394.
Its `case_list_config.tsv` is byte-identical to that core candidate's rules.
The preprocessing wrapper's tool lock also pins these revisions. Do not use
the old EC2 case-list configuration for newly prepared studies.

Case generation is gap-fill only. Virtual `_all` and existing stable IDs under
custom filenames count as existing lists. Study-local nonempty curated lists
with the expected non-generic category also count; unrelated occupied filenames
raise an explicit conflict. No annotations, curated memberships,
or existing physical lists are deleted or rewritten to force acceptance.
The importer's mandatory primary `_all`, `_sequenced` and `_cna` stable IDs are
not replaced by category equivalence. The category fallback applies to additional
generated roles such as `_cnaseq`, not those primary profiled-sample requirements.

## Local preparation and validation

Run `public_rollout_batch.py --help` and each subcommand's `--help` for arguments.
The following commands run from the cmo-pipelines checkout. The `/work` paths
are examples; every output directory must be new.

```bash
python3 import-scripts/public_rollout_batch.py capture-references \
  --seed-references /work/original-batch/references \
  --clickhouse-config /path/to/read-only-public-client.yaml \
  --importer /work/cbioportal-core/scripts/importer \
  --output /work/public-references

python3 import-scripts/public_rollout_batch.py prepare-case-lists \
  --batch /work/original-batch \
  --references /work/public-references \
  --generator /work/cmo-case-list-rule-parity/import-scripts/generate_case_lists.py \
  --importer /work/cbioportal-core/scripts/importer \
  --output /work/candidate-batch

python3 import-scripts/public_rollout_batch.py validate \
  --manifest /work/candidate-batch/candidates.json \
  --root /work/candidate-batch \
  --importer /work/cbioportal-core/scripts/importer \
  --python /path/to/core-requirements-venv/bin/python \
  --workers 3 --output /work/candidate-batch/validation

python3 import-scripts/public_rollout_batch.py select \
  --manifest /work/candidate-batch/validation/validation-manifest.json \
  --count 50 --seed public-rollout-sample-v1 \
  --manifest-key staging/_batches/candidate-batch/selection.json \
  --output /work/candidate-batch/selection.json
```

Reference capture reads only public reference tables and `/api/info`. It
confirms the gene and alias exports equal the original preprocessing snapshot,
checks API/database reference versions, and checks management state before and
after capture. This is not a transactional database snapshot; keep automated
imports paused and do not modify reference tables during capture.

The bundle includes info, cancer types, genes, aliases, gene sets and their
version, gene panels with member genes, pinned OncoTree, and case-list rules.
Missing/null/hash-mismatched references fail; they cannot silently turn off
checks. OncoTree stays read-only. Credentials never enter the bundle.

Case-list rechecking extracts hash-verified archives to temporary copies. If
no lists are added, the exact original compressed bytes are retained. Otherwise
the replacement archive and original hash are recorded. Existing study files
must remain byte-identical. This step does not rerun the other preprocessing
transformations and cannot turn a previously held study into an eligible one.

Validation uses `validateData.py -p REFERENCES --oncotree-file SNAPSHOT -v`.
Every candidate gets a status, exit code, rejection reason, and text/HTML logs.
Exit 0/3 passes; exit 1 rejects the study; other exits or input/reference failures
are infrastructure errors that prevent sample selection. Warnings remain
permitted, matching `metaImport.py -o`.

Selection hashes `seed + NUL + study_id` and ranks the passing IDs, giving a
stable randomized order independent of input order/Python version. It refuses
to shrink a requested sample or sample around infrastructure errors. A short
passing inventory requires more prepared and validated candidates, not relaxed
validation. Keep the complete rejected/held inventory for the later exclusion
review. These sample results do not validate the complete Datahub collection.

## Publishing and deployment boundary

None of these commands uploads, deploys, triggers an import, or switches traffic.
Publish the reference bundle and selection manifest under their exact recorded
S3 keys, and the selected study archives under `staging/<id>.tar.gz`. The
reference bundle uses a `.bundle` suffix (gzip tar content) so study discovery
cannot mistake it for a study archive. Preserve existing archives; unchanged
hashes need no upload, and changed archives need explicit versioned publication.
Record S3 verification and keep validation logs with the batch provenance.

The generated `.params.json` sets `database=public`, `study_prefix=staging`,
the exact selected IDs, manifest key/hash, and
`skip_tasks=["transfer_deployment_color"]`. This is a no-swap sample configuration,
not authorization to execute it. Public runs require the no-swap setting until
the separate cutover step explicitly changes that guard. Standby activation
rejects unknown management output, wrong public database/host settings and
missing/unlimited JDBC socket timeouts before copying properties or importing.
Management lookup is bounded to 120 seconds, subprocesses to six hours and tasks
to 24 hours. Public JDBC socket timeouts are 600,000 ms in both colors.

Build/pin the candidate core image before deployment. Validation/import workers
verify all top-level importer `.py`/`.tsv` hashes against the manifest and check
the case-list rules against the references. A different validator requires
revalidation/reselection. Build tooling must not silently modify those files.

Both validation and import copy each archive locally, verify the hash, then
extract that same copy. Unsafe tar members, links, duplicate entries, changed
archives, mismatched study IDs, and selection changes fail. A selected study
that subsequently fails validation or import fails the run; it is not filtered
out or replaced. Import revalidation uses the same reference bundle.

Deploy only after checking the effective task commands and mounted public
configuration. The existing management-state/no-swap behavior is preserved;
this change does not independently establish production safety.

## Tests

```bash
python3 -m unittest discover -s tests -p 'test_public_rollout*.py' -v
python3 -m py_compile dags/import_public_hackathon.py dags/public_rollout.py \
  import-scripts/public_rollout_batch.py
```

Task-body tests execute the real task functions with mocked infrastructure.
They do not substitute for parsing/rendering in the deployment's Airflow version
or a no-swap standby import using the built image.
