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
    "COUNT",
    "ORD_NAME",
    "SET_NAME",
    "START_DATE",
    "STOP_DATE"
})
public class Chemotherapy implements Serializable {
    @JsonProperty("COUNT")
    private Integer cOUNT;
    @JsonProperty("ORD_NAME")
    private String oRDNAME;
    @JsonProperty("SET_NAME")
    private String sETNAME;
    @JsonProperty("START_DATE")
    private String sTARTDATE;
    @JsonProperty("STOP_DATE")
    private String sTOPDATE;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("COUNT")
    public Integer getCOUNT() {
        return cOUNT;
    }

    @JsonProperty("COUNT")
    public void setCOUNT(Integer cOUNT) {
        this.cOUNT = cOUNT;
    }

    @JsonProperty("ORD_NAME")
    public String getORDNAME() {
        return oRDNAME;
    }

    @JsonProperty("ORD_NAME")
    public void setORDNAME(String oRDNAME) {
        this.oRDNAME = oRDNAME;
    }

    @JsonProperty("SET_NAME")
    public String getSETNAME() {
        return sETNAME;
    }

    @JsonProperty("SET_NAME")
    public void setSETNAME(String sETNAME) {
        this.sETNAME = sETNAME;
    }

    @JsonProperty("START_DATE")
    public String getSTARTDATE() {
        return sTARTDATE;
    }

    @JsonProperty("START_DATE")
    public void setSTARTDATE(String sTARTDATE) {
        this.sTARTDATE = sTARTDATE;
    }

    @JsonProperty("STOP_DATE")
    public String getSTOPDATE() {
        return sTOPDATE;
    }

    @JsonProperty("STOP_DATE")
    public void setSTOPDATE(String sTOPDATE) {
        this.sTOPDATE = sTOPDATE;
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
