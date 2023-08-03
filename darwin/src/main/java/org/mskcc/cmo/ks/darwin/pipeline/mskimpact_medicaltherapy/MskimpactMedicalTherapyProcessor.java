/*
 * Copyright (c) 2017, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.mskcc.cmo.ks.darwin.pipeline.mskimpact_medicaltherapy;

import java.util.*;
import org.apache.commons.collections.map.MultiKeyMap;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactMedicalTherapy;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This processor takes a list of medical therapy records all for a single patient, grouped by
 * DMP-ID/drug-name/dosage/dose-unit/dispensed quantity and collapses them down into a single record
 * with a start/stop date based on a sort of the AGE_AT_DISPENSE_DATE_IN_DAYS values across all records.
 *
 * @author Benjamin Gross
 */
public class MskimpactMedicalTherapyProcessor implements ItemProcessor<List<MskimpactMedicalTherapy>, MskimpactMedicalTherapy> {

    @Autowired
    private DarwinUtils darwinUtils;

    /**
     * All items in the list are identical with the exception of AGE_AT_DISPENSE_DATE_IN_DAYS, the time at which
     * the drug was dispensed to the patient.
     */
    @Override
    public MskimpactMedicalTherapy process(final List<MskimpactMedicalTherapy> listMedicalTherapyRecords) throws Exception {
        checkConsistencyOfRecords(listMedicalTherapyRecords);
        Collections.sort(listMedicalTherapyRecords);
        MskimpactMedicalTherapy oldestRecord = listMedicalTherapyRecords.get(0);
        MskimpactMedicalTherapy youngestRecord = listMedicalTherapyRecords.get(listMedicalTherapyRecords.size()-1);
        // we can use any record in the list to populate all fields except for START_DATE, STOP_DATE
        return new MskimpactMedicalTherapy(darwinUtils.convertWhitespace(oldestRecord.getPT_ID_PHARMACY()),
                                           darwinUtils.convertWhitespace(oldestRecord.getDMP_ID_PHARMACY()),
                                           -1,
                                           darwinUtils.convertWhitespace(oldestRecord.getGENERIC_DRUG_NAME()),
                                           oldestRecord.getDOSAGE(),
                                           darwinUtils.convertWhitespace(oldestRecord.getDOSE_UNIT()),
                                           oldestRecord.getDISPENSED_QUANTITY(),
                                           oldestRecord.getAGE_AT_DISPENSE_DATE_IN_DAYS(),
                                           youngestRecord.getAGE_AT_DISPENSE_DATE_IN_DAYS(),
                                           oldestRecord.getSAMPLE_ID_PATH_DMP());
    }

    private void checkConsistencyOfRecords(List<MskimpactMedicalTherapy> listMedicalTherapyRecords) {
        MultiKeyMap byRecordAttributes = new MultiKeyMap();
        for (MskimpactMedicalTherapy record : listMedicalTherapyRecords) {
            byRecordAttributes.put(record.getDMP_ID_PHARMACY(), record.getGENERIC_DRUG_NAME(),
                                 record.getDOSAGE(), record.getDOSE_UNIT(), record.getDISPENSED_QUANTITY());
        }
        assert byRecordAttributes.keySet().size() == 1;
    }
}
