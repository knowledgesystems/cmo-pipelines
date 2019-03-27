/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.crdb.pipeline;

import java.util.*;
import org.mskcc.cmo.ks.crdb.pipeline.model.CRDBPDXClinicalAnnotationMapping;
import org.mskcc.cmo.ks.crdb.pipeline.util.CRDBUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class for processing the CRDB Dataset results for the staging file.
 *
 * @author Avery Wang
 */

public class CRDBPDXClinicalAnnotationMappingProcessor implements ItemProcessor<CRDBPDXClinicalAnnotationMapping, String> {

    @Autowired
    private CRDBUtils crdbUtils;

    private List<String> CRDB_PDX_MAPPING_FIELD_ORDER = CRDBPDXClinicalAnnotationMapping.getFieldNames();

    @Override
    public String process(final CRDBPDXClinicalAnnotationMapping crdbPDXClinicalAnnotationMapping) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : CRDB_PDX_MAPPING_FIELD_ORDER) {
            String value = crdbPDXClinicalAnnotationMapping.getClass().getMethod("get" + field).invoke(crdbPDXClinicalAnnotationMapping).toString();
            record.add(crdbUtils.convertWhitespace(value));
        }
        return String.join("\t", record);
    }
}
