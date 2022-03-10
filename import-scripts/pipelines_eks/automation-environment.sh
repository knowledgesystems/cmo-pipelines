#!/bin/bash

#######################
# general paths/options for system executables
#######################
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.amzn2.0.1.x86_64
export JAVA_PROXY_ARGS="-Dhttp.proxyHost=jxi2.mskcc.org -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts=draco.mskcc.org|pidvudb1.mskcc.org|phcrdbd2.mskcc.org|dashi-dev.cbio.mskcc.org|pipelines.cbioportal.mskcc.org|localhost"
export JAVA_BINARY=$JAVA_HOME/bin/java
export PYTHON_BINARY=/usr/bin/python
export PYTHON3_BINARY=/usr/bin/python3
export MAVEN_BINARY=/opt/apache-maven-3.8.3/bin/mvn
export HG_BINARY=/usr/bin/hg
export GIT_BINARY=/usr/bin/git
export PATH=$(bash --login -c 'echo $PATH')

#######################
# environment variables for top-level data repositories / code bases
#######################
export PORTAL_HOME=/data/portal-cron
export PORTAL_DATA_HOME=$PORTAL_HOME/cbio-portal-data

#######################
# environment variables for our git code bases
#######################
export PORTAL_GIT_HOME=$PORTAL_HOME/git-repos
export CMO_PIPELINES_HOME=$PORTAL_GIT_HOME/cmo-pipelines
export PIPELINES_HOME=$PORTAL_GIT_HOME/pipelines
export CBIOPORTAL_HOME=$PORTAL_GIT_HOME/cbioportal
export GENOME_NEXUS_ANNOTATOR_HOME=$PORTAL_GIT_HOME/genome-nexus-annotation-pipeline
export ANNOTATOR_JAR=$GENOME_NEXUS_ANNOTATOR_HOME/annotationPipeline/target/annotationPipeline*.jar
export ONCO_HOME=$PORTAL_GIT_HOME/oncotree
export ONCOKB_ANNOTATOR_HOME=$PORTAL_GIT_HOME/oncokb-annotator
export CDD_HOME=$PORTAL_GIT_HOME/clinical-data-dictionary
export DDP_CREDENTIALS_FILE=$PORTAL_HOME/pipelines-credentials/application-secure.properties
export AWS_SSL_TRUSTSTORE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore
export AWS_SSL_TRUSTSTORE_PASSWORD_FILE=$PORTAL_HOME/pipelines-credentials/AwsSsl.truststore.password
export SLACK_URL_FILE=$PORTAL_HOME/pipelines-credentials/slack.url
export GMAIL_CREDS_FILE=$PORTAL_HOME/pipelines-credentials/gmail.credentials
export START_TRIAGE_IMPORT_TRIGGER_FILENAME=$PORTAL_HOME/import-trigger/triage-import-start-request
export KILL_TRIAGE_IMPORT_TRIGGER_FILENAME=$PORTAL_HOME/import-trigger/triage-import-kill-request
export TRIAGE_IMPORT_IN_PROGRESS_FILENAME=$PORTAL_HOME/import-trigger/triage-import-in-progress
export TRIAGE_IMPORT_KILLING_FILENAME=$PORTAL_HOME/import-trigger/triage-import-killing
export START_HGNC_IMPORT_TRIGGER_FILENAME=$PORTAL_HOME/import-trigger/hgnc-import-start-request
export KILL_HGNC_IMPORT_TRIGGER_FILENAME=$PORTAL_HOME/import-trigger/hgnc-import-kill-request
export HGNC_IMPORT_IN_PROGRESS_FILENAME=$PORTAL_HOME/import-trigger/hgnc-import-in-progress
export HGNC_IMPORT_KILLING_FILENAME=$PORTAL_HOME/import-trigger/hgnc-import-killing
export PUBLIC_CLUSTER_KUBECONFIG=$PORTAL_HOME/pipelines-credentials/public-cluster-config

#######################
# SSL args (for AWS + redcap)
#######################
export JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$AWS_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=`cat $AWS_SSL_TRUSTSTORE_PASSWORD_FILE`"

#######################
# environment variables for configuration / properties files
#######################
export PORTAL_CONFIG_HOME=$PORTAL_GIT_HOME/portal-configuration
export PIPELINES_CONFIG_HOME=$PORTAL_GIT_HOME/pipelines-configuration

#######################
# environment variables for top level data repositories
#######################
export BIC_LEGACY_DATA_HOME=$PORTAL_DATA_HOME/bic-mskcc-legacy
export CMO_ARGOS_DATA_HOME="$PORTAL_DATA_HOME/cmo-argos"
export PDX_DATA_HOME=$PORTAL_DATA_HOME/crdb_pdx
export PRIVATE_DATA_HOME=$PORTAL_DATA_HOME/private
export DMP_DATA_HOME=$PORTAL_DATA_HOME/dmp
export FOUNDATION_DATA_HOME=$PORTAL_DATA_HOME/foundation
export IMPACT_DATA_HOME=$PORTAL_DATA_HOME/impact
export DATAHUB_DATA_HOME=$PORTAL_DATA_HOME/datahub/public
export MSK_MIND_DATA_HOME=$PORTAL_DATA_HOME/msk-mind
export MSK_SHAHLAB_DATA_HOME=$PORTAL_DATA_HOME/datahub_shahlab

#######################
# environment variables used across import scripts
#######################
#export INHIBIT_RECACHING_FROM_TOPBRAID=true
export CASE_LIST_CONFIG_FILE=$PIPELINES_CONFIG_HOME/resources/case_list_config.tsv

#######################
# environment variables used in the fetch-and-import-dmp-impact-data script
#######################
# trigger files to communicate between fetch-dmp-data and import-dmp-data
export MSK_DMP_TMPDIR=$PORTAL_HOME/tmp/import-cron-dmp-msk
export MSK_IMPACT_CONSUME_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_consume_trigger.txt
export MSK_HEMEPACT_CONSUME_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_heme_consume_trigger.txt
export MSK_ARCHER_CONSUME_TRIGGER=$MSK_DMP_TMPDIR/mskarcher_consume_trigger.txt
export MSK_ACCESS_CONSUME_TRIGGER=$MSK_DMP_TMPDIR/mskaccess_consume_trigger.txt
export MSK_ARCHER_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskarcher_import_trigger.txt
export MSK_SOLID_HEME_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/msk_solid_heme_import_trigger.txt
export MSK_KINGS_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/kingscounty_import_trigger.txt
export MSK_LEHIGH_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/lehighvalley_import_trigger.txt
export MSK_QUEENS_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/queenscancercenter_import_trigger.txt
export MSK_MCI_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/miamicancerinstitute_import_trigger.txt
export MSK_HARTFORD_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/hartfordhealthcare_import_trigger.txt
export MSK_RALPHLAUREN_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/ralphlauren_import_trigger.txt
export MSK_RIKENGENESISJAPAN_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/msk_rikengenesisjapan_import_trigger.txt
export MSK_SCLC_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/sclc_mskimpact_import_trigger.txt
export MSKIMPACT_PED_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_ped_import_trigger.txt
export LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/lymphoma_super_cohort_fmi_msk_import_trigger.txt
# data directories
export MSK_IMPACT_DATA_HOME=$DMP_DATA_HOME/mskimpact
export MSK_RAINDANCE_DATA_HOME=$DMP_DATA_HOME/mskraindance
export MSK_HEMEPACT_DATA_HOME=$DMP_DATA_HOME/mskimpact_heme
export MSK_ARCHER_DATA_HOME=$DMP_DATA_HOME/mskarcher
export MSK_ARCHER_UNFILTERED_DATA_HOME=$DMP_DATA_HOME/mskarcher_unfiltered
export MSK_ACCESS_DATA_HOME=$DMP_DATA_HOME/mskaccess
export MSK_MIXEDPACT_DATA_HOME=$DMP_DATA_HOME/mixedpact
export MSK_SOLID_HEME_DATA_HOME=$DMP_DATA_HOME/msk_solid_heme
export MSK_KINGS_DATA_HOME=$DMP_DATA_HOME/msk_kingscounty
export MSK_LEHIGH_DATA_HOME=$DMP_DATA_HOME/msk_lehighvalley
export MSK_QUEENS_DATA_HOME=$DMP_DATA_HOME/msk_queenscancercenter
export MSK_MCI_DATA_HOME=$DMP_DATA_HOME/msk_miamicancerinstitute
export MSK_HARTFORD_DATA_HOME=$DMP_DATA_HOME/msk_hartfordhealthcare
export MSK_RALPHLAUREN_DATA_HOME=$DMP_DATA_HOME/msk_ralphlauren
export MSK_RIKENGENESISJAPAN_DATA_HOME=$DMP_DATA_HOME/msk_rikengenesisjapan
export MSK_SCLC_DATA_HOME=$DMP_DATA_HOME/sclc_mskimpact_2017
export MSKIMPACT_PED_DATA_HOME=$DMP_DATA_HOME/mskimpact_ped
export LYMPHOMA_SUPER_COHORT_DATA_HOME=$DMP_DATA_HOME/lymphoma_super_cohort_fmi_msk
export MSK_EXTRACT_COHORT_DATA_HOME=$MSK_MIND_DATA_HOME/datahub/msk_extract_cohort2_2019
export MSK_SPECTRUM_COHORT_DATA_HOME=$MSK_SHAHLAB_DATA_HOME/msk_spectrum
# read-only data directories
export FMI_BATLEVI_DATA_HOME=$FOUNDATION_DATA_HOME/mixed/lymphoma/mskcc/foundation/lymph_landscape_fmi_201611

#######################
# environment variables used in the backup-redcap-data.sh script
#######################
export REDCAP_BACKUP_DATA_HOME=$PORTAL_DATA_HOME/redcap-snapshot
export MSKIMPACT_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskimpact
export HEMEPACT_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskimpact_heme
export ARCHER_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskarcher
export ACCESS_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskaccess

#######################
# environment variables used in the import-pdx-data script
#######################
export CRDB_FETCHER_PDX_HOME=$PDX_DATA_HOME/crdb_pdx_raw_data

#######################
# environment variables used for oncokb annotator script
#######################
export ONCOKB_TOKEN_FILE=$PORTAL_HOME/pipelines-credentials/oncokb.token
