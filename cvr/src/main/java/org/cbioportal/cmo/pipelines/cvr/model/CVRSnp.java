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
 * @author jake
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
    "aa_change",
    "alt_allele",
    "cDNA_change",
    "chromosome",
    "clinical-signed-out",
    "comments",
    "confidence_class",
    "confidence_cv_id",
    "cosmic_id",
    "dbSNP_id",
    "dmp_sample_mrev_id",
    "dmp_sample_so_id",
    "dmp_variant_id",
    "exon_num",
    "gene_id",
    "is_hotspot",
    "mafreq_1000g",
    "mrev_comments",
    "mrev_status_cv_id",
    "mrev_status_name",
    "normal_ad",
    "normal_dp",
    "normal_vfreq",
    "occurance_in_normal",
    "occurance_in_pop",
    "ref_allele",
    "snp_indel_tool_name",
    "snp_indel_variant_id",
    "so_comments",
    "so_status_cv_id",
    "so_status_name",
    "start_position",
    "transcript_id",
    "tumor_ad",
    "tumor_dp",
    "tumor_vfreq",
    "s_tumor_ad",
    "s_tumor_dp",
    "s_tumor_vfreq",
    "variant_class",
    "variant_class_cv_id",
    "variant_status_cv_id",
    "variant_status_name"
})
public class CVRSnp {
    @JsonProperty("aa_change")
    private String aaChange;
    @JsonProperty("alt_allele")
    private String altAllele;
    @JsonProperty("cDNA_change")
    private String cDNAChange;
    @JsonProperty("chromosome")
    private String chromosome;
    @JsonProperty("clinical-signed-out")
    private String clinicalSignedOut;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("confidence_class")
    private String confidenceClass;
    @JsonProperty("confidence_cv_id")
    private Integer confidenceCvId;
    @JsonProperty("cosmic_id")
    private String cosmicId;
    @JsonProperty("dbSNP_id")
    private String dbSNPId;
    @JsonProperty("dmp_sample_mrev_id")
    private Integer dmpSampleMrevId;
    @JsonProperty("dmp_sample_so_id")
    private Integer dmpSampleSoId;
    @JsonProperty("dmp_variant_id")
    private Integer dmpVariantId;
    @JsonProperty("exon_num")
    private String exonNum;
    @JsonProperty("gene_id")
    private String geneId;
    @JsonProperty("is_hotspot")
    private Integer isHotspot;
    @JsonProperty("mafreq_1000g")
    private String mafreq1000g;
    @JsonProperty("mrev_comments")
    private String mrevComments;
    @JsonProperty("mrev_status_cv_id")
    private Integer mrevStatusCvId;
    @JsonProperty("mrev_status_name")
    private String mrevStatusName;
    @JsonProperty("normal_ad")
    private Integer normalAd;
    @JsonProperty("normal_dp")
    private Integer normalDp;
    @JsonProperty("normal_vfreq")
    private Object normalVfreq;
    @JsonProperty("occurance_in_normal")
    private String occuranceInNormal;
    @JsonProperty("occurance_in_pop")
    private String occuranceInPop;
    @JsonProperty("ref_allele")
    private String refAllele;
    @JsonProperty("snp_indel_tool_name")
    private String snpIndelToolName;
    @JsonProperty("snp_indel_variant_id")
    private String snpIndelVariantId;
    @JsonProperty("so_comments")
    private String soComments;
    @JsonProperty("so_status_cv_id")
    private Integer soStatusCvId;
    @JsonProperty("so_status_name")
    private String soStatusName;
    @JsonProperty("start_position")
    private Integer startPosition;
    @JsonProperty("transcript_id")
    private String transcriptId;
    @JsonProperty("tumor_ad")
    private Integer tumorAd;
    @JsonProperty("tumor_dp")
    private Integer tumorDp;
    @JsonProperty("tumor_vfreq")
    private Object tumorVfreq;
    @JsonProperty("s_tumor_ad")
    private Integer sTumorAd;
    @JsonProperty("s_tumor_dp")
    private Integer sTumorDp;
    @JsonProperty("s_tumor_vfreq")
    private Object sTumorVfreq;
    @JsonProperty("variant_class")
    private String variantClass;
    @JsonProperty("variant_class_cv_id")
    private Integer variantClassCvId;
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
    public CVRSnp() {
    }

    /**
     *
     * @param aaChange
     * @param altAllele
     * @param cDNAChange
     * @param chromosome
     * @param clinicalSignedOut
     * @param comments
     * @param confidenceClass
     * @param confidenceCvId
     * @param cosmicId
     * @param dbSNPId
     * @param dmpSampleMrevId
     * @param dmpSampleSoId
     * @param dmpVariantId
     * @param exonNum
     * @param geneId
     * @param isHotspot
     * @param mafreq1000g
     * @param mrevComments
     * @param mrevStatusCvId
     * @param mrevStatusName
     * @param normalAd
     * @param normalDp
     * @param normalVfreq
     * @param occuranceInNormal
     * @param occuranceInPop
     * @param refAllele
     * @param snpIndelToolName
     * @param snpIndelVariantId
     * @param soComments
     * @param soStatusCvId
     * @param soStatusName
     * @param startPosition
     * @param transcriptId
     * @param tumorAd
     * @param tumorDp
     * @param tumorVfreq
     * @param sTumorAd
     * @param sTumorDp
     * @param sTumorVfreq
     * @param variantClass
     * @param variantClassCvId
     * @param variantStatusCvId
     * @param variantStatusName
     */
    public CVRSnp(String aaChange, String altAllele, String cDNAChange, String chromosome, String clinicalSignedOut, String comments, String confidenceClass, Integer confidenceCvId, String cosmicId, String dbSNPId, Integer dmpSampleMrevId, Integer dmpSampleSoId, Integer dmpVariantId, String exonNum, String geneId, Integer isHotspot, String mafreq1000g, String mrevComments, Integer mrevStatusCvId, String mrevStatusName, Integer normalAd, Integer normalDp, Object normalVfreq, String occuranceInNormal, String occuranceInPop, String refAllele, String snpIndelToolName, String snpIndelVariantId, String soComments, Integer soStatusCvId, String soStatusName, Integer startPosition, String transcriptId, Integer tumorAd, Integer tumorDp, Object tumorVfreq, Integer sTumorAd, Integer sTumorDp, Object sTumorVfreq, String variantClass, Integer variantClassCvId, Integer variantStatusCvId, String variantStatusName) {
        this.aaChange = aaChange;
        this.altAllele = altAllele;
        this.cDNAChange = cDNAChange;
        this.chromosome = chromosome;
        this.clinicalSignedOut = clinicalSignedOut;
        this.comments = comments;
        this.confidenceClass = confidenceClass;
        this.confidenceCvId = confidenceCvId;
        this.cosmicId = cosmicId;
        this.dbSNPId = dbSNPId;
        this.dmpSampleMrevId = dmpSampleMrevId;
        this.dmpSampleSoId = dmpSampleSoId;
        this.dmpVariantId = dmpVariantId;
        this.exonNum = exonNum;
        this.geneId = geneId;
        this.isHotspot = isHotspot;
        this.mafreq1000g = mafreq1000g;
        this.mrevComments = mrevComments;
        this.mrevStatusCvId = mrevStatusCvId;
        this.mrevStatusName = mrevStatusName;
        this.normalAd = normalAd;
        this.normalDp = normalDp;
        this.normalVfreq = normalVfreq;
        this.occuranceInNormal = occuranceInNormal;
        this.occuranceInPop = occuranceInPop;
        this.refAllele = refAllele;
        this.snpIndelToolName = snpIndelToolName;
        this.snpIndelVariantId = snpIndelVariantId;
        this.soComments = soComments;
        this.soStatusCvId = soStatusCvId;
        this.soStatusName = soStatusName;
        this.startPosition = startPosition;
        this.transcriptId = transcriptId;
        this.tumorAd = tumorAd;
        this.tumorDp = tumorDp;
        this.tumorVfreq = tumorVfreq;
        this.sTumorAd = sTumorAd;
        this.sTumorDp = sTumorDp;
        this.sTumorVfreq = sTumorVfreq;
        this.variantClass = variantClass;
        this.variantClassCvId = variantClassCvId;
        this.variantStatusCvId = variantStatusCvId;
        this.variantStatusName = variantStatusName;
    }

    /**
     *
     * @return The aaChange
     */
    @JsonProperty("aa_change")
    public String getAaChange() {
        return aaChange;
    }

    /**
     *
     * @param aaChange The aa_change
     */
    @JsonProperty("aa_change")
    public void setAaChange(String aaChange) {
        this.aaChange = aaChange;
    }

    /**
     *
     * @return The altAllele
     */
    @JsonProperty("alt_allele")
    public String getAltAllele() {
        return altAllele;
    }

    /**
     *
     * @param altAllele The alt_allele
     */
    @JsonProperty("alt_allele")
    public void setAltAllele(String altAllele) {
        this.altAllele = altAllele;
    }

    /**
     *
     * @return The cDNAChange
     */
    @JsonProperty("cDNA_change")
    public String getCDNAChange() {
        return cDNAChange;
    }

    /**
     *
     * @param cDNAChange The cDNA_change
     */
    @JsonProperty("cDNA_change")
    public void setCDNAChange(String cDNAChange) {
        this.cDNAChange = cDNAChange;
    }

    /**
     *
     * @return The chromosome
     */
    @JsonProperty("chromosome")
    public String getChromosome() {
        return chromosome;
    }

    /**
     *
     * @param chromosome The chromosome
     */
    @JsonProperty("chromosome")
    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    /**
     *
     * @return The clinicalSignedOut
     */
    @JsonProperty("clinical-signed-out")
    public String getClinicalSignedOut() {
        return clinicalSignedOut;
    }

    /**
     *
     * @param clinicalSignedOut The clinical-signed-out
     */
    @JsonProperty("clinical-signed-out")
    public void setClinicalSignedOut(String clinicalSignedOut) {
        this.clinicalSignedOut = clinicalSignedOut;
    }

    /**
     *
     * @return The comments
     */
    @JsonProperty("comments")
    public String getComments() {
        return comments;
    }

    /**
     *
     * @param comments The comments
     */
    @JsonProperty("comments")
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     *
     * @return The confidenceClass
     */
    @JsonProperty("confidence_class")
    public String getConfidenceClass() {
        return confidenceClass;
    }

    /**
     *
     * @param confidenceClass The confidence_class
     */
    @JsonProperty("confidence_class")
    public void setConfidenceClass(String confidenceClass) {
        this.confidenceClass = confidenceClass;
    }

    /**
     *
     * @return The confidenceCvId
     */
    @JsonProperty("confidence_cv_id")
    public Integer getConfidenceCvId() {
        return confidenceCvId;
    }

    /**
     *
     * @param confidenceCvId The confidence_cv_id
     */
    @JsonProperty("confidence_cv_id")
    public void setConfidenceCvId(Integer confidenceCvId) {
        this.confidenceCvId = confidenceCvId;
    }

    /**
     *
     * @return The cosmicId
     */
    @JsonProperty("cosmic_id")
    public String getCosmicId() {
        return cosmicId;
    }

    /**
     *
     * @param cosmicId The cosmic_id
     */
    @JsonProperty("cosmic_id")
    public void setCosmicId(String cosmicId) {
        this.cosmicId = cosmicId;
    }

    /**
     *
     * @return The dbSNPId
     */
    @JsonProperty("dbSNP_id")
    public String getDbSNPId() {
        return dbSNPId;
    }

    /**
     *
     * @param dbSNPId The dbSNP_id
     */
    @JsonProperty("dbSNP_id")
    public void setDbSNPId(String dbSNPId) {
        this.dbSNPId = dbSNPId;
    }

    /**
     *
     * @return The dmpSampleMrevId
     */
    @JsonProperty("dmp_sample_mrev_id")
    public Integer getDmpSampleMrevId() {
        return dmpSampleMrevId;
    }

    /**
     *
     * @param dmpSampleMrevId The dmp_sample_mrev_id
     */
    @JsonProperty("dmp_sample_mrev_id")
    public void setDmpSampleMrevId(Integer dmpSampleMrevId) {
        this.dmpSampleMrevId = dmpSampleMrevId;
    }

    /**
     *
     * @return The dmpSampleSoId
     */
    @JsonProperty("dmp_sample_so_id")
    public Integer getDmpSampleSoId() {
        return dmpSampleSoId;
    }

    /**
     *
     * @param dmpSampleSoId The dmp_sample_so_id
     */
    @JsonProperty("dmp_sample_so_id")
    public void setDmpSampleSoId(Integer dmpSampleSoId) {
        this.dmpSampleSoId = dmpSampleSoId;
    }

    /**
     *
     * @return The dmpVariantId
     */
    @JsonProperty("dmp_variant_id")
    public Integer getDmpVariantId() {
        return dmpVariantId;
    }

    /**
     *
     * @param dmpVariantId The dmp_variant_id
     */
    @JsonProperty("dmp_variant_id")
    public void setDmpVariantId(Integer dmpVariantId) {
        this.dmpVariantId = dmpVariantId;
    }

    /**
     *
     * @return The exonNum
     */
    @JsonProperty("exon_num")
    public String getExonNum() {
        return exonNum;
    }

    /**
     *
     * @param exonNum The exon_num
     */
    @JsonProperty("exon_num")
    public void setExonNum(String exonNum) {
        this.exonNum = exonNum;
    }

    /**
     *
     * @return The geneId
     */
    @JsonProperty("gene_id")
    public String getGeneId() {
        return geneId;
    }

    /**
     *
     * @param geneId The gene_id
     */
    @JsonProperty("gene_id")
    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    /**
     *
     * @return The isHotspot
     */
    @JsonProperty("is_hotspot")
    public Integer getIsHotspot() {
        return isHotspot;
    }

    /**
     *
     * @param isHotspot The is_hotspot
     */
    @JsonProperty("is_hotspot")
    public void setIsHotspot(Integer isHotspot) {
        this.isHotspot = isHotspot;
    }

    /**
     *
     * @return The mafreq1000g
     */
    @JsonProperty("mafreq_1000g")
    public String getMafreq1000g() {
        return mafreq1000g;
    }

    /**
     *
     * @param mafreq1000g The mafreq_1000g
     */
    @JsonProperty("mafreq_1000g")
    public void setMafreq1000g(String mafreq1000g) {
        this.mafreq1000g = mafreq1000g;
    }

    /**
     *
     * @return The mrevComments
     */
    @JsonProperty("mrev_comments")
    public String getMrevComments() {
        return mrevComments;
    }

    /**
     *
     * @param mrevComments The mrev_comments
     */
    @JsonProperty("mrev_comments")
    public void setMrevComments(String mrevComments) {
        this.mrevComments = mrevComments;
    }

    /**
     *
     * @return The mrevStatusCvId
     */
    @JsonProperty("mrev_status_cv_id")
    public Integer getMrevStatusCvId() {
        return mrevStatusCvId;
    }

    /**
     *
     * @param mrevStatusCvId The mrev_status_cv_id
     */
    @JsonProperty("mrev_status_cv_id")
    public void setMrevStatusCvId(Integer mrevStatusCvId) {
        this.mrevStatusCvId = mrevStatusCvId;
    }

    /**
     *
     * @return The mrevStatusName
     */
    @JsonProperty("mrev_status_name")
    public String getMrevStatusName() {
        return mrevStatusName;
    }

    /**
     *
     * @param mrevStatusName The mrev_status_name
     */
    @JsonProperty("mrev_status_name")
    public void setMrevStatusName(String mrevStatusName) {
        this.mrevStatusName = mrevStatusName;
    }

    /**
     *
     * @return The normalAd
     */
    @JsonProperty("normal_ad")
    public Integer getNormalAd() {
        return normalAd;
    }

    /**
     *
     * @param normalAd The normal_ad
     */
    @JsonProperty("normal_ad")
    public void setNormalAd(Integer normalAd) {
        this.normalAd = normalAd;
    }

    /**
     *
     * @return The normalDp
     */
    @JsonProperty("normal_dp")
    public Integer getNormalDp() {
        return normalDp;
    }

    /**
     *
     * @param normalDp The normal_dp
     */
    @JsonProperty("normal_dp")
    public void setNormalDp(Integer normalDp) {
        this.normalDp = normalDp;
    }

    /**
     *
     * @return The normalVfreq
     */
    @JsonProperty("normal_vfreq")
    public Object getNormalVfreq() {
        return normalVfreq;
    }

    /**
     *
     * @param normalVfreq The normal_vfreq
     */
    @JsonProperty("normal_vfreq")
    public void setNormalVfreq(Object normalVfreq) {
        this.normalVfreq = normalVfreq;
    }

    /**
     *
     * @return The occuranceInNormal
     */
    @JsonProperty("occurance_in_normal")
    public String getOccuranceInNormal() {
        return occuranceInNormal;
    }

    /**
     *
     * @param occuranceInNormal The occurance_in_normal
     */
    @JsonProperty("occurance_in_normal")
    public void setOccuranceInNormal(String occuranceInNormal) {
        this.occuranceInNormal = occuranceInNormal;
    }

    /**
     *
     * @return The occuranceInPop
     */
    @JsonProperty("occurance_in_pop")
    public String getOccuranceInPop() {
        return occuranceInPop;
    }

    /**
     *
     * @param occuranceInPop The occurance_in_pop
     */
    @JsonProperty("occurance_in_pop")
    public void setOccuranceInPop(String occuranceInPop) {
        this.occuranceInPop = occuranceInPop;
    }

    /**
     *
     * @return The refAllele
     */
    @JsonProperty("ref_allele")
    public String getRefAllele() {
        return refAllele;
    }

    /**
     *
     * @param refAllele The ref_allele
     */
    @JsonProperty("ref_allele")
    public void setRefAllele(String refAllele) {
        this.refAllele = refAllele;
    }

    /**
     *
     * @return The snpIndelToolName
     */
    @JsonProperty("snp_indel_tool_name")
    public String getSnpIndelToolName() {
        return snpIndelToolName;
    }

    /**
     *
     * @param snpIndelToolName The snp_indel_tool_name
     */
    @JsonProperty("snp_indel_tool_name")
    public void setSnpIndelToolName(String snpIndelToolName) {
        this.snpIndelToolName = snpIndelToolName;
    }

    /**
     *
     * @return The snpIndelVariantId
     */
    @JsonProperty("snp_indel_variant_id")
    public String getSnpIndelVariantId() {
        return snpIndelVariantId;
    }

    /**
     *
     * @param snpIndelVariantId The snp_indel_variant_id
     */
    @JsonProperty("snp_indel_variant_id")
    public void setSnpIndelVariantId(String snpIndelVariantId) {
        this.snpIndelVariantId = snpIndelVariantId;
    }

    /**
     *
     * @return The soComments
     */
    @JsonProperty("so_comments")
    public String getSoComments() {
        return soComments;
    }

    /**
     *
     * @param soComments The so_comments
     */
    @JsonProperty("so_comments")
    public void setSoComments(String soComments) {
        this.soComments = soComments;
    }

    /**
     *
     * @return The soStatusCvId
     */
    @JsonProperty("so_status_cv_id")
    public Integer getSoStatusCvId() {
        return soStatusCvId;
    }

    /**
     *
     * @param soStatusCvId The so_status_cv_id
     */
    @JsonProperty("so_status_cv_id")
    public void setSoStatusCvId(Integer soStatusCvId) {
        this.soStatusCvId = soStatusCvId;
    }

    /**
     *
     * @return The soStatusName
     */
    @JsonProperty("so_status_name")
    public String getSoStatusName() {
        return soStatusName;
    }

    /**
     *
     * @param soStatusName The so_status_name
     */
    @JsonProperty("so_status_name")
    public void setSoStatusName(String soStatusName) {
        this.soStatusName = soStatusName;
    }

    /**
     *
     * @return The startPosition
     */
    @JsonProperty("start_position")
    public Integer getStartPosition() {
        return startPosition;
    }

    /**
     *
     * @param startPosition The start_position
     */
    @JsonProperty("start_position")
    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }

    /**
     *
     * @return The transcriptId
     */
    @JsonProperty("transcript_id")
    public String getTranscriptId() {
        return transcriptId;
    }

    /**
     *
     * @param transcriptId The transcript_id
     */
    @JsonProperty("transcript_id")
    public void setTranscriptId(String transcriptId) {
        this.transcriptId = transcriptId;
    }

    /**
     *
     * @return The tumorAd
     */
    @JsonProperty("tumor_ad")
    public Integer getTumorAd() {
        return tumorAd;
    }

    /**
     *
     * @param tumorAd The tumor_ad
     */
    @JsonProperty("tumor_ad")
    public void setTumorAd(Integer tumorAd) {
        this.tumorAd = tumorAd;
    }

    /**
     *
     * @return The tumorDp
     */
    @JsonProperty("tumor_dp")
    public Integer getTumorDp() {
        return tumorDp;
    }

    /**
     *
     * @param tumorDp The tumor_dp
     */
    @JsonProperty("tumor_dp")
    public void setTumorDp(Integer tumorDp) {
        this.tumorDp = tumorDp;
    }

    /**
     *
     * @return The tumorVfreq
     */
    @JsonProperty("tumor_vfreq")
    public Object getTumorVfreq() {
        return tumorVfreq;
    }

    /**
     *
     * @param tumorVfreq The tumor_vfreq
     */
    @JsonProperty("tumor_vfreq")
    public void setTumorVfreq(Object tumorVfreq) {
        this.tumorVfreq = tumorVfreq;
    }

    /**
     *
     * @return The sTumorAd
     */
    @JsonProperty("s_tumor_ad")
    public Integer getSTumorAd() {
        return sTumorAd;
    }

    /**
     *
     * @param sTumorAd The s_tumor_ad
     */
    @JsonProperty("s_tumor_ad")
    public void setSTumorAd(Integer sTumorAd) {
        this.sTumorAd = sTumorAd;
    }

    /**
     *
     * @return The sTumorDp
     */
    @JsonProperty("s_tumor_dp")
    public Integer getSTumorDp() {
        return sTumorDp;
    }

    /**
     *
     * @param tumorDp The tumor_dp
     */
    @JsonProperty("s_tumor_dp")
    public void setSTumorDp(Integer sTumorDp) {
        this.sTumorDp = sTumorDp;
    }

    /**
     *
     * @return The sTumorVfreq
     */
    @JsonProperty("s_tumor_vfreq")
    public Object getSTumorVfreq() {
        return sTumorVfreq;
    }

    /**
     *
     * @param sTumorVfreq The s_tumor_vfreq
     */
    @JsonProperty("s_tumor_vfreq")
    public void setSTumorVfreq(Object sTumorVfreq) {
        this.sTumorVfreq = sTumorVfreq;
    }

    /**
     *
     * @return The variantClass
     */
    @JsonProperty("variant_class")
    public String getVariantClass() {
        return variantClass;
    }

    /**
     *
     * @param variantClass The variant_class
     */
    @JsonProperty("variant_class")
    public void setVariantClass(String variantClass) {
        this.variantClass = variantClass;
    }

    /**
     *
     * @return The variantClassCvId
     */
    @JsonProperty("variant_class_cv_id")
    public Integer getVariantClassCvId() {
        return variantClassCvId;
    }

    /**
     *
     * @param variantClassCvId The variant_class_cv_id
     */
    @JsonProperty("variant_class_cv_id")
    public void setVariantClassCvId(Integer variantClassCvId) {
        this.variantClassCvId = variantClassCvId;
    }

    /**
     *
     * @return The variantStatusCvId
     */
    @JsonProperty("variant_status_cv_id")
    public Integer getVariantStatusCvId() {
        return variantStatusCvId;
    }

    /**
     *
     * @param variantStatusCvId The variant_status_cv_id
     */
    @JsonProperty("variant_status_cv_id")
    public void setVariantStatusCvId(Integer variantStatusCvId) {
        this.variantStatusCvId = variantStatusCvId;
    }

    /**
     *
     * @return The variantStatusName
     */
    @JsonProperty("variant_status_name")
    public String getVariantStatusName() {
        return variantStatusName;
    }

    /**
     *
     * @param variantStatusName The variant_status_name
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
