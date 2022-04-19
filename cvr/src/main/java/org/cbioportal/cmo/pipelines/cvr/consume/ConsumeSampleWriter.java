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

package org.cbioportal.cmo.pipelines.cvr.consume;

import java.util.List;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRConsumeSample;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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

    Logger log = Logger.getLogger(ConsumeSampleWriter.class);

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

    private void consumeSample(String sampleId) {
        HttpEntity requestEntity = getRequestEntity();
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory =
            (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        requestFactory.setReadTimeout(10000);
        requestFactory.setConnectTimeout(10000);
        ResponseEntity<CVRConsumeSample> responseEntity;
        try {
            responseEntity = restTemplate.exchange(dmpConsumeUrl + sampleId, HttpMethod.GET, requestEntity, CVRConsumeSample.class);
            if (responseEntity.getBody().getaffectedRows()==0) {
                String message = "No consumption for sample " + sampleId;
                log.warn(message);
            }
            if (responseEntity.getBody().getaffectedRows()>1) {
                String message = "Multiple samples consumed (" + responseEntity.getBody().getaffectedRows() + ") for sample " + sampleId;
                log.warn(message);
            }
            if (responseEntity.getBody().getaffectedRows()==1) {
                String message = "Sample "+ sampleId +" consumed succesfully";
                log.info(message);
                // skip adding gml samples to this list since it is for smile only
                // and smile is not taking in gml sample metadata
                if (!gmlMode) {
                    cvrSampleListUtil.updateSmileSamplesToPublishList(sampleId);
                }
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Error consuming sample " + sampleId + " response body is: '" + e.getResponseBodyAsString() + "'");
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Error consuming sample " + sampleId + "(" + e + ")");
        }
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
