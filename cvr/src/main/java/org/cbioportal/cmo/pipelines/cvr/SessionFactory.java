/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr;

import java.time.Instant;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

public class SessionFactory {

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

    @Value("${dmp.create_session_initial_response_timeout}")
    private Integer dmpCreateSessionInitialResponseTimeout;

    @Value("${dmp.create_session_maximum_response_timeout}")
    private Integer dmpCreateSessionMaximumResponseTimeout;

    private Logger log = Logger.getLogger(SessionFactory.class);

    public SessionFactory() {
    }

    private void logCreateSessionFailure(String dmpServerName, int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error creating CVR session for server %s (after %d attempts) : %s", dmpServerName, numberOfRequestsAttempted, message));
    }

    /*
    * FULL URL: server_name/create_session/user_name/password/TYPE
    * The TYPE can be 0 or 1, 0 is de-identified, 1 is identified (for clinical information)
    * for CVR fetch for the portal, use 0
    */

    /* Gets the sessionId from CVR
     * TODO: this approach creates the session during the Bean Initialization phase (before actual application startup). It also
     *       constrains each run of the cvr pipeline to having only a single session. Sessions expire after 3 hours, and this
     *       does not allow for the possibility of beginning subsequent sessions for later exchanges with the cvr servers if the
     *       first session expires. We should consider encapsulating the management of sessions into a session manager class, so
     *       that sessions are automatiacally renewed when expiration is approaching.
    */
    private String createSession(String requestedServerName, String createSessionEndpoint, String username, String password, String dropDeadInstantString) {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        String dmpUrl = String.format("%s%s/%s/%s/0", requestedServerName, createSessionEndpoint, username, password);
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpCreateSessionInitialResponseTimeout,
                dmpCreateSessionMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                true); // on a server error response, keep trying. If we cannot create a session, the overall fetch fails.
        ResponseEntity<CVRSession> responseEntity = client.exchange(dmpUrl, HttpMethod.POST, requestEntity, null, CVRSession.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logCreateSessionFailure(requestedServerName, client.getNumberOfRequestsAttempted(), message);
            throw new RuntimeException(String.format("Error creating CVR session for server %s : %s", requestedServerName, message)); // crash on startup (during Bean Initialization phase)
        }
        return responseEntity.getBody().getSessionId();
    }

    public String createCvrSessionAndGetId(String dropDeadInstantString) {
        return createSession(dmpServerName, dmpCreateSession, dmpUserName, dmpPassword, dropDeadInstantString);
    }

    public String createGmlSessionAndGetId(String dropDeadInstantString) {
        return createSession(gmlServerName, gmlCreateSession, dmpUserName, dmpPassword, dropDeadInstantString);
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }
}
