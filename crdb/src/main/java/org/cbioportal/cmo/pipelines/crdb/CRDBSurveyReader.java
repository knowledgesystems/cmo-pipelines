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

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;

import org.cbioportal.cmo.pipelines.crdb.model.CRDBSurvey;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.jdbc.pool.OracleDataSource;


/**
 * @author ochoaa
 */
public class CRDBSurveyReader implements ItemStreamReader<CRDBSurvey> {
    @Value("${crdb.survey_view}")
    private String crdbSurveyView;

    @Value("${crdb.username}")
    private String username;
    
    @Value("${crdb.password}")
    private String password;
    
    @Value("${crdb.connection_string}")
    private String connection_string;
    
    private List<CRDBSurvey> crdbSurvey;    
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        //read from CRDB view 
        OracleDataSource crdbDataSource = null;
        try {
            crdbDataSource = getCrdbDataSource();
        } catch (SQLException ex) {
            Logger.getLogger(CRDBSurveyReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.crdbSurvey = getCrdbSurveyResults(crdbDataSource);                
    }
    
    @Transactional
    private List<CRDBSurvey> getCrdbSurveyResults(OracleDataSource crdbDataSource) {
        System.out.println("Beginning CRDB Survey View import...");
        
        SQLTemplates templates = new OracleTemplates();
        com.querydsl.sql.Configuration config = new com.querydsl.sql.Configuration(templates);
        SQLQueryFactory queryFactory = new SQLQueryFactory(config, crdbDataSource);
        
        CRDBSurvey qCRDBS = alias(CRDBSurvey.class, crdbSurveyView);  
        List<CRDBSurvey> crdbSurveyResults = queryFactory.select(Projections.constructor(CRDBSurvey.class, $(qCRDBS.getDMP_ID()), $(qCRDBS.getQS_DATE()), $(qCRDBS.getADJ_TXT()),
                $(qCRDBS.getNOSYSTXT()), $(qCRDBS.getPRIOR_RX()), $(qCRDBS.getBRAINMET()), $(qCRDBS.getECOG()), $(qCRDBS.getCOMMENTS()))).from($(qCRDBS)).fetch();
        Integer numRows = crdbSurveyResults.size();
                             
        System.out.println("Imported "+numRows+" records from CRDB Survey View.");
        return crdbSurveyResults;
    }

    private OracleDataSource getCrdbDataSource() throws SQLException {
        OracleDataSource crdbDataSource = new OracleDataSource();
        crdbDataSource.setUser(username);
        crdbDataSource.setPassword(password);
        crdbDataSource.setURL(connection_string);
        return crdbDataSource;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CRDBSurvey read() throws Exception {
        if (!crdbSurvey.isEmpty()) {            
            return crdbSurvey.remove(0);            
        }
        return null;
    } 
}
