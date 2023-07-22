/*
 * Copyright (c) 2017, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.consume;

import java.time.Instant;
import java.util.List;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRConsumeSample;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

/**
 *
 * @author ochoaa
 */
public class ConsumeSampleWriter implements ItemStreamWriter<String> {

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.gml_server_name}")
    private String dmpGmlServerName;

    @Value("${dmp.tokens.consume_sample}")
    private String dmpConsumeSample;

    @Value("${dmp.tokens.consume_gml_sample}")
    private String dmpConsumeGmlSample;

    @Value("${dmp.consume_sample_initial_response_timeout}")
    private Integer dmpConsumeInitialResponseTimeout;

    @Value("${dmp.consume_sample_maximum_response_timeout}")
    private Integer dmpConsumeMaximumResponseTimeout;

    @Value("#{jobParameters[dropDeadInstantString]}")
    private String dropDeadInstantString;

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("#{jobParameters[testingMode]}")
    private boolean testingMode;

    @Value("#{jobParameters[gmlMode]}")
    private Boolean gmlMode;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private String dmpConsumeUrl;

    private Logger log = Logger.getLogger(ConsumeSampleWriter.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // determine which dmp server url to use based on the file basename
        if (gmlMode) {
            this.dmpConsumeUrl = dmpGmlServerName + dmpConsumeGmlSample + "/" + sessionId + "/";
        }
        else {
            this.dmpConsumeUrl = dmpServerName + dmpConsumeSample + "/" + sessionId + "/";
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public void write(List<? extends String> sampleIdList) throws Exception {
        for (String sampleId : sampleIdList) {
            if (testingMode) {
                log.info("[TESTING MODE]: sample id will not be consumed: " + sampleId);
            }
            else {
                consumeSample(sampleId);
            }
        }
    }

    private void logConsumeSampleFailure(String sampleId, int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error consuming sample %s (after %d attempts) %s", sampleId, numberOfRequestsAttempted, message));
    }

    private void consumeSample(String sampleId) {
        HttpEntity<LinkedMultiValueMap<String,Object>> requestEntity = getRequestEntity();
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpConsumeInitialResponseTimeout,
                dmpConsumeMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                false); // on a server error response, stop trying and fail/log (but continue on to other samples)
        ResponseEntity<CVRConsumeSample> responseEntity = client.exchange(dmpConsumeUrl + sampleId, HttpMethod.GET, requestEntity, null, CVRConsumeSample.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logConsumeSampleFailure(sampleId, client.getNumberOfRequestsAttempted(), message);
            return; // a failure to consume does not prevent continuation of the run
        }
        if (responseEntity.getBody().getaffectedRows()==0) {
            String message = String.format("No consumption for sample %s", sampleId);
            log.warn(message);
        }
        if (responseEntity.getBody().getaffectedRows()>1) {
            String message = String.format("Multiple samples consumed (%d) for sample %s", responseEntity.getBody().getaffectedRows(), sampleId);
            log.warn(message);
        }
        if (responseEntity.getBody().getaffectedRows()==1) {
            String message = String.format("Sample %s consumed successfully", sampleId);
            log.info(message);
            // skip adding gml samples to this list since it is for smile only
            // and smile is not taking in gml sample metadata
            if (!gmlMode) {
                cvrSampleListUtil.updateSmileSamplesToPublishList(sampleId);
            }
        }
    }

    private HttpEntity<LinkedMultiValueMap<String,Object>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<LinkedMultiValueMap<String,Object>>(headers);
    }

}
