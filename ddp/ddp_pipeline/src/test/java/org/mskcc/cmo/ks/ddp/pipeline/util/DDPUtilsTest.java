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

    @Test
    public void testMockCompositePatientRecordsInitialization() {
        if (mockCompositePatientRecords.isEmpty()) {
            Assert.fail("mockCompositePatientRecords not initialized properly!");
        }
    }

    @Test
    public void resolvePatientCurrentAgeTest() throws ParseException {
        String returnedValule = ddpUtils.resolvePatientCurrentAge(mockCompositePatientRecords.get("P01-000002"));
    }

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

}
