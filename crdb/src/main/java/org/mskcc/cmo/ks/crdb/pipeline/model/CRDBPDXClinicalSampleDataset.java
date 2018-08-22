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
    private String COLLAB_ID;
    private String AGE_AT_INITIAL_DIAGNOSIS;
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
    private String ER_POSITIVE;
    private String ER_NEGATIVE;
    private String HER2_POSITIVE;
    private String HER2_NEGATIVE;
    private String HPV_POSITIVE;
    private String HPV_NEGATIVE;
    private String P16_POSITIVE;
    private String P16_NEGATIVE;
    private String PR_POSITIVE;
    private String PR_NEGATIVE;
    private String IDH1_POSITIVE;
    private String IDH1_NEGATIVE;
    private String IDH2_POSITIVE;
    private String IDH2_NEGATIVE;
    private String EGFR_POSITIVE;
    private String EGFR_NEGATIVE;
    private String ALK_POSITIVE;
    private String ALK_NEGATIVE;
    private String BRCA1_POSITIVE;
    private String BRCA1_NEGATIVE;
    private String BRCA2_POSITIVE;
    private String BRCA2_NEGATIVE;
    private String C_MYC_POSITIVE;
    private String C_MYC_NEGATIVE;
    private String AR_POSITIVE;
    private String AR_NEGATIVE;
    private String KRAS_POSITIVE;
    private String KRAS_NEGATIVE;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    
    /**
    * No args constructor for use in serialization
    *
    */
    public CRDBPDXClinicalSampleDataset() {
    }

    public CRDBPDXClinicalSampleDataset(String PATIENT_ID, String SAMPLE_ID, String PDX_ID, String DESTINATION_STUDY_ID, 
                                        String COLLAB_ID, String AGE_AT_INITIAL_DIAGNOSIS, String PASSAGE_ID, String ONCOTREE_CODE, 
                                        String STAGE_CODE, String T_STAGE, String N_STAGE, String M_STAGE, String GRADE, 
                                        String SAMPLE_TYPE, String PRIMARY_SITE, String SAMPLE_CLASS, String PROCEDURE_TYPE, 
                                        String PRETREATED, String TREATED, String ER_POSITIVE, String ER_NEGATIVE, 
                                        String HER2_POSITIVE, String HER2_NEGATIVE, String HPV_POSITIVE, String HPV_NEGATIVE, 
                                        String P16_POSITIVE, String P16_NEGATIVE, String PR_POSITIVE, String PR_NEGATIVE, 
                                        String IDH1_POSITIVE, String IDH1_NEGATIVE, String IDH2_POSITIVE, String IDH2_NEGATIVE, 
                                        String EGFR_POSITIVE, String EGFR_NEGATIVE, String ALK_POSITIVE, String ALK_NEGATIVE, 
                                        String BRCA1_POSITIVE, String BRCA1_NEGATIVE, String BRCA2_POSITIVE, String BRCA2_NEGATIVE, 
                                        String C_MYC_POSITIVE, String C_MYC_NEGATIVE, String AR_POSITIVE, String AR_NEGATIVE, 
                                        String KRAS_POSITIVE, String KRAS_NEGATIVE) { 
        this.PATIENT_ID = PATIENT_ID == null ? "NA" : PATIENT_ID;
        this.SAMPLE_ID = SAMPLE_ID == null ? "NA" : SAMPLE_ID;
        this.PDX_ID = PDX_ID == null ? "NA" : PDX_ID;
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID == null ? "NA" : DESTINATION_STUDY_ID;
        this.COLLAB_ID = COLLAB_ID == null ? "NA" : COLLAB_ID;
        this.AGE_AT_INITIAL_DIAGNOSIS = AGE_AT_INITIAL_DIAGNOSIS == null ? "NA" : AGE_AT_INITIAL_DIAGNOSIS;
        this.PASSAGE_ID = PASSAGE_ID == null ? "NA" : PASSAGE_ID;
        this.ONCOTREE_CODE = ONCOTREE_CODE == null ? "NA" : ONCOTREE_CODE;
        this.STAGE_CODE = STAGE_CODE == null ? "NA" : STAGE_CODE;
        this.T_STAGE = T_STAGE == null ? "NA" : T_STAGE;
        this.N_STAGE = N_STAGE == null ? "NA" : N_STAGE;
        this.M_STAGE = M_STAGE == null ? "NA" : M_STAGE;
        this.GRADE = GRADE == null ? "NA" : GRADE;
        this.SAMPLE_TYPE = SAMPLE_TYPE == null ? "NA" : SAMPLE_TYPE;
        this.PRIMARY_SITE = PRIMARY_SITE == null ? "NA" : PRIMARY_SITE;
        this.SAMPLE_CLASS = SAMPLE_CLASS == null ? "NA" : SAMPLE_CLASS;
        this.PROCEDURE_TYPE = PROCEDURE_TYPE == null ? "NA" : PROCEDURE_TYPE;
        this.PRETREATED = PRETREATED == null ? "NA" : PRETREATED;
        this.TREATED = TREATED == null ? "NA" : TREATED;
        this.ER_POSITIVE = ER_POSITIVE == null ? "NA" : ER_POSITIVE;
        this.ER_NEGATIVE = ER_NEGATIVE == null ? "NA" : ER_NEGATIVE;
        this.HER2_POSITIVE = HER2_POSITIVE == null ? "NA" : HER2_POSITIVE;
        this.HER2_NEGATIVE = HER2_NEGATIVE == null ? "NA" : HER2_NEGATIVE;
        this.HPV_POSITIVE = HPV_POSITIVE == null ? "NA" : HPV_POSITIVE;
        this.HPV_NEGATIVE = HPV_NEGATIVE == null ? "NA" : HPV_NEGATIVE;
        this.P16_POSITIVE = P16_POSITIVE == null ? "NA" : P16_POSITIVE;
        this.P16_NEGATIVE = P16_NEGATIVE == null ? "NA" : P16_NEGATIVE;
        this.PR_POSITIVE = PR_POSITIVE == null ? "NA" : PR_POSITIVE;
        this.PR_NEGATIVE = PR_NEGATIVE == null ? "NA" : PR_NEGATIVE;
        this.IDH1_POSITIVE = IDH1_POSITIVE == null ? "NA" : IDH1_POSITIVE;
        this.IDH1_NEGATIVE = IDH1_NEGATIVE == null ? "NA" : IDH1_NEGATIVE;
        this.IDH2_POSITIVE = IDH2_POSITIVE == null ? "NA" : IDH2_POSITIVE;
        this.IDH2_NEGATIVE = IDH2_NEGATIVE == null ? "NA" : IDH2_NEGATIVE;
        this.EGFR_POSITIVE = EGFR_POSITIVE == null ? "NA" : EGFR_POSITIVE;
        this.EGFR_NEGATIVE = EGFR_NEGATIVE == null ? "NA" : EGFR_NEGATIVE;
        this.ALK_POSITIVE = ALK_POSITIVE == null ? "NA" : ALK_POSITIVE;
        this.ALK_NEGATIVE = ALK_NEGATIVE == null ? "NA" : ALK_NEGATIVE;
        this.BRCA1_POSITIVE = BRCA1_POSITIVE == null ? "NA" : BRCA1_POSITIVE;
        this.BRCA1_NEGATIVE = BRCA1_NEGATIVE == null ? "NA" : BRCA1_NEGATIVE;
        this.BRCA2_POSITIVE = BRCA2_POSITIVE == null ? "NA" : BRCA2_POSITIVE;
        this.BRCA2_NEGATIVE = BRCA2_NEGATIVE == null ? "NA" : BRCA2_NEGATIVE;
        this.C_MYC_POSITIVE = C_MYC_POSITIVE == null ? "NA" : C_MYC_POSITIVE;
        this.C_MYC_NEGATIVE = C_MYC_NEGATIVE == null ? "NA" : C_MYC_NEGATIVE;
        this.AR_POSITIVE = AR_POSITIVE == null ? "NA" : AR_POSITIVE;
        this.AR_NEGATIVE = AR_NEGATIVE == null ? "NA" : AR_NEGATIVE;
        this.KRAS_POSITIVE = KRAS_POSITIVE == null ? "NA" : KRAS_POSITIVE;
        this.KRAS_NEGATIVE = KRAS_NEGATIVE == null ? "NA" : KRAS_NEGATIVE;
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
     * @return COLLAB_ID
     */
    public String getCOLLAB_ID() {
        return COLLAB_ID;
    }

    /**
     * @param COLLAB_ID
     */
    public void setCOLLAB_ID(String COLLAB_ID) {
        this.COLLAB_ID = COLLAB_ID;
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
     *
     * @return ER_POSITIVE
     */
    public String getER_POSITIVE() {
        return ER_POSITIVE;
    }

    public void setER_POSITIVE(String ER_POSITIVE) {
        this.ER_POSITIVE = ER_POSITIVE;
    }

    /**
     *
     * @return ER_NEGATIVE
     */
    public String getER_NEGATIVE() {
        return ER_NEGATIVE;
    }

    /**
     * @param ER_NEGATIVE
     */  
    public void setER_NEGATIVE(String ER_NEGATIVE) {
        this.ER_NEGATIVE = ER_NEGATIVE;
    }

    /**
     *
     * @return HER2_POSITIVE;
     */
    public String getHER2_POSITIVE() {
        return HER2_POSITIVE;
    }
    
    /**
     * @param HER2_POSITIVE
     */ 
    public void setHER2_POSITIVE(String HER2_POSITIVE) {
        this.HER2_POSITIVE = HER2_POSITIVE;
    }

    /**
     *
     * @return HER2_NEGATIVE
     */
    public String getHER2_NEGATIVE() {
        return HER2_NEGATIVE;
    }

    /**
     * @param HER2_NEGATIVE
     */ 
    public void setHER2_NEGATIVE(String HER2_NEGATIVE) {
        this.HER2_NEGATIVE = HER2_NEGATIVE;
    }

    /**
     *
     * @return HPV_POSITIVE
     */
    public String getHPV_POSITIVE() {
        return HPV_POSITIVE;
    }

    /**
     * @param HPV_POSITIVE
     */ 
    public void setHPV_POSITIVE(String HPV_POSITIVE) {
        this.HPV_POSITIVE = HPV_POSITIVE;
    }

    /**
     *
     * @return HPV_NEGATIVE
     */
    public String getHPV_NEGATIVE() {
        return HPV_NEGATIVE;
    }

    /**
     * @param HPV_NEGATIVE
     */ 
    public void setHPV_NEGATIVE(String HPV_NEGATIVE) {
        this.HPV_NEGATIVE = HPV_NEGATIVE;
    }

    /**
     *
     * @return P16_POSITIVE
     */
    public String getP16_POSITIVE() {
        return P16_POSITIVE;
    }

    /**
     * @param P16_POSITIVE
     */ 
    public void setP16_POSITIVE(String p16_POSITIVE) {
        P16_POSITIVE = p16_POSITIVE;
    }

    /**
     *
     * @return P16_NEGATIVE
     */
    public String getP16_NEGATIVE() {
        return P16_NEGATIVE;
    }

    /**
     * @param P16_NEGATIVE
     */ 
    public void setP16_NEGATIVE(String p16_NEGATIVE) {
        P16_NEGATIVE = p16_NEGATIVE;
    }

    /**
     *
     * @return PR_POSITIVE
     */
    public String getPR_POSITIVE() {
        return PR_POSITIVE;
    }

    /**
     * @param PR_POSITIVE
     */ 
    public void setPR_POSITIVE(String PR_POSITIVE) {
        this.PR_POSITIVE = PR_POSITIVE;
    }

    /**
     *
     * @return PR_NEGATIVE
     */
    public String getPR_NEGATIVE() {
        return PR_NEGATIVE;
    }

    /**
     * @param PR_NEGATIVE
     */ 
    public void setPR_NEGATIVE(String PR_NEGATIVE) {
        this.PR_NEGATIVE = PR_NEGATIVE;
    }

    /**
     *
     * @return IDH1_POSITIVE
     */
    public String getIDH1_POSITIVE() {
        return IDH1_POSITIVE;
    }

    /**
     * @param IDH1_POSITIVE
     */ 
    public void setIDH1_POSITIVE(String IDH1_POSITIVE) {
        this.IDH1_POSITIVE = IDH1_POSITIVE;
    }

    /**
     *
     * @return IDH1_NEGATIVE
     */
    public String getIDH1_NEGATIVE() {
        return IDH1_NEGATIVE;
    }

    /**
     * @param IDH1_NEGATIVE
     */ 
    public void setIDH1_NEGATIVE(String IDH1_NEGATIVE) {
        this.IDH1_NEGATIVE = IDH1_NEGATIVE;
    }

    /**
     *
     * @return IDH2_POSITIVE
     */
    public String getIDH2_POSITIVE() {
        return IDH2_POSITIVE;
    }

    /**
     * @param IDH2_POSITIVE
     */ 
    public void setIDH2_POSITIVE(String IDH2_POSITIVE) {
        this.IDH2_POSITIVE = IDH2_POSITIVE;
    }

    /**
     *
     * @return IDH2_NEGATIVE
     */
    public String getIDH2_NEGATIVE() {
        return IDH2_NEGATIVE;
    }

    /**
     * @param IDH2_NEGATIVE
     */ 
    public void setIDH2_NEGATIVE(String IDH2_NEGATIVE) {
        this.IDH2_NEGATIVE = IDH2_NEGATIVE;
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
     * @return EGFR_NEGATIVE
     */
    public String getEGFR_NEGATIVE() {
        return EGFR_NEGATIVE;
    }

    /**
     * @param EGFR_NEGATIVE
     */ 
    public void setEGFR_NEGATIVE(String EGFR_NEGATIVE) {
        this.EGFR_NEGATIVE = EGFR_NEGATIVE;
    }

    /**
     *
     * @return ALK_POSITIVE
     */
    public String getALK_POSITIVE() {
        return ALK_POSITIVE;
    }

    /**
     * @param ALK_POSITIVE
     */ 
    public void setALK_POSITIVE(String ALK_POSITIVE) {
        this.ALK_POSITIVE = ALK_POSITIVE;
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
     * @return BRCA1_POSITIVE
     */
    public String getBRCA1_POSITIVE() {
        return BRCA1_POSITIVE;
    }

    /**
     * @param BRCA1_POSITIVE
     */ 
    public void setBRCA1_POSITIVE(String BRCA1_POSITIVE) {
        this.BRCA1_POSITIVE = BRCA1_POSITIVE;
    }

    /**
     *
     * @return BRCA1_NEGATIVE
     */
    public String getBRCA1_NEGATIVE() {
        return BRCA1_NEGATIVE;
    }

    /**
     * @param BRCA1_NEGATIVE
     */ 
    public void setBRCA1_NEGATIVE(String BRCA1_NEGATIVE) {
        this.BRCA1_NEGATIVE = BRCA1_NEGATIVE;
    }

    /**
     *
     * @return BRCA2_POSITIVE
     */
    public String getBRCA2_POSITIVE() {
        return BRCA2_POSITIVE;
    }

    /**
     * @param BRCA2_POSITIVE
     */ 
    public void setBRCA2_POSITIVE(String BRCA2_POSITIVE) {
        this.BRCA2_POSITIVE = BRCA2_POSITIVE;
    }

    /**
     *
     * @return BRCA2_NEGATIVE
     */
    public String getBRCA2_NEGATIVE() {
        return BRCA2_NEGATIVE;
    }

    /**
     * @param BRCA2_NEGATIVE
     */ 
    public void setBRCA2_NEGATIVE(String BRCA2_NEGATIVE) {
        this.BRCA2_NEGATIVE = BRCA2_NEGATIVE;
    }

    /**
     *
     * @return C_MYC_POSITIVE
     */
    public String getC_MYC_POSITIVE() {
        return C_MYC_POSITIVE;
    }

    /**
     * @param C_MYC_POSITIVE
     */ 
    public void setC_MYC_POSITIVE(String c_MYC_POSITIVE) {
        C_MYC_POSITIVE = c_MYC_POSITIVE;
    }

    /**
     *
     * @return C_MYC_NEGATIVE
     */
    public String getC_MYC_NEGATIVE() {
        return C_MYC_NEGATIVE;
    }

    /**
     * @param C_MYC_NEGATIVE
     */ 
    public void setC_MYC_NEGATIVE(String c_MYC_NEGATIVE) {
        C_MYC_NEGATIVE = c_MYC_NEGATIVE;
    }

    /**
     *
     * @return AR_POSITIVE
     */
    public String getAR_POSITIVE() {
        return AR_POSITIVE;
    }

    /**
     * @param AR_POSITIVE
     */ 
    public void setAR_POSITIVE(String AR_POSITIVE) {
        this.AR_POSITIVE = AR_POSITIVE;
    }

    /**
     *
     * @return AR_NEGATIVE
     */
    public String getAR_NEGATIVE() {
        return AR_NEGATIVE;
    }

    /**
     * @param AR_NEGATIVE
     */ 
    public void setAR_NEGATIVE(String AR_NEGATIVE) {
        this.AR_NEGATIVE = AR_NEGATIVE;
    }

    /**
     *
     * @return KRAS_POSITIVE
     */
    public String getKRAS_POSITIVE() {
        return KRAS_POSITIVE;
    }

    /**
     * @param KRAS_POSITIVE
     */ 
    public void setKRAS_POSITIVE(String KRAS_POSITIVE) {
        this.KRAS_POSITIVE = KRAS_POSITIVE;
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
     * Returns the field names in CRDBDataset without additional properties.
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("PDX_ID");
        //fieldNames.add("DESTINATION_STUDY_ID"); // This field is not a true clinical attribute -- do not output
        //fieldNames.add("COLLAB_ID");  // This field is not yet available - update reader also when it is
        fieldNames.add("AGE_AT_INITIAL_DIAGNOSIS");
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
        fieldNames.add("ER_POSITIVE");
        fieldNames.add("ER_NEGATIVE");
        fieldNames.add("HER2_POSITIVE");
        fieldNames.add("HER2_NEGATIVE");
        fieldNames.add("HPV_POSITIVE");
        fieldNames.add("HPV_NEGATIVE");
        fieldNames.add("P16_POSITIVE");
        fieldNames.add("P16_NEGATIVE");
        fieldNames.add("PR_POSITIVE");
        fieldNames.add("PR_NEGATIVE");
        fieldNames.add("IDH1_POSITIVE");
        fieldNames.add("IDH1_NEGATIVE");
        fieldNames.add("IDH2_POSITIVE");
        fieldNames.add("IDH2_NEGATIVE");
        fieldNames.add("EGFR_POSITIVE");
        fieldNames.add("EGFR_NEGATIVE");
        fieldNames.add("ALK_POSITIVE");
        fieldNames.add("ALK_NEGATIVE");
        fieldNames.add("BRCA1_POSITIVE");
        fieldNames.add("BRCA1_NEGATIVE");
        fieldNames.add("BRCA2_POSITIVE");
        fieldNames.add("BRCA2_NEGATIVE");
        fieldNames.add("C_MYC_POSITIVE");
        fieldNames.add("C_MYC_NEGATIVE");
        fieldNames.add("AR_POSITIVE");
        fieldNames.add("AR_NEGATIVE");
        fieldNames.add("KRAS_POSITIVE");
        fieldNames.add("KRAS_NEGATIVE");
        return fieldNames;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
