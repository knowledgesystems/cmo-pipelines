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
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.util.ValueNormalizer;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Repository;

@Repository
public class RedcapRepository {

    private class ImportDataComparisonResult {
        public Set<String> recordNamesSeenInFile = new HashSet<>();
        public Set<String> recordNamesForUnmatchedProjectRecords = new HashSet<>();
        public List<String> recordsToImport = new ArrayList<>();
        public ImportDataComparisonResult() {}
    };

    @Autowired
    private MetadataCache metadataCache;

    @Autowired
    private RedcapSessionManager redcapSessionManager;

    @Autowired
    private ValueNormalizer valueNormalizer;

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
    public void importClinicalData(String projectToken, List<String> dataForImport, boolean keepExistingProjectData) throws Exception {
        throwExceptionIfHeaderMismatchDetected(projectToken, dataForImport);
        replaceExternalHeadersWithRedcapIds(dataForImport);
        // fetch the redcap project's "identifier"/record name from the redcap-header/metadata API
        List<RedcapProjectAttribute> redcapAttributeArray = getAttributesByToken(projectToken); // {form_name}_complete fields are filtered out within this function
        String recordNameField = getRecordNameFieldNameFromRedcapAttributes(redcapAttributeArray);
        // TODO: replace the next line with a call to redcapSessionManager.getProjectInfoByToken() and see if using_autonumbering is set
        boolean recordNameFieldIsRecordId = (recordNameField.equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID));
        throwExceptionIfKeepingExistingDataWithAutonumberingProject(recordNameFieldIsRecordId, keepExistingProjectData);
        List<String> redcapAttributeNameList = createRedcapAttributeNameList(redcapAttributeArray, recordNameFieldIsRecordId);
        List<String> fileAttributeNameList = createFileAttributeNameList(dataForImport.get(0));
        Map<String, Integer> fileAttributeNameToPositionMap = createAttributeNameToPositionMap(fileAttributeNameList);
        List<Integer> fileFieldSelectionOrder = createFieldSelectionOrder(fileAttributeNameToPositionMap, redcapAttributeNameList);
        Map<String, String> existingRedcapRecordMap = createExistingRedcapRecordMap(projectToken, redcapAttributeNameList, recordNameField);
        ImportDataComparisonResult comparisonResult = compareDataForImportToExistingRedcapProjectData(dataForImport, fileFieldSelectionOrder, recordNameFieldIsRecordId, existingRedcapRecordMap);
        if (!keepExistingProjectData) {
            deleteProjectRecordsNotMatchingThoseBeingImported(projectToken, comparisonResult);
        }
        if (comparisonResult.recordsToImport.size() > 0) {
            importNewOrModifiedRecordsToProject(projectToken, recordNameFieldIsRecordId, fileAttributeNameList, redcapAttributeNameList, comparisonResult.recordsToImport);
        }
    }

    private void throwExceptionIfHeaderMismatchDetected(String projectToken, List<String> dataForImport) throws Exception {
        //TODO : pass in flag to tell whether this is a autonumbered project and then skip the first field regardless of field name
        List<String> normalizedDataFileHeader = Arrays.asList(externalFieldNamesToRedcapFieldIds(dataForImport.get(0).split("\t",-1)));
        List<RedcapProjectAttribute> redcapAttributes = getAttributesByToken(projectToken);
        List<String> redcapProjectHeader = new ArrayList<String>();
        for (int i = 0; i < redcapAttributes.size(); i++) {
            if (redcapAttributes.get(i).getFieldName().equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
                if (i == 0) {
                    continue; // skip autonumbered record_id field in first column
                }
            }
            redcapProjectHeader.add(redcapAttributes.get(i).getFieldName());
        }
        Collections.sort(normalizedDataFileHeader);
        Collections.sort(redcapProjectHeader);
        if (!normalizedDataFileHeader.equals(redcapProjectHeader)) {
            logMessageAndThrowException("file for import has differing headers in redcap ... aborting attempt to import data");
        }
    }

    private void throwExceptionIfKeepingExistingDataWithAutonumberingProject(boolean recordNameFieldIsRecordId, boolean keepExistingProjectData) throws Exception {
        if (recordNameFieldIsRecordId && keepExistingProjectData) {
            logMessageAndThrowException("\"--keep-existing-project-data\" cannot be used when importing to projects that use Autonumbering for record ids");
        }
    }

    private void logMessageAndThrowException(String message) throws Exception {
        log.error(message);
        throw new Exception(message);
    }

    /* Computes a diff of the data for import to the existing data in redcap project.
     */
    private ImportDataComparisonResult compareDataForImportToExistingRedcapProjectData(List<String> dataForImport, List<Integer> fileFieldSelectionOrder, boolean recordNameFieldIsRecordId, Map<String, String> existingRedcapRecordMap) {
        ImportDataComparisonResult comparisonResult = new ImportDataComparisonResult();
        comparisonResult.recordNamesForUnmatchedProjectRecords.addAll(existingRedcapRecordMap.values()); // will be removed as records are matched
        HashMap<String, Integer> dataForImportOccurrenceCount = new HashMap<>();
        ListIterator<String> fileRecordIterator = dataForImport.listIterator(1);
        boolean duplicateDataForImportDetected = false;
        while (fileRecordIterator.hasNext()) {
            List<String> fileRecordFieldValues = Arrays.asList(fileRecordIterator.next().split("\t", -1));
            List<String> orderedFileRecordFieldValues = new ArrayList<>();
            for (int index : fileFieldSelectionOrder) {
                String normalizedFieldValue = valueNormalizer.normalize(fileRecordFieldValues.get(index));
                orderedFileRecordFieldValues.add(normalizedFieldValue);
            }
            String orderedFileRecord = String.join("\t", orderedFileRecordFieldValues);
            // remember which record names were seen anywhere in the file
            if (!recordNameFieldIsRecordId) {
                comparisonResult.recordNamesSeenInFile.add(orderedFileRecordFieldValues.get(0));
            }
            // check for exact matches to record content within redcap
            if (!existingRedcapRecordMap.containsKey(orderedFileRecord)) {
                if (!dataForImportOccurrenceCount.containsKey(orderedFileRecord)) {
                    comparisonResult.recordsToImport.add(orderedFileRecord);
                }
            } else {
                String recordNameForMatchedProjectRecord = existingRedcapRecordMap.get(orderedFileRecord);
                comparisonResult.recordNamesForUnmatchedProjectRecords.remove(recordNameForMatchedProjectRecord);
            }
            // maintain running tally of record occurrence counts from dataForImport
            if (dataForImportOccurrenceCount.containsKey(orderedFileRecord)) {
                dataForImportOccurrenceCount.put(orderedFileRecord, dataForImportOccurrenceCount.get(orderedFileRecord) + 1);
                duplicateDataForImportDetected = true;
            } else {
                dataForImportOccurrenceCount.put(orderedFileRecord, 1);
            }
        }
        if (duplicateDataForImportDetected) {
            logWarningsAboutDuplicatedDataForImport(dataForImportOccurrenceCount);
        }
        return comparisonResult;
    }

    private void logWarningsAboutDuplicatedDataForImport(Map<String, Integer> dataForImportOccurrenceCount) {
        StringBuilder message = new StringBuilder("Duplications of the following records from the import file were ignored (field order is ordered to match redcap project):\n");
        for (String record : dataForImportOccurrenceCount.keySet()) {
            int occurrenceCount = dataForImportOccurrenceCount.get(record);
            if (occurrenceCount > 1) {
                message.append("    " + record + " (" + Integer.toString(occurrenceCount) + " occurrences seen)\n");
            }
        }
        log.warn(message);
    }

    private void deleteProjectRecordsNotMatchingThoseBeingImported(String projectToken, ImportDataComparisonResult comparisonResult) {
        // get keys that are in redcap project but not imported file, then delete
        Set<String> recordNamesToDelete = new HashSet<>();
        for (String recordName : comparisonResult.recordNamesForUnmatchedProjectRecords) {
            if (!comparisonResult.recordNamesSeenInFile.contains(recordName)) {
                recordNamesToDelete.add(recordName);
            }
        }
        if (recordNamesToDelete.size() > 0) {
            redcapSessionManager.deleteRedcapProjectData(projectToken, recordNamesToDelete);
        }
    }

    private void importNewOrModifiedRecordsToProject(String projectToken, boolean recordNameFieldIsRecordId, List<String> fileAttributeNameList, List<String> redcapAttributeNameList, List<String> recordsToImport) {
        boolean prependRecordIdColumn = recordNameFieldIsRecordId && !fileAttributeNameList.contains(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID);
        // add ordered redcap header string to request
        List<String> headerFieldsForImports = new ArrayList<>(redcapAttributeNameList);
        if (prependRecordIdColumn) {
            headerFieldsForImports.add(0, RedcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID);
            Integer nextAvailableAutonumberedRecordName = redcapSessionManager.getNextRecordNameForAutonumberedProject(projectToken);
            addRecordIdColumnIfMissingInFileAndPresentInProject(recordsToImport, nextAvailableAutonumberedRecordName.intValue());
        }
        String orderedHeaderCSV = String.join(",", headerFieldsForImports);
        List<String> recordsToImportCSV = valueNormalizer.convertTSVtoCSV(recordsToImport, true);
        String formattedRecordsToImport = "\n" + orderedHeaderCSV + "\n" +  String.join("\n",recordsToImportCSV.toArray(new String[0])) + "\n";
        redcapSessionManager.importClinicalData(projectToken, formattedRecordsToImport);
    }

    private void addRecordIdColumnIfMissingInFileAndPresentInProject(List<String> recordsToImport, int nextAvailableAutonumberedRecordName) {
        for (int index = 0; index < recordsToImport.size(); index++) {
            String expandedLine = Integer.toString(nextAvailableAutonumberedRecordName) + "\t" + recordsToImport.get(index);
            recordsToImport.set(index, expandedLine);
            nextAvailableAutonumberedRecordName = nextAvailableAutonumberedRecordName + 1;
        }
    }

    /** this function selects just the ordered list of redcap field ids which are expected to be imported
     *  by constucting a string composed of values from these fields (in this order) we can compare / match
     *  records in the imported file to records in the existing redcap project
     */
    public List<String> createRedcapAttributeNameList(List<RedcapProjectAttribute> attributeList, boolean recordNameFieldIsRecordId) {
        List<String> attributeNameList = new ArrayList<String>();
        for (RedcapProjectAttribute attribute : attributeList) {
            if (recordNameFieldIsRecordId && attribute.getFieldName().equals(redcapSessionManager.REDCAP_FIELD_NAME_FOR_RECORD_ID)) {
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

    private Map<String, String> createExistingRedcapRecordMap(String projectToken, List<String> redcapAttributeNameList, String recordNameField) {
        JsonNode[] redcapDataRecords = redcapSessionManager.getRedcapDataForProjectByToken(projectToken);
        Map<String, String> existingRedcapRecordMap = new HashMap<>();
        for (JsonNode redcapResponse : redcapDataRecords) {
            String existingRedcapRecordString = createExistingRedcapRecordString(redcapAttributeNameList, redcapResponse);
            String recordName = redcapResponse.get(recordNameField).asText();
            existingRedcapRecordMap.put(existingRedcapRecordString, recordName);
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
        return filterRedcapInstrumentCompleteFields(redcapAttributeByToken);
    }

    public List<Map<String, String>> getRedcapDataForProject(String projectToken) {
        JsonNode[] redcapDataRecords = redcapSessionManager.getRedcapDataForProjectByToken(projectToken);
        //TODO : we could eliminate the next line if we store the instrument names at the time the the headers are requested through ClinicalDataSource.get[Project|Sample|Patient]Header()
        RedcapProjectAttribute[] attributeArray = redcapSessionManager.getRedcapAttributeByToken(projectToken);
        Set<String> instrumentCompleteFieldNames = getInstrumentCompleteFieldNames(attributeArray);
        List<Map<String, String>> redcapDataForProject = new ArrayList<>();
        for (JsonNode redcapResponse : redcapDataRecords) {
            Map<String, String> redcapDataRecord = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> redcapNodeIterator = redcapResponse.fields();
            while (redcapNodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>)redcapNodeIterator.next();
                String redcapId = entry.getKey();
                RedcapAttributeMetadata metadata = null;
                if (instrumentCompleteFieldNames.contains(redcapId)) {
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

    public String getRecordNameFieldNameFromRedcapAttributes(List<RedcapProjectAttribute>redcapAttributeArray) {
        //TODO maybe check for empty list and throw exception
        if (redcapAttributeArray == null || redcapAttributeArray.size() < 1) {
            String errorMessage = "Error retrieving record name field from project : no attributes available";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return redcapAttributeArray.get(0).getFieldName(); // we are making the assumption tha the first attribute in the metadata is always the record name field
    }

    public List<RedcapProjectAttribute> filterRedcapInstrumentCompleteFields(RedcapProjectAttribute[] redcapAttributeArray) {
        //TODO maybe check for empty list and throw exception
        if (redcapAttributeArray == null || redcapAttributeArray.length < 1) {
            String errorMessage = "Error retrieving instrument name from project : no attributes available";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        Set<String> instrumentCompleteFieldNames = getInstrumentCompleteFieldNames(redcapAttributeArray);
        List<RedcapProjectAttribute> filteredProjectAttributeList = new ArrayList<>();
        for (RedcapProjectAttribute redcapProjectAttribute : redcapAttributeArray) {
            if (!instrumentCompleteFieldNames.contains(redcapProjectAttribute.getFieldName())) {
                filteredProjectAttributeList.add(redcapProjectAttribute);
            }
        }
        return filteredProjectAttributeList;
    }

    public Set<String> getInstrumentCompleteFieldNames(RedcapProjectAttribute[] redcapAttributeArray) {
        Set<String> instrumentCompleteFieldNames = new HashSet<String>();
        for (RedcapProjectAttribute redcapProjectAttribute : redcapAttributeArray) {
            String instrumentCompleteFieldName = redcapProjectAttribute.getFormName() + "_complete";
            if (!instrumentCompleteFieldNames.contains(instrumentCompleteFieldName)) {
                instrumentCompleteFieldNames.add(instrumentCompleteFieldName);
            }
        }
        return instrumentCompleteFieldNames;
    }

    public String getRedcapInstrumentName(RedcapProjectAttribute[] attributeArray) {
        return attributeArray[0].getFormName();
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
