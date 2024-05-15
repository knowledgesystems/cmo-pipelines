/*
 * Copyright (c) 2016 - 2017, 2024 Memorial Sloan Kettering Cancer Center.
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
 * @author jake
 */

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.*;
import jakarta.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "sample-count",
    "disclaimer",
    "results"
})

public class GMLData {
    @JsonProperty("sample-count")
    private Integer sampleCount;
    @JsonProperty("disclaimer")
    private String disclaimer;
    @JsonProperty("results")
    private List<GMLResult> results;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public GMLData() {
    }

    public GMLData(Integer sampleCount, String disclaimer, List<GMLResult> results) {
        this.sampleCount = sampleCount;
        this.disclaimer = disclaimer;
        this.results = results;
    }

    @JsonIgnore
    public void addResult(GMLResult result) {
        this.results.add(result);
    }

    /**
     *
     * @return The sampleCount
     */
    @JsonProperty("sample-count")
    public Integer getSampleCount() {
        return sampleCount;
    }

    /**
     *
     * @param sampleCount The sample-count
     */
    @JsonProperty("sample-count")
    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
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
     * @return The results
     */
    @JsonProperty("results")
    public List<GMLResult> getResults() {
        return results;
    }

    /**
     *
     * @param results The results
     */
    @JsonProperty("results")
    public void setResults(List<GMLResult> results) {
        this.results = results;
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
