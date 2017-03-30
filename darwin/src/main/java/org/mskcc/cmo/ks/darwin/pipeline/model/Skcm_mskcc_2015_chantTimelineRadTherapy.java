/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.darwin.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantTimelineRadTherapy implements Skcm_mskcc_2015_chantTimelineRecord {
    
    private String melrtPtid;
    private String melrtRttxTypeDesc;
    private String melrtRttxAdjDesc;
    private Integer melrtRttxStrtYear;
    private Integer melrtRttxEndDt;
    private Integer melatRttxDaysDuration;

    public Skcm_mskcc_2015_chantTimelineRadTherapy() {}

    public Skcm_mskcc_2015_chantTimelineRadTherapy(String melrtPtid,
            String melrtRttxTypeDesc,
            String melrtRttxAdjDesc,
            Integer melrtRttxStrtYear,
            Integer melrtRttxEndDt,
            Integer melatRttxDaysDuration
            ) {
        this.melrtPtid = StringUtils.isNotEmpty(melrtPtid) ? melrtPtid : "NA";
        this.melrtRttxTypeDesc = StringUtils.isNotEmpty(melrtRttxTypeDesc) ? melrtRttxTypeDesc : "NA";
        this.melrtRttxAdjDesc = StringUtils.isNotEmpty(melrtRttxAdjDesc) ? melrtRttxAdjDesc : "NA";
        this.melrtRttxStrtYear = melrtRttxStrtYear != null ? melrtRttxStrtYear : -1;
        this.melrtRttxEndDt = melrtRttxEndDt != null ? melrtRttxEndDt : -1;
        this.melatRttxDaysDuration = melatRttxDaysDuration != null ? melatRttxDaysDuration : -1;
    }        
    
    public String getMELRT_PTID() {
        return melrtPtid;
    }
    
    public void setMELRT_PTID(String melrtPtid) {
        this.melrtPtid = melrtPtid;
    }
    
    public String getMELRT_RTTX_TYPE_DESC() {
        return melrtRttxTypeDesc;
    }
    
    public void setMELRT_RTTX_TYPE_DESC(String melrtRttxTypeDesc) {
        this.melrtRttxTypeDesc = melrtRttxTypeDesc;
    }

    public String getMELRT_RTTX_ADJ_DESC() {
        return melrtRttxAdjDesc;
    }
    
    public void setMELRT_RTTX_ADJ_DESC(String melrtRttxAdjDesc) {
        this.melrtRttxAdjDesc = melrtRttxAdjDesc;
    }   
    
    public Integer getMELRT_RTTX_STRT_YEAR() {
        return melrtRttxStrtYear;
    }
    
    public void setMELRT_RTTX_STRT_YEAR(Integer melrtRttxStrtYear) {
        this.melrtRttxStrtYear = melrtRttxStrtYear;
    }       
    
    public Integer getMELRT_RTTX_END_DT() {
        return melrtRttxEndDt;
    }
    
    public void setMELRT_RTTX_END_DT(Integer melrtRttxEndDt) {
        this.melrtRttxEndDt = melrtRttxEndDt;
    } 

    public Integer getMELAT_RTTX_DAYS_DURATION() {
        return melatRttxDaysDuration;
    }
    
    public void setMELAT_RTTX_DAYS_DURATION(Integer melatRttxDaysDuration) {
        this.melatRttxDaysDuration = melatRttxDaysDuration;
    }     
    
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
                
        return fieldNames;
    }    

    @Override
    public String getPATIENT_ID() {
        return melrtPtid;
    }
    
    // due to insufficient data in darwin, start date is 0
    @Override
    public String getSTART_DATE() {
        return "0";
    }

    @Override
    public String getSTOP_DATE() {
        return String.valueOf(melatRttxDaysDuration);
    }

    @Override
    public String getEVENT_TYPE() {
        return "TREATMENT";
    }

    @Override
    public String getTREATMENT_TYPE() {
        return melrtRttxTypeDesc;
        
    }

    @Override
    public String getSUBTYPE() {
        return melrtRttxAdjDesc;
    }
    
}
