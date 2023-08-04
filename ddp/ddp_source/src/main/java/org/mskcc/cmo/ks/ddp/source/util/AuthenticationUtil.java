/*
 * Copyright (c) 2018, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

import java.io.IOException;
import java.lang.Thread;
import java.util.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.StatusLine;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.mskcc.cmo.ks.ddp.source.exception.InvalidAuthenticationException;
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
    private final int COOKIE_ATTEMPT_WAIT_TIME = 10000; // milliseconds
    private final int COOKIE_ATTEMPT_COUNT_LIMIT = 27;
    private final int COOKIE_EXPIRATION_TIME_LIMIT = 30; // minutes

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
    public synchronized Cookie getAuthenticationCookie() {
        Calendar calendar = Calendar.getInstance(); // right now
        calendar.add(Calendar.MINUTE, COOKIE_EXPIRATION_TIME_LIMIT);
        // if we don't have a cookie, or it will expire too quickly
        if (authenticationCookie == null || authenticationCookie.isExpired(calendar.getTime())) {
            for (int count = 0; count < COOKIE_ATTEMPT_COUNT_LIMIT; count++) {
                try {
                    fillAuthCookie();
                    break;
                } catch (Exception e) {
                    // exception thrown from authentication endpoint
                    // sleep before trying again (in case DDP is temporarily down)
                    LOG.warn("Failed to generate authentication cookie ... trying again in " + (COOKIE_ATTEMPT_WAIT_TIME / 1000) + " seconds");
                    try {
                        Thread.sleep(COOKIE_ATTEMPT_WAIT_TIME);
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
        RequestConfig customRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        CloseableHttpClient customClient = HttpClients.custom().setDefaultRequestConfig(customRequestConfig).build();
        HttpPost postRequest = new HttpPost(url);
        StringEntity input = new StringEntity(new JSONObject(getUserCredentials()).toString());
        input.setContentType("application/json");
        postRequest.setEntity(input);
        CloseableHttpResponse response = customClient.execute(postRequest, context);

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
        customClient.close();
        response.close();
    }
}
