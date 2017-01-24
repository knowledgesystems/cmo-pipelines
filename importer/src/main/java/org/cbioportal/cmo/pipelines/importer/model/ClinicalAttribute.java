/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.model;

import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class ClinicalAttribute {
    private String ATTR_ID;
    private String DISPLAY_NAME;
    private String DESCRIPTION;
    private String DATATYPE;
    private boolean PATIENT_ATTRIBUTE;
    private String PRIORITY;

    public ClinicalAttribute() {}
    
    public ClinicalAttribute(String attrId, String displayName, String description, String datatype, boolean patientAttribute, String priority) {
        this.ATTR_ID = attrId;
        this.DISPLAY_NAME = displayName;
        this.DESCRIPTION = description;
        this.DATATYPE = datatype;
        this.PATIENT_ATTRIBUTE = patientAttribute;
        this.PRIORITY = priority;
    }
    
    /**
     * @return the ATTR_ID
     */
    public String getATTR_ID() {
        return ATTR_ID;
    }

    /**
     * @param ATTR_ID the ATTR_ID to set
     */
    public void setATTR_ID(String ATTR_ID) {
        this.ATTR_ID = ATTR_ID;
    }

    /**
     * @return the DISPLAY_NAME
     */
    public String getDISPLAY_NAME() {
        return DISPLAY_NAME;
    }

    /**
     * @param DISPLAY_NAME the DISPLAY_NAME to set
     */
    public void setDISPLAY_NAME(String DISPLAY_NAME) {
        this.DISPLAY_NAME = DISPLAY_NAME;
    }

    /**
     * @return the DESCRIPTION
     */
    public String getDESCRIPTION() {
        return DESCRIPTION;
    }

    /**
     * @param DESCRIPTION the DESCRIPTION to set
     */
    public void setDESCRIPTION(String DESCRIPTION) {
        this.DESCRIPTION = DESCRIPTION;
    }

    /**
     * @return the DATATYPE
     */
    public String getDATATYPE() {
        return DATATYPE;
    }

    /**
     * @param DATATYPE the DATATYPE to set
     */
    public void setDATATYPE(String DATATYPE) {
        this.DATATYPE = DATATYPE;
    }

    /**
     * @return the PATIENT_ATTRIBUTE
     */
    public boolean isPATIENT_ATTRIBUTE() {
        return PATIENT_ATTRIBUTE;
    }

    /**
     * @param PATIENT_ATTRIBUTE the PATIENT_ATTRIBUTE to set
     */
    public void setPATIENT_ATTRIBUTE(boolean PATIENT_ATTRIBUTE) {
        this.PATIENT_ATTRIBUTE = PATIENT_ATTRIBUTE;
    }

    /**
     * @return the PRIORITY
     */
    public String getPRIORITY() {
        return PRIORITY;
    }

    /**
     * @param PRIORITY the PRIORITY to set
     */
    public void setPRIORITY(String PRIORITY) {
        this.PRIORITY = PRIORITY;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    /**
     * Returns the field names in CancerStudy
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("ATTR_ID");
        fieldNames.add("DISPLAY_NAME");
        fieldNames.add("DESCRIPTION");
        fieldNames.add("DATATYPE");
        fieldNames.add("PATIENT_ATTRIBUTE");
        fieldNames.add("PRIORITY");
        
        return fieldNames;
    }
}