"""
secret_manager.py

SecretManager resolves credentials that live in an on-disk config (properties) file
into ready-to-use, authenticated clients.

Tasks pass only the *path* of a config file (e.g. ``clickhouse_config_file``); the
file's contents — host, port, credentials, service name, management DB name — never
travel through XCom or DAG params. All heavyweight imports (clickhouse, boto3) happen
lazily inside the methods so importing this module on the Airflow scheduler stays
cheap and dependency-free.
"""
from __future__ import annotations

import logging
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:  # pragma: no cover - typing only, avoids importing heavy deps at parse time
    from clickhouse_connect.driver.client import Client
    from airflow.providers.slack.hooks.slack_webhook import SlackWebhookHook

logger = logging.getLogger(__name__)

# Keys we expect to find inside the ClickHouse config (properties) file.
_CH_HOST_KEY = "host"
_CH_PORT_KEY = "port"
_CH_USER_KEY = "username"
_CH_PASSWORD_KEY = "password"
_CH_SERVICE_KEY = "service_name"
_CH_MANAGEMENT_DB_KEY = "management_db_name"


class SecretManager:
    """Reads credentials from an on-disk config file and returns authenticated clients."""

    SLACK_CONN_ID = "slack_default"
    # None => unsigned/public access, which is what the hackathon bucket uses.
    AWS_CONN_ID: str | None = None

    @classmethod
    def read_config_file(cls, config_file: str) -> dict[str, str]:
        """Parses a java-style ``key=value`` properties file into a dict of strings."""
        props: dict[str, str] = {}
        with open(config_file) as f:
            for raw in f:
                line = raw.strip()
                if not line or line.startswith("#") or line.startswith("!"):
                    continue
                for sep in ("=", ":"):
                    if sep in line:
                        key, value = line.split(sep, 1)
                        props[key.strip()] = value.strip()
                        break
        return props

    @classmethod
    def clickhouse_client(cls, clickhouse_config_file: str) -> "Client":
        """
        Builds a connected ClickHouse client from the named config file.

        The file is the single source of truth for all ClickHouse connection details:
        host, port, credentials, service name, and management DB name.
        """
        import clickhouse_connect

        props = cls.read_config_file(clickhouse_config_file)
        missing = [k for k in (_CH_HOST_KEY, _CH_USER_KEY, _CH_PASSWORD_KEY) if k not in props]
        if missing:
            raise ValueError(
                f"ClickHouse config file '{clickhouse_config_file}' is missing required keys: {missing}"
            )

        return clickhouse_connect.get_client(
            host=props[_CH_HOST_KEY],
            port=int(props.get(_CH_PORT_KEY, 8443)),
            username=props[_CH_USER_KEY],
            password=props[_CH_PASSWORD_KEY],
        )

    @classmethod
    def clickhouse_properties(cls, clickhouse_config_file: str) -> dict[str, str]:
        """
        Returns the raw ClickHouse properties from the config file without connecting.

        Useful for tasks that need the service name or management DB name (e.g. to know
        which database to wipe / clone / mark as running) alongside the client.
        """
        return cls.read_config_file(clickhouse_config_file)

    @classmethod
    def s3_client(cls) -> Any:
        """boto3 S3 client. Unsigned for the public hackathon bucket."""
        import boto3
        from botocore import UNSIGNED
        from botocore.config import Config

        if cls.AWS_CONN_ID is None:
            return boto3.client("s3", config=Config(signature_version=UNSIGNED))

        # Private buckets: resolve real AWS creds from the Airflow connection.
        from airflow.providers.amazon.aws.hooks.base_aws import AwsBaseHook

        hook = AwsBaseHook(aws_conn_id=cls.AWS_CONN_ID, client_type="s3")
        return hook.get_client_type("s3")

    @classmethod
    def slack_hook(cls) -> "SlackWebhookHook":
        """Slack webhook hook, used to post import-result notifications."""
        from airflow.providers.slack.hooks.slack_webhook import SlackWebhookHook

        return SlackWebhookHook(slack_webhook_conn_id=cls.SLACK_CONN_ID)


__all__ = ["SecretManager"]
