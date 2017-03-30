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
package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class ClinicalSampleDataProcessor implements ItemProcessor<Map<String, String>, ClinicalDataComposite> {
    
    @Value("#{stepExecutionContext['sampleHeader']}")
    private Map<String, List<String>> total_header;
        
    @Override
    public ClinicalDataComposite process(Map<String, String> i) throws Exception {
        ClinicalDataComposite composite = new ClinicalDataComposite(i);
        List<String> record = new ArrayList();
        List<String> header = total_header.get("header");
        
        // get the sample and patient ids first before processing the other columns
        record.add(i.get("SAMPLE_ID"));
        record.add(i.get("PATIENT_ID"));       
        
        for(String column : header) {
            if(!column.equals("SAMPLE_ID")) {
                record.add(i.getOrDefault(column, ""));
            }
        }        
        
        composite.setSampleResult(StringUtils.join(record, "\t"));
        return composite;
    }       
}
