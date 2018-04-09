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

    /** dataForImport : first element is a tab delimited string holding the headers from the file, additional elements are tab delimited records
    */
    public void importClinicalData(String projectToken, List<String> dataForImport) throws Exception {
        if (!dataFileHeadersEqualRedcapProjectHeaders(dataForImport, projectToken)) {
            log.error("file for import has differing headers in redcap ... aborting attempt to import data");
            throw new Exception("file for import has differing headers in redcap ... aborting attempt to import data");
        }
        replaceExternalHeadersWithRedcapIds(dataForImport);
        // fetch the redcap project's "identifier"/primary key from the redcap-header/metadata API
        List<RedcapProjectAttribute> redcapAttributeArray = getAttributesByToken(projectToken); // my_first_instrument_complete is filtered out in this function
        String primaryKeyField = getPrimaryKeyFieldNameFromRedcapAttributes(redcapAttributeArray);
        boolean primaryKeyIsRecordId = (primaryKeyField.equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID));
        List<String> redcapAttributeNameList = createRedcapAttributeNameList(redcapAttributeArray, primaryKeyIsRecordId);
        List<String> fileAttributeNameList = createFileAttributeNameList(dataForImport.get(0));
        Map<String, Integer> fileAttributeNameToPositionMap = createAttributeNameToPositionMap(fileAttributeNameList);
        List<Integer> fileFieldSelectionOrder = createFieldSelectionOrder(fileAttributeNameToPositionMap, redcapAttributeNameList);
        Map<String, String> existingRedcapRecordMap = createExistingRedcapRecordMap(projectToken, redcapAttributeNameList, primaryKeyField);
        int maximumObservedRecordId = Integer.MIN_VALUE;
        if (primaryKeyIsRecordId) {
            maximumObservedRecordId = findMaximumRecordIdFromRecordMap(existingRedcapRecordMap);
        }
        // begin comparison
        Set<String> primaryKeysSeenInFile = new HashSet<>();
        List<String> recordsToImport = new ArrayList<>();
        ListIterator<String> fileRecordIterator = dataForImport.listIterator(1);
        while (fileRecordIterator.hasNext()) {
            List<String> fileRecordFieldValues = Arrays.asList(fileRecordIterator.next().split("\t", -1));
            List<String> orderedFileRecordFieldValues = new ArrayList<>();
            for (int index : fileFieldSelectionOrder) {
                orderedFileRecordFieldValues.add(fileRecordFieldValues.get(index));
            }
            String orderedFileRecord = String.join("\t", orderedFileRecordFieldValues);
            // remember which primary key values were seen anywhere in the file
            if (!primaryKeyIsRecordId) {
                primaryKeysSeenInFile.add(orderedFileRecordFieldValues.get(0));
            }
            // check for exact matches to record content within redcap
            if (!existingRedcapRecordMap.containsKey(orderedFileRecord)) {
                recordsToImport.add(orderedFileRecord);
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
        if (primaryKeysToDelete.size() > 0) {
            redcapSessionManager.deleteRedcapProjectData(projectToken, primaryKeysToDelete);
        }
        boolean prependRecordIdColumn = primaryKeyIsRecordId && !fileAttributeNameList.contains(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID);
        // need to add ordered header to string being passed into request
        List<String> headerFieldsForImports = new ArrayList<>(redcapAttributeNameList);
        if (prependRecordIdColumn ) {
            headerFieldsForImports.add(0, RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID);
        }
        String orderedHeaderCSV = String.join(",", headerFieldsForImports);
        if (recordsToImport.size() > 0) {
            if (prependRecordIdColumn) {
                addRecordIdColumnIfMissingInFileAndPresentInProject(recordsToImport, maximumObservedRecordId);
            }
            List<String> recordsToImportCSV = convertTSVtoCSV(recordsToImport, true);
            String formattedRecordsToImport = "\n" + orderedHeaderCSV + "\n" +  String.join("\n",recordsToImportCSV.toArray(new String[0])) + "\n";
            redcapSessionManager.importClinicalData(projectToken, formattedRecordsToImport);
        }
    }

    public void addRecordIdColumnIfMissingInFileAndPresentInProject(List<String> recordsToImport, int maximumExistingRecordId) {
        int nextRecordId = maximumExistingRecordId + 1;
        for (int index = 0; index < recordsToImport.size(); index++) {
            String expandedLine = Integer.toString(nextRecordId) + "\t" + recordsToImport.get(index);
            recordsToImport.set(index, expandedLine);
            nextRecordId = nextRecordId + 1;
        }
    }

    private List<String> convertTSVtoCSV(List<String> tsvLines, boolean dropDuplicatedKeys) {
        HashSet<String> seen = new HashSet<String>();
        LinkedList<String> csvLines = new LinkedList<String>();
        for (String tsvLine : tsvLines) {
            String[] tsvFields = tsvLine.split("\t", -1);
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
    public List<String> createRedcapAttributeNameList(List<RedcapProjectAttribute> attributeList, boolean primaryKeyIsRecordId) {
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
        for (String attributeName : fileAttributeHeader.split("\t", -1)) {
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

    private int findMaximumRecordIdFromRecordMap(Map<String, String> existingRedcapRecordMap) {
        int maximumRecordIdFound = 0; // so that the next available recordId for import is 1 when the project is empty
        for (String recordIdString : existingRedcapRecordMap.values()) {
            try {
                int recordId = Integer.parseInt(recordIdString);
                if (recordId > maximumRecordIdFound) {
                    maximumRecordIdFound = recordId;
                }
            } catch (NumberFormatException e) {
                String errorMessage = "Error : primary key is record_id, and redcap contained a non-integer value : " + recordIdString;
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        }
        return maximumRecordIdFound;
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
                            redcapId + " not having metadata defined in the Clinical Data Dictionary";
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

    // check to ensure that datafile headers match those in redcap project
    // TO DO: pass in a project title instead of token?
    public boolean dataFileHeadersEqualRedcapProjectHeaders(List<String> dataForImport, String projectToken) {
        List<String> normalizedDataFileHeader = Arrays.asList(externalFieldNamesToRedcapFieldIds(dataForImport.get(0).split("\t",-1)));
        List<RedcapProjectAttribute> redcapAttributes = getAttributesByToken(projectToken);
        List<String> redcapProjectHeader = new ArrayList<String>();
        for (int i = 0; i < redcapAttributes.size(); i++) {
            if (!redcapAttributes.get(i).getFieldName().equals("record_id")) {
                redcapProjectHeader.add(redcapAttributes.get(i).getFieldName());
            }
        }
        Collections.sort(normalizedDataFileHeader);
        Collections.sort(redcapProjectHeader);
        return normalizedDataFileHeader.equals(redcapProjectHeader);
    }

    private void replaceExternalHeadersWithRedcapIds(List<String> dataForImport) {
        String[] externalHeaderFields = dataForImport.get(0).split("\t",-1);
        String[] redcapHeaderIds = externalFieldNamesToRedcapFieldIds(externalHeaderFields);
        String newHeaderLine = String.join("\t", redcapHeaderIds);
        dataForImport.set(0, newHeaderLine);
    }

    public String[] externalFieldNamesToRedcapFieldIds(String[] externalFieldNames) {
        if (externalFieldNames == null) {
            return new String[0];
        }
        String[] redcapFieldIds = new String[externalFieldNames.length];
        for (int i = 0; i < externalFieldNames.length; i++) {
            redcapFieldIds[i] =  convertColumnHeaderToRedcapId(externalFieldNames[i]);
        }
        return redcapFieldIds;
    }
}
