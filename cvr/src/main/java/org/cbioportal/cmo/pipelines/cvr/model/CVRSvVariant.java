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
import jakarta.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "annotation",
    "breakpoint_type",
    "comments",
    "confidence_class",
    "conn_type",
    "connection_type",
    "event_info",
    "mapq",
    "normal_read_count",
    "normal_variant_count",
    "paired_end_read_support",
    "site1_chrom",
    "site1_desc",
    "site1_exon",
    "site1_gene",
    "site1_pos",
    "site2_chrom",
    "site2_desc",
    "site2_exon",
    "site2_gene",
    "site2_pos",
    "split_read_support",
    "sv_class_name",
    "sv_desc",
    "sv_length",
    "sv_variant_id",
    "tumor_read_count",
    "tumor_variant_count",
    "variant_status_name",
    "gene1",
    "gene2",
    "exon1",
    "exon2",
    "sample_comment"
})
public class CVRSvVariant {
    @JsonProperty("annotation")
    private String annotation;
    @JsonProperty("breakpoint_type")
    private String breakpointType;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("confidence_class")
    private String confidenceClass;
    @JsonProperty("conn_type")
    private String connType;
    @JsonProperty("connection_type")
    private String connectionType;
    @JsonProperty("event_info")
    private String eventInfo;
    @JsonProperty("mapq")
    private String mapq;
    @JsonProperty("normal_read_count")
    private String normalReadCount;
    @JsonProperty("normal_variant_count")
    private String normalVariantCount;
    @JsonProperty("paired_end_read_support")
    private String pairedEndReadSupport;
    @JsonProperty("site1_chrom")
    private String site1Chrom;
    @JsonProperty("site1_desc")
    private String site1Desc;
    @JsonProperty("site1_exon")
    private String site1Exon;
    @JsonProperty("site1_gene")
    private String site1Gene;
    @JsonProperty("site1_pos")
    private String site1Pos;
    @JsonProperty("site2_chrom")
    private String site2Chrom;
    @JsonProperty("site2_desc")
    private String site2Desc;
    @JsonProperty("site2_exon")
    private String site2Exon;
    @JsonProperty("site2_gene")
    private String site2Gene;
    @JsonProperty("site2_pos")
    private String site2Pos;
    @JsonProperty("split_read_support")
    private String splitReadSupport;
    @JsonProperty("sv_class_name")
    private String svClassName;
    @JsonProperty("sv_desc")
    private String svDesc;
    @JsonProperty("sv_length")
    private String svLength;
    @JsonProperty("sv_variant_id")
    private String svVariantId;
    @JsonProperty("tumor_read_count")
    private String tumorReadCount;
    @JsonProperty("variant_status_name")
    private String variantStatusName;
    @JsonProperty("tumor_variant_count")
    private String tumorVariantCount;
    @JsonProperty("gene1")
    private String gene1;
    @JsonProperty("gene2")
    private String gene2;
    @JsonProperty("exon1")
    private String exon1;
    @JsonProperty("exon2")
    private String exon2;
    @JsonProperty("sample_comment")
    private String sampleComment;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRSvVariant() {
    }

    /**
     * @param annotation
     * @param breakpointType
     * @param comments
     * @param confidenceClass
     * @param connType
     * @param connectionType
     * @param eventInfo
     * @param mapq
     * @param normalReadCount
     * @param normalVariantCount
     * @param pairedEndReadSupport
     * @param site1Chrom
     * @param site1Desc
     * @param site1Exon
     * @param site1Gene
     * @param site1Pos
     * @param site2Chrom
     * @param site2Desc
     * @param site2Exon
     * @param site2Gene
     * @param site2Pos
     * @param splitReadSupport
     * @param svClassName
     * @param svDesc
     * @param svLength
     * @param svVariantId
     * @param tumorReadCount
     * @param variantStatusName
     * @param tumorVariantCount
     * @param gene1
     * @param gene2
     * @param exon1
     * @param exon2
     * @param sampleComment
     * @param additionalProperties
     */
    public CVRSvVariant(String annotation, String breakpointType, String comments, String confidenceClass, String connType, String connectionType, String eventInfo, String mapq, String normalReadCount, String normalVariantCount, String pairedEndReadSupport, String site1Chrom, String site1Desc, String site1Exon, String site1Gene, String site1Pos, String site2Chrom, String site2Desc, String site2Exon, String site2Gene, String site2Pos, String splitReadSupport, String svClassName, String svDesc, String svLength, String svVariantId, String tumorReadCount, String variantStatusName, String tumorVariantCount, String gene1, String gene2, String exon1, String exon2, String sampleComment, Map<String, Object> additionalProperties) {
        this.annotation = annotation;
        this.breakpointType = breakpointType;
        this.comments = comments;
        this.confidenceClass = confidenceClass;
        this.connType = connType;
        this.connectionType = connectionType;
        this.eventInfo = eventInfo;
        this.mapq = mapq;
        this.normalReadCount = normalReadCount;
        this.normalVariantCount = normalVariantCount;
        this.pairedEndReadSupport = pairedEndReadSupport;
        this.site1Chrom = site1Chrom;
        this.site1Desc = site1Desc;
        this.site1Exon = site1Exon;
        this.site1Gene = site1Gene;
        this.site1Pos = site1Pos;
        this.site2Chrom = site2Chrom;
        this.site2Desc = site2Desc;
        this.site2Exon = site2Exon;
        this.site2Gene = site2Gene;
        this.site2Pos = site2Pos;
        this.splitReadSupport = splitReadSupport;
        this.svClassName = svClassName;
        this.svDesc = svDesc;
        this.svLength = svLength;
        this.svVariantId = svVariantId;
        this.tumorReadCount = tumorReadCount;
        this.variantStatusName = variantStatusName;
        this.tumorVariantCount = tumorVariantCount;
        this.gene1 = gene1;
        this.gene2 = gene2;
        this.exon1 = exon1;
        this.exon2 = exon2;
        this.sampleComment = sampleComment;
        this.additionalProperties = additionalProperties;
    }

    /**
    *
    * @return
    * The annotation
    */
    @JsonProperty("annotation")
    public String getAnnotation() {
        return annotation;
    }

    /**
    *
    * @param annotation
    * The annotation
    */
    @JsonProperty("annotation")
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    /**
    *
    * @return
    * The breakpointType
    */
    @JsonProperty("breakpoint_type")
    public String getBreakpoint_Type() {
        return breakpointType;
    }

    /**
    *
    * @param breakpointType
    * The breakpoint_type
    */
    @JsonProperty("breakpoint_type")
    public void setBreakpoint_Type(String breakpointType) {
        this.breakpointType = breakpointType;
    }

    /**
    *
    * @return
    * The comments
    */
    @JsonProperty("comments")
    public String getComments() {
        return comments != null ? comments : sampleComment;
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
    public String getConfidence_Class() {
        return confidenceClass;
    }

    /**
    *
    * @param confidenceClass
    * The confidence_class
    */
    @JsonProperty("confidence_class")
    public void setConfidence_Class(String confidenceClass) {
        this.confidenceClass = confidenceClass;
    }

    /**
    *
    * @return
    * The connType
    */
    @JsonProperty("conn_type")
    public String getConn_Type() {
        return connType;
    }

    /**
    *
    * @param connType
    * The conn_type
    */
    @JsonProperty("conn_type")
    public void setConn_Type(String connType) {
        this.connType = connType;
    }

    /**
    *
    * @return
    * The connectionType
    */
    @JsonProperty("connection_type")
    public String getConnection_Type() {
        return connectionType;
    }

    /**
    *
    * @param connectionType
    * The connection_type
    */
    @JsonProperty("connection_type")
    public void setConnection_Type(String connectionType) {
        this.connectionType = connectionType;
    }

    /**
    *
    * @return
    * The eventInfo
    */
    @JsonProperty("event_info")
    public String getEvent_Info() {
        return eventInfo;
    }

    /**
    *
    * @param eventInfo
    * The event_info
    */
    @JsonProperty("event_info")
    public void setEvent_Info(String eventInfo) {
        this.eventInfo = eventInfo;
    }

    /**
    *
    * @return
    * The mapq
    */
    @JsonProperty("mapq")
    public String getMapq() {
        return mapq;
    }

    /**
    *
    * @param mapq
    * The mapq
    */
    @JsonProperty("mapq")
    public void setMapq(String mapq) {
        this.mapq = mapq;
    }

    /**
    *
    * @return
    * The normalReadCount
    */
    @JsonProperty("normal_read_count")
    public String getNormal_Read_Count() {
        return normalReadCount;
    }

    /**
    *
    * @param normalReadCount
    * The normal_read_count
    */
    @JsonProperty("normal_read_count")
    public void setNormal_Read_Count(String normalReadCount) {
        this.normalReadCount = normalReadCount;
    }

    /**
    *
    * @return
    * The normalVariantCount
    */
    @JsonProperty("normal_variant_count")
    public String getNormal_Variant_Count() {
        return normalVariantCount;
    }

    /**
    *
    * @param normalVariantCount
    * The normal_variant_count
    */
    @JsonProperty("normal_variant_count")
    public void setNormal_Variant_Count(String normalVariantCount) {
        this.normalVariantCount = normalVariantCount;
    }

    /**
    *
    * @return
    * The pairedEndReadSupport
    */
    @JsonProperty("paired_end_read_support")
    public String getPaired_End_Read_Support() {
        return pairedEndReadSupport;
    }

    /**
    *
    * @param pairedEndReadSupport
    * The paired_end_read_support
    */
    @JsonProperty("paired_end_read_support")
    public void setPaired_End_Read_Support(String pairedEndReadSupport) {
        this.pairedEndReadSupport = pairedEndReadSupport;
    }

    /**
    *
    * @return
    * The site1Chrom
    */
    @JsonProperty("site1_chrom")
    public String getSite1_Chrom() {
        return site1Chrom;
    }

    /**
    *
    * @param site1Chrom
    * The site1_chrom
    */
    @JsonProperty("site1_chrom")
    public void setSite1_Chrom(String site1Chrom) {
        this.site1Chrom = site1Chrom;
    }

    /**
    *
    * @return
    * The site1Desc
    */
    @JsonProperty("site1_desc")
    public String getSite1_Desc() {
        return site1Desc;
    }

    /**
    *
    * @param site1Desc
    * The site1_desc
    */
    @JsonProperty("site1_desc")
    public void setSite1_Desc(String site1Desc) {
        this.site1Desc = site1Desc;
    }

    /**
     *
     * @return
     * The site1Exon
     */
    @JsonProperty("site1_exon")
    public String getSite1_Exon() {
        return site1Exon;
    }

    /**
     *
     * @param site1Exon
     * The site1_exon
     */
    @JsonProperty("site1_exon")
    public void setSite1_Exon(String site1Exon) {
        this.site1Exon = site1Exon;
    }

    /**
    *
    * @return
    * The site1Gene
    */
    @JsonProperty("site1_gene")
    public String getSite1_Gene() {
        return site1Gene != null ? site1Gene : gene1;
    }

    /**
    *
    * @param site1Gene
    * The site1_gene
    */
    @JsonProperty("site1_gene")
    public void setSite1_Gene(String site1Gene) {
        this.site1Gene = site1Gene;
    }

    /**
    *
    * @return
    * The site1Pos
    */
    @JsonProperty("site1_pos")
    public String getSite1_Pos() {
        return site1Pos;
    }

    /**
    *
    * @param site1Pos
    * The site1_pos
    */
    @JsonProperty("site1_pos")
    public void setSite1_Pos(String site1Pos) {
        this.site1Pos = site1Pos;
    }

    /**
    *
    * @return
    * The site2Chrom
    */
    @JsonProperty("site2_chrom")
    public String getSite2_Chrom() {
        return site2Chrom;
    }

    /**
    *
    * @param site2Chrom
    * The site2_chrom
    */
    @JsonProperty("site2_chrom")
    public void setSite2_Chrom(String site2Chrom) {
        this.site2Chrom = site2Chrom;
    }

    /**
    *
    * @return
    * The site2Desc
    */
    @JsonProperty("site2_desc")
    public String getSite2_Desc() {
        return site2Desc;
    }

    /**
    *
    * @param site2Desc
    * The site2_desc
    */
    @JsonProperty("site2_desc")
    public void setSite2_Desc(String site2Desc) {
        this.site2Desc = site2Desc;
    }

    /**
     *
     * @return
     * The site2Exon
     */
    @JsonProperty("site2_exon")
    public String getSite2_Exon() {
        return site2Exon;
    }

    /**
     *
     * @param site2Exon
     * The site2_exon
     */
    @JsonProperty("site2_exon")
    public void setSite2_Exon(String site2Exon) {
        this.site1Exon = site2Exon;
    }

    /**
    *
    * @return
    * The site2Gene
    */
    @JsonProperty("site2_gene")
    public String getSite2_Gene() {
        return site2Gene != null ? site2Gene : gene2;
    }

    /**
    *
    * @param site2Gene
    * The site2_gene
    */
    @JsonProperty("site2_gene")
    public void setSite2_Gene(String site2Gene) {
        this.site2Gene = site2Gene;
    }

    /**
    *
    * @return
    * The site2Pos
    */
    @JsonProperty("site2_pos")
    public String getSite2_Pos() {
        return site2Pos;
    }

    /**
    *
    * @param site2Pos
    * The site2_pos
    */
    @JsonProperty("site2_pos")
    public void setSite2_Pos(String site2Pos) {
        this.site2Pos = site2Pos;
    }

    /**
    *
    * @return
    * The splitReadSupport
    */
    @JsonProperty("split_read_support")
    public String getSplit_Read_Support() {
        return splitReadSupport;
    }

    /**
    *
    * @param splitReadSupport
    * The split_read_support
    */
    @JsonProperty("split_read_support")
    public void setSplit_Read_Support(String splitReadSupport) {
        this.splitReadSupport = splitReadSupport;
    }

    /**
    *
    * @return
    * The svClassName
    */
    @JsonProperty("sv_class_name")
    public String getSv_Class_Name() {
        return svClassName;
    }

    /**
    *
    * @param svClassName
    * The sv_class_name
    */
    @JsonProperty("sv_class_name")
    public void setSv_Class_Name(String svClassName) {
        this.svClassName = svClassName;
    }

    /**
    *
    * @return
    * The svDesc
    */
    @JsonProperty("sv_desc")
    public String getSv_Desc() {
        return svDesc;
    }

    /**
    *
    * @param svDesc
    * The sv_desc
    */
    @JsonProperty("sv_desc")
    public void setSv_Desc(String svDesc) {
        this.svDesc = svDesc;
    }

    /**
    *
    * @return
    * The svLength
    */
    @JsonProperty("sv_length")
    public String getSv_Length() {
        return svLength;
    }

    /**
    *
    * @param svLength
    * The sv_length
    */
    @JsonProperty("sv_length")
    public void setSv_Length(String svLength) {
        this.svLength = svLength;
    }

    /**
    *
    * @return
    * The svVariantId
    */
    @JsonProperty("sv_variant_id")
    public String getSv_VariantId() {
        return svVariantId;
    }

    /**
    *
    * @param svVariantId
    * The sv_variant_id
    */
    @JsonProperty("sv_variant_id")
    public void setSv_VariantId(String svVariantId) {
        this.svVariantId = svVariantId;
    }

    /**
    *
    * @return
    * The tumorReadCount
    */
    @JsonProperty("tumor_read_count")
    public String getTumor_Read_Count() {
        return tumorReadCount;
    }

    /**
    *
    * @param tumorReadCount
    * The tumor_read_count
    */
    @JsonProperty("tumor_read_count")
    public void setTumor_Read_Count(String tumorReadCount) {
        this.tumorReadCount = tumorReadCount;
    }

    /**
    *
    * @return
    * The tumorVariantCount
    */
    @JsonProperty("tumor_variant_count")
    public String getTumor_Variant_Count() {
        return tumorVariantCount;
    }

    /**
    *
    * @param tumorVariantCount
    * The tumor_variant_count
    */
    @JsonProperty("tumor_variant_count")
    public void setTumor_Variant_Count(String tumorVariantCount) {
        this.tumorVariantCount = tumorVariantCount;
    }

    /**
    *
    * @return
    * The variantStatusName
    */
    @JsonProperty("variant_status_name")
    public String getVariant_Status_Name() {
        return variantStatusName;
    }

    /**
    *
    * @param variantStatusName
    * The variant_status_name
    */
    @JsonProperty("variant_status_name")
    public void setVariant_Status_Name(String variantStatusName) {
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

    /**
     * @return the gene1
     */
    public String getGene1() {
        return gene1;
    }

    /**
     * @param gene1 the gene1 to set
     */
    public void setGene1(String gene1) {
        this.gene1 = gene1;
    }

    /**
     * @return the gene2
     */
    public String getGene2() {
        return gene2;
    }

    /**
     * @param gene2 the gene2 to set
     */
    public void setGene2(String gene2) {
        this.gene2 = gene2;
    }

    /**
     * @return the exon1
     */
    public String getExon1() {
        return exon1;
    }

    /**
     * @param exon1 the exon1 to set
     */
    public void setExon1(String exon1) {
        this.exon1 = exon1;
    }

    /**
     * @return the exon2
     */
    public String getExon2() {
        return exon2;
    }

    /**
     * @param exon2 the exon2 to set
     */
    public void setExon2(String exon2) {
        this.exon2 = exon2;
    }

    /**
     * @return the sampleComment
     */
    public String getSampleComment() {
        return sampleComment;
    }

    /**
     * @param sampleComment the sampleComment to set
     */
    public void setSampleComment(String sampleComment) {
        this.sampleComment = sampleComment;
    }
}
