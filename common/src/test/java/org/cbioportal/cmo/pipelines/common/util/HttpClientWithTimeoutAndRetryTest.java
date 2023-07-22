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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.common.util;

import java.time.Instant;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes=Object.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class HttpClientWithTimeoutAndRetryTest {
    private static PausingHttpService pausingHttpService;
    private static PausingHttpHandler pausingHttpHandler;
    private static int pausingHttpServicePort;
    private static String baseUrl;

    @BeforeClass
    public static void preTestingSetup() {
        pausingHttpService = new PausingHttpService();
        pausingHttpHandler = pausingHttpService.getHandler();
        pausingHttpService.start();
        pausingHttpServicePort = pausingHttpService.getPort();
        baseUrl = String.format("http://127.0.0.1:%d/", pausingHttpServicePort);
        // warm server
        HttpEntity request = getRequestEntity();
        int initialTimeout = 100;
        int maximumTimeout = 2000;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(30000), retryOnServerError);
        ResponseEntity<String> response = client.exchange(baseUrl, HttpMethod.GET, request, null, String.class);
    }

    @AfterClass
    public static void postTestingSetup() {
        pausingHttpService.stop();
    }

    @Test
    public void testClockStartsAtInitial() throws Exception {
        // First request should succeed: server pauses for 1 millisecond, client has initial wait of 100 milliseconds
        pausingHttpHandler.setPausePeriodMillis(1);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 100;
        int maximumTimeout = 2000;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(30000), retryOnServerError);
        ResponseEntity<String> response = client.exchange(baseUrl, HttpMethod.GET, request, null, String.class);
        Assert.assertNotNull("received null response", response);
        Assert.assertTrue("received unsuccessful response", response.getStatusCode().is2xxSuccessful());
        Assert.assertTrue("multiple requests made", client.getNumberOfRequestsAttempted() == 1);
        Assert.assertTrue("timeout progressed", client.getLastTimeoutUsed() == initialTimeout);
        Assert.assertFalse("reached drop dead instant", client.getReachedDropDeadInstant());
    }

    @Test
    public void testTimeoutLeadsToProgression() throws Exception {
        // First request fails, but subsequent succeeds: server pauses for 10 ms, client has initial wait of 8 ms
        pausingHttpHandler.setPausePeriodMillis(10);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 8;
        int maximumTimeout = 2000;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(5000), retryOnServerError);
        ResponseEntity<String> response = client.exchange(baseUrl, HttpMethod.GET, request, null, String.class);
        Assert.assertNotNull("received null response", response);
        Assert.assertTrue("received unsuccessful response", response.getStatusCode().is2xxSuccessful());
        Assert.assertTrue("only single request made", client.getNumberOfRequestsAttempted() > 1);
        Assert.assertTrue("timeout did not progress", client.getLastTimeoutUsed() > initialTimeout);
        Assert.assertFalse("reached drop dead instant", client.getReachedDropDeadInstant());
    }

    @Test
    public void testTimeoutConstrainedByMaximum() throws Exception {
        // Requests all fail until drop_dead_instant: server pauses for 100 ms, client has initial wait of 8 ms
        pausingHttpHandler.setPausePeriodMillis(100);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 8;
        int maximumTimeout = 9;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(256), retryOnServerError);
        ResponseEntity<String> response = client.exchange(baseUrl, HttpMethod.GET, request, null, String.class);
        Assert.assertNull("received null response", response);
        Assert.assertTrue("only single request made", client.getNumberOfRequestsAttempted() > 1);
        Assert.assertTrue("timeout exceeded maximum", client.getLastTimeoutUsed() <= maximumTimeout);
        Assert.assertTrue("did not reach drop dead instant", client.getReachedDropDeadInstant());
    }

    @Test
    public void testNoConnectionLeadsToFailure() throws Exception {
        // Requests made to non open port
        pausingHttpHandler.setPausePeriodMillis(1);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 56;
        int maximumTimeout = 256;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(256), retryOnServerError);
        ResponseEntity<String> response = client.exchange("http://127.0.0.1:1/", HttpMethod.GET, request, null, String.class);
        Assert.assertNull("received null response", response);
        Assert.assertTrue("only single request made", client.getNumberOfRequestsAttempted() > 1);
        Assert.assertTrue("timeout exceeded maximum", client.getLastTimeoutUsed() <= maximumTimeout);
        Assert.assertTrue("did not reach drop dead instant", client.getReachedDropDeadInstant());
    }

    @Test
    public void testRetryOnServerErrorWorks() throws Exception {
        // Requests made to failing service
        pausingHttpHandler.setPausePeriodMillis(1);
        pausingHttpHandler.setExpectedResponseContentType("application/json");
        pausingHttpHandler.setRequestCausesServerError(true);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 56;
        int maximumTimeout = 256;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(256), retryOnServerError);
        ResponseEntity<Object> response = client.exchange(baseUrl, HttpMethod.GET, request, null, Object.class);
        pausingHttpHandler.setRequestCausesServerError(false);
        Assert.assertNull("received null response", response);
        Assert.assertTrue("only single request made", client.getNumberOfRequestsAttempted() > 1);
        Assert.assertTrue("did not reach drop dead instant", client.getReachedDropDeadInstant());
    }

    @Test
    public void testNoRetryOnServerErrorWorks() throws Exception {
        // Requests made to failing service
        pausingHttpHandler.setPausePeriodMillis(1);
        pausingHttpHandler.setExpectedResponseContentType("application/json");
        pausingHttpHandler.setRequestCausesServerError(true);
        HttpEntity request = getRequestEntity();
        int initialTimeout = 56;
        int maximumTimeout = 256;
        boolean retryOnServerError = false;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(256), retryOnServerError);
        ResponseEntity<Object> response = client.exchange(baseUrl, HttpMethod.GET, request, null, Object.class);
        pausingHttpHandler.setRequestCausesServerError(false);
        Assert.assertNull("received null response", response);
        Assert.assertTrue("multiple requests made", client.getNumberOfRequestsAttempted() == 1);
        Assert.assertFalse("reached drop dead instant", client.getReachedDropDeadInstant());
    }

    private static HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
