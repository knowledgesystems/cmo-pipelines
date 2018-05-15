/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline.util;

import java.util.*;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import java.text.ParseException;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.ClinicalRecord;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientDemographics;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;
import org.mskcc.cmo.ks.ddp.source.internal.DDPSourceTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=DDPUtilsTestConfiguration.class)
public class DDPUtilsTest {

    @Resource(name="mockCompositePatientRecords")
    private Map<String, DDPCompositeRecord> mockCompositePatientRecords;

    @Autowired
    private DDPUtils ddpUtils;

    private static long MILLISECONDS_PER_STANDARD_DAY = 24 * 60 * 60 * 1000;
    private static long MINIMUM_SAFE_MILLISECONDS_TO_MIDNIGHT = 2 * 1000; // two seconds to midnight will require a pause

    @Test
    public void testMockCompositePatientRecordsInitialization() {
        if (mockCompositePatientRecords.isEmpty()) {
            Assert.fail("mockCompositePatientRecords not initialized properly!");
        }
    }

    /* Tests for resolvePatientCurrentAge()
     * if PatientBirthDate is null or empty -- anonymize PatientAge. Cases : 0, 17, 18, 89, 90, 91
     * otherwise, base age on birthdate :
     *    category 1 : patient deceased : days since birth subtracted from days since death date. Cases : death date is 20 * 365.2422 + {364 365 366, 367} days after birth
     *    category 2 : patient not deceased : days since birth subtracted from todays date. Cases : birth date is 20 * 365.2422 + {364 365 366 367} days before today .. but if now() is within 2 seconds of midnight, then wait until after
    */

    @Test(expected = NullPointerException.class)
    public void resolvePatientCurrentAgeNullPointerExceptionTest() throws ParseException {
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, null, null);
    }

    @Test(expected = ParseException.class)
    public void resolvePatientCurrentAgeParseExceptionTest() throws ParseException {
        resolvePatientCurrentAgeAndAssert("invalid date", "", "", "", 0, 50, "18");
    }

    @Test
    // if PatientBirthDate is null or empty -- anonymize PatientAge. Cases : 0, 17, 18, 89, 90, 91
    public void resolvePatientCurrentAgeNoBirthdateTest() throws ParseException {
        // demographics age should be used first, if available
        resolvePatientCurrentAgeAndAssert("", "", "", "", 0, 50, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 0, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", 17, 50, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 17, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", 18, 50, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 18, "18");
        resolvePatientCurrentAgeAndAssert("", "", "", "", 89, 50, "89");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 89, "89");
        resolvePatientCurrentAgeAndAssert("", "", "", "", 90, 50, "90");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 90, "90");
        resolvePatientCurrentAgeAndAssert("", "", "", "", 91, 50, "90");
        resolvePatientCurrentAgeAndAssert("", "", "", "", null, 91, "90");
    }

    @Test
    //category 1 : patient deceased : days since birth subtracted from days since death date. Cases : death date is 20 * 365.2422 + {364 365 366 367} days after birth
    public void resolvePatientCurrentAgeBirthdateDead() throws ParseException {
        // demographics dateOfBirth and dateOfDeath should be used first, if available
        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1990-12-30", "ignored", null, null, "20"); // 7668 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1990-12-30", null, null, "20"); // 7668 days later
        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1990-12-30", "ignored", null, null, "20"); // 7668 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1990-12-30", null, null, "20"); // 7668 days later

        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1990-12-31", "ignored", null, null, "20"); // 7669 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1990-12-31", null, null, "20"); // 7669 days later
        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1990-12-31", "ignored", null, null, "20"); // 7669 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1990-12-31", null, null, "20"); // 7669 days later

        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1991-01-00", "ignored", null, null, "20"); // 7670 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1991-01-00", null, null, "20"); // 7670 days later
        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1991-01-00", "ignored", null, null, "20"); // 7670 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1991-01-00", null, null, "20"); // 7670 days later

        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1991-02-00", "ignored", null, null, "21"); // 7671 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1991-02-00", null, null, "21"); // 7671 days later
        resolvePatientCurrentAgeAndAssert("1970-01-01", "ignored", "1991-02-00", "ignored", null, null, "21"); // 7671 days later
        resolvePatientCurrentAgeAndAssert("", "1970-01-01", "", "1991-02-00", null, null, "21"); // 7671 days later
    }

    @Test
    public void resolvePatientCurrentAgeBirthdateAlive() throws ParseException {
        wait_until_midnight_if_necessary();
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String todayDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        // approximately 2 days before 21st birthday, with no hours, minutes, or seconds
        calendar.add(Calendar.DATE, - ((int)Math.floor(DDPUtils.DAYS_TO_YEARS_CONVERSION * 21) - 2));
        String birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7668 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7668 days later
        calendar.add(Calendar.DATE, -1); // one day older
        birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7669 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7669 days later
        calendar.add(Calendar.DATE, -1); // two days older
        // Note on their birthday they are not actually 21 yet
        // Dates are:
        //   todayDateString: 2018-5-14
        //   birthDateString: 1997-5-14
        // And computed values are:
        //   diff in days: 7670
        //   age in years: 20.999763992222146
        //   years as int: 20
        birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7670 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7670 days later
        calendar.add(Calendar.DATE, -1); // three days older
        birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "21"); // 7671 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "21"); // 7671 days later
    }

    /* Tests for resolvePatientSex()
    * if patientDemographics.getGender() and cohortPatient.getPTSEX() are null or empty, return NA
    * otherwise, if patientDemographics.getGender() is not null or empty, and equals "M" or "MALE" (ignore case) then return "Male"
    * otherwise, if patientDemographics.getGender() is not null or empty, return "Female"
    * otherwise, if cohortPatient.getPTSEX() equals "M" or "MALE" (ignore case) then return "Male"
    * otherwise, return "Female"
    */

    @Test
    public void resolvePatientSexTest() {
        resolvePatientSexAndAssert(null, null, "NA");
        resolvePatientSexAndAssert("", "", "NA");
        resolvePatientSexAndAssert(null, "", "NA");
        resolvePatientSexAndAssert("m", "F", "Male");
        resolvePatientSexAndAssert("MALE", null, "Male");
        resolvePatientSexAndAssert("FEMALE", "male", "Female");
        // TODO if we were given "NA" as patientDemographics.getGender() we would return "Female" -- is this correct?
        resolvePatientSexAndAssert("UNRECOGNIZED", "", "Female");
        resolvePatientSexAndAssert("", "F", "Female");
        resolvePatientSexAndAssert(null, "Male", "Male");
    }

    /* Tests for resolveOsStatus()
    * if compositePatient.getCohortPatient() and compositePatient.getCohortPatient().getPTVITALSTATUS() is not null or empty
    *   and getPTVITALSTATUS() equals (ignore case) "ALIVE" then "LIVING"
    * otherwise if compositePatient.getCohortPatient() and compositePatient.getCohortPatient().getPTVITALSTATUS() is not null or empty
    *   then "DECEASED"
    * otherwise if compositePatient.getPatientDemographics()
    *   and (compositePatient.getPatientDemographics().getDeceasedDate() or compositePatient.getPatientDemographics().getPTDEATHDTE() are not null or empty)
    *   then "DECEASED"
    * otherwise if compositePatient.getPatientDemographics()
    *   then "LIVING"
    * otherwise "NA"
    */

    @Test
    public void resolveOsStatusTest() {
        resolveOsStatusAndAssert(true, true, "Alive", "ignore", "ignore", "LIVING");
        resolveOsStatusAndAssert(true, true, "ANYTHING_CAN_GO_HERE", "ignore", "ignore", "DECEASED");
        resolveOsStatusAndAssert(true, true, "", "ANYTHING_CAN_GO_HERE", null, "DECEASED");
        resolveOsStatusAndAssert(false, true, null, null, "ANYTHING_CAN_GO_HERE", "DECEASED");
        resolveOsStatusAndAssert(true, true, "", "", "", "LIVING");
        resolveOsStatusAndAssert(true, true, null, null, null, "LIVING");
        resolveOsStatusAndAssert(true, false, null, "ignore", "ignore", "NA");
        resolveOsStatusAndAssert(false, false, null, null, null, "NA");
    }

    /* Tests for resolveOsMonths()
    * if os status is "LIVING" and getPLALASTACTVDTE is null or "", expect "NA"
    * otherwise os status is not "LIVING" and getDeceasedDate is null or "", expect "NA"
    * otherwise if first diagnosis date is null, expect "NA"
    * otherwise return the difference in months between first tumor diagnosis date and
    *   either compositePatient.getPatientDemographics().getPLALASTACTVDTE() if living
    *   or compositePatient.getPatientDemographics().getDeceasedDate() if not living
    */

    @Test(expected = ParseException.class)
    public void resolveOsMonthsParseExceptionTest() throws ParseException {
        resolveOsMonthsAndAssert("LIVING", "invalid date", "ignore", "ignore", "NA");
    }

    @Test
    public void resolveOsMonthsTest() throws ParseException {
        resolveOsMonthsAndAssert("LIVING", null, "ignore", "2016-01-02", "NA");
        resolveOsMonthsAndAssert("LIVING", "", "ignore", "2016-01-02", "NA");
        resolveOsMonthsAndAssert("DECEASED", "ignore", null, "2016-01-02", "NA");
        resolveOsMonthsAndAssert("DECEASED", "ignore", "", "2016-01-02", "NA");
        resolveOsMonthsAndAssert("DECEASED", "ignore", "2016-01-02", null, "NA");
        resolveOsMonthsAndAssert("LIVING", "2018-04-20", "ignore", "2018-02-19",
            String.format("%.3f", 60 / DDPUtils.DAYS_TO_MONTHS_CONVERSION));
        resolveOsMonthsAndAssert("DECEASED", "ignore", "2018-04-21", "2018-02-19",
            String.format("%.3f", 61 / DDPUtils.DAYS_TO_MONTHS_CONVERSION));
    }

    /* Tests for resolvePatientAgeAtDiagnosis()
    * // TODO is this correct?  it is just the current age, not age at diagnosis
    * if patientDemographics.getPTBIRTHDTE() is null or empty and patientDemographics.getDateOfBirth() is null or empty
    *   and patientDemographics.getCurrentAge() is not null,
    *   use patientDemographics.getCurrentAge()
    *   otherwise if patient birth dates are null or empty use cohortPatient.getAGE() (even if it is null or empty)
    * otherwise if patientDemographics.getPTBIRTHDTE() is not null or empty, use that
    *   otherwise use patientDemographics.getDateOfBirth() (even if null or empty)
    *   then subtract birth date from current date and anonymize the result
    */

    @Test(expected = NullPointerException.class)
    public void resolvePatientAgeAtDiagnosisNullPointerExceptionTest() throws Exception {
        resolvePatientAgeAtDiagnosisAndAssert(null, null, null, null, "18");
    }

    @Test(expected = ParseException.class)
    public void resolvePatientAgeAtDiagnosisParseExceptionTest() throws Exception {
        resolvePatientAgeAtDiagnosisAndAssert("invalid date", null, null, null, "18");
    }

    @Test
    public void resolvePatientAgeAtDiagnosisTest() throws ParseException {
        resolvePatientAgeAtDiagnosisAndAssert(null, null, 91, 15, "90");
        resolvePatientAgeAtDiagnosisAndAssert("", null, 91, 15, "90");
        resolvePatientAgeAtDiagnosisAndAssert("", "", 91, 15, "90");
        resolvePatientAgeAtDiagnosisAndAssert(null, null, null, 15, "18");
        resolvePatientAgeAtDiagnosisAndAssert("", null, null, 15, "18");
        resolvePatientAgeAtDiagnosisAndAssert("", "", null, 15, "18");

        wait_until_midnight_if_necessary();
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // approximately 2 days before 21st birthday, with no hours, minutes, or seconds
        calendar.add(Calendar.DATE, - ((int)Math.floor(DDPUtils.DAYS_TO_YEARS_CONVERSION * 21) - 1));
        String birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientAgeAtDiagnosisAndAssert(birthDateString, null, null, null, "20");
        calendar.add(Calendar.DATE, -1); // one day older
        birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientAgeAtDiagnosisAndAssert(birthDateString, null, null, null, "20"); // still 20 on birthday
        calendar.add(Calendar.DATE, -1); // two days older
        birthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientAgeAtDiagnosisAndAssert(null, birthDateString, null, null, "21"); // now 21 on day after birthday

        // test which birth date takes precedence
        calendar.add(Calendar.DATE, 2); // two days younger (again) - age 20
        String alternateBirthDateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DATE);
        resolvePatientAgeAtDiagnosisAndAssert(alternateBirthDateString, birthDateString, null, null, "20"); // use 20 birthday not 21
    }

    /* Tests for getFirstTumorDiagnosisDate()
    * if patientDiagnosis is null, return null
    * otherwise if patientDiagnosis is empty, return null
    * otherwise find earliest patient diagnosis date
    */
    @Test
    public void getFirstTumorDiagnosisDateTest() throws ParseException {
        getFirstTumorDiagnosisDateAndAssert(null, null);
        List<String> patientDiagnoses = new ArrayList<String>();
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, null);
        patientDiagnoses.add(null);
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, null);
        patientDiagnoses.add("");
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, null);
        patientDiagnoses.add("2017-06-18");
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, "2017-06-18");
        patientDiagnoses.add("2017-06-17");
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, "2017-06-17");
        patientDiagnoses.set(0, "2017-06-16");
        getFirstTumorDiagnosisDateAndAssert(patientDiagnoses, "2017-06-16");
    }

    /* Tests for resolveTimelineEventDateInDays()
    * subtract birth date from event date and return the result
    */

    @Test(expected = NullPointerException.class)
    public void resolveTimelineEventDateInDaysNullPointerExceptionTest() throws Exception {
        Assert.assertEquals(null, ddpUtils.resolveTimelineEventDateInDays(null, null));
    }

    @Test
    public void resolveTimelineEventDateInDaysTest() throws ParseException {
        Assert.assertEquals("60", ddpUtils.resolveTimelineEventDateInDays("2018-02-19", "2018-04-20"));
        // we probably would not get data like this
        Assert.assertEquals("-60", ddpUtils.resolveTimelineEventDateInDays("2018-04-20", "2018-02-19"));
    }

    /* Tests for constructRecord()
    * exception thrown if "getFieldNames" method does not exist
    * exception thrown if one of the values are null
    */

    @Test(expected = NoSuchMethodException.class)
    public void constructRecordNoSuchMethodExceptionTest() throws Exception {
        ddpUtils.constructRecord(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void constructRecordNullPointerExceptionTest() throws Exception {
        ddpUtils.constructRecord(new ClinicalRecord());
    } 

    @Test
    public void constructRecordTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setOS_MONTHS("3.123");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY");
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // it does a trim too
        clinicalRecord.setSURGERY("MY_SURGERY");
        String expectedValue = "MY_PT_ID\t20\tFemale\tLIVING\t3.123\tMY_RADIATION_THERAPY\tMY_CHEMOTHERAPY\tMY_SURGERY";
        String returnedValue = ddpUtils.constructRecord(clinicalRecord);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void resolvePatientCurrentAgeAndAssert(String birthDte, String dateOfBirth, String deathDte, String deceasedDate, Integer demographicsAge, Integer cohortAge, String expectedValue) throws ParseException {
        DDPCompositeRecord testPatient = new DDPCompositeRecord();
        PatientDemographics testDemographics = new PatientDemographics();
        CohortPatient testCohortPatient = new CohortPatient();
        testDemographics.setPTBIRTHDTE(birthDte);
        testDemographics.setDateOfBirth(dateOfBirth);
        testDemographics.setPTDEATHDTE(deathDte);
        testDemographics.setDeceasedDate(deceasedDate);
        testDemographics.setCurrentAge(demographicsAge);
        testCohortPatient.setAGE(cohortAge);
        testPatient.setPatientDemographics(testDemographics);
        testPatient.setCohortPatient(testCohortPatient);
        String returnedValue = ddpUtils.resolvePatientCurrentAge(testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void wait_until_midnight_if_necessary() {
        while (true) {
            long utcMilliseconds = (new Date()).getTime();
            long thisDayMilliseconds = utcMilliseconds % MILLISECONDS_PER_STANDARD_DAY;
            if (MILLISECONDS_PER_STANDARD_DAY - thisDayMilliseconds > MINIMUM_SAFE_MILLISECONDS_TO_MIDNIGHT) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
    }

    private void resolvePatientSexAndAssert(String gender, String sex, String expectedValue) {
        DDPCompositeRecord testPatient = new DDPCompositeRecord();
        PatientDemographics testDemographics = new PatientDemographics();
        CohortPatient testCohortPatient = new CohortPatient();
        testDemographics.setGender(gender);
        testCohortPatient.setPTSEX(sex);
        testPatient.setPatientDemographics(testDemographics);
        testPatient.setCohortPatient(testCohortPatient);
        String returnedValue = ddpUtils.resolvePatientSex(testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void resolveOsStatusAndAssert(boolean initCohortPatient, boolean initDemographics, String vitalStatus, String deceasedDate, String deathDte, String expectedValue) {
        DDPCompositeRecord testPatient = new DDPCompositeRecord();
        if (initDemographics) {
            PatientDemographics testDemographics = new PatientDemographics();
            testDemographics.setPTDEATHDTE(deathDte);
            testDemographics.setDeceasedDate(deceasedDate);
            testPatient.setPatientDemographics(testDemographics);
        }
        if (initCohortPatient) {
            CohortPatient testCohortPatient = new CohortPatient();
            testCohortPatient.setPTVITALSTATUS(vitalStatus);
            testPatient.setCohortPatient(testCohortPatient);
        }
        String returnedValue = ddpUtils.resolveOsStatus(testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void resolveOsMonthsAndAssert(String osStatus, String plaLastActvDte, String deceasedDate, String firstDiagnosisDate, String expectedValue) throws ParseException {
        DDPCompositeRecord testPatient = new DDPCompositeRecord();
        PatientDemographics testDemographics = new PatientDemographics();
        testDemographics.setDeceasedDate(deceasedDate);
        testDemographics.setPLALASTACTVDTE(plaLastActvDte);
        testPatient.setPatientDemographics(testDemographics);
        PatientDiagnosis patientDiagnosis = new PatientDiagnosis();
        patientDiagnosis.setTumorDiagnosisDate(firstDiagnosisDate);
        testPatient.setPatientDiagnosis(Collections.singletonList(patientDiagnosis));
        String returnedValue = ddpUtils.resolveOsMonths(osStatus, testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void resolvePatientAgeAtDiagnosisAndAssert(String birthDte, String dateOfBirth, Integer demographicsAge, Integer cohortAge, String expectedValue) throws ParseException {
        DDPCompositeRecord testPatient = new DDPCompositeRecord();
        PatientDemographics testDemographics = new PatientDemographics();
        CohortPatient testCohortPatient = new CohortPatient();
        testDemographics.setPTBIRTHDTE(birthDte);
        testDemographics.setDateOfBirth(dateOfBirth);
        testDemographics.setCurrentAge(demographicsAge);
        testCohortPatient.setAGE(cohortAge);
        testPatient.setPatientDemographics(testDemographics);
        testPatient.setCohortPatient(testCohortPatient);
        String returnedValue = ddpUtils.resolvePatientAgeAtDiagnosis(testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void getFirstTumorDiagnosisDateAndAssert(List<String> tumorDiagnosisDates, String expectedValue) throws ParseException {
        String returnedValue;
        if (tumorDiagnosisDates == null) {
            returnedValue = ddpUtils.getFirstTumorDiagnosisDate(null);
        } else {
            List<PatientDiagnosis> patientDiagnoses = new ArrayList<>();
            for (String tumorDiagnosisDate : tumorDiagnosisDates) {
                PatientDiagnosis patientDiagnosis = new PatientDiagnosis();
                patientDiagnosis.setTumorDiagnosisDate(tumorDiagnosisDate);
                patientDiagnoses.add(patientDiagnosis);
            }
            returnedValue = ddpUtils.getFirstTumorDiagnosisDate(patientDiagnoses);
        }
        Assert.assertEquals(expectedValue, returnedValue);
    }
}
