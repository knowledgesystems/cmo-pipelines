/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import com.ibm.db2.jcc.DB2SimpleDataSource;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.DB2Templates;
import java.sql.SQLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
/**
 *
 * @author jake
 */
@Configuration
@PropertySource("classpath:application.properties")
public class DarwinConfiguration {
    
    @Value("${darwin.username}")
    private String username;
        
    @Value("${darwin.password}")
    private String password;
    
    @Value("${darwin.server}")
    private String server;
    
    @Value("${darwin.port}")
    private Integer port;
    
    @Value("${darwin.database}")
    private String database;
    
    @Value("${darwin.schema}")
    private String schema;
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
        
    @Bean
    public SQLQueryFactory darwinQueryFactory() throws SQLException{
        DB2Templates templates = new DB2Templates();
        com.querydsl.sql.Configuration config = new com.querydsl.sql.Configuration(templates);
        return new SQLQueryFactory(config, darwinDataSource()); 
    }
    
    
    
    public DB2SimpleDataSource darwinDataSource(){
        DB2SimpleDataSource dataSource = new DB2SimpleDataSource();
        dataSource.setPortNumber(port);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setDatabaseName(database);
        dataSource.setCurrentSchema(schema);
        dataSource.setServerName(server);
        dataSource.setDriverType(4);
        return dataSource;
    }    
    
}
