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

import org.mskcc.cmo.ks.ddp.source.model.*;
import org.mskcc.cmo.ks.ddp.source.util.DDPResponseUtil;
import org.mskcc.cmo.ks.ddp.source.util.AuthenticationUtil;

import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import org.apache.commons.lang.exception.ExceptionUtils;
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

    @Value("${ddp.pt.radiation.endpoint}")
    private String ddpPtRadiationEndpoint;

    @Value("${ddp.pt.chemo.endpoint}")
    private String ddpPtChemoEndpoint;

    @Value("${ddp.pt.surgery.endpoint}")
    private String ddpPtSurgeryEndpoint;

    @Autowired
    AuthenticationUtil authenticationUtil;

    @Autowired
    DDPResponseUtil ddpResponseUtil;

    private final String BEARER_KEYWORD = "Bearer ";
    private final String HTTP_401_UNAUTHORIZED = "401 UNAUTHORIZED";

    private final Logger LOG = Logger.getLogger(DDPRepository.class);

    /**
     * Returns authorized cohorts for given user.
     *
     * @return
     */
    public List<Cohort> getAuthorizedCohorts() {
        String url = ddpBaseUrl + ddpCohortsEndpoint;
        RestTemplate restTemplate = new RestTemplate();
        List<Cohort> cohortData = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getRequestEntity(), String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                cohortData = (List<Cohort>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<Cohort>>(){});
            }
            else {
                throw new RuntimeException("Error fetching cohorts, no data in response body");
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            LOG.error("Failed to get cohorts");
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Error fetching authorized cohorts for user: " + authenticationUtil.getUsername(), e);
        }
        return cohortData;
    }

    /**
     * Returns list of CohortPatient instances given a cohort id.
     *
     * @param cohortId
     * @return
     */
    public List<CohortPatient> getPatientsByCohort(Integer cohortId) {
        String url = ddpBaseUrl + ddpCohortsEndpoint + String.valueOf(cohortId) + "/" + ddpCohortsPatientEndpoint;
        RestTemplate restTemplate = new RestTemplate();
        List<CohortPatient> cohortPatients = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getRequestEntity(), String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                cohortPatients = (List<CohortPatient>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<CohortPatient>>(){});
            }
            else {
                throw new RuntimeException("Error fetching patients for cohort '" + cohortId + "', no data in response body");
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to cohort data
            // if 400 then bad request
            String message = "Failed to fetch patients for cohort '" + cohortId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        return cohortPatients;
    }

    /**
     * Returns patient demographics given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public PatientDemographics getPatientDemographics(String patientId) {
        String url = ddpBaseUrl + ddpPtDemographicsEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        List<PatientDemographics> patientDemographics = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientDemographics = (List<PatientDemographics>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<PatientDemographics>>(){});
            }
            else {
                throw new RuntimeException("Error fetching patient demographics, no data in response body");
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Failed to fetch patient demographics: '" + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        PatientDemographics toReturn = new PatientDemographics();
        if (!patientDemographics.isEmpty()) {
            toReturn = patientDemographics.get(0);
        }
        return toReturn;
    }

    /**
     * Returns list of patient diagnoses given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public List<PatientDiagnosis> getPatientDiagnoses(String patientId) {
        String url = ddpBaseUrl + ddpPtDiagnosisEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        List<PatientDiagnosis> patientDiagnosisList = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientDiagnosisList = (List<PatientDiagnosis>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<PatientDiagnosis>>(){});
            }
            else {
                throw new RuntimeException("Error fetching patient diagnoses, no data in response body");
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Failed to fetch patient diagnoses: '" + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        return patientDiagnosisList;
    }

    /**
     * Returns patient identifiers given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public PatientIdentifiers getPatientIdentifiers(String patientId) {
        String url = ddpBaseUrl + ddpPtIdentifiersEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        PatientIdentifiers patientIdentifiers = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientIdentifiers = (PatientIdentifiers) ddpResponseUtil.parseData(response.getBody(), new TypeReference<PatientIdentifiers>(){});
            }
            else {
                throw new RuntimeException("Error fetching patient identifier, no data in response body");
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Error fetching patient identifier: '" + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        return patientIdentifiers;
    }

    /**
     * Returns list of radiation procedures given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public List<Radiation> getPatientRadiationProcedures(String patientId) {
        String url = ddpBaseUrl + ddpPtRadiationEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        List<Radiation> patientRadiationProcedures = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientRadiationProcedures = (List<Radiation>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<Radiation>>(){});
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Failed to fetch patient radiation procedures: " + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        return patientRadiationProcedures;
    }

    /**
     * Returns list of chemo procedures given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public List<Chemotherapy> getPatientChemoProcedures(String patientId) {
        String url = ddpBaseUrl + ddpPtChemoEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        List<Chemotherapy> patientChemoProcedures = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientChemoProcedures = (List<Chemotherapy>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<Chemotherapy>>(){});
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Failed to fetch patient chemo procedures: " + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        return patientChemoProcedures;
    }

    /**
     * Returns list of surgical procedures given a patient id.
     * Although patient ID can be any of the following:
     *  - MRN
     *  - De-identified patient ID
     *  - DMP patient ID
     *
     * Only DMP patient ID's or DDP internal de-identified
     * are used throughout to prevent leaking identifiable
     * patient information.
     *
     * @param patientId
     * @return
     */
    public List<Surgery> getPatientSurgicalProcedures(String patientId) {
        String url = ddpBaseUrl + ddpPtSurgeryEndpoint;
        HttpEntity<Map<String, String>> requestEntity = getRequestEntityWithId(patientId);
        RestTemplate restTemplate = new RestTemplate();
        List<Surgery> patientSurgicalProcedures = new ArrayList();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!Strings.isNullOrEmpty(response.getBody())) {
                patientSurgicalProcedures = (List<Surgery>) ddpResponseUtil.parseData(response.getBody(), new TypeReference<List<Surgery>>(){});
            }
        }
        catch (Exception e) {
            // this can be a RestClientException from restTemplate.exchange, from ddpResponseUtil.parseData, or the exception we throw
            // if 401 unauthorized then we do not have access to patient data
            // if 400 then the dmp id provided does not resolve to a patient id in DDP system
            String message = "Failed to fetch patient surgical procedures: " + patientId + "' - " + e.getLocalizedMessage();
            LOG.error(message);
            LOG.debug(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(message, e);
        }
        // filter out surgical procedures with empty procedure description
        // these are likely records meant for recording purposes only
        List<Surgery> filteredSurgicalProcedures = new ArrayList();
        for (Surgery procedure : patientSurgicalProcedures) {
            if (!Strings.isNullOrEmpty(procedure.getProcedureDescription())) {
                filteredSurgicalProcedures.add(procedure);
            }
        }
        return filteredSurgicalProcedures;
    }

    public HttpEntity<Map<String, String>> getRequestEntityWithId(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", BEARER_KEYWORD + authenticationUtil.getAuthenticationToken());
        Map<String, String> idMap = new HashMap<>();
        idMap.put("id", id);
        return new HttpEntity<>(idMap, headers);
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
