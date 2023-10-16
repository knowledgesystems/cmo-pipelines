#!/usr/bin/env bash

FLOCK_FILEPATH="/data/portal-cron/cron-lock/import-public-data.lock"
(
    echo $(date)

    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $flock_fd ; then
        echo "Failure : could not acquire lock for $FLOCK_FILEPATH another instance of this process seems to still be running."
        exit 1
    fi

    # set necessary env variables with automation-environment.sh
    if [[ -z $PORTAL_HOME || -z $JAVA_BINARY ]] ; then
        echo "Error : import-public-data.sh cannot be run without setting PORTAL_HOME and JAVA_BINARY environment variables. (Use automation-environment.sh)"
        exit 1
    fi

    CHECK_PUBLIC_IMPORT_TRIGGER_FILE=0
    PUBLIC_IMPORT_TRIGGER_FILE=/data/portal-cron/START_PUBLIC_IMPORT
    if [ "$CHECK_PUBLIC_IMPORT_TRIGGER_FILE" -eq "1" ] ; then
        if ! [ -r $PUBLIC_IMPORT_TRIGGER_FILE ] ; then
            echo looked for but could not find/read trigger file $PUBLIC_IMPORT_TRIGGER_FILE
            exit 3
        fi
        PUBLIC_IMPORT_TRIGGER=`head -n 1 $PUBLIC_IMPORT_TRIGGER_FILE`
        if ! [ "$PUBLIC_IMPORT_TRIGGER" == "1" ] ; then
            # no import trigger -- exit quietly
            exit 0
        fi
        # reset trigger to 0
        echo "0" > $PUBLIC_IMPORT_TRIGGER_FILE
    fi

    # clear temp directory
    tmp=$PORTAL_HOME/tmp/import-cron-public
    if ! [ -d "$tmp" ] ; then
        if ! mkdir -p "$tmp" ; then
            echo "Error : could not create tmp directory '$tmp'" >&2
            exit 1
        fi
    fi
    if [[ -d "$tmp" && "$tmp" != "/" ]]; then
        rm -rf "$tmp"/*
    fi

    now=$(date "+%Y-%m-%d-%H-%M-%S")
    ENABLE_DEBUGGING=0
    java_debug_args=""
    if [ $ENABLE_DEBUGGING != "0" ] ; then
        java_debug_args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=27185"
    fi
    IMPORTER_JAR_FILENAME="$PORTAL_HOME/lib/public-importer.jar"
    JAVA_IMPORTER_ARGS="$JAVA_PROXY_ARGS $java_debug_args $JAVA_SSL_ARGS -Dspring.profiles.active=dbcp -Djava.io.tmpdir=$tmp -ea -cp $IMPORTER_JAR_FILENAME org.mskcc.cbio.importer.Admin"
    public_portal_notification_file=$(mktemp $tmp/public-portal-update-notification.$now.XXXXXX)
    ONCOTREE_VERSION_TO_USE=oncotree_latest_stable

    PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
    CDD_ONCOTREE_RECACHE_FAIL=0
    if ! [ -z $INHIBIT_RECACHING_FROM_TOPBRAID ] ; then
        # refresh cdd and oncotree cache
        bash $PORTAL_HOME/scripts/refresh-cdd-oncotree-cache.sh
        if [ $? -gt 0 ]; then
            CDD_ONCOTREE_RECACHE_FAIL=1
            message="Failed to refresh CDD and/or ONCOTREE cache during TRIAGE import!"
            echo $message
            echo -e "$message" | mail -s "CDD and/or ONCOTREE cache failed to refresh" $PIPELINES_EMAIL_LIST
        fi
    fi

    DB_VERSION_FAIL=0
    # check database version before importing anything
    echo "Checking if database version is compatible"
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --check-db-version
    if [ $? -gt 0 ]; then
        echo "Database version expected by portal does not match version in database!"
        DB_VERSION_FAIL=1
    fi

    # fetch updates in studies repository
    echo "fetching updates from cbio-portal-data..."
    CBIO_PORTAL_DATA_FETCH_FAIL=0
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source knowledge-systems-curated-studies --run-date latest
    if [ $? -gt 0 ]; then
        echo "cbio-portal-data fetch failed!"
        CBIO_PORTAL_DATA_FETCH_FAIL=1
        EMAIL_BODY="The cbio-portal-data data fetch failed."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: cbio-portal-data" $PIPELINES_EMAIL_LIST
    fi

    # fetch updates in CMO impact
    echo "fetching updates from impact..."
    CMO_IMPACT_FETCH_FAIL=0
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source impact --run-date latest
    if [ $? -gt 0 ]; then
        echo "impact fetch failed!"
        CMO_IMPACT_FETCH_FAIL=1
        EMAIL_BODY="The impact data fetch failed."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: impact" $PIPELINES_EMAIL_LIST
    fi

    # fetch updates in private repository
    echo "fetching updates from private..."
    PRIVATE_FETCH_FAIL=0
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source private --run-date latest
    if [ $? -gt 0 ]; then
        echo "private fetch failed!"
        PRIVATE_FETCH_FAIL=1
        EMAIL_BODY="The private data fetch failed."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: private" $PIPELINES_EMAIL_LIST
    fi

    echo "fetching updates from datahub..."
    DATAHUB_FETCH_FAIL=0
    $JAVA_BINARY $JAVA_IMPORTER_ARGS --fetch-data --data-source datahub --run-date latest
    if [ $? -gt 0 ]; then
        echo "datahub fetch failed!"
        DATAHUB_FETCH_FAIL=1
        EMAIL_BODY="The datahub data fetch failed."
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Data fetch failure: datahub" $PIPELINES_EMAIL_LIST
    fi

    if [[ $DB_VERSION_FAIL -eq 0 && $PRIVATE_FETCH_FAIL -eq 0 && $CMO_IMPACT_FETCH_FAIL -eq 0 && $CBIO_PORTAL_DATA_FETCH_FAIL -eq 0 && $DATAHUB_FETCH_FAIL -eq 0 && $CDD_ONCOTREE_RECACHE_FAIL -eq 0 ]]; then
        # import public studies into public portal (now in aws)
        echo "importing cancer type updates into public portal database..."
        $JAVA_BINARY -Xmx16g $JAVA_IMPORTER_ARGS --import-types-of-cancer --oncotree-version ${ONCOTREE_VERSION_TO_USE}
        echo "importing study data into public portal database..."
        $JAVA_BINARY -Xmx64g $JAVA_IMPORTER_ARGS --update-study-data --portal public-portal --update-worksheet --notification-file "$public_portal_notification_file" --oncotree-version ${ONCOTREE_VERSION_TO_USE} --transcript-overrides-source uniprot
        IMPORT_EXIT_STATUS=$?
        if [ $IMPORT_EXIT_STATUS -ne 0 ]; then
            echo "Public import failed!"
            EMAIL_BODY="Public import failed"
            echo -e "Sending email $EMAIL_BODY"
            echo -e "$EMAIL_BODY" | mail -s "Import failure: public" $PIPELINES_EMAIL_LIST
        fi
        num_studies_updated=''
        num_studies_updated_filename="$tmp/num_studies_updated.txt"
        if [ -r "$num_studies_updated_filename" ] ; then
            num_studies_updated=$(cat "$num_studies_updated_filename")
        fi
        echo "'$num_studies_updated' studies have been updated"
    fi

    EMAIL_BODY="The Public database version is incompatible. Imports will be skipped until database is updated."
    # send email if db version isn't compatible
    if [ $DB_VERSION_FAIL -gt 0 ]; then
        echo -e "Sending email $EMAIL_BODY"
        echo -e "$EMAIL_BODY" | mail -s "Public Update Failure: DB version is incompatible" $PIPELINES_EMAIL_LIST
    fi

    $JAVA_BINARY $JAVA_IMPORTER_ARGS --send-update-notification --portal public-portal --notification-file "$public_portal_notification_file"

    echo "Cleaning up any untracked files from CBIO-PUBLIC import..."
    bash $PORTAL_HOME/scripts/datasource-repo-cleanup.sh $PORTAL_DATA_HOME $PORTAL_DATA_HOME/impact $PORTAL_DATA_HOME/private $PORTAL_DATA_HOME/datahub
) {flock_fd}>$FLOCK_FILEPATH
