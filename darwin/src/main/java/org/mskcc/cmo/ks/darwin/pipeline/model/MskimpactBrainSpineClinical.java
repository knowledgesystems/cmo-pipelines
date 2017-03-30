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

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
/**
 *
 * @author jake
 */
public class MskimpactBrainSpineClinical {
    private String DMT_PATIENT_ID_BRAINSPINECLIN;
    private String DMP_PATIENT_ID_BRAINSPINECLIN;
    private String DMP_SAMPLE_ID_BRAINSPINECLIN;
    private String AGE;
    private String SEX;
    private String OS_STATUS;
    private String OS_MONTHS;
    private String DFS_STATUS;
    private String DFS_MONTHS;
    private String HISTOLOGY; 
    private String WHO_GRADE;
    private String MGMT_STATUS;
    
    public MskimpactBrainSpineClinical(){}
    
    public MskimpactBrainSpineClinical(
            String DMP_PATIENT_ID_BRAINSPINECLIN,
            String DMP_SAMPLE_ID_BRAINSPINECLIN,
            String AGE,
            String SEX,
            String OS_STATUS,
            String OS_MONTHS,
            String DFS_STATUS,
            String DFS_MONTHS,
            String HISTOLOGY,
            String WHO_GRADE,
            String MGMT_STATUS){
        
        this.DMP_PATIENT_ID_BRAINSPINECLIN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_BRAINSPINECLIN) ? DMP_PATIENT_ID_BRAINSPINECLIN : "NA";
        this.DMP_SAMPLE_ID_BRAINSPINECLIN =  StringUtils.isNotEmpty(DMP_SAMPLE_ID_BRAINSPINECLIN) ? DMP_SAMPLE_ID_BRAINSPINECLIN : "NA";
        this.AGE =  StringUtils.isNotEmpty(AGE) ? AGE : "NA";
        this.SEX =  StringUtils.isNotEmpty(SEX) ? SEX : "NA";
        this.OS_STATUS =  StringUtils.isNotEmpty(OS_STATUS) ? OS_STATUS : "NA";
        this.OS_MONTHS =  StringUtils.isNotEmpty(OS_MONTHS) ? OS_MONTHS : "NA";
        this.DFS_STATUS =  StringUtils.isNotEmpty(DFS_STATUS) ? DFS_STATUS : "NA";
        this.DFS_MONTHS =  StringUtils.isNotEmpty(DFS_MONTHS) ? DFS_MONTHS : "NA";
        this.HISTOLOGY =  StringUtils.isNotEmpty(HISTOLOGY) ? HISTOLOGY : "NA";
        this.WHO_GRADE =  StringUtils.isNotEmpty(WHO_GRADE) ? WHO_GRADE : "NA";
        this.MGMT_STATUS =  StringUtils.isNotEmpty(MGMT_STATUS) ? MGMT_STATUS : "NA";
    }
    
   

    public String getDMP_PATIENT_ID_BRAINSPINECLIN() {
        return DMP_PATIENT_ID_BRAINSPINECLIN;
    }

    public void setDMP_PATIENT_ID_BRAINSPINECLIN(String DMP_PATIENT_ID_BRAINSPINECLIN) {
        this.DMP_PATIENT_ID_BRAINSPINECLIN =  StringUtils.isNotEmpty(DMP_PATIENT_ID_BRAINSPINECLIN) ? DMP_PATIENT_ID_BRAINSPINECLIN : "NA";
    }

    public String getDMP_SAMPLE_ID_BRAINSPINECLIN() {
        return DMP_SAMPLE_ID_BRAINSPINECLIN;
    }

    public void setDMP_SAMPLE_ID_BRAINSPINECLIN(String DMP_SAMPLE_ID_BRAINSPINECLIN) {
        this.DMP_SAMPLE_ID_BRAINSPINECLIN =  StringUtils.isNotEmpty(DMP_SAMPLE_ID_BRAINSPINECLIN) ? DMP_SAMPLE_ID_BRAINSPINECLIN : "NA";
    }

    public String getAGE() {
        return AGE;
    }

    public void setAGE(String AGE) {
        this.AGE =  StringUtils.isNotEmpty(AGE) ? AGE : "NA";
    }

    public String getSEX() {
        return SEX;
    }

    public void setSEX(String SEX) {
        this.SEX =  StringUtils.isNotEmpty(SEX) ? SEX : "NA";
    }

    public String getOS_STATUS() {
        return OS_STATUS;
    }

    public void setOS_STATUS(String OS_STATUS) {
        this.OS_STATUS =  StringUtils.isNotEmpty(OS_STATUS) ? OS_STATUS : "NA";
    }

    public String getOS_MONTHS() {
        return OS_MONTHS;
    }

    public void setOS_MONTHS(String OS_MONTHS) {
        this.OS_MONTHS =  StringUtils.isNotEmpty(OS_MONTHS) ? OS_MONTHS : "NA";
    }

    public String getDFS_STATUS() {
        return DFS_STATUS;
    }

    public void setDFS_STATUS(String DFS_STATUS) {
        this.DFS_STATUS =  StringUtils.isNotEmpty(DFS_STATUS) ? DFS_STATUS : "NA";
    }

    public String getDFS_MONTHS() {
        return DFS_MONTHS;
    }

    public void setDFS_MONTHS(String DFS_MONTHS) {
        this.DFS_MONTHS =  StringUtils.isNotEmpty(DFS_MONTHS) ? DFS_MONTHS : "NA";
    }

    public String getHISTOLOGY() {
        return HISTOLOGY;
    }

    public void setHISTOLOGY(String HISTOLOGY) {
        this.HISTOLOGY =  StringUtils.isNotEmpty(HISTOLOGY) ? HISTOLOGY : "NA";
    }

    public String getWHO_GRADE() {
        return WHO_GRADE;
    }

    public void setWHO_GRADE(String WHO_GRADE) {
        this.WHO_GRADE =  StringUtils.isNotEmpty(WHO_GRADE) ? WHO_GRADE : "NA";
    }

    public String getMGMT_STATUS() {
        return MGMT_STATUS;
    }

    public void setMGMT_STATUS(String MGMT_STATUS) {
        this.MGMT_STATUS =  StringUtils.isNotEmpty(MGMT_STATUS) ? MGMT_STATUS : "NA";
    }
    
    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }
    
    public List<String> getFieldNames(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("DMP_SAMPLE_ID_BRAINSPINECLIN");
        fieldNames.add("AGE");
        fieldNames.add("SEX");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");
        fieldNames.add("DFS_STATUS");
        fieldNames.add("DFS_MONTHS");
        fieldNames.add("HISTOLOGY");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("DMP_PATIENT_ID_BRAINSPINECLIN");
        
        return fieldNames;
    }
    public List<String> getHeaders(){
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("SAMPLE_ID");
        fieldNames.add("AGE");
        fieldNames.add("SEX");
        fieldNames.add("OS_STATUS");
        fieldNames.add("OS_MONTHS");
        fieldNames.add("DFS_STATUS");
        fieldNames.add("DFS_MONTHS");
        fieldNames.add("HISTOLOGICAL_DIAGNOSIS");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("PATIENT_ID");
        
        return fieldNames;
    }


}
