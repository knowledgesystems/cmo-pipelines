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
import org.apache.log4j.Logger;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author heinsz
 */
public class ClinicalPatientDataWriter implements ItemStreamWriter<ClinicalDataComposite> {

    @Value("#{jobParameters[directory]}")
    private String directory;

    @Value("#{jobParameters[mergeClinicalDataSources]}")
    private boolean mergeClinicalDataSources;

    @Value("#{stepExecutionContext['projectTitle']}")
    private String projectTitle;

    @Value("#{stepExecutionContext['writeClinicalPatient']}")
    private boolean writeClinicalPatient;

    @Value("#{stepExecutionContext['patientHeader']}")
    private Map<String, List<String>> header;

    private File stagingFile;
    private Set<String> patientsProcessed = new HashSet();
    private Set<String> skippedPatients = new HashSet();
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<String>();
    private List<String> writeList = new ArrayList<>();

    private final Logger log = Logger.getLogger(ClinicalPatientDataWriter.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (writeClinicalPatient) {
            String outputFilename = "data_clinical_patient";
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
                    writer.write("#" + getMetaLine(header.get("display_names")) + "\n");
                    writer.write("#" + getMetaLine(header.get("descriptions")) + "\n");
                    writer.write("#" +getMetaLine(header.get("datatypes")) + "\n");
                    writer.write("#" + getMetaLine(header.get("priorities")) + "\n");
                    writer.write(getMetaLine(header.get("header")));
                }
            });
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
        if (writeClinicalPatient) {
            ec.put("clinicalPatientFile", stagingFile.getName());
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (writeClinicalPatient) {
            flatFileItemWriter.close();
        }
    }

    @Override
    public void write(List<? extends ClinicalDataComposite> items) throws Exception {
        if (writeClinicalPatient) {
            writeList.clear();
            for (ClinicalDataComposite composite : items) {
                if (!patientsProcessed.add(composite.getData().get("PATIENT_ID"))) {
                    if (skippedPatients.add(composite.getData().get("PATIENT_ID"))) {
                        log.warn("Clinical data for patient " + composite.getData().get("PATIENT_ID")
                            + " has already been added - skipping record.");
                    }
                    continue;
                }
                writeList.add(composite.getPatientResult());
            }
            flatFileItemWriter.write(writeList);
        }
    }

    private String getMetaLine(List<String> metaData) {
        int pidIndex = header.get("header").indexOf("PATIENT_ID");
        return metaData.remove(pidIndex) + "\t" + StringUtils.join(metaData, "\t");
    }

}
