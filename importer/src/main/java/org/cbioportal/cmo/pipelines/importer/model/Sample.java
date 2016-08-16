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
public class Sample {
    private int INTERNAL_ID;
    private String STABLE_ID;
    private String SAMPLE_TYPE;
    private int PATIENT_ID;
    private String TYPE_OF_CANCER_ID;

    /**
     * @return the INTERNAL_ID
     */
    public int getINTERNAL_ID() {
        return INTERNAL_ID;
    }

    /**
     * @param INTERNAL_ID the INTERNAL_ID to set
     */
    public void setINTERNAL_ID(int INTERNAL_ID) {
        this.INTERNAL_ID = INTERNAL_ID;
    }

    /**
     * @return the STABLE_ID
     */
    public String getSTABLE_ID() {
        return STABLE_ID;
    }

    /**
     * @param STABLE_ID the STABLE_ID to set
     */
    public void setSTABLE_ID(String STABLE_ID) {
        this.STABLE_ID = STABLE_ID;
    }

    /**
     * @return the SAMPLE_TYPE
     */
    public String getSAMPLE_TYPE() {
        return SAMPLE_TYPE;
    }

    /**
     * @param SAMPLE_TYPE the SAMPLE_TYPE to set
     */
    public void setSAMPLE_TYPE(String SAMPLE_TYPE) {
        this.SAMPLE_TYPE = SAMPLE_TYPE;
    }

    /**
     * @return the PATIENT_ID
     */
    public int getPATIENT_ID() {
        return PATIENT_ID;
    }

    /**
     * @param PATIENT_ID the PATIENT_ID to set
     */
    public void setPATIENT_ID(int PATIENT_ID) {
        this.PATIENT_ID = PATIENT_ID;
    }

    /**
     * @return the TYPE_OF_CANCER_ID
     */
    public String getTYPE_OF_CANCER_ID() {
        return TYPE_OF_CANCER_ID;
    }

    /**
     * @param TYPE_OF_CANCER_ID the TYPE_OF_CANCER_ID to set
     */
    public void setTYPE_OF_CANCER_ID(String TYPE_OF_CANCER_ID) {
        this.TYPE_OF_CANCER_ID = TYPE_OF_CANCER_ID;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    /**
     * Returns the field names in Sample
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("INTERNAL_ID");
        fieldNames.add("STABLE_ID");
        fieldNames.add("SAMPLE_TYPE");
        fieldNames.add("PATIENT_ID");
        fieldNames.add("TYPE_OF_CANCER_ID");
        
        return fieldNames;
    }       
    
}
