/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.crdb.model;

import java.util.*;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Model for CRDBPDXClinicalPatientDataset results.
 *
 * @author averyniceday
 */

public class CRDBPDXClinicalPatientDataset {

    private String PATIENT_ID;
    private String DESTINATION_STUDY_ID;
    private String SEX;
    private String ETHNICITY;
    private String RACE;
    private String SMOKING_HISTORY;
    private String CROHN_DISEASE;
    private String ULCERATIVE_COLITIS;
    private String BARRETTS_ESOPHAGUS;
    private String H_PYLORI;
    private String MDS_RISK_FACTOR;
    private String MENOPAUSE_STATUS;
    private String UV_EXPOSURE;
    private String RADIATION_THERAPY;
    private String BREAST_IMPLANTS;
    private String BRCA;
    private String RETINOBLASTOMA;
    private String GRADE_1;
    private String GRADE_2;
    private String GRADE_3;
    private String PLATINUM_SENSITIVE;
    private String PLATINUM_RESISTANT;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     */
    public CRDBPDXClinicalPatientDataset() {
    }

    public CRDBPDXClinicalPatientDataset(String PATIENT_ID, String DESTINATION_STUDY_ID, String SEX, String ETHNICITY, String RACE, 
                                         String SMOKING_HISTORY, String CROHN_DISEASE, String ULCERATIVE_COLITIS, String BARRETTS_ESOPHAGUS,
                                         String H_PYLORI, String MDS_RISK_FACTOR, String MENOPAUSE_STATUS, String UV_EXPOSURE, String RADIATION_THERAPY,
                                         String BREAST_IMPLANTS, String BRCA, String RETINOBLASTOMA, String GRADE_1, String GRADE_2, 
                                         String GRADE_3, String PLATINUM_SENSITIVE, String PLATINUM_RESISTANT) {
        this.PATIENT_ID = PATIENT_ID == null ? "NA" : PATIENT_ID;
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID == null ? "NA" : DESTINATION_STUDY_ID;
        this.SEX = SEX == null ? "NA" : SEX;
        this.ETHNICITY = ETHNICITY == null ? "NA" : ETHNICITY;
        this.RACE = RACE == null ? "NA" : RACE;
        this.SMOKING_HISTORY = SMOKING_HISTORY == null ? "NA" : SMOKING_HISTORY;
        this.CROHN_DISEASE = CROHN_DISEASE == null ? "NA" : CROHN_DISEASE;
        this.ULCERATIVE_COLITIS = ULCERATIVE_COLITIS == null ? "NA" : ULCERATIVE_COLITIS;
        this.BARRETTS_ESOPHAGUS = BARRETTS_ESOPHAGUS == null ? "NA" : BARRETTS_ESOPHAGUS;
        this.H_PYLORI = H_PYLORI == null ? "NA" : H_PYLORI;
        this.MDS_RISK_FACTOR = MDS_RISK_FACTOR == null ? "NA" : MDS_RISK_FACTOR;
        this.MENOPAUSE_STATUS = MENOPAUSE_STATUS == null ? "NA" : MENOPAUSE_STATUS;
        this.UV_EXPOSURE = UV_EXPOSURE == null ? "NA" : UV_EXPOSURE;
        this.RADIATION_THERAPY = RADIATION_THERAPY == null ? "NA" : RADIATION_THERAPY;
        this.BREAST_IMPLANTS = BREAST_IMPLANTS == null ? "NA" : BREAST_IMPLANTS;
        this.BRCA = BRCA == null ? "NA" : BRCA;
        this.RETINOBLASTOMA = RETINOBLASTOMA == null ? "NA" : RETINOBLASTOMA;
        this.GRADE_1 = GRADE_1 == null ? "NA" : GRADE_1;
        this.GRADE_2 = GRADE_2 == null ? "NA" : GRADE_2;
        this.GRADE_3 = GRADE_3 == null ? "NA" : GRADE_3;
        this.PLATINUM_SENSITIVE = PLATINUM_SENSITIVE == null ? "NA" : PLATINUM_SENSITIVE;
        this.PLATINUM_RESISTANT = PLATINUM_RESISTANT == null ? "NA" : PLATINUM_RESISTANT;
    }
    
    /**
     *
     * @return PATIENT_ID
     */
    public String getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     *
     * @return DESTINATION_STUDY_ID
     */
    public String getDESTINATION_STUDY_ID() {
        return DESTINATION_STUDY_ID;
    }

    /**
     * @param DESTINATION_STUDY_ID
     */
    public void setDESTINATION_STUDY_ID(String DESTINATION_STUDY_ID) {
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID;
    }

    /**
     *
     * @return SEX
     */
    public String getSEX() {
        return SEX;
    }

    /**
     * @param SEX
     */
    public void setSEX(String SEX) {
        this.SEX = SEX;
    }
    
    /**
     *
     * @return ETHNICITY
     */
    public String getETHNICITY() {
        return ETHNICITY;
    }

    /**
     * @param ETHNICITY
     */
    public void setETHNICITY(String ETHNICITY) {
        this.ETHNICITY = ETHNICITY;
    }

    /**
     *
     * @return RACE
     */
    public String getRACE() {
        return RACE;
    }

    /**
     * @param RACE
     */
    public void setRACE(String RACE) {
        this.RACE = RACE;
    }

    /**
     *
     * @return SMOKING_HISTORY
     */
    public String getSMOKING_HISTORY() {
        return SMOKING_HISTORY;
    }

    /**
     * @param SMOKING_HISTORY
     */
    public void setSMOKING_HISTORY(String SMOKING_HISTORY) {
        this.SMOKING_HISTORY = SMOKING_HISTORY;
    }

    public String getCROHN_DISEASE() {
        return CROHN_DISEASE;
    }

    public void setCROHN_DISEASE(String CROHN_DISEASE) {
        this.CROHN_DISEASE = CROHN_DISEASE;
    }

    public String getULCERATIVE_COLITIS() {
        return ULCERATIVE_COLITIS;
    }

    public void setULCERATIVE_COLITIS(String ULCERATIVE_COLITIS) {
        this.ULCERATIVE_COLITIS = ULCERATIVE_COLITIS;
    }

    public String getBARRETTS_ESOPHAGUS() {
        return BARRETTS_ESOPHAGUS;
    }

    public void setBARRETTS_ESOPHAGUS(String BARRETTS_ESOPHAGUS) {
        this.BARRETTS_ESOPHAGUS = BARRETTS_ESOPHAGUS;
    }

    public String getH_PYLORI() {
        return H_PYLORI;
    }

    public void setH_PYLORI(String h_PYLORI) {
        H_PYLORI = h_PYLORI;
    }

    public String getMDS_RISK_FACTOR() {
        return MDS_RISK_FACTOR;
    }

    public void setMDS_RISK_FACTOR(String MDS_RISK_FACTOR) {
        this.MDS_RISK_FACTOR = MDS_RISK_FACTOR;
    }

    public String getMENOPAUSE_STATUS() {
        return MENOPAUSE_STATUS;
    }

    public void setMENOPAUSE_STATUS(String MENOPAUSE_STATUS) {
        this.MENOPAUSE_STATUS = MENOPAUSE_STATUS;
    }

    public String getUV_EXPOSURE() {
        return UV_EXPOSURE;
    }

    public void setUV_EXPOSURE(String UV_EXPOSURE) {
        this.UV_EXPOSURE = UV_EXPOSURE;
    }

    public String getRADIATION_THERAPY() {
        return RADIATION_THERAPY;
    }

    public void setRADIATION_THERAPY(String RADIATION_THERAPY) {
        this.RADIATION_THERAPY = RADIATION_THERAPY;
    }

    public String getBREAST_IMPLANTS() {
        return BREAST_IMPLANTS;
    }

    public void setBREAST_IMPLANTS(String BREAST_IMPLANTS) {
        this.BREAST_IMPLANTS = BREAST_IMPLANTS;
    }

    public String getBRCA() {
        return BRCA;
    }

    public void setBRCA(String BRCA) {
        this.BRCA = BRCA;
    }

    public String getRETINOBLASTOMA() {
        return RETINOBLASTOMA;
    }

    public void setRETINOBLASTOMA(String RETINOBLASTOMA) {
        this.RETINOBLASTOMA = RETINOBLASTOMA;
    }

    public String getGRADE_1() {
        return GRADE_1;
    }

    public void setGRADE_1(String GRADE_1) {
        this.GRADE_1 = GRADE_1;
    }

    public String getGRADE_2() {
        return GRADE_2;
    }

    public void setGRADE_2(String GRADE_2) {
        this.GRADE_2 = GRADE_2;
    }

    public String getGRADE_3() {
        return GRADE_3;
    }

    public void setGRADE_3(String GRADE_3) {
        this.GRADE_3 = GRADE_3;
    }

    public String getPLATINUM_SENSITIVE() {
        return PLATINUM_SENSITIVE;
    }

    public void setPLATINUM_SENSITIVE(String PLATINUM_SENSITIVE) {
        this.PLATINUM_SENSITIVE = PLATINUM_SENSITIVE;
    }

    public String getPLATINUM_RESISTANT() {
        return PLATINUM_RESISTANT;
    }

    public void setPLATINUM_RESISTANT(String PLATINUM_RESISTANT) {
        this.PLATINUM_RESISTANT = PLATINUM_RESISTANT;
    }

    /**
     * Returns the field names in CRDBDataset without additional properties.
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        //fieldNames.add("DESTINATION_STUDY_ID"); // This field is not a true clinical attribute -- do not output
        fieldNames.add("SEX");
        fieldNames.add("ETHNICITY");
        fieldNames.add("RACE");
        fieldNames.add("SMOKING_HISTORY");
        fieldNames.add("CROHN_DISEASE");
        fieldNames.add("ULCERATIVE_COLITIS");
        fieldNames.add("BARRETTS_ESOPHAGUS");
        fieldNames.add("H_PYLORI");
        //fieldNames.add("MDS_RISK_FACTOR"); // This field is not yet available - update reader also when it is
        fieldNames.add("MENOPAUSE_STATUS");
        fieldNames.add("UV_EXPOSURE");
        fieldNames.add("RADIATION_THERAPY");
        fieldNames.add("BREAST_IMPLANTS");
        fieldNames.add("BRCA");
        fieldNames.add("RETINOBLASTOMA");
        fieldNames.add("GRADE_1");
        fieldNames.add("GRADE_2");
        fieldNames.add("GRADE_3");
        fieldNames.add("PLATINUM_SENSITIVE");
        fieldNames.add("PLATINUM_RESISTANT");
        return fieldNames;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
