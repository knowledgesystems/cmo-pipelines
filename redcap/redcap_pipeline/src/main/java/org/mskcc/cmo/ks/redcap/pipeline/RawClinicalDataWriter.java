/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.mskcc.cmo.ks.redcap.pipeline;

import java.io.*;
import java.util.*;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

public class RawClinicalDataWriter implements ItemStreamWriter<String> {

    @Value("#{jobParameters[directory]}")
    private String directory;

    @Value("#{stepExecutionContext['projectTitle']}")
    private String projectTitle;

    @Value("#{stepExecutionContext['fullHeader']}")
    private List<String> fullHeader;
    
    @Value("#{stepExecutionContext['writeRawClinicalData']}")
    private boolean writeRawClinicalData;

    @Autowired
    public ClinicalDataSource clinicalDataSource;

    private static final String OUTPUT_FILENAME_PREFIX = "data_clinical_";
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<String>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (writeRawClinicalData) {
            File stagingFile = new File(directory, OUTPUT_FILENAME_PREFIX + projectTitle + ".txt");
            PassThroughLineAggregator<String> aggr = new PassThroughLineAggregator<>();
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setResource( new FileSystemResource(stagingFile));
            flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
                @Override
                public void writeHeader(Writer writer) throws IOException {
                    writer.write(getMetaLine(fullHeader));
                }
            });
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
        if (writeRawClinicalData) {
            flatFileItemWriter.close();
        }        
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        if (writeRawClinicalData) {
            flatFileItemWriter.write(items);
        }        
    }

    private String getMetaLine(List<String> sampleMetadata) {
        int sidIndex = sampleMetadata.indexOf("SAMPLE_ID");
        int pidIndex = sampleMetadata.indexOf("PATIENT_ID");
        StringBuilder metaLine = new StringBuilder();
        if (sidIndex != -1) {
            metaLine.append(sampleMetadata.get(sidIndex));
        }
        if (pidIndex != -1) {
            if (metaLine.toString().length() != 0) {
                metaLine.append("\t");
            }
            metaLine.append(sampleMetadata.get(pidIndex));
        }
        int index = 0;
        for (String item : sampleMetadata) {
            if (index != sidIndex && index != pidIndex) {
                if (metaLine.toString().length() != 0) {
                    metaLine.append("\t");
                }
                metaLine.append(item);
            }
            index = index + 1;
        }
        return metaLine.toString();
    }

}
