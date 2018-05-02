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
    "BMI",
    "BSA",
    "Current Age",
    "Date of Birth",
    "Deceased Date",
    "Deident PT #",
    "Disease Category",
    "Ethnicity",
    "First Name",
    "Gender",
    "Height CM",
    "Height Inches",
    "Initial CA Category",
    "Last Activity",
    "Last Attended Appt",
    "Last Chemo",
    "Last Contact Date",
    "Last Dr Appt",
    "Last Hosp Adm",
    "Last Hosp Dsch",
    "Last Name",
    "Last RadOnc",
    "Last Surgery",
    "Last Treatment",
    "MRN",
    "MRN Create Date",
    "Marital Status",
    "PLA_LAST_ACTV_DTE",
    "PLA_LAST_ADM_DTE",
    "PLA_LAST_APPT_DTE",
    "PLA_LAST_CHEMO_DTE",
    "PLA_LAST_CONTACT_DTE",
    "PLA_LAST_DRVST_DTE",
    "PLA_LAST_DSCH_DTE",
    "PLA_LAST_RT_DTE",
    "PLA_LAST_SURG_DTE",
    "PLA_LAST_TX_DTE",
    "PT_BIRTH_DTE",
    "PT_DEATH_DTE",
    "Race",
    "Religion",
    "Weight KG",
    "Weight LB"
})
public class PatientDemographics implements Serializable {
    @JsonProperty("BMI")
    private String bMI;
    @JsonProperty("BSA")
    private String bSA;
    @JsonProperty("Current Age")
    private Integer currentAge;
    @JsonProperty("Date of Birth")
    private String dateOfBirth;
    @JsonProperty("Deceased Date")
    private String deceasedDate;
    @JsonProperty("Deident PT #")
    private Integer deidentPT;
    @JsonProperty("Disease Category")
    private String diseaseCategory;
    @JsonProperty("Ethnicity")
    private String ethnicity;
    @JsonProperty("First Name")
    private String firstName;
    @JsonProperty("Gender")
    private String gender;
    @JsonProperty("Height CM")
    private String heightCM;
    @JsonProperty("Height Inches")
    private String heightInches;
    @JsonProperty("Initial CA Category")
    private String initialCACategory;
    @JsonProperty("Last Activity")
    private String lastActivity;
    @JsonProperty("Last Attended Appt")
    private String lastAttendedAppt;
    @JsonProperty("Last Chemo")
    private String lastChemo;
    @JsonProperty("Last Contact Date")
    private String lastContactDate;
    @JsonProperty("Last Dr Appt")
    private String lastDrAppt;
    @JsonProperty("Last Hosp Adm")
    private String lastHospAdm;
    @JsonProperty("Last Hosp Dsch")
    private String lastHospDsch;
    @JsonProperty("Last Name")
    private String lastName;
    @JsonProperty("Last RadOnc")
    private String lastRadOnc;
    @JsonProperty("Last Surgery")
    private String lastSurgery;
    @JsonProperty("Last Treatment")
    private String lastTreatment;
    @JsonProperty("MRN")
    private String mRN;
    @JsonProperty("MRN Create Date")
    private String mRNCreateDate;
    @JsonProperty("Marital Status")
    private String maritalStatus;
    @JsonProperty("PLA_LAST_ACTV_DTE")
    private String pLALASTACTVDTE;
    @JsonProperty("PLA_LAST_ADM_DTE")
    private String pLALASTADMDTE;
    @JsonProperty("PLA_LAST_APPT_DTE")
    private String pLALASTAPPTDTE;
    @JsonProperty("PLA_LAST_CHEMO_DTE")
    private String pLALASTCHEMODTE;
    @JsonProperty("PLA_LAST_CONTACT_DTE")
    private String pLALASTCONTACTDTE;
    @JsonProperty("PLA_LAST_DRVST_DTE")
    private String pLALASTDRVSTDTE;
    @JsonProperty("PLA_LAST_DSCH_DTE")
    private String pLALASTDSCHDTE;
    @JsonProperty("PLA_LAST_RT_DTE")
    private String pLALASTRTDTE;
    @JsonProperty("PLA_LAST_SURG_DTE")
    private String pLALASTSURGDTE;
    @JsonProperty("PLA_LAST_TX_DTE")
    private String pLALASTTXDTE;
    @JsonProperty("PT_BIRTH_DTE")
    private String pTBIRTHDTE;
    @JsonProperty("PT_DEATH_DTE")
    private String pTDEATHDTE;
    @JsonProperty("Race")
    private String race;
    @JsonProperty("Religion")
    private String religion;
    @JsonProperty("Weight KG")
    private String weightKG;
    @JsonProperty("Weight LB")
    private String weightLB;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("BMI")
    public String getBMI() {
        return bMI;
    }

    @JsonProperty("BMI")
    public void setBMI(String bMI) {
        this.bMI = bMI;
    }

    @JsonProperty("BSA")
    public String getBSA() {
        return bSA;
    }

    @JsonProperty("BSA")
    public void setBSA(String bSA) {
        this.bSA = bSA;
    }

    @JsonProperty("Current Age")
    public Integer getCurrentAge() {
        return currentAge;
    }

    @JsonProperty("Current Age")
    public void setCurrentAge(Integer currentAge) {
        this.currentAge = currentAge;
    }

    @JsonProperty("Date of Birth")
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    @JsonProperty("Date of Birth")
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @JsonProperty("Deceased Date")
    public String getDeceasedDate() {
        return deceasedDate;
    }

    @JsonProperty("Deceased Date")
    public void setDeceasedDate(String deceasedDate) {
        this.deceasedDate = deceasedDate;
    }

    @JsonProperty("Deident PT #")
    public Integer getDeidentPT() {
        return deidentPT;
    }

    @JsonProperty("Deident PT #")
    public void setDeidentPT(Integer deidentPT) {
        this.deidentPT = deidentPT;
    }

    @JsonProperty("Disease Category")
    public String getDiseaseCategory() {
        return diseaseCategory;
    }

    @JsonProperty("Disease Category")
    public void setDiseaseCategory(String diseaseCategory) {
        this.diseaseCategory = diseaseCategory;
    }

    @JsonProperty("Ethnicity")
    public String getEthnicity() {
        return ethnicity;
    }

    @JsonProperty("Ethnicity")
    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    @JsonProperty("First Name")
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty("First Name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonProperty("Gender")
    public String getGender() {
        return gender;
    }

    @JsonProperty("Gender")
    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonProperty("Height CM")
    public String getHeightCM() {
        return heightCM;
    }

    @JsonProperty("Height CM")
    public void setHeightCM(String heightCM) {
        this.heightCM = heightCM;
    }

    @JsonProperty("Height Inches")
    public String getHeightInches() {
        return heightInches;
    }

    @JsonProperty("Height Inches")
    public void setHeightInches(String heightInches) {
        this.heightInches = heightInches;
    }

    @JsonProperty("Initial CA Category")
    public String getInitialCACategory() {
        return initialCACategory;
    }

    @JsonProperty("Initial CA Category")
    public void setInitialCACategory(String initialCACategory) {
        this.initialCACategory = initialCACategory;
    }

    @JsonProperty("Last Activity")
    public String getLastActivity() {
        return lastActivity;
    }

    @JsonProperty("Last Activity")
    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    @JsonProperty("Last Attended Appt")
    public String getLastAttendedAppt() {
        return lastAttendedAppt;
    }

    @JsonProperty("Last Attended Appt")
    public void setLastAttendedAppt(String lastAttendedAppt) {
        this.lastAttendedAppt = lastAttendedAppt;
    }

    @JsonProperty("Last Chemo")
    public String getLastChemo() {
        return lastChemo;
    }

    @JsonProperty("Last Chemo")
    public void setLastChemo(String lastChemo) {
        this.lastChemo = lastChemo;
    }

    @JsonProperty("Last Contact Date")
    public String getLastContactDate() {
        return lastContactDate;
    }

    @JsonProperty("Last Contact Date")
    public void setLastContactDate(String lastContactDate) {
        this.lastContactDate = lastContactDate;
    }

    @JsonProperty("Last Dr Appt")
    public String getLastDrAppt() {
        return lastDrAppt;
    }

    @JsonProperty("Last Dr Appt")
    public void setLastDrAppt(String lastDrAppt) {
        this.lastDrAppt = lastDrAppt;
    }

    @JsonProperty("Last Hosp Adm")
    public String getLastHospAdm() {
        return lastHospAdm;
    }

    @JsonProperty("Last Hosp Adm")
    public void setLastHospAdm(String lastHospAdm) {
        this.lastHospAdm = lastHospAdm;
    }

    @JsonProperty("Last Hosp Dsch")
    public String getLastHospDsch() {
        return lastHospDsch;
    }

    @JsonProperty("Last Hosp Dsch")
    public void setLastHospDsch(String lastHospDsch) {
        this.lastHospDsch = lastHospDsch;
    }

    @JsonProperty("Last Name")
    public String getLastName() {
        return lastName;
    }

    @JsonProperty("Last Name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonProperty("Last RadOnc")
    public String getLastRadOnc() {
        return lastRadOnc;
    }

    @JsonProperty("Last RadOnc")
    public void setLastRadOnc(String lastRadOnc) {
        this.lastRadOnc = lastRadOnc;
    }

    @JsonProperty("Last Surgery")
    public String getLastSurgery() {
        return lastSurgery;
    }

    @JsonProperty("Last Surgery")
    public void setLastSurgery(String lastSurgery) {
        this.lastSurgery = lastSurgery;
    }

    @JsonProperty("Last Treatment")
    public String getLastTreatment() {
        return lastTreatment;
    }

    @JsonProperty("Last Treatment")
    public void setLastTreatment(String lastTreatment) {
        this.lastTreatment = lastTreatment;
    }

    @JsonProperty("MRN")
    public String getMRN() {
        return mRN;
    }

    @JsonProperty("MRN")
    public void setMRN(String mRN) {
        this.mRN = mRN;
    }

    @JsonProperty("MRN Create Date")
    public String getMRNCreateDate() {
        return mRNCreateDate;
    }

    @JsonProperty("MRN Create Date")
    public void setMRNCreateDate(String mRNCreateDate) {
        this.mRNCreateDate = mRNCreateDate;
    }

    @JsonProperty("Marital Status")
    public String getMaritalStatus() {
        return maritalStatus;
    }

    @JsonProperty("Marital Status")
    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    @JsonProperty("PLA_LAST_ACTV_DTE")
    public String getPLALASTACTVDTE() {
        return pLALASTACTVDTE;
    }

    @JsonProperty("PLA_LAST_ACTV_DTE")
    public void setPLALASTACTVDTE(String pLALASTACTVDTE) {
        this.pLALASTACTVDTE = pLALASTACTVDTE;
    }

    @JsonProperty("PLA_LAST_ADM_DTE")
    public String getPLALASTADMDTE() {
        return pLALASTADMDTE;
    }

    @JsonProperty("PLA_LAST_ADM_DTE")
    public void setPLALASTADMDTE(String pLALASTADMDTE) {
        this.pLALASTADMDTE = pLALASTADMDTE;
    }

    @JsonProperty("PLA_LAST_APPT_DTE")
    public String getPLALASTAPPTDTE() {
        return pLALASTAPPTDTE;
    }

    @JsonProperty("PLA_LAST_APPT_DTE")
    public void setPLALASTAPPTDTE(String pLALASTAPPTDTE) {
        this.pLALASTAPPTDTE = pLALASTAPPTDTE;
    }

    @JsonProperty("PLA_LAST_CHEMO_DTE")
    public String getPLALASTCHEMODTE() {
        return pLALASTCHEMODTE;
    }

    @JsonProperty("PLA_LAST_CHEMO_DTE")
    public void setPLALASTCHEMODTE(String pLALASTCHEMODTE) {
        this.pLALASTCHEMODTE = pLALASTCHEMODTE;
    }

    @JsonProperty("PLA_LAST_CONTACT_DTE")
    public String getPLALASTCONTACTDTE() {
        return pLALASTCONTACTDTE;
    }

    @JsonProperty("PLA_LAST_CONTACT_DTE")
    public void setPLALASTCONTACTDTE(String pLALASTCONTACTDTE) {
        this.pLALASTCONTACTDTE = pLALASTCONTACTDTE;
    }

    @JsonProperty("PLA_LAST_DRVST_DTE")
    public String getPLALASTDRVSTDTE() {
        return pLALASTDRVSTDTE;
    }

    @JsonProperty("PLA_LAST_DRVST_DTE")
    public void setPLALASTDRVSTDTE(String pLALASTDRVSTDTE) {
        this.pLALASTDRVSTDTE = pLALASTDRVSTDTE;
    }

    @JsonProperty("PLA_LAST_DSCH_DTE")
    public String getPLALASTDSCHDTE() {
        return pLALASTDSCHDTE;
    }

    @JsonProperty("PLA_LAST_DSCH_DTE")
    public void setPLALASTDSCHDTE(String pLALASTDSCHDTE) {
        this.pLALASTDSCHDTE = pLALASTDSCHDTE;
    }

    @JsonProperty("PLA_LAST_RT_DTE")
    public String getPLALASTRTDTE() {
        return pLALASTRTDTE;
    }

    @JsonProperty("PLA_LAST_RT_DTE")
    public void setPLALASTRTDTE(String pLALASTRTDTE) {
        this.pLALASTRTDTE = pLALASTRTDTE;
    }

    @JsonProperty("PLA_LAST_SURG_DTE")
    public String getPLALASTSURGDTE() {
        return pLALASTSURGDTE;
    }

    @JsonProperty("PLA_LAST_SURG_DTE")
    public void setPLALASTSURGDTE(String pLALASTSURGDTE) {
        this.pLALASTSURGDTE = pLALASTSURGDTE;
    }

    @JsonProperty("PLA_LAST_TX_DTE")
    public String getPLALASTTXDTE() {
        return pLALASTTXDTE;
    }

    @JsonProperty("PLA_LAST_TX_DTE")
    public void setPLALASTTXDTE(String pLALASTTXDTE) {
        this.pLALASTTXDTE = pLALASTTXDTE;
    }

    @JsonProperty("PT_BIRTH_DTE")
    public String getPTBIRTHDTE() {
        return pTBIRTHDTE;
    }

    @JsonProperty("PT_BIRTH_DTE")
    public void setPTBIRTHDTE(String pTBIRTHDTE) {
        this.pTBIRTHDTE = pTBIRTHDTE;
    }

    @JsonProperty("PT_DEATH_DTE")
    public String getPTDEATHDTE() {
        return pTDEATHDTE;
    }

    @JsonProperty("PT_DEATH_DTE")
    public void setPTDEATHDTE(String pTDEATHDTE) {
        this.pTDEATHDTE = pTDEATHDTE;
    }

    @JsonProperty("Race")
    public String getRace() {
        return race;
    }

    @JsonProperty("Race")
    public void setRace(String race) {
        this.race = race;
    }

    @JsonProperty("Religion")
    public String getReligion() {
        return religion;
    }

    @JsonProperty("Religion")
    public void setReligion(String religion) {
        this.religion = religion;
    }

    @JsonProperty("Weight KG")
    public String getWeightKG() {
        return weightKG;
    }

    @JsonProperty("Weight KG")
    public void setWeightKG(String weightKG) {
        this.weightKG = weightKG;
    }

    @JsonProperty("Weight LB")
    public String getWeightLB() {
        return weightLB;
    }

    @JsonProperty("Weight LB")
    public void setWeightLB(String weightLB) {
        this.weightLB = weightLB;
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
