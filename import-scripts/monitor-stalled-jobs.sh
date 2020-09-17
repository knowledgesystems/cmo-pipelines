#!/bin/bash

if [ -z "$SLACK_URL_FILE" ] ; then
    echo "could not run monitor-stalled-jobs.sh: automation-environment.sh script must be run in order to set needed environment variables (like SLACK_URL_FILE, ...)"
    exit 1
fi

SLACK_PIPELINES_MONITOR_URL=`cat $SLACK_URL_FILE`

# converts timestamp (D:H:M:S) to seconds
function convert_to_seconds () {
    elapsed_time=$1
    elapsed_time_in_seconds=`echo "$elapsed_time" | awk -F: '{ total=0; m=1; } { for (i=0; i < NF; i++) {total += $(NF-i)*m; m *= i >= 2 ? 24 : 60 }} {print total}'`
    echo $elapsed_time_in_seconds
}

# function for sending notification emails
function send_email_notification () {
    process_name=$1
    hostname=`hostname`
    ### FAILURE EMAIL ###
    EMAIL_BODY="Following processes appear to be stalled.\nHostname: ${hostname}\ndate: ${now}\nrunning processes: see below\n\nCMD\tPID\tSTART_TIME\tETIME\n${process_name}\n"
    echo -e "Sending email\n$EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Alert: Import jobs stalled on ${hostname}" cbioportal-pipelines@cbio.mskcc.org
}

# Function for alerting slack channel of stalled jobs
function send_slack_warning_message () {
    curl -X POST --data-urlencode "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"A stalled nightly import process has been detected.\", \"icon_emoji\": \":tired_face:\"}" $SLACK_PIPELINES_MONITOR_URL
    # this comment corrects vim syntax coloring "
}

# Array of process names being checked
checked_process_list=(
    'import_portal_users_genie.sh'
    'importUsers.py'
    'import-dmp-impact-data.sh'
    'import-temp-study.sh'
    'oncokb-annotator.sh'
)

# Stalled times
mt_users_genie=$(( 5 * 60 )) # import_portal_users_genie.sh: 5 minutes
mt_import_users=$(( 15 * 60 )) # importUsers.py: 15 minutes
mt_import_dmp=$(( 10 * 60 * 60 )) # import-dmp-impact-data.sh: 10 hours
mt_import_temp_study=$(( 3 * 60 * 60 )) # import-temp-study.sh: 3 hours
mt_oncokb_annotator=$(( 4 * 60 * 60 )) # oncokb-annotator.sh: 4 hours
max_time=($mt_users_genie $mt_import_users $mt_import_dmp $mt_import_temp_study $mt_oncokb_annotator)
email_times=(0 0 0 0 0)

while :
do
    now=$(date "+%Y-%m-%d-%H-%M-%S")
    echo date: ${now} : scanning for long running import jobs
    myuidoutput=`id`
    myuid=-1
    if [[ ${myuidoutput} =~ uid=([[:digit:]]+).* ]] ; then
        myuid=${BASH_REMATCH[1]}
    fi
    if [ $myuid -eq -1 ] ; then
        echo Error : could not determine uid
        exit 1
    fi

    ex1="CMD" #exclude the header from ps
    ex2="grep\|ps\|tail\|vim" #exclude grep, ps, tail, and vim commands
    ex3="triage\|hot-deploy" #exclude triage or hot-deploy imports
    ex4="scan-for-stalled-import-jobs\.sh" #exclude this command (scan-for-stalled-import-jobs.sh)
    ex5="\.log" #exclude accesses to log files (less, cat, grep)

    for (( i=0; i<${#checked_process_list[@]}; i++ ));
    do
        # get ps for user and sort by elapsed time (take longest running process) and convert to seconds
        ps_output=`export COLUMNS=24000 ; ps --user $myuid -o cmd:100,pid:15,start_time:15,etime:15 --sort=etime | grep "${checked_process_list[i]}" | grep -ve "$ex1\|$ex2\|$ex3\|$ex4\|$ex5" | sed 's/\s\s\s*/\t/g' | head -1`
        ps_etime=`echo "$ps_output" | cut -f4 | sed 's/-/:/g'`
        ps_etime_seconds=$(convert_to_seconds $ps_etime)

        # if process is not stalled then set date to current time so next break triggers email
        if [ $ps_etime_seconds -le ${max_time[i]} ] ; then
            email_times[i]="$(date +%H%M)"
        else
        # if process is stalled and current time is greater than 'email time' - send email and set email time to current time plus 3 hours
            if [ $(date +%H%M) -gt ${email_times[i]} ] ; then
                send_email_notification "$ps_output"
                send_slack_warning_message
                email_times[i]="$((10#$(date -d '+3 hours' +"%H%M")))"
            fi
        fi
    done

    # kill script at 23:30 - restart at midnight in crontab
    if [ $(date +"%H%M") -gt 2330 ] ; then
        echo "Exiting monitoring script... script will restart at midnight"
        exit 0
    fi

    # sleep 10 minutes before trying again
    sleep 600
done
