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
                    self.assertEqual(claims, {"cdsi-curation-hgnc-s3-pvc"} if hgnc else
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

    def test_hgnc_stages_use_the_selected_plan_and_promote_only_after_transfer(self):
        from unittest.mock import Mock, call
        ti = Mock()
        plan = {"studies": {"a": "digest"}}
        ti.xcom_pull.return_value = plan
        params = dict(self.dag.params, target="public_v7", skip_tasks=[])
        run = SimpleNamespace(run_id="manual__test", conf=params)
        bodies = {t.task_id: t.python_callable for t in self.dag.tasks}
        with patch.dict(bodies["verify_studies_exist"].__globals__, hgnc=Mock()) as globals_:
            runtime = globals_["hgnc"]
            runtime.prepare.return_value = plan
            runtime.validate_study.return_value = "a"
            self.assertEqual(bodies["verify_studies_exist"](["a"], params, ti, run), ["a"])
            ti.xcom_push.assert_called_with(key="hgnc_plan", value=plan)
            bodies["verify_cluster_state"](params=params, ti=ti)
            valid = bodies["pull_and_validate_study"]("a", params, ti)
            bodies["collect_valid_studies"]([valid], params, ti)
            ti.xcom_push.assert_called_with(key="hgnc_began", value=True)
            bodies["clone_live_database_into_standby"](params=params, ti=ti)
            bodies["import_into_standby_database"]([valid], params, ti)
            bodies["create_derived_tables_in_standby_database"]([valid], params, ti)
            bodies["transfer_deployment_color"](params=params, ti=ti)
            ti.get_dagrun.return_value.get_task_instance.return_value.state = "success"
            bodies["finalize_import_state"](params=params, ti=ti)
            self.assertEqual(runtime.mock_calls[1:], [call.verify_portal(plan), call.validate_study("a", plan),
                             call.begin(plan), call.clone(plan), call.import_studies(plan),
                             call.derive(plan, publish=True), call.transfer(plan), call.complete(plan)])
            ti.get_dagrun.return_value.get_task_instance.return_value.state = "skipped"
            bodies["finalize_import_state"](params=dict(params, skip_tasks=["transfer_deployment_color"]), ti=ti)
            runtime.abandon.assert_called_once_with(plan)
