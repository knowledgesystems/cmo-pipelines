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
    "chromosome",
    "clinical-signed-out",
    "cnv_class_cv_id",
    "cnv_class_name",
    "cnv_filter_cv_id",
    "cnv_filter_name",
    "cnv_variant_id",
    "comments",
    "confidence_class",
    "confidence_cv_id",
    "cytoband",
    "gene_fold_change",
    "gene_id",
    "gene_p_value",
    "is_significant",
    "variant_status_cv_id",
    "variant_status_name"
})
public class CVRCnvVariant {

    @JsonProperty("chromosome")
    private String chromosome;
    @JsonProperty("clinical-signed-out")
    private String clinicalSignedOut;
    @JsonProperty("cnv_class_cv_id")
    private Integer cnvClassCvId;
    @JsonProperty("cnv_class_name")
    private String cnvClassName;
    @JsonProperty("cnv_filter_cv_id")
    private Integer cnvFilterCvId;
    @JsonProperty("cnv_filter_name")
    private String cnvFilterName;
    @JsonProperty("cnv_variant_id")
    private Integer cnvVariantId;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("confidence_class")
    private String confidenceClass;
    @JsonProperty("confidence_cv_id")
    private Integer confidenceCvId;
    @JsonProperty("cytoband")
    private String cytoband;
    @JsonProperty("gene_fold_change")
    private Double geneFoldChange;
    @JsonProperty("gene_id")
    private String geneId;
    @JsonProperty("gene_p_value")
    private Object genePValue;
    @JsonProperty("is_significant")
    private Integer isSignificant;
    @JsonProperty("variant_status_cv_id")
    private Integer variantStatusCvId;
    @JsonProperty("variant_status_name")
    private String variantStatusName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRCnvVariant() {
    }

    /**
    *
    * @param confidenceCvId
    * @param clinicalSignedOut
    * @param cnvClassCvId
    * @param confidenceClass
    * @param cnvVariantId
    * @param cnvFilterCvId
    * @param cnvFilterName
    * @param chromosome
    * @param variantStatusCvId
    * @param geneFoldChange
    * @param isSignificant
    * @param genePValue
    * @param geneId
    * @param cnvClassName
    * @param variantStatusName
    * @param cytoband
    * @param comments
    */
    public CVRCnvVariant(String chromosome, String clinicalSignedOut, Integer cnvClassCvId, String cnvClassName, Integer cnvFilterCvId, String cnvFilterName, Integer cnvVariantId, String comments, String confidenceClass, Integer confidenceCvId, String cytoband, Double geneFoldChange, String geneId, Object genePValue, Integer isSignificant, Integer variantStatusCvId, String variantStatusName) {
        this.chromosome = chromosome;
        this.clinicalSignedOut = clinicalSignedOut;
        this.cnvClassCvId = cnvClassCvId;
        this.cnvClassName = cnvClassName;
        this.cnvFilterCvId = cnvFilterCvId;
        this.cnvFilterName = cnvFilterName;
        this.cnvVariantId = cnvVariantId;
        this.comments = comments;
        this.confidenceClass = confidenceClass;
        this.confidenceCvId = confidenceCvId;
        this.cytoband = cytoband;
        this.geneFoldChange = geneFoldChange;
        this.geneId = geneId;
        this.genePValue = genePValue;
        this.isSignificant = isSignificant;
        this.variantStatusCvId = variantStatusCvId;
        this.variantStatusName = variantStatusName;
    }

    /**
    *
    * @return
    * The chromosome
    */
    @JsonProperty("chromosome")
    public String getChromosome() {
        return chromosome;
    }

    /**
    *
    * @param chromosome
    * The chromosome
    */
    @JsonProperty("chromosome")
    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    /**
    *
    * @return
    * The clinicalSignedOut
    */
    @JsonProperty("clinical-signed-out")
    public String getClinicalSignedOut() {
        return clinicalSignedOut;
    }

    /**
    *
    * @param clinicalSignedOut
    * The clinical-signed-out
    */
    @JsonProperty("clinical-signed-out")
    public void setClinicalSignedOut(String clinicalSignedOut) {
        this.clinicalSignedOut = clinicalSignedOut;
    }

    /**
    *
    * @return
    * The cnvClassCvId
    */
    @JsonProperty("cnv_class_cv_id")
    public Integer getCnvClassCvId() {
        return cnvClassCvId;
    }

    /**
    *
    * @param cnvClassCvId
    * The cnv_class_cv_id
    */
    @JsonProperty("cnv_class_cv_id")
    public void setCnvClassCvId(Integer cnvClassCvId) {
        this.cnvClassCvId = cnvClassCvId;
    }

    /**
    *
    * @return
    * The cnvClassName
    */
    @JsonProperty("cnv_class_name")
    public String getCnvClassName() {
        return cnvClassName;
    }

    /**
    *
    * @param cnvClassName
    * The cnv_class_name
    */
    @JsonProperty("cnv_class_name")
    public void setCnvClassName(String cnvClassName) {
        this.cnvClassName = cnvClassName;
    }

    /**
    *
    * @return
    * The cnvFilterCvId
    */
    @JsonProperty("cnv_filter_cv_id")
    public Integer getCnvFilterCvId() {
        return cnvFilterCvId;
    }

    /**
    *
    * @param cnvFilterCvId
    * The cnv_filter_cv_id
    */
    @JsonProperty("cnv_filter_cv_id")
    public void setCnvFilterCvId(Integer cnvFilterCvId) {
        this.cnvFilterCvId = cnvFilterCvId;
    }

    /**
    *
    * @return
    * The cnvFilterName
    */
    @JsonProperty("cnv_filter_name")
    public String getCnvFilterName() {
        return cnvFilterName;
    }

    /**
    *
    * @param cnvFilterName
    * The cnv_filter_name
    */
    @JsonProperty("cnv_filter_name")
    public void setCnvFilterName(String cnvFilterName) {
        this.cnvFilterName = cnvFilterName;
    }

    /**
    *
    * @return
    * The cnvVariantId
    */
    @JsonProperty("cnv_variant_id")
    public Integer getCnvVariantId() {
        return cnvVariantId;
    }

    /**
    *
    * @param cnvVariantId
    * The cnv_variant_id
    */
    @JsonProperty("cnv_variant_id")
    public void setCnvVariantId(Integer cnvVariantId) {
        this.cnvVariantId = cnvVariantId;
    }

    /**
    *
    * @return
    * The comments
    */
    @JsonProperty("comments")
    public String getComments() {
        return comments;
    }

    /**
    *
    * @param comments
    * The comments
    */
    @JsonProperty("comments")
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
    *
    * @return
    * The confidenceClass
    */
    @JsonProperty("confidence_class")
    public String getConfidenceClass() {
        return confidenceClass;
    }

    /**
    *
    * @param confidenceClass
    * The confidence_class
    */
    @JsonProperty("confidence_class")
    public void setConfidenceClass(String confidenceClass) {
        this.confidenceClass = confidenceClass;
    }

    /**
    *
    * @return
    * The confidenceCvId
    */
    @JsonProperty("confidence_cv_id")
    public Integer getConfidenceCvId() {
        return confidenceCvId;
    }

    /**
    *
    * @param confidenceCvId
    * The confidence_cv_id
    */
    @JsonProperty("confidence_cv_id")
    public void setConfidenceCvId(Integer confidenceCvId) {
        this.confidenceCvId = confidenceCvId;
    }

    /**
    *
    * @return
    * The cytoband
    */
    @JsonProperty("cytoband")
    public String getCytoband() {
        return cytoband;
    }

    /**
    *
    * @param cytoband
    * The cytoband
    */
    @JsonProperty("cytoband")
    public void setCytoband(String cytoband) {
        this.cytoband = cytoband;
    }

    /**
    *
    * @return
    * The geneFoldChange
    */
    @JsonProperty("gene_fold_change")
    public Double getGeneFoldChange() {
        return geneFoldChange;
    }

    /**
    *
    * @param geneFoldChange
    * The gene_fold_change
    */
    @JsonProperty("gene_fold_change")
    public void setGeneFoldChange(Double geneFoldChange) {
        this.geneFoldChange = geneFoldChange;
    }

    /**
    *
    * @return
    * The geneId
    */
    @JsonProperty("gene_id")
    public String getGeneId() {
        return geneId;
    }

    /**
    *
    * @param geneId
    * The gene_id
    */
    @JsonProperty("gene_id")
    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    /**
    *
    * @return
    * The genePValue
    */
    @JsonProperty("gene_p_value")
    public Object getGenePValue() {
        return genePValue;
    }

    /**
    *
    * @param genePValue
    * The gene_p_value
    */
    @JsonProperty("gene_p_value")
    public void setGenePValue(Object genePValue) {
        this.genePValue = genePValue;
    }

    /**
    *
    * @return
    * The isSignificant
    */
    @JsonProperty("is_significant")
    public Integer getIsSignificant() {
        return isSignificant;
    }

    /**
    *
    * @param isSignificant
    * The is_significant
    */
    @JsonProperty("is_significant")
    public void setIsSignificant(Integer isSignificant) {
        this.isSignificant = isSignificant;
    }

    /**
    *
    * @return
    * The variantStatusCvId
    */
    @JsonProperty("variant_status_cv_id")
    public Integer getVariantStatusCvId() {
        return variantStatusCvId;
    }

    /**
    *
    * @param variantStatusCvId
    * The variant_status_cv_id
    */
    @JsonProperty("variant_status_cv_id")
    public void setVariantStatusCvId(Integer variantStatusCvId) {
        this.variantStatusCvId = variantStatusCvId;
    }

    /**
    *
    * @return
    * The variantStatusName
    */
    @JsonProperty("variant_status_name")
    public String getVariantStatusName() {
        return variantStatusName;
    }

    /**
    *
    * @param variantStatusName
    * The variant_status_name
    */
    @JsonProperty("variant_status_name")
    public void setVariantStatusName(String variantStatusName) {
        this.variantStatusName = variantStatusName;
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
