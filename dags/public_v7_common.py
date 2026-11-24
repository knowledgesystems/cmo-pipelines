"""Target validation and ClickHouse transport shared by prep tools and the DAG."""

import base64
import json
import re
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

PORTAL = "public_v7"
DATABASES = {color: "cbioportal_public_v7_" + color for color in ("blue", "green")}
MANAGEMENT = "publicdb_update_management_database"
HOST = "dl96orhu96.us-east-1.aws.clickhouse.cloud"
PREVIEW_HOST = "hgnc.cbioportal.aws.mskcc.org"


def load_properties(path):
    result = {}
    for line in Path(path).read_text().splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or key.strip() in result:
            raise ValueError("Invalid or duplicate property: " + key.strip())
        result[key.strip()] = value.strip()
    return result


def validate_management(props, local=False):
    expected = {
        "portal_database_name": PORTAL,
        "clickhouse_update_management_database": MANAGEMENT,
        "clickhouse_blue_database_name": DATABASES["blue"],
        "clickhouse_green_database_name": DATABASES["green"],
    }
    if not local:
        expected["clickhouse_server_host_name"] = HOST
        expected["clickhouse_server_port"] = "9440"
    for key, value in expected.items():
        if props.get(key) != value:
            raise ValueError("Unexpected target for " + key)
    for key in ("clickhouse_server_username", "clickhouse_server_password"):
        if not props.get(key) or "${" in props[key] or props[key] == "[REDACTED]":
            raise ValueError("Unresolved credential: " + key)


def validate_application(props, color, local=False):
    if color not in DATABASES:
        raise ValueError("Unknown database color")
    jdbc = props.get("spring.datasource.url", "")
    if not jdbc.startswith("jdbc:clickhouse://"):
        raise ValueError("Expected ClickHouse JDBC URL")
    parsed = urllib.parse.urlparse(jdbc[len("jdbc:"):])
    if parsed.path != "/" + DATABASES[color]:
        raise ValueError("Application properties target the wrong database")
    if not local and (parsed.hostname != HOST or parsed.port != 8443):
        raise ValueError("Application properties target the wrong service")
    query = urllib.parse.parse_qs(parsed.query, keep_blank_values=True)
    if parsed.username or parsed.password or parsed.fragment or set(query) - {"ssl", "socket_timeout"}:
        raise ValueError("Unexpected JDBC connection override")
    if any(len(values) != 1 for values in query.values()) or (not local and query.get("ssl") != ["true"]):
        raise ValueError("Expected unambiguous TLS connection properties")
    for key in ("spring.datasource.username", "spring.datasource.password"):
        if not props.get(key) or props[key] == "[REDACTED]" or "${" in props[key]:
            raise ValueError("Unresolved application credential")


def active_color(rows, expected_state="complete"):
    if len(rows) != 1 or rows[0].get("portal_database") != PORTAL:
        raise ValueError("Expected exactly one public_v7 management entry")
    row = rows[0]
    if row.get("current_database_in_production") not in DATABASES:
        raise ValueError("Management entry has an invalid color")
    if row.get("update_process_status") != expected_state:
        raise ValueError("public_v7 is not in state " + expected_state)
    return row["current_database_in_production"]


def study_ids(values):
    if not isinstance(values, list) or not values:
        raise ValueError("Select at least one study")
    if any(not isinstance(x, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", x) for x in values):
        raise ValueError("Invalid study identifier")
    if len(set(values)) != len(values):
        raise ValueError("Duplicate study selection")
    return values


class ClickHouse:
    def __init__(self, props, local_url=None):
        validate_management(props, local=local_url is not None)
        if local_url:
            parsed = urllib.parse.urlparse(local_url)
            if parsed.scheme != "http" or parsed.hostname not in {"127.0.0.1", "localhost", "clickhouse"}:
                raise ValueError("Test transport must target local ClickHouse")
        self.url = local_url or "https://" + HOST + ":8443/"
        self.auth = base64.b64encode(
            (props["clickhouse_server_username"] + ":" + props["clickhouse_server_password"]).encode()
        ).decode()

    def execute(self, sql, database=None):
        if database is not None and database not in {*DATABASES.values(), MANAGEMENT}:
            raise ValueError("SQL database is outside the v7 environment")
        params = {"output_format_json_quote_64bit_integers": 0}
        if database:
            params["database"] = database
        url = self.url + "?" + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, data=sql.encode(), headers={"Authorization": "Basic " + self.auth})
        try:
            with urllib.request.urlopen(req, timeout=600) as response:
                return response.read().decode()
        except urllib.error.HTTPError as exc:
            # SQL in server exceptions may include secrets for CREATE USER.
            raise RuntimeError("ClickHouse request failed with HTTP " + str(exc.code)) from None

    def rows(self, sql):
        return json.loads(self.execute(sql + " FORMAT JSON"))["data"]

    def management(self):
        return self.rows("SELECT * FROM " + MANAGEMENT + ".update_status WHERE portal_database = 'public_v7'")
