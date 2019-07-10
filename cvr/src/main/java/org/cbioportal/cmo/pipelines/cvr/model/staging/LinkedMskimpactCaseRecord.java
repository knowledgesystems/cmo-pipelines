/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cbioportal.cmo.pipelines.cvr.model.staging;

import java.util.*;

/**
 *
 * @author heinsz
 */
public class LinkedMskimpactCaseRecord {

    private String sampleId;
    private String linkedMskImpactCase;

    public LinkedMskimpactCaseRecord() {}

    public LinkedMskimpactCaseRecord(String sampleId, String linkedMskImpactCase) {
        this.sampleId = sampleId;
        this.linkedMskImpactCase = linkedMskImpactCase;
    }

    /**
     * @return the sampleId
     */
    public String getSAMPLE_ID() {
        return sampleId;
    }

    /**
     * @param sampleId the sampleId to set
     */
    public void setSAMPLE_ID(String sampleId) {
        this.sampleId = sampleId;
    }

    /**
     * @return the linkedMskImpactCase
     */
    public String getLINKED_MSKIMPACT_CASE() {
        return linkedMskImpactCase != null ? linkedMskImpactCase : "";
    }

    /**
     * @param linkedMskImpactCase the linkedMskImpactCase to set
     */
    public void setLINKED_MSKIMPACT_CASE(String linkedMskImpactCase) {
        this.linkedMskImpactCase = linkedMskImpactCase;
    }

    /**
     * @return the fieldNames
     */
    public static List<String> getFieldNames() {
        return Arrays.asList("SAMPLE_ID", "LINKED_MSKIMPACT_CASE");
    }
}
