/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical;

import org.mskcc.cmo.ks.darwin.pipeline.model.*;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.core.io.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.*;
import java.util.*;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantClinicalSampleWriter implements ItemStreamWriter<Skcm_mskcc_2015_chantClinicalCompositeRecord> {
    
    @Value("#{jobParameters[outputDirectory]}")
    private String stagingDirectory;    
    
    @Value("${darwin.skcm_mskcc_2015_chant_clinical_sample_filename}")
    private String filename;    
    
    @Autowired
    public MetadataManager metadataManager;
    
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;    
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException {
                List<String> header = new Skcm_mskcc_2015_chantClinicalRecord().getFieldNames();
                Map<String, List<String>> fullHeader = metadataManager.getFullHeader(header);
                writer.write("#" + getMetaLine(fullHeader.get("display_names"), fullHeader).replace("\n", "") + "\n");
                writer.write("#" + getMetaLine(fullHeader.get("descriptions"), fullHeader).replace("\n", "") + "\n");
                writer.write("#" + getMetaLine(fullHeader.get("datatypes"), fullHeader).replace("\n", "") + "\n");
                writer.write("#" + getMetaLine(fullHeader.get("priorities"), fullHeader).replace("\n", "") + "\n");
                writer.write( getMetaLine(fullHeader.get("header"), fullHeader).replace("\n", "") + "\n");
            }
        });
        stagingFile = Paths.get(stagingDirectory).resolve(filename).toString();
        flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
        flatFileItemWriter.open(executionContext);
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}
    
    @Override
    public void close() throws ItemStreamException {
        flatFileItemWriter.close();
    }
    
    @Override
    public void write(List<? extends Skcm_mskcc_2015_chantClinicalCompositeRecord> items) throws Exception {
        List<String> writeList = new ArrayList<>();
        for (Skcm_mskcc_2015_chantClinicalCompositeRecord result : items) {
            String sampleLine = result.getSampleRecord();
            writeList.add(sampleLine.replace("\n", ""));
        }
        
        flatFileItemWriter.write(writeList);
    }
    
    private String getMetaLine(List<String> metaData, Map<String, List<String>> header) {
        List<String> attributeTypes = header.get("attribute_types");
        List<String> header_values = header.get("header");
        List<String> metaDataToWrite = new ArrayList<>();
        for (int i = 0; i < metaData.size(); i++) {
            if (attributeTypes.get(i).equals("SAMPLE") || header_values.get(i).equals("PATIENT_ID")) {
                metaDataToWrite.add(metaData.get(i));
            }
        }
        
        return StringUtils.join(metaDataToWrite, "\t");        
    }
    
}
