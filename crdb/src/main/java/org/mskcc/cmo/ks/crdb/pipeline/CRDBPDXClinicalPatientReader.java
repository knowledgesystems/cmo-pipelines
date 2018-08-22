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

import org.mskcc.cmo.ks.crdb.model.CRDBPDXClinicalPatientDataset;

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
 * Class for querying the CRDB Dataset view.
 *
 * @author ochoaa
 */

public class CRDBPDXClinicalPatientReader implements ItemStreamReader<CRDBPDXClinicalPatientDataset> {
    @Value("${crdb.pdx_clinical_patient_dataset_view}")
    private String crdbPDXClinicalPatientDatasetView;

    @Autowired
    SQLQueryFactory crdbQueryFactory;

    private List<CRDBPDXClinicalPatientDataset> crdbPDXClinicalPatientDatasetResults;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbPDXClinicalPatientDatasetResults = getCrdbPDXClinicalPatientDatasetResults();
        if (crdbPDXClinicalPatientDatasetResults.isEmpty()) {
            throw new ItemStreamException("Error fetching records from CRDB PDX Clinical Patient Dataset View");
        }
    }

    /**
     * Creates an alias for the CRDB Dataset view query type and projects query as
     * a list of CRDBPDXClinicalPatientDataset objects
     *
     * @return List<CRDBPDXClinicalPatientDataset>
     */
    @Transactional
    private List<CRDBPDXClinicalPatientDataset> getCrdbPDXClinicalPatientDatasetResults() {
        System.out.println("Beginning CRDB PDX Clinical Patient Dataset View import...");

        CRDBPDXClinicalPatientDataset qCRDBD = alias(CRDBPDXClinicalPatientDataset.class, crdbPDXClinicalPatientDatasetView);
        List<CRDBPDXClinicalPatientDataset> crdbPDXClinicalPatientDatasetResults = crdbQueryFactory.selectDistinct(
                Projections.constructor(CRDBPDXClinicalPatientDataset.class, $(qCRDBD.getPATIENT_ID()), $(qCRDBD.getDESTINATION_STUDY_ID()),
                                        $(qCRDBD.getSEX()), $(qCRDBD.getETHNICITY()), $(qCRDBD.getRACE()), $(qCRDBD.getSMOKING_HISTORY()),
                                        $(qCRDBD.getCROHN_DISEASE()), $(qCRDBD.getULCERATIVE_COLITIS()), $(qCRDBD.getBARRETTS_ESOPHAGUS()),
                                        $(qCRDBD.getH_PYLORI()),
                                        ConstantImpl.create(""), //$(qCRDBD.getMDS_RISK_FACTOR()), // not yet available : when available, update model class too and drop empty string arg
                                        $(qCRDBD.getMENOPAUSE_STATUS()), $(qCRDBD.getUV_EXPOSURE()),
                                        $(qCRDBD.getRADIATION_THERAPY()), $(qCRDBD.getBREAST_IMPLANTS()), $(qCRDBD.getBRCA()),
                                        $(qCRDBD.getRETINOBLASTOMA()), $(qCRDBD.getGRADE_1()), $(qCRDBD.getGRADE_2()), $(qCRDBD.getGRADE_3()),
                                        $(qCRDBD.getPLATINUM_SENSITIVE()), $(qCRDBD.getPLATINUM_RESISTANT())))
                .from($(qCRDBD))
                .fetch();
        System.out.println("Imported " + crdbPDXClinicalPatientDatasetResults.size() + " records from CRDB PDX Clinical Patient Dataset View.");
        return crdbPDXClinicalPatientDatasetResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBPDXClinicalPatientDataset read() throws Exception {
        if (!crdbPDXClinicalPatientDatasetResults.isEmpty()) {
            return crdbPDXClinicalPatientDatasetResults.remove(0);
        }
        return null;
    }
}
