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

import org.mskcc.cmo.ks.ddp.source.model.Chemotherapy;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineChemoRecord;
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
public class TimelineChemoProcessor implements ItemProcessor<DDPCompositeRecord, List<String>> {

    private final Logger LOG = Logger.getLogger(TimelineChemoProcessor.class);

    @Override
    public List<String> process(DDPCompositeRecord compositeRecord) throws Exception {
        // we can't generate chemo timeline records without a reference date or if patient
        // hasn't had any chemo procedures - return null
        String firstTumorDiagnosisDate = DDPUtils.getFirstTumorDiagnosisDate(compositeRecord.getPatientDiagnosis());
        if (Strings.isNullOrEmpty(firstTumorDiagnosisDate) ||
                compositeRecord.getChemoProcedures()== null ||
                compositeRecord.getChemoProcedures().isEmpty()) {
            return null;
        }
        // convert chemo procedures into timeline records
        List<TimelineChemoRecord> timelineChemoRecords = convertTimelineChemoRecords(compositeRecord.getDmpPatientId(), firstTumorDiagnosisDate, compositeRecord.getChemoProcedures());
        if (timelineChemoRecords ==  null || timelineChemoRecords.isEmpty()) {
            LOG.error("Failed to convert any chemo procedures into timeline records for patient: " + compositeRecord.getDmpPatientId());
            return null;
        }
        // construct records into strings for writing to output file
        List<String> records = new ArrayList<>();
        for (TimelineChemoRecord chemoRecord : timelineChemoRecords) {
            try {
                records.add(DDPUtils.constructRecord(chemoRecord));
            }
            catch (NullPointerException e) {
                LOG.error("Error converting chemo record to timeline record string: " + chemoRecord.toString());
            }
        }
        return records;

    }

    /**
     * Converts chemo procedures into timeline chemo records.
     *
     * @param dmpPatientId
     * @param firstTumorDiagnosisDate
     * @param chemoProcedures
     * @return
     */
    private List<TimelineChemoRecord> convertTimelineChemoRecords(String dmpPatientId, String firstTumorDiagnosisDate, List<Chemotherapy> chemoProcedures) {
        List<TimelineChemoRecord> timelineChemoRecords = new ArrayList<>();
        for (Chemotherapy procedure : chemoProcedures) {
            TimelineChemoRecord record;
            try {
                record = new TimelineChemoRecord(dmpPatientId, firstTumorDiagnosisDate, procedure);
            }
            catch (ParseException e) {
                LOG.error("Error converting chemo procedure into timeline chemo record: " +
                        dmpPatientId + ", " + procedure.getORDNAME());
                continue;
            }
            timelineChemoRecords.add(record);
        }
        return timelineChemoRecords;
    }
}
