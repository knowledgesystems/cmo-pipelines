/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.crdb.pipeline.model;

import java.util.*;

/**
 * Model for CRDBPDXClinicalPatientDataset results.
 *
 * @author Avery Wang
 */

public class CRDBPDXClinicalAnnotationMapping {

    private String PATIENT_ID;
    private String SOURCE_STUDY_ID;
    private String DESTINATION_STUDY_ID;
    private String CLINICAL_ATTRIBUTE;

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    private static List<String> fieldNameList = initializeFieldNameList();

    /**
     * No args constructor for use in serialization
     */
    public CRDBPDXClinicalAnnotationMapping() {
    }

    public CRDBPDXClinicalAnnotationMapping(String SOURCE_STUDY_ID, String CLINICAL_ATTRIBUTE, String DESTINATION_STUDY_ID) {
        this.SOURCE_STUDY_ID = SOURCE_STUDY_ID == null ? "NA" : SOURCE_STUDY_ID;
        this.CLINICAL_ATTRIBUTE = CLINICAL_ATTRIBUTE == null ? "NA" : CLINICAL_ATTRIBUTE;
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID == null ? "NA" : DESTINATION_STUDY_ID;
    }

    /**
     *
     * @return SOURCE_STUDY_ID
     */
    public String getSOURCE_STUDY_ID() {
        return SOURCE_STUDY_ID;
    }

    /**
     *
     * @param SOURCE_STUDY_ID
     */
    public void setSOURCE_STUDY_ID(String SOURCE_STUDY_ID) {
        this.SOURCE_STUDY_ID = SOURCE_STUDY_ID;
    }

    /**
     *
     * @return DESTINATION_STUDY_ID
     */
    public String getDESTINATION_STUDY_ID() {
        return DESTINATION_STUDY_ID;
    }

    /**
     *
     * @param DESTINATION_STUDY_ID
     */
    public void setDESTINATION_STUDY_ID(String DESTINATION_STUDY_ID) {
        this.DESTINATION_STUDY_ID = DESTINATION_STUDY_ID;
    }

    /**
     *
     * @return CLINICAL_ATTRIBUTE
     */
    public String getCLINICAL_ATTRIBUTE() {
        return CLINICAL_ATTRIBUTE;
    }

    /**
     *
     * @param CLINICAL_ATTRIBUTE
     */
    public void setCLINICAL_ATTRIBUTE(String CLINICAL_ATTRIBUTE) {
        this.CLINICAL_ATTRIBUTE = CLINICAL_ATTRIBUTE;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    /**
     * Returns the field names without additional properties.
     * @return List<String>
     */
    public static List<String> getFieldNames() {
        return CRDBPDXClinicalAnnotationMapping.fieldNameList;
    }

    private static List<String> initializeFieldNameList() {
        List<String> fieldNameList = new ArrayList<String>(3);
        fieldNameList.add("SOURCE_STUDY_ID");
        fieldNameList.add("CLINICAL_ATTRIBUTE");
        fieldNameList.add("DESTINATION_STUDY_ID");
        return Collections.unmodifiableList(fieldNameList);
    }
}
