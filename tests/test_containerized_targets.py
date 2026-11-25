"""Verify the scheduler graph with the Airflow version used in the worker image."""
import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch


@unittest.skipUnless(importlib.util.find_spec("airflow"), "requires the Airflow worker image")
class ContainerizedTargetsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        from airflow.models.dagbag import DagBag
        from airflow.serialization.serialized_objects import SerializedDAG

        path = Path(__file__).resolve().parents[1] / "dags" / "import_containerized_dag.py"
        with patch("airflow.models.Variable.get", return_value="[]"):
            bag = DagBag(str(path), include_examples=False)
        if bag.import_errors:
            raise RuntimeError(bag.import_errors)
        cls.dag = bag.dags["import_containerized_dag"]
        SerializedDAG.to_dict(cls.dag)

    def test_trigger_parameters(self):
        self.assertEqual(self.dag.params["target"], "public")
        self.assertEqual(set(self.dag.params), {"target", "cancer_study_ids", "buckets", "skip_tasks"})

    def test_one_workflow_for_every_target(self):
        backbone = ["verify_studies_exist", "verify_cluster_state", "pull_and_validate_study",
                    "collect_valid_studies", "clone_live_database_into_standby", "import_into_standby_database",
                    "create_derived_tables_in_standby_database", "transfer_deployment_color", "finalize_import_state"]
        self.assertEqual(set(self.dag.task_ids), set(backbone) | {"report_failure"})
        for parent, child in zip(backbone, backbone[1:]):
            self.assertIn(parent, self.dag.get_task(child).upstream_task_ids)
        self.assertEqual(self.dag.get_task("report_failure").upstream_task_ids, set(backbone))

    def test_worker_mounts_are_selected_for_each_target(self):
        import json
        from airflow.providers.cncf.kubernetes.pod_generator import PodGenerator
        from copy import deepcopy
        for task in self.dag.tasks:
            for target in ("containerized", "public", "public_v7"):
                with self.subTest(task=task.task_id, target=target):
                    pod = deepcopy(task.executor_config["pod_override"])
                    self.assertFalse(pod.spec.volumes)
                    specs = json.loads(pod.metadata.annotations["cbioportal.org/import-worker-profiles"])
                    profile = specs["public_v7" if target == "public_v7" else "public"]
                    pod = PodGenerator.reconcile_pods(pod, PodGenerator.deserialize_model_dict(profile))
                    secret_names = {v.secret.secret_name for v in pod.spec.volumes if v.secret}
                    hgnc = target == "public_v7"
                    self.assertEqual(secret_names, {"public-v7-import-credentials" if hgnc else "pipelines-credentials"})
                    claims = {v.persistent_volume_claim.claim_name for v in pod.spec.volumes if v.persistent_volume_claim}
                    self.assertEqual(claims, {"cdsi-curation-unpublished-s3-pvc"} if hgnc else
                                     {"testing-studies-s3-pvc", "cdsi-curation-unpublished-s3-pvc", "databricks-s3-pvc"})

    def test_validation_only_hgnc_run(self):
        from airflow.exceptions import AirflowSkipException
        from dags.containerized_workflow import SKIPPABLE_TASK_IDS
        from unittest.mock import Mock
        params = dict(self.dag.params, target="public_v7", skip_tasks=list(SKIPPABLE_TASK_IDS))
        plan = {"studies": {"study": "hash"}}
        ti = Mock()
        ti.xcom_pull.return_value = plan
        with patch("dags.public_v7_runtime.begin") as begin:
            valid = self.dag.get_task("collect_valid_studies").python_callable(["study"], params=params, ti=ti)
            self.assertEqual(valid, ["study"])
            begin.assert_not_called()
            for stage in SKIPPABLE_TASK_IDS:
                args = [valid] if stage in ("import_into_standby_database", "create_derived_tables_in_standby_database") else []
                with self.assertRaises(AirflowSkipException):
                    self.dag.get_task(stage).python_callable(*args, params=params, ti=ti)
            ti.get_dagrun.return_value.get_task_instance.return_value.state = "skipped"
            with patch("dags.public_v7_runtime.abandon") as abandon:
                self.dag.get_task("finalize_import_state").python_callable(params=params, ti=ti)
                abandon.assert_not_called()

    def test_first_enabled_write_stage_acquires_management(self):
        from dags.containerized_workflow import SKIPPABLE_TASK_IDS
        from unittest.mock import Mock
        task = self.dag.get_task("collect_valid_studies").python_callable
        start = task.__globals__["_start_write_stage"]
        plan = {"studies": {"a": "hash"}}
        for index, first in enumerate(SKIPPABLE_TASK_IDS):
            with self.subTest(first=first), patch("dags.public_v7_runtime.begin") as begin:
                params = dict(self.dag.params, target="public_v7", skip_tasks=list(SKIPPABLE_TASK_IDS[:index]))
                ti = Mock()
                ti.xcom_pull.return_value = plan
                self.assertEqual(task(["a"], params, ti), ["a"])
                begin.assert_not_called()
                for stage in SKIPPABLE_TASK_IDS[index:]:
                    start(stage, params, ti)
                begin.assert_called_once_with(plan)
                ti.xcom_push.assert_called_once_with(key="hgnc_began", value=True)

    def test_bucket_defaults_and_overrides(self):
        from dags.study_sources import DEFAULT_BUCKET, HGNC_BUCKET, target_buckets
        self.assertEqual(HGNC_BUCKET, "cdsi-curation-unpublished")
        self.assertEqual(self.dag.params["buckets"], [])
        # Airflow renders the multiselect as required unless the schema accepts null.
        for key in ("buckets", "skip_tasks"):
            self.assertIn("null", self.dag.params.get_param(key).schema["type"])
        self.assertEqual(target_buckets("public_v7", None), [HGNC_BUCKET])
        self.assertEqual(target_buckets("public_v7", []), [HGNC_BUCKET])
        self.assertEqual(target_buckets("public_v7", [HGNC_BUCKET]), [HGNC_BUCKET])
        for target in ("public", "containerized"):
            self.assertEqual(target_buckets(target, []), [DEFAULT_BUCKET])
            self.assertEqual(target_buckets(target, ["cdsi-testing"]), ["cdsi-testing"])
            self.assertEqual(target_buckets(target, [HGNC_BUCKET]), [HGNC_BUCKET])
        with self.assertRaises(ValueError):
            target_buckets("public_v7", ["cdsi-testing"])

    def test_shared_importer_commands_use_hgnc_inputs_and_standby(self):
        from contextlib import nullcontext
        from tempfile import TemporaryDirectory
        from unittest.mock import Mock
        params = dict(self.dag.params, target="public_v7", skip_tasks=[])
        ti = Mock()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            env = {"PORTAL_HOME": tmp}
            ti.xcom_pull.return_value = {"studies": {"a": "hash"}, "report_dir": tmp}
            bodies = {t.task_id: t.python_callable for t in self.dag.tasks}
            run = Mock(return_value=SimpleNamespace(returncode=0))
            with patch.dict(bodies["verify_studies_exist"].__globals__, _run_and_stream=run,
                            _pinned_inputs=lambda *a, **kw: nullcontext((root, lambda sid: nullcontext(root / sid), root)),
                            _standby_environment=lambda *a: nullcontext((env, root))):
                valid = bodies["pull_and_validate_study"]("a", params, ti)
                bodies["import_into_standby_database"]([valid], params, ti)
                bodies["create_derived_tables_in_standby_database"]([valid], params, ti)
                calls = run.call_args_list
                self.assertEqual([Path(c.args[0][1]).name for c in calls],
                                 ["validateData.py", "metaImport.py", "metaImport.py", "cbioportalImporter.py"])
                self.assertEqual(calls[1].args[0][2:], ["-s", str(root / "a"), "-p", tmp, "-o", "--no-derive-tables"])
                self.assertEqual(calls[2].args[0][2:], ["derive-tables", "--derived-table-sql", str(root / "clickhouse.sql")])
                self.assertEqual(calls[3].args[0][2:], ["make-studies-available", "-ids", "a"])
                for c in calls[1:]:
                    self.assertEqual(c.kwargs["env"], env)

    def test_hgnc_failure_cleanup_checks_which_task_acquired_management(self):
        from unittest.mock import Mock
        from airflow.exceptions import AirflowException
        report = self.dag.get_task("report_failure").python_callable
        ti = Mock()
        run = ti.get_dagrun.return_value
        run.get_task_instances.return_value = [SimpleNamespace(state="failed")]
        run.get_task_instance.return_value.state = "upstream_failed"
        plan = {"studies": {"a": "hash"}}
        ti.xcom_pull.side_effect = lambda task_ids, key: plan if key == "hgnc_plan" else task_ids == "import_into_standby_database"
        params = dict(self.dag.params, target="public_v7", skip_tasks=["clone_live_database_into_standby"])
        with patch("dags.public_v7_runtime.failure_cleanup") as cleanup, self.assertRaises(AirflowException):
            report(ti=ti, params=params)
        cleanup.assert_called_once_with(plan, began=True, transfer_started=False)
