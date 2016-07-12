/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;
import org.cbioportal.cmo.pipelines.darwin.model.StudyIDRecord;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
/**
 *
 * @author jake
 */
public class StudyIDProcessor implements ItemProcessor<StudyIDRecord, String>{
    
    @Override
    public String process(final StudyIDRecord studyIDResults) throws Exception{
        List<String> record = new ArrayList<>();
        for(String field : new StudyIDRecord().getFieldNames()){
            record.add(studyIDResults.getClass().getMethod("get"+field).invoke(studyIDResults).toString());
        }
        
        return StringUtils.join(record, "\t");
    }
}
