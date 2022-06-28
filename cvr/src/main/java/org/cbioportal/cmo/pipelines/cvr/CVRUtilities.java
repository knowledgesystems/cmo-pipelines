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

import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.models.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author heinsz
 */

public class CVRUtilities {
    private String METADATA_PREFIX = "#";
    private String DELIMITER = "\t";

    // pipeline filenames
    public static final String CVR_FILE = "cvr_data.json";
    public static final String MUTATION_FILE = "data_mutations_extended.txt";
    public static final String NONSIGNEDOUT_MUTATION_FILE = "data_nonsignedout_mutations.txt";
    public static final String DEFAULT_CLINICAL_FILE = "data_clinical.txt";
    public static final String SEQ_DATE_CLINICAL_FILE = "cvr/seq_date.txt";
    public static final String DDP_AGE_FILE = "ddp/ddp_age.txt";
    public static final String CORRESPONDING_ID_FILE = "cvr/linked_cases.txt";
    public static final String CNA_FILE = "data_CNA.txt";
    public static final String SEG_FILE = "_data_cna_hg19.seg";
    public static final String SV_FILE = "data_sv.txt";
    public static final String GENE_PANEL_FILE = "data_gene_matrix.txt";
    public static final String GML_FILE = "cvr_gml_data.json";
    public static final String GENE_PANEL = "gene_panels/impact468_gene_panel.txt";
    public static final String ZERO_VARIANT_WHITELIST_FILE = "cvr/zero_variant_whitelist.txt";
    public static final List<String> SUPPORTED_SEQ_DATE_STUDY_IDS = Arrays.asList("mskimpact", "mskimpact_heme", "mskaccess");

    // pipeline globals
    public static final String CNA_HEADER_HUGO_SYMBOL = "Hugo_Symbol";
    public static final Integer DEFAULT_MAX_NUM_SAMPLES_TO_REMOVE = -1;
    public static final SimpleDateFormat CVR_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz");

    private static final String CENTER_MSKCC = "MSKCC";
    private static final String DEFAULT_BUILD_NUMBER = "37";
    private static final String DEFAULT_STRAND = "+";
    private static final String VALIDATION_STATUS_UNKNOWN = "Unknown";
    private static final String DEFAULT_IMPACT_SEQUENCER = "MSK-IMPACT";

    Logger log = Logger.getLogger(CVRUtilities.class);
    private String genesStableId;
    private String genesDescription;
    private String genesCancerStudyId;
    private List<String> geneSymbols;

    private List<String> variationList = Arrays.asList(new String[] {
        "INS", "SNP", "DNP", "TNP", "ONP"});

    public static Map<String, List<String>> DATATYPES_TO_SKIP_BY_STUDY = datatypesToSkipByStudy();
    private static Map<String, List<String>> datatypesToSkipByStudy() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("mskarcher", Arrays.asList(new String[]{"mutations", "cna", "seg"}));
        map.put("mskraindance", Arrays.asList(new String[]{"cna", "seg", "sv"}));
        map.put("mskaccess", Arrays.asList(new String[]{"seg"}));
        return map;
    }

    public static List<String> DEFAULT_GENETIC_PROFILES = new LinkedList(Arrays.asList(new String[]{"mutations"}));
    public static Map<String, List<String>> GENETIC_PROFILES_BY_STUDY = geneticProfilesByStudy();
    private static Map<String, List<String>> geneticProfilesByStudy() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("mskimpact", new LinkedList(Arrays.asList(new String[]{"mutations", "cna"})));
        map.put("mskimpact_heme", new LinkedList(Arrays.asList(new String[]{"mutations", "cna"})));
        map.put("mskarcher", new LinkedList(Arrays.asList(new String[]{"mutations"})));
        map.put("mskraindance", new LinkedList(Arrays.asList(new String[]{"mutations"})));
        map.put("mskaccess", new LinkedList(Arrays.asList(new String[]{"mutations", "cna"})));
        return map;
    }

    public CVRUtilities() {}

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
                bamFile, sequencer, tumorSampleUUID, matchedNormSampleUUID, tRefCount, nRefCount, tAltCount, nAltCount, "", "", "", "", "", "", "", "", "", "", "", "", additionalProperties);
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
        String tRefCount = String.valueOf(snp.getDepth() - snp.getAlleleDepth());
        String nRefCount = "";
        String tAltCount = String.valueOf(snp.getAlleleDepth());
        String nAltCount = "";
        String cDNA_Change = snp.getCDNAChange();
        String aminoAcidChange = snp.getAaChange();
        String transcript = snp.getTranscriptId();
        String comments = (snp.getInterpretation() != null ? snp.getInterpretation() : "").replaceAll("\r\n", " ").replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("\r", " ");
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

    public String getVariantAsHgvs(MutationRecord record) {
        String variant = record.getCHROMOSOME() + ":g." + record.getSTART_POSITION();
        if (record.getVARIANT_TYPE().equalsIgnoreCase("INS") || record.getVARIANT_TYPE().equalsIgnoreCase("DEL")) {
            variant += "_" + record.getEND_POSITION();
            // want to capture INDEL details, so modify variant string with both if applicable
            if (!StringUtils.isEmpty(record.getTUMOR_SEQ_ALLELE2()) && !record.getTUMOR_SEQ_ALLELE2().equals("-")) {
                variant += "ins" + record.getTUMOR_SEQ_ALLELE2();
            }
            if (!StringUtils.isEmpty(record.getREFERENCE_ALLELE()) && !record.getREFERENCE_ALLELE().equals("-")) {
                variant += "del" + record.getREFERENCE_ALLELE();
            }
        }
        else {
            variant += record.getREFERENCE_ALLELE() + ">" + record.getTUMOR_SEQ_ALLELE2();
        }
        return variant;
    }

    /**
     * Calculates the age at seq report for a list of CVRClinical records given a patient age
     * and the reference calculation date.
     * @param referenceCalculationDate
     * @param records
     * @param patientAge
     * @throws ParseException
     */
    public void calculateAgeAtSeqReportedYearsForPatient(Date referenceCalculationDate, List<CVRClinicalRecord> records, String patientAge) throws ParseException {
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
                    record.setAGE_AT_SEQ_REPORTED_YEARS(">90");
                } else {
                    record.setAGE_AT_SEQ_REPORTED_YEARS(String.valueOf(ageAtSeqReport.intValue()));
                }
            }
            else {
                record.setAGE_AT_SEQ_REPORTED_YEARS("NA");
            }
        }
    }

    public String convertWhitespace(String s) {
        return s.replaceAll("^[\\t|\\n|\\r]+", "").replaceAll("[\\t|\\n|\\r]+$", "").replaceAll("[\\t|\\n|\\r]+", " ");
    }

    public String[] getFileHeader(File dataFile) throws IOException {
        String[] columnNames;

        try (FileReader reader = new FileReader(dataFile)) {
            BufferedReader buff = new BufferedReader(reader);
            String line = buff.readLine();

            // keep reading until line does not start with meta data prefix
            while (line.startsWith(METADATA_PREFIX)) {
                line = buff.readLine();
            }
            // extract the maf file header
            columnNames = splitDataFields(line);
            reader.close();
        }

        return columnNames;
    }

    private String[] splitDataFields(String line) {
        line = line.replaceAll("^" + METADATA_PREFIX + "+", "");
        String[] fields = line.split(DELIMITER, -1);
        return fields;
    }

}
