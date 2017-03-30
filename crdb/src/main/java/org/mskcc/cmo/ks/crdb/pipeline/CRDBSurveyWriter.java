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

package org.mskcc.cmo.ks.crdb;

import org.mskcc.cmo.ks.crdb.model.CRDBSurvey;

import org.springframework.core.io.*;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * Class for writing the CRDB Survey results to the staging file.
 * 
 * @author ochoaa
 */

public class CRDBSurveyWriter implements ItemStreamWriter<String>
{   
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    @Value("${crdb.survey_filename}")
    private String surveyFilename;

    private final List<String> writeList = new ArrayList<>();
    private final FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
            @Override
            public void writeHeader(Writer writer) throws IOException {
                writer.write(normalizeHeaders(new CRDBSurvey().getFieldNames()));
            }
        });
        
        if (stagingDirectory.endsWith("/")){
            stagingFile = stagingDirectory+surveyFilename;
        }
        else{
            stagingFile = stagingDirectory+"/"+surveyFilename;
        }
        flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
        flatFileItemWriter.open(executionContext);
    }
    
    private String normalizeHeaders(List<String> columns) {        
        List<String> normColumns = new ArrayList<>();
        for (String col : columns){
            if (col.equals("DMP_ID")){
                normColumns.add("PATIENT_ID");
            }
            else if (col.equals("COMMENTS")) {
                normColumns.add("CRDB_SURVEY_"+col);
            }
            else if (!col.equals("QS_DATE")){
                normColumns.add("CRDB_"+col);
            }
        }
        return StringUtils.join(normColumns, "\t");
    }
            
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {
        flatFileItemWriter.close();
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        writeList.clear();
        List<String> writeList = new ArrayList<>();
        for (String result : items) {
            writeList.add(result);
        }
        flatFileItemWriter.write(writeList);
    }
}
