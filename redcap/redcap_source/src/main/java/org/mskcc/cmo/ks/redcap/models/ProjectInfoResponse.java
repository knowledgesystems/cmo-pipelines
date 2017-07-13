/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.redcap.models;

/* ALL FIELDS IN RETURNED JSON:
"project_id":"430"
"project_title":"test1"
"creation_time":"2017-06-28 18:18:47"
"production_time":"2017-06-28 18:19:48"
"in_production":"1"
"project_language":"English"
"purpose":"0"
"purpose_other":""
"project_notes":"ewrqwer"
"custom_record_label":""
"secondary_unique_field":""
"is_longitudinal":0
"surveys_enabled":"0"
"scheduling_enabled":"0"
"record_autonumbering_enabled":"1"
"randomization_enabled":"0"
"ddp_enabled":"0"
"project_irb_number":""
"project_grant_number":""
"project_pi_firstname":""
"project_pi_lastname":""
"display_today_now_button":"1"}
*/

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "project_id",
    "project_title",
    "in_production"
})
public class ProjectInfoResponse {
    @JsonProperty("project_id")
    private String projectId;
    @JsonProperty("project_title")
    private String projectTitle;
    @JsonProperty("in_production")
    private String inProduction;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public ProjectInfoResponse() {}

    public ProjectInfoResponse(String projectId, String projectTitle, String inProduction) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.inProduction = inProduction;
    }

    @JsonProperty("project_id")
    public String getProjectId() {
        return projectId;
    }

    @JsonProperty("project_id")
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @JsonProperty("project_title")
    public String getProjectTitle() {
        return projectTitle;
    }

    @JsonProperty("project_title")
    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    @JsonProperty("in_production")
    public String getInProduction() {
        return inProduction;
    }

    @JsonProperty("in_production")
    public void setInProduction(String inProduction) {
        this.inProduction = projectTitle;
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
