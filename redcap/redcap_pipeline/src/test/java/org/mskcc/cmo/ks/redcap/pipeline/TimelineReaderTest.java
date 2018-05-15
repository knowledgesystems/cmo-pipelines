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

package org.mskcc.cmo.ks.redcap.pipeline;

import java.io.*;
import java.lang.StringBuilder;
import java.util.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.cmo.ks.redcap.pipeline.TimelineReader;
import org.mskcc.cmo.ks.redcap.pipeline.ClinicalDataReaderTestConfiguration;
import org.mskcc.cmo.ks.redcap.pipeline.util.JobParameterUtils;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.*;

@ContextConfiguration(classes=ClinicalDataReaderTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TimelineReaderTest {

    @Autowired
    private TimelineReader timelineReader;

    @Autowired
    private JobParameterUtils jobParameterUtils;
   
    @Test
    public void testTimelineReaderWithOneMaskedProject() {
        List<String> listOfMaskedProjects = new ArrayList<>();
        listOfMaskedProjects.add(ClinicalDataReaderTestConfiguration.MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TITLE);
        listOfMaskedProjects.add(ClinicalDataReaderTestConfiguration.MSKIMPACT_MASKED_TIMELINE_PROJECT_TITLE);
        jobParameterUtils.setListOfMaskedProjects(listOfMaskedProjects);
        compareReturnedToExpectedTimelineRecords(makeMaskedTimelineProjectsExpectedPatientToRecordMap());
    }

    public void compareReturnedToExpectedTimelineRecords(Map<String, Map<String, String>> expectedPatientToRecordMap) {
        ExecutionContext ec = new ExecutionContext();
        timelineReader.open(ec);
        StringBuilder errorMessage = new StringBuilder("\nFailures:\n");
        int failCount = 0;
        Map<String, Map<String, String>> returnedPatientToRecordMap = new HashMap<>();
        // compare returned values to expected
        while (true) {
            Map<String, String> returnedRecord = null;
            try {
                returnedRecord = timelineReader.read();
            } catch (Exception e) {
                Assert.fail("Exception thrown from TimelineReader.read() : " + e.getMessage());
            }
            if (returnedRecord == null) {
                break;
            }
            String patient_id = returnedRecord.get("PATIENT_ID");
            if (patient_id == null) {
                failCount = failCount + 1;
                errorMessage.append("TimelineReader.read() returned record with null PATIENT_ID : " + recordToString(returnedRecord) + "\n");
                continue;
            }
            returnedPatientToRecordMap.put(patient_id, returnedRecord);
        }
        for (String returnedPatientId : returnedPatientToRecordMap.keySet()) {
            if (returnedPatientId == null) {
                continue; //already reported above
            }
            Map<String, String> returnedRecord = returnedPatientToRecordMap.get(returnedPatientId);
            Map<String, String> expectedRecord = expectedPatientToRecordMap.get(returnedPatientId);
            if (expectedRecord == null) {
                failCount = failCount + 1;
                errorMessage.append("Record from TimelineReader.read() has unexpected PATIENT_ID : " + recordToString(returnedRecord) + "\n");
                continue;
            }
            Set<String> returnedRecordKeys = returnedRecord.keySet();
            Set<String> expectedRecordKeys = expectedRecord.keySet();
            for (String key : returnedRecordKeys) {
                String returnedRecordValue = returnedRecord.get(key);
                String expectedRecordValue = expectedRecord.get(key);
                if (expectedRecordValue == null) {
                    failCount = failCount + 1;
                    errorMessage.append("Record from TimelineReader.read() has unexpected field " + key + " : " + recordToString(returnedRecord) + "\n");
                    continue;
                }
                expectedRecordKeys.remove(key);
                if (returnedRecordValue != null && !returnedRecordValue.equals(expectedRecordValue)) {
                    failCount = failCount + 1;
                    errorMessage.append("Record from TimelineReader.read() has unexpected value (expected: " + expectedRecordValue + ") in field " + key + " : " + recordToString(returnedRecord) + "\n");
                    continue;
                }
            }
            if (!expectedRecordKeys.isEmpty()) {
                failCount = failCount + 1;
                errorMessage.append("Record from TimelineReader.read() has missing fields ( ");
                for (String key : expectedRecordKeys) {
                    errorMessage.append(key + " ");
                }
                errorMessage.append(") " + recordToString(returnedRecord) + "\n");
            }
        }
        for (String expectedPatientId : expectedPatientToRecordMap.keySet()) {
            if (!returnedPatientToRecordMap.containsKey(expectedPatientId)) {
                failCount = failCount + 1;
                errorMessage.append("Record expected from TimelineReader.read() was not seen : " + recordToString(expectedPatientToRecordMap.get(expectedPatientId)) + "\n");
                continue;
            }
        }
        if (failCount > 0) {
            Assert.fail(errorMessage.toString());
        }
    }

    private String recordToString(Map<String, String> record) {
        StringBuilder returnString = new StringBuilder("[");
        int initialStringLength = returnString.length();
        LinkedHashSet<String> keys = new LinkedHashSet<>(record.keySet());
        if (keys.contains("patient_id")) {
            addToRecordToStringOutput(returnString, "patient_id", record, initialStringLength);
            keys.remove("patient_id");
        }
        if (keys.contains("patient_id")) {
            addToRecordToStringOutput(returnString, "patient_id", record, initialStringLength);
            keys.remove("patient_id");
        }
        for (String key : record.keySet()) {
            addToRecordToStringOutput(returnString, key, record, initialStringLength);
        }
        returnString.append("]");
        return returnString.toString();
    }

    private void addToRecordToStringOutput(StringBuilder returnString, String key, Map<String, String> record, int initialStringLength) {
        if (returnString.length() > initialStringLength) {
            returnString.append(",");
        }
        returnString.append(key + ":" + record.get(key));
    }

    private Map<String, Map<String, String>> makeMaskedTimelineProjectsExpectedPatientToRecordMap() {
        Map<String, Map<String, String>> expectedRecordMap = new HashMap<>(3);
        Map<String, String> nextRecord = new HashMap<>();
        nextRecord.put("PATIENT_ID", "P-0000001");
        nextRecord.put("START_DATE", "5");
        nextRecord.put("STOP_DATE", "20");
        expectedRecordMap.put(nextRecord.get("PATIENT_ID"), nextRecord);
        nextRecord = new HashMap<>();
        nextRecord.put("PATIENT_ID", "P-0000002");
        nextRecord.put("START_DATE", "12");
        nextRecord.put("STOP_DATE", "87");
        expectedRecordMap.put(nextRecord.get("PATIENT_ID"), nextRecord);
        nextRecord = new HashMap<>();
        nextRecord.put("PATIENT_ID", "P-0000003");
        nextRecord.put("START_DATE", "13");
        nextRecord.put("STOP_DATE", "24");
        expectedRecordMap.put(nextRecord.get("PATIENT_ID"), nextRecord);
        return expectedRecordMap;
    }
}
