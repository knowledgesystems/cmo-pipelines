/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;

import java.io.File;
import org.springframework.batch.item.*;

import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import org.springframework.batch.item.support.CompositeItemWriter;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineCompositeWriter implements ItemStreamWriter<TimelineBrainSpineComposite>{
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;
    
    @Value("${darwin.timeline_bs_status}")
    private String statusFilename;
    
    @Value("${darwin.timeline_bs_specimen}")
    private String specimenFilename;
    
    @Value("${darwin.timeline_bs_treatment}")
    private String treatmentFilename;
    
    @Value("${darwin.timeline_bs_imaging}")
    private String imagingFilename;
    
    @Value("${darwin.timeline_bs_surgery}")
    private String surgeryFilename;
    
    private final String seperator = File.separator;
    private String statusFile;
    private String specimenFile;
    private String treatmentFile;
    private String imagingFile;
    private String surgeryFile;
    
    List<ItemStreamWriter> delegates = new ArrayList<>();
    MSK_ImpactTimelineBrainSpineStatusWriter statusWriter = new MSK_ImpactTimelineBrainSpineStatusWriter();
    MSK_ImpactTimelineBrainSpineSpecimenWriter specimenWriter = new MSK_ImpactTimelineBrainSpineSpecimenWriter();
    MSK_ImpactTimelineBrainSpineTreatmentWriter treatmentWriter = new MSK_ImpactTimelineBrainSpineTreatmentWriter();
    MSK_ImpactTimelineBrainSpineImagingWriter imagingWriter = new MSK_ImpactTimelineBrainSpineImagingWriter();
    MSK_ImpactTimelineBrainSpineSurgeryWriter surgeryWriter = new MSK_ImpactTimelineBrainSpineSurgeryWriter();
    CompositeItemWriter compWriter = new CompositeItemWriter();
    @Override
    public void close() throws ItemStreamException{
        statusWriter.close();
        specimenWriter.close();
        treatmentWriter.close();
        imagingWriter.close();
        surgeryWriter.close();
        compWriter.close();
    }
        
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {

        statusFile = stagingDirectory + seperator + statusFilename;
        specimenFile = stagingDirectory + seperator + specimenFilename;
        treatmentFile = stagingDirectory + seperator + treatmentFilename;
        imagingFile = stagingDirectory + seperator + imagingFilename;
        surgeryFile = stagingDirectory + seperator + surgeryFilename;
        
        statusWriter.setStagingFile(statusFile);
        specimenWriter.setStagingFile(specimenFile); 
        treatmentWriter.setStagingFile(treatmentFile);
        imagingWriter.setStagingFile(imagingFile);
        surgeryWriter.setStagingFile(surgeryFile);
        statusWriter.open(executionContext);
        specimenWriter.open(executionContext);
        treatmentWriter.open(executionContext);
        imagingWriter.open(executionContext);
        surgeryWriter.open(executionContext);
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void write(List<? extends TimelineBrainSpineComposite> items) throws Exception{
        delegates.clear();
        delegates.add(statusWriter);
        delegates.add(specimenWriter);
        delegates.add(treatmentWriter);
        delegates.add(imagingWriter);
        delegates.add(surgeryWriter);
        compWriter.setDelegates(delegates);
        compWriter.write(items);
    }
    
}
