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

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapToken;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.*;
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
public class MetadataManagerRedcapImpl implements MetadataManager {

    @Autowired
    private RedcapSessionManager redcapSessionManager;

    private List<RedcapAttributeMetadata> metadata;
    private List<RedcapAttributeMetadata> namespace;

    String metadataToken;
    String namespaceToken;

     private final Logger log = Logger.getLogger(MetadataManagerRedcapImpl.class);

    @Override
    public Map<String, List<String>> getFullHeader(List<String> header) {
        metadata = getMetadata();
        namespace = getNamespace();
        Map<String, RedcapAttributeMetadata> combinedAttributeMap = new LinkedHashMap<>();
        for (String attribute : header) {
            String extColHeader = "";
            for (RedcapAttributeMetadata namespaceEntry : namespace) {
                if (namespaceEntry.getExternalColumnHeader().equals(attribute.toUpperCase())) {
                    // update attribute name and external column header values
                    // since coming from a namespace and not the metadata source
                    attribute = namespaceEntry.getNormalizedColumnHeader();
                    extColHeader = namespaceEntry.getExternalColumnHeader();
                }
            }
            for (RedcapAttributeMetadata meta : metadata) {
                if (attribute.toUpperCase().equals(meta.getRedcapId().toUpperCase())) {
                    // update external column header if not empty
                    if (!extColHeader.isEmpty()) {
                        meta.setExternalColumnHeader(extColHeader);
                    }
                    combinedAttributeMap.put(attribute, meta);
                }
            }
        }
        return makeHeader(combinedAttributeMap);
    }

    private List<RedcapAttributeMetadata> getMetadata() {
        if (metadata != null) {
            return metadata;
        }
        RestTemplate restTemplate = new RestTemplate();

        log.info("Getting attribute metadatas...");

        String metadataToken = redcapSessionManager.getMetadataTokenFilterAllStudiesIfNecessary();
        LinkedMultiValueMap<String, String> uriVariables = getUriVariables(metadataToken);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = redcapSessionManager.getRequestEntity(uriVariables);
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(redcapSessionManager.getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapAttributeMetadata[].class);
        return Arrays.asList(responseEntity.getBody());
    }

    private List<RedcapAttributeMetadata> getNamespace() {
        if (namespace != null) {
            return namespace;
        }
        RestTemplate restTemplate = new RestTemplate();

        log.info("Getting attribute namespace...");

        String namespaceToken = redcapSessionManager.getNamespaceTokenFilterAllStudiesIfNecessary();
        LinkedMultiValueMap<String, String> uriVariables = getUriVariables(namespaceToken);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = redcapSessionManager.getRequestEntity(uriVariables);
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(redcapSessionManager.getRedcapApiURI(), HttpMethod.POST, requestEntity, RedcapAttributeMetadata[].class);
        return Arrays.asList(responseEntity.getBody());
    }

    private Map<String,List<String>> makeHeader(Map<String, RedcapAttributeMetadata> attributeMap) {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        List<String> displayNames = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        List<String> datatypes = new ArrayList<>();
        List<String> priorities = new ArrayList<>();
        List<String> attributeTypes = new ArrayList<>();
        List<String> externalHeader = new ArrayList<>();
        List<String> header = new ArrayList<>();

        for (Map.Entry<String, RedcapAttributeMetadata> entry : attributeMap.entrySet()) {
            displayNames.add(entry.getValue().getDisplayName());
            descriptions.add(entry.getValue().getDescriptions());
            datatypes.add(entry.getValue().getDatatype());
            priorities.add(entry.getValue().getPriority());
            attributeTypes.add(entry.getValue().getAttributeType());
            externalHeader.add(entry.getValue().getExternalColumnHeader());
            header.add(entry.getValue().getNormalizedColumnHeader());
        }

        headerMap.put("display_names", displayNames);
        headerMap.put("descriptions", descriptions);
        headerMap.put("datatypes", datatypes);
        headerMap.put("priorities", priorities);
        headerMap.put("attribute_types", attributeTypes);
        headerMap.put("external_header", externalHeader);
        headerMap.put("header", header);
        return headerMap;
    }

    private LinkedMultiValueMap<String, String> getUriVariables(String token) {
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        uriVariables.add("token", token);
        uriVariables.add("content", "record");
        uriVariables.add("format", "json");
        uriVariables.add("type", "flat");
        return uriVariables;
    }
}
