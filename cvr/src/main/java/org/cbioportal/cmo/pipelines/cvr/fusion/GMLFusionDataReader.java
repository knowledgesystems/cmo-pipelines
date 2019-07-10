/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

import com.mysql.jdbc.StringUtils;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRFusionRecord;
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
public class GMLFusionDataReader implements ItemStreamReader<CVRFusionRecord> {

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    private Set<String> gmlFusionsSeen = new HashSet<>();
    private List<CVRFusionRecord> gmlFusionRecords = new ArrayList<>();

    private Logger LOG = Logger.getLogger(GMLFusionDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        // load new germline fusions from json
        processJsonFile();
        // load existing germline fusions
        processGmlFusionsFile();

        // set fusion file and header to write to for processor, writer
        ec.put("fusionsFilename", CVRUtilities.FUSION_GML_FILE);
        ec.put("fusionHeader", CVRFusionRecord.getGermlineFieldNames());
    }

    private void processJsonFile() {
        GMLData gmlData = new GMLData();
        // load gml cvr data from cvr_gml_data.json file
        File cvrGmlFile =  new File(stagingDirectory, CVRUtilities.GML_FILE);
        try {
            gmlData = cvrUtilities.readGMLJson(cvrGmlFile);
        } catch (IOException ex) {
            LOG.error("Error reading file: " + cvrGmlFile);
            ex.printStackTrace();
        }
        // load all new fusion germline events from json
        for (GMLResult result : gmlData.getResults()) {
            if (result.getCnvIntragenicVariantsGml().isEmpty()) {
                continue;
            }
            // build germline fusion records from cnv intragenic events for each patient sample
            String patientId = result.getMetaData().getDmpPatientId();
            List<String> samples = cvrSampleListUtil.getGmlPatientSampleMap().get(patientId);
            if (samples != null) {
                for (GMLCnvIntragenicVariant cnv : result.getCnvIntragenicVariantsGml()) {
                    for (String sampleId : samples) {
                        CVRFusionRecord record = new CVRFusionRecord(cnv, sampleId);
                        String fusion = getGmlFusionKey(record);
                        // skip if already added fusion event for sample
                        if (!gmlFusionsSeen.add(fusion)) {
                            continue;
                        }
                        gmlFusionRecords.add(record);
                    }
                }
            }
        }
    }

    private void processGmlFusionsFile() {
        File gmlFusionsFile = new File(stagingDirectory, CVRUtilities.FUSION_GML_FILE);
        if (!gmlFusionsFile.exists()) {
            LOG.info("File does not exist - skipping data loading from germline fusions file: " + gmlFusionsFile.getName());
            return;
        }
        LOG.info("Loading germline fusions data from: " + gmlFusionsFile.getName());

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<CVRFusionRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new CVRFusionFieldSetMapper());

        FlatFileItemReader<CVRFusionRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(gmlFusionsFile));
        reader.setLineMapper(mapper);
        reader.setLinesToSkip(1);
        reader.open(new ExecutionContext());

        try {
            CVRFusionRecord to_add;
            while ((to_add = reader.read()) != null) {
                String patientId = cvrSampleListUtil.getSamplePatientId(to_add.getTumor_Sample_Barcode());
                // check that matching patient id can be found from patient-sample mapping
                // and whether patient is in new dmp germline patients (to prevent duplicates)
                if (!StringUtils.isNullOrEmpty(patientId) && !cvrSampleListUtil.getNewDmpGmlPatients().contains(patientId)) {
                    String fusion = getGmlFusionKey(to_add);
                    if (gmlFusionsSeen.add(fusion)) {
                        gmlFusionRecords.add(to_add);
                    }
                }
            }
        } catch (Exception e){
            LOG.info("Error loading data from germline fusions file: " + gmlFusionsFile.getName());
            throw new ItemStreamException(e);
        }
        reader.close();
    }

    private String getGmlFusionKey(CVRFusionRecord record) {
        return record.getTumor_Sample_Barcode() + "|" + record.getHugo_Symbol() + "|" + record.getFusion();
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CVRFusionRecord read() throws Exception {
        while (!gmlFusionRecords.isEmpty()) {
            CVRFusionRecord record = gmlFusionRecords.remove(0);
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getTumor_Sample_Barcode())) {
                cvrSampleListUtil.addSampleRemoved(record.getTumor_Sample_Barcode());
                continue;
            }
            return record;
        }
        return null;
    }
    
}