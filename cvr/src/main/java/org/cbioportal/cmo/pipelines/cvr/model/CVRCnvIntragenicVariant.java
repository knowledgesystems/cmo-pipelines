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
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "cluster_1",
    "cluster_2",
    "cnv_variant_id",
    "comments",
    "confidence_cv_id",
    "cytoband",
    "gene_id",
    "refseq_acc",
    "variant_status_cv_id"
})
public class CVRCnvIntragenicVariant {
    @JsonProperty("cluster_1")
    private String cluster1;
    @JsonProperty("cluster_2")
    private String cluster2;
    @JsonProperty("cnv_variant_id")
    private Integer cnvVariantId;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("confidence_cv_id")
    private Integer confidenceCvId;
    @JsonProperty("cytoband")
    private String cytoband;
    @JsonProperty("gene_id")
    private String geneId;
    @JsonProperty("refseq_acc")
    private String refseqAcc;
    @JsonProperty("variant_status_cv_id")
    private Integer variantStatusCvId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRCnvIntragenicVariant() {
    }

    /**
    *
    *@param cluster1
    *@param cluster2
    *@param cnvVariantId
    *@param comments
    *@param confidenceCvId
    *@param cytoband
    *@param geneId
    *@param refseqAcc
    *@param variantStatusCvId
    */
    public CVRCnvIntragenicVariant(String cluster1, String cluster2, Integer cnvVariantId, String comments,
            Integer confidenceCvId, String cytoband, String geneId, String refseqAcc, Integer variantStatusCvId) {
        this.cluster1 = cluster1;
        this.cluster2 = cluster2;
        this.cnvVariantId = cnvVariantId;
        this.comments = comments;
        this.confidenceCvId = confidenceCvId;
        this.cytoband = cytoband;
        this.geneId = geneId;
        this.refseqAcc = refseqAcc;
        this.variantStatusCvId = variantStatusCvId;
    }

    /**
    *
    *@return
    *The cluster1
    */
    @JsonProperty("cluster_1")
    public String getCluster1() {
        return cluster1;
    }

    /**
    *
    *@param cluster1
    *The cluster_1
    */
    @JsonProperty("cluster_1")
    public void setCluster1(String cluster1) {
        this.cluster1 = cluster1;
    }

    /**
    *
    *@return
    *The cluster2
    */
    @JsonProperty("cluster_2")
    public String getCluster2() {
        return cluster2;
    }

    /**
    *
    *@param cluster2
    *The cluster_2
    */
    @JsonProperty("cluster_2")
    public void setCluster2(String cluster2) {
        this.cluster2 = cluster2;
    }

    /**
    *
    *@return
    *The cnvVariantId
    */
    @JsonProperty("cnv_variant_id")
    public Integer getCnvVariantId() {
        return cnvVariantId;
    }

    /**
    *
    *@param cnvVariantId
    *The cnv_variant_id
    */
    @JsonProperty("cnv_variant_id")
    public void setCnvVariantId(Integer cnvVariantId) {
        this.cnvVariantId = cnvVariantId;
    }

    /**
    *
    *@return
    *The comments
    */
    @JsonProperty("comments")
    public String getComments() {
        return comments;
    }

    /**
    *
    *@param comments
    *The comments
    */
    @JsonProperty("comments")
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
    *
    *@return
    *The confidenceCvId
    */
    @JsonProperty("confidence_cv_id")
    public Integer getConfidenceCvId() {
        return confidenceCvId;
    }

    /**
    *
    *@param confidenceCvId
    *The confidence_cv_id
    */
    @JsonProperty("confidence_cv_id")
    public void setConfidenceCvId(Integer confidenceCvId) {
        this.confidenceCvId = confidenceCvId;
    }

    /**
    *
    *@return
    *The cytoband
    */
    @JsonProperty("cytoband")
    public String getCytoband() {
        return cytoband;
    }

    /**
    *
    *@param cytoband
    *The cytoband
    */
    @JsonProperty("cytoband")
    public void setCytoband(String cytoband) {
        this.cytoband = cytoband;
    }

    /**
    *
    *@return
    *The geneId
    */
    @JsonProperty("gene_id")
    public String getGeneId() {
        return geneId;
    }

    /**
    *
    *@param geneId
    *The gene_id
    */
    @JsonProperty("gene_id")
    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    /**
    *
    *@return
    *The refseqAcc
    */
    @JsonProperty("refseq_acc")
    public String getRefseqAcc() {
        return refseqAcc;
    }

    /**
    *
    *@param refseqAcc
    *The refseq_acc
    */
    @JsonProperty("refseq_acc")
    public void setRefseqAcc(String refseqAcc) {
        this.refseqAcc = refseqAcc;
    }

    /**
    *
    *@return
    *The variantStatusCvId
    */
    @JsonProperty("variant_status_cv_id")
    public Integer getVariantStatusCvId() {
        return variantStatusCvId;
    }

    /**
    *
    *@param variantStatusCvId
    *The variant_status_cv_id
    */
    @JsonProperty("variant_status_cv_id")
    public void setVariantStatusCvId(Integer variantStatusCvId) {
        this.variantStatusCvId = variantStatusCvId;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public CVRCnvIntragenicVariant withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }
}
