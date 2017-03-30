/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.apache.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.JobScope;

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
    
    @Value("${redcap_url}")
    private String redcapUrl;        
    
    @Value("${mapping_token}")
    private String mappingToken;        
    
    @Value("#{jobParameters[redcap_project]}")
    private String project;
    
    @Value("${metadata_project}")
    private String metadataProject;
          
    private Map<String, String> tokens = new HashMap<>();
    private Map<String, String>  timeline = new HashMap<>();
    private List<Map<String, String>> records;
    private List<Map<String, String>> timelineRecords;
    List<RedcapAttributeMetadata> metadata;
    
    private List<String> sampleHeader;
    private List<String> patientHeader;
    private List<String> combinedHeader;
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
        List<String> keys = new ArrayList(tokens.keySet());
        nextClinicalId = keys.get(0);
        return nextClinicalId;
    }
    
    @Override
    public String getNextTimelineStudyId() {
        checkTokens();
        List<String> keys = new ArrayList(timeline.keySet());
        nextTimelineId = keys.get(0);
        return nextTimelineId;
    }
    
    @Override
    public boolean hasMoreTimelineData() {
        return !timeline.isEmpty();
    }
    
    @Override
    public boolean hasMoreClinicalData() {
        return !tokens.isEmpty();
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
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(redcapUrl, HttpMethod.POST, requestEntity, RedcapAttributeMetadata[].class);
        return Arrays.asList(responseEntity.getBody());        
    }
    
    private List<RedcapProjectAttribute> getAttributes(boolean timelineData) {
        String projectToken;
        if(timelineData) {
            projectToken = timeline.get(nextTimelineId);
        }
        else {
            projectToken = tokens.get(nextClinicalId);
        }  
        
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", projectToken);
        uriVariables.add("content", "metadata");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting attributes for project...");
        ResponseEntity<RedcapProjectAttribute[]> responseEntity = restTemplate.exchange(redcapUrl, HttpMethod.POST, requestEntity, RedcapProjectAttribute[].class);
        return Arrays.asList(responseEntity.getBody());
    }
    
    private  List<Map<String, String>> getClinicalData(boolean timelineData) {        
        String projectToken;
        if(timelineData) {
            projectToken = timeline.remove(nextTimelineId);
        }
        else {
            Set<String> keySet = tokens.keySet();
            projectToken = tokens.remove(nextClinicalId);
        }
        
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", projectToken);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting data for project...");
        ResponseEntity<ObjectNode[]> responseEntity = restTemplate.exchange(redcapUrl, HttpMethod.POST, requestEntity, ObjectNode[].class);    
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
        ResponseEntity<RedcapToken[]> responseEntity = restTemplate.exchange(redcapUrl, HttpMethod.POST, requestEntity, RedcapToken[].class);
        
        for (RedcapToken token : responseEntity.getBody()) {
            if (token.getStableId().equals(project)) {
                if (token.getStudyId().toUpperCase().contains("TIMELINE")) {
                    timeline.put(token.getStudyId(), token.getApiToken());
                }
                else {
                    tokens.put(token.getStudyId(), token.getApiToken());
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
    
    public static void main(String[] args) {}

}
