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
import java.util.List;
import java.util.Map;
import jakarta.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "disclaimer",
    "signedout-cases",
    "total"
})
public class CVRMasterList {

    @JsonProperty("disclaimer")
    private String disclaimer;

    @JsonProperty("signedout-cases")
    private List<Map<String, String>> samples;

    @JsonProperty("total")
    private Integer sampleCount;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRMasterList() {
    }

    /**
    *
    * @param disclaimer
    * @param samples
    * @param sampleCount
    */
    public CVRMasterList(String disclaimer, List samples, Integer sampleCount) {
        this.disclaimer = disclaimer;
        this.samples = samples;
        this.sampleCount = sampleCount;
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
    * @return samples
    */
    @JsonProperty("signedout-cases")
    public List<Map<String, String>> getSamples() {
        return samples;
    }

    /**
    *
    * @param samples
    */
    @JsonProperty("signedout-cases")
    public void setSamples(List<Map<String, String>> samples) {
        this.samples = samples;
    }

    /**
    *
    * @return sample count
    */
    @JsonProperty("total")
    public Integer getSampleCount() {
        return sampleCount;
    }

    /**
    *
    * @param sampleCount
    */
    @JsonProperty("total")
    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CVRMasterList withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
