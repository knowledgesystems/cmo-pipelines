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
public class Patient {
    
    public static int NO_SUCH_PATIENT = -1;
    
    
    private int INTERNAL_ID;
    private String STABLE_ID;
    private int CANCER_STUDY_ID;
//    private Map<String, ClinicalData> clinicalDataMap;    
    
    public Patient() {}
    
    public Patient(String stableId, int cancerStudyId) {
        this.INTERNAL_ID = NO_SUCH_PATIENT;
        this.STABLE_ID = stableId;
        this.CANCER_STUDY_ID = cancerStudyId;        
    }
    
    public Patient(int internalId, String stableId, int cancerStudyId) {
        this.INTERNAL_ID = internalId;
        this.STABLE_ID = stableId;
        this.CANCER_STUDY_ID = cancerStudyId;        
    }    
    
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
     * @return the CANCER_STUDY_ID
     */
    public int getCANCER_STUDY_ID() {
        return CANCER_STUDY_ID;
    }

    /**
     * @param CANCER_STUDY_ID the CANCER_STUDY_ID to set
     */
    public void setCANCER_STUDY_ID(int CANCER_STUDY_ID) {
        this.CANCER_STUDY_ID = CANCER_STUDY_ID;
    }    

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    /**
     * Returns the field names in Patient
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("INTERNAL_ID");
        fieldNames.add("STABLE_ID");
        fieldNames.add("CANCER_STUDY_ID");
        
        return fieldNames;
    }                
}
