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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Zachary Heins
 *
 * Use Redcap to fetch clinical metadata and data
 *
 */
@Repository
public class ClinicalDataSourceRedcapImpl implements ClinicalDataSource {

    @Autowired
    private RedcapSessionManager redcapSessionManager;

    private Map<String, String> clinicalDataTokens = null;
    private Map<String, String> clinicalTimelineTokens = null;
    private String metadataToken = null;
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

    private final Logger log = Logger.getLogger(ClinicalDataSourceRedcapImpl.class);

    @Override
    public boolean projectExists(String projectTitle) {
        checkTokensByProjectTitle(projectTitle);
        return (!clinicalDataTokens.isEmpty() || !clinicalTimelineTokens.isEmpty());
    }

    @Override
    public List<String> getSampleHeader(String stableId) {
        checkTokensByStableId(stableId);
        getClinicalHeaderData();
        return sampleHeader;
    }

    @Override
    public List<String> getPatientHeader(String stableId) {
        checkTokensByStableId(stableId);
        getClinicalHeaderData();
        return patientHeader;
    }

    @Override
    public List<String> getTimelineHeader(String stableId) {
        checkTokensByStableId(stableId);
        getTimelineHeaderData();
        return combinedHeader;
    }

    @Override
    public List<Map<String, String>> getClinicalData(String stableId) {
        checkTokensByStableId(stableId);
        return records = getClinicalData(false);
    }

    @Override
    public List<Map<String, String>> getTimelineData(String stableId) {
        checkTokensByStableId(stableId);
        return timelineRecords = getClinicalData(true);

    }

    @Override
    public String getNextClinicalProjectTitle(String stableId) {
        checkTokensByStableId(stableId);
        List<String> keys = new ArrayList(clinicalDataTokens.keySet());
        nextClinicalId = keys.get(0);
        return nextClinicalId;
    }

    @Override
    public String getNextTimelineProjectTitle(String stableId) {
        checkTokensByStableId(stableId);
        List<String> keys = new ArrayList(clinicalTimelineTokens.keySet());
        nextTimelineId = keys.get(0);
        return nextTimelineId;
    }

    @Override
    public boolean hasMoreTimelineData(String stableId) {
        checkTokensByStableId(stableId);
        return !clinicalTimelineTokens.isEmpty();
    }

    @Override
    public boolean hasMoreClinicalData(String stableId) {
        checkTokensByStableId(stableId);
        return !clinicalDataTokens.isEmpty();
    }

    @Override
    public void importClinicalDataFile(String projectTitle, String filename) {
        checkTokensByProjectTitle(projectTitle);
        String token = clinicalDataTokens.getOrDefault(projectTitle, null);
        if (token == null) {
            token = clinicalTimelineTokens.getOrDefault(projectTitle, null);
            if (token == null) {
                log.error("Project not found in redcap clinicalDataTokens or clincalTimelineTokens: " + projectTitle);
                return;
            }
        }
        try {
            File file = new File(filename);
            if (!file.exists()) {
                log.error("error : could not find file " + filename);
                return;
            }
            List<String> dataFileContentsTSV = readClinicalFile(file);
            List<String> dataFileContentsCSV = convertTSVtoCSV(dataFileContentsTSV);
            if (dataFileContentsCSV.size() == 0) {
                log.error("error: file " + filename + " was empty ... aborting attempt to import data");
                return;
            }
            String dataForImport = String.join("\n",dataFileContentsCSV.toArray(new String[0])) + "\n";
            redcapSessionManager.deleteRedcapProjectData(token);
            if (dataFileContentsCSV.size() == 1) {
                log.warn("file " + filename + " contained a single line (presumed to be the header). RedCap project has been cleared (now has 0 records).");
            } else {
                redcapSessionManager.importClinicalData(token, dataForImport);
                log.info("import completed, " + Integer.toString(dataFileContentsCSV.size() - 1) + " records imported");
            }
        } catch (IOException e) {
            log.error("IOException thrown while attempting to read file " + filename + " : " + e.getMessage());
        }
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
        if (metadata != null) {
            return metadata;
        }
        RestTemplate restTemplate = new RestTemplate();

        log.info("Getting attribute metadatas...");

        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", metadataToken);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = redcapSessionManager.getRequestEntity(uriVariables);
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(redcapSessionManager.getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapAttributeMetadata[].class);
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
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = redcapSessionManager.getRequestEntity(uriVariables);
        log.info("Getting attributes for project...");
        ResponseEntity<RedcapProjectAttribute[]> responseEntity = restTemplate.exchange(redcapSessionManager.getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapProjectAttribute[].class);
        return Arrays.asList(responseEntity.getBody());
    }

    private List<Map<String, String>> getClinicalData(boolean timelineData) {
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
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = redcapSessionManager.getRequestEntity(uriVariables);
        log.info("Getting data for project...");
        ResponseEntity<ObjectNode[]> responseEntity = restTemplate.exchange(redcapSessionManager.getRedcapApiURI(), HttpMethod.POST, requestEntity, ObjectNode[].class);
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

    private boolean gotTokens() {
        return clinicalTimelineTokens != null && clinicalDataTokens != null && metadataToken != null;
    }

    private void checkTokensByProjectTitle(String projectTitle) {
        if (!gotTokens()) {
            clinicalTimelineTokens = redcapSessionManager.getTimelineTokenMapByProjectTitle(projectTitle);
            clinicalDataTokens = redcapSessionManager.getClinicalTokenMapByProjectTitle(projectTitle);
            metadataToken = redcapSessionManager.getMetadataTokenByProjectTitle(projectTitle);
        }
    }

    private void checkTokensByStableId(String stableId) {
        if (!gotTokens()) {
            clinicalTimelineTokens = redcapSessionManager.getTimelineTokenMapByStableId(stableId);
            clinicalDataTokens = redcapSessionManager.getClinicalTokenMapByStableId(stableId);
            metadataToken = redcapSessionManager.getMetadataTokenByStableId(stableId);
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

    private List<String> readClinicalFile(File file) throws IOException {
        LinkedList<String> lineList = new LinkedList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        while (bufferedReader.ready()) {
            String line = bufferedReader.readLine();
            if (line != null) {
                lineList.add(line);
            }
        }
        return lineList;
    }

    private List<String> convertTSVtoCSV(List<String> tsvLines) {
        LinkedList<String> csvLines = new LinkedList<String>();
        for (String tsvLine : tsvLines) {
            String[] tsvFields = tsvLine.split("\t",-1);
            String[] csvFields = new String[tsvFields.length];
            for (int i = 0; i < tsvFields.length; i++) {
                String tsvField = tsvFields[i];
                String csvField = tsvField;
                if (tsvField.indexOf(",") != -1) {
                    csvField = StringEscapeUtils.escapeCsv(tsvField);
                }
                csvFields[i] = csvField;
            }
            csvLines.add(String.join(",", csvFields));
        }
        return csvLines;
    }

    public static void main(String[] args) {}
}
