/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.crdb;

import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the CRDB query factory.
 * 
 * @author ochoaa
 */

@Configuration
public class CRDBConfiguration {
    @Value("${crdb.username}")
    private String username;
    
    @Value("${crdb.password}")
    private String password;
    
    @Value("${crdb.connection_string}")
    private String connection_string;
        
    @Bean
    public SQLQueryFactory crdbQueryFactory() throws SQLException {
        SQLTemplates templates = new OracleTemplates();
        com.querydsl.sql.Configuration config = new com.querydsl.sql.Configuration(templates);
        return  new SQLQueryFactory(config, crdbDataSource());
    }
    
    public OracleDataSource crdbDataSource() throws SQLException {
        OracleDataSource crdbDataSource = new OracleDataSource();        
        crdbDataSource.setUser(username);
        crdbDataSource.setPassword(password);
        crdbDataSource.setURL(connection_string);
        return crdbDataSource;
    }        
}
