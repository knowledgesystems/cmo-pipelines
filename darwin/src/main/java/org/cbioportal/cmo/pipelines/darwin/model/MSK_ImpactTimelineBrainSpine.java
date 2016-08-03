/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
/**
 *
 * @author jake
 */
public class MSK_ImpactTimelineBrainSpine {
    private String DMT_PATIENT_ID_BRAINSPINETMLN;
    private String DMP_PATIENT_ID_MIN_BRAINSPINETMLN;
    private String DMP_PATIENT_ID_MAX_BRAINSPINETMLN;
    private String DMP_PATIENT_ID_COUNT_BRAINSPINETMLN;
    private String DMP_PATIENT_ID_ALL_BRAINSPINETMLN;
    private String START_DATE;
    private String STOP_DATE;
    private String EVENT_TYPE;
    private String TREATMENT_TYPE;
    private String SUBTYPE;
    private String AGENT;
    private String SPECIMEN_REFERENCE_NUMBER;
    private String SPECIMEN_SITE;
    private String SPECIMEN_TYPE;
    private String STATUS;
    private String KARNOFSKY_PERFORMANCE_SCORE;
    private String SURGERY_DETAILS;
    private String EVENT_TYPE_DETAILED;
    private String HISTOLOGY;
    private String WHO_GRADE;
    private String MGMT_STATUS;
    private String SOURCE_PATHOLOGY;
    private String NOTE;
    private String DIAGNOSTIC_TYPE;
    private String DIAGNOSTIC_TYPE_DETAILED;
    private String SOURCE;
    private Map<String, Object> additionalProperties = new HashMap<>();
    
    public MSK_ImpactTimelineBrainSpine(){}
    
    public MSK_ImpactTimelineBrainSpine(String DMT_PATIENT_ID_BRAINSPINETMLN, String DMP_PATIENT_ID_MIN_BRAINSPINETMLN,
        String DMP_PATIENT_ID_MAX_BRAINSPINETMLN,
        String DMP_PATIENT_ID_COUNT_BRAINSPINETMLN,
        String DMP_PATIENT_ID_ALL_BRAINSPINETMLN,
        String START_DATE,
        String STOP_DATE,
        String EVENT_TYPE,
        String TREATMENT_TYPE,
        String SUBTYPE,
        String AGENT,
        String SPECIMEN_REFERENCE_NUMBER,
        String SPECIMEN_SITE,
        String SPECIMEN_TYPE,
        String STATUS,
        String KARNOFSKY_PERFORMANCE_SCORE,
        String SURGERY_DETAILS,
        String EVENT_TYPE_DETAILED,
        String HISTOLOGY,
        String WHO_GRADE,
        String MGMT_STATUS,
        String SOURCE_PATHOLOGY,
        String NOTE,
        String DIAGNOSTIC_TYPE,
        String DIAGNOSTIC_TYPE_DETAILED,
        String SOURCE){
        this.DMT_PATIENT_ID_BRAINSPINETMLN  =  StringUtils.isNotEmpty(DMT_PATIENT_ID_BRAINSPINETMLN)  ? DMT_PATIENT_ID_BRAINSPINETMLN : "NA";
        this.DMP_PATIENT_ID_MIN_BRAINSPINETMLN  =  StringUtils.isNotEmpty(DMP_PATIENT_ID_MIN_BRAINSPINETMLN)  ? DMP_PATIENT_ID_MIN_BRAINSPINETMLN : "NA";
        this.DMP_PATIENT_ID_MAX_BRAINSPINETMLN  =  StringUtils.isNotEmpty(DMP_PATIENT_ID_MAX_BRAINSPINETMLN)  ? DMP_PATIENT_ID_MAX_BRAINSPINETMLN : "NA";
        this.DMP_PATIENT_ID_COUNT_BRAINSPINETMLN  =  StringUtils.isNotEmpty(DMP_PATIENT_ID_COUNT_BRAINSPINETMLN)  ? DMP_PATIENT_ID_COUNT_BRAINSPINETMLN : "NA";
        this.DMP_PATIENT_ID_ALL_BRAINSPINETMLN  =  StringUtils.isNotEmpty(DMP_PATIENT_ID_ALL_BRAINSPINETMLN)  ? DMP_PATIENT_ID_ALL_BRAINSPINETMLN : "NA";
        this.START_DATE  =  StringUtils.isNotEmpty(START_DATE)  ? START_DATE : "NA";
        this.STOP_DATE  =  StringUtils.isNotEmpty(STOP_DATE)  ? STOP_DATE : "NA";
        this.EVENT_TYPE  =  StringUtils.isNotEmpty(EVENT_TYPE)  ? EVENT_TYPE : "NA";
        this.TREATMENT_TYPE  =  StringUtils.isNotEmpty(TREATMENT_TYPE)  ? TREATMENT_TYPE : "NA";
        this.SUBTYPE  =  StringUtils.isNotEmpty(SUBTYPE)  ? SUBTYPE : "NA";
        this.AGENT  =  StringUtils.isNotEmpty(AGENT)  ? AGENT : "NA";
        this.SPECIMEN_REFERENCE_NUMBER =  StringUtils.isNotEmpty(SPECIMEN_REFERENCE_NUMBER)  ? SPECIMEN_REFERENCE_NUMBER : "NA";
        this.SPECIMEN_SITE =  StringUtils.isNotEmpty(SPECIMEN_SITE)  ? SPECIMEN_SITE : "NA";
        this.SPECIMEN_TYPE =  StringUtils.isNotEmpty(SPECIMEN_TYPE)  ? SPECIMEN_TYPE : "NA";
        this.STATUS =  StringUtils.isNotEmpty(STATUS)  ? STATUS : "NA";
        this.KARNOFSKY_PERFORMANCE_SCORE =  StringUtils.isNotEmpty(KARNOFSKY_PERFORMANCE_SCORE)  ? KARNOFSKY_PERFORMANCE_SCORE : "NA";
        this.SURGERY_DETAILS =  StringUtils.isNotEmpty(SURGERY_DETAILS)  ? SURGERY_DETAILS : "NA";
        this.EVENT_TYPE_DETAILED =  StringUtils.isNotEmpty(EVENT_TYPE_DETAILED)  ? EVENT_TYPE_DETAILED : "NA";
        this.HISTOLOGY =  StringUtils.isNotEmpty(HISTOLOGY)  ? HISTOLOGY : "NA";
        this.WHO_GRADE =  StringUtils.isNotEmpty(WHO_GRADE)  ? WHO_GRADE : "NA";
        this.MGMT_STATUS =  StringUtils.isNotEmpty(MGMT_STATUS)  ? MGMT_STATUS : "NA";
        this.SOURCE_PATHOLOGY =  StringUtils.isNotEmpty(SOURCE_PATHOLOGY)  ? SOURCE_PATHOLOGY : "NA";
        this.NOTE =  StringUtils.isNotEmpty(NOTE)  ? NOTE : "NA";
        this.DIAGNOSTIC_TYPE =  StringUtils.isNotEmpty(DIAGNOSTIC_TYPE)  ? DIAGNOSTIC_TYPE : "NA";
        this.DIAGNOSTIC_TYPE_DETAILED =  StringUtils.isNotEmpty(DIAGNOSTIC_TYPE_DETAILED)  ? DIAGNOSTIC_TYPE_DETAILED : "NA";
        this.SOURCE =  StringUtils.isNotEmpty(SOURCE)  ? SOURCE : "NA";
    }
    
    public String getDMT_PATIENT_ID_BRAINSPINETMLN(){
        return DMT_PATIENT_ID_BRAINSPINETMLN;
    }
    public void setDMT_PATIENT_ID_BRAINSPINETMLN(String DMT_PATIENT_ID_BRAINSPINETMLN){
        this.DMT_PATIENT_ID_BRAINSPINETMLN =  StringUtils.isNotEmpty(DMT_PATIENT_ID_BRAINSPINETMLN)  ? DMT_PATIENT_ID_BRAINSPINETMLN : "NA" ;
    }
    
    public String getDMP_PATIENT_ID_MIN_BRAINSPINETMLN(){
        return DMP_PATIENT_ID_MIN_BRAINSPINETMLN;
    }
    public void setDMP_PATIENT_ID_MIN_BRAINSPINETMLN(String DMP_PATIENT_ID_MIN_BRAINSPINETMLN){
        this.DMP_PATIENT_ID_MIN_BRAINSPINETMLN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_MIN_BRAINSPINETMLN)  ? DMP_PATIENT_ID_MIN_BRAINSPINETMLN : "NA" ;
    }
    
    public String getDMP_PATIENT_ID_MAX_BRAINSPINETMLN(){
        return DMP_PATIENT_ID_MAX_BRAINSPINETMLN;
    }
    public void setDMP_PATIENT_ID_MAX_BRAINSPINETMLN(String DMP_PATIENT_ID_MAX_BRAINSPINETMLN){
        this.DMP_PATIENT_ID_MAX_BRAINSPINETMLN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_MAX_BRAINSPINETMLN)  ? DMP_PATIENT_ID_MAX_BRAINSPINETMLN : "NA" ;
    }
    
    public String getDMP_PATIENT_ID_COUNT_BRAINSPINETMLN(){
        return DMP_PATIENT_ID_COUNT_BRAINSPINETMLN;
    }
    public void setDMP_PATIENT_ID_COUNT_BRAINSPINETMLN(String DMP_PATIENT_ID_COUNT_BRAINSPINETMLN){
        this.DMP_PATIENT_ID_COUNT_BRAINSPINETMLN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_COUNT_BRAINSPINETMLN)  ? DMP_PATIENT_ID_COUNT_BRAINSPINETMLN : "NA" ;
    }
    
    public String getDMP_PATIENT_ID_ALL_BRAINSPINETMLN(){
        return DMP_PATIENT_ID_ALL_BRAINSPINETMLN;
    }
    public void setDMP_PATIENT_ID_ALL_BRAINSPINETMLN(String DMP_PATIENT_ID_ALL_BRAINSPINETMLN){
        this.DMP_PATIENT_ID_ALL_BRAINSPINETMLN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_ALL_BRAINSPINETMLN)  ? DMP_PATIENT_ID_ALL_BRAINSPINETMLN : "NA" ;
    }
    
    public String getSTART_DATE(){
        return START_DATE;
    }
    public void setSTART_DATE(String START_DATE){
        this.START_DATE =  StringUtils.isNotEmpty(START_DATE)  ? START_DATE : "NA" ;
    }
    
    public String getSTOP_DATE(){
        return STOP_DATE;
    }
    public void setSTOP_DATE(String STOP_DATE){
        this.STOP_DATE =  StringUtils.isNotEmpty(STOP_DATE)  ? STOP_DATE : "NA" ;
    }
    
    public String getEVENT_TYPE(){
        return EVENT_TYPE;
    }
    public void setEVENT_TYPE(String EVENT_TYPE){
        this.EVENT_TYPE =  StringUtils.isNotEmpty(EVENT_TYPE)  ? EVENT_TYPE : "NA" ;
    }
    
    public String getTREATMENT_TYPE(){
        return TREATMENT_TYPE;
    }
    public void setTREATMENT_TYPE(String TREATMENT_TYPE){
        this.TREATMENT_TYPE =  StringUtils.isNotEmpty(TREATMENT_TYPE)  ? TREATMENT_TYPE : "NA" ;
    }
    
    public String getSUBTYPE(){
        return SUBTYPE;
    }
    public void setSUBTYPE(String SUBTYPE){
        this.SUBTYPE =  StringUtils.isNotEmpty(SUBTYPE)  ? SUBTYPE : "NA" ;
    }
    
    public String getAGENT(){
        return AGENT;
    }
    public void setAGENT(String AGENT){
        this.AGENT =  StringUtils.isNotEmpty(AGENT)  ? AGENT : "NA" ;
    }
    
    public String getSPECIMEN_REFERENCE_NUMBER(){
        return SPECIMEN_REFERENCE_NUMBER;
    }
    public void setSPECIMEN_REFERENCE_NUMBER(String SPECIMEN_REFERENCE_NUMBER){
        this.SPECIMEN_REFERENCE_NUMBER =  StringUtils.isNotEmpty(SPECIMEN_REFERENCE_NUMBER)  ? SPECIMEN_REFERENCE_NUMBER : "NA" ;
    }
    
    public String getSPECIMEN_SITE(){
        return SPECIMEN_SITE;
    }
    public void setSPECIMEN_SITE(String SPECIMEN_SITE){
        this.SPECIMEN_SITE =  StringUtils.isNotEmpty(SPECIMEN_SITE)  ? SPECIMEN_SITE : "NA" ;
    }
    
    public String getSPECIMEN_TYPE(){
        return SPECIMEN_TYPE;
    }
    public void setSPECIMEN_TYPE(String SPECIMEN_TYPE){
        this.SPECIMEN_TYPE =  StringUtils.isNotEmpty(SPECIMEN_TYPE)  ? SPECIMEN_TYPE : "NA" ;
    }
    
    public String getSTATUS(){
        return STATUS;
    }
    public void setSTATUS(String STATUS){
        this.STATUS =  StringUtils.isNotEmpty(STATUS)  ? STATUS : "NA" ;
    }
    
    public String getKARNOFSKY_PERFORMANCE_SCORE(){
        return KARNOFSKY_PERFORMANCE_SCORE;
    }
    public void setKARNOFSKY_PERFORMANCE_SCORE(String KARNOFSKY_PERFORMANCE_SCORE){
        this.KARNOFSKY_PERFORMANCE_SCORE =  StringUtils.isNotEmpty(KARNOFSKY_PERFORMANCE_SCORE)  ? KARNOFSKY_PERFORMANCE_SCORE : "NA" ;
    }
    
    public String getSURGERY_DETAILS(){
        return SURGERY_DETAILS;
    }
    public void setSURGERY_DETAILS(String SURGERY_DETAILS){
        this.SURGERY_DETAILS =  StringUtils.isNotEmpty(SURGERY_DETAILS)  ? SURGERY_DETAILS : "NA" ;
    }
    
    public String getEVENT_TYPE_DETAILED(){
        return EVENT_TYPE_DETAILED;
    }
    public void setEVENT_TYPE_DETAILED(String EVENT_TYPE_DETAILED){
        this.EVENT_TYPE_DETAILED =  StringUtils.isNotEmpty(EVENT_TYPE_DETAILED)  ? EVENT_TYPE_DETAILED : "NA" ;
    }
    
    public String getHISTOLOGY(){
        return HISTOLOGY;
    }
    public void setHISTOLOGY(String HISTOLOGY){
        this.HISTOLOGY =  StringUtils.isNotEmpty(HISTOLOGY)  ? HISTOLOGY : "NA" ;
    }
    
    public String getWHO_GRADE(){
        return WHO_GRADE;
    }
    public void setWHO_GRADE(String WHO_GRADE){
        this.WHO_GRADE =  StringUtils.isNotEmpty(WHO_GRADE)  ? WHO_GRADE : "NA" ;
    }
    
    public String getMGMT_STATUS(){
        return MGMT_STATUS;
    }
    public void setMGMT_STATUS(String MGMT_STATUS){
        this.MGMT_STATUS =  StringUtils.isNotEmpty(MGMT_STATUS)  ? MGMT_STATUS : "NA" ;
    }
    
    public String getSOURCE_PATHOLOGY(){
        return SOURCE_PATHOLOGY;
    }
    public void setSOURCE_PATHOLOGY(String SOURCE_PATHOLOGY){
        this.SOURCE_PATHOLOGY =  StringUtils.isNotEmpty(SOURCE_PATHOLOGY)  ? SOURCE_PATHOLOGY : "NA" ;
    }
    
    public String getNOTE(){
        return NOTE;
    }
    public void setNOTE(String NOTE){
        this.NOTE =  StringUtils.isNotEmpty(NOTE)  ? NOTE : "NA" ;
    }
    
    public String getDIAGNOSTIC_TYPE(){
        return DIAGNOSTIC_TYPE;
    }
    public void setDIAGNOSTIC_TYPE(String DIAGNOSTIC_TYPE){
        this.DIAGNOSTIC_TYPE =  StringUtils.isNotEmpty(DIAGNOSTIC_TYPE)  ? DIAGNOSTIC_TYPE : "NA" ;
    }
    
    public String getDIAGNOSTIC_TYPE_DETAILED(){
        return DIAGNOSTIC_TYPE_DETAILED;
    }
    public void setDIAGNOSTIC_TYPE_DETAILED(String DIAGNOSTIC_TYPE_DETAILED){
        this.DIAGNOSTIC_TYPE_DETAILED =  StringUtils.isNotEmpty(DIAGNOSTIC_TYPE_DETAILED)  ? DIAGNOSTIC_TYPE_DETAILED : "NA" ;
    }
    
    public String getSOURCE(){
        return SOURCE;
    }
    public void setSOURCE(String SOURCE){
        this.SOURCE =  StringUtils.isNotEmpty(SOURCE)  ? SOURCE : "NA" ;
    }
    
    public Map<String, Object> getAditionalProperties(){
        return this.additionalProperties;
    }
    public void setAdditionalProperty(String name, Object value){
        this.additionalProperties.put(name, value);
    }
    
    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }
    
    public List<String> getStatusFields(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("STATUS");
        fieldNames.add("KARNOFSKY_PERFORMANCE_SCORE");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
                
        return fieldNames;
    }
    public List<String> getStatusHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("STATUS");
        fieldNames.add("KARNOFSKY_PERFORMANCE_SCORE");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    
    public List<String> getSpecimenFields(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("SPECIMEN_REFERENCE_NUMBER");
        fieldNames.add("SPECIMEN_SITE");
        fieldNames.add("SPECIMEN_TYPE");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    public List<String> getSpecimenHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("SPECIMEN_REFERENCE_NUMBER");
        fieldNames.add("SPECIMEN_SITE");
        fieldNames.add("SPECIMEN_TYPE");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    
    public List<String> getTreatmentFields(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    public List<String> getTreatmentHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    
    public List<String> getImagingFields(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE_DETAILED");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    public List<String> getImagingHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE_DETAILED");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    
    public List<String> getSurgeryFields(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("SURGERY_DETAILS");
        fieldNames.add("EVENT_TYPE_DETAILED");
        fieldNames.add("HISTOLOGY");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("SOURCE_PATHOLOGY");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
    public List<String> getSurgeryHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("SURGERY_DETAILS");
        fieldNames.add("EVENT_TYPE_DETAILED");
        fieldNames.add("HISTOLOGY");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("SOURCE_PATHOLOGY");
        fieldNames.add("SOURCE");
        
        return fieldNames;
    }
        
}
