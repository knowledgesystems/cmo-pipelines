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

package org.cbioportal.cmo.pipelines.cvr.requeue;

import java.time.Instant;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRRequeueRecord;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

/**
 *
 * @author Manda Wilson, ochoaa
 */
public class CvrRequeueTasklet implements Tasklet {

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.tokens.requeue.impact}")
    private String dmpRequeue;

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Value("#{jobParameters[testingMode]}")
    private boolean testingMode;

    @Value("${dmp.requeue_sample_initial_response_timeout}")
    private Integer dmpRequeueSampleInitialResponseTimeout;

    @Value("${dmp.requeue_sample_maximum_response_timeout}")
    private Integer dmpRequeueSampleMaximumResponseTimeout;

    @Value("#{jobParameters[dropDeadInstantString]}")
    private String dropDeadInstantString;

    @Autowired
    private CvrSampleListUtil cvrSampleListUtil;

    private Logger log = Logger.getLogger(CvrRequeueTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        List<CVRRequeueRecord> failedRequeue = new ArrayList();
        if (cvrSampleListUtil.getDmpSamplesNotInPortal().isEmpty()) {
            String message = String.format("No samples to requeue for study: %s", studyId);
            if (testingMode) {
                message = String.format("[TESTING MODE]: %s", message);
            }
            log.info(message);
        } else {
            if (testingMode) {
                log.info(String.format("[TESTING MODE]: samples will not be requeued (%d samples)", cvrSampleListUtil.getDmpSamplesNotInPortal().size()));
            } else {
                failedRequeue = requeueSamples();
            }
        }
        // add failed requeue samples to execution context for listener
        cc.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("failedToRequeueSamples", failedRequeue);
        return RepeatStatus.FINISHED;
    }

    private List<CVRRequeueRecord> requeueSamples() {
        List<CVRRequeueRecord> failedRequeueList = new ArrayList();
        // if sample fails to requeue then add to list
        for (String sampleId : cvrSampleListUtil.getDmpSamplesNotInPortal()) {
            CVRRequeueRecord requeuedSample = requeueSample(sampleId);
            if (requeuedSample.getResult() != 1) {
                failedRequeueList.add(requeuedSample);
            }
        }
        return failedRequeueList;
    }

    private void logRequeueSampleFailure(String sampleId, int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error requeueing sample %s (after %d attempts) %s", sampleId, numberOfRequestsAttempted, message));
    }

    private CVRRequeueRecord makeFailureCVRRequeueRecord(String sampleId) {
        return new CVRRequeueRecord("This record was generated by " + CvrRequeueTasklet.class,
                "Error requeuing sample (request failed to return result)",
                0,
                sampleId);
    }

    private CVRRequeueRecord requeueSample(String sampleId) {
        log.info(String.format("Requeueing '%s'", sampleId));
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpRequeueSampleInitialResponseTimeout,
                dmpRequeueSampleMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                false); // on a server error response, stop trying and fail/log (but continue on to other samples)
        String dmpRequeueUrl = String.format("%s%s/%s/%s", dmpServerName, dmpRequeue, sessionId, sampleId);
        ResponseEntity<CVRRequeueRecord> responseEntity = client.exchange(dmpRequeueUrl, HttpMethod.GET, requestEntity, null, CVRRequeueRecord.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logRequeueSampleFailure(sampleId, client.getNumberOfRequestsAttempted(), message);
            return makeFailureCVRRequeueRecord(sampleId); // a failure to requeue does not prevent continuation of the run
        }
        return responseEntity.getBody();
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<LinkedMultiValueMap<String, Object>>(headers);
    }

}
