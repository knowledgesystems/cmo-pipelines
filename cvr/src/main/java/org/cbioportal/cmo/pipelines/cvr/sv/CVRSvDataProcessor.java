/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.sv;

import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class CVRSvDataProcessor implements ItemProcessor<CVRSvRecord, CompositeSvRecord> {

    @Autowired
    private CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Override
    public CompositeSvRecord process(CVRSvRecord i) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : i.getFieldNames()) {
            String queryField = field;
            if (field == "Class") {
                queryField = "SV_Class";
            }
            record.add(cvrUtilities.convertWhitespace(i.getClass().getMethod("get" + queryField).invoke(i).toString()));
        }
        CompositeSvRecord compRecord = new CompositeSvRecord();
        if (cvrSampleListUtil.getNewDmpSamples().contains(i.getSample_ID())) {
            compRecord.setNewSvRecord(StringUtils.join(record, "\t").trim());
        } else {
            compRecord.setOldSvRecord(StringUtils.join(record, "\t").trim());
        }
        return compRecord;
    }
}
