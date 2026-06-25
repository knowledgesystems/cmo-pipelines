"""
config_manager.py

ConfigManager resolves non-secret blue/green deployment configuration that lives in an
on-disk YAML file, and provides the Kubernetes control-plane API used to read
deployment/ingress state and to swap the production color.

Tasks pass only the *path* of the color-swap config file (e.g. ``color_swap_config_file``);
its parsed contents (deployment lists, ingress maps, swap settings, and the kubeconfig
path under ``cluster_cfg``) are returned as a plain dict. Heavyweight imports happen
lazily inside methods so importing this module on the Airflow scheduler stays cheap.
"""
from __future__ import annotations

import logging
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:  # pragma: no cover - typing only, avoids importing heavy deps at parse time
    from kubernetes.client import CoreV1Api

logger = logging.getLogger(__name__)


class ConfigManager:
    """Reads the on-disk color-swap config and provides the control-plane API."""

    @classmethod
    def color_swap_config(cls, color_swap_config_file: str) -> dict[str, Any]:
        """
        Reads the color-swap YAML file and returns the parsed blue/green config.

        The file holds the contents of today's ``*-db-color-swap-config.yaml``:
        deployment lists, ingress maps, host->service maps, swap settings, and the
        ``cluster_cfg`` kubeconfig path used by :meth:`core_v1_api`.
        """
        import yaml

        with open(color_swap_config_file) as f:
            return yaml.safe_load(f)

    @classmethod
    def core_v1_api(cls, kubeconfig_path: str | None = None) -> "CoreV1Api":
        """
        Returns a CoreV1Api.

        Loads from ``kubeconfig_path`` (e.g. the ``cluster_cfg`` value from the
        color-swap config) when given; otherwise tries in-cluster config and finally
        the default local kubeconfig.
        """
        from kubernetes import client, config

        if kubeconfig_path:
            config.load_kube_config(config_file=kubeconfig_path)
        else:
            try:
                config.load_incluster_config()
            except config.ConfigException:
                config.load_kube_config()
        return client.CoreV1Api()


__all__ = ["ConfigManager"]
