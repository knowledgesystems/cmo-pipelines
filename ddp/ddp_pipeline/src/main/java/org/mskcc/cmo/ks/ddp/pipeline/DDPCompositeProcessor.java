/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPPatientListUtil;
import org.mskcc.cmo.ks.ddp.source.DDPDataSource;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.mskcc.cmo.ks.ddp.pipeline.model.CompositeResult;

/**
 *
 * @author ochoaa
 */
public class DDPCompositeProcessor implements ItemProcessor<DDPCompositeRecord, CompositeResult> {
    @Autowired
    private DDPDataSource ddpDataSource;

    @Autowired
    private DDPPatientListUtil ddpPatientListUtil;

    @Autowired
    private ClinicalProcessor clinicalProcessor;

    @Autowired
    private TimelineRadiationProcessor timelineRadiationProcessor;

    @Autowired
    private TimelineChemoProcessor timelineChemoProcessor;

    @Autowired
    private TimelineSurgeryProcessor timelineSurgeryProcessor;

    private final Logger LOG = Logger.getLogger(DDPCompositeProcessor.class);

    @Override
    public CompositeResult process(DDPCompositeRecord compositeRecord) throws Exception {
        // we don't have to check if ddpDataSource.getPatientDemographics or ddpDataSource.getPatientDiagnoses
        // are null because an exception will be thrown in that case too (by the repository)
        try {
            compositeRecord.setPatientDemographics(ddpDataSource.getPatientDemographics(compositeRecord.getDmpPatientId()));
        } catch (Exception e) {
            // demographics is necessary to calculate/resolve clinical fields so save
            // dmp patient id and return null so that writer skips this record
            ddpPatientListUtil.addPatientsMissingDemographics(compositeRecord.getDmpPatientId());
            return null;
        }
        try {
            compositeRecord.setPatientDiagnosis(ddpDataSource.getPatientDiagnoses(compositeRecord.getDmpPatientId()));
        } catch (Exception e) {
            ddpPatientListUtil.addPatientsMissingDiagnoses(compositeRecord.getDmpPatientId());
        }
        // get all available procedures for patient
        compositeRecord.setRadiationProcedures(ddpDataSource.getPatientRadiationProcedures(compositeRecord.getDmpPatientId()));
        compositeRecord.setChemoProcedures(ddpDataSource.getPatientChemoProcedures(compositeRecord.getDmpPatientId()));
        compositeRecord.setSurgicalProcedures(ddpDataSource.getPatientSurgicalProcedures(compositeRecord.getDmpPatientId()));

        // check that clinical result is valid - return null if not so that this record is skipped by writer
        String clinicalResult = null;
        try {
            clinicalResult = clinicalProcessor.process(compositeRecord);
        }
        catch (Exception e) {}
        if (Strings.isNullOrEmpty(clinicalResult)) {
            LOG.error("Error converting composite record into clinical record: " + compositeRecord.getDmpPatientId());
            return null;
        }
        // create composite result and call remaining processors
        CompositeResult compositeResult = new CompositeResult();
        compositeResult.setClinicalResult(clinicalResult);
        compositeResult.setTimelineRadiationResults(timelineRadiationProcessor.process(compositeRecord));
        compositeResult.setTimelineChemoResults(timelineChemoProcessor.process(compositeRecord));
        compositeResult.setTimelineSurgeryResults(timelineSurgeryProcessor.process(compositeRecord));
        return compositeResult;
    }
}
