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

package org.mskcc.cmo.ks.redcap.models;

import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "study_id",
    "api_token",
    "stable_id",
    "my_first_instrument_complete"
})
public class RedcapToken {

    @JsonProperty("study_id")
    private String studyId;
    @JsonProperty("api_token")
    private String apiToken;
    @JsonProperty("stable_id")
    private String stableId;
    @JsonProperty("my_first_instrument_complete")
    private String myFirstInstrumentComplete;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public RedcapToken() {
    }

    /**
    *
    * @param studyId
    * @param apiToken
    * @param stableId
    * @param myFirstInstrumentComplete
    */
    public RedcapToken(String studyId, String apiToken, String stableId, String myFirstInstrumentComplete) {
        this.studyId = studyId;
        this.apiToken = apiToken;
        this.stableId = stableId;
        this.myFirstInstrumentComplete = myFirstInstrumentComplete;
    }

    /**
    *
    * @return
    * The studyId
    */
    @JsonProperty("study_id")
    public String getStudyId() {
        return studyId;
    }

    /**
    *
    * @param studyId
    * The study_id
    */
    @JsonProperty("study_id")
    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    /**
    *
    * @return
    * The apiToken
    */
    @JsonProperty("api_token")
    public String getApiToken() {
        return apiToken;
    }

    /**
    *
    * @param apiToken
    * The api_token
    */
    @JsonProperty("api_token")
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    /**
    *
    * @return
    * The stableId
    */
    @JsonProperty("stable_id")
    public String getStableId() {
        return stableId;
    }

    /**
    *
    * @param stableId
    * The stable_id
    */
    @JsonProperty("stable_id")
    public void setStableId(String stableId) {
        this.stableId = stableId;
    }

    /**
    *
    * @return
    * The myFirstInstrumentComplete
    */
    @JsonProperty("my_first_instrument_complete")
    public String getMyFirstInstrumentComplete() {
        return myFirstInstrumentComplete;
    }

    /**
    *
    * @param myFirstInstrumentComplete
    * The my_first_instrument_complete
    */
    @JsonProperty("my_first_instrument_complete")
    public void setMyFirstInstrumentComplete(String myFirstInstrumentComplete) {
        this.myFirstInstrumentComplete = myFirstInstrumentComplete;
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
