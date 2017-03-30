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

package org.cbioportal.cmo.pipelines.cvr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.models.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author heinsz
 */

@Configuration
public class CVRUtilities {

    public static final String CVR_FILE = "cvr_data.json";
    public final String MUTATION_FILE = "data_mutations_extended.txt";
    public final String UNFILTERED_MUTATION_FILE = "data_mutations_unfiltered.txt";
    public final String CLINICAL_FILE = "data_clinical.txt";
    public final String CNA_FILE = "data_CNA.txt";
    public final String SEG_FILE = "_data_cna_hg19.seg";
    public final String FUSION_FILE = "data_fusions.txt";
    public final String SV_FILE = "data_SV.txt";
    public final String GENE_PANEL_FILE = "data_gene_matrix.txt";
    public final String CNA_HEADER_HUGO_SYMBOL = "Hugo_Symbol";
    public static final String GML_FILE = "cvr_gml_data.json";
    public final String GENE_PANEL = "gene_panels/impact468_gene_panel.txt";
    public final String IS_NEW = "NEWRECORD";

    private final String CENTER_MSKCC = "MSKCC";
    private final String DEFAULT_BUILD_NUMBER = "37";
    private final String DEFAULT_STRAND = "+";
    private final String VALIDATION_STATUS_UNKNOWN = "Unknown";
    private final String DEFAULT_IMPACT_SEQUENCER = "MSK-IMPACT";

    private final List<String> variationList = createVariationList();
    private Set<String> newIds = new HashSet<>();
    private Set<String> allIds = new HashSet<>();
    Logger log = Logger.getLogger(CVRUtilities.class);
    private String genesStableId;
    private String genesDescription;
    private String genesCancerStudyId;
    private List<String> geneSymbols;

    @Bean
    public CVRUtilities CVRUtilities() {
        return new CVRUtilities();
    }

    public CVRUtilities() {
    }

    public void addNewId(String id) {
        newIds.add(id);
    }

    public Set<String> getNewIds() {
        return newIds;
    }

    public Set<String> getAllIds() {
        return allIds;
    }

    public void addAllIds(String id) {
        allIds.add(id);
    }

    public CVRData readJson(File cvrFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(cvrFile, CVRData.class);
    }
    public GMLData readGMLJson(File gmlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(gmlFile, GMLData.class);
    }

    public String getGenesStableId() {
        return this.genesStableId;
    }

    public String getGenesDescription() {
        return this.genesDescription;
    }

    public String getGeneCancerStudyId() {
        return this.genesCancerStudyId;
    }

    public List<String> getGeneSymbols() {
        return this.geneSymbols;
    }

    public void importGenePanel(String geneFileName) throws Exception {
        File geneFile = new File(geneFileName);
        Properties properties = new Properties();
        properties.load(new FileInputStream(geneFile));

        this.genesStableId = getPropertyValue("stable_id", properties, false);
        this.genesDescription = getPropertyValue("description", properties, true);
        this.genesCancerStudyId = getPropertyValue("cancer_study_identifier", properties, false);
        this.geneSymbols = getGeneSymbols("gene_list", properties);
    }

    private static String getPropertyValue(String propertyName, Properties properties, boolean spaces) throws IllegalArgumentException {
        String propertyValue = properties.getProperty(propertyName).trim();
        if (propertyValue == null || propertyValue.length() == 0) {
            throw new IllegalArgumentException(propertyName + " is not specified!");
        }
        if (!spaces && propertyValue.contains(" ")) {
            throw new IllegalArgumentException(propertyName + " cannot contain spaces: " + propertyValue);
        }
        return propertyValue;
    }

    private static List<String> getGeneSymbols(String propertyName, Properties properties) throws IllegalArgumentException {
        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue == null || propertyValue.length() == 0) {
            throw new IllegalArgumentException(propertyName + " is not specified!");
        }
        String[] symbols = propertyValue.split("\t");
        return Arrays.asList(symbols);
    }
    
    public List<String> processFileComments(File dataFile, boolean withAllSampleIds) throws FileNotFoundException, IOException {
        List<String> comments  = new ArrayList();
        BufferedReader reader  = new BufferedReader(new FileReader(dataFile));
        String line;
        while ((line = reader.readLine()) != null && line.startsWith("#")) {
            comments.add(line);
            if (withAllSampleIds && line.startsWith("#sequenced_samples")) {
                for (String sample : line.split(":")[1].split(" ")) {
                    if (!sample.trim().isEmpty()) {
                        addAllIds(sample);
                    }
                }
            }
        }
        reader.close();
        
        return comments;
    }
    
    public boolean isDuplicateRecord(MutationRecord snp, List<AnnotatedRecord> annotatedRecords) {
        if (annotatedRecords == null || annotatedRecords.isEmpty()) {
            return false;
        }
        
        for (AnnotatedRecord record : annotatedRecords) {
            if (record.getChromosome().equals(snp.getChromosome()) && 
                    record.getStart_Position().equals(snp.getStart_Position()) &&
                    record.getEnd_Position().equals(snp.getEnd_Position()) &&
                    record.getReference_Allele().equals(snp.getReference_Allele()) &&
                    record.getTumor_Seq_Allele2().equals(snp.getTumor_Seq_Allele2()) && 
                    record.getHugo_Symbol().equals(snp.getHugo_Symbol())) {
                return true;
            }
        }
        return false;
    }

    public AnnotatedRecord buildCVRAnnotatedRecord(MutationRecord record) {
        String hugoSymbol = record.getHugo_Symbol();
        String entrezGeneId = record.getEntrez_Gene_Id();
        String center = record.getCenter();
        String ncbiBuild = record.getNCBI_Build();
        String chromosome = record.getChromosome();
        String startPosition = record.getStart_Position();
        String strand = record.getStrand();
        String variantClassification = record.getVariant_Classification();
        String variantType = record.getVariant_Type();
        String referenceAllele = record.getReference_Allele();
        String endPosition = record.getEnd_Position();
        String tumorSeqAllele1 = record.getTumor_Seq_Allele1();
        String tumorSeqAllele2 = record.getTumor_Seq_Allele2();
        String dbSnpRs = record.getdbSNP_RS();
        String dbSnpValStatus = record.getdbSNP_Val_Status();
        String tumorSampleBarcode = record.getTumor_Sample_Barcode();
        String matchedNormSampleBarcode = record.getMatched_Norm_Sample_Barcode();
        String matchedNormSeqAllele1 = record.getMatch_Norm_Seq_Allele1();
        String matchedNormSeqAllele2 = record.getMatch_Norm_Seq_Allele2();
        String tumorValidationAllele1 = record.getTumor_Validation_Allele1();
        String tumorValidationAllele2 = record.getTumor_Validation_Allele2();
        String matchNormValidationAllele1 = record.getMatch_Norm_Validation_Allele1();
        String matchNormValidationAllele2 = record.getMatch_Norm_Validation_Allele2();
        String verificationStatus = record.getVerification_Status();
        String validationStatus = record.getValidation_Status();
        String mutationStatus = record.getMutation_Status();
        String sequencingPhase = record.getSequencing_Phase();
        String sequencingSource = record.getSequence_Source();
        String validationMethod = record.getValidation_Method();
        String score = record.getScore();
        String bamFile = record.getBAM_File();
        String sequencer = record.getSequencer();
        String tumorSampleUUID = record.getTumor_Sample_UUID();
        String matchedNormSampleUUID = record.getMatched_Norm_Sample_UUID();
        String tRefCount = record.gett_ref_count();
        String nRefCount = record.getn_ref_count();
        String tAltCount = record.gett_alt_count();
        String nAltCount = record.getn_alt_count();
        Map<String ,String> additionalProperties = record.getAdditionalProperties();
        return new AnnotatedRecord(hugoSymbol, entrezGeneId, center, ncbiBuild, chromosome,
                startPosition, endPosition, strand, variantClassification, variantType, referenceAllele,
                tumorSeqAllele1, tumorSeqAllele2, dbSnpRs, dbSnpValStatus, tumorSampleBarcode,
                matchedNormSampleBarcode, matchedNormSeqAllele1, matchedNormSeqAllele2, tumorValidationAllele1,
                tumorValidationAllele2, matchNormValidationAllele1, matchNormValidationAllele2, verificationStatus,
                validationStatus, mutationStatus, sequencingPhase, sequencingSource, validationMethod, score,
                bamFile, sequencer, tumorSampleUUID, matchedNormSampleUUID, tRefCount, nRefCount, tAltCount, nAltCount, "", "", "", "", "", "", "", "", "", "", additionalProperties);
    }

    public MutationRecord buildCVRMutationRecord(CVRSnp snp, String sampleId, String somaticStatus) {
        String hugoSymbol = snp.getGeneId();
        String entrezGeneId = "0";
        String center = CENTER_MSKCC;
        String ncbiBuild = DEFAULT_BUILD_NUMBER;
        String chromosome = snp.getChromosome();
        String startPosition = String.valueOf(snp.getStartPosition());
        String strand = DEFAULT_STRAND;
        String variantClassification = snp.getVariantClass();
        String variantType = resolveVariantType(snp.getRefAllele(), snp.getAltAllele());
        String referenceAllele = snp.getRefAllele();
        String endPosition = resolveEndPosition(variantType, startPosition, referenceAllele);
        String tumorSeqAllele1 = snp.getRefAllele();
        String tumorSeqAllele2 = snp.getAltAllele();
        String dbSnpRs = snp.getDbSNPId();
        String dbSnpValStatus = "";
        String tumorSampleBarcode = sampleId;
        String matchedNormSampleBarcode = "";
        String matchedNormSeqAllele1 = "";
        String matchedNormSeqAllele2 = "";
        String tumorValidationAllele1 = "";
        String tumorValidationAllele2 = "";
        String matchNormValidationAllele1 = "";
        String matchNormValidationAllele2 = "";
        String verificationStatus = "";
        String validationStatus = VALIDATION_STATUS_UNKNOWN;
        String mutationStatus = somaticStatus.equals("Matched") ? "SOMATIC" : "UNKNOWN";
        String sequencingPhase = "";
        String sequencingSource = "";
        String validationMethod = "";
        String score = DEFAULT_IMPACT_SEQUENCER; // Why?
        String bamFile = "";
        String sequencer = "";
        String tumorSampleUUID = "";
        String matchedNormSampleUUID = "";
        String tRefCount = String.valueOf(snp.getTumorDp() - snp.getTumorAd());
        String nRefCount = String.valueOf(snp.getNormalDp() - snp.getNormalAd());
        String tAltCount = String.valueOf(snp.getTumorAd());
        String nAltCount = String.valueOf(snp.getNormalAd());
        Map<String ,String> additionalProperties = new LinkedHashMap<>();
        return new MutationRecord(hugoSymbol, entrezGeneId, center, ncbiBuild, chromosome,
                startPosition, endPosition, strand, variantClassification, variantType, referenceAllele,
                tumorSeqAllele1, tumorSeqAllele2, dbSnpRs, dbSnpValStatus, tumorSampleBarcode,
                matchedNormSampleBarcode, matchedNormSeqAllele1, matchedNormSeqAllele2, tumorValidationAllele1,
                tumorValidationAllele2, matchNormValidationAllele1, matchNormValidationAllele2, verificationStatus,
                validationStatus, mutationStatus, sequencingPhase, sequencingSource, validationMethod, score,
                bamFile, sequencer, tumorSampleUUID, matchedNormSampleUUID, tRefCount ,tAltCount, nRefCount, nAltCount, additionalProperties);
    }

    public MutationRecord buildGMLMutationRecord(GMLSnp snp, String sampleId) {
        String hugoSymbol = snp.getGeneId();
        String entrezGeneId = "0";
        String center = CENTER_MSKCC;
        String ncbiBuild = DEFAULT_BUILD_NUMBER;
        String chromosome = snp.getChromosome();
        String startPosition = String.valueOf(snp.getStartPosition());
        String variantType = resolveVariantType(snp.getRefAllele(), snp.getAltAllele());
        String referenceAllele = snp.getRefAllele();
        String endPosition = resolveEndPosition(variantType, startPosition, referenceAllele);
        String strand = DEFAULT_STRAND;
        String variantClassification = "";
        String tumorSeqAllele1 = snp.getRefAllele();
        String tumorSeqAllele2 = snp.getAltAllele();
        String dbSnpRs = snp.getDbSNPId();
        String dbSnpValStatus = "";
        String tumorSampleBarcode = sampleId;
        String matchedNormSampleBarcode = "";
        String matchedNormSeqAllele1 = "";
        String matchedNormSeqAllele2 = "";
        String tumorValidationAllele1 = "";
        String tumorValidationAllele2 = "";
        String matchNormValidationAllele1 = "";
        String matchNormValidationAllele2 = "";
        String verificationStatus = "";
        String validationStatus = VALIDATION_STATUS_UNKNOWN;
        String mutationStatus = "GERMLINE";
        String sequencingPhase = "";
        String sequencingSource = "";
        String validationMethod = "";
        String score = DEFAULT_IMPACT_SEQUENCER; // Why?
        String bamFile = "";
        String sequencer = "";
        String tumorSampleUUID = "";
        String matchedNormSampleUUID = "";
        String tRefCount = String.valueOf(snp.getDepth()- snp.getAlleleDepth());
        String nRefCount = "";
        String tAltCount = String.valueOf(snp.getAlleleDepth());
        String nAltCount = "";
        String cDNA_Change = snp.getCDNAChange();
        String aminoAcidChange = snp.getAaChange();
        String transcript = snp.getTranscriptId();
        String comments = snp.getInterpretation().replaceAll("\r\n", " ").replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("\r", " ");
        Map<String ,String> additionalProperties = new LinkedHashMap<>();
        additionalProperties.put("COMMENTS", comments);
        return new MutationRecord(hugoSymbol, entrezGeneId, center, ncbiBuild, chromosome,
                startPosition, endPosition, strand, variantClassification, variantType, referenceAllele,
                tumorSeqAllele1, tumorSeqAllele2, dbSnpRs, dbSnpValStatus, tumorSampleBarcode,
                matchedNormSampleBarcode, matchedNormSeqAllele1, matchedNormSeqAllele2, tumorValidationAllele1,
                tumorValidationAllele2, matchNormValidationAllele1, matchNormValidationAllele2, verificationStatus,
                validationStatus, mutationStatus, sequencingPhase, sequencingSource, validationMethod, score,
                bamFile, sequencer, tumorSampleUUID, matchedNormSampleUUID, tRefCount, tAltCount, nRefCount, nAltCount, additionalProperties);
    }

    private String resolveEndPosition(String variantType, String startPosition, String referenceAllele) {
        switch (variantType) {
            case "INS":
                // ( $start, $stop ) = ( $pos, ( $ref eq "-" ? $pos + 1 : $pos + $ref_length ));
                String ins_ep = referenceAllele.equals("-") ? String.valueOf(Integer.parseInt(startPosition) + 1) : String.valueOf(Integer.parseInt(startPosition) + referenceAllele.length() - 1);
                return ins_ep;
            case "DEL":
                //( $start, $stop ) = ( $pos + 1, $pos + $ref_length );
                String del_ep = String.valueOf(Integer.parseInt(startPosition) + referenceAllele.length() - 1);
                return del_ep;
            default:
                return String.valueOf(Integer.parseInt(startPosition) + referenceAllele.length() -1);
        }
    }

    private String resolveVariantType(String refAllele, String altAllele) {
        if (!StringUtils.isEmpty(refAllele) && !StringUtils.isEmpty(altAllele)) {
            if (refAllele.equals("-")) {
                return "INS";
            }
            if (altAllele.equals("-") || altAllele.length() < refAllele.length()) {
                return "DEL";
            }
            if (refAllele.length() < altAllele.length()) {
                return "INS";
            }
            if (refAllele.length() < variationList.size()) {
                return variationList.get(refAllele.length());
            }
            if (refAllele.length() > variationList.size()) {
                return variationList.get(variationList.size() - 1);
            }
        }
        return "UNK";
    }

    private List<String> createVariationList() {
        // the order in which these are added is important!
        List<String> list = new ArrayList();
        list.add("INS");
        list.add("SNP");
        list.add("DNP");
        list.add("TNP");
        list.add("ONP");
        return list;
    }
}
