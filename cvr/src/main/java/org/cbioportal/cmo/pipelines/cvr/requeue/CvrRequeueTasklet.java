/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
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

import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRRequeueRecord;

import java.util.*;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    private CvrSampleListUtil cvrSampleListUtil;

    private List<CVRRequeueRecord> failedRequeue = new ArrayList();

    Logger log = Logger.getLogger(CvrRequeueTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        if (cvrSampleListUtil.getDmpSamplesNotInPortal().isEmpty()) {
            String message = "No samples to requeue for study: " + studyId;
            if (testingMode) {
                message = "[TESTING MODE]: " + message;
            }
            log.info(message);
        }
        else {
            if (testingMode) {
                log.info("[TESTING MODE]: samples will not be requeued (" + String.valueOf(cvrSampleListUtil.getDmpSamplesNotInPortal().size()) + " samples)");
            }
            else {
                this.failedRequeue = requeueSamples();
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
            CVRRequeueRecord requeuedSample = requeue(sampleId);
            if (requeuedSample.getResult() != 1) {
                failedRequeueList.add(requeuedSample);
            }
        }

        return failedRequeueList;
    }

    private CVRRequeueRecord requeue(String sampleId) {
        log.info("Requeueing '" + sampleId + "'");
        String dmpRequeueUrl = dmpServerName + dmpRequeue + "/" + sessionId + "/" + sampleId;
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        ResponseEntity<CVRRequeueRecord> responseEntity;
        try {
            responseEntity = restTemplate.exchange(dmpRequeueUrl, HttpMethod.GET, requestEntity, CVRRequeueRecord.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Error requeueing sample " + sampleId + " response body is: '" + e.getResponseBodyAsString() + "'");
            return new CVRRequeueRecord("This record was generated by " + CvrRequeueTasklet.class,
                "Error requeuing sample (request failed to return result)",
                0,
                sampleId);
        } catch (org.springframework.web.client.RestClientException e) {
            String message = "Error getting requeuing sample: " + sampleId;
            log.error(message);
            return new CVRRequeueRecord("This record was generated by " + CvrRequeueTasklet.class,
                "Error requeuing sample (request failed to return result)",
                0,
                sampleId);
        }
        return responseEntity.getBody();
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
