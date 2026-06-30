# -------- Stage 1: build the JAR --------
FROM maven:3-eclipse-temurin-21 AS jar_builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Stage 2: Airflow + cbioportal tools --------
FROM apache/airflow:2.10.5

USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl \
      perl \
  && rm -rf /var/lib/apt/lists/* \
  && cd /tmp && curl -fsSL https://clickhouse.com/ | sh \
  && ./clickhouse install --noninteractive \
  && rm -f /tmp/clickhouse

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

# boto3 is needed by the DAG tasks for S3 downloads
COPY requirements.txt /tmp/cbioportal_requirements.txt
RUN pip install --no-cache-dir -r /tmp/cbioportal_requirements.txt && \
    pip install --no-cache-dir boto3
