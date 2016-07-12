/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineSurgeryProcessor implements ItemProcessor<TimelineBrainSpineComposite, TimelineBrainSpineComposite>{
    
    @Override
    public TimelineBrainSpineComposite process(final TimelineBrainSpineComposite composite) throws Exception{
        List<String> recordPost = new ArrayList<>();
        for(String field : composite.getRecord().getSurgeryFields()){
            recordPost.add(composite.getRecord().getClass().getMethod("get" + field).invoke(composite.getRecord()).toString());
        }
        if(recordPost.contains("SURGERY")){
            composite.setSurgeryResult(StringUtils.join(recordPost, "\t"));
        }
        
        return composite;
    }
}