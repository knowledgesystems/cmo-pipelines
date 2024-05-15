/*
 * Copyright (c) 2016, 2017, 2024 Memorial Sloan Kettering Cancer Center.
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
 * @author heinsz
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
    "session_id",
    "time_created",
    "time_expired",
    "disclaimer"
})
public class CVRSession {
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("time_created")
    private String timeCreated;
    @JsonProperty("time_expired")
    private String timeExpired;
    @JsonProperty("disclaimer")
    private String disclaimer;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRSession() {
    }

    /**
    *
    * @param sessionId
    * @param timeCreated
    * @param timeExpired
    * @param disclaimer
    */
    public CVRSession(String sessionId, String timeCreated, String timeExpired, String disclaimer) {
        this.sessionId = sessionId;
        this.timeCreated = timeCreated;
        this.timeExpired = timeExpired;
        this.disclaimer = disclaimer;
    }

    /**
    *
    * @return
    * The sessionId
    */
    @JsonProperty("session_id")
    public String getSessionId() {
        return sessionId;
    }

    /**
    *
    * @param sessionId
    * The sample_id
    */
    @JsonProperty("session_id")
    public void setSampleId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
    *
    * @return
    * The timeCreated
    */
    @JsonProperty("time_created")
    public String getTimeCreated() {
        return timeCreated;
    }

    /**
    *
    * @param timeCreated
    * The time_created
    */
    @JsonProperty("time_created")
    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    /**
    *
    * @return
    * The timeExpired
    */
    @JsonProperty("time_expired")
    public String getTimeExpired() {
        return timeExpired;
    }

    /**
    *
    * @param timeExpired
    * The time_expired
    */
    @JsonProperty("time_expired")
    public void setTimeExpired(String timeExpired) {
        this.timeExpired = timeExpired;
    }

    /**
    *
    * @return
    * The disclaimer
    */
    @JsonProperty("disclaimer")
    public String getDisclaimer() {
        return timeExpired;
    }

    /**
    *
    * @param disclaimer
    * The disclaimer
    */
    @JsonProperty("disclaimer")
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CVRSession withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }
}
