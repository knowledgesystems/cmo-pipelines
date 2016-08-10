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

package org.cbioportal.cmo.pipelines.foundation;

import org.cbioportal.cmo.pipelines.foundation.model.CaseType;
import org.cbioportal.cmo.pipelines.foundation.model.staging.MutationData;

import com.google.common.base.Strings;
import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author Prithi Chakrapani, ochoaa
 */
public class MutationDataWriter implements ItemStreamWriter <CompositeResultBean>{    
    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;
    
    private static final Log LOG = LogFactory.getLog(MutationDataWriter.class);

    private List<String> writeList = new ArrayList<String>();
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<String>();
        
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {                
        // retrieve list of foundation cases from execution context
        final List<CaseType> fmiCaseList = (List<CaseType>) executionContext.get("fmiCaseList");
                
        String stagingFile = outputDirectory +  "data_mutations_extended.txt" ;
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
            @Override
            public void writeHeader(Writer writer) throws IOException {
                writer.write(getHeader(fmiCaseList));
            }
        });
        flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
        flatFileItemWriter.open(executionContext);
    }
    public String getHeader(List<CaseType> fmiCaseList){
        List<String> header = new ArrayList();        
        List <String> columns = new ArrayList();
        Map<String, String> map = new MutationData().getStagingMap();
        for (String key : map.keySet() ) {
            columns.add(key);
        }
        
        // format the MAF header
        header.add(getSequencedSamples(fmiCaseList));
        header.add(StringUtils.join(columns, "\t"));
        return StringUtils.join(header, "\n"); 
    }
    
    public String getSequencedSamples(List<CaseType> fmiCaseList) {
        List<String> sequencedSamples = new ArrayList();
        sequencedSamples.add("#sequenced_samples:");
        for (CaseType ct : fmiCaseList) {
            sequencedSamples.add(ct.getCase());
        }
        
        return StringUtils.join(sequencedSamples, " ");
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException
    {
        flatFileItemWriter.close();
    }

    @Override
    public void write(List<? extends CompositeResultBean> items) throws Exception
    {
        writeList.clear();
        List<String> writeList = new ArrayList<String>();
        for (CompositeResultBean resultList : items) {
            for (String result : resultList.getMutationDataResult().split("\n")) {
                if (!Strings.isNullOrEmpty(result)) {
                    writeList.add(result);
                }
            }            
        }
        
        flatFileItemWriter.write(writeList);
    }
}
