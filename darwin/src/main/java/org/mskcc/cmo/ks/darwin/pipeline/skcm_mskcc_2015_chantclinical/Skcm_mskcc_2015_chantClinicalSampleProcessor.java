/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical;

import org.mskcc.cmo.ks.darwin.pipeline.model.*;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cmo.ks.redcap.source.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantClinicalSampleProcessor implements ItemProcessor<Skcm_mskcc_2015_chantClinicalRecord, Skcm_mskcc_2015_chantClinicalCompositeRecord> {
    @Autowired
    public MetadataManager metadataManager;
    @Autowired
     public ClinicalDataSource clinicalDataSource;
    @Override
    public Skcm_mskcc_2015_chantClinicalCompositeRecord process(final Skcm_mskcc_2015_chantClinicalRecord melanomaClinicalRecord) throws Exception{
        List<String> header =  melanomaClinicalRecord.getFieldNames();
        Map<String, List<String>> fullHeader = metadataManager.getFullHeader(header);              
        
        List<String> record = new ArrayList<>();
        for (String field : melanomaClinicalRecord.getFieldNames()) {
            String value = melanomaClinicalRecord.getClass().getMethod("get" + field).invoke(melanomaClinicalRecord).toString();
            Set<String> uniqueValues = new HashSet(Arrays.asList(value.split("\\|")));            
            List<String> values = Arrays.asList(value.split("\\|"));
            if (fullHeader.get("attribute_types").get(header.indexOf(field)).equals("SAMPLE") || field.equals("PATIENT_ID")) {
                if (uniqueValues.size() == 1) {
                    record.add(values.get(0));
                }
                else {
                    record.add(value);
                 }            
            }
        }
        Skcm_mskcc_2015_chantClinicalCompositeRecord compositeRecord = new Skcm_mskcc_2015_chantClinicalCompositeRecord(melanomaClinicalRecord);
        compositeRecord.setSampleRecord(StringUtils.join(record, "\t"));
        return compositeRecord;
    }
}
