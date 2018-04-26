/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
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

package org.mskcc.cmo.ks.ddp.source.model;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "AJCC",
    "CATEGORY1",
    "CATEGORY2",
    "CATEGORY3",
    "CATEGORY4",
    "Clinical Group",
    "Diagnosis",
    "Diagnosis Date",
    "Diagnosis Description",
    "Diagnosis Type",
    "ICD-9/10 Dx Code",
    "ICD-9/10 Dx Desc",
    "ICD-O Histology Code",
    "ICD-O Histology Desc",
    "ICD-O Site Code",
    "ICD-O Site Desc",
    "MRN",
    "MSK Stage",
    "Min ICD-9/10 Dx Date",
    "P_ID",
    "Path Group",
    "Summary",
    "Tumor Diagnosis Date"
})
public class PatientDiagnosis implements Serializable {
    @JsonProperty("AJCC")
    private String aJCC;
    @JsonProperty("CATEGORY1")
    private String cATEGORY1;
    @JsonProperty("CATEGORY2")
    private String cATEGORY2;
    @JsonProperty("CATEGORY3")
    private String cATEGORY3;
    @JsonProperty("CATEGORY4")
    private String cATEGORY4;
    @JsonProperty("Clinical Group")
    private String clinicalGroup;
    @JsonProperty("Diagnosis")
    private List<String> diagnosis = null;
    @JsonProperty("Diagnosis Date")
    private String diagnosisDate;
    @JsonProperty("Diagnosis Description")
    private String diagnosisDescription;
    @JsonProperty("Diagnosis Type")
    private String diagnosisType;
    @JsonProperty("ICD-9/10 Dx Code")
    private String iCD910DxCode;
    @JsonProperty("ICD-9/10 Dx Desc")
    private String iCD910DxDesc;
    @JsonProperty("ICD-O Histology Code")
    private String iCDOHistologyCode;
    @JsonProperty("ICD-O Histology Desc")
    private String iCDOHistologyDesc;
    @JsonProperty("ICD-O Site Code")
    private String iCDOSiteCode;
    @JsonProperty("ICD-O Site Desc")
    private String iCDOSiteDesc;
    @JsonProperty("MRN")
    private String mRN;
    @JsonProperty("MSK Stage")
    private String mSKStage;
    @JsonProperty("Min ICD-9/10 Dx Date")
    private String minICD910DxDate;
    @JsonProperty("P_ID")
    private String pID;
    @JsonProperty("Path Group")
    private String pathGroup;
    @JsonProperty("Summary")
    private String summary;
    @JsonProperty("Tumor Diagnosis Date")
    private String tumorDiagnosisDate;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("AJCC")
    public String getAJCC() {
        return aJCC;
    }

    @JsonProperty("AJCC")
    public void setAJCC(String aJCC) {
        this.aJCC = aJCC;
    }

    @JsonProperty("CATEGORY1")
    public String getCATEGORY1() {
        return cATEGORY1;
    }

    @JsonProperty("CATEGORY1")
    public void setCATEGORY1(String cATEGORY1) {
        this.cATEGORY1 = cATEGORY1;
    }

    @JsonProperty("CATEGORY2")
    public String getCATEGORY2() {
        return cATEGORY2;
    }

    @JsonProperty("CATEGORY2")
    public void setCATEGORY2(String cATEGORY2) {
        this.cATEGORY2 = cATEGORY2;
    }

    @JsonProperty("CATEGORY3")
    public String getCATEGORY3() {
        return cATEGORY3;
    }

    @JsonProperty("CATEGORY3")
    public void setCATEGORY3(String cATEGORY3) {
        this.cATEGORY3 = cATEGORY3;
    }

    @JsonProperty("CATEGORY4")
    public String getCATEGORY4() {
        return cATEGORY4;
    }

    @JsonProperty("CATEGORY4")
    public void setCATEGORY4(String cATEGORY4) {
        this.cATEGORY4 = cATEGORY4;
    }

    @JsonProperty("Clinical Group")
    public String getClinicalGroup() {
        return clinicalGroup;
    }

    @JsonProperty("Clinical Group")
    public void setClinicalGroup(String clinicalGroup) {
        this.clinicalGroup = clinicalGroup;
    }

    @JsonProperty("Diagnosis")
    public List<String> getDiagnosis() {
        return diagnosis;
    }

    @JsonProperty("Diagnosis")
    public void setDiagnosis(List<String> diagnosis) {
        this.diagnosis = diagnosis;
    }

    @JsonProperty("Diagnosis Date")
    public String getDiagnosisDate() {
        return diagnosisDate;
    }

    @JsonProperty("Diagnosis Date")
    public void setDiagnosisDate(String diagnosisDate) {
        this.diagnosisDate = diagnosisDate;
    }

    @JsonProperty("Diagnosis Description")
    public String getDiagnosisDescription() {
        return diagnosisDescription;
    }

    @JsonProperty("Diagnosis Description")
    public void setDiagnosisDescription(String diagnosisDescription) {
        this.diagnosisDescription = diagnosisDescription;
    }

    @JsonProperty("Diagnosis Type")
    public String getDiagnosisType() {
        return diagnosisType;
    }

    @JsonProperty("Diagnosis Type")
    public void setDiagnosisType(String diagnosisType) {
        this.diagnosisType = diagnosisType;
    }

    @JsonProperty("ICD-9/10 Dx Code")
    public String getICD910DxCode() {
        return iCD910DxCode;
    }

    @JsonProperty("ICD-9/10 Dx Code")
    public void setICD910DxCode(String iCD910DxCode) {
        this.iCD910DxCode = iCD910DxCode;
    }

    @JsonProperty("ICD-9/10 Dx Desc")
    public String getICD910DxDesc() {
        return iCD910DxDesc;
    }

    @JsonProperty("ICD-9/10 Dx Desc")
    public void setICD910DxDesc(String iCD910DxDesc) {
        this.iCD910DxDesc = iCD910DxDesc;
    }

    @JsonProperty("ICD-O Histology Code")
    public String getICDOHistologyCode() {
        return iCDOHistologyCode;
    }

    @JsonProperty("ICD-O Histology Code")
    public void setICDOHistologyCode(String iCDOHistologyCode) {
        this.iCDOHistologyCode = iCDOHistologyCode;
    }

    @JsonProperty("ICD-O Histology Desc")
    public String getICDOHistologyDesc() {
        return iCDOHistologyDesc;
    }

    @JsonProperty("ICD-O Histology Desc")
    public void setICDOHistologyDesc(String iCDOHistologyDesc) {
        this.iCDOHistologyDesc = iCDOHistologyDesc;
    }

    @JsonProperty("ICD-O Site Code")
    public String getICDOSiteCode() {
        return iCDOSiteCode;
    }

    @JsonProperty("ICD-O Site Code")
    public void setICDOSiteCode(String iCDOSiteCode) {
        this.iCDOSiteCode = iCDOSiteCode;
    }

    @JsonProperty("ICD-O Site Desc")
    public String getICDOSiteDesc() {
        return iCDOSiteDesc;
    }

    @JsonProperty("ICD-O Site Desc")
    public void setICDOSiteDesc(String iCDOSiteDesc) {
        this.iCDOSiteDesc = iCDOSiteDesc;
    }

    @JsonProperty("MRN")
    public String getMRN() {
        return mRN;
    }

    @JsonProperty("MRN")
    public void setMRN(String mRN) {
        this.mRN = mRN;
    }

    @JsonProperty("MSK Stage")
    public String getMSKStage() {
        return mSKStage;
    }

    @JsonProperty("MSK Stage")
    public void setMSKStage(String mSKStage) {
        this.mSKStage = mSKStage;
    }

    @JsonProperty("Min ICD-9/10 Dx Date")
    public String getMinICD910DxDate() {
        return minICD910DxDate;
    }

    @JsonProperty("Min ICD-9/10 Dx Date")
    public void setMinICD910DxDate(String minICD910DxDate) {
        this.minICD910DxDate = minICD910DxDate;
    }

    @JsonProperty("P_ID")
    public String getPID() {
        return pID;
    }

    @JsonProperty("P_ID")
    public void setPID(String pID) {
        this.pID = pID;
    }

    @JsonProperty("Path Group")
    public String getPathGroup() {
        return pathGroup;
    }

    @JsonProperty("Path Group")
    public void setPathGroup(String pathGroup) {
        this.pathGroup = pathGroup;
    }

    @JsonProperty("Summary")
    public String getSummary() {
        return summary;
    }

    @JsonProperty("Summary")
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JsonProperty("Tumor Diagnosis Date")
    public String getTumorDiagnosisDate() {
        return tumorDiagnosisDate;
    }

    @JsonProperty("Tumor Diagnosis Date")
    public void setTumorDiagnosisDate(String tumorDiagnosisDate) {
        this.tumorDiagnosisDate = tumorDiagnosisDate;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
