/*
 * Copyright (c) 2016 - 2018 Memorial Sloan-Kettering Cancer Center.
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
import org.mskcc.cmo.ks.crdb.pipeline.model.CRDBSurvey;
import org.mskcc.cmo.ks.crdb.pipeline.util.CRDBUtils;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class for processing the CRDB Survey results for the staging file.
 *
 * @author ochoaa
 */

public class CRDBSurveyProcessor implements ItemProcessor<CRDBSurvey, String> {

    @Autowired
    private CRDBUtils crdbUtils;

    private List<String> CRDB_SURVEY_FIELD_ORDER = CRDBSurvey.getFieldNames();

    @Override
    public String process(final CRDBSurvey crdbSurvey) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : CRDB_SURVEY_FIELD_ORDER) {
            if (!field.startsWith("QS_DATE")) {
                String value = crdbSurvey.getClass().getMethod("get" + field).invoke(crdbSurvey).toString();
                record.add(crdbUtils.convertWhitespace(value));
            }
        }
        return String.join("\t", record);
    }
}
