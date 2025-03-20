/*
 * Copyright (c) 2016, 2017, 2023, 2025 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.mutation;

import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.models.AnnotatedRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class CVRMutationDataProcessor implements ItemProcessor<AnnotatedRecord, String> {

    @Value("#{stepExecutionContext['mutationHeader']}")
    private List<String> header;

    @Autowired
    private CVRUtilities cvrUtilities;

    private static final String REFERENCE_ALLELE_COLUMN = "REFERENCE_ALLELE";
    private static final String TUMOR_SEQ_ALLELE1_COLUMN = "TUMOR_SEQ_ALLELE1";

    @Override
    public String process(AnnotatedRecord record) {
        List<String> output = new ArrayList<>();

        for (String field : header) {
            // always override 'Tumor_Seq_Allele1' with value of 'Reference_Allele'
            // these fields should always match for our internal datasets
            if (TUMOR_SEQ_ALLELE1_COLUMN.equalsIgnoreCase(field)) {
                field = REFERENCE_ALLELE_COLUMN;
            }
            String value = getFieldValue(record, field);
            if (value == null) {
                value = "";
            } else {
                value = cvrUtilities.convertWhitespace(value);
            }
            output.add(value);
        }
        return String.join("\t", output);
    }

    /**
     * Retrieves the field value from AnnotatedRecord.
     */
    private String getFieldValue(AnnotatedRecord record, String field) {
        switch (field.toUpperCase()) {
            case "HGVSC": return record.getHGVSC();
            case "HGVSP": return record.getHGVSP();
            case "HGVSP_SHORT": return record.getHGVSP_SHORT();
            case "TRANSCRIPT_ID": return record.getTRANSCRIPT_ID();
            case "REFSEQ": return record.getREFSEQ();
            case "PROTEIN_POSITION": return record.getPROTEIN_POSITION();
            case "CODONS": return record.getCODONS();
            case "EXON_NUMBER": return record.getEXON_NUMBER();
            case "HOTSPOT": return record.getHOTSPOT();
            case "CONSEQUENCE": return record.getCONSEQUENCE();
            case "GNOMAD_AF": return record.getGNOMAD_AF();
            case "GNOMAD_AFR_AF": return record.getGNOMAD_AFR_AF();
            case "GNOMAD_AMR_AF": return record.getGNOMAD_AMR_AF();
            case "GNOMAD_ASJ_AF": return record.getGNOMAD_ASJ_AF();
            case "GNOMAD_EAS_AF": return record.getGNOMAD_EAS_AF();
            case "GNOMAD_FIN_AF": return record.getGNOMAD_FIN_AF();
            case "GNOMAD_NFE_AF": return record.getGNOMAD_NFE_AF();
            case "GNOMAD_OTH_AF": return record.getGNOMAD_OTH_AF();
            case "GNOMAD_SAS_AF": return record.getGNOMAD_SAS_AF();
            case "ANNOTATION_STATUS": return record.getANNOTATION_STATUS();
            case "HUGO_SYMBOL": return record.getHUGO_SYMBOL();
            case "ENTREZ_GENE_ID": return record.getENTREZ_GENE_ID();
            case "CENTER": return record.getCENTER();
            case "NCBI_BUILD": return record.getNCBI_BUILD();
            case "CHROMOSOME": return record.getCHROMOSOME();
            case "START_POSITION": return record.getSTART_POSITION();
            case "END_POSITION": return record.getEND_POSITION();
            case "STRAND": return record.getSTRAND();
            case "VARIANT_CLASSIFICATION": return record.getVARIANT_CLASSIFICATION();
            case "VARIANT_TYPE": return record.getVARIANT_TYPE();
            case "REFERENCE_ALLELE": return record.getREFERENCE_ALLELE();
            case "TUMOR_SEQ_ALLELE1": return record.getTUMOR_SEQ_ALLELE1();
            case "TUMOR_SEQ_ALLELE2": return record.getTUMOR_SEQ_ALLELE2();
            case "DBSNP_RS": return record.getDBSNP_RS();
            case "DBSNP_VAL_STATUS": return record.getDBSNP_VAL_STATUS();
            case "TUMOR_SAMPLE_BARCODE": return record.getTUMOR_SAMPLE_BARCODE();
            case "MATCHED_NORM_SAMPLE_BARCODE": return record.getMATCHED_NORM_SAMPLE_BARCODE();
            case "MATCH_NORM_SEQ_ALLELE1": return record.getMATCH_NORM_SEQ_ALLELE1();
            case "MATCH_NORM_SEQ_ALLELE2": return record.getMATCH_NORM_SEQ_ALLELE2();
            case "TUMOR_VALIDATION_ALLELE1": return record.getTUMOR_VALIDATION_ALLELE1();
            case "TUMOR_VALIDATION_ALLELE2": return record.getTUMOR_VALIDATION_ALLELE2();
            case "MATCH_NORM_VALIDATION_ALLELE1": return record.getMATCH_NORM_VALIDATION_ALLELE1();
            case "MATCH_NORM_VALIDATION_ALLELE2": return record.getMATCH_NORM_VALIDATION_ALLELE2();
            case "VERIFICATION_STATUS": return record.getVERIFICATION_STATUS();
            case "VALIDATION_STATUS": return record.getVALIDATION_STATUS();
            case "MUTATION_STATUS": return record.getMUTATION_STATUS();
            case "SEQUENCING_PHASE": return record.getSEQUENCING_PHASE();
            case "SEQUENCE_SOURCE": return record.getSEQUENCE_SOURCE();
            case "VALIDATION_METHOD": return record.getVALIDATION_METHOD();
            case "SCORE": return record.getSCORE();
            case "BAM_FILE": return record.getBAM_FILE();
            case "SEQUENCER": return record.getSEQUENCER();
            case "TUMOR_SAMPLE_UUID": return record.getTUMOR_SAMPLE_UUID();
            case "MATCHED_NORM_SAMPLE_UUID": return record.getMATCHED_NORM_SAMPLE_UUID();
            case "T_REF_COUNT": return record.getT_REF_COUNT();
            case "T_ALT_COUNT": return record.getT_ALT_COUNT();
            case "N_REF_COUNT": return record.getN_REF_COUNT();
            case "N_ALT_COUNT": return record.getN_ALT_COUNT();
            default:
                return record.getAdditionalProperties().getOrDefault(field, "");
        }
    }
}
