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

package org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical;

import java.util.*;
import org.mskcc.cmo.ks.darwin.pipeline.model.*;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantClinicalPatientProcessor implements ItemProcessor<Skcm_mskcc_2015_chantClinicalCompositeRecord, Skcm_mskcc_2015_chantClinicalCompositeRecord> {

    @Autowired
    private DarwinUtils darwinUtils;

    @Value("#{stepExecutionContext['patientHeader']}")
    private Map<String, List<String>> patientHeader;

    @Override
    public Skcm_mskcc_2015_chantClinicalCompositeRecord process(final Skcm_mskcc_2015_chantClinicalCompositeRecord melanomaClinicalCompositeRecord) throws Exception {
        Skcm_mskcc_2015_chantNormalizedClinicalRecord melanomaClinicalRecord = melanomaClinicalCompositeRecord.getRecord();
        List<String> header =  melanomaClinicalRecord.getFieldNames();
        List<String> record = new ArrayList<>();

        // first add patient id to record then iterate through rest of patient header
        record.add(darwinUtils.convertWhitespace(melanomaClinicalRecord.getPATIENT_ID()).split("\\|")[0]);
        for (int i=0; i<patientHeader.get("header").size(); i++) {
            String normColumn = patientHeader.get("header").get(i);
            if (normColumn.equals("PATIENT_ID")) {
                continue;
            }
            // get value by external column header (same as melanoma clinical record field name)
            // field data might contain '|'-delimited values - if only one unique
            // value then use that, otherwise just use the data that's there
            String value = melanomaClinicalRecord.getClass().getMethod("get" + normColumn).invoke(melanomaClinicalRecord).toString();
            Set<String> uniqueValues = new HashSet(Arrays.asList(darwinUtils.convertWhitespace(value).split("\\|")));
            List<String> values = Arrays.asList(value.split("\\|"));
            if (uniqueValues.size() == 1) {
                record.add(values.get(0));
            }
            else {
                record.add(value);
            }
        }
        melanomaClinicalCompositeRecord.setPatientRecord(String.join("\t", record));
        return melanomaClinicalCompositeRecord;
    }
}
