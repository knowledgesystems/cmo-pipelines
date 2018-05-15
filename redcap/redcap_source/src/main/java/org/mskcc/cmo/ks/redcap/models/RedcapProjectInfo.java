/*
 * Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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
    "creation_time",
    "production_time",
    "in_production",
    "project_language",
    "purpose",
    "purpose_other",
    "project_notes",
    "custom_record_label",
    "secondary_unique_field",
    "is_longitudinal",
    "surveys_enabled",
    "scheduling_enabled",
    "record_autonumbering_enabled",
    "randomization_enabled",
    "ddp_enabled",
    "project_irb_number",
    "project_grant_number",
    "project_pi_firstname",
    "project_pi_lastname",
    "display_today_now_button"
})
public class RedcapProjectInfo {
    @JsonProperty("project_id")
    private String projectId;
    @JsonProperty("project_title")
    private String projectTitle;
    @JsonProperty("creation_time")
    private String creationTime;
    @JsonProperty("production_time")
    private String productionTime;
    @JsonProperty("in_production")
    private String inProduction;
    @JsonProperty("project_language")
    private String projectLanguage;
    @JsonProperty("purpose")
    private String purpose;
    @JsonProperty("purpose_other")
    private String purposeOther;
    @JsonProperty("project_notes")
    private String projectNotes;
    @JsonProperty("custom_record_label")
    private String customRecordLabel;
    @JsonProperty("secondary_unique_field")
    private String secondaryUniqueField;
    @JsonProperty("is_longitudinal")
    private String isLongitudinal;
    @JsonProperty("surveys_enabled")
    private String surveysEnabled;
    @JsonProperty("scheduling_enabled")
    private String schedulingEnabled;
    @JsonProperty("record_autonumbering_enabled")
    private String recordAutonumberingEnabled;
    @JsonProperty("randomization_enabled")
    private String randomizationEnabled;
    @JsonProperty("ddp_enabled")
    private String ddpEnabled;
    @JsonProperty("project_irb_number")
    private String projectIrbNumber;
    @JsonProperty("project_grant_number")
    private String projectGrantNumber;
    @JsonProperty("project_pi_firstname")
    private String projectPiFirstname;
    @JsonProperty("project_pi_lastname")
    private String projectPiLastname;
    @JsonProperty("display_today_now_button")
    private String displayTodayNowButton;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public RedcapProjectInfo() {}



    @JsonProperty("project_id")
    public String getProjectId() {
        return this.projectId;
    }

    @JsonProperty("project_id")
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @JsonProperty("project_title")
    public String getProjectTitle() {
        return this.projectTitle;
    }

    @JsonProperty("project_title")
    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    @JsonProperty("creation_time")
    public String getCreationTime() {
        return this.creationTime;
    }

    @JsonProperty("creation_time")
    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    @JsonProperty("production_time")
    public String getProductionTime() {
        return this.productionTime;
    }

    @JsonProperty("production_time")
    public void setProductionTime(String productionTime) {
        this.productionTime = productionTime;
    }

    @JsonProperty("in_production")
    public String getInProduction() {
        return this.inProduction;
    }

    @JsonProperty("in_production")
    public void setInProduction(String inProduction) {
        this.inProduction = inProduction;
    }

    @JsonProperty("project_language")
    public String getProjectLanguage() {
        return this.projectLanguage;
    }

    @JsonProperty("project_language")
    public void setProjectLanguage(String projectLanguage) {
        this.projectLanguage = projectLanguage;
    }

    @JsonProperty("purpose")
    public String getPurpose() {
        return this.purpose;
    }

    @JsonProperty("purpose")
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @JsonProperty("purpose_other")
    public String getPurposeOther() {
        return this.purposeOther;
    }

    @JsonProperty("purpose_other")
    public void setPurposeOther(String purposeOther) {
        this.purposeOther = purposeOther;
    }

    @JsonProperty("project_notes")
    public String getProjectNotes() {
        return this.projectNotes;
    }

    @JsonProperty("project_notes")
    public void setProjectNotes(String projectNotes) {
        this.projectNotes = projectNotes;
    }

    @JsonProperty("custom_record_label")
    public String getCustomRecordLabel() {
        return this.customRecordLabel;
    }

    @JsonProperty("custom_record_label")
    public void setCustomRecordLabel(String customRecordLabel) {
        this.customRecordLabel = customRecordLabel;
    }

    @JsonProperty("secondary_unique_field")
    public String getSecondaryUniqueField() {
        return this.secondaryUniqueField;
    }

    @JsonProperty("secondary_unique_field")
    public void setSecondaryUniqueField(String secondaryUniqueField) {
        this.secondaryUniqueField = secondaryUniqueField;
    }

    @JsonProperty("is_longitudinal")
    public String getIsLongitudinal() {
        return this.isLongitudinal;
    }

    @JsonProperty("is_longitudinal")
    public void setIsLongitudinal(String isLongitudinal) {
        this.isLongitudinal = isLongitudinal;
    }

    @JsonProperty("surveys_enabled")
    public String getSurveysEnabled() {
        return this.surveysEnabled;
    }

    @JsonProperty("surveys_enabled")
    public void setSurveysEnabled(String surveysEnabled) {
        this.surveysEnabled = surveysEnabled;
    }

    @JsonProperty("scheduling_enabled")
    public String getSchedulingEnabled() {
        return this.schedulingEnabled;
    }

    @JsonProperty("scheduling_enabled")
    public void setSchedulingEnabled(String schedulingEnabled) {
        this.schedulingEnabled = schedulingEnabled;
    }

    @JsonProperty("record_autonumbering_enabled")
    public String getRecordAutonumberingEnabled() {
        return this.recordAutonumberingEnabled;
    }

    @JsonProperty("record_autonumbering_enabled")
    public void setRecordAutonumberingEnabled(String recordAutonumberingEnabled) {
        this.recordAutonumberingEnabled = recordAutonumberingEnabled;
    }

    @JsonProperty("randomization_enabled")
    public String getRandomizationEnabled() {
        return this.randomizationEnabled;
    }

    @JsonProperty("randomization_enabled")
    public void setRandomizationEnabled(String randomizationEnabled) {
        this.randomizationEnabled = randomizationEnabled;
    }

    @JsonProperty("ddp_enabled")
    public String getDdpEnabled() {
        return this.ddpEnabled;
    }

    @JsonProperty("ddp_enabled")
    public void setDdpEnabled(String ddpEnabled) {
        this.ddpEnabled = ddpEnabled;
    }

    @JsonProperty("project_irb_number")
    public String getProjectIrbNumber() {
        return this.projectIrbNumber;
    }

    @JsonProperty("project_irb_number")
    public void setProjectIrbNumber(String projectIrbNumber) {
        this.projectIrbNumber = projectIrbNumber;
    }

    @JsonProperty("project_grant_number")
    public String getProjectGrantNumber() {
        return this.projectGrantNumber;
    }

    @JsonProperty("project_grant_number")
    public void setProjectGrantNumber(String projectGrantNumber) {
        this.projectGrantNumber = projectGrantNumber;
    }

    @JsonProperty("project_pi_firstname")
    public String getProjectPiFirstname() {
        return this.projectPiFirstname;
    }

    @JsonProperty("project_pi_firstname")
    public void setProjectPiFirstname(String projectPiFirstname) {
        this.projectPiFirstname = projectPiFirstname;
    }

    @JsonProperty("project_pi_lastname")
    public String getProjectPiLastname() {
        return this.projectPiLastname;
    }

    @JsonProperty("project_pi_lastname")
    public void setProjectPiLastname(String projectPiLastname) {
        this.projectPiLastname = projectPiLastname;
    }

    @JsonProperty("display_today_now_button")
    public String getDisplayTodayNowButton() {
        return this.displayTodayNowButton;
    }

    @JsonProperty("display_today_now_button")
    public void setDisplayTodayNowButton(String displayTodayNowButton) {
        this.displayTodayNowButton = displayTodayNowButton;
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
