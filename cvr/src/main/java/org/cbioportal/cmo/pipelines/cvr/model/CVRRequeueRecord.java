/*
 * Copyright (c) 2017, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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
 * @author Manda Wilson
 */

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "disclaimer",
    "information",
    "result",
    "sample_id"
})
public class CVRRequeueRecord {

    @JsonProperty("disclaimer")
    private String disclaimer;

    @JsonProperty("information")
    private String information;

    @JsonProperty("result")
    private Integer result;

    @JsonProperty("sample_id")
    private String sampleId;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRRequeueRecord() {
    }

    /**
    *
    * @param disclaimer
    * @param information
    * @param result
    * @param sampleId
    */
    public CVRRequeueRecord(String disclaimer, String information, Integer result, String sampleId) {
        this.disclaimer = disclaimer;
        this.information = information;
        this.result = result;
        this.sampleId = sampleId;
    }

    /**
    *
    * @return disclaimer
    */
    @JsonProperty("disclaimer")
    public String getDisclaimer() {
        return disclaimer;
    }

    /**
    *
    * @param disclaimer
    */
    @JsonProperty("disclaimer")
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    /**
    *
    * @return information
    */
    @JsonProperty("information")
    public String getInformation() {
        return information;
    }

    /**
    *
    * @param information
    */
    @JsonProperty("information")
    public void setInformation(String information) {
        this.information = information;
    }

    /**
    *
    * @return result
    */
    @JsonProperty("result")
    public Integer getResult() {
        return result;
    }

    /**
    *
    * @param result
    */
    @JsonProperty("result")
    public void setResult(Integer result) {
        this.result = result;
    }

    /**
    *
    * @return sample id
    */
    @JsonProperty("sample_id")
    public String getSampleId() {
        return sampleId;
    }

    /**
    *
    * @param sampleId
    */
    @JsonProperty("sample_id")
    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CVRRequeueRecord withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
