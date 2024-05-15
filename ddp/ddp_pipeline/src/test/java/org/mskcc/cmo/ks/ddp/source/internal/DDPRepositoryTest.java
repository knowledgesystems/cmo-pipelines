/*
 * Copyright (c) 2018, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.mskcc.cmo.ks.ddp.source.internal;

import com.google.common.base.Strings;
import org.mskcc.cmo.ks.ddp.source.model.*;

import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=DDPSourceTestConfiguration.class)
public class DDPRepositoryTest {

    @Autowired
    private DDPRepository ddpRepository;

    @Resource(name = "mockCohortMapping")
    private Map<String, Integer> mockCohortMapping;

    private final Map<Integer, Integer> expectedPatientsCountByCohort = makeMockExpectedPatientsCountByCohort();
    private final Map<Integer, String> expectedPatientDmpIds = makeMockExpectedPatientDmpIds();
    private final Map<Integer, String> expectedPatientDemographicsDOBs = makeMockExpectedPatientDemographicsDOBs();
    private final Map<Integer, String> expectedPatientDemographicsDODs = makeMockExpectedPatientDemographicsDODs();
    private final Map<Integer, String> expectedPatientDemographicsSex = makeMockExpectedPatientDemographicsSex();
    private final Map<Integer, Integer> expectedPatientDiagnosisSize = makeMockExpectedPatientDiagnosisSize();
    private final Map<Integer, Integer> expectedPatientRadiationProcedureSize = makeMockExpectedPatientRadiationProcedureSize();
    private final Map<Integer, Integer> expectedPatientChemoProcedureSize = makeMockExpectedPatientChemoProcedureSize();
    private final Map<Integer, Integer> expectedPatientSurgeryProcedureSize = makeMockExpectedPatientSurgeryProcedureSize();

    /**
     * Test mock response for authorized cohorts.
     */
    @Test
    public void testGetAuthorizedCohorts() {
        if (ddpRepository == null) {
            Assert.fail("ddpRepository was not intialized properly for testing");
        }
        List<Cohort> authorizedCohorts = ddpRepository.getAuthorizedCohorts();
        Assert.assertEquals(2, authorizedCohorts.size());
        // assert that all authorized cohorts exist in mockCohortMapping
        for (Cohort cohort : authorizedCohorts) {
            Assert.assertTrue(mockCohortMapping.containsValue(cohort.getCHTCOHORTID()));
        }
    }

    /**
     * Test active patient count for authorized cohorts.
     */
    @Test
    public void testGetAuthorizedCohortsActivePatients() {
        List<Cohort> authorizedCohorts = ddpRepository.getAuthorizedCohorts();
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Cohort cohort : authorizedCohorts) {
            Integer expectedCount = expectedPatientsCountByCohort.get(cohort.getCHTCOHORTID());
            if (!Objects.equals(cohort.getACTIVEPATIENTCOUNT(), expectedCount)) {
                errorMessages.append("Active patient count for cohort ")
                        .append(cohort.getCHTCOHORTID())
                        .append(" (")
                        .append(cohort.getACTIVEPATIENTCOUNT())
                        .append(") does not match expected active patient count: ")
                        .append(expectedCount)
                        .append("\n");
                errors++;
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Test patient list size for authorized cohorts.
     */
    @Test
    public void testGetAuthorizedCohortsPatientList() {
        List<Cohort> authorizedCohorts = ddpRepository.getAuthorizedCohorts();
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Cohort cohort : authorizedCohorts) {
            Integer expectedCount = expectedPatientsCountByCohort.get(cohort.getCHTCOHORTID());
            List<CohortPatient> cohortPatients = ddpRepository.getPatientsByCohort(cohort.getCHTCOHORTID());
            if (!Objects.equals(cohortPatients.size(), expectedCount)) {
                errorMessages.append("Patient list size for cohort ")
                        .append(cohortPatients.size())
                        .append(" (")
                        .append(cohort.getACTIVEPATIENTCOUNT())
                        .append(") does not match expected patient count: ")
                        .append(expectedCount)
                        .append("\n");
                errors++;
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Test resolved dmp ids for all patients.
     */
    @Test
    public void testGetPatientDmpIds() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            PatientIdentifiers pids = ddpRepository.getPatientIdentifiers(deIdentifiedPid.toString());
            if (!pids.getDmpPatientId().equals(expectedPatientDmpIds.get(deIdentifiedPid))) {
                errorMessages.append("Patient DMP ID '")
                        .append(pids.getDmpPatientId())
                        .append("' does not match expected DMP ID '")
                        .append(expectedPatientDmpIds.get(deIdentifiedPid))
                        .append("'\n");
                errors++;
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Tests patient demographics response.
     *
     * Compares:
     *  - date of birth
     *  - deceased date
     *  - sex
     */
    @Test
    public void testGetPatientDemographics() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            if (Strings.isNullOrEmpty(expectedPatientDmpIds.get(deIdentifiedPid))) {
                continue; // skip patient 3 since does not resolve to dmp id
            }
            boolean patientTestFail = Boolean.FALSE;
            StringBuilder patientErrorDetails = new StringBuilder();
            patientErrorDetails.append("\nPatient demographics errors: ")
                    .append(deIdentifiedPid);

            PatientDemographics demographics = ddpRepository.getPatientDemographics(deIdentifiedPid.toString());
            if (demographics == null) {
                // if demographics returned is null and it shouldn't be then log error and continue to next record
                if (!DDPSourceTestConfiguration.UNAUTHORIZED_PATIENT_DE_IDS.contains(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tDemographics data is null and is expected to be not null");
                }
            }
            else {
                if (!demographics.getDateOfBirth().equals(expectedPatientDemographicsDOBs.get(deIdentifiedPid))) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tDOB fetched (")
                            .append(demographics.getDateOfBirth())
                            .append(") does not match expected DOB: ")
                            .append(expectedPatientDemographicsDOBs.get(deIdentifiedPid));
                }
                if (!demographics.getDeceasedDate().equals(expectedPatientDemographicsDODs.get(deIdentifiedPid))) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tDeceased date fetched (")
                            .append(demographics.getDeceasedDate())
                            .append(") does not match expected deceased date: ")
                            .append(expectedPatientDemographicsDODs.get(deIdentifiedPid));
                }
                if (!demographics.getGender().equals(expectedPatientDemographicsSex.get(deIdentifiedPid))) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tSex fetched (")
                            .append(demographics.getGender())
                            .append(") does not match expected sex: ")
                            .append(expectedPatientDemographicsSex.get(deIdentifiedPid));
                }
            }
            if (patientTestFail) {
                errors++;
                errorMessages.append(patientErrorDetails.toString());
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Tests patient diagnosis response.
     */
    @Test
    public void testGetPatientDiagnosis() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            if (Strings.isNullOrEmpty(expectedPatientDmpIds.get(deIdentifiedPid))) {
                continue; // skip patient 3 since does not resolve to dmp id
            }
            boolean patientTestFail = Boolean.FALSE;
            StringBuilder patientErrorDetails = new StringBuilder();
            patientErrorDetails.append("\nPatient diagnosis errors: ")
                    .append(deIdentifiedPid);

            List<PatientDiagnosis> diagnoses = ddpRepository.getPatientDiagnoses(deIdentifiedPid.toString());
            if (diagnoses == null) {
                // if diagnosis list returned is null and it shouldn't be then log error and continue to next record
                if (!DDPSourceTestConfiguration.UNAUTHORIZED_PATIENT_DE_IDS.contains(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tDiagnosis data is null and is expected to be not null");
                }
            }
            else {
                if (diagnoses.size() != expectedPatientDiagnosisSize.get(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tDiagnosis data size (")
                            .append(diagnoses.size())
                            .append(") does not match expected size: ")
                            .append(expectedPatientDiagnosisSize.get(deIdentifiedPid));
                }
            }
            if (patientTestFail) {
                errors++;
                errorMessages.append(patientErrorDetails.toString());
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Tests patient radiation procedures response.
     */
    @Test
    public void testGetPatientRadiationProcedures() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            if (Strings.isNullOrEmpty(expectedPatientDmpIds.get(deIdentifiedPid))) {
                continue; // skip patient 3 since does not resolve to dmp id
            }
            boolean patientTestFail = Boolean.FALSE;
            StringBuilder patientErrorDetails = new StringBuilder();
            patientErrorDetails.append("\nPatient radiation procedure errors: ")
                    .append(deIdentifiedPid);

            List<Radiation> procedures = ddpRepository.getPatientRadiationProcedures(deIdentifiedPid.toString());
            if (procedures == null) {
                // if procedures list returned is null and it shouldn't be then log error and continue to next record
                if (!DDPSourceTestConfiguration.UNAUTHORIZED_PATIENT_DE_IDS.contains(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tRadiation procedures data is null and is expected to be not null");
                }
            }
            else {
                if (procedures.size() != expectedPatientRadiationProcedureSize.get(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tRadiation procedures data size (")
                            .append(procedures.size())
                            .append(") does not match expected size: ")
                            .append(expectedPatientRadiationProcedureSize.get(deIdentifiedPid));
                }
            }
            if (patientTestFail) {
                errors++;
                errorMessages.append(patientErrorDetails.toString());
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Tests patient chemo procedures response.
     */
    @Test
    public void testGetPatientChemoProcedures() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            if (Strings.isNullOrEmpty(expectedPatientDmpIds.get(deIdentifiedPid))) {
                continue; // skip patient 3 since does not resolve to dmp id
            }
            boolean patientTestFail = Boolean.FALSE;
            StringBuilder patientErrorDetails = new StringBuilder();
            patientErrorDetails.append("\nPatient chemo procedure errors: ")
                    .append(deIdentifiedPid);

            List<Chemotherapy> procedures = ddpRepository.getPatientChemoProcedures(deIdentifiedPid.toString());
            if (procedures == null) {
                // if procedures list returned is null and it shouldn't be then log error and continue to next record
                if (!DDPSourceTestConfiguration.UNAUTHORIZED_PATIENT_DE_IDS.contains(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tChemo procedures data is null and is expected to be not null");
                }
            }
            else {
                if (procedures.size() != expectedPatientChemoProcedureSize.get(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tChemo procedures data size (")
                            .append(procedures.size())
                            .append(") does not match expected size: ")
                            .append(expectedPatientChemoProcedureSize.get(deIdentifiedPid));
                }
            }
            if (patientTestFail) {
                errors++;
                errorMessages.append(patientErrorDetails.toString());
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Tests patient surgery procedures response.
     */
    @Test
    public void testGetPatientSurgeryProcedures() {
        int errors = 0;
        StringBuilder errorMessages = new StringBuilder();
        for (Integer deIdentifiedPid : DDPSourceTestConfiguration.COHORT_PATIENT_DE_IDS) {
            if (Strings.isNullOrEmpty(expectedPatientDmpIds.get(deIdentifiedPid))) {
                continue; // skip patient 3 since does not resolve to dmp id
            }
            boolean patientTestFail = Boolean.FALSE;
            StringBuilder patientErrorDetails = new StringBuilder();
            patientErrorDetails.append("\nPatient surgery procedure errors: ")
                    .append(deIdentifiedPid);

            List<Surgery> procedures = ddpRepository.getPatientSurgicalProcedures(deIdentifiedPid.toString());
            if (procedures == null) {
                // if procedures list returned is null and it shouldn't be then log error and continue to next record
                if (!DDPSourceTestConfiguration.UNAUTHORIZED_PATIENT_DE_IDS.contains(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tSurgery procedures data is null and is expected to be not null");
                }
            }
            else {
                if (procedures.size() != expectedPatientSurgeryProcedureSize.get(deIdentifiedPid)) {
                    patientTestFail = Boolean.TRUE;
                    patientErrorDetails.append("\n\tSurgery procedures data size (")
                            .append(procedures.size())
                            .append(") does not match expected size: ")
                            .append(expectedPatientSurgeryProcedureSize.get(deIdentifiedPid));
                }
            }
            if (patientTestFail) {
                errors++;
                errorMessages.append(patientErrorDetails.toString());
            }
        }
        if (errors > 0) {
            Assert.fail(errorMessages.toString());
        }
    }

    /**
     * Mock expected patient counts by cohort.
     * @return
     */
    private Map<Integer, Integer> makeMockExpectedPatientsCountByCohort() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1002, 5);
        map.put(2030, 3);
        return map;
    }

    /**
     * Mock expected patient dmp ids.
     * @return
     */
    private Map<Integer, String> makeMockExpectedPatientDmpIds() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1000001, "P01-000001");
        map.put(1000002, "P01-000002");
        map.put(1000003, "");
        map.put(1000004, "P01-000004");
        map.put(1000005, "P01-000005");
        map.put(1000006, "P01-000006");
        map.put(1000007, "P01-000007");
        map.put(1000008, "P01-000008");
        return map;
    }

    /**
     * Mock expected date of births.
     * NOTE:
     * - patient 3 does not resolve to dmp id so no demographics data available
     * - unauthorized access to patient 4 data, so no demographics data available
     * @return
     */
    private Map<Integer, String> makeMockExpectedPatientDemographicsDOBs() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1000001, "1960-01-01");
        map.put(1000002, "1922-05-01");
        map.put(1000004, null);
        map.put(1000005, "1960-04-26");
        map.put(1000006, "1954-02-11");
        map.put(1000007, "1958-01-30");
        map.put(1000008, "2010-03-11");
        return map;
    }

    /**
     * Mock expected date of deaths.
     * NOTE:
     * - patient 3 does not resolve to dmp id so no demographics data available
     * - unauthorized access to patient 4 data, so no demographics data available
     * @return
     */
    private Map<Integer, String> makeMockExpectedPatientDemographicsDODs() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1000001, "");
        map.put(1000002, "2018-03-01");
        map.put(1000004, null);
        map.put(1000005, "2018-05-08");
        map.put(1000006, "");
        map.put(1000007, "2018-01-31");
        map.put(1000008, "");
        return map;
    }

    /**
     * Mock expected patient sex.
     * NOTE:
     * - patient 3 does not resolve to dmp id so no demographics data available
     * - unauthorized access to patient 4 data, so no demographics data available
     * @return
     */
    private Map<Integer, String> makeMockExpectedPatientDemographicsSex() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1000001, "FEMALE");
        map.put(1000002, "MALE");
        map.put(1000004, null);
        map.put(1000005, "FEMALE");
        map.put(1000006, "FEMALE");
        map.put(1000007, "MALE");
        map.put(1000008, "MALE");
        return map;
    }

    /**
     * Mock expected patient diagnosis size.
     * @return
     */
    private Map<Integer, Integer> makeMockExpectedPatientDiagnosisSize() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1000001, 2);
        map.put(1000002, 3);
        map.put(1000004, null);
        map.put(1000005, 0);
        map.put(1000006, 5);
        map.put(1000007, 2);
        map.put(1000008, 1);
        return map;
    }

    /**
     * Mock expected patient radiation procedures size.
     * @return
     */
    private Map<Integer, Integer> makeMockExpectedPatientRadiationProcedureSize() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1000001, 0);
        map.put(1000002, 2);
        map.put(1000004, null);
        map.put(1000005, 0);
        map.put(1000006, 3);
        map.put(1000007, 0);
        map.put(1000008, 0);
        return map;
    }

    /**
     * Mock expected patient chemo procedures size.
     * @return
     */
    private Map<Integer, Integer> makeMockExpectedPatientChemoProcedureSize() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1000001, 0);
        map.put(1000002, 4);
        map.put(1000004, null);
        map.put(1000005, 0);
        map.put(1000006, 0);
        map.put(1000007, 3);
        map.put(1000008, 1);
        return map;
    }

    /**
     * Mock expected patient surgery procedures size.
     * @return
     */
    private Map<Integer, Integer> makeMockExpectedPatientSurgeryProcedureSize() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1000001, 2);
        map.put(1000002, 4);
        map.put(1000004, null);
        map.put(1000005, 0);
        map.put(1000006, 0);
        map.put(1000007, 0);
        map.put(1000008, 6);
        return map;
    }
}
