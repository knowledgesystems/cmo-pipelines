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

package org.cbioportal.cmo.pipelines.cvr.genepanel;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.*;
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

/**
 *
 * @author heinsz
 */
public class CVRGenePanelReader implements ItemStreamReader<CVRGenePanelRecord> {
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    private List<CVRGenePanelRecord> genePanelRecords = new ArrayList();
    Logger log = Logger.getLogger(CVRGenePanelReader.class);

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

        File genePanelFile = new File(stagingDirectory, cvrUtilities.GENE_PANEL_FILE);
        if (!genePanelFile.exists()) {
            log.error("File does not exist - skipping data loading from gene panel file: " + genePanelFile.getName());
        }
        else {
            log.info("Loading gene panel data from: " + genePanelFile.getName());
            final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRGenePanelRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRGenePanelFieldSetMapper());

            FlatFileItemReader<CVRGenePanelRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(genePanelFile));
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
                CVRGenePanelRecord to_add;
                while ((to_add = reader.read()) != null) {
                    if (!cvrUtilities.getNewIds().contains(to_add.getSAMPLE_ID()) && to_add.getSAMPLE_ID() != null) {
                        genePanelRecords.add(to_add);
                    }
                }
            }
            catch (Exception e) {
                log.error("Error reading data from gene panel file: " + genePanelFile.getName());
                throw new ItemStreamException(e);
            }
            reader.close();
        }

        for (CVRMergedResult result : cvrData.getResults()) {
            CVRGenePanelRecord record = new CVRGenePanelRecord(result.getMetaData());
            genePanelRecords.add(record);
        }
        setGenePanelHeader(ec, genePanelRecords.get(0));
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRGenePanelRecord read() throws Exception {
        if (!genePanelRecords.isEmpty()) {
            return genePanelRecords.remove(0);
        }
        return null;
    }

    private void setGenePanelHeader(ExecutionContext ec, CVRGenePanelRecord record) {
        List<String> header = new ArrayList<>();
        header.add("SAMPLE_ID");
        header.addAll(record.getPanelMap().keySet());
        ec.put("genePanelHeader", header);
    }
}
