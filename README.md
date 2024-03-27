# cmo-pipelines

A collection of applications and scripts for managing the fetching of source data from various respositories and resourcs, and for maintaining
the proper functioning of a linux host executing periodic fetch and import pipeline processes.

## Contents

There are these Java components and appliations:

- common : a java library of helpful utilities used (as a dependency) in other components
- gdd : the "genome directed diagnosis pipeline", which is not currently being maintained (delete?)
- crdb : "crdb_fetcher", a pipeline which fetches data from the clinical research database
- redcap : "redcap_pipeline", a pipeline which uploads data to or downloads data from the redcap clinical database server
- cvr : "cvr_fetcher", a pipeline which downloads samples with identified genomic variants and clinical data from the CVR servers (tumor and germline)
- gene : "gene_data_updater", a pipeline which processes a downloaded NCBI human gene info file and encorporates info into the cBioPortal gene table. No longer maintained. (delete?)
- ddp :  "ddp_fetcher", a pipeline which fetches clinical data from the darwin discovery platform web API

There is this compiled linux executable:
- src : "import-tool", a program which writes appropriate improt trigger files for users who control the running of the import pipelines with import-tool scripts.

There are numerous scripts for fetch / import / montior / notification / configuration in the "import-scripts" subdirectory. Also included are current schedule crontab entries.
