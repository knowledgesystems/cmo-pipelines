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

package org.cbioportal.cmo.pipelines.importer.config.tasklet;

import org.cbioportal.cmo.pipelines.importer.model.ClinicalAttribute;
import org.cbioportal.cmo.pipelines.importer.util.DataFileUtils;

import java.io.*;
import java.util.*;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Resource;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.logging.*;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;

/**
 *
 * @author ochoaa
 */
public class ClinicalAttributeTasklet implements Tasklet {
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    @Resource(name="clinicalMetadata")
    MultiKeyMap clinicalMetadata;
    
    private int newClinicalAttributes;
    
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private static final Log LOG = LogFactory.getLog(ClinicalAttributeTasklet.class);
    
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        String stepName = chunkContext.getStepContext().getStepName();
        String attributeType = clinicalMetadata.get(stepName, "attribute_type").toString();
        String dataFilename = clinicalMetadata.get(stepName, "data_filename").toString();
        
        List<File> dataFiles = DataFileUtils.listDataFiles(stagingDirectory, dataFilename);
        
        Map<String, List<ClinicalAttribute>> dataFileClinicalAttributes = new HashMap<>();
        for (File dataFile : dataFiles) {
            LOG.info("Loading clinical attribute meta data from: " + dataFile.getName());
            List<ClinicalAttribute> clinicalAttributes = loadClinicalAttributesMetadata(dataFile, attributeType);
            if (!clinicalAttributes.isEmpty()) {
                LOG.info("Loaded " + clinicalAttributes.size() + " clinical attributes from: " + dataFile.getName());
                dataFileClinicalAttributes.put(dataFile.getCanonicalPath(), clinicalAttributes);
            }
            else {
                LOG.error("Could not load any clinical attributes from: " + dataFile.getName());
            }            
        }        
        
        // add data file clinical attributes to the execution context
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("newClinicalAttributes", this.newClinicalAttributes);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("stepName", stepName);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("attributeType", stepName);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("dataFileClinicalAttributes", dataFileClinicalAttributes);
        return RepeatStatus.FINISHED; 
    }

    /**
     * load clinical attributes meta data from input file
     * @param clinicalDataFile
     * @param attributeType
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private List<ClinicalAttribute> loadClinicalAttributesMetadata(File clinicalDataFile, String attributeType) throws Exception {
        
        // load clinical attribute metadata from file
        List<ClinicalAttribute> clinicalAttributes = new ArrayList();
        try (FileReader reader = new FileReader(clinicalDataFile)) {
            BufferedReader buff = new BufferedReader(reader);
            String line = buff.readLine();
            String[] displayNames = DataFileUtils.splitDataFields(line);
            String[] descriptions, datatypes, priorities, colnames;
            if (line.startsWith(DataFileUtils.METADATA_PREFIX)) {
                descriptions = DataFileUtils.splitDataFields(buff.readLine());
                datatypes = DataFileUtils.splitDataFields(buff.readLine());
                priorities = DataFileUtils.splitDataFields(buff.readLine());
                colnames = DataFileUtils.splitDataFields(buff.readLine());
            }
            else {
                colnames = displayNames;
                descriptions = new String[colnames.length];
                Arrays.fill(descriptions, DataFileUtils.DEFAULT_DESCRIPTION);
                datatypes = new String[colnames.length];
                Arrays.fill(datatypes, DataFileUtils.DEFAULT_DATATYPE);
                priorities = new String[colnames.length];
                Arrays.fill(priorities, DataFileUtils.DEFAULT_PRIORITY);
            }   
            
            // fill in attribute types
            String[] attributeTypes = new String[displayNames.length];
            switch (attributeType) {
                case "PATIENT":
                case "SAMPLE":
                    Arrays.fill(attributeTypes, attributeType);
                default:
                    Arrays.fill(attributeTypes, "SAMPLE");
            }   //clinicalAttributes = new ArrayList();
            for (int i=0; i<colnames.length; i++) {
                ClinicalAttribute attr = new ClinicalAttribute(colnames[i].trim().toUpperCase(), displayNames[i],
                        descriptions[i], datatypes[i], attributeTypes[i].equals("PATIENT"), priorities[i]);
                
                // check if clinical attribute exists already in db
                if (getClinicalAttribute(attr.getATTR_ID()) == null) {
                    LOG.info("Importing new " + attributeTypes[i].toLowerCase() +"-level clinical attribute");
                    addClinicalAttribute(attr);
                    this.newClinicalAttributes++;
                }
                clinicalAttributes.add(attr);                
            }
        }
        
        return clinicalAttributes;
    }
    
    /**
     * insert clinical attribute record into CLINICAL_ATTRIBUTE
     * @param clinicalAttribute
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException 
     */
    private void addClinicalAttribute(ClinicalAttribute clinicalAttribute) throws Exception {
        String SQL = "INSERT INTO clinical_attribute " + 
                "(attr_id, display_name, description, datatype, patient_attribute, priority) " +
                "VALUES (:attr_id, :display_name, :description, :datatype, :patient_attribute, :priority)";
        
        Map<String, Object> namedParameters = new HashMap<>();
        for (String field : clinicalAttribute.getFieldNames()) {
            String columnName = field;
            namedParameters.put(columnName.toLowerCase(), clinicalAttribute.getClass().getMethod("get"+field).invoke(clinicalAttribute));
        }
        
        try {
            namedParameterJdbcTemplate.update(SQL, namedParameters);
        }
        catch (DataAccessException ex) {
            LOG.error("Error importing new clinical attribute: " + clinicalAttribute.getATTR_ID());
        }        
    }
    
    /**
     * get clinical attribute record from CLINICAL_ATTRIBUTE
     * @param attrId
     * @return 
     */
    private ClinicalAttribute getClinicalAttribute(String attrId) {
        String SQL = "SELECT * FROM clinical_attribute WHERE attr_id = :attr_id";        
        SqlParameterSource namedParameters = new MapSqlParameterSource("attr_id", attrId);
        
        ClinicalAttribute clinicalAttribute = null;
        try {
            clinicalAttribute = (ClinicalAttribute) namedParameterJdbcTemplate.queryForObject(SQL, namedParameters, new BeanPropertyRowMapper(ClinicalAttribute.class));
        }
        catch (DataAccessException ex) {}
        
        return clinicalAttribute;
    }
}