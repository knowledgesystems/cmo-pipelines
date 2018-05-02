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
    "Age at Proc",
    "CPT Code",
    "Extended Description",
    "MP_FIRST_NM",
    "MP_LAST_NM",
    "MRN",
    "PBD Code",
    "PID",
    "Phy Dept",
    "Phy Service",
    "Physician",
    "Physician Email",
    "Proc IP-OP",
    "Proc Year",
    "Procedure Date",
    "Procedure Description"
})
public class Surgery implements Serializable {
    @JsonProperty("Age at Proc")
    private Integer ageAtProc;
    @JsonProperty("CPT Code")
    private String cPTCode;
    @JsonProperty("Extended Description")
    private String extendedDescription;
    @JsonProperty("MP_FIRST_NM")
    private String mPFIRSTNM;
    @JsonProperty("MP_LAST_NM")
    private String mPLASTNM;
    @JsonProperty("MRN")
    private String mRN;
    @JsonProperty("PBD Code")
    private String pBDCode;
    @JsonProperty("PID")
    private Integer pID;
    @JsonProperty("Phy Dept")
    private String phyDept;
    @JsonProperty("Phy Service")
    private String phyService;
    @JsonProperty("Physician")
    private String physician;
    @JsonProperty("Physician Email")
    private String physicianEmail;
    @JsonProperty("Proc IP-OP")
    private String procIPOP;
    @JsonProperty("Proc Year")
    private Integer procYear;
    @JsonProperty("Procedure Date")
    private String procedureDate;
    @JsonProperty("Procedure Description")
    private String procedureDescription;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("Age at Proc")
    public Integer getAgeAtProc() {
        return ageAtProc;
    }

    @JsonProperty("Age at Proc")
    public void setAgeAtProc(Integer ageAtProc) {
        this.ageAtProc = ageAtProc;
    }

    @JsonProperty("CPT Code")
    public String getCPTCode() {
        return cPTCode;
    }

    @JsonProperty("CPT Code")
    public void setCPTCode(String cPTCode) {
        this.cPTCode = cPTCode;
    }

    @JsonProperty("Extended Description")
    public String getExtendedDescription() {
        return extendedDescription;
    }

    @JsonProperty("Extended Description")
    public void setExtendedDescription(String extendedDescription) {
        this.extendedDescription = extendedDescription;
    }

    @JsonProperty("MP_FIRST_NM")
    public String getMPFIRSTNM() {
        return mPFIRSTNM;
    }

    @JsonProperty("MP_FIRST_NM")
    public void setMPFIRSTNM(String mPFIRSTNM) {
        this.mPFIRSTNM = mPFIRSTNM;
    }

    @JsonProperty("MP_LAST_NM")
    public String getMPLASTNM() {
        return mPLASTNM;
    }

    @JsonProperty("MP_LAST_NM")
    public void setMPLASTNM(String mPLASTNM) {
        this.mPLASTNM = mPLASTNM;
    }

    @JsonProperty("MRN")
    public String getMRN() {
        return mRN;
    }

    @JsonProperty("MRN")
    public void setMRN(String mRN) {
        this.mRN = mRN;
    }

    @JsonProperty("PBD Code")
    public String getPBDCode() {
        return pBDCode;
    }

    @JsonProperty("PBD Code")
    public void setPBDCode(String pBDCode) {
        this.pBDCode = pBDCode;
    }

    @JsonProperty("PID")
    public Integer getPID() {
        return pID;
    }

    @JsonProperty("PID")
    public void setPID(Integer pID) {
        this.pID = pID;
    }

    @JsonProperty("Phy Dept")
    public String getPhyDept() {
        return phyDept;
    }

    @JsonProperty("Phy Dept")
    public void setPhyDept(String phyDept) {
        this.phyDept = phyDept;
    }

    @JsonProperty("Phy Service")
    public String getPhyService() {
        return phyService;
    }

    @JsonProperty("Phy Service")
    public void setPhyService(String phyService) {
        this.phyService = phyService;
    }

    @JsonProperty("Physician")
    public String getPhysician() {
        return physician;
    }

    @JsonProperty("Physician")
    public void setPhysician(String physician) {
        this.physician = physician;
    }

    @JsonProperty("Physician Email")
    public String getPhysicianEmail() {
        return physicianEmail;
    }

    @JsonProperty("Physician Email")
    public void setPhysicianEmail(String physicianEmail) {
        this.physicianEmail = physicianEmail;
    }

    @JsonProperty("Proc IP-OP")
    public String getProcIPOP() {
        return procIPOP;
    }

    @JsonProperty("Proc IP-OP")
    public void setProcIPOP(String procIPOP) {
        this.procIPOP = procIPOP;
    }

    @JsonProperty("Proc Year")
    public Integer getProcYear() {
        return procYear;
    }

    @JsonProperty("Proc Year")
    public void setProcYear(Integer procYear) {
        this.procYear = procYear;
    }

    @JsonProperty("Procedure Date")
    public String getProcedureDate() {
        return procedureDate;
    }

    @JsonProperty("Procedure Date")
    public void setProcedureDate(String procedureDate) {
        this.procedureDate = procedureDate;
    }

    @JsonProperty("Procedure Description")
    public String getProcedureDescription() {
        return procedureDescription;
    }

    @JsonProperty("Procedure Description")
    public void setProcedureDescription(String procedureDescription) {
        this.procedureDescription = procedureDescription;
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
