/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.source.model;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "AGE",
    "CHP_ACTIVE_IND",
    "COHORT_ID",
    "DAYS_ON_COHORT",
    "MRN",
    "NOTES",
    "PT_SEX",
    "PT_VITAL_STATUS",
    "P_ID"
})
public class CohortPatient implements Serializable {
    @JsonProperty("AGE")
    private Integer aGE;
    @JsonProperty("CHP_ACTIVE_IND")
    private String cHPACTIVEIND;
    @JsonProperty("COHORT_ID")
    private Integer cOHORTID;
    @JsonProperty("DAYS_ON_COHORT")
    private Integer dAYSONCOHORT;
    @JsonProperty("MRN")
    private String mRN;
    @JsonProperty("NOTES")
    private String nOTES;
    @JsonProperty("PT_SEX")
    private String pTSEX;
    @JsonProperty("PT_VITAL_STATUS")
    private String pTVITALSTATUS;
    @JsonProperty("P_ID")
    private Integer pID;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("AGE")
    public Integer getAGE() {
        return aGE;
    }

    @JsonProperty("AGE")
    public void setAGE(Integer aGE) {
        this.aGE = aGE;
    }

    @JsonProperty("CHP_ACTIVE_IND")
    public String getCHPACTIVEIND() {
        return cHPACTIVEIND;
    }

    @JsonProperty("CHP_ACTIVE_IND")
    public void setCHPACTIVEIND(String cHPACTIVEIND) {
        this.cHPACTIVEIND = cHPACTIVEIND;
    }

    @JsonProperty("COHORT_ID")
    public Integer getCOHORTID() {
        return cOHORTID;
    }

    @JsonProperty("COHORT_ID")
    public void setCOHORTID(Integer cOHORTID) {
        this.cOHORTID = cOHORTID;
    }

    @JsonProperty("DAYS_ON_COHORT")
    public Integer getDAYSONCOHORT() {
        return dAYSONCOHORT;
    }

    @JsonProperty("DAYS_ON_COHORT")
    public void setDAYSONCOHORT(Integer dAYSONCOHORT) {
        this.dAYSONCOHORT = dAYSONCOHORT;
    }

    @JsonProperty("MRN")
    public String getMRN() {
        return mRN;
    }

    @JsonProperty("MRN")
    public void setMRN(String mRN) {
        this.mRN = mRN;
    }

    @JsonProperty("NOTES")
    public String getNOTES() {
        return nOTES;
    }

    @JsonProperty("NOTES")
    public void setNOTES(String nOTES) {
        this.nOTES = nOTES;
    }

    @JsonProperty("PT_SEX")
    public String getPTSEX() {
        return pTSEX;
    }

    @JsonProperty("PT_SEX")
    public void setPTSEX(String pTSEX) {
        this.pTSEX = pTSEX;
    }

    @JsonProperty("PT_VITAL_STATUS")
    public String getPTVITALSTATUS() {
        return pTVITALSTATUS;
    }

    @JsonProperty("PT_VITAL_STATUS")
    public void setPTVITALSTATUS(String pTVITALSTATUS) {
        this.pTVITALSTATUS = pTVITALSTATUS;
    }

    @JsonProperty("P_ID")
    public Integer getPID() {
        return pID;
    }

    @JsonProperty("P_ID")
    public void setPID(Integer pID) {
        this.pID = pID;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
