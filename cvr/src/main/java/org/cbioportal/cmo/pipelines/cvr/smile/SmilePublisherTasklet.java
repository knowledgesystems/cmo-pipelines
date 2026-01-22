package org.cbioportal.cmo.pipelines.cvr.smile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMetaData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSnp;
import org.mskcc.cmo.messaging.Gateway;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author ochoaa
 */
public class SmilePublisherTasklet implements Tasklet {
    @Value("#{jobParameters[testingMode]}")
    private boolean testingMode;

    @Value("#{jobParameters[jsonFilename]}")
    private String jsonFilename;

    @Value("${smile.dmp_new_sample_topic}")
    private String smileDmpNewSampleTopic;

    @Autowired
    private CvrSampleListUtil cvrSampleListUtil;

    @Autowired
    private CVRUtilities cvrUtilities;

    @Autowired
    private Gateway messagingGateway;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = Logger.getLogger(SmilePublisherTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        if (testingMode) {
            log.info("[TEST MODE] samples will not be published to smile");
            return RepeatStatus.FINISHED;
        }

        if (!messagingGateway.isConnected()) {
            log.info("Unable to connect to SMILE, samples will not be published.");
            return RepeatStatus.FINISHED;
        }

        // nothing to do if our reference list is empty
        if (cvrSampleListUtil.getSmileSamplesToPublishList().isEmpty()) {
            log.info("No samples to publish to SMILE");
            return RepeatStatus.FINISHED;
        }
        // iterate through json file and if sample id is in list of samples that were
        // consumed successfully then publish its metadata to smile
        Integer samplesPublished = loadAndPublishDmpSamplesFromJson();
        log.info("Total samples published to SMILE: " + samplesPublished);

        return RepeatStatus.FINISHED;
    }

    private Integer loadAndPublishDmpSamplesFromJson() throws JsonProcessingException, Exception {
        File cvrFile = new File(jsonFilename);
        CVRData cvrData = new CVRData();
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }

        int samplesPublished = 0;
        // add sample metadata to list that should be published to smile
        Set<String> samplesToPublish = cvrSampleListUtil.getSmileSamplesToPublishList();
        for (CVRMergedResult result : cvrData.getResults()) {
            String dmpId = result.getMetaData().getDmpSampleId();
            if (samplesToPublish.contains(dmpId)) {
                Map<String, Object> sampleMap = mapper.convertValue(result.getMetaData(), Map.class);
                // add recommended_coverage to sample data map for smile
                String recommendedCoverage = resolveRecommendedCoverageFromVafs(result.getAllSignedoutCvrSnps());
                sampleMap.put("recommended_coverage", recommendedCoverage);

                log.info("Publishing sample metadata to SMILE: " + dmpId);
                messagingGateway.publish(smileDmpNewSampleTopic, mapper.writeValueAsString(sampleMap));
                samplesPublished++;
            }
        }
        return samplesPublished;
    }

    private String resolveRecommendedCoverageFromVafs(List<CVRSnp> variants) {
        List<Double> vafs = new ArrayList<>();
        for (CVRSnp v : variants) {
            if (v.getTumorAd() == null || v.getTumorDp() == null) {
                continue;
            }
            double vaf = (double) v.getTumorAd() / v.getTumorDp();
            vafs.add(vaf);
        }

        // if no valid vafs calculated then return default 'Missing_Data'
        if (vafs.isEmpty()) {
            return "Missing_Data";
        }

        Median med = new Median();
        double[] values = new double[vafs.size()];
        for (int i=0;i<vafs.size();i++) {
            values[i] = vafs.get(i);
        }
        Double medianExonicVaf = med.evaluate(values);
        return medianExonicVaf >= 0.15 ? "Standard_Coverage" : "Higher_Coverage";
    }
}
