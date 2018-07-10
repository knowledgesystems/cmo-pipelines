#!/bin/bash
export PORTAL_HOME=/data/portal-cron
export PORTAL_DATA_HOME=$PORTAL_HOME/cbio-portal-data
export PORTAL_GIT_HOME=$PORTAL_HOME/git-repos
export PORTAL_CONFIG_HOME=$PORTAL_DATA_HOME/portal-configuration
export PIPELINES_CONFIG_HOME=$PORTAL_GIT_HOME/pipelines-configuration
export MSK_IMPACT_DATA_HOME=$PORTAL_DATA_HOME/dmp/mskimpact
export MSK_RAINDANCE_DATA_HOME=$PORTAL_DATA_HOME/dmp/mskraindance
export MSK_HEMEPACT_DATA_HOME=$PORTAL_DATA_HOME/dmp/mskimpact_heme
export MSK_ARCHER_DATA_HOME=$PORTAL_DATA_HOME/dmp/mskarcher
export MSK_MIXEDPACT_DATA_HOME=$PORTAL_DATA_HOME/dmp/mixedpact
export MSK_SOLID_HEME_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_solid_heme
export MSK_KINGS_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_kingscounty
export MSK_LEHIGH_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_lehighvalley
export MSK_QUEENS_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_queenscancercenter
export MSK_MCI_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_miamicancerinstitute
export MSK_HARTFORD_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_hartfordhealthcare
export MSK_RALPHLAUREN_DATA_HOME=$PORTAL_DATA_HOME/dmp/msk_ralphlauren
export IMPACT_DATA_HOME=$PORTAL_DATA_HOME/impact
export FOUNDATION_DATA_HOME=$PORTAL_DATA_HOME/foundation
export FMI_BATLEVI_DATA_HOME=$FOUNDATION_DATA_HOME/mixed/lymphoma/mskcc/foundation/lymph_landscape_fmi_201611
export LYMPHOMA_SUPER_COHORT_DATA_HOME=$PORTAL_DATA_HOME/dmp/lymphoma_super_cohort_fmi_msk
export DLBCL_IMPACT_DATA_HOME=$PORTAL_DATA_HOME/dmp/dlbcl_impact_fmi_2018
export MSK_SCLC_DATA_HOME=$PORTAL_DATA_HOME/dmp/sclc_mskimpact_2017
export REDCAP_BACKUP_DATA_HOME=$PORTAL_DATA_HOME/redcap-snapshot
export MSKIMPACT_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskimpact
export HEMEPACT_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskimpact_heme
export RAINDANCE_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskraindance
export ARCHER_REDCAP_BACKUP=$REDCAP_BACKUP_DATA_HOME/mskarcher
export MSKIMPACT_PED_DATA_HOME=$PORTAL_DATA_HOME/dmp/mskimpact_ped
export DDP_CREDENTIALS_FILE=$PORTAL_HOME/pipelines-credentials/application-secure.properties
export CMO_PIPELINES_HOME=$PORTAL_GIT_HOME/cmo-pipelines
export PIPELINES_HOME=$PORTAL_GIT_HOME/pipelines
export CBIOPORTAL_HOME=$PORTAL_GIT_HOME/cbioportal
export GENOME_NEXUS_ANNOTATOR_HOME=$PORTAL_GIT_HOME/genome-nexus-annotation-pipeline
export ONCO_HOME=$PORTAL_GIT_HOME/oncotree
export CDD_HOME=$PORTAL_GIT_HOME/clinical-data-dictionary

# trigger files to communicate between fetch-dmp-data and import-dmp-data
export MSK_DMP_TMPDIR=$PORTAL_HOME/tmp/import-cron-dmp-msk
export MSK_IMPACT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_import_trigger.txt
export MSK_RAINDANCE_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskraindance_import_trigger.txt
export MSK_HEMEPACT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_heme_import_trigger.txt
export MSK_ARCHER_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskarcher_import_trigger.txt
export MSK_MIXEDPACT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mixedpact_import_trigger.txt
export MSK_SOLID_HEME_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/msk_solid_heme_import_trigger.txt
export MSK_KINGS_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/kingscounty_import_trigger.txt
export MSK_LEHIGH_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/lehighvalley_import_trigger.txt
export MSK_QUEENS_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/queenscancercenter_import_trigger.txt
export MSK_MCI_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/miamicancerinstitute_import_trigger.txt
export MSK_HARTFORD_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/hartfordhealthcare_import_trigger.txt
export MSK_RALPHLAUREN_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/ralphlauren_import_trigger.txt
export MSK_SCLC_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/sclc_mskimpact_import_trigger.txt
export MSKIMPACT_PED_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/mskimpact_ped_import_trigger.txt
export LYMPHOMA_SUPER_COHORT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/lymphoma_super_cohort_fmi_msk_import_trigger.txt
export DLBCL_IMPACT_IMPORT_TRIGGER=$MSK_DMP_TMPDIR/dlbcl_impact_fmi_2018_import_trigger.txt

# CMO_DATA_HOME looks unused
#export CMO_DATA_HOME=/data/cbio-portal-data/bic-mskcc
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk.x86_64
export JAVA_SSL_ARGS="-Djavax.net.ssl.trustStore=$PORTAL_CONFIG_HOME/properties/redcap-pipeline/cacerts_java1.8_plus_pipelines"
export JAVA_PROXY_ARGS="-Dhttp.proxyHost=jxi2.mskcc.org -Dhttp.proxyPort=8080"
export PYTHON_BINARY=/data/tools/python2.7/bin/python
export MAVEN_BINARY=/data/portal-cron/bin/mvn
export HG_BINARY=/usr/bin/hg
export PATH=/data/tools/python2.7/bin:/home/grossb/local/bin/:$PATH:/usr/bin/:/bin/
