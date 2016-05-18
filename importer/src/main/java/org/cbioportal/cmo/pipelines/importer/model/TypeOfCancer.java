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
public class TypeOfCancer {
    
    private String TYPE_OF_CANCER_ID;
    private String NAME;
    private String CLINICAL_TRIAL_KEYWORDS;
    private String DEDICATED_COLOR;
    private String SHORT_NAME;
    private String PARENT;
    
    public TypeOfCancer(){}
    
    
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

    /**
     * @return the NAME
     */
    public String getNAME() {
        return NAME;
    }

    /**
     * @param NAME the NAME to set
     */
    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    /**
     * @return the CLINICAL_TRIAL_KEYWORDS
     */
    public String getCLINICAL_TRIAL_KEYWORDS() {
        return CLINICAL_TRIAL_KEYWORDS;
    }

    /**
     * @param CLINICAL_TRIAL_KEYWORDS the CLINICAL_TRIAL_KEYWORDS to set
     */
    public void setCLINICAL_TRIAL_KEYWORDS(String CLINICAL_TRIAL_KEYWORDS) {
        this.CLINICAL_TRIAL_KEYWORDS = CLINICAL_TRIAL_KEYWORDS;
    }

    /**
     * @return the DEDICATED_COLOR
     */
    public String getDEDICATED_COLOR() {
        return DEDICATED_COLOR;
    }

    /**
     * @param DEDICATED_COLOR the DEDICATED_COLOR to set
     */
    public void setDEDICATED_COLOR(String DEDICATED_COLOR) {
        this.DEDICATED_COLOR = DEDICATED_COLOR;
    }

    /**
     * @return the SHORT_NAME
     */
    public String getSHORT_NAME() {
        return SHORT_NAME;
    }

    /**
     * @param SHORT_NAME the SHORT_NAME to set
     */
    public void setSHORT_NAME(String SHORT_NAME) {
        this.SHORT_NAME = SHORT_NAME;
    }

    /**
     * @return the PARENT
     */
    public String getPARENT() {
        return PARENT;
    }

    /**
     * @param PARENT the PARENT to set
     */
    public void setPARENT(String PARENT) {
        this.PARENT = PARENT;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    /**
     * Returns the field NAMEs in TypeOfCancer
     * @return List<String>
     */
   
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("TYPE_OF_CANCER_ID");
        fieldNames.add("NAME");
        fieldNames.add("CLINICAL_TRIAL_KEYWORDS");
        fieldNames.add("DEDICATED_COLOR");
        fieldNames.add("SHORT_NAME");
        fieldNames.add("PARENT");
        
        return fieldNames; 
    }
}