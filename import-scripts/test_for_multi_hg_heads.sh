#!/bin/bash

# the first entry (.) in the following list represents the root repository (cbio-portal-data); other entries are first level subdirectories of it
HG_SUB_REPO_LIST=". bic-mouse bic-mskcc brlo_tcga gdac_provisional_tcga genie grail immunotherapy impact levine_mskcc portal-configuration private rudin_mskcc solit_mskcc ucec_tcga yale_brain_tumor dm"
HG_ERROR_CODE=1
NO_HEAD_FOUND_CODE=255

PIPELINES_EMAIL_LIST="cbioportal-pipelines@cbioportal.org"

if [ -z ${PORTAL_DATA_HOME} ] || [ -z ${HG_BINARY} ] ; then
	message="test could not run: automation-environment.sh script must be run in order to set PORTAL_DATA_HOME and HG_BINARY environment variables"
	echo ${message}
	echo ${message} | mail -s "Mercurial Repository Problem Report" $PIPELINES_EMAIL_LIST
	exit 250
fi
function repository_has_single_head () {
	checkdir=$1
	cd ${checkdir}
	totalheadcount=$(${HG_BINARY} heads | grep -e '^changeset:' | wc -l )
	namedbranchheadcount=$(${HG_BINARY} heads | grep -e '^branch:' | wc -l )
	if [ -z "${totalheadcount}" ] || [ -z "${namedbranchheadcount}" ] ; then
		echo "error during mercurial command to read head counts in repo ${checkdir}"
		return ${HG_ERROR_CODE}  #error during mercurial
	fi
	defaultbranchheadcount=$(( ${totalheadcount} - ${namedbranchheadcount} ))
	if [ ${defaultbranchheadcount} -gt 1 ] ; then
		return ${defaultbranchheadcount}
	fi
	if [ ${defaultbranchheadcount} -lt 1 ] ; then
		return ${NO_HEAD_FOUND_CODE} #no heads found at all
	fi
	return 0 #just one head found (not multiple)
}

sendemailflag=0
tempfilename=$(mktemp /tmp/test_for_multi_hg_heads_email.XXXXXX)
if [ -z ${tempfilename} ] || ! [ -w ${tempfilename} ] ; then
	message="test could not run: tempfile \"${tempfilename}\" could not be created/written"
	echo ${message}
	echo ${message} | mail -s "Mercurial Repository Problem Report" $PIPELINES_EMAIL_LIST
	exit 249
fi

echo "Mercurial repository integrity test problem report" >> ${tempfilename}
date >> ${tempfilename}
echo "Each individual repository is tested for multiple active heads on the default branch." >> ${tempfilename}
echo "Below is a report of cases where tests do not run correctly," >> ${tempfilename}
echo "or where repos have other than a single head on the default branch." >> ${tempfilename}
echo "Note: this is a test of the cbioportal_importer repository clones," >> ${tempfilename}
echo "located at or below: ${PORTAL_DATA_HOME}" >> ${tempfilename}
echo >> ${tempfilename}
for subdir in ${HG_SUB_REPO_LIST} ; do
	dirpath=${PORTAL_DATA_HOME}/${subdir}
	if [ ${subdir} == "." ] ; then
		dirpath=${PORTAL_DATA_HOME}
	fi
	if [ -d ${dirpath} ] ; then
		repository_has_single_head ${dirpath}
		returnvalue=$?
		if [ ${returnvalue} -ne 0 ] ; then
			sendemailflag=1
			echo ------------------------------------------------------------------------ >> ${tempfilename}
			echo problem found in ${dirpath} >> ${tempfilename}
			echo ------------------------------------------------------------------------ >> ${tempfilename}
			case ${returnvalue} in
				${NO_HEAD_FOUND_CODE} )
					echo "no heads found in default branch" >> ${tempfilename}
					;;
				${HG_ERROR_CODE} )
					echo "error running mercurial command to find heads : command output missing or malformatted" >> ${tempfilename}
					;;
				* )
					echo "${returnvalue} heads on the default branch:" >> ${tempfilename}
					echo Output of running '"hg head"': >> ${tempfilename}
					cd ${checkdir} ; ${HG_BINARY} heads >> ${tempfilename}
					;;
			esac
			echo >> ${tempfilename}
		fi
	fi
done
if [ ${sendemailflag} -ne 0 ] ; then
	cat ${tempfilename}
	cat ${tempfilename} | mail -s "Mercurial Repository Problem Report" $PIPELINES_EMAIL_LIST
fi
rm -f ${tempfilename}
exit ${sendemailflag}
