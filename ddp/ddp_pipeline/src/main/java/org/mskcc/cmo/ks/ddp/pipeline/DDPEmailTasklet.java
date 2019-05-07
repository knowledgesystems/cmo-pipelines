/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.ddp.pipeline;

import org.cbioportal.cmo.pipelines.common.util.EmailUtil;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPPatientListUtil;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 *
 * @author Manda Wilson
 */
@Service
@ComponentScan(basePackages = {"org.mskcc.cmo.ks.ddp.pipeline", "org.cbioportal.cmo.pipelines.common"})
public class DDPEmailTasklet implements Tasklet {

    @Value("${email.subject}")
    private String subject;

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private DDPPatientListUtil ddpPatientListUtil;

    Logger log = Logger.getLogger(DDPEmailTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        StringBuilder body = new StringBuilder();
        Set<String> patientsMissingDMPIds = ddpPatientListUtil.getPatientsMissingDMPIds();
        if (patientsMissingDMPIds.size() > 0) {
            body.append(constructMissingPatientsText(patientsMissingDMPIds, "patients filtered because of missing DMP IDs"));
        }
        Set<String> patientsMissingDemographics = ddpPatientListUtil.getPatientsMissingDemographics();
        if (patientsMissingDemographics.size() > 0) {
            body.append(constructMissingPatientsText(patientsMissingDemographics, "patients that were included are missing demographics"));
        }
        Set<String> patientsMissingDiagnoses = ddpPatientListUtil.getPatientsMissingDiagnoses();
        if (patientsMissingDiagnoses.size() > 0) {
            body.append(constructMissingPatientsText(patientsMissingDiagnoses, "patients that were included are missing diagnoses"));
        }
        Set<String> patientsMissingSurvival = DDPUtils.getPatientsMissingSurvival();
        if (patientsMissingSurvival.size() > 0) { // size will be zero if we either did not include survival information or if every patient has it
            body.append(constructMissingPatientsText(patientsMissingSurvival, "patients that were included are missing survival information"));
        }
        if (!body.toString().isEmpty()) {
            emailUtil.sendEmailToDefaultRecipient(subject, body.toString());
        }
        return RepeatStatus.FINISHED;
    }

    private String constructMissingPatientsText(Set<String> missingPatientIds, String reason) {
        StringBuilder text = new StringBuilder();
        text.append("\n");
        text.append(missingPatientIds.size());
        text.append(" ");
        text.append(reason);
        text.append(":\n");
        for (String patientId : missingPatientIds) {
            text.append("\n\t");
            text.append(patientId);
        }
        text.append("\n\n");
        return text.toString();
    }
}
