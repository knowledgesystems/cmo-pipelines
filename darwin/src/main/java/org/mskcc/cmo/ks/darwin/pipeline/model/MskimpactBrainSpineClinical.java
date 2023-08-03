/*
 * Copyright (c) 2016, 2023 Memorial Sloan Kettering Cancer Center.
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

import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.cbioportal.cmo.pipelines.common.util.ClinicalValueUtil;

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

        this.DMP_PATIENT_ID_BRAINSPINECLIN = ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_BRAINSPINECLIN);
        this.DMP_SAMPLE_ID_BRAINSPINECLIN = ClinicalValueUtil.defaultWithNA(DMP_SAMPLE_ID_BRAINSPINECLIN);
        this.AGE = ClinicalValueUtil.defaultWithNA(AGE);
        this.SEX = ClinicalValueUtil.defaultWithNA(SEX);
        this.OS_STATUS = ClinicalValueUtil.defaultWithNA(OS_STATUS);
        this.OS_MONTHS = ClinicalValueUtil.defaultWithNA(OS_MONTHS);
        this.DFS_STATUS = ClinicalValueUtil.defaultWithNA(DFS_STATUS);
        this.DFS_MONTHS = ClinicalValueUtil.defaultWithNA(DFS_MONTHS);
        this.HISTOLOGY = ClinicalValueUtil.defaultWithNA(HISTOLOGY);
        this.WHO_GRADE = ClinicalValueUtil.defaultWithNA(WHO_GRADE);
        this.MGMT_STATUS = ClinicalValueUtil.defaultWithNA(MGMT_STATUS);
    }

    public String getDMP_PATIENT_ID_BRAINSPINECLIN() {
        return DMP_PATIENT_ID_BRAINSPINECLIN;
    }

    public void setDMP_PATIENT_ID_BRAINSPINECLIN(String DMP_PATIENT_ID_BRAINSPINECLIN) {
        this.DMP_PATIENT_ID_BRAINSPINECLIN = ClinicalValueUtil.defaultWithNA(DMP_PATIENT_ID_BRAINSPINECLIN);
    }

    public String getDMP_SAMPLE_ID_BRAINSPINECLIN() {
        return DMP_SAMPLE_ID_BRAINSPINECLIN;
    }

    public void setDMP_SAMPLE_ID_BRAINSPINECLIN(String DMP_SAMPLE_ID_BRAINSPINECLIN) {
        this.DMP_SAMPLE_ID_BRAINSPINECLIN = ClinicalValueUtil.defaultWithNA(DMP_SAMPLE_ID_BRAINSPINECLIN);
    }

    public String getAGE() {
        return AGE;
    }

    public void setAGE(String AGE) {
        this.AGE = ClinicalValueUtil.defaultWithNA(AGE);
    }

    public String getSEX() {
        return SEX;
    }

    public void setSEX(String SEX) {
        this.SEX = ClinicalValueUtil.defaultWithNA(SEX);
    }

    public String getOS_STATUS() {
        return OS_STATUS;
    }

    public void setOS_STATUS(String OS_STATUS) {
        this.OS_STATUS = ClinicalValueUtil.defaultWithNA(OS_STATUS);
    }

    public String getOS_MONTHS() {
        return OS_MONTHS;
    }

    public void setOS_MONTHS(String OS_MONTHS) {
        this.OS_MONTHS = ClinicalValueUtil.defaultWithNA(OS_MONTHS);
    }

    public String getDFS_STATUS() {
        return DFS_STATUS;
    }

    public void setDFS_STATUS(String DFS_STATUS) {
        this.DFS_STATUS = ClinicalValueUtil.defaultWithNA(DFS_STATUS);
    }

    public String getDFS_MONTHS() {
        return DFS_MONTHS;
    }

    public void setDFS_MONTHS(String DFS_MONTHS) {
        this.DFS_MONTHS = ClinicalValueUtil.defaultWithNA(DFS_MONTHS);
    }

    public String getHISTOLOGY() {
        return HISTOLOGY;
    }

    public void setHISTOLOGY(String HISTOLOGY) {
        this.HISTOLOGY = ClinicalValueUtil.defaultWithNA(HISTOLOGY);
    }

    public String getWHO_GRADE() {
        return WHO_GRADE;
    }

    public void setWHO_GRADE(String WHO_GRADE) {
        this.WHO_GRADE = ClinicalValueUtil.defaultWithNA(WHO_GRADE);
    }

    public String getMGMT_STATUS() {
        return MGMT_STATUS;
    }

    public void setMGMT_STATUS(String MGMT_STATUS) {
        this.MGMT_STATUS = ClinicalValueUtil.defaultWithNA(MGMT_STATUS);
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
        fieldNames.add("SEX_DMT");
        fieldNames.add("OS_STATUS_DMT");
        fieldNames.add("OS_MONTHS_DMT");
        fieldNames.add("DFS_STATUS");
        fieldNames.add("DFS_MONTHS");
        fieldNames.add("HISTOLOGICAL_DIAGNOSIS");
        fieldNames.add("WHO_GRADE");
        fieldNames.add("MGMT_STATUS");
        fieldNames.add("PATIENT_ID");

        return fieldNames;
    }

}
