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

import org.mskcc.cmo.ks.ddp.source.exception.InvalidAuthenticationException;

import java.io.IOException;
import java.lang.Thread;
import java.util.*;
import org.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.StatusLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
@PropertySource("classpath:application-secure.properties")
public class AuthenticationUtil {

    @Value("${ddp.base_url}")
    private String ddpBaseUrl;

    @Value("${ddp.authcookie.endpoint}")
    private String ddpAuthCookieEndpoint;

    @Value("${ddp.username}")
    private String username;

    @Value("${ddp.password}")
    private String password;

    private Cookie authenticationCookie;

    private final Logger LOG = Logger.getLogger(AuthenticationUtil.class);

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the authenticationCookie
     * attempts max 3 times to generate an authentication cookie
     * failure throws an InvalidAuthenticationException that is handled differently
     */
    public Cookie getAuthenticationCookie() {
        Calendar calendar = Calendar.getInstance(); // right now
        calendar.add(Calendar.MINUTE, 30);
        // if we don't have a cookie, or it will have expired in 30 minutes
        if (authenticationCookie == null || authenticationCookie.isExpired(calendar.getTime())) {
            for (int count = 0; count < 3; count++) {
                try {
                    fillAuthCookie();
                    break;
                } catch (Exception e) {
                    // exception thrown from authentication endpoint
                    // sleep 1.5 minutes (in case DDP is temporarily down) before trying again
                    LOG.warn("Failed to generate authentication cookie ... trying again in a minute");
                    try {
                        Thread.sleep(90000);
                    } catch (InterruptedException interruptException) {
                        LOG.warn("InterruptedException thrown");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        if (authenticationCookie == null || authenticationCookie.isExpired(calendar.getTime())) {
            LOG.error("Failed to generate authentication cookie (multiple tries attempted)");
            throw new InvalidAuthenticationException("Failed to generate authentication cookie (multiple tries attempted)");
        }
        return authenticationCookie;
    }

    /**
     * @param authenticationCookie the authenticationCookie to set
     */
    public void setAuthenticationCookie(Cookie authenticationCookie) {
        this.authenticationCookie = authenticationCookie;
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

    private void fillAuthCookie() throws IOException {
        String url = ddpBaseUrl + ddpAuthCookieEndpoint;
        HttpClientContext context = HttpClientContext.create();
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost postRequest = new HttpPost(url);
        StringEntity input = new StringEntity(new JSONObject(getUserCredentials()).toString());
        input.setContentType("application/json");
        postRequest.setEntity(input);
        CloseableHttpResponse response = client.execute(postRequest, context);

        StatusLine statusLine = response.getStatusLine(); 
        if (statusLine.getStatusCode() == HttpStatus.OK.value()) {
            // get the cookie
            List<Cookie> cookies = context.getCookieStore().getCookies();
            for (Cookie cookie : cookies) {
                LOG.debug("Cookie name: '" + cookie.getName() + "' value: '" + cookie.getValue() + "'");
                if (cookie.getName().equals("session")) {
                    this.authenticationCookie = cookie;
                    break; // found the cookie
                }
            }
        } else {
            LOG.error("Response status: '" + statusLine + "'");
        }

        // close stuff
        client.close();
        response.close();
    }
}
