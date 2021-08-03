/*
 * Copyright (c) 2017-2018 Memorial Sloan-Kettering Cancer Center.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.redcap.source.internal;

import java.util.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.source.internal.RedcapRepository;
import org.mskcc.cmo.ks.redcap.source.internal.RedcapSourceTestConfiguration;
import org.springframework.beans.factory.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@TestPropertySource(
    properties = { "redcap.batch.size=2"
    },
    inheritLocations = false
)
@RunWith(SpringRunner.class)
@ContextConfiguration(classes=RedcapSourceTestConfiguration.class)
public class RedcapRepositoryTest {

    @Autowired
    private RedcapRepository redcapRepository;
    @Autowired
    private RedcapSourceTestConfiguration redcapSourceTestConfiguration;

    private static final boolean DROP_EXISTING_RECORDS = false;
    private static final boolean KEEP_EXISTING_RECORDS = true;
    private static String RECORD_ID_FIRST_TOKEN;
    private static String RECORD_ID_LATER_TOKEN;
    private static String RECORD_ID_ABSENT_TOKEN;

    @BeforeClass
    public static void setupForTests() {
        RECORD_ID_FIRST_TOKEN = RedcapSourceTestConfiguration.RECORD_ID_AS_RECORD_NAME_FIELD_PROJECT_TOKEN;
        RECORD_ID_LATER_TOKEN = RedcapSourceTestConfiguration.RECORD_ID_NOT_AS_RECORD_NAME_FIELD_PROJECT_TOKEN;
        RECORD_ID_ABSENT_TOKEN = RedcapSourceTestConfiguration.RECORD_ID_NOT_PRESENT_PROJECT_TOKEN;
    }

    /* This test mocks the RedcapSessionManager to add a project with fields {"PATIENT_ID", "CRDB_CONSENT_DATE_DAYS", "PARTA_CONSENTED_12_245"}.
     * getRedcapDataForProject() is called to test whether the mocked data is returned for all fields.
     */
    @Test
    public void testGetDataForRedcapProjectID() {
        if (redcapRepository == null) {
            Assert.fail("redcapRepository was not initialized properly for testing");
        }
        List<Map<String, String>> dataReturned = redcapRepository.getRedcapDataForProject(RedcapSourceTestConfiguration.ONE_DIGIT_PROJECT_TOKEN);
        List<Map<String, String>> expectedReturnValue = createExpectedReturnExportFromRedcapOneNumericAttributeName();
        String differenceBetweenReturnedAndExpected = getDifferenceBetweenReturnedAndExpected(dataReturned, expectedReturnValue);
        if (differenceBetweenReturnedAndExpected != null && differenceBetweenReturnedAndExpected.length() > 0) {
            Assert.fail("difference between returned and expected values: " + differenceBetweenReturnedAndExpected);
        }
    }

    /* createRedcapAttributeNameList should convert list of RedcapProjectAttributes to list of String (field name)
     * order should be preserved
     * record_id/my_first_instrument should not be included - unless record_id is not primary key
     */
    @Test
    public void testCreateRedcapAttributeNameList() {
        List<RedcapProjectAttribute> attributeListWithRecordIdAsRecordNameField = redcapRepository.getAttributesByToken(RECORD_ID_FIRST_TOKEN);
        List<RedcapProjectAttribute> attributeListWithRecordIdNotAsRecordNameField = redcapRepository.getAttributesByToken(RECORD_ID_LATER_TOKEN);
        List<RedcapProjectAttribute> attributeListWithoutRecordId = redcapRepository.getAttributesByToken(RECORD_ID_ABSENT_TOKEN);

        List<String> attributeNameListWithRecordIdAsRecordNameField = redcapRepository.createRedcapAttributeNameList(attributeListWithRecordIdAsRecordNameField, true);
        List<String> attributeNameListWithRecordIdNotAsRecordNameField = redcapRepository.createRedcapAttributeNameList(attributeListWithRecordIdNotAsRecordNameField, false);
        List<String> attributeNameListWithoutRecordId = redcapRepository.createRedcapAttributeNameList(attributeListWithoutRecordId, true);

        List<String> expectedAttributeNameList = createExpectedAttributeNameList();
        List<String> expectedAttributeNameListWithRecordId = createExpectedAttributeNameListWithRecordId();
        if (!checkAttributeNameListsAreEqual(attributeNameListWithRecordIdAsRecordNameField, expectedAttributeNameList)) {
            Assert.fail("Returned list is different from expected: " + String.join(",", attributeNameListWithRecordIdAsRecordNameField) + "\t" + String.join(",", expectedAttributeNameList));
        }
        if (!checkAttributeNameListsAreEqual(attributeNameListWithRecordIdNotAsRecordNameField, expectedAttributeNameListWithRecordId)) {
            Assert.fail("Returned list is different from expected: " + String.join(",", attributeNameListWithRecordIdNotAsRecordNameField) + "\t" + String.join(",", expectedAttributeNameListWithRecordId));
        }
        if (!checkAttributeNameListsAreEqual(attributeNameListWithoutRecordId, expectedAttributeNameList)) {
            Assert.fail("Returned list is different from expected: " + String.join(",", attributeNameListWithoutRecordId) + "\t" + String.join(",", expectedAttributeNameList));
        }
    }

    /* tests import of various datafiles into different redcap projects
     * datafiles can contain new records, changed records, less records, or identical records
     * redcap project can have record_id as primary key or not contain record_id
     */
    @Test
    public void testImportClinicalData() throws Exception {
        List<String> defaultFileForImport = createDefaultFileForImport();
        List<String> newFileForImport = createNewFileForImport();
        List<String> changedFileForImport = createChangedFileForImport();
        List<String> variedFileForImport = createVariedFileForImport();
        List<String> newDuplicateFileForImport = createNewDuplicateFileForImport();
        List<String> existingDuplicateFileForImport = createExistingDuplicateFileForImport();
        List<String> excessRecordFileForImport = createExcessRecordFileForImport();

        Set<String> expectedRecordsForDefaultImport = new HashSet<>();
        Set<String> expectedRecordsForNewImport = createExpectedRecordsForNewImport();
        Set<String> expectedRecordsForNewImportWithRecordIdAsRecordNameField = createExpectedRecordsForNewImportWithRecordIdAsRecordNameField();
        Set<String> expectedRecordsForChangedImport = createExpectedRecordsForChangedImport();
        Set<String> expectedRecordsForChangedImportWithRecordIdAsRecordNameField = createExpectedRecordsForChangedImportWithRecordIdAsRecordNameField();
        Set<String> expectedRecordsForVariedImportWithKeepExisting = createExpectedRecordsForVariedImportWithKeepExisting();
        Set<String> expectedRecordsForNewDuplicateImport = createExpectedRecordsForNewDuplicateImport();
        Set<String> expectedRecordsForExistingDuplicateImport = createExpectedRecordsForExistingDuplicateImport();
        Set<String> expectedRecordsForExcessImport = createExpectedRecordsForExcessImport();

        Set<String> expectedSetForDefaultDeletion = new HashSet<>();
        Set<String> expectedSetForChangedDeletion = createExpectedSetForChangedDeletion();
        Set<String> expectedSetForChangedDeletionWithRecordIdAsRecordNameField = createExpectedSetForChangedDeletionWithRecordIdAsRecordNameField();
        Set<String> expectedSetForChangedDeletionWithKeepExisting = new HashSet<>(); // no deletion when keeping existing
        Set<String> expectedSetForDeletionWithExcessRecords = createExpectedSetForDeletionWithExcessRecords();

        StringBuilder errorMessage = new StringBuilder();

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_FIRST_TOKEN, defaultFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForDefaultDeletion, expectedRecordsForDefaultImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, defaultFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForDefaultDeletion, expectedRecordsForDefaultImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_FIRST_TOKEN, newFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForDefaultDeletion, expectedRecordsForNewImportWithRecordIdAsRecordNameField));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, newFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForDefaultDeletion, expectedRecordsForNewImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_FIRST_TOKEN, changedFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForChangedDeletionWithRecordIdAsRecordNameField, expectedRecordsForChangedImportWithRecordIdAsRecordNameField));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, changedFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForChangedDeletion, expectedRecordsForChangedImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, variedFileForImport, KEEP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForChangedDeletionWithKeepExisting, expectedRecordsForVariedImportWithKeepExisting));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, newDuplicateFileForImport, KEEP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForChangedDeletionWithKeepExisting, expectedRecordsForNewDuplicateImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, existingDuplicateFileForImport, KEEP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForChangedDeletionWithKeepExisting, expectedRecordsForExistingDuplicateImport));

        redcapSourceTestConfiguration.resetRedcapSessionManagerHistory();
        redcapRepository.importClinicalData(RECORD_ID_ABSENT_TOKEN, excessRecordFileForImport, DROP_EXISTING_RECORDS);
        errorMessage.append(compareRecordsForDeletionAndImport(expectedSetForDeletionWithExcessRecords, expectedRecordsForExcessImport));

        String errorString = errorMessage.toString();
        if (!errorString.isEmpty()) {
            Assert.fail(errorString);
        }
    }

    @Test(expected = Exception.class)
    public void testImportClinicalDataToAutonumberedProjectWithKeepExistingRecords() throws Exception {
        List<String> defaultFileForImport = createDefaultFileForImport();
        redcapRepository.importClinicalData(RECORD_ID_FIRST_TOKEN, defaultFileForImport, KEEP_EXISTING_RECORDS);
    }

    private List<String> createExpectedAttributeNameList() {
        List<String> expectedAttributeNameList = new ArrayList<String>();
        expectedAttributeNameList.add("patient_id");
        expectedAttributeNameList.add("sample_id");
        expectedAttributeNameList.add("necrosis");
        expectedAttributeNameList.add("ethnicity");
        return expectedAttributeNameList;
    }

    private List<String> createExpectedAttributeNameListWithRecordId() {
        List<String> expectedAttributeNameListWithRecordId = createExpectedAttributeNameList();
        expectedAttributeNameListWithRecordId.add("record_id");
        return expectedAttributeNameListWithRecordId;
    }

    private boolean checkAttributeNameListsAreEqual(List<String> attributeNameList, List<String> expectedAttributeNameList) {
        boolean attributeNameListsAreEqual = true;
        if (attributeNameList.size() != expectedAttributeNameList.size()) {
            return false;
        }
        for (int i = 0; i < attributeNameList.size(); i++) {
            if (!attributeNameList.get(i).equals(expectedAttributeNameList.get(i))) {
                return false;
            }
        }
        return attributeNameListsAreEqual;
    }

    private List<String> createDefaultFileForImport() {
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("PATIENT_ID	SAMPLE_ID	NECROSIS	ETHNICITY");
        returnValue.add("P-0000001	P-0000001-T01	YES	Asian");
        returnValue.add("P-0000002	P-0000002-T02	NO	Caucasian");
        returnValue.add("P-0000003	P-0000003-T03	YES	Caucasian");
        return returnValue;
    }

    private List<String> createNewFileForImport() {
        List<String> returnValue = createDefaultFileForImport();
        returnValue.add("P-0000004	P-0000004-T04	NO	Asian");
        return returnValue;
    }

    private List<String> createChangedFileForImport() {
        List<String> returnValue = createDefaultFileForImport();
        returnValue.remove("P-0000002	P-0000002-T02	NO	Caucasian");
        returnValue.add("P-0000002	P-0000002-T02	YES	Caucasian");
        returnValue.remove("P-0000003	P-0000003-T03	YES	Caucasian");
        return returnValue;
    }

    private List<String> createVariedFileForImport() {
        //Record P-0000001 is identical, P-0000002 is altered, P-0000003 is missing, P-0000004 is new
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("PATIENT_ID	SAMPLE_ID	NECROSIS	ETHNICITY");
        returnValue.add("P-0000001	P-0000001-T01	YES	Asian");
        returnValue.add("P-0000002	P-0000002-T02	YES	Caucasian");
        returnValue.add("P-0000004	P-0000004-T03	NO	Caucasian");
        return returnValue;
    }

    private List<String> createNewDuplicateFileForImport() {
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("PATIENT_ID	SAMPLE_ID	NECROSIS	ETHNICITY");
        returnValue.add("P-0000004	P-0000004-T03	NO	Caucasian");
        returnValue.add("P-0000004	P-0000004-T03	NO	Caucasian");
        returnValue.add("P-0000004	P-0000004-T03	NO	Caucasian");
        return returnValue;
    }

    private List<String> createExistingDuplicateFileForImport() {
        //modified record
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("PATIENT_ID	SAMPLE_ID	NECROSIS	ETHNICITY");
        returnValue.add("P-0000002	P-0000002-T02	YES	Caucasian");
        returnValue.add("P-0000002	P-0000002-T02	YES	Caucasian");
        returnValue.add("P-0000002	P-0000002-T02	YES	Caucasian");
        return returnValue;
    }

    private List<String> createExcessRecordFileForImport() {
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("PATIENT_ID	SAMPLE_ID	NECROSIS	ETHNICITY");
        returnValue.add("P-0000005	P-0000005-T05	YES	Asian");
        returnValue.add("P-0000006	P-0000006-T06	NO	Caucasian");
        returnValue.add("P-0000007	P-0000007-T07	YES	Caucasian");
        returnValue.add("P-0000008	P-0000008-T08	YES	Caucasian");
        return returnValue;
    }

    private Set<String> createExpectedRecordsForNewImportWithRecordIdAsRecordNameField() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\nrecord_id,patient_id,sample_id,necrosis,ethnicity"
            + "\n4,P-0000004,P-0000004-T04,NO,Asian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForChangedImport() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000002,P-0000002-T02,YES,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForChangedImportWithRecordIdAsRecordNameField() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\nrecord_id,patient_id,sample_id,necrosis,ethnicity"
            + "\n4,P-0000002,P-0000002-T02,YES,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForVariedImportWithKeepExisting() {
        Set<String> returnStringSet = new HashSet<>();
        //Record P-0000001 is identical, P-0000002 is altered, P-0000003 is missing, P-0000004 is new
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000002,P-0000002-T02,YES,Caucasian"
            + "\nP-0000004,P-0000004-T03,NO,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForNewDuplicateImport() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000004,P-0000004-T03,NO,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForExistingDuplicateImport() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000002,P-0000002-T02,YES,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForNewImport() {
        Set<String> returnStringSet = new HashSet<>();
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000004,P-0000004-T04,NO,Asian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedRecordsForExcessImport() {
        Set<String> returnStringSet = new HashSet<>();
        //Record P-0000001 is identical, P-0000002 is altered, P-0000003 is missing, P-0000004 is new
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000005,P-0000005-T05,YES,Asian"
            + "\nP-0000006,P-0000006-T06,NO,Caucasian\n");
        returnStringSet.add("\npatient_id,sample_id,necrosis,ethnicity"
            + "\nP-0000007,P-0000007-T07,YES,Caucasian"
            + "\nP-0000008,P-0000008-T08,YES,Caucasian\n");
        return returnStringSet;
    }

    private Set<String> createExpectedSetForChangedDeletion() {
        Set<String> expectedSetForChangedDeletion = new HashSet<>();
        expectedSetForChangedDeletion.add("P-0000003");
        return expectedSetForChangedDeletion;
    }

    private Set<String> createExpectedSetForChangedDeletionWithRecordIdAsRecordNameField() {
        Set<String> expectedSetForChangedDeletionWithRecordIdAsRecordNameField = new HashSet<>();
        expectedSetForChangedDeletionWithRecordIdAsRecordNameField.add("2");
        expectedSetForChangedDeletionWithRecordIdAsRecordNameField.add("3");
        return expectedSetForChangedDeletionWithRecordIdAsRecordNameField;
    }

    private Set<String> createExpectedSetForDeletionWithExcessRecords() {
        Set<String> expectedSetForDeletionWithExcessRecords = new HashSet<>();
        expectedSetForDeletionWithExcessRecords.add("P-0000001");
        expectedSetForDeletionWithExcessRecords.add("P-0000002");
        expectedSetForDeletionWithExcessRecords.add("P-0000003");
        return expectedSetForDeletionWithExcessRecords;
    }

    private String compareRecordsForDeletionAndImport(Set<String> expectedSetForDeletion, Set<String> expectedRecordsForImport) {
        StringBuilder results = new StringBuilder();
        Set<String> returnedSetForDeletion = redcapSourceTestConfiguration.getRecordsPassedToRedcapSessionManagerForDeletion();
        Set<String> returnedRecordsForImport = redcapSourceTestConfiguration.getRecordsPassedToRedcapSessionManagerForUpload();

        if (expectedSetForDeletion != null || returnedSetForDeletion != null) {
            if (returnedSetForDeletion == null || (returnedSetForDeletion != null && !returnedSetForDeletion.equals(expectedSetForDeletion))) {
                String expectedSetForDeletionString = "null";
                if (expectedSetForDeletion != null) {
                    expectedSetForDeletionString = expectedSetForDeletion.toString();
                }
                String returnedSetForDeletionString = "null";
                if (returnedSetForDeletion != null) {
                    returnedSetForDeletionString = returnedSetForDeletion.toString();
                }
                results.append("Returned set for deletion differs from expected:\n"
                        + "Expected: " + expectedSetForDeletionString + "\n"
                        + "Returned: " + returnedSetForDeletionString + "\n");
            }
        }
        if (returnedRecordsForImport != null || expectedRecordsForImport != null) {
            if (returnedRecordsForImport == null || (returnedRecordsForImport != null && !returnedRecordsForImport.equals(expectedRecordsForImport))) {
                results.append("Returned string for import differs from expected:\n"
                        + "Expected: " + expectedRecordsForImport + "\n"
                        + "Returned: " + returnedRecordsForImport + "\n");
            }
        }
        return results.toString();
    }

    private String getDifferenceBetweenReturnedAndExpected(List<Map<String, String>> dataReturned, List<Map<String, String>> expectedReturnValue) {
        if (dataReturned == null) {
            return "Data returned was null";
        }
        if (expectedReturnValue == null) {
            return "Expected return value was null";
        }
        if (dataReturned.size() != expectedReturnValue.size()) {
            return "Different number of records returned (" + Integer.toString(dataReturned.size()) + ") versus expected (" + Integer.toString(expectedReturnValue.size()) + ")";
        }
        StringBuilder result = new StringBuilder();
        for (int pos = 0; pos < dataReturned.size() ; pos++) {
            Map<String, String> dataReturnedItem = dataReturned.get(pos);
            Map<String, String> expectedReturnValueItem = expectedReturnValue.get(pos);
            if (dataReturnedItem.size() != expectedReturnValueItem.size()) {
                result.append("\n\tDifferent number of attribute for items at index " + Integer.toString(pos) + " returnedItem has (" + Integer.toString(dataReturnedItem.size()) + ") versus expectedItem has  (" + Integer.toString(expectedReturnValueItem.size()) + ")");
            }
            for (String key : dataReturnedItem.keySet()) {
                if (!expectedReturnValueItem.containsKey(key)) {
                    result.append("\n\tattribute name '" + key + "' returned but expected value does not contain any key with that name (in record number " + Integer.toString(pos) + ")");
                } else {
                    String dataReturnedValue = dataReturnedItem.get(key);
                    String expectedValue = expectedReturnValueItem.get(key);
                    if (!dataReturnedValue.equals(expectedValue)) {
                        result.append("\n\tattribute name '" + key + "' has value '" + dataReturnedValue + "' in returned value in record number " + Integer.toString(pos) + ", but expected value was '" + expectedValue);
                    }
               }
            }
        }
        return result.toString();
    }

    private List<Map<String, String>> createExpectedReturnExportFromRedcapOneNumericAttributeName() {
        List<Map<String, String>> returnValue = new ArrayList<Map<String, String>>();
        Map<String, String> returnValue1 = new HashMap<String, String>();
        Map<String, String> returnValue2 = new HashMap<String, String>();
        Map<String, String> returnValue3 = new HashMap<String, String>();
        returnValue1.put("PATIENT_ID", "P-0000004");
        returnValue1.put("CRDB_CONSENT_DATE_DAYS", "14484");
        returnValue1.put("PARTA_CONSENTED_12_245", "YES");
        returnValue.add(returnValue1);
        returnValue2.put("PATIENT_ID", "P-0000012");
        returnValue2.put("CRDB_CONSENT_DATE_DAYS", "21192");
        returnValue2.put("PARTA_CONSENTED_12_245", "YES");
        returnValue.add(returnValue2);
        returnValue3.put("PATIENT_ID", "P-9999999");
        returnValue3.put("CRDB_CONSENT_DATE_DAYS", "99999");
        returnValue3.put("PARTA_CONSENTED_12_245", "");
        returnValue.add(returnValue3);
        return returnValue;
    }

}
