/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;
import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineCompositeProcessor implements ItemProcessor<MSK_ImpactTimelineBrainSpine, String>{
    List<ItemProcessor> delegates = new ArrayList<>();
    List<String> record = new ArrayList<>();
    
    MSK_ImpactTimelineBrainSpineStatusProcessor processor1 = new MSK_ImpactTimelineBrainSpineStatusProcessor();
    MSK_ImpactTimelineBrainSpineSpecimenProcessor processor2 = new MSK_ImpactTimelineBrainSpineSpecimenProcessor();
    MSK_ImpactTimelineBrainSpineTreatmentProcessor processor3 = new MSK_ImpactTimelineBrainSpineTreatmentProcessor();
    MSK_ImpactTimelineBrainSpineImagingProcessor processor4 = new MSK_ImpactTimelineBrainSpineImagingProcessor();
    MSK_ImpactTimelineBrainSpineSurgeryProcessor processor5 = new MSK_ImpactTimelineBrainSpineSurgeryProcessor();
    CompositeItemProcessor compProcessor = new CompositeItemProcessor();
    
    @Override
    public String process(MSK_ImpactTimelineBrainSpine darwinTimelineBrainSpine) throws Exception{
        delegates.clear();
        delegates.add(processor1);
        delegates.add(processor2);
        delegates.add(processor3);
        delegates.add(processor4);
        delegates.add(processor5);
        TimelineBrainSpineComposite composite = new TimelineBrainSpineComposite(darwinTimelineBrainSpine);
        compProcessor.setDelegates(delegates);
        compProcessor.process(composite);
        return StringUtils.join(composite.getJointRecord(), "\n");
    }
}
