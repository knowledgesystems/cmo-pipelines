/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr;

import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrTestConfiguration;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.MskimpactAge;

import java.text.*;
import java.util.*;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@ContextConfiguration(classes=CvrTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CalculateAgeAtSeqReportForPatientTest {

    @Autowired
    private CVRUtilities cvrUtilities;

    private SimpleDateFormat fileCreationDateFormat = new SimpleDateFormat("yyyy-MM-dd zzz");

    /**
     * Test calculation of age at seq report using the year old file creation date as
     * the reference calculation date.
     * @throws ParseException
     */
    @Test
    public void testAgeAtSeqReportYearOldFileCreationDate() throws ParseException {
        StringBuilder errorMessage = new StringBuilder("\nFailures:\n");
        Date yearOldFileCreationDate = fileCreationDateFormat.parse("2016-12-31 GMT");
        Map<String, String> yearOldFileExpectedAges = makeMockYearOldFileExpectedAge();
        int failCount = testMockAgeAtSeqReportCalculationsByDate(yearOldFileCreationDate, yearOldFileExpectedAges, errorMessage);

        if (failCount > 0) {
            Assert.fail(errorMessage.toString());
        }
    }

    /**
     * Test calculation of age at seq report using the current  file creation date as
     * the reference calculation date
     * @throws ParseException
     */
    @Test
    public void testAgeAtSeqReportCurrentFileCreationDate() throws ParseException {
        StringBuilder errorMessage = new StringBuilder("\nFailures:\n");
        Date currentFileCreationDate = fileCreationDateFormat.parse("2017-11-27 GMT");
        Map<String, String> currentFileExpectedAges = makeMockCurrentFileExpectedAge();
        int failCount = testMockAgeAtSeqReportCalculationsByDate(currentFileCreationDate, currentFileExpectedAges, errorMessage);

        if (failCount > 0) {
            Assert.fail(errorMessage.toString());
        }
    }

    /**
     * Helper function for testing the mock data and comparing to expected age at seq report values.
     * @param referenceCalculationDate
     * @param expectedValues
     * @param errorMessage
     * @return
     * @throws ParseException
     */
    private Integer testMockAgeAtSeqReportCalculationsByDate(Date referenceCalculationDate, Map<String, String> expectedValues, StringBuilder errorMessage) throws ParseException {
        Map<String, List<CVRClinicalRecord>> cvrPatientToRecordMap = makeMockCvrClinicalRecords();
        List<MskimpactAge> mskimpactAgeRecords = makeMockMskimpactAgeRecords();
        int failCount = 0;

        for (MskimpactAge mskimpactAgeRecord : mskimpactAgeRecords) {
            cvrUtilities.calculateAgeAtSeqReportedYearsForPatient(referenceCalculationDate,
                    cvrPatientToRecordMap.get(mskimpactAgeRecord.getPATIENT_ID()), mskimpactAgeRecord.getAGE());
            for (CVRClinicalRecord cvrClinicalRecord : cvrPatientToRecordMap.get(mskimpactAgeRecord.getPATIENT_ID())) {
                if (cvrClinicalRecord.getAGE_AT_SEQ_REPORTED_YEARS() == null) {
                    failCount += 1;
                    errorMessage.append("testAgeAtSeqReportByFileCreationDate(),  AGE_SEQ_REPORTED_YEARS is null for record '")
                            .append(cvrClinicalRecord.getSAMPLE_ID())
                            .append("' + \n");
                    continue;
                }
                if (!cvrClinicalRecord.getAGE_AT_SEQ_REPORTED_YEARS().equals(expectedValues.get(cvrClinicalRecord.getSAMPLE_ID()))) {
                    failCount += 1;
                    errorMessage.append("testAgeAtSeqReportByFileCreationDate(),  AGE_SEQ_REPORTED_YEARS value mismatch for record '")
                            .append(cvrClinicalRecord.getSAMPLE_ID())
                            .append("'. Expected '")
                            .append(expectedValues.get(cvrClinicalRecord.getSAMPLE_ID()))
                            .append("' and got '")
                            .append(cvrClinicalRecord.getAGE_AT_SEQ_REPORTED_YEARS())
                            .append("' \n");
                }
            }
        }
        return failCount;
    }

    private Map<String, String> makeMockYearOldFileExpectedAge() {
        Map<String, String> map = new HashMap<>();
        map.put("P-0000001-T01-IM5", ">90");
        map.put("P-0000002-T01-IM5", "10");
        map.put("P-0000003-T01-IM5", "44");
        map.put("P-0000003-T02-IM5", "45");
        map.put("P-0000005-T01-IM5", "NA");
        return map;
    }

    private Map<String, String> makeMockCurrentFileExpectedAge() {
        Map<String, String> map = new HashMap<>();
        map.put("P-0000001-T01-IM5", ">90");
        map.put("P-0000002-T01-IM5", "10");
        map.put("P-0000003-T01-IM5", "43");
        map.put("P-0000003-T02-IM5", "44");
        map.put("P-0000005-T01-IM5", "NA");
        return map;
    }

    private Map<String, List<CVRClinicalRecord>> makeMockCvrClinicalRecords() {
        Map<String, List<CVRClinicalRecord>> records = new HashMap<>();
        List<CVRClinicalRecord> record = new ArrayList<>();
        CVRClinicalRecord record1 = new CVRClinicalRecord();
        record1.setPATIENT_ID("P-0000001");
        record1.setSAMPLE_ID("P-0000001-T01-IM5");
        record1.setSEQ_DATE("Tue, 16 Feb 2016 18:20:03 GMT");
        record.add(record1);
        records.put(record1.getPATIENT_ID(), record);

        record = new ArrayList<>();
        CVRClinicalRecord record2 = new CVRClinicalRecord();
        record2.setPATIENT_ID("P-0000002");
        record2.setSAMPLE_ID("P-0000002-T01-IM5");
        record2.setSEQ_DATE("Sat, 31 Dec 2016 19:45:05 GMT");
        record.add(record2);
        records.put(record2.getPATIENT_ID(), record);

        record = new ArrayList<>();
        CVRClinicalRecord record3 = new CVRClinicalRecord();
        record3.setPATIENT_ID("P-0000003");
        record3.setSAMPLE_ID("P-0000003-T01-IM5");
        record3.setSEQ_DATE("Wed, 11 Nov 2015 10:19:02 GMT");
        record.add(record3);

        CVRClinicalRecord record4 = new CVRClinicalRecord();
        record4.setPATIENT_ID("P-0000003");
        record4.setSAMPLE_ID("P-0000003-T02-IM5");
        record4.setSEQ_DATE("Sat, 16 Jan 2016 08:46:08 GMT");
        record.add(record4);
        records.put(record3.getPATIENT_ID(), record);

        record = new ArrayList<>();
        CVRClinicalRecord record5 = new CVRClinicalRecord();
        record5.setPATIENT_ID("P-0000005");
        record5.setSAMPLE_ID("P-0000005-T01-IM5");
        record5.setSEQ_DATE("");
        record.add(record5);
        records.put(record5.getPATIENT_ID(), record);

        return records;
    }

    private List<MskimpactAge> makeMockMskimpactAgeRecords() {
        List<MskimpactAge> ageRecords = new ArrayList<>();
        MskimpactAge record1 = new MskimpactAge();
        record1.setPATIENT_ID("P-0000001");
        record1.setAGE("95");
        ageRecords.add(record1);

        MskimpactAge record2 = new MskimpactAge();
        record2.setPATIENT_ID("P-0000002");
        record2.setAGE("10");
        ageRecords.add(record2);

        MskimpactAge record3 = new MskimpactAge();
        record3.setPATIENT_ID("P-0000003");
        record3.setAGE("45");
        ageRecords.add(record3);

        return ageRecords;
    }
}
