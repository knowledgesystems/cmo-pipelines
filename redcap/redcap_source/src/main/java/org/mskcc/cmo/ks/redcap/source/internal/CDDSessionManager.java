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

package org.mskcc.cmo.ks.redcap.source.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.OverriddenCancerStudy;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Repository
public class CDDSessionManager {

    private static URI cddBaseURI = null;
    private static URI cddApiURI = null;
    private static URI cddStudyIdURI = null;
    private static Boolean cachedUsePostRequestForEraseProjectData = null;

    @Value("${cdd_base_url}")
    private String cddBaseUrl;

    private final Logger log = Logger.getLogger(CDDSessionManager.class);

    // SECTION : URI construction
    public URI getCddURI() {
        if (cddBaseURI == null) {
            try {
                cddBaseURI = new URI(cddBaseUrl + "/");
            } catch (URISyntaxException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return cddBaseURI;
    }

    public URI getCddStudyIdURI(String studyId) {
        if (cddApiURI == null) {
            URI base = getCddURI();
            cddApiURI = base.resolve("?cancerStudy=" + studyId);
        }
        return cddApiURI;
    }

    public URI getCddOverridesURI() {
        if (cddStudyIdURI == null) {
            URI base = getCddURI();
            cddStudyIdURI = base.resolve("cancerStudies/");
        }
        return cddStudyIdURI;
    }

    public HttpEntity<LinkedMultiValueMap<String, String>> getRequestEntity(LinkedMultiValueMap<String, String> uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return new HttpEntity<LinkedMultiValueMap<String, String>>(uriVariables, headers);
    }

    public OverriddenCancerStudy[] getOverriddenStudies(String studyId) {
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, String>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting list of studies with overrides...");
        ResponseEntity<OverriddenCancerStudy[]> responseEntity = restTemplate.exchange(getCddOverridesURI(), HttpMethod.GET, requestEntity, OverriddenCancerStudy[].class);
        return responseEntity.getBody();
    }

    public RedcapAttributeMetadata[] getRedcapMetadata() {
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, String>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting default attribute metadata..");
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(getCddURI(), HttpMethod.GET, requestEntity, RedcapAttributeMetadata[].class);
        return responseEntity.getBody();
    }

    public RedcapAttributeMetadata[] getRedcapMetadataWithOverrides(String studyId) {
        LinkedMultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, String>> requestEntity = getRequestEntity(uriVariables);
        log.info("Getting " + studyId + "overridden attribute metadata..");
        ResponseEntity<RedcapAttributeMetadata[]> responseEntity = restTemplate.exchange(getCddStudyIdURI(studyId), HttpMethod.GET, requestEntity, RedcapAttributeMetadata[].class);
        return responseEntity.getBody();
    }
}
