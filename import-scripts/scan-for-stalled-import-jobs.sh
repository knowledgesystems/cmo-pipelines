#!/bin/bash

now=$(date "+%Y-%m-%d-%H-%M-%S")
echo date: ${now} : scanning for long running import jobs
myuidoutput=`id`
myuid=-1
if [[ ${myuidoutput} =~ uid=([[:digit:]]+).* ]]; then
    myuid=${BASH_REMATCH[1]}
fi
if [ $myuid -eq -1 ]; then
    echo Error : could not determine uid
    exit 1
fi
ex1="CMD" #exclude the header from ps
ex2="grep\|ps\|tail\|vim" #exclude grep, ps, tail, and vim commands
ex3="importUsers[a-zA-Z0-9-]*.py\|users[a-zA-Z0-9-]*\.sh" #exclude strings from import user scripts
ex4="triage\|hot-deploy" #exclude triage or hot-deploy imports
ex5="scan-for-stalled-import-jobs\.sh" #exclude this command (scan-for-stalled-import-jobs.sh)
ex6="\.log" #exclude accesses to log files (less, cat, grep)

importprocesses=`export COLUMNS=24000 ; ps --user $myuid -o pid,start_time,cutime,cmd | grep import | grep -ve "$ex1\|$ex2\|$ex3\|$ex4\|$ex5\|$ex6"`
if [[ $importprocesses =~ .*[[:alnum:]].* ]] ; then
    #running
    hostname=`hostname`
    ### FAILURE EMAIL ###
    EMAIL_BODY="Import processes appear to be stalled.\nHostname: ${hostname}\ndate: ${now}\nrunning processes: see below\n\nPID\tSTART\tCPUTIME\tCMD\n${importprocesses}\n"
    echo -e "Sending email\n$EMAIL_BODY"
    echo -e "$EMAIL_BODY" | mail -s "Alert: Import jobs stalled on ${hostname}" cbioportal-pipelines@cbioportal.org
fi
