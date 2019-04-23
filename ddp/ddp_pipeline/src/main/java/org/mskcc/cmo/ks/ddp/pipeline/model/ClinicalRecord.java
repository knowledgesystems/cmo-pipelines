/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline.model;

import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import java.text.ParseException;
import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class ClinicalRecord {
    private String PATIENT_ID;
    private String AGE_CURRENT;
    private String RACE;
    private String RELIGION;
    private String SEX;
    private String ETHNICITY;
    private String OS_STATUS;
    private String PED_IND;
    private String OS_MONTHS;
    private String RADIATION_THERAPY;
    private String CHEMOTHERAPY;
    private String SURGERY;

    public ClinicalRecord(){}

    public ClinicalRecord(DDPCompositeRecord compositeRecord) throws ParseException {
        this.PATIENT_ID = compositeRecord.getDmpPatientId();
        this.AGE_CURRENT = DDPUtils.resolvePatientCurrentAge(compositeRecord);
        this.RACE = compositeRecord.getPatientRace() == null ? "NA" : compositeRecord.getPatientRace();
        this.RELIGION = compositeRecord.getPatientReligion() == null ? "NA" : compositeRecord.getPatientReligion();
        this.SEX = DDPUtils.resolvePatientSex(compositeRecord);
        this.ETHNICITY = compositeRecord.getPatientEthnicity() == null ? "NA" : compositeRecord.getPatientEthnicity();;
        this.OS_STATUS = DDPUtils.resolveOsStatus(compositeRecord);
        this.PED_IND = DDPUtils.resolvePediatricCohortPatientStatus(compositeRecord.getPediatricPatientStatus());
        this.OS_MONTHS = DDPUtils.resolveOsMonths(OS_STATUS, compositeRecord);
        this.RADIATION_THERAPY = compositeRecord.hasReceivedRadiation() ? "Yes" : "No";
        this.CHEMOTHERAPY = compositeRecord.hasReceivedChemo() ? "Yes" : "No";
        this.SURGERY = compositeRecord.hasReceivedSurgery() ? "Yes" : "No";
    }

    /**
     * @return the PATIENT_ID
     */
    public String getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID the PATIENT_ID to set
     */
    public void setPATIENT_ID(String PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * @return the AGE_CURRENT
     */
    public String getAGE_CURRENT() {
        return AGE_CURRENT;
    }

    /**
     * @param AGE_CURRENT the AGE_CURRENT to set
     */
    public void setAGE_CURRENT(String AGE_CURRENT) {
        this.AGE_CURRENT = AGE_CURRENT;
    }

    /**
     * @return the RACE
     */
    public String getRACE() {
        return RACE;
    }

    /**
     * @param RACE the RACE to set
     */
    public void setRACE(String RACE) {
        this.RACE = RACE;
    }

    /**
     * @return the RELIGION
     */
    public String getRELIGION() {
        return RELIGION;
    }

    /**
     * @param RELIGION the RELIGION to set
     */
    public void setRELIGION(String RELIGION) {
        this.RELIGION = RELIGION;
    }

    /**
     * @return the SEX
     */
    public String getSEX() {
        return SEX;
    }

    /**
     * @param SEX the SEX to set
     */
    public void setSEX(String SEX) {
        this.SEX = SEX;
    }

    /**
     * @return the ETHNICITY
     */
    public String getETHNICITY() {
        return ETHNICITY;
    }

    /**
     * @param ETHNICITY the ETHNICITY to set
     */
    public void setETHNICITY(String ETHNICITY) {
        this.ETHNICITY = ETHNICITY;
    }

    /**
     * @return the OS_STATUS
     */
    public String getOS_STATUS() {
        return OS_STATUS;
    }

    /**
     * @param OS_STATUS the OS_STATUS to set
     */
    public void setOS_STATUS(String OS_STATUS) {
        this.OS_STATUS = OS_STATUS;
    }

    /**
     * @return the PED_IND
     */
    public String getPED_IND() {
        return PED_IND;
    }

    /**
     * @param PED_IND the PED_IND to set
     */
    public void setPED_IND(String PED_IND) {
        this.PED_IND = PED_IND;
    }

    /**
     * @return the OS_MONTHS
     */
    public String getOS_MONTHS() {
        return OS_MONTHS;
    }

    /**
     * @param OS_MONTHS the OS_MONTHS to set
     */
    public void setOS_MONTHS(String OS_MONTHS) {
        this.OS_MONTHS = OS_MONTHS;
    }

    /**
     * @return the RADIATION_THERAPY
     */
    public String getRADIATION_THERAPY() {
        return RADIATION_THERAPY;
    }

    /**
     * @param RADIATION_THERAPY the RADIATION_THERAPY to set
     */
    public void setRADIATION_THERAPY(String RADIATION_THERAPY) {
        this.RADIATION_THERAPY = RADIATION_THERAPY;
    }

    /**
     * @return the CHEMOTHERAPY
     */
    public String getCHEMOTHERAPY() {
        return CHEMOTHERAPY;
    }

    /**
     * @param CHEMOTHERAPY the CHEMOTHERAPY to set
     */
    public void setCHEMOTHERAPY(String CHEMOTHERAPY) {
        this.CHEMOTHERAPY = CHEMOTHERAPY;
    }

    /**
     * @return the SURGERY
     */
    public String getSURGERY() {
        return SURGERY;
    }

    /**
     * @param SURGERY the SURGERY to set
     */
    public void setSURGERY(String SURGERY) {
        this.SURGERY = SURGERY;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Returns field names as list of strings.
     *
     * @return
     */
    public static List<String> getFieldNames(Boolean includeDiagnosis, Boolean includeRadiation, Boolean includeChemotherapy, Boolean includeSurgery) {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("AGE_CURRENT");
        fieldNames.add("RACE");
        fieldNames.add("RELIGION");
        fieldNames.add("SEX");
        fieldNames.add("ETHNICITY");
        fieldNames.add("OS_STATUS");
        fieldNames.add("PED_IND");
        // OS_MONTHS depends on fields from the diagnosis query
        // so if querying diagnosis is turned off all fields will be NA
        if (includeDiagnosis) {
            fieldNames.add("OS_MONTHS");
        }
        if (includeRadiation) {
            fieldNames.add("RADIATION_THERAPY");
        }
        if (includeChemotherapy) {
            fieldNames.add("CHEMOTHERAPY");
        }
        if (includeSurgery) {
            fieldNames.add("SURGERY");
        }
        return fieldNames;
    }
}
