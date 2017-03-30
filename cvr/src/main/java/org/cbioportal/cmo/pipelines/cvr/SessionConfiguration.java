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

package org.cbioportal.cmo.pipelines.cvr;

import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author heinsz
 */

@Configuration
public class SessionConfiguration {

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.gml_server_name}")
    private String gmlServerName;

    @Value("${dmp.user_name}")
    private String dmpUserName;

    @Value("${dmp.password}")
    private String dmpPassword;

    @Value("${dmp.tokens.create_session}")
    private String dmpCreateSession;

    @Value("${dmp.tokens.create_gml_session}")
    private String gmlCreateSession;

    Logger log = Logger.getLogger(SessionConfiguration.class);

    public final static String SESSION_ID = "cvrSessionId";
    public final static String GML_SESSION = "gmlSessionId";

    /*
    * FULL URL: server_name/create_session/user_name/password/TYPE
    * The TYPE can be 0 or 1, 0 is de-identified, 1 is identified (for clinical information)
    * for CVR fetch for the portal, use 0
    */
    private String dmpUrl;
    private String gmlUrl;

    // Gets the sessionId from CVR
    @Bean
    public String cvrSessionId() {
        try {
            dmpUrl = dmpServerName + dmpCreateSession + "/" + dmpUserName + "/" + dmpPassword + "/0";
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
            ResponseEntity<CVRSession> responseEntity = restTemplate.exchange(dmpUrl, HttpMethod.POST, requestEntity, CVRSession.class);
            return responseEntity.getBody().getSessionId();
        } catch (Exception e) {
            log.error("Unable to secure connection");
            return "NA";
        }
    }

    @Bean
    public String gmlSessionId() {
        try {
            gmlUrl = gmlServerName + gmlCreateSession + "/" + dmpUserName + "/" + dmpPassword + "/0";
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
            ResponseEntity<CVRSession> responseEntity = restTemplate.exchange(gmlUrl, HttpMethod.POST, requestEntity, CVRSession.class);
            return responseEntity.getBody().getSessionId();
        } catch (Exception e) {
            log.error("Unable to secure connection");
            return "NA";
        }
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }
}
