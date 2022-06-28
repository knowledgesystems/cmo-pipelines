/*
 * Copyright (c) 2018 - 2022 Memorial Sloan-Kettering Cancer Center.
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

import com.google.common.base.Strings;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLResult;
import org.cbioportal.cmo.pipelines.cvr.model.GMLCnvIntragenicVariant;

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
 * @author ochoaa
 */
public class GMLSvDataReader implements ItemStreamReader<CVRSvRecord> {

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Value("#{jobParameters[privateDirectory]}")
    private String privateDirectory;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    private Set<String> gmlSvSeen = new HashSet<>();
    private List<CVRSvRecord> gmlSvRecords = new ArrayList<>();

    private Logger LOG = Logger.getLogger(GMLSvDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // load new germline svs from json
        processJsonFile();
        // load existing germline svs
        processGmlSvFile();

        // set sv file and header to write to for processor, writer
        ec.put("svsFilename", CVRUtilities.SV_FILE);
        ec.put("svHeader", CVRSvRecord.getFieldNames());
    }

    private void processJsonFile() {
        GMLData gmlData = new GMLData();
        // load gml cvr data from cvr_gml_data.json file
        File cvrGmlFile =  new File(privateDirectory, CVRUtilities.GML_FILE);
        try {
            gmlData = cvrUtilities.readGMLJson(cvrGmlFile);
        } catch (IOException ex) {
            LOG.error("Error reading file: " + cvrGmlFile);
            ex.printStackTrace();
        }
        // load all new sv germline events from json
        for (GMLResult result : gmlData.getResults()) {
            if (result.getCnvIntragenicVariantsGml().isEmpty()) {
                continue;
            }
            // build germline sv records from cnv intragenic events for each patient sample
            String patientId = result.getMetaData().getDmpPatientId();
            List<String> samples = cvrSampleListUtil.getGmlPatientSampleMap().get(patientId);
            if (samples != null) {
                for (GMLCnvIntragenicVariant cnv : result.getCnvIntragenicVariantsGml()) {
                    for (String sampleId : samples) {
                        CVRSvRecord record = new CVRSvRecord(cnv, sampleId);
                        String sv = getGmlSvKey(record);
                        // skip if already added sv event for sample
                        if (!gmlSvSeen.add(sv)) {
                            continue;
                        }
                        gmlSvRecords.add(record);
                    }
                }
            }
        }
    }

    private void processGmlSvFile() {
        File gmlSvFile = new File(stagingDirectory, CVRUtilities.SV_FILE);
        if (!gmlSvFile.exists()) {
            LOG.info("File does not exist - skipping data loading from sv file: " + gmlSvFile.getName());
            return;
        }
        LOG.info("Loading germline sv data from: " + gmlSvFile.getName());

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<CVRSvRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new CVRSvFieldSetMapper());

        FlatFileItemReader<CVRSvRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(gmlSvFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.open(new ExecutionContext());

        try {
            CVRSvRecord to_add;
            while ((to_add = reader.read()) != null) {
                String patientId = cvrSampleListUtil.getSamplePatientId(to_add.getSample_ID());
                // check that matching patient id can be found from patient-sample mapping
                // and whether patient is in new dmp germline patients (to prevent duplicates)
                if (!Strings.isNullOrEmpty(patientId) && !cvrSampleListUtil.getNewDmpGmlPatients().contains(patientId)) {
                    String sv = getGmlSvKey(to_add);
                    if (gmlSvSeen.add(sv)) {
                        gmlSvRecords.add(to_add);
                    }
                }
            }
        } catch (Exception e){
            LOG.info("Error loading data from germline sv file: " + gmlSvFile.getName());
            throw new ItemStreamException(e);
        }
        reader.close();
    }

    private String getGmlSvKey(CVRSvRecord record) {
        return record.getSample_ID() + "|" + record.getSite1_Hugo_Symbol() + "|" + record.getSite2_Hugo_Symbol() + "|" + record.getEvent_Info();
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRSvRecord read() throws Exception {
        while (!gmlSvRecords.isEmpty()) {
            CVRSvRecord record = gmlSvRecords.remove(0);
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getSample_ID())) {
                cvrSampleListUtil.addSampleRemoved(record.getSample_ID());
                continue;
            }
            return record;
        }
        return null;
    }

}
