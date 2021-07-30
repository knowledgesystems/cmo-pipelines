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

package org.mskcc.cmo.ks.ddp.source.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.source.model.*;
import org.mskcc.cmo.ks.ddp.source.util.DDPResponseUtil;
import org.mskcc.cmo.ks.ddp.source.util.AuthenticationUtil;
import com.google.common.base.Strings;
import java.io.*;
import java.util.*;
import org.mockito.*;
import org.springframework.context.annotation.*;

/**
 *
 * @author ochoaa
 */
@Configuration
@SuppressWarnings("unchecked")
public class DDPSourceTestConfiguration {
    public static final List<Integer> COHORT_IDS = Arrays.asList(new Integer[]{1002, 2030});
    public static final List<Integer> COHORT_PATIENT_DE_IDS = Arrays.asList(new Integer[]{
        1000001, 1000002, 1000003, 1000004, 1000005, 1000006, 1000007, 1000008});
    public static final List<Integer> UNAUTHORIZED_PATIENT_DE_IDS = Arrays.asList(new Integer[]{1000004});
    // pid #1000003 does not resolve to dmp patient id
    public static final Map<Integer, String> COHORT_PATIENT_ID_MAP;
    static {
        COHORT_PATIENT_ID_MAP = new HashMap<>();
        COHORT_PATIENT_ID_MAP.put(1000001, "P01-000001");
        COHORT_PATIENT_ID_MAP.put(1000002, "P01-000002");
        COHORT_PATIENT_ID_MAP.put(1000003, "");
        COHORT_PATIENT_ID_MAP.put(1000004, "P01-000004");
        COHORT_PATIENT_ID_MAP.put(1000005, "P01-000005");
        COHORT_PATIENT_ID_MAP.put(1000006, "P01-000006");
        COHORT_PATIENT_ID_MAP.put(1000007, "P01-000007");
        COHORT_PATIENT_ID_MAP.put(1000008, "P01-000008");
    }

    public final String AUTHORIZED_COHORTS_JSON = "src/test/resources/data/authorized_cohorts.json";
    public final String COHORT_1002_PATIENTS_JSON = "src/test/resources/data/cohort_1002_patients.json";
    public final String COHORT_2030_PATIENTS_JSON = "src/test/resources/data/cohort_2030_patients.json";

    // mock patient data files
    // identifiers
    public final String P1_IDENTIFIERS_JSON = "src/test/resources/data/p1_identifiers.json";
    public final String P2_IDENTIFIERS_JSON = "src/test/resources/data/p2_identifiers.json";
    public final String P3_IDENTIFIERS_JSON = "src/test/resources/data/p3_identifiers.json";
    public final String P4_IDENTIFIERS_JSON = "src/test/resources/data/p4_identifiers.json";
    public final String P5_IDENTIFIERS_JSON = "src/test/resources/data/p5_identifiers.json";
    public final String P6_IDENTIFIERS_JSON = "src/test/resources/data/p6_identifiers.json";
    public final String P7_IDENTIFIERS_JSON = "src/test/resources/data/p7_identifiers.json";
    public final String P8_IDENTIFIERS_JSON = "src/test/resources/data/p8_identifiers.json";
    // demographics
    public final String P1_DEMOGRAPHICS_JSON = "src/test/resources/data/p1_demographics.json";
    public final String P2_DEMOGRAPHICS_JSON = "src/test/resources/data/p2_demographics.json";
    public final String P5_DEMOGRAPHICS_JSON = "src/test/resources/data/p5_demographics.json";
    public final String P6_DEMOGRAPHICS_JSON = "src/test/resources/data/p6_demographics.json";
    public final String P7_DEMOGRAPHICS_JSON = "src/test/resources/data/p7_demographics.json";
    public final String P8_DEMOGRAPHICS_JSON = "src/test/resources/data/p8_demographics.json";
    // diagnosis
    public final String P1_DIAGNOSIS_JSON = "src/test/resources/data/p1_diagnosis.json";
    public final String P2_DIAGNOSIS_JSON = "src/test/resources/data/p2_diagnosis.json";
    public final String P6_DIAGNOSIS_JSON = "src/test/resources/data/p6_diagnosis.json";
    public final String P7_DIAGNOSIS_JSON = "src/test/resources/data/p7_diagnosis.json";
    public final String P8_DIAGNOSIS_JSON = "src/test/resources/data/p8_diagnosis.json";
    // radiation
    public final String P2_RADIATION_JSON = "src/test/resources/data/p2_radiation.json";
    public final String P6_RADIATION_JSON = "src/test/resources/data/p6_radiation.json";
    // chemo
    public final String P2_CHEMOTHERAPY_JSON = "src/test/resources/data/p2_chemotherapy.json";
    public final String P7_CHEMOTHERAPY_JSON = "src/test/resources/data/p7_chemotherapy.json";
    public final String P8_CHEMOTHERAPY_JSON = "src/test/resources/data/p8_chemotherapy.json";
    // surgery
    public final String P1_SURGERY_JSON = "src/test/resources/data/p1_surgery.json";
    public final String P2_SURGERY_JSON = "src/test/resources/data/p2_surgery.json";
    public final String P8_SURGERY_JSON = "src/test/resources/data/p8_surgery.json";

    @Bean
    public AuthenticationUtil authenticationUtil () {
        return new AuthenticationUtil();
    }

    private DDPResponseUtil ddpResponseUtil;
    @Bean
    public DDPResponseUtil ddpResponseUtil() {
        this.ddpResponseUtil = new DDPResponseUtil();
        return ddpResponseUtil;
    }

    private Map<String, Integer> mockCohortMapping;
    @Bean(name = "mockCohortMapping")
    public Map<String, Integer> mockCohortMapping() {
        this.mockCohortMapping = new HashMap<>();
        this.mockCohortMapping.put("cohort_1002", 1002);
        this.mockCohortMapping.put("cohort_2030", 2030);
        return mockCohortMapping;
    }

    private List<Cohort> mockAuthorizedCohortsJsonResponse() throws Exception {
        File mockJsonFile = new File(AUTHORIZED_COHORTS_JSON);
        return (List<Cohort>) ddpResponseUtil.parseData(mockJsonFile, new TypeReference<List<Cohort>>(){});
    }

    private Map<Integer, List<CohortPatient>> mockCohortPatients;
    @Bean(name="mockPatientsByCohort")
    public Map<Integer, List<CohortPatient>> mockPatientsByCohort() throws Exception {
        TypeReference cohortPatientTypeReference = new TypeReference<List<CohortPatient>>(){};
        this.mockCohortPatients = new HashMap<>();
        this.mockCohortPatients.put(1002,
                (List<CohortPatient>) ddpResponseUtil.parseData(new File(COHORT_1002_PATIENTS_JSON), cohortPatientTypeReference));
        this.mockCohortPatients.put(2030,
                (List<CohortPatient>) ddpResponseUtil.parseData(new File(COHORT_2030_PATIENTS_JSON), cohortPatientTypeReference));
        return mockCohortPatients;
    }

    private Map<Integer, PatientIdentifiers> mockPatientIdentifiers;
    @Bean(name="mockPatientIdentifiers")
    public Map<Integer, PatientIdentifiers> mockPatientIdentifiers() throws Exception {
        TypeReference patientIdentifiersTypeReference = new TypeReference<PatientIdentifiers>(){};
        this.mockPatientIdentifiers = new HashMap<>();
        this.mockPatientIdentifiers.put(1000001,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P1_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000002,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P2_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000003,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P3_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000004,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P4_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000005,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P5_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000006,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P6_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000007,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P7_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        this.mockPatientIdentifiers.put(1000008,
                (PatientIdentifiers) ddpResponseUtil.parseData(new File(P8_IDENTIFIERS_JSON), patientIdentifiersTypeReference));
        return mockPatientIdentifiers;
    }

    private Map<Integer, PatientDemographics> mockPatientDemographics;
    @Bean(name="mockPatientDemographics")
    public Map<Integer, PatientDemographics> mockPatientDemographics() throws Exception {
        TypeReference patientDemographicsTypeReference = new TypeReference<List<PatientDemographics>>(){};
        this.mockPatientDemographics = new HashMap<>();
        PatientDemographics demographics = ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P1_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0);
        this.mockPatientDemographics.put(1000001,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P1_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        this.mockPatientDemographics.put(1000002,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P2_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        this.mockPatientDemographics.put(1000004, null); // unauthorized acccess to patient 4 data
        this.mockPatientDemographics.put(1000005,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P5_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        this.mockPatientDemographics.put(1000006,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P6_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        this.mockPatientDemographics.put(1000007,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P7_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        this.mockPatientDemographics.put(1000008,
                ((List<PatientDemographics>) ddpResponseUtil.parseData(new File(P8_DEMOGRAPHICS_JSON), patientDemographicsTypeReference)).get(0));
        return mockPatientDemographics;
    }

    private Map<Integer, List<PatientDiagnosis>> mockPatientDiagnoses;
    @Bean(name="mockPatientDiagnoses")
    public Map<Integer, List<PatientDiagnosis>> mockPatientDiagnoses() throws Exception {
        TypeReference patientDiagnosisTypeReference = new TypeReference<List<PatientDiagnosis>>(){};
        this.mockPatientDiagnoses = new HashMap<>();
        this.mockPatientDiagnoses.put(1000001,
                (List<PatientDiagnosis>) ddpResponseUtil.parseData(new File(P1_DIAGNOSIS_JSON), patientDiagnosisTypeReference));
        this.mockPatientDiagnoses.put(1000002,
                (List<PatientDiagnosis>) ddpResponseUtil.parseData(new File(P2_DIAGNOSIS_JSON), patientDiagnosisTypeReference));
        this.mockPatientDiagnoses.put(1000004, null); // unauthorized acccess to patient 4 data
        this.mockPatientDiagnoses.put(1000005, new ArrayList<>()); // patient 5 diagnosis data unavailable
        this.mockPatientDiagnoses.put(1000006,
                (List<PatientDiagnosis>) ddpResponseUtil.parseData(new File(P6_DIAGNOSIS_JSON), patientDiagnosisTypeReference));
        this.mockPatientDiagnoses.put(1000007,
                (List<PatientDiagnosis>) ddpResponseUtil.parseData(new File(P7_DIAGNOSIS_JSON), patientDiagnosisTypeReference));
        this.mockPatientDiagnoses.put(1000008,
                (List<PatientDiagnosis>) ddpResponseUtil.parseData(new File(P8_DIAGNOSIS_JSON), patientDiagnosisTypeReference));
        return mockPatientDiagnoses;
    }

    private Map<Integer, List<Radiation>> mockPatientRadiationProcedures;
    @Bean(name="mockPatientRadiationProcedures")
    public Map<Integer, List<Radiation>> mockPatientRadiationProcedures() throws Exception {
        TypeReference radiationTypeReference = new TypeReference<List<Radiation>>(){};
        this.mockPatientRadiationProcedures = new HashMap<>();
        this.mockPatientRadiationProcedures.put(1000001, new ArrayList<>()); // patient 1 did not have any radiation
        this.mockPatientRadiationProcedures.put(1000002,
                (List<Radiation>) ddpResponseUtil.parseData(new File(P2_RADIATION_JSON), radiationTypeReference));
        this.mockPatientRadiationProcedures.put(1000004, null); // unauthorized access to patient 4 data
        this.mockPatientRadiationProcedures.put(1000005, new ArrayList<>()); // patient 5 data unavailable
        this.mockPatientRadiationProcedures.put(1000006,
                (List<Radiation>) ddpResponseUtil.parseData(new File(P6_RADIATION_JSON), radiationTypeReference));
        this.mockPatientRadiationProcedures.put(1000007, new ArrayList<>()); // patient 7 did not have any radiation
        this.mockPatientRadiationProcedures.put(1000008, new ArrayList<>()); // patient 8 did not have any radiation
        return mockPatientRadiationProcedures;
    }

    private Map<Integer, List<Chemotherapy>> mockPatientChemoProcedures;
    @Bean(name="mockPatientChemoProcedures")
    public Map<Integer, List<Chemotherapy>> mockPatientChemoProcedures() throws Exception {
        TypeReference chemoTypeReference = new TypeReference<List<Chemotherapy>>(){};
        this.mockPatientChemoProcedures = new HashMap<>();
        this.mockPatientChemoProcedures.put(1000001, new ArrayList<>()); // patient 1 did not have any chemo
        this.mockPatientChemoProcedures.put(1000002,
                (List<Chemotherapy>) ddpResponseUtil.parseData(new File(P2_CHEMOTHERAPY_JSON), chemoTypeReference));
        this.mockPatientChemoProcedures.put(1000004, null); // unauthorized access to patient 4 data
        this.mockPatientChemoProcedures.put(1000005, new ArrayList<>()); // patient 5 data unavailable
        this.mockPatientChemoProcedures.put(1000006, new ArrayList<>()); // patient 6 did not have any chemo
        this.mockPatientChemoProcedures.put(1000007,
                (List<Chemotherapy>) ddpResponseUtil.parseData(new File(P7_CHEMOTHERAPY_JSON), chemoTypeReference));
        this.mockPatientChemoProcedures.put(1000008,
                (List<Chemotherapy>) ddpResponseUtil.parseData(new File(P8_CHEMOTHERAPY_JSON), chemoTypeReference));
        return mockPatientChemoProcedures;
    }

    private Map<Integer, List<Surgery>> mockPatientSurgicalProcedures;
    @Bean(name="mockPatientSurgicalProcedures")
    public Map<Integer, List<Surgery>> mockPatientSurgicalProcedures() throws Exception {
        TypeReference surgeryTypeReference = new TypeReference<List<Surgery>>(){};
        this.mockPatientSurgicalProcedures = new HashMap<>();
        this.mockPatientSurgicalProcedures.put(1000001,
                (List<Surgery>) ddpResponseUtil.parseData(new File(P1_SURGERY_JSON), surgeryTypeReference));
        this.mockPatientSurgicalProcedures.put(1000002,
                (List<Surgery>) ddpResponseUtil.parseData(new File(P2_SURGERY_JSON), surgeryTypeReference));
        this.mockPatientSurgicalProcedures.put(1000004, null); // unauthorized access to patient 4 data
        this.mockPatientSurgicalProcedures.put(1000005, new ArrayList<>()); // patient 5 data unavailable
        this.mockPatientSurgicalProcedures.put(1000006, new ArrayList<>()); // patient 6 did not have any surgeries
        this.mockPatientSurgicalProcedures.put(1000007, new ArrayList<>()); // patient 7 did not have any surgeries
        this.mockPatientSurgicalProcedures.put(1000008,
                (List<Surgery>) ddpResponseUtil.parseData(new File(P8_SURGERY_JSON), surgeryTypeReference));
        return mockPatientSurgicalProcedures;
    }

    @Bean
    public DDPRepository ddpRepository() throws Exception {
        DDPRepository mockDDPRepository = Mockito.mock(DDPRepository.class);
        Mockito.when(mockDDPRepository.getAuthorizedCohorts()).thenReturn(mockAuthorizedCohortsJsonResponse());
        // mock patients by cohort response
        for (Integer cohortId : COHORT_IDS) {
            Mockito.when(mockDDPRepository.getPatientsByCohort(ArgumentMatchers.eq(cohortId))).thenReturn(mockCohortPatients.get(cohortId));
        }
        // mock patient identifiers response
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            Mockito.when(mockDDPRepository.getPatientIdentifiers(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(mockPatientIdentifiers.get(deIdentifiedPid));
        }
        // mock patient demographics response
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            Mockito.when(mockDDPRepository.getPatientDemographics(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(mockPatientDemographics.get(deIdentifiedPid));
        }
        // mock patient diagnosis response
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            Mockito.when(mockDDPRepository.getPatientDiagnoses(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(mockPatientDiagnoses.get(deIdentifiedPid));
        }
        // mock patient radiation response
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            Mockito.when(mockDDPRepository.getPatientRadiationProcedures(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(mockPatientRadiationProcedures.get(deIdentifiedPid));
        }
        // mock patient chemo response
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            Mockito.when(mockDDPRepository.getPatientChemoProcedures(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(mockPatientChemoProcedures.get(deIdentifiedPid));
        }
        // mock patient surgery response - ignore records with no description provided
        for (Integer deIdentifiedPid : COHORT_PATIENT_DE_IDS) {
            List<Surgery> procedures = mockPatientSurgicalProcedures.get(deIdentifiedPid);
            List<Surgery> toReturn = new ArrayList<>();
            if (procedures == null || procedures.isEmpty()) {
                toReturn = procedures;
            }
            else {
                for (Surgery procedure : procedures) {
                    if (!Strings.isNullOrEmpty(procedure.getProcedureDescription())) {
                        toReturn.add(procedure);
                    }
                }
            }
            Mockito.when(mockDDPRepository.getPatientSurgicalProcedures(ArgumentMatchers.eq(deIdentifiedPid.toString()))).thenReturn(toReturn);
        }
        return mockDDPRepository;
    }

    @Bean(name="mockCompositePatientRecords")
    public Map<String, DDPCompositeRecord> mockCompositePatientRecords() throws Exception {
        Map<String, DDPCompositeRecord> map = new HashMap<>();

        // patient 1 composite record - does not have radiation, chemo data
        // TO-DO: mock surgery data
        DDPCompositeRecord record = new DDPCompositeRecord("P01-000001");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000001).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000001));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000001));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000001));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000001));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000001));
        map.put(record.getDmpPatientId(), record);

        // patient 2 composite record
        // TO-DO: mock surgery, radiation, chemo data
        record = new DDPCompositeRecord("P01-000002");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000002).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000002));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000002));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000002));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000002));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000002));
        map.put(record.getDmpPatientId(), record);

        // patient 3 does not resolve to dmp patient so no data available, nothing to construct

        // patient 4 - user unauthorized, no demographics, diagnosis, or treatment data available
        record = new DDPCompositeRecord("P01-000004");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000004).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000004));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000004));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000004));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000004));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000004));
        map.put(record.getDmpPatientId(), record);

        // patient 5 composite record - does not have diagnosis data, so cannot calculate start/stop dates for treatment data
        // therefore no treatment data available either
        // NOTE: if there's another reference date we can use instead of first date of diagnosis
        // then we can calculate start/stop dates regardless of availability of diagnosis data
        record = new DDPCompositeRecord("P01-000005");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000005).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000005));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000005));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000005));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000005));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000005));
        map.put(record.getDmpPatientId(), record);

        // patient 6 composite record - does not have chemo, surgery data
        // TO-DO: mock radiation
        record = new DDPCompositeRecord("P01-000006");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000006).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000006));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000006));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000006));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000006));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000006));
        map.put(record.getDmpPatientId(), record);

        // patient 7 composite record - does not have radiation, surgery data
        // TO-DO: mock chemo
        record = new DDPCompositeRecord("P01-000007");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000007).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000007));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000007));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000007));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000007));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000007));
        map.put(record.getDmpPatientId(), record);

        // patient 8 composite record - does not have radiation data
        // TO-DO: mock chemo, surgery
        record = new DDPCompositeRecord("P01-000008");
        record.setDmpSampleIds(mockPatientIdentifiers.get(1000008).getDmpSampleIds());
        record.setPatientDemographics(mockPatientDemographics.get(1000008));
        record.setPatientDiagnosis(mockPatientDiagnoses.get(1000008));
        record.setRadiationProcedures(mockPatientRadiationProcedures.get(1000008));
        record.setChemoProcedures(mockPatientChemoProcedures.get(1000008));
        record.setSurgicalProcedures(mockPatientSurgicalProcedures.get(1000008));
        map.put(record.getDmpPatientId(), record);

        return map;
    }
}
