/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.StudyIDRecord;

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
public class StudyIDWriter implements ItemStreamWriter<String>{
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    @Value("${darwin.studyID_filename}")
    private String datasetFilename;
    
    private List<String> writeList = new ArrayList<>();
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
    private String stagingFile;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();
        flatFileItemWriter.setLineAggregator(aggr);
        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback(){
            @Override
            public void writeHeader(Writer writer) throws IOException{
                writer.write(StringUtils.join(new StudyIDRecord().getFieldNames(), "\t"));
            }
        });
        if(stagingDirectory.endsWith("/")){
            stagingFile = stagingDirectory + datasetFilename;
        }
        else{
            stagingFile = stagingDirectory + "/" + datasetFilename;
        }
        flatFileItemWriter.setResource(new FileSystemResource(stagingFile));
        flatFileItemWriter.open(executionContext);
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{
        flatFileItemWriter.close();
    }
    
    @Override
    public void write(List<? extends String> items) throws Exception{
        writeList.clear();
        List<String> writeList = new ArrayList<>();
        for(String result : items){
            writeList.add(result);
        }
        flatFileItemWriter.write(writeList);
    }
}
