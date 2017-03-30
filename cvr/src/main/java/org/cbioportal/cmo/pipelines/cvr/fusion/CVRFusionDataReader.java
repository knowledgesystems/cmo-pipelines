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

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRFusionRecord;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSvVariant;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
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
public class CVRFusionDataReader implements ItemStreamReader<CVRFusionRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    private List<CVRFusionRecord> fusionRecords = new ArrayList();
    private Set<String> fusionsSeen = new HashSet();
    Logger log = Logger.getLogger(CVRFusionDataReader.class);

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

        File fusionFile = new File(stagingDirectory, cvrUtilities.FUSION_FILE);
        if (!fusionFile.exists()) {
            log.info("File does not exist - skipping data loading from fusion file: " + fusionFile.getName());
        }
        else {
            log.info("Loading fusion data from: " + fusionFile.getName());

            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRFusionRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRFusionFieldSetMapper());

            FlatFileItemReader<CVRFusionRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(fusionFile));
            reader.setLineMapper(mapper);
            reader.setLinesToSkip(1);
            reader.open(ec);

            try {
                CVRFusionRecord to_add;
                while ((to_add = reader.read()) != null) {
                    if (!cvrUtilities.getNewIds().contains(to_add.getTumor_Sample_Barcode()) && to_add.getTumor_Sample_Barcode() != null) {
                        String fusion = to_add.getHugo_Symbol() + "|" +to_add.getTumor_Sample_Barcode() + "|" + to_add.getFusion();
                        if (!fusionsSeen.contains(fusion)) {
                            fusionRecords.add(to_add);
                            fusionsSeen.add(fusion);
                        }
                    }
                }
            } catch (Exception e){
                log.info("Error loading data from fusion file: " + fusionFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }

        // merge existing and new fusion records
        for (CVRMergedResult result : cvrData.getResults()) {
            String sampleId = result.getMetaData().getDmpSampleId();
            List<CVRSvVariant> variants = result.getSvVariants();
            for (CVRSvVariant variant : variants) {
                CVRFusionRecord record = new CVRFusionRecord(variant, sampleId, false);
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
        if (!fusionRecords.isEmpty()) {
            return fusionRecords.remove(0);
        }
        return null;
    }
}
