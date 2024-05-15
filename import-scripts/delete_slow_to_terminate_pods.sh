#!/usr/bin/env bash

MY_FLOCK_FILEPATH="/data/portal-cron/cron-lock/delete_slow_to_terminate_pods.lock"
(
    # this comment repairs vim syntax coloring in subshell block )
    # check lock so that script executions do not overlap
    if ! flock --nonblock --exclusive $my_flock_fd ; then
        exit 0
    fi

    K8S_BINARY="/data/tools/kubectl"
    TIMEOUT_BINARY="/usr/bin/timeout"
    TIMEOUT_EXHAUSTED_STATUS=124
    SECONDS_BEFORE_TIMEOUT=90 # wait 90 seconds for deletion to occur
    WORK_DIR=/data/portal-cron/tmp/terminating_pods
    POD_TERMINATING_TIMESTAMP_FILE=$WORK_DIR/terminating_pod_timestamp
    POD_LIST_FILE=$WORK_DIR/pod_list_file
    TRIGGER_STATE="Terminating"
    TRIGGER_STATE_DURATION_FOR_DELETE=300 # 5 minutes observed in the Terminating state causes deletion

    if [ -z "$PORTAL_HOME" ] ; then
        export PORTAL_HOME=/data/portal-cron
    fi
    source "$PORTAL_HOME/scripts/slack-message-functions.sh"

    function check_for_dependencies () {
        if [ ! -x "$TIMEOUT_BINARY" ] ; then
            echo "Error : delete_slow_to_terminate_pods.sh requires the timeout command to be available ($TIMEOUT_BINARY)" 1>&2
            exit 1
        fi
        if [ ! -x "$K8S_BINARY" ] ; then
            echo "Error : delete_slow_to_terminate_pods.sh requires the kubectl command to be available ($K8S_BINARY)" 1>&2
            exit 1
        fi
    }

    function delete_pod () {
        pod_id=$1
        namespace=$2
        use_force=$3
        if [ -z "$use_force" ] ; then
            $TIMEOUT_BINARY $SECONDS_BEFORE_TIMEOUT $K8S_BINARY delete pod $pod_id --namespace $namespace
        else
            $TIMEOUT_BINARY $SECONDS_BEFORE_TIMEOUT $K8S_BINARY delete pod $pod_id --namespace $namespace --force --grace_period=0
        fi
        exit_status=$?
        if [ $exit_status -eq $TIMEOUT_EXHAUSTED_STATUS ] ; then
            if [ ! -z "$use_force" ] ; then
                echo attempt to forcibly delete pod $pod_id timed out before completion 1>&2
            else
                echo attempt to gently delete pod $pod_id timed out before completion 1>&2
            fi
        fi
        return $exit_status
    }

    function make_work_dir_if_necessary () {
        if [ ! -d $WORK_DIR ] ; then
            mkdir -p $WORK_DIR
        fi
    }

    function find_current_pods () {
        $K8S_BINARY get pods --all-namespaces > $POD_LIST_FILE
        if [ "$?" -ne 0 ] ; then
            echo Error : could not get pod listing 1>&2
            exit 1
        fi
        namespace_index=0
        id_index=-1
        state_index=-1
        processing_header=1
        while read line ; do # read lines from the pod list file
            if [ "$processing_header" -ne 1 ] ; then
                # extract field values
                namespace=$(echo ${line:$namespace_index} | cut -f 1 --delim $" ")
                id=$(echo ${line:$id_index} | cut -f 1 --delim $" ")
                state=$(echo ${line:$state_index} | cut -f 1 --delim $" ")
                pod_to_state[$id]=$state
                pod_to_namespace[$id]=$namespace
            else
                # find header offsets
                namespace_index=$(echo "$line" | grep -b -o --basic-regexp NAMESPACE[[:space:]] | cut -f 1 --delim :)
                id_index=$(echo "$line" | grep -b -o --basic-regexp NAME[[:space:]] | cut -f 1 --delim :)
                state_index=$(echo "$line" | grep -b -o --basic-regexp STATUS[[:space:]] | cut -f 1 --delim :)
            fi
            processing_header=0
        done < $POD_LIST_FILE
        rm -f $POD_LIST_FILE
    }

    function get_timestamps_for_terminating_pods () {
        if [ ! -e $POD_TERMINATING_TIMESTAMP_FILE ] ; then
            touch $POD_TERMINATING_TIMESTAMP_FILE
        fi
        while read line ; do # read lines from pod timestamp file
            id=$(echo $line | cut -f 1 --delim $" ")
            timestamp=$(echo $line | cut -f 2 --delim $" ")
            notification_sent=$(echo $line | cut -f 3 --delim $" ")
            if [ "${pod_to_state[$id]}" == "$TRIGGER_STATE" ] ; then
                pod_to_timestamp[$id]=$timestamp
                pod_to_notification_sent[$id]=$notification_sent
            fi
        done < $POD_TERMINATING_TIMESTAMP_FILE
    }

    function add_new_terminating_pods_to_timestamps () {
        for id in "${!pod_to_state[@]}" ; do
            if [ "${pod_to_state[$id]}" == "$TRIGGER_STATE" ] ; then
                # there is a terminating pod
                if [ -z "${pod_to_timestamp[$id]}" ] ; then
                    pod_to_timestamp[$id]=$now
                    pod_to_notification_sent[$id]=0
                fi
            fi
        done
    }

    function pod_is_slow_to_terminate () {
        pod_id=$1
        timestamp="${pod_to_timestamp[$pod_id]}"
        if [ -z "$timestamp" ] ; then
            echo "Error - pod $pod_id has no timestamp for $TRIGGER_STATE state" 1>&2
            return 1
        fi
        number_re='^[0-9]+$'
        if ! [[ "$timestamp" =~ $number_re ]] ; then
            echo "Error - pod $pod_id has a non numeric timestamp for $TRIGGER_STATE state" 1>&2
            return 1
        fi
        difference=$(($now - $timestamp))
        if [ $difference -ge $TRIGGER_STATE_DURATION_FOR_DELETE ] ; then
            return 0
        fi
        return 1
    }

    function send_slow_to_terminate_notification () {
        pod_id=$1
        notification_sent=${pod_to_notification_sent[$pod_id]}
        if [ "$notification_sent" -eq 0 ] ; then
            message="kubernetes alert : pod $id is still in Terminating state after $TRIGGER_STATE_DURATION_FOR_DELETE seconds."
            send_slack_message_to_channel "#status" "string" "$message :turtle:"
            pod_to_notification_sent[$pod_id]=1
        fi
    }

    function send_deletion_notification () {
        pod_id=$1
        message="kubernetes alert : pod $id has been successfully deleted by a monitoring script."
        send_slack_message_to_channel "#status" "string" "$message :toilet:"
    }

    function delete_all_slow_to_terminate_pods () {
        for id in "${!pod_to_timestamp[@]}" ; do
            if pod_is_slow_to_terminate $id ; then
                send_slow_to_terminate_notification $id
                delete_pod $id ${pod_to_namespace[$id]}
                status=$?
                if [ $status -ne 0 ] ; then
                    delete_pod $id ${pod_to_namespace[$id]} use_force
                    status=$?
                    if [ $status -ne 0 ] ; then
                        echo "Tried and failed to delete pod $pod_id in state $TRIGGER_STATE" 1>&2
                    else
                        send_deletion_notification $id
                    fi
                else
                    send_deletion_notification $id
                fi
            fi
        done
    }

    function write_timestamps () {
        temp_file=${POD_TERMINATING_TIMESTAMP_FILE}.tmp
        rm -f $temp_file
        touch $temp_file
        for id in "${!pod_to_timestamp[@]}" ; do
            timestamp=${pod_to_timestamp[$id]}
            notification_sent=${pod_to_notification_sent[$id]}
            echo -e "$id\t$timestamp\t$notification_sent" >> $temp_file
        done
        mv $temp_file $POD_TERMINATING_TIMESTAMP_FILE
    }

    /data/portal-cron/scripts/authenticate_service_account.sh eks
    check_for_dependencies
    now=$(date +%s)
    make_work_dir_if_necessary
    declare -A pod_to_namespace
    declare -A pod_to_state
    find_current_pods
    declare -A pod_to_timestamp
    declare -A pod_to_notification_sent
    get_timestamps_for_terminating_pods
    add_new_terminating_pods_to_timestamps
    delete_all_slow_to_terminate_pods
    write_timestamps

) {my_flock_fd}>$MY_FLOCK_FILEPATH

exit 0
