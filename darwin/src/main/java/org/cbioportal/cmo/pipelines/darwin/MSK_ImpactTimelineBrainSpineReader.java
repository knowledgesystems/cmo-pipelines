/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;

import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.log4j.Logger;

import java.util.*;
import static com.querydsl.core.alias.Alias.$;
import static com.querydsl.core.alias.Alias.alias;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpineReader implements ItemStreamReader<MSK_ImpactTimelineBrainSpine>{
    @Value("${darwin.timeline_view}")
    private String timelineBrainSpineView;
    
    @Autowired
    SQLQueryFactory darwinQueryFactory;
    
    private List<MSK_ImpactTimelineBrainSpine> darwinTimelineResults;
    
    Logger log = Logger.getLogger(MSK_ImpactTimelineBrainSpineReader.class);
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinTimelineResults = getDarwinTimelineResults();
    }
    
    @Transactional
    private List<MSK_ImpactTimelineBrainSpine> getDarwinTimelineResults(){
        log.info("Start of Darwin Timeline Brain Spine View import...");
        MSK_ImpactTimelineBrainSpine qDTR = alias(MSK_ImpactTimelineBrainSpine.class, timelineBrainSpineView);
        List<MSK_ImpactTimelineBrainSpine> darwinTimelineResults = darwinQueryFactory.select(Projections.constructor(MSK_ImpactTimelineBrainSpine.class, $(qDTR.getDMT_PATIENT_ID_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_MIN_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_MAX_BRAINSPINETMLN()), $(qDTR.getDMP_PATIENT_ID_COUNT_BRAINSPINETMLN()),
                        $(qDTR.getDMP_PATIENT_ID_ALL_BRAINSPINETMLN()), $(qDTR.getSTART_DATE()), $(qDTR.getSTOP_DATE()),
                        $(qDTR.getEVENT_TYPE()), $(qDTR.getTREATMENT_TYPE()), $(qDTR.getSUBTYPE()), $(qDTR.getAGENT()),
                        $(qDTR.getSPECIMEN_REFERENCE_NUMBER()), $(qDTR.getSPECIMEN_SITE()), $(qDTR.getSPECIMEN_TYPE()),
                        $(qDTR.getSTATUS()), $(qDTR.getKARNOFSKY_PERFORMANCE_SCORE()), $(qDTR.getSURGERY_DETAILS()),
                        $(qDTR.getEVENT_TYPE_DETAILED()), $(qDTR.getHISTOLOGY()), $(qDTR.getWHO_GRADE()),
                        $(qDTR.getMGMT_STATUS()), $(qDTR.getSOURCE_PATHOLOGY()), $(qDTR.getNOTE()), $(qDTR.getDIAGNOSTIC_TYPE()),
                        $(qDTR.getDIAGNOSTIC_TYPE_DETAILED()), $(qDTR.getSOURCE())))
                .from($(qDTR))
                .where($(qDTR.getDMT_PATIENT_ID_BRAINSPINETMLN()).isNotNull())
                .fetch();

        log.info("Imported " + darwinTimelineResults.size() + " records from Darwin Timeline Brain Spine View.");
        return darwinTimelineResults;
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{}
    
    @Override
    public MSK_ImpactTimelineBrainSpine read() throws Exception{
        if(!darwinTimelineResults.isEmpty()){
            return darwinTimelineResults.remove(0);
        }
        return null;
    }
}
