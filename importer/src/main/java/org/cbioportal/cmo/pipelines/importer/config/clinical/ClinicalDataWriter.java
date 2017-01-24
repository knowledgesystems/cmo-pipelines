/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.config.clinical;

import org.cbioportal.cmo.pipelines.importer.model.*;
import org.cbioportal.cmo.pipelines.importer.config.composite.CompositeClinicalData;

import java.util.*;
import org.apache.commons.logging.*;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.*;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.*;

/**
 *
 * @author ochoaa
 */
public class ClinicalDataWriter implements ItemWriter<CompositeClinicalData> {
    
    @Autowired
    ClinicalDataProcessor clinicalDataProcessor;
    
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private static final Log LOG = LogFactory.getLog(ClinicalDataWriter.class);
    
    private ExecutionContext stepExecutionContext;
    @BeforeStep
    public void saveStepExecutionContext(StepExecution stepExecution) {
        this.stepExecutionContext = stepExecution.getExecutionContext();
    }
            
    private int patientCount;
    private int sampleCount;   
    private int patientAttributeCount;
    private int sampleAttributeCount;
    @Autowired
    public void setCounts() {
        this.patientCount = stepExecutionContext.getInt("patientCount");
        this.sampleCount = stepExecutionContext.getInt("sampleCount");
        this.patientAttributeCount = stepExecutionContext.getInt("patientAttributeCount");
        this.sampleAttributeCount = stepExecutionContext.getInt("sampleAttributeCount");
    }
    
    @AfterStep
    public void updateCounts(StepExecution stepExecution) {
        stepExecution.getExecutionContext().put("patientCount", this.patientCount);
        stepExecution.getExecutionContext().put("sampleCount", this.sampleCount);
        stepExecution.getExecutionContext().put("patientAttributeCount", this.patientAttributeCount);
        stepExecution.getExecutionContext().put("sampleAttributeCount", this.sampleAttributeCount);
    }
    
    @Override
    public void write(List<? extends CompositeClinicalData> list) throws Exception {
        List<ClinicalData> patientClinicalData = new ArrayList();
        List<ClinicalData> sampleClinicalData = new ArrayList();
        for (CompositeClinicalData ccd : list) {
            // update internal ids if necessary before adding to clinical data lists
            ccd = updateCompositeData(ccd);            
            patientClinicalData.addAll(ccd.getPatientClinicalData());
            sampleClinicalData.addAll(ccd.getSampleClinicalData());
        }
        
        if (!patientClinicalData.isEmpty()) {
            addClinicalDataBatch("clinical_patient", patientClinicalData);
        }
        if (!sampleClinicalData.isEmpty()) {
            addClinicalDataBatch("clinical_sample", sampleClinicalData);
        }
    }
    
    /**
     * update internal ids in composite clinical data
     * @param composite
     * @return
     * @throws Exception 
     */
    private CompositeClinicalData updateCompositeData(CompositeClinicalData composite) throws Exception {
        if (composite.getPatient().getINTERNAL_ID() != -1 && composite.getSample().getINTERNAL_ID() != -1) {
            return composite;
        }

        // insert new patient or sample record if not already in db
        if (composite.getPatient().getINTERNAL_ID() == -1) {
            int patientInternalId = addPatient(composite.getPatient());
            composite.updatePatientInternalId(patientInternalId);
            this.patientCount++;
        }
        if (composite.getSample().getINTERNAL_ID() == -1) {
            int sampleInternalId = addSample(composite.getSample());
            composite.updateSampleInternalId(sampleInternalId);
            this.sampleCount++;
        }
                
        return composite;
    }
    
    /**
     * insert sample record into SAMPLE
     * @param patient
     * @return 
     */
    private int addSample(Sample sample) throws Exception {
        String SQL = "INSERT INTO sample " + 
                "(stable_id, sample_type, patient_id, type_of_cancer_id) " + 
                "VALUES (:stable_id, :sample_type, :patient_id, :type_of_cancer_id)";
        Map<String, Object> namedParameters = new HashMap<>();
        for (String field : sample.getFieldNames()) {
            namedParameters.put(field.toLowerCase(), sample.getClass().getMethod("get"+field).invoke(sample));
        }
        
        int internalId = -1;
        try {
            namedParameterJdbcTemplate.update(SQL, namedParameters);
            internalId = clinicalDataProcessor.getSampleByPatient(sample.getSTABLE_ID(), sample.getPATIENT_ID()).getINTERNAL_ID();
        }
        catch (DataAccessException ex) {
            LOG.error("Could not import sample record: " + sample.getSTABLE_ID());
        }
        
        return internalId;
    }

    /**
     * insert patient record into PATIENT
     * @param patient
     * @return 
     */
    private int addPatient(Patient patient) {
        String SQL = "INSERT INTO patient (stable_id, cancer_study_id)" + 
                "VALUES (:stable_id, :cancer_study_id)";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("stable_id", patient.getSTABLE_ID());
        namedParameters.put("cancer_study_id", patient.getCANCER_STUDY_ID());
        
        int internalId = -1;
        try {
            namedParameterJdbcTemplate.update(SQL, namedParameters);
            internalId = clinicalDataProcessor.getPatient(patient.getSTABLE_ID()).getINTERNAL_ID();
        }
        catch (DataAccessException ex) {
            LOG.error("Could not import patient record: " + patient.getSTABLE_ID());
        }
        
        return internalId;
    }    
        
    /**
     * insert batch of clinical data into CLINICAL_PATIENT or CLINICAL_SAMPLE
     * @param tableName
     * @param clinicalData 
     */
    private void addClinicalDataBatch(String tableName, final List<ClinicalData> clinicalData) {
        String SQL = "INSERT INTO " + tableName + 
                " (internal_id, attr_id, attr_value) " + 
                "VALUES (:internal_id, :attr_id, :attr_value)";        
        int[] rows = namedParameterJdbcTemplate.batchUpdate(SQL, 
            SqlParameterSourceUtils.createBatch(clinicalData.toArray()));
        
        int rowsAffected = 0;
        for (int r : rows) {
            rowsAffected += r;
        }
        
        if (tableName.contains("patient")) {
            this.patientAttributeCount += rowsAffected;
        }
        else {
            this.sampleAttributeCount += rowsAffected;
        }
    }    
}