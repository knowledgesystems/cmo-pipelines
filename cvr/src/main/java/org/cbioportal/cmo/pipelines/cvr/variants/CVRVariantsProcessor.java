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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

/**
 *
 * @author heinsz
 */
public class CVRVariantsProcessor implements ItemProcessor<CvrResponse, String> {

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.tokens.retrieve_segment_data}")
    private String dmpRetrieveSegmentData;

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("#{jobParameters[skipSeg]}")
    private boolean skipSeg;

    @Value("${dmp.get_segments_initial_response_timeout}")
    private Integer dmpGetSegmentsInitialResponseTimeout;

    @Value("${dmp.get_segments_maximum_response_timeout}")
    private Integer dmpGetSegmentsMaximumResponseTimeout;

    @Value("#{jobParameters[dropDeadInstantString]}")
    private String dropDeadInstantString;

    @Autowired
    private CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private Logger log = Logger.getLogger(CVRVariantsProcessor.class);

    // Need to call get_seg_data against the CVR webservice for every sample, then merge the results together (CVRMergedResult)
    // All of these get put into the cvrData object, which contains everything and is what get sent to the writer
    @Override
    public String process(CvrResponse i) throws Exception {
        Map<String, CVRResult> results = i.getResults();
        CVRData cvrData = new CVRData(i.getSampleCount(), i.getDisclaimer(), new ArrayList<CVRMergedResult>());
        ObjectMapper mapper = new ObjectMapper();
        Iterator it = results.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String sampleId = cvrUtilities.convertWhitespace((String)pair.getKey());
            cvrSampleListUtil.addNewDmpSample(sampleId);

            CVRResult result = (CVRResult)pair.getValue();
            CVRSegData segData = new CVRSegData();
            if (!skipSeg) {
                segData = getSegmentData(sampleId);
                // TODO : we could clean this segData object of whitespace (by adding a new cvrUtil method)
            }
            if (segData != null) {
                CVRMergedResult mergedResult = new CVRMergedResult(result, segData);
                cvrData.addResult(mergedResult);
            }
        }
        // All the merged data is sent as a json string to the writer at this point
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cvrData);
    }

    private void logGetSegDataFailure(String sampleId, int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error getting seg data for sample %s (after %d attempts) %s", sampleId, numberOfRequestsAttempted, message));
    }

    private CVRSegData getSegmentData(String sampleId) {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        String dmpSegmentUrl = String.format("%s%s/%s/%s", dmpServerName, dmpRetrieveSegmentData, sessionId, sampleId);
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpGetSegmentsInitialResponseTimeout,
                dmpGetSegmentsMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                false); // on a server error response, stop trying and move on. We accept samples even if they are missing their seg data
        ResponseEntity<CVRSegData> responseEntity = client.exchange(dmpSegmentUrl, HttpMethod.GET, requestEntity, null, CVRSegData.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logGetSegDataFailure(sampleId, client.getNumberOfRequestsAttempted(), message);
            //TODO: consider crashing if seg data cannot be retrieved for a sample -- otherwise we import the sample without seg data and will not recover
            //      throw new RuntimeException(String.format("Error getting seg data for sample %s : %s", sampleId, message)); // crash
            return new CVRSegData();
        }
        return responseEntity.getBody();
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<LinkedMultiValueMap<String, Object>>(headers);
    }

}
