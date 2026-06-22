#!/bin/bash
PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"
export tab=$'\t'
tmp=$PORTAL_HOME/tmp/import-cron-dmp-msk
sourcefilename=$MSK_IMPACT_DATA_HOME/data_mutations_extended.txt

#find header field indices
tempfilename=$(mktemp $tmp/mskimpact_mut_filed_test.XXXXXX)
head ${sourcefilename} |
		grep Mutation_Status |
		grep t_ref_count |
		grep t_alt_count |
		grep n_ref_count |
		grep n_alt_count > ${tempfilename}
headerlinecount=$(cat ${tempfilename} | wc -l)
if [ ${headerlinecount} -ne 1 ] ; then
	message="failed to complete mskimpact missing tumor/normal-ref/alt counts test because no header was found"
	echo ${message}
	echo -e "${message}" | mail -s "MSKIMPACT scan for missing tumor/normal-ref/alt counts failure" $PIPELINES_EMAIL_LIST
	rm -f ${tempfilename}
	exit 1
fi
mut_stat_index=$(awk -F '\t' -v col='Mutation_Status' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $tempfilename)
t_ref_index=$(awk -F '\t' -v col='t_ref_count' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $tempfilename)
t_alt_index=$(awk -F '\t' -v col='t_alt_count' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $tempfilename)
n_ref_index=$(awk -F '\t' -v col='n_ref_count' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $tempfilename)
n_alt_index=$(awk -F '\t' -v col='n_alt_count' 'NR==1{for (i=1; i<=NF; i++) if ($i==col) {print i;exit}}' $tempfilename)
rm -f ${tempfilename}.fields

# sanity check for empty indices
if [ -z ${mut_stat_index} ] || [ -z ${t_ref_index} ] || [ -z ${t_alt_index} ] || [ -z ${n_ref_index} ] || [ -z ${n_alt_index} ] ; then
	message="failed to complete mskimpact missing tumor/normal-ref/alt counts test because header was missing a necessary field"
	echo ${message}
	echo -e "${message}" | mail -s "MSKIMPACT scan for missing tumor/normal-ref/alt counts failure" $PIPELINES_EMAIL_LIST
	rm ${tempfilename}
	exit 2
fi
neededfields=${mut_stat_index},${t_ref_index},${t_alt_index},${n_ref_index},${n_alt_index}
headerfilter="grep -v \"T01-IM3\""
#count only records which are not germline (also ignore empty fields)
germlinefilter="grep SOMATIC\|UNKNOWN"
cut -f ${neededfields} ${sourcefilename} | ${headerfilter} | ${germlinefilter} | cut -f 2-5 > ${tempfilename}
sed "s/${tab}/,/g" ${tempfilename} | grep ",," > ${tempfilename}.blanks
recordswithblanks=$(cat ${tempfilename}.blanks | wc -l)
rm -f ${tempfilename} ${tempfilename}.blanks
if [ ${recordswithblanks} -gt 0 ] ; then
	message="mskimpact missing tumor/normal-ref/alt counts test failed : ${recordswithblanks} (somatic/unknown) records with blanks found in field t_ref_count, t_alt_count, n_ref_count, or n_alt_count"
	echo ${message}
	echo -e "${message}" | mail -s "MSKIMPACT scan for missing tumor/normal-ref/alt counts failure" $PIPELINES_EMAIL_LIST
	exit 3
fi
#test passed - no blanks found in somatic/unknown records
exit 0
