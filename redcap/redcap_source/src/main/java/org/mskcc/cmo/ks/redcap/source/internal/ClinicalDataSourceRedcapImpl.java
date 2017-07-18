/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.models.ProjectInfoResponse;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Zachary Heins
 *
 * Use Redcap to fetch clinical metadata and data
 *
 */

@Configuration
@JobScope
public class ClinicalDataSourceRedcapImpl implements ClinicalDataSource {

    //Do not use these directly .. instead call redcapBaseURI() and redcapApiURI() methods
    //TODO: move these into a URI management class/bean (singleton)
    private static URI redcapBaseURI = null;
    private static URI redcapApiURI = null;

    @Value("${redcap_base_url}")
    private String redcapBaseUrl;
    @Value("${redcap_erase_project_data_url_path}")
    private String redcapEraseProjectDataUrlPath;
    @Value("${redcap_username}")
    private String redcapUsername;
    @Value("${redcap_password}")
    private String redcapPassword;
    @Value("${redcap_login_hidden_input_name}")
    private String redcapLoginHiddenInputName;
    @Value("${mapping_token}")
    private String mappingToken;
    @Value("#{jobParameters[redcap_project]}")
    private String project;
    @Value("${metadata_project}")
    private String metadataProject;

    private Map<String, String> clinicalDataTokens = new HashMap<>();
    private Map<String, String>  clinicalTimelineTokens = new HashMap<>();
    private List<Map<String, String>> records;
    private List<Map<String, String>> timelineRecords;
    List<RedcapAttributeMetadata> metadata;

    private List<String> sampleHeader;
    private List<String> patientHeader;
    private List<String> combinedHeader;
    private Map<String, List<String>> fullPatientHeader = new HashMap<>();
    private Map<String, List<String>> fullSampleHeader = new HashMap<>();
    String nextClinicalId;
    String nextTimelineId;
    String metadataToken;
    boolean gotTokens;

     private final Logger log = Logger.getLogger(ClinicalDataSourceRedcapImpl.class);

    @Override
    public List<String> getSampleHeader() {
        checkTokens();
        getClinicalHeaderData();
        return sampleHeader;
    }

    @Override
    public List<String> getPatientHeader() {
        checkTokens();
        getClinicalHeaderData();
        return patientHeader;
    }

    @Override
    public List<String> getTimelineHeader() {
        checkTokens();
        getTimelineHeaderData();
        return combinedHeader;
    }

    @Override
    public List<Map<String, String>> getClinicalData() {
        checkTokens();
        return records = getClinicalData(false);
    }

    @Override
    public List<Map<String, String>> getTimelineData() {
        checkTokens();
        return timelineRecords = getClinicalData(true);

    }

    @Override
    public String getNextClinicalStudyId() {
        checkTokens();
        List<String> keys = new ArrayList(clinicalDataTokens.keySet());
        nextClinicalId = keys.get(0);
        return nextClinicalId;
    }

    @Override
    public String getNextTimelineStudyId() {
        checkTokens();
        List<String> keys = new ArrayList(clinicalTimelineTokens.keySet());
        nextTimelineId = keys.get(0);
        return nextTimelineId;
    }

    @Override
    public boolean hasMoreTimelineData() {
        return !clinicalTimelineTokens.isEmpty();
    }

    @Override
    public boolean hasMoreClinicalData() {
        return !clinicalDataTokens.isEmpty();
    }

    @Override
    public void importClinicalDataFile(String studyId, String filename) {
        checkTokens();
        String token = clinicalDataTokens.getOrDefault(studyId, null);
//TODO: ROB .. we must also look for the timeline token here
        if (token == null) {
            log.error("Study not found in redcap clinicalDataTokens: " + studyId);
            return;
        }
        deleteRedcapProjectData(token);
        formatClinicalData(filename);
        importClinicalData(token, filename);
    }

    private URI getRedcapURI() {
        if (redcapBaseURI == null) {
            try {
                redcapBaseURI = new URI(redcapBaseUrl.trim() + "/");
             } catch (URISyntaxException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return redcapBaseURI;
    }

    private URI getRedcapApiURI() {
        if (redcapApiURI == null) {
            URI base = getRedcapURI();
            redcapApiURI = base.resolve("api/");
        }
        return redcapApiURI;
    }

    private URI getRedcapEraseProjectDataURI(String projectId) {
        URI base = getRedcapURI();
        return base.resolve(redcapEraseProjectDataUrlPath + "/?pid=" + projectId + "&action=erase_data");
    }

    private void getClinicalHeaderData() {
        metadata = getMetadata();
        List<RedcapProjectAttribute> attributes = getAttributes(false);

        Map<RedcapProjectAttribute, RedcapAttributeMetadata> sampleAttributeMap = new LinkedHashMap<>();
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> patientAttributeMap = new LinkedHashMap<>();

        for (RedcapProjectAttribute attribute : attributes) {
            for (RedcapAttributeMetadata meta : metadata) {
                if (attribute.getFieldName().toUpperCase().equals(meta.getNormalizedColumnHeader().toUpperCase())) {
                    if(meta.getAttributeType().equals("SAMPLE")) {
                        sampleAttributeMap.put(attribute, meta);
                        break;
                    }
                    else {
                        patientAttributeMap.put(attribute, meta);
                        break;
                    }
                }
            }
        }
        sampleHeader = makeHeader(sampleAttributeMap);
        patientHeader = makeHeader(patientAttributeMap);
    }

    private void getTimelineHeaderData() {
        metadata = getMetadata();
        List<RedcapProjectAttribute> attributes = getAttributes(true);
        Map<RedcapProjectAttribute, RedcapAttributeMetadata> combinedAttributeMap = new LinkedHashMap<>();
         for (RedcapProjectAttribute attribute : attributes) {
            for (RedcapAttributeMetadata meta : metadata) {
                if (attribute.getFieldName().toUpperCase().equals(meta.getRedcapId().toUpperCase())) {
                    combinedAttributeMap.put(attribute, meta);
                }
            }
        }

         combinedHeader = makeHeader(combinedAttributeMap);
    }

    private List<RedcapAttributeMetadata> getMetadata() {
        if (metadata  != null) {
            return metadata;
        }
        RestTemplate restTemplate = new RestTemplate();

        log.info("Getting attribute metadatas...");

        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", metadataToken);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapAttributeMetadata[].class);
        return Arrays.asList(responseEntity.getBody());
    }

    private List<RedcapProjectAttribute> getAttributes(boolean timelineData) {
        String projectToken;
        if(timelineData) {
            projectToken = clinicalTimelineTokens.get(nextTimelineId);
        }
        else {
            projectToken = clinicalDataTokens.get(nextClinicalId);
        }

        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", projectToken);
        uriVariables.add("content", "metadata");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting attributes for project...");
        ResponseEntity<RedcapProjectAttribute[]> responseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapProjectAttribute[].class);
        return Arrays.asList(responseEntity.getBody());
    }

    private  List<Map<String, String>> getClinicalData(boolean timelineData) {
        String projectToken;
        if(timelineData) {
            projectToken = clinicalTimelineTokens.remove(nextTimelineId);
        }
        else {
            Set<String> keySet = clinicalDataTokens.keySet();
            projectToken = clinicalDataTokens.remove(nextClinicalId);
        }

        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", projectToken);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting data for project...");
        ResponseEntity<ObjectNode[]> responseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, requestEntity, ObjectNode[].class);
        List<Map<String, String>> responses = new ArrayList<>();

        for(ObjectNode response : responseEntity.getBody())
        {
            Map<String, String> map = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> nodeIterator = response.fields();
            while (nodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodeIterator.next();
                map.put(entry.getKey().toUpperCase(), entry.getValue().asText());
            }
            responses.add(map);
        }
        return responses;
    }

    private List<String> makeHeader(Map<RedcapProjectAttribute, RedcapAttributeMetadata> attributeMap) {
        List<String> header = new ArrayList<>();
        for (Map.Entry<RedcapProjectAttribute, RedcapAttributeMetadata> entry : attributeMap.entrySet()) {
            header.add(entry.getValue().getNormalizedColumnHeader());
        }

        return header;
    }

    private void fillTokens() {
        RestTemplate restTemplate = new RestTemplate();

        log.info("Getting tokens for clinical data processor...");

        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", mappingToken);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        ResponseEntity<RedcapToken[]> responseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapToken[].class);

        for (RedcapToken token : responseEntity.getBody()) {
            if (token.getStableId().equals(project)) {
                if (token.getStudyId().toUpperCase().contains("TIMELINE")) {
                    clinicalTimelineTokens.put(token.getStudyId(), token.getApiToken());
                }
                else {
                    clinicalDataTokens.put(token.getStudyId(), token.getApiToken());
                }
            }
            if (token.getStableId().equals(metadataProject)) {
                metadataToken = token.getApiToken();
            }
        }
    }

    private HttpEntity getRequestEntity(LinkedMultiValueMap<String, String> uriVariables)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return new HttpEntity<LinkedMultiValueMap<String, String>>(uriVariables, headers);
    }

    private void checkTokens() {
        if (!gotTokens) {
            fillTokens();
            gotTokens = true;
        }
    }

    /**
     * Generates list of patient attributes from full header from redcap.
     * @param fullHeader
     * @return
     */
    @Override
    public Map<String, List<String>> getFullPatientHeader(Map<String, List<String>> fullHeader) {
        List<String> displayNames = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        List<String> datatypes = new ArrayList<>();
        List<String> priorities = new ArrayList<>();
        List<String> externalHeader = new ArrayList<>();
        List<String> header = new ArrayList<>();

        for (int i=0; i<fullHeader.get("header").size(); i++) {
            if (fullHeader.get("attribute_types").get(i).equals("PATIENT")) {
                displayNames.add(fullHeader.get("display_names").get(i));
                descriptions.add(fullHeader.get("descriptions").get(i));
                datatypes.add(fullHeader.get("datatypes").get(i));
                priorities.add(fullHeader.get("priorities").get(i));
                externalHeader.add(fullHeader.get("external_header").get(i));
                header.add(fullHeader.get("header").get(i));
            }
        }
        fullPatientHeader.put("display_names", displayNames);
        fullPatientHeader.put("descriptions", descriptions);
        fullPatientHeader.put("datatypes", datatypes);
        fullPatientHeader.put("priorities", priorities);
        fullPatientHeader.put("external_header", externalHeader);
        fullPatientHeader.put("header", header);
        return fullPatientHeader;
    }

    /**
     * Generates list of sample attributes from full header from redcap.
     * @param fullHeader
     * @return
     */
    @Override
    public Map<String, List<String>> getFullSampleHeader(Map<String, List<String>> fullHeader) {
        List<String> displayNames = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        List<String> datatypes = new ArrayList<>();
        List<String> priorities = new ArrayList<>();
        List<String> externalHeader = new ArrayList<>();
        List<String> header = new ArrayList<>();

        for (int i=0; i<fullHeader.get("header").size(); i++) {
            if (fullHeader.get("attribute_types").get(i).equals("SAMPLE")) {
                displayNames.add(fullHeader.get("display_names").get(i));
                descriptions.add(fullHeader.get("descriptions").get(i));
                datatypes.add(fullHeader.get("datatypes").get(i));
                priorities.add(fullHeader.get("priorities").get(i));
                externalHeader.add(fullHeader.get("external_header").get(i));
                header.add(fullHeader.get("header").get(i));
            }
        }
        fullSampleHeader.put("display_names", displayNames);
        fullSampleHeader.put("descriptions", descriptions);
        fullSampleHeader.put("datatypes", datatypes);
        fullSampleHeader.put("priorities", priorities);
        fullSampleHeader.put("external_header", externalHeader);
        fullSampleHeader.put("header", header);
        return fullSampleHeader;
    }

    private String getSessionCookieFromRedcap() {
        RestTemplate restTemplate = new RestTemplate();
        LinkedMultiValueMap<String, String> loginUriVariables = new LinkedMultiValueMap<>();
        loginUriVariables.add("username", redcapUsername);
        loginUriVariables.add("password", redcapPassword);
        loginUriVariables.add(redcapLoginHiddenInputName, "");
        loginUriVariables.add("submitted", "1");
        HttpEntity<LinkedMultiValueMap<String, Object>> loginRequestEntity = getRequestEntity(loginUriVariables);
        ResponseEntity<String> loginResponseEntity = restTemplate.exchange(getRedcapURI(), HttpMethod.POST, loginRequestEntity, String.class);
        HttpStatus responseStatus = loginResponseEntity.getStatusCode();
        if (!responseStatus.is2xxSuccessful() && !responseStatus.is3xxRedirection()) {
            log.warn("RedCap login with username/password/hiddenInput attempt failed. HTTP status code = " + Integer.toString(loginResponseEntity.getStatusCode().value()));
            return null;
        }
        Predicate<String> notDeleted = (String s) -> !s.contains("deleted");
        List<String> cookies = loginResponseEntity.getHeaders().get("Set-Cookie").stream().filter(notDeleted).collect(Collectors.toList());
        if (cookies.size() < 1) {
            log.warn("RedCap login succeeded but no Set-Cookie header field was included.");
            return null;
        }
        return cookies.get(0).split(";")[0];
    }

    private String getProjectIdFromRedcap(String token, String cookie) {
        RestTemplate restTemplate = new RestTemplate();
        LinkedMultiValueMap<String, String> projectInfoUriVariables = new LinkedMultiValueMap<>();
        projectInfoUriVariables.add("token", token);
        projectInfoUriVariables.add("content", "project");
        projectInfoUriVariables.add("format", "json");
        projectInfoUriVariables.add("returnFormat", "json");
        HttpEntity<LinkedMultiValueMap<String, Object>> projectInfoRequestEntity = getRequestEntity(projectInfoUriVariables);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<?> rq = new HttpEntity<>(headers);
        ResponseEntity<ProjectInfoResponse> projectInfoResponseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, projectInfoRequestEntity, ProjectInfoResponse.class);
        HttpStatus responseStatus = projectInfoResponseEntity.getStatusCode();
        if (!responseStatus.is2xxSuccessful() && !responseStatus.is3xxRedirection()) {
            log.warn("RedCap request for project data failed. HTTP status code = " + Integer.toString(projectInfoResponseEntity.getStatusCode().value()));
            return null;
        }
        return projectInfoResponseEntity.getBody().getProjectId();
    }

    private void deleteRedcapProjectData(String cookie, String projectId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<?> rq = new HttpEntity<>(headers);
        HttpEntity<?> response = restTemplate.exchange(getRedcapEraseProjectDataURI(projectId), HttpMethod.GET, rq, String.class);
    }

    private void deleteRedcapProjectData(String token) {
        String cookie = getSessionCookieFromRedcap();
        if (cookie == null) {
            log.warn("RedCap session cookie not available; unable to delete project data");
            throw new RuntimeException("RedCap session cookie not available; unable to delete project data");
        }
        String projectId = getProjectIdFromRedcap(token, cookie);
        if (projectId == null) {
            log.warn("ProjectId not available from RedCap getProjectData API request");
            throw new RuntimeException("ProjectId not available from RedCap getProjectData API request");
        }
        log.info("delete using cookie: " + cookie);
        log.info("delete using project: " + projectId);
        deleteRedcapProjectData(cookie, projectId);
    }

    private void formatClinicalData(String filename) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void importClinicalData(String token, String dataFile) {
        RestTemplate restTemplate = new RestTemplate();
        LinkedMultiValueMap<String, String> importRecordUriVariables = new LinkedMultiValueMap<>();
        importRecordUriVariables.add("token", token);
        importRecordUriVariables.add("content", "record");
        importRecordUriVariables.add("format", "csv");
        importRecordUriVariables.add("overwriteBehavior", "overwrite");
        String cannedData = "sample_id,patient_id\n" +
                            "P-0000000-T01-IM3,P-0000000\n" +
                            "P-0000000-T02-IM3,P-0000000\n";
        importRecordUriVariables.add("data", cannedData);
        HttpEntity<LinkedMultiValueMap<String, Object>> importRecordRequestEntity = getRequestEntity(importRecordUriVariables);
        ResponseEntity<String> importRecordResponseEntity = restTemplate.exchange(getRedcapApiURI(), HttpMethod.POST, importRecordRequestEntity, String.class);
        HttpStatus responseStatus = importRecordResponseEntity.getStatusCode();
        if (!responseStatus.is2xxSuccessful() && !responseStatus.is3xxRedirection()) {
            log.warn("RedCap import record API call failed. HTTP status code = " + Integer.toString(importRecordResponseEntity.getStatusCode().value()));
            throw new RuntimeException("RedCap import record API call failed. HTTP status code");
        }
log.info("Return from call to Import Recap Record API:" + importRecordResponseEntity.getBody());
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) {}
}
