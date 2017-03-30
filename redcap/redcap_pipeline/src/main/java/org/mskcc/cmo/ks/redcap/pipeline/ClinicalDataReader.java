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
package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.source.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class ClinicalDataReader implements ItemStreamReader<Map<String, String>> {    
    
    @Autowired
    public ClinicalDataSource clinicalDataSource;
        
    @Autowired
    public MetadataManager metadataManager;
    
    @Value("#{jobParameters[mergeClinicalDataSources]}")
    private boolean mergeClinicalDataSources;
    
    private Map<String, List<String>> fullSampleHeader = new HashMap<>();
    private Map<String, List<String>> fullPatientHeader = new HashMap<>();    
    private List<Map<String, String>> clinicalRecords = new ArrayList();
    private Map<String, Map<String, String>> compiledClinicalRecords = new LinkedHashMap<>();
    
    private final Logger log = Logger.getLogger(ClinicalDataReader.class);
    
    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        String studyId = clinicalDataSource.getNextClinicalStudyId();
        
        log.info("Getting sample header for project: " + studyId);
        this.fullSampleHeader = metadataManager.getFullHeader(clinicalDataSource.getSampleHeader());
        
        log.info("Getting patient header for project: " + studyId);
        this.fullPatientHeader = metadataManager.getFullHeader(clinicalDataSource.getPatientHeader());
        
        this.clinicalRecords = clinicalDataSource.getClinicalData();        
        if (mergeClinicalDataSources && clinicalDataSource.hasMoreClinicalData()) {
            for (Map<String, String> record : clinicalRecords) {
                compiledClinicalRecords.put(record.get("SAMPLE_ID"), record);
            }
            // now merge remaining clinical data sources 
            mergeClinicalDataSources();
            this.clinicalRecords = new ArrayList(compiledClinicalRecords.values());
        }
        
        // if sample header size is <= 1 then skip writing the sample clinical data file
        boolean writeClinicalSample = true;
        if (fullSampleHeader.get("header").size() <= 1) {
            log.warn("Sample header size for project <=1 - clinical sample data file will not be generated");
            writeClinicalSample = false;
        }

        // if patient header size is <= 1 then skip writing the patient clinical data file
        boolean writeClinicalPatient = true;
        if (fullPatientHeader.get("header").size() <= 1) {
            log.warn("Patient header size for project <=1 - clinical patient data file will not be generated");
            writeClinicalPatient = false;
        }
        
        // add headers and booleans to execution context for processors and writers
        ec.put("studyId", studyId);
        ec.put("sampleHeader", fullSampleHeader);
        ec.put("patientHeader", fullPatientHeader);
        ec.put("writeClinicalSample", writeClinicalSample);
        ec.put("writeClinicalPatient", writeClinicalPatient);
    }

    private void mergeClinicalDataSources() {        
        while (clinicalDataSource.hasMoreClinicalData()) {
            String studyId = clinicalDataSource.getNextClinicalStudyId();
            
            // get sample header for project and merge into global sample header list
            log.info("Merging sample header for project: " + studyId);
            Map<String, List<String>> sampleHeader = metadataManager.getFullHeader(clinicalDataSource.getSampleHeader());
            List<String> sampleColumnNames = sampleHeader.get("header");
            for (int i=0;i<sampleColumnNames.size();i++) {
                if (!fullSampleHeader.get("header").contains(sampleColumnNames.get(i))) {                    
                    for (String metadataName : fullSampleHeader.keySet()) {
                        this.fullSampleHeader.get(metadataName).add(sampleHeader.get(metadataName).get(i));
                    }
                }
            }
            
            // get patient header for project and merge into global patient header list
            log.info("Merging patient header for project: " + studyId);
            Map<String, List<String>> patientHeader = metadataManager.getFullHeader(clinicalDataSource.getPatientHeader());
            List<String> patientColumnNames = patientHeader.get("header");
            for (int i=0;i<patientColumnNames.size();i++) {
                if (!fullPatientHeader.get("header").contains(patientColumnNames.get(i))) {                    
                    for (String metadataName : fullPatientHeader.keySet()) {
                        this.fullPatientHeader.get(metadataName).add(patientHeader.get(metadataName).get(i));
                    }
                }
            }
            
            // get clinical records and merge into existing clinical records
            for (Map<String, String> record : clinicalDataSource.getClinicalData()) {
                Map<String, String> existingData = compiledClinicalRecords.getOrDefault(record.get("SAMPLE_ID"), new HashMap<>());
                for (String attribute : record.keySet()) {
                    if (!existingData.containsKey(attribute)) {
                        existingData.put(attribute, record.get(attribute));
                    }
                    else {
                        if (!existingData.get(attribute).isEmpty() && !record.get(attribute).isEmpty()) {
                            log.info("Clinical attribute " + attribute + " already loaded for sample " + record.get("SAMPLE_ID") + " - skipping.");
                        }
                        else {
                            String value = (!existingData.get(attribute).isEmpty()) ? existingData.get(attribute) : record.get(attribute);
                            existingData.put(attribute, value);
                        }
                    }
                }
                this.compiledClinicalRecords.put(record.get("SAMPLE_ID"), existingData);
            }
        }
    }
    
    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public Map<String, String> read() throws Exception {
        if (!clinicalRecords.isEmpty()) {            
            return clinicalRecords.remove(0);         
        }
        return null;
    }       
}
