/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.darwin.pipeline.model;

/**
 *
 * @author heinsz
 */
public class Skcm_mskcc_2015_chantClinicalCompositeRecord {
    
    private Skcm_mskcc_2015_chantClinicalRecord record;
    private String sampleRecord;
    private String patientRecord;
    
    public Skcm_mskcc_2015_chantClinicalCompositeRecord() {}
    public Skcm_mskcc_2015_chantClinicalCompositeRecord(Skcm_mskcc_2015_chantClinicalRecord record) {
        this.record = record;
    }
    
    public Skcm_mskcc_2015_chantClinicalRecord getRecord() {
        return this.record;
    }
    
    public void setRecord(Skcm_mskcc_2015_chantClinicalRecord record) {
        this.record = record;
    }
    
    public String getSampleRecord() {
        return sampleRecord;
    }
    
    public void setSampleRecord(String sampleRecord) {
        this.sampleRecord = sampleRecord;
    }
    
    public String getPatientRecord() {
        return patientRecord;
    }
    
    public void setPatientRecord(String patientRecord) {
        this.patientRecord = patientRecord;
    }
}
