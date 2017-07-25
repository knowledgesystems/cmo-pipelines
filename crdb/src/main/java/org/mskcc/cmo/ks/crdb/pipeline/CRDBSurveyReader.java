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

package org.mskcc.cmo.ks.crdb;

import org.mskcc.cmo.ks.crdb.model.CRDBSurvey;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Class for querying the CRDB Survey view.
 *
 * @author ochoaa
 */

public class CRDBSurveyReader implements ItemStreamReader<CRDBSurvey> {
    @Value("${crdb.survey_view}")
    private String crdbSurveyView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBSurvey> crdbSurveyResults;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbSurveyResults = getCrdbSurveyResults();
    }

    /**
     * Creates an alias for the CRDB Survey view query type and projects query as
     * a list of CRDBSurvey objects
     *
     * @return List<CRDBSurvey>
     */
    @Transactional
    private List<CRDBSurvey> getCrdbSurveyResults() {
        System.out.println("Beginning CRDB Survey View import...");

        CRDBSurvey qCRDBS = alias(CRDBSurvey.class, crdbSurveyView);
        List<CRDBSurvey> crdbSurveyResults = new ArrayList<>();
        List<String> dmpIdList = crdbQueryFactory.selectDistinct($(qCRDBS.getDMP_ID())).from($(qCRDBS)).fetch();
        for (String dmpId : dmpIdList) {
            CRDBSurvey record = crdbQueryFactory.select(
                Projections.constructor(CRDBSurvey.class, $(qCRDBS.getDMP_ID()),
                        $(qCRDBS.getQS_DATE()), $(qCRDBS.getADJ_TXT()),
                        $(qCRDBS.getNOSYSTXT()), $(qCRDBS.getPRIOR_RX()),
                        $(qCRDBS.getBRAINMET()), $(qCRDBS.getECOG()),
                        $(qCRDBS.getCOMMENTS())))
                    .from($(qCRDBS))
                    .where($(qCRDBS.getDMP_ID()).eq(dmpId))
                    .orderBy($(qCRDBS.getQS_DATE()).desc())
                    .fetchFirst();
            crdbSurveyResults.add(record);
        }

        System.out.println("Imported "+crdbSurveyResults.size()+" records from CRDB Survey View.");
        return crdbSurveyResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBSurvey read() throws Exception {
        if (!crdbSurveyResults.isEmpty()) {
            return crdbSurveyResults.remove(0);
        }
        return null;
    }
}
