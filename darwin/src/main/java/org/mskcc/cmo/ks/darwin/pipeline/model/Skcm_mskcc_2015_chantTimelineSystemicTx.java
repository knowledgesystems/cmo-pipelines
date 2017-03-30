/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.darwin.pipeline.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantTimelineSystemicTx implements Skcm_mskcc_2015_chantTimelineRecord {
    
    private String melstPtid;
    private String melstSystxTypDesc;
    private String melstSystxTypeOth;
    private Integer melstSystxStrtYear;
    private Integer melstSystxEndYear;
    private Integer melstSystxDaysDuration;  
    private List<String> EXCLUDED_TREATMENTS = Arrays.asList("Vaccine, please specify", "CombChemo, please specify", "Other, please specify", "HILP/ILI, please specify");

    public Skcm_mskcc_2015_chantTimelineSystemicTx() {}
    
    public Skcm_mskcc_2015_chantTimelineSystemicTx(String melstPtid,
            String melstSystxTypDesc,
            String melstSystxTypeOth,
            Integer melstSystxStrtYear,
            Integer melstSystxEndYear,
            Integer melstSystxDaysDuration
            ) {
        this.melstPtid = StringUtils.isNotEmpty(melstPtid) ? melstPtid : "NA";
        this.melstSystxTypDesc = StringUtils.isNotEmpty(melstSystxTypDesc) ? melstSystxTypDesc : "NA";
        this.melstSystxTypeOth = StringUtils.isNotEmpty(melstSystxTypeOth) ? melstSystxTypeOth : "NA";
        this.melstSystxStrtYear = melstSystxStrtYear != null ? melstSystxStrtYear : -1;
        this.melstSystxEndYear = melstSystxEndYear != null ? melstSystxEndYear : -1;
        this.melstSystxDaysDuration = melstSystxDaysDuration != null ? melstSystxDaysDuration : -1;
    }        
    
    public String getMELST_PTID() {
        return melstPtid;
    }
    
    public void setMELST_PTID(String melstPtid) {
        this.melstPtid = melstPtid;
    }
    
    public String getMELST_SYSTX_TYP_DESC() {
        return melstSystxTypDesc;
    }
    
    public void setMELST_SYSTX_TYP_DESC(String melstSystxTypeDesc) {
        this.melstSystxTypDesc = melstSystxTypeDesc;
    }

    public String getMELST_SYSTX_TYPE_OTH() {
        return melstSystxTypeOth;
    }
    
    public void setMELST_SYSTX_TYPE_OTH(String melstSystxTypeOth) {
        this.melstSystxTypeOth = melstSystxTypeOth;
    }   
    
    public Integer getMELST_SYSTX_STRT_YEAR() {
        return melstSystxStrtYear;
    }
    
    public void setMELST_SYSTX_STRT_YEAR(Integer melstSystxStrtYear) {
        this.melstSystxStrtYear = melstSystxStrtYear;
    }       
    
    public Integer getMELST_SYSTX_END_YEAR() {
        return melstSystxEndYear;
    }
    
    public void setMELST_SYSTX_END_YEAR(Integer melstSystxEndYear) {
        this.melstSystxEndYear = melstSystxEndYear;
    } 

    public Integer getMELST_SYSTX_DAYS_DURATION() {
        return melstSystxDaysDuration;
    }
    
    public void setMELST_SYSTX_DAYS_DURATION(Integer melstSystxDaysDuration) {
        this.melstSystxDaysDuration = melstSystxDaysDuration;
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
        return melstPtid;
    }
    
    // due to insufficient data in darwin, start date is 0
    @Override
    public String getSTART_DATE() {
        return "0";
    }

    @Override
    public String getSTOP_DATE() {
        return String.valueOf(melstSystxDaysDuration);
    }

    @Override
    public String getEVENT_TYPE() {
        return "TREATMENT";
    }

    @Override
    public String getTREATMENT_TYPE() {
        return EXCLUDED_TREATMENTS.contains(melstSystxTypDesc) ? melstSystxTypeOth : melstSystxTypDesc;
    }

    @Override
    public String getSUBTYPE() {
        return "";
    }
}
