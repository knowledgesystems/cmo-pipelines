/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientDemographics;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientIcdoRecord;

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
public class MSK_ImpactPatientDemographicsReader implements ItemStreamReader<MSK_ImpactPatientDemographics>{
    @Value("${darwin.demographics_view}")
    private String patientDemographicsView;
    
    @Value("${darwin.icdo_view}")
    private String patientIcdoView;
    
    @Autowired
    SQLQueryFactory darwinQueryFactory;
    
    private List<MSK_ImpactPatientDemographics> darwinDemographicsResults;
    private List<String> darwinDemographicsIDs = new ArrayList<>();
    private Integer missingTM_DX_YEAR = 0;
    
    Logger log = Logger.getLogger(MSK_ImpactPatientDemographicsReader.class);
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException{
        this.darwinDemographicsResults = getDarwinDemographicsResults();
    }
            
    @Transactional
    private List<MSK_ImpactPatientDemographics> getDarwinDemographicsResults(){
        log.info("Start of Darwin Patient Demographics View Import...");
        MSK_ImpactPatientDemographics qDPD = alias(MSK_ImpactPatientDemographics.class, patientDemographicsView);
        MSK_ImpactPatientIcdoRecord qDPIR = alias(MSK_ImpactPatientIcdoRecord.class, patientIcdoView);
        List<MSK_ImpactPatientDemographics> darwinDemographicsResults = darwinQueryFactory.selectDistinct(Projections.constructor(MSK_ImpactPatientDemographics.class,
                $(qDPD.getDMP_ID_DEMO()),
                $(qDPD.getGENDER()),
                $(qDPD.getRACE()),
                $(qDPD.getRELIGION()),
                $(qDPD.getVITAL_STATUS()),
                $(qDPD.getPT_BIRTH_YEAR()),
                $(qDPD.getPT_DEATH_YEAR()),
                $(qDPIR.getTM_DX_YEAR())))
                .from($(qDPD))
                .join($(qDPIR))
                .on($(qDPD.getDMP_ID_DEMO()).eq($(qDPIR.getDMP_ID_ICDO())))
                .orderBy($(qDPIR.getTM_DX_YEAR()).asc())
                .fetch();
        darwinDemographicsResults.addAll(darwinQueryFactory.select(Projections.constructor(MSK_ImpactPatientDemographics.class,
                $(qDPD.getDMP_ID_DEMO()),
                $(qDPD.getGENDER()),
                $(qDPD.getRACE()),
                $(qDPD.getRELIGION()),
                $(qDPD.getVITAL_STATUS()),
                $(qDPD.getPT_BIRTH_YEAR()),
                $(qDPD.getPT_DEATH_YEAR())))
                .from($(qDPD))
                .fetch());
        return darwinDemographicsResults;
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException{}
    
    @Override
    public void close() throws ItemStreamException{}
    
    @Override
    public MSK_ImpactPatientDemographics read() throws Exception{
        
        if(!darwinDemographicsResults.isEmpty()){
            //This logic flow is to ensure one record for each patient as multiple tumor years may exist for each ID
            if (!darwinDemographicsIDs.isEmpty()) {
                //Checks if next ID has been processed yet
                if (darwinDemographicsIDs.contains(darwinDemographicsResults.get(0).getDMP_ID_DEMO())) {
                    while (true) {
                        darwinDemographicsResults.remove(0);//Pops off any records already sent to processor
                        //Check for end of imports
                        if (darwinDemographicsResults.isEmpty()) {
                            log.info("Imported " + darwinDemographicsIDs.size() + " records from Demographics View.");
                            log.info(missingTM_DX_YEAR + " records missing TM_DX_YEAR!");
                            return null;
                        } 
                        //Checks if the new ID has been processed yet
                        else if (!darwinDemographicsIDs.contains(darwinDemographicsResults.get(0).getDMP_ID_DEMO())) {
                            break;
                        }
                    }
                }
                //Pop off to processor and add new ID to list
                if(darwinDemographicsResults.get(0).getTM_DX_YEAR().equals(-1)){
                    missingTM_DX_YEAR++;
                }
                darwinDemographicsIDs.add(darwinDemographicsResults.get(0).getDMP_ID_DEMO());
                return darwinDemographicsResults.remove(0);
            }
            //Pop off to processor and add new ID to list
            if(darwinDemographicsResults.get(0).getTM_DX_YEAR().equals(-1)){
                missingTM_DX_YEAR++;
            }
            darwinDemographicsIDs.add(darwinDemographicsResults.get(0).getDMP_ID_DEMO());
            return darwinDemographicsResults.remove(0);
        }
        log.info("Imported " + darwinDemographicsIDs.size() + " records from Demographics View.");
        log.info(missingTM_DX_YEAR + " records missing TM_DX_YEAR!");
        return null;
    }
}
