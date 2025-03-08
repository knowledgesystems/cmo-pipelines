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

package org.cbioportal.cmo.pipelines.cvr.variants;

import java.time.Instant;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

/**
 *
 * @author jake
 */
public class GMLVariantsReader implements ItemStreamReader<GMLVariant> {

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("${dmp.gml_server_name}")
    private String dmpServerName;

    @Value("${dmp.tokens.retrieve_gml_variants}")
    private String dmpRetreiveVariants;

    @Value("${dmp.retrieve_variants_initial_response_timeout}")
    private Integer dmpRetrieveVariantsInitialResponseTimeout;

    @Value("${dmp.retrieve_variants_maximum_response_timeout}")
    private Integer dmpRetrieveVariantsMaximumResponseTimeout;

    @Value("#{jobParameters[dropDeadInstantString]}")
    private String dropDeadInstantString;

    private final Deque<GMLVariant> gmlVariants = new LinkedList<>();

    private Logger log = Logger.getLogger(GMLVariantsReader.class);

    private void logGetGmlSamplesFailure(int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error getting GML samples (after %d attempts) %s", numberOfRequestsAttempted, message));
    }

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        String dmpUrl = String.format("%s%s/%s/0", dmpServerName, dmpRetreiveVariants, sessionId);
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpRetrieveVariantsInitialResponseTimeout,
                dmpRetrieveVariantsMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                true); // on a server error response, keep trying. If we cannot get the variants list, the overall fetch fails.
        ResponseEntity<GMLVariant> responseEntity = client.exchange(dmpUrl, HttpMethod.GET, requestEntity, null, GMLVariant.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logGetGmlSamplesFailure(client.getNumberOfRequestsAttempted(), message);
            throw new RuntimeException(String.format("Error getting GML samples : %s", message)); // crash
        }
        gmlVariants.add(responseEntity.getBody());
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public GMLVariant read() throws Exception {
        if (!gmlVariants.isEmpty()) {
            return gmlVariants.pollFirst();
        }
        return null;
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<LinkedMultiValueMap<String, Object>>(headers);
    }
}
