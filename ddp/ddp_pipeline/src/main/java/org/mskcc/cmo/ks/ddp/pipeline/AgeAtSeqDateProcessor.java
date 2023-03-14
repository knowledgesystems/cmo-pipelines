/*
 * Copyright (c) 2023 Memorial Sloan Kettering Cancer Center.
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
import org.mskcc.cmo.ks.ddp.pipeline.model.AgeAtSeqDateRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import java.util.*;
import java.text.ParseException;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 *
 * @author Manda Wilson and Calla Chennault
 */
@Component
public class AgeAtSeqDateProcessor implements ItemProcessor<DDPCompositeRecord, List<String>> {

    private final Logger LOG = Logger.getLogger(AgeAtSeqDateProcessor.class);

    @Override
    public List<String> process(DDPCompositeRecord compositeRecord) throws Exception {
        List<AgeAtSeqDateRecord> ageAtSeqDateRecords = convertAgeAtSeqDateRecord(compositeRecord.getDmpPatientId(), compositeRecord.getDmpSampleIds(), compositeRecord.getPatientBirthDate());
        // construct records into strings for writing to output file
        List<String> records = new ArrayList<>();
        for (AgeAtSeqDateRecord ageAtSeqDateRecord : ageAtSeqDateRecords) {
            try {
                records.add(DDPUtils.constructRecord(ageAtSeqDateRecord));
            }
            catch (NullPointerException e) {
                continue;
            }
        }
        return records;
    }

    /**
     * Create age at seq records.
     *
     * @param sampleId
     * @return
     */
    private List<AgeAtSeqDateRecord> convertAgeAtSeqDateRecord(String patientId, List<String> sampleIds, String patientBirthDate) {
        List<AgeAtSeqDateRecord> ageAtSeqDateRecords = new ArrayList<>();
        for (String sampleId : sampleIds) {
            try {
                ageAtSeqDateRecords.add(new AgeAtSeqDateRecord(patientId, sampleId, patientBirthDate));
            }
            catch (ParseException e) {
                continue;
            }
        }
        return ageAtSeqDateRecords;
    }
}
