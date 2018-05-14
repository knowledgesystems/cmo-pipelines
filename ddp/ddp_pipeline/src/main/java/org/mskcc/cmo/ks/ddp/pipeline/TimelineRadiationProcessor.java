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

import org.mskcc.cmo.ks.ddp.source.model.Radiation;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineRadiationRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import java.util.*;
import com.google.common.base.Strings;
import java.text.ParseException;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class TimelineRadiationProcessor implements ItemProcessor<DDPCompositeRecord, List<String>> {

    private final Logger LOG = Logger.getLogger(TimelineRadiationProcessor.class);

    @Override
    public List<String> process(DDPCompositeRecord compositeRecord) throws Exception {
        // we can't generate radiation timeline records without a reference date or if patient
        // hasn't had any radiation procedures - return null
        String firstTumorDiagnosisDate = DDPUtils.getFirstTumorDiagnosisDate(compositeRecord.getPatientDiagnosis());
        if (Strings.isNullOrEmpty(firstTumorDiagnosisDate) ||
                compositeRecord.getRadiationProcedures() == null ||
                compositeRecord.getRadiationProcedures().isEmpty()) {
            return null;
        }
        // convert radiation procedures into timeline records
        List<TimelineRadiationRecord> timelineRadiationRecords = convertTimelineRadiationRecords(compositeRecord.getDmpPatientId(), firstTumorDiagnosisDate, compositeRecord.getRadiationProcedures());
        if (timelineRadiationRecords ==  null || timelineRadiationRecords.isEmpty()) {
            LOG.error("Failed to convert any radiation procedures into timeline records for patient: " + compositeRecord.getDmpPatientId());
            return null;
        }
        // construct records into strings for writing to output file
        List<String> records = new ArrayList<>();
        for (TimelineRadiationRecord radiationRecord : timelineRadiationRecords) {
            try {
                records.add(DDPUtils.constructRecord(radiationRecord));
            }
            catch (NullPointerException e) {
                LOG.error("Error converting radiation record to timeline record string: " + radiationRecord.toString());
            }
        }
        return records;

    }

    /**
     * Converts radiation procedures into timeline radiation records.
     *
     * @param dmpPatientId
     * @param firstTumorDiagnosisDate
     * @param radiationProcedures
     * @return
     */
    private List<TimelineRadiationRecord> convertTimelineRadiationRecords(String dmpPatientId, String firstTumorDiagnosisDate, List<Radiation> radiationProcedures) {
        List<TimelineRadiationRecord> timelineRadiationRecords = new ArrayList<>();
        for (Radiation procedure : radiationProcedures) {
            TimelineRadiationRecord record;
            try {
                record = new TimelineRadiationRecord(dmpPatientId, firstTumorDiagnosisDate, procedure);
            }
            catch (ParseException e) {
                LOG.error("Error converting radiation procedure into timeline radiation record: " +
                        dmpPatientId + ", " + procedure.getPlanName());
                continue;
            }
            timelineRadiationRecords.add(record);
        }
        return timelineRadiationRecords;
    }
}
