# cmo-pipelines

A collection of applications and scripts for managing the fetching of source data from various respositories and resourcs, and for maintaining
the proper functioning of a linux host executing periodic fetch and import pipeline processes.

## Contents

There are these Java components and applications:

- common : a java library of helpful utilities used (as a dependency) in other components
- crdb : "crdb_fetcher", a pipeline which fetches data from the clinical research database
- cvr : "cvr_fetcher", a pipeline which downloads samples with identified genomic variants and clinical data from the CVR servers (tumor and germline)
- ddp :  "ddp_fetcher", a pipeline which fetches clinical data from the darwin discovery platform web API
- redcap : "redcap_pipeline", a pipeline which uploads data to or downloads data from the redcap clinical database server

There are numerous scripts for fetch / import / montior / notification / configuration in the "import-scripts" subdirectory. See [README](import-scripts/README.md) for some script descriptions. Also included are current schedule crontab entries.
