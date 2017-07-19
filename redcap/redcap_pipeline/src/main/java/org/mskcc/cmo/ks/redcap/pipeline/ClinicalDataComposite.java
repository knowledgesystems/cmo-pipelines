/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.*;

/**
 *
 * @author heinsz
 */
public class ClinicalDataComposite {
    private String patientResult = "";
    private String sampleResult = "";
    private Map<String, String> data;

    public ClinicalDataComposite(Map<String, String> data) {
        this.data = data;
    }

    public void setPatientResult(String patientResult) {
        this.patientResult = patientResult;
    }

    public void setSampleResult(String sampleResult) {
        this.sampleResult = sampleResult;
    }

    public String getSampleResult() {
        return sampleResult;
    }

    public String getPatientResult() {
        return patientResult;
    }

    public Map<String, String> getData() {
        return data;
    }

}
