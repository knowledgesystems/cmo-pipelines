/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.*;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.core.io.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineSpecimenWriter implements ItemStreamWriter<TimelineBrainSpineComposite>{
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    private List<String> writeList = new ArrayList<>();
    private final FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        stagingFile = stagingDirectory + File.separator + "data_timeline_specimen_caisis_gbm.txt";
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException{
                writer.write(StringUtils.join(new MSK_ImpactTimelineBrainSpine().getSpecimenHeaders(), "\t"));
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
    public void write(List<? extends TimelineBrainSpineComposite> items) throws Exception{
        writeList.clear();
        for (TimelineBrainSpineComposite result : items) {
            if (!result.getSpecimenResult().equals(TimelineBrainSpineComposite.NO_RESULT)) {
                writeList.add(result.getSpecimenResult());
            }
        }
        if(!writeList.isEmpty()){
            flatFileItemWriter.write(writeList);
        }
    }
}
