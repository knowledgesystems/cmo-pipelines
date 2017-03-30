/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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
import javax.annotation.Generated;

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
    "site1_gene",
    "site1_pos",
    "site2_chrom",
    "site2_desc",
    "site2_gene",
    "site2_pos",
    "split_read_support",
    "sv_class_name",
    "sv_desc",
    "sv_length",
    "sv_variant_id",
    "tumor_read_count",
    "tumor_variant_count",
    "variant_status_name"
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
    @JsonProperty("site1_gene")
    private String site1Gene;
    @JsonProperty("site1_pos")
    private String site1Pos;
    @JsonProperty("site2_chrom")
    private String site2Chrom;
    @JsonProperty("site2_desc")
    private String site2Desc;
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
    @JsonProperty("tumor_variant_count")
    private String tumorVariantCount;
    @JsonProperty("variant_status_name")
    private String variantStatusName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public CVRSvVariant() {
    }

    /**
    *
    * @param pairedEndReadSupport
    * @param site1Pos
    * @param svLength
    * @param eventInfo
    * @param site2Desc
    * @param svVariantId
    * @param splitReadSupport
    * @param normalReadCount
    * @param svClassName
    * @param breakpointType
    * @param site2Pos
    * @param normalVariantCount
    * @param tumorVariantCount
    * @param tumorReadCount
    * @param confidenceClass
    * @param site1Chrom
    * @param connectionType
    * @param site1Gene
    * @param svDesc
    * @param connType
    * @param mapq
    * @param variantStatusName
    * @param site1Desc
    * @param comments
    * @param site2Chrom
    * @param site2Gene
    * @param annotation
    */
    public CVRSvVariant(String annotation, String breakpointType, String comments, String confidenceClass, String connType, String connectionType, String eventInfo, String mapq, String normalReadCount, String normalVariantCount, String pairedEndReadSupport, String site1Chrom, String site1Desc, String site1Gene, String site1Pos, String site2Chrom, String site2Desc, String site2Gene, String site2Pos, String splitReadSupport, String svClassName, String svDesc, String svLength, String svVariantId, String tumorReadCount, String tumorVariantCount, String variantStatusName) {
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
        this.site1Gene = site1Gene;
        this.site1Pos = site1Pos;
        this.site2Chrom = site2Chrom;
        this.site2Desc = site2Desc;
        this.site2Gene = site2Gene;
        this.site2Pos = site2Pos;
        this.splitReadSupport = splitReadSupport;
        this.svClassName = svClassName;
        this.svDesc = svDesc;
        this.svLength = svLength;
        this.svVariantId = svVariantId;
        this.tumorReadCount = tumorReadCount;
        this.tumorVariantCount = tumorVariantCount;
        this.variantStatusName = variantStatusName;
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
    * The site1Gene
    */
    @JsonProperty("site1_gene")
    public String getSite1_Gene() {
        return site1Gene;
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
    * The site2Gene
    */
    @JsonProperty("site2_gene")
    public String getSite2_Gene() {
        return site2Gene;
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
}
