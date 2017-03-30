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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
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
public class CVRClinicalDataReader implements ItemStreamReader<CVRClinicalRecord> {
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    private List<CVRClinicalRecord> clinicalRecords = new ArrayList();

    Logger log = Logger.getLogger(CVRClinicalDataReader.class);

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
        
        File clinicalFile = new File(stagingDirectory, cvrUtilities.CLINICAL_FILE);
        if (!clinicalFile.exists()) {
            log.error("File does not exist - skipping data loading from clinical file: " + clinicalFile.getName());
        }
        else {
            log.info("Loading clinical data from: " + clinicalFile.getName());
            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRClinicalRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRClinicalFieldSetMapper());
            
            FlatFileItemReader<CVRClinicalRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(clinicalFile));
            reader.setLineMapper(mapper);
            reader.setLinesToSkip(1);
            reader.open(ec);
            
            try {
                CVRClinicalRecord to_add;
                while ((to_add = reader.read()) != null) {
                    if (!cvrUtilities.getNewIds().contains(to_add.getSAMPLE_ID()) && to_add.getSAMPLE_ID() != null) {
                        clinicalRecords.add(to_add);
                        cvrUtilities.addAllIds(to_add.getSAMPLE_ID());
                    }
                }                
            }
            catch (Exception e) {
                log.error("Error reading data from clinical file: " + clinicalFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }

        for (CVRMergedResult result : cvrData.getResults()) {
            CVRClinicalRecord record = new CVRClinicalRecord(result.getMetaData());
            record.setIsNew(cvrUtilities.IS_NEW);
            clinicalRecords.add(record);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRClinicalRecord read() throws Exception {
        if (!clinicalRecords.isEmpty()) {
            return clinicalRecords.remove(0);
        }
        return null;
    }
}
