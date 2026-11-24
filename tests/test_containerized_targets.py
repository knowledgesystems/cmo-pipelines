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

    def test_targets_only_schedule_their_own_worker_tasks(self):
        public = self.dag.get_task("select_public_workers")
        preview = self.dag.get_task("select_hgnc_workers")
        public_tasks = public.get_flat_relative_ids(upstream=False)
        preview_tasks = preview.get_flat_relative_ids(upstream=False)
        self.assertFalse(public_tasks & preview_tasks)
        self.assertEqual(public_tasks | preview_tasks | {public.task_id, preview.task_id},
                         set(self.dag.task_ids))
        self.assertIn("transfer_deployment_color", public_tasks)
        self.assertIn("transfer", preview_tasks)
        for target in ("public", "containerized", "public_v7"):
            with self.subTest(target=target):
                params = dict(self.dag.params, target=target)
                context = dict(params=params, dag_run=SimpleNamespace(conf={}))
                self.assertEqual(public.python_callable(False, **context), target != "public_v7")
                self.assertEqual(preview.python_callable(True, **context), target == "public_v7")

    def test_both_targets_require_validated_inputs_before_cloning(self):
        self.assertIn("collect_valid_studies",
                      self.dag.get_task("clone_live_database_into_standby").upstream_task_ids)
        self.assertIn("validate", self.dag.get_task("verify_portal").upstream_task_ids)
        self.assertIn("verify_portal", self.dag.get_task("clone").upstream_task_ids)
