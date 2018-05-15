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

import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.models.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 *
 * @author heinsz
 */

public class CVRUtilities {

    public static final String CVR_FILE = "cvr_data.json";
    public final String MUTATION_FILE = "data_mutations_extended.txt";
    public final String UNFILTERED_MUTATION_FILE = "data_mutations_unfiltered.txt";
    public static final String DEFAULT_CLINICAL_FILE = "data_clinical.txt";
    public final String SEQ_DATE_CLINICAL_FILE = "cvr/seq_date.txt";
    public final String DARWIN_AGE_FILE = "darwin/darwin_age.txt";
    public final String CORRESPONDING_ID_FILE = "linked_mskimpact_cases.txt";
    public final String CNA_FILE = "data_CNA.txt";
    public final String SEG_FILE = "_data_cna_hg19.seg";
    public final String FUSION_FILE = "data_fusions.txt";
    public final String SV_FILE = "data_SV.txt";
    public final String GENE_PANEL_FILE = "data_gene_matrix.txt";
    public final String CNA_HEADER_HUGO_SYMBOL = "Hugo_Symbol";
    public static final String GML_FILE = "cvr_gml_data.json";
    public final String GENE_PANEL = "gene_panels/impact468_gene_panel.txt";
    public final String IS_NEW = "NEWRECORD";
    public static final Integer DEFAULT_MAX_NUM_SAMPLES_TO_REMOVE = -1;
    public static final SimpleDateFormat CVR_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz");

    private final String CENTER_MSKCC = "MSKCC";
    private final String DEFAULT_BUILD_NUMBER = "37";
    private final String DEFAULT_STRAND = "+";
    private final String VALIDATION_STATUS_UNKNOWN = "Unknown";
    private final String DEFAULT_IMPACT_SEQUENCER = "MSK-IMPACT";


    Logger log = Logger.getLogger(CVRUtilities.class);
    private String genesStableId;
    private String genesDescription;
    private String genesCancerStudyId;
    private List<String> geneSymbols;

    private List<String> variationList = new ArrayList();
    @Autowired
    private void createVariationList() {
        // the order in which these are added is important!
        List<String> list = new ArrayList();
        list.add("INS");
        list.add("SNP");
        list.add("DNP");
        list.add("TNP");
        list.add("ONP");
        this.variationList = list;
    }

    public CVRUtilities() {
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
        return genesStableId;
    }

    public String getGenesDescription() {
        return genesDescription;
    }

    public String getGeneCancerStudyId() {
        return genesCancerStudyId;
    }

    public List<String> getGeneSymbols() {
        return geneSymbols;
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

    public List<String> processFileComments(File dataFile) throws FileNotFoundException, IOException {
        List<String> comments  = new ArrayList();
        BufferedReader reader  = new BufferedReader(new FileReader(dataFile));
        String line;
        while ((line = reader.readLine()) != null && line.startsWith("#")) {
            comments.add(line);
        }
        reader.close();

        return comments;
    }

    public boolean isDuplicateRecord(MutationRecord snp, List<AnnotatedRecord> annotatedRecords) {
        if (annotatedRecords == null || annotatedRecords.isEmpty()) {
            return false;
        }

        for (AnnotatedRecord record : annotatedRecords) {
            if (record.getCHROMOSOME().equals(snp.getCHROMOSOME()) &&
                    record.getSTART_POSITION().equals(snp.getSTART_POSITION()) &&
                    record.getEND_POSITION().equals(snp.getEND_POSITION()) &&
                    record.getREFERENCE_ALLELE().equals(snp.getREFERENCE_ALLELE()) &&
                    record.getTUMOR_SEQ_ALLELE2().equals(snp.getTUMOR_SEQ_ALLELE2()) &&
                    record.getHUGO_SYMBOL().equals(snp.getHUGO_SYMBOL())) {
                return true;
            }
        }
        return false;
    }

    public AnnotatedRecord buildCVRAnnotatedRecord(MutationRecord record) {
        String hugoSymbol = record.getHUGO_SYMBOL();
        String entrezGeneId = record.getENTREZ_GENE_ID();
        String center = record.getCENTER();
        String ncbiBuild = record.getNCBI_BUILD();
        String chromosome = record.getCHROMOSOME();
        String startPosition = record.getSTART_POSITION();
        String strand = record.getSTRAND();
        String variantClassification = record.getVARIANT_CLASSIFICATION();
        String variantType = record.getVARIANT_TYPE();
        String referenceAllele = record.getREFERENCE_ALLELE();
        String endPosition = record.getEND_POSITION();
        String tumorSeqAllele1 = record.getTUMOR_SEQ_ALLELE1();
        String tumorSeqAllele2 = record.getTUMOR_SEQ_ALLELE2();
        String dbSnpRs = record.getDBSNP_RS();
        String dbSnpValStatus = record.getDBSNP_VAL_STATUS();
        String tumorSampleBarcode = record.getTUMOR_SAMPLE_BARCODE();
        String matchedNormSampleBarcode = record.getMATCHED_NORM_SAMPLE_BARCODE();
        String matchedNormSeqAllele1 = record.getMATCH_NORM_SEQ_ALLELE1();
        String matchedNormSeqAllele2 = record.getMATCH_NORM_SEQ_ALLELE2();
        String tumorValidationAllele1 = record.getTUMOR_VALIDATION_ALLELE1();
        String tumorValidationAllele2 = record.getTUMOR_VALIDATION_ALLELE2();
        String matchNormValidationAllele1 = record.getMATCH_NORM_VALIDATION_ALLELE1();
        String matchNormValidationAllele2 = record.getMATCH_NORM_VALIDATION_ALLELE2();
        String verificationStatus = record.getVERIFICATION_STATUS();
        String validationStatus = record.getVALIDATION_STATUS();
        String mutationStatus = record.getMUTATION_STATUS();
        String sequencingPhase = record.getSEQUENCING_PHASE();
        String sequencingSource = record.getSEQUENCE_SOURCE();
        String validationMethod = record.getVALIDATION_METHOD();
        String score = record.getSCORE();
        String bamFile = record.getBAM_FILE();
        String sequencer = record.getSEQUENCER();
        String tumorSampleUUID = record.getTUMOR_SAMPLE_UUID();
        String matchedNormSampleUUID = record.getMATCHED_NORM_SAMPLE_UUID();
        String tRefCount = record.getT_REF_COUNT();
        String nRefCount = record.getN_REF_COUNT();
        String tAltCount = record.getT_ALT_COUNT();
        String nAltCount = record.getN_ALT_COUNT();
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
        String tRefCount = "";
        String nRefCount = String.valueOf(snp.getDepth() - snp.getAlleleDepth());
        String tAltCount = "";
        String nAltCount = String.valueOf(snp.getAlleleDepth());
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

    /**
     * Calculates the age at seq report for a list of CVRClinical records given a patient age
     * and the reference calculation date.
     * @param referenceCalculationDate
     * @param records
     * @param patientAge
     * @throws ParseException
     */
    public void calculateAgeAtSeqReportForPatient(Date referenceCalculationDate, List<CVRClinicalRecord> records, String patientAge) throws ParseException {
        for (CVRClinicalRecord record : records) {
            if (record.getSEQ_DATE() != null && !record.getSEQ_DATE().isEmpty() && !record.getSEQ_DATE().equals("NA")) {
                Date cvrDateSequenced = CVR_DATE_FORMAT.parse(record.getSEQ_DATE());
                // We know age of patient now from darwin, and the time at which the patient was sequenced.
                // The age of the patient when sequenced is therefore AGE_NOW - YEARS_SINCE_SEQUENCING
                // This converts the date arithmetic from miliseconds to years.
                // 1000ms -> 1s, 60s -> 1m, 60m -> 1h, 24h -> 1d, 365.2422d -> 1y
                Double diffYears = (referenceCalculationDate.getTime() - cvrDateSequenced.getTime()) / 1000L / 60L / 60L / 24L / 365.2422;
                Double ageAtSeqReport = Math.ceil(Integer.parseInt(patientAge) - diffYears);
                if (ageAtSeqReport > 90) {
                    ageAtSeqReport = 90D;
                }
                if (ageAtSeqReport < 15) {
                    ageAtSeqReport = 15D;
                }
                record.setAGE_AT_SEQ_REPORT(String.valueOf(ageAtSeqReport.intValue()));
            }
            else {
                record.setAGE_AT_SEQ_REPORT("NA");
            }
        }
    }

}
