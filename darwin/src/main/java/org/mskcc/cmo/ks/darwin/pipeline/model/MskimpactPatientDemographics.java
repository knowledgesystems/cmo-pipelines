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
package org.mskcc.cmo.ks.darwin.pipeline.model;

import org.apache.commons.lang.StringUtils;
import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author jake
 */
public class MskimpactPatientDemographics {
    private final Integer currentYear = Calendar.getInstance().get(1);
    private String PT_ID_DEMO;
    private String DMP_ID_DEMO;
    private String GENDER;
    private String RACE;
    private String RELIGION;
    private Integer AGE_AT_DATE_OF_DEATH_IN_DAYS;
    private Integer AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS;
    private Integer AGE_AT_TM_DX_DATE_IN_DAYS;
    private String DEATH_SOURCE_DESCRIPTION;
    private String PT_COUNTRY;
    private String PT_STATE;
    private String PT_ZIP3_CD;
    private Integer PT_BIRTH_YEAR;
    private String PT_SEX_DESC;
    private String PT_VITAL_STATUS;
    private String PT_MARITAL_STS_DESC;
    private Integer PT_DEATH_YEAR;
    private String PT_MRN_CREATE_YEAR;
    private Integer TM_DX_YEAR;
    private String OS_STATUS;
    private String OS_MONTHS;
    private Map<String, Object> additionalProperties = new HashMap<>();

    public MskimpactPatientDemographics() {
    }

    public MskimpactPatientDemographics(String PT_ID_DEMO, String DMP_ID_DEMO, String GENDER,
            String RACE, String RELIGION, Integer AGE_AT_DATE_OF_DEATH_IN_DAYS, String DEATH_SOURCE_DESCRIPTION,
            String PT_COUNTRY, String PT_STATE, String PT_ZIP3_CD, Integer PT_BIRTH_YEAR, String PT_SEX_DESC,
            String PT_VITAL_STATUS, String PT_MARITAL_STS_DESC, Integer PT_DEATH_YEAR, String PT_MRN_CREATE_YEAR, Integer TM_DX_YEAR) {
        this.PT_ID_DEMO =  StringUtils.isNotEmpty(PT_ID_DEMO) ? PT_ID_DEMO : "NA";
        this.DMP_ID_DEMO =  StringUtils.isNotEmpty(DMP_ID_DEMO) ? DMP_ID_DEMO : "NA";
        this.GENDER =  StringUtils.isNotEmpty(GENDER) ? GENDER : "NA";
        this.RACE =  StringUtils.isNotEmpty(RACE) ? RACE : "NA";
        this.RELIGION =  StringUtils.isNotEmpty(RELIGION) ? RELIGION : "NA";
        this.AGE_AT_DATE_OF_DEATH_IN_DAYS = AGE_AT_DATE_OF_DEATH_IN_DAYS != null ? AGE_AT_DATE_OF_DEATH_IN_DAYS : -1;
        this.DEATH_SOURCE_DESCRIPTION =  StringUtils.isNotEmpty(DEATH_SOURCE_DESCRIPTION) ? DEATH_SOURCE_DESCRIPTION : "NA";
        this.PT_COUNTRY =  StringUtils.isNotEmpty(PT_COUNTRY) ? PT_COUNTRY : "NA";
        this.PT_STATE =  StringUtils.isNotEmpty(PT_STATE) ? PT_STATE : "NA";
        this.PT_ZIP3_CD =  StringUtils.isNotEmpty(PT_ZIP3_CD) ? PT_ZIP3_CD : "NA";
        this.PT_BIRTH_YEAR = PT_BIRTH_YEAR != null ? PT_BIRTH_YEAR : -1;
        this.PT_SEX_DESC =  StringUtils.isNotEmpty(PT_SEX_DESC) ? PT_SEX_DESC : "NA";
        this.PT_VITAL_STATUS =  StringUtils.isNotEmpty(PT_VITAL_STATUS) ? PT_VITAL_STATUS : "NA";
        this.PT_MARITAL_STS_DESC =  StringUtils.isNotEmpty(PT_MARITAL_STS_DESC) ? PT_MARITAL_STS_DESC : "NA";
        this.PT_DEATH_YEAR = PT_DEATH_YEAR != null ? PT_DEATH_YEAR : -1;
        this.PT_MRN_CREATE_YEAR =  StringUtils.isNotEmpty(PT_MRN_CREATE_YEAR) ? PT_MRN_CREATE_YEAR : "NA";
        this.TM_DX_YEAR = TM_DX_YEAR != null ? TM_DX_YEAR : -1;
        this.OS_STATUS = this.PT_VITAL_STATUS;
    }
    
    public MskimpactPatientDemographics(String DMP_ID_DEMO, String GENDER, String RACE, String RELIGION, String PT_VITAL_STATUS, Integer PT_BIRTH_YEAR, Integer PT_DEATH_YEAR, Integer TM_DX_YEAR, Integer AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS, Integer AGE_AT_TM_DX_DATE_IN_DAYS, Integer AGE_AT_DATE_OF_DEATH_IN_DAYS){
        this.DMP_ID_DEMO =  StringUtils.isNotEmpty(DMP_ID_DEMO) ? DMP_ID_DEMO : "NA";
        this.GENDER =  StringUtils.isNotEmpty(GENDER) ? GENDER : "NA";
        this.RACE =  StringUtils.isNotEmpty(RACE) ? RACE : "NA";
        this.RELIGION =  StringUtils.isNotEmpty(RELIGION) ? RELIGION : "NA";
        this.PT_VITAL_STATUS =  StringUtils.isNotEmpty(PT_VITAL_STATUS) ? PT_VITAL_STATUS : "NA";
        this.TM_DX_YEAR = TM_DX_YEAR != null ? TM_DX_YEAR : -1;
        this.PT_BIRTH_YEAR = PT_BIRTH_YEAR != null ? PT_BIRTH_YEAR : -1;
        this.PT_DEATH_YEAR = PT_DEATH_YEAR != null ? PT_DEATH_YEAR : -1;
        this.OS_STATUS = this.PT_VITAL_STATUS;
        this.AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS = AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS;
        this.AGE_AT_TM_DX_DATE_IN_DAYS = AGE_AT_TM_DX_DATE_IN_DAYS;
        this.AGE_AT_DATE_OF_DEATH_IN_DAYS = AGE_AT_DATE_OF_DEATH_IN_DAYS;
    }
    public MskimpactPatientDemographics(String DMP_ID_DEMO, String GENDER, String RACE, String RELIGION, String PT_VITAL_STATUS, Integer PT_BIRTH_YEAR, Integer PT_DEATH_YEAR){
        this.DMP_ID_DEMO =  StringUtils.isNotEmpty(DMP_ID_DEMO) ? DMP_ID_DEMO : "NA";
        this.GENDER =  StringUtils.isNotEmpty(GENDER) ? GENDER : "NA";
        this.RACE =  StringUtils.isNotEmpty(RACE) ? RACE : "NA";
        this.RELIGION =  StringUtils.isNotEmpty(RELIGION) ? RELIGION : "NA";
        this.PT_VITAL_STATUS =  StringUtils.isNotEmpty(PT_VITAL_STATUS) ? PT_VITAL_STATUS : "NA";
        this.TM_DX_YEAR = -1;
        this.PT_BIRTH_YEAR = PT_BIRTH_YEAR != null ? PT_BIRTH_YEAR : -1;
        this.PT_DEATH_YEAR = PT_DEATH_YEAR != null ? PT_DEATH_YEAR : -1;
        this.OS_STATUS = this.PT_VITAL_STATUS;
    }
    
    public Integer getTM_DX_YEAR() {
        return TM_DX_YEAR;
    }

    public void setTM_DX_YEAR(Integer TM_DX_YEAR) {
        this.TM_DX_YEAR = TM_DX_YEAR != null ? TM_DX_YEAR : -1;
    }
    
    public String getAGE_AT_DIAGNOSIS(){
        if(this.PT_BIRTH_YEAR>-1 && this.TM_DX_YEAR>-1 && this.TM_DX_YEAR>this.PT_BIRTH_YEAR){
                Integer i = this.TM_DX_YEAR - this.PT_BIRTH_YEAR;
                //Age > 90 is considered identifying
                if(i<90){
                    return i.toString();
                }
                else{
                    return "NA";
                }
        }
        else{
            return "NA";
        }
    }

    public String getPT_ID_DEMO() {
        return PT_ID_DEMO;
    }

    public void setPT_ID_DEMO(String PT_ID_DEMO) {
        this.PT_ID_DEMO =  StringUtils.isNotEmpty(PT_ID_DEMO) ? PT_ID_DEMO : "NA";
    }

    public String getDMP_ID_DEMO() {
        return DMP_ID_DEMO;
    }

    public void setDMP_ID_DEMO(String DMP_ID_DEMO) {
        this.DMP_ID_DEMO =  StringUtils.isNotEmpty(DMP_ID_DEMO) ? DMP_ID_DEMO : "NA";
    }

    public String getGENDER() {
        return GENDER;
    }

    public void setGENDER(String GENDER) {
        this.GENDER =  StringUtils.isNotEmpty(GENDER) ? GENDER : "NA";
    }

    public String getRACE() {
        return RACE;
    }

    public void setRACE(String RACE) {
        this.RACE =  StringUtils.isNotEmpty(RACE) ? RACE : "NA";
    }

    public String getRELIGION() {
        return RELIGION;
    }

    public void setRELIGION(String RELIGION) {
        this.RELIGION =  StringUtils.isNotEmpty(RELIGION) ? RELIGION : "NA";
    }

    public Integer getAGE_AT_DATE_OF_DEATH_IN_DAYS() {
        return AGE_AT_DATE_OF_DEATH_IN_DAYS;
    }

    public void setAGE_AT_DATE_OF_DEATH_IN_DAYS(Integer AGE_AT_DATE_OF_DEATH_IN_DAYS) {
        this.AGE_AT_DATE_OF_DEATH_IN_DAYS = AGE_AT_DATE_OF_DEATH_IN_DAYS != null ? AGE_AT_DATE_OF_DEATH_IN_DAYS : -1;
    }

    public Integer getAGE_AT_LAST_KNOWN_ALIVE_IN_DAYS() {
        return AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS;
    }

    public void setAGE_AT_LAST_KNOWN_ALIVE_IN_DAYS(Integer AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS) {
        this.AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS = AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS != null ? AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS : -1;
    }

    public Integer getAGE_AT_TM_DX_DATE_IN_DAYS() {
        return AGE_AT_DATE_OF_DEATH_IN_DAYS;
    }

    public void setAGE_AT_TM_DX_DATE_IN_DAYS(Integer AGE_AT_TM_DX_DATE_IN_DAYS) {
        this.AGE_AT_TM_DX_DATE_IN_DAYS = AGE_AT_TM_DX_DATE_IN_DAYS != null ? AGE_AT_TM_DX_DATE_IN_DAYS : -1;
    }    
    
    public String getDEATH_SOURCE_DESCRIPTION() {
        return DEATH_SOURCE_DESCRIPTION;
    }

    public void setDEATH_SOURCE_DESCRIPTION(String DEATH_SOURCE_DESCRIPTION) {
        this.DEATH_SOURCE_DESCRIPTION =  StringUtils.isNotEmpty(DEATH_SOURCE_DESCRIPTION) ? DEATH_SOURCE_DESCRIPTION : "NA";
    }

    public String getPT_COUNTRY() {
        return PT_COUNTRY;
    }

    public void setPT_COUNTRY(String PT_COUNTRY) {
        this.PT_COUNTRY =  StringUtils.isNotEmpty(PT_COUNTRY) ? PT_COUNTRY : "NA";
    }

    public String getPT_STATE() {
        return PT_STATE;
    }

    public void setPT_STATE(String PT_STATE) {
        this.PT_STATE =  StringUtils.isNotEmpty(PT_STATE) ? PT_STATE : "NA";
    }

    public String getPT_ZIP3_CD() {
        return PT_ZIP3_CD;
    }

    public void setPT_ZIP3_CD(String PT_ZIP3_CD) {
        this.PT_ZIP3_CD =  StringUtils.isNotEmpty(PT_ZIP3_CD) ? PT_ZIP3_CD : "NA";
    }

    public Integer getPT_BIRTH_YEAR() {
        return PT_BIRTH_YEAR;
    }

    public void setPT_BIRTH_YEAR(Integer PT_BIRTH_YEAR) {
        this.PT_BIRTH_YEAR = PT_BIRTH_YEAR != null ? PT_BIRTH_YEAR : -1;
    }

    public String getPT_SEX_DESC() {
        return PT_SEX_DESC;
    }

    public void setPT_SEX_DESC(String PT_SEX_DESC) {
        this.PT_SEX_DESC =  StringUtils.isNotEmpty(PT_SEX_DESC) ? PT_SEX_DESC : "NA";
    }

    public String getPT_VITAL_STATUS() {
        return PT_VITAL_STATUS;
    }

    public void setPT_VITAL_STATUS(String PT_VITAL_STATUS) {
        this.PT_VITAL_STATUS =  StringUtils.isNotEmpty(PT_VITAL_STATUS) ? PT_VITAL_STATUS : "NA";
    }

    public String getPT_MARITAL_STS_DESC() {
        return PT_MARITAL_STS_DESC;
    }

    public void setPT_MARITAL_STS_DESC(String PT_MARITAL_STS_DESC) {
        this.PT_MARITAL_STS_DESC =  StringUtils.isNotEmpty(PT_MARITAL_STS_DESC) ? PT_MARITAL_STS_DESC : "NA";
    }

    public Integer getPT_DEATH_YEAR() {
        return PT_DEATH_YEAR;
    }

    public void setPT_DEATH_YEAR(Integer PT_DEATH_YEAR) {
        this.PT_DEATH_YEAR = PT_DEATH_YEAR != null ? PT_DEATH_YEAR : -1;
    }

    public String getPT_MRN_CREATE_YEAR() {
        return PT_MRN_CREATE_YEAR;
    }

    public void setPT_MRN_CREATE_YEAR(String PT_MRN_CREATE_YEAR) {
        this.PT_MRN_CREATE_YEAR =  StringUtils.isNotEmpty(PT_MRN_CREATE_YEAR) ? PT_MRN_CREATE_YEAR : "NA";
    }
    
    public String getOS_STATUS(){
        return OS_STATUS.trim().equals("ALIVE") ? "LIVING" : "DECEASED";
    }
    
    public void setOS_STATUS(String OS_STATUS) {
        this.OS_STATUS = StringUtils.isNotEmpty(OS_STATUS) ? OS_STATUS.trim() : "NA";
    }
    
    public String getOS_MONTHS() {
        if (getOS_STATUS().equals("LIVING")) {
            if (AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS != null && AGE_AT_TM_DX_DATE_IN_DAYS != null) {
                return String.format("%.3f", (AGE_AT_LAST_KNOWN_ALIVE_IN_DAYS - AGE_AT_TM_DX_DATE_IN_DAYS) / 30.4167);
            }            
        }
        else {
            if (AGE_AT_DATE_OF_DEATH_IN_DAYS != null && AGE_AT_TM_DX_DATE_IN_DAYS != null) {
                return String.format("%.3f", (AGE_AT_DATE_OF_DEATH_IN_DAYS - AGE_AT_TM_DX_DATE_IN_DAYS) / 30.4167);
            }            
        }
        return "NA";
    }
    
    public void setOS_MONTHS() {
        this.OS_MONTHS = StringUtils.isNotEmpty(OS_MONTHS) ? OS_MONTHS : "NA";
    }
    
    public String getDARWIN_PATIENT_AGE(){
        if(this.PT_BIRTH_YEAR>-1){
            if(this.PT_DEATH_YEAR>-1){
                Integer i = this.PT_DEATH_YEAR-this.PT_BIRTH_YEAR;
                //Age > 90 is considered identifying
                if (i >= 90){
                    return "90";
                }
                return i.toString();
            }
            Integer i = currentYear-this.PT_BIRTH_YEAR;
            //Age > 90 is considered identifying
            if (i >= 90) {
                return "90";
            }
            return i.toString();
        }
        else{
            return "NA";
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_ID_DEMO");
        fieldNames.add("DARWIN_PATIENT_AGE");
        fieldNames.add("RACE");
        fieldNames.add("RELIGION");
        fieldNames.add("GENDER");
        fieldNames.add("PT_VITAL_STATUS");
        fieldNames.add("AGE_AT_DIAGNOSIS");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");

        return fieldNames;

    }
    public List<String> getHeaders() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("DARWIN_PATIENT_AGE");
        fieldNames.add("DARWIN_RACE");
        fieldNames.add("DARWIN_RELIGION");
        fieldNames.add("DARWIN_SEX");
        fieldNames.add("DARWIN_VITAL_STATUS");
        fieldNames.add("DARWIN_AGE_AT_DIAGNOSIS");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");

        return fieldNames;

    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

}
