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
 * This represents a cancer study, with a set of cases and some data sets.
 *
 * @author ochoaa
 */
public class CancerStudy {
    // default internal id when importing new cancer study record
    public static final int NO_SUCH_STUDY = -1;
    
    private int CANCER_STUDY_ID;
    private String CANCER_STUDY_IDENTIFIER;
    private String TYPE_OF_CANCER_ID;
    private String NAME;
    private String SHORT_NAME;
    private String DESCRIPTION;
    private boolean PUBLIC_STUDY;
    private String PMID;
    private String CITATION;
    private String GROUPS;
    private int STATUS;
    private Date IMPORT_DATE;
//    private boolean updateStudyDelete;
//    private boolean addGlobalCaseLists;
    
    public CancerStudy() {}
    
    public CancerStudy(Properties properties) {
        
        // default cancer_study_id = -1 if loading from properties        
        this.CANCER_STUDY_ID = NO_SUCH_STUDY;
        
        // required meta_study.txt properties
        this.CANCER_STUDY_IDENTIFIER = properties.getProperty("cancer_study_identifier");
        this.TYPE_OF_CANCER_ID = properties.getProperty("type_of_cancer");
        this.NAME = properties.getProperty("name");        
        this.DESCRIPTION = properties.getProperty("description");        
        
        // option properties with defaults set --> change to property value if they exist
        this.SHORT_NAME = NAME;
        this.PUBLIC_STUDY = false;
                
        this.PMID = "";
        this.CITATION = "";
        this.GROUPS = "";
        this.STATUS = 0;
        try {
            this.SHORT_NAME = properties.getProperty("short_name");
            this.PUBLIC_STUDY = properties.getProperty("public_study").equals("true");
            
            this.PMID = properties.getProperty("pmid");
            this.CITATION = properties.getProperty("citation");
            this.GROUPS = properties.getProperty("groups");
            this.STATUS = properties.getProperty("status").equals("1")?1:0;
        }
        catch (NullPointerException ex) {}

//        this.updateStudyDelete = false;
//        this.addGlobalCaseLists = false;
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

    /**
     * @return the CANCER_STUDY_IDENTIFIER
     */
    public String getCANCER_STUDY_IDENTIFIER() {
        return CANCER_STUDY_IDENTIFIER;
    }

    /**
     * @param CANCER_STUDY_IDENTIFIER the CANCER_STUDY_IDENTIFIER to set
     */
    public void setCANCER_STUDY_IDENTIFIER(String CANCER_STUDY_IDENTIFIER) {
        this.CANCER_STUDY_IDENTIFIER = CANCER_STUDY_IDENTIFIER;
    }

    /**
     * @return the TYPE_OF_CANCER_ID
     */
    public String getTYPE_OF_CANCER_ID() {
//        return DaoTypeOfCancer.getTypeOfCancer(this.TYPE_OF_CANCER_ID).getNAME();
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
     * @return the SHORT_NAME
     */
    public String getSHORT_NAME() {
        if (SHORT_NAME==null || SHORT_NAME.length()==0) {
            return CANCER_STUDY_IDENTIFIER;
        }
        return SHORT_NAME;
    }

    /**
     * @param SHORT_NAME the SHORT_NAME to set
     */
    public void setSHORT_NAME(String SHORT_NAME) {
        this.SHORT_NAME = SHORT_NAME;
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
     * @return the PUBLIC_STUDY
     */
    public boolean getPUBLIC_STUDY() {
        return PUBLIC_STUDY;
    }

    /**
     * @param PUBLIC_STUDY the PUBLIC_STUDY to set
     */
    public void setPUBLIC_STUDY(boolean PUBLIC_STUDY) {
        this.PUBLIC_STUDY = PUBLIC_STUDY;
    }

    /**
     * @return the PMID
     */
    public String getPMID() {
        return PMID;
    }

    /**
     * @param PMID the PMID to set
     */
    public void setPMID(String PMID) {
        this.PMID = PMID;
    }

    /**
     * @return the CITATION
     */
    public String getCITATION() {
        return CITATION;
    }

    /**
     * @param CITATION the CITATION to set
     */
    public void setCITATION(String CITATION) {
        this.CITATION = CITATION;
    }

    /**
     * @return the GROUPS
     */
    public String getGROUPS() {
        return GROUPS;
    }

    /**
     * @param GROUPS the GROUPS to set
     */
    public void setGROUPS(String GROUPS) {
        this.GROUPS = GROUPS;
    }

    /**
     * @return the STATUS
     */
    public int getSTATUS() {
        return STATUS;
    }

    /**
     * @param STATUS the STATUS to set
     */
    public void setSTATUS(int STATUS) {
        this.STATUS = STATUS;
    }

    /**
     * @return the IMPORT_DATE
     */
    public Date getIMPORT_DATE() {
        return IMPORT_DATE;
    }

    /**
     * @param IMPORT_DATE the IMPORT_DATE to set
     */
    public void setIMPORT_DATE(Date IMPORT_DATE) {
        this.IMPORT_DATE = IMPORT_DATE;
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
        fieldNames.add("CANCER_STUDY_ID");
        fieldNames.add("CANCER_STUDY_IDENTIFIER");
        fieldNames.add("TYPE_OF_CANCER_ID");
        fieldNames.add("NAME");
        fieldNames.add("SHORT_NAME");        
        fieldNames.add("DESCRIPTION");
        fieldNames.add("PUBLIC_STUDY");
        fieldNames.add("PMID");
        fieldNames.add("CITATION");
        fieldNames.add("GROUPS");
        fieldNames.add("STATUS");
        fieldNames.add("IMPORT_DATE");
        
        return fieldNames;
    }
}