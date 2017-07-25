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
package org.mskcc.cmo.ks.crdb.model;

import java.util.*;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Model for CRDBSurvey results.
 *
 * @author ochoaa
 */

public class CRDBSurvey {
    private String DMP_ID;
    private Date QS_DATE;
    private String ADJ_TXT;
    private String NOSYSTXT;
    private String PRIOR_RX;
    private String BRAINMET;
    private String ECOG;
    private String COMMENTS;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

/**
* No args constructor for use in serialization
*
*/
public CRDBSurvey() {
}

/**
*
* @param DMP_ID
* @param QS_DATE
* @param ADJ_TXT
* @param NOSYSTXT
* @param PRIOR_RX
* @param BRAINMET
* @param ECOG
* @param COMMENTS
*/
public CRDBSurvey(String DMP_ID, Date QS_DATE, String ADJ_TXT, String NOSYSTXT,
        String PRIOR_RX, String BRAINMET, String ECOG, String COMMENTS) {
this.DMP_ID = DMP_ID==null?"NA":DMP_ID;
this.QS_DATE = QS_DATE==null?null:QS_DATE;
this.ADJ_TXT = ADJ_TXT==null?"NA":ADJ_TXT;
this.NOSYSTXT = NOSYSTXT==null?"NA":NOSYSTXT;
this.PRIOR_RX = PRIOR_RX==null?"NA":PRIOR_RX;
this.BRAINMET = BRAINMET==null?"NA":BRAINMET;
this.ECOG = ECOG==null?"NA":ECOG;
this.COMMENTS = COMMENTS==null?"NA":COMMENTS;
}

    /**
     *
     * @return DMP_ID
     */
    public String getDMP_ID() {
        return DMP_ID;
    }

    /**
     *
     * @param DMP_ID
     */
    public void setDMP_ID(String DMP_ID) {
        this.DMP_ID = DMP_ID;
    }

    /**
     *
     * @param DMP_ID
     * @return
     */
    public CRDBSurvey withDMP_ID(String DMP_ID) {
        this.DMP_ID = DMP_ID;
        return this;
    }

    /**
     *
     * @return QS_DATE
     */
    public Date getQS_DATE() {
        return QS_DATE;
    }

    /**
     *
     * @param QS_DATE
     */
    public void setQS_DATE(Date QS_DATE) {
        this.QS_DATE = QS_DATE;
    }

    /**
     *
     * @param QS_DATE
     * @return
     */
    public CRDBSurvey withQS_DATE(Date QS_DATE) {
        this.QS_DATE = QS_DATE;
        return this;
    }

    /**
     *
     * @return ADJ_TXT
     */
    public String getADJ_TXT() {
        return ADJ_TXT;
    }

    /**
     *
     * @param ADJ_TXT
     */
    public void setADJ_TXT(String ADJ_TXT) {
        this.ADJ_TXT = ADJ_TXT;
    }

    /**
     *
     * @param ADJ_TXT
     * @return
     */
    public CRDBSurvey withADJ_TXT(String ADJ_TXT) {
        this.ADJ_TXT = ADJ_TXT;
        return this;
    }

    /**
     *
     * @return NOSYSTXT
     */
    public String getNOSYSTXT() {
        return NOSYSTXT;
    }

    /**
     *
     * @param NOSYSTXT
     */
    public void setNOSYSTXT(String NOSYSTXT) {
        this.NOSYSTXT = NOSYSTXT;
    }

    /**
     *
     * @param NOSYSTXT
     * @return
     */
    public CRDBSurvey withNOSYSTXT(String NOSYSTXT) {
        this.NOSYSTXT = NOSYSTXT;
        return this;
    }

    /**
     *
     * @return PRIOR_RX
     */
    public String getPRIOR_RX() {
        return PRIOR_RX;
    }

    /**
     *
     * @param PRIOR_RX
     */
    public void setPRIOR_RX(String PRIOR_RX) {
        this.PRIOR_RX = PRIOR_RX;
    }

    /**
     *
     * @param PRIOR_RX
     * @return
     */
    public CRDBSurvey withPRIOR_RX(String PRIOR_RX) {
        this.PRIOR_RX = PRIOR_RX;
        return this;
    }

    /**
     *
     * @return BRAINMET
     */
    public String getBRAINMET() {
        return BRAINMET;
    }

    /**
     *
     * @param BRAINMET
     */
    public void setBRAINMET(String BRAINMET) {
        this.BRAINMET = BRAINMET;
    }

    /**
     *
     * @param BRAINMET
     * @return
     */
    public CRDBSurvey withBRAINMET(String BRAINMET) {
        this.BRAINMET = BRAINMET;
        return this;
    }

    /**
     *
     * @return ECOG
     */
    public String getECOG() {
        return ECOG;
    }

    /**
     *
     * @param ECOG
     */
    public void setECOG(String ECOG) {
        this.ECOG = ECOG;
    }

    /**
     *
     * @param ECOG
     * @return
     */
    public CRDBSurvey withECOG(String ECOG) {
        this.ECOG = ECOG;
        return this;
    }

    /**
     *
     * @return COMMENTS
     */
    public String getCOMMENTS() {
        return COMMENTS;
    }

    /**
     *
     * @param COMMENTS
     */
    public void setCOMMENTS(String COMMENTS) {
        this.COMMENTS = COMMENTS;
    }

    /**
     *
     * @param COMMENTS
     * @return
     */
    public CRDBSurvey withCOMMENTS(String COMMENTS) {
        this.COMMENTS = COMMENTS;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Returns the field names in CRDBSurvey without additional properties.
     * @return List<String>
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        for (String field : toString().substring(toString().indexOf("[")+1,toString().indexOf("]")).split(",")) {
            if (!field.startsWith("additionalProperties")) {
                fieldNames.add(field.split("=")[0]);
            }
        }
        return fieldNames;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CRDBSurvey withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }
}
