"""Task implementations for the fixed-target public v7 importer."""

import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from contextlib import contextmanager
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from dags.public_rollout import sha256, study_input
from dags.study_sources import HGNC_BUCKET, verified_study_directory
from public_v7_common import (
    ClickHouse, DATABASES, PREVIEW_HOST, active_color, load_properties,
    study_ids, validate_application, validate_management,
)

CREDS = Path("/data/portal-cron/pipelines-credentials")
CONFIG = Path("/v7-config/environment.json")
SCRIPTS = Path("/data/portal-cron/scripts")
MANAGE = CREDS / "public_v7.manage.properties"
INPUT = Path("/mnt/hgnc-studies")
REPORTS = Path("/opt/airflow/logs/public_v7")


def settings():
    if not CONFIG.is_file() or not MANAGE.is_file():
        raise ValueError("HGNC imports require public-v7-config and public-v7-import-credentials in Airflow")
    cfg = json.loads(CONFIG.read_text())
    if cfg["portal"] != "public_v7" or cfg["databases"] != DATABASES or cfg["preview_host"] != PREVIEW_HOST:
        raise ValueError("Unexpected environment mapping")
    if cfg.get("study_bucket") != HGNC_BUCKET:
        raise ValueError(f"Expected HGNC study source {HGNC_BUCKET}")
    props = load_properties(MANAGE)
    validate_management(props)
    for color in DATABASES:
        application = load_properties(CREDS / ("public_v7.application.properties." + color))
        validate_application(application, color)
        if application["spring.datasource.username"] != props["clickhouse_server_username"] or application["spring.datasource.password"] != props["clickhouse_server_password"]:
            raise ValueError("Application and management credentials do not match")
    if hashlib.sha256((CREDS / "public_v7.clickhouse.sql").read_bytes()).hexdigest() != cfg["derived_sql_sha256"]:
        raise ValueError("Derived SQL differs from the reviewed artifact")
    return cfg, props, ClickHouse(props)


def run(command, log=None, env=None):
    # Arguments never contain credentials. Stream all subprocess output to the
    # Airflow log and, when requested, a durable file on the log PVC.
    output = open(log, "w") if log else None
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=env)
        for line in process.stdout:
            print(line, end="", flush=True)
            if output:
                output.write(line)
        code = process.wait()
        if code:
            raise RuntimeError("Command failed with exit " + str(code) + ": " + str(command[0]))
    finally:
        if output:
            output.close()


def support(script, *args):
    # Source the image's own environment so tools resolve identically to the
    # established importer. Positional parameters avoid shell interpolation.
    run(["bash", "-c", 'source "$1/automation-environment.sh"; shift; exec "$@"',
         "v7", str(SCRIPTS), str(SCRIPTS / script), *map(str, args)])


def fingerprint(path):
    if path.is_symlink():
        raise ValueError("Study inputs must not contain symlinks")
    if path.is_file():
        return sha256(path)
    digest = hashlib.sha256()
    entries = list(path.rglob("*"))
    if any(p.is_symlink() for p in entries):
        raise ValueError("Study inputs must not contain symlinks")
    files = sorted(p for p in entries if p.is_file())
    if not files:
        raise ValueError("Empty study input")
    for file in files:
        digest.update(str(file.relative_to(path)).encode() + b"\0")
        with file.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(block)
    return digest.hexdigest()


def study_path(study):
    root = INPUT
    choices = [p for p in [root / study, root / (study + ".tar.gz")] if p.exists()]
    if len(choices) != 1 or choices[0].is_symlink() or not choices[0].resolve().is_relative_to(root.resolve()):
        raise ValueError("Missing, ambiguous, or escaping study input: " + study)
    return choices[0]


@contextmanager
def selected_input(plan, study):
    path = study_path(study)
    expected = plan["studies"][study]
    if path.is_file():
        entry = {"study_id": study, "key": path.name, "sha256": expected}
        with study_input(INPUT, entry) as directory:
            yield directory
    else:
        if fingerprint(path) != expected:
            raise ValueError("Study input changed after selection: " + study)
        yield Path(verified_study_directory(path, study))


def references(client, database):
    if database not in DATABASES.values():
        raise ValueError("Invalid reference source")
    queries = {
        "genes": "SELECT entrez_gene_id AS entrezGeneId,hugo_gene_symbol AS hugoGeneSymbol FROM {db}.gene ORDER BY entrez_gene_id,hugo_gene_symbol",
        "genesaliases": "SELECT entrez_gene_id AS entrezGeneId,gene_alias AS alias FROM {db}.gene_alias ORDER BY entrez_gene_id,gene_alias",
        "cancer-types": "SELECT type_of_cancer_id AS cancerTypeId,name,dedicated_color AS dedicatedColor,short_name AS shortName,parent FROM {db}.type_of_cancer ORDER BY type_of_cancer_id",
        "genesets": "SELECT external_id AS genesetId FROM {db}.geneset ORDER BY external_id",
        "gene-panels": "SELECT p.stable_id AS genePanelId,arraySort(x -> x['entrezGeneId'],groupArrayIf(map('entrezGeneId',l.gene_id),l.gene_id != 0)) AS genes FROM {db}.gene_panel p LEFT JOIN {db}.gene_panel_list l ON p.internal_id=l.internal_id GROUP BY p.stable_id ORDER BY p.stable_id",
    }
    result = {key: client.rows(sql.format(db=database)) for key, sql in queries.items()}
    info = client.rows("SELECT db_schema_version,geneset_version,gene_table_version FROM " + database + ".info")
    if len(info) != 1 or info[0]["db_schema_version"] != "3.1.0" or not info[0]["gene_table_version"] or not result["genes"] or not result["cancer-types"]:
        raise ValueError("Reference data is not ready; load genes, cancer types, and one versioned info record")
    validate_cancer_types(result["cancer-types"])
    result["info"] = {"portalVersion": "7.2.3", "dbSchemaVersion": info[0]["db_schema_version"]}
    result["genesets_version"] = info[0]["geneset_version"] or ""
    result["reference_release"] = info[0]["gene_table_version"]
    return result


def validate_cancer_types(rows):
    # The portal stops at 'tissue', even when the seed omits a row for it.
    # Reject malformed input here instead of discovering it after promotion.
    parents = {row["cancerTypeId"]: row["parent"] for row in rows}
    if not parents or len(parents) != len(rows):
        raise ValueError("Expected a nonempty cancer type hierarchy with unique IDs")
    for start in parents:
        current = start
        visited = set()
        while current != "tissue":
            if current in visited or current not in parents:
                raise ValueError("Cancer type hierarchy has a missing parent or cycle")
            visited.add(current)
            current = parents[current]


def reference_hash(data):
    return hashlib.sha256(json.dumps(data, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def reference_files(data, destination):
    for key, value in data.items():
        if key != "reference_release":
            (destination / (key + ".json")).write_text(json.dumps(value))


def check_plan(plan, state="complete"):
    cfg, props, client = settings()
    color = active_color(client.management(), state)
    if color != plan["source_color"]:
        raise ValueError("Active color changed since preflight")
    refs = references(client, DATABASES[color])
    if reference_hash(refs) != plan["reference_sha256"]:
        raise ValueError("Reference data changed since preflight")
    return cfg, props, client, refs


def prepare(selected_studies, run_id, core_image, cmo_image):
    cfg, props, client = settings()
    if cfg.get("promotion_enabled") is not True:
        raise ValueError("Enable promotion only after both v7 portal colors and Argo routing are ready")
    for name, image in (("core_image", core_image), ("cmo_image", cmo_image)):
        if not re.search(r"@sha256:[0-9a-f]{64}$", image) or cfg.get(name) != image:
            raise ValueError("Pin " + name + " to the same digest in the DAG and ConfigMap")
    if not client.rows("SELECT hostName() AS host FROM clusterAllReplicas('default','system','one')"):
        raise ValueError("The importer cannot verify replica access")
    color = active_color(client.management())
    client.rows("SELECT sum(rows) AS part_rows FROM clusterAllReplicas('default','system','parts') WHERE database='" + DATABASES[color] + "' AND table='info'")
    refs = references(client, DATABASES[color])
    if not cfg.get("reference_release") or cfg["reference_release"] != refs["reference_release"]:
        raise ValueError("The configured reference release is not loaded")
    selected = study_ids(selected_studies)
    key = hashlib.sha256(run_id.encode()).hexdigest()[:24]
    report_dir = REPORTS / key
    report_dir.mkdir(parents=True, exist_ok=True)
    plan = {
        "source_color": color,
        "target_color": "blue" if color == "green" else "green",
        "reference_sha256": reference_hash(refs),
        "reference_release": refs["reference_release"],
        "studies": {study: fingerprint(study_path(study)) for study in selected},
        "report_dir": str(report_dir),
        "core_image": cfg["core_image"],
        "cmo_image": cfg["cmo_image"],
    }
    (report_dir / "plan.json").write_text(json.dumps(plan, indent=2))
    return plan


@contextmanager
def seeded_references(plan, importing):
    _, _, client, refs = check_plan(plan, "running" if importing else "complete")
    if importing:
        target_refs = references(client, DATABASES[plan["target_color"]])
        if reference_hash(target_refs) != plan["reference_sha256"]:
            raise ValueError("Clone did not preserve reference data")
    with tempfile.TemporaryDirectory(prefix="hgnc-refs-") as tmp:
        directory = Path(tmp)
        reference_files(refs, directory)
        yield directory


def begin(plan):
    check_plan(plan)
    if (SCRIPTS / "automation-environment.sh").is_file():
        support("set_update_process_state.sh", MANAGE, "running")
    else:
        # Core workers run import and derive when the clone stage was skipped.
        with tempfile.TemporaryDirectory(prefix="hgnc-management-") as tmp:
            subprocess.run(["/scripts/clickhouse_import_support/set_update_process_state.sh",
                            str(MANAGE), "running"], cwd=tmp, check=True)


def clone(plan):
    check_plan(plan, "running")
    support("drop_tables_in_clickhouse_database.sh", MANAGE, plan["target_color"])
    support("clone_clickhouse_database.sh", MANAGE, plan["source_color"], plan["target_color"])


def activate(props, color, directory):
    shutil.copyfile(CREDS / ("public_v7.application.properties." + color), directory / "application.properties")
    shutil.copyfile(CREDS / "public_v7.clickhouse.sql", directory / "clickhouse.sql")
    env = dict(os.environ)
    env.update({
        "PORTAL_HOME": str(directory), "CLICKHOUSE_HOST": props["clickhouse_server_host_name"],
        "CLICKHOUSE_NATIVE_PORT": props["clickhouse_server_port"], "CLICKHOUSE_USER": props["clickhouse_server_username"],
        "CLICKHOUSE_PASSWORD": props["clickhouse_server_password"], "CLICKHOUSE_DB": DATABASES[color],
    })
    return env


def authenticate_portal():
    # IRSA belongs to the Airflow account, not the public portal account.
    os.environ.pop("AWS_ROLE_ARN", None)
    os.environ.pop("AWS_WEB_IDENTITY_TOKEN_FILE", None)
    support("authenticate_service_account.sh", "public")
    os.environ["KUBECONFIG"] = str(CREDS / "publicargocd-cluster-kubeconfig")


def kubectl(*args):
    result = subprocess.run(["kubectl", "--request-timeout=60s", "-n", "default", *args],
                            check=True, text=True, capture_output=True)
    return result.stdout


def route():
    ingress = json.loads(kubectl("get", "ingress", "public-v7-ingress", "-o", "json"))
    rules = ingress["spec"]["rules"]
    if len(rules) != 1 or rules[0]["host"] != PREVIEW_HOST or len(rules[0]["http"]["paths"]) != 1:
        raise ValueError("Unexpected preview ingress shape or host")
    service = rules[0]["http"]["paths"][0]["backend"]["service"]["name"]
    if service not in {"cbioportal-backend-public-v7-" + c for c in DATABASES}:
        raise ValueError("Preview ingress points outside v7")
    return service


def verify_portal(plan):
    check_plan(plan)
    authenticate_portal()
    if route() != "cbioportal-backend-public-v7-" + plan["source_color"]:
        raise ValueError("Preview ingress and management color disagree")
    for color in DATABASES:
        name = "cbioportal-backend-public-v7-" + color
        kubectl("get", "deployment/" + name, "service/" + name)


def transfer(plan):
    check_plan(plan, "running")
    authenticate_portal()
    source = "cbioportal-backend-public-v7-" + plan["source_color"]
    target = "cbioportal-backend-public-v7-" + plan["target_color"]
    if route() != source:
        raise ValueError("Preview ingress changed during the import")
    kubectl("rollout", "restart", "deployment/" + target)
    kubectl("rollout", "status", "deployment/" + target, "--timeout=600s")
    # Both color deployments stay running. A failed finalization leaves the
    # old color available for a deliberate rollback. Argo preserves this
    # ingress field and the v7 restart timestamps, so no Git push is needed.
    path = "/spec/rules/0/http/paths/0/backend/service/name"
    patch = [{"op": "test", "path": path, "value": source},
             {"op": "replace", "path": path, "value": target}]
    kubectl("patch", "ingress", "public-v7-ingress", "--type=json", "-p", json.dumps(patch))
    if route() != target:
        raise RuntimeError("Preview route verification failed after switch")


def complete(plan):
    check_plan(plan, "running")
    support("set_update_process_state.sh", MANAGE, "complete")
    authenticate_portal()
    if route() != "cbioportal-backend-public-v7-" + plan["target_color"]:
        raise ValueError("Preview ingress no longer routes to the promoted target")
    _, _, client = settings()
    if active_color(client.management()) != plan["target_color"]:
        raise ValueError("Management finalization did not match the promoted target")


def failure_cleanup(plan, began, transfer_started):
    if not began:
        return
    if transfer_started:
        raise RuntimeError("Promotion may have changed routing. Leave management state untouched and follow recovery runbook.")
    abandon(plan)


def abandon(plan):
    check_plan(plan, "running")
    support("set_update_process_state.sh", MANAGE, "abandoned")
