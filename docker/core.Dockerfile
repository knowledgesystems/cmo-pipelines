# cbioportal-core importer JAR + ClickHouse helper scripts on the Airflow base.
# IMPORTANT: build context is a cbioportal-core checkout, NOT this repo:
#   docker build --platform linux/amd64 -f docker/core.Dockerfile -t cbioportal-core:dev /path/to/cbioportal-core
#
# -------- Stage 1: build the JAR --------
FROM maven:3-eclipse-temurin-21 AS jar_builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Stage 2: Airflow + cbioportal tools --------
FROM apache/airflow:2.10.5-python3.12

USER root

# clickhouse-client via apt: the old clickhouse.com self-installer exhausted the
# build VM's disk and was an unpinned pipe-to-shell
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

COPY --from=jar_builder /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

COPY --from=jar_builder /app/core-*.jar /
COPY scripts/ /scripts/
RUN chmod -R a+x /scripts/

ENV PORTAL_HOME=/

# bind-mounted at runtime via K8s Secret
RUN touch /application.properties /clickhouse.sql

USER airflow

# installed unconstrained: cbioportal-core pins legacy Jinja2/markupsafe that
# hard-conflict with the Airflow constraints file
COPY requirements.txt /tmp/cbioportal_requirements.txt
RUN pip install --no-cache-dir -r /tmp/cbioportal_requirements.txt
# boto3 constrained so it resolves Airflow-compatible
RUN pip install --no-cache-dir boto3 \
      --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.10.5/constraints-3.12.txt"
