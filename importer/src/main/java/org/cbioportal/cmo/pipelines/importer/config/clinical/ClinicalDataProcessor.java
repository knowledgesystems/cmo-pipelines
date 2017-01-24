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
import com.google.common.base.Strings;
import org.apache.commons.logging.*;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;

/**
 *
 * @author ochoaa
 */
public class ClinicalDataProcessor implements ItemProcessor<CompositeClinicalData, CompositeClinicalData> {

    @Value("#{stepExecutionContext[cancerStudy]}")
    private CancerStudy cancerStudy;
       
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private static final Log LOG = LogFactory.getLog(ClinicalDataProcessor.class);    
    
    @Override
    public CompositeClinicalData process(CompositeClinicalData composite) throws Exception {
        int patientInternalId = -1;
        int sampleInternalId = -1;
        
        // update patient internal id in composite object if possible
        if (!Strings.isNullOrEmpty(composite.getPatient().getSTABLE_ID())){
             Patient existingPatient = getPatient(composite.getPatient().getSTABLE_ID());
             if (existingPatient != null) {
                 patientInternalId = existingPatient.getINTERNAL_ID();
                 composite.updatePatientInternalId(patientInternalId);
             }
        }
        
        // update sample internal id in composite object if possible
        if (!Strings.isNullOrEmpty(composite.getSample().getSTABLE_ID())) {
            Sample existingSample;
            if (patientInternalId != -1) {
                existingSample = getSampleByPatient(composite.getSample().getSTABLE_ID(), patientInternalId);
            }
            else {
                existingSample = getSampleByStudy(composite.getSample().getSTABLE_ID());                
            }
            
            if (existingSample != null) {
                sampleInternalId = existingSample.getINTERNAL_ID();
                patientInternalId = existingSample.getPATIENT_ID();
                composite.updateSampleInternalId(sampleInternalId);
            }
        }
        
        return composite;
    }
    
    /**
     * get sample record from SAMPLE
     * @param stableId
     * @param cancerStudyId
     * @return 
     */
    public Sample getSampleByPatient(String stableId, int patientId) {
        String SQL = "SELECT * FROM sample " + 
                "WHERE stable_id = :stable_id AND patient_id = :patient_id";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("stable_id", stableId);
        namedParameters.put("patient_id", patientId);
        
        Sample existingSample = null;
        try {
            existingSample = (Sample) namedParameterJdbcTemplate.queryForObject(SQL, namedParameters, new BeanPropertyRowMapper(Sample.class));
        }
        catch (DataAccessException ex) {}
        
        return existingSample;
    }
    
    /**
     * get sample record from SAMPLE
     * @param stableId
     * @return 
     */
    public Sample getSampleByStudy(String stableId) {
        String SQL = "SELECT * FROM sample " + 
                "WHERE stable_id = :stable_id AND patient_id IN (:patient_ids)";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("stable_id", stableId);
        namedParameters.put("patient_ids", listPatientIdsByStudy());
        
        Sample sample = null;
        try {
            sample = (Sample) namedParameterJdbcTemplate.queryForObject(SQL, namedParameters, new BeanPropertyRowMapper(Sample.class));
        }
        catch (DataAccessException ex) {}
        
        return sample;
    }    

    /**
     * get patient record from PATIENT
     * @param stableId
     * @return 
     */
    public Patient getPatient(String stableId) {
        String SQL = "SELECT * FROM patient " + 
                "WHERE stable_id = :stable_id AND cancer_study_id = :cancer_study_id";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("stable_id", stableId);
        namedParameters.put("cancer_study_id", cancerStudy.getCANCER_STUDY_ID());
        
        Patient patient = null;
        try {
            patient = (Patient) namedParameterJdbcTemplate.queryForObject(SQL, namedParameters, new BeanPropertyRowMapper(Patient.class));
        }
        catch (DataAccessException ex) {}
        
        return patient;
    }
    
    /**
     * list patient ids by cancer study
     * @return 
     */
    public List<Integer> listPatientIdsByStudy() {
        String SQL = "SELECT * FROM patient " + 
                "WHERE cancer_study_id = :cancer_study_id";
        SqlParameterSource namedParameters = new MapSqlParameterSource("cancer_study_id", cancerStudy.getCANCER_STUDY_ID());
        
        List<Integer> patientIds = new ArrayList();
        try {
            patientIds = (List<Integer>) namedParameterJdbcTemplate.query(SQL, namedParameters, new BeanPropertyRowMapper(Integer.class));
        }
        catch (DataAccessException ex) {}
        
        return patientIds;
    }
    
}