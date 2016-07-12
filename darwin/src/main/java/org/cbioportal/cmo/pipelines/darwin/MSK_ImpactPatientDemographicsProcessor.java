/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientDemographics;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;

/**
 *
 * @author jake
 */
public class MSK_ImpactPatientDemographicsProcessor implements ItemProcessor<MSK_ImpactPatientDemographics, String>{
    
    @Override
    public String process(final MSK_ImpactPatientDemographics darwinPatientDemographics) throws Exception{
        List<String> record = new ArrayList<>();
        for(String field : new MSK_ImpactPatientDemographics().getFieldNames()){
            record.add(darwinPatientDemographics.getClass().getMethod("get"+field).invoke(darwinPatientDemographics).toString());
        }
        
        return StringUtils.join(record, "\t");
    }
}
