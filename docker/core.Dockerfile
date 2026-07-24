# Airflow image bundling the cbioportal-core importer JAR + its ClickHouse
# helper scripts, for the DAG tasks that run the JAR-based import.
#
# IMPORTANT — build context is the cbioportal-core repo, NOT cmo-pipelines. The
# COPYs below (pom.xml, src/, scripts/, requirements.txt) are cbioportal-core's
# layout; they do not exist at the cmo-pipelines root, so this file cannot build
# from here. Build it from a cbioportal-core checkout, e.g.:
#   docker build --platform linux/amd64 \
#     -f /path/to/cmo-pipelines/docker/core.Dockerfile \
#     -t cbioportal-core:dev /path/to/cbioportal-core
#
# -------- Stage 1: build the JAR --------
FROM maven:3-eclipse-temurin-21 AS jar_builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Stage 2: Airflow + cbioportal tools --------
# Pin the Python version explicitly so the base can't drift (matches
# docker/Dockerfile); the plain 2.10.5 tag currently resolves to python3.12.
FROM apache/airflow:2.10.5-python3.12

USER root

# clickhouse-client via the apt repo (matches docker/Dockerfile). The old
# `curl https://clickhouse.com/ | sh && clickhouse install` self-installer pulled
# a ~177 MB binary that decompressed to ~650 MB in-layer and exhausted the build
# VM's disk (see docker/TEST_RESULTS.md); it was also an unpinned pipe-to-shell.
# The apt client package is small and needs no decompression step.
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl \
      perl \
      ca-certificates \
      gnupg \
  && curl -fsSL 'https://packages.clickhouse.com/rpm/lts/repodata/repomd.xml.key' \
      | gpg --dearmor -o /usr/share/keyrings/clickhouse-keyring.gpg \
  && echo 'deb [signed-by=/usr/share/keyrings/clickhouse-keyring.gpg] https://packages.clickhouse.com/deb stable main' \
      > /etc/apt/sources.list.d/clickhouse.list \
  && apt-get update && apt-get install -y --no-install-recommends clickhouse-client \
  && rm -rf /var/lib/apt/lists/*

# Copy JDK from builder stage — avoids apt source issues on the Airflow base image
COPY --from=jar_builder /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

COPY --from=jar_builder /app/core-*.jar /
COPY scripts/ /scripts/
RUN chmod -R a+x /scripts/

ENV PORTAL_HOME=/

# Placeholders — both are bind-mounted at runtime via K8s Secret
RUN touch /application.properties /clickhouse.sql

USER airflow

# cbioportal-core's requirements.txt is installed as-is (unconstrained): it pins
# legacy Jinja2/markupsafe that predate this Airflow base, and applying the
# Airflow constraints file here would hard-conflict and fail the build. Reconciling
# those pins with Airflow belongs in cbioportal-core, not here.
COPY requirements.txt /tmp/cbioportal_requirements.txt
RUN pip install --no-cache-dir -r /tmp/cbioportal_requirements.txt
# boto3 is the cmo-pipelines addition (DAG tasks use it for S3 downloads). Pin it
# under the Airflow constraints file so it resolves to an Airflow-compatible
# version instead of floating to whatever is latest.
RUN pip install --no-cache-dir boto3 \
      --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.10.5/constraints-3.12.txt"
