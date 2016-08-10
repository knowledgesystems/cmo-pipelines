/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.foundation.model.staging;

import org.cbioportal.cmo.pipelines.foundation.model.ShortVariantType;
import org.cbioportal.cmo.pipelines.foundation.util.FoundationUtils;

import java.util.*;
import com.google.common.base.Strings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Prithi Chakrapani, ochoaa
 */
public class MutationData {
    private static final Log LOG = LogFactory.getLog(MutationData.class);

    private String gene;
    private String entrezGeneId;
    private String center;
    private String build;
    private String chromosome;
    private String startPosition;
    private String endPosition;
    private String strand;
    private String variantClassification;
    private String variantType;
    private String refAllele;
    private String tumorAllele1;
    private String tumorAllele2;
    private String dbSNPRS;
    private String dbSNPValStatus;
    private String tumorSampleBarcode;
    private String matchedNormSampleBarcode;
    private String matchNormSeqAllele1;
    private String mathNormSeqAllele2;
    private String tumorValidationAllele1;
    private String tumorValidationAllele2;
    private String matchNormValidationAllele1;
    private String matchNormValidationAllele2;
    private String verificationStatus;
    private String validationStatus;
    private String mutationStatus;
    private String sequencingPhase;
    private String sequenceSource;
    private String validationMethod;
    private String score;
    private String bamFile;
    private String sequencer;
    private String tumorSampleUUID;
    private String matchedNormSampleUUID;
    private String tRefCount;
    private String tAltCount;
    private String nRefCount;
    private String nAltCount;
    private String cDNA_change;
    private String aaChange;
    private String transcript;
    private String comments;
   
    public MutationData() {}
    
    public MutationData(String sampleId, ShortVariantType shortVariant) {
        this.tumorSampleBarcode = sampleId;
        this.gene = shortVariant.getGene();
        this.entrezGeneId ="0";
        this.chromosome = shortVariant.getPosition().split(":")[0].replace("chr","");
        this.strand = "+";
        this.aaChange = shortVariant.getProteinEffect();
        this.cDNA_change = shortVariant.getCdsEffect();
        this.transcript = shortVariant.getTranscript();
        
        /**
         * resolve reference allele, tumor allele, variant type, and end position.
         */        
        // get nucleotide bases from the short variant cds effect
        String bases = shortVariant.getCdsEffect().replaceAll("[^tcgaTCGA]", " ").trim();
        String[] alleleList = bases.toUpperCase().split(" ");
        
        this.refAllele = FoundationUtils.resolveReferenceAllele(bases, alleleList, shortVariant.getCdsEffect(), shortVariant.getStrand());
        if (this.refAllele.isEmpty()) {
            LOG.warn("Could not resolve reference allele for sample id: " + sampleId + " from cds effect: " + shortVariant.getCdsEffect());
        }

        this.tumorAllele1 = FoundationUtils.resolveTumorAllele(bases, alleleList, shortVariant.getCdsEffect(), shortVariant.getStrand());
        if (this.tumorAllele1.isEmpty()) {
            LOG.warn("Could not resolve tumor allele for sample id: " + sampleId + " from cds effect: " + shortVariant.getCdsEffect());
        }                
        
        this.variantType = FoundationUtils.resolveVariantType(this.refAllele, this.tumorAllele1, shortVariant.getCdsEffect());
        if (this.variantType.equals("UNK")) {
            LOG.warn("Could not resolve variant type for sample id: " + sampleId + "from cds effect: " + shortVariant.getCdsEffect());
        }
        
        this.variantClassification = FoundationUtils.resolveVariantClassification(shortVariant.getFunctionalEffect(), this.variantType);
        this.startPosition = FoundationUtils.resolveStartPosition(shortVariant.getPosition(), this.variantType);
        this.endPosition = FoundationUtils.calculateEndPosition(shortVariant.getCdsEffect(), Integer.valueOf(this.startPosition));
        
        /**
         * calculate mutation data.
         */        
        Float depth = Float.parseFloat(shortVariant.getDepth().toString());
        Float percentReads = Float.parseFloat(shortVariant.getPercentReads().toString());
        Long altCount=  Math.round( depth * (percentReads/100.0));
        
        this.tAltCount =  Long.toString(Math.round( depth * (percentReads/100.0)));        
        this.tRefCount = Long.toString((long) (depth - altCount));
        
        /**
         * set defaults for MutationData.
         */
        this.center = FoundationUtils.FOUNDATION_CENTER;
        this.build = FoundationUtils.FOUNDATION_BUILD;
        this.tumorAllele2 = FoundationUtils.FOUNDATION_NA;        
        this.dbSNPRS = FoundationUtils.FOUNDATION_EMPTY;
        this.dbSNPValStatus = FoundationUtils.FOUNDATION_EMPTY;
        this.matchedNormSampleBarcode = FoundationUtils.FOUNDATION_EMPTY;
        this.matchNormSeqAllele1 = FoundationUtils.FOUNDATION_EMPTY;
        this.mathNormSeqAllele2 = FoundationUtils.FOUNDATION_EMPTY;
        this.tumorValidationAllele1 = FoundationUtils.FOUNDATION_EMPTY;
        this.tumorValidationAllele2 = FoundationUtils.FOUNDATION_EMPTY;
        this.matchNormValidationAllele1 = FoundationUtils.FOUNDATION_EMPTY;
        this.matchNormValidationAllele2 = FoundationUtils.FOUNDATION_EMPTY;
        this.verificationStatus = FoundationUtils.FOUNDATION_UNKNOWN;
        this.mutationStatus = FoundationUtils.FOUNDATION_EMPTY;
        this.sequencingPhase = FoundationUtils.FOUNDATION_EMPTY;
        this.sequenceSource = FoundationUtils.FOUNDATION_EMPTY;
        this.validationMethod = FoundationUtils.FOUNDATION_EMPTY;
        this.score = FoundationUtils.FOUNDATION_EMPTY;
        this.bamFile = FoundationUtils.FOUNDATION_EMPTY;
        this.sequencer = FoundationUtils.FOUNDATION_EMPTY;
        this.tumorSampleUUID = FoundationUtils.FOUNDATION_EMPTY;
        this.matchedNormSampleUUID = FoundationUtils.FOUNDATION_EMPTY;
        this.nRefCount = FoundationUtils.FOUNDATION_EMPTY;
        this.nAltCount = FoundationUtils.FOUNDATION_EMPTY;
        this.comments = FoundationUtils.FOUNDATION_EMPTY;                
    }

    /**
     * @return the gene
     */
    public String getGene() {
        return gene;
    }

    /**
     * @param gene the gene to set
     */
    public void setGene(String gene) {
        this.gene = gene;
    }

    /**
     * @return the entrezGeneId
     */
    public String getEntrezGeneId() {
        return entrezGeneId;
    }

    /**
     * @param entrezGeneId the entrezGeneId to set
     */
    public void setEntrezGeneId(String entrezGeneId) {
        this.entrezGeneId = entrezGeneId;
    }

    /**
     * @return the center
     */
    public String getCenter() {
        return center;
    }

    /**
     * @param center the center to set
     */
    public void setCenter(String center) {
        this.center = center;
    }

    /**
     * @return the build
     */
    public String getBuild() {
        return build;
    }

    /**
     * @param build the build to set
     */
    public void setBuild(String build) {
        this.build = build;
    }

    /**
     * @return the chromosome
     */
    public String getChromosome() {
        return chromosome;
    }

    /**
     * @param chromosome the chromosome to set
     */
    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    /**
     * @return the startPosition
     */
    public String getStartPosition() {
        return startPosition;
    }

    /**
     * @param startPosition the startPosition to set
     */
    public void setStartPosition(String startPosition) {
        this.startPosition = startPosition;
    }

    /**
     * @return the endPosition
     */
    public String getEndPosition() {
        return endPosition;
    }

    /**
     * @param endPosition the endPosition to set
     */
    public void setEndPosition(String endPosition) {
        this.endPosition = endPosition;
    }

    /**
     * @return the strand
     */
    public String getStrand() {
        return strand;
    }

    /**
     * @param strand the strand to set
     */
    public void setStrand(String strand) {
        this.strand = strand;
    }

    /**
     * @return the variantClassification
     */
    public String getVariantClassification() {
        return variantClassification;
    }

    /**
     * @param variantClassification the variantClassification to set
     */
    public void setVariantClassification(String variantClassification) {
        this.variantClassification = variantClassification;
    }

    /**
     * @return the varaintType
     */
    public String getVariantType() {
        return variantType;
    }

    /**
     * @param variantType
     */
    public void setVariantType(String variantType) {
        this.variantType = variantType;
    }

    /**
     * @return the refAllele
     */
    public String getRefAllele() {
        return refAllele;
    }

    /**
     * @param refAllele the refAllele to set
     */
    public void setRefAllele(String refAllele) {
        this.refAllele = refAllele;
    }

    /**
     * @return the tumorAllele1
     */
    public String getTumorAllele1() {
        return tumorAllele1;
    }

    /**
     * @param tumorAllele1 the tumorAllele1 to set
     */
    public void setTumorAllele1(String tumorAllele1) {
        this.tumorAllele1 = tumorAllele1;
    }

    /**
     * @return the tumorAllele2
     */
    public String getTumorAllele2() {
        return tumorAllele2;
    }

    /**
     * @param tumorAllele2 the tumorAllele2 to set
     */
    public void setTumorAllele2(String tumorAllele2) {
        this.tumorAllele2 = tumorAllele2;
    }

    /**
     * @return the dbSNPRS
     */
    public String getDbSNPRS() {
        return dbSNPRS;
    }

    /**
     * @param dbSNPRS the dbSNPRS to set
     */
    public void setDbSNPRS(String dbSNPRS) {
        this.dbSNPRS = dbSNPRS;
    }

    /**
     * @return the dbSNPValStatus
     */
    public String getDbSNPValStatus() {
        return dbSNPValStatus;
    }

    /**
     * @param dbSNPValStatus the dbSNPValStatus to set
     */
    public void setDbSNPValStatus(String dbSNPValStatus) {
        this.dbSNPValStatus = dbSNPValStatus;
    }

    /**
     * @return the tumorSampleBarcode
     */
    public String getTumorSampleBarcode() {
        return tumorSampleBarcode;
    }

    /**
     * @param tumorSampleBarcode the tumorSampleBarcode to set
     */
    public void setTumorSampleBarcode(String tumorSampleBarcode) {
        this.tumorSampleBarcode = tumorSampleBarcode;
    }

    /**
     * @return the matchedNormSampleBarcode
     */
    public String getMatchedNormSampleBarcode() {
        return matchedNormSampleBarcode;
    }
    /**
     * @param matchedNormSampleBarcode the matchedNormSampleBarcode to set
     */
    public void setMatchedNormSampleBarcode(String matchedNormSampleBarcode) {
        this.matchedNormSampleBarcode = matchedNormSampleBarcode;
    }

    /**
     * @return the matchNormSeqAllele1
     */
    public String getMatchNormSeqAllele1() {
        return matchNormSeqAllele1;
    }

    /**
     * @param matchNormSeqAllele1 the matchNormSeqAllele1 to set
     */
    public void setMatchNormSeqAllele1(String matchNormSeqAllele1) {
        this.matchNormSeqAllele1 = matchNormSeqAllele1;
    }

    /**
     * @return the mathNormSeqAllele2
     */
    public String getMathNormSeqAllele2() {
        return mathNormSeqAllele2;
    }

    /**
     * @param mathNormSeqAllele2 the mathNormSeqAllele2 to set
     */
    public void setMathNormSeqAllele2(String mathNormSeqAllele2) {
        this.mathNormSeqAllele2 = mathNormSeqAllele2;
    }

    /**
     * @return the tumorValidationAllele1
     */
    public String getTumorValidationAllele1() {
        return tumorValidationAllele1;
    }

    /**
     * @param tumorValidationAllele1 the tumorValidationAllele1 to set
     */
    public void setTumorValidationAllele1(String tumorValidationAllele1) {
        this.tumorValidationAllele1 = tumorValidationAllele1;
    }

    /**
     * @return the tumorValidationAllele2
     */
    public String getTumorValidationAllele2() {
        return tumorValidationAllele2;
    }

    /**
     * @param tumorValidationAllele2 the tumorValidationAllele2 to set
     */
    public void setTumorValidationAllele2(String tumorValidationAllele2) {
        this.tumorValidationAllele2 = tumorValidationAllele2;
    }

    /**
     * @return the matchNormValidationAllele1
     */
    public String getMatchNormValidationAllele1() {
        return matchNormValidationAllele1;
    }

    /**
     * @param matchNormValidationAllele1 the matchNormValidationAllele1 to set
     */
    public void setMatchNormValidationAllele1(String matchNormValidationAllele1) {
        this.matchNormValidationAllele1 = matchNormValidationAllele1;
    }

    /**
     * @return the matchNormValidationAllele2
     */
    public String getMatchNormValidationAllele2() {
        return matchNormValidationAllele2;
    }

    /**
     * @param matchNormValidationAllele2 the matchNormValidationAllele2 to set
     */
    public void setMatchNormValidationAllele2(String matchNormValidationAllele2) {
        this.matchNormValidationAllele2 = matchNormValidationAllele2;
    }

    /**
     * @return the verificationStatus
     */
    public String getVerificationStatus() {
        return verificationStatus;
    }

    /**
     * @param verificationStatus the verificationStatus to set
     */
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    /**
     * @return the validationStatus
     */
    public String getValidationStatus() {
        return !Strings.isNullOrEmpty(validationStatus)?validationStatus:FoundationUtils.FOUNDATION_UNKNOWN;
    }

    /**
     * @param validationStatus the validationStatus to set
     */
    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    /**
     * @return the mutationStatus
     */
    public String getMutationStatus() {
        return !Strings.isNullOrEmpty(mutationStatus)?mutationStatus:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param mutationStatus the mutationStatus to set
     */
    public void setMutationStatus(String mutationStatus) {
        this.mutationStatus = mutationStatus;
    }

    /**
     * @return the sequencingPhase
     */
    public String getSequencingPhase() {
        return !Strings.isNullOrEmpty(sequencingPhase)?sequencingPhase:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param sequencingPhase the sequencingPhase to set
     */
    public void setSequencingPhase(String sequencingPhase) {
        this.sequencingPhase = sequencingPhase;
    }

    /**
     * @return the sequenceSource
     */
    public String getSequenceSource() {
        return !Strings.isNullOrEmpty(sequenceSource)?sequenceSource:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param sequenceSource the sequenceSource to set
     */
    public void setSequenceSource(String sequenceSource) {
        this.sequenceSource = sequenceSource;
    }

    /**
     * @return the validationMethod
     */
    public String getValidationMethod() {
        return !Strings.isNullOrEmpty(validationMethod)?validationMethod:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param validationMethod the validationMethod to set
     */
    public void setValidationMethod(String validationMethod) {
        this.validationMethod = validationMethod;
    }

    /**
     * @return the score
     */
    public String getScore() {
        return !Strings.isNullOrEmpty(score)?score:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param score the score to set
     */
    public void setScore(String score) {
        this.score = score;
    }

    /**
     * @return the bamFile
     */
    public String getBamFile() {
        return !Strings.isNullOrEmpty(bamFile)?bamFile:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param BamFile the BamFile to set
     */
    public void setBamFile(String bamFile) {
        this.bamFile = bamFile;
    }

    /**
     * @return the sequencer
     */
    public String getSequencer() {
        return !Strings.isNullOrEmpty(sequencer)?sequencer:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param sequencer the sequencer to set
     */
    public void setSequencer(String sequencer) {
        this.sequencer = sequencer;
    }

    /**
     * @return the tumorSampleUUID
     */
    public String getTumorSampleUUID() {
        return !Strings.isNullOrEmpty(tumorSampleUUID)?tumorSampleUUID:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param tumorSampleUUID the tumorSampleUUID to set
     */
    public void setTumorSampleUUID(String tumorSampleUUID) {
        this.tumorSampleUUID = tumorSampleUUID;
    }

    /**
     * @return the matchedNormSampleUUID
     */
    public String getMatchedNormSampleUUID() {
        return !Strings.isNullOrEmpty(matchedNormSampleUUID)?matchedNormSampleUUID:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param matchedNormSampleUUID the matchedNormSampleUUID to set
     */
    public void setMatchedNormSampleUUID(String matchedNormSampleUUID) {
        this.matchedNormSampleUUID = matchedNormSampleUUID;
    }
    /**
     * @return the tRefCount
     */
    public String getTRefCount() {
        return tRefCount;
    }

    /**
     * @param tRefCount the tRefCount to set
     */
    public void setTRefCount(String tRefCount) {
        this.tRefCount = tRefCount;
    }
    /**
     * @return the tAltCount
     */
    public String getTAltCount() {
        return tAltCount;
    }

    /**
     * @param tAltCount the tAltCount to set
     */
    public void setTAltCount(String tAltCount) {
        this.tAltCount = tAltCount;
    }        
    
    /**
     * @return the nAltCount
     */
    public String getNAltCount() {
        return !Strings.isNullOrEmpty(nAltCount)?nAltCount:FoundationUtils.FOUNDATION_EMPTY;
    }
    
    /**
     * @param nAltCount the nAltCount to set
     */
    public void setNAltCount(String nAltCount) {
        this.nAltCount = nAltCount;
    }
       
    
    /**
     * @return the nRefCount
     */
    public String getNRefCount() {
        return !Strings.isNullOrEmpty(nRefCount)?nRefCount:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param nRefCount the nRefCount to set
     */
    public void setNRefCount(String nRefCount) {
        this.nRefCount = nRefCount;
    }

 /**
     * @return the cDNA_change
     */
    public String getCDNA_change() {
        return cDNA_change;
    }

    /**
     * @param cDNA_change the cDNA_change to set
     */
    public void setCDNA_change(String cDNA_change) {
        this.cDNA_change = cDNA_change;
    }

    /**
     * @return the aaChange
     */
    public String getAAChange() {
        return aaChange;
    }
   
    /**
     * @param aaChange the aaChange to set
     */
    public void setAAChange(String aaChange) {
        this.aaChange = aaChange;
    }

    /**
     * @return the transcript
     */
    public String getTranscript() {
        return transcript;
    }

    /**
     * @param transcript the transcript to set
     */
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    /**
     * @return the comments
     */
    public String getComments() {
        return !Strings.isNullOrEmpty(comments)?comments:FoundationUtils.FOUNDATION_EMPTY;
    }

    /**
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    /**
     * Returns a map linking the staging file column name to the appropriate getter method.
     * @return 
     */
    public Map<String, String> getStagingMap(){
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Hugo_Symbol", "getGene");
        map.put("Entrez_Gene_Id", "getEntrezGeneId");
        map.put("Center", "getCenter");
        map.put("NCBI_Build", "getBuild");
        map.put("Chromosome", "getChromosome");
        map.put("Start_Position", "getStartPosition");
        map.put("End_Position", "getEndPosition");
        map.put("Strand", "getStrand");
        map.put("Variant_Classification", "getVariantClassification");
        map.put("Variant_Type", "getVariantType");
        map.put("Reference_Allele", "getRefAllele");
        map.put("Tumor_Seq_Allele1", "getTumorAllele1");
        map.put("Tumor_Seq_Allele2", "getTumorAllele2");
        map.put("dbSNP_RS", "getDbSNPRS");
        map.put("dbSNP_Val_Status", "getDbSNPValStatus");
        map.put("Tumor_Sample_Barcode", "getTumorSampleBarcode");
        map.put("Matched_Norm_Sample_Barcode", "getMatchedNormSampleBarcode");
        map.put("Match_Norm_Seq_Allele1", "getMatchNormSeqAllele1");
        map.put("Match_Norm_Seq_Allele2", "getMathNormSeqAllele2");
        map.put("Tumor_Validation_Allele1", "getTumorValidationAllele1");
        map.put("Tumor_Validation_Allele2", "getTumorValidationAllele2");
        map.put("Match_Norm_Validation_Allele1", "getMatchNormValidationAllele1");
        map.put("Match_Norm_Validation_Allele2", "getMatchNormValidationAllele2");
        map.put("Verification_Status", "getVerificationStatus");
        map.put("Validation_Status", "getValidationStatus");
        map.put("Mutation_Status", "getMutationStatus");
        map.put("Sequencing_Phase", "getSequencingPhase");
        map.put("Sequence_Source", "getSequenceSource");
        map.put("Validation_Method", "getValidationMethod");
        map.put("Score", "getScore");
        map.put("BAM_File", "getBamFile");
        map.put("Sequencer", "getSequencer");
        map.put("Tumor_Sample_UUID", "getTumorSampleUUID");
        map.put("Matched_Norm_Sample_UUID", "getMatchedNormSampleUUID");
        map.put("t_ref_count", "getTRefCount");
        map.put("t_alt_count", "getTAltCount");
        map.put("n_ref_count", "getNAltCount");
        map.put("n_alt_count", "getNRefCount");
        map.put("cDNA_Change", "getCDNA_change");
        map.put("Amino_Acid_Change", "getAAChange");
        map.put("Transcript", "getTranscript");
        map.put("Comments", "getComments");

        return map;
    }
  
}