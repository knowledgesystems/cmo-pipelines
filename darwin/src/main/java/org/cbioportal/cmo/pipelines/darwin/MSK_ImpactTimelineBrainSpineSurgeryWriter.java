/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;

import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.core.io.*;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;

/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineSurgeryWriter implements ItemStreamWriter<TimelineBrainSpineComposite>{
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    private List<String> writeList = new ArrayList<>();
    private final FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        stagingFile = stagingDirectory + File.separator + "data_timeline_surgery_caisis_gbm.txt";
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException{
                writer.write(StringUtils.join(new MSK_ImpactTimelineBrainSpine().getSurgeryHeaders(), "\t"));
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
            if (!result.getSurgeryResult().equals(TimelineBrainSpineComposite.NO_RESULT)) {
                writeList.add(result.getSurgeryResult());
            }
        }
        if(!writeList.isEmpty()){
            flatFileItemWriter.write(writeList);
        }
    }
}
