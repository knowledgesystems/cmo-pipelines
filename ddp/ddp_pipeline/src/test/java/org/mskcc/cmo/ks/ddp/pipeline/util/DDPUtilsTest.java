/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.ddp.pipeline.DDPConfiguration;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.ClinicalRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.TimelineRadiationRecord;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientDemographics;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;

import java.util.*;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;
import java.text.ParseException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(
        properties = {
            "ddp.cohort.map = {\"mskimpact\":2033,\"mskimpact_ped\":1852}",
            "naaccr.ethnicity = naaccr_ethnicity.json",
            "naaccr.race = naaccr_race.json",
            "naaccr.sex = naaccr_sex.json"
        },
        inheritLocations = false
)
@ContextConfiguration(classes={DDPConfiguration.class, DDPUtilsTestConfiguration.class})
public class DDPUtilsTest {

    @Resource(name="mockCompositePatientRecords")
    private Map<String, DDPCompositeRecord> mockCompositePatientRecords;

    private static final long MILLISECONDS_PER_STANDARD_DAY = 24 * 60 * 60 * 1000;
    private static final long MINIMUM_SAFE_MILLISECONDS_TO_MIDNIGHT = 2 * 1000; // two seconds to midnight will require a pause

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
        resolvePatientSexAndAssert("UNRECOGNIZED", "", "NA");
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

    /**
     * Tests for resolvePatientAgeAtDiagnosis().
     * NOTE: Darwin pipeline outputs patient age at diagnosis ("AGE") to darwin/darwin_age.txt.
     *      As of 2018/06/05, this file is only used for genie and is not generated
     *      by the DDP pipeline yet. At this time there is no need to implement the
     *      DDP equivalent of the darwin/darwin_age.txt file as it is still being
     *      generated by the Darwin pipeline.
     *
     *      The Darwin pipeline calculates "AGE" as the years since birth:
     *              (currentYear - patientBirthYear)
     *      This behavior is mirrored in DDPUtils.resolvePatientAgeAtDiagnosis() and
     *      will not be modified until further notice.
     *
     *      Eventually the age of diagnosis will be calculated on a per-sample basis, using
     *      diagnosis data and mapping diagnosis ICD* codes to SEER, ONCOTREE_CODES to determine
     *      the current age at which the patient was diagnosed for each sample/specimen collection.
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
        Assert.assertEquals(null, DDPUtils.resolveTimelineEventDateInDays(null, null));
    }

    @Test
    public void resolveTimelineEventDateInDaysTest() throws ParseException {
        Assert.assertEquals("60", DDPUtils.resolveTimelineEventDateInDays("2018-02-19", "2018-04-20"));
        // we probably would not get data like this
        Assert.assertEquals("-60", DDPUtils.resolveTimelineEventDateInDays("2018-04-20", "2018-02-19"));
    }

    /* Tests for constructRecord(Object) and constructRecord(Object, Boolean, Boolean, Boolean, Boolean)
    * exception thrown if "getFieldNames" method does not exist
    * exception thrown if one of the values are null
    */

    @Test(expected = NoSuchMethodException.class)
    public void constructRecordNoSuchMethodExceptionTest() throws Exception {
        DDPUtils.constructRecord(new Object());
    }

    @Test(expected = NoSuchMethodException.class)
    public void constructRecordOverloadedNoSuchMethodExceptionTest() throws Exception {
        DDPUtils.constructRecord(new Object(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
    }

    @Test(expected = NullPointerException.class)
    public void constructRecordNullPointerExceptionTest() throws Exception {
        DDPUtils.constructRecord(new TimelineRadiationRecord());
    }

    @Test(expected = NullPointerException.class)
    public void constructRecordOverloadedNullPointerExceptionTest() throws Exception {
        DDPUtils.constructRecord(new ClinicalRecord(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void constructRecordTest() throws Exception {
        TimelineRadiationRecord timelineRadiationRecord = new TimelineRadiationRecord();
        timelineRadiationRecord.setPATIENT_ID("MY_PT_ID");
        timelineRadiationRecord.setSTART_DATE("18732");
        timelineRadiationRecord.setSTOP_DATE("18736");
        timelineRadiationRecord.setEVENT_TYPE("TREATMENT");
        timelineRadiationRecord.setTREATMENT_TYPE("Radiation Therapy");
        timelineRadiationRecord.setSUBTYPE("R");
        timelineRadiationRecord.setANATOMY("  BRAIN "); // it does a trim too
        timelineRadiationRecord.setPLANNED_DOSE("1000.00");
        timelineRadiationRecord.setDELIVERED_DOSE("1000.00");
        timelineRadiationRecord.setPLANNED_FRACTIONS("4");
        timelineRadiationRecord.setDELIVERED_FRACTIONS("4");
        timelineRadiationRecord.setREF_POINT_SITE("PTV30");
        String expectedValue = "MY_PT_ID\t18732\t18736\tTREATMENT\tRadiation Therapy\tR\tBRAIN\t1000.00\t1000.00\t4\t4\tPTV30";
        String returnedValue = DDPUtils.constructRecord(timelineRadiationRecord);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    @Test
    public void constructRecordOverloadedTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setRACE("MY_RACE");
        clinicalRecord.setRELIGION("MY_RELIGION");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setETHNICITY("MY_ETHNICITY");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setPED_IND("Yes");
        clinicalRecord.setOS_MONTHS("3.123");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY");
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // it does a trim too
        clinicalRecord.setSURGERY("MY_SURGERY");
        String expectedValue = "MY_PT_ID\t20\tMY_RACE\tMY_RELIGION\tFemale\tMY_ETHNICITY\tLIVING\tYes\t3.123\tMY_RADIATION_THERAPY\tMY_CHEMOTHERAPY\tMY_SURGERY";
        String returnedValue = DDPUtils.constructRecord(clinicalRecord, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    @Test
    public void constructRecordOverloadedNoDiagnosisTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setRACE("MY_RACE");
        clinicalRecord.setRELIGION("MY_RELIGION");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setETHNICITY("MY_ETHNICITY");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setPED_IND("No");
        clinicalRecord.setOS_MONTHS("NA");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY");
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // it does a trim too
        clinicalRecord.setSURGERY("MY_SURGERY");
        String expectedValue = "MY_PT_ID\t20\tMY_RACE\tMY_RELIGION\tFemale\tMY_ETHNICITY\tLIVING\tNo\tMY_RADIATION_THERAPY\tMY_CHEMOTHERAPY\tMY_SURGERY";
        String returnedValue = DDPUtils.constructRecord(clinicalRecord, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    @Test
    public void constructRecordOverloadedNoRadTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setRACE("MY_RACE");
        clinicalRecord.setRELIGION("MY_RELIGION");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setETHNICITY("MY_ETHNICITY");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setPED_IND("NA");
        clinicalRecord.setOS_MONTHS("3.123");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY"); // doesn't matter if this was set
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // it does a trim too
        clinicalRecord.setSURGERY("MY_SURGERY");
        String expectedValue = "MY_PT_ID\t20\tMY_RACE\tMY_RELIGION\tFemale\tMY_ETHNICITY\tLIVING\tNA\t3.123\tMY_CHEMOTHERAPY\tMY_SURGERY";
        String returnedValue = DDPUtils.constructRecord(clinicalRecord, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    @Test
    public void constructRecordOverloadedNoChemoTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setRACE("MY_RACE");
        clinicalRecord.setRELIGION("MY_RELIGION");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setETHNICITY("MY_ETHNICITY");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setPED_IND("Yes");
        clinicalRecord.setOS_MONTHS("3.123");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY");
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // doesn't matter if this was set
        clinicalRecord.setSURGERY("MY_SURGERY");
        String expectedValue = "MY_PT_ID\t20\tMY_RACE\tMY_RELIGION\tFemale\tMY_ETHNICITY\tLIVING\tYes\t3.123\tMY_RADIATION_THERAPY\tMY_SURGERY";
        String returnedValue = DDPUtils.constructRecord(clinicalRecord, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    @Test
    public void constructRecordOverloadedNoSurgeryTest() throws Exception {
        ClinicalRecord clinicalRecord = new ClinicalRecord();
        clinicalRecord.setPATIENT_ID("MY_PT_ID");
        clinicalRecord.setAGE_CURRENT("20");
        clinicalRecord.setRACE("MY_RACE");
        clinicalRecord.setRELIGION("MY_RELIGION");
        clinicalRecord.setSEX("Female");
        clinicalRecord.setETHNICITY("MY_ETHNICITY");
        clinicalRecord.setOS_STATUS("LIVING");
        clinicalRecord.setPED_IND("No");
        clinicalRecord.setOS_MONTHS("3.123");
        clinicalRecord.setRADIATION_THERAPY("MY_RADIATION_THERAPY");
        clinicalRecord.setCHEMOTHERAPY("  MY_CHEMOTHERAPY  "); // it does a trim too
        clinicalRecord.setSURGERY("MY_SURGERY"); // doesn't matter if this was set
        String expectedValue = "MY_PT_ID\t20\tMY_RACE\tMY_RELIGION\tFemale\tMY_ETHNICITY\tLIVING\tNo\t3.123\tMY_RADIATION_THERAPY\tMY_CHEMOTHERAPY";
        String returnedValue = DDPUtils.constructRecord(clinicalRecord, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    /**
     * Test method for resolving a patient's years since birth.
     *
     * Confirm that empty string, null, or NA values return NA.
     * Confirm that birth dates occurring at beginning and end of year
     * return expected values.
     * @throws Exception
     */
    @Test
    public void resolvePatientYearsSinceBirthTest()throws Exception {
        wait_until_midnight_if_necessary();
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Integer currentYear = calendar.get(Calendar.YEAR);

        resolvePatientYearsSinceBirthAndAssert("", "NA");
        resolvePatientYearsSinceBirthAndAssert(null, "NA");
        resolvePatientYearsSinceBirthAndAssert("NA", "NA");
        resolvePatientYearsSinceBirthAndAssert("2000-05-01", String.valueOf(currentYear - 2000));
        resolvePatientYearsSinceBirthAndAssert("2015-01-01", String.valueOf(currentYear - 2015));
        resolvePatientYearsSinceBirthAndAssert("2014-12-31", String.valueOf(currentYear - 2014));
    }

    /**
     * Test that invalid date of births will throw a ParseException.
     *
     * @throws Exception
     */
    @Test(expected = ParseException.class)
    public void resolvePatientYearsSinceBirthWithExceptionTest() throws Exception {
        wait_until_midnight_if_necessary();
        DDPUtils.resolveYearsSinceBirth("05/01/2000");
    }

    /**
     * Tests that NAACCR JSON mappings files are loaded and modeled correctly and
     * returning expected values.
     *
     * @throws Exception
     */
    @Test
    public void loadNaaccrMappingsTest() throws Exception {
        Map<String, String> naaccrEthnicityMap = DDPUtils.getNaaccrEthnicityMap();
        Assert.assertTrue(!naaccrEthnicityMap.isEmpty());
        Assert.assertEquals("1", DDPUtils.resolveNaaccrEthnicityCode("Mexican (includes Chicano)"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrEthnicityCode("MADEUPETHNICITYVALUE"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrEthnicityCode(null));

        Map<String, String> naaccrRaceMap = DDPUtils.getNaaccrRaceMap();
        Assert.assertTrue(!naaccrRaceMap.isEmpty());
        Assert.assertEquals("1", DDPUtils.resolveNaaccrRaceCode("White"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrRaceCode("VULCAN"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrRaceCode(null));

        Map<String, String> naaccrSexMap = DDPUtils.getNaaccrSexMap();
        Assert.assertTrue(!naaccrSexMap.isEmpty());
        Assert.assertEquals("1", DDPUtils.resolveNaaccrSexCode("Male"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrSexCode("TURTLE"));
        Assert.assertEquals("NA", DDPUtils.resolveNaaccrSexCode(null));
    }

    private void resolvePatientYearsSinceBirthAndAssert(String birthDate, String expectedValue) throws ParseException {
        String returnedValue = DDPUtils.resolveYearsSinceBirth(birthDate);
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
        String returnedValue = DDPUtils.resolvePatientCurrentAge(testPatient);
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
        String returnedValue = DDPUtils.resolvePatientSex(testPatient);
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
        String returnedValue = DDPUtils.resolveOsStatus(testPatient);
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
        String returnedValue = DDPUtils.resolveOsMonths(osStatus, testPatient);
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
        String returnedValue = DDPUtils.resolvePatientAgeAtDiagnosis(testPatient);
        Assert.assertEquals(expectedValue, returnedValue);
    }

    private void getFirstTumorDiagnosisDateAndAssert(List<String> tumorDiagnosisDates, String expectedValue) throws ParseException {
        String returnedValue;
        if (tumorDiagnosisDates == null) {
            returnedValue = DDPUtils.getFirstTumorDiagnosisDate(null);
        } else {
            List<PatientDiagnosis> patientDiagnoses = new ArrayList<>();
            for (String tumorDiagnosisDate : tumorDiagnosisDates) {
                PatientDiagnosis patientDiagnosis = new PatientDiagnosis();
                patientDiagnosis.setTumorDiagnosisDate(tumorDiagnosisDate);
                patientDiagnoses.add(patientDiagnosis);
            }
            returnedValue = DDPUtils.getFirstTumorDiagnosisDate(patientDiagnoses);
        }
        Assert.assertEquals(expectedValue, returnedValue);
    }
}
