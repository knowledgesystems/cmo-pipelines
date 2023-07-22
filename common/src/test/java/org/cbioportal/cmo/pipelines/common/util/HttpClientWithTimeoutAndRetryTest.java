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

@ContextConfiguration(classes=UtilTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class HttpClientWithTimeoutAndRetryTest {

    @Test
    public void testClockStartsAtInitial() throws Exception {
        HttpEntity request = getRequestEntity();
        int initialTimeout = 1000;
        int maximumTimeout = 10000;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(30000), true);

        ResponseEntity<String> response = client.exchange("https://ibm.com/", HttpMethod.GET, request, null, String.class);
        //TODO: instead of querying actual web server, open local port and have web service running locally : see cvr/src/test/java/org/cbioportal/cmo/pipelines/cvr/RestTemplateTimeoutTest.java
        Assert.assertNotNull("received null response", response);
        Assert.assertTrue("received unsuccessful response", response.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testTimeoutLeadsToProgression() throws Exception {
        HttpEntity request = getRequestEntity();
        int initialTimeout = 10;
        int maximumTimeout = 10000;
        boolean retryOnServerError = true;
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(initialTimeout, maximumTimeout, Instant.now().plusMillis(5000), true);

        ResponseEntity<String> response = client.exchange("https://ibm.com/", HttpMethod.GET, request, null, String.class);
        //TODO: instead of querying actual web server, open local port and have web service running locally : see cvr/src/test/java/org/cbioportal/cmo/pipelines/cvr/RestTemplateTimeoutTest.java
        Assert.assertNotNull("received null response", response);
        Assert.assertTrue("received unsuccessful response", response.getStatusCode().is2xxSuccessful());
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
