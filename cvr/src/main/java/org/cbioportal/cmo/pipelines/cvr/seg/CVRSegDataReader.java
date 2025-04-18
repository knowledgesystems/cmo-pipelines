/*
 * Copyright (c) 2016 - 2017, 2025 Memorial Sloan-Kettering Cancer Center.
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

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSegRecord;
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
 * @author jake-rose
 */
public class CVRSegDataReader implements ItemStreamReader<CVRSegRecord> {

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Value("#{jobParameters[studyId]}")
    private String studyId;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    private CvrSampleListUtil cvrSampleListUtil;

    private final Deque<CVRSegRecord> cvrSegRecords = new LinkedList<>();

    Logger log = Logger.getLogger(CVRSegDataReader.class);

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
                    if (!cvrSampleListUtil.getNewDmpSamples().contains(to_add.getID())
                        && cvrSampleListUtil.getPortalSamples().contains(to_add.getID())) {
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
            if (cvrSampleListUtil.getPortalSamples().contains(id)) {
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
                            String field = indexMap.get(i).replace(".", "_"); //dots in source; replaced for method
                            CVRSegFieldSetMapper.setFieldValue(cvrSegRecord, field, segData.get(i));
                        }
                        cvrSegRecords.add(cvrSegRecord);
                    }
                }
            }
        }
    }

    @Override
    public CVRSegRecord read() throws Exception {
        while (!cvrSegRecords.isEmpty()) {
            CVRSegRecord record = cvrSegRecords.pollFirst();
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getID())) {
                continue;
            }
            return record;
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
