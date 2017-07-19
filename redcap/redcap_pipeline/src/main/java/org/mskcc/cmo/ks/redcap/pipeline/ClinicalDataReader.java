/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("#{jobParameters[stableId]}")
    private String stableId;

    private Map<String, List<String>> fullSampleHeader = new HashMap<>();
    private Map<String, List<String>> fullPatientHeader = new HashMap<>();
    private List<Map<String, String>> clinicalRecords = new ArrayList();
    private Map<String, Map<String, String>> compiledClinicalSampleRecords = new LinkedHashMap<>();
    private Map<String, Map<String, String>> compiledClinicalPatientRecords = new LinkedHashMap<>();

    private final Logger log = Logger.getLogger(ClinicalDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        
        String projectTitle = clinicalDataSource.getNextClinicalProjectTitle(stableId);

        log.info("Getting sample header for project: " + projectTitle);
        this.fullSampleHeader = metadataManager.getFullHeader(clinicalDataSource.getSampleHeader(stableId));

        log.info("Getting patient header for project: " + projectTitle);
        this.fullPatientHeader = metadataManager.getFullHeader(clinicalDataSource.getPatientHeader(stableId));

        // get clinical and sample data for current clinical data source
        for (Map<String, String> record : clinicalDataSource.getClinicalData(stableId)) {
            updateClinicalData(record, true);
            updateClinicalData(record, false);
        }
        // merge remaining clinical data sources if in merge mode and more clinical data exists
        if (mergeClinicalDataSources && clinicalDataSource.hasMoreClinicalData(stableId)) {
            mergeClinicalDataSources();
        }
        // associate patient data with their samples so that patient data for each sample is the same
        this.clinicalRecords = mergePatientSampleClinicalRecords();

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
        ec.put("projectTitle", projectTitle);
        ec.put("sampleHeader", fullSampleHeader);
        ec.put("patientHeader", fullPatientHeader);
        ec.put("writeClinicalSample", writeClinicalSample);
        ec.put("writeClinicalPatient", writeClinicalPatient);
    }
    
    /**
     * Associates patient data with each sample it's associated with.
     * @return 
     */
    private List<Map<String,String>> mergePatientSampleClinicalRecords() {
        List<Map<String,String>> mergedClinicalRecords = new ArrayList();
        for (Map<String, String> record : compiledClinicalSampleRecords.values()) {
            Map<String, String> patientData = compiledClinicalPatientRecords.getOrDefault(record.get("PATIENT_ID"), new HashMap<>());
            record.putAll(patientData);
            mergedClinicalRecords.add(record);
        }
        return mergedClinicalRecords;
    }

    private void mergeClinicalDataSources() {
        while (clinicalDataSource.hasMoreClinicalData(stableId)) {
            String projectTitle = clinicalDataSource.getNextClinicalProjectTitle(stableId);

            // get sample header for project and merge into global sample header list
            log.info("Merging sample header for project: " + projectTitle);
            Map<String, List<String>> sampleHeader = metadataManager.getFullHeader(clinicalDataSource.getSampleHeader(stableId));
            List<String> sampleColumnNames = sampleHeader.get("header");
            for (int i=0;i<sampleColumnNames.size();i++) {
                if (!fullSampleHeader.get("header").contains(sampleColumnNames.get(i))) {
                    for (String metadataName : fullSampleHeader.keySet()) {
                        this.fullSampleHeader.get(metadataName).add(sampleHeader.get(metadataName).get(i));
                    }
                }
            }

            // get patient header for project and merge into global patient header list
            log.info("Merging patient header for project: " + projectTitle);
            Map<String, List<String>> patientHeader = metadataManager.getFullHeader(clinicalDataSource.getPatientHeader(stableId));
            List<String> patientColumnNames = patientHeader.get("header");
            for (int i=0;i<patientColumnNames.size();i++) {
                if (!fullPatientHeader.get("header").contains(patientColumnNames.get(i))) {
                    for (String metadataName : fullPatientHeader.keySet()) {
                        this.fullPatientHeader.get(metadataName).add(patientHeader.get(metadataName).get(i));
                    }
                }
            }

            // get clinical records and merge into existing clinical records
            for (Map<String, String> record : clinicalDataSource.getClinicalData(stableId)) {
                updateClinicalData(record, true);
                updateClinicalData(record, false);
            }
        }
    }
    
    private void updateClinicalData(Map<String, String> record, boolean isSampleData) {
        Map<String, String> existingData = isSampleData ? 
                compiledClinicalSampleRecords.getOrDefault(record.get("SAMPLE_ID"), new HashMap<>()) : 
                compiledClinicalPatientRecords.getOrDefault(record.get("PATIENT_ID"), new HashMap<>());
        List<String> clinicalHeader = isSampleData ? fullSampleHeader.get("header") : fullPatientHeader.get("header");
        
        if (isSampleData && !existingData.containsKey("PATIENT_ID")) {
            existingData.put("PATIENT_ID", record.get("PATIENT_ID"));
        }
        for (String attribute : clinicalHeader) {
            if (!record.containsKey(attribute)) {
                continue;
            }
            String existingValue = existingData.getOrDefault(attribute, "");
            String recordValue = record.getOrDefault(attribute, "");
            if (!existingValue.isEmpty() && !recordValue.isEmpty()) {
                if (isSampleData) {
                    log.info("Clinical attribute " + attribute + " already loaded for sample " + record.get("SAMPLE_ID") + " - skipping.");
                }
                else {
                    log.info("Clinical attribute " + attribute + " already loaded for patient " + record.get("PATIENT_ID") + " - skipping.");
                }                
                continue;
            }
            String value = (!existingValue.isEmpty()) ? existingValue : recordValue;
            existingData.put(attribute, value);
        }
        if (isSampleData) {
            compiledClinicalSampleRecords.put(record.get("SAMPLE_ID"), existingData);
        }
        else {
            compiledClinicalPatientRecords.put(record.get("PATIENT_ID"), existingData);
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
