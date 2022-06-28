/*
 * Copyright (c) 2016 - 2022 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.sv;

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
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
public class CVRSvDataReader implements ItemStreamReader<CVRSvRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;
    
    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private List<CVRSvRecord> svRecords = new ArrayList();

    Logger log = Logger.getLogger(CVRSvDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        CVRData cvrData = new CVRData();        
        // load cvr data from cvr_data.json file
        File cvrFile = new File(privateDirectory, cvrUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        
        File svFile = new File(stagingDirectory, cvrUtilities.SV_FILE);        
        if (!svFile.exists()) {
            log.info("File does not exist - skipping data loading from SV file: " + svFile.getName());
        }
        else {
            log.info("Loading SV data from: " + svFile.getName());
            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRSvRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRSvFieldSetMapper());
            
            FlatFileItemReader<CVRSvRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(svFile));
            reader.setLineMapper(mapper);
            reader.setLinesToSkip(1);
            reader.open(ec);
                
            try {
                CVRSvRecord to_add;
                while ((to_add = reader.read()) != null) {
                    if (!cvrSampleListUtil.getNewDmpSamples().contains(to_add.getSample_ID()) && to_add.getSample_ID()!= null) {
                        to_add.setSite1_Hugo_Symbol(to_add.getSite1_Hugo_Symbol().trim());
                        to_add.setSite2_Hugo_Symbol(to_add.getSite2_Hugo_Symbol().trim());
                        svRecords.add(to_add);
                    }
                }
            } catch (Exception e) {
                log.error("Error loading data from SV file: " + svFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }
        
        for (CVRMergedResult result : cvrData.getResults()) {
            String sampleId = result.getMetaData().getDmpSampleId();
            List<CVRSvVariant> variants = result.getSvVariants();
            for (CVRSvVariant variant : variants) {
                CVRSvRecord record = new CVRSvRecord(variant, sampleId);
                svRecords.add(record);
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
    public CVRSvRecord read() throws Exception {
        while (!svRecords.isEmpty()) {
            CVRSvRecord record = svRecords.remove(0);
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getSample_ID())) {
                cvrSampleListUtil.addSampleRemoved(record.getSample_ID());
                continue;
            }
            return record;
        }
        return null;
    }
}
