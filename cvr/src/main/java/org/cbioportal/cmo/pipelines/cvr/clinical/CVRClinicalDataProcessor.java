/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.MskimpactSeqDate;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class CVRClinicalDataProcessor implements ItemProcessor<CVRClinicalRecord, CompositeClinicalRecord> {

    @Autowired
    private CVRUtilities cvrUtilitiess;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    Logger log = Logger.getLogger(CVRClinicalDataProcessor.class);

    @Override
    public CompositeClinicalRecord process(CVRClinicalRecord i) throws Exception {
        if (i == null) return null;
        List<String> record = new ArrayList<>();
        List<String> seqDateRecord = new ArrayList<>();
        for (String field : CVRClinicalRecord.getFieldNames()) {
            try {
                record.add(cvrUtilitiess.convertWhitespace(i.getClass().getMethod("get" + field).invoke(i).toString().trim()));
            } catch (NullPointerException e) {
                log.error("Null pointer expection: " + field);
                record.add("");
            }
        }
        for (String field : MskimpactSeqDate.getFieldNames()) {
            seqDateRecord.add(cvrUtilitiess.convertWhitespace(i.getClass().getMethod("get" + field).invoke(i).toString().trim()));
        }
        CompositeClinicalRecord compRecord = new CompositeClinicalRecord();
        if (cvrSampleListUtil.getNewDmpSamples().contains(i.getSAMPLE_ID())) {
            compRecord.setNewClinicalRecord(String.join("\t", record));
        } else {
            compRecord.setOldClinicalRecord(String.join("\t", record));
        }
        compRecord.setSeqDateRecord(String.join("\t", seqDateRecord));
        return compRecord;
    }
}
