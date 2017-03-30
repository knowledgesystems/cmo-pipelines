/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class TimelineProcessor implements ItemProcessor<Map<String, String>, String> {
    
    @Value("#{stepExecutionContext['combinedHeader']}")
    List<String> header;
    
    @Override
    public String process(Map<String, String> i) throws Exception {
        List<String> record = new ArrayList();
        
        if(header.contains("SAMPLE_ID")) {
            record.add(i.get("SAMPLE_ID"));
        }
        record.add(i.get("PATIENT_ID"));
        
        // get the sample and patient ids first before processing the other columns                   
        for (String column : header) {
            if(!column.equals("PATIENT_ID") && !column.equals("SAMPLE_ID") && !column.equals("RECORD_ID")) {
                record.add(i.getOrDefault(column, ""));
            }
        }        
        return StringUtils.join(record, "\t");
    }
}
