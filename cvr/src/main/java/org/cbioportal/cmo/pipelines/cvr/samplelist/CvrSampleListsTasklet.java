/*
 * Copyright (c) 2017, 2023, 2024 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.samplelist;

import jakarta.annotation.Resource;
import java.io.*;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMasterList;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLResult;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 *
 * @author ochoaa
 */
public class CvrSampleListsTasklet implements Tasklet {

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.tokens.retrieve_master_list.route}")
    private String dmpRetrieveMasterListRoute;

    @Value("${dmp.get_masterlist_initial_response_timeout}")
    private Integer dmpMasterListInitialResponseTimeout;

    @Value("${dmp.get_masterlist_maximum_response_timeout}")
    private Integer dmpMasterListMaximumResponseTimeout;

    @Value("#{jobParameters[dropDeadInstantString]}")
    private String dropDeadInstantString;

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Value("#{jobParameters[maxNumSamplesToRemove]}")
    private Integer maxNumSamplesToRemove;

    @Value("#{jobParameters[jsonMode]}")
    private Boolean jsonMode;

    @Value("#{jobParameters[gmlMode]}")
    private Boolean gmlMode;

    @Value("#{jobParameters[gmlMasterListSessionId]}")
    private String gmlMasterListSessionId;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Resource(name="masterListTokensMap")
    private Map<String, String> masterListTokensMap;

    private Logger log = Logger.getLogger(CvrSampleListsTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        // load master list from CVR
        log.info("Loading master list from CVR for study: " + studyId);
        Set<String> dmpMasterList = new HashSet<>();
        try {
            dmpMasterList = generateDmpMasterList();
            if (dmpMasterList.size() > 0) {
                log.info("DMP master list for " + studyId + " contains " + String.valueOf(dmpMasterList.size()) + " samples");
            } else {
                log.warn("No sample IDs were returned using DMP master list endpoint for study " + studyId);
            }
        } catch (HttpClientErrorException e) {
            log.warn("Error occurred while retrieving master list for " + studyId + " - the default master list will be set to samples already in portal.\n"
                    + e.getLocalizedMessage());
        }
        // load whited listed samples with zero variants
        Set<String> whitedListedSamplesWithZeroVariants = new HashSet<>();
        try {
            whitedListedSamplesWithZeroVariants = loadWhitelistedSamplesWithZeroVariants();
        } catch (Exception e) {
            log.warn("Error loading whitelisted samples with zero variants from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE + "\n" + e.getLocalizedMessage());
        }
        // init new dmp samples (or patients) list if running in json/gmlJson mode
        if (jsonMode) {
            if (gmlMode) {
                initNewDmpGmlPatientsForJsonMode();
            } else {
                initNewDmpSamplesForJsonMode();
            }
        }

        // update cvr sample list util
        cvrSampleListUtil.setDmpMasterList(dmpMasterList);
        cvrSampleListUtil.setMaxNumSamplesToRemove(maxNumSamplesToRemove);
        cvrSampleListUtil.setWhitelistedSamplesWithZeroVariants(whitedListedSamplesWithZeroVariants);
        return RepeatStatus.FINISHED;
    }

    private void initNewDmpGmlPatientsForJsonMode() {
        log.info("Loading new DMP GML patient IDs from: " + CVRUtilities.GML_FILE);
        GMLData gmlData = new GMLData();
        // load cvr data from cvr_gml_data.json file
        File cvrGmlFile = new File(privateDirectory, CVRUtilities.GML_FILE);
        try {
            gmlData = cvrUtilities.readGMLJson(cvrGmlFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrGmlFile.getName());
            throw new RuntimeException(e);
        }
        Set<String> newDmpGmlPatients = new HashSet<>();
        for (GMLResult result : gmlData.getResults()) {
            newDmpGmlPatients.add(result.getMetaData().getDmpPatientId());
        }
        cvrSampleListUtil.setNewDmpGmlPatients(newDmpGmlPatients);
    }

    private void initNewDmpSamplesForJsonMode() {
        log.info("Loading new DMP sample IDs from: " + CVRUtilities.CVR_FILE);
        CVRData cvrData = new CVRData();
        // load cvr data from cvr_data.json file
        File cvrFile = new File(privateDirectory, CVRUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new RuntimeException(e);
        }
        Set<String> newDmpSamples = new HashSet<>();
        for (CVRMergedResult result : cvrData.getResults()) {
            newDmpSamples.add(result.getMetaData().getDmpSampleId());
        }
        cvrSampleListUtil.setNewDmpSamples(newDmpSamples);
    }

    private Set<String> loadWhitelistedSamplesWithZeroVariants() throws Exception {
        Set<String> whitedListedSamplesWithZeroVariants = new HashSet<>();
        File whitelistedSamplesFile = new File(stagingDirectory, CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
        if (!whitelistedSamplesFile.exists()) {
            log.info("File does not exist - skipping data loading from whitelisted samples file: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
        } else {
            Scanner reader = new Scanner(whitelistedSamplesFile);
            while (reader.hasNext()) {
                whitedListedSamplesWithZeroVariants.add(reader.nextLine().trim());
            }
            reader.close();
            // log warning if whitelisted samples file exists but empty or nothing loaded from it
            if (whitedListedSamplesWithZeroVariants.isEmpty()) {
                log.warn("White listed file exists but nothing loaded from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            } else {
                log.info("Loaded " + whitedListedSamplesWithZeroVariants.size() + " whitelisted samples with zero variants from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            }
        }
        return whitedListedSamplesWithZeroVariants;
    }

    private void logRetrieveMasterListFailure(int numberOfRequestsAttempted, String message) {
        log.error(String.format("Error fetching CVR master list for study %s (after %d attempts) %s", studyId, numberOfRequestsAttempted, message));
    }

    private Set<String> generateDmpMasterList() {
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        String activeSessionId;
        if (gmlMode) {
            activeSessionId = gmlMasterListSessionId;
        } else {
            activeSessionId = sessionId;
        }
        String studyRetrieveMasterListEndpoint = masterListTokensMap.get(studyId);
        String dmpUrl = String.format("%s%s/%s/%s", dmpServerName, dmpRetrieveMasterListRoute, activeSessionId, studyRetrieveMasterListEndpoint);
        HttpClientWithTimeoutAndRetry client = new HttpClientWithTimeoutAndRetry(
                dmpMasterListInitialResponseTimeout,
                dmpMasterListMaximumResponseTimeout,
                InstantStringUtil.createInstant(dropDeadInstantString),
                false); // on a server error response, stop trying and move on. We continue processing even when there is no retrieved master list
        Set<String> dmpSamples = new HashSet<String>();
        ResponseEntity<CVRMasterList> responseEntity = client.exchange(dmpUrl, HttpMethod.GET, requestEntity, null, CVRMasterList.class);
        if (responseEntity == null) {
            String message = "";
            if (client.getLastResponseBodyStringAfterException() != null) {
                message = String.format("final response body was: '%s'", client.getLastResponseBodyStringAfterException());
            } else {
                if (client.getLastRestClientException() != null) {
                    message = String.format("final exception was: (%s)", client.getLastRestClientException());
                }
            }
            logRetrieveMasterListFailure(client.getNumberOfRequestsAttempted(), message);
            //TODO : consider failing if master list retrieval fails. Otherwise, all samples will be deleted from the study and we will fail validation during import <wasted time>
            //       throw new RuntimeException(String.format("Error retrieving master list for study %s : %s", studyId, message)); // crash
            return dmpSamples; // continue without master list
        }
        CVRMasterList cvrMasterList = responseEntity.getBody();
        for (Map<String, String> samples : cvrMasterList.getSamples()) {
            // there is only one pair per Map
            if (samples.entrySet().iterator().hasNext()) {
                Map.Entry pair = (Map.Entry) samples.entrySet().iterator().next();
                String sample = (String) pair.getValue();
                log.debug("open(): sample '" + sample + "' in master list");
                dmpSamples.add(sample);
            }
        }
        return dmpSamples;
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<LinkedMultiValueMap<String, Object>>(headers);
    }

}
