/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.importer.tasklet;

import org.cbioportal.cmo.pipelines.importer.model.CancerStudy;

import java.io.*;
import java.util.*;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;


/**
 * Tasklet to import cancer study from meta_study.txt
 * @author ochoaa
 */
public class CancerStudyTasklet implements Tasklet {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private static final Log LOG = LogFactory.getLog(CancerStudyTasklet.class);
    
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        String metaFilename = stagingDirectory +  "meta_study.txt";
        CancerStudy cancerStudy = loadCancerStudy(metaFilename);
        
        if (cancerStudy == null) {
            return RepeatStatus.FINISHED;
        }
        
        CancerStudy existingStudy = getCancerStudy(cancerStudy.getCANCER_STUDY_IDENTIFIER());
        if (existingStudy != null) {
            LOG.info("Cancer study found with matching cancer study id: " + existingStudy.getCANCER_STUDY_IDENTIFIER());
            deleteCancerStudy(existingStudy.getCANCER_STUDY_ID());
        }

        LOG.info("Importing cancer study: " + cancerStudy.getCANCER_STUDY_IDENTIFIER());
        int assignedId = addCancerStudy(cancerStudy);

       // add cancer study id to job parameters
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("cancerStudyId", assignedId);
        
        return RepeatStatus.FINISHED;
    }

    /**
     * Load cancer study from meta_study.txt
     * @param metaFilename
     * @return
     * @throws IOException 
     */
    private CancerStudy loadCancerStudy(String metaFilename) throws IOException {
        File metaStudy = new File(metaFilename);
        Properties properties = new Properties();
        properties.load(new FileInputStream(metaStudy));

        CancerStudy newCancerStudy = null;
        try {
            newCancerStudy = new CancerStudy(properties);
        }
        catch (NullPointerException ex) {}
        
        if (newCancerStudy == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error loading " + metaFilename);
            }
            return null;
        }
        
        return newCancerStudy;
    }
    
    /**
     * insert cancer study record into CANCER_STUDY
     * @param cancerStudy
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException 
     */
    private int addCancerStudy(CancerStudy cancerStudy) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {   
        String SQL = "INSERT INTO cancer_study " + 
                "(cancer_study_identifier, type_of_cancer_id, name, short_name, description, public, pmid, citation, groups, status, import_date) " +
                "VALUES (:cancer_study_identifier, :type_of_cancer_id, :name, :short_name, :description, :public, :pmid, :citation, :groups, :status, :import_date)"; 
        
        Map<String, Object> namedParameters = new HashMap<>();
        for (String field : cancerStudy.getFieldNames()) {
            String columnName = field;
            if (field.contains("PUBLIC")) {
                columnName = "PUBLIC";
            }
            namedParameters.put(columnName.toLowerCase(), cancerStudy.getClass().getMethod("get"+field).invoke(cancerStudy));
        }        

        int rows = 0;
        int  cancerStudyId = -1;
        try{
            rows = namedParameterJdbcTemplate.update(SQL, namedParameters);            
            LOG.info("Successfully imported cancer study metadata for CANCER_STUDY_IDENTIFIER: " + cancerStudy.getCANCER_STUDY_IDENTIFIER() + 
                    "\nNumber of rows affected: " + rows);    
            
            cancerStudyId = getCancerStudy(cancerStudy.getCANCER_STUDY_IDENTIFIER()).getCANCER_STUDY_ID();
            LOG.info("Updating job parameters with cancer study id: " + cancerStudyId);
        }        
        catch (DataAccessException ex) {                        
            if (LOG.isErrorEnabled()) {
                LOG.error("Error importing CANCER_STUDY record with CANCER_STUDY_IDENTIFIER: " + cancerStudy.getCANCER_STUDY_IDENTIFIER() + ex.getMessage());
            }
        }        
        
        return cancerStudyId;
    }
    
    /** 
     * get cancer study record from CANCER_STUDY
     * @param cancerStudyIdentifier
     * @return 
     */
    private CancerStudy getCancerStudy(String cancerStudyIdentifier) {
        String SQL = "SELECT * FROM cancer_study WHERE cancer_study_identifier = :cancer_study_identifier";
        
        SqlParameterSource namedParameters = new MapSqlParameterSource("cancer_study_identifier", cancerStudyIdentifier);
        
        CancerStudy cancerStudy = null;
        try {
            cancerStudy = (CancerStudy) namedParameterJdbcTemplate.queryForObject(SQL, namedParameters, new BeanPropertyRowMapper(CancerStudy.class));
        }
        catch (DataAccessException ex) {}
        
        return cancerStudy;
    }
    
    /**
     * delete cancer study record from CANCER_STUDY
     * @param cancerStudyId 
     */
    private void deleteCancerStudy(int cancerStudyId) {
        String SQL = "DELETE FROM cancer_study WHERE cancer_study_id = :cancer_study_id";
        
        SqlParameterSource namedParameters = new MapSqlParameterSource("cancer_study_id", cancerStudyId);
        LOG.info("Deleting CANCER_STUDY record with CANCER_STUDY_ID: " + cancerStudyId);
        try {
            namedParameterJdbcTemplate.update(SQL, namedParameters);            
        }
        catch (DataAccessException ex) {
            if (LOG.isErrorEnabled()) {
                LOG.error(ex.getMessage());
            }
        }        
    }

}
