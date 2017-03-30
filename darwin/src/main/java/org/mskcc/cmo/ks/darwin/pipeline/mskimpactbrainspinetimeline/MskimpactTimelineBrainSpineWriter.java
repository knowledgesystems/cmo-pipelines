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
package org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline;

import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineCompositeTimeline;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactBrainSpineTimeline;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.core.io.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.darwin.pipeline.BatchConfiguration;

/**
 *
 * @author jake
 */
public class MskimpactTimelineBrainSpineWriter implements ItemStreamWriter<MskimpactBrainSpineCompositeTimeline>{
    private BatchConfiguration.BrainSpineTimelineType type;
    
    public MskimpactTimelineBrainSpineWriter(BatchConfiguration.BrainSpineTimelineType type) {
        this.type = type;
    }
    
    Logger log = Logger.getLogger(MskimpactTimelineBrainSpineWriter.class);
    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;
    
    private List<String> writeList = new ArrayList<>();
    private final FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        stagingFile = outputDirectory + File.separator + "data_timeline_" + type.toString().toLowerCase() + "_caisis_gbm.txt";
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException{
                MskimpactBrainSpineTimeline mskImpactBrainSpineTimeline = new MskimpactBrainSpineTimeline();
                try {
                    writer.write(StringUtils.join((List<String>)mskImpactBrainSpineTimeline.getClass().getMethod("get" + type.toString() + "Headers").invoke(mskImpactBrainSpineTimeline), "\t"));
                }
                catch(Exception e) {
                    throw new ItemStreamException(e);
                }                
            }
        });
        
        flatFileItemWriter.setResource(new FileSystemResource(this.stagingFile));
        flatFileItemWriter.open(executionContext);
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{
        flatFileItemWriter.close();
    }
    
    @Override
    public void write(List<? extends MskimpactBrainSpineCompositeTimeline> items) throws Exception{
        writeList.clear();
        for (MskimpactBrainSpineCompositeTimeline result : items) {
            if (!result.getClass().getMethod("get" + type.toString() + "Result").invoke(result).equals(MskimpactBrainSpineCompositeTimeline.NO_RESULT)) {
                writeList.add((String)result.getClass().getMethod("get" + type.toString() + "Result").invoke(result));
            }
        }
        if(!writeList.isEmpty()){
            flatFileItemWriter.write(writeList);
        }
    }
}
