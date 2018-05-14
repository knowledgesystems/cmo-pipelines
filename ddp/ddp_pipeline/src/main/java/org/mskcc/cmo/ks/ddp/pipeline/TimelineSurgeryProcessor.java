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

import org.mskcc.cmo.ks.ddp.source.model.Surgery;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineSurgeryRecord;
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
public class TimelineSurgeryProcessor implements ItemProcessor<DDPCompositeRecord, List<String>> {

    private final Logger LOG = Logger.getLogger(TimelineSurgeryProcessor.class);

    @Override
    public List<String> process(DDPCompositeRecord compositeRecord) throws Exception {
        // we can't generate surgery timeline records without a reference date or if patient
        // hasn't had any surgery procedures - return null
        String firstTumorDiagnosisDate = DDPUtils.getFirstTumorDiagnosisDate(compositeRecord.getPatientDiagnosis());
        if (Strings.isNullOrEmpty(firstTumorDiagnosisDate) ||
                compositeRecord.getSurgicalProcedures()== null ||
                compositeRecord.getSurgicalProcedures().isEmpty()) {
            return null;
        }
        // convert surgery procedures into timeline records
        List<TimelineSurgeryRecord> timelineSurgeryRecords = convertTimelineSurgeryRecords(compositeRecord.getDmpPatientId(), firstTumorDiagnosisDate, compositeRecord.getSurgicalProcedures());
        if (timelineSurgeryRecords ==  null || timelineSurgeryRecords.isEmpty()) {
            LOG.error("Failed to convert any surgical procedures into timeline records for patient: " + compositeRecord.getDmpPatientId());
            return null;
        }
        // construct records into strings for writing to output file
        List<String> records = new ArrayList<>();
        for (TimelineSurgeryRecord surgeryRecord : timelineSurgeryRecords) {
            try {
                records.add(DDPUtils.constructRecord(surgeryRecord));
            }
            catch (NullPointerException e) {
                LOG.error("Error converting surgery record to timeline record string: " + surgeryRecord.toString());
            }
        }
        return records;
    }

    /**
     * Converts surgery procedures into timeline surgery records.
     *
     * @param dmpPatientId
     * @param firstTumorDiagnosisDate
     * @param surgicalProcedures
     * @return
     */
    private List<TimelineSurgeryRecord> convertTimelineSurgeryRecords(String dmpPatientId, String firstTumorDiagnosisDate, List<Surgery> surgicalProcedures) {
        List<TimelineSurgeryRecord> timelineSurgeryRecords = new ArrayList<>();
        for (Surgery procedure : surgicalProcedures) {
            TimelineSurgeryRecord record;
            try {
                record = new TimelineSurgeryRecord(dmpPatientId, firstTumorDiagnosisDate, procedure);
            }
            catch (ParseException e) {
                LOG.error("Error converting surgical procedure into timeline surgery record: " +
                        dmpPatientId + ", " + procedure.getProcedureDescription());
                continue;
            }
            timelineSurgeryRecords.add(record);
        }
        return timelineSurgeryRecords;
    }
}
