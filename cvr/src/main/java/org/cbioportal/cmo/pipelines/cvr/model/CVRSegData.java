/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.model;

/**
 *
 * @author heinsz
 */

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.util.HashMap;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "alys2sample_id",
    "disclaimer",
    "dmp_sample_id",
    "seg-data"
})
public class CVRSegData {

    @JsonProperty("alys2sample_id")
    private String alys2sampleId;
    @JsonProperty("disclaimer")
    private String disclaimer;
    @JsonProperty("dmp_sample_id")
    private String dmpSampleId;
    @JsonProperty("seg-data")
    private List<List<String>> segData;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public CVRSegData() {
    }

    /**
     *
     * @param segData
     * @param disclaimer
     * @param dmpSampleId
     * @param alys2sampleId
     */
    public CVRSegData(String alys2sampleId, String disclaimer, String dmpSampleId, List<List<String>> segData) {
        this.alys2sampleId = alys2sampleId;
        this.disclaimer = disclaimer;
        this.dmpSampleId = dmpSampleId;
        this.segData = segData;
    }

    /**
     *
     * @return The alys2sampleId
     */
    @JsonProperty("alys2sample_id")
    public String getAlys2sampleId() {
        return alys2sampleId;
    }

    /**
     *
     * @param alys2sampleId The alys2sample_id
     */
    @JsonProperty("alys2sample_id")
    public void setAlys2sampleId(String alys2sampleId) {
        this.alys2sampleId = alys2sampleId;
    }

    /**
     *
     * @return The disclaimer
     */
    @JsonProperty("disclaimer")
    public String getDisclaimer() {
        return disclaimer;
    }

    /**
     *
     * @param disclaimer The disclaimer
     */
    @JsonProperty("disclaimer")
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    /**
     *
     * @return The dmpSampleId
     */
    @JsonProperty("dmp_sample_id")
    public String getDmpSampleId() {
        return dmpSampleId;
    }

    /**
     *
     * @param dmpSampleId The dmp_sample_id
     */
    @JsonProperty("dmp_sample_id")
    public void setDmpSampleId(String dmpSampleId) {
        this.dmpSampleId = dmpSampleId;
    }

    /**
     *
     * @return The segData
     */
    @JsonProperty("seg-data")
    public List<List<String>> getSegData() {
        return segData;
    }

    /**
     *
     * @param segData The seg-data
     */
    @JsonProperty("seg-data")
    public void setSegData(List<List<String>> segData) {
        this.segData = segData;
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
