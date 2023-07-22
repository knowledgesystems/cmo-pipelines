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

package org.cbioportal.cmo.pipelines.cvr;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@ContextConfiguration(classes=CvrTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RestTemplateTimeoutTest {

    public static final String PAUSE_PERIOD_HEADER_NAME = "pauseperiod";

    public static class PausingHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long initialTimeMillis = System.currentTimeMillis();
            long pauseDuration = 0L;
            String pauseDurationString = null;
            List<String> pauseDurationStrings = exchange.getRequestHeaders().get(PAUSE_PERIOD_HEADER_NAME);
            if (pauseDurationStrings.size() > 0) {
                pauseDurationString = pauseDurationStrings.get(0);
            }
            if (pauseDurationString != null) {
                try {
                    pauseDuration = Long.valueOf(pauseDurationString);
                } catch (NumberFormatException e) {
                    // duration unset with improper header
                }
                long endTimeMillis = initialTimeMillis + pauseDuration;
                while (true) {
                    long pauseTimeMillis = endTimeMillis - System.currentTimeMillis();
                    try {
                        Thread.sleep(pauseTimeMillis);
                    } catch (IllegalArgumentException e) {
                        // don't pause on negative duration
                    } catch (InterruptedException e) {
                        // if interrupted we re-enter the loop and re-sleep
                    }
                    if (pauseTimeMillis <= 0) {
                        break;
                    }
                }
            }
            String response = "<http><head></head><body>Response</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes());
            output.close();
        }
    }

    public static class PausingWebService {
        public static final int WEB_SERVER_UNAVAILABLE_PORT = -1;
        HttpServer server;

        public boolean isAvailable() {
            return server != null;
        }

        public int getPort() {
            if (isAvailable()) {
                return server.getAddress().getPort();
            } else {
                return WEB_SERVER_UNAVAILABLE_PORT;
            }
        }

        public void start() {
            InetSocketAddress socket = new InetSocketAddress(0); // ephemeral port
            try {
                server = HttpServer.create(socket, 0); // no backlog
                server.createContext("/", new PausingHandler());
                server.setExecutor(null);
                server.start();
            } catch (IOException e) {
                throw new RuntimeException("Could not start http service on any port\n", e);
            }
        }

        public void stop() {
            server.stop(1);
            server = null;
        }

        public PausingWebService() {
             server = null;
        }

    }

    static PausingWebService pausingWebService = null;

    @BeforeClass
    public static void preTestingSetup() {
        pausingWebService = new PausingWebService();
        pausingWebService.start();
    }

    @AfterClass
    public static void postTestingSetup() {
        pausingWebService.stop();
        pausingWebService = null;
    }

    public ResponseEntity<String> testRequestCompletesWithPauseAndTimeout(int pauseMillis, int timeoutMillis) {
        Assert.assertTrue(pausingWebService.isAvailable());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.add(PAUSE_PERIOD_HEADER_NAME, String.format("%d", pauseMillis));
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(null, headers);
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        requestFactory.setReadTimeout(timeoutMillis);
        requestFactory.setConnectTimeout(timeoutMillis);
        try {
            String pausingWebServiceUrl = String.format("http://127.0.0.1:%d/", pausingWebService.getPort());
            return restTemplate.exchange(pausingWebServiceUrl, HttpMethod.GET, requestEntity, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            return new ResponseEntity(HttpStatus.GATEWAY_TIMEOUT);
        } catch (org.springframework.web.client.RestClientException e) {
            return new ResponseEntity(HttpStatus.GATEWAY_TIMEOUT);
        }
    }

    @Test
    public void testRequestCompletesBeforeTimeout() throws Exception {
        ResponseEntity<String> responseEntity = testRequestCompletesWithPauseAndTimeout(50, 500); // server will pause for 50 milliseconds, timeout set to 500
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testRequestFailsAfterTimeout() throws Exception {
        ResponseEntity<String> responseEntity = testRequestCompletesWithPauseAndTimeout(500, 50); // server will pause for 500 milliseconds, timeout set to 50
        Assert.assertTrue(responseEntity.getStatusCode().is5xxServerError());
    }

}
