/*
 * Copyright (c) 2017-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.gene.config;

import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.*;

/**
 *
 * @author ochoaa
 */
@Configuration
public class JdbcConfiguration {
    
    @Value("${db.user}")
    private String dbUser;
    
    @Value("${db.password}")
    private String dbPassword;
    
    @Value("${db.driver}")
    private String dbDriver;
        
    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.use_ssl:false}")
    private Boolean dbUseSsl;
    
    @Value("${DATABASE_NAME}")
    private String DATABASE_NAME;

    public DataSource mainDataSource() throws SQLException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName(dbDriver);        
        dataSource.setUrl(dbUrl + DATABASE_NAME + "?useSSL=" + String.valueOf(dbUseSsl));
        System.out.println(dataSource.getUrl());
        dataSource.setDefaultTransactionIsolation(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        return dataSource; 
    }

    /**
     * Bean that holds the named parameter jdbc template.
     * 
     * @return NamedParameterJdbcTemplate
     * @throws SQLException 
     */
    @Bean(name="namedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() throws SQLException {
        return new NamedParameterJdbcTemplate(mainDataSource());
    }
}
