/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineStatusProcessor implements ItemProcessor<MSK_ImpactTimelineBrainSpine, TimelineBrainSpineComposite>{
    
    @Override
    public TimelineBrainSpineComposite process(final MSK_ImpactTimelineBrainSpine timelineBrainSpine) throws Exception{
        TimelineBrainSpineComposite composite = new TimelineBrainSpineComposite(timelineBrainSpine);
        List<String> recordPost = new ArrayList<>();
        for(String field : composite.getRecord().getStatusFields()){
            recordPost.add(composite.getRecord().getClass().getMethod("get" + field).invoke(composite.getRecord()).toString());
        }
        if(recordPost.contains("STATUS")){
            composite.setStatusResult(StringUtils.join(recordPost, "\t"));
        }
        
        return composite;
    }
}
