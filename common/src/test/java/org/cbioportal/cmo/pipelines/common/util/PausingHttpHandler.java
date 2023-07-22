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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

public class PausingHttpHandler implements HttpHandler {

    private long pausePeriodMillis;
    private String expectedResponseContentType;
    private String expectedResponseBody;
    private boolean requestCausesServerError;

    public PausingHttpHandler() {
        setPausePeriodMillis(0L);
        setExpectedResponseContentType("text/html; charset=utf-8");
        setExpectedResponseBody("<html><head></head><body>Response not configured</body></html>");
        setRequestCausesServerError(false);
    }

    public PausingHttpHandler(long pausePeriodMillis, String expectedResponseContentType, String expectedResponseBody) {
        setPausePeriodMillis(pausePeriodMillis);
        setExpectedResponseContentType(expectedResponseContentType);
        setExpectedResponseBody(expectedResponseBody);
        setRequestCausesServerError(false);
    }

    public void setPausePeriodMillis(long pausePeriodMillis) {
        this.pausePeriodMillis = pausePeriodMillis;
    }

    public void setExpectedResponseContentType(String expectedResponseContentType) {
        if (expectedResponseContentType != null) {
            this.expectedResponseContentType = expectedResponseContentType;
        }
    }

    public void setExpectedResponseBody(String expectedResponseBody) {
        if (expectedResponseBody != null) {
            this.expectedResponseBody = expectedResponseBody;
        }
    }

    public void setRequestCausesServerError(boolean requestCausesServerError) {
        this.requestCausesServerError = requestCausesServerError;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long initialTimeMillis = System.currentTimeMillis();
        long endTimeMillis = initialTimeMillis + pausePeriodMillis;
        while (true) {
            long pauseTimeMillis = endTimeMillis - System.currentTimeMillis();
            if (pauseTimeMillis <= 0) {
                break;
            }
            tryToPause(pauseTimeMillis);
        }
        int httpStatus = 200;
        if (requestCausesServerError) {
            httpStatus = 500;
        }
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("content-type", expectedResponseContentType);
        exchange.sendResponseHeaders(httpStatus, expectedResponseBody.length());
        OutputStream output = exchange.getResponseBody();
        output.write(expectedResponseBody.getBytes());
        output.close();
    }

    private void tryToPause(long pauseTimeMillis) {
        try {
            Thread.sleep(pauseTimeMillis);
        } catch (IllegalArgumentException e) {
            // don't pause on negative duration
        } catch (InterruptedException e) {
            // if interrupted we will re-enter the outer loop and re-sleep
        }
    }
}
