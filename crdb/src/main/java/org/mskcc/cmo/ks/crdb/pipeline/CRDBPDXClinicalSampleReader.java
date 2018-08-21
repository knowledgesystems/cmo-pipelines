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

import org.mskcc.cmo.ks.crdb.model.CRDBPDXClinicalSampleDataset;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.ConstantImpl; // TODO: delete this when no longer needed
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Class for querying the CRDB PDX Clinical Sample Dataset view.
 *
 * @author ochoaa
 */

public class CRDBPDXClinicalSampleReader implements ItemStreamReader<CRDBPDXClinicalSampleDataset> {
    @Value("${crdb.pdx_clinical_sample_dataset_view}")
    private String crdbPDXClinicalSampleDatasetView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBPDXClinicalSampleDataset> crdbPDXClinicalSampleDatasetResults;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbPDXClinicalSampleDatasetResults = getCrdbPDXClinicalSampleDatasetResults();
        if (crdbPDXClinicalSampleDatasetResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from CRDB PDX Clinical Sample Dataset View");
        }
    }

    /**
     * Creates an alias for the CRDB PDX Clinical Sample Dataset view query type and projects query as
     * a list of CRDBPDXClinicalSampleDataset objects
     *
     * @return List<CRDBPDXClinicalSampleDataset>
     */
    @Transactional
    private List<CRDBPDXClinicalSampleDataset> getCrdbPDXClinicalSampleDatasetResults() {
        System.out.println("Beginning CRDB PDX Clinical Sample Dataset View import...");

        CRDBPDXClinicalSampleDataset qCRDBD = alias(CRDBPDXClinicalSampleDataset.class, crdbPDXClinicalSampleDatasetView);
        List<CRDBPDXClinicalSampleDataset> crdbPDXClinicalSampleDatasetResults = crdbQueryFactory.selectDistinct(
                Projections.constructor(CRDBPDXClinicalSampleDataset.class, $(qCRDBD.getPATIENT_ID()), $(qCRDBD.getSAMPLE_ID()), $(qCRDBD.getPDX_ID()),
                                        $(qCRDBD.getDESTINATION_STUDY_ID()),
                                        ConstantImpl.create(""), //$(qCRDBD.getCOLLAB_ID()), // not yet available : when available, update model class too and drop empty string arg
                                        $(qCRDBD.getAGE_AT_INITIAL_DIAGNOSIS()), $(qCRDBD.getPASSAGE_ID()),
                                        $(qCRDBD.getONCOTREE_CODE()), $(qCRDBD.getSTAGE_CODE()), $(qCRDBD.getT_STAGE()), $(qCRDBD.getN_STAGE()),
                                        $(qCRDBD.getM_STAGE()), $(qCRDBD.getGRADE()), $(qCRDBD.getSAMPLE_TYPE()), $(qCRDBD.getPRIMARY_SITE()),
                                        $(qCRDBD.getSAMPLE_CLASS()), $(qCRDBD.getPROCEDURE_TYPE()), $(qCRDBD.getPRETREATED()), $(qCRDBD.getTREATED()),
                                        $(qCRDBD.getER_POSITIVE()), $(qCRDBD.getER_NEGATIVE()), $(qCRDBD.getHER2_POSITIVE()), $(qCRDBD.getHER2_NEGATIVE()),
                                        $(qCRDBD.getHPV_POSITIVE()), $(qCRDBD.getHPV_NEGATIVE()), $(qCRDBD.getP16_POSITIVE()), $(qCRDBD.getP16_NEGATIVE()),
                                        $(qCRDBD.getPR_POSITIVE()), $(qCRDBD.getPR_NEGATIVE()), $(qCRDBD.getIDH1_POSITIVE()), $(qCRDBD.getIDH1_NEGATIVE()),
                                        $(qCRDBD.getIDH2_POSITIVE()), $(qCRDBD.getIDH2_NEGATIVE()), $(qCRDBD.getEGFR_POSITIVE()), $(qCRDBD.getEGFR_NEGATIVE()),
                                        $(qCRDBD.getALK_POSITIVE()), $(qCRDBD.getALK_NEGATIVE()), $(qCRDBD.getBRCA1_POSITIVE()), $(qCRDBD.getBRCA1_NEGATIVE()),
                                        $(qCRDBD.getBRCA2_POSITIVE()), $(qCRDBD.getBRCA2_NEGATIVE()), $(qCRDBD.getC_MYC_POSITIVE()), $(qCRDBD.getC_MYC_NEGATIVE()),
                                        $(qCRDBD.getAR_POSITIVE()), $(qCRDBD.getAR_NEGATIVE()), $(qCRDBD.getKRAS_POSITIVE()), $(qCRDBD.getKRAS_NEGATIVE())))
                .from($(qCRDBD))
                .fetch();
        System.out.println("Imported " + crdbPDXClinicalSampleDatasetResults.size() + " records from CRDB PDX Clinical Sample Dataset View.");
        return crdbPDXClinicalSampleDatasetResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBPDXClinicalSampleDataset read() throws Exception {
        if (!crdbPDXClinicalSampleDatasetResults.isEmpty()) {
            return crdbPDXClinicalSampleDatasetResults.remove(0);
        }
        return null;
    }
}
