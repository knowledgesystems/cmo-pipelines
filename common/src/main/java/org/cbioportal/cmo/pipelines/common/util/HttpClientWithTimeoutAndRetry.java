/*
 * Copyright (c) 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.common.util;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.Properties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class HttpClientWithTimeoutAndRetry {
    private int initialTimeout;
    private int maximumTimeout;
    private Instant dropDeadInstant;
    private boolean retryOnErroneousServerResponse;
    private int numberOfRequestsAttempted;
    private int lastTimeoutUsed;
    private boolean reachedDropDeadInstant;
    private RestClientException lastRestClientException;
    private String lastResponseBodyStringAfterException;
    private Logger log = Logger.getLogger(HttpClientWithTimeoutAndRetry.class);

    public HttpClientWithTimeoutAndRetry() {
        this.initialTimeout = 1000; // default to 1 second initial timeout
        this.maximumTimeout = 60000; // default to 1 minute maximum timeout
        this.dropDeadInstant = createDefaultDropDeadInstant(this.maximumTimeout);
        this.retryOnErroneousServerResponse = false;
        this.numberOfRequestsAttempted = 0;
        this.lastTimeoutUsed = 0;
        this.reachedDropDeadInstant = false;
        this.lastRestClientException = null;
    }

    public HttpClientWithTimeoutAndRetry(int initialTimeout, int maximumTimeout, Instant dropDeadInstant, boolean retryOnErroneousServerResponse) {
        this.initialTimeout = initialTimeout;
        this.maximumTimeout = maximumTimeout;
        if (dropDeadInstant == null) {
            this.dropDeadInstant = createDefaultDropDeadInstant(this.maximumTimeout);
        } else {
            this.dropDeadInstant = dropDeadInstant;
        }
        this.retryOnErroneousServerResponse = retryOnErroneousServerResponse;
        this.numberOfRequestsAttempted = 0;
        this.lastTimeoutUsed = 0;
        this.reachedDropDeadInstant = false;
        this.lastRestClientException = null;
    }

    public int getNumberOfRequestsAttempted() {
        return numberOfRequestsAttempted;
    }

    public int getLastTimeoutUsed() {
        return lastTimeoutUsed;
    }

    public boolean getReachedDropDeadInstant() {
        return reachedDropDeadInstant;
    }

    public RestClientException getLastRestClientException() {
        return lastRestClientException;
    }

    public String getLastResponseBodyStringAfterException() {
        return lastResponseBodyStringAfterException;
    }

    public boolean exceptionCausedByTimeout(RestClientException e) {
        return e.getRootCause() instanceof SocketTimeoutException && e.getRootCause().getMessage().toLowerCase().contains("timed out");
    }
    /* the exchange method takes similar arguments to HttpTemplate.exchange and it sets the intial timeout from data member initialTimeout.
     * if the timeout is reached (either for forming a connection or waiting to receive a response) a RestClientException is raised. If
     * the exception is due to timeout, the timeout is adjusted according to the HttpRequestTimeoutProgression object, and the request
     * is reattempted. A final DropDeadInstant is also monitored and if it arrives a null response is returned.
    */
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity requestEntity, Map<String,?> uriVariables, Class<T> returnType) {
        HttpRequestTimeoutProgression timeoutProgression = new HttpRequestTimeoutProgression(initialTimeout, maximumTimeout, dropDeadInstant);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        if (uriVariables == null) {
            uriVariables = new HashMap<String, String>();
        }
        numberOfRequestsAttempted = 0;
        lastTimeoutUsed = 0;
        reachedDropDeadInstant = false;
        lastRestClientException = null;
        lastResponseBodyStringAfterException = null;
        while (Instant.now().isBefore(dropDeadInstant)) {
            try {
                lastRestClientException = null; // reset before each request
                lastResponseBodyStringAfterException = null; // reset before each request
                numberOfRequestsAttempted = numberOfRequestsAttempted + 1;
                lastTimeoutUsed = timeoutProgression.getNextTimeoutForRequest();
                requestFactory.setConnectTimeout(lastTimeoutUsed);
                requestFactory.setReadTimeout(lastTimeoutUsed);
                ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, returnType, uriVariables);
                if (response.getStatusCode().is5xxServerError()) {
                    pauseForMilliseconds(lastTimeoutUsed);
                    continue;
                }
                return response;
            } catch (RestClientResponseException e) {
                log.error("RestClientResponseException: " + e.getMessage());
                // these exceptions typically occur when the response from the server is not properly deserialized/objectMapped.
                // Perhaps the server has responded with a general message about server problems, or about an invalid request (as html when json was expected)
                lastRestClientException = e;
                lastResponseBodyStringAfterException = e.getResponseBodyAsString();
                if (!retryOnErroneousServerResponse) {
                    return null; // fail now : the exception and response body have been captured in instance variables
                }
                pauseForMilliseconds(lastTimeoutUsed);
            } catch (RestClientException e) {
                log.error("RestClientException: " + e.getMessage());
                // if the server responds with something which does not match the expected model, consider that a "server error"
                // this would happen if the server sends a 200 "OK" http status but contains a message such as this json:
                // { "error": "Error occurred while processing your request. get_seg_data cant be processed..." }
                // or if it sends an html page response when we expect a json object.
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Could not extract response") && !retryOnErroneousServerResponse) {
                    return null; // fail now : the exception and response body have been captured in instance variables
                }
                // these exceptions are either timeouts or other low level exceptions. Continue to attempt the request.
                lastRestClientException = e;
                if (!exceptionCausedByTimeout(e)) {
                    // some other low level issue (IOException usually - maybe NoRouteToHost if interface is down?)
                    log.debug(String.format("RestClientException ocurred during retry loop : %s", e.toString()));
                }
                pauseForMilliseconds(lastTimeoutUsed);
            }
        }
        reachedDropDeadInstant = true;
        return null;
    }

    private void pauseForMilliseconds(int period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
        }
    }

    private Instant createDefaultDropDeadInstant(int maximumTimeout) {
        return Instant.now().plusMillis(4 * maximumTimeout);
    }

}
