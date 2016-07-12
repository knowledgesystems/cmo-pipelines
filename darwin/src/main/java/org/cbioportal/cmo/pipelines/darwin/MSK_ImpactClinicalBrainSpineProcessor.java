/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactClinicalBrainSpine;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class MSK_ImpactClinicalBrainSpineProcessor implements ItemProcessor<MSK_ImpactClinicalBrainSpine, String>{
    
    @Override
    public String process(final MSK_ImpactClinicalBrainSpine darwinClinicalBrainSpine) throws Exception{
        List<String> record = new ArrayList<>();
        for(String field : new MSK_ImpactClinicalBrainSpine().getFieldNames()){
            record.add(darwinClinicalBrainSpine.getClass().getMethod("get"+field).invoke(darwinClinicalBrainSpine).toString());
        }
        
        return StringUtils.join(record, "\t");
    }
    
}
