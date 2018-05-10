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
import org.mskcc.cmo.ks.ddp.source.model.PatientDemographics;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
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
        String todayDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
        calendar.add(Calendar.DATE, -7668);
        String birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
System.out.println("test for 7668 day interval");
System.out.println("todayDateString: " + todayDateString);
System.out.println("birthDateString: " + birthDateString);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7668 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7668 days later
        calendar.add(Calendar.DATE, 1);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
System.out.println("test for 7669 day interval");
System.out.println("todayDateString: " + todayDateString);
System.out.println("birthDateString: " + birthDateString);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7669 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7669 days later
        calendar.add(Calendar.DATE, 1);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
System.out.println("test for 7670 day interval");
System.out.println("todayDateString: " + todayDateString);
System.out.println("birthDateString: " + birthDateString);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "20"); // 7670 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "20"); // 7670 days later
        calendar.add(Calendar.DATE, 1);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
System.out.println("test for 7671 day interval");
System.out.println("todayDateString: " + todayDateString);
System.out.println("birthDateString: " + birthDateString);
        birthDateString = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE);
        resolvePatientCurrentAgeAndAssert(birthDateString, "ignored", "", "", null, null, "21"); // 7671 days later
        resolvePatientCurrentAgeAndAssert("", birthDateString, "", "", null, null, "21"); // 7671 days later
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
        

        //DDPCompositeRecord testPatient = mockCompositePatientRecords.get("P01-000002");

    //public static String resolvePatientCurrentAge(DDPCompositeRecord compositePatient) throws ParseException {
/*
        // if patient birth date is null/empty then use current age value
        if (Strings.isNullOrEmpty(compositePatient.getPatientBirthDate())) {
            return anonymizePatientAge(compositePatient.getPatientAge());
        }
        Long birthDateInDays = getDateInDays(compositePatient.getPatientBirthDate());
        Long referenceDateInDays;
        // use current date as reference date if patient not deceased, otherwise use date of death
        if (!Strings.isNullOrEmpty(compositePatient.getPatientDeathDate())) {
            referenceDateInDays = getDateInDays(compositePatient.getPatientDeathDate());
        }
        else {
            referenceDateInDays = getDateInDays(new Date());
        }
        Double age = (referenceDateInDays - birthDateInDays) / (DAYS_TO_YEARS_CONVERSION);
        return anonymizePatientAge(age.intValue());
*/
    //}

    //public static String resolvePatientAgeAtDiagnosis(DDPCompositeRecord compositePatient) throws ParseException {
    //public static String resolvePatientSex(DDPCompositeRecord compositePatient) {
    //public static String resolveOsStatus(DDPCompositeRecord compositePatient) {
    //public static String resolveOsMonths(String osStatus, DDPCompositeRecord compositePatient) throws ParseException {
    //public static String getFirstTumorDiagnosisDate(List<PatientDiagnosis> patientDiagnosis) throws ParseException {
    //public static String resolveTimelineEventDateInDays(String birthDate, String eventDate) throws ParseException {
    //public static String constructRecord(Object object) throws Exception {

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
}
