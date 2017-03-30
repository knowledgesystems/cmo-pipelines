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
public class Skcm_mskcc_2015_chantTimelineAdjuvantTx implements Skcm_mskcc_2015_chantTimelineRecord { 
    
    private String melatPtid;
    private Integer melatAdjtxTypCd;
    private String melatAdjtxTypDesc;
    private String melatAdjtxTypeOth;
    private Integer melatAdjtxStrtYear;
    private Integer melatAdjtxEndYear;
    private Integer melatAdjtxDaysDuration;
    
    public Skcm_mskcc_2015_chantTimelineAdjuvantTx() {}
    
    public Skcm_mskcc_2015_chantTimelineAdjuvantTx(String melatPtid, 
            Integer melatAdjtxTypCd, 
            String melatAdjtxTypDesc,
            String melatAdjtxTypeOth,
            Integer melatAdjtxStrtYear,
            Integer melatAdjtxEndYear,
            Integer melatAdjtxDaysDuration) {
        this.melatPtid = StringUtils.isNotEmpty(melatPtid) ? melatPtid : "NA";
        this.melatAdjtxTypCd = melatAdjtxTypCd != null ? melatAdjtxTypCd : -1;
        this.melatAdjtxTypDesc = StringUtils.isNotEmpty(melatAdjtxTypDesc) ? melatAdjtxTypDesc : "NA";
        this.melatAdjtxTypeOth = StringUtils.isNotEmpty(melatAdjtxTypeOth) ? melatAdjtxTypeOth : "NA";
        this.melatAdjtxStrtYear = melatAdjtxStrtYear != null ? melatAdjtxStrtYear : -1;
        this.melatAdjtxEndYear = melatAdjtxEndYear != null ? melatAdjtxEndYear : -1;
        this.melatAdjtxDaysDuration = melatAdjtxDaysDuration != null ? melatAdjtxDaysDuration : -1;
    }
    
    public String getMELAT_PTID() {
        return melatPtid;
    }
    
    public void setMELAT_PTID(String melatPtid) {
        this.melatPtid = melatPtid;
    }
    
    public Integer getMELAT_ADJTX_TYP_CD() {
        return melatAdjtxTypCd;
    }
    
    public void setMELAT_ADJTX_TYP_CD(Integer melatAdjtxTypCd) {
        this.melatAdjtxTypCd = melatAdjtxTypCd;
    }    
    
    public String getMELAT_ADJTX_TYP_DESC() {
        return melatAdjtxTypDesc;
    }
    
    public void setMELAT_ADJTX_TYP_DESC(String melatAdjtxTypDesc) {
        this.melatAdjtxTypDesc = melatAdjtxTypDesc;
    }    

    public String getMELAT_ADJTX_TYPE_OTH() {
        return melatAdjtxTypeOth;
    }
    
    public void setMELAT_ADJTX_TYPE_OTH(String melatAdjtxTypeOth) {
        this.melatAdjtxTypeOth = melatAdjtxTypeOth;
    }    

    public Integer getMELAT_ADJTX_STRT_YEAR() {
        return melatAdjtxStrtYear;
    }
    
    public void setMELAT_ADJTX_STRT_YEAR(Integer melatAdjtxStrtYear) {
        this.melatAdjtxStrtYear = melatAdjtxStrtYear;
    }        
    
    public Integer getMELAT_ADJTX_END_YEAR() {
        return melatAdjtxEndYear;
    }
    
    public void setMELAT_ADJTX_END_YEAR(Integer melatAdjtxEndYear) {
        this.melatAdjtxEndYear = melatAdjtxEndYear;
    }      
    
    public Integer getMELAT_ADJTX_DAYS_DURATION() {
        return melatAdjtxDaysDuration;
    }
    
    public void setMELAT_ADJTX_DAYS_DURATION(Integer melatAdjtxDaysDuration) {
        this.melatAdjtxDaysDuration = melatAdjtxDaysDuration;
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
        return melatPtid;
    }
    
    // due to insufficient data in darwin, start date is 0
    @Override
    public String getSTART_DATE() {
        return "0";
    }

    @Override
    public String getSTOP_DATE() {
        return String.valueOf(melatAdjtxDaysDuration);
    }

    @Override
    public String getEVENT_TYPE() {
        return "TREATMENT";
    }

    @Override
    public String getTREATMENT_TYPE() {
        return melatAdjtxTypDesc;
        
    }

    @Override
    public String getSUBTYPE() {
        return melatAdjtxTypeOth;
    }
}
