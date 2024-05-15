#!/bin/bash

# Test if the redis service is available.
# - if not running, start it up
# - if running but unresponsive, shut it down and restart

REDIS_PROPERTIES_FILE=/data/portal-cron/pipelines-credentials/redis.properties
REDIS_PASSWORD=`grep password $REDIS_PROPERTIES_FILE | sed 's/password://g'`
REDIS_SERVER_HOST=127.0.0.1
REDIS_SERVER_PORT=6379
REDIS_HOME=/srv/data/redis/redis-stable
REDIS_CONF_FILE=$REDIS_HOME/redis.conf
REDIS_BINARY=$REDIS_HOME/src/redis-server
REDIS_PING_FAST_TIMEOUT=3
REDIS_PING_SLOW_TIMEOUT=3
MAX_REDIS_PING_ATTEMPTS=10
DEBUG_MODE=0

if ! which redis-cli >> /dev/null ; then
    echo Error : redis-cli command not available in the current \$PATH
    exit 1
fi

if [ -z "$PORTAL_HOME" ] ; then
    export PORTAL_HOME=/data/portal-cron
fi
source "$PORTAL_HOME/scripts/slack-message-functions.sh"

# Function for alerting slack channel of any failures
function sendRedisRestartNotification {
    MESSAGE=$1
    send_slack_message_to_channel "#msk-pipeline-logs" "string" "redis server monitor script :flushed: : $MESSAGE"
}

function ping_reqeust_succeeds {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function ping_reqeust_succeeds ; fi
    timeout=$1
    if timeout $timeout redis-cli -a $REDIS_PASSWORD -p $REDIS_SERVER_PORT ping >> /dev/null ; then
        return 0 # 0 means "success" or "yes"
    else
        return 1
    fi
}

function test_for_redis_response_and_exit_on_success {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function test_for_redis_response_and_exit_on_success ; fi
    if ping_reqeust_succeeds $REDIS_PING_SLOW_TIMEOUT ; then
        exit 0
    fi
}

function redis_server_is_in_process_list {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function redis_server_is_in_process_list ; fi
    PS_FILTER_REGEX="grep\|ps\|tail\|head\|vi"
    process_linecount=`ps aux | grep redis-server | grep $REDIS_SEVER_HOST:$REDIS_SERVER_PORT | grep -ve $PS_FILTER_REGEX | wc -l`
    if [ $process_linecount -eq 0 ] ; then
        return 1 # 1 means "failure" or "no"
    else
        return 0 # 0 means "success" or "yes"
    fi
}

function start_redis_server {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function start_redis_server ; fi
    $REDIS_BINARY $REDIS_CONF_FILE & disown
    sendRedisRestartNotification "redis-server restarted"
}

function redis_server_responds_after_several_attempts {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function redis_server_responds_after_several_attempts ; fi
    number_of_failed_attempts=0
    while [ $number_of_failed_attempts -lt $MAX_REDIS_PING_ATTEMPTS ] ; do
        if ping_reqeust_succeeds $REDIS_PING_FAST_TIMEOUT; then
            return 0 # 0 means "success" or "yes"
        fi
        number_of_failed_attempts=$(( $number_of_failed_attempts + 1 ))
    done
    return 1 # 1 means "failure" or "no"
}

function find_and_kill_redis_process {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function find_and_kill_redis_process ; fi
    REDIS_PROCESS_NUMBER=`netstat -tanp | grep LISTEN | sed 's/\s\s\s*/\t/g' | grep -P ":$REDIS_SERVER_PORT\t" -m 1 | cut -f6 | sed 's/\/.*//'`
    if [ ! -z $REDIS_PROCESS_NUMBER ] ; then
        kill -9 $REDIS_PROCESS_NUMBER
        if [ $? -gt 0 ] ; then
            echo "failed to kill process $REDIS_PROCESS_NUMBER, please check for running redis-server process"
            return 1 # 1 means "failure" or "no"
        fi
    else
        echo "no redis-server process found"
        return 1 # 1 means "failure" or "no"
    fi
    return 0 # 0 means "success" or "yes"
}

function kill_and_restart_redis {
    if [ $DEBUG_MODE -ne 0 ] ; then echo entering function kill_and_restart_redis ; fi
    if find_and_kill_redis_process ; then
        start_redis_server
    fi
}

if [ $DEBUG_MODE -ne 0 ] ; then echo starting monitor-redis-and-restart.sh execution; fi
test_for_redis_response_and_exit_on_success
if ! redis_server_is_in_process_list ; then
    start_redis_server
    exit 0
fi
# lastly, try a number of ping attempts to the non-responsive redis server, and kill/restart if still no response
if ! redis_server_responds_after_several_attempts ; then
    kill_and_restart_redis
fi
if [ $DEBUG_MODE -ne 0 ] ; then echo monitor-redis-and-restart.sh execution complete; fi
