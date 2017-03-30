#!/bin/bash

#
# Script will compare MSK-IMPACT glioma patient list with Mellinghoff mixed gbm and look for new glioma samples
#

tmp=/data/ben/tmp/glioma-update-check
rm -rf "$tmp"/*

# update mellinghoff data
# (assume MSK-IMPACT repos has been pulled in the morning - in fact we will break that process if we pull here)
cd $IMPACT_DATA_HOME;$HG_BINARY pull

# create list of glioma samples from MSK-IMPACT
glioma_samples="$tmp"/glioma-samples.txt
grep "Glioma" $MSK_IMPACT_DATA_HOME/data_clinical.txt | cut -f1 | sort -u > $glioma_samples

# create list of glioma samples from merged study
glioma_samples_from_merged_study="$tmp"/glioma-samples-from-merged_study.txt
grep -P "^P\-\d\d\d\d\d\d\d\-T\d\d-IM\d.*" $IMPACT_DATA_HOME/MERGED/mixed/gbm/mellinghoff_impact/data_clinical.txt | cut -f1 | sort -u > $glioma_samples_from_merged_study

# see if there is a difference
diff -a $glioma_samples $glioma_samples_from_merged_study > /dev/null 2>&1
if [ $? -ne 0 ]
then
    email_body="$tmp/email-body.txt"
    echo -e "MSK-IMPACT Glioma sample list:\n" > $email_body
    cat $glioma_samples >> $email_body
    echo -e "\n" >> $email_body
    echo -e "foundation/gbm/mskcc/foundation sample list:\n" >> $email_body
    cut -f1 $FOUNDATION_DATA_HOME/gbm/mskcc/foundation/data_clinical.txt | grep -P "^TRF.*" | sort -u >> $email_body
    mail -s "impact/MERGED/mixed/gbm/mellinghoff_impact needs updating" < $email_body -c heinsz@mskcc.org -c grossb1@mskcc.org chenh4@mskcc.org
fi
