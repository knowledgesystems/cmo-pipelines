/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactClinicalBrainSpine;

import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.log4j.Logger;

import java.util.*;
/**
 *
 * @author jake
 */
public class MSK_ImpactClinicalBrainSpineReader implements ItemStreamReader<MSK_ImpactClinicalBrainSpine>{
    @Value("${darwin.clinical_view}")
    private String clinicalBrainSpineView;
    
    @Autowired
    SQLQueryFactory darwinQueryFactory;
    
    private List<MSK_ImpactClinicalBrainSpine> clinicalBrainSpineResults;
    
    Logger log = Logger.getLogger(MSK_ImpactClinicalBrainSpineReader.class);
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.clinicalBrainSpineResults = getClinicalBrainSpineResults();
    }
    
    @Transactional
    private List<MSK_ImpactClinicalBrainSpine> getClinicalBrainSpineResults(){
        log.info("Start of Clinical Brain Spine View Import...");
        MSK_ImpactClinicalBrainSpine qCBSR = alias(MSK_ImpactClinicalBrainSpine.class, clinicalBrainSpineView);
        List<MSK_ImpactClinicalBrainSpine> clinicalBrainSpineResults = darwinQueryFactory.select(Projections.constructor(MSK_ImpactClinicalBrainSpine.class, 
                $(qCBSR.getDMP_PATIENT_ID_BRAINSPINECLIN()),
                $(qCBSR.getDMP_SAMPLE_ID_BRAINSPINECLIN()),
                $(qCBSR.getAGE()),
                $(qCBSR.getSEX()),
                $(qCBSR.getOS_STATUS()),
                $(qCBSR.getOS_MONTHS()),
                $(qCBSR.getDFS_STATUS()),
                $(qCBSR.getDFS_MONTHS()),
                $(qCBSR.getHISTOLOGY()),
                $(qCBSR.getWHO_GRADE()),
                $(qCBSR.getMGMT_STATUS())))
            .where($(qCBSR.getDMP_PATIENT_ID_BRAINSPINECLIN()).isNotEmpty())
            .from($(qCBSR))
            .fetch();
        
        log.info("Imported " + clinicalBrainSpineResults.size() + " records from Clinical Brain Spine View.");
        return clinicalBrainSpineResults;
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{}
    
    @Override
    public MSK_ImpactClinicalBrainSpine read() throws Exception{
        if(!clinicalBrainSpineResults.isEmpty()){
            return clinicalBrainSpineResults.remove(0);
        }
        return null;
    }
    
}
