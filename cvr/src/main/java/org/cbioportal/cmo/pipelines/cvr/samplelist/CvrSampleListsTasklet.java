/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.cvr.samplelist;

import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMasterList;

import org.apache.log4j.Logger;
import java.io.*;
import java.util.*;
import javax.annotation.Resource;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLResult;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLResult;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

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

    Logger log = Logger.getLogger(CvrSampleListsTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        // load master list from CVR
        log.info("Loading master list from CVR for study: " + studyId);
        Set<String> dmpMasterList = new HashSet<>();
        try {
            dmpMasterList = generateDmpMasterList();
            if (dmpMasterList.size() > 0) {
                log.info("DMP master list for " + studyId + " contains " + String.valueOf(dmpMasterList.size()) + " samples");
            }
            else {
                log.warn("No sample IDs were returned using DMP master list endpoint for study " + studyId);
            }
        }
        catch (HttpClientErrorException e) {
            log.warn("Error occurred while retrieving master list for " + studyId + " - the default master list will be set to samples already in portal.\n"
                    + e.getLocalizedMessage());
        }
        // load whited listed samples with zero variants
        Set<String> whitedListedSamplesWithZeroVariants = new HashSet<>();
        try {
            whitedListedSamplesWithZeroVariants = loadWhitelistedSamplesWithZeroVariants();
        }
        catch (Exception e) {
            log.warn("Error loading whitelisted samples with zero variants from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE + "\n" + e.getLocalizedMessage());
        }
        // init new dmp samples (or patients) list if running in json/gmlJson mode
        if (jsonMode) {
            if (gmlMode) {
                initNewDmpGmlPatientsForJsonMode();
            }
            else {
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
        File cvrGmlFile = new File(stagingDirectory, CVRUtilities.GML_FILE);
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
        File cvrFile = new File(stagingDirectory, CVRUtilities.CVR_FILE);
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
        }
        else {
            Scanner reader = new Scanner(whitelistedSamplesFile);
            while (reader.hasNext()) {
                whitedListedSamplesWithZeroVariants.add(reader.nextLine().trim());
            }
            reader.close();
            // log warning if whitelisted samples file exists but empty or nothing loaded from it
            if (whitedListedSamplesWithZeroVariants.isEmpty()) {
                log.warn("White listed file exists but nothing loaded from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            }
            else {
                log.info("Loaded " + whitedListedSamplesWithZeroVariants.size() + " whitelisted samples with zero variants from: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            }
        }
        return whitedListedSamplesWithZeroVariants;
    }

    private Set<String> generateDmpMasterList() {
        String dmpUrl = dmpServerName + dmpRetrieveMasterListRoute + "/";
        if (gmlMode) {
            dmpUrl += gmlMasterListSessionId;
        } else {
            dmpUrl += sessionId;
        }
        dmpUrl += "/" + masterListTokensMap.get(studyId);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        ResponseEntity<CVRMasterList> responseEntity;
        Set<String> dmpSamples = new HashSet<>();
        try {
            responseEntity = restTemplate.exchange(dmpUrl, HttpMethod.GET, requestEntity, CVRMasterList.class);
            for (Map<String, String> samples : responseEntity.getBody().getSamples()) {
                // there is only one pair per Map
                if (samples.entrySet().iterator().hasNext()) {
                    Map.Entry pair = (Map.Entry) samples.entrySet().iterator().next();
                    String sample = (String) pair.getValue();
                    log.debug("open(): sample '" + sample + "' in master list");
                    dmpSamples.add(sample);
                }
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Error fetching CVR master list, response body is: '" + e.getResponseBodyAsString() + "'");
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Error fetching CVR master list");
        }
        return dmpSamples;
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }

}
