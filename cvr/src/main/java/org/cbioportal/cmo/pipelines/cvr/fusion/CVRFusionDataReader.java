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

package org.cbioportal.cmo.pipelines.cvr.fusion;

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRFusionRecord;
import com.google.common.base.Strings;
import org.cbioportal.cmo.pipelines.cvr.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class CVRFusionDataReader implements ItemStreamReader<CVRFusionRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;
    
    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private List<CVRFusionRecord> fusionRecords = new ArrayList();
    private Set<String> fusionsSeen = new HashSet();
    
    private Logger LOG = Logger.getLogger(CVRFusionDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // load existing fusions from data file
        processFusionsFile();
        // merge existing and new fusion records
        processJsonFile();

        // set fusion file and header to write to for processor, writer
        ec.put("fusionsFilename", CVRUtilities.FUSION_FILE);
        ec.put("fusionHeader", CVRFusionRecord.getFieldNames());
    }

    private void processFusionsFile() {
        File fusionFile = new File(stagingDirectory, CVRUtilities.FUSION_FILE);
        if (!fusionFile.exists()) {
            LOG.info("File does not exist - skipping data loading from fusion file: " + fusionFile.getName());
            return;
        }
        LOG.info("Loading fusion data from: " + fusionFile.getName());

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<CVRFusionRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new CVRFusionFieldSetMapper());

        FlatFileItemReader<CVRFusionRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(fusionFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.open(new ExecutionContext());

        try {
            CVRFusionRecord to_add;
            while ((to_add = reader.read()) != null) {
                if (!cvrSampleListUtil.getNewDmpSamples().contains(to_add.getTumor_Sample_Barcode()) && to_add.getTumor_Sample_Barcode() != null) {
                    String fusion = to_add.getHugo_Symbol() + "|" +to_add.getTumor_Sample_Barcode() + "|" + to_add.getFusion();
                    if (!fusionsSeen.contains(fusion)) {
                        fusionRecords.add(to_add);
                        fusionsSeen.add(fusion);
                    }
                }
            }
        } catch (Exception e){
            LOG.info("Error loading data from fusion file: " + fusionFile.getName());
            throw new ItemStreamException(e);
        }
        reader.close();
    }

    private void processJsonFile() {
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
            String sampleId = result.getMetaData().getDmpSampleId();
            List<CVRSvVariant> variants = result.getSvVariants();
            for (CVRSvVariant variant : variants) {
                // skip records where both gene 1 and gene 2 are null or empty
                if (Strings.isNullOrEmpty(variant.getSite1_Gene()) && Strings.isNullOrEmpty(variant.getSite2_Gene())) {
                    LOG.warn("Skipping fusion record where genes are missing for sample '" + sampleId + "'" + 
                            (Strings.isNullOrEmpty(variant.getAnnotation()) ? "" :  " - record annotation: " + variant.getAnnotation().replaceAll("[\\t\\n\\r]+"," ")));
                    continue;
                }
                CVRFusionRecord record = null;
                try {
                    record = new CVRFusionRecord(variant, sampleId, false);
                }
                catch (NullPointerException e) {
                    // log error if both variant gene sites are not null
                    if (variant.getSite1_Gene() != null && variant.getSite2_Gene() != null) {
                        LOG.error("Error creating fusion record for sample, gene1-gene2 event: " + sampleId + ", " + variant.getSite1_Gene() + "-" + variant.getSite2_Gene());
                    }
                    continue;
                }
                String fusion = record.getHugo_Symbol() + "|" +record.getTumor_Sample_Barcode() + "|" + record.getFusion();
                CVRFusionRecord recordReversed = new CVRFusionRecord(variant, sampleId, true);
                if (!fusionsSeen.contains(fusion)) {
                    fusionRecords.add(record);
                    fusionsSeen.add(fusion);
                }
                if (!variant.getSite1_Gene().equals(variant.getSite2_Gene())) {
                    fusion = recordReversed.getHugo_Symbol() + "|" +recordReversed.getTumor_Sample_Barcode() + "|" + recordReversed.getFusion();
                    if (!fusionsSeen.contains(fusion)) {
                        fusionRecords.add(recordReversed);
                        fusionsSeen.add(fusion);
                    }
                }
            }
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRFusionRecord read() throws Exception {
        while (!fusionRecords.isEmpty()) {
            CVRFusionRecord record = fusionRecords.remove(0);
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getTumor_Sample_Barcode())) {
                cvrSampleListUtil.addSampleRemoved(record.getTumor_Sample_Barcode());
                continue;
            }
            return record;
        }
        return null;
    }
}
