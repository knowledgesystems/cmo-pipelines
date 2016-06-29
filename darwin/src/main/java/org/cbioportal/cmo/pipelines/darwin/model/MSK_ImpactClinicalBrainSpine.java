/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin.model;

import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;
/**
 *
 * @author jake
 */
public class MSK_ImpactClinicalBrainSpine {
    private String DMT_PATIENT_ID_BRAINSPINECLIN;
    private String DMP_PATIENT_ID_BRAINSPINECLIN;
    private String DMP_SAMPLE_ID_BRAINSPINECLIN;
    private String AGE;
    private String SEX;
    private String OS_STATUS;
    private String OS_MONTHS;
    private String DFS_STATUS;
    private String DFS_MONTHS;
    private String HISTOLOGY; 
    private String WHO_GRADE;
    private String MGMT_STATUS;
    
    public MSK_ImpactClinicalBrainSpine(){}
    
    public MSK_ImpactClinicalBrainSpine(
            String DMP_PATIENT_ID_BRAINSPINECLIN,
            String DMP_SAMPLE_ID_BRAINSPINECLIN,
            String AGE,
            String SEX,
            String OS_STATUS,
            String OS_MONTHS,
            String DFS_STATUS,
            String DFS_MONTHS,
            String HISTOLOGY,
            String WHO_GRADE,
            String MGMT_STATUS){
        
        this.DMP_PATIENT_ID_BRAINSPINECLIN = DMP_PATIENT_ID_BRAINSPINECLIN != null ? DMP_PATIENT_ID_BRAINSPINECLIN : "N/A";
        this.DMP_SAMPLE_ID_BRAINSPINECLIN = DMP_SAMPLE_ID_BRAINSPINECLIN != null ? DMP_SAMPLE_ID_BRAINSPINECLIN : "N/A";
        this.AGE = AGE != null ? AGE : "N/A";
        this.SEX = SEX != null ? SEX : "N/A";
        this.OS_STATUS = OS_STATUS != null ? OS_STATUS : "N/A";
        this.OS_MONTHS = OS_MONTHS != null ? OS_MONTHS : "N/A";
        this.DFS_STATUS = DFS_STATUS != null ? DFS_STATUS : "N/A";
        this.DFS_MONTHS = DFS_MONTHS != null ? DFS_MONTHS : "N/A";
        this.HISTOLOGY = HISTOLOGY != null ? HISTOLOGY : "N/A";
        this.WHO_GRADE = WHO_GRADE != null ? WHO_GRADE : "N/A";
        this.MGMT_STATUS = MGMT_STATUS != null ? MGMT_STATUS : "N/A";
    }
    
   

    public String getDMP_PATIENT_ID_BRAINSPINECLIN() {
        return DMP_PATIENT_ID_BRAINSPINECLIN;
    }

    public void setDMP_PATIENT_ID_BRAINSPINECLIN(String DMP_PATIENT_ID_BRAINSPINECLIN) {
        this.DMP_PATIENT_ID_BRAINSPINECLIN = DMP_PATIENT_ID_BRAINSPINECLIN != null ? DMP_PATIENT_ID_BRAINSPINECLIN : "N/A";
    }

    public String getDMP_SAMPLE_ID_BRAINSPINECLIN() {
        return DMP_SAMPLE_ID_BRAINSPINECLIN;
    }

    public void setDMP_SAMPLE_ID_BRAINSPINECLIN(String DMP_SAMPLE_ID_BRAINSPINECLIN) {
        this.DMP_SAMPLE_ID_BRAINSPINECLIN = DMP_SAMPLE_ID_BRAINSPINECLIN != null ? DMP_SAMPLE_ID_BRAINSPINECLIN : "N/A";
    }

    public String getAGE() {
        return AGE;
    }

    public void setAGE(String AGE) {
        this.AGE = AGE != null ? AGE : "N/A";
    }

    public String getSEX() {
        return SEX;
    }

    public void setSEX(String SEX) {
        this.SEX = SEX != null ? SEX : "N/A";
    }

    public String getOS_STATUS() {
        return OS_STATUS;
    }

    public void setOS_STATUS(String OS_STATUS) {
        this.OS_STATUS = OS_STATUS != null ? OS_STATUS : "N/A";
    }

    public String getOS_MONTHS() {
        return OS_MONTHS;
    }

    public void setOS_MONTHS(String OS_MONTHS) {
        this.OS_MONTHS = OS_MONTHS != null ? OS_MONTHS : "N/A";
    }

    public String getDFS_STATUS() {
        return DFS_STATUS;
    }

    public void setDFS_STATUS(String DFS_STATUS) {
        this.DFS_STATUS = DFS_STATUS != null ? DFS_STATUS : "N/A";
    }

    public String getDFS_MONTHS() {
        return DFS_MONTHS;
    }

    public void setDFS_MONTHS(String DFS_MONTHS) {
        this.DFS_MONTHS = DFS_MONTHS != null ? DFS_MONTHS : "N/A";
    }

    public String getHISTOLOGY() {
        return HISTOLOGY;
    }

    public void setHISTOLOGY(String HISTOLOGY) {
        this.HISTOLOGY = HISTOLOGY != null ? HISTOLOGY : "N/A";
    }

    public String getWHO_GRADE() {
        return WHO_GRADE;
    }

    public void setWHO_GRADE(String WHO_GRADE) {
        this.WHO_GRADE = WHO_GRADE != null ? WHO_GRADE : "N/A";
    }

    public String getMGMT_STATUS() {
        return MGMT_STATUS;
    }

    public void setMGMT_STATUS(String MGMT_STATUS) {
        this.MGMT_STATUS = MGMT_STATUS != null ? MGMT_STATUS : "N/A";
    }
    
    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }
    
    public List<String> getFieldNames(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_SAMPLE_ID_BRAINSPINECLIN");
        fieldNames.add("AGE");
        fieldNames.add("SEX");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");
        fieldNames.add("DFS_STATUS");
        fieldNames.add("DFS_MONTHS");
        fieldNames.add("HISTOLOGY");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("DMP_PATIENT_ID_BRAINSPINECLIN");
        
        return fieldNames;
    }
    public List<String> getHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("AGE");
        fieldNames.add("SEX");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");
        fieldNames.add("DFS_STATUS");
        fieldNames.add("DFS_MONTHS");
        fieldNames.add("HISTOLOGICAL_DIAGNOSIS");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("PATIENT_ID");
        
        return fieldNames;
    }


}
