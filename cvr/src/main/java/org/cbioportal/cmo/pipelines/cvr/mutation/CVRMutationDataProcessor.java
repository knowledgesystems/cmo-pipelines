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

package org.cbioportal.cmo.pipelines.cvr.mutation;

import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.models.AnnotatedRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class CVRMutationDataProcessor implements ItemProcessor<AnnotatedRecord, String> {

    @Value("#{stepExecutionContext['mutationHeader']}")
    private List<String> header;

    @Autowired
    private CVRUtilities cvrUtilities;

    private final String REFERENCE_ALLELE_COLUMN = "REFERENCE_ALLELE";
    private final String TUMOR_SEQ_ALLELE1_COLUMN = "TUMOR_SEQ_ALLELE1";

    @Override
    public String process(AnnotatedRecord i) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : header) {
            // always override 'Tumor_Seq_Allele1' with value of 'Reference_Allele'
            // these fields should always match for our internal datasets
            if (field.equalsIgnoreCase(TUMOR_SEQ_ALLELE1_COLUMN)) {
                field = REFERENCE_ALLELE_COLUMN;
            }
            try {
                record.add(cvrUtilities.convertWhitespace(i.getClass().getMethod("get" + field.toUpperCase()).invoke(i).toString()));
            } catch (Exception e) {
                record.add(cvrUtilities.convertWhitespace(i.getAdditionalProperties().getOrDefault(field, "")));
            }
        }
        return String.join("\t", record);
    }
}
