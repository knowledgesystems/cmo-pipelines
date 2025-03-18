/*
 * Copyright (c) 2016 - 2022, 2025 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.mutation;

import org.cbioportal.annotator.*;
import org.cbioportal.annotator.internal.AnnotationSummaryStatistics;
import org.cbioportal.cmo.pipelines.cvr.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.models.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class CVRMutationDataReader implements ItemStreamReader<AnnotatedRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Value("#{jobParameters[forceAnnotation]}")
    private boolean forceAnnotation;

    @Value("#{jobParameters[stopZeroVariantWarnings]}")
    private boolean stopZeroVariantWarnings;

    @Value("${genomenexus.post_interval_size}")
    private Integer postIntervalSize;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Autowired
    private Annotator annotator;

    private final Deque<AnnotatedRecord> mutationRecords = new LinkedList<>();
    private Map<String, List<MutationRecord>> mutationMap = new HashMap<>();

    private File mutationFile;
    Set<String> header = new LinkedHashSet<>();
    private AnnotationSummaryStatistics summaryStatistics;

    Logger log = Logger.getLogger(CVRMutationDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        this.summaryStatistics = new AnnotationSummaryStatistics(annotator);
        CVRData cvrData = new CVRData();
        // load cvr data from cvr_data.json file
        File cvrFile = new File(privateDirectory, CVRUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        // load mutation records from cvr data
        loadMutationRecordsFromJson(cvrData);

        // load mutation records from existing maf
        this.mutationFile = new File(stagingDirectory, CVRUtilities.MUTATION_FILE);
        if (!mutationFile.exists()) {
            log.info("File does not exist - skipping data loading from mutation file: " + mutationFile.getName());
        }
        else {
            try {
                loadExistingMutationRecords();
                // add comment lines to execution context
                ec.put("commentLines", cvrUtilities.processFileComments(mutationFile));
            } catch (Exception e) {
                log.error("Error loading data from mutation file: " + mutationFile.getName());
                throw new ItemStreamException(e);
            }
        }
        // add header and filename to write to for writer
        ec.put("mutationHeader", new ArrayList(header));
        ec.put("mafFilename", CVRUtilities.MUTATION_FILE);
        summaryStatistics.printSummaryStatistics();
    }

    private void loadMutationRecordsFromJson(CVRData cvrData) {
        List<MutationRecord> recordsToAnnotate = new ArrayList<>();
        for (CVRMergedResult result : cvrData.getResults()) {
            String sampleId = result.getMetaData().getDmpSampleId();
            int countSignedOutSnps = result.getAllSignedoutCvrSnps().size();
            cvrSampleListUtil.updateSignedoutSampleSnpCounts(sampleId, countSignedOutSnps);
            if (cvrSampleListUtil.getPortalSamples().contains(sampleId)) {
                String somaticStatus = result.getMetaData().getSomaticStatus() != null ? result.getMetaData().getSomaticStatus() : "N/A";
                for (CVRSnp snp : result.getAllSignedoutCvrSnps()) {
                    MutationRecord to_add = cvrUtilities.buildCVRMutationRecord(snp, sampleId, somaticStatus);
                    recordsToAnnotate.add(to_add);
                    addRecordToMap(to_add);
                }
            }
            if (!stopZeroVariantWarnings && countSignedOutSnps == 0) {
                log.warn(sampleId + " has no snps (might be whitelisted)");
            }
        }
        log.info("Loaded " + String.valueOf(recordsToAnnotate.size()) + " records from JSON");
        try {
            annotateRecordsWithPOST(recordsToAnnotate, true);
        } catch (Exception e) {
            log.error("Error annotating with POSTs", e);
            throw new RuntimeException(e);
        }
    }

    private void loadExistingMutationRecords() throws Exception {
        log.info("Loading mutation data from: " + mutationFile.getName());
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<MutationRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new CVRMutationFieldSetMapper());

        FlatFileItemReader<MutationRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(mutationFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.setSkippedLinesCallback(new LineCallbackHandler() {
            @Override
            public void handleLine(String line) {
                tokenizer.setNames(line.split("\t"));
            }
        });
        reader.open(new ExecutionContext());
        List<MutationRecord> recordsToAnnotate = new ArrayList<>();
        MutationRecord to_add;
        while ((to_add = reader.read()) != null && to_add.getTUMOR_SAMPLE_BARCODE() != null) {
            // skip if new sample or if mutation record for sample seen already
            if (cvrSampleListUtil.getNewDmpSamples().contains(to_add.getTUMOR_SAMPLE_BARCODE()) ||
                !cvrSampleListUtil.getPortalSamples().contains(to_add.getTUMOR_SAMPLE_BARCODE()) ||
                    cvrUtilities.isDuplicateRecord(to_add, mutationMap.get(to_add.getTUMOR_SAMPLE_BARCODE()))) {
                continue;
            }
            cvrSampleListUtil.updateSignedoutSampleSnpCounts(to_add.getTUMOR_SAMPLE_BARCODE(), 1);
            recordsToAnnotate.add(to_add);
            addRecordToMap(to_add);
        }
        reader.close();
        log.info("Loaded " + String.valueOf(recordsToAnnotate.size()) + " records from MAF");
        annotateRecordsWithPOST(recordsToAnnotate, forceAnnotation);
    }

    private void annotateRecordsWithPOST(List<MutationRecord> records, boolean reannotate) throws Exception {
        int totalVariantsToAnnotateCount = records.size();
        int annotatedVariantsCount = 0;
        // annotate with GenomeNexusImpl annotator from genome nexus annotation pipeline
        // records will be partitioned inside annotator client
        // records which do not get a response back will automatically be defaulted to an AnnotatedRecord(record)
        List<AnnotatedRecord> annotatedRecords = annotator.getAnnotatedRecordsUsingPOST(summaryStatistics, records, "mskcc", true, postIntervalSize, reannotate, "StripEntireSharedPrefix", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
        mutationRecords.addAll(annotatedRecords);
        for (AnnotatedRecord ar : annotatedRecords) {
            logAnnotationProgress(++annotatedVariantsCount, totalVariantsToAnnotateCount, postIntervalSize);
            header.addAll(ar.getHeaderWithAdditionalFields());
        }
    }

    private void logAnnotationProgress(Integer annotatedVariantsCount, Integer totalVariantsToAnnotateCount, Integer intervalSize) {
        if (annotatedVariantsCount % intervalSize == 0 || Objects.equals(annotatedVariantsCount, totalVariantsToAnnotateCount)) {
            log.info("\tOn record " + String.valueOf(annotatedVariantsCount) + " out of " + String.valueOf(totalVariantsToAnnotateCount) +
                    ", annotation " + String.valueOf((int)(((annotatedVariantsCount * 1.0)/totalVariantsToAnnotateCount) * 100)) + "% complete");
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public AnnotatedRecord read() throws Exception {
        while (!mutationRecords.isEmpty()) {
            return mutationRecords.pollFirst();
        }
        return null;
    }

    private void addRecordToMap(MutationRecord record) {
        String sampleId = record.getTUMOR_SAMPLE_BARCODE();
        mutationMap.computeIfAbsent(sampleId, k -> new ArrayList<>()).add(record);
    }
}
