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

package org.mskcc.cmo.ks.ddp.source.util;

import org.mskcc.cmo.ks.ddp.source.model.AuthenticationToken;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.apache.log4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ochoaa
 */
@Repository
public class AuthenticationUtil {

    @Value("${ddp.base_url}")
    private String ddpBaseUrl;

    @Value("${ddp.authtoken.endpoint}")
    private String ddpAuthTokenEndpoint;

    @Value("${ddp.username}")
    private String username;

    @Value("${ddp.password}")
    private String password;

    private String authenticationToken;

    private final Logger LOG = Logger.getLogger(AuthenticationUtil.class);

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the authenticationToken
     */
    public String getAuthenticationToken() {
        if (authenticationToken == null || authenticationToken.isEmpty()) {
            fillAuthToken();
        }
        return authenticationToken;
    }

    /**
     * @param authenticationToken the authenticationToken to set
     */
    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    /**
     * Returns formatted string with credential info.
     * @return
     */
    private Map<String, String> getUserCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("password", password);
        credentials.put("username", username);
        return credentials;
    }

    private void fillAuthToken() {
        String url = ddpBaseUrl + ddpAuthTokenEndpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Map<String, String> credentials = getUserCredentials();
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(credentials, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<AuthenticationToken> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, AuthenticationToken.class);
        if (response != null) {
            this.authenticationToken = response.getBody().getAuthToken();
            LOG.info("Authentication token: " + authenticationToken);
        }
        else {
            throw new RuntimeException("Error getting authentication token!");
        }
    }
}
