/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.cbioportal.cmo.pipelines.crdb.model;

import java.util.*;

import org.apache.commons.lang.builder.ToStringBuilder;

public class CRDBDataset {
    private String DMP_ID;
    private String CONSENT_DATE_DAYS;
    private String PRIM_DISEASE_12245;
    private String INITIAL_SX_DAYS;
    private String INITIAL_DX_DAYS;
    private String FIRST_METASTASIS_DAYS;
    private String INIT_DX_STATUS_ID;
    private String INIT_DX_STATUS;
    private String INIT_DX_STATUS_DAYS;
    private String INIT_DX_STAGING_DSCRP;
    private String INIT_DX_STAGE;
    private String INIT_DX_STAGE_DSCRP;
    private String INIT_DX_GRADE;
    private String INIT_DX_GRADE_DSCRP;
    private String INIT_DX_T_STAGE;
    private String INIT_DX_T_STAGE_DSCRP;
    private String INIT_DX_N_STAGE;
    private String INIT_DX_N_STAGE_DSCRP;
    private String INIT_DX_M_STAGE;
    private String INIT_DX_M_STAGE_DSCRP;
    private String INIT_DX_HIST;
    private String INIT_DX_SUB_HIST;
    private String INIT_DX_SUB_SUB_HIST;
    private String INIT_DX_SUB_SUB_SUB_HIST;
    private String INIT_DX_SITE;
    private String INIT_DX_SUB_SITE;
    private String INIT_DX_SUB_SUB_SITE;
    private String ENROLL_DX_STATUS_ID;
    private String ENROLL_DX_STATUS;
    private String ENROLL_DX_STATUS_DAYS;
    private String ENROLL_DX_STAGING_DSCRP;
    private String ENROLL_DX_STAGE;
    private String ENROLL_DX_STAGE_DSCRP;
    private String ENROLL_DX_GRADE;
    private String ENROLL_DX_GRADE_DSCRP;
    private String ENROLL_DX_T_STAGE;
    private String ENROLL_DX_T_STAGE_DSCRP;
    private String ENROLL_DX_N_STAGE;
    private String ENROLL_DX_N_STAGE_DSCRP;
    private String ENROLL_DX_M_STAGE;
    private String ENROLL_DX_M_STAGE_DSCRP;
    private String ENROLL_DX_HIST;
    private String ENROLL_DX_SUB_HIST;
    private String ENROLL_DX_SUB_SUB_HIST;
    private String ENROLL_DX_SUB_SUB_SUB_HIST;
    private String ENROLL_DX_SITE;
    private String ENROLL_DX_SUB_SITE;
    private String ENROLL_DX_SUB_SUB_SITE;
    private String SURVIVAL_STATUS;
    private String TREATMENT_END_DAYS;
    private String OFF_STUDY_DAYS;
    private String COMMENTS;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

/**
* No args constructor for use in serialization
* 
*/
public CRDBDataset() {
}

/**
* @param   DMP_ID
* @param  CONSENT_DATE_DAYS
* @param  PRIM_DISEASE_12245
* @param  INITIAL_SX_DAYS
* @param  INITIAL_DX_DAYS
* @param  FIRST_METASTASIS_DAYS
* @param  INIT_DX_STATUS_ID
* @param  INIT_DX_STATUS
* @param  INIT_DX_STATUS_DAYS
* @param  INIT_DX_STAGING_DSCRP
* @param  INIT_DX_STAGE
* @param  INIT_DX_STAGE_DSCRP
* @param  INIT_DX_GRADE
* @param  INIT_DX_GRADE_DSCRP
* @param  INIT_DX_T_STAGE
* @param  INIT_DX_T_STAGE_DSCRP
* @param  INIT_DX_N_STAGE
* @param  INIT_DX_N_STAGE_DSCRP
* @param  INIT_DX_M_STAGE
* @param  INIT_DX_M_STAGE_DSCRP
* @param  INIT_DX_HIST
* @param  INIT_DX_SUB_HIST
* @param  INIT_DX_SUB_SUB_HIST
* @param  INIT_DX_SUB_SUB_SUB_HIST
* @param  INIT_DX_SITE
* @param  INIT_DX_SUB_SITE
* @param  INIT_DX_SUB_SUB_SITE
* @param  ENROLL_DX_STATUS_ID
* @param  ENROLL_DX_STATUS
* @param  ENROLL_DX_STATUS_DAYS
* @param  ENROLL_DX_STAGING_DSCRP
* @param  ENROLL_DX_STAGE
* @param  ENROLL_DX_STAGE_DSCRP
* @param  ENROLL_DX_GRADE
* @param  ENROLL_DX_GRADE_DSCRP
* @param  ENROLL_DX_T_STAGE
* @param  ENROLL_DX_T_STAGE_DSCRP
* @param  ENROLL_DX_N_STAGE
* @param  ENROLL_DX_N_STAGE_DSCRP
* @param  ENROLL_DX_M_STAGE
* @param  ENROLL_DX_M_STAGE_DSCRP
* @param  ENROLL_DX_HIST
* @param  ENROLL_DX_SUB_HIST
* @param  ENROLL_DX_SUB_SUB_HIST
* @param  ENROLL_DX_SUB_SUB_SUB_HIST
* @param  ENROLL_DX_SITE
* @param  ENROLL_DX_SUB_SITE
* @param  ENROLL_DX_SUB_SUB_SITE
* @param  SURVIVAL_STATUS
* @param  TREATMENT_END_DAYS
* @param  OFF_STUDY_DAYS
* @param  COMMENTS
**/
public CRDBDataset(String DMP_ID, String CONSENT_DATE_DAYS, String PRIM_DISEASE_12245, 
        String INITIAL_SX_DAYS, String INITIAL_DX_DAYS, String FIRST_METASTASIS_DAYS, 
        String INIT_DX_STATUS_ID, String INIT_DX_STATUS, String INIT_DX_STATUS_DAYS, 
        String INIT_DX_STAGING_DSCRP, String INIT_DX_STAGE, String INIT_DX_STAGE_DSCRP, 
        String INIT_DX_GRADE, String INIT_DX_GRADE_DSCRP, String INIT_DX_T_STAGE, 
        String INIT_DX_T_STAGE_DSCRP, String INIT_DX_N_STAGE, String INIT_DX_N_STAGE_DSCRP, 
        String INIT_DX_M_STAGE, String INIT_DX_M_STAGE_DSCRP, String INIT_DX_HIST, 
        String INIT_DX_SUB_HIST, String INIT_DX_SUB_SUB_HIST, String INIT_DX_SUB_SUB_SUB_HIST, 
        String INIT_DX_SITE, String INIT_DX_SUB_SITE, String INIT_DX_SUB_SUB_SITE, 
        String ENROLL_DX_STATUS_ID, String ENROLL_DX_STATUS, String ENROLL_DX_STATUS_DAYS, 
        String ENROLL_DX_STAGING_DSCRP, String ENROLL_DX_STAGE, String ENROLL_DX_STAGE_DSCRP, 
        String ENROLL_DX_GRADE, String ENROLL_DX_GRADE_DSCRP, String ENROLL_DX_T_STAGE, 
        String ENROLL_DX_T_STAGE_DSCRP, String ENROLL_DX_N_STAGE, String ENROLL_DX_N_STAGE_DSCRP, 
        String ENROLL_DX_M_STAGE, String ENROLL_DX_M_STAGE_DSCRP, String ENROLL_DX_HIST, 
        String ENROLL_DX_SUB_HIST, String ENROLL_DX_SUB_SUB_HIST, String ENROLL_DX_SUB_SUB_SUB_HIST, 
        String ENROLL_DX_SITE, String ENROLL_DX_SUB_SITE, String ENROLL_DX_SUB_SUB_SITE, 
        String SURVIVAL_STATUS, String TREATMENT_END_DAYS, String OFF_STUDY_DAYS, String COMMENTS) {

    this.DMP_ID = DMP_ID;
    this.CONSENT_DATE_DAYS = CONSENT_DATE_DAYS;
    this.PRIM_DISEASE_12245 = PRIM_DISEASE_12245;
    this.INITIAL_SX_DAYS = INITIAL_SX_DAYS;
    this.INITIAL_DX_DAYS = INITIAL_DX_DAYS;
    this.FIRST_METASTASIS_DAYS = FIRST_METASTASIS_DAYS;
    this.INIT_DX_STATUS_ID = INIT_DX_STATUS_ID;
    this.INIT_DX_STATUS = INIT_DX_STATUS;
    this.INIT_DX_STATUS_DAYS = INIT_DX_STATUS_DAYS;
    this.INIT_DX_STAGING_DSCRP = INIT_DX_STAGING_DSCRP;
    this.INIT_DX_STAGE = INIT_DX_STAGE;
    this.INIT_DX_STAGE_DSCRP = INIT_DX_STAGE_DSCRP;
    this.INIT_DX_GRADE = INIT_DX_GRADE;
    this.INIT_DX_GRADE_DSCRP = INIT_DX_GRADE_DSCRP;
    this.INIT_DX_T_STAGE = INIT_DX_T_STAGE;
    this.INIT_DX_T_STAGE_DSCRP = INIT_DX_T_STAGE_DSCRP;
    this.INIT_DX_N_STAGE = INIT_DX_N_STAGE;
    this.INIT_DX_N_STAGE_DSCRP = INIT_DX_N_STAGE_DSCRP;
    this.INIT_DX_M_STAGE = INIT_DX_M_STAGE;
    this.INIT_DX_M_STAGE_DSCRP = INIT_DX_M_STAGE_DSCRP;
    this.INIT_DX_HIST = INIT_DX_HIST;
    this.INIT_DX_SUB_HIST = INIT_DX_SUB_HIST;
    this.INIT_DX_SUB_SUB_HIST = INIT_DX_SUB_SUB_HIST;
    this.INIT_DX_SUB_SUB_SUB_HIST = INIT_DX_SUB_SUB_SUB_HIST;
    this.INIT_DX_SITE = INIT_DX_SITE;
    this.INIT_DX_SUB_SITE = INIT_DX_SUB_SITE;
    this.INIT_DX_SUB_SUB_SITE = INIT_DX_SUB_SUB_SITE;
    this.ENROLL_DX_STATUS_ID = ENROLL_DX_STATUS_ID;
    this.ENROLL_DX_STATUS = ENROLL_DX_STATUS;
    this.ENROLL_DX_STATUS_DAYS = ENROLL_DX_STATUS_DAYS;
    this.ENROLL_DX_STAGING_DSCRP = ENROLL_DX_STAGING_DSCRP;
    this.ENROLL_DX_STAGE = ENROLL_DX_STAGE;
    this.ENROLL_DX_STAGE_DSCRP = ENROLL_DX_STAGE_DSCRP;
    this.ENROLL_DX_GRADE = ENROLL_DX_GRADE;
    this.ENROLL_DX_GRADE_DSCRP = ENROLL_DX_GRADE_DSCRP;
    this.ENROLL_DX_T_STAGE = ENROLL_DX_T_STAGE;
    this.ENROLL_DX_T_STAGE_DSCRP = ENROLL_DX_T_STAGE_DSCRP;
    this.ENROLL_DX_N_STAGE = ENROLL_DX_N_STAGE;
    this.ENROLL_DX_N_STAGE_DSCRP = ENROLL_DX_N_STAGE_DSCRP;
    this.ENROLL_DX_M_STAGE = ENROLL_DX_M_STAGE;
    this.ENROLL_DX_M_STAGE_DSCRP = ENROLL_DX_M_STAGE_DSCRP;
    this.ENROLL_DX_HIST = ENROLL_DX_HIST;
    this.ENROLL_DX_SUB_HIST = ENROLL_DX_SUB_HIST;
    this.ENROLL_DX_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_HIST;
    this.ENROLL_DX_SUB_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_SUB_HIST;
    this.ENROLL_DX_SITE = ENROLL_DX_SITE;
    this.ENROLL_DX_SUB_SITE = ENROLL_DX_SUB_SITE;
    this.ENROLL_DX_SUB_SUB_SITE = ENROLL_DX_SUB_SUB_SITE;
    this.SURVIVAL_STATUS = SURVIVAL_STATUS;
    this.TREATMENT_END_DAYS = TREATMENT_END_DAYS;
    this.OFF_STUDY_DAYS = OFF_STUDY_DAYS;
    this.COMMENTS = COMMENTS;           
}

public CRDBDataset(Object[] crdbDataset) {
    this.DMP_ID = crdbDataset[0]==null?"NA":crdbDataset[0].toString();
    this.CONSENT_DATE_DAYS = crdbDataset[1]==null?"NA":crdbDataset[1].toString();
    this.PRIM_DISEASE_12245 = crdbDataset[2]==null?"NA":crdbDataset[2].toString();
    this.INITIAL_SX_DAYS = crdbDataset[3]==null?"NA":crdbDataset[3].toString();
    this.INITIAL_DX_DAYS = crdbDataset[4]==null?"NA":crdbDataset[4].toString();
    this.FIRST_METASTASIS_DAYS = crdbDataset[5]==null?"NA":crdbDataset[5].toString();
    this.INIT_DX_STATUS_ID = crdbDataset[6]==null?"NA":crdbDataset[6].toString();
    this.INIT_DX_STATUS = crdbDataset[7]==null?"NA":crdbDataset[7].toString();
    this.INIT_DX_STATUS_DAYS = crdbDataset[8]==null?"NA":crdbDataset[8].toString();
    this.INIT_DX_STAGING_DSCRP = crdbDataset[9]==null?"NA":crdbDataset[9].toString();
    this.INIT_DX_STAGE = crdbDataset[10]==null?"NA":crdbDataset[10].toString();
    this.INIT_DX_STAGE_DSCRP = crdbDataset[11]==null?"NA":crdbDataset[11].toString();
    this.INIT_DX_GRADE = crdbDataset[12]==null?"NA":crdbDataset[12].toString();
    this.INIT_DX_GRADE_DSCRP = crdbDataset[13]==null?"NA":crdbDataset[13].toString();
    this.INIT_DX_T_STAGE = crdbDataset[14]==null?"NA":crdbDataset[14].toString();
    this.INIT_DX_T_STAGE_DSCRP = crdbDataset[15]==null?"NA":crdbDataset[15].toString();
    this.INIT_DX_N_STAGE = crdbDataset[16]==null?"NA":crdbDataset[16].toString();
    this.INIT_DX_N_STAGE_DSCRP = crdbDataset[17]==null?"NA":crdbDataset[17].toString();
    this.INIT_DX_M_STAGE = crdbDataset[18]==null?"NA":crdbDataset[18].toString();
    this.INIT_DX_M_STAGE_DSCRP = crdbDataset[19]==null?"NA":crdbDataset[19].toString();
    this.INIT_DX_HIST = crdbDataset[20]==null?"NA":crdbDataset[20].toString();
    this.INIT_DX_SUB_HIST = crdbDataset[21]==null?"NA":crdbDataset[21].toString();
    this.INIT_DX_SUB_SUB_HIST = crdbDataset[22]==null?"NA":crdbDataset[22].toString();
    this.INIT_DX_SUB_SUB_SUB_HIST = crdbDataset[23]==null?"NA":crdbDataset[23].toString();
    this.INIT_DX_SITE = crdbDataset[24]==null?"NA":crdbDataset[24].toString();
    this.INIT_DX_SUB_SITE = crdbDataset[25]==null?"NA":crdbDataset[25].toString();
    this.INIT_DX_SUB_SUB_SITE = crdbDataset[26]==null?"NA":crdbDataset[26].toString();
    this.ENROLL_DX_STATUS_ID = crdbDataset[27]==null?"NA":crdbDataset[27].toString();
    this.ENROLL_DX_STATUS = crdbDataset[28]==null?"NA":crdbDataset[28].toString();
    this.ENROLL_DX_STATUS_DAYS = crdbDataset[29]==null?"NA":crdbDataset[29].toString();
    this.ENROLL_DX_STAGING_DSCRP = crdbDataset[30]==null?"NA":crdbDataset[30].toString();
    this.ENROLL_DX_STAGE = crdbDataset[31]==null?"NA":crdbDataset[31].toString();
    this.ENROLL_DX_STAGE_DSCRP = crdbDataset[32]==null?"NA":crdbDataset[32].toString();
    this.ENROLL_DX_GRADE = crdbDataset[33]==null?"NA":crdbDataset[33].toString();
    this.ENROLL_DX_GRADE_DSCRP = crdbDataset[34]==null?"NA":crdbDataset[34].toString();
    this.ENROLL_DX_T_STAGE = crdbDataset[35]==null?"NA":crdbDataset[35].toString();
    this.ENROLL_DX_T_STAGE_DSCRP = crdbDataset[36]==null?"NA":crdbDataset[36].toString();
    this.ENROLL_DX_N_STAGE = crdbDataset[37]==null?"NA":crdbDataset[37].toString();
    this.ENROLL_DX_N_STAGE_DSCRP = crdbDataset[38]==null?"NA":crdbDataset[38].toString();
    this.ENROLL_DX_M_STAGE = crdbDataset[39]==null?"NA":crdbDataset[39].toString();
    this.ENROLL_DX_M_STAGE_DSCRP = crdbDataset[40]==null?"NA":crdbDataset[40].toString();
    this.ENROLL_DX_HIST = crdbDataset[41]==null?"NA":crdbDataset[41].toString();
    this.ENROLL_DX_SUB_HIST = crdbDataset[42]==null?"NA":crdbDataset[42].toString();
    this.ENROLL_DX_SUB_SUB_HIST = crdbDataset[43]==null?"NA":crdbDataset[43].toString();
    this.ENROLL_DX_SUB_SUB_SUB_HIST = crdbDataset[44]==null?"NA":crdbDataset[44].toString();
    this.ENROLL_DX_SITE = crdbDataset[45]==null?"NA":crdbDataset[45].toString();
    this.ENROLL_DX_SUB_SITE = crdbDataset[46]==null?"NA":crdbDataset[46].toString();
    this.ENROLL_DX_SUB_SUB_SITE = crdbDataset[47]==null?"NA":crdbDataset[47].toString();
    this.SURVIVAL_STATUS = crdbDataset[48]==null?"NA":crdbDataset[48].toString();
    this.TREATMENT_END_DAYS = crdbDataset[49]==null?"NA":crdbDataset[49].toString();
    this.OFF_STUDY_DAYS = crdbDataset[50]==null?"NA":crdbDataset[50].toString();
    this.COMMENTS = crdbDataset[51]==null?"NA":crdbDataset[51].toString();     
}
    /**
     * 
     * @return DMP_ID
     */
    public String getDMP_ID() {
        return DMP_ID;
    }
    
    /**
     * 
     * @param DMP_ID 
     */
    public void setDMP_ID(String DMP_ID) {
        this.DMP_ID = DMP_ID;
    }

    /**
     * 
     * @param DMP_ID
     * @return 
     */
    public CRDBDataset withDMP_ID(String DMP_ID) {
        this.DMP_ID = DMP_ID;
        return this;
    }
	/**
     * 
     * @return CONSENT_DATE_DAYS
     */
    public String getCONSENT_DATE_DAYS() {
        return CONSENT_DATE_DAYS;
    }
    
    /**
     * 
     * @param CONSENT_DATE_DAYS 
     */
    public void setCONSENT_DATE_DAYS(String CONSENT_DATE_DAYS) {
        this.CONSENT_DATE_DAYS = CONSENT_DATE_DAYS;
    }

    /**
     * 
     * @param CONSENT_DATE_DAYS
     * @return 
     */
    public CRDBDataset withCONSENT_DATE_DAYS(String CONSENT_DATE_DAYS) {
        this.CONSENT_DATE_DAYS = CONSENT_DATE_DAYS;
        return this;
    }
	/**
     * 
     * @return PRIM_DISEASE_12245
     */
    public String getPRIM_DISEASE_12245() {
        return PRIM_DISEASE_12245;
    }
    
    /**
     * 
     * @param PRIM_DISEASE_12245 
     */
    public void setPRIM_DISEASE_12245(String PRIM_DISEASE_12245) {
        this.PRIM_DISEASE_12245 = PRIM_DISEASE_12245;
    }

    /**
     * 
     * @param PRIM_DISEASE_12245
     * @return 
     */
    public CRDBDataset withPRIM_DISEASE_12245(String PRIM_DISEASE_12245) {
        this.PRIM_DISEASE_12245 = PRIM_DISEASE_12245;
        return this;
    }
	/**
     * 
     * @return INITIAL_SX_DAYS
     */
    public String getINITIAL_SX_DAYS() {
        return INITIAL_SX_DAYS;
    }
    
    /**
     * 
     * @param INITIAL_SX_DAYS 
     */
    public void setINITIAL_SX_DAYS(String INITIAL_SX_DAYS) {
        this.INITIAL_SX_DAYS = INITIAL_SX_DAYS;
    }

    /**
     * 
     * @param INITIAL_SX_DAYS
     * @return 
     */
    public CRDBDataset withINITIAL_SX_DAYS(String INITIAL_SX_DAYS) {
        this.INITIAL_SX_DAYS = INITIAL_SX_DAYS;
        return this;
    }
	/**
     * 
     * @return INITIAL_DX_DAYS
     */
    public String getINITIAL_DX_DAYS() {
        return INITIAL_DX_DAYS;
    }
    
    /**
     * 
     * @param INITIAL_DX_DAYS 
     */
    public void setINITIAL_DX_DAYS(String INITIAL_DX_DAYS) {
        this.INITIAL_DX_DAYS = INITIAL_DX_DAYS;
    }

    /**
     * 
     * @param INITIAL_DX_DAYS
     * @return 
     */
    public CRDBDataset withINITIAL_DX_DAYS(String INITIAL_DX_DAYS) {
        this.INITIAL_DX_DAYS = INITIAL_DX_DAYS;
        return this;
    }
	/**
     * 
     * @return FIRST_METASTASIS_DAYS
     */
    public String getFIRST_METASTASIS_DAYS() {
        return FIRST_METASTASIS_DAYS;
    }
    
    /**
     * 
     * @param FIRST_METASTASIS_DAYS 
     */
    public void setFIRST_METASTASIS_DAYS(String FIRST_METASTASIS_DAYS) {
        this.FIRST_METASTASIS_DAYS = FIRST_METASTASIS_DAYS;
    }

    /**
     * 
     * @param FIRST_METASTASIS_DAYS
     * @return 
     */
    public CRDBDataset withFIRST_METASTASIS_DAYS(String FIRST_METASTASIS_DAYS) {
        this.FIRST_METASTASIS_DAYS = FIRST_METASTASIS_DAYS;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STATUS_ID
     */
    public String getINIT_DX_STATUS_ID() {
        return INIT_DX_STATUS_ID;
    }
    
    /**
     * 
     * @param INIT_DX_STATUS_ID 
     */
    public void setINIT_DX_STATUS_ID(String INIT_DX_STATUS_ID) {
        this.INIT_DX_STATUS_ID = INIT_DX_STATUS_ID;
    }

    /**
     * 
     * @param INIT_DX_STATUS_ID
     * @return 
     */
    public CRDBDataset withINIT_DX_STATUS_ID(String INIT_DX_STATUS_ID) {
        this.INIT_DX_STATUS_ID = INIT_DX_STATUS_ID;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STATUS
     */
    public String getINIT_DX_STATUS() {
        return INIT_DX_STATUS;
    }
    
    /**
     * 
     * @param INIT_DX_STATUS 
     */
    public void setINIT_DX_STATUS(String INIT_DX_STATUS) {
        this.INIT_DX_STATUS = INIT_DX_STATUS;
    }

    /**
     * 
     * @param INIT_DX_STATUS
     * @return 
     */
    public CRDBDataset withINIT_DX_STATUS(String INIT_DX_STATUS) {
        this.INIT_DX_STATUS = INIT_DX_STATUS;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STATUS_DAYS
     */
    public String getINIT_DX_STATUS_DAYS() {
        return INIT_DX_STATUS_DAYS;
    }
    
    /**
     * 
     * @param INIT_DX_STATUS_DAYS 
     */
    public void setINIT_DX_STATUS_DAYS(String INIT_DX_STATUS_DAYS) {
        this.INIT_DX_STATUS_DAYS = INIT_DX_STATUS_DAYS;
    }

    /**
     * 
     * @param INIT_DX_STATUS_DAYS
     * @return 
     */
    public CRDBDataset withINIT_DX_STATUS_DAYS(String INIT_DX_STATUS_DAYS) {
        this.INIT_DX_STATUS_DAYS = INIT_DX_STATUS_DAYS;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STAGING_DSCRP
     */
    public String getINIT_DX_STAGING_DSCRP() {
        return INIT_DX_STAGING_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_STAGING_DSCRP 
     */
    public void setINIT_DX_STAGING_DSCRP(String INIT_DX_STAGING_DSCRP) {
        this.INIT_DX_STAGING_DSCRP = INIT_DX_STAGING_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_STAGING_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_STAGING_DSCRP(String INIT_DX_STAGING_DSCRP) {
        this.INIT_DX_STAGING_DSCRP = INIT_DX_STAGING_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STAGE
     */
    public String getINIT_DX_STAGE() {
        return INIT_DX_STAGE;
    }
    
    /**
     * 
     * @param INIT_DX_STAGE 
     */
    public void setINIT_DX_STAGE(String INIT_DX_STAGE) {
        this.INIT_DX_STAGE = INIT_DX_STAGE;
    }

    /**
     * 
     * @param INIT_DX_STAGE
     * @return 
     */
    public CRDBDataset withINIT_DX_STAGE(String INIT_DX_STAGE) {
        this.INIT_DX_STAGE = INIT_DX_STAGE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_STAGE_DSCRP
     */
    public String getINIT_DX_STAGE_DSCRP() {
        return INIT_DX_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_STAGE_DSCRP 
     */
    public void setINIT_DX_STAGE_DSCRP(String INIT_DX_STAGE_DSCRP) {
        this.INIT_DX_STAGE_DSCRP = INIT_DX_STAGE_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_STAGE_DSCRP(String INIT_DX_STAGE_DSCRP) {
        this.INIT_DX_STAGE_DSCRP = INIT_DX_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_GRADE
     */
    public String getINIT_DX_GRADE() {
        return INIT_DX_GRADE;
    }
    
    /**
     * 
     * @param INIT_DX_GRADE 
     */
    public void setINIT_DX_GRADE(String INIT_DX_GRADE) {
        this.INIT_DX_GRADE = INIT_DX_GRADE;
    }

    /**
     * 
     * @param INIT_DX_GRADE
     * @return 
     */
    public CRDBDataset withINIT_DX_GRADE(String INIT_DX_GRADE) {
        this.INIT_DX_GRADE = INIT_DX_GRADE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_GRADE_DSCRP
     */
    public String getINIT_DX_GRADE_DSCRP() {
        return INIT_DX_GRADE_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_GRADE_DSCRP 
     */
    public void setINIT_DX_GRADE_DSCRP(String INIT_DX_GRADE_DSCRP) {
        this.INIT_DX_GRADE_DSCRP = INIT_DX_GRADE_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_GRADE_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_GRADE_DSCRP(String INIT_DX_GRADE_DSCRP) {
        this.INIT_DX_GRADE_DSCRP = INIT_DX_GRADE_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_T_STAGE
     */
    public String getINIT_DX_T_STAGE() {
        return INIT_DX_T_STAGE;
    }
    
    /**
     * 
     * @param INIT_DX_T_STAGE 
     */
    public void setINIT_DX_T_STAGE(String INIT_DX_T_STAGE) {
        this.INIT_DX_T_STAGE = INIT_DX_T_STAGE;
    }

    /**
     * 
     * @param INIT_DX_T_STAGE
     * @return 
     */
    public CRDBDataset withINIT_DX_T_STAGE(String INIT_DX_T_STAGE) {
        this.INIT_DX_T_STAGE = INIT_DX_T_STAGE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_T_STAGE_DSCRP
     */
    public String getINIT_DX_T_STAGE_DSCRP() {
        return INIT_DX_T_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_T_STAGE_DSCRP 
     */
    public void setINIT_DX_T_STAGE_DSCRP(String INIT_DX_T_STAGE_DSCRP) {
        this.INIT_DX_T_STAGE_DSCRP = INIT_DX_T_STAGE_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_T_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_T_STAGE_DSCRP(String INIT_DX_T_STAGE_DSCRP) {
        this.INIT_DX_T_STAGE_DSCRP = INIT_DX_T_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_N_STAGE
     */
    public String getINIT_DX_N_STAGE() {
        return INIT_DX_N_STAGE;
    }
    
    /**
     * 
     * @param INIT_DX_N_STAGE 
     */
    public void setINIT_DX_N_STAGE(String INIT_DX_N_STAGE) {
        this.INIT_DX_N_STAGE = INIT_DX_N_STAGE;
    }

    /**
     * 
     * @param INIT_DX_N_STAGE
     * @return 
     */
    public CRDBDataset withINIT_DX_N_STAGE(String INIT_DX_N_STAGE) {
        this.INIT_DX_N_STAGE = INIT_DX_N_STAGE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_N_STAGE_DSCRP
     */
    public String getINIT_DX_N_STAGE_DSCRP() {
        return INIT_DX_N_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_N_STAGE_DSCRP 
     */
    public void setINIT_DX_N_STAGE_DSCRP(String INIT_DX_N_STAGE_DSCRP) {
        this.INIT_DX_N_STAGE_DSCRP = INIT_DX_N_STAGE_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_N_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_N_STAGE_DSCRP(String INIT_DX_N_STAGE_DSCRP) {
        this.INIT_DX_N_STAGE_DSCRP = INIT_DX_N_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_M_STAGE
     */
    public String getINIT_DX_M_STAGE() {
        return INIT_DX_M_STAGE;
    }
    
    /**
     * 
     * @param INIT_DX_M_STAGE 
     */
    public void setINIT_DX_M_STAGE(String INIT_DX_M_STAGE) {
        this.INIT_DX_M_STAGE = INIT_DX_M_STAGE;
    }

    /**
     * 
     * @param INIT_DX_M_STAGE
     * @return 
     */
    public CRDBDataset withINIT_DX_M_STAGE(String INIT_DX_M_STAGE) {
        this.INIT_DX_M_STAGE = INIT_DX_M_STAGE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_M_STAGE_DSCRP
     */
    public String getINIT_DX_M_STAGE_DSCRP() {
        return INIT_DX_M_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param INIT_DX_M_STAGE_DSCRP 
     */
    public void setINIT_DX_M_STAGE_DSCRP(String INIT_DX_M_STAGE_DSCRP) {
        this.INIT_DX_M_STAGE_DSCRP = INIT_DX_M_STAGE_DSCRP;
    }

    /**
     * 
     * @param INIT_DX_M_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withINIT_DX_M_STAGE_DSCRP(String INIT_DX_M_STAGE_DSCRP) {
        this.INIT_DX_M_STAGE_DSCRP = INIT_DX_M_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return INIT_DX_HIST
     */
    public String getINIT_DX_HIST() {
        return INIT_DX_HIST;
    }
    
    /**
     * 
     * @param INIT_DX_HIST 
     */
    public void setINIT_DX_HIST(String INIT_DX_HIST) {
        this.INIT_DX_HIST = INIT_DX_HIST;
    }

    /**
     * 
     * @param INIT_DX_HIST
     * @return 
     */
    public CRDBDataset withINIT_DX_HIST(String INIT_DX_HIST) {
        this.INIT_DX_HIST = INIT_DX_HIST;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SUB_HIST
     */
    public String getINIT_DX_SUB_HIST() {
        return INIT_DX_SUB_HIST;
    }
    
    /**
     * 
     * @param INIT_DX_SUB_HIST 
     */
    public void setINIT_DX_SUB_HIST(String INIT_DX_SUB_HIST) {
        this.INIT_DX_SUB_HIST = INIT_DX_SUB_HIST;
    }

    /**
     * 
     * @param INIT_DX_SUB_HIST
     * @return 
     */
    public CRDBDataset withINIT_DX_SUB_HIST(String INIT_DX_SUB_HIST) {
        this.INIT_DX_SUB_HIST = INIT_DX_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SUB_SUB_HIST
     */
    public String getINIT_DX_SUB_SUB_HIST() {
        return INIT_DX_SUB_SUB_HIST;
    }
    
    /**
     * 
     * @param INIT_DX_SUB_SUB_HIST 
     */
    public void setINIT_DX_SUB_SUB_HIST(String INIT_DX_SUB_SUB_HIST) {
        this.INIT_DX_SUB_SUB_HIST = INIT_DX_SUB_SUB_HIST;
    }

    /**
     * 
     * @param INIT_DX_SUB_SUB_HIST
     * @return 
     */
    public CRDBDataset withINIT_DX_SUB_SUB_HIST(String INIT_DX_SUB_SUB_HIST) {
        this.INIT_DX_SUB_SUB_HIST = INIT_DX_SUB_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SUB_SUB_SUB_HIST
     */
    public String getINIT_DX_SUB_SUB_SUB_HIST() {
        return INIT_DX_SUB_SUB_SUB_HIST;
    }
    
    /**
     * 
     * @param INIT_DX_SUB_SUB_SUB_HIST 
     */
    public void setINIT_DX_SUB_SUB_SUB_HIST(String INIT_DX_SUB_SUB_SUB_HIST) {
        this.INIT_DX_SUB_SUB_SUB_HIST = INIT_DX_SUB_SUB_SUB_HIST;
    }

    /**
     * 
     * @param INIT_DX_SUB_SUB_SUB_HIST
     * @return 
     */
    public CRDBDataset withINIT_DX_SUB_SUB_SUB_HIST(String INIT_DX_SUB_SUB_SUB_HIST) {
        this.INIT_DX_SUB_SUB_SUB_HIST = INIT_DX_SUB_SUB_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SITE
     */
    public String getINIT_DX_SITE() {
        return INIT_DX_SITE;
    }
    
    /**
     * 
     * @param INIT_DX_SITE 
     */
    public void setINIT_DX_SITE(String INIT_DX_SITE) {
        this.INIT_DX_SITE = INIT_DX_SITE;
    }

    /**
     * 
     * @param INIT_DX_SITE
     * @return 
     */
    public CRDBDataset withINIT_DX_SITE(String INIT_DX_SITE) {
        this.INIT_DX_SITE = INIT_DX_SITE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SUB_SITE
     */
    public String getINIT_DX_SUB_SITE() {
        return INIT_DX_SUB_SITE;
    }
    
    /**
     * 
     * @param INIT_DX_SUB_SITE 
     */
    public void setINIT_DX_SUB_SITE(String INIT_DX_SUB_SITE) {
        this.INIT_DX_SUB_SITE = INIT_DX_SUB_SITE;
    }

    /**
     * 
     * @param INIT_DX_SUB_SITE
     * @return 
     */
    public CRDBDataset withINIT_DX_SUB_SITE(String INIT_DX_SUB_SITE) {
        this.INIT_DX_SUB_SITE = INIT_DX_SUB_SITE;
        return this;
    }
	/**
     * 
     * @return INIT_DX_SUB_SUB_SITE
     */
    public String getINIT_DX_SUB_SUB_SITE() {
        return INIT_DX_SUB_SUB_SITE;
    }
    
    /**
     * 
     * @param INIT_DX_SUB_SUB_SITE 
     */
    public void setINIT_DX_SUB_SUB_SITE(String INIT_DX_SUB_SUB_SITE) {
        this.INIT_DX_SUB_SUB_SITE = INIT_DX_SUB_SUB_SITE;
    }

    /**
     * 
     * @param INIT_DX_SUB_SUB_SITE
     * @return 
     */
    public CRDBDataset withINIT_DX_SUB_SUB_SITE(String INIT_DX_SUB_SUB_SITE) {
        this.INIT_DX_SUB_SUB_SITE = INIT_DX_SUB_SUB_SITE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STATUS_ID
     */
    public String getENROLL_DX_STATUS_ID() {
        return ENROLL_DX_STATUS_ID;
    }
    
    /**
     * 
     * @param ENROLL_DX_STATUS_ID 
     */
    public void setENROLL_DX_STATUS_ID(String ENROLL_DX_STATUS_ID) {
        this.ENROLL_DX_STATUS_ID = ENROLL_DX_STATUS_ID;
    }

    /**
     * 
     * @param ENROLL_DX_STATUS_ID
     * @return 
     */
    public CRDBDataset withENROLL_DX_STATUS_ID(String ENROLL_DX_STATUS_ID) {
        this.ENROLL_DX_STATUS_ID = ENROLL_DX_STATUS_ID;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STATUS
     */
    public String getENROLL_DX_STATUS() {
        return ENROLL_DX_STATUS;
    }
    
    /**
     * 
     * @param ENROLL_DX_STATUS 
     */
    public void setENROLL_DX_STATUS(String ENROLL_DX_STATUS) {
        this.ENROLL_DX_STATUS = ENROLL_DX_STATUS;
    }

    /**
     * 
     * @param ENROLL_DX_STATUS
     * @return 
     */
    public CRDBDataset withENROLL_DX_STATUS(String ENROLL_DX_STATUS) {
        this.ENROLL_DX_STATUS = ENROLL_DX_STATUS;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STATUS_DAYS
     */
    public String getENROLL_DX_STATUS_DAYS() {
        return ENROLL_DX_STATUS_DAYS;
    }
    
    /**
     * 
     * @param ENROLL_DX_STATUS_DAYS 
     */
    public void setENROLL_DX_STATUS_DAYS(String ENROLL_DX_STATUS_DAYS) {
        this.ENROLL_DX_STATUS_DAYS = ENROLL_DX_STATUS_DAYS;
    }

    /**
     * 
     * @param ENROLL_DX_STATUS_DAYS
     * @return 
     */
    public CRDBDataset withENROLL_DX_STATUS_DAYS(String ENROLL_DX_STATUS_DAYS) {
        this.ENROLL_DX_STATUS_DAYS = ENROLL_DX_STATUS_DAYS;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STAGING_DSCRP
     */
    public String getENROLL_DX_STAGING_DSCRP() {
        return ENROLL_DX_STAGING_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_STAGING_DSCRP 
     */
    public void setENROLL_DX_STAGING_DSCRP(String ENROLL_DX_STAGING_DSCRP) {
        this.ENROLL_DX_STAGING_DSCRP = ENROLL_DX_STAGING_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_STAGING_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_STAGING_DSCRP(String ENROLL_DX_STAGING_DSCRP) {
        this.ENROLL_DX_STAGING_DSCRP = ENROLL_DX_STAGING_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STAGE
     */
    public String getENROLL_DX_STAGE() {
        return ENROLL_DX_STAGE;
    }
    
    /**
     * 
     * @param ENROLL_DX_STAGE 
     */
    public void setENROLL_DX_STAGE(String ENROLL_DX_STAGE) {
        this.ENROLL_DX_STAGE = ENROLL_DX_STAGE;
    }

    /**
     * 
     * @param ENROLL_DX_STAGE
     * @return 
     */
    public CRDBDataset withENROLL_DX_STAGE(String ENROLL_DX_STAGE) {
        this.ENROLL_DX_STAGE = ENROLL_DX_STAGE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_STAGE_DSCRP
     */
    public String getENROLL_DX_STAGE_DSCRP() {
        return ENROLL_DX_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_STAGE_DSCRP 
     */
    public void setENROLL_DX_STAGE_DSCRP(String ENROLL_DX_STAGE_DSCRP) {
        this.ENROLL_DX_STAGE_DSCRP = ENROLL_DX_STAGE_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_STAGE_DSCRP(String ENROLL_DX_STAGE_DSCRP) {
        this.ENROLL_DX_STAGE_DSCRP = ENROLL_DX_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_GRADE
     */
    public String getENROLL_DX_GRADE() {
        return ENROLL_DX_GRADE;
    }
    
    /**
     * 
     * @param ENROLL_DX_GRADE 
     */
    public void setENROLL_DX_GRADE(String ENROLL_DX_GRADE) {
        this.ENROLL_DX_GRADE = ENROLL_DX_GRADE;
    }

    /**
     * 
     * @param ENROLL_DX_GRADE
     * @return 
     */
    public CRDBDataset withENROLL_DX_GRADE(String ENROLL_DX_GRADE) {
        this.ENROLL_DX_GRADE = ENROLL_DX_GRADE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_GRADE_DSCRP
     */
    public String getENROLL_DX_GRADE_DSCRP() {
        return ENROLL_DX_GRADE_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_GRADE_DSCRP 
     */
    public void setENROLL_DX_GRADE_DSCRP(String ENROLL_DX_GRADE_DSCRP) {
        this.ENROLL_DX_GRADE_DSCRP = ENROLL_DX_GRADE_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_GRADE_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_GRADE_DSCRP(String ENROLL_DX_GRADE_DSCRP) {
        this.ENROLL_DX_GRADE_DSCRP = ENROLL_DX_GRADE_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_T_STAGE
     */
    public String getENROLL_DX_T_STAGE() {
        return ENROLL_DX_T_STAGE;
    }
    
    /**
     * 
     * @param ENROLL_DX_T_STAGE 
     */
    public void setENROLL_DX_T_STAGE(String ENROLL_DX_T_STAGE) {
        this.ENROLL_DX_T_STAGE = ENROLL_DX_T_STAGE;
    }

    /**
     * 
     * @param ENROLL_DX_T_STAGE
     * @return 
     */
    public CRDBDataset withENROLL_DX_T_STAGE(String ENROLL_DX_T_STAGE) {
        this.ENROLL_DX_T_STAGE = ENROLL_DX_T_STAGE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_T_STAGE_DSCRP
     */
    public String getENROLL_DX_T_STAGE_DSCRP() {
        return ENROLL_DX_T_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_T_STAGE_DSCRP 
     */
    public void setENROLL_DX_T_STAGE_DSCRP(String ENROLL_DX_T_STAGE_DSCRP) {
        this.ENROLL_DX_T_STAGE_DSCRP = ENROLL_DX_T_STAGE_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_T_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_T_STAGE_DSCRP(String ENROLL_DX_T_STAGE_DSCRP) {
        this.ENROLL_DX_T_STAGE_DSCRP = ENROLL_DX_T_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_N_STAGE
     */
    public String getENROLL_DX_N_STAGE() {
        return ENROLL_DX_N_STAGE;
    }
    
    /**
     * 
     * @param ENROLL_DX_N_STAGE 
     */
    public void setENROLL_DX_N_STAGE(String ENROLL_DX_N_STAGE) {
        this.ENROLL_DX_N_STAGE = ENROLL_DX_N_STAGE;
    }

    /**
     * 
     * @param ENROLL_DX_N_STAGE
     * @return 
     */
    public CRDBDataset withENROLL_DX_N_STAGE(String ENROLL_DX_N_STAGE) {
        this.ENROLL_DX_N_STAGE = ENROLL_DX_N_STAGE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_N_STAGE_DSCRP
     */
    public String getENROLL_DX_N_STAGE_DSCRP() {
        return ENROLL_DX_N_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_N_STAGE_DSCRP 
     */
    public void setENROLL_DX_N_STAGE_DSCRP(String ENROLL_DX_N_STAGE_DSCRP) {
        this.ENROLL_DX_N_STAGE_DSCRP = ENROLL_DX_N_STAGE_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_N_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_N_STAGE_DSCRP(String ENROLL_DX_N_STAGE_DSCRP) {
        this.ENROLL_DX_N_STAGE_DSCRP = ENROLL_DX_N_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_M_STAGE
     */
    public String getENROLL_DX_M_STAGE() {
        return ENROLL_DX_M_STAGE;
    }
    
    /**
     * 
     * @param ENROLL_DX_M_STAGE 
     */
    public void setENROLL_DX_M_STAGE(String ENROLL_DX_M_STAGE) {
        this.ENROLL_DX_M_STAGE = ENROLL_DX_M_STAGE;
    }

    /**
     * 
     * @param ENROLL_DX_M_STAGE
     * @return 
     */
    public CRDBDataset withENROLL_DX_M_STAGE(String ENROLL_DX_M_STAGE) {
        this.ENROLL_DX_M_STAGE = ENROLL_DX_M_STAGE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_M_STAGE_DSCRP
     */
    public String getENROLL_DX_M_STAGE_DSCRP() {
        return ENROLL_DX_M_STAGE_DSCRP;
    }
    
    /**
     * 
     * @param ENROLL_DX_M_STAGE_DSCRP 
     */
    public void setENROLL_DX_M_STAGE_DSCRP(String ENROLL_DX_M_STAGE_DSCRP) {
        this.ENROLL_DX_M_STAGE_DSCRP = ENROLL_DX_M_STAGE_DSCRP;
    }

    /**
     * 
     * @param ENROLL_DX_M_STAGE_DSCRP
     * @return 
     */
    public CRDBDataset withENROLL_DX_M_STAGE_DSCRP(String ENROLL_DX_M_STAGE_DSCRP) {
        this.ENROLL_DX_M_STAGE_DSCRP = ENROLL_DX_M_STAGE_DSCRP;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_HIST
     */
    public String getENROLL_DX_HIST() {
        return ENROLL_DX_HIST;
    }
    
    /**
     * 
     * @param ENROLL_DX_HIST 
     */
    public void setENROLL_DX_HIST(String ENROLL_DX_HIST) {
        this.ENROLL_DX_HIST = ENROLL_DX_HIST;
    }

    /**
     * 
     * @param ENROLL_DX_HIST
     * @return 
     */
    public CRDBDataset withENROLL_DX_HIST(String ENROLL_DX_HIST) {
        this.ENROLL_DX_HIST = ENROLL_DX_HIST;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SUB_HIST
     */
    public String getENROLL_DX_SUB_HIST() {
        return ENROLL_DX_SUB_HIST;
    }
    
    /**
     * 
     * @param ENROLL_DX_SUB_HIST 
     */
    public void setENROLL_DX_SUB_HIST(String ENROLL_DX_SUB_HIST) {
        this.ENROLL_DX_SUB_HIST = ENROLL_DX_SUB_HIST;
    }

    /**
     * 
     * @param ENROLL_DX_SUB_HIST
     * @return 
     */
    public CRDBDataset withENROLL_DX_SUB_HIST(String ENROLL_DX_SUB_HIST) {
        this.ENROLL_DX_SUB_HIST = ENROLL_DX_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SUB_SUB_HIST
     */
    public String getENROLL_DX_SUB_SUB_HIST() {
        return ENROLL_DX_SUB_SUB_HIST;
    }
    
    /**
     * 
     * @param ENROLL_DX_SUB_SUB_HIST 
     */
    public void setENROLL_DX_SUB_SUB_HIST(String ENROLL_DX_SUB_SUB_HIST) {
        this.ENROLL_DX_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_HIST;
    }

    /**
     * 
     * @param ENROLL_DX_SUB_SUB_HIST
     * @return 
     */
    public CRDBDataset withENROLL_DX_SUB_SUB_HIST(String ENROLL_DX_SUB_SUB_HIST) {
        this.ENROLL_DX_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SUB_SUB_SUB_HIST
     */
    public String getENROLL_DX_SUB_SUB_SUB_HIST() {
        return ENROLL_DX_SUB_SUB_SUB_HIST;
    }
    
    /**
     * 
     * @param ENROLL_DX_SUB_SUB_SUB_HIST 
     */
    public void setENROLL_DX_SUB_SUB_SUB_HIST(String ENROLL_DX_SUB_SUB_SUB_HIST) {
        this.ENROLL_DX_SUB_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_SUB_HIST;
    }

    /**
     * 
     * @param ENROLL_DX_SUB_SUB_SUB_HIST
     * @return 
     */
    public CRDBDataset withENROLL_DX_SUB_SUB_SUB_HIST(String ENROLL_DX_SUB_SUB_SUB_HIST) {
        this.ENROLL_DX_SUB_SUB_SUB_HIST = ENROLL_DX_SUB_SUB_SUB_HIST;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SITE
     */
    public String getENROLL_DX_SITE() {
        return ENROLL_DX_SITE;
    }
    
    /**
     * 
     * @param ENROLL_DX_SITE 
     */
    public void setENROLL_DX_SITE(String ENROLL_DX_SITE) {
        this.ENROLL_DX_SITE = ENROLL_DX_SITE;
    }

    /**
     * 
     * @param ENROLL_DX_SITE
     * @return 
     */
    public CRDBDataset withENROLL_DX_SITE(String ENROLL_DX_SITE) {
        this.ENROLL_DX_SITE = ENROLL_DX_SITE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SUB_SITE
     */
    public String getENROLL_DX_SUB_SITE() {
        return ENROLL_DX_SUB_SITE;
    }
    
    /**
     * 
     * @param ENROLL_DX_SUB_SITE 
     */
    public void setENROLL_DX_SUB_SITE(String ENROLL_DX_SUB_SITE) {
        this.ENROLL_DX_SUB_SITE = ENROLL_DX_SUB_SITE;
    }

    /**
     * 
     * @param ENROLL_DX_SUB_SITE
     * @return 
     */
    public CRDBDataset withENROLL_DX_SUB_SITE(String ENROLL_DX_SUB_SITE) {
        this.ENROLL_DX_SUB_SITE = ENROLL_DX_SUB_SITE;
        return this;
    }
	/**
     * 
     * @return ENROLL_DX_SUB_SUB_SITE
     */
    public String getENROLL_DX_SUB_SUB_SITE() {
        return ENROLL_DX_SUB_SUB_SITE;
    }
    
    /**
     * 
     * @param ENROLL_DX_SUB_SUB_SITE 
     */
    public void setENROLL_DX_SUB_SUB_SITE(String ENROLL_DX_SUB_SUB_SITE) {
        this.ENROLL_DX_SUB_SUB_SITE = ENROLL_DX_SUB_SUB_SITE;
    }

    /**
     * 
     * @param ENROLL_DX_SUB_SUB_SITE
     * @return 
     */
    public CRDBDataset withENROLL_DX_SUB_SUB_SITE(String ENROLL_DX_SUB_SUB_SITE) {
        this.ENROLL_DX_SUB_SUB_SITE = ENROLL_DX_SUB_SUB_SITE;
        return this;
    }
	/**
     * 
     * @return SURVIVAL_STATUS
     */
    public String getSURVIVAL_STATUS() {
        return SURVIVAL_STATUS;
    }
    
    /**
     * 
     * @param SURVIVAL_STATUS 
     */
    public void setSURVIVAL_STATUS(String SURVIVAL_STATUS) {
        this.SURVIVAL_STATUS = SURVIVAL_STATUS;
    }

    /**
     * 
     * @param SURVIVAL_STATUS
     * @return 
     */
    public CRDBDataset withSURVIVAL_STATUS(String SURVIVAL_STATUS) {
        this.SURVIVAL_STATUS = SURVIVAL_STATUS;
        return this;
    }
	/**
     * 
     * @return TREATMENT_END_DAYS
     */
    public String getTREATMENT_END_DAYS() {
        return TREATMENT_END_DAYS;
    }
    
    /**
     * 
     * @param TREATMENT_END_DAYS 
     */
    public void setTREATMENT_END_DAYS(String TREATMENT_END_DAYS) {
        this.TREATMENT_END_DAYS = TREATMENT_END_DAYS;
    }

    /**
     * 
     * @param TREATMENT_END_DAYS
     * @return 
     */
    public CRDBDataset withTREATMENT_END_DAYS(String TREATMENT_END_DAYS) {
        this.TREATMENT_END_DAYS = TREATMENT_END_DAYS;
        return this;
    }
	/**
     * 
     * @return OFF_STUDY_DAYS
     */
    public String getOFF_STUDY_DAYS() {
        return OFF_STUDY_DAYS;
    }
    
    /**
     * 
     * @param OFF_STUDY_DAYS 
     */
    public void setOFF_STUDY_DAYS(String OFF_STUDY_DAYS) {
        this.OFF_STUDY_DAYS = OFF_STUDY_DAYS;
    }

    /**
     * 
     * @param OFF_STUDY_DAYS
     * @return 
     */
    public CRDBDataset withOFF_STUDY_DAYS(String OFF_STUDY_DAYS) {
        this.OFF_STUDY_DAYS = OFF_STUDY_DAYS;
        return this;
    }
	/**
     * 
     * @return COMMENTS
     */
    public String getCOMMENTS() {
        return COMMENTS;
    }
    
    /**
     * 
     * @param COMMENTS 
     */
    public void setCOMMENTS(String COMMENTS) {
        this.COMMENTS = COMMENTS;
    }

    /**
     * 
     * @param COMMENTS
     * @return 
     */
    public CRDBDataset withCOMMENTS(String COMMENTS) {
        this.COMMENTS = COMMENTS;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String[] getFieldNames() {
        return toString().substring(toString().indexOf("[")+1,toString().indexOf("]")).replaceAll("=<null>", "").split(",");
    }
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CRDBDataset withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
