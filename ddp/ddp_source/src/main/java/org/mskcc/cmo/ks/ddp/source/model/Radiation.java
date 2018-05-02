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
    "Course ID",
    "Delivered Dose",
    "Delivered Fractions",
    "MRN",
    "Plan Name",
    "Plan Name Anatomy",
    "Planned Dose",
    "Planned Fractions",
    "RadOnc Treatment Course Start Date",
    "RadOnc Treatment Course Stop Date",
    "Reference Point/Site"
})
public class Radiation implements Serializable {
    @JsonProperty("Course ID")
    private Integer courseID;
    @JsonProperty("Delivered Dose")
    private String deliveredDose;
    @JsonProperty("Delivered Fractions")
    private Integer deliveredFractions;
    @JsonProperty("MRN")
    private String mRN;
    @JsonProperty("Plan Name")
    private String planName;
    @JsonProperty("Plan Name Anatomy")
    private String planNameAnatomy;
    @JsonProperty("Planned Dose")
    private String plannedDose;
    @JsonProperty("Planned Fractions")
    private Integer plannedFractions;
    @JsonProperty("RadOnc Treatment Course Start Date")
    private String radOncTreatmentCourseStartDate;
    @JsonProperty("RadOnc Treatment Course Stop Date")
    private String radOncTreatmentCourseStopDate;
    @JsonProperty("Reference Point/Site")
    private String referencePointSite;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("Course ID")
    public Integer getCourseID() {
        return courseID;
    }

    @JsonProperty("Course ID")
    public void setCourseID(Integer courseID) {
        this.courseID = courseID;
    }

    @JsonProperty("Delivered Dose")
    public String getDeliveredDose() {
        return deliveredDose;
    }

    @JsonProperty("Delivered Dose")
    public void setDeliveredDose(String deliveredDose) {
        this.deliveredDose = deliveredDose;
    }

    @JsonProperty("Delivered Fractions")
    public Integer getDeliveredFractions() {
        return deliveredFractions;
    }

    @JsonProperty("Delivered Fractions")
    public void setDeliveredFractions(Integer deliveredFractions) {
        this.deliveredFractions = deliveredFractions;
    }

    @JsonProperty("MRN")
    public String getMRN() {
        return mRN;
    }

    @JsonProperty("MRN")
    public void setMRN(String mRN) {
        this.mRN = mRN;
    }

    @JsonProperty("Plan Name")
    public String getPlanName() {
        return planName;
    }

    @JsonProperty("Plan Name")
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    @JsonProperty("Plan Name Anatomy")
    public String getPlanNameAnatomy() {
        return planNameAnatomy;
    }

    @JsonProperty("Plan Name Anatomy")
    public void setPlanNameAnatomy(String planNameAnatomy) {
        this.planNameAnatomy = planNameAnatomy;
    }

    @JsonProperty("Planned Dose")
    public String getPlannedDose() {
        return plannedDose;
    }

    @JsonProperty("Planned Dose")
    public void setPlannedDose(String plannedDose) {
        this.plannedDose = plannedDose;
    }

    @JsonProperty("Planned Fractions")
    public Integer getPlannedFractions() {
        return plannedFractions;
    }

    @JsonProperty("Planned Fractions")
    public void setPlannedFractions(Integer plannedFractions) {
        this.plannedFractions = plannedFractions;
    }

    @JsonProperty("RadOnc Treatment Course Start Date")
    public String getRadOncTreatmentCourseStartDate() {
        return radOncTreatmentCourseStartDate;
    }

    @JsonProperty("RadOnc Treatment Course Start Date")
    public void setRadOncTreatmentCourseStartDate(String radOncTreatmentCourseStartDate) {
        this.radOncTreatmentCourseStartDate = radOncTreatmentCourseStartDate;
    }

    @JsonProperty("RadOnc Treatment Course Stop Date")
    public String getRadOncTreatmentCourseStopDate() {
        return radOncTreatmentCourseStopDate;
    }

    @JsonProperty("RadOnc Treatment Course Stop Date")
    public void setRadOncTreatmentCourseStopDate(String radOncTreatmentCourseStopDate) {
        this.radOncTreatmentCourseStopDate = radOncTreatmentCourseStopDate;
    }

    @JsonProperty("Reference Point/Site")
    public String getReferencePointSite() {
        return referencePointSite;
    }

    @JsonProperty("Reference Point/Site")
    public void setReferencePointSite(String referencePointSite) {
        this.referencePointSite = referencePointSite;
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
