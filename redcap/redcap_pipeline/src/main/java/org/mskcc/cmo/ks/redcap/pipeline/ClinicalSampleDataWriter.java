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
package org.mskcc.cmo.ks.redcap.pipeline;

import java.io.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
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

/**
 *
 * @author heinsz
 */
public class ClinicalSampleDataWriter implements ItemStreamWriter<ClinicalDataComposite> {

    @Value("#{jobParameters[directory]}")
    private String directory;

    @Value("#{jobParameters[mergeClinicalDataSources]}")
    private boolean mergeClinicalDataSources;

    @Value("#{stepExecutionContext['projectTitle']}")
    private String projectTitle;

    @Value("#{stepExecutionContext['writeClinicalSample']}")
    private boolean writeClinicalSample;

    @Value("#{stepExecutionContext['sampleHeader']}")
    private Map<String, List<String>> sampleHeader;

    @Value("#{stepExecutionContext['patientHeader']}")
    private Map<String, List<String>> patientHeader;

    @Autowired
    public ClinicalDataSource clinicalDataSource;

    private File stagingFile;
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<String>();
    private List<String> writeList = new ArrayList<>();

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (writeClinicalSample) {
            String outputFilename = "data_clinical_sample";
            if (!mergeClinicalDataSources) {
                outputFilename = outputFilename + "_" + projectTitle;
            }
            outputFilename = outputFilename + ".txt";
            this.stagingFile = new File(directory, outputFilename);
            PassThroughLineAggregator aggr = new PassThroughLineAggregator();
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setResource( new FileSystemResource(stagingFile));
            flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
                @Override
                public void writeHeader(Writer writer) throws IOException {
                    writer.write("#" + getMetaLine(sampleHeader.get("display_names"), patientHeader.get("display_names")) + "\n");
                    writer.write("#" + getMetaLine(sampleHeader.get("descriptions"), patientHeader.get("descriptions")) + "\n");
                    writer.write("#" + getMetaLine(sampleHeader.get("datatypes"), patientHeader.get("datatypes")) + "\n");
                    writer.write("#" + getMetaLine(sampleHeader.get("priorities"), patientHeader.get("priorities")) + "\n");
                    writer.write(getMetaLine(sampleHeader.get("header"), patientHeader.get("header")));
                }
            });
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
        if (writeClinicalSample) {
            ec.put("clinicalSampleFile", stagingFile.getName());
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (writeClinicalSample) {
            flatFileItemWriter.close();
        }
    }

    @Override
    public void write(List<? extends ClinicalDataComposite> items) throws Exception {
        if (writeClinicalSample) {
            writeList.clear();
            for (ClinicalDataComposite composite : items) {
                writeList.add(composite.getSampleResult());
            }
            flatFileItemWriter.write(writeList);
        }
    }

    private String getMetaLine(List<String> sampleMetadata, List<String> patientMetadata) {
        int sidIndex = sampleHeader.get("header").indexOf("SAMPLE_ID");
        int pidIndex = patientHeader.get("header").indexOf("PATIENT_ID");
        return sampleMetadata.remove(sidIndex) + "\t" + patientMetadata.get(pidIndex) + "\t" + StringUtils.join(sampleMetadata, "\t");
    }
}
