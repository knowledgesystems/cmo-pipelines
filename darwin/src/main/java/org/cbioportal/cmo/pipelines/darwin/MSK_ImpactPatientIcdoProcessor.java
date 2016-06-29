/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientIcdoRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class MSK_ImpactPatientIcdoProcessor implements ItemProcessor<MSK_ImpactPatientIcdoRecord, String>{
    ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String process(final MSK_ImpactPatientIcdoRecord darwinPatientIcdoRecord) throws Exception{
        List<String> record = new ArrayList<>();
        for(String field : new MSK_ImpactPatientIcdoRecord().getFieldNames()){
            record.add(darwinPatientIcdoRecord.getClass().getMethod("get"+field).invoke(darwinPatientIcdoRecord).toString());
        }
        
        return StringUtils.join(record, "\t");
    }
}
