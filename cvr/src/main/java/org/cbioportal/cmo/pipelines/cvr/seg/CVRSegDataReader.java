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

package org.cbioportal.cmo.pipelines.cvr.seg;

/**
 *
 * @author jake-rose
 */

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSegData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSegRecord;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

public class CVRSegDataReader implements ItemStreamReader<CVRSegRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[studyId]}")
    private String studyId;
    
    @Autowired
    public CVRUtilities cvrUtilities;

    private List<CVRSegRecord> cvrSegRecords = new ArrayList();

    Logger log = Logger.getLogger(CVRSegDataReader.class);

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

        // only read from seg file if exists
        File segFile = new File(stagingDirectory, studyId + cvrUtilities.SEG_FILE);
        if (!segFile.exists()) {
            log.error("File does not exist - skipping data loading from SEG file: " + segFile.getName());
        }
        else {
            log.info("Loading SEG data from: " + segFile.getName());
            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRSegRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRSegFieldSetMapper());
            
            FlatFileItemReader<CVRSegRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(segFile));
            reader.setLineMapper(mapper);
            reader.setLinesToSkip(1);
            reader.open(ec);
            
            try {
                CVRSegRecord to_add;
                while ((to_add = reader.read()) != null && to_add.getID() !=  null) {
                    if (!cvrUtilities.getNewIds().contains(to_add.getID())) {
                        cvrSegRecords.add(to_add);
                    }
                }
            }
            catch (Exception e) {
                log.error("Error loading data from SEG file: " + segFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }
        
        // merge cvr SEG data existing SEG data and new data from CVR
        for (CVRMergedResult result : cvrData.getResults()) {
            CVRSegData cvrSegData = result.getSegData();
            if (cvrSegData.getSegData() == null) {
                continue;
            }
            HashMap<Integer,String> indexMap = new HashMap<>();
            boolean first = true;
            String id = result.getMetaData().getDmpSampleId();
            for (List<String> segData : cvrSegData.getSegData()) {                
                if (first) {
                    for (int i=0;i<segData.size();i++) {
                        indexMap.put(i, segData.get(i));
                    }
                    first = false;
                } else {
                    CVRSegRecord cvrSegRecord = new CVRSegRecord();
                    for (int i=0;i<segData.size();i++) {
                        cvrSegRecord.setID(id);
                        String field = indexMap.get(i).replace(".", "_");//dots in source; replaced for method
                        try {
                            cvrSegRecord.getClass().getMethod("set" + field, String.class).invoke(cvrSegRecord, segData.get(i));
                        } 
                        catch (Exception e) {
                            log.warn("No such method 'set" + field + "' for CVRSegRecord");
                        }
                    }
                    cvrSegRecord.setIsNew(cvrUtilities.IS_NEW);
                    cvrSegRecords.add(cvrSegRecord);
                }
            }
        }
    }

    @Override
    public CVRSegRecord read() throws Exception {
        if (!cvrSegRecords.isEmpty()) {
            return cvrSegRecords.remove(0);
        }
        return null;
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }
}
