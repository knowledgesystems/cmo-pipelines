/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.crdb.model.CRDBPDXTimelineDataset;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Class for querying the CRDB PDX Timeline Dataset view.
 *
 * @author ochoaa
 */

public class CRDBPDXTimelineReader implements ItemStreamReader<CRDBPDXTimelineDataset> {
    @Value("${crdb.pdx_timeline_dataset_view}")
    private String crdbPDXTimelineDatasetView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBPDXTimelineDataset> crdbTimelineDatasetResults;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbTimelineDatasetResults = getCrdbTimelineDatsetResults();
        if (crdbTimelineDatasetResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from CRDB PDX Timeline Dataset View");
        }
    }

    /**
     * Creates an alias for the CRDB PDX Timeline Dataset view query type and projects query as
     * a list of CRDBPDXTimelineDataset objects
     *
     * @return List<CRDBPDXTimelineDataset>
     */
    @Transactional
    private List<CRDBPDXTimelineDataset> getCrdbTimelineDatsetResults() {
        System.out.println("Beginning CRDB PDX Timeline Dataset View import...");

        CRDBPDXTimelineDataset qCRDBD = alias(CRDBPDXTimelineDataset.class, crdbPDXTimelineDatasetView);
        List<CRDBPDXTimelineDataset> crdbTimelineDatasetResults = crdbQueryFactory.selectDistinct(
                Projections.constructor(CRDBPDXTimelineDataset.class, $(qCRDBD.getPATIENT_ID()), $(qCRDBD.getSAMPLE_ID()), $(qCRDBD.getPDX_ID()),
                                        $(qCRDBD.getDESTINATION_STUDY_ID()), $(qCRDBD.getSTART_DATE()), $(qCRDBD.getSTOP_DATE()), $(qCRDBD.getEVENT_TYPE()),
                                        $(qCRDBD.getPASSAGE_ID()), $(qCRDBD.getTREATMENT_TYPE()), $(qCRDBD.getSUBTYPE()), $(qCRDBD.getAGENT()),
                                        $(qCRDBD.getRESPONSE()), $(qCRDBD.getRESPONSE_DURATION_MONTHS()), $(qCRDBD.getREASON_FOR_TX_DISCONTINUATION()),
                                        $(qCRDBD.getSURGERY_DETAILS()), $(qCRDBD.getEVENT_TYPE_DETAILED()), $(qCRDBD.getPROCEDURE_LOCATION()),
                                        $(qCRDBD.getPROCEDURE_LOCATION_SPECIFY()), $(qCRDBD.getDIAGNOSTIC_TYPE()), $(qCRDBD.getDIAGNOSTIC_TYPE_SITE()),
                                        $(qCRDBD.getIMAGING()), $(qCRDBD.getSPECIMEN_TYPE()), $(qCRDBD.getSPECIMEN_SITE()), $(qCRDBD.getAGE_AT_PROCEDURE()),
                                        $(qCRDBD.getLATERALITY()), $(qCRDBD.getDISEASE_STATUS()), $(qCRDBD.getMETASTATIC_SITE()),
                                        $(qCRDBD.getTIME_TO_METASTASIS_MONTHS()), $(qCRDBD.getSAMPLE_TYPE()), $(qCRDBD.getSITE_OF_RECURRENCE()),
                                        $(qCRDBD.getTIME_TO_RECURRENCE())))
                .from($(qCRDBD))
                .fetch();
        System.out.println("Imported " + crdbTimelineDatasetResults.size() + " records from CRDB PDX Timeline Dataset View.");
        return crdbTimelineDatasetResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBPDXTimelineDataset read() throws Exception {
        if (!crdbTimelineDatasetResults.isEmpty()) {
            return crdbTimelineDatasetResults.remove(0);
        }
        return null;
    }
}
