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
 * Model for CRDBPDXClinicalSampleDataset results.
 *
 * @author averyniceday
 */

public class CRDBPDXClinicalSampleDataset {
    private String PATIENT_ID;
    private String SAMPLE_ID;
    private String PDX_ID;
    private String DESTINATION_STUDY_ID;
    private String AGE_AT_INITIAL_DIAGNOSIS;
    private String EGFR_POSITIVE;
    private String ALK_NEGATIVE;
    private String KRAS_NEGATIVE;
    private String PASSAGE_ID;
    private String ONCOTREE_CODE;
    private String STAGE_CODE;
    private String T_STAGE;
    private String N_STAGE;
    private String M_STAGE;
    private String GRADE;
    private String SAMPLE_TYPE;
    private String PRIMARY_SITE;
    private String SAMPLE_CLASS;
    private String PROCEDURE_TYPE;
    private String PRETREATED;
    private String TREATED;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    
    /**
    * No args constructor for use in serialization
    *
    */
    public CRDBPDXClinicalSampleDataset() {
    }


    public CRDBPDXClinicalSampleDataset(String PATIENT_ID, String SAMPLE_ID, String PDX_ID, String DESTINATION_STUDY_ID, 
                                        String AGE_AT_INITIAL_DIAGNOSIS, String EGFR_POSITIVE, String ALK_NEGATIVE, 
                                        String KRAS_NEGATIVE, String PASSAGE_ID, String ONCOTREE_CODE, String STAGE_CODE, 
                                        String t_STAGE, String n_STAGE, String m_STAGE, String GRADE, String SAMPLE_TYPE,
                                        String PRIMARY_SITE, String SAMPLE_CLASS, String PROCEDURE_TYPE, String PRETREATED, 
                                        String TREATED) {
        this.PATIENT_ID = PATIENT_ID == null ? "NA" : PATIENT_ID;
        this.SAMPLE_ID = SAMPLE_ID == null ? "NA" : SAMPLE_ID;
        this.PDX_ID = PDX_ID == null ? "NA" : PDX_ID;
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID == null ? "NA" : DESTINATION_STUDY_ID;
        this.AGE_AT_INITIAL_DIAGNOSIS = AGE_AT_INITIAL_DIAGNOSIS == null ? "NA" : AGE_AT_INITIAL_DIAGNOSIS;
        this.EGFR_POSITIVE = EGFR_POSITIVE == null ? "NA" : EGFR_POSITIVE;
        this.ALK_NEGATIVE = ALK_NEGATIVE == null ? "NA" : ALK_NEGATIVE;
        this.KRAS_NEGATIVE = KRAS_NEGATIVE == null ? "NA" : KRAS_NEGATIVE;
        this.PASSAGE_ID = PASSAGE_ID == null ? "NA" : PASSAGE_ID;
        this.ONCOTREE_CODE = ONCOTREE_CODE == null ? "NA" : ONCOTREE_CODE;
        this.STAGE_CODE = STAGE_CODE == null ? "NA" : STAGE_CODE;
        this.T_STAGE = t_STAGE == null ? "NA" : t_STAGE;
        this.N_STAGE = n_STAGE == null ? "NA" : n_STAGE;
        this.M_STAGE = m_STAGE == null ? "NA" : m_STAGE;
        this.GRADE = GRADE == null ? "NA" : GRADE;
        this.SAMPLE_TYPE = SAMPLE_TYPE == null ? "NA" : SAMPLE_TYPE;
        this.PRIMARY_SITE = PRIMARY_SITE == null ? "NA" : PRIMARY_SITE;
        this.SAMPLE_CLASS = SAMPLE_CLASS == null ? "NA" : SAMPLE_CLASS;
        this.PROCEDURE_TYPE = PROCEDURE_TYPE == null ? "NA" : PROCEDURE_TYPE;
        this.PRETREATED = PRETREATED == null ? "NA" : PRETREATED;
        this.TREATED = TREATED == null ? "NA" : TREATED;
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
     * @return SAMPLE_ID
     */
    public String getSAMPLE_ID() {
        return SAMPLE_ID;
    }

    /**
     * @param SAMPLE_ID
     */
    public void setSAMPLE_ID(String SAMPLE_ID) {
        this.SAMPLE_ID = SAMPLE_ID;
    }

    /**
     *
     * @return PDX_ID
     */
    public String getPDX_ID() {
        return PDX_ID;
    }

    /**
     * @param PDX_ID
     */
    public void setPDX_ID(String PDX_ID) {
        this.PDX_ID = PDX_ID;
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
     * @return AGE_AT_INITIAL_DIAGNOSIS
     */
    public String getAGE_AT_INITIAL_DIAGNOSIS() {
        return AGE_AT_INITIAL_DIAGNOSIS;
    }

    /**
     * @param AGE_AT_INITIAL_DIAGNOSIS
     */
    public void setAGE_AT_INITIAL_DIAGNOSIS(String AGE_AT_INITIAL_DIAGNOSIS) {
        this.AGE_AT_INITIAL_DIAGNOSIS = AGE_AT_INITIAL_DIAGNOSIS;
    }

    /**
     *
     * @return EGFR_POSITIVE
     */
    public String getEGFR_POSITIVE() {
        return EGFR_POSITIVE;
    }

    
    /**
     * @param EGFR_POSITIVE
     */
    public void setEGFR_POSITIVE(String EGFR_POSITIVE) {
        this.EGFR_POSITIVE = EGFR_POSITIVE;
    }

    /**
     *
     * @return ALK_NEGATIVE
     */
    public String getALK_NEGATIVE() {
        return ALK_NEGATIVE;
    }

    /**
     * @param ALK_NEGATIVE
     */
    public void setALK_NEGATIVE(String ALK_NEGATIVE) {
        this.ALK_NEGATIVE = ALK_NEGATIVE;
    }

    /**
     *
     * @return KRAS_NEGATIVE
     */
    public String getKRAS_NEGATIVE() {
        return KRAS_NEGATIVE;
    }

    /**
     * @param KRAS_NEGATIVE
     */
    public void setKRAS_NEGATIVE(String KRAS_NEGATIVE) {
        this.KRAS_NEGATIVE = KRAS_NEGATIVE;
    }

    /**
     *
     * @return PASSAGE_ID
     */
    public String getPASSAGE_ID() {
        return PASSAGE_ID;
    }

    /**
     * @param PASSAGE_ID
     */
    public void setPASSAGE_ID(String PASSAGE_ID) {
        this.PASSAGE_ID = PASSAGE_ID;
    }

    /**
     *
     * @return ONCOTREE_CODE
     */
    public String getONCOTREE_CODE() {
        return ONCOTREE_CODE;
    }

    /**
     * @param ONCOTREE_CODE
     */
    public void setONCOTREE_CODE(String ONCOTREE_CODE) {
        this.ONCOTREE_CODE = ONCOTREE_CODE;
    }

    /**
     *
     * @return STAGE_CODE
     */
    public String getSTAGE_CODE() {
        return STAGE_CODE;
    }

    /**
     * @param STAGE_CODE
     */
    public void setSTAGE_CODE(String STAGE_CODE) {
        this.STAGE_CODE = STAGE_CODE;
    }

    /**
     *
     * @return T_STAGE
     */
    public String getT_STAGE() {
        return T_STAGE;
    }

    /**
     * @param T_STAGE
     */
    public void setT_STAGE(String t_STAGE) {
        T_STAGE = t_STAGE;
    }

    /**
     *
     * @return N_STAGE
     */
    public String getN_STAGE() {
        return N_STAGE;
    }

    /**
     * @param N_STAGE
     */
    public void setN_STAGE(String n_STAGE) {
        N_STAGE = n_STAGE;
    }

    /**
     *
     * @return M_STAGE
     */
    public String getM_STAGE() {
        return M_STAGE;
    }

    /**
     * @param M_STAGE
     */
    public void setM_STAGE(String m_STAGE) {
        M_STAGE = m_STAGE;
    }

    /**
     *
     * @return GRADE
     */
    public String getGRADE() {
        return GRADE;
    }

    /**
     * @param GRADE
     */
    public void setGRADE(String GRADE) {
        this.GRADE = GRADE;
    }

    /**
     *
     * @return SAMPLE_TYPE
     */
    public String getSAMPLE_TYPE() {
        return SAMPLE_TYPE;
    }

    /**
     * @param SAMPLE_TYPE
     */
    public void setSAMPLE_TYPE(String SAMPLE_TYPE) {
        this.SAMPLE_TYPE = SAMPLE_TYPE;
    }

    /**
    *
    * @return PRIMARY_SITE
    */
    public String getPRIMARY_SITE() {
        return PRIMARY_SITE;
    }

    /**
    * @param PRIMARY_SITE
    */
    public void setPRIMARY_SITE(String PRIMARY_SITE) {
        this.PRIMARY_SITE = PRIMARY_SITE;
    }

    /**
     *
     * @return SAMPLE_CLASS
     */
    public String getSAMPLE_CLASS() {
        return SAMPLE_CLASS;
    }

    /**
     * @param SAMPLE_CLASS
     */
    public void setSAMPLE_CLASS(String SAMPLE_CLASS) {
        this.SAMPLE_CLASS = SAMPLE_CLASS;
    }

    /**
     *
     * @return PROCEDURE_TYPE
     */
    public String getPROCEDURE_TYPE() {
        return PROCEDURE_TYPE;
    }

    /**
     * @param PROCEDURE_TYPE
     */
    public void setPROCEDURE_TYPE(String PROCEDURE_TYPE) {
        this.PROCEDURE_TYPE = PROCEDURE_TYPE;
    }

    /**
     *
     * @return PRETREATED
     */
    public String getPRETREATED() {
        return PRETREATED;
    }

    /**
     * @param PRETREATED
     */
    public void setPRETREATED(String PRETREATED) {
        this.PRETREATED = PRETREATED;
    }

    /**
     *
     * @return TREATED
     */
    public String getTREATED() {
        return TREATED;
    }

    /**
     * @param TREATED
     */
    public void setTREATED(String TREATED) {
        this.TREATED = TREATED;
    }

    /**
     * Returns the field names in CRDBDataset without additional properties.
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PDX_ID");
        fieldNames.add("DESTINATION_STUDY_ID");
        fieldNames.add("AGE_AT_INITIAL_DIAGNOSIS");
        fieldNames.add("EGFR_POSITIVE");
        fieldNames.add("ALK_NEGATIVE");
        fieldNames.add("KRAS_NEGATIVE");
        fieldNames.add("PASSAGE_ID");
        fieldNames.add("ONCOTREE_CODE");
        fieldNames.add("STAGE_CODE");
        fieldNames.add("T_STAGE");
        fieldNames.add("N_STAGE");
        fieldNames.add("M_STAGE");
        fieldNames.add("GRADE");
        fieldNames.add("SAMPLE_TYPE");
        fieldNames.add("PRIMARY_SITE");
        fieldNames.add("SAMPLE_CLASS");
        fieldNames.add("PROCEDURE_TYPE");
        fieldNames.add("PRETREATED");
        fieldNames.add("TREATED");
        return fieldNames;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
