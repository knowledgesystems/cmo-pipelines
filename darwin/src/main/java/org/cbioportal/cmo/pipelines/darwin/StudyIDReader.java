/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;
import org.cbioportal.cmo.pipelines.darwin.model.StudyIDRecord;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
/**
 *
 * @author jake
 */
public class StudyIDReader implements ItemStreamReader<StudyIDRecord>{
    
    @Autowired
    SQLQueryFactory darwinQueryFactory;
    
    private List<StudyIDRecord> studyIDResults;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.studyIDResults = getStudyIDResults();
    }
    
    @Transactional
    public List<StudyIDRecord> getStudyIDResults(){
        return studyIDResults;
    }
    
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{}
    
    @Override
    public StudyIDRecord read() throws Exception{
        if(!studyIDResults.isEmpty()){
            return studyIDResults.remove(0);
        }
        return null;
    }
}
