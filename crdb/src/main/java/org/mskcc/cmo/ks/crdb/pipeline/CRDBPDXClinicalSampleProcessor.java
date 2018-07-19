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

package org.mskcc.cmo.ks.crdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.ks.crdb.model.CRDBPDXClinicalSampleDataset;
import org.mskcc.cmo.ks.crdb.pipeline.util.CRDBUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.item.ItemProcessor;

/**
 * Class for processing the CRDB Dataset results for the staging file.
 *
 * @author ochoaa
 */

public class CRDBPDXClinicalSampleProcessor implements ItemProcessor<CRDBPDXClinicalSampleDataset, String> {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CRDBUtils crdbUtils;

    @Override
    public String process(final CRDBPDXClinicalSampleDataset crdbPDXClinicalSampleDataset) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : new CRDBPDXClinicalSampleDataset().getFieldNames()) {
            String value;
            if (field.equals("PARTA_CONSENTED")) {
                // resolve part a consented value
                value = resolvePartAConsented(crdbPDXClinicalSampleDataset.getPATIENT_ID());
            } else {
                value = crdbPDXClinicalSampleDataset.getClass().getMethod("get"+field).invoke(crdbPDXClinicalSampleDataset).toString();
            }
            record.add(crdbUtils.convertWhitespace(value));
        }
        return String.join("\t", record);
    }

    /**
     * Resolves the value for the PARTA_CONSENTED field.
     * @param consentDays
     * @return
     */
    private String resolvePartAConsented(String consentDays) {
        String value = "NO"; // default value is NO to minimize NA's in data

        // if consent days is larger than 0 then set value to YES, else set to NO
        try {
            if (Integer.valueOf(consentDays) > 0) {
                value = "YES";
            }
        }
        catch (NumberFormatException ex) {}

        return value;
    }
}
