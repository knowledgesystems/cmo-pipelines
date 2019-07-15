/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.cvr.linkedimpactcase;

import org.cbioportal.cmo.pipelines.cvr.model.staging.LinkedMskimpactCaseRecord;
import com.google.common.base.Strings;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.EmailUtil;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class LinkedMskimpactCaseReader implements ItemStreamReader<LinkedMskimpactCaseRecord> {

    @Value("${dmp.email.sender}")
    private String sender;

    @Value("${dmp.email.recipient}")
    private String dmpRecipient;

    @Value("${email.recipient}")
    private String defaultRecipient;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Autowired
    private EmailUtil emailUtil;

    private final Double DROP_THRESHOLD = 0.9;
    private Map<String, LinkedMskimpactCaseRecord> existingLinkedIdsMap = new HashMap<>();
    private Map<String, LinkedMskimpactCaseRecord> compiledLinkedIdsMap = new HashMap<>();
    private List<LinkedMskimpactCaseRecord> linkedIds = new ArrayList<>();

    private static final Logger LOG = Logger.getLogger(LinkedMskimpactCaseReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // load new linked ARCHER sample data
        loadNewLinkedIds();
        // load existing linked ARCHER sample data
        loadExistingLinkedIds();

        // if size of linkedIds is significantly lower than count of existing records
        // then there might be an issue with the CVR JSON returned - we do not want
        // to override the linked_cases.txt file with empty data
        if (compiledLinkedIdsMap.isEmpty() || compiledLinkedIdsMap.size() < (DROP_THRESHOLD * existingLinkedIdsMap.size())) {
            StringBuilder message = new StringBuilder();
            String subject;
            // different message and body for email/logger based on condition met
            if (compiledLinkedIdsMap.isEmpty()) {
                subject = "[URGENT] CVR Pipeline: MSKARCHER 'linked_mskimpact_case' data missing";
                message.append("MSKARCHER meta-data is missing 'linked_mskimpact_case' data for all ARCHER samples - please address ASAP!");
            }
            else {
                subject = "[WARNING] CVR Pipeline: MSKARCHER significant drop in 'linked_mskimpact_case' data";
                message.append("ARCHER linked IDs update dropped > 90% of current linked IDs count: ")
                    .append("\n\tExisting linked IDs count = ")
                    .append(existingLinkedIdsMap.size())
                    .append("\n\tLinked IDs count from latest CVR update = ")
                    .append(compiledLinkedIdsMap.size());
            }
            // send email and log message
            String[] recipients = {defaultRecipient, dmpRecipient};
            emailUtil.sendEmail(sender, recipients, subject, message.toString());
            LOG.error(message.toString());

            // add the existing linked ids to the list of records to be passed to processor/writer
            // existing linkages will override data for any overlapping sample ids
            compiledLinkedIdsMap.putAll(existingLinkedIdsMap);
        }
        this.linkedIds = new ArrayList(compiledLinkedIdsMap.values());
    }

    private void loadNewLinkedIds() {
        CVRData cvrData = new CVRData();
        // load cvr data from cvr_data.json file
        File cvrFile = new File(stagingDirectory, cvrUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            LOG.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        for (CVRMergedResult result : cvrData.getResults()) {
            String linkedId = result.getMetaData().getLinkedMskimpactCase();
            if (!Strings.isNullOrEmpty(linkedId) && !linkedId.equals("NA")) {
                compiledLinkedIdsMap.put(result.getMetaData().getDmpSampleId(), 
                        new LinkedMskimpactCaseRecord(result.getMetaData().getDmpSampleId(), linkedId));
            }
        }
    }
    private void loadExistingLinkedIds() {
        File stagingFile = new File(stagingDirectory, cvrUtilities.CORRESPONDING_ID_FILE);
        if (!stagingFile.exists()) {
            LOG.warn("File does not exist - skipping data loading from linked ARCHER samples file: " + stagingFile.getName());
            return;
        }
        LOG.info("Loading linked ARCHER sample data from: " + stagingFile.getName());
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<LinkedMskimpactCaseRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new LinkedImpactCaseFieldSetMapper());

        FlatFileItemReader<LinkedMskimpactCaseRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(stagingFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.open(new ExecutionContext());

        try {
            LinkedMskimpactCaseRecord to_add;
            while ((to_add = reader.read()) != null) {
                // only add samples that are not in the new dmp sample list
                if (!cvrSampleListUtil.getNewDmpSamples().contains(to_add.getSAMPLE_ID())) {
                    compiledLinkedIdsMap.put(to_add.getSAMPLE_ID(), to_add);
                }
                // keep a backup in case JSON returned dropped all "linked_mskimpact_case" data
                existingLinkedIdsMap.put(to_add.getSAMPLE_ID(), to_add);
            }
        }
        catch (Exception e) {
            LOG.error("Error reading linked ARCHER sample data from file: " + stagingFile.getName());
            throw new ItemStreamException(e);
        }
        finally {
            reader.close();
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public LinkedMskimpactCaseRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!linkedIds.isEmpty()) {
            return linkedIds.remove(0);
        }
        return null;
    }
}
