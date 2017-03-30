#!/bin/bash

# get args
msk_portal_logfile=$1

num_entries=`gunzip --to-stdout $msk_portal_logfile | grep "restful_login: Query initiated by user:" | wc -l`
echo $num_entries
