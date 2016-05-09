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

package org.cbioportal.cmo.pipelines.crdb;

import org.cbioportal.cmo.pipelines.crdb.model.CRDBDataset;

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
 * Class for querying the CRDB Dataset view.
 * 
 * @author ochoaa
 */

public class CRDBDatasetReader implements ItemStreamReader<CRDBDataset> {
    @Value("${crdb.dataset_view}")
    private String crdbDatasetView;
    
    @Autowired
    SQLQueryFactory crdbQueryFactory;    

    private List<CRDBDataset> crdbDatasetResults;    
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.crdbDatasetResults = getCrdbDatasetResults();                
    }
    
    /**
     * Creates an alias for the CRDB Dataset view query type and projects query as
     * a list of CRDBDataset objects
     * 
     * @return List<CRDBDataset>
     */    
    @Transactional
    private List<CRDBDataset> getCrdbDatasetResults() {
        System.out.println("Beginning CRDB Dataset View import...");
        
        CRDBDataset qCRDBD = alias(CRDBDataset.class, crdbDatasetView);  
        List<CRDBDataset> crdbDatasetResults = crdbQueryFactory.select(
                Projections.constructor(CRDBDataset.class, $(qCRDBD.getDMP_ID()), 
                    $(qCRDBD.getCONSENT_DATE_DAYS()), $(qCRDBD.getPRIM_DISEASE_12245()), 
                    $(qCRDBD.getINITIAL_SX_DAYS()), $(qCRDBD.getINITIAL_DX_DAYS()), 
                    $(qCRDBD.getFIRST_METASTASIS_DAYS()), $(qCRDBD.getINIT_DX_STATUS_ID()), 
                    $(qCRDBD.getINIT_DX_STATUS()), $(qCRDBD.getINIT_DX_STATUS_DAYS()), 
                    $(qCRDBD.getINIT_DX_STAGING_DSCRP()), $(qCRDBD.getINIT_DX_STAGE()), 
                    $(qCRDBD.getINIT_DX_STAGE_DSCRP()), $(qCRDBD.getINIT_DX_GRADE()), 
                    $(qCRDBD.getINIT_DX_GRADE_DSCRP()), $(qCRDBD.getINIT_DX_T_STAGE()), 
                    $(qCRDBD.getINIT_DX_T_STAGE_DSCRP()), $(qCRDBD.getINIT_DX_N_STAGE()), 
                    $(qCRDBD.getINIT_DX_N_STAGE_DSCRP()), $(qCRDBD.getINIT_DX_M_STAGE()), 
                    $(qCRDBD.getINIT_DX_M_STAGE_DSCRP()), $(qCRDBD.getINIT_DX_HIST()), 
                    $(qCRDBD.getINIT_DX_SUB_HIST()), $(qCRDBD.getINIT_DX_SUB_SUB_HIST()), 
                    $(qCRDBD.getINIT_DX_SUB_SUB_SUB_HIST()), $(qCRDBD.getINIT_DX_SITE()), 
                    $(qCRDBD.getINIT_DX_SUB_SITE()), $(qCRDBD.getINIT_DX_SUB_SUB_SITE()), 
                    $(qCRDBD.getENROLL_DX_STATUS_ID()), $(qCRDBD.getENROLL_DX_STATUS()), 
                    $(qCRDBD.getENROLL_DX_STATUS_DAYS()), $(qCRDBD.getENROLL_DX_STAGING_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_STAGE()), $(qCRDBD.getENROLL_DX_STAGE_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_GRADE()), $(qCRDBD.getENROLL_DX_GRADE_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_T_STAGE()), $(qCRDBD.getENROLL_DX_T_STAGE_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_N_STAGE()), $(qCRDBD.getENROLL_DX_N_STAGE_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_M_STAGE()), $(qCRDBD.getENROLL_DX_M_STAGE_DSCRP()), 
                    $(qCRDBD.getENROLL_DX_HIST()), $(qCRDBD.getENROLL_DX_SUB_HIST()), 
                    $(qCRDBD.getENROLL_DX_SUB_SUB_HIST()), $(qCRDBD.getENROLL_DX_SUB_SUB_SUB_HIST()), 
                    $(qCRDBD.getENROLL_DX_SITE()), $(qCRDBD.getENROLL_DX_SUB_SITE()), 
                    $(qCRDBD.getENROLL_DX_SUB_SUB_SITE()), $(qCRDBD.getSURVIVAL_STATUS()), 
                    $(qCRDBD.getTREATMENT_END_DAYS()), $(qCRDBD.getOFF_STUDY_DAYS()), 
                    $(qCRDBD.getCOMMENTS())))
                .from($(qCRDBD))
                .fetch();

        System.out.println("Imported "+crdbDatasetResults.size()+" records from CRDB Dataset View.");
        return crdbDatasetResults;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBDataset read() throws Exception {
        if (!crdbDatasetResults.isEmpty()) {            
            return crdbDatasetResults.remove(0);            
        }
        return null;
    } 
}
