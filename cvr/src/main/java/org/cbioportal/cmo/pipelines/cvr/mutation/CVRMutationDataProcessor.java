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

package org.cbioportal.cmo.pipelines.cvr.mutation;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.cbioportal.models.AnnotatedRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class CVRMutationDataProcessor implements ItemProcessor<AnnotatedRecord, String> {
    
    @Value("#{stepExecutionContext['mutation_header']}")
    private List<String> header;

    @Override
    public String process(AnnotatedRecord i) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : header) {
            try {
                record.add(i.getClass().getMethod("get" + field).invoke(i).toString().replaceAll("[\\t\\n\\r]+"," "));
            } catch (Exception e) {
                record.add(i.getAdditionalProperties().get(field).replace("\r\n", " ").replaceAll("[\\t\\n\\r]+"," "));
            }
        }
        return StringUtils.join(record, "\t");
    }
}
