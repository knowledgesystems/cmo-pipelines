/*
 * Copyright (c) 2017, 2018, 2025 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.cvr.requeue;

import org.cbioportal.cmo.pipelines.common.util.EmailUtil;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRRequeueRecord;

import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Manda Wilson
 */
public class CvrRequeueListener implements StepExecutionListener {

    @Autowired
    private EmailUtil emailUtil;

    @Value("${dmp.email.sender}")
    private String sender;

    @Value("${dmp.email.recipient}")
    private String dmpRecipient;

    @Value("${email.recipient}")
    private String defaultRecipient;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    Logger log = Logger.getLogger(CvrRequeueListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.debug("afterStep(): checking for portal samples not in dmp or for samples that failed to requeue...");

        String studyId = stepExecution.getJobParameters().getString("studyId");
        Set<String> portalSamplesNotInDmp = cvrSampleListUtil.getPortalSamplesNotInDmp();
        Map<String, String> sampleListStats = cvrSampleListUtil.getSampleListStats();
        List<CVRRequeueRecord> failedToRequeueSamples = (List<CVRRequeueRecord>) stepExecution.getJobExecution().getExecutionContext().get("failedToRequeueSamples");
        Set<String> zeroVariantSamples = cvrSampleListUtil.getNonWhitelistedZeroVariantSamples();
        Map<String, Integer> nonSignedoutSampleSnpCounts = cvrSampleListUtil.getNonSignedoutSampleSnpCounts();
        Set<String> samplesInvalidPatientId = cvrSampleListUtil.getSamplesInvalidPatientIdList();

        String subject = "CVR pipeline master list errors: " +  studyId;
        StringBuilder body = new StringBuilder();

        // build email body text for samples with invalid patient ids
        if (samplesInvalidPatientId != null && samplesInvalidPatientId.size() > 0) {
            log.warn(samplesInvalidPatientId.size() + " samples with invalid patient ids");
            body.append("\nSamples with invalid patient ids:\n");
            for (String sampleId : samplesInvalidPatientId) {
                body.append("\n\t");
                body.append(sampleId);
            }
            body.append("\n");
        }

        // build email body text for samples that failed requeue
        if (failedToRequeueSamples != null && failedToRequeueSamples.size() > 0) {
            log.warn(failedToRequeueSamples.size() + " samples from the dmp master list failed to requeue");
            body.append("\nSamples that failed to requeue:\n");
            for (CVRRequeueRecord requeueRecord : failedToRequeueSamples) {
                body.append("\n\t");
                body.append(requeueRecord.getSampleId());
            }
            body.append("\n");
        }
        else {
            log.info("No samples failed to requeue");
        }

        // build email body text for samples that were not in dmp master list
        if (portalSamplesNotInDmp != null && portalSamplesNotInDmp.size() > 0) {
            log.warn(portalSamplesNotInDmp.size() + " portal samples are not in the dmp master list");
            body.append("\nPortal samples not found in DMP master list  " + portalSamplesNotInDmp.size() + " samples: ");

            int count = 1;
            for (String sample : portalSamplesNotInDmp) {
                log.warn("Portal sample '" + sample + "' is not in the dmp master list");
                if (count <= 30) {
                    body.append("\n\t");
                    body.append(sample);
                } else {
                    break;
                }
                count++;
            }
            if (portalSamplesNotInDmp.size() > 30) {
                String additionalSamples = "\n\tplus " + String.valueOf(portalSamplesNotInDmp.size() - 30) + " additional samples";
                body.append("\n\t...");
                body.append(additionalSamples);
            }
            body.append("\n");
        }

        // build email body text for samples that have zero variants and are not whitelisted
        if (zeroVariantSamples != null && zeroVariantSamples.size() > 0) {
            Set<String> zeroVariantSamplesToReport = new HashSet<>(); // sample ids that get reported in email
            Set<String> unreportedZeroVariantSamples = new HashSet<>(); // sample ids that the white list file is updated with
            log.warn(zeroVariantSamples.size() + " samples have zero variants and are not whitelisted");
            for (String sampleId : zeroVariantSamples) {
                Integer nonSignedoutCount = nonSignedoutSampleSnpCounts.getOrDefault(sampleId, 0);
                // if both signed out and non-signed out variant counts are zero
                // then safe to update white listed samples file with current sample
                if (nonSignedoutCount == 0) {
                    unreportedZeroVariantSamples.add(sampleId);
                }
                else {
                    zeroVariantSamplesToReport.add(sampleId);
                }
            }
            // update cvrSampleListUtil with unreported zero variant samples
            cvrSampleListUtil.setNewUnreportedSamplesWithZeroVariants(unreportedZeroVariantSamples);
            if (zeroVariantSamplesToReport.isEmpty()) {
                log.warn("All samples with zero signed out variants also have zero non-signed out variants - these will be added to " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            }
            else {
                body.append("\nSamples that have zero variants and are not whitelisted:\n");
                int count = 1;
                for (String sampleId : zeroVariantSamplesToReport) {
                    if (count <= 30) {
                        body.append("\n\t");
                        body.append(sampleId);
                        Integer nonSignedoutCount = nonSignedoutSampleSnpCounts.getOrDefault(sampleId, 0);
                        body.append(" (non-signedout snp count: ");
                        body.append(nonSignedoutCount.toString());
                        body.append(")");
                    } else {
                        break;
                    }
                    count++;
                }
                if (zeroVariantSamplesToReport.size() > 30) {
                    String additionalSamples = "\n\tplus " + String.valueOf(zeroVariantSamplesToReport.size() - 30) + " additional samples";
                    body.append("\n\t...");
                    body.append(additionalSamples);
                }
                body.append("\n");
            }
        }
        else {
            log.info("No samples had zero variants that were not whitelisted");
        }

        if (sampleListStats != null && !sampleListStats.isEmpty()) {
            body.append("\nSample list counts: ");
            for (String listName : sampleListStats.keySet()) {
                body.append("\n\t" + listName + " = " + sampleListStats.get(listName));
            }
            body.append("\n");
        }

        if (!body.toString().isEmpty()) {
            String[] recipients = {defaultRecipient, dmpRecipient};
            emailUtil.sendEmail(sender, recipients, subject, body.toString());
        }
        return ExitStatus.COMPLETED;
    }

}
