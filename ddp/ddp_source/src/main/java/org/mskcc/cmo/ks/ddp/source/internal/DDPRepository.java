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

import org.mskcc.cmo.ks.ddp.source.model.Cohort;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientDemographics;
import org.mskcc.cmo.ks.ddp.source.model.PatientDiagnosis;
import org.mskcc.cmo.ks.ddp.source.model.PatientIdentifiers;
import org.mskcc.cmo.ks.ddp.source.util.DDPResponseUtil;
import org.mskcc.cmo.ks.ddp.source.util.AuthenticationUtil;

import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ochoaa
 */
@Repository
public class DDPRepository {

    @Value("${ddp.base_url}")
    private String ddpBaseUrl;

    @Value("${ddp.cohorts.endpoint}")
    private String ddpCohortsEndpoint;

    @Value("${ddp.cohorts.pt.endpoint}")
    private String ddpCohortsPatientEndpoint;

    @Value("${ddp.pt.demographics.endpoint}")
    private String ddpPtDemographicsEndpoint;

    @Value("${ddp.pt.diagnosis.endpoint}")
    private String ddpPtDiagnosisEndpoint;

    @Value("${ddp.pt.identifiers.endpoint}")
    private String ddpPtIdentifiersEndpoint;

    @Autowired
    AuthenticationUtil authenticationUtil;

    @Autowired
    DDPResponseUtil ddpResponseUtil;

    private final String BEARER_KEYWORD = "Bearer ";

    private final Logger LOG = Logger.getLogger(DDPRepository.class);

    public List<Cohort> getAuthorizedCohorts() {
        String url = ddpBaseUrl + ddpCohortsEndpoint;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getRequestEntity(), String.class);
        List<Cohort> cohortData = new ArrayList();
        if (response.getBody() !=  null) {
            try {
                cohortData = (List<Cohort>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<Cohort>>(){});
            }
            catch (Exception e) {
                throw new RuntimeException("Error fetching authorized cohorts for user: " + authenticationUtil.getUsername(), e);
            }
        }
        return cohortData;
    }

    public List<CohortPatient> getPatientsByCohort(Integer cohortId) {
        String url = ddpBaseUrl + ddpCohortsEndpoint + String.valueOf(cohortId) + "/" + ddpCohortsPatientEndpoint;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getRequestEntity(), String.class);
        List<CohortPatient> cohortPatients = new ArrayList();
        if (response.getBody() != null) {
            try {
                cohortPatients = (List<CohortPatient>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<CohortPatient>>(){});
            }
            catch (Exception e) {
                throw new RuntimeException("Error fetching patients by cohort id", e);
            }
        }
        return cohortPatients;
    }

    /**
     * Returns patient demographics given a patient id.
     * Patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * @param patientId
     * @return
     */
    public PatientDemographics getPatientDemographics(String patientId) {
        String url = ddpBaseUrl + ddpPtDemographicsEndpoint;
        HttpEntity<String> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        List<PatientDemographics> patientDemographics = null;
        if (!Strings.isNullOrEmpty(response.getBody())) {
            try {
                patientDemographics = (List<PatientDemographics>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<PatientDemographics>>(){});
            }
            catch (Exception e) {
                throw new RuntimeException("Error fetching patient demographics", e);
            }
        }
        PatientDemographics toReturn = new PatientDemographics();
        if (!patientDemographics.isEmpty()) {
            toReturn = patientDemographics.get(0);
        }
        return toReturn;
    }

    public List<PatientDiagnosis> getPatientDiagnoses(String patientId) {
        String url = ddpBaseUrl + ddpPtDiagnosisEndpoint;
        HttpEntity<String> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        List<PatientDiagnosis> patientDiagnosisList = new ArrayList();
        // TO-DO: Not all patients have diagnosis data! We should store/report these for vetting
        if (!Strings.isNullOrEmpty(response.getBody())) {
            try {
                patientDiagnosisList = (List<PatientDiagnosis>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<PatientDiagnosis>>(){});
            }
            catch (Exception e) {
                throw new RuntimeException("Error fetching patient diagnoses", e);
            }
        }
        return patientDiagnosisList;
    }

    public PatientIdentifiers getPatientIdentifiers(String patientId) {
        String url = ddpBaseUrl + ddpPtIdentifiersEndpoint;
        HttpEntity<String> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        PatientIdentifiers patientIdentifiers = null;
        if (!Strings.isNullOrEmpty(response.getBody())) {
            try {
                patientIdentifiers = (PatientIdentifiers) ddpResponseUtil.parseData(response.getBody(), new TypeReference<PatientIdentifiers>(){});
            }
            catch (Exception e) {
                throw new RuntimeException("Error fetching patient identifiers", e);
            }
        }
        return patientIdentifiers;
    }

    public HttpEntity<String> getRequestEntityWithId(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", BEARER_KEYWORD + authenticationUtil.getAuthenticationToken());
        String idParamStr = "{\"id\": \"" + id + "\"}";
        return new HttpEntity<>(idParamStr, headers);
    }

    public HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", BEARER_KEYWORD + authenticationUtil.getAuthenticationToken());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        return requestEntity;
    }
}
