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

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.*;
import org.springframework.batch.item.file.transform.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.BindException;

/**
 *
 * @author ochoaa
 */
public class ClinicalDataReader implements ItemStreamReader<CompositeClinicalData> {

    @Value("#{stepExecutionContext[cancerStudy]}")
    private CancerStudy cancerStudy;
    
    @Value("#{stepExecutionContext[dataFileClinicalAttributes]}")
    private Map<String, List<ClinicalAttribute>> dataFileClinicalAttributes;

    private List<CompositeClinicalData> compositeClinicalDataResults;
    
    private static final Log LOG = LogFactory.getLog(ClinicalDataReader.class);    
    
    public static enum MissingAttributeValues {
        NOT_APPLICABLE("Not Applicable"),
        NOT_AVAILABLE("Not Available"),
        PENDING("Pending"),
        DISCREPANCY("Discrepancy"),
        COMPLETED("Completed"),
        NULL("null"),
        MISSING(""),
        NA("NA"),
        N_A("N/A"),
        UNKNOWN("unknown");

        private String propertyName;
        
        MissingAttributeValues(String propertyName) { this.propertyName = propertyName; }
        public String toString() { return propertyName; }

        static public boolean has(String value) {
            if (value == null) return false;
            if (value.trim().equals("")) return true;
            try { 
                value = value.replaceAll("[\\[|\\]\\/]", "");
                value = value.replaceAll(" ", "_");
                return valueOf(value.toUpperCase()) != null;
            }
            catch (IllegalArgumentException ex) {
                return false;
            }
        }

        static public String getNotAvailable() {
            return "[" + NOT_AVAILABLE.toString() + "]";
        }
    }
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
                
        // for each file, read in the clinical data
        for (String dataFilename : dataFileClinicalAttributes.keySet()) {
            List<CompositeClinicalData> compositeClinicalData = new ArrayList();
            try {
                 compositeClinicalData = loadClinicalData(dataFilename);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            if (!compositeClinicalData.isEmpty()) {
                this.compositeClinicalDataResults.addAll(compositeClinicalData);
            }
        }
    }
  
    /**
     * loads clinical data from file
     * @param dataFilename
     * @return
     * @throws Exception 
     */
    private List<CompositeClinicalData> loadClinicalData(String dataFilename) throws Exception {
        
        // get clinical attribute names and determine where file has sample id col
        List<String> clinicalAttributeNames = new ArrayList();
        boolean hasSampleIdCol = false;
        for (ClinicalAttribute attr : dataFileClinicalAttributes.get(dataFilename)) {
            if (attr.getATTR_ID().equals("SAMPLE_ID")) {
                hasSampleIdCol = true;
            }
            clinicalAttributeNames.add(attr.getATTR_ID());
        }
        
        // extract clinical data using clinical attribute names
        List<CompositeClinicalData> compositeClinicalData = extractClinicalData(dataFilename, hasSampleIdCol, clinicalAttributeNames);
        if (compositeClinicalData.isEmpty()) {
            LOG.error("Could not load composite clinical data from: " + dataFilename);
        }

        return compositeClinicalData;
    }
    
    /**
     * extract clinical data from data file using given list of attributes
     * @param dataFilename
     * @param hasSampleIdCol
     * @param attributes
     * @return
     * @throws Exception 
     */
    private List<CompositeClinicalData> extractClinicalData(final String dataFilename, final boolean hasSampleIdCol, List<String> attributes) throws Exception {
        
        // init tab-delim tokenizer with names of clinical attributes
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        tokenizer.setNames(attributes.toArray(new String[attributes.size()]));
        
        // init line mapper for clinical data file
        DefaultLineMapper<CompositeClinicalData> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(clinicalFieldSetMapper(dataFilename, hasSampleIdCol));
        
        // set up clinical data file reader context
        FlatFileItemReader<CompositeClinicalData> clinicalDataReader = new FlatFileItemReader();
        clinicalDataReader.setResource(new FileSystemResource(dataFilename));        
        clinicalDataReader.setLineMapper(lineMapper);
        clinicalDataReader.setLinesToSkip(1);
        clinicalDataReader.open(new ExecutionContext());

        // read through each record in clinical data file
        List<CompositeClinicalData> compositeClinicalData = new ArrayList();
        CompositeClinicalData record = clinicalDataReader.read();
        while (record != null) {
            compositeClinicalData.add(record);
            record = clinicalDataReader.read();
        }
        clinicalDataReader.close();

        return compositeClinicalData;
    }

    /**
     * returns a field set mapper for the clinical data file
     * @param dataFilename
     * @param hasSampleIdCol
     * @return 
     */
    private FieldSetMapper clinicalFieldSetMapper(final String dataFilename, final boolean hasSampleIdCol) {
        return new FieldSetMapper() {
            @Override
            public Object mapFieldSet(FieldSet fs) throws BindException {
                // patient id column should always be present
                Patient patient = new Patient(fs.readString("PATIENT_ID"), cancerStudy.getCANCER_STUDY_ID());
                
                // set stable sample id if file has sample id column
                String sampleStableId = "";
                if (hasSampleIdCol) {
                    sampleStableId = fs.readString("SAMPLE_ID");
                }
                Sample sample = new Sample(sampleStableId, patient.getINTERNAL_ID(), cancerStudy.getTYPE_OF_CANCER_ID());
                
                // if attr is not patient id or sample id, and val not "missing" then add to list
                List<ClinicalData> patientClinicalData = new ArrayList();
                List<ClinicalData> sampleClinicalData = new ArrayList();
                for (ClinicalAttribute attr : dataFileClinicalAttributes.get(dataFilename)) {
                    if (!attr.getATTR_ID().equals("PATIENT_ID") && !attr.getATTR_ID().equals("SAMPLE_ID")) {
                        String attrVal = fs.readString(attr.getATTR_ID());
                        if (!MissingAttributeValues.has(attrVal)) {
                            ClinicalData newClinicalDatum = new ClinicalData(attr.getATTR_ID(), attrVal);
                            if (attr.isPATIENT_ATTRIBUTE()) {
                                patientClinicalData.add(newClinicalDatum);
                            }
                            else {
                                if (attr.getATTR_ID().equals("SAMPLE_TYPE")) {
                                    sample.setSAMPLE_TYPE(attrVal);
                                }
                                sampleClinicalData.add(newClinicalDatum);
                            }
                        }
                    }
                }
                CompositeClinicalData composite = new CompositeClinicalData(patient, sample, patientClinicalData, sampleClinicalData);
                
                return composite;
            }
        };
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}    

    @Override
    public CompositeClinicalData read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!compositeClinicalDataResults.isEmpty()) {
            return compositeClinicalDataResults.remove(0);
        }
        return null;
    }        
}