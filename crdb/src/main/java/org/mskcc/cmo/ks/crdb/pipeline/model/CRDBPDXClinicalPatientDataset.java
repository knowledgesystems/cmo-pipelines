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
    private String DESTINATION_STUDY;
    private String SEX;
    private String ETHNICITY;
    private String RACE;
    private String SMOKING_HISTORY;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     */
    public CRDBPDXClinicalPatientDataset() {
    }

    public CRDBPDXClinicalPatientDataset(String PATIENT_ID, String DESTINATION_STUDY, String SEX, String ETHNICITY, String RACE, String SMOKING_HISTORY) {
        this.PATIENT_ID = PATIENT_ID;
        this.DESTINATION_STUDY = DESTINATION_STUDY;
        this.SEX = SEX;
        this.ETHNICITY = ETHNICITY;
        this.RACE = RACE;
        this.SMOKING_HISTORY = SMOKING_HISTORY;
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
     * @return DESTINATION_STUDY
     */
    public String getDESTINATION_STUDY() {
        return DESTINATION_STUDY;
    }

    /**
     * @param DESTINATION_STUDY
     */
    public void setDESTINATION_STUDY(String DESTINATION_STUDY) {
        this.DESTINATION_STUDY = DESTINATION_STUDY;
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

    /**
     * Returns the field names in CRDBDataset without additional properties.
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("PATIENT_ID");
        fieldNames.add("DESTINATION_STUDY");
        fieldNames.add("SEX");
        fieldNames.add("ETHNICITY");
        fieldNames.add("RACE");
        fieldNames.add("SMOKING_HISTORY");
        return fieldNames;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
