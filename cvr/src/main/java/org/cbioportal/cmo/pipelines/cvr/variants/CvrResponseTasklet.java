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

package org.cbioportal.cmo.pipelines.cvr.variants;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cbioportal.cmo.pipelines.cvr.model.CvrResponse;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;

import java.io.*;
import java.util.*;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.CVRResult;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSegData;
import org.cbioportal.cmo.pipelines.util.CVRUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ochoaa
 */
public class CvrResponseTasklet implements Tasklet {

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Value("#{jobParameters[extractTransformJsonMode]}")
    private Boolean extractTransformJsonMode;

    @Value("#{jobParameters[extractTransformJsonFile]}")
    private String extractTransformJsonFile;

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Autowired
    private CVRUtils cvrUtils;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Resource(name="retrieveVariantTokensMap")
    private Map<String, String> retrieveVariantTokensMap;

    private static Logger log = Logger.getLogger(CvrResponseTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {

        // save the CVR response in the sample util and add the sample count to the execution context
        // for the CVR response job execution decider
        CvrResponse cvrResponse = null;
        if (extractTransformJsonMode) {
            cvrResponse = loadCvrResponseFromFile();
        }
        else {
            cvrResponse = getCvrResponseFromServer();
        }
        cvrSampleListUtil.setCvrResponse(cvrResponse);
        cc.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("sampleCount", cvrResponse.getSampleCount());
        return RepeatStatus.FINISHED;
    }

    private CvrResponse loadCvrResponseFromFile() {
        CvrResponse cvrResponse = null;
        File jsonFile = new File(extractTransformJsonFile);
        try {
            ObjectMapper mapper = new ObjectMapper();
            cvrResponse = mapper.readValue(jsonFile, CvrResponse.class);
        } catch (IOException ex) {
            if (jsonFile.exists()) {
                log.error("Error loading CVR response from: " + jsonFile.getName() + " - check that the schema is correct");
            }
            else {
                log.error("No such file: " + jsonFile.getName());
            }
            throw new ItemStreamException(ex);
        }
        // fix patient ids for results in CvrResponse (sometimes patient ids for all samples are the same invalid id P-0000000)
        Map<String, CVRResult> results = cvrResponse.getResults();
        Iterator it = results.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String sampleId = cvrUtils.convertWhitespace((String)pair.getKey());
            CVRResult result = (CVRResult)pair.getValue();
            String patientId = cvrUtils.convertWhitespace(result.getMetaData().getDmpPatientId());
            if (CVRUtilities.INVALID_PATIENT_IDS.contains(patientId)) {
                result.getMetaData().setDmpPatientId(sampleId);
            }
            results.put(sampleId, result);
        }
        cvrResponse.setResults(results);
        return cvrResponse;
    }

    private CvrResponse getCvrResponseFromServer() {
        // get retrieve variants token by study id
        String dmpUrl = dmpServerName + retrieveVariantTokensMap.get(studyId) + "/" + sessionId + "/0";
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        ResponseEntity<CvrResponse> responseEntity = restTemplate.exchange(dmpUrl, HttpMethod.GET, requestEntity, CvrResponse.class);
        return responseEntity.getBody();
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
