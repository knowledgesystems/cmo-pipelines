#!/bin/bash

date
echo executing fetch-dmp-data-for-import.sh
/data/portal-cron/scripts/fetch-dmp-data-for-import.sh
date
echo executing import-dmp-impact-data.sh
/data/portal-cron/scripts/import-dmp-impact-data.sh
date
echo wrapper complete
