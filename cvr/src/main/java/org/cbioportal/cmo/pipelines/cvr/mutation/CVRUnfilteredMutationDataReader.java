/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

import java.io.*;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.annotator.*;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.models.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.HttpServerErrorException;

/**
 *
 * @author heinsz
 */
public class CVRUnfilteredMutationDataReader implements ItemStreamReader<AnnotatedRecord> {
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    private Annotator annotator;

    private File mutationFile;
    private List<AnnotatedRecord> mutationRecords = new ArrayList<>();
    private Map<String, List<AnnotatedRecord>> mutationMap = new HashMap<>();
    private Set<String> additionalPropertyKeys = new LinkedHashSet<>();

    Logger log = Logger.getLogger(CVRUnfilteredMutationDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        CVRData cvrData = new CVRData();        
        // load cvr data from cvr_data.json file
        File cvrFile = new File(stagingDirectory, cvrUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        
        Set<String> header = new LinkedHashSet<>();
        for (CVRMergedResult result : cvrData.getResults()) {
            String sampleId = result.getMetaData().getDmpSampleId();
            String somaticStatus = result.getMetaData().getSomaticStatus() != null ? result.getMetaData().getSomaticStatus() : "N/A";
            List<CVRSnp> snps = new ArrayList<>();
            snps.addAll(result.getSnpIndelExonic());
            snps.addAll(result.getSnpIndelExonicNp());
            snps.addAll(result.getSnpIndelSilent());
            snps.addAll(result.getSnpIndelSilentNp());
            for (CVRSnp snp : snps) {
                MutationRecord record = cvrUtilities.buildCVRMutationRecord(snp, sampleId, somaticStatus);
                AnnotatedRecord annotatedRecord = new AnnotatedRecord();
                try {
                    annotatedRecord = annotator.annotateRecord(record, false, "mskcc", false);
                } catch (HttpServerErrorException e) {
                    log.warn("Failed to annotate a record from json! Sample: " + sampleId + " Variant: " + record.getChromosome() + ":" + record.getStart_Position() + record.getReference_Allele() + ">" + record.getTumor_Seq_Allele2());
                    annotatedRecord = cvrUtilities.buildCVRAnnotatedRecord(record);
                }
                Map<String, String> additionalProperties = annotatedRecord.getAdditionalProperties();
                additionalProperties.put("IS_NEW", cvrUtilities.IS_NEW);
                record.setAdditionalProperties(additionalProperties);
                mutationRecords.add(annotatedRecord);
                header.addAll(record.getHeaderWithAdditionalFields());
                additionalPropertyKeys.addAll(record.getAdditionalProperties().keySet());
                mutationMap.getOrDefault(annotatedRecord.getTumor_Sample_Barcode(), new ArrayList()).add(annotatedRecord);
            }
        }

        this.mutationFile = new File(stagingDirectory, cvrUtilities.UNFILTERED_MUTATION_FILE);
        if (!mutationFile.exists()) {
            log.info("File does not exist - skipping data loading from mutation file: " + mutationFile.getName());
        }
        else {
            log.info("Loading mutation data from: " + mutationFile.getName());            
            final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
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
            reader.open(ec);
                        
            try {
                MutationRecord to_add;
                while ((to_add = reader.read()) != null && to_add.getTumor_Sample_Barcode() != null) {
                    if (!cvrUtilities.getNewIds().contains(to_add.getTumor_Sample_Barcode()) && 
                            !cvrUtilities.isDuplicateRecord(to_add, mutationMap.get(to_add.getTumor_Sample_Barcode()))) {
                        AnnotatedRecord to_add_annotated;
                        try {
                            to_add_annotated = annotator.annotateRecord(to_add, false, "mskcc", false);
                        } catch (HttpServerErrorException e) {
                            log.warn("Failed to annotate a record from existing file! Sample: " + to_add.getTumor_Sample_Barcode() + " Variant: " + to_add.getChromosome() + ":" + to_add.getStart_Position() + to_add.getReference_Allele() + ">" + to_add.getTumor_Seq_Allele2());
                            to_add_annotated = cvrUtilities.buildCVRAnnotatedRecord(to_add);
                        }
                        mutationRecords.add(to_add_annotated);
                        mutationMap.getOrDefault(to_add_annotated.getTumor_Sample_Barcode(), new ArrayList()).add(to_add_annotated);
                        header.addAll(to_add_annotated.getHeaderWithAdditionalFields());
                        additionalPropertyKeys.addAll(to_add_annotated.getAdditionalProperties().keySet());
                    }
                }
                ec.put("commentLines", cvrUtilities.processFileComments(mutationFile, false));
            }
            catch (Exception e) {
                log.warn("Error loading data from mutation file: " + mutationFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }
        ec.put("mutation_header", new ArrayList(header));
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public AnnotatedRecord read() throws Exception {
        if (!mutationRecords.isEmpty()) {
            AnnotatedRecord annotatedRecord = mutationRecords.remove(0);
            for (String additionalProperty : additionalPropertyKeys) {
                Map<String, String> additionalProperties = annotatedRecord.getAdditionalProperties();
                if (!additionalProperties.keySet().contains(additionalProperty)) {
                    additionalProperties.put(additionalProperty, "");
                }
            }
            return annotatedRecord;
        }
        return null;
    }

}
