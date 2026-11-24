# Build from the cmo-pipelines checkout that supplies the production scripts.
# Reuse the public worker runtime, including its native saml2aws adapter and
# pinned Core ClickHouse helpers. Airflow deploys the public DAG via git-sync.
FROM ghcr.io/cbioportal/containerized-importer-cmo@sha256:d67fe65687d66cb5ce4f7f2bebd96bc8bc144a82c02e47b40814a15814af9ad6
COPY --chown=airflow:0 \
    import-scripts/airflow-verify-management.sh \
    import-scripts/airflow-clone-db.sh \
    import-scripts/airflow-import-direct-to-clickhouse.sh \
    import-scripts/airflow-transfer-deployment.sh \
    import-scripts/verify-management-state.sh \
    import-scripts/transfer-deployment-color.sh \
    import-scripts/color-config-parsing-functions.sh \
    import-scripts/clear-persistence-cache-shell-functions.sh \
    import-scripts/clear_cbioportal_persistence_cache.sh \
    import-scripts/slack-message-functions.sh \
    import-scripts/set_extra_cbioportal_configmap_color.sh \
    import-scripts/scale_clickhouse_service.py \
    /data/portal-cron/scripts/
# The production wrappers invoke the Bash color lookup through sh. Debian sh
# is dash, so invoke the helper with bash in this image.
RUN sed -i 's/$(sh \$GET_DB_IN_PROD_SCRIPT_FILEPATH/$(bash \$GET_DB_IN_PROD_SCRIPT_FILEPATH/g' \
    /data/portal-cron/scripts/airflow-clone-db.sh \
    /data/portal-cron/scripts/airflow-import-direct-to-clickhouse.sh \
    /data/portal-cron/scripts/airflow-transfer-deployment.sh
