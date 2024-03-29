/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.darwin.pipeline.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.cbioportal.cmo.pipelines.common.util.ClinicalValueUtil;

/**
 *
 * @author jake
 */
public class MskimpactBrainSpineTimeline {
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

    public MskimpactBrainSpineTimeline() {}

    public MskimpactBrainSpineTimeline(
            String DMT_PATIENT_ID_BRAINSPINETMLN,
            String DMP_PATIENT_ID_MIN_BRAINSPINETMLN,
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
            String SOURCE) {
        this.DMT_PATIENT_ID_BRAINSPINETMLN  =  ClinicalValueUtil.defaultWithNA(DMT_PATIENT_ID_BRAINSPINETMLN);
        this.DMP_PATIENT_ID_MIN_BRAINSPINETMLN  =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_MIN_BRAINSPINETMLN);
        this.DMP_PATIENT_ID_MAX_BRAINSPINETMLN  =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_MAX_BRAINSPINETMLN);
        this.DMP_PATIENT_ID_COUNT_BRAINSPINETMLN  =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_COUNT_BRAINSPINETMLN);
        this.DMP_PATIENT_ID_ALL_BRAINSPINETMLN  =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_ALL_BRAINSPINETMLN);
        this.START_DATE  =  ClinicalValueUtil.defaultWithNA(START_DATE);
        this.STOP_DATE  =  ClinicalValueUtil.defaultWithNA(STOP_DATE);
        this.EVENT_TYPE  =  ClinicalValueUtil.defaultWithNA(EVENT_TYPE);
        this.TREATMENT_TYPE  =  ClinicalValueUtil.defaultWithNA(TREATMENT_TYPE);
        this.SUBTYPE  =  ClinicalValueUtil.defaultWithNA(SUBTYPE);
        this.AGENT  =  ClinicalValueUtil.defaultWithNA(AGENT);
        this.SPECIMEN_REFERENCE_NUMBER =  ClinicalValueUtil.defaultWithNA(SPECIMEN_REFERENCE_NUMBER);
        this.SPECIMEN_SITE =  ClinicalValueUtil.defaultWithNA(SPECIMEN_SITE);
        this.SPECIMEN_TYPE =  ClinicalValueUtil.defaultWithNA(SPECIMEN_TYPE);
        this.STATUS =  ClinicalValueUtil.defaultWithNA(STATUS);
        this.KARNOFSKY_PERFORMANCE_SCORE =  ClinicalValueUtil.defaultWithNA(KARNOFSKY_PERFORMANCE_SCORE);
        this.SURGERY_DETAILS =  ClinicalValueUtil.defaultWithNA(SURGERY_DETAILS);
        this.EVENT_TYPE_DETAILED =  ClinicalValueUtil.defaultWithNA(EVENT_TYPE_DETAILED);
        this.HISTOLOGY =  ClinicalValueUtil.defaultWithNA(HISTOLOGY);
        this.WHO_GRADE =  ClinicalValueUtil.defaultWithNA(WHO_GRADE);
        this.MGMT_STATUS =  ClinicalValueUtil.defaultWithNA(MGMT_STATUS);
        this.SOURCE_PATHOLOGY =  ClinicalValueUtil.defaultWithNA(SOURCE_PATHOLOGY);
        this.NOTE =  ClinicalValueUtil.defaultWithNA(NOTE);
        this.DIAGNOSTIC_TYPE =  ClinicalValueUtil.defaultWithNA(DIAGNOSTIC_TYPE);
        this.DIAGNOSTIC_TYPE_DETAILED =  ClinicalValueUtil.defaultWithNA(DIAGNOSTIC_TYPE_DETAILED);
        this.SOURCE =  ClinicalValueUtil.defaultWithNA(SOURCE);
    }

    public String getDMT_PATIENT_ID_BRAINSPINETMLN() {
        return DMT_PATIENT_ID_BRAINSPINETMLN;
    }

    public void setDMT_PATIENT_ID_BRAINSPINETMLN(String DMT_PATIENT_ID_BRAINSPINETMLN) {
        this.DMT_PATIENT_ID_BRAINSPINETMLN =  ClinicalValueUtil.defaultWithNA(DMT_PATIENT_ID_BRAINSPINETMLN);
    }

    public String getDMP_PATIENT_ID_MIN_BRAINSPINETMLN() {
        return DMP_PATIENT_ID_MIN_BRAINSPINETMLN;
    }

    public void setDMP_PATIENT_ID_MIN_BRAINSPINETMLN(String DMP_PATIENT_ID_MIN_BRAINSPINETMLN) {
        this.DMP_PATIENT_ID_MIN_BRAINSPINETMLN =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_MIN_BRAINSPINETMLN);
    }

    public String getDMP_PATIENT_ID_MAX_BRAINSPINETMLN() {
        return DMP_PATIENT_ID_MAX_BRAINSPINETMLN;
    }

    public void setDMP_PATIENT_ID_MAX_BRAINSPINETMLN(String DMP_PATIENT_ID_MAX_BRAINSPINETMLN) {
        this.DMP_PATIENT_ID_MAX_BRAINSPINETMLN =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_MAX_BRAINSPINETMLN);
    }

    public String getDMP_PATIENT_ID_COUNT_BRAINSPINETMLN() {
        return DMP_PATIENT_ID_COUNT_BRAINSPINETMLN;
    }

    public void setDMP_PATIENT_ID_COUNT_BRAINSPINETMLN(String DMP_PATIENT_ID_COUNT_BRAINSPINETMLN) {
        this.DMP_PATIENT_ID_COUNT_BRAINSPINETMLN =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_COUNT_BRAINSPINETMLN);
    }

    public String getDMP_PATIENT_ID_ALL_BRAINSPINETMLN() {
        return DMP_PATIENT_ID_ALL_BRAINSPINETMLN;
    }

    public void setDMP_PATIENT_ID_ALL_BRAINSPINETMLN(String DMP_PATIENT_ID_ALL_BRAINSPINETMLN) {
        this.DMP_PATIENT_ID_ALL_BRAINSPINETMLN =  ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_ALL_BRAINSPINETMLN);
    }

    public String getSTART_DATE() {
        return START_DATE;
    }

    public void setSTART_DATE(String START_DATE) {
        this.START_DATE =  ClinicalValueUtil.defaultWithNA(START_DATE);
    }

    public String getSTOP_DATE() {
        return STOP_DATE;
    }

    public void setSTOP_DATE(String STOP_DATE) {
        this.STOP_DATE =  ClinicalValueUtil.defaultWithNA(STOP_DATE);
    }

    public String getEVENT_TYPE() {
        return EVENT_TYPE;
    }

    public void setEVENT_TYPE(String EVENT_TYPE) {
        this.EVENT_TYPE =  ClinicalValueUtil.defaultWithNA(EVENT_TYPE);
    }

    public String getTREATMENT_TYPE() {
        return TREATMENT_TYPE;
    }

    public void setTREATMENT_TYPE(String TREATMENT_TYPE) {
        this.TREATMENT_TYPE =  ClinicalValueUtil.defaultWithNA(TREATMENT_TYPE);
    }

    public String getSUBTYPE() {
        return SUBTYPE;
    }

    public void setSUBTYPE(String SUBTYPE) {
        this.SUBTYPE =  ClinicalValueUtil.defaultWithNA(SUBTYPE);
    }

    public String getAGENT() {
        return AGENT;
    }

    public void setAGENT(String AGENT) {
        this.AGENT =  ClinicalValueUtil.defaultWithNA(AGENT);
    }

    public String getSPECIMEN_REFERENCE_NUMBER() {
        return SPECIMEN_REFERENCE_NUMBER;
    }

    public void setSPECIMEN_REFERENCE_NUMBER(String SPECIMEN_REFERENCE_NUMBER) {
        this.SPECIMEN_REFERENCE_NUMBER =  ClinicalValueUtil.defaultWithNA(SPECIMEN_REFERENCE_NUMBER);
    }

    public String getSPECIMEN_SITE() {
        return SPECIMEN_SITE;
    }

    public void setSPECIMEN_SITE(String SPECIMEN_SITE) {
        this.SPECIMEN_SITE =  ClinicalValueUtil.defaultWithNA(SPECIMEN_SITE);
    }

    public String getSPECIMEN_TYPE() {
        return SPECIMEN_TYPE;
    }

    public void setSPECIMEN_TYPE(String SPECIMEN_TYPE) {
        this.SPECIMEN_TYPE =  ClinicalValueUtil.defaultWithNA(SPECIMEN_TYPE);
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS =  ClinicalValueUtil.defaultWithNA(STATUS);
    }

    public String getKARNOFSKY_PERFORMANCE_SCORE() {
        return KARNOFSKY_PERFORMANCE_SCORE;
    }

    public void setKARNOFSKY_PERFORMANCE_SCORE(String KARNOFSKY_PERFORMANCE_SCORE) {
        this.KARNOFSKY_PERFORMANCE_SCORE =  ClinicalValueUtil.defaultWithNA(KARNOFSKY_PERFORMANCE_SCORE);
    }

    public String getSURGERY_DETAILS() {
        return SURGERY_DETAILS;
    }

    public void setSURGERY_DETAILS(String SURGERY_DETAILS) {
        this.SURGERY_DETAILS =  ClinicalValueUtil.defaultWithNA(SURGERY_DETAILS);
    }

    public String getEVENT_TYPE_DETAILED() {
        return EVENT_TYPE_DETAILED;
    }

    public void setEVENT_TYPE_DETAILED(String EVENT_TYPE_DETAILED) {
        this.EVENT_TYPE_DETAILED =  ClinicalValueUtil.defaultWithNA(EVENT_TYPE_DETAILED);
    }

    public String getHISTOLOGY() {
        return HISTOLOGY;
    }

    public void setHISTOLOGY(String HISTOLOGY) {
        this.HISTOLOGY =  ClinicalValueUtil.defaultWithNA(HISTOLOGY);
    }

    public String getWHO_GRADE() {
        return WHO_GRADE;
    }

    public void setWHO_GRADE(String WHO_GRADE) {
        this.WHO_GRADE =  ClinicalValueUtil.defaultWithNA(WHO_GRADE);
    }

    public String getMGMT_STATUS() {
        return MGMT_STATUS;
    }

    public void setMGMT_STATUS(String MGMT_STATUS) {
        this.MGMT_STATUS =  ClinicalValueUtil.defaultWithNA(MGMT_STATUS);
    }

    public String getSOURCE_PATHOLOGY() {
        return SOURCE_PATHOLOGY;
    }

    public void setSOURCE_PATHOLOGY(String SOURCE_PATHOLOGY) {
        this.SOURCE_PATHOLOGY =  ClinicalValueUtil.defaultWithNA(SOURCE_PATHOLOGY);
    }

    public String getNOTE() {
        return NOTE;
    }

    public void setNOTE(String NOTE) {
        this.NOTE =  ClinicalValueUtil.defaultWithNA(NOTE);
    }

    public String getDIAGNOSTIC_TYPE() {
        return DIAGNOSTIC_TYPE;
    }

    public void setDIAGNOSTIC_TYPE(String DIAGNOSTIC_TYPE) {
        this.DIAGNOSTIC_TYPE =  ClinicalValueUtil.defaultWithNA(DIAGNOSTIC_TYPE);
    }

    public String getDIAGNOSTIC_TYPE_DETAILED() {
        return DIAGNOSTIC_TYPE_DETAILED;
    }

    public void setDIAGNOSTIC_TYPE_DETAILED(String DIAGNOSTIC_TYPE_DETAILED) {
        this.DIAGNOSTIC_TYPE_DETAILED =  ClinicalValueUtil.defaultWithNA(DIAGNOSTIC_TYPE_DETAILED);
    }

    public String getSOURCE() {
        return SOURCE;
    }

    public void setSOURCE(String SOURCE) {
        this.SOURCE =  ClinicalValueUtil.defaultWithNA(SOURCE);
    }

    public Map<String, Object> getAditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> getStatusFields() {
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

    public List<String> getStatusHeaders() {
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

    public List<String> getSpecimenFields() {
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

    public List<String> getSpecimenHeaders() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("SAMPLE_ID"); // specimen reference number should be sample id
        fieldNames.add("SPECIMEN_SITE");
        fieldNames.add("SPECIMEN_TYPE");
        fieldNames.add("SOURCE");
        return fieldNames;
    }

    public List<String> getTreatmentFields() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
        return fieldNames;
    }

    public List<String> getTreatmentHeaders() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("STOP_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("TREATMENT_TYPE");
        fieldNames.add("SUBTYPE");
        fieldNames.add("AGENT");
        fieldNames.add("NOTE");
        fieldNames.add("SOURCE");
        return fieldNames;
    }

    public List<String> getImagingFields() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_PATIENT_ID_ALL_BRAINSPINETMLN");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE_DETAILED");
        fieldNames.add("SOURCE");
        return fieldNames;
    }

    public List<String> getImagingHeaders() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("START_DATE");
        fieldNames.add("EVENT_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE");
        fieldNames.add("DIAGNOSTIC_TYPE_DETAILED");
        fieldNames.add("SOURCE");
        return fieldNames;
    }

    public List<String> getSurgeryFields() {
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

    public List<String> getSurgeryHeaders() {
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
