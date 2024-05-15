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
    "alys2sample_id",
    "dmp_alys_task_name",
    "dmp_patient_id",
    "dmp_sample_id",
    "gender",
    "gene-panel",
    "legacy_patient_id",
    "legacy_sample_id",
    "retrieve_status",
    "sample_coverage",
    "so_comments",
    "so_status_name",
})
public class GMLMetaData {
    @JsonProperty("alys2sample_id")
    private Integer alys2sampleId;
    @JsonProperty("dmp_alys_task_name")
    private String dmpAlysTaskName;
    @JsonProperty("dmp_patient_id")
    private String dmpPatientId;
    @JsonProperty("dmp_sample_id")
    private String dmpSampleId;
    @JsonProperty("gender")
    private Integer gender;
    @JsonProperty("gene-panel")
    private String genePanel;
    @JsonProperty("legacy_patient_id")
    private String legacyPatientId;
    @JsonProperty("legacy_sample_id")
    private String legacySampleId;
    @JsonProperty("retrieve_status")
    private Integer retrieveStatus;
    @JsonProperty("sample_coverage")
    private Integer sampleCoverage;
    @JsonProperty("so_comments")
    private String soComments;
    @JsonProperty("so_status_name")
    private String soStatusName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    public GMLMetaData() {
    }

    public GMLMetaData(Integer alys2sampleId, String dmpAlysTaskName, String dmpPatientId,
            String dmpSampleId, Integer gender, String genePanel,
            String legacyPatientId, String legacySampleId, Integer retrieveStatus,
            Integer sampleCoverage, String soComments, String soStatusName) {
        this.alys2sampleId = alys2sampleId;
        this.dmpAlysTaskName = dmpAlysTaskName;
        this.dmpPatientId = dmpPatientId;
        this.dmpSampleId = dmpSampleId;
        this.gender = gender;
        this.genePanel = genePanel;
        this.legacyPatientId = legacyPatientId;
        this.legacySampleId = legacySampleId;
        this.retrieveStatus = retrieveStatus;
        this.sampleCoverage = sampleCoverage;
        this.soComments = soComments;
        this.soStatusName = soStatusName;
    }

    @JsonProperty("alys2sample_id")
    public Integer getAlys2sampleId() {
        return alys2sampleId;
    }

    @JsonProperty("dmp_alys_task_name")
    public String getDmpAlysTaskName() {
        return dmpAlysTaskName;
    }

    @JsonProperty("dmp_patient_id")
    public String getDmpPatientId() {
        return dmpPatientId;
    }

    @JsonProperty("dmp_sample_id")
    public String getDmpSampleId() {
        return dmpSampleId;
    }

    @JsonProperty("gender")
    public Integer getGender() {
        return gender;
    }

    @JsonProperty("gene-panel")
    public String getGenePanel() {
        return genePanel;
    }

    @JsonProperty("legacy_patient_id")
    public String getLegacyPatientId() {
        return legacyPatientId;
    }

    @JsonProperty("legacy_sample_id")
    public String getLegacySampleId() {
        return legacySampleId;
    }

    @JsonProperty("retrieve_status")
    public Integer getRetrieveStatus() {
        return retrieveStatus;
    }

    @JsonProperty("sample_coverage")
    public Integer getSampleCoverage() {
        return sampleCoverage;
    }

    @JsonProperty("so_comments")
    public String getSoComments() {
        return soComments;
    }

    @JsonProperty("so_status_name")
    public String getSoStatusName() {
        return soStatusName;
    }

    @JsonProperty("alys2sample_id")
    public void setAlys2sampleId(Integer alys2sampleId) {
        this.alys2sampleId = alys2sampleId;
    }

    @JsonProperty("dmp_alys_task_name")
    public void setDmpAlysTaskName(String dmpAlysTaskName) {
        this.dmpAlysTaskName = dmpAlysTaskName;
    }

    @JsonProperty("dmp_patient_id")
    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    @JsonProperty("dmp_sample_id")
    public void setDmpSampleId(String dmpSampleId) {
        this.dmpSampleId = dmpSampleId;
    }

    @JsonProperty("gender")
    public void setGender(Integer gender) {
        this.gender = gender;
    }

    @JsonProperty("gene-panel")
    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
    }

    @JsonProperty("legacy_patient_id")
    public void setLegacyPatientId(String legacyPatientId) {
        this.legacyPatientId = legacyPatientId;
    }

    @JsonProperty("legacy_sample_id")
    public void setLegacySampleId(String legacySampleId) {
        this.legacySampleId = legacySampleId;
    }

    @JsonProperty("retrieve_status")
    public void setRetrieveStatus(Integer retrieveStatus) {
        this.retrieveStatus = retrieveStatus;
    }

    @JsonProperty("sample_coverage")
    public void setSampleCoverage(Integer sampleCoverage) {
        this.sampleCoverage = sampleCoverage;
    }

    @JsonProperty("so_comments")
    public void setSoComments(String soComments) {
        this.soComments = soComments;
    }

    @JsonProperty("so_status_name")
    public void setSoStatusName(String soStatusName) {
        this.soStatusName = soStatusName;
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
