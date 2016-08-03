/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.darwin.model;

import java.util.*;
//import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;

/**
 *
 * @author jake
 */

public class TimelineBrainSpineComposite {
    
    private String statusResult;
    private String specimenResult;
    private String treatmentResult;
    private String imagingResult;
    private String surgeryResult;
    private MSK_ImpactTimelineBrainSpine record;
    
    public static final String NO_RESULT = "NO RESULT";

    public TimelineBrainSpineComposite(MSK_ImpactTimelineBrainSpine record){
        this.statusResult = NO_RESULT;
        this.specimenResult = NO_RESULT;
        this.treatmentResult = NO_RESULT;
        this.imagingResult = NO_RESULT;
        this.surgeryResult = NO_RESULT;
        this.record = record;
    }
    
    public String getSurgeryResult(){
        return surgeryResult;
    }
    public void setSurgeryResult(String s){
        this.surgeryResult = s;
    }
    public String getImagingResult(){
        return imagingResult;
    }
    public void setImagingResult(String s){
        this.imagingResult = s;
    }
    
    public String getStatusResult(){
        return statusResult;
    }
    public void setStatusResult(String s){
        this.statusResult = s;
    }
    
    public String getTreatmentResult(){
        return treatmentResult;
    }
    public void setTreatmentResult(String s){
        this.treatmentResult = s;
    }
    
    public String getSpecimenResult(){
        return specimenResult;
    }
    public void setSpecimenResult(String s){
        this.specimenResult = s;
    }
    
    public MSK_ImpactTimelineBrainSpine getRecord(){
        return this.record;
    }
    public void setRecord(MSK_ImpactTimelineBrainSpine record){
        this.record = record;
    }
    
    public List<String> getJointRecord(){
        List<String> jointRecord = new ArrayList();
        jointRecord.add(this.statusResult);
        jointRecord.add(this.specimenResult);
        jointRecord.add(this.treatmentResult);
        jointRecord.add(this.imagingResult);
        jointRecord.add(this.surgeryResult);
        return jointRecord;
    }
    
    
    
}
