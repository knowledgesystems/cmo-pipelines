/*
 * Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.apache.commons.text.StringEscapeUtils;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Repository;

@Repository
public class RedcapRepository {

    @Autowired
    private MetadataCache metadataCache;

    @Autowired
    private RedcapSessionManager redcapSessionManager;

    private final Logger log = Logger.getLogger(RedcapRepository.class);

    public String convertRedcapIdToColumnHeader(String redcapId) {
        return redcapId.toUpperCase();
    }

    public String convertColumnHeaderToRedcapId(String columnHeader) {
        return columnHeader.toLowerCase();
    }

    public boolean projectExists(String projectTitle) {
        return redcapSessionManager.getTokenByProjectTitle(projectTitle) != null;
    }

    public boolean redcapDataTypeIsTimeline(String projectTitle) {
        return redcapSessionManager.redcapDataTypeIsTimeline(projectTitle);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public String getTokenByProjectTitle(String projectTitle) {
        return redcapSessionManager.getTokenByProjectTitle(projectTitle);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public Map<String, String> getClinicalTokenMapByStableId(String stableId) {
        return redcapSessionManager.getClinicalTokenMapByStableId(stableId);
    }

    //TODO: eliminate this function and make upper layers unaware of token handling
    public Map<String, String> getTimelineTokenMapByStableId(String stableId) {
        return redcapSessionManager.getTimelineTokenMapByStableId(stableId);
    }

    //TODO: change this to accept the project name instead .. not token
    public void deleteRedcapProjectData(String projectToken) {
        redcapSessionManager.deleteRedcapProjectData(projectToken);
    }

    /** dataForImport : first element is a tab delimited string holding the headers from the file, additional elements are tab delimited records
    */
    public void importClinicalData(String projectToken, List<String> dataForImport) {
        // fetch the redcap project's "identifier"/primary key from the redcap-header/metadata API
        List<RedcapProjectAttribute> redcapAttributeArray = getAttributesByToken(projectToken); // my_first_instrument_complete is filtered out in this function 
        String primaryKeyField = getPrimaryKeyFieldNameFromRedcapAttributes(redcapAttributeArray);
        boolean primaryKeyIsRecordId = (primaryKeyField.equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID));
        List<String> redcapAttributeNameList = createRedcapAttributeNameList(redcapAttributeArray, primaryKeyIsRecordId);
        List<String> fileAttributeNameList = createFileAttributeNameList(dataForImport.get(0));
        Map<String, Integer> fileAttributeNameToPositionMap = createAttributeNameToPositionMap(fileAttributeNameList);
        List<Integer> fileFieldSelectionOrder = createFieldSelectionOrder(fileAttributeNameToPositionMap, redcapAttributeNameList);
        Map<String, String> existingRedcapRecordMap = createExistingRedcapRecordMap(projectToken, redcapAttributeNameList, primaryKeyField);
        
        // begin comparison
        Set<String> primaryKeysSeenInFile = new HashSet<>();
        List<String> recordsToUpload = new ArrayList<>();
        ListIterator<String> fileRecordIterator = dataForImport.listIterator(1);
        while (fileRecordIterator.hasNext()) {
            List<String> fileRecordFieldValues = Arrays.asList(fileRecordIterator.next().split("\t"));
            List<String> orderedFileRecordFieldValues = new ArrayList<>();
            for (int index : fileFieldSelectionOrder) {
                // try-catch block to handle records where last attribute has empty value
                try {
                    orderedFileRecordFieldValues.add(fileRecordFieldValues.get(index));
                } catch (Exception e) {
                    orderedFileRecordFieldValues.add("");
                }
            }
            String orderedFileRecord = String.join("\t", orderedFileRecordFieldValues);
            // remember which primary key values were seen anywhere in the file
            if (!primaryKeyIsRecordId) {
                primaryKeysSeenInFile.add(orderedFileRecordFieldValues.get(0));
            }
            // check for exact matches to record content within redcap
            if (!existingRedcapRecordMap.containsKey(orderedFileRecord)) {
                recordsToUpload.add(orderedFileRecord);
            } else {
                existingRedcapRecordMap.put(orderedFileRecord, null);
            }
        }
       
        // get keys that are in redcap but not imported file - delete
        Set<String> primaryKeysToDelete = new HashSet<>();
        for (String value : existingRedcapRecordMap.values()) {
            if (value != null && !primaryKeysSeenInFile.contains(value)) {
                primaryKeysToDelete.add(value);       
            } 
        }
        System.out.println("Deleting: " + primaryKeysToDelete.size());
        if (primaryKeysToDelete.size() > 0) {
            redcapSessionManager.deleteRedcapProjectData(projectToken, primaryKeysToDelete);
        }
        
        // need to add ordered header to string being passed into request
        String orderedHeaderCSV = RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID + "," + String.join(",", redcapAttributeNameList);
        
        if (recordsToUpload.size() > 0) {    
            addRecordIdColumnIfMissingInFileAndPresentInProject(recordsToUpload, projectToken);
            List<String> recordsToUploadCSV = convertTSVtoCSV(recordsToUpload, true);
            String formattedRecordsToUpload = "\n" + orderedHeaderCSV + "\n" +  String.join("\n",recordsToUploadCSV.toArray(new String[0])) + "\n";
            System.out.println("WOWOWOWOWOWOWOW:" + formattedRecordsToUpload);     
            redcapSessionManager.importClinicalData(projectToken, formattedRecordsToUpload); 
        }
        // add logic to delete the records in primaryKeysToDelete 
        // add logic to import records from recordsToUpload  using "overwrite behavior" to rewrite changed redcap records
        



        // TODO : consider calling the header compatibility check call here
        
        // fetch current contents of redcap project
        
        // if the primary key is present in the uploaded file, make a map from the whole redcap record contents to the primary field value (per record)
        // else make a map from the whole redcap record record minus the record_id/primary key field to the primary field value (per record) -- NOTE : this must be a multi-map, in case there are duplicate records in redcap
        // maybe capture the redcap field order as exported in the metadata
        // then when we compare, we can reorder the fields in the uploaded file to match the recap field order, and do a hashmap lookup to see if the record is present in the redcap project map
        // TODO : add conversion of dataForImport into proper import to redap format (CSV?)
        // redcapSessionManager.importClinicalData(projectToken, dataForImport);
    }
    
    private void addRecordIdColumnIfMissingInFileAndPresentInProject(List<String> dataFileContentsTSV, String projectToken) {
        if (dataFileContentsTSV.get(0).startsWith(RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
            return; // RECORD_ID field is already the first field in the file
        }
        Integer maximumRecordIdInProject = redcapSessionManager.getMaximumRecordIdInRedcapProjectIfPresent(projectToken);
        if (maximumRecordIdInProject == null) {
            return; // record_id field is not present in project
        }
        int nextRecordId = maximumRecordIdInProject + 1;
        boolean headerHandled = false;
        for (int index = 0; index < dataFileContentsTSV.size(); index++) {
            String expandedLine = Integer.toString(nextRecordId) + "\t" + dataFileContentsTSV.get(index);
            dataFileContentsTSV.set(index, expandedLine);
            nextRecordId = nextRecordId + 1;

        }
    }
    
    private List<String> convertTSVtoCSV(List<String> tsvLines, boolean dropDuplicatedKeys) {
        HashSet<String> seen = new HashSet<String>();
        LinkedList<String> csvLines = new LinkedList<String>();
        for (String tsvLine : tsvLines) {
            String[] tsvFields = tsvLine.split("\t",-1);
            String key = tsvFields[0].trim();
            if (dropDuplicatedKeys && seen.contains(key)) {
                continue;
            }
            seen.add(key);
            String[] csvFields = new String[tsvFields.length];
            for (int i = 0; i < tsvFields.length; i++) {
                String tsvField = tsvFields[i];
                String csvField = tsvField;
                if (tsvField.indexOf(",") != -1) {
                    csvField = StringEscapeUtils.escapeCsv(tsvField);
                }
                csvFields[i] = csvField;
            }
            csvLines.add(String.join(",", csvFields));
        }
        return csvLines;
    }

    /** this function selects just the ordered list of redcap field ids which are expected to be imported
     *  by constucting a string composed of values from these fields (in this order) we can compare / match
     *  records in the imported file to records in the existing redcap project
     */
    private List<String> createRedcapAttributeNameList(List<RedcapProjectAttribute> attributeList, boolean primaryKeyIsRecordId) {
        List<String> attributeNameList = new ArrayList<String>();
        for (RedcapProjectAttribute attribute : attributeList) {
            if (primaryKeyIsRecordId && attribute.getFieldName().equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
                continue;
            }
            attributeNameList.add(attribute.getFieldName());
        }
        return attributeNameList;
    }

    private List<String> createFileAttributeNameList(String fileAttributeHeader) {
        List<String> fileAttributeNameList = new ArrayList<>();
        for (String attributeName : fileAttributeHeader.split("\t")) {
            fileAttributeNameList.add(convertColumnHeaderToRedcapId(attributeName));
        }
        return fileAttributeNameList;
    }

    private Map<String, Integer> createAttributeNameToPositionMap(List<String> attributeNameList) {
        Map<String, Integer> fileAttributeNameToPositionMap = new HashMap<>(attributeNameList.size());
        for (int position = 0; position < attributeNameList.size(); position = position + 1) {
            fileAttributeNameToPositionMap.put(attributeNameList.get(position), position);
        }
        return fileAttributeNameToPositionMap;
    }

    private List<Integer> createFieldSelectionOrder(Map<String, Integer> attributeNameToPositionMap, List<String> attributeNameList) {
        List<Integer> fieldSelectionOrder = new ArrayList<>(attributeNameList.size());
        for (String attribute : attributeNameList) {
            fieldSelectionOrder.add(attributeNameToPositionMap.get(attribute));
        }
        return fieldSelectionOrder;
    }

    private Map<String, String> createExistingRedcapRecordMap(String projectToken, List<String> redcapAttributeNameList, String primaryKeyField) {
        JsonNode[] redcapDataRecords = redcapSessionManager.getRedcapDataForProjectByToken(projectToken);
        Map<String, String> existingRedcapRecordMap = new HashMap<>();
        for (JsonNode redcapResponse : redcapDataRecords) {
            String existingRedcapRecordString = createExistingRedcapRecordString(redcapAttributeNameList, redcapResponse); 
            String primaryKeyValue = redcapResponse.get(primaryKeyField).asText();
            existingRedcapRecordMap.put(existingRedcapRecordString, primaryKeyValue);
        }
        return existingRedcapRecordMap;
    }

    private String createExistingRedcapRecordString(List<String> redcapAttributeNameList, JsonNode redcapResponse) {
        List<String> orderedRedcapRecordValues = new ArrayList<>();
        for (String attributeName : redcapAttributeNameList) {
            orderedRedcapRecordValues.add(redcapResponse.get(attributeName).asText());
        }
        return String.join("\t", orderedRedcapRecordValues);
    }         

    public List<RedcapProjectAttribute> getAttributesByToken(String projectToken) {
        RedcapProjectAttribute[] redcapAttributeByToken = redcapSessionManager.getRedcapAttributeByToken(projectToken);
        return filterRedcapInstrumentCompleteField(redcapAttributeByToken);
    }

    public List<Map<String, String>> getRedcapDataForProject(String projectToken) {
        JsonNode[] redcapDataRecords = redcapSessionManager.getRedcapDataForProjectByToken(projectToken);
        //TODO : we could eliminate the next line if we store the instrument name at the time the the headers are requested through ClinicalDataSource.get[Project|Sample|Patient]Header()
        RedcapProjectAttribute[] attributeArray = redcapSessionManager.getRedcapAttributeByToken(projectToken);
        String redcapInstrumentCompleteFieldName = getRedcapInstrumentName(attributeArray) + "_complete";
        List<Map<String, String>> redcapDataForProject = new ArrayList<>();
        for (JsonNode redcapResponse : redcapDataRecords) {
            Map<String, String> redcapDataRecord = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> redcapNodeIterator = redcapResponse.fields();
            while (redcapNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>)redcapNodeIterator.next();
                String redcapId = entry.getKey();
                RedcapAttributeMetadata metadata = null;
                if (redcapId.equals(redcapInstrumentCompleteFieldName)) {
                    continue;
                }
                try {
                    metadata = metadataCache.getMetadataByNormalizedColumnHeader(convertRedcapIdToColumnHeader(redcapId));
                } catch (RuntimeException e) {
                    String errorString = "Error: attempt to export data from redcap failed due to redcap_id " +
                            redcapId + " not having metadata defined in the Google clinical attributes worksheet";
                    log.warn(errorString);
                    throw new RuntimeException(errorString);
                }
                redcapDataRecord.put(metadata.getNormalizedColumnHeader(), entry.getValue().asText());
            }
            redcapDataForProject.add(redcapDataRecord);
        }
        return redcapDataForProject;
    }

    public String getPrimaryKeyFieldNameFromRedcapAttributes(List<RedcapProjectAttribute>redcapAttributeArray) {
        //TODO maybe check for empty list and throw exception
        if (redcapAttributeArray == null || redcapAttributeArray.size() < 1) {
            String errorMessage = "Error retrieving primary key from project : no attributes available";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return redcapAttributeArray.get(0).getFieldName(); // we are making the assumption tha the first attribute in the metadata is always the primary key
    }

    public List<RedcapProjectAttribute> filterRedcapInstrumentCompleteField(RedcapProjectAttribute[] redcapAttributeArray) {
        //TODO maybe check for empty list and throw exception
        if (redcapAttributeArray == null || redcapAttributeArray.length < 1) {
            String errorMessage = "Error retrieving instrument name from project : no attributes available";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        List<RedcapProjectAttribute> filteredProjectAttributeList = new ArrayList<>();
        String redcapInstrumentCompleteFieldName = redcapAttributeArray[0].getFormName() + "_complete";
        for (RedcapProjectAttribute redcapProjectAttribute : redcapAttributeArray) {
            if (!redcapInstrumentCompleteFieldName.equals(redcapProjectAttribute.getFieldName())) {
                filteredProjectAttributeList.add(redcapProjectAttribute);
            }
        }
        return filteredProjectAttributeList;
    }

    /** add record_id column if missing in data file contents and present in redcap project */
    //TODO make this method private
    public void adjustDataForRedcapImport(List<String> dataFileContentsTSV, String projectToken) {
        if (dataFileContentsTSV.get(0).startsWith(RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
            return; // RECORD_ID field is already the first field in the file
        }
        Integer maximumRecordIdInProject = redcapSessionManager.getMaximumRecordIdInRedcapProjectIfPresent(projectToken);
        if (maximumRecordIdInProject == null) {
            return; // record_id field is not present in project
        }
        int nextRecordId = maximumRecordIdInProject + 1;
        boolean headerHandled = false;
        for (int index = 0; index < dataFileContentsTSV.size(); index++) {
            if (headerHandled) {
                String expandedLine = Integer.toString(nextRecordId) + "\t" + dataFileContentsTSV.get(index);
                dataFileContentsTSV.set(index, expandedLine);
                nextRecordId = nextRecordId + 1;
            } else {
                String expandedLine = RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID + "\t" + dataFileContentsTSV.get(index);
                dataFileContentsTSV.set(index, expandedLine);
                headerHandled = true;
            }
        }
    }

    public String getRedcapInstrumentName(RedcapProjectAttribute[] attributeArray) {
        return attributeArray[0].getFormName();
    }

//    public String getRedcapFieldNameOrder(RedcapProjectAttribute[] attributeArray, String projectToken) {
 //       RedcapProjectAttribute[] attributeArray = redcapSessionManager.getRedcapAttributeByToken(projectToken);
  //      if (attributeArray == null || attributeArray.length < 1) {
   //         String errorMessage = "Error retrieving instrument name from project : no attributes available";
    //        log.error(errorMessage);
     //       throw new RuntimeException(errorMessage);
      //  }
       // return attributeArray[0].getFormName();
    //}


}
