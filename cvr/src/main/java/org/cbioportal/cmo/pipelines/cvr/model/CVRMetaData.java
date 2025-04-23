/*
 * Copyright (c) 2016 - 2017, 2024, 2025 Memorial Sloan Kettering Cancer Center.
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import java.util.*;
import jakarta.annotation.Generated;

import org.cbioportal.cmo.pipelines.cvr.CVRExtractionDeserializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonIgnoreProperties({"mrev_comments"})
@JsonPropertyOrder({
    "alys2sample_id",
    "cbx_patient_id",
    "cbx_sample_id",
    "ct",
    "date_tumor_sequencing",
    "dmp_alys_task_id",
    "dmp_alys_task_name",
    "dmp_patient_id",
    "dmp_sample_id",
    "dmp_sample_so_id",
    "gender",
    "gene-panel",
    "is_metastasis",
    "linked_mskimpact_case",
    "legacy_patient_id",
    "legacy_sample_id",
    "metastasis_site",
    "msi-comment",
    "msi-score",
    "msi-type",
    "outside_institute",
    "primary_site",
    "retrieve_status",
    "sample_coverage",
    "so_comments",
    "so_status_name",
    "somatic_status",
    "tmb_cohort_percentile",
    "tmb_score",
    "tmb_tt_percentile",
    "tumor_purity",
    "tumor_type_code",
    "tumor_type_name",
    "consent-parta",
    "consent-partc",
    "slide-viewer-id",
    "extraction"
})
public class CVRMetaData {

    @JsonProperty("alys2sample_id")
    private Integer alys2sampleId;
    @JsonProperty("cbx_patient_id")
    private Integer cbxPatientId;
    @JsonProperty("cbx_sample_id")
    private Integer cbxSampleId;
    @JsonProperty("ct")
    private Integer cycleThreshold;
    @JsonProperty("date_tumor_sequencing")
    private String dateTumorSequencing;
    @JsonProperty("linked_mskimpact_case")
    private String linkedMskimpactCase;
    @JsonProperty("dmp_alys_task_id")
    private Integer dmpAlysTaskId;
    @JsonProperty("dmp_alys_task_name")
    private String dmpAlysTaskName;
    @JsonProperty("dmp_patient_id")
    private String dmpPatientId;
    @JsonProperty("dmp_sample_id")
    private String dmpSampleId;
    @JsonProperty("dmp_sample_so_id")
    private Integer dmpSampleSoId;
    @JsonProperty("gender")
    private Integer gender;
    @JsonProperty("gene-panel")
    private String genePanel;
    @JsonProperty("is_metastasis")
    private Integer isMetastasis;
    @JsonProperty("legacy_patient_id")
    private String legacyPatientId;
    @JsonProperty("legacy_sample_id")
    private String legacySampleId;
    @JsonProperty("metastasis_site")
    private String metastasisSite;
    @JsonProperty("msi-comment")
    private String msiComment;
    @JsonProperty("msi-score")
    private String msiScore;
    @JsonProperty("msi-type")
    private String msiType;
    @JsonProperty("outside_institute")
    private String outsideInstitute;
    @JsonProperty("primary_site")
    private String primarySite;
    @JsonProperty("retrieve_status")
    private Integer retrieveStatus;
    @JsonProperty("sample_coverage")
    private Integer sampleCoverage;
    @JsonProperty("so_comments")
    private String soComments;
    @JsonProperty("so_status_name")
    private String soStatusName;
    @JsonProperty("somatic_status")
    private String somaticStatus;
    @JsonProperty("tmb_cohort_percentile")
    private Double tmbCohortPercentile;
    @JsonProperty("tmb_score")
    private Double tmbScore;
    @JsonProperty("tmb_tt_percentile")
    private Double tmbTtPercentile;
    @JsonProperty("tumor_purity")
    private String tumorPurity;
    @JsonProperty("tumor_type_code")
    private String tumorTypeCode;
    @JsonProperty("tumor_type_name")
    private String tumorTypeName;
    @JsonProperty("consent-parta")
    private String consentPartA;
    @JsonProperty("consent-partc")
    private String consentPartC;
    @JsonProperty("slide-viewer-id")
    private String wholeSlideViewerId;
    @JsonDeserialize(using = CVRExtractionDeserializer.class)
    @JsonProperty("extraction")
    private CVRExtraction extraction;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRMetaData() {
    }

    /**
     *
     * @param alys2sampleId
     * @param cbxPatientId
     * @param cbxSampleId
     * @param ct
     * @param dateTumorSequencing
     * @param linkedMskimpactCase
     * @param dmpAlysTaskId
     * @param dmpAlysTaskName
     * @param dmpPatientId
     * @param dmpSampleId
     * @param dmpSampleSoId
     * @param gender
     * @param genePanel
     * @param isMetastasis
     * @param legacyPatientId
     * @param legacySampleId
     * @param metastasisSite
     * @param msiComment
     * @param msiScore
     * @param msiType
     * @param outsideInstitute
     * @param primarySite
     * @param retrieveStatus
     * @param sampleCoverage
     * @param soComments
     * @param soStatusName
     * @param somaticStatus
     * @param tmbCohortPercentile
     * @param tmbScore
     * @param tmbTtPercentile
     * @param tumorPurity
     * @param tumorTypeCode
     * @param tumorTypeName
     * @param consentPartA
     * @param consentPartC
     * @param wholeSlideViewerId
     * @param extraction
     */
    public CVRMetaData(Integer alys2sampleId, Integer cbxPatientId, Integer cbxSampleId, Integer ct, String dateTumorSequencing, String linkedMskimpactCase,
            Integer dmpAlysTaskId, String dmpAlysTaskName, String dmpPatientId, String dmpSampleId, Integer dmpSampleSoId, Integer gender,
            String genePanel, Integer isMetastasis, String legacyPatientId, String legacySampleId, String metastasisSite,
            String msiComment, String msiScore, String msiType, String outsideInstitute, String primarySite, Integer retrieveStatus,
            Integer sampleCoverage, String soComments, String soStatusName, String somaticStatus,Double tmbCohortPercentile,
            Double tmbScore, Double tmbTtPercentile, String tumorPurity, String tumorTypeCode, String tumorTypeName,
            String consentPartA, String consentPartC, String wholeSlideViewerId, CVRExtraction extraction) {
        this.alys2sampleId = alys2sampleId;
        this.cbxPatientId = cbxPatientId;
        this.cbxSampleId = cbxSampleId;
        this.cycleThreshold = ct;
        this.dateTumorSequencing = dateTumorSequencing;
        this.dmpAlysTaskId = dmpAlysTaskId;
        this.dmpAlysTaskName = dmpAlysTaskName;
        this.dmpPatientId = dmpPatientId;
        this.dmpSampleId = dmpSampleId;
        this.dmpSampleSoId = dmpSampleSoId;
        this.gender = gender;
        this.genePanel = genePanel;
        this.isMetastasis = isMetastasis;
        this.legacyPatientId = legacyPatientId;
        this.legacySampleId = legacySampleId;
        this.metastasisSite = metastasisSite;
        this.msiComment = msiComment;
        this.msiScore = msiScore;
        this.msiType = msiType;
        this.outsideInstitute = outsideInstitute;
        this.primarySite = primarySite;
        this.retrieveStatus = retrieveStatus;
        this.sampleCoverage = sampleCoverage;
        this.soComments = soComments;
        this.soStatusName = soStatusName;
        this.somaticStatus = somaticStatus;
        this.tmbCohortPercentile = tmbCohortPercentile;
        this.tmbScore = tmbScore;
        this.tmbTtPercentile = tmbTtPercentile;
        this.tumorPurity = tumorPurity;
        this.tumorTypeCode = tumorTypeCode;
        this.tumorTypeName = tumorTypeName;
        this.linkedMskimpactCase = linkedMskimpactCase;
        this.consentPartA = consentPartA;
        this.consentPartC = consentPartC;
        this.wholeSlideViewerId = wholeSlideViewerId;
        this.extraction = extraction;
    }

    @JsonProperty("somatic_status")
    public String getSomaticStatus() {
        return somaticStatus;
    }

    @JsonProperty("somatic_status")
    public void setSomatic_status(String somaticStatus) {
        this.somaticStatus = somaticStatus;
    }

    /**
    *
    * @return
    * The alys2sampleId
    */
    @JsonProperty("alys2sample_id")
    public Integer getAlys2sampleId() {
        return alys2sampleId;
    }

    /**
    *
    * @param alys2sampleId
    * The alys2sample_id
    */
    @JsonProperty("alys2sample_id")
    public void setAlys2sampleId(Integer alys2sampleId) {
        this.alys2sampleId = alys2sampleId;
    }

    /**
    *
    * @return
    * The cbxPatientId
    */
    @JsonProperty("cbx_patient_id")
    public Integer getCbxPatientId() {
        return cbxPatientId;
    }

    /**
    *
    * @param cbxPatientId
    * The cbx_patient_id
    */
    @JsonProperty("cbx_patient_id")
    public void setCbxPatientId(Integer cbxPatientId) {
        this.cbxPatientId = cbxPatientId;
    }

    /**
    *
    * @return
    * The cbxSampleId
    */
    @JsonProperty("cbx_sample_id")
    public Integer getCbxSampleId() {
        return cbxSampleId;
    }

    /**
    *
    * @param cbxSampleId
    * The cbx_sample_id
    */
    @JsonProperty("cbx_sample_id")
    public void setCbxSampleId(Integer cbxSampleId) {
        this.cbxSampleId = cbxSampleId;
    }

    /**
     *
     * @return
     * The cycleThreshold
     */
    @JsonProperty("ct")
    public Integer getCycleThreshold() {
        return cycleThreshold;
    }

    /**
     *
     * @param cycleThreshold
     * The ct
     */
    @JsonProperty("ct")
    public void setCycleThreshold(Integer cycleThreshold) {
        this.cycleThreshold = cycleThreshold;
    }

    /**
    *
    * @return
    * The dateTumorSequencing
    */
    @JsonProperty("date_tumor_sequencing")
    public String getDateTumorSequencing() {
        return dateTumorSequencing;
    }

    /**
    *
    * @param dateTumorSequencing
    * The date_tumor_sequencing
    */
    @JsonProperty("date_tumor_sequencing")
    public void setDateTumorSequencing(String dateTumorSequencing) {
        this.dateTumorSequencing = dateTumorSequencing;
    }

    /**
    *
    * @return
    * The dmpAlysTaskId
    */
    @JsonProperty("dmp_alys_task_id")
    public Integer getDmpAlysTaskId() {
        return dmpAlysTaskId;
    }

    /**
    *
    * @return
    * The correspondingImpactCase
    */
    @JsonProperty("linked_mskimpact_case")
    public String getLinkedMskimpactCase() {
        return linkedMskimpactCase;
    }

    /**
    *
    * @param linkedMskimpactCase
    * The linked_mskimpact_case
    */
    @JsonProperty("linked_mskimpact_case")
    public void setLinkedMskimpactCase(String linkedMskimpactCase) {
        this.linkedMskimpactCase = linkedMskimpactCase;
    }

    /**
    *
    * @param dmpAlysTaskId
    * The dmp_alys_task_id
    */
    @JsonProperty("dmp_alys_task_id")
    public void setDmpAlysTaskId(Integer dmpAlysTaskId) {
        this.dmpAlysTaskId = dmpAlysTaskId;
    }

    /**
    *
    * @return
    * The dmpAlysTaskName
    */
    @JsonProperty("dmp_alys_task_name")
    public String getDmpAlysTaskName() {
        return dmpAlysTaskName;
    }

    /**
    *
    * @param dmpAlysTaskName
    * The dmp_alys_task_name
    */
    @JsonProperty("dmp_alys_task_name")
    public void setDmpAlysTaskName(String dmpAlysTaskName) {
        this.dmpAlysTaskName = dmpAlysTaskName;
    }

    /**
    *
    * @return
    * The dmpPatientId
    */
    @JsonProperty("dmp_patient_id")
    public String getDmpPatientId() {
        return dmpPatientId;
    }

    /**
    *
    * @param dmpPatientId
    * The dmp_patient_id
    */
    @JsonProperty("dmp_patient_id")
    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    /**
    *
    * @return
    * The dmpSampleId
    */
    @JsonProperty("dmp_sample_id")
    public String getDmpSampleId() {
        return dmpSampleId;
    }

    /**
    *
    * @param dmpSampleId
    * The dmp_sample_id
    */
    @JsonProperty("dmp_sample_id")
    public void setDmpSampleId(String dmpSampleId) {
        this.dmpSampleId = dmpSampleId;
    }

    /**
    *
    * @return
    * The dmpSampleSoId
    */
    @JsonProperty("dmp_sample_so_id")
    public Integer getDmpSampleSoId() {
        return dmpSampleSoId;
    }

    /**
    *
    * @param dmpSampleSoId
    * The dmp_sample_so_id
    */
    @JsonProperty("dmp_sample_so_id")
    public void setDmpSampleSoId(Integer dmpSampleSoId) {
        this.dmpSampleSoId = dmpSampleSoId;
    }

    /**
    *
    * @return
    * The gender
    */
    @JsonProperty("gender")
    public Integer getGender() {
        return gender;
    }

    /**
    *
    * @param gender
    * The gender
    */
    @JsonProperty("gender")
    public void setGender(Integer gender) {
        this.gender = gender;
    }

    /**
    *
    * @return
    * The genePanel
    */
    @JsonProperty("gene-panel")
    public String getGenePanel() {
        return genePanel;
    }

    /**
    *
    * @param genePanel
    * The gene-panel
    */
    @JsonProperty("gene-panel")
    public void setGenePanel(String genePanel) {
        this.genePanel = genePanel;
    }

    /**
    *
    * @return
    * The isMetastasis
    */
    @JsonProperty("is_metastasis")
    public Integer getIsMetastasis() {
        return isMetastasis;
    }

    /**
    *
    * @param isMetastasis
    * The is_metastasis
    */
    @JsonProperty("is_metastasis")
    public void setIsMetastasis(Integer isMetastasis) {
        this.isMetastasis = isMetastasis;
    }

    /**
    *
    * @return
    * The legacyPatientId
    */
    @JsonProperty("legacy_patient_id")
    public String getLegacyPatientId() {
        return legacyPatientId;
    }

    /**
    *
    * @param legacyPatientId
    * The legacy_patient_id
    */
    @JsonProperty("legacy_patient_id")
    public void setLegacyPatientId(String legacyPatientId) {
        this.legacyPatientId = legacyPatientId;
    }

    /**
    *
    * @return
    * The legacySampleId
    */
    @JsonProperty("legacy_sample_id")
    public String getLegacySampleId() {
        return legacySampleId;
    }

    /**
    *
    * @param legacySampleId
    * The legacy_sample_id
    */
    @JsonProperty("legacy_sample_id")
    public void setLegacySampleId(String legacySampleId) {
        this.legacySampleId = legacySampleId;
    }

    /**
    *
    * @return
    * The metastasisSite
    */
    @JsonProperty("metastasis_site")
    public String getMetastasisSite() {
        return metastasisSite;
    }

    /**
    *
    * @param metastasisSite
    * The metastasis_site
    */
    @JsonProperty("metastasis_site")
    public void setMetastasisSite(String metastasisSite) {
        this.metastasisSite = metastasisSite;
    }

    /**
    *
    * @return
    * The msiComment
    */
    @JsonProperty("msi-comment")
    public String getMsiComment() {
        return msiComment;
    }

    /**
    *
    * @param msiComment
    * The msi-comment
    */
    @JsonProperty("msi-comment")
    public void setMsiComment(String msiComment) {
        this.msiComment = msiComment;
    }

    /**
    *
    * @return
    * The msiScore
    */
    @JsonProperty("msi-score")
    public String getMsiScore() {
        return msiScore;
    }

    /**
    *
    * @param msiScore
    * The msi-score
    */
    @JsonProperty("msi-score")
    public void setMsiScore(String msiScore) {
        this.msiScore = msiScore;
    }

    /**
    *
    * @return
    * The msiType
    */
    @JsonProperty("msi-type")
    public String getMsiType() {
        return msiType;
    }

    /**
    *
    * @param msiType
    * The msi-type
    */
    @JsonProperty("msi-type")
    public void setMsiType(String msiType) {
        this.msiType = msiType;
    }

    /**
    *
    * @return
    * The outsideInstitute
    */
    @JsonProperty("outside_institute")
    public String getOutsideInstitute() {
        return outsideInstitute;
    }

    /**
    *
    * @param outsideInstitute
    * The outside-institute
    */
    @JsonProperty("outside_institute")
    public void setOutsideInstitute(String outsideInstitute) {
        this.outsideInstitute = outsideInstitute;
    }

    /**
    *
    * @return
    * The primarySite
    */
    @JsonProperty("primary_site")
    public String getPrimarySite() {
        return primarySite;
    }

    /**
    *
    * @param primarySite
    * The primary_site
    */
    @JsonProperty("primary_site")
    public void setPrimarySite(String primarySite) {
        this.primarySite = primarySite;
    }

    /**
    *
    * @return
    * The retrieveStatus
    */
    @JsonProperty("retrieve_status")
    public Integer getRetrieveStatus() {
        return retrieveStatus;
    }

    /**
    *
    * @param retrieveStatus
    * The retrieve_status
    */
    @JsonProperty("retrieve_status")
    public void setRetrieveStatus(Integer retrieveStatus) {
        this.retrieveStatus = retrieveStatus;
    }

    /**
    *
    * @return
    * The sampleCoverage
    */
    @JsonProperty("sample_coverage")
    public Integer getSampleCoverage() {
        return sampleCoverage;
    }

    /**
    *
    * @param sampleCoverage
    * The sample_coverage
    */
    @JsonProperty("sample_coverage")
    public void setSampleCoverage(Integer sampleCoverage) {
        this.sampleCoverage = sampleCoverage;
    }

    /**
    *
    * @return
    * The soComments
    */
    @JsonProperty("so_comments")
    public String getSoComments() {
        return soComments;
    }

    /**
    *
    * @param soComments
    * The so_comments
    */
    @JsonProperty("so_comments")
    public void setSoComments(String soComments) {
        this.soComments = soComments;
    }

    /**
    *
    * @return
    * The soStatusName
    */
    @JsonProperty("so_status_name")
    public String getSoStatusName() {
        return soStatusName;
    }

    /**
    *
    * @param soStatusName
    * The so_status_name
    */
    @JsonProperty("so_status_name")
    public void setSoStatusName(String soStatusName) {
        this.soStatusName = soStatusName;
    }

    /**
     *
     * @return
     * The tmbCohortPercentile
     */
    @JsonProperty("tmb_cohort_percentile")
    public Double getTmbCohortPercentile() {
        return tmbCohortPercentile;
    }

    /**
     *
     * @param tmbCohortPercentile
     * The tmb_cohort_percentile
     */
    @JsonProperty("tmb_cohort_percentile")
    public void setTmbCohortPercentile(Double tmbCohortPercentile) {
        this.tmbCohortPercentile = tmbCohortPercentile;
    }

    /**
     *
     * @return
     * The tmbScore
     */
    @JsonProperty("tmb_score")
    public Double getTmbScore() {
        return tmbScore;
    }

    /**
     *
     * @param tmbScore
     * The tmb_score
     */
    @JsonProperty("tmb_score")
    public void setTmbScore(Double tmbScore) {
        this.tmbScore = tmbScore;
    }

    /**
     *
     * @return
     * The tmbTtPercentile
     */
    @JsonProperty("tmb_tt_percentile")
    public Double getTmbTtPercentile() {
        return tmbTtPercentile;
    }

    /**
     *
     * @param tmbTtPercentile
     * The tmb_tt_percentile
     */
    @JsonProperty("tmb_tt_percentile")
    public void setTmbTtPercentile(Double tmbTtPercentile) {
        this.tmbTtPercentile = tmbTtPercentile;
    }

    /**
    *
    * @return
    * The tumorPurity
    */
    @JsonProperty("tumor_purity")
    public String getTumorPurity() {
        return tumorPurity;
    }

    /**
    *
    * @param tumorPurity
    * The tumor_purity
    */
    @JsonProperty("tumor_purity")
    public void setTumorPurity(String tumorPurity) {
        this.tumorPurity = tumorPurity;
    }

    /**
    *
    * @return
    * The tumorTypeCode
    */
    @JsonProperty("tumor_type_code")
    public String getTumorTypeCode() {
        return tumorTypeCode;
    }

    /**
    *
    * @param tumorTypeCode
    * The tumor_type_code
    */
    @JsonProperty("tumor_type_code")
    public void setTumorTypeCode(String tumorTypeCode) {
        this.tumorTypeCode = tumorTypeCode;
    }

    /**
    *
    * @return
    * The tumorTypeName
    */
    @JsonProperty("tumor_type_name")
    public String getTumorTypeName() {
        return tumorTypeName;
    }

    /**
    *
    * @param tumorTypeName
    * The tumor_type_name
    */
    @JsonProperty("tumor_type_name")
    public void setTumorTypeName(String tumorTypeName) {
        this.tumorTypeName = tumorTypeName;
    }

    /**
     *
     * @return
     * The consent-parta
     */
    @JsonProperty("consent-parta")
    public String getConsentPartA() {
        if (!Strings.isNullOrEmpty(consentPartA)) {
            return (consentPartA.equalsIgnoreCase("true") || consentPartA.equalsIgnoreCase("yes")) ? "YES" : "NO";
        }
        return "";
    }

    /**
     *
     * @param consentPartA
     * The consent-parta
     */
    @JsonProperty("consent-parta")
    public void setConsentPartA(String consentPartA) {
        this.consentPartA = consentPartA;
    }

    /**
     *
     * @return
     * The consent-partc
     */
    @JsonProperty("consent-partc")
    public String getConsentPartC() {
        if (!Strings.isNullOrEmpty(consentPartC)) {
            return (consentPartC.equalsIgnoreCase("true") || consentPartC.equalsIgnoreCase("yes")) ? "YES" : "NO";
        }
        return "";
    }

    /**
     *
     * @param consentPartC
     * The consent-partc
     */
    @JsonProperty("consent-partc")
    public void setConsentPartC(String consentPartC) {
        this.consentPartC = consentPartC;
    }

    /**
     *
     * @return
     * The slide-viewer-id
     */
    @JsonProperty("slide-viewer-id")
    public String getWholeSlideViewerId() {
        return wholeSlideViewerId;
    }

    /**
     *
     * @param wholeSlideViewerId
     * The slide-viewer-id
     */
    @JsonProperty("slide-viewer-id")
    public void setWholeSlideViewerId(String wholeSlideViewerId) {
        this.wholeSlideViewerId = wholeSlideViewerId;
    }

    /**
     *
     * @return
     * The extraction
     */
    @JsonProperty("extraction")
    public CVRExtraction getExtraction() {
        return extraction;
    }

    /**
     *
     * @param extraction
     * The extraction
     */
    @JsonProperty("extraction")
    public void setExtraction(CVRExtraction extraction) {
        this.extraction = extraction;
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
