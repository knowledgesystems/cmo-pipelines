#!/bin/bash

URL="http://oncotree.mskcc.org/cdd/api/"
SERVICE_UNAVAILABLE=503
OK=200

http_code=`curl -o /dev/null --silent --write-out '%{http_code}\n' $URL`
if [ "$http_code" -ne $OK ]
then
    (>&2 echo "GET '$URL' returned '$http_code' status code, expected '$OK'")
    if [ "$http_code" -eq $SERVICE_UNAVAILABLE ]
    then
        (>&2 echo "The CDD cache is empty, attempts to refresh the cache from Google must have failed")
    fi
fi
